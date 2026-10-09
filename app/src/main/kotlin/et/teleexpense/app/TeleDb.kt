package et.teleexpense.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import et.teleexpense.Candidate
import et.teleexpense.Classification
import et.teleexpense.ExpenseTx
import et.teleexpense.TxStatus
import java.time.LocalDateTime

/**
 * Plain SQLite (no annotation processing). Stores extracted fields only.
 * The raw SMS body is never written; sms_hash is a one-way SHA-256 used to ignore duplicate deliveries.
 */
class TeleDb(context: Context) : SQLiteOpenHelper(context.applicationContext, "tele.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE candidates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sms_hash TEXT NOT NULL UNIQUE,
                classification TEXT NOT NULL,
                provider TEXT NOT NULL,
                category TEXT NOT NULL,
                amount REAL,
                currency TEXT NOT NULL,
                direction TEXT NOT NULL,
                status TEXT NOT NULL,
                package_name TEXT,
                recipient TEXT,
                transaction_id TEXT,
                transfer_id TEXT,
                date_time TEXT NOT NULL,
                received_at TEXT NOT NULL,
                date_explicit INTEGER NOT NULL,
                reason TEXT NOT NULL,
                confidence REAL NOT NULL,
                source TEXT NOT NULL DEFAULT 'paid'
            )"""
        )
        db.execSQL(
            """CREATE TABLE transactions (
                tx_key TEXT PRIMARY KEY,
                provider TEXT NOT NULL,
                category TEXT NOT NULL,
                amount REAL NOT NULL,
                package_name TEXT,
                recipient TEXT,
                transaction_id TEXT,
                date_time TEXT NOT NULL,
                sms_count INTEGER NOT NULL,
                confidence REAL NOT NULL,
                user_edited INTEGER NOT NULL,
                status TEXT NOT NULL,
                source TEXT NOT NULL DEFAULT 'paid'
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Never drop user data silently.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE candidates ADD COLUMN source TEXT NOT NULL DEFAULT 'paid'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'paid'")
        }
    }

    // ---------- candidates ----------

    fun insertCandidate(c: Candidate, hash: String): Boolean {
        val v = ContentValues().apply {
            put("sms_hash", hash)
            put("classification", c.classification.name)
            put("provider", c.provider)
            put("category", c.category)
            put("amount", c.amount)   // null stays null
            put("currency", c.currency)
            put("direction", c.direction)
            put("status", c.status)
            put("package_name", c.packageName)
            put("recipient", c.recipient)
            put("transaction_id", c.transactionId)
            put("transfer_id", c.transferId)
            put("date_time", c.dateTime.toString())
            put("received_at", c.receivedAt.toString())
            put("date_explicit", if (c.dateIsExplicit) 1 else 0)
            put("reason", c.reason)
            put("confidence", c.confidence)
            put("source", c.source)
        }
        return writableDatabase.insertWithOnConflict("candidates", null, v, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    fun candidates(): List<Candidate> {
        val out = ArrayList<Candidate>()
        readableDatabase.rawQuery("SELECT * FROM candidates", null).use { c ->
            while (c.moveToNext()) {
                out += Candidate(
                    classification = Classification.valueOf(c.str("classification")),
                    provider = c.str("provider"),
                    category = c.str("category"),
                    amount = c.dbl("amount"),
                    currency = c.str("currency"),
                    direction = c.str("direction"),
                    status = c.str("status"),
                    packageName = c.strOrNull("package_name"),
                    recipient = c.strOrNull("recipient"),
                    transactionId = c.strOrNull("transaction_id"),
                    transferId = c.strOrNull("transfer_id"),
                    dateTime = LocalDateTime.parse(c.str("date_time")),
                    receivedAt = LocalDateTime.parse(c.str("received_at")),
                    dateIsExplicit = c.int("date_explicit") == 1,
                    reason = c.str("reason"),
                    confidence = c.dbl("confidence") ?: 0.0,
                    source = c.str("source"),
                )
            }
        }
        return out
    }

    // ---------- transactions ----------

    fun transactions(): List<ExpenseTx> {
        val out = ArrayList<ExpenseTx>()
        readableDatabase.rawQuery("SELECT * FROM transactions ORDER BY date_time DESC", null).use { c ->
            while (c.moveToNext()) out += c.toTx()
        }
        return out
    }

    fun upsertTransaction(t: ExpenseTx) {
        val v = ContentValues().apply {
            put("tx_key", t.key)
            put("provider", t.provider)
            put("category", t.category)
            put("amount", t.amount)
            put("package_name", t.packageName)
            put("recipient", t.recipient)
            put("transaction_id", t.transactionId)
            put("date_time", t.dateTime.toString())
            put("sms_count", t.smsCount)
            put("confidence", t.confidence)
            put("user_edited", if (t.userEdited) 1 else 0)
            put("status", t.status.name)
            put("source", t.source)
        }
        writableDatabase.insertWithOnConflict("transactions", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Remove automatically created transactions that regrouping no longer produces. User-edited rows are never removed. */
    fun deleteAutoTransactionsNotIn(keep: Set<String>) {
        val db = writableDatabase
        val stale = ArrayList<String>()
        db.rawQuery("SELECT tx_key FROM transactions WHERE user_edited = 0", null).use { c ->
            while (c.moveToNext()) { val k = c.getString(0); if (k !in keep) stale += k }
        }
        db.beginTransaction()
        try {
            for (k in stale) db.delete("transactions", "tx_key = ?", arrayOf(k))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun setStatus(key: String, status: TxStatus) {
        val v = ContentValues().apply { put("status", status.name); put("user_edited", 1) }
        writableDatabase.update("transactions", v, "tx_key = ?", arrayOf(key))
    }

    fun edit(key: String, category: String, amount: Double, dateTime: LocalDateTime) {
        val v = ContentValues().apply {
            put("category", category); put("amount", amount); put("date_time", dateTime.toString()); put("user_edited", 1)
        }
        writableDatabase.update("transactions", v, "tx_key = ?", arrayOf(key))
    }

    fun hiddenCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM transactions WHERE status != 'ACTIVE'", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun restoreHidden() {
        val v = ContentValues().apply { put("status", TxStatus.ACTIVE.name); put("user_edited", 1) }
        writableDatabase.update("transactions", v, "status != ?", arrayOf(TxStatus.ACTIVE.name))
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete("candidates", null, null)
        db.delete("transactions", null, null)
    }

    // ---------- cursor helpers ----------

    private fun Cursor.idx(n: String) = getColumnIndexOrThrow(n)
    private fun Cursor.str(n: String): String = getString(idx(n))
    private fun Cursor.strOrNull(n: String): String? = idx(n).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.dbl(n: String): Double? = idx(n).let { if (isNull(it)) null else getDouble(it) }
    private fun Cursor.int(n: String): Int = getInt(idx(n))

    private fun Cursor.toTx() = ExpenseTx(
        key = str("tx_key"),
        provider = str("provider"),
        category = str("category"),
        amount = dbl("amount") ?: 0.0,
        packageName = strOrNull("package_name"),
        recipient = strOrNull("recipient"),
        transactionId = strOrNull("transaction_id"),
        dateTime = LocalDateTime.parse(str("date_time")),
        smsCount = int("sms_count"),
        confidence = dbl("confidence") ?: 0.0,
        userEdited = int("user_edited") == 1,
        status = runCatching { TxStatus.valueOf(str("status")) }.getOrDefault(TxStatus.ACTIVE),
        source = str("source"),
    )
}
