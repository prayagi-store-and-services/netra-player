package com.prayagi.netraplayer

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Tracks

/**
 * Video-to-Audio mode: play a video's sound like music.
 *
 * The choice is stored per media item (stable content URI) so it survives
 * app stop/start, screen lock and service recreation. The video track is
 * disabled through Media3 track selection - one player, no second instance,
 * so position, queue, notification and session controls are untouched.
 */
internal object AudioOnlyMode {
    private const val PREFS = "audio_only_mode"

    // Activity and service run on the main thread in the same process. A new
    // process starts in the background until the player UI reports STARTED.
    // The service uses this on queue transitions even without a UI listener.
    internal var appForeground: Boolean = false

    fun isAudioOnly(context: Context, mediaId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key(mediaId), false)

    fun setAudioOnly(context: Context, mediaId: String, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key(mediaId), on).apply()
    }

    internal fun key(mediaId: String): String = "m:$mediaId"

    /**
     * Single source of truth for when the video track must be disabled:
     * the user chose audio-only, or the app is not in the foreground.
     * Nothing else may re-enable video behind the user's back.
     */
    fun disableVideoTrack(audioOnly: Boolean, appForeground: Boolean): Boolean = audioOnly || !appForeground

    /** Video-track AVAILABILITY (does the item carry video?) - independent of whether video is selected right now. */
    fun hasVideoTrack(tracks: Tracks): Boolean =
        tracks.groups.any { it.type == C.TRACK_TYPE_VIDEO && it.length > 0 }
}
