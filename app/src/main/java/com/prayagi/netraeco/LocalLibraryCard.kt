package com.prayagi.netraplayer

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun LocalLibraryCard(onPlay: (List<LibraryMedia>, Int) -> Unit) {
    val c = LocalContext.current
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    var access by remember { mutableStateOf(LocalMediaLibrary.access(c)) }
    var refresh by remember { mutableIntStateOf(0) }
    var files by remember { mutableStateOf<List<LibraryMedia>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var folder by remember { mutableStateOf<String?>(null) }
    var video by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { access = LocalMediaLibrary.access(c); refresh++ }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { access = LocalMediaLibrary.access(c); refresh++ } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(access, refresh) {
        files = emptyList() // Never show stale rows after access is revoked.
        folder = null
        if (access.any) {
            busy = true; error = null
            try { files = withContext(Dispatchers.IO) { LocalMediaLibrary.scan(c, access) } }
            catch (_: SecurityException) { access = LocalMediaLibrary.access(c); error = "Media access changed. Allow access or use the file picker." }
            catch (_: IllegalStateException) { error = "Over 2000 media files. Use Open folder below to browse a smaller collection." }
            catch (_: Exception) { error = "Local library unavailable. Refresh or use the file picker." }
            finally { busy = false }
        }
    }
    Card(Modifier.fillMaxWidth(), colors = playerCardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Local library", style = MaterialTheme.typography.titleMedium)
            Text("Browse shared videos and MP3 files on this phone. Names, folders and thumbnails stay here. Android asks which media you allow; nothing is uploaded.")
            OutlinedButton(onClick = { permission.launch(LocalMediaLibrary.permissions()) }) { Text(if (access.any) "Manage media access" else "Enable local library") }
            if (!access.any) Text("Media access is off. You can still choose individual files below.")
            else {
                if (access.selectedVideo) Text("Selected videos only. Other videos are hidden until you allow them.")
                if (!access.audio) Text("MP3 access is off.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = video, onClick = { video = true; folder = null }, label = { Text("Videos") })
                    FilterChip(selected = !video, onClick = { video = false; folder = null }, label = { Text("MP3") })
                    OutlinedButton(onClick = { refresh++ }) { Text("Refresh") }
                }
                OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search local files") }, modifier = Modifier.fillMaxWidth())
                if (busy) Text("Reading permitted media...")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                val matching = files.filter { it.video == video && (search.isBlank() || it.name.contains(search, true)) }
                if (!busy && matching.isEmpty()) Text("No permitted ${if (video) "videos" else "MP3 files"} found.")
                if (folder == null) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        items(matching.groupBy { it.folder }.toSortedMap().entries.toList(), key = { it.key }) { entry ->
                            ListItem(headlineContent = { Text(entry.key) }, supportingContent = { Text("${entry.value.size} files") }, modifier = Modifier.clickable { folder = entry.key })
                        }
                    }
                } else {
                    OutlinedButton(onClick = { folder = null }) { Text("All folders") }
                    val queue = matching.filter { it.folder == folder }
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        items(queue, key = { it.uri }) { file ->
                            ListItem(headlineContent = { Text(file.name, maxLines = 2) }, supportingContent = {
                                Text(LastPlayed.formatPosition(file.durationMs) + if (file.video && file.width > 0 && file.height > 0) " | ${file.width}x${file.height}" else "")
                            }, leadingContent = { LibraryThumbnail(file) }, modifier = Modifier.clickable { onPlay(queue, queue.indexOf(file)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryThumbnail(file: LibraryMedia) {
    val c = LocalContext.current
    var bitmap by remember(file.uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file.uri) {
        bitmap = withContext(Dispatchers.IO) {
            if (!file.video) null else try {
                if (Build.VERSION.SDK_INT >= 29) c.contentResolver.loadThumbnail(Uri.parse(file.uri), Size(144, 96), null)
                else MediaStoreThumbnail.load(c, Uri.parse(file.uri))
            } catch (_: Exception) { null }
        }
    }
    val image = bitmap
    if (image != null) Image(image.asImageBitmap(), contentDescription = null, modifier = Modifier.size(72.dp, 48.dp))
    else Text(if (file.video) "Video" else "MP3", modifier = Modifier.width(72.dp))
}

private object MediaStoreThumbnail {
    fun load(c: android.content.Context, uri: Uri): Bitmap? {
        // Bounded retriever frame for API26-28, opened through the granted content URI only.
        val retriever = android.media.MediaMetadataRetriever()
        return try { retriever.setDataSource(c, uri); if (Build.VERSION.SDK_INT >= 27) retriever.getScaledFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 144, 96) else android.provider.MediaStore.Video.Thumbnails.getThumbnail(c.contentResolver, android.content.ContentUris.parseId(uri), android.provider.MediaStore.Video.Thumbnails.MINI_KIND, null) }
        finally { retriever.release() }
    }
}
