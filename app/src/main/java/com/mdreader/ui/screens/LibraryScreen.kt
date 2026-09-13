package com.mdreader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                        ListItem(
                            headlineContent = { Text(uriString.substringAfterLast('/')) },
                            leadingContent = {
                                Icon(Icons.Filled.Description, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenRecent(uriString) }
                        )
                    }
                }
            }
        }
    }
}
