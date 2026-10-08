package com.prayagi.netraplayer

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Owns playback beyond the activity. Media3 owns the foreground media notification. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            setHandleAudioBecomingNoisy(true)
        }
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Own UI and Android's trusted media controls only. Other apps cannot browse local files.
        return if (controllerInfo.packageName == packageName || controllerInfo.isTrusted) session else null
    }

    override fun onDestroy() {
        session?.let {
            val item = it.player.currentMediaItem
            val uri = item?.localConfiguration?.uri?.toString()
            if (uri != null) LastPlayed.save(this, uri,
                item.mediaMetadata.title?.toString() ?: "Selected file", it.player.currentPosition)
            it.player.release()
            it.release()
        }
        session = null
        super.onDestroy()
    }
}
