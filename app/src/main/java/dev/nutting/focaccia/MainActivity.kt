package dev.nutting.focaccia

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nutting.focaccia.model.AppInfo
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var viewModel: AppListViewModel? = null
    private val relockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel?.refreshUnlockState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        requestNotificationPermission()
        setContent {
            MaterialTheme {
                val vm: AppListViewModel = viewModel()
                viewModel = vm
                MainScreen(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.refreshUnlockState()
        registerReceiver(
            relockReceiver,
            IntentFilter(UnlockCountdownService.ACTION_RELOCK),
            RECEIVER_NOT_EXPORTED
        )
        nfcAdapter?.let { adapter ->
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_MUTABLE
            )
            val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
            adapter.enableForegroundDispatch(this, pendingIntent, filters, null)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(relockReceiver)
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != NfcAdapter.ACTION_TAG_DISCOVERED) return

        @Suppress("DEPRECATION")
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val tagId = tag.id.joinToString("") { "%02X".format(it) }
        val vm = viewModel ?: return
        val state = vm.uiState.value

        if (state.isRegistering) {
            vm.registerTag(tagId)
        } else if (tagId == state.registeredTagId) {
            vm.unlockBlocking()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppListViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focaccia") },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!state.accessibilityEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Accessibility service not enabled",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Focaccia needs the accessibility service to block apps. Enable it in Settings to activate app blocking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        ) {
                            Text("Open Accessibility Settings")
                        }
                    }
                }
            }

            val controlsAlpha = if (state.accessibilityEnabled) 1f else 0.5f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .alpha(controlsAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("App blocking", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = state.blockingEnabled,
                    enabled = state.accessibilityEnabled,
                    onCheckedChange = { viewModel.setBlockingEnabled(it) }
                )
            }

            // NFC Tag section
            NfcTagSection(
                registeredTagId = state.registeredTagId,
                blockingDisabledUntil = state.blockingDisabledUntil,
                isRegistering = state.isRegistering,
                justRegistered = state.justRegistered,
                onRegister = { viewModel.startRegistering() },
                onCancelRegister = { viewModel.stopRegistering() },
                onClear = { viewModel.clearTag() },
                onRelock = { viewModel.relockBlocking() },
                onRegisteredSeen = { viewModel.clearJustRegistered() },
                onDebugRegister = { viewModel.registerTag("DEBUG00") },
                onDebugUnlock = { viewModel.unlockBlocking() }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val allSelected = state.apps.isNotEmpty() &&
                    state.apps.all { it.packageName in state.blockedApps }
                Text("Select all", style = MaterialTheme.typography.bodyLarge)
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { viewModel.toggleSelectAll() }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            LazyColumn {
                items(state.apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        isBlocked = app.packageName in state.blockedApps,
                        onToggle = { viewModel.toggleAppBlocked(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun NfcTagSection(
    registeredTagId: String?,
    blockingDisabledUntil: Long,
    isRegistering: Boolean,
    justRegistered: Boolean = false,
    onRegister: () -> Unit,
    onCancelRegister: () -> Unit,
    onClear: () -> Unit,
    onRelock: () -> Unit = {},
    onRegisteredSeen: () -> Unit = {},
    onDebugRegister: () -> Unit = {},
    onDebugUnlock: () -> Unit = {}
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear NFC tag?") },
            text = { Text("You'll need to scan a tag again to unlock apps.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClear()
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (isRegistering) {
            val infiniteTransition = rememberInfiniteTransition(label = "nfc-pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "nfc-pulse-alpha"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_nfc),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(pulseAlpha),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan an NFC tag to register it...", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = onCancelRegister) {
                Text("Cancel")
            }
        } else if (registeredTagId != null) {
            if (justRegistered) {
                Text(
                    "Tag registered!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LaunchedEffect(Unit) {
                    delay(2000L)
                    onRegisteredSeen()
                }
            }
            Text(
                "NFC tag registered: $registeredTagId",
                style = MaterialTheme.typography.bodyMedium
            )

            val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(blockingDisabledUntil) {
                while (blockingDisabledUntil > now.longValue) {
                    delay(15_000L)
                    now.longValue = System.currentTimeMillis()
                }
            }
            val remaining = blockingDisabledUntil - now.longValue
            if (remaining > 0) {
                val minutes = (remaining / 60_000) + 1
                Text(
                    "Apps unblocked for ~$minutes min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onRelock) {
                    Text("Reblock")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = { showClearDialog = true }) {
                Text("Clear Tag")
            }
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onDebugUnlock) {
                    Text("[Debug] Simulate Tap")
                }
            }
        } else {
            Button(onClick = onRegister) {
                Text("Register NFC Tag")
            }
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onDebugRegister) {
                    Text("[Debug] Register Fake Tag")
                }
            }
        }
    }
}

@Composable
fun AppRow(app: AppInfo, isBlocked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { drawable ->
            val bitmap = remember(drawable) { drawable.toBitmap(48, 48).asImageBitmap() }
            val grayscale = ColorMatrix().apply { setToSaturation(0f) }
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .size(40.dp)
                    .then(if (isBlocked) Modifier.alpha(0.4f) else Modifier),
                colorFilter = if (isBlocked) ColorFilter.colorMatrix(grayscale) else null
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(checked = isBlocked, onCheckedChange = { onToggle() })
    }
}
