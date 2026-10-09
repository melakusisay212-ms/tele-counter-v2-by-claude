package et.teleexpense

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class Period(val label: String) {
    TODAY("Today"), WEEK("This Week"), MONTH("This Month"), YEAR("This Year"), CUSTOM("Custom")
}

data class DateRange(val from: LocalDate, val to: LocalDate, val title: String) {
    val days: Int get() = (ChronoUnit.DAYS.between(from, to) + 1).toInt()
    operator fun contains(d: LocalDate) = !d.isBefore(from) && !d.isAfter(to)
}

object Periods {
    private fun monthStart(y: Int, m: Int) = EthiopianCalendar.toGregorian(EthDate(y, m, 1))

    /** offset moves the period back (negative) or forward in whole days / weeks / months / years. */
    fun range(period: Period, offset: Int = 0, today: LocalDate = LocalDate.now(),
              customFrom: EthDate? = null, customTo: EthDate? = null): DateRange = when (period) {
        Period.TODAY -> {
            val d = today.plusDays(offset.toLong())
            DateRange(d, d, EthiopianCalendar.fromGregorian(d).toString())
        }
        Period.WEEK -> {
            val s = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusWeeks(offset.toLong())
            val e = s.plusDays(6)
            val a = EthiopianCalendar.fromGregorian(s)
            val b = EthiopianCalendar.fromGregorian(e)
            val title = if (a.month == b.month) "${a.monthName} ${a.day}–${b.day}, ${b.year}"
            else "${a.monthName} ${a.day} – ${b.monthName} ${b.day}, ${b.year}"
            DateRange(s, e, title)
        }
        Period.MONTH -> {
            val now = EthiopianCalendar.fromGregorian(today)
            val (y, m) = EthiopianCalendar.shiftMonth(now.year, now.month, offset)
            val (ny, nm) = EthiopianCalendar.shiftMonth(y, m, 1)
            DateRange(monthStart(y, m), monthStart(ny, nm).minusDays(1), "${EthDate.MONTHS[m - 1]} $y")
        }
        Period.YEAR -> {
            val y = EthiopianCalendar.fromGregorian(today).year + offset
            DateRange(monthStart(y, 1), monthStart(y + 1, 1).minusDays(1), "$y")
        }
        Period.CUSTOM -> {
            val now = EthiopianCalendar.fromGregorian(today)
            val a = customFrom ?: EthDate(now.year, now.month, 1)
            val b = customTo ?: now
            val (lo, hi) = if (a <= b) a to b else b to a
            DateRange(EthiopianCalendar.toGregorian(lo), EthiopianCalendar.toGregorian(hi), "$lo – $hi")
        }
    }
}
