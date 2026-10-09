package et.teleexpense.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import et.teleexpense.Analytics
import et.teleexpense.CategoryClassifier
import et.teleexpense.ExpenseTx
import et.teleexpense.Spending

@Composable
fun HomeScreen(
    txs: List<ExpenseTx>,
    mine: Set<String>,
    period: PeriodState,
    scanning: Boolean,
    scanMsg: String?,
    onScan: () -> Unit,
    onNav: (Int) -> Unit,
    onOpen: (String) -> Unit,
) {
    val range = period.range
    val list = Analytics.inRange(txs, range)
    val summary = Analytics.summary(list, range)
    val b = Spending.breakdown(list, mine)
    val groups = b.byGroup
    val anyData = Analytics.active(txs).isNotEmpty()
    // Voice, Data and SMS always show; Combo and Others only when something landed there.
    val tiles = Spending.groups.filter { it in listOf("Voice", "Data", "SMS") || (groups[it] ?: 0.0) > 0.0 }
    val topGroup = tiles.maxByOrNull { groups[it] ?: 0.0 }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Green hero with the white sheet rising out of it, like the telebirr home screen.
            Column(Modifier.fillMaxWidth().background(Brand.Green)) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LogoMark(40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Selam", color = Brand.Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Where is your money going?", color = Brand.Ink, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    PeriodSelector(period, onGreen = true)
                    Spacer(Modifier.height(6.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Telecom spending (ETB)", color = Brand.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(Fmt.money(summary.total), color = Brand.Ink, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Transactions", color = Brand.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("${summary.count}", color = Brand.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Average / day (ETB)", color = Brand.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(Fmt.money(summary.avgPerDay), color = Brand.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .padding(16.dp),
                ) {
                    Text("Spending by service", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brand.Ink)
                    Text("Tap a service to see its transactions", fontSize = 12.sp, color = Brand.Muted)
                    if (scanMsg != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(scanMsg, fontSize = 12.sp, color = Brand.GreenDeep, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(12.dp))
                    for ((rowIndex, rowTiles) in tiles.chunked(3).withIndex()) {
                        if (rowIndex > 0) Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (g in rowTiles) {
                                val v = groups[g] ?: 0.0
                                ServiceTile(
                                    group = g,
                                    amount = v,
                                    share = Fmt.percent(v, summary.total),
                                    highlighted = topGroup == g && v > 0.0,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    period.groupFilter = g
                                    onNav(1)
                                }
                            }
                            for (i in rowTiles.size until 3) Spacer(Modifier.weight(1f))
                        }
                    }
                    if (b.unassigned > 0.005) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brand.Field).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlassIcon(Ic.Wallet, Brand.Grey, Brand.Muted, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Airtime not yet used", fontWeight = FontWeight.SemiBold, color = Brand.Ink, fontSize = 14.sp)
                                Text("Recharged, not yet spent on a bundle", fontSize = 11.sp, color = Brand.Muted)
                            }
                            Text("${Fmt.etb(b.unassigned)} ETB", fontWeight = FontWeight.Bold, color = Brand.Ink)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SplitBar(tiles.map { Brand.group(it) to (groups[it] ?: 0.0) } + listOf(Brand.Grey to b.unassigned))

                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brand.Ink)
                        if (list.size > 5) {
                            TextButton(onClick = { period.groupFilter = "All"; onNav(1) }) {
                                Text("See all", color = Brand.GreenDeep, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (list.isEmpty()) {
                        if (!anyData) {
                            EmptyState(
                                Ic.Inbox,
                                "No telecom expenses yet",
                                "Tap Scan New Messages to read new telecom SMS. You can also load sample data from Settings to explore the app.",
                            )
                        } else {
                            EmptyState(Ic.Leaf, "Nothing in this period", "Use the arrows above to look at another period.")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (t in list.take(5)) TxCard(t) { onOpen(t.key) }
                        }
                    }
                    Spacer(Modifier.height(96.dp))
                }
            }
        }

        // Pinned blue call-to-action, like "Scan QR" in telebirr.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White)) {
            Button(
                onClick = onScan,
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(54.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Blue, contentColor = Color.White),
            ) {
                Icon(Ic.Scan, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (scanning) "Scanning…" else "Scan New Messages",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
