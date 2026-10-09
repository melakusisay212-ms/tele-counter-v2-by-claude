package et.teleexpense

import java.io.File
import java.time.LocalDateTime
import kotlin.test.*

/** Runs every labelled example in data/sms_training.json through the parser. Add real SMS there to grow coverage. */
class DatasetTest {
    private class Json(val s: String) {
        var i = 0
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun value(): Any? { ws(); return when (s[i]) {
            '{' -> obj(); '[' -> arr(); '"' -> str()
            else -> { val st = i; while (i < s.length && s[i] !in ",}] \n\r\t") i++
                val t = s.substring(st, i); when (t) { "null" -> null; "true" -> true; "false" -> false; else -> t.toDouble() } } } }
        fun obj(): Map<String, Any?> { val m = LinkedHashMap<String, Any?>(); i++; ws()
            if (s[i] == '}') { i++; return m }
            while (true) { ws(); val k = str(); ws(); i++; m[k] = value(); ws(); if (s[i] == ',') i++ else { i++; return m } } }
        fun arr(): List<Any?> { val l = ArrayList<Any?>(); i++; ws()
            if (s[i] == ']') { i++; return l }
            while (true) { l += value(); ws(); if (s[i] == ',') i++ else { i++; return l } } }
        fun str(): String { val sb = StringBuilder(); i++
            while (s[i] != '"') { if (s[i] == '\\') { i++; when (s[i]) { 'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'u' -> { sb.append(s.substring(i + 1, i + 5).toInt(16).toChar()); i += 4 } else -> sb.append(s[i]) } } else sb.append(s[i]); i++ }
            i++; return sb.toString() }
    }

    private fun file(): File {
        val candidates = listOfNotNull(System.getProperty("dataset"), "../data/sms_training.json", "data/sms_training.json")
        return candidates.map { File(it) }.firstOrNull { it.exists() } ?: fail("sms_training.json not found; tried $candidates")
    }

    @Test fun everyLabelledExampleParsesAsExpected() {
        @Suppress("UNCHECKED_CAST")
        val rows = Json(file().readText()).value() as List<Map<String, Any?>>
        assertTrue(rows.size >= 10, "dataset too small")
        for (row in rows) {
            val name = row["name"] as String
            @Suppress("UNCHECKED_CAST") val exp = row["expect"] as Map<String, Any?>
            val c = SmsParser.parse(row["sender"] as String?, row["body"] as String, LocalDateTime.parse(row["received"] as String))
            exp["relevant"]?.let { assertEquals(it, c.isRelevant, "$name: relevant") }
            exp["classification"]?.let { assertEquals(it, c.classification.name, "$name: classification") }
            exp["category"]?.let { assertEquals(it, c.category, "$name: category") }
            if (exp.containsKey("amount")) assertEquals((exp["amount"] as Double?), c.amount, "$name: amount")
            exp["direction"]?.let { assertEquals(it, c.direction, "$name: direction") }
            exp["transactionId"]?.let { assertEquals(it, c.transactionId, "$name: transactionId") }
            exp["transferId"]?.let { assertEquals(it, c.transferId, "$name: transferId") }
            exp["provider"]?.let { assertEquals(it, c.provider, "$name: provider") }
            exp["packageName"]?.let { assertEquals(it, c.packageName, "$name: packageName") }
            exp["recipient"]?.let { assertEquals(it, c.recipient, "$name: recipient") }
            exp["dateExplicit"]?.let { assertEquals(it, c.dateIsExplicit, "$name: dateExplicit") }
            exp["dateTime"]?.let { assertEquals(LocalDateTime.parse(it as String), c.dateTime, "$name: dateTime") }
            exp["eth"]?.let { val (y, m, d) = (it as String).split("-").map(String::toInt); assertEquals(EthDate(y, m, d), c.ethDate, "$name: ethDate") }
        }
    }
}
