package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("volume_boost_settings", Context.MODE_PRIVATE)

    private val _masterEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_MASTER_ENABLED, false))
    val masterEnabledFlow: StateFlow<Boolean> = _masterEnabledFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _dynamicColorFlow = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
    val dynamicColorFlow: StateFlow<Boolean> = _dynamicColorFlow.asStateFlow()

    private val _hapticsFlow = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS_ENABLED, true))
    val hapticsFlow: StateFlow<Boolean> = _hapticsFlow.asStateFlow()

    private val _hasSeenOnboardingFlow = MutableStateFlow(prefs.getBoolean(KEY_SEEN_ONBOARDING, false))
    val hasSeenOnboardingFlow: StateFlow<Boolean> = _hasSeenOnboardingFlow.asStateFlow()

    private val _resumeOnBootFlow = MutableStateFlow(prefs.getBoolean(KEY_RESUME_ON_BOOT, false))
    val resumeOnBootFlow: StateFlow<Boolean> = _resumeOnBootFlow.asStateFlow()

    var isMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MASTER_ENABLED, value).apply()
            _masterEnabledFlow.value = value
        }

    var defaultBoostPercent: Int
        get() = prefs.getInt(KEY_DEFAULT_BOOST, 100)
        set(value) {
            prefs.edit().putInt(KEY_DEFAULT_BOOST, value.coerceIn(0, 300)).apply()
        }

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
            _themeModeFlow.value = value
        }

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
            _dynamicColorFlow.value = value
        }

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()
            _hapticsFlow.value = value
        }

    var hasSeenOnboarding: Boolean
        get() = prefs.getBoolean(KEY_SEEN_ONBOARDING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SEEN_ONBOARDING, value).apply()
            _hasSeenOnboardingFlow.value = value
        }

    var resumeOnBoot: Boolean
        get() = prefs.getBoolean(KEY_RESUME_ON_BOOT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_RESUME_ON_BOOT, value).apply()
            _resumeOnBootFlow.value = value
        }

    var lastHeadphoneWarningTimestamp: Long
        get() = prefs.getLong(KEY_HEADPHONE_WARN_TIME, 0L)
        set(value) {
            prefs.edit().putLong(KEY_HEADPHONE_WARN_TIME, value).apply()
        }

    var headphoneWarningBypassedForSession: Boolean = false

    companion object {
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_DEFAULT_BOOST = "default_boost_percent"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_RESUME_ON_BOOT = "resume_on_boot"
        private const val KEY_HEADPHONE_WARN_TIME = "headphone_warn_time"
    }
}
