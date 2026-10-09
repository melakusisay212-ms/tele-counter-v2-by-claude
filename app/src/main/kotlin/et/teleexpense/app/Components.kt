package et.teleexpense.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import et.teleexpense.CategoryClassifier
import et.teleexpense.DateRange
import et.teleexpense.EthDate
import et.teleexpense.EthiopianCalendar
import et.teleexpense.ExpenseTx
import et.teleexpense.Period
import et.teleexpense.Periods
import java.time.LocalDate

/** View state shared by Home, Transactions and Analytics so the app feels consistent. */
@Stable
class PeriodState {
    var period by mutableStateOf(Period.MONTH)
    var offset by mutableIntStateOf(0)
    var customFrom by mutableStateOf<EthDate?>(null)
    var customTo by mutableStateOf<EthDate?>(null)

    /** "All", or one of Voice / Data / SMS / Other. Set by tapping a service tile on Home. */
    var groupFilter by mutableStateOf("All")

    val range: DateRange
        get() = Periods.range(period, offset, LocalDate.now(), customFrom, customTo)

    fun select(p: Period) {
        period = p
        offset = 0
    }
}

// ---------- surfaces ----------

@Composable
fun TeleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    var m = modifier
        .clip(shape)
        .background(Color.White)
        .border(1.dp, Brand.Line, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(modifier = m, content = content)
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    TeleCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brand.Ink)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassIcon(icon, Brand.Green, Brand.GreenDeep, size = 64.dp, corner = 20.dp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brand.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, fontSize = 14.sp, color = Brand.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun LogoMark(dimension: Dp) {
    Canvas(Modifier.size(dimension)) {
        val s = this.size.minDimension
        drawCircle(color = Color.White, radius = s / 2f)
        drawCircle(color = Brand.Ink, radius = s / 2f * 0.84f)
        drawCircle(color = Color.White, radius = s / 2f * 0.78f)
        val bw = s * 0.11f
        val base = s * 0.68f
        val x0 = s * 0.30f
        drawRect(Brand.Ink, topLeft = Offset(x0, base - s * 0.14f), size = Size(bw, s * 0.14f))
        drawRect(Brand.Ink, topLeft = Offset(x0 + bw * 1.6f, base - s * 0.26f), size = Size(bw, s * 0.26f))
        drawRect(Brand.GreenDeep, topLeft = Offset(x0 + bw * 3.2f, base - s * 0.38f), size = Size(bw, s * 0.38f))
    }
}

// ---------- chips, tabs, tiles ----------

/** Rounded pill. On the green hero the selected pill is black; on white pages it is green. */
@Composable
fun PillChip(label: String, selected: Boolean, onGreen: Boolean, onClick: () -> Unit) {
    val bg = when {
        selected && onGreen -> Brand.Ink
        selected -> Brand.Green
        onGreen -> Color.White.copy(alpha = 0.55f)
        else -> Brand.Field
    }
    val fg = when {
        selected && onGreen -> Color.White
        else -> Brand.Ink
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** telebirr-style segmented control: pale green track, solid green selected segment. */
@Composable
fun SegmentedTabs(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brand.GreenPale),
    ) {
        for (o in options) {
            val sel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(if (sel) Brand.Green else Color.Transparent)
                    .clickable { onSelect(o) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    o,
                    color = Brand.Ink,
                    fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** One service tile (Voice / Data / SMS / Other) with a share badge, like the telebirr denomination tiles. */
@Composable
fun ServiceTile(group: String, amount: Double, share: String, highlighted: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(shape)
                .background(Color.White)
                .border(if (highlighted) 2.dp else 1.dp, if (highlighted) Brand.Green else Brand.Line, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlassIcon(Ic.forGroup(group), Brand.group(group), Brand.groupDeep(group), size = 44.dp)
            Spacer(Modifier.height(8.dp))
            Text(group, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Brand.Ink, maxLines = 1)
            Text(Fmt.groupHint(group), fontSize = 10.sp, color = Brand.Muted, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(Fmt.etb(amount), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brand.Ink, maxLines = 1)
            Text("ETB", fontSize = 11.sp, color = Brand.Muted)
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(50))
                .background(if (highlighted) Brand.Green else Brand.Field)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) { Text(share, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink) }
    }
}

/** Thin stacked bar showing how the total splits across services. */
@Composable
fun SplitBar(parts: List<Pair<Color, Double>>) {
    val total = parts.sumOf { it.second }
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val r = this.size.height / 2f
        drawRoundRect(color = Brand.Field, size = this.size, cornerRadius = CornerRadius(r, r))
        if (total > 0.0) {
            var x = 0f
            for ((color, v) in parts) {
                if (v <= 0.0) continue
                val w = (v / total * this.size.width).toFloat()
                drawRect(color = color, topLeft = Offset(x, 0f), size = Size(w, this.size.height))
                x += w
            }
        }
    }
}

// ---------- period selector ----------

@Composable
private fun ArrowButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Text(symbol, color = if (enabled) Brand.Ink else Brand.Ink.copy(alpha = 0.3f), fontSize = 26.sp)
    }
}

@Composable
fun PeriodSelector(state: PeriodState, onGreen: Boolean) {
    var pickCustom by remember { mutableStateOf(false) }
    val range = state.range
    val custom = state.period == Period.CUSTOM
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (p in Period.values()) {
                PillChip(p.label, state.period == p, onGreen) {
                    state.select(p)
                    if (p == Period.CUSTOM) pickCustom = true
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (custom) Spacer(Modifier.width(48.dp)) else ArrowButton("‹", true) { state.offset = state.offset - 1 }
            Text(
                range.title,
                modifier = Modifier
                    .weight(1f)
                    .then(if (custom) Modifier.clickable { pickCustom = true } else Modifier),
                color = Brand.Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            if (custom) Spacer(Modifier.width(48.dp)) else ArrowButton("›", state.offset < 0) { state.offset = state.offset + 1 }
        }
    }
    if (pickCustom) CustomRangeDialog(state) { pickCustom = false }
}

@Composable
private fun CustomRangeDialog(state: PeriodState, onClose: () -> Unit) {
    val today = EthiopianCalendar.today()
    var from by remember { mutableStateOf(state.customFrom ?: EthDate(today.year, today.month, 1)) }
    var to by remember { mutableStateOf(state.customTo ?: today) }
    var editing by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Custom period", color = Brand.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField("From", from) { editing = 1 }
                DateField("To", to) { editing = 2 }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val lo = if (from <= to) from else to
                val hi = if (from <= to) to else from
                state.customFrom = lo
                state.customTo = hi
                state.period = Period.CUSTOM
                state.offset = 0
                onClose()
            }) { Text("Apply", color = Brand.GreenDeep, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel", color = Brand.Muted) } },
    )
    if (editing == 1) EthDatePickerDialog("From", from, { editing = 0 }) { from = it; editing = 0 }
    if (editing == 2) EthDatePickerDialog("To", to, { editing = 0 }) { to = it; editing = 0 }
}

@Composable
fun DateField(label: String, value: EthDate, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brand.Field)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Brand.Muted, fontSize = 13.sp, modifier = Modifier.width(56.dp))
        Text(value.toString(), color = Brand.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("Change", color = Brand.GreenDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(60.dp), fontSize = 13.sp, color = Brand.Muted)
        IconButton(onClick = onMinus) { Text("‹", fontSize = 24.sp, color = Brand.Ink) }
        Text(value, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = Brand.Ink)
        IconButton(onClick = onPlus) { Text("›", fontSize = 24.sp, color = Brand.Ink) }
    }
}

/** Ethiopian date picker: year, month (incl. Pagume) and day steppers. Days are clamped to the month length. */
@Composable
fun EthDatePickerDialog(title: String, initial: EthDate, onDismiss: () -> Unit, onPick: (EthDate) -> Unit) {
    var y by remember { mutableIntStateOf(initial.year) }
    var m by remember { mutableIntStateOf(initial.month) }
    var d by remember { mutableIntStateOf(initial.day) }
    val maxDay = EthiopianCalendar.daysInMonth(y, m)
    val day = if (d > maxDay) maxDay else d
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Brand.Ink) },
        text = {
            Column {
                Stepper("Year", y.toString(), { if (y > 2000) y = y - 1 }, { if (y < 2100) y = y + 1 })
                Stepper("Month", EthDate.MONTHS[m - 1], { m = if (m == 1) 13 else m - 1 }, { m = if (m == 13) 1 else m + 1 })
                Stepper("Day", day.toString(), { d = if (day == 1) maxDay else day - 1 }, { d = if (day >= maxDay) 1 else day + 1 })
                Spacer(Modifier.height(8.dp))
                Text(
                    Fmt.gregorian(EthiopianCalendar.toGregorian(EthDate(y, m, day))),
                    fontSize = 12.sp,
                    color = Brand.Muted,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onPick(EthDate(y, m, day)) }) { Text("OK", color = Brand.GreenDeep, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Brand.Muted) } },
    )
}

// ---------- transaction card ----------

@Composable
fun TxCard(t: ExpenseTx, onClick: () -> Unit) {
    val group = if (t.category == "airtime_sent") "Others" else CategoryClassifier.groupOf(t.category)
    TeleCard(Modifier.fillMaxWidth(), onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(Ic.forCategory(t.category), Brand.group(group), Brand.groupDeep(group), size = 46.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(t.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Brand.Ink)
                Text("${t.ethDate} · ${Fmt.time24(t.dateTime)}", fontSize = 12.sp, color = Brand.Muted)
                Text(if (t.isCash) "${t.provider} · $group" else "Paid from airtime · $group", fontSize = 12.sp, color = Brand.Muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${Fmt.etb(t.amount)} ETB", fontWeight = FontWeight.Bold, color = Brand.Ink)
                if (t.userEdited) Text("edited", fontSize = 10.sp, color = Brand.Muted)
            }
        }
    }
}

// ---------- charts ----------

@Composable
fun Donut(values: List<Pair<Color, Double>>, centerTop: String, centerBottom: String, modifier: Modifier = Modifier) {
    val total = values.sumOf { it.second }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.16f
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = Brand.Field, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(width = stroke),
            )
            if (total > 0.0) {
                val nonZero = values.count { it.second > 0.0 }
                val gap = if (nonZero > 1) 3f else 0f
                var start = -90f
                for ((color, v) in values) {
                    if (v <= 0.0) continue
                    val sweep = (v / total * 360.0).toFloat()
                    drawArc(
                        color = color, startAngle = start + gap / 2f, sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTop, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brand.Ink)
            Text(centerBottom, fontSize = 11.sp, color = Brand.Muted)
        }
    }
}

@Composable
fun LegendRow(group: String, amount: Double, total: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Brand.group(group)))
        Spacer(Modifier.width(8.dp))
        Text(group, modifier = Modifier.weight(1f), color = Brand.Ink, fontSize = 14.sp)
        Text(Fmt.etb(amount), fontWeight = FontWeight.SemiBold, color = Brand.Ink, fontSize = 14.sp)
        Text("  ${Fmt.percent(amount, total)}", color = Brand.Muted, fontSize = 12.sp)
    }
}

@Composable
fun BarChart(data: List<Pair<String, Double>>, color: Color, chartHeight: Dp = 120.dp, labelEvery: Int = 1) {
    val maxValue = (data.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            val n = data.size.coerceAtLeast(1)
            val slot = this.size.width / n
            val barWidth = slot * 0.62f
            val minBar = 4.dp.toPx()
            data.forEachIndexed { i, entry ->
                val v = entry.second
                if (v > 0.0) {
                    val h = ((v / maxValue).toFloat() * this.size.height).coerceAtLeast(minBar)
                    val x = i * slot + (slot - barWidth) / 2f
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, this.size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
                    )
                }
            }
            drawLine(color = Brand.Line, start = Offset(0f, this.size.height), end = Offset(this.size.width, this.size.height), strokeWidth = 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            data.forEachIndexed { i, entry ->
                Text(
                    if (labelEvery <= 1 || i % labelEvery == 0) entry.first else "",
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    color = Brand.Muted,
                )
            }
        }
    }
}


@Composable
fun ShareBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Brand.Field)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

/** Rename dialog for a phone number (own SIM or someone else). Saving a blank name removes the name. */
@Composable
fun NameDialog(number: String, current: String, onSave: (String) -> Unit, onClose: () -> Unit) {
    var text by remember(number) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Name for $number") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(30) },
                singleLine = true,
                label = { Text("Name (for example Mom)") },
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text); onClose() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
