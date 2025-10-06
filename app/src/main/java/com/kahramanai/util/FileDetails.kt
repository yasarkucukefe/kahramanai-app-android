package com.kahramanai.util

import android.net.Uri
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

fun getFileDetailsFromUri(uri: Uri): FileDetails {

    // Get the MIME type
    var mimeType: String? = null

    // Get the file size
    var fileSize: Long? = null
    try {
        uri.path?.let {
            val file = File(it)
            if (file.exists()) {
                fileSize = file.length()
            }
        }

        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        if (fileExtension != null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(
                Locale.ROOT))
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
