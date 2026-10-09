package et.teleexpense

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.*

class PeriodsAnalyticsTest {
    private val today = LocalDate.of(2026, 10, 3)   // Meskerem 23, 2019

    private fun tx(key: String, cat: String, amt: Double, d: LocalDateTime, name: String? = null, status: TxStatus = TxStatus.ACTIVE) =
        ExpenseTx(key, "telebirr", cat, amt, name, null, null, d, 1, 0.9, false, status)

    @Test fun monthRangeIsMeskerem2019() {
        val r = Periods.range(Period.MONTH, 0, today)
        assertEquals("Meskerem 2019", r.title)
        assertEquals(LocalDate.of(2026, 9, 11), r.from); assertEquals(LocalDate.of(2026, 10, 10), r.to); assertEquals(30, r.days)
    }
    @Test fun previousMonthIsPagume2018WithFiveDays() {
        val r = Periods.range(Period.MONTH, -1, today)
        assertEquals("Pagume 2018", r.title); assertEquals(5, r.days)
        assertEquals(LocalDate.of(2026, 9, 6), r.from); assertEquals(LocalDate.of(2026, 9, 10), r.to)
    }
    @Test fun pagumeInLeapYearHasSixDays() {
        // Pagume 2019 is the month 13 months after Meskerem 2019 minus 1 -> offset 12 from Meskerem 2019
        val r = Periods.range(Period.MONTH, 12, today)
        assertEquals("Pagume 2019", r.title); assertEquals(6, r.days)
    }
    @Test fun yearRange() {
        val r = Periods.range(Period.YEAR, 0, today)
        assertEquals("2019", r.title); assertEquals(LocalDate.of(2026, 9, 11), r.from); assertEquals(366, r.days)
    }
    @Test fun todayAndWeek() {
        assertEquals("Meskerem 23, 2019", Periods.range(Period.TODAY, 0, today).title)
        val w = Periods.range(Period.WEEK, 0, today)
        assertEquals(7, w.days); assertTrue(today in w); assertEquals(java.time.DayOfWeek.SUNDAY, w.from.dayOfWeek)
    }
    @Test fun customRangeAcceptsEitherOrder() {
        val a = EthDate(2019, 1, 7); val b = EthDate(2019, 1, 20)
        val r1 = Periods.range(Period.CUSTOM, 0, today, a, b); val r2 = Periods.range(Period.CUSTOM, 0, today, b, a)
        assertEquals(r1, r2); assertEquals(14, r1.days)
    }
    @Test fun analyticsTotalsAndGroups() {
        val list = listOf(
            tx("1", "data_package", 35.0, LocalDateTime.of(2026, 9, 11, 15, 52), "Student pack"),
            tx("2", "data_package", 35.0, LocalDateTime.of(2026, 9, 17, 8, 38), "Student pack"),
            tx("3", "airtime_recharge", 100.0, LocalDateTime.of(2026, 9, 20, 10, 0)),
            tx("4", "airtime", 5.0, LocalDateTime.of(2026, 9, 17, 9, 0)),
            tx("5", "sms_package", 10.0, LocalDateTime.of(2026, 9, 25, 9, 0)),
            tx("6", "voice_package", 20.0, LocalDateTime.of(2026, 9, 26, 9, 0)),
            tx("7", "data_package", 999.0, LocalDateTime.of(2026, 9, 26, 9, 0), status = TxStatus.DELETED),
            tx("8", "data_package", 50.0, LocalDateTime.of(2026, 8, 1, 9, 0)),
        )
        val r = Periods.range(Period.MONTH, 0, today)
        val inMonth = Analytics.inRange(list, r)
        assertEquals(6, inMonth.size)
        val s = Analytics.summary(inMonth, r, today)
        assertEquals(205.0, s.total); assertEquals(6, s.count)
        assertEquals(205.0 / 23, s.avgPerDay, 1e-9); assertEquals(205.0 / 6, s.avgPerTx!!, 1e-9)
        val b = Spending.breakdown(inMonth, emptySet())
        assertEquals(70.0, b.byGroup["Data"]); assertEquals(20.0, b.byGroup["Voice"]); assertEquals(10.0, b.byGroup["SMS"])
        assertEquals(0.0, b.byGroup["Combo"]); assertEquals(105.0, b.unassigned); assertEquals(205.0, b.total)
        assertEquals(30, Analytics.daily(inMonth, r).size)
        assertEquals(35.0, Analytics.daily(inMonth, r)[0].second)           // Meskerem 1 = Sep 11
        assertEquals(205.0, Analytics.weekly(inMonth, r).sumOf { it.second })
        assertEquals(205.0, Analytics.monthly(list, 2019)[0].second)
        assertEquals(50.0, Analytics.monthly(list, 2018).sumOf { it.second })
        assertEquals("Student pack", Analytics.topPurchases(inMonth).first { it.second == 2 }.first)
    }
    @Test fun emptySummaryHasNoAverageClaim() {
        val r = Periods.range(Period.MONTH, 0, today)
        val s = Analytics.summary(emptyList(), r, today)
        assertEquals(0.0, s.total); assertNull(s.avgPerTx)
    }
}

class SampleDataTest {
    @Test fun sampleMessagesProduceExactlySixExpenses() {
        val now = java.time.LocalDateTime.of(2026, 10, 4, 15, 0, 0)
        val cands = SampleData.build(now).map { SmsParser.parse(it.sender, it.body, it.received) }.filter { it.isRelevant }
        val tx = TransactionGrouper().group(cands)
        assertEquals(6, tx.size)
        assertEquals(5.0 + 35 + 35 + 100 + 5 + 10, tx.sumOf { it.amount })
        assertEquals(setOf(5.0, 35.0, 35.0, 100.0, 5.0, 10.0), tx.map { it.amount }.toSet())
        assertEquals(3, tx.first { it.primary.transferId == "802500000001" }.smsCount)
        assertEquals(2, tx.first { it.primary.transactionId == "TXN0BBB222" }.smsCount)
        assertEquals(2, tx.first { it.primary.transactionId == "TXN0CCC333" }.smsCount)
    }
}
