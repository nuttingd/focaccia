package dev.nutting.focaccia

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocacciaE2ETest {

    private lateinit var device: UiDevice
    private lateinit var context: Context

    companion object {
        private const val TIMEOUT = 5000L
        private const val BLOCKED_PACKAGE = "com.android.calculator2"
        private const val UNBLOCKED_PACKAGE = "com.android.deskclock"
    }

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Configure blocked apps via repository
        BlockedAppsRepository.setBlockedApps(context, setOf(BLOCKED_PACKAGE))
        BlockedAppsRepository.setBlockingEnabled(context, true)

        // Go home first
        device.pressHome()
        device.waitForIdle()
    }

    @Test
    fun blockedApp_showsBlockedScreen() {
        // Launch a blocked app
        val intent = context.packageManager.getLaunchIntentForPackage(BLOCKED_PACKAGE)
        assumeNotNull("$BLOCKED_PACKAGE not installed – skipping", intent)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        // Wait for BlockedActivity to appear
        val blockedText = device.wait(Until.findObject(By.text("Not right now")), TIMEOUT)
        assertNotNull("BlockedActivity should be shown for blocked app", blockedText)
    }

    @Test
    fun unblockedApp_opensNormally() {
        // Launch an unblocked app (Settings is always available)
        val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        // "App Blocked" should NOT appear within the timeout window
        val blockedText = device.wait(Until.findObject(By.text("Not right now")), TIMEOUT)
        assertNull("BlockedActivity should NOT be shown for unblocked app", blockedText)
    }
}
