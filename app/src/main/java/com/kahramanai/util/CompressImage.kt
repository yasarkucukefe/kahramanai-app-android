package com.kahramanai.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

const val MAX_FILE_SIZE_MB = 1
const val TARGET_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024

fun compressImage(context: Context, originalUri: Uri): Uri? {
    // First, check the original file size
    val originalFileSize = getFileSizeFromUri(originalUri)
    if (originalFileSize != null) {
        if (originalFileSize <= TARGET_FILE_SIZE_BYTES) {
            // If the file is already small enough, just return the original URI
            return originalUri
        }
    }

    // Decode the image to a Bitmap
    val originalBitmap = uriToBitmap(context, originalUri) ?: return null

    val rotatedBitmap = rotateImageIfRequired(context, originalBitmap, originalUri)

    val outputStream = ByteArrayOutputStream()
    var quality = 90 // Start with a high quality

    // Loop to find the right quality to get the size under 1MB
    do {
        outputStream.reset() // Reset the stream for each compression attempt
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        quality -= 5 // Decrease quality by 5 in each iteration
    } while (outputStream.size() > TARGET_FILE_SIZE_BYTES && quality > 10)

    // Save the compressed bitmap to a new file in the cache directory
    return try {
        val outputFile = File(context.cacheDir, "compressed_image_${System.currentTimeMillis()}.jpg")
        val fileOutputStream = FileOutputStream(outputFile)
        fileOutputStream.write(outputStream.toByteArray())
        fileOutputStream.flush()
        fileOutputStream.close()

        // Get the URI for the new file using FileProvider
        FileProvider.getUriForFile(context, "${context.packageName}.provider", outputFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
    val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return bitmap

    val exif = ExifInterface(inputStream)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        // You can also handle flipping cases here if needed
        else -> return bitmap // No rotation needed
    }

    // Create a new bitmap from the original bitmap, applying the rotation matrix
    // and recycle the old bitmap if it's not the same as the new one.
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotatedBitmap != bitmap) {
        bitmap.recycle()
    }
    return rotatedBitmap
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getFileSizeFromUri( uri: Uri): Long? {
    var fileSize: Long? = 0
    try {
        uri.path?.let {
            val file = File(it)
            if (file.exists()) {
                fileSize = file.length()
            }
        }
    } catch (e: Exception) {
        // Handle the exception
        println("Error ${e.message}")
    }
    return fileSize
}