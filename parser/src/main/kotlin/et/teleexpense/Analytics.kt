package et.teleexpense

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Summary(val total: Double, val count: Int, val avgPerDay: Double, val avgPerTx: Double?)

object Analytics {
    fun active(all: List<ExpenseTx>) = all.filter { it.status == TxStatus.ACTIVE }

    fun inRange(all: List<ExpenseTx>, r: DateRange) =
        active(all).filter { it.dateTime.toLocalDate() in r }.sortedByDescending { it.dateTime }

    fun summary(list: List<ExpenseTx>, r: DateRange, today: LocalDate = LocalDate.now()): Summary {
        val cash = list.filter { it.isCash }
        val total = cash.sumOf { it.amount }
        val end = if (today.isBefore(r.to)) today else r.to
        val elapsed = (ChronoUnit.DAYS.between(r.from, end) + 1).toInt().coerceIn(1, r.days)
        return Summary(total, cash.size, total / elapsed, if (cash.isEmpty()) null else total / cash.size)
    }

    /** (Ethiopian day-of-month label, amount) for every day of the range. */
    fun daily(list: List<ExpenseTx>, r: DateRange): List<Pair<String, Double>> =
        (0 until r.days).map { i ->
            val d = r.from.plusDays(i.toLong())
            EthiopianCalendar.fromGregorian(d).day.toString() to
                list.filter { it.isCash && it.dateTime.toLocalDate() == d }.sumOf { it.amount }
        }

    fun weekly(list: List<ExpenseTx>, r: DateRange): List<Pair<String, Double>> =
        (0 until (r.days + 6) / 7).map { w ->
            val a = r.from.plusDays(w * 7L)
            val b = a.plusDays(6)
            "W${w + 1}" to list.filter { val d = it.dateTime.toLocalDate(); it.isCash && !d.isBefore(a) && !d.isAfter(b) }.sumOf { it.amount }
        }

    /** 13 values, Meskerem..Pagume, for one Ethiopian year. */
    fun monthly(all: List<ExpenseTx>, ethYear: Int): List<Pair<String, Double>> =
        (1..13).map { m ->
            EthDate.MONTHS[m - 1].take(3) to active(all).filter { it.isCash && it.ethDate.year == ethYear && it.ethDate.month == m }.sumOf { it.amount }
        }

    fun topPurchases(list: List<ExpenseTx>, n: Int = 5): List<Triple<String, Int, Double>> =
        list.filter { it.isCash }.groupBy { it.title }.map { (k, v) -> Triple(k, v.size, v.sumOf { it.amount }) }
            .sortedByDescending { it.third }.take(n)
}
