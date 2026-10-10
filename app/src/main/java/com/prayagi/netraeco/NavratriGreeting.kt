package com.prayagi.netraplayer

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

internal object GreetingPolicy {
    val day: LocalDate = LocalDate.of(2026, 10, 11)
    fun eligible(date: LocalDate, seen: Boolean) = date == day && !seen
    fun maySpeak(silent: Boolean, mediaActive: Boolean) = !silent && !mediaActive
}

/** Device-only greeting. No downloaded voice, network request or interruption of playing media. */
@Composable
internal fun NavratriGreeting() {
    val context = LocalContext.current
    val clock = LocalPlayerDesignClock.current
    var date by remember { mutableStateOf(clock().toLocalDate()) }
    LaunchedEffect(clock) { while (true) { date = clock().toLocalDate(); delay(1000) } }
    if (date != GreetingPolicy.day) return
    Text("Jay Mata Di", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    DisposableEffect(date) {
        val prefs = context.getSharedPreferences("netra_greeting", Context.MODE_PRIVATE)
        val seen = prefs.getBoolean("oct11_seen", false)
        var engine: TextToSpeech? = null
        var disposed = false
        if (GreetingPolicy.eligible(date, seen)) {
            // Mark first open even if silent, active media or no installed offline voice. Never retry on each open.
            prefs.edit().putBoolean("oct11_seen", true).apply()
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (GreetingPolicy.maySpeak(audio.ringerMode != AudioManager.RINGER_MODE_NORMAL, audio.isMusicActive)) {
                engine = TextToSpeech(context) { status ->
                    val t = engine
                    if (!disposed && status == TextToSpeech.SUCCESS && t != null &&
                        GreetingPolicy.maySpeak(audio.ringerMode != AudioManager.RINGER_MODE_NORMAL, audio.isMusicActive)) {
                        val voice = t.voices?.filter { !it.isNetworkConnectionRequired &&
                            !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                            ?.sortedBy { when (it.locale.language) { "hi" -> 0; "en" -> 1; else -> 2 } }
                            ?.firstOrNull { it.locale.language in listOf("hi", "en") }
                        if (voice != null && t.setVoice(voice) == TextToSpeech.SUCCESS) {
                            t.setAudioAttributes(android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
                            t.speak("Jay Mata Di", TextToSpeech.QUEUE_FLUSH, null, "netra_oct11")
                        }
                    }
                }
            }
        }
        onDispose { disposed = true; engine?.stop(); engine?.shutdown() }
    }
}
