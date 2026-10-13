package com.prayagi.netraplayer

/** Background-only policy. Manual downloads keep their existing explicit user action. */
internal object PreDownloadPolicy {
    const val MOBILE_LIMIT_BYTES = 100L * 1024 * 1024
    fun allowed(online: Boolean, wifi: Boolean, mobile: Boolean, roaming: Boolean,
                dataSaver: Boolean, batteryPercent: Int?, charging: Boolean,
                alreadySaved: Boolean, size: Long): Boolean {
        if (!online || dataSaver || alreadySaved || size <= 0 || batteryPercent == null) return false
        if (!charging && batteryPercent < 20) return false
        return wifi || (mobile && !roaming && size <= MOBILE_LIMIT_BYTES)
    }
}
