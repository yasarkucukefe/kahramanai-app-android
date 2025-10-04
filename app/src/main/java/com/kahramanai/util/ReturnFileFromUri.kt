package com.kahramanai.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun getFileFromUri(context: Context, uri: Uri): File? {
    // 1. Create a temporary file in the cache directory
    val fileExtension = getFileExtension(context, uri)
    val fileName = "temp_file" + if (fileExtension != null) ".$fileExtension" else ""
    val tempFile = File(context.cacheDir, fileName)
    tempFile.createNewFile()

    try {
        // 2. Open an InputStream from the URI
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(tempFile)

        // 3. Copy the content from the InputStream to the temporary file
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null // Handle exceptions
    }

    return tempFile
}

// Helper function to get the file extension from a URI
private fun getFileExtension(context: Context, uri: Uri): String? {
    var extension: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    val displayName = cursor.getString(displayNameIndex)
                    if (displayName != null) {
                        val dotIndex = displayName.lastIndexOf('.')
                        if (dotIndex != -1) {
                            extension = displayName.substring(dotIndex + 1)
                        }
                    }
                }
            }
        }
    }
    return extension
}