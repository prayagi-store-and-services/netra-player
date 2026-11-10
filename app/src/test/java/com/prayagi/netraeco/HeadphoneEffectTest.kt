package com.prayagi.netraplayer

import android.media.AudioDeviceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadphoneEffectTest {
    @Test fun headphoneDeviceTypesRecognised() {
        assertTrue(HeadphoneEffect.isHeadphoneDevice(AudioDeviceInfo.TYPE_WIRED_HEADSET))
        assertTrue(HeadphoneEffect.isHeadphoneDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
        assertTrue(HeadphoneEffect.isHeadphoneDevice(AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
        assertTrue(HeadphoneEffect.isHeadphoneDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertTrue(HeadphoneEffect.isHeadphoneDevice(AudioDeviceInfo.TYPE_USB_HEADSET))
        assertTrue(HeadphoneEffect.isHeadphoneDevice(26)) // TYPE_BLE_HEADSET, API 31 literal
    }

    @Test fun speakersAndUnknownAreNotHeadphones() {
        assertFalse(HeadphoneEffect.isHeadphoneDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertFalse(HeadphoneEffect.isHeadphoneDevice(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE))
        assertFalse(HeadphoneEffect.isHeadphoneDevice(0))
    }

    @Test fun effectRecreatedOnlyForNewOrMissingSession() {
        assertTrue(HeadphoneEffect.shouldRecreate(hasEffect = false, attachedId = 0, newId = 7))
        assertTrue(HeadphoneEffect.shouldRecreate(hasEffect = true, attachedId = 3, newId = 7))
        assertFalse(HeadphoneEffect.shouldRecreate(hasEffect = true, attachedId = 7, newId = 7))
    }
}
