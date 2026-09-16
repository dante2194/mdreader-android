package com.mdreader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mdreader.data.repository.PrefsRepository
import kotlin.math.ceil

/**
 * Home screen: a button to open a markdown file, and a simple list
 * of recently opened files below it.
 */
@Composable
fun LibraryScreen(
    recentFiles: List<String>,
    onOpenFilePicker: () -> Unit,
    onOpenRecent: (String) -> Unit,
    onOpenSettings: () -> Unit,
    prefs: PrefsRepository // Passed to get progress
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MD Reader") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenFilePicker,
                icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                text = { Text("Open file") },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (recentFiles.isEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "No files yet — tap \"Open file\" to load a Markdown document.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text("Recent", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(recentFiles.reversed()) { uriString ->
                        var progress by remember(uriString) { mutableStateOf<Pair<Int, Int>?>(null) }
                        LaunchedEffect(uriString) { progress = prefs.getRecentFileProgress(uriString) }
                        val (wordOffset, totalWords) = progress ?: Pair(0, 0)
                        val percentRead = if (totalWords > 0) (wordOffset * 100 / totalWords) else 0
                        val pagesRead = ceil(wordOffset / 400.0).toInt()
                        val totalPages = ceil(totalWords / 400.0).toInt()
                        RecentFileItem(
                            uriString = uriString,
                            wordOffset = wordOffset,
                            totalWords = totalWords,
                            percentRead = percentRead,
                            pagesRead = pagesRead,
                            totalPages = totalPages,
                            onClick = { onOpenRecent(uriString) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentFileItem(
    uriString: String,
    wordOffset: Int,
    totalWords: Int,
    percentRead: Int,
    pagesRead: Int,
    totalPages: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = uriString.substringAfterLast('/'),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Page $pagesRead of $totalPages • $percentRead% read",
                style = MaterialTheme.typography.bodySmall
            )
        }
        // Progress bar
        LinearProgressIndicator(
            modifier = Modifier
                .width(120.dp)
                .height(4.dp),
            progress = percentRead / 100f
        )
        // Page info
        Text(
            text = "$pagesRead/$totalPages",
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
