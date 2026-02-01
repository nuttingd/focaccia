package dev.nutting.focaccia

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppBlockerAccessibilityService : AccessibilityService() {

    private var lastBlockedTime = 0L
    private lateinit var ignoredPackages: Set<String>

    override fun onCreate() {
        super.onCreate()
        val pm = packageManager
        val ignored = mutableSetOf(
            "dev.nutting.focaccia",
            "com.android.systemui",
            "com.android.settings"
        )

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        pm.resolveActivity(homeIntent, 0)?.activityInfo?.packageName?.let {
            ignored.add(it)
        }

        val dialIntent = Intent(Intent.ACTION_DIAL)
        pm.resolveActivity(dialIntent, 0)?.activityInfo?.packageName?.let {
            ignored.add(it)
        }

        ignoredPackages = ignored
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName in ignoredPackages) return

        val now = System.currentTimeMillis()
        if (now - lastBlockedTime < 1000) return

        if (!BlockedAppsRepository.isBlockingEnabled(this)) return
        if (System.currentTimeMillis() < BlockedAppsRepository.getBlockingDisabledUntil(this)) return
        if (packageName !in BlockedAppsRepository.getBlockedApps(this)) return

        lastBlockedTime = now
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
