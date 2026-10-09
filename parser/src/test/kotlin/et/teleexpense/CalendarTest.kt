package et.teleexpense

import java.time.LocalDate
import kotlin.test.*

class CalendarTest {
    private fun e(y: Int, m: Int, d: Int) = EthiopianCalendar.fromGregorian(LocalDate.of(y, m, d))

    @Test fun knownDates() {
        assertEquals(EthDate(2019, 1, 7), e(2026, 9, 17))   // from the spec
        assertEquals(EthDate(2019, 1, 1), e(2026, 9, 11))   // Enkutatash 2019
        assertEquals(EthDate(2018, 13, 5), e(2026, 9, 10))  // last day of Pagume 2018 (5 days)
        assertEquals(EthDate(2016, 1, 1), e(2023, 9, 12))   // new year after Ethiopian leap year 2015
        assertEquals(EthDate(2015, 13, 6), e(2023, 9, 11))  // Pagume 6 exists in leap year
        assertEquals(EthDate(2017, 1, 1), e(2024, 9, 11))
        assertEquals(EthDate(2018, 1, 1), e(2025, 9, 11))
        assertEquals(EthDate(2019, 1, 23), e(2026, 10, 3))
    }
    @Test fun gregorianLeapDay() {
        assertEquals(EthDate(2016, 6, 21), e(2024, 2, 29))
        assertEquals(EthDate(2016, 6, 22), e(2024, 3, 1))
    }
    @Test fun leapYearsAndMonthLengths() {
        assertTrue(EthiopianCalendar.isLeap(2015)); assertTrue(EthiopianCalendar.isLeap(2019))
        assertFalse(EthiopianCalendar.isLeap(2018)); assertFalse(EthiopianCalendar.isLeap(2020))
        assertEquals(6, EthiopianCalendar.daysInMonth(2019, 13)); assertEquals(5, EthiopianCalendar.daysInMonth(2018, 13))
        assertEquals(30, EthiopianCalendar.daysInMonth(2019, 1))
        assertTrue(EthiopianCalendar.isValid(2019, 13, 6)); assertFalse(EthiopianCalendar.isValid(2018, 13, 6))
        assertFalse(EthiopianCalendar.isValid(2019, 14, 1)); assertFalse(EthiopianCalendar.isValid(2019, 1, 31))
    }
    @Test fun roundTripEveryDayForTwentyYears() {
        var d = LocalDate.of(2010, 1, 1)
        repeat(365 * 20) {
            val eth = EthiopianCalendar.fromGregorian(d)
            assertTrue(EthiopianCalendar.isValid(eth.year, eth.month, eth.day), "invalid $eth for $d")
            assertEquals(d, EthiopianCalendar.toGregorian(eth))
            d = d.plusDays(1)
        }
    }
    @Test fun consecutiveDaysAreConsecutive() {
        var prev = EthiopianCalendar.fromGregorian(LocalDate.of(2020, 1, 1))
        var d = LocalDate.of(2020, 1, 2)
        repeat(3000) {
            val cur = EthiopianCalendar.fromGregorian(d)
            val expectNext = if (prev.day < EthiopianCalendar.daysInMonth(prev.year, prev.month)) EthDate(prev.year, prev.month, prev.day + 1)
            else if (prev.month < 13) EthDate(prev.year, prev.month + 1, 1) else EthDate(prev.year + 1, 1, 1)
            assertEquals(expectNext, cur, "after $prev on $d"); prev = cur; d = d.plusDays(1)
        }
    }
    @Test fun gridColumnAndShift() {
        assertEquals(5, EthiopianCalendar.firstDayColumn(2019, 1))  // Sep 11 2026 is a Friday
        assertEquals(2019 to 13, EthiopianCalendar.shiftMonth(2020, 1, -1))
        assertEquals(2019 to 1, EthiopianCalendar.shiftMonth(2018, 13, 1))
        assertEquals(2018 to 12, EthiopianCalendar.shiftMonth(2019, 1, -2))
    }
}
