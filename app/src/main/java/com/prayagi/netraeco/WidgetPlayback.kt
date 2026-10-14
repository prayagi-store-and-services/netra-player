package com.prayagi.netraplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

/** Private explicit widget commands, routed to the one existing playback session. */
class WidgetPlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(PLAY_PAUSE, NEXT)) return
        val pending = goAsync()
        val app = context.applicationContext
        val future = try { MediaController.Builder(app, SessionToken(app,
            android.content.ComponentName(app, PlaybackService::class.java))).buildAsync() }
        catch (_: Exception) { pending.finish(); return }
        future.addListener({
            try {
                val controller = future.get()
                when (intent.action) {
                    PLAY_PAUSE -> if (controller.mediaItemCount > 0) {
                        if (controller.isPlaying) controller.pause() else controller.play()
                    }
                    NEXT -> if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
                }
                PlayerWidgetProvider.refresh(context)
            } catch (_: Exception) { /* No playback-success claim from command dispatch. */ }
            finally { MediaController.releaseFuture(future); pending.finish() }
        }, ContextCompat.getMainExecutor(app))
    }
    companion object {
        const val PLAY_PAUSE = "com.prayagi.netraplayer.widget.PLAY_PAUSE"
        const val NEXT = "com.prayagi.netraplayer.widget.NEXT"
    }
}

object WidgetPlaybackState {
    var active = false
    var playing = false
    var next = false
    var title: String? = null
    fun update(player: androidx.media3.common.Player) {
        active = player.mediaItemCount > 0
        playing = player.isPlaying
        next = player.hasNextMediaItem()
        title = player.currentMediaItem?.mediaMetadata?.title?.toString()
    }
    fun reset() { active = false; playing = false; next = false; title = null }
}
