package et.teleexpense.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import et.teleexpense.Analytics
import et.teleexpense.DateRange
import et.teleexpense.EthDate
import et.teleexpense.EthiopianCalendar
import et.teleexpense.ExpenseTx

@Composable
fun CalendarScreen(txs: List<ExpenseTx>, onOpen: (String) -> Unit) {
    val today = EthiopianCalendar.today()
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.month) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    val days = EthiopianCalendar.daysInMonth(year, month)
    val startCol = EthiopianCalendar.firstDayColumn(year, month)
    val monthStart = EthiopianCalendar.toGregorian(EthDate(year, month, 1))
    val range = DateRange(monthStart, monthStart.plusDays(days - 1L), "")
    val inMonth = Analytics.inRange(txs, range).filter { it.isCash }
    val byDay = inMonth.groupBy { it.ethDate.day }
    val maxDay = byDay.values.maxOfOrNull { l -> l.sumOf { it.amount } } ?: 0.0
    val canGoForward = year * 13 + month < today.year * 13 + today.month

    fun go(by: Int) {
        val next = EthiopianCalendar.shiftMonth(year, month, by)
        year = next.first
        month = next.second
        selected = null
    }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Calendar", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Brand.Ink)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { go(-1) }) { Text("‹", fontSize = 28.sp, color = Brand.Ink) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${EthDate.MONTHS[month - 1]} $year", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brand.Ink)
                Text(
                    "ETB ${Fmt.etb(inMonth.sumOf { it.amount })} · ${Fmt.plural(inMonth.size, "transaction", "transactions")}",
                    fontSize = 12.sp,
                    color = Brand.Muted,
                )
            }
            IconButton(onClick = { go(1) }, enabled = canGoForward) {
                Text("›", fontSize = 28.sp, color = if (canGoForward) Brand.Ink else Brand.Ink.copy(alpha = 0.3f))
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth()) {
            for (name in listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")) {
                Text(name, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = Brand.Muted)
            }
        }
        Spacer(Modifier.height(4.dp))

        val rows = (startCol + days + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - startCol + 1
                    if (day < 1 || day > days) {
                        Box(Modifier.weight(1f).height(56.dp))
                    } else {
                        val spend = byDay[day]?.sumOf { it.amount } ?: 0.0
                        val heat = if (maxDay > 0.0) (spend / maxDay).toFloat() else 0f
                        DayCell(
                            modifier = Modifier.weight(1f),
                            day = day,
                            spend = spend,
                            heat = heat,
                            isToday = year == today.year && month == today.month && day == today.day,
                            isSelected = selected == day,
                        ) { selected = day }
                    }
                }
            }
        }

        val sel = selected
        if (sel != null) {
            Spacer(Modifier.height(14.dp))
            val items = byDay[sel].orEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${EthDate.MONTHS[month - 1]} $sel, $year", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brand.Ink)
                Text(
                    Fmt.gregorian(EthiopianCalendar.toGregorian(EthDate(year, month, sel))),
                    fontSize = 12.sp,
                    color = Brand.Muted,
                )
                Text("Total: ${Fmt.etb(items.sumOf { it.amount })} ETB", fontWeight = FontWeight.SemiBold, color = Brand.Ink)
                if (items.isEmpty()) {
                    Text("No telecom spending on this day.", fontSize = 13.sp, color = Brand.Muted)
                } else {
                    for (t in items) TxCard(t) { onOpen(t.key) }
                }
            }
        } else {
            Spacer(Modifier.height(14.dp))
            Text(
                "Tap a day to see what you spent. Darker days mean more spending.",
                fontSize = 12.sp,
                color = Brand.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DayCell(
    modifier: Modifier,
    day: Int,
    spend: Double,
    heat: Float,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (spend > 0.0) Brand.Green.copy(alpha = 0.25f + 0.6f * heat) else Color.Transparent
    val textColor = Brand.Ink
    val ring = if (isSelected) Brand.GreenDeep else Brand.Muted
    Box(
        modifier
            .padding(2.dp)
            .height(52.dp)
            .clip(shape)
            .background(bg)
            .then(if (isSelected || isToday) Modifier.border(2.dp, ring, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                color = textColor,
                fontSize = 15.sp,
                fontWeight = if (spend > 0.0) FontWeight.Bold else FontWeight.Normal,
            )
            if (spend > 0.0) {
                Text(Fmt.etb(spend), color = textColor, fontSize = 9.sp, maxLines = 1)
            } else {
                Spacer(Modifier.height(11.dp))
            }
        }
    }
}
