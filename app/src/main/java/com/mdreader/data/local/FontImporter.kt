package com.mdreader.data.local

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Handles importing font files (TTF/OTF) from Uri to app-private storage.
 */
class FontImporter(private val context: Context) {

    private val fontsDir: File

    init {
        fontsDir = File(context.filesDir, "fonts")
        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }
    }

    /**
     * Copies a font from the given Uri to the app's fonts directory.
     * @return the file name of the copied font, or null if failed
     */
    fun importFont(uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val fileName = getFileName(uri) ?: return null
            val outFile = File(fontsDir, fileName)
            FileOutputStream(outFile).use { output ->
                copy(input, output)
            }
            fileName
        }
    }

    /**
     * Deletes an imported font file.
     * @return true if deleted
     */
    fun deleteFont(fileName: String): Boolean {
        val file = File(fontsDir, fileName)
        return if (file.exists()) file.delete() else false
    }

    private fun getFileName(uri: Uri): String? {
        return uri.lastPathSegment?.takeIf { it.endsWith(".ttf") || it.endsWith(".otf") }
    }

    private fun copy(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
    }
}
