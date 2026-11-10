package com.prayagi.netraplayer

import android.content.Context

/**
 * Per-item "resume where you stopped" memory, kept on this phone only.
 * Remembers a position per file (not just the last one), the most recently
 * played files, and survives app restarts. Nothing leaves the device.
 */
data class ResumeEntry(val uri: String, val name: String, val positionMs: Long, val savedAtMs: Long)

object ResumeStore {
    private const val PREFS = "netra_resume"
    private const val INDEX_KEY = "idx"
    private const val MIGRATED_KEY = "migrated_v1"
    const val MAX_ENTRIES = 50
    /** Positions below this are treated as "at the start" and not offered as resume points. */
    const val MIN_RESUME_MS = 10_000L

    // ---------- pure, unit-testable core ----------

    fun keyFor(uri: String): String = "e_" + Integer.toHexString(uri.hashCode()) + "_" + uri.length

    /** Encodes one entry as "pos|at|name|uri" with '\' and '|' escaped in the text fields. */
    fun encodeEntry(positionMs: Long, savedAtMs: Long, name: String, uri: String): String =
        positionMs.coerceAtLeast(0L).toString() + "|" + savedAtMs.coerceAtLeast(0L).toString() + "|" + escape(name) + "|" + escape(uri)

    /** Decodes a value written by [encodeEntry]; null when the shape is wrong. */
    fun decodeEntry(raw: String?): ResumeEntry? {
        if (raw == null) return null
        val parts = splitEscaped(raw)
        if (parts.size != 4) return null
        val pos = parts[0].toLongOrNull() ?: return null
        val at = parts[1].toLongOrNull() ?: return null
        return ResumeEntry(uri = unescape(parts[3]), name = unescape(parts[2]), positionMs = pos.coerceAtLeast(0L), savedAtMs = at.coerceAtLeast(0L))
    }

    /** New index with [key] first, duplicates removed, capped at [MAX_ENTRIES]. */
    fun indexWithFirst(index: List<String>, key: String): List<String> =
        (listOf(key) + index.filter { it != key }).take(MAX_ENTRIES)

    /** Keys present in [oldIndex] but dropped from [newIndex] (to be deleted). */
    fun evictedKeys(oldIndex: List<String>, newIndex: List<String>): List<String> = oldIndex.filter { it !in newIndex }

    private fun escape(s: String): String {
        val out = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                '\\' -> out.append("\\\\")
                '|' -> out.append("\\|")
                else -> out.append(c)
            }
        }
        return out.toString()
    }

    private fun splitEscaped(raw: String): List<String> {
        val parts = mutableListOf<String>()
        val cur = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                cur.append(c).append(raw[i + 1])
                i += 2
            } else if (c == '|') {
                parts.add(cur.toString()); cur.setLength(0); i++
            } else { cur.append(c); i++ }
        }
        parts.add(cur.toString())
        return parts
    }

    private fun unescape(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) { out.append(s[i + 1]); i += 2 }
            else { out.append(c); i++ }
        }
        return out.toString()
    }

    // ---------- storage ----------

    private fun migrate(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getBoolean(MIGRATED_KEY, false)) return
        LastPlayed.load(context)?.let { legacy ->
            save(context, legacy.uri, legacy.name, legacy.positionMs)
        }
        context.getSharedPreferences("netra_last_played", Context.MODE_PRIVATE).edit().clear().apply()
        p.edit().putBoolean(MIGRATED_KEY, true).apply()
    }

    private fun readIndex(p: android.content.SharedPreferences): List<String> =
        p.getString(INDEX_KEY, "")?.split(',')?.filter { it.isNotEmpty() } ?: emptyList()

    /** Remembers [positionMs] for [uri] and moves it to the front of the recent list. */
    fun save(context: Context, uri: String, name: String, positionMs: Long) {
        migrate(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = keyFor(uri)
        val old = readIndex(p)
        val next = indexWithFirst(old, key)
        val e = p.edit()
        e.putString(INDEX_KEY, next.joinToString(","))
        e.putString(key, encodeEntry(positionMs, System.currentTimeMillis(), name, uri))
        evictedKeys(old, next).forEach { e.remove(it) }
        e.apply()
    }

    /** Saved position for [uri], or null when unknown/corrupt. */
    fun positionFor(context: Context, uri: String): Long? {
        migrate(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return decodeEntry(p.getString(keyFor(uri), null))?.positionMs
    }

    /** Most recently played entries first, up to [limit]. */
    fun recent(context: Context, limit: Int): List<ResumeEntry> {
        migrate(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return readIndex(p).mapNotNull { decodeEntry(p.getString(it, null)) }.take(limit)
    }

    /** Forgets a file (used when its stored URI can no longer be opened). */
    fun remove(context: Context, uri: String) {
        migrate(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = keyFor(uri)
        val next = readIndex(p).filter { it != key }
        p.edit().putString(INDEX_KEY, next.joinToString(",")).remove(key).apply()
    }
}
