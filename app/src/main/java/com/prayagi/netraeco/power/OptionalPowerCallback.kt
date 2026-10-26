package com.prayagi.netraplayer.power

/** Unsupported/broken system callback service must not prevent the app from opening. */
internal object OptionalPowerCallback {
    fun register(start: () -> Unit, onUnavailable: () -> Unit): Boolean = try {
        start()
        true
    } catch (_: RuntimeException) {
        onUnavailable()
        false
    }
}
