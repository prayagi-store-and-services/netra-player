package com.prayagi.netraplayer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.pointer.pointerInput
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

open class MainActivity : ComponentActivity() {
    private var regionDenied = false

    private fun denyRegion(): Boolean {
        if (!RegionPolicy.isBlocked(this)) return false
        regionDenied = true
        stopService(android.content.Intent(this, PlaybackService::class.java))
        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Text("Not available in this region", color = Color.White, modifier = Modifier.padding(24.dp))
                }
                LaunchedEffect(Unit) { delay(2500); finishAndRemoveTask() }
            }
        }
        return true
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (denyRegion()) return
        UpdateAlert.handle(this, intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (denyRegion()) return
        UpdateAlert.start(this)
        CrashReporter.install(this)
        setContent { NetraTheme { AppUpdatePrompt(); PlayerScreen() } }
    }

    override fun onResume() {
        super.onResume()
        if (regionDenied || denyRegion()) return
        // Delete installer files left from a finished or cancelled update (not while a download runs).
        Thread {
            UsagePing.pingIfDue(applicationContext)
        }.start()
    }

    private val pipModeState = mutableStateOf(false)
    internal val inPipMode: androidx.compose.runtime.State<Boolean> get() = pipModeState

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipModeState.value = isInPictureInPictureMode
    }
}

private val Teal = Color(0xFF00796B)
private val TealDark = Color(0xFF004D40)

@Composable
fun NetraTheme(content: @Composable () -> Unit) {
    val redesign by rememberPlayerDesign()
    val clock = LocalPlayerDesignClock.current
    var festive by remember { mutableStateOf(clock().toLocalDate() == GreetingPolicy.day) }
    LaunchedEffect(clock) { while (true) { festive = clock().toLocalDate() == GreetingPolicy.day; delay(1000) } }
    val dark = isSystemInDarkTheme()
    val colors = if (redesign) darkColorScheme(
        primary = if (festive) Color(0xFFFBBF24) else Color(0xFF29DDE0), onPrimary = Color(0xFF002A34),
        background = Color(0xFF04101E), onBackground = Color(0xFFE8F6FA),
        surface = Color(0xFF0A2033), onSurface = Color(0xFFE8F6FA),
        surfaceVariant = Color(0xFF0E2D43), onSurfaceVariant = Color(0xFFB5CCD8),
        outline = Color(0xFF284B61)
    ) else if (dark) darkColorScheme(
        primary = Color(0xFF4DB6AC), onPrimary = Color(0xFF00201C),
        background = Color(0xFF101414), surface = Color(0xFF182020), onSurface = Color(0xFFE0E6E4),
        surfaceVariant = Color(0xFF22302E)
    ) else lightColorScheme(
        primary = Teal, onPrimary = Color.White,
        background = Color(0xFFF3F7F6), surface = Color.White, onSurface = Color(0xFF16201E),
        surfaceVariant = Color(0xFFE0EEEB)
    )
    MaterialTheme(colorScheme = colors) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides colors.onBackground, content = content)
    }
}

/** True while the video fills the whole screen (header, footer and banner are hidden). */
internal val playerFullscreen = mutableStateOf(false)

private enum class Section(val label: String) { Play("Play"), Update("Update"), About("About") }

@Composable
fun PlayerScreen() {
    val redesign by rememberPlayerDesign()
    var section by remember { mutableStateOf(Section.Play) }
    val full by playerFullscreen
    val inPip = (LocalContext.current as? MainActivity)?.inPipMode?.value == true
    val isFull = (full || inPip) && section == Section.Play
    Column(Modifier.fillMaxSize().background(if (isFull) Color.Black else MaterialTheme.colorScheme.background)) {
        if (!isFull) { Header(); NavratriGreeting(); if (redesign) Footer(section) { section = it } }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().then(if (isFull) Modifier else Modifier.verticalScroll(rememberScrollState()).padding(16.dp)), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!isFull) FestivalBannerCard(modifier = Modifier.fillMaxWidth())
                when (section) {
                    Section.Play -> PlaySection()
                    Section.Update -> { UpdateSection(); CrashReportCard(); PermissionsCard(playerPermissions()) }
                    Section.About -> { AboutSection(); if (redesign) PlayerRoadmapCard() }
                }
            }
        }
        if (!isFull && !redesign) Footer(section) { section = it }
    }
}

@Composable
private fun Header() {
    val redesign by rememberPlayerDesign()
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    PlayerClockTicks { now = it }
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(if (redesign) if (LocalPlayerDesignClock.current().toLocalDate() == GreetingPolicy.day) listOf(Color(0xFF78350F), Color(0xFF451A03)) else listOf(Color(0xFF092A3F), Color(0xFF04101E)) else listOf(Teal, TealDark))).statusBarsPadding().height(if (redesign) 76.dp else 56.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Column {
            val version = remember { Net.installed(context, context.packageName)?.second?.ifBlank { null } ?: "Unavailable" }
            Text("Netra Player  v$version", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            if (redesign) Text("Play local. Stay private. No login.", fontSize = 12.sp, color = Color(0xFF80E7EB))
            Text(HeaderText.date(now) + "   " + HeaderText.clock(now), fontSize = 12.sp, maxLines = 1, color = Color(0xFFD0ECE8))
        }
    }
}

@Composable
private fun Footer(current: Section, onPick: (Section) -> Unit) {
    val redesign by rememberPlayerDesign()
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).then(if (redesign) Modifier else Modifier.navigationBarsPadding()).padding(8.dp),
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

/** Player scope: common audio and video. MIME type leads; the file extension is a fallback
 *  only when the MIME is missing or generic (some phones report no type). Actual playback
 *  still depends on Media3 extractors and device codecs - unsupported files fail with the
 *  existing playback error path, never silently. */
internal fun isPlayableMedia(mime: String?, name: String?): Boolean {
    val m = mime?.lowercase().orEmpty()
    if (m.startsWith("video/") || m.startsWith("audio/")) return true
    if (m.isNotEmpty() && m != "application/octet-stream") return false
    val ext = name?.lowercase()?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() } ?: return false
    return ext in MEDIA_FALLBACK_EXTENSIONS
}

/** Extensions trusted only as a fallback when MIME is missing or application/octet-stream. */
private val MEDIA_FALLBACK_EXTENSIONS = setOf(
    "mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus",
    "mp4", "m4v", "mkv", "avi", "webm", "3gp", "ts",
)

@OptIn(ExperimentalLayoutApi::class)
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
    var currentUri by remember { mutableStateOf(player.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() }) }
    var hasVideo by remember { mutableStateOf(player.currentTracks.isTypeSelected(androidx.media3.common.C.TRACK_TYPE_VIDEO)) }
    var videoAvailable by remember { mutableStateOf(AudioOnlyMode.hasVideoTrack(player.currentTracks)) }
    var audioOnly by remember { mutableStateOf(player.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() }?.let { AudioOnlyMode.isAudioOnly(context, it) } ?: false) }
    fun setVideoTrackEnabled(enabled: Boolean) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, !enabled).build()
    }
    val inPip = (context as? MainActivity)?.inPipMode?.value == true
    val pipAvailable = remember { PipSupport.deviceSupports(context) }
    var tracks by remember { mutableStateOf(player.currentTracks) }
    var choosingTracks by remember { mutableStateOf(false) }
    var videoFit by remember { mutableStateOf(VideoFit.FIT) }
    val folderScope = rememberCoroutineScope()
    var folderFiles by remember { mutableStateOf<List<FolderMedia>>(emptyList()) }
    var folderBusy by remember { mutableStateOf(false) }
    var folderPage by remember { mutableStateOf(0) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) folderScope.launch {
            folderBusy = true
            try {
                context.contentResolver.takePersistableUriPermission(tree, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                folderFiles = withContext(Dispatchers.IO) { listFolderMedia(context, tree) }
                folderPage = 0
                error = if (folderFiles.isEmpty()) "No supported audio or video files in this folder." else null
            } catch (e: Exception) {
                folderFiles = emptyList()
                error = if (e.message == "Folder too large") "This folder has over 1000 media files. Choose a smaller folder." else "Folder unavailable. Choose it again."
            }
            finally { folderBusy = false }
        }
    }
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { subtitle ->
        if (subtitle != null) {
            val name = displayName(context, subtitle)
            if (!name.endsWith(".srt", ignoreCase = true)) {
                error = "Choose an .srt subtitle file."
            } else if (player.currentMediaItem == null) {
                error = "Open a video first."
            } else {
                try {
                    val descriptor = context.contentResolver.openAssetFileDescriptor(subtitle, "r")
                        ?: throw java.io.IOException("Subtitle unavailable")
                    descriptor.use { if (it.length > 5L * 1024L * 1024L) throw java.io.IOException("Subtitle too large") }
                    context.contentResolver.takePersistableUriPermission(subtitle, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val item = player.currentMediaItem!!
                    val position = player.currentPosition
                    val playing = player.playWhenReady
                    val config = MediaItem.SubtitleConfiguration.Builder(subtitle)
                        .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                        .setLabel(name).setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT).build()
                    val index = player.currentMediaItemIndex
                    val updated = item.buildUpon().setUri(item.mediaId)
                        .setSubtitleConfigurations(listOf(config)).build()
                    // A same-URI replace can retain the old prepared source. Rebuild the queue
                    // so the side-loaded text source is created, keeping the selected item/time.
                    val queue = (0 until player.mediaItemCount).map { i ->
                        if (i == index) updated else player.getMediaItemAt(i).let { existing ->
                            existing.buildUpon().setUri(existing.mediaId).build()
                        }
                    }
                    player.setMediaItems(queue, index, position)
                    player.prepare()
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false).build()
                    player.seekTo(position)
                    player.playWhenReady = playing
                    error = null
                } catch (_: Exception) { error = "Subtitle file unavailable. Pick it again." }
            }
        }
    }
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
        if (uri != null && !isPlayableMedia(context.contentResolver.getType(uri), displayName(context, uri))) {
            error = "Choose an audio or video file. Playback depends on the file format and this device."
        } else if (uri != null) {
            error = null
            title = displayName(context, uri)
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            currentUri = uri.toString()
            LastPlayed.save(context, currentUri!!, title ?: "Selected file", 0L)
            PlayerWidgetProvider.refresh(context)
            player.setMediaItem(MediaItem.Builder().setUri(uri).setMediaId(uri.toString()).setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()).build())
            player.prepare()
            player.playWhenReady = true
        }
    }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                AudioOnlyMode.appForeground = true
                // Foreground: video comes back ONLY when the user did not choose audio-only for this item.
                setVideoTrackEnabled(!AudioOnlyMode.disableVideoTrack(audioOnly, appForeground = true))
            }
            if (event == Lifecycle.Event.ON_STOP) {
                AudioOnlyMode.appForeground = false
                // Screen off/background: detach video selection; audio continues in the service.
                setVideoTrackEnabled(!AudioOnlyMode.disableVideoTrack(audioOnly, appForeground = false))
                val u = currentUri
                if (u != null) {
                    LastPlayed.save(context, u, title ?: "Selected file", player.currentPosition)
                    PlayerWidgetProvider.refresh(context)
                }
            }
        }
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                title = item?.mediaMetadata?.title?.toString()
                currentUri = item?.mediaId?.takeIf { it.isNotEmpty() }
                error = null
                audioOnly = currentUri?.let { AudioOnlyMode.isAudioOnly(context, it) } ?: false
                setVideoTrackEnabled(!AudioOnlyMode.disableVideoTrack(audioOnly,
                    appForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)))
            }
            override fun onPlayerError(e: androidx.media3.common.PlaybackException) { error = playbackErrorMessage(e.errorCode) }
            override fun onTracksChanged(updated: androidx.media3.common.Tracks) {
                tracks = updated
                hasVideo = updated.isTypeSelected(androidx.media3.common.C.TRACK_TYPE_VIDEO)
                videoAvailable = AudioOnlyMode.hasVideoTrack(updated)
            }
        }
        AudioOnlyMode.appForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        setVideoTrackEnabled(!AudioOnlyMode.disableVideoTrack(audioOnly, AudioOnlyMode.appForeground))
        lifecycle.addObserver(observer)
        player.addListener(listener)
        onDispose {
            lifecycle.removeObserver(observer); player.removeListener(listener)
            if (playerFullscreen.value) { playerFullscreen.value = false }
            val act = context as? android.app.Activity
            if (act != null) {
                val lp = act.window.attributes
                lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                act.window.attributes = lp
            }
        }
    }

    LaunchedEffect(player) {
        val u = player.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() }
        if (u != null) {
            audioOnly = AudioOnlyMode.isAudioOnly(context, u)
            setVideoTrackEnabled(!AudioOnlyMode.disableVideoTrack(audioOnly,
                appForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)))
        }
    }
    if (choosingTracks && !inPip) TrackChoiceDialog(player, tracks) { choosingTracks = false }
    val screenH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val openPicker = {
        try { picker.launch(arrayOf("video/*", "audio/*")) } catch (e: android.content.ActivityNotFoundException) { error = NO_PICKER_MESSAGE }
    }
    var controllerVisible by remember { mutableStateOf(true) }
    var gestureText by remember { mutableStateOf<String?>(null) }
    val playerViewHolder = remember { arrayOfNulls<PlayerView>(1) }
    LaunchedEffect(gestureText) {
        if (gestureText != null) { delay(800); gestureText = null }
    }
    val playerView: @Composable (Modifier) -> Unit = { mod ->
        Box(mod.background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        playerViewHolder[0] = this
                        this.player = player
                        useController = true
                        controllerShowTimeoutMs = 4000
                        setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                            controllerVisible = visibility == android.view.View.VISIBLE
                        })
                        setShowPreviousButton(false); setShowNextButton(false)
                        setShowRewindButton(true); setShowFastForwardButton(true)
                        // Keep D-pad/keyboard navigation without consuming the first touch for focus.
                        isFocusable = true; isFocusableInTouchMode = false
                    }
                },
                update = { v ->
                    v.resizeMode = videoFit.resizeMode
                    v.useController = !inPip
                    v.setFullscreenButtonClickListener { on -> setFull(on) }
                }
            )
            // Touch gestures, active only while the native controller is hidden so its buttons keep working.
            if (!inPip && !controllerVisible) {
                val audioManager = remember { context.getSystemService(android.media.AudioManager::class.java) }
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(player) {
                            detectTapGestures(
                                onTap = {
                                    // This layer exists only while the controller is hidden, so a tap always brings it back.
                                    playerViewHolder[0]?.showController()
                                },
                                onDoubleTap = { offset ->
                                    val forward = offset.x >= size.width / 2f
                                    player.seekTo(PlayerGestures.doubleTapTargetMs(player.currentPosition, forward, player.duration))
                                    gestureText = if (forward) "+10s" else "-10s"
                                }
                            )
                        }
                        .pointerInput(player) {
                            var startXFraction = 0.5f
                            var accDx = 0f
                            var accDy = 0f
                            var mode = PlayerGestures.DragMode.UNDECIDED
                            var startLevel = 0.5f
                            var startVolume = 0
                            var maxVolume = 15
                            var startPosition = 0L
                            detectDragGestures(
                                onDragStart = { offset ->
                                    accDx = 0f; accDy = 0f
                                    mode = PlayerGestures.DragMode.UNDECIDED
                                    startXFraction = if (size.width > 0) offset.x / size.width.toFloat() else 0.5f
                                    startPosition = player.currentPosition
                                    if (audioManager != null) {
                                        startVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                                        maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                    }
                                    val act = context as? android.app.Activity
                                    startLevel = act?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
                                },
                                onDrag = { change, dragAmount ->
                                    accDx += dragAmount.x; accDy += dragAmount.y
                                    if (mode == PlayerGestures.DragMode.UNDECIDED) {
                                        mode = PlayerGestures.decideDragMode(startXFraction, accDx, accDy)
                                    }
                                    when (mode) {
                                        PlayerGestures.DragMode.BRIGHTNESS -> {
                                            val v = PlayerGestures.levelAfterDrag(startLevel, accDy, size.height.toFloat())
                                            val act = context as? android.app.Activity
                                            if (act != null) {
                                                val lp = act.window.attributes
                                                lp.screenBrightness = v
                                                act.window.attributes = lp
                                            }
                                            gestureText = "Brightness ${PlayerGestures.levelPercent(v)}%"
                                        }
                                        PlayerGestures.DragMode.VOLUME -> {
                                            val base = if (maxVolume > 0) startVolume / maxVolume.toFloat() else 0.5f
                                            val v = PlayerGestures.levelAfterDrag(base, accDy, size.height.toFloat())
                                            val target = (v * maxVolume).toInt().coerceIn(0, maxVolume)
                                            audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
                                            gestureText = "Volume $target/$maxVolume"
                                        }
                                        PlayerGestures.DragMode.SEEK -> {
                                            gestureText = "Seek ${PlayerGestures.formatSeekDelta(PlayerGestures.seekDeltaMs(accDx, size.width.toFloat()))}"
                                        }
                                        PlayerGestures.DragMode.UNDECIDED -> { }
                                    }
                                    change.consume()
                                },
                                onDragEnd = {
                                    if (mode == PlayerGestures.DragMode.SEEK) {
                                        player.seekTo(PlayerGestures.seekTargetMs(startPosition, PlayerGestures.seekDeltaMs(accDx, size.width.toFloat()), player.duration))
                                    }
                                    mode = PlayerGestures.DragMode.UNDECIDED
                                },
                                onDragCancel = { mode = PlayerGestures.DragMode.UNDECIDED }
                            )
                        }
                )
            }
            gestureText?.let {
                Text(it, color = Color.White, fontSize = 16.sp, modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 6.dp))
            }
            if (!inPip && (!full || controllerVisible)) {
                FlowRow(Modifier.align(Alignment.TopEnd).fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (PipSupport.shouldOffer(hasVideo, pipAvailable)) OutlinedButton(colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.8f)), onClick = {
                        val act = context as? MainActivity
                        if (act == null || !PipSupport.enter(act, player.videoSize.width, player.videoSize.height)) {
                            error = "PiP is not available on this phone."
                        }
                    }) { Text("PiP", color = Color.White) }
                    if (hasVideo) OutlinedButton(colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.8f)), onClick = { videoFit = videoFit.next() }) {
                        Text("Size: ${videoFit.label}", color = Color.White)
                    }
                    if (videoAvailable) OutlinedButton(colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.8f)), onClick = {
                        val next = !audioOnly
                        audioOnly = next
                        currentUri?.let { AudioOnlyMode.setAudioOnly(context, it, next) }
                        setVideoTrackEnabled(!AudioOnlyMode.disableVideoTrack(next,
                            appForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)))
                    }) { Text(if (audioOnly) "Play Video" else "Listen as Audio", color = Color.White) }
                    OutlinedButton(colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.8f)), onClick = { choosingTracks = true }) { Text("Tracks", color = Color.White) }
                }
            }
            if (!inPip && !hasVideo && (!full || controllerVisible)) {
                Column(Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 20.dp, end = 112.dp), horizontalAlignment = Alignment.Start) {
                    Text("Audio", color = Color(0xFF4DB6AC), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(title ?: "", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                }
            }
        }
    }
    if (!full && !inPip) {
        OutlinedButton(onClick = {
            try { folderPicker.launch(null) }
            catch (_: android.content.ActivityNotFoundException) { error = NO_PICKER_MESSAGE }
        }, enabled = !folderBusy) { Text(if (folderBusy) "Reading folder..." else "Open folder") }
        if (player.mediaItemCount > 1) {
            Text("Queue: ${player.currentMediaItemIndex + 1} of ${player.mediaItemCount}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = player.hasPreviousMediaItem(), onClick = { player.seekToPreviousMediaItem() }) { Text("Previous") }
                OutlinedButton(enabled = player.hasNextMediaItem(), onClick = { player.seekToNextMediaItem() }) { Text("Next") }
            }
        }
        OutlinedButton(enabled = currentUri != null, onClick = {
            try { subtitlePicker.launch(arrayOf("application/x-subrip", "text/plain", "application/octet-stream")) }
            catch (_: android.content.ActivityNotFoundException) { error = NO_PICKER_MESSAGE }
        }) { Text("Open .srt subtitles") }

        // Headphone 3D ("stadium") sound: offered only with headphones connected,
        // and only when this phone actually supports the effect.
        val fx by HeadphoneEffect.state.collectAsState()
        if (fx.headphonesConnected) {
            if (fx.effectSupported) {
                OutlinedButton(onClick = { HeadphoneEffect.setEnabled(!fx.effectOn) }) {
                    Text(if (fx.effectOn) "3D stadium sound: On" else "3D stadium sound: Off", maxLines = 1)
                }
            } else {
                Text(
                    "3D stadium sound is not available on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
    if (inPip && currentUri != null) {
        playerView(Modifier.fillMaxSize())
    } else if (full && currentUri != null) {
        playerView(Modifier.fillMaxWidth().height(screenH.dp))
    } else if (currentUri == null) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = playerCardColors()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Play audio or video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Selected media plays on this phone and is never uploaded. Format support depends on this device.", style = MaterialTheme.typography.bodySmall)
                Button(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = { openPicker() }) { Text("Open audio or video file") }
                val resume = last
                if (resume != null) {
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), onClick = {
                        try {
                            val u = android.net.Uri.parse(resume.uri)
                            error = null
                            title = resume.name
                            currentUri = resume.uri
                            player.setMediaItem(MediaItem.Builder().setUri(u).setMediaId(u.toString()).setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()).build())
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
    if (!full && !inPip) {
        if (folderFiles.isNotEmpty()) {
            Text("Folder files: ${folderFiles.size}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = folderPage > 0, onClick = { folderPage-- }) { Text("Earlier files") }
                OutlinedButton(enabled = (folderPage + 1) * 30 < folderFiles.size, onClick = { folderPage++ }) { Text("More files") }
            }
        }
        folderFiles.drop(folderPage * 30).take(30).forEachIndexed { offset, file ->
            val index = folderPage * 30 + offset
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                player.setMediaItems(folderFiles.map { f -> MediaItem.Builder().setUri(f.uri).setMediaId(f.uri.toString())
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(f.name).build()).build() }, index, 0L)
                title = file.name
                currentUri = file.uri.toString()
                player.prepare()
                player.playWhenReady = true
            }) { Text(file.name, maxLines = 2) }
        }
    }
}

@Composable
private fun UpdateSection() { UpdateCheckCard(Modifier.fillMaxWidth()) }

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    var usage by remember { mutableStateOf(UsagePing.isEnabled(context)) }
    val version = remember { Net.installed(context, context.packageName)?.second ?: "Unavailable" }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = playerCardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Netra Player", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("v$version | Netra Ecosystem. Video and music player by Prayagi Team. Your media stays on this device.", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Share anonymous usage count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Once a day the app adds 1 to a public counter so the Netra site can show roughly how many people use it. No ID, no location, no files.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = usage, onCheckedChange = { usage = it; UsagePing.setEnabled(context, it) })
            }
            Spacer(Modifier.height(2.dp))
            Text("Local playback works offline. Updates and optional reports or usage counts use internet.", style = MaterialTheme.typography.bodySmall)
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
    // Selection stays local until Apply; dismissing the sheet changes no playback state.
    var pending by remember(tracks) { mutableStateOf(player.trackSelectionParameters) }
    val select: (Int, androidx.media3.common.TrackGroup?, Int?, Boolean) -> Unit = { type, group, index, off ->
        val builder = pending.buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type, off)
        if (group != null && index != null) builder.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group, index))
        pending = builder.build()
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Audio and subtitles") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(androidx.media3.common.C.TRACK_TYPE_AUDIO to "Audio", androidx.media3.common.C.TRACK_TYPE_TEXT to "Subtitles").forEach { (type, heading) ->
                    Text(heading, fontWeight = FontWeight.Bold)
                    val groups = tracks.groups.filter { it.type == type }
                    val disabled = type in pending.disabledTrackTypes
                    val trackOverride = pending.overrides.values.firstOrNull { it.type == type }
                    var offered = 0
                    groups.forEach { group ->
                        for (index in 0 until group.length) {
                            if (group.isTrackSupported(index)) {
                                offered++
                                val format = group.getTrackFormat(index)
                                val label = trackChoiceLabel(format.label, format.language, offered)
                                val selected = !disabled && trackOverride != null && trackOverride.mediaTrackGroup == group.mediaTrackGroup && index in trackOverride.trackIndices
                                TrackChoiceRow(label, selected) { select(type, group.mediaTrackGroup, index, false) }
                            }
                        }
                    }
                    if (offered == 0) Text("Unavailable: this file has no supported $heading track.")
                    TrackChoiceRow("Automatic $heading", !disabled && trackOverride == null) { select(type, null, null, false) }
                    if (type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                        TrackChoiceRow("Subtitles off", disabled) { select(type, null, null, true) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { player.trackSelectionParameters = pending; onClose() }) { Text("Use selected tracks") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text("Cancel") } }
    )
}

@Composable
private fun TrackChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Text(label, modifier = Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
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

/** Only the app-owned notification PendingIntent can open this non-exported entry. */
class UpdateEntryActivity : MainActivity()

/** Preserve the millisecond display while visible; no clock work while the activity is stopped. */
@Composable
internal fun PlayerClockTicks(onTick: (Date) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var started by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(started) {
        if (started) while (true) {
            onTick(Date())
            delay(33)
        }
    }
}
