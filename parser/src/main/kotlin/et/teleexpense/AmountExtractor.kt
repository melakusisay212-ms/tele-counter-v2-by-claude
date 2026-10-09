package et.teleexpense

/** Amounts are only taken from semantically anchored patterns, never "the first number". */
object AmountExtractor {
    const val NUM = """(\d+(?:,\d{3})*(?:\.\d+)?)"""
    const val ETB = """ETB\s?$NUM"""
    const val BIRR_SUFFIX = """$NUM\s?Birr"""

    fun parse(s: String): Double = s.replace(",", "").toDouble()
}
