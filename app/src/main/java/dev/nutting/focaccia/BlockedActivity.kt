package dev.nutting.focaccia

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class BlockedActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val hasTag = BlockedAppsRepository.getRegisteredTagId(this) != null
        setContent {
            BlockedScreen(
                onGoBack = { goHome() },
                showNfcHint = hasTag,
                onDebugUnlock = { unlock() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
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
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != NfcAdapter.ACTION_TAG_DISCOVERED) return

        @Suppress("DEPRECATION")
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val tagId = tag.id.joinToString("") { "%02X".format(it) }
        val registeredId = BlockedAppsRepository.getRegisteredTagId(this)

        if (tagId == registeredId) {
            unlock()
        }
    }

    private fun unlock() {
        val until = System.currentTimeMillis() + BlockedAppsRepository.UNLOCK_DURATION_MS
        BlockedAppsRepository.setBlockingDisabledUntil(this, until)
        startService(Intent(this, UnlockCountdownService::class.java))
        finish()
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }
}

@Composable
fun BlockedScreen(
    onGoBack: () -> Unit,
    showNfcHint: Boolean = false,
    onDebugUnlock: () -> Unit = {}
) {
    val textColor = Color(0xFF37474F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F0)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Not right now",
                color = textColor,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This app is blocked right now.",
                color = textColor.copy(alpha = 0.6f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            if (showNfcHint) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scan your NFC tag to unblock",
                    color = textColor.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = onGoBack) {
                Text("Go Back")
            }
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onDebugUnlock) {
                    Text("[Debug] Simulate Tap")
                }
            }
        }
    }
}
