package com.mdreader.ui.screens

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat

/**
 * JavaScript interface that receives word tap events from the WebView.
 */
class WordTapListener(
    private val context: Context,
    private val onWordTap: (String, Int) -> Unit // word, wordIndex
) {

    @JavascriptInterface
    fun onWordTap(word: String, wordIndex: Int) {
        // Called from JS thread, need to post to UI thread if we want to show UI
        // We'll handle threading in the composable
        onWordTap(word, wordIndex)
    }
}
