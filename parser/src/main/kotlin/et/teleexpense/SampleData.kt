package et.teleexpense

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Realistic fictional messages (same wording as real Ethio telecom / telebirr / eBirr SMS) for trying the app without a SIM. */
object SampleData {
    data class Sms(val sender: String, val body: String, val received: LocalDateTime)

    private val numeric = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
    private val textual = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a", Locale.ENGLISH)

    fun build(now: LocalDateTime): List<Sms> {
        fun day(daysAgo: Long, h: Int, m: Int, s: Int = 0) = now.toLocalDate().minusDays(daysAgo).atTime(h, m, s)
        val pack = "Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus"
        val ebirr = now.minusMinutes(90)
        val p1 = day(6, 15, 52, 15)
        val p2 = day(2, 8, 38, 4)
        val pay300 = day(1, 12, 1, 0)
        return listOf(
            Sms("eBirr", "[-EBIRR-KAAFI-] Transfer ID: 802500000001, You have successfuly sent airtime top-up of ETB5 to 251911000111", ebirr),
            Sms("eBirr", "[-EBIRR-KAAFI-] Transfer ID: 802500000001, You have received airtime top-up of ETB5 from 251911000111", ebirr.plusSeconds(2)),
            Sms("eBirr", "[-EBIRR-KAAFI-] You have recharged ETB5 to 251911000111, your balance is ETB3.6", ebirr.plusSeconds(3)),
            Sms("telebirr", "Dear USER\n\nYou have paid ETB 35.00 for package $pack purchase made for 911000111 on ${p1.format(numeric)}.\n\nYour transaction number is TXN0AAA111.", p1.plusSeconds(45)),
            Sms("telebirr", "You have transferred ETB 10.00 to ABEBE KEBEDE (2519****0000) on ${day(6, 21, 21, 46).format(numeric)}.\n\nThe service fee is ETB 0.87 and 15% VAT on the service fee is ETB 0.13.", day(6, 21, 22)),
            Sms("telebirr", "You have paid ETB 35.00 for package $pack purchase made for 911000111 on ${p2.format(numeric)}.\n\nYour transaction number is TXN0BBB222.", p2.plusSeconds(30)),
            Sms("127", "As per your request the new service offer $pack from telebirr to expire after 30 days is added to your service number 0911000111.\n\nThe service offer is effective as of ${p2.format(textual)}.", p2.plusSeconds(6)),
            Sms("127", "Your prepaid account has been recharged successfully.\n\nYour Recharged balance is 100.00 Birr.\n\nYour balance is 100.00 Birr.", day(4, 10, 0)),
            Sms("127", "Your prepaid account has been recharged successfully.\n\nYour Recharged balance is 5.00 Birr.\n\nYour balance is 5.00 Birr.", day(3, 11, 0)),
            Sms("127", "Ethio telecom wishes you a Happy birthday!\n\nPlease enjoy 1GB internet, 20 Min and 20 SMS package Free gift, valid for 24-hours.", day(1, 7, 0)),
            Sms("127", "You have successfully sent Daily internet Package 300 MB to 0911000222.", day(1, 12, 0)),
            Sms("telebirr", "You have paid ETB 10.00 for Daily internet Package 300 MB to 0911000222 on ${pay300.format(numeric)}. Your transaction number is TXN0CCC333.", pay300.plusSeconds(20)),
        )
    }
}
