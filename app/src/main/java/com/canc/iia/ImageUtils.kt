package com.canc.iia

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream
import java.nio.ByteBuffer

object ImageUtils {

    /**
     * Converts raw RGB ByteArray from C++ to an Android Bitmap.
     * stable-diffusion.cpp usually outputs 3 channels (RGB).
     */
    fun decodeRGB(data: ByteArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val buffer = ByteBuffer.allocate(width * height * 4) // 4 bytes for ARGB

        var i = 0
        while (i < data.size) {
            val r = data[i].toInt() and 0xFF
            val g = data[i + 1].toInt() and 0xFF
            val b = data[i + 2].toInt() and 0xFF
            
            // Pack into ARGB_8888 format
            buffer.put(b.toByte()) // Blue
            buffer.put(g.toByte()) // Green
            buffer.put(r.toByte()) // Red
            buffer.put(255.toByte()) // Alpha (Opaque)
            i += 3
        }
        buffer.flip()
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    /**
     * Saves the generated Bitmap to the public Gallery (Pictures/IIA_Outputs).
     */
    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "IIA_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/IIA_Outputs")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            fos = resolver.openOutputStream(uri)
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
        return imageUri
    }
}
