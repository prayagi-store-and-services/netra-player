package com.prayagi.netraplayer

/** Original Netra controls using Media3's native resize modes. No VLC code or assets. */
internal enum class VideoFit(val label: String, val resizeMode: Int) {
    FIT("Fit", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT),
    CROP("Crop", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL);

    fun next(): VideoFit = entries[(ordinal + 1) % entries.size]
}
