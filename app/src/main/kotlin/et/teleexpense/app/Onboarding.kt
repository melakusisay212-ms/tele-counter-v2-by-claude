package et.teleexpense.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import et.teleexpense.Analytics
import et.teleexpense.EthiopianCalendar
import et.teleexpense.ExpenseTx
import et.teleexpense.Period
import et.teleexpense.Periods

private val DarkBrush = Brush.verticalGradient(listOf(Brand.Green, Brand.Green))

@Composable
fun WelcomeScreen(
    permissionDenied: Boolean,
    onChoose: (thisMonth: Boolean) -> Unit,
    onSamples: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(DarkBrush).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        LogoMark(84.dp)
        Spacer(Modifier.height(20.dp))
        Text("Welcome to Tele Expense", color = Brand.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Your telecom spending,\nautomatically tracked.", color = Brand.Ink, fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Everything stays on your phone.", color = Brand.Ink, fontSize = 14.sp)
        Spacer(Modifier.height(26.dp))

        TeleCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Build your starting history?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = scheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    "We can scan telecom transaction messages from the current month to create your starting spending history.",
                    fontSize = 14.sp,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tele Expense needs SMS access to detect telecom purchases and recharges automatically. Your messages are processed locally on your phone.",
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onChoose(true) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand.Blue, contentColor = Color.White),
                ) { Text("Scan This Month", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onChoose(false) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Start From Today", color = Brand.Ink) }

                if (permissionDenied) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "SMS access was not granted. If Android blocked the request for an app installed outside the Play Store: open App info, tap the ⋮ menu (top right), choose \"Allow restricted settings\", then come back and try again.",
                        fontSize = 12.sp,
                        color = Brand.Danger,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Open app settings", color = Brand.Ink) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onSamples) { Text("Try it with sample data", color = Brand.Ink, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(16.dp))
        Text("Developed by Melaku Sisay", color = Brand.Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun SplashScreen() {
    Column(
        Modifier.fillMaxSize().background(Brand.Green).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1f))
        LogoMark(112.dp)
        Spacer(Modifier.height(22.dp))
        Text("Tele Expense", color = Brand.Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Know where your airtime goes", color = Brand.Ink, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text("Developed by", color = Brand.Ink.copy(alpha = 0.7f), fontSize = 12.sp)
        Text("Melaku Sisay", color = Brand.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ScanningScreen() {
    Column(
        Modifier.fillMaxSize().background(DarkBrush).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LogoMark(72.dp)
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = Brand.Ink)
        Spacer(Modifier.height(20.dp))
        Text("Analyzing telecom messages…", color = Brand.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("This stays on your phone.", color = Brand.Ink, fontSize = 13.sp)
    }
}

@Composable
fun ResultScreen(result: ScanResult?, txs: List<ExpenseTx>, thisMonth: Boolean, onContinue: () -> Unit) {
    val range = Periods.range(Period.MONTH, 0)
    val total = Analytics.inRange(txs, range).sumOf { it.amount }
    val expenses = Analytics.active(txs).size
    Column(
        Modifier.fillMaxSize().background(DarkBrush).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LogoMark(72.dp)
        Spacer(Modifier.height(20.dp))
        val scanError = result?.error
        if (scanError != null) {
            Text("Could not scan messages", color = Brand.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(scanError, color = Brand.Danger, fontSize = 14.sp, textAlign = TextAlign.Center)
        } else if (thisMonth && result != null) {
            Text("Found:", color = Brand.Ink, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            FoundLine("${result.relevant}", "relevant messages")
            FoundLine("${result.events}", "transaction events")
            FoundLine("$expenses", "telecom expenses")
        } else {
            Text("Tracking started", color = Brand.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "New telecom messages will be added automatically from now on.",
                color = Brand.Ink,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("Your telecom spending", color = Brand.Ink, fontSize = 15.sp)
        Text(range.title, color = Brand.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("ETB ${Fmt.etb(total)}", color = Brand.Ink, fontSize = 52.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand.Blue, contentColor = Color.White),
        ) { Text("Continue", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun FoundLine(number: String, label: String) {
    Text("$number $label", color = Brand.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 2.dp))
}
