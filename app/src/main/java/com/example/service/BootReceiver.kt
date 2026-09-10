package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.VolumeBoostApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            val app = context.applicationContext as? VolumeBoostApplication
            val resumeOnBoot = app?.settingsRepository?.resumeOnBoot == true
            val masterEnabled = app?.settingsRepository?.isMasterEnabled == true

            if (resumeOnBoot && masterEnabled) {
                AudioEngineService.startService(context)
                app.audioEngineManager.setMasterEnabled(true)
            }
        }
    }
}
