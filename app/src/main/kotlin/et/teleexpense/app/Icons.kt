package et.teleexpense.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Rounded Material icons used across the app (no emoji). */
object Ic {
    val Home: ImageVector get() = Icons.Rounded.Home
    val Transactions: ImageVector get() = Icons.Rounded.ReceiptLong
    val Calendar: ImageVector get() = Icons.Rounded.CalendarMonth
    val Analytics: ImageVector get() = Icons.Rounded.BarChart
    val Settings: ImageVector get() = Icons.Rounded.Settings
    val Voice: ImageVector get() = Icons.Rounded.Call
    val Data: ImageVector get() = Icons.Rounded.Public
    val Sms: ImageVector get() = Icons.Rounded.Sms
    val Others: ImageVector get() = Icons.Rounded.Group
    val Combo: ImageVector get() = Icons.Rounded.CardGiftcard
    val Wallet: ImageVector get() = Icons.Rounded.AccountBalanceWallet
    val Scan: ImageVector get() = Icons.Rounded.Refresh
    val Inbox: ImageVector get() = Icons.Rounded.Inbox
    val Leaf: ImageVector get() = Icons.Rounded.Eco

    fun forGroup(group: String): ImageVector = when (group) {
        "Voice" -> Voice
        "Data" -> Data
        "SMS" -> Sms
        "Others" -> Others
        "Combo" -> Combo
        else -> Wallet
    }

    fun forCategory(category: String): ImageVector = when (category) {
        "data_package" -> Data
        "airtime", "airtime_recharge", "voice_package" -> Voice
        "sms_package" -> Sms
        "mixed_package" -> Combo
        "airtime_sent" -> Others
        else -> Wallet
    }
}

/** Soft translucent "glass" tile behind an icon: tinted at low alpha with the icon in the full tint. */
@Composable
fun GlassIcon(icon: ImageVector, tint: Color, iconTint: Color = tint, size: Dp = 44.dp, corner: Dp = 14.dp, glass: Float = 0.18f) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(corner)).background(tint.copy(alpha = glass)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.54f))
    }
}
