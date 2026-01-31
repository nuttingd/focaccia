package com.focaccia.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppBlockerAccessibilityService : AccessibilityService() {

    private var lastBlockedTime = 0L

    private val ignoredPackages = setOf(
        "com.focaccia.app",
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.android.settings"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName in ignoredPackages) return

        val now = System.currentTimeMillis()
        if (now - lastBlockedTime < 1000) return

        if (!BlockedAppsRepository.isBlockingEnabled(this)) return
        if (packageName !in BlockedAppsRepository.getBlockedApps(this)) return

        lastBlockedTime = now
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
