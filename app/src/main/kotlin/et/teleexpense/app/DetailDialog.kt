package et.teleexpense.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import et.teleexpense.EthDate
import et.teleexpense.EthiopianCalendar
import et.teleexpense.ExpenseTx
import et.teleexpense.TxStatus
import java.time.LocalDateTime

@Composable
fun DetailDialog(
    tx: ExpenseTx,
    onClose: () -> Unit,
    onSave: (category: String, amount: Double, dateTime: LocalDateTime) -> Unit,
    onStatus: (TxStatus) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var editing by remember(tx.key) { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(26.dp),
            color = scheme.surface,
        ) {
            Column(
                Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                if (editing) EditContent(tx, onCancel = { editing = false }, onSave = onSave)
                else ViewContent(tx, onClose = onClose, onEdit = { editing = true }, onStatus = onStatus)
            }
        }
    }
}

@Composable
private fun ViewContent(tx: ExpenseTx, onClose: () -> Unit, onEdit: () -> Unit, onStatus: (TxStatus) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val group = Fmt.groupOf(tx.category)
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlassIcon(Ic.forCategory(tx.category), Brand.group(group), Brand.groupDeep(group), size = 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(Fmt.categoryLabel(tx.category), color = scheme.onSurfaceVariant, fontSize = 13.sp)
            Text("ETB ${Fmt.etb(tx.amount)}", fontWeight = FontWeight.Bold, fontSize = 32.sp, color = scheme.onSurface)
        }
        TextButton(onClick = onClose) { Text("Close") }
    }
    if (tx.packageName != null) {
        Spacer(Modifier.height(6.dp))
        Text(tx.packageName!!, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
    }
    Spacer(Modifier.height(14.dp))

    Info("Ethiopian date", tx.ethDate.toString())
    Info("Gregorian", "${Fmt.gregorian(tx.dateTime.toLocalDate())} · ${Fmt.time12(tx.dateTime)}")
    Info("Provider", tx.provider)
    if (tx.recipient != null) Info("Recipient", tx.recipient!!)
    if (tx.transactionId != null) Info("Transaction", tx.transactionId!!)
    Info(
        "Detected by",
        "SMS rules · ${Math.round(tx.confidence * 100)}% confident" + if (tx.userEdited) " · edited by you" else "",
    )
    Info("Grouped SMS", Fmt.plural(tx.smsCount, "related SMS", "related SMS"))

    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
    ) { Text("Edit") }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onStatus(TxStatus.NOT_EXPENSE) },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Not an expense", fontSize = 13.sp) }
        OutlinedButton(
            onClick = { onStatus(TxStatus.DELETED) },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Delete", fontSize = 13.sp, color = Brand.Danger) }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        "Hidden transactions can be restored from Settings.",
        fontSize = 11.sp,
        color = scheme.onSurfaceVariant,
    )
}

@Composable
private fun EditContent(
    tx: ExpenseTx,
    onCancel: () -> Unit,
    onSave: (String, Double, LocalDateTime) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var category by remember(tx.key) { mutableStateOf(tx.category) }
    var amountText by remember(tx.key) { mutableStateOf(editableAmount(tx.amount)) }
    var date by remember(tx.key) { mutableStateOf(tx.ethDate) }
    var pickDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Text("Edit transaction", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = scheme.onSurface)
    Spacer(Modifier.height(14.dp))

    Text("Category", fontSize = 12.sp, color = scheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (c in Fmt.editableCategories) {
            PillChip(Fmt.categoryLabel(c), category == c, onGreen = false) { category = c }
        }
    }
    Spacer(Modifier.height(14.dp))

    OutlinedTextField(
        value = amountText,
        onValueChange = { raw -> amountText = raw.filter { ch -> ch.isDigit() || ch == '.' } },
        label = { Text("Amount (ETB)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    DateField("Date", date) { pickDate = true }
    if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(error!!, color = Brand.Danger, fontSize = 13.sp)
    }
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Cancel") }
        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    error = "Enter an amount greater than 0."
                } else if (!EthiopianCalendar.isValid(date.year, date.month, date.day)) {
                    error = "That Ethiopian date does not exist."
                } else {
                    val dt = EthiopianCalendar.toGregorian(date).atTime(tx.dateTime.toLocalTime())
                    onSave(category, amount, dt)
                }
            },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Save") }
    }
    if (pickDate) {
        EthDatePickerDialog("Transaction date", date, { pickDate = false }) { date = it; pickDate = false }
    }
}

private fun editableAmount(v: Double): String =
    if (Math.abs(v - Math.round(v)) < 0.005) Math.round(v).toString() else String.format(java.util.Locale.US, "%.2f", v)

@Composable
private fun Info(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, fontSize = 11.sp, color = scheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp, color = scheme.onSurface)
    }
}
