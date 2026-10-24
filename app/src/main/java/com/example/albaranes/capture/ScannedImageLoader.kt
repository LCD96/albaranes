package com.example.albaranes.capture

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.IOException

/**
 * Decodes the scanner output into a full-resolution [Bitmap].
 *
 * The image is kept at its original size because recognition will work on it later; the scanner
 * already writes it upright, so no EXIF rotation is applied. Call from a background thread.
 */
object ScannedImageLoader {

    fun load(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val stream = contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open $uri")
        return stream.use { BitmapFactory.decodeStream(it) }
            ?: throw IOException("Cannot decode $uri")
    }
}
