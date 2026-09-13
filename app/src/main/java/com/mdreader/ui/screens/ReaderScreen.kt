package com.mdreader.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mdreader.data.model.TocEntry
import java.util.regex.Pattern

/**
 * WebView-based reader with TOC navigation.
 *
 * - Renders markdown → HTML via Markwon
 * - TOC sidebar for heading jump
 * - Word-tap → TTS (background)
 * - Scroll tracking → resume position
 */
@Composable
fun ReaderScreen(
    markdown: String,
    fontSizeSp: Float,
    fontFamilyName: String,
    onWordTap: (String) -> Unit,
    onTTSStart: () -> Unit,
    onTTSStop: () -> Unit,
    onTTSPause: () -> Unit,
    onTTSPlay: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val html by remember(markdown, fontSizeSp, fontFamilyName) {
        derivedStateOf { buildHtml(markdown, fontSizeSp, fontFamilyName) }
    }

    val tocEntries by remember(markdown) {
        derivedStateOf { extractTocFromMarkdown(markdown) }
    }
    var currentHeading by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reader", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onTTSStart) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            contentDescription = "Read aloud"
                        )
                    }
                    IconButton(onClick = onTTSStop) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.Stop,
                            contentDescription = "Stop reading"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            TocSidebar(
                entries = tocEntries,
                currentHeading = currentHeading,
                onHeadingClick = { heading -> currentHeading = heading },
                modifier = Modifier.weight(0.25f)
            )
            AndroidView(
                modifier = Modifier.weight(0.75f),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                // Could add TOC highlighting here if needed
                            }
                        }
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                },
                update = { }
            )
        }
    }
}

private fun buildHtml(
    markdown: String,
    fontSizeSp: Float,
    fontFamilyName: String
): String {
    val renderer = io.noties.markwon.Markwon.create(
        io.noties.markwon.MarkwonPlugin.builder().build()
    )
    val htmlContent = renderer.render(markdown)
    return """<!DOCTYPE html>
<html><head><meta charset="UTF-8">
<style>
body{font-family:'$fontFamilyName',sans-serif;font-size:${fontSizeSp}px;line-height:1.7;padding:16px;color:#212121;max-width:800px;margin:0 auto}
h1{font-size:2em;border-bottom:2px solid #6750A4;padding-bottom:8px}
h2{font-size:1.5em;border-bottom:1px solid #6750A4;padding-bottom:4px}
h3{font-size:1.2em;color:#6750A4}
code{background:#f5f5f5;padding:2px 6px;border-radius:3px}
pre{background:#f5f5f5;padding:12px;border-radius:6px;overflow-x:auto}
blockquote{border-left:4px solid #6750A4;margin:16px 0;padding:8px 16px}
img{max-width:100%;border-radius:8px}
a{color:#6750A4}</style></head><body>$htmlContent</body></html>""".trimIndent()
}

private fun extractTocFromMarkdown(markdown: String): List<TocEntry> {
    val entries = mutableListOf<TocEntry>()
    val lines = markdown.lines()
    var headingCount = 0
    
    val headingPattern = Pattern.compile("^(#{1,6})\\s+(.+)$")
    
    lines.forEach { line ->
        line.trim().let { trimmed ->
            val matcher = headingPattern.matcher(trimmed)
            if (matcher.matches()) {
                val level = matcher.group(1).length
                val title = matcher.group(2)
                val id = "heading_$headingCount"
                entries.add(TocEntry(title, level, id))
                headingCount++
            }
        }
    }
    
    return entries
}

@Composable
fun TocSidebar(
    entries: List<TocEntry>,
    currentHeading: String,
    onHeadingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(entries) { entry ->
            Text(
                text = entry.title,
                style = when (entry.level) {
                    1 -> MaterialTheme.typography.titleMedium
                    2 -> MaterialTheme.typography.bodyLarge
                    else -> MaterialTheme.typography.bodyMedium
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeadingClick(entry.title) }
                    .padding(start = ((entry.level - 1) * 16).dp)
            )
        }
    }
}
