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
import androidx.compose.foundation.layout.aspectRatio
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
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        UpdateAlert.handle(this, intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateAlert.start(this)
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

/** True while the video fills the whole screen (header, footer and banner are hidden). */
internal val playerFullscreen = mutableStateOf(false)

private enum class Section(val label: String) { Play("Play"), Update("Update"), About("About") }

@Composable
fun PlayerScreen() {
    var section by remember { mutableStateOf(Section.Play) }
    val full by playerFullscreen
    val isFull = full && section == Section.Play
    Column(Modifier.fillMaxSize().background(if (isFull) Color.Black else MaterialTheme.colorScheme.background)) {
        if (!isFull) Header()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().then(if (isFull) Modifier else Modifier.verticalScroll(rememberScrollState()).padding(16.dp)), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!isFull) FestivalBannerCard(modifier = Modifier.fillMaxWidth())
                when (section) {
                    Section.Play -> PlaySection()
                    Section.Update -> { UpdateSection(); DownloadManagerCard(); CrashReportCard(); PermissionsCard(playerPermissions()) }
                    Section.About -> AboutSection()
                }
            }
        }
        if (!isFull) Footer(section) { section = it }
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
    var connectedPlayer by remember { mutableStateOf<androidx.media3.session.MediaController?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) {
        val token = androidx.media3.session.SessionToken(context,
            android.content.ComponentName(context, PlaybackService::class.java))
        val future = androidx.media3.session.MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try { connectedPlayer = future.get() }
            catch (_: Exception) { connectionError = "Playback service unavailable. Reopen the app to retry." }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
        onDispose { androidx.media3.session.MediaController.releaseFuture(future) }
    }
    val player = connectedPlayer
    if (player == null) {
        Text(connectionError ?: "Connecting to player...")
        return
    }
    val tv = remember { isTelevision(context) }
    var title by remember { mutableStateOf(player.mediaMetadata.title?.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentUri by remember { mutableStateOf(player.currentMediaItem?.localConfiguration?.uri?.toString()) }
    var hasVideo by remember { mutableStateOf(true) }
    var tracks by remember { mutableStateOf(player.currentTracks) }
    var choosingTracks by remember { mutableStateOf(false) }
    val full by playerFullscreen
    fun setFull(on: Boolean) {
        playerFullscreen.value = on
        val act = context as? android.app.Activity ?: return
        act.requestedOrientation = if (on) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val c = androidx.core.view.WindowCompat.getInsetsController(act.window, act.window.decorView)
        if (on) { c.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE; c.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars()) }
        else c.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    }
    androidx.activity.compose.BackHandler(enabled = full) { setFull(false) }
    var last by remember { mutableStateOf(LastPlayed.load(context)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !isVideoOrMp3(context.contentResolver.getType(uri), displayName(context, uri))) {
            error = "Netra Player plays video and MP3 files only. Please pick a video or an MP3."
        } else if (uri != null) {
            error = null
            title = displayName(context, uri)
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            currentUri = uri.toString()
            LastPlayed.save(context, currentUri!!, title ?: "Selected file", 0L)
            PlayerWidgetProvider.refresh(context)
            player.setMediaItem(MediaItem.Builder().setUri(uri).setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()).build())
            player.prepare()
            player.playWhenReady = true
        }
    }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, false).build()
            }
            if (event == Lifecycle.Event.ON_STOP) {
                // Screen off/background: detach video selection; audio continues in the service.
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, true).build()
                val u = currentUri
                if (u != null) {
                    LastPlayed.save(context, u, title ?: "Selected file", player.currentPosition)
                    PlayerWidgetProvider.refresh(context)
                }
            }
        }
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) { error = playbackErrorMessage(e.errorCode) }
            override fun onTracksChanged(updated: androidx.media3.common.Tracks) {
                tracks = updated
                hasVideo = updated.isTypeSelected(androidx.media3.common.C.TRACK_TYPE_VIDEO)
            }
        }
        lifecycle.addObserver(observer)
        player.addListener(listener)
        onDispose { lifecycle.removeObserver(observer); player.removeListener(listener); if (playerFullscreen.value) { playerFullscreen.value = false } }
    }

    if (choosingTracks) TrackChoiceDialog(player, tracks) { choosingTracks = false }
    val screenH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val openPicker = {
        try { picker.launch(arrayOf("video/*", "audio/mpeg")) } catch (e: android.content.ActivityNotFoundException) { error = NO_PICKER_MESSAGE }
    }
    val playerView: @Composable (Modifier) -> Unit = { mod ->
        Box(mod.background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        controllerShowTimeoutMs = 4000
                        setShowPreviousButton(false); setShowNextButton(false)
                        setShowRewindButton(true); setShowFastForwardButton(true)
                        isFocusable = true; isFocusableInTouchMode = true
                    }
                },
                update = { v -> v.setFullscreenButtonClickListener { on -> setFull(on) } }
            )
            OutlinedButton(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                onClick = { choosingTracks = true }
            ) { Text("Tracks", color = Color.White) }
            if (!hasVideo) {
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Audio", color = Color(0xFF4DB6AC), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(title ?: "", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                }
            }
        }
    }
    if (full && currentUri != null) {
        playerView(Modifier.fillMaxWidth().height(screenH.dp))
    } else if (currentUri == null) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Play a video or MP3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Plays on this phone only. Nothing is uploaded. Video and MP3 files only.", style = MaterialTheme.typography.bodySmall)
                Button(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = { openPicker() }) { Text("Open video or MP3 file") }
                val resume = last
                if (resume != null) {
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = {
                        try {
                            val u = android.net.Uri.parse(resume.uri)
                            error = null
                            title = resume.name
                            currentUri = resume.uri
                            player.setMediaItem(MediaItem.Builder().setUri(u).setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()).build())
                            player.prepare()
                            player.seekTo(resume.positionMs)
                            player.playWhenReady = true
                        } catch (e: Exception) {
                            currentUri = null
                            error = "This file can no longer be opened. Pick it again."
                        }
                    }) { Text("Continue: ${resume.name} at ${LastPlayed.formatPosition(resume.positionMs)}", maxLines = 2) }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            playerView(Modifier.fillMaxWidth().aspectRatio(if (tv) 16f / 9f else 16f / 9f).clip(RoundedCornerShape(12.dp)))
            Text(title ?: "", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = { openPicker() }) { Text("Open another file") }
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
                SelfDownloadButton(r)
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

/** Uses only the supported tracks reported by the open file. No invented languages. */
@Composable
internal fun TrackChoiceDialog(
    player: androidx.media3.common.Player,
    tracks: androidx.media3.common.Tracks,
    onClose: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Audio and subtitles") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(androidx.media3.common.C.TRACK_TYPE_AUDIO to "Audio", androidx.media3.common.C.TRACK_TYPE_TEXT to "Subtitles").forEach { (type, heading) ->
                    Text(heading, fontWeight = FontWeight.Bold)
                    val groups = tracks.groups.filter { it.type == type }
                    var offered = 0
                    groups.forEach { group ->
                        for (index in 0 until group.length) {
                            if (group.isTrackSupported(index)) {
                                offered++
                                val format = group.getTrackFormat(index)
                                val label = trackChoiceLabel(format.label, format.language, offered)
                                val selected = group.isTrackSelected(index)
                                OutlinedButton(onClick = {
                                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                        .setTrackTypeDisabled(type, false)
                                        .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, index))
                                        .build()
                                    onClose()
                                }) { Text((if (selected) "Selected: " else "") + label) }
                            }
                        }
                    }
                    if (offered == 0) Text("Unavailable: this file has no supported $heading track.")
                    OutlinedButton(onClick = {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(type).setTrackTypeDisabled(type, false).build()
                        onClose()
                    }) { Text("Automatic $heading") }
                    if (type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                        OutlinedButton(onClick = {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(type).setTrackTypeDisabled(type, true).build()
                            onClose()
                        }) { Text("Subtitles off") }
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text("Close") } }
    )
}

internal fun trackChoiceLabel(label: String?, language: String?, number: Int): String {
    val name = label?.trim()?.takeIf { it.isNotEmpty() }
    val lang = language?.trim()?.takeIf { it.isNotEmpty() && it != "und" }
    return when {
        name != null && lang != null -> "$name ($lang)"
        name != null -> name
        lang != null -> lang
        else -> "Track $number (language unavailable)"
    }
}
