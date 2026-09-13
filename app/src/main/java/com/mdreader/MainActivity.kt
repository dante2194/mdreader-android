package com.mdreader

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mdreader.data.local.FileReader
import com.mdreader.data.repository.PrefsRepository
import com.mdreader.ui.screens.LibraryScreen
import com.mdreader.ui.screens.ReaderScreen
import com.mdreader.ui.screens.SettingsScreen
import com.mdreader.ui.theme.MDReaderTheme
import kotlinx.coroutines.launch

private sealed class Screen {
    data object Library : Screen()
    data class Reader(val uri: Uri) : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true) // default: normal chrome, fullscreen is opt-in
        prefs = PrefsRepository(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()
            val theme by prefs.theme.collectAsState(initial = com.mdreader.ui.theme.AppTheme.SYSTEM)
            val fontSize by prefs.fontSize.collectAsState(initial = 16f)
            val recentFiles by prefs.recentFiles.collectAsState(initial = emptyList())

            var screen by remember { mutableStateOf<Screen>(Screen.Library) }
            // Fullscreen starts OFF every time a document is opened, by design.
            var isFullscreen by remember { mutableStateOf(false) }

            val openDocument = rememberLauncherForOpenDocument { uri ->
                if (uri != null) {
                    scope.launch { prefs.addRecentFile(uri.toString()) }
                    isFullscreen = false
                    screen = Screen.Reader(uri)
                }
            }

            // Push actual system status/nav bar visibility to match the in-app toggle.
            LaunchedEffect(isFullscreen) {
                applyFullscreen(isFullscreen)
            }

            MDReaderTheme(appTheme = theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val current = screen) {
                        is Screen.Library -> LibraryScreen(
                            recentFiles = recentFiles,
                            onOpenFilePicker = { openDocument.launch(arrayOf("text/markdown", "text/plain", "*/*")) },
                            onOpenRecent = { uriString ->
                                screen = Screen.Reader(Uri.parse(uriString))
                            },
                            onOpenSettings = { screen = Screen.Settings },
                        )

                        is Screen.Reader -> {
                            val content = remember(current.uri) {
                                FileReader.readText(this@MainActivity, current.uri)
                            }
                            val title = remember(current.uri) {
                                FileReader.displayName(this@MainActivity, current.uri)
                            }
                            ReaderScreen(
                                title = title,
                                content = content,
                                fontSizeSp = fontSize,
                                isFullscreen = isFullscreen,
                                onToggleFullscreen = { isFullscreen = !isFullscreen },
                                onBack = {
                                    isFullscreen = false
                                    screen = Screen.Library
                                },
                            )
                        }

                        is Screen.Settings -> SettingsScreen(
                            currentTheme = theme,
                            fontSize = fontSize,
                            onThemeChange = { newTheme -> scope.launch { prefs.setTheme(newTheme) } },
                            onFontSizeChange = { newSize -> scope.launch { prefs.setFontSize(newSize) } },
                            onBack = { screen = Screen.Library },
                        )
                    }
                }
            }
        }
    }

    /** Hides/shows the system status + navigation bars for edge-to-edge reading. */
    private fun applyFullscreen(enabled: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !enabled)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@androidx.compose.runtime.Composable
private fun rememberLauncherForOpenDocument(
    onResult: (Uri?) -> Unit
) = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = onResult,
)
