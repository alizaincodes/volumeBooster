package com.example.platform

import android.content.Context
import com.example.VolumeBoostApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Native interface channel for AudioEngine communication.
 * Connects platform method calls and state streams.
 */
class AudioEngineChannel(private val context: Context) {

    companion object {
        const val METHOD_CHANNEL_NAME = "com.volumeboost.app/audio_engine"
        const val EVENT_CHANNEL_NAME = "com.volumeboost.app/active_sessions"
    }

    private val app = context.applicationContext as VolumeBoostApplication
    private val engineManager = app.audioEngineManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null

    interface StateStreamListener {
        fun onStateChanged(stateMap: Map<String, Any?>)
        fun onHeadphoneDisconnected(message: String)
    }

    var listener: StateStreamListener? = null
        set(value) {
            field = value
            if (value != null) {
                startObservingState()
            } else {
                stopObservingState()
            }
        }

    fun handleMethodCall(method: String, arguments: Map<String, Any?>?, result: (Any?) -> Unit) {
        when (method) {
            "setMasterBoost" -> {
                val enabled = (arguments?.get("enabled") as? Boolean) ?: false
                engineManager.setMasterEnabled(enabled)
                result(true)
            }
            "setGlobalBoost" -> {
                val boostPercent = (arguments?.get("boostPercent") as? Number)?.toInt() ?: 100
                engineManager.setGlobalBoostPercent(boostPercent)
                result(true)
            }
            "resetAllBoosts" -> {
                engineManager.resetAllBoostsToSafe()
                result(true)
            }
            "isHeadphonesConnected" -> {
                result(engineManager.isHeadphonesConnected())
            }
            "getHeadphoneName" -> {
                result(engineManager.getHeadphoneName())
            }
            "getEngineStatus" -> {
                val state = engineManager.engineState.value
                result(
                    mapOf(
                        "isMasterActive" to state.isMasterActive,
                        "engineStatus" to state.engineStatus,
                        "isHeadphonesConnected" to state.isHeadphonesConnected,
                        "headphoneName" to state.headphoneName,
                        "activeBoostCount" to state.activeBoostCount,
                        "currentBoostPercent" to state.currentBoostPercent,
                        "globalEffectAttached" to state.globalEffectAttached
                    )
                )
            }
            else -> result(null)
        }
    }

    private fun startObservingState() {
        stateObserverJob?.cancel()
        stateObserverJob = scope.launch {
            engineManager.engineState.collectLatest { state ->
                val map = mapOf(
                    "isMasterActive" to state.isMasterActive,
                    "engineStatus" to state.engineStatus,
                    "isHeadphonesConnected" to state.isHeadphonesConnected,
                    "headphoneName" to state.headphoneName,
                    "activeBoostCount" to state.activeBoostCount,
                    "currentBoostPercent" to state.currentBoostPercent,
                    "globalEffectAttached" to state.globalEffectAttached,
                    "activeSessionPackage" to state.activeSession?.packageName,
                    "activeSessionName" to state.activeSession?.appName,
                    "activeSessionBoost" to state.activeSession?.boostPercent
                )
                listener?.onStateChanged(map)

                if (state.headphoneDisconnectedBanner != null) {
                    listener?.onHeadphoneDisconnected(state.headphoneDisconnectedBanner)
                }
            }
        }
    }

    private fun stopObservingState() {
        stateObserverJob?.cancel()
        stateObserverJob = null
    }
}
