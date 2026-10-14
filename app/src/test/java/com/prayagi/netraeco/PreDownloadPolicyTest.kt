package com.prayagi.netraplayer

import org.junit.Assert.*
import org.junit.Test

class PreDownloadPolicyTest {
    private fun allowed(online: Boolean = true, wifi: Boolean = false, mobile: Boolean = true,
        roaming: Boolean = false, saver: Boolean = false, battery: Int? = 80, charging: Boolean = false,
        saved: Boolean = false, size: Long = PreDownloadPolicy.MOBILE_LIMIT_BYTES) =
        PreDownloadPolicy.allowed(online, wifi, mobile, roaming, saver, battery, charging, saved, size)
    @Test fun wifiAnyPositiveSize() { assertTrue(allowed(wifi=true, size=500L*1024*1024)) }
    @Test fun mobileAtLimitOnly() { assertTrue(allowed()); assertFalse(allowed(size=PreDownloadPolicy.MOBILE_LIMIT_BYTES+1)) }
    @Test fun mobileRoamingBlocked() { assertFalse(allowed(roaming=true)) }
    @Test fun saverBlockedEvenOnWifi() { assertFalse(allowed(wifi=true,saver=true)) }
    @Test fun lowBatteryUnlessCharging() { assertFalse(allowed(battery=19)); assertTrue(allowed(battery=19,charging=true)); assertTrue(allowed(battery=20)) }
    @Test fun unknownBatteryFailsClosed() { assertFalse(allowed(battery=null)) }
    @Test fun offlineSavedAndZeroSizeBlocked() { assertFalse(allowed(online=false)); assertFalse(allowed(saved=true)); assertFalse(allowed(size=0)) }
    @Test fun ethernetIsNotMobileOrWifi() { assertFalse(allowed(mobile=false,wifi=false)) }
}
