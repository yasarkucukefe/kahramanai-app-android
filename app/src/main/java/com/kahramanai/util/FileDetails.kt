package com.kahramanai.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.content.Context
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale


data class FileDetails(val mimeType: String?, val fileSize: Long?)

/**
 * Retrieves the MIME type and file size from a given Uri.
 *
 * @param context The context to access the ContentResolver.
 * @param uri The Uri of the file to inspect.
 * @return A FileDetails object containing the mimeType and fileSize, or null for values that couldn't be determined.
 */

fun getFileDetailsFromUri(context: Context, uri: Uri): FileDetails {

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
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(
                Locale.ROOT))
        }

    } catch (e: Exception) {
        // Handle the exception
        println("Error ${e.message}")
    }

    return FileDetails(mimeType, fileSize)
}
