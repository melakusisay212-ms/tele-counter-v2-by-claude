package et.teleexpense

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Finds an explicit transaction date inside an SMS. Never invents one. */
object DateExtractor {
    private val textual = Regex("""\b([A-Z][a-z]{2}) (\d{1,2}), (\d{4}) (\d{1,2}):(\d{2}):(\d{2}) ([AaPp][Mm])""")
    private val numeric = Regex("""\b(\d{1,2})/(\d{1,2})/(\d{4}|\d{2})(?:[ T,]+(\d{1,2}):(\d{2})(?::(\d{2}))?)?""")
    private val iso = Regex("""\b(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?""")
    private val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a", Locale.ENGLISH)

    fun extract(text: String): LocalDateTime? {
        textual.find(text)?.let { m ->
            val s = "${m.groupValues[1]} ${m.groupValues[2]}, ${m.groupValues[3]} " +
                "${m.groupValues[4]}:${m.groupValues[5]}:${m.groupValues[6]} ${m.groupValues[7].uppercase()}"
            runCatching { LocalDateTime.parse(s, fmt) }.getOrNull()?.let { return it }
        }
        numeric.find(text)?.let { m ->
            val g = m.groupValues
            val year = if (g[3].length == 2) 2000 + g[3].toInt() else g[3].toInt()
            // Ethiopian SMS use day/month/year.
            runCatching {
                LocalDateTime.of(year, g[2].toInt(), g[1].toInt(),
                    g[4].toIntOrNull() ?: 0, g[5].toIntOrNull() ?: 0, g[6].toIntOrNull() ?: 0)
            }.getOrNull()?.let { return it }
        }
        iso.find(text)?.let { m ->
            val g = m.groupValues
            runCatching {
                LocalDateTime.of(g[1].toInt(), g[2].toInt(), g[3].toInt(),
                    g[4].toIntOrNull() ?: 0, g[5].toIntOrNull() ?: 0, g[6].toIntOrNull() ?: 0)
            }.getOrNull()?.let { return it }
        }
        return null
    }
}
