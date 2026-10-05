package com.prayagi.netraplayer

import android.content.Context

/** The last file the user played, kept on this phone only: its address, name, position and the time it was saved. */
data class LastPlayedItem(val uri: String, val name: String, val positionMs: Long, val savedAtMs: Long)

object LastPlayed {
    private const val PREFS = "netra_last_played"

    fun save(context: Context, uri: String, name: String, positionMs: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("uri", uri).putString("name", name)
            .putLong("pos", positionMs.coerceAtLeast(0L)).putLong("at", System.currentTimeMillis()).apply()
    }

    fun load(context: Context): LastPlayedItem? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uri = p.getString("uri", null) ?: return null
        val name = p.getString("name", null) ?: return null
        return LastPlayedItem(uri, name, p.getLong("pos", 0L), p.getLong("at", 0L))
    }

    /** 75000 -> "1:15", 3725000 -> "1:02:05". */
    fun formatPosition(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) / 1000L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(java.util.Locale.US, "%d:%02d", m, s)
    }
}
