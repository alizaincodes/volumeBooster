package com.example.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.util.Log

data class DetectedSession(
    val sessionId: Int,
    val clientUid: Int,
    val packageName: String,
    val appName: String,
    val isActive: Boolean
)

class AudioSessionDetector(
    private val context: Context,
    private val onSessionsChanged: (List<DetectedSession>) -> Unit
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val packageManager: PackageManager = context.packageManager
    private var isRegistered = false

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
            super.onPlaybackConfigChanged(configs)
            processConfigs(configs ?: emptyList())
        }
    }

    fun start() {
        if (isRegistered) return
        try {
            audioManager.registerAudioPlaybackCallback(playbackCallback, null)
            isRegistered = true
            // Process initial state
            val currentConfigs = audioManager.activePlaybackConfigurations
            processConfigs(currentConfigs)
            Log.d("AudioSessionDetector", "AudioPlaybackCallback registered successfully")
        } catch (e: Exception) {
            Log.e("AudioSessionDetector", "Failed to register AudioPlaybackCallback: ${e.message}")
        }
    }

    fun stop() {
        if (!isRegistered) return
        try {
            audioManager.unregisterAudioPlaybackCallback(playbackCallback)
            isRegistered = false
            Log.d("AudioSessionDetector", "AudioPlaybackCallback unregistered")
        } catch (e: Exception) {
            Log.e("AudioSessionDetector", "Failed to unregister AudioPlaybackCallback: ${e.message}")
        }
    }

    private fun processConfigs(configs: List<AudioPlaybackConfiguration>) {
        val detected = mutableListOf<DetectedSession>()

        for (config in configs) {
            val sessionId = extractSessionId(config)
            val clientUid = extractClientUid(config)
            val isActive = extractIsActive(config)

            if (sessionId < 0) continue

            val pkgName = resolvePackageName(clientUid)
            val appName = resolveAppName(pkgName)

            detected.add(
                DetectedSession(
                    sessionId = sessionId,
                    clientUid = clientUid,
                    packageName = pkgName,
                    appName = appName,
                    isActive = isActive
                )
            )
        }

        onSessionsChanged(detected)
    }

    private fun extractSessionId(config: AudioPlaybackConfiguration): Int {
        return try {
            val methods = config.javaClass.methods
            val method = methods.firstOrNull {
                it.name.equals("getAudioSessionId", ignoreCase = true) ||
                it.name.equals("getClientAudioSessionId", ignoreCase = true)
            }
            (method?.invoke(config) as? Number)?.toInt() ?: 0
        } catch (e: Throwable) {
            0
        }
    }

    private fun extractClientUid(config: AudioPlaybackConfiguration): Int {
        return try {
            val methods = config.javaClass.methods
            val method = methods.firstOrNull {
                it.name.equals("getClientUid", ignoreCase = true)
            }
            (method?.invoke(config) as? Number)?.toInt() ?: -1
        } catch (e: Throwable) {
            -1
        }
    }

    private fun extractIsActive(config: AudioPlaybackConfiguration): Boolean {
        return try {
            val methods = config.javaClass.methods
            val method = methods.firstOrNull {
                it.name.equals("isActive", ignoreCase = true)
            }
            (method?.invoke(config) as? Boolean) ?: true
        } catch (e: Throwable) {
            true
        }
    }

    private fun resolvePackageName(uid: Int): String {
        if (uid < 0) return context.packageName
        val packages = try {
            packageManager.getPackagesForUid(uid)
        } catch (e: Exception) {
            null
        }
        return packages?.firstOrNull() ?: "unknown.audio.app"
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
