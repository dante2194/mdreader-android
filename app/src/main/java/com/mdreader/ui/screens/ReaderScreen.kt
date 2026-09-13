package com.mdreader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays the loaded markdown as plain styled text.
 *
 * Fullscreen is a toggle in the top bar, OFF by default — tapping it hides
 * the app bar for an edge-to-edge reading view; tapping again restores it.
 * (Hiding the Android system status/nav bars is wired up from MainActivity
 * via the onToggleFullscreen callback, since that's a platform-level call.)
 */
@Composable
fun ReaderScreen(
    title: String,
    content: String,
    fontSizeSp: Float,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            // Entirely hidden in fullscreen mode so the text takes the full screen.
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(title, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = "Enter fullscreen")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = content,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.5f).sp,
                    )
                }
            }

            // Small floating control to exit fullscreen without needing the app bar back.
            if (isFullscreen) {
                FilledTonalIconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit fullscreen")
                }
            }
        }
    }
}
