package com.example.audio

import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class LoudnessEnhancerManager {

    companion object {
        private const val TAG = "LoudnessEnhancerManager"
        const val MAX_GAIN_MB = 2000 // ~20 dB hard safety ceiling
        const val MAX_PERCENT = 300

        fun percentToMillibels(percent: Int): Int {
            if (percent <= 100) return 0
            val clamped = percent.coerceIn(100, MAX_PERCENT)
            val normalized = (clamped - 100).toDouble() / 200.0 // 0.0 to 1.0
            // Perceptually tuned curve so mid-slider (150-200%) feels smooth and balanced
            val curved = normalized.pow(1.3)
            val gainMb = (curved * MAX_GAIN_MB).toInt()
            return gainMb.coerceIn(0, MAX_GAIN_MB)
        }
    }

    // Map of active session ID to LoudnessEnhancer instance
    private val activeEffects = ConcurrentHashMap<Int, LoudnessEnhancer>()

    /**
     * Attaches or updates LoudnessEnhancer on the given audio session ID.
     * Wrapped in try/catch to safely handle DRM/OEM limitations.
     */
    fun applyBoost(sessionId: Int, boostPercent: Int): Boolean {
        if (sessionId < 0) return false
        val gainMb = percentToMillibels(boostPercent)

        return try {
            var enhancer = activeEffects[sessionId]
            if (enhancer == null) {
                enhancer = LoudnessEnhancer(sessionId)
                activeEffects[sessionId] = enhancer
            }

            enhancer.setTargetGain(gainMb)
            enhancer.enabled = gainMb > 0
            Log.d(TAG, "Applied boost $boostPercent% ($gainMb mB) to session $sessionId")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Could not attach or update LoudnessEnhancer for session $sessionId: ${e.message}")
            try {
                activeEffects.remove(sessionId)?.release()
            } catch (_: Throwable) {}
            false
        }
    }

    /**
     * Clamps all active sessions to safe <=100% (0 mB gain).
     * Used on headphone disconnect or emergency safety reset.
     */
    fun clampAllToSafe() {
        for ((sessionId, enhancer) in activeEffects) {
            try {
                enhancer.setTargetGain(0)
                enhancer.enabled = false
                Log.d(TAG, "Clamped session $sessionId to 0 mB")
            } catch (e: Throwable) {
                Log.w(TAG, "Error clamping session $sessionId: ${e.message}")
            }
        }
    }

    /**
     * Releases LoudnessEnhancer for a specific session.
     */
    fun releaseSession(sessionId: Int) {
        val enhancer = activeEffects.remove(sessionId)
        if (enhancer != null) {
            try {
                enhancer.enabled = false
                enhancer.release()
                Log.d(TAG, "Released LoudnessEnhancer for session $sessionId")
            } catch (e: Throwable) {
                Log.w(TAG, "Error releasing session $sessionId: ${e.message}")
            }
        }
    }

    /**
     * Releases all active effect handles to prevent memory leaks.
     */
    fun releaseAll() {
        for ((sessionId, enhancer) in activeEffects) {
            try {
                enhancer.enabled = false
                enhancer.release()
            } catch (e: Throwable) {
                Log.w(TAG, "Error releasing session $sessionId: ${e.message}")
            }
        }
        activeEffects.clear()
        Log.d(TAG, "All LoudnessEnhancer instances released")
    }

    fun getActiveSessionCount(): Int = activeEffects.size
}
