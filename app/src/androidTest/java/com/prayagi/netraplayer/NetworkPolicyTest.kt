package com.prayagi.netraplayer

import android.security.NetworkSecurityPolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class NetworkPolicyTest {
    @Test fun explicitPolicyDeniesCleartextAndTrustsOnlySystemCertificates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val policy = NetworkSecurityPolicy.getInstance()
        assertFalse("App-wide cleartext denied", policy.isCleartextTrafficPermitted)
        for (host in listOf("github.com", "firestore.googleapis.com", "formsubmit.co", "example.com")) {
            assertFalse("Cleartext denied for $host", policy.isCleartextTrafficPermitted(host))
        }
        val parser = context.resources.getXml(R.xml.network_security_config)
        val anchors = mutableListOf<String>()
        var base = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "base-config" -> {
                        base = true
                        assertEquals("false", parser.getAttributeValue(null, "cleartextTrafficPermitted"))
                    }
                    "certificates" -> anchors.add(parser.getAttributeValue(null, "src"))
                    "debug-overrides", "domain-config" -> fail("No trust or domain exceptions allowed")
                }
            }
            parser.next()
        }
        parser.close()
        assertTrue("Explicit base policy present", base)
        assertEquals(listOf("system"), anchors)
    }
}
