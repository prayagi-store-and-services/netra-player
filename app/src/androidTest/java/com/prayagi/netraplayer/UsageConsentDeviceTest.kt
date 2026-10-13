package com.prayagi.netraplayer

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class UsageConsentDeviceTest {
    @Test fun storedLegacyTrueRequiresFreshOptIn() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = c.getSharedPreferences("netra_usage_count", android.content.Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            assertFalse(UsagePing.isEnabled(c))
            // Disabled path must return before any connection. It must not record a send.
            UsagePing.pingIfDue(c)
            assertFalse(prefs.contains("last_day"))
            assertFalse(prefs.contains("last_month"))
            prefs.edit().putBoolean("enabled", true).commit()
            assertFalse(UsagePing.isEnabled(c))
            UsagePing.pingIfDue(c)
            assertFalse(prefs.contains("last_day"))
            UsagePing.setEnabled(c, true)
            assertTrue(UsagePing.isEnabled(c))
            assertEquals(1, prefs.getInt("consent_version", 0))
            UsagePing.setEnabled(c, false)
            assertFalse(UsagePing.isEnabled(c))
        } finally { prefs.edit().clear().commit() }
    }
}
