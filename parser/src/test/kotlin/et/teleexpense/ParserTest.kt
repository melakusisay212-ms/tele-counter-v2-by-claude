package et.teleexpense

import et.teleexpense.Classification.*
import et.teleexpense.Fx.at
import java.time.LocalDateTime
import kotlin.test.*

class ParserTest {
    private val t0 = at(9, 17, 9, 0, 0)
    private fun p(s: String, t: LocalDateTime = t0, sender: String? = null) = SmsParser.parse(sender, s, t)

    @Test fun exampleA_airtimeSentIsCount() {
        val c = p(Fx.A)
        assertEquals(COUNT, c.classification); assertEquals("airtime", c.category); assertEquals(5.0, c.amount)
        assertEquals("outgoing", c.direction); assertEquals("successful", c.status)
        assertEquals("802520883630", c.transferId); assertEquals("251981801919", c.recipient)
        assertEquals("eBirr", c.provider)
    }
    @Test fun exampleB_airtimeReceivedIsIgnored() {
        val c = p(Fx.B)
        assertEquals(IGNORE, c.classification); assertEquals("airtime", c.category)
        assertEquals(5.0, c.amount); assertEquals("incoming", c.direction)
    }
    @Test fun exampleC_rechargeConfirmationIsReferenceAndBalanceNotRead() {
        val c = p(Fx.C)
        assertEquals(REFERENCE, c.classification); assertEquals("airtime_recharge", c.category)
        assertEquals(5.0, c.amount); assertEquals("outgoing", c.direction)
    }
    @Test fun telebirrPackage() {
        val c = p(Fx.PKG1)
        assertEquals(COUNT, c.classification); assertEquals("data_package", c.category)
        assertEquals(35.0, c.amount); assertEquals("DIB1N45Z9Z", c.transactionId)
        assertEquals("Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus", c.packageName)
        assertEquals(at(9, 11, 15, 52, 15), c.dateTime); assertTrue(c.dateIsExplicit)
        assertEquals(EthDate(2019, 1, 1), c.ethDate)      // Ethiopian New Year 2019
        assertEquals("981801919", c.recipient); assertEquals("telebirr", c.provider)
    }
    @Test fun secondPackage() {
        val c = p(Fx.PKG2)
        assertEquals(COUNT, c.classification); assertEquals(35.0, c.amount); assertEquals("DIH5SLOKQ1", c.transactionId)
        assertEquals(at(9, 17, 8, 38, 4), c.dateTime); assertEquals(EthDate(2019, 1, 7), c.ethDate)
    }
    @Test fun personToPersonIgnored_feeAndVatNotRead() {
        val c = p(Fx.P2P)
        assertEquals(IGNORE, c.classification); assertEquals("person_to_person_transfer", c.category)
        assertNull(c.amount); assertEquals(at(9, 11, 21, 21, 46), c.dateTime)
    }
    @Test fun activationIsReferenceWithTextualDate() {
        val c = p(Fx.ACT)
        assertEquals(REFERENCE, c.classification); assertEquals("package_activation", c.category)
        assertNull(c.amount); assertEquals(at(9, 17, 8, 38, 4), c.dateTime)
        assertEquals("0981801919", c.recipient)
    }
    @Test fun prepaidRecharge100_balanceNotCounted() {
        val c = p(Fx.PRE100)
        assertEquals(COUNT, c.classification); assertEquals("airtime_recharge", c.category); assertEquals(100.0, c.amount)
        assertFalse(c.dateIsExplicit); assertEquals(t0, c.dateTime)
    }
    @Test fun prepaidRecharge5() = p(Fx.PRE5).let { assertEquals(COUNT, it.classification); assertEquals(5.0, it.amount) }
    @Test fun freePackageIgnored() = p(Fx.BDAY).let { assertEquals(IGNORE, it.classification); assertEquals("free_package", it.category); assertEquals(0.0, it.amount) }
    @Test fun packageSentWithoutPriceIsReference() {
        val c = p(Fx.SENT300)
        assertEquals(REFERENCE, c.classification); assertEquals("data_package", c.category)
        assertNull(c.amount); assertEquals("0943178701", c.recipient); assertEquals("outgoing", c.direction)
    }
    @Test fun paidPackageToAnotherNumber() {
        val c = p(Fx.PAID300)
        assertEquals(COUNT, c.classification); assertEquals(10.0, c.amount)
        assertEquals("Daily internet Package 300 MB", c.packageName); assertEquals("0943178701", c.recipient)
    }
    @Test fun unrelatedSmsIsNotRelevant() {
        val c = p("Your verification code is 482913. It is free to use.", sender = "SomeApp")
        assertFalse(c.isRelevant)
    }
    @Test fun ordinaryBankSmsNotRelevant() {
        assertFalse(p("Your account has been debited with ETB 500.00 transfer to Abebe. Balance ETB 2000.00.", sender = "CBE").isRelevant)
    }
    @Test fun failedAndReversedNeverCount() {
        assertEquals(IGNORE, p("You have paid ETB 35.00 for package X 1GB purchase made for 981801919 on 11/09/2026 15:52:15. Transaction failed.").classification)
        assertEquals(IGNORE, p("Your payment of ETB 10 was reversed. telebirr").classification)
    }
    @Test fun unknownFormatIsConservative() {
        val c = p("Ethio telecom: dial *999# for offers")
        assertEquals(REFERENCE, c.classification); assertNull(c.amount)
    }
    @Test fun dateFormats() {
        assertEquals(at(9, 11, 15, 52, 15), DateExtractor.extract("on 11/09/2026 15:52:15."))
        assertEquals(at(9, 17, 8, 38, 4), DateExtractor.extract("Sep 17, 2026 8:38:04 AM"))
        assertEquals(at(9, 17, 20, 38, 4), DateExtractor.extract("Sep 17, 2026 8:38:04 PM"))
        assertEquals(LocalDateTime.of(2026, 9, 17, 0, 0), DateExtractor.extract("2026-09-17"))
        assertEquals(LocalDateTime.of(2026, 9, 30, 0, 0), DateExtractor.extract("30/09/26"))
        assertNull(DateExtractor.extract("no date here, ETB 5 to 251981801919"))
    }
    @Test fun phoneNumbersAndSizesAreNotAmounts() {
        val c = p("You have paid ETB 15.00 for package Weekly 300MB purchase made for 0981801919 on 01/10/2026 10:00:00. Your transaction number is ZZZ111AAA2.")
        assertEquals(15.0, c.amount)
    }
    @Test fun categories() {
        assertEquals("data_package", CategoryClassifier.ofPackage("Monthly student pack 1.2GB + 120SMS"))
        assertEquals("sms_package", CategoryClassifier.ofPackage("Daily 100 SMS"))
        assertEquals("voice_package", CategoryClassifier.ofPackage("Weekly 50 Min"))
        assertEquals("mixed_package", CategoryClassifier.ofPackage("1GB internet and 20 Min"))
        assertEquals("voice_package", CategoryClassifier.ofPackage("Weekly Voice 50 Min"))
        assertEquals("voice_package", CategoryClassifier.ofPackage("Daily call bundle"))
        assertEquals("data_package", CategoryClassifier.ofPackage("Weekly Internet 1 GB"))
    }
    @Test fun spendingGroupsAreVoiceDataSms() {
        assertEquals(listOf("Voice", "Data", "SMS", "Combo"), CategoryClassifier.groups)
        assertEquals("Voice", CategoryClassifier.groupOf("voice_package"))
        assertEquals("Airtime", CategoryClassifier.groupOf("airtime"))
        assertEquals("Airtime", CategoryClassifier.groupOf("airtime_recharge"))
        assertEquals("Data", CategoryClassifier.groupOf("data_package"))
        assertEquals("SMS", CategoryClassifier.groupOf("sms_package"))
        assertEquals("Combo", CategoryClassifier.groupOf("mixed_package"))
        assertEquals("Combo", CategoryClassifier.groupOf("other"))
    }
}
