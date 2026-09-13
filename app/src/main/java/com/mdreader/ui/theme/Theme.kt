package com.mdreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material 3 palette, taken from the design proposal
private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    surface = Color(0xFFFEF7FF),
    background = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    surface = Color(0xFF141218),
    background = Color(0xFF1C1B1F),
)

enum class AppTheme { LIGHT, DARK, SYSTEM }

@Composable
fun MDReaderTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}
