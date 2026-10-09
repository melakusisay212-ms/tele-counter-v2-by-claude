package et.teleexpense

/** Everything the Home screen shows, derived from one list of expenses and the user's own numbers. */
data class Breakdown(
    /** Cash that really left a bank or telebirr. Airtime-funded items never add to this. */
    val total: Double,
    val count: Int,
    /** Voice, Data, SMS, Combo, Others. Includes bundles bought from airtime. */
    val byGroup: Map<String, Double>,
    /** Airtime recharged but not (yet) used for a bundle or a transfer. */
    val unassigned: Double,
    /** Airtime-funded items beyond what was recharged (e.g. gifted airtime was used). Normally 0. */
    val overflow: Double,
)

object Spending {
    val groups = listOf("Voice", "Data", "SMS", "Combo", "Others")

    fun last9(n: String) = n.filter { it.isDigit() }.takeLast(9)

    fun isMine(number: String?, mine: Set<String>) = number != null && last9(number) in mine

    fun groupOf(t: ExpenseTx, mine: Set<String>): String {
        if (t.category == "airtime_sent") return if (isMine(t.recipient, mine)) "Self" else "Others"
        if (mine.isNotEmpty() && t.recipient != null && !isMine(t.recipient, mine)) return "Others"
        return CategoryClassifier.groupOf(t.category)
    }

    fun breakdown(list: List<ExpenseTx>, mine: Set<String>): Breakdown {
        val map = groups.associateWith { 0.0 }.toMutableMap()
        var total = 0.0
        var count = 0
        var pool = 0.0
        var allocated = 0.0
        for (t in list) {
            val g = groupOf(t, mine)
            if (t.isCash) {
                total += t.amount
                count++
                if (g == "Airtime") pool += t.amount else map[g] = (map[g] ?: 0.0) + t.amount
            } else {
                if (g == "Self") continue
                allocated += t.amount
                map[g] = (map[g] ?: 0.0) + t.amount
            }
        }
        return Breakdown(total, count, map, maxOf(0.0, pool - allocated), maxOf(0.0, allocated - pool))
    }
}

/** Finds the user's own numbers inside a message, without any extra permission. Stored as the last 9 digits. */
object OwnNumbers {
    private val patterns = listOf(
        Regex("""your service number (\d{9,12})""", RegexOption.IGNORE_CASE),
        Regex("""to your telebirr account (\d{9,12})""", RegexOption.IGNORE_CASE),
    )

    fun extract(text: String): Set<String> =
        patterns.flatMap { p -> p.findAll(SmsParser.normalise(text)).map { Spending.last9(it.groupValues[1]) }.toList() }.toSet()
}
