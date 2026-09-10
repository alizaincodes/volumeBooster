package com.example.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import com.example.data.repository.SettingsRepository
import com.example.logging.AppLog
import com.example.logging.LogCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val GLOBAL_OUTPUT_SESSION_ID = 0

data class ActiveSessionInfo(
    val sessionId: Int = 0,
    val packageName: String = "global",
    val appName: String = "All Audio",
    val boostPercent: Int = 100,
    val isBoostActive: Boolean = false
)

data class AudioEngineState(
    val isMasterActive: Boolean = false,
    val activeSession: ActiveSessionInfo? = null,
    val isHeadphonesConnected: Boolean = false,
    val headphoneName: String? = null,
    val headphoneDisconnectedBanner: String? = null,
    val engineStatus: String = "Idle",
    val activeBoostCount: Int = 0,
    val currentBoostPercent: Int = 100,
    val currentGainMillibels: Int = 0,
    val globalEffectAttached: Boolean = false,
    val unsupportedDeviceBanner: String? = null
)

class AudioEngineManager private constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        @Volatile
        private var instance: AudioEngineManager? = null

        fun getInstance(
            context: Context,
            settingsRepository: SettingsRepository
        ): AudioEngineManager {
            return instance ?: synchronized(this) {
                instance ?: AudioEngineManager(context.applicationContext, settingsRepository).also { instance = it }
            }
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sessionDetector = AudioSessionDetector(context) { sessions -> handlePlaybackDetected(sessions) }
    private val headphoneDetector = HeadphoneDetector(
        context = context,
        onHeadphoneStateChanged = { isConnected, name ->
            _engineState.update { it.copy(isHeadphonesConnected = isConnected, headphoneName = name) }
            if (isConnected) {
                AppLog.i(LogCategory.HEADPHONES, "Headphones connected: ${name ?: "device"}")
            }
        },
        onHeadphonesDisconnected = { handleHeadphonesDisconnected() }
    )
    private var globalEnhancer: LoudnessEnhancer? = null
    private var hasLoggedNoPlayback = false

    private val _engineState = MutableStateFlow(
        AudioEngineState(
            isMasterActive = settingsRepository.isMasterEnabled,
            activeSession = ActiveSessionInfo(
                packageName = "global",
                appName = "All Audio",
                boostPercent = settingsRepository.globalBoostPercent,
                isBoostActive = settingsRepository.isMasterEnabled
            ),
            currentBoostPercent = settingsRepository.globalBoostPercent,
            currentGainMillibels = LoudnessEnhancerManager.percentToMillibels(settingsRepository.globalBoostPercent),
            engineStatus = if (settingsRepository.isMasterEnabled) "Active" else "Idle"
        )
    )
    val engineState: StateFlow<AudioEngineState> = _engineState.asStateFlow()

    init {
        AppLog.i(LogCategory.SYSTEM, "AudioEngineManager initialized")
        headphoneDetector.startListening()
        if (settingsRepository.isMasterEnabled) {
            startEngine()
        }
        logCapabilities()
    }

    fun setMasterEnabled(enabled: Boolean) {
        AppLog.i(
            LogCategory.PERMISSIONS,
            "Boost enable attempt: requested enabled=$enabled, MODIFY_AUDIO_SETTINGS=${hasPermission(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)}, POST_NOTIFICATIONS=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) hasPermission(android.Manifest.permission.POST_NOTIFICATIONS) else "N/A"}, batteryOptExempt=${isIgnoringBatteryOptimizations()}"
        )
        settingsRepository.isMasterEnabled = enabled
        _engineState.update {
            it.copy(
                isMasterActive = enabled,
                engineStatus = if (enabled) "Active" else "Idle",
                activeSession = it.activeSession?.copy(isBoostActive = enabled) ?: ActiveSessionInfo(isBoostActive = enabled),
                activeBoostCount = if (enabled) 1 else 0
            )
        }
        if (enabled) {
            startEngine()
        } else {
            stopEngine()
        }
    }

    fun setGlobalBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 300)
        settingsRepository.globalBoostPercent = clamped
        val gain = LoudnessEnhancerManager.percentToMillibels(clamped)
        _engineState.update {
            it.copy(
                currentBoostPercent = clamped,
                currentGainMillibels = gain,
                activeSession = it.activeSession?.copy(boostPercent = clamped, isBoostActive = clamped > 100) ?: ActiveSessionInfo(boostPercent = clamped, isBoostActive = clamped > 100)
            )
        }
        if (settingsRepository.isMasterEnabled) {
            applyGlobalBoost(clamped)
        }
    }

    fun updateGlobalBoost(percent: Int) = setGlobalBoostPercent(percent)

    fun updateBoostForPackage(packageName: String, boostPercent: Int) = setGlobalBoostPercent(boostPercent)
    fun resetAllBoostsToSafe() = setGlobalBoostPercent(100)

    private fun startEngine() {
        AppLog.i(LogCategory.ENGINE, "AudioEngineService starting")
        _engineState.update {
            it.copy(isMasterActive = true, engineStatus = "Active (Global Boost)")
        }
        sessionDetector.start()
        applyGlobalBoost(settingsRepository.globalBoostPercent)
    }

    private fun stopEngine() {
        sessionDetector.stop()
        releaseGlobalEnhancer()
        _engineState.update {
            it.copy(
                isMasterActive = false,
                activeSession = it.activeSession?.copy(isBoostActive = false) ?: ActiveSessionInfo(isBoostActive = false),
                engineStatus = "Idle",
                activeBoostCount = 0,
                globalEffectAttached = false,
                currentGainMillibels = 0,
                currentBoostPercent = 100
            )
        }
        AppLog.i(LogCategory.ENGINE, "LoudnessEnhancer released, engine stopped")
    }

    private fun handlePlaybackDetected(sessions: List<DetectedSession>) {
        if (sessions.isEmpty()) {
            if (!hasLoggedNoPlayback) {
                AppLog.i(LogCategory.PLAYBACK_DETECTION, "No active audio playback")
                hasLoggedNoPlayback = true
            }
            return
        }
        hasLoggedNoPlayback = false
        val active = sessions.firstOrNull { it.isActive } ?: sessions.first()
        AppLog.i(LogCategory.PLAYBACK_DETECTION, "Playback detected: ${active.appName} (${active.packageName}) (session ${active.sessionId})")
    }

    private fun handleHeadphonesDisconnected() {
        AppLog.i(LogCategory.HEADPHONES, "Headphones disconnected — boost reset to safe level")
        val safePercent = 100
        settingsRepository.globalBoostPercent = safePercent
        _engineState.update {
            it.copy(
                headphoneDisconnectedBanner = "Headphones disconnected — boost reset to a safe level for speaker playback.",
                currentBoostPercent = safePercent,
                currentGainMillibels = LoudnessEnhancerManager.percentToMillibels(safePercent),
                activeSession = it.activeSession?.copy(boostPercent = safePercent, isBoostActive = false) ?: ActiveSessionInfo(boostPercent = safePercent, isBoostActive = false)
            )
        }
        if (settingsRepository.isMasterEnabled) {
            applyGlobalBoost(safePercent)
        }
    }

    fun dismissHeadphoneDisconnectedBanner() {
        _engineState.update { it.copy(headphoneDisconnectedBanner = null) }
    }

    fun applyGlobalBoost(boostPercent: Int) {
        val gainMb = LoudnessEnhancerManager.percentToMillibels(boostPercent)
        AppLog.i(LogCategory.ENGINE, "Setting target gain to $gainMb mB (${boostPercent}%)")
        try {
            val enhancer = getOrCreateGlobalEnhancer()
            enhancer.setTargetGain(gainMb)
            enhancer.enabled = boostPercent > 100
            val actualEnabled = enhancer.enabled
            AppLog.i(LogCategory.ENGINE, "Requested enabled=${boostPercent > 100}, effect reports enabled=$actualEnabled")
            _engineState.update {
                it.copy(
                    currentBoostPercent = boostPercent,
                    currentGainMillibels = gainMb,
                    activeSession = it.activeSession?.copy(boostPercent = boostPercent, isBoostActive = boostPercent > 100) ?: ActiveSessionInfo(boostPercent = boostPercent, isBoostActive = boostPercent > 100),
                    globalEffectAttached = true,
                    engineStatus = if (boostPercent > 100) "Active (Global Boost)" else "Active (Normal)"
                )
            }
        } catch (e: Throwable) {
            AppLog.e(LogCategory.ENGINE, "Failed to attach LoudnessEnhancer: ${e.message}. This device or Android build may not support session-0 global effects.")
            _engineState.update {
                it.copy(
                    globalEffectAttached = false,
                    unsupportedDeviceBanner = "⚠️ Volume boost isn't supported on this device. See Diagnostics for details.",
                    engineStatus = "Unsupported device"
                )
            }
        }
    }

    private fun getOrCreateGlobalEnhancer(): LoudnessEnhancer {
        val existing = globalEnhancer
        if (existing != null) return existing

        AppLog.i(LogCategory.ENGINE, "Attaching LoudnessEnhancer to global output mix (session 0)")
        return try {
            val enhancer = LoudnessEnhancer(GLOBAL_OUTPUT_SESSION_ID)
            globalEnhancer = enhancer
            AppLog.success(LogCategory.ENGINE, "LoudnessEnhancer attached successfully.")
            enhancer
        } catch (e: Throwable) {
            AppLog.e(LogCategory.ENGINE, "Failed to attach LoudnessEnhancer: ${e.message}. This device or Android build may not support session-0 global effects.")
            throw e
        }
    }

    fun releaseGlobalEnhancer() {
        val enhancer = globalEnhancer
        if (enhancer != null) {
            try {
                enhancer.enabled = false
                enhancer.release()
                AppLog.i(LogCategory.ENGINE, "LoudnessEnhancer released, engine stopped")
            } catch (e: Throwable) {
                AppLog.e(LogCategory.ENGINE, "Failed to release LoudnessEnhancer: ${e.message}")
            } finally {
                globalEnhancer = null
            }
        }
    }

    fun isHeadphonesConnected(): Boolean = headphoneDetector.isHeadphonesConnected()
    fun getHeadphoneName(): String? = headphoneDetector.getHeadphoneDescription()

    fun runDiagnosticCheck() {
        logCapabilities()
        val permissionStatus = buildString {
            append("MODIFY_AUDIO_SETTINGS granted? ${hasPermission(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)}; ")
            append("Notification permission granted? ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) hasPermission(android.Manifest.permission.POST_NOTIFICATIONS) else "N/A"}; ")
            append("Battery optimization exempt? ${isIgnoringBatteryOptimizations()}; ")
            append("Foreground service started? ${settingsRepository.isMasterEnabled}")
        }
        AppLog.i(LogCategory.PERMISSIONS, permissionStatus)
    }

    private fun logCapabilities() {
        val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        AppLog.i(LogCategory.SYSTEM, "Output sample rate: ${sampleRate ?: "unknown"}")
        val effectDescriptors = AudioEffect.queryEffects().map { descriptor ->
            descriptor.name + " (" + descriptor.type + ")"
        }
        AppLog.i(LogCategory.SYSTEM, "Available audio effects: ${effectDescriptors.ifEmpty { "none" }}")
        val loudnessPresent = effectDescriptors.any { it.contains("Loudness Enhancer", ignoreCase = true) }
        if (!loudnessPresent) {
            AppLog.e(LogCategory.SYSTEM, "This device does not report a Loudness Enhancer audio effect — volume boosting is not supported on this hardware/ROM")
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
