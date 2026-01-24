package com.kahramanai.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale


data class FileDetails(val mimeType: String?, val fileSize: Long?)

/**
 * Retrieves the MIME type and file size from a given Uri.
 *
 * @param uri The Uri of the file to inspect.
 * @return A FileDetails object containing the mimeType and fileSize, or null for values that couldn't be determined.
 */

fun getFileDetailsFromUri(context: Context, uri: Uri): FileDetails {

    // Get the MIME type
    var mimeType: String? = null

    // Get the file size
    var fileSize: Long? = null
    try {
        // Handle file:// URIs (from camera capture)
        if (uri.scheme == "file" || uri.scheme == null) {
            val path = uri.path
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    fileSize = file.length()
                }
            }
        } else if (uri.scheme == "content") {
            // Handle content:// URIs (from FileProvider, etc.)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0) {
                            fileSize = size
                        }
                    }
                }
            }
            // If ContentResolver didn't provide size, try reading from file path
            if (fileSize == null) {
                uri.path?.let {
                    val file = File(it)
                    if (file.exists()) {
                        fileSize = file.length()
                    }
                }
            }
        } else {
            // Fallback: try to read from path
            uri.path?.let {
                val file = File(it)
                if (file.exists()) {
                    fileSize = file.length()
                }
            }
        }

        // Get MIME type from file extension or ContentResolver
        if (uri.scheme == "content") {
            mimeType = context.contentResolver.getType(uri)
        }
        
        // Fallback to extension-based MIME type
        if (mimeType == null) {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (fileExtension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(
                    Locale.ROOT))
            }
        }

    } catch (e: Exception) {
        // Handle the exception
        println("Error ${e.message}")
    }

    return FileDetails(mimeType, fileSize)
}

fun deleteFileFromUri(fileUri: Uri): Boolean {
    fileUri.path?.let { path ->
        val file = File(path)
        if (file.exists()) {
            return file.delete()
        }
    }
    return false
}
