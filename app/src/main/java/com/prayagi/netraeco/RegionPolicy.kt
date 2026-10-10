package com.prayagi.netraplayer

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/** Availability hints only, not proof of physical location. Nothing is transmitted. */
internal object RegionPolicy {
    val BLOCKED_COUNTRIES = setOf("PK", "BD", "AF")

    fun blocked(sim: String?, network: String?, locale: String?): Boolean {
        fun normalized(value: String?) = value?.trim()?.uppercase(Locale.ROOT).orEmpty()
        val s = normalized(sim)
        val n = normalized(network)
        if (s == "IN") return false
        if (s.isNotEmpty() || n.isNotEmpty()) return s in BLOCKED_COUNTRIES || n in BLOCKED_COUNTRIES
        return normalized(locale) in BLOCKED_COUNTRIES
    }

    fun isBlocked(context: Context): Boolean {
        val phone = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val sim = runCatching { phone?.simCountryIso }.getOrNull()
        val network = runCatching { phone?.networkCountryIso }.getOrNull()
        val locale = context.resources.configuration.locales.let { if (it.isEmpty) "" else it[0].country }
        return blocked(sim, network, locale)
    }
}
