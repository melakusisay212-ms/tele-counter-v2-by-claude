package et.teleexpense.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** telebirr-inspired palette: white pages, black text, green accents, one blue call-to-action. */
object Brand {
    val Green = Color(0xFF8DC63F)
    val GreenDeep = Color(0xFF5C8A1E)
    val GreenPale = Color(0xFFEAF4DC)
    val Ink = Color(0xFF111111)
    val Muted = Color(0xFF5B6560)
    val Line = Color(0xFFE3E8DF)
    val Field = Color(0xFFF3F5F1)
    val Blue = Color(0xFF0088D1)
    val Yellow = Color(0xFFF5B800)
    val Grey = Color(0xFF9AA59F)
    val Danger = Color(0xFFD32F2F)

    /** Darker shade of each service colour, for icons and text on its translucent tile. */
    fun groupDeep(name: String): Color = when (name) {
        "Voice" -> Color(0xFF0072B8)
        "Data" -> Color(0xFF4C7F14)
        "SMS" -> Color(0xFFB07F00)
        "Combo" -> Color(0xFFB8590A)
        "Others" -> Color(0xFF5E35B1)
        else -> Color(0xFF5B6560)
    }

    /** Voice = blue, Data = green, SMS = yellow, Other = grey. */
    fun group(name: String): Color = when (name) {
        "Voice" -> Blue
        "Data" -> Green
        "SMS" -> Yellow
        "Combo" -> Color(0xFFF28C28)
        "Others" -> Color(0xFF7E57C2)
        else -> Grey
    }
}

private val TeleColors = lightColorScheme(
    primary = Brand.GreenDeep,
    onPrimary = Color.White,
    primaryContainer = Brand.GreenPale,
    onPrimaryContainer = Brand.Ink,
    secondary = Brand.GreenDeep,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Brand.Ink,
    surface = Color.White,
    onSurface = Brand.Ink,
    surfaceVariant = Brand.Field,
    onSurfaceVariant = Brand.Muted,
    outline = Brand.Line,
    error = Brand.Danger,
)

@Composable
fun TeleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TeleColors, content = content)
}
