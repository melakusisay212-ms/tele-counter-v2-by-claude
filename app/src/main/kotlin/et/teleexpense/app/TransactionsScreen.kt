package et.teleexpense.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import et.teleexpense.Spending
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import et.teleexpense.Analytics
import et.teleexpense.CategoryClassifier
import et.teleexpense.EthiopianCalendar
import et.teleexpense.ExpenseTx

@Composable
fun TransactionsScreen(
    txs: List<ExpenseTx>,
    mine: Set<String>,
    names: Map<String, String>,
    period: PeriodState,
    onRename: (String, String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val range = period.range
    val filter = period.groupFilter
    val inRange = Analytics.inRange(txs, range)
    val list = if (filter == "All") inRange else inRange.filter { Spending.groupOf(it, mine) == filter }
    var renaming by remember { mutableStateOf<String?>(null) }
    val days = list.groupBy { it.dateTime.toLocalDate() }.entries.sortedByDescending { it.key }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("Transactions", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Brand.Ink)
            Spacer(Modifier.height(10.dp))
            PeriodSelector(period, onGreen = false)
            Spacer(Modifier.height(8.dp))
            SegmentedTabs(listOf("All") + Spending.groups, filter) { period.groupFilter = it }
            Spacer(Modifier.height(8.dp))
            Text(
                "${Fmt.plural(list.size, "transaction", "transactions")} · ETB ${Fmt.etb(list.filter { it.isCash }.sumOf { it.amount })} paid" +
                    (list.filter { !it.isCash }.sumOf { it.amount }.let { if (it > 0.0) " · ETB ${Fmt.etb(it)} from airtime" else "" }),
                fontSize = 13.sp,
                color = Brand.Muted,
            )
        }
        if (list.isEmpty()) {
            EmptyState(Ic.Transactions, "No transactions here", "Nothing matches this period and filter yet.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (filter == "Others") {
                    val byNumber = list.groupBy { it.recipient?.let { r -> Spending.last9(r) } ?: "unknown" }.entries
                        .sortedByDescending { e -> e.value.sumOf { it.amount } }
                    for (entry in byNumber) {
                        val number = entry.key
                        item(key = "number-$number") {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(names[number] ?: number, fontWeight = FontWeight.SemiBold, color = Brand.Ink)
                                    if (names[number] != null) Text(number, fontSize = 12.sp, color = Brand.Muted)
                                }
                                Text("${Fmt.etb(entry.value.sumOf { it.amount })} ETB", fontSize = 13.sp, color = Brand.Muted)
                                if (number != "unknown") {
                                    TextButton(onClick = { renaming = number }) { Text(if (names[number] == null) "Name" else "Rename") }
                                }
                            }
                        }
                        items(entry.value, key = { it.key }) { t -> TxCard(t) { onOpen(t.key) } }
                    }
                    return@LazyColumn
                }
                for (entry in days) {
                    val eth = EthiopianCalendar.fromGregorian(entry.key)
                    item(key = "header-${entry.key}") {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(eth.toString(), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Brand.Ink)
                            Text("${Fmt.etb(entry.value.sumOf { it.amount })} ETB", fontSize = 13.sp, color = Brand.Muted)
                        }
                    }
                    items(entry.value, key = { it.key }) { t -> TxCard(t) { onOpen(t.key) } }
                }
            }
        }
    }

    val toRename = renaming
    if (toRename != null) {
        NameDialog(toRename, names[toRename].orEmpty(), onSave = { onRename(toRename, it) }, onClose = { renaming = null })
    }
}
