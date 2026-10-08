package com.prayagi.netraplayer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

internal data class FolderMedia(val uri: Uri, val name: String)

/** Lists the chosen folder only, not the whole device. Runs off the UI thread. */
internal fun listFolderMedia(context: Context, tree: Uri): List<FolderMedia> {
    val parentId = DocumentsContract.getTreeDocumentId(tree)
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
    val result = mutableListOf<FolderMedia>()
    context.contentResolver.query(children, arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val name = cursor.getString(1) ?: "Selected file"
            val mime = cursor.getString(2)
            if (isVideoOrMp3(mime, name)) {
                if (result.size >= 1000) throw java.io.IOException("Folder too large")
                result += FolderMedia(DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)), name)
            }
        }
    } ?: throw java.io.IOException("Folder unavailable")
    return result.sortedBy { it.name.lowercase(java.util.Locale.ROOT) }
}
