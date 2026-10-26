package com.prayagi.netraplayer.power

/** Pure decision layer. Battery cost budgets require physical measurements, not constants here. */
enum class PowerTier { NORMAL, SAVER, CRITICAL, SURVIVAL }

data class PowerInputs(
    val batteryPercent: Int?,
    val charging: Boolean,
    val batterySaver: Boolean,
    val thermalThrottling: Boolean,
    val visible: Boolean,
    val screenInteractive: Boolean
)

object PowerPolicy {
    /** Enter restrictions immediately; recover with a 2-point margin to avoid boundary flapping. */
    fun tier(input: PowerInputs, previous: PowerTier? = null): PowerTier {
        val percent = input.batteryPercent?.coerceIn(0, 100)
        var next = when {
            // Charging selects Normal as specified; Battery Saver and thermal remain overrides.
            input.charging -> if (input.batterySaver) PowerTier.SAVER else PowerTier.NORMAL
            percent != null && percent < 5 -> PowerTier.SURVIVAL
            percent != null && percent < 15 -> PowerTier.CRITICAL
            input.batterySaver -> PowerTier.SAVER
            percent == null || percent <= 30 -> PowerTier.SAVER
            else -> PowerTier.NORMAL
        }
        if (previous != null && next.ordinal < previous.ordinal && !input.charging) {
            val recovery = when (previous) {
                PowerTier.SURVIVAL -> 7
                PowerTier.CRITICAL -> 17
                PowerTier.SAVER -> 33
                PowerTier.NORMAL -> 0
            }
            if (percent == null || percent < recovery) next = previous
        }
        if (input.thermalThrottling && next != PowerTier.SURVIVAL) {
            next = PowerTier.entries[next.ordinal + 1]
        }
        return next
    }

    /** Decorative header has no work outside a visible, interactive activity. */
    fun headerIntervalMillis(input: PowerInputs, tier: PowerTier): Long? {
        if (!input.visible || !input.screenInteractive) return null
        return when (tier) {
            PowerTier.NORMAL -> 30_000L
            PowerTier.SAVER -> 60_000L
            PowerTier.CRITICAL -> 300_000L
            PowerTier.SURVIVAL -> null
        }
    }
}
