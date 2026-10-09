package et.teleexpense.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import et.teleexpense.TxStatus
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Bumped on every resume so screens re-check permission and catch up on missed messages.
    private val resumeTick = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = Repo.get(applicationContext)
        BackgroundScan.schedule(applicationContext)
        setContent {
            TeleTheme {
                AppRoot(repo, resumeTick.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.value = resumeTick.value + 1
    }
}

private enum class Stage { SPLASH, WELCOME, SCANNING, RESULT, MAIN }

@Composable
fun AppRoot(repo: Repo, resumeTick: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val txs by repo.txs.collectAsState()

    var stage by rememberSaveable { mutableStateOf(Stage.SPLASH.name) }
    var thisMonth by rememberSaveable { mutableStateOf(true) }
    var denied by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ScanResult?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        if (repo.hasSmsPermission()) {
            denied = false
            stage = Stage.SCANNING.name
        } else {
            denied = true
        }
    }

    LaunchedEffect(Unit) { repo.load() }

    LaunchedEffect(stage) {
        if (stage == Stage.SPLASH.name) {
            kotlinx.coroutines.delay(1600)
            stage = if (repo.prefs.isSetUp) Stage.MAIN.name else Stage.WELCOME.name
        }
    }

    LaunchedEffect(stage) {
        if (stage == Stage.SCANNING.name) {
            result = repo.initialScan(thisMonth)
            repo.load()
            stage = Stage.RESULT.name
        }
    }

    when (stage) {
        Stage.SPLASH.name -> SplashScreen()
        Stage.WELCOME.name -> WelcomeScreen(
            permissionDenied = denied,
            onChoose = { month ->
                thisMonth = month
                if (repo.hasSmsPermission()) {
                    stage = Stage.SCANNING.name
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                }
            },
            onSamples = {
                scope.launch {
                    repo.loadSamples()
                    repo.load()
                    stage = Stage.MAIN.name
                }
            },
            onOpenSettings = { openAppSettings(context) },
        )
        Stage.SCANNING.name -> ScanningScreen()
        Stage.RESULT.name -> ResultScreen(result, txs, thisMonth) { stage = Stage.MAIN.name }
        else -> MainScreen(repo, resumeTick) {
            denied = false
            result = null
            stage = Stage.WELCOME.name
        }
    }
}

@Composable
fun MainScreen(repo: Repo, resumeTick: Int, onReset: () -> Unit) {
    val scope = rememberCoroutineScope()
    val txs by repo.txs.collectAsState()
    val mine by repo.mine.collectAsState()
    val names by repo.names.collectAsState()
    val period = remember { PeriodState() }
    var tab by rememberSaveable { mutableStateOf(0) }
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var scanMsg by remember { mutableStateOf<String?>(null) }
    val permissionGranted = repo.hasSmsPermission()

    fun describe(r: ScanResult): String = when {
        r.error != null -> r.error
        r.relevant == 0 -> "No new telecom messages."
        else -> "Checked ${r.relevant} new telecom message(s) · ${r.newExpenses} new expense(s)."
    }

    val runScan: () -> Unit = {
        if (!scanning) {
            scanning = true
            scope.launch {
                val r = repo.scanNew()
                scanning = false
                scanMsg = describe(r)
            }
        }
    }

    // Quietly catch up on anything received while the app was closed.
    LaunchedEffect(resumeTick) {
        if (repo.hasSmsPermission()) {
            val r = repo.scanNew()
            if (r.relevant > 0 || r.error != null) scanMsg = describe(r)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomBar(tab) { tab = it } },
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(txs, mine, period, scanning, scanMsg, runScan, { tab = it }, { detailKey = it })
                1 -> TransactionsScreen(txs, mine, names, period, { n, name -> repo.setName(n, name) }) { detailKey = it }
                2 -> CalendarScreen(txs) { detailKey = it }
                3 -> AnalyticsScreen(txs, mine, period)
                else -> SettingsScreen(repo, txs, permissionGranted, scanning, scanMsg, runScan, onReset)
            }
        }
    }

    val key = detailKey
    val detail = if (key == null) null else txs.firstOrNull { it.key == key }
    if (detail != null) {
        DetailDialog(
            tx = detail,
            onClose = { detailKey = null },
            onSave = { category, amount, dateTime ->
                scope.launch { repo.update(detail.key, category, amount, dateTime) }
                detailKey = null
            },
            onStatus = { status: TxStatus ->
                scope.launch { repo.setStatus(detail.key, status) }
                detailKey = null
            },
        )
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Ic.Home to "Home",
        Ic.Transactions to "Transactions",
        Ic.Calendar to "Calendar",
        Ic.Analytics to "Analytics",
        Ic.Settings to "Settings",
    )
    NavigationBar(containerColor = Brand.Green) {
        items.forEachIndexed { i, item ->
            NavigationBarItem(
                selected = selected == i,
                onClick = { onSelect(i) },
                icon = { Icon(item.first, contentDescription = item.second, tint = Brand.Ink, modifier = Modifier.size(24.dp)) },
                label = { Text(item.second, fontSize = 10.sp, maxLines = 1, softWrap = false) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = Brand.Ink,
                    unselectedTextColor = Brand.Ink,
                    indicatorColor = Color.White.copy(alpha = 0.45f),
                ),
            )
        }
    }
}
