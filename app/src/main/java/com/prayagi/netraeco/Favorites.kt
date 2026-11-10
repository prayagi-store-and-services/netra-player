package com.prayagi.netraplayer

import android.content.Context

/** Files the user starred, kept on this phone only. */
object Favorites {
    private const val PREFS = "netra_favorites"
    private const val SET_KEY = "uris"
    private const val NAME_PREFIX = "n_"

    fun isFavorite(context: Context, uri: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(SET_KEY, emptySet())?.contains(uri) == true

    /** Toggles the star for [uri]; returns the new state. */
    fun toggle(context: Context, uri: String, name: String): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = p.getStringSet(SET_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        val nowOn = if (cur.contains(uri)) { cur.remove(uri); false } else { cur.add(uri); true }
        val e = p.edit().putStringSet(SET_KEY, cur)
        if (nowOn) e.putString(NAME_PREFIX + ResumeStore.keyFor(uri), name) else e.remove(NAME_PREFIX + ResumeStore.keyFor(uri))
        e.apply()
        return nowOn
    }

    /** All favorites, sorted by name (case-insensitive). */
    fun list(context: Context): List<Pair<String, String>> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uris = p.getStringSet(SET_KEY, emptySet()) ?: emptySet()
        return uris.map { it to (p.getString(NAME_PREFIX + ResumeStore.keyFor(it), null) ?: it) }
            .sortedBy { it.second.lowercase() }
    }

    fun remove(context: Context, uri: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = p.getStringSet(SET_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        cur.remove(uri)
        p.edit().putStringSet(SET_KEY, cur).remove(NAME_PREFIX + ResumeStore.keyFor(uri)).apply()
    }
}
