package com.example.audio

import android.content.Context
import android.util.Log
import com.example.data.model.AppProfile
import com.example.data.repository.AppProfileRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveSessionInfo(
    val sessionId: Int,
    val packageName: String,
    val appName: String,
    val boostPercent: Int,
    val isBoostActive: Boolean
)

data class AudioEngineState(
    val isMasterActive: Boolean = false,
    val activeSession: ActiveSessionInfo? = null,
    val isHeadphonesConnected: Boolean = false,
    val headphoneName: String? = null,
    val headphoneDisconnectedBanner: String? = null,
    val engineStatus: String = "Idle",
    val activeBoostCount: Int = 0
)

class AudioEngineManager private constructor(
    private val context: Context,
    private val appProfileRepository: AppProfileRepository,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        private const val TAG = "AudioEngineManager"

        @Volatile
        private var instance: AudioEngineManager? = null

        fun getInstance(
            context: Context,
            appProfileRepository: AppProfileRepository,
            settingsRepository: SettingsRepository
        ): AudioEngineManager {
            return instance ?: synchronized(this) {
                instance ?: AudioEngineManager(
                    context.applicationContext,
                    appProfileRepository,
                    settingsRepository
                ).also { instance = it }
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val enhancerManager = LoudnessEnhancerManager()

    private val _engineState = MutableStateFlow(
        AudioEngineState(
            isMasterActive = settingsRepository.isMasterEnabled,
            isHeadphonesConnected = false,
            engineStatus = if (settingsRepository.isMasterEnabled) "Active" else "Idle"
        )
    )
    val engineState: StateFlow<AudioEngineState> = _engineState.asStateFlow()

    private val activeSessionsMap = mutableMapOf<Int, DetectedSession>()

    private val headphoneDetector = HeadphoneDetector(
        context = context,
        onHeadphoneStateChanged = { isConnected, name ->
            _engineState.update {
                it.copy(isHeadphonesConnected = isConnected, headphoneName = name)
            }
        },
        onHeadphonesDisconnected = {
            handleHeadphonesDisconnected()
        }
    )

    private val sessionDetector = AudioSessionDetector(context) { currentSessions ->
        handleDetectedSessionsChanged(currentSessions)
    }

    init {
        headphoneDetector.startListening()
        if (settingsRepository.isMasterEnabled) {
            startEngine()
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        settingsRepository.isMasterEnabled = enabled
        _engineState.update {
            it.copy(
                isMasterActive = enabled,
                engineStatus = if (enabled) "Active" else "Idle"
            )
        }
        if (enabled) {
            startEngine()
        } else {
            stopEngine()
        }
    }

    private fun startEngine() {
        sessionDetector.start()
        _engineState.update {
            it.copy(
                isMasterActive = true,
                engineStatus = "Active (Monitoring Audio)"
            )
        }
        reapplyAllActiveSessions()
    }

    private fun stopEngine() {
        sessionDetector.stop()
        enhancerManager.releaseAll()
        activeSessionsMap.clear()
        _engineState.update {
            it.copy(
                isMasterActive = false,
                activeSession = null,
                engineStatus = "Idle (0% CPU)",
                activeBoostCount = 0
            )
        }
    }

    private fun handleHeadphonesDisconnected() {
        Log.d(TAG, "Headphones disconnected - auto-clamping all active boosts to safe level")
        enhancerManager.clampAllToSafe()
        _engineState.update {
            it.copy(
                headphoneDisconnectedBanner = "Headphones disconnected — boost reset to a safe level for speaker playback.",
                activeSession = it.activeSession?.copy(boostPercent = 100, isBoostActive = false)
            )
        }
    }

    fun dismissHeadphoneDisconnectedBanner() {
        _engineState.update { it.copy(headphoneDisconnectedBanner = null) }
    }

    private fun handleDetectedSessionsChanged(sessions: List<DetectedSession>) {
        if (!_engineState.value.isMasterActive) return

        val currentSessionIds = sessions.map { it.sessionId }.toSet()

        // Release ended sessions
        val endedSessionIds = activeSessionsMap.keys.filter { it !in currentSessionIds }
        for (endedId in endedSessionIds) {
            enhancerManager.releaseSession(endedId)
            activeSessionsMap.remove(endedId)
            Log.d(TAG, "Released ended session: $endedId")
        }

        // Apply boost for active sessions
        coroutineScope.launch(Dispatchers.IO) {
            for (session in sessions) {
                activeSessionsMap[session.sessionId] = session
                applyBoostForSession(session)
            }

            val primarySession = sessions.firstOrNull()
            if (primarySession != null) {
                val profile = appProfileRepository.getProfile(primarySession.packageName)
                val boostPercent = if (profile != null && profile.isEnabled) {
                    profile.boostPercent
                } else {
                    settingsRepository.defaultBoostPercent
                }

                _engineState.update {
                    it.copy(
                        activeSession = ActiveSessionInfo(
                            sessionId = primarySession.sessionId,
                            packageName = primarySession.packageName,
                            appName = primarySession.appName,
                            boostPercent = boostPercent,
                            isBoostActive = boostPercent > 100
                        ),
                        activeBoostCount = enhancerManager.getActiveSessionCount(),
                        engineStatus = "Active (Boosting ${primarySession.appName})"
                    )
                }
            } else {
                _engineState.update {
                    it.copy(
                        activeSession = null,
                        activeBoostCount = 0,
                        engineStatus = "Active (Idle — waiting for playback)"
                    )
                }
            }
        }
    }

    private suspend fun applyBoostForSession(session: DetectedSession) {
        val profile = appProfileRepository.getProfile(session.packageName)
        val boostPercent: Int = if (profile != null) {
            if (profile.isEnabled) profile.boostPercent else 100
        } else {
            settingsRepository.defaultBoostPercent
        }

        // Safety check for headphones
        val effectiveBoost = if (_engineState.value.isHeadphonesConnected && boostPercent > 150) {
            if (settingsRepository.headphoneWarningBypassedForSession) boostPercent else 150
        } else {
            boostPercent
        }

        enhancerManager.applyBoost(session.sessionId, effectiveBoost)
    }

    fun updateBoostForPackage(packageName: String, boostPercent: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            appProfileRepository.updateBoost(packageName, boostPercent)
            // If this package is currently playing, update in real-time!
            val matchingSession = activeSessionsMap.values.firstOrNull { it.packageName == packageName }
            if (matchingSession != null && _engineState.value.isMasterActive) {
                enhancerManager.applyBoost(matchingSession.sessionId, boostPercent)
                _engineState.update {
                    it.copy(
                        activeSession = it.activeSession?.takeIf { s -> s.packageName == packageName }?.copy(
                            boostPercent = boostPercent,
                            isBoostActive = boostPercent > 100
                        ) ?: it.activeSession
                    )
                }
            }
        }
    }

    fun resetAllBoostsToSafe() {
        coroutineScope.launch(Dispatchers.IO) {
            appProfileRepository.resetAllBoosts()
            enhancerManager.clampAllToSafe()
            _engineState.update {
                it.copy(
                    activeSession = it.activeSession?.copy(boostPercent = 100, isBoostActive = false)
                )
            }
        }
    }

    private fun reapplyAllActiveSessions() {
        coroutineScope.launch(Dispatchers.IO) {
            for (session in activeSessionsMap.values) {
                applyBoostForSession(session)
            }
        }
    }

    fun isHeadphonesConnected(): Boolean = headphoneDetector.isHeadphonesConnected()

    fun getHeadphoneName(): String? = headphoneDetector.getHeadphoneDescription()
}
