package com.kahramanai.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream

fun getFileFromUri(context: Context, uri: Uri): File? {
    // 1. Create a temporary file in the cache directory with a unique name
    val fileExtension = getFileExtension(context, uri)
    val uniqueSuffix = System.currentTimeMillis() + (Math.random() * 1000).toLong()
    val fileName = "upload_${uniqueSuffix}" + if (fileExtension != null) ".$fileExtension" else ".jpg"
    val tempFile = File(context.cacheDir, fileName)
    
    try {
        // 2. Handle file:// URIs directly (from camera capture)
        if (uri.scheme == "file" || uri.scheme == null) {
            val sourceFile = File(uri.path ?: return null)
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                return null
            }
            // Copy directly from file to file, ensuring we read fresh data
            sourceFile.inputStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            // Verify the copy was successful
            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return null
            }
        } else {
            // 3. For content:// URIs, use ContentResolver
            // First, try to verify the file exists and has content via ContentResolver
            val fileSize = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            cursor.getLong(sizeIndex)
                        } else null
                    } else null
                }
            } catch (e: Exception) {
                null
            }
            
            // If we got a size, verify it's not zero
            if (fileSize != null && fileSize == 0L) {
                return null
            }
            
            val inputStream: InputStream? = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                null
            }
            
            if (inputStream == null) {
                return null
            }
            
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            
            // Verify the copy was successful and has content
            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // Clean up temp file on error
        tempFile.delete()
        return null
    }

    return tempFile
}

// Helper function to get the file extension from a URI
private fun getFileExtension(context: Context, uri: Uri): String? {
    // Handle file:// URIs (from camera capture)
    if (uri.scheme == "file" || uri.scheme == null) {
        val path = uri.path
        if (path != null) {
            val dotIndex = path.lastIndexOf('.')
            if (dotIndex != -1 && dotIndex < path.length - 1) {
                return path.substring(dotIndex + 1)
            }
        }
        return null
    }
    
    // Handle content:// URIs
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    val displayName = cursor.getString(displayNameIndex)
                    if (displayName != null) {
                        val dotIndex = displayName.lastIndexOf('.')
                        if (dotIndex != -1) {
                            return displayName.substring(dotIndex + 1)
                        }
                    }
                }
            }
        }
    }
    return null
}