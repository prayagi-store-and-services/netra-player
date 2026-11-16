package com.prayagi.netraplayer

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UsageConsentTest {
    @Test fun freshInstallAndLegacyImplicitEnableDoNotGrantConsent() {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("netra_usage_count", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        assertFalse(UsagePing.isEnabled(context))
        prefs.edit().putBoolean("enabled", true).commit()
        assertFalse("Legacy default-on key is not explicit opt-in", UsagePing.isEnabled(context))
        UsagePing.setEnabled(context, true)
        assertTrue(UsagePing.isEnabled(context))
        UsagePing.setEnabled(context, false)
        assertFalse(UsagePing.isEnabled(context))
    }

    @Test fun disabledCounterReturnsBeforeNetworkOrStateMutation() {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("netra_usage_count", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        UsagePing.pingIfDue(context)
        assertTrue("No upload attempt markers without consent", prefs.all.isEmpty())
    }
}
