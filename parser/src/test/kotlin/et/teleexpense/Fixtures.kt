package et.teleexpense

import java.time.LocalDateTime

object Fx {
    const val A = "[-EBIRR-KAAFI-] Transfer ID: 802520883630, You have successfuly sent airtime top-up of ETB5 to 251981801919"
    const val B = "[-EBIRR-KAAFI-] Transfer ID: 802520883630, You have received airtime top-up of ETB5 from 251981801919"
    const val C = "[-EBIRR-KAAFI-] You have recharged ETB5 to 251981801919, your balance is ETB3.6"
    const val PKG1 = "Dear MELAKU\n\nYou have paid ETB 35.00 for package Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus purchase made for 981801919 on 11/09/2026 15:52:15.\n\nYour transaction number is DIB1N45Z9Z."
    const val PKG2 = "You have paid ETB 35.00 for package Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus purchase made for 981801919 on 17/09/2026 08:38:04.\n\nYour transaction number is DIH5SLOKQ1."
    const val P2P = "You have transferred ETB 10.00 to RUTA TAKELE (2519****3230) on 11/09/2026 21:21:46.\n\nThe service fee is ETB 0.87 and 15% VAT on the service fee is ETB 0.13."
    const val ACT = "As per your request the new service offer Monthly student pack 1.2GB + 120SMS plus 1.2GB night bonus from telebirr to expire after 30 days is added to your service number 0981801919.\n\nThe service offer is effective as of Sep 17, 2026 8:38:04 AM."
    const val PRE100 = "Your prepaid account has been recharged successfully.\n\nYour Recharged balance is 100.00 Birr.\n\nYour balance is 100.00 Birr."
    const val PRE5 = "Your prepaid account has been recharged successfully.\n\nYour Recharged balance is 5.00 Birr.\n\nYour balance is 5.00 Birr."
    const val BDAY = "Ethio telecom wishes you a Happy birthday!\n\nPlease enjoy 1GB internet, 20 Min and 20 SMS package Free gift, valid for 24-hours."
    const val SENT300 = "You have successfully sent Daily internet Package 300 MB to 0943178701."
    const val PAID300 = "You have paid ETB 10.00 for Daily internet Package 300 MB to 0943178701 on 17/09/2026 08:37:00. Your transaction number is ABC123XYZ9."

    fun at(m: Int, d: Int, h: Int, mi: Int, s: Int = 0): LocalDateTime = LocalDateTime.of(2026, m, d, h, mi, s)
    fun cands(vararg s: Pair<String, LocalDateTime>) = s.map { SmsParser.parse(null, it.first, it.second) }
}
