package com.mdreader.ui.screens

import android.content.Context
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mdreader.data.local.FileReader
import kotlin.math.ceil

@Composable
fun ReaderScreen(
    title: String,
    content: String,
    fontSizeSp: Float,
    fontName: String?,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    onWordTap: (String, Int) -> Unit, // For TTS and AI
    ttsServiceBinder: Any? // Binder to TtsService, null if not bound
) {
    var webViewState by remember { mutableStateOf(WebViewState()) }
    var showToc by remember { mutableStateOf(false) }
    var showWordToolbar by remember { mutableStateOf(false) }
    var wordToolbarState by remember { mutableStateOf(WordToolbarState()) }
    var ttsIsPlaying by remember { mutableStateOf(false) }
    var showTtsControls by remember { mutableStateOf(true) } // Show in top bar when not fullscreen?

    val context = LocalContext.current
    val webView = remember { WebView(context) }

    // Configure WebView settings
    DisposableEffect(webView) {
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }
        webView.setWebChromeClient(WebChromeClient())
        webView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // Let WebView handle the request
            }
        })
        // Add JavaScript interface for word taps
        webView.addJavascriptInterface(
            WordTapListener(context) { word, wordIndex ->
                // Run on UI thread
                onWordTap(word, wordIndex)
                showWordToolbar = true
                wordToolbarState = WordToolbarState(word, wordIndex)
            },
            "WordTapInterface"
        )
        onDispose {
            webView.destroy()
        }
    }

    // Convert markdown to HTML with Markwon (simplified - in real code we'd use Markwon properly)
    // For now, we'll just wrap in <pre> but ideally we'd use Markwon to HTML.
    // Since we don't have Markwon setup in this snippet, we'll use a placeholder.
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { 
                    font-family: '$fontName', sans-serif; 
                    font-size: $fontSizeSp pt;
                    margin: 20px;
                    color: #000;
                    background-color: #fff;
                }
                /* TODO: Add proper styling from theme */
                h1, h2, h3, h4, h5, h6 {
                    scroll-margin-top: 20px; /* For TOC scrolling */
                }
                /* Highlight the current TTS word? Not implemented yet */
            </style>
        </head>
        <body>
            <!-- In a real implementation, we would use Markwon to convert markdown to HTML -->
            <!-- and add ids to headings for TOC -->
            <div id="content">
                $content
            </div>
        </body>
        </html>
    """.trimIndent()

    // Load the HTML into WebView
    LaunchedEffect(content, fontSizeSp, fontName) {
        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // TOC button
                        IconButton(onClick = { showToc = true }) {
                            Icon(Icons.Filled.Description, contentDescription = "Table of Contents")
                        }
                        // Word toolbar is floating, not in top bar
                        if (!isFullscreen && showTtsControls) {
                            // TTS controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { /* Toggle TTS play/pause */ },
                                    enabled = ttsServiceBinder != null
                                ) {
                                    if (ttsIsPlaying) {
                                        Icon(Icons.Filled.Pause, contentDescription = "Pause")
                                    } else {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                                    }
                                }
                                IconButton(
                                    onClick = { /* Stop TTS */ },
                                    enabled = ttsServiceBinder != null
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = "Stop")
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        // Main content: WebView
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )

        // Floating word toolbar (appears when a word is tapped)
        if (showWordToolbar) {
            WordToolbar(
                state = wordToolbarState,
                onDismiss = { showWordToolbar = false },
                onRead = { /* Start TTS from this word */ },
                onAi = { /* Open AI menu with this word */ }
            )
        }

        // TOC Drawer
        if (showToc) {
            ModalDrawerSheet(
                showToc = showToc,
                onDismiss = { showToc = false },
                title = "Table of Contents",
                // TODO: Generate TOC from headings in the markdown
                tocItems = emptyList() // Placeholder
            ) { heading ->
                // When a TOC item is clicked, scroll to that heading in WebView
                // webView.evaluateJavascript("document.getElementById('$heading.anchor').scrollIntoView(true)")
            }
        }
    }
}

// Data classes for state
data class WebViewState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class WordToolbarState(
    val word: String = "",
    val wordIndex: Int = -1
)

// Placeholder for TOC item
data class TocItem(
    val level: Int,
    val title: String,
    val anchor: String
)

// Placeholder for ModalDrawerSheet (we'll implement later)
@Composable
fun ModalDrawerSheet(
    showToc: Boolean,
    onDismiss: () -> Unit,
    title: String,
    tocItems: List<TocItem>,
    onItemClick: (TocItem) -> Unit
) {
    // TODO: Implement actual drawer
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() }
    ) {
        // Dismiss on tap outside
        Text("TOC Placeholder", modifier = Modifier.align(Alignment.Center))
    }
}

// Placeholder for WordToolbar
@Composable
fun WordToolbar(
    state: WordToolbarState,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onAi: () -> Unit
) {
    // TODO: Implement actual toolbar
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = 16.dp, y = 16.dp)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(8.dp)
    ) {
        Column {
            Text("Word: ${state.word}")
            Button(onClick = { onRead(); onDismiss() }) { Text("Read") }
            Button(onClick = { onAi(); onDismiss() }) { Text("AI") }
            Button(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
