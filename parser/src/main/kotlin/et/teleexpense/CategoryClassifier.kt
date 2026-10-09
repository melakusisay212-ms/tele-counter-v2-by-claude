package et.teleexpense

object CategoryClassifier {
    private val dataRe = Regex("""\d\s?(gb|mb|tb)\b|internet|\bdata\b""", RegexOption.IGNORE_CASE)
    private val voiceRe = Regex("""\bmin(ute)?s?\b|\bvoice\b|\bcalls?\b""", RegexOption.IGNORE_CASE)
    private val smsRe = Regex("""\bsms\b|\d\s?sms""", RegexOption.IGNORE_CASE)
    private val packageish = Regex("""package|pack\b|bundle|internet|airtime|\d\s?(gb|mb)|sms|\bmin\b""", RegexOption.IGNORE_CASE)

    fun ofPackage(name: String): String {
        val d = dataRe.containsMatchIn(name)
        val v = voiceRe.containsMatchIn(name)
        val s = smsRe.containsMatchIn(name)
        return when {
            d && v -> "mixed_package"
            d -> "data_package"      // "1.2GB + 120SMS" is still a data package
            v -> "voice_package"
            s -> "sms_package"
            else -> "other"
        }
    }

    fun looksLikePackage(name: String) = packageish.containsMatchIn(name)

    /**
     * Spending group shown to the user: Voice / Data / SMS (+ Other for bundles that mix services).
     * Airtime top-ups and recharges are call credit in Ethiopia, so they count as Voice.
     * To show airtime on its own instead, map "airtime" and "airtime_recharge" to a new "Airtime" group here.
     */
    fun groupOf(category: String): String = when (category) {
        "voice_package" -> "Voice"
        "data_package" -> "Data"
        "sms_package" -> "SMS"
        "airtime", "airtime_recharge" -> "Airtime"   // call credit not yet spent on a bundle
        else -> "Combo"                               // mixed bundles and anything unclassified
    }

    val groups = listOf("Voice", "Data", "SMS", "Combo")
}
