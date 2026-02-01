package dev.nutting.focaccia

import android.content.Context

object BlockedAppsRepository {

    private const val PREFS_NAME = "focaccia_prefs"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_BLOCKING_ENABLED = "blocking_enabled"
    private const val KEY_NFC_TAG_ID = "nfc_tag_id"
    private const val KEY_BLOCKING_DISABLED_UNTIL = "blocking_disabled_until"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBlockedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()

    fun setBlockedApps(context: Context, apps: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED_APPS, apps).apply()
    }

    fun isBlockingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCKING_ENABLED, false)

    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCKING_ENABLED, enabled).apply()
    }

    fun getRegisteredTagId(context: Context): String? =
        prefs(context).getString(KEY_NFC_TAG_ID, null)

    fun setRegisteredTagId(context: Context, tagId: String?) {
        prefs(context).edit().putString(KEY_NFC_TAG_ID, tagId).apply()
    }

    fun getBlockingDisabledUntil(context: Context): Long =
        prefs(context).getLong(KEY_BLOCKING_DISABLED_UNTIL, 0L)

    fun setBlockingDisabledUntil(context: Context, until: Long) {
        prefs(context).edit().putLong(KEY_BLOCKING_DISABLED_UNTIL, until).apply()
    }
}
