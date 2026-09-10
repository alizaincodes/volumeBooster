package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VolumeBoostApplication
import com.example.audio.AudioEngineState
import com.example.data.model.AppProfile
import com.example.service.AudioEngineService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isAlreadyBoosted: Boolean = false,
    val existingBoost: Int = 100
)

data class PendingHeadphoneConfirmation(
    val packageName: String,
    val appName: String,
    val targetPercent: Int,
    val isMasterSwitch: Boolean = false
)

class VolumeBoostViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VolumeBoostApplication
    private val appProfileRepo = app.appProfileRepository
    private val settingsRepo = app.settingsRepository
    private val audioEngineManager = app.audioEngineManager
    private val packageManager: PackageManager = application.packageManager

    val profiles: StateFlow<List<AppProfile>> = appProfileRepo.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val engineState: StateFlow<AudioEngineState> = audioEngineManager.engineState

    val masterEnabled: StateFlow<Boolean> = settingsRepo.masterEnabledFlow
    val globalBoostPercent: StateFlow<Int> = settingsRepo.globalBoostPercentFlow
    val themeMode: StateFlow<String> = settingsRepo.themeModeFlow
    val dynamicColor: StateFlow<Boolean> = settingsRepo.dynamicColorFlow
    val hapticsEnabled: StateFlow<Boolean> = settingsRepo.hapticsFlow
    val hasSeenOnboarding: StateFlow<Boolean> = settingsRepo.hasSeenOnboardingFlow
    val resumeOnBoot: StateFlow<Boolean> = settingsRepo.resumeOnBootFlow

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _pendingHeadphoneConfirm = MutableStateFlow<PendingHeadphoneConfirmation?>(null)
    val pendingHeadphoneConfirm: StateFlow<PendingHeadphoneConfirmation?> = _pendingHeadphoneConfirm.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = packageManager.queryIntentActivities(intent, 0)
                val existingProfiles = appProfileRepo.getAllProfiles().associateBy { it.packageName }

                val appsList = resolveInfos.mapNotNull { resolveInfo ->
                    val pkgName = resolveInfo.activityInfo.packageName
                    if (pkgName == getApplication<Application>().packageName) return@mapNotNull null

                    val label = resolveInfo.loadLabel(packageManager).toString()
                    val existing = existingProfiles[pkgName]

                    InstalledAppItem(
                        packageName = pkgName,
                        appName = label,
                        isAlreadyBoosted = existing != null,
                        existingBoost = existing?.boostPercent ?: 100
                    )
                }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }

                _installedApps.value = appsList
            } catch (e: Exception) {
                // Fallback to basic list
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    fun toggleMaster(enabled: Boolean) {
        if (enabled && audioEngineManager.isHeadphonesConnected() && !settingsRepo.headphoneWarningBypassedForSession) {
            _pendingHeadphoneConfirm.value = PendingHeadphoneConfirmation(
                packageName = "global",
                appName = "All Audio",
                targetPercent = 160,
                isMasterSwitch = true
            )
            return
        }

        executeMasterToggle(enabled)
    }

    private fun executeMasterToggle(enabled: Boolean) {
        audioEngineManager.setMasterEnabled(enabled)
        val context = getApplication<Application>()
        if (enabled) {
            AudioEngineService.startService(context)
        } else {
            AudioEngineService.stopService(context)
        }
    }

    fun requestBoostChange(packageName: String, appName: String, targetPercent: Int) {
        val finalTarget = targetPercent.coerceIn(0, 300)
        if (finalTarget > 150 && audioEngineManager.isHeadphonesConnected() && !settingsRepo.headphoneWarningBypassedForSession) {
            _pendingHeadphoneConfirm.value = PendingHeadphoneConfirmation(
                packageName = packageName,
                appName = appName,
                targetPercent = finalTarget,
                isMasterSwitch = false
            )
        } else {
            audioEngineManager.updateGlobalBoost(finalTarget)
        }
    }

    fun setGlobalBoostPercent(targetPercent: Int) {
        val clamped = targetPercent.coerceIn(0, 300)
        audioEngineManager.setGlobalBoostPercent(clamped)
    }

    fun runDiagnosticsCheck() {
        audioEngineManager.runDiagnosticCheck()
    }

    fun confirmHeadphoneSafety() {
        val pending = _pendingHeadphoneConfirm.value ?: return
        settingsRepo.headphoneWarningBypassedForSession = true
        _pendingHeadphoneConfirm.value = null

        if (pending.isMasterSwitch) {
            executeMasterToggle(true)
        } else {
            audioEngineManager.updateGlobalBoost(pending.targetPercent)
        }
    }

    fun dismissHeadphoneSafety() {
        _pendingHeadphoneConfirm.value = null
    }

    fun resetAllBoostsToSafe() {
        audioEngineManager.resetAllBoostsToSafe()
    }

    fun setThemeMode(mode: String) {
        settingsRepo.themeMode = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        settingsRepo.dynamicColor = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) {
        settingsRepo.hapticsEnabled = enabled
    }

    fun setResumeOnBoot(enabled: Boolean) {
        settingsRepo.resumeOnBoot = enabled
    }

    fun completeOnboarding() {
        settingsRepo.hasSeenOnboarding = true
    }

    fun dismissHeadphoneDisconnectedBanner() {
        audioEngineManager.dismissHeadphoneDisconnectedBanner()
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(getApplication<Application>().packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
