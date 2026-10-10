package com.prayagi.netraplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class UpdateIntegrityTest {
    @Test fun restoreRequiresHashAndDropsSameLengthCorruptionAndLegacyRecords() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val prefs = context.getSharedPreferences("netra_eco_downloads", android.content.Context.MODE_PRIVATE)
        val data = "device update integrity fixture".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
        val key = "integrity-fixture-v1.apk"
        val legacy = "integrity-fixture-legacy.apk"
        val f = File(dir, key)
        val old = File(dir, legacy)
        try {
            f.writeBytes(data)
            prefs.edit().putString(key, "${context.packageName}|fixture|999|${data.size}|Fixture|integrity-fixture|$hash").commit()
            DownloadCenter.items.value = emptyMap()
            DownloadCenter.refresh(context)
            assertEquals(DownloadCenter.DONE, DownloadCenter.items.value[key]?.state)
            assertTrue(f.exists())
            // A process restart has no in-memory DONE record. Same length is not enough.
            f.writeBytes(ByteArray(data.size))
            DownloadCenter.items.value = emptyMap()
            DownloadCenter.refresh(context)
            assertFalse(f.exists())
            assertNull(DownloadCenter.items.value[key])
            old.writeBytes(data)
            prefs.edit().putString(legacy, "${context.packageName}|fixture|999|${data.size}|Fixture|integrity-fixture").commit()
            DownloadCenter.refresh(context)
            assertFalse(old.exists())
            assertNull(DownloadCenter.items.value[legacy])
            assertFalse(prefs.contains(key))
            assertFalse(prefs.contains(legacy))
        } finally {
            f.delete(); old.delete()
            prefs.edit().remove(key).remove(legacy).commit()
            DownloadCenter.items.value = emptyMap()
        }
    }
}
