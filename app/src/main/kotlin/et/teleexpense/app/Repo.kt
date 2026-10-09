package et.teleexpense.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import et.teleexpense.Candidate
import et.teleexpense.Classification
import et.teleexpense.OwnNumbers
import et.teleexpense.SampleData
import et.teleexpense.Spending
import et.teleexpense.EthDate
import et.teleexpense.EthiopianCalendar
import et.teleexpense.ExpenseTx
import et.teleexpense.SmsParser
import et.teleexpense.TransactionGrouper
import et.teleexpense.TxStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("tele_prefs", Context.MODE_PRIVATE)
    var startedAt: Long get() = p.getLong("startedAt", 0L); set(v) { p.edit().putLong("startedAt", v).apply() }
    var lastProcessed: Long get() = p.getLong("lastProcessed", 0L); set(v) { p.edit().putLong("lastProcessed", v).apply() }
    var lastScanAt: Long get() = p.getLong("lastScanAt", 0L); set(v) { p.edit().putLong("lastScanAt", v).apply() }
    var samplesLoaded: Boolean get() = p.getBoolean("samplesLoaded", false); set(v) { p.edit().putBoolean("samplesLoaded", v).apply() }
    val isSetUp: Boolean get() = startedAt != 0L

    /** The user's own numbers (last 9 digits), learned from messages or added by hand. */
    var myNumbers: Set<String>
        get() = p.getStringSet("myNumbers", emptySet()).orEmpty().toSet()
        set(v) { p.edit().putStringSet("myNumbers", v.toSet()).apply() }
    /** Numbers the user removed; never re-added automatically. */
    var removedNumbers: Set<String>
        get() = p.getStringSet("removedNumbers", emptySet()).orEmpty().toSet()
        set(v) { p.edit().putStringSet("removedNumbers", v.toSet()).apply() }
    /** Names the user gave to numbers (mine or other people's), stored as "number|name". */
    var numberNames: Map<String, String>
        get() = p.getStringSet("numberNames", emptySet()).orEmpty().mapNotNull { e ->
            val i = e.indexOf('|'); if (i <= 0) null else e.substring(0, i) to e.substring(i + 1)
        }.toMap()
        set(v) { p.edit().putStringSet("numberNames", v.map { "${it.key}|${it.value}" }.toSet()).apply() }
    fun reset() { p.edit().clear().apply() }
}

data class ScanResult(val relevant: Int, val events: Int, val newExpenses: Int, val error: String? = null)

/** The only place that touches the database, the SMS inbox and the grouping engine. Thread-safe. */
class Repo private constructor(private val app: Context) {
    val prefs = Prefs(app)
    val txs = MutableStateFlow<List<ExpenseTx>>(emptyList())
    val mine = MutableStateFlow<Set<String>>(prefs.myNumbers)
    val names = MutableStateFlow<Map<String, String>>(prefs.numberNames)

    private val db = TeleDb(app)
    private val lock = Mutex()
    private val grouper = TransactionGrouper()
    private val zone: ZoneId get() = ZoneId.systemDefault()

    companion object {
        @Volatile private var instance: Repo? = null
        fun get(context: Context): Repo = instance ?: synchronized(this) {
            instance ?: Repo(context.applicationContext).also { instance = it }
        }
    }

    fun hasSmsPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            app.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    // ---------- loading ----------

    suspend fun load() = withContext(Dispatchers.IO) { lock.withLock { txs.value = db.transactions() } }

    suspend fun hiddenCount(): Int = withContext(Dispatchers.IO) { lock.withLock { db.hiddenCount() } }

    // ---------- setup & scanning ----------

    /** First run. "This month" reads only the current Ethiopian month, never the whole inbox. */
    suspend fun initialScan(thisMonth: Boolean): ScanResult = withContext(Dispatchers.IO) {
        lock.withLock {
            val now = System.currentTimeMillis()
            prefs.startedAt = now
            prefs.lastProcessed = now
            if (!thisMonth) { prefs.lastScanAt = now; ScanResult(0, 0, 0) }
            else scanLocked(currentEthMonthStartMillis())
        }
    }

    /** "Scan New Messages": only after the last processed timestamp. */
    suspend fun scanNew(): ScanResult = withContext(Dispatchers.IO) {
        lock.withLock {
            // Re-read a recent window instead of trusting a "last processed" timestamp: duplicates are
            // dropped by the per-SMS hash, so this can never double count, and it cannot miss messages
            // whose timestamps arrive out of order.
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            scanLocked(maxOf(trackingFloor(), weekAgo))
        }
    }

    /** Called by the live SMS receiver. */
    suspend fun onIncoming(sender: String?, body: String, timestampMillis: Long) = withContext(Dispatchers.IO) {
        lock.withLock {
            if (!prefs.isSetUp || timestampMillis < trackingFloor()) return@withLock
            if (ingestLocked(sender, body, timestampMillis)) regroupLocked()
            if (timestampMillis > prefs.lastProcessed) prefs.lastProcessed = timestampMillis
        }
    }

    /** Tracking counts everything from the start of the day it was switched on ("Start From Today" includes today). */
    private fun trackingFloor(): Long =
        java.time.Instant.ofEpochMilli(prefs.startedAt).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    private fun currentEthMonthStartMillis(): Long {
        val e = EthiopianCalendar.today()
        return EthiopianCalendar.toGregorian(EthDate(e.year, e.month, 1)).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun scanLocked(sinceMillis: Long): ScanResult {
        if (!hasSmsPermission()) return ScanResult(0, 0, 0, "SMS access is not granted. Allow it in Settings to scan messages.")
        val before = activeCount()
        var relevant = 0
        var events = 0
        var newest = sinceMillis
        try {
            app.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(sinceMillis.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val sender = c.getString(0)
                    val body = c.getString(1) ?: continue
                    val ts = c.getLong(2)
                    if (ts > newest) newest = ts
                    val cand = analyse(sender, body, ts) ?: continue
                    relevant++
                    if (cand.classification != Classification.IGNORE) events++
                    db.insertCandidate(cand, hash(sender, body, ts))
                }
            }
        } catch (e: SecurityException) {
            return ScanResult(0, 0, 0, "SMS access was denied by the system.")
        } catch (e: RuntimeException) {
            return ScanResult(relevant, events, 0, "Could not read messages on this device.")
        }
        if (newest > prefs.lastProcessed) prefs.lastProcessed = newest
        prefs.lastScanAt = System.currentTimeMillis()
        regroupLocked()
        return ScanResult(relevant, events, (activeCount() - before).coerceAtLeast(0))
    }

    private fun analyse(sender: String?, body: String, ts: Long): Candidate? {
        learnNumbers(body)
        val received = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), zone)
        val cand = SmsParser.parse(sender, body, received)
        return if (cand.isRelevant) cand else null   // unrelated SMS are dropped and never stored
    }

    private fun ingestLocked(sender: String?, body: String, ts: Long): Boolean {
        val cand = analyse(sender, body, ts) ?: return false
        db.insertCandidate(cand, hash(sender, body, ts))
        return true
    }

    private fun activeCount() = db.transactions().count { it.status == TxStatus.ACTIVE }

    private fun hash(sender: String?, body: String, ts: Long): String {
        val d = MessageDigest.getInstance("SHA-256").digest("${sender.orEmpty()}|$ts|$body".toByteArray())
        return d.joinToString("") { "%02x".format(it) }.take(32)
    }

    // ---------- grouping ----------

    private fun keyOf(c: Candidate) =
        c.transactionId ?: c.transferId ?: "${c.category}|${c.amount}|${c.recipient.orEmpty()}|${c.receivedAt}"

    private fun regroupLocked() {
        val groups = grouper.group(db.candidates())
        val existing = db.transactions().associateBy { it.key }
        val keep = HashSet<String>()
        for (g in groups) {
            val p = g.primary
            val key = keyOf(p)
            keep += key
            val old = existing[key]
            if (old != null && old.userEdited) {
                // Never overwrite the user's corrections; only refresh the grouped-SMS count.
                db.upsertTransaction(old.copy(smsCount = g.smsCount))
            } else {
                db.upsertTransaction(
                    ExpenseTx(
                        key = key, provider = p.provider, category = p.category, amount = g.amount,
                        packageName = p.packageName, recipient = p.recipient,
                        transactionId = p.transactionId ?: p.transferId,
                        dateTime = p.dateTime, smsCount = g.smsCount, confidence = p.confidence,
                        source = p.source,
                    )
                )
            }
        }
        db.deleteAutoTransactionsNotIn(keep)
        txs.value = db.transactions()
    }

    // ---------- my numbers ----------

    /** Called under the lock. Only the number is kept, never the message. */
    private fun learnNumbers(body: String) {
        val found = OwnNumbers.extract(body) - prefs.removedNumbers
        if (found.isNotEmpty() && !prefs.myNumbers.containsAll(found)) {
            prefs.myNumbers = prefs.myNumbers + found
            mine.value = prefs.myNumbers
        }
    }

    fun addNumber(raw: String): Boolean {
        val n = Spending.last9(raw)
        if (n.length < 9) return false
        prefs.removedNumbers = prefs.removedNumbers - n
        prefs.myNumbers = prefs.myNumbers + n
        mine.value = prefs.myNumbers
        return true
    }

    fun removeNumber(n: String) {
        prefs.myNumbers = prefs.myNumbers - n
        prefs.removedNumbers = prefs.removedNumbers + n
        mine.value = prefs.myNumbers
    }

    /** Blank name removes the name. */
    fun setName(number: String, name: String) {
        val key = Spending.last9(number)
        val m = prefs.numberNames.toMutableMap()
        if (name.isBlank()) m.remove(key) else m[key] = name.trim()
        prefs.numberNames = m
        names.value = m
    }

    // ---------- user corrections ----------

    suspend fun update(key: String, category: String, amount: Double, dateTime: LocalDateTime) = withContext(Dispatchers.IO) {
        lock.withLock { db.edit(key, category, amount, dateTime); txs.value = db.transactions() }
    }

    suspend fun setStatus(key: String, status: TxStatus) = withContext(Dispatchers.IO) {
        lock.withLock { db.setStatus(key, status); txs.value = db.transactions() }
    }

    suspend fun restoreHidden() = withContext(Dispatchers.IO) {
        lock.withLock { db.restoreHidden(); txs.value = db.transactions() }
    }

    // ---------- tools ----------

    /** Runs fictional sample messages through the real pipeline. Starts tracking if it has not started yet. */
    suspend fun loadSamples(): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            if (prefs.samplesLoaded) return@withLock false
            if (!prefs.isSetUp) {
                val now = System.currentTimeMillis()
                prefs.startedAt = now
                prefs.lastProcessed = now
            }
            for (s in SampleData.build(LocalDateTime.now())) {
                ingestLocked(s.sender, s.body, s.received.atZone(zone).toInstant().toEpochMilli())
            }
            prefs.samplesLoaded = true
            regroupLocked()
            true
        }
    }

    suspend fun eraseEverything() = withContext(Dispatchers.IO) {
        lock.withLock { db.clearAll(); prefs.reset(); txs.value = emptyList(); mine.value = emptySet(); names.value = emptyMap() }
    }

    fun today(): LocalDate = LocalDate.now()
}
