package com.mdreader.data.local

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

/** Reads plain-text markdown content from a content:// or file:// Uri. */
object FileReader {
    fun readText(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return "Could not open file."
        return stream.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        }
    }

    fun displayName(context: Context, uri: Uri): String {
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled.md"
    }
}
