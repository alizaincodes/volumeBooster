package com.example

import android.app.Application
import com.example.audio.AudioEngineManager
import com.example.data.db.AppDatabase
import com.example.data.model.AppProfile
import com.example.data.repository.AppProfileRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VolumeBoostApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var appProfileRepository: AppProfileRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var audioEngineManager: AudioEngineManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        appProfileRepository = AppProfileRepository(database.appProfileDao())
        settingsRepository = SettingsRepository(this)
        audioEngineManager = AudioEngineManager.getInstance(
            this,
            settingsRepository
        )

        preseedPopularProfilesIfNeeded()
    }

    private fun preseedPopularProfilesIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = appProfileRepository.getAllProfiles()
            if (existing.isEmpty()) {
                val seedProfiles = listOf(
                    AppProfile(
                        packageName = "com.spotify.music",
                        appName = "Spotify",
                        boostPercent = 140,
                        isEnabled = true
                    ),
                    AppProfile(
                        packageName = "com.google.android.youtube",
                        appName = "YouTube",
                        boostPercent = 130,
                        isEnabled = true
                    ),
                    AppProfile(
                        packageName = "com.google.android.apps.youtube.music",
                        appName = "YouTube Music",
                        boostPercent = 150,
                        isEnabled = true
                    ),
                    AppProfile(
                        packageName = "com.netflix.mediaclient",
                        appName = "Netflix",
                        boostPercent = 120,
                        isEnabled = true
                    ),
                    AppProfile(
                        packageName = "com.soundcloud.android",
                        appName = "SoundCloud",
                        boostPercent = 160,
                        isEnabled = true
                    )
                )
                for (profile in seedProfiles) {
                    appProfileRepository.saveProfile(profile)
                }
            }
        }
    }
}
