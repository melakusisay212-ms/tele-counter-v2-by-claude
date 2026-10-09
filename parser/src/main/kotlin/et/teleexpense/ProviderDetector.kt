package et.teleexpense

/** Provider rules are data, not control flow: add a line to extend. */
object ProviderDetector {
    private class TextRule(val provider: String, val regex: Regex)

    private val textRules = listOf(
        TextRule("eBirr", Regex("""ebirr""", RegexOption.IGNORE_CASE)),
        TextRule("telebirr", Regex("""telebirr|you have paid etb|you have transferred etb|your transaction number is""", RegexOption.IGNORE_CASE)),
        TextRule("Ethio telecom", Regex("""ethio ?telecom|prepaid account|service offer|recharged successfully|internet package|successfully sent .*\d\s?(mb|gb)""", RegexOption.IGNORE_CASE)),
    )
    private val senderRules = listOf(
        TextRule("eBirr", Regex("""ebirr""", RegexOption.IGNORE_CASE)),
        TextRule("telebirr", Regex("""telebirr""", RegexOption.IGNORE_CASE)),
        TextRule("Ethio telecom", Regex("""ethio|^127$|^994$""", RegexOption.IGNORE_CASE)),
        TextRule("Bank", Regex("""cbe|commercial|dashen|awash|abyssinia|zemen|wegagen|nib|siinqee|bank""", RegexOption.IGNORE_CASE)),
    )

    fun detect(sender: String?, text: String): String {
        textRules.firstOrNull { it.regex.containsMatchIn(text) }?.let { return it.provider }
        if (!sender.isNullOrBlank()) senderRules.firstOrNull { it.regex.containsMatchIn(sender) }?.let { return it.provider }
        return "Unknown"
    }
}
