package et.teleexpense.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import et.teleexpense.Analytics
import et.teleexpense.CategoryClassifier
import et.teleexpense.EthiopianCalendar
import et.teleexpense.ExpenseTx
import et.teleexpense.Period
import et.teleexpense.Periods
import et.teleexpense.Spending

@Composable
fun AnalyticsScreen(txs: List<ExpenseTx>, mine: Set<String>, period: PeriodState) {
    val scheme = MaterialTheme.colorScheme
    val range = period.range
    val list = Analytics.inRange(txs, range)
    val summary = Analytics.summary(list, range)
    val b = Spending.breakdown(list, mine)
    val groups = b.byGroup
    val ethYear = EthiopianCalendar.fromGregorian(range.from).year

    // A comparison is only shown when the previous period actually has data. No "savings" claims.
    val previous = if (period.period == Period.CUSTOM) null else Periods.range(period.period, period.offset - 1)
    val prevTotal = if (previous == null) 0.0 else Analytics.inRange(txs, previous).filter { it.isCash }.sumOf { it.amount }

    Column(
        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Where is my money going?", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = scheme.onBackground)
        PeriodSelector(period, onGreen = false)

        SectionCard("Total spending") {
            Text("ETB ${Fmt.etb(summary.total)}", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
            if (previous != null && prevTotal > 0.0) {
                val change = (summary.total - prevTotal) / prevTotal * 100.0
                val word = if (change >= 0) "more" else "less"
                Text(
                    "${Math.abs(Math.round(change))}% $word than ${previous.title}",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Transactions", summary.count.toString(), Modifier.weight(1f))
                Stat("Avg / day", Fmt.etb(summary.avgPerDay), Modifier.weight(1f))
                Stat("Avg / transaction", summary.avgPerTx?.let { Fmt.etb(it) } ?: "—", Modifier.weight(1f))
            }
        }

        SectionCard("By service") {
            if (summary.total <= 0.0) {
                Text("No spending in this period.", fontSize = 14.sp, color = scheme.onSurfaceVariant)
            } else {
                for (g in Spending.groups) {
                    val v = groups[g] ?: 0.0
                    if (v <= 0.0) continue
                    Row(Modifier.fillMaxWidth()) {
                        Text(g, modifier = Modifier.weight(1f), color = scheme.onSurface, fontWeight = FontWeight.Medium)
                        Text("${Fmt.percent(v, summary.total)} · ${Fmt.etb(v)} ETB", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(5.dp))
                    ShareBar((v / summary.total).toFloat(), Brand.group(g))
                    Spacer(Modifier.height(10.dp))
                }
                if (b.unassigned > 0.005) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("Airtime not yet used", modifier = Modifier.weight(1f), color = scheme.onSurface, fontWeight = FontWeight.Medium)
                        Text("${Fmt.percent(b.unassigned, summary.total)} · ${Fmt.etb(b.unassigned)} ETB", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(5.dp))
                    ShareBar((b.unassigned / summary.total).toFloat(), Brand.Grey)
                }
            }
        }

        if (range.days in 2..31) {
            SectionCard("Daily spending") {
                BarChart(Analytics.daily(list, range), Brand.Green, labelEvery = if (range.days > 14) 5 else 1)
            }
        }
        if (range.days > 7) {
            SectionCard("Weekly spending") {
                BarChart(Analytics.weekly(list, range), Brand.Blue, labelEvery = if (range.days > 100) 4 else 1)
            }
        }
        SectionCard("Monthly spending · $ethYear") {
            BarChart(Analytics.monthly(txs, ethYear), Brand.Yellow, labelEvery = 1)
        }

        val top = Analytics.topPurchases(list)
        if (top.isNotEmpty()) {
            SectionCard("Biggest spending") {
                for (item in top) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(item.first, color = scheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 2)
                            Text("×${item.second}", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                        }
                        Text("${Fmt.etb(item.third)} ETB", fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    }
                }
            }
        }

        if (prevTotal <= 0.0) {
            Text(
                "Trends and comparisons appear once there is enough history.",
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = scheme.onSurface)
        Text(label, fontSize = 11.sp, color = scheme.onSurfaceVariant)
    }
}
