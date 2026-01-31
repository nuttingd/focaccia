package com.focaccia.app

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import com.focaccia.app.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(
    val apps: List<AppInfo> = emptyList(),
    val blockedApps: Set<String> = emptySet(),
    val blockingEnabled: Boolean = false
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

        val apps = pm.getInstalledApplications(0)
            .filter { app ->
                app.packageName in launchablePackages &&
                app.packageName != context.packageName &&
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
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
            blockingEnabled = BlockedAppsRepository.isBlockingEnabled(context)
        )
    }

    fun toggleAppBlocked(packageName: String) {
        val context = getApplication<Application>()
        val current = _uiState.value.blockedApps.toMutableSet()
        if (packageName in current) current.remove(packageName) else current.add(packageName)
        BlockedAppsRepository.setBlockedApps(context, current)
        _uiState.value = _uiState.value.copy(blockedApps = current)
    }

    fun setBlockingEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        BlockedAppsRepository.setBlockingEnabled(context, enabled)
        _uiState.value = _uiState.value.copy(blockingEnabled = enabled)
    }
}
