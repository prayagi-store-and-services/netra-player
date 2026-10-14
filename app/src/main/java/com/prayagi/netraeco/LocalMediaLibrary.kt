package com.prayagi.netraplayer

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat

data class LibraryMedia(val uri: String, val name: String, val folder: String, val durationMs: Long, val width: Int, val height: Int, val video: Boolean, val folderKey: String = folder)

internal data class LibraryAccess(val video: Boolean, val audio: Boolean, val selectedVideo: Boolean) {
    val any: Boolean get() = video || audio || selectedVideo
}

/** Queries only Android's permitted shared media rows. No filesystem crawl or network. */
internal object LocalMediaLibrary {
    fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    fun access(c: Context): LibraryAccess {
        fun has(p: String) = ContextCompat.checkSelfPermission(c, p) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT < 33) return has(Manifest.permission.READ_EXTERNAL_STORAGE).let { LibraryAccess(it, it, false) }
        val video = has(Manifest.permission.READ_MEDIA_VIDEO)
        return LibraryAccess(video, has(Manifest.permission.READ_MEDIA_AUDIO), Build.VERSION.SDK_INT >= 34 && !video && has(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
    }
    fun scan(c: Context, access: LibraryAccess): List<LibraryMedia> {
        val result = mutableListOf<LibraryMedia>()
        fun query(video: Boolean) {
            val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val columns = mutableListOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DURATION)
            val relative = Build.VERSION.SDK_INT >= 29
            columns += if (relative) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
            if (video) columns += listOf(MediaStore.Video.Media.WIDTH, MediaStore.Video.Media.HEIGHT)
            val where = if (video) null else "${MediaStore.MediaColumns.MIME_TYPE} IN (?, ?)"
            val args = if (video) null else arrayOf("audio/mpeg", "audio/mp3")
            c.contentResolver.query(base, columns.toTypedArray(), where, args, "${MediaStore.MediaColumns.DISPLAY_NAME} ASC")?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (result.size >= 2000) throw IllegalStateException("Library exceeds 2000 files; use folder picker")
                    val path = cursor.getString(3).orEmpty()
                    // Do not retain absolute storage paths, including on pre-scoped-storage Android.
                    val folder = if (relative) path.trimEnd('/').ifBlank { "Shared media" } else path.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { "Shared media" }
                    result += LibraryMedia(uri = ContentUris.withAppendedId(base, cursor.getLong(0)).toString(), name = cursor.getString(1) ?: "Unnamed media", durationMs = cursor.getLong(2).coerceAtLeast(0), folder = folder, width = if (video) cursor.getInt(4) else 0, height = if (video) cursor.getInt(5) else 0, video = video, folderKey = java.security.MessageDigest.getInstance("SHA-256").digest(path.substringBeforeLast('/', "").toByteArray()).joinToString("") { "%02x".format(it) })
                }
            }
        }
        if (access.video || access.selectedVideo) query(true)
        if (access.audio) query(false)
        return result
    }
}
