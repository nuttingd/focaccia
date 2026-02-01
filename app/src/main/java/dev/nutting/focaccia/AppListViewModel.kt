package dev.nutting.focaccia

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import dev.nutting.focaccia.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(
    val apps: List<AppInfo> = emptyList(),
    val blockedApps: Set<String> = emptySet(),
    val blockingEnabled: Boolean = false,
    val registeredTagId: String? = null,
    val blockingDisabledUntil: Long = 0L,
    val isRegistering: Boolean = false,
    val accessibilityEnabled: Boolean = false
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchablePackages = pm.queryIntentActivities(launchIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()

        val excludedPackages = mutableSetOf(context.packageName)

        // Default launcher
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        pm.resolveActivity(homeIntent, 0)?.activityInfo?.packageName?.let {
            excludedPackages.add(it)
        }

        // Default phone/dialer
        val dialIntent = Intent(Intent.ACTION_DIAL)
        pm.resolveActivity(dialIntent, 0)?.activityInfo?.packageName?.let {
            excludedPackages.add(it)
        }

        // Settings
        excludedPackages.add("com.android.settings")

        val apps = pm.getInstalledApplications(0)
            .filter { app ->
                app.packageName in launchablePackages &&
                app.packageName !in excludedPackages
            }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = pm.getApplicationIcon(app)
                )
            }
            .sortedBy { it.label.lowercase() }

        _uiState.value = UiState(
            apps = apps,
            blockedApps = BlockedAppsRepository.getBlockedApps(context),
            blockingEnabled = BlockedAppsRepository.isBlockingEnabled(context),
            registeredTagId = BlockedAppsRepository.getRegisteredTagId(context),
            blockingDisabledUntil = BlockedAppsRepository.getBlockingDisabledUntil(context),
            isRegistering = false,
            accessibilityEnabled = isAccessibilityServiceEnabled(context)
        )
    }

    fun refreshUnlockState() {
        val context = getApplication<Application>()
        _uiState.value = _uiState.value.copy(
            blockingDisabledUntil = BlockedAppsRepository.getBlockingDisabledUntil(context),
            registeredTagId = BlockedAppsRepository.getRegisteredTagId(context),
            accessibilityEnabled = isAccessibilityServiceEnabled(context)
        )
    }

    private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
        val serviceName = "${context.packageName}/${AppBlockerAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
            .any { it.equals(serviceName, ignoreCase = true) }
    }

    fun toggleAppBlocked(packageName: String) {
        val context = getApplication<Application>()
        val current = _uiState.value.blockedApps.toMutableSet()
        if (packageName in current) current.remove(packageName) else current.add(packageName)
        BlockedAppsRepository.setBlockedApps(context, current)
        _uiState.value = _uiState.value.copy(blockedApps = current)
    }

    fun toggleSelectAll() {
        val context = getApplication<Application>()
        val allPackages = _uiState.value.apps.map { it.packageName }.toSet()
        val newBlocked = if (_uiState.value.blockedApps.containsAll(allPackages)) {
            emptySet()
        } else {
            allPackages
        }
        BlockedAppsRepository.setBlockedApps(context, newBlocked)
        _uiState.value = _uiState.value.copy(blockedApps = newBlocked)
    }

    fun setBlockingEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        BlockedAppsRepository.setBlockingEnabled(context, enabled)
        _uiState.value = _uiState.value.copy(blockingEnabled = enabled)
    }

    fun startRegistering() {
        _uiState.value = _uiState.value.copy(isRegistering = true)
    }

    fun stopRegistering() {
        _uiState.value = _uiState.value.copy(isRegistering = false)
    }

    fun registerTag(id: String) {
        val context = getApplication<Application>()
        BlockedAppsRepository.setRegisteredTagId(context, id)
        _uiState.value = _uiState.value.copy(registeredTagId = id, isRegistering = false)
    }

    fun clearTag() {
        val context = getApplication<Application>()
        BlockedAppsRepository.setRegisteredTagId(context, null)
        BlockedAppsRepository.setBlockingDisabledUntil(context, 0L)
        context.stopService(Intent(context, UnlockCountdownService::class.java))
        _uiState.value = _uiState.value.copy(registeredTagId = null, blockingDisabledUntil = 0L)
    }

    fun relockBlocking() {
        val context = getApplication<Application>()
        BlockedAppsRepository.setBlockingDisabledUntil(context, 0L)
        context.stopService(Intent(context, UnlockCountdownService::class.java))
        _uiState.value = _uiState.value.copy(blockingDisabledUntil = 0L)
    }

    fun unlockBlocking() {
        val context = getApplication<Application>()
        val until = System.currentTimeMillis() + BlockedAppsRepository.UNLOCK_DURATION_MS
        BlockedAppsRepository.setBlockingDisabledUntil(context, until)
        context.startService(Intent(context, UnlockCountdownService::class.java))
        _uiState.value = _uiState.value.copy(blockingDisabledUntil = until)
    }
}
