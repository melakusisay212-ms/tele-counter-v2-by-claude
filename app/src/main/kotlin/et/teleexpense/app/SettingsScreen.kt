package et.teleexpense.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import et.teleexpense.EthiopianCalendar
import et.teleexpense.ExpenseTx
import et.teleexpense.TxStatus
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@Composable
fun SettingsScreen(
    repo: Repo,
    txs: List<ExpenseTx>,
    permissionGranted: Boolean,
    scanning: Boolean,
    scanMsg: String?,
    onScan: () -> Unit,
    onReset: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmErase by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    val mine by repo.mine.collectAsState()
    val names by repo.names.collectAsState()
    var renaming by remember { mutableStateOf<String?>(null) }
    var newNumber by remember { mutableStateOf("") }

    val hidden = txs.count { it.status != TxStatus.ACTIVE }
    val startedMillis = repo.prefs.startedAt
    val lastScanMillis = repo.prefs.lastScanAt

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = scheme.onBackground)

        SectionCard("Tracking") {
            if (startedMillis > 0L) {
                val d = Instant.ofEpochMilli(startedMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                Text("Tracking since ${EthiopianCalendar.fromGregorian(d)}", color = scheme.onSurface, fontWeight = FontWeight.Medium)
                Text(Fmt.gregorian(d), fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "SMS access: " + if (permissionGranted) "allowed" else "not allowed",
                fontSize = 13.sp,
                color = if (permissionGranted) scheme.onSurfaceVariant else Brand.Danger,
            )
            if (lastScanMillis > 0L) {
                Text(
                    "Last checked: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastScanMillis)),
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onScan,
                enabled = !scanning && permissionGranted,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (scanning) "Scanning…" else "Scan New Messages") }
            if (scanMsg != null) {
                Spacer(Modifier.height(6.dp))
                Text(scanMsg, fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
            if (!permissionGranted) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "SMS access is needed to detect telecom messages. If Android blocks the permission for an app installed outside the Play Store, open App info, tap the ⋮ menu and choose \"Allow restricted settings\", then allow SMS.",
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { openAppSettings(context) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Open app settings") }
            }
        }

        SectionCard("Your data") {
            Text("Everything is stored on this phone only.", fontSize = 13.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            if (hidden > 0) {
                OutlinedButton(
                    onClick = { scope.launch { repo.restoreHidden(); info = "Restored $hidden hidden transaction(s)." } },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Restore $hidden hidden transaction(s)") }
                Spacer(Modifier.height(8.dp))
            }
            if (!repo.prefs.samplesLoaded) {
                OutlinedButton(
                    onClick = { scope.launch { repo.loadSamples(); info = "Sample data added. Erase all data to remove it." } },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Load sample data") }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = { confirmErase = true },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Erase all data", color = Brand.Danger) }
            if (info != null) {
                Spacer(Modifier.height(6.dp))
                Text(info!!, fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
        }

        SectionCard("My numbers") {
            Text(
                "Your own SIMs. Anything bought for or sent to a number that is not listed here goes under Others. " +
                    "Numbers are found automatically from your messages; you can add or remove them.",
                fontSize = 13.sp,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            if (mine.isEmpty()) {
                Text("No numbers yet.", fontSize = 13.sp, color = scheme.onSurfaceVariant)
            }
            for (n in mine.sorted()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(names[n] ?: n, color = scheme.onSurface, fontWeight = FontWeight.Medium)
                        if (names[n] != null) Text(n, fontSize = 12.sp, color = scheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { renaming = n }) { Text(if (names[n] == null) "Name" else "Rename") }
                    TextButton(onClick = { repo.removeNumber(n) }) { Text("Remove", color = Brand.Danger) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newNumber,
                    onValueChange = { newNumber = it.filter { ch -> ch.isDigit() }.take(13) },
                    singleLine = true,
                    label = { Text("Add a number") },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (repo.addNumber(newNumber)) newNumber = "" }, enabled = newNumber.length >= 9) { Text("Add") }
            }
        }

        SectionCard("Automatic tracking") {
            Text(
                "New telecom messages are read automatically as they arrive, and the app also checks in the background every 15 minutes. " +
                    "If your phone stops background apps, allow Tele Expense to run in the background in the phone's battery settings.",
                fontSize = 13.sp,
                color = scheme.onSurface,
            )
        }

        SectionCard("Privacy") {
            Text(
                "No account, no server and no internet needed. Messages are read and processed on this phone only. " +
                    "The full text of a message is never saved — only the extracted amount, date and reference numbers.",
                fontSize = 13.sp,
                color = scheme.onSurface,
            )
        }

        SectionCard("About") {
            Text("Tele Expense · version 2.1", color = scheme.onSurface, fontWeight = FontWeight.Medium)
            Text("Developed by Melaku Sisay", color = scheme.onSurface, fontSize = 13.sp)
            Text(
                "Rule-based SMS parser with transaction grouping. Ethiopian calendar first.",
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    val toRename = renaming
    if (toRename != null) {
        NameDialog(toRename, names[toRename].orEmpty(), onSave = { repo.setName(toRename, it) }, onClose = { renaming = null })
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase all data?") },
            text = { Text("This removes every transaction and starts the app from scratch. Your SMS inbox is not touched.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmErase = false
                    scope.launch { repo.eraseEverything(); onReset() }
                }) { Text("Erase", color = Brand.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } },
        )
    }
}
