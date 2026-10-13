package com.prayagi.netraplayer

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Owns playback beyond the activity. Media3 owns the foreground media notification. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val historyHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private fun saveHistory() {
        val player = session?.player ?: return
        val item = player.currentMediaItem ?: return
        val uri = item.mediaId.takeIf { it.isNotEmpty() } ?: return
        LastPlayed.save(this, uri, item.mediaMetadata.title?.toString() ?: "Selected file", player.currentPosition)
    }
    private val checkpoint = object : Runnable {
        override fun run() {
            if (session?.player?.isPlaying == true) saveHistory()
            historyHandler.postDelayed(this, 15000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (RegionPolicy.isBlocked(this)) { stopSelf(); return }
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            setHandleAudioBecomingNoisy(true)
        }
        session = MediaSession.Builder(this, player).build()
        RecentFiles.start(this)
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) { saveHistory() }
            override fun onIsPlayingChanged(isPlaying: Boolean) { saveHistory() }
        })
        historyHandler.postDelayed(checkpoint, 15000)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (RegionPolicy.isBlocked(this)) {
            session?.player?.stop()
            stopSelf()
            return null
        }
        // Own UI and Android's trusted media controls only. Other apps cannot browse local files.
        return if (controllerInfo.packageName == packageName || controllerInfo.isTrusted) session else null
    }

    override fun onDestroy() {
        historyHandler.removeCallbacks(checkpoint)
        session?.let {
            val item = it.player.currentMediaItem
            val uri = item?.mediaId?.takeIf { it.isNotEmpty() }
            if (uri != null) LastPlayed.save(this, uri,
                item.mediaMetadata.title?.toString() ?: "Selected file", it.player.currentPosition)
            it.player.release()
            it.release()
        }
        session = null
        super.onDestroy()
    }
}
