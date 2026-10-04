package com.prayagi.netraplayer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.install(this)
        setContent { NetraTheme { PlayerScreen() } }
    }

    override fun onResume() {
        super.onResume()
        // Delete installer files left from a finished or cancelled update (not while a download runs).
        Thread {
            Net.cleanLeftovers(applicationContext)
            UsagePing.pingIfDue(applicationContext)
        }.start()
    }
}

private val Teal = Color(0xFF00796B)
private val TealDark = Color(0xFF004D40)

@Composable
fun NetraTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFF4DB6AC), onPrimary = Color(0xFF00201C),
        background = Color(0xFF101414), surface = Color(0xFF182020), onSurface = Color(0xFFE0E6E4),
        surfaceVariant = Color(0xFF22302E)
    ) else lightColorScheme(
        primary = Teal, onPrimary = Color.White,
        background = Color(0xFFF3F7F6), surface = Color.White, onSurface = Color(0xFF16201E),
        surfaceVariant = Color(0xFFE0EEEB)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

private enum class Section(val label: String) { Play("Play"), Update("Update"), About("About") }

@Composable
fun PlayerScreen() {
    var section by remember { mutableStateOf(Section.Play) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Header()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (section) {
                    Section.Play -> PlaySection()
                    Section.Update -> { UpdateSection(); CrashReportCard() }
                    Section.About -> AboutSection()
                }
            }
        }
        Footer(section) { section = it }
    }
}

@Composable
private fun Header() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(33) } }
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Teal, TealDark))).statusBarsPadding().height(56.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Column {
            val version = remember { Net.installed(context, context.packageName)?.second?.ifBlank { null } ?: "Unavailable" }
            Text("Netra Player  v$version", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(HeaderText.date(now) + "   " + HeaderText.clock(now), fontSize = 12.sp, maxLines = 1, color = Color(0xFFD0ECE8))
        }
    }
}

@Composable
private fun Footer(current: Section, onPick: (Section) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).navigationBarsPadding().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Section.values().forEach { s ->
            if (s == current) Button(onClick = { onPick(s) }) { Text(s.label) }
            else OutlinedButton(onClick = { onPick(s) }) { Text(s.label) }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "Selected file"
} catch (e: Exception) {
    "Selected file"
}

/** Player scope: video files and MP3 only. Some phones report MP3 as audio/mp3 or no type at all, so the file name is checked too. */
internal fun isVideoOrMp3(mime: String?, name: String?): Boolean {
    val m = mime?.lowercase().orEmpty()
    if (m.startsWith("video/")) return true
    if (m == "audio/mpeg" || m == "audio/mp3") return true
    return (m.isEmpty() || m == "application/octet-stream") && name?.lowercase()?.endsWith(".mp3") == true
}

@Composable
private fun PlaySection() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val player = remember { ExoPlayer.Builder(context).build() }
    val tv = remember { isTelevision(context) }
    var title by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !isVideoOrMp3(context.contentResolver.getType(uri), displayName(context, uri))) {
            error = "Netra Player plays video and MP3 files only. Please pick a video or an MP3."
        } else if (uri != null) {
            error = null
            title = displayName(context, uri)
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
        }
    }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) { error = "This file could not be played on this phone." }
        }
        lifecycle.addObserver(observer)
        player.addListener(listener)
        onDispose { lifecycle.removeObserver(observer); player.removeListener(listener); player.release() }
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Play a video or MP3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Pick a video or an MP3 file from this phone. Other file types are not supported. Nothing is uploaded; it plays on this device.", style = MaterialTheme.typography.bodySmall)
            Button(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = {
                try { picker.launch(arrayOf("video/*", "audio/mpeg")) } catch (e: android.content.ActivityNotFoundException) { error = NO_PICKER_MESSAGE }
            }) {
                Text("Open video or MP3 file")
            }
            Text(title?.let { "Now playing: $it" } ?: "No file chosen yet", style = MaterialTheme.typography.bodyMedium)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(playerHeightDp(tv).dp).clip(RoundedCornerShape(12.dp)),
                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true; isFocusable = true; isFocusableInTouchMode = true } }
            )
        }
    }
}

@Composable
private fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<String?>(null) }
    var release by remember { mutableStateOf<LatestRelease?>(null) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    message = null
                    release = null
                    val self = Net.selfApp(context)
                    val (installed, latest) = withContext(Dispatchers.IO) { Net.installed(context, self.packageName) to Net.fetchLatest(self) }
                    val status = statusForRelease(installed?.first, installed?.second, latest)
                    message = selfUpdateMessage(installed?.second.orEmpty().ifBlank { "Unavailable" }, status, latest?.versionName)
                    if (status == Status.UpdateAvailable) release = latest
                    busy = false
                }
            }) { Text(if (busy) "Checking..." else "Check for update") }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            progress?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            release?.let { r ->
                val notes = usefulNotes(r.notes)
                if (notes.isNotBlank()) {
                    Text("What's new in " + r.versionName + ":", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(notes.take(800), style = MaterialTheme.typography.bodySmall)
                }
                Button(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val file = withContext(Dispatchers.IO) { Net.download(context, Net.selfApp(context), r) { d, t, e -> progress = DownloadText.line(d, t, e) } }
                            Net.install(context, file)
                        } catch (e: Exception) {
                            message = e.message ?: "Update failed."
                        }
                        busy = false
                        progress = null
                    }
                }) { Text("Update to " + r.versionName) }
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    var usage by remember { mutableStateOf(UsagePing.isEnabled(context)) }
    val version = remember { Net.installed(context, context.packageName)?.second ?: "Unavailable" }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Netra Player", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Version $version. Video and music player by Prayagi Team. Everything stays on this device.", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Share anonymous usage count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Once a day the app adds 1 to a public counter so the Netra site can show roughly how many people use it. No ID, no location, no files.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = usage, onCheckedChange = { usage = it; UsagePing.setEnabled(context, it) })
            }
            Spacer(Modifier.height(2.dp))
            Text("What is coming next is listed on the Netra website.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
