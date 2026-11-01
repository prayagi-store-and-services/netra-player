package com.prayagi.netraplayer

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Headphone 3D ("stadium") sound effect.
 *
 * The effect is a Virtualizer on the player's audio session. It is offered
 * only while headphones are connected, and only when the device actually
 * supports the effect - otherwise the UI says so honestly instead of
 * pretending the sound changed. Same-process singleton: PlaybackService
 * attaches it to the audio session, the player screen toggles it.
 */
object HeadphoneEffect {

    data class State(
        val headphonesConnected: Boolean = false,
        val effectSupported: Boolean = false,
        val effectOn: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private var virtualizer: Virtualizer? = null
    private var audioManager: AudioManager? = null
    private var deviceCallback: AudioDeviceCallback? = null
    private var wantedOn: Boolean = false

    /** Which output device types count as headphones for this feature. */
    fun isHeadphoneDevice(type: Int): Boolean = when (type) {
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, // 4
        AudioDeviceInfo.TYPE_WIRED_HEADSET, // 3
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, // 8
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO, // 7
        AudioDeviceInfo.TYPE_USB_HEADSET, // 22
        26, // TYPE_BLE_HEADSET (API 31; literal keeps minSdk simple)
        -> true
        else -> false
    }

    @Synchronized
    fun attach(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0) return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = am
        if (virtualizer == null) {
            virtualizer = try {
                Virtualizer(0, audioSessionId).also { v ->
                    if (v.strengthSupported) {
                        v.setStrength(1000.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w("HeadphoneEffect", "3D stadium sound is not available on this device", e)
                null
            }
        }
        if (deviceCallback == null) {
            val cb = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh(am)
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh(am)
            }
            am.registerAudioDeviceCallback(cb, null)
            deviceCallback = cb
        }
        refresh(am)
    }

    @Synchronized
    private fun refresh(am: AudioManager) {
        val connected = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { isHeadphoneDevice(it.type) }
        val supported = virtualizer != null
        val on = wantedOn && connected && supported
        try {
            virtualizer?.enabled = on
        } catch (e: Exception) {
            Log.w("HeadphoneEffect", "Could not switch the effect", e)
        }
        _state.value = State(
            headphonesConnected = connected,
            effectSupported = supported,
            effectOn = on && virtualizer?.enabled == true,
        )
    }

    @Synchronized
    fun setEnabled(on: Boolean) {
        wantedOn = on
        audioManager?.let { refresh(it) }
    }

    @Synchronized
    fun detach() {
        deviceCallback?.let { audioManager?.unregisterAudioDeviceCallback(it) }
        deviceCallback = null
        try {
            virtualizer?.release()
        } catch (_: Exception) { }
        virtualizer = null
        audioManager = null
        wantedOn = false
        _state.value = State()
    }
}
