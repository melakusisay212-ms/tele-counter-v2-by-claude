package et.teleexpense

import java.time.LocalDateTime

enum class Classification { COUNT, IGNORE, REFERENCE }

/** One analysed SMS. The raw SMS body is never kept. */
data class Candidate(
    val classification: Classification,
    val provider: String,
    val category: String,
    val amount: Double? = null,
    val currency: String = "ETB",
    val direction: String = "outgoing",
    val status: String = "successful",
    val packageName: String? = null,
    val recipient: String? = null,
    val transactionId: String? = null,
    val transferId: String? = null,
    /** Explicit transaction date if the SMS has one, otherwise the received time. */
    val dateTime: LocalDateTime,
    val receivedAt: LocalDateTime,
    val dateIsExplicit: Boolean,
    val reason: String,
    val confidence: Double = 0.9,
    /** "paid" = money left the bank/telebirr. "airtime" = paid from airtime already bought (never new cash). */
    val source: String = "paid",
) {
    val ethDate: EthDate get() = EthiopianCalendar.fromGregorian(dateTime.toLocalDate())

    /** False for SMS that have nothing to do with telecom spending (OTPs, ads, ordinary bank activity). */
    val isRelevant: Boolean get() = provider != "Unknown" && category != "bank_other"
}

/** A final expense: one COUNT message plus every supporting SMS grouped with it. */
data class TelecomTransaction(
    val primary: Candidate,
    val related: MutableList<Candidate> = mutableListOf(),
) {
    val amount: Double get() = primary.amount ?: 0.0
    val smsCount: Int get() = 1 + related.size
}

enum class TxStatus { ACTIVE, DELETED, NOT_EXPENSE }

/** What the app stores and shows. */
data class ExpenseTx(
    val key: String,
    val provider: String,
    val category: String,
    val amount: Double,
    val packageName: String?,
    val recipient: String?,
    val transactionId: String?,
    val dateTime: LocalDateTime,
    val smsCount: Int,
    val confidence: Double,
    val userEdited: Boolean = false,
    val status: TxStatus = TxStatus.ACTIVE,
    val source: String = "paid",
) {
    val isCash: Boolean get() = source == "paid"

    val ethDate: EthDate get() = EthiopianCalendar.fromGregorian(dateTime.toLocalDate())

    val title: String
        get() = packageName ?: when (category) {
            "airtime_sent" -> "Airtime sent"
            "airtime" -> "Airtime top-up"
            "airtime_recharge" -> "Airtime recharge"
            "data_package" -> "Data package"
            "sms_package" -> "SMS package"
            "voice_package" -> "Voice package"
            "mixed_package" -> "Mixed package"
            else -> "Telecom payment"
        }
}
