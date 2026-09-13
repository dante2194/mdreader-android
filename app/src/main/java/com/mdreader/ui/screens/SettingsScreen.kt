package com.mdreader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mdreader.ui.theme.AppTheme

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    fontSize: Float,
    onThemeChange: (AppTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onBack: () -> Unit,
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
        }
    }
}
