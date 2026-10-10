package com.prayagi.netraplayer
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.ByteArrayInputStream
import java.security.MessageDigest
class ApkIntegrityTest {
    private val bytes = "test update bytes".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    @Test fun exactBytesPassAndSameLengthCorruptionFails() {
        val f = File.createTempFile("integrity-test", ".part")
        try {
            ApkIntegrity.copyVerified(ByteArrayInputStream(bytes), f, bytes.size.toLong(), hash) {}
            assertTrue(ApkIntegrity.matches(f, bytes.size.toLong(), hash))
            f.writeBytes(ByteArray(bytes.size))
            assertFalse(ApkIntegrity.matches(f, bytes.size.toLong(), hash))
        } finally { f.delete() }
    }
    @Test fun truncatedOversizedAndWrongHashNeverPass() {
        val f = File.createTempFile("integrity-test", ".part")
        try {
            for ((data, sha) in listOf(bytes.dropLast(1).toByteArray() to hash, (bytes + byteArrayOf(1)) to hash, bytes to "0".repeat(64))) {
                try { ApkIntegrity.copyVerified(ByteArrayInputStream(data), f, bytes.size.toLong(), sha) {}; fail("Bad bytes accepted") }
                catch (_: java.io.IOException) {}
            }
        } finally { f.delete() }
    }
}
