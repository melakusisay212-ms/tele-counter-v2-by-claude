package et.teleexpense

import java.time.LocalDate

data class EthDate(val year: Int, val month: Int, val day: Int) : Comparable<EthDate> {
    val monthName: String get() = MONTHS[month - 1]
    override fun compareTo(other: EthDate): Int =
        compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
    override fun toString() = "$monthName $day, $year"

    companion object {
        val MONTHS = listOf(
            "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit", "Megabit",
            "Miazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume"
        )
    }
}

/**
 * Julian-day-number based conversion. No fixed offsets.
 * Ethiopian leap year: year % 4 == 3 (Pagume has 6 days instead of 5).
 */
object EthiopianCalendar {
    private const val ETH_EPOCH_JDN = 1723856L
    private const val UNIX_EPOCH_JDN = 2440588L

    fun isLeap(year: Int): Boolean = Math.floorMod(year, 4) == 3

    fun daysInMonth(year: Int, month: Int): Int = when {
        month in 1..12 -> 30
        month == 13 -> if (isLeap(year)) 6 else 5
        else -> throw IllegalArgumentException("Ethiopian month must be 1..13, was $month")
    }

    fun isValid(year: Int, month: Int, day: Int): Boolean =
        month in 1..13 && day in 1..daysInMonth(year, month)

    fun fromGregorian(d: LocalDate): EthDate {
        val days = d.toEpochDay() + UNIX_EPOCH_JDN - ETH_EPOCH_JDN
        val r = Math.floorMod(days, 1461L)
        val q = Math.floorDiv(days, 1461L)
        val n = r % 365 + 365 * (r / 1460)
        val year = 4 * q + r / 365 - r / 1460
        return EthDate(year.toInt(), (n / 30 + 1).toInt(), (n % 30 + 1).toInt())
    }

    fun toGregorian(e: EthDate): LocalDate {
        val jdn = ETH_EPOCH_JDN + 365L * e.year + Math.floorDiv(e.year, 4) +
            30L * (e.month - 1) + e.day - 1
        return LocalDate.ofEpochDay(jdn - UNIX_EPOCH_JDN)
    }

    fun today(): EthDate = fromGregorian(LocalDate.now())

    /** Column in a Su..Sa grid (Sunday = 0). */
    fun weekdayColumn(date: LocalDate): Int = date.dayOfWeek.value % 7

    fun firstDayColumn(year: Int, month: Int): Int = weekdayColumn(toGregorian(EthDate(year, month, 1)))

    /** Shift an Ethiopian (year, month) by n months, 13 months per year. */
    fun shiftMonth(year: Int, month: Int, by: Int): Pair<Int, Int> {
        val idx = year * 13 + (month - 1) + by
        return Math.floorDiv(idx, 13) to Math.floorMod(idx, 13) + 1
    }
}
