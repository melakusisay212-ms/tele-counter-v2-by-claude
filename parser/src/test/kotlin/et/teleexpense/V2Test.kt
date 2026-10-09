package et.teleexpense

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Real messages sent by users (names and numbers as received). */
class V2Test {
    private val now = LocalDateTime.of(2026, 10, 6, 15, 0)
    private fun p(sender: String, body: String, at: LocalDateTime = now) = SmsParser.parse(sender, body, at)
    private fun group(vararg c: Candidate) = TransactionGrouper().group(c.toList())

    private val recharge5 = "Dear MELAKU You have recharged ETB 5.00 airtime for 981801919 on 06/10/2026 14:54:39. Your transaction number is DJ68HH1X0U. Your current  balance is  ETB 131.00. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DJ68HH1X0U\nThank you for using telebirr\nEthio telecom"
    private val received5 = "Dear Customer\nYou have received ETB 5.00 airtime from 251981801919 on 06/10/2026 14:54:39. Your transaction number is DJ68HH1X0U.\nThank you for using telebirr\nethio telecom"
    private val prepaid100 = "Dear Customer, your prepaid account has been recharged successfully.your recharged balance is 100.00 Birr.your balance is 163.33 Birr.With this balance your account will expire on 15/09/2027.ethio telecom"
    private val transfer = "Dear customer, you transfer 2.00 Birr to the number 972285747 successfully. The transfer fee is 0.12 Birr. Your balance is 1.88 Birr now.Download telebirr SuperApp http://onelink.to/fpgu4m and transfer airtime with no transaction fee and 10% additional rewards. ethio telecom"
    private val offer = "Dear Customer As per your request, the new service offer One Birr 4 Min Voice Package to be expired after 1 hour is added to your service number 0981801919. The offer is effective as of Oct 6, 2026 2:58:40 PM and will be expired on Oct 6, 2026 3:58:40 PM. Happy New Year! Ethio telecom"
    private val student = "Dear Abirham\nYou have paid ETB 64.00 for package Monthly Student Pack with 438 minutes, 1.2GB of data, 180 SMS, and a bonus of 438 minutes at night purchase made for 920002688 on 29/09/2026 12:21:03. Your transaction number is  DIT68TLQSO. Your current balance is ETB 5.50.To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DIT68TLQSO\nThank you for using telebirr\nEthio telecom"
    private val bankIn = "Dear Abirham,\nYou have received  ETB 65.00 by transaction number DIT68SLW3Y on 2026-09-29 11:54:41 from Commercial Bank of Ethiopia to your telebirr Account 251948251234 - Abirham Geremew Gessess. Your current balance is ETB 65.15.\nThank you for using telebirr\nEthio telecom"
    private val selfIn = "Dear Abirham \nYou have received ETB 64.00 from Abirham Geremew(2519****1234)  on 29/09/2026 11:57:24. Your transaction number is DIT58SPAKJ. Your current E-Money Account balance is ETB 69.50.\nThank you for using telebirr\nEthio telecom"
    private val daily95 = "Dear Solomon\nYou have paid ETB 95.00 for package Daily unlimited Internet purchase made for 961157870 on 30/09/2026 20:47:20. Your transaction number is  DIU4AISHGA. Your current balance is ETB 98.68.To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DIU4AISHGA\nThank you for using telebirr\nEthio telecom"

    @Test fun telebirrAirtimePurchaseIsTheExpenseAndDeliveryNoticeMergesIntoIt() {
        val a = p("telebirr", recharge5, LocalDateTime.of(2026, 10, 6, 14, 54, 41))
        assertEquals(Classification.COUNT, a.classification)
        assertEquals(5.0, a.amount); assertEquals("airtime_recharge", a.category)
        assertEquals("DJ68HH1X0U", a.transactionId); assertEquals("981801919", a.recipient)
        val b = p("994", received5, LocalDateTime.of(2026, 10, 6, 14, 54, 45))
        assertEquals(Classification.IGNORE, b.classification); assertEquals("DJ68HH1X0U", b.transactionId)
        val tx = group(a, b)
        assertEquals(1, tx.size); assertEquals(5.0, tx[0].amount); assertEquals(2, tx[0].smsCount)
    }

    @Test fun giftedAirtimeWithNoPaymentCreatesNoExpense() {
        assertEquals(0, group(p("994", received5)).size)
    }

    @Test fun telebirrPurchaseAndPrepaidRechargeAreOneExpense() {
        val t = p("telebirr", recharge5.replace("5.00", "100.00").replace("DJ68HH1X0U", "DJ1100AAAA"), LocalDateTime.of(2026, 10, 7, 13, 44, 0))
        val r = p("994", prepaid100, LocalDateTime.of(2026, 10, 7, 13, 44, 20))
        assertEquals(100.0, r.amount); assertEquals(Classification.COUNT, r.classification)
        val tx = group(t, r)
        assertEquals(1, tx.size); assertEquals(100.0, tx[0].amount)
    }

    @Test fun airtimeSentIsFundedByAirtimeAndIncludesTheFee() {
        val c = p("994", transfer)
        assertEquals(Classification.COUNT, c.classification); assertEquals("airtime_sent", c.category)
        assertEquals("airtime", c.source); assertEquals(2.12, c.amount!!, 1e-9); assertEquals("972285747", c.recipient)
    }

    @Test fun bundleBoughtFromAirtimeIsALabelWithPriceFromTheName() {
        val c = p("994", offer)
        assertEquals("voice_package", c.category); assertEquals("airtime", c.source)
        assertEquals(1.0, c.amount); assertEquals("0981801919", c.recipient)
        assertEquals(1.0, SmsParser.priceFromName("One Birr 4 Min Voice Package"))
        assertEquals(5.0, SmsParser.priceFromName("5 Birr Daily Internet"))
        assertNull(SmsParser.priceFromName("Daily Internet Package 120 MB"))
    }

    @Test fun bundleWithoutPriceInNameIsNotCounted() {
        val c = p("994", offer.replace("One Birr 4 Min Voice Package", "Weekly Voice 50 Min Package"))
        assertEquals(Classification.REFERENCE, c.classification)
        assertEquals(0, group(c).size)
    }

    @Test fun airtimeFundedItemNeverMergesWithACashPurchase() {
        val cash = p("telebirr", recharge5.replace("5.00", "1.00"), LocalDateTime.of(2026, 10, 6, 14, 58, 0))
        val alloc = p("994", offer, LocalDateTime.of(2026, 10, 6, 14, 58, 41))
        assertEquals(2, group(cash, alloc).size)
    }

    @Test fun bankAndSelfTransfersAreNotExpenses() {
        assertEquals(0, group(p("telebirr", bankIn), p("telebirr", selfIn)).size)
    }

    @Test fun comboPackIsCombo() {
        val c = p("telebirr", student)
        assertEquals("mixed_package", c.category); assertEquals("Combo", CategoryClassifier.groupOf(c.category))
    }

    @Test fun duplicateDeliveriesOfSameTransactionNumberAreOne() {
        assertEquals(1, group(p("telebirr", daily95), p("telebirr", daily95, now.plusMinutes(1))).size)
    }

    @Test fun ownNumbersAreLearnedFromMessages() {
        assertEquals(setOf("981801919"), OwnNumbers.extract(offer))
        assertEquals(setOf("948251234"), OwnNumbers.extract(bankIn))
        assertTrue(OwnNumbers.extract(student).isEmpty())
    }

    private fun tx(cat: String, amt: Double, rcpt: String?, src: String = "paid", key: String = "$cat$amt$rcpt") =
        ExpenseTx(key, "x", cat, amt, null, rcpt, null, now, 1, 0.9, source = src)

    @Test fun homeBreakdownSplitsServicesOthersAndUnassignedAirtime() {
        val mine = setOf("981801919", "948251234")
        val list = listOf(
            tx("airtime_recharge", 100.0, "981801919"),                    // my recharge: pool 100
            tx("voice_package", 35.0, "981801919", src = "airtime"),       // bundle from airtime
            tx("data_package", 95.0, "981801919"),                          // direct cash
            tx("mixed_package", 64.0, "981801919"),                         // combo cash
            tx("data_package", 64.0, "920002688"),                          // bought for someone else
            tx("airtime_sent", 2.12, "972285747", src = "airtime"),        // airtime sent to someone
            tx("airtime_sent", 3.0, "948251234", src = "airtime"),         // between my own numbers
            tx("airtime_recharge", 10.0, "972285747"),                      // recharged for someone else
        )
        val b = Spending.breakdown(list, mine)
        assertEquals(100.0 + 95 + 64 + 64 + 10, b.total, 1e-9)                // cash only
        assertEquals(5, b.count)
        assertEquals(35.0, b.byGroup["Voice"]); assertEquals(95.0, b.byGroup["Data"]); assertEquals(64.0, b.byGroup["Combo"])
        assertEquals(64.0 + 2.12 + 10.0, b.byGroup["Others"]!!, 1e-9)
        assertEquals(100.0 - 35.0 - 2.12, b.unassigned, 1e-9)
        assertEquals(0.0, b.overflow)
        val sum = b.byGroup.values.sum() + b.unassigned
        assertEquals(b.total, sum, 1e-9)                                       // tiles always add up to the total
    }

    @Test fun withoutKnownNumbersNothingIsCalledOthers() {
        val b = Spending.breakdown(listOf(tx("data_package", 64.0, "920002688")), emptySet())
        assertEquals(0.0, b.byGroup["Others"]); assertEquals(64.0, b.byGroup["Data"])
        assertFalse(Spending.isMine("920002688", emptySet()))
    }
}
