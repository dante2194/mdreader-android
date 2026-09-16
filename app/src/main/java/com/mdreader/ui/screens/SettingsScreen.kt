package com.mdreader.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mdreader.data.fonts.FontInfo
import com.mdreader.data.fonts.FontRegistry
import com.mdreader.data.repository.PrefsRepository
import com.mdreader.ui.theme.AppTheme
import com.mdreader.ui.theme.MDReaderTheme

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    fontSize: Float,
    fontName: String?,
    onThemeChange: (AppTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontNameChange: (String?) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            AppTheme.entries.forEach { theme ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = theme == currentTheme,
                            onClick = { onThemeChange(theme) }
                        )
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = theme == currentTheme, onClick = { onThemeChange(theme) })
                    Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Font size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = fontSize,
                onValueChange = onFontSizeChange,
                valueRange = 12f..32f,
                steps = 19,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Font", style = MaterialTheme.typography.titleMedium)
            // Font picker
            val context = LocalContext.current
            val fontRegistry = remember { FontRegistry(context) }
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items = fontRegistry.bundledFonts) { font ->
                    FontRow(
                        font = font,
                        isSelected = font.name == fontName,
                        onClick = { onFontNameChange(font.name) }
                    )
                }
                // Add a section for imported fonts (placeholder)
                item {
                    Text("Imported fonts", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(items = listOf<FontInfo>()) { font -> // TODO: load imported fonts from registry
                    FontRow(
                        font = font,
                        isSelected = font.name == fontName,
                        onClick = { onFontNameChange(font.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Placeholder for AI settings
            Text("AI Settings", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { /* TODO: Open AI settings */ }) {
                Text("Configure AI (OpenRouter)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Placeholder for TTS settings
            Text("Text-to-Speech", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { /* TODO: Open TTS settings */ }) {
                Text("Configure TTS")
            }
        }
    }
}

@Composable
private fun FontRow(
    font: FontInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        // Show a sample of the font
        Text(
            text = font.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 12.dp)
        )
        // Preview text in the font
        val previewFontFamily = if (font.name == "System") null else FontFamily.Serif
        Text(
            text = "The quick brown fox",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = previewFontFamily),
            modifier = Modifier.weight(1f, fill = false)
        )
        RadioButton(selected = isSelected, onClick = { onClick() })
    }
}
