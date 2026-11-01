package com.prayagi.netraplayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadphoneEffectTest {

    @Test
    fun `headphone output types are detected`() {
        // Literal values match android.media.AudioDeviceInfo constants:
        // 3 = WIRED_HEADSET, 4 = WIRED_HEADPHONES, 7 = BLUETOOTH_SCO,
        // 8 = BLUETOOTH_A2DP, 22 = USB_HEADSET, 26 = BLE_HEADSET
        listOf(3, 4, 7, 8, 22, 26).forEach { type ->
            assertTrue("type $type should count as headphones", HeadphoneEffect.isHeadphoneDevice(type))
        }
    }

    @Test
    fun `speakers and other outputs are not headphones`() {
        // 1 = BUILTIN_EARPIECE (phone earpiece, not a headphone output for music),
        // 2 = BUILTIN_SPEAKER, 11 = USB_DEVICE, 27 = BLE_SPEAKER, 9 = HDMI
        listOf(1, 2, 11, 27, 9).forEach { type ->
            assertFalse("type $type must not count as headphones", HeadphoneEffect.isHeadphoneDevice(type))
        }
    }
}
