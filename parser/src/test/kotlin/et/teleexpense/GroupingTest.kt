package et.teleexpense

import et.teleexpense.Fx.at
import et.teleexpense.Fx.cands
import java.time.LocalDateTime
import kotlin.test.*

class GroupingTest {
    private val t0 = at(9, 17, 8, 38, 0)
    private fun group(vararg s: Pair<String, LocalDateTime>) = TransactionGrouper().group(cands(*s))

    @Test fun threeEbirrSmsAreOneFiveBirrExpense() {
        val tx = group(Fx.A to t0, Fx.B to t0.plusSeconds(2), Fx.C to t0.plusSeconds(3))
        assertEquals(1, tx.size); assertEquals(5.0, tx[0].amount); assertEquals(3, tx[0].smsCount)
    }
    @Test fun threeEbirrSmsInAnyOrder() {
        val tx = group(Fx.C to t0.plusSeconds(3), Fx.B to t0.plusSeconds(2), Fx.A to t0)
        assertEquals(1, tx.size); assertEquals(3, tx[0].smsCount)
    }
    @Test fun twoPackagePurchasesAreTwoExpenses() {
        val tx = group(Fx.PKG1 to at(9, 11, 15, 53), Fx.PKG2 to at(9, 17, 8, 39))
        assertEquals(2, tx.size); assertTrue(tx.all { it.amount == 35.0 }); assertEquals(70.0, tx.sumOf { it.amount })
    }
    @Test fun duplicateSmsWithSameIdCountedOnce() =
        assertEquals(1, group(Fx.PKG2 to t0, Fx.PKG2 to t0.plusSeconds(30)).size)
    @Test fun p2pAndFreeGiveZeroExpenses() = assertEquals(0, group(Fx.P2P to t0, Fx.BDAY to t0).size)
    @Test fun unpricedPackageIsZeroUntilPaid() {
        assertEquals(0, group(Fx.SENT300 to t0).size)
        val tx = group(Fx.SENT300 to t0, Fx.PAID300 to t0.plusMinutes(1))
        assertEquals(1, tx.size); assertEquals(10.0, tx[0].amount); assertEquals(2, tx[0].smsCount)
    }
    @Test fun activationLinksToPaymentNotNewExpense() {
        val tx = group(Fx.PKG2 to at(9, 17, 8, 39), Fx.ACT to at(9, 17, 8, 38, 10))
        assertEquals(1, tx.size); assertEquals(2, tx[0].smsCount)
    }
    @Test fun activationPicksClosestPurchase() {
        val tx = group(Fx.PKG1 to at(9, 11, 15, 53), Fx.PKG2 to at(9, 17, 8, 39), Fx.ACT to at(9, 17, 8, 38, 10))
        assertEquals(2, tx.size)
        assertEquals(1, tx.first { it.primary.transactionId == "DIB1N45Z9Z" }.smsCount)
        assertEquals(2, tx.first { it.primary.transactionId == "DIH5SLOKQ1" }.smsCount)
    }
    @Test fun prepaidRechargesAreSeparateExpenses() {
        val tx = group(Fx.PRE100 to at(9, 20, 10, 0), Fx.PRE5 to at(9, 20, 11, 0))
        assertEquals(2, tx.size); assertEquals(105.0, tx.sumOf { it.amount })
    }
    @Test fun sameTopUpReportedByEbirrAndEthioTelecomIsOneExpense() {
        val tx = group(Fx.A to t0, Fx.B to t0.plusSeconds(2), Fx.C to t0.plusSeconds(3), Fx.PRE5 to t0.plusSeconds(8))
        assertEquals(1, tx.size); assertEquals(5.0, tx[0].amount)
    }
    @Test fun sameAmountFarApartAreSeparate() {
        val tx = group(Fx.PRE5 to t0, Fx.PRE5 to t0.plusHours(5))
        assertEquals(2, tx.size)
    }
    @Test fun regroupingIsStable() {
        val c = cands(Fx.A to t0, Fx.B to t0.plusSeconds(2), Fx.C to t0.plusSeconds(3), Fx.PKG2 to t0.plusMinutes(1))
        val first = TransactionGrouper().group(c).map { it.primary.transferId ?: it.primary.transactionId }
        val second = TransactionGrouper().group(c.reversed()).map { it.primary.transferId ?: it.primary.transactionId }
        assertEquals(first.toSet(), second.toSet())
    }
    @Test fun fullMonthScenarioTotals() {
        val tx = group(
            Fx.A to t0, Fx.B to t0.plusSeconds(2), Fx.C to t0.plusSeconds(3),
            Fx.PKG1 to at(9, 11, 15, 53), Fx.P2P to at(9, 11, 21, 22), Fx.PKG2 to at(9, 17, 8, 39),
            Fx.ACT to at(9, 17, 8, 38, 10), Fx.PRE100 to at(9, 20, 10, 0), Fx.PRE5 to at(9, 20, 11, 0),
            Fx.BDAY to at(9, 22, 7, 0), Fx.SENT300 to at(9, 23, 12, 0),
        )
        assertEquals(5, tx.size)
        assertEquals(5.0 + 35 + 35 + 100 + 5, tx.sumOf { it.amount })
    }
}
