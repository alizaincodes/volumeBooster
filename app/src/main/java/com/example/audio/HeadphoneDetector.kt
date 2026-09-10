package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

class HeadphoneDetector(
    private val context: Context,
    private val onHeadphoneStateChanged: (isConnected: Boolean, deviceName: String?) -> Unit,
    private val onHeadphonesDisconnected: () -> Unit
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isRegistered = false

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            checkCurrentHeadphoneState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val hadHeadphones = removedDevices?.any { isHeadphoneDevice(it) } == true
            if (hadHeadphones) {
                Log.d("HeadphoneDetector", "Headphones device removed!")
                onHeadphonesDisconnected()
            }
            checkCurrentHeadphoneState()
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.d("HeadphoneDetector", "Audio becoming noisy - headphones unplugged!")
                onHeadphonesDisconnected()
                checkCurrentHeadphoneState()
            }
        }
    }

    fun startListening() {
        if (isRegistered) return
        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(noisyReceiver, filter)
            isRegistered = true
            checkCurrentHeadphoneState()
        } catch (e: Exception) {
            Log.e("HeadphoneDetector", "Error starting headphone listener: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isRegistered) return
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            context.unregisterReceiver(noisyReceiver)
            isRegistered = false
        } catch (e: Exception) {
            Log.e("HeadphoneDetector", "Error stopping headphone listener: ${e.message}")
        }
    }

    fun isHeadphonesConnected(): Boolean {
        return getConnectedHeadphoneDevice() != null
    }

    fun getConnectedHeadphoneDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.firstOrNull { isHeadphoneDevice(it) }
    }

    fun getHeadphoneDescription(): String? {
        val device = getConnectedHeadphoneDevice() ?: return null
        return when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && device.productName.isNotEmpty()) {
                    device.productName.toString()
                } else "Bluetooth Headphones"
            }
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            else -> "Headphones"
        }
    }

    private fun checkCurrentHeadphoneState() {
        val device = getConnectedHeadphoneDevice()
        val isConnected = device != null
        val name = if (isConnected) getHeadphoneDescription() else null
        onHeadphoneStateChanged(isConnected, name)
    }

    private fun isHeadphoneDevice(device: AudioDeviceInfo): Boolean {
        return when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET -> true
            else -> false
        }
    }
}
