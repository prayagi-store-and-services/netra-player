package com.prayagi.netraplayer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object Net {
    const val CATALOG_URL = "https://prayagi-store-and-services.github.io/netra-eco/projects.json"

    private fun open(url: String, fresh: Boolean = false): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        if (fresh) { c.useCaches = false; c.setRequestProperty("Cache-Control", "no-cache") }
        c.connectTimeout = 10000
        c.readTimeout = 20000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "netra-player")
        return c
    }

    /** Small text download (catalog or latest.json). Returns null when offline or the server says no. */
    fun fetchText(url: String, fresh: Boolean = false): String? = try {
        val c = open(url, fresh)
        if (c.responseCode != 200) null else {
            val bytes = c.inputStream.use { readBounded(it, 1024 * 1024) }
            if (bytes.size > 1024 * 1024) null else String(bytes, Charsets.UTF_8)
        }
    } catch (e: Exception) {
        null
    }

    internal fun readBounded(input: java.io.InputStream, limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer, 0, minOf(buffer.size, limit - out.size() + 1))
            if (n < 0) return out.toByteArray()
            if (out.size() + n > limit) throw java.io.IOException("Response exceeds limit")
            out.write(buffer, 0, n)
        }
    }

    /** "sha256:abc..." from the GitHub API to a plain lowercase hash, or null when it is not a SHA-256. */
    fun shaFromDigest(digest: String): String? =
        digest.removePrefix("sha256:").lowercase().takeIf { digest.startsWith("sha256:") && isValidSha256(it) }

    fun parseLatest(json: String, repo: String): LatestRelease? = try {
        val o = JSONObject(json)
        val tag = o.getString("tag")
        val sha = o.getString("sha256").lowercase()
        val size = o.getLong("size")
        val code = o.getLong("versionCode")
        if (!isValidTag(tag) || !isValidSha256(sha) || size <= 0L || code <= 0L) null else LatestRelease(
            tag = tag,
            versionName = o.optString("versionName", tag.removePrefix("v")),
            versionCode = code,
            notes = o.optString("notes", "").trim(),
            apkUrl = "https://github.com/$repo/releases/download/$tag/app-release.apk",
            sha256 = sha,
            size = size
        )
    } catch (e: Exception) {
        null
    }

    /** Installed version code and name, or null when the app is not installed. */
    fun installed(context: Context, packageName: String): Pair<Long, String>? = try {
        val info = context.packageManager.getPackageInfo(packageName, 0)
        Pair(PackageInfoCompat.getLongVersionCode(info), info.versionName ?: "")
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun cleanDir(dir:File?):Int { var n=0;dir?.listFiles()?.forEach{if(it.delete())n++};return n }
}
