package et.teleexpense.app

import et.teleexpense.CategoryClassifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val clock24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val clock12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    private val longDate = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)

    /** Always two decimals, like the telebirr balance ("1,247.00"). */
    fun money(v: Double): String = String.format(Locale.US, "%,.2f", v)

    fun etb(v: Double): String {
        val rounded = Math.round(v)
        return if (Math.abs(v - rounded) < 0.005) String.format(Locale.US, "%,d", rounded)
        else String.format(Locale.US, "%,.2f", v)
    }

    fun time24(dt: LocalDateTime): String = dt.format(clock24)
    fun time12(dt: LocalDateTime): String = dt.format(clock12)
    fun gregorian(d: LocalDate): String = d.format(longDate)

    fun percent(part: Double, total: Double): String =
        if (total <= 0.0) "0%" else "${Math.round(part / total * 100)}%"

    fun plural(n: Int, one: String, many: String): String = if (n == 1) "$n $one" else "$n $many"

    fun categoryLabel(c: String): String = when (c) {
        "data_package" -> "Data package"
        "airtime" -> "Airtime top-up"
        "airtime_recharge" -> "Airtime recharge"
        "sms_package" -> "SMS package"
        "voice_package" -> "Voice package"
        "mixed_package" -> "Bundle (mixed)"
        else -> "Other"
    }

    fun groupHint(group: String): String = when (group) {
        "Voice" -> "calls"
        "Data" -> "internet"
        "SMS" -> "text messages"
        "Combo" -> "mixed bundles"
        "Others" -> "for other people"
        else -> "other"
    }

    /** Categories a user may pick when correcting a transaction. */
    val editableCategories: List<String> = listOf(
        "data_package", "airtime", "airtime_recharge", "sms_package", "voice_package", "mixed_package", "other",
    )

    fun groupOf(category: String): String = CategoryClassifier.groupOf(category)
}
