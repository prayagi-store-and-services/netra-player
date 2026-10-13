package com.prayagi.netraplayer

import org.junit.Assert.*
import org.junit.Test

class UsageConsentTest {
    @Test fun freshInstallDoesNotConsent() { assertFalse(UsagePing.hasConsent(false, 0)) }
    @Test fun legacyTrueDoesNotConsent() { assertFalse(UsagePing.hasConsent(true, 0)) }
    @Test fun explicitOptInEnablesCounter() { assertTrue(UsagePing.hasConsent(true, 1)) }
    @Test fun optOutStopsCounter() { assertFalse(UsagePing.hasConsent(false, 1)) }
    @Test fun unknownConsentVersionIsNotAccepted() { assertFalse(UsagePing.hasConsent(true, 2)) }
}
