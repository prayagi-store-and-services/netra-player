package com.prayagi.netraplayer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.util.Rational

/**
 * Picture-in-picture for video playback.
 * The decision helpers are pure so they run in plain JVM tests; the Android entry never throws.
 */
object PipSupport {

    /** PiP is offered only when a video track is actually selected and the device has PiP. */
    fun shouldOffer(hasVideo: Boolean, deviceSupports: Boolean): Boolean = hasVideo && deviceSupports

    /** Android accepts ratios from 0.418410 to 2.39; anything outside falls back to 16:9. */
    fun aspectOrDefault(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 16 to 9
        val ratio = width.toDouble() / height.toDouble()
        return when {
            ratio < 0.4184 -> 16 to 9
            ratio > 2.39 -> 239 to 100
            else -> width to height
        }
    }

    fun deviceSupports(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /** Returns false when PiP is unavailable or the system refused; the caller shows an honest message. */
    fun enter(activity: Activity, videoWidth: Int, videoHeight: Int): Boolean {
        if (!deviceSupports(activity)) return false
        return try {
            val (w, h) = aspectOrDefault(videoWidth, videoHeight)
            activity.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(w, h)).build())
        } catch (_: Exception) {
            false
        }
    }
}
