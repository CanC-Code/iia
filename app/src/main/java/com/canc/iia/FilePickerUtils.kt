package com.canc.iia

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FilePickerUtils {

    /**
     * C++ needs a raw file path, but Android gives us a URI.
     * This function copies the model to the app's internal cache 
     * so the C++ engine can "see" it.
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor?.moveToFirst()
        val name = returnCursor?.getString(nameIndex ?: 0)
        returnCursor?.close()

        val file = File(context.cacheDir, name ?: "temp_model.gguf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
