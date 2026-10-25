package com.prayagi.netraplayer

/** Local, user-requested snapshot. No network, identity or third-party package data. */
internal data class HeatReading(
    val elapsedMs: Long,
    val processCpuMs: Long,
    val batteryDeciC: Int?,
    val charging: Boolean?,
    val thermalStatus: Int?
)

internal data class HeatPreview(
    val windowMs: Long,
    val processCpuMs: Long,
    // One fully busy core is 100%; a multi-core process may exceed 100%.
    val cpuCorePercent: Double,
    val batteryDeciC: Int?,
    val charging: Boolean?,
    val thermalStatus: Int?
) {
    companion object {
        fun between(start: HeatReading, end: HeatReading): HeatPreview? {
            if (start.elapsedMs < 0 || start.processCpuMs < 0 || end.elapsedMs <= start.elapsedMs ||
                end.processCpuMs < start.processCpuMs) return null
            val wall = end.elapsedMs - start.elapsedMs
            // Reject short/noisy samples and delayed/background samples.
            if (wall !in 5_000L..15_000L) return null
            val cpu = end.processCpuMs - start.processCpuMs
            return HeatPreview(wall, cpu, cpu.toDouble() * 100.0 / wall,
                end.batteryDeciC?.takeIf { it in -400..1_000 }, end.charging,
                end.thermalStatus?.takeIf { it in 0..6 })
        }
    }
}

/** Memory-only gate. OFF on every new screen; no sampler or upload worker is started here. */
internal class HeatPreviewGate {
    var enabled: Boolean = false
        private set
    var preview: HeatPreview? = null
        private set
    private var generation: Long = 0
    private var active: Long? = null

    fun setEnabled(value: Boolean) {
        enabled = value
        generation++
        active = null
        preview = null
    }

    fun begin(): Long? {
        if (!enabled || active != null) return null
        generation++
        preview = null
        return generation.also { active = it }
    }

    fun finish(token: Long, value: HeatPreview?) {
        if (enabled && active == token) {
            preview = value
            active = null
        }
    }

    fun isCurrent(token: Long): Boolean = enabled && generation == token

    fun cancel() { generation++; active = null }
    fun clear() { cancel(); preview = null }
}
