package com.mdreader

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
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
import com.mdreader.data.tts.TtsService
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
    private var ttsServiceBinder: TtsService.LocalBinder? = null
    private var ttsServiceConnection: ServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        prefs = PrefsRepository(applicationContext)

        // Bind to TTS service
        bindTtsService()

        setContent {
            val scope = rememberCoroutineScope()
            val theme by prefs.theme.collectAsState(initial = com.mdreader.ui.theme.AppTheme.SYSTEM)
            val fontSize by prefs.fontSize.collectAsState(initial = 16f)
            val fontName by prefs.fontName.collectAsState(initial = null)
            val recentFiles by prefs.recentFiles.collectAsState(initial = emptyList())

            var screen by remember { mutableStateOf<Screen>(Screen.Library) }
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
                            prefs = prefs
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
                                fontName = fontName,
                                isFullscreen = isFullscreen,
                                onToggleFullscreen = { isFullscreen = !isFullscreen },
                                onBack = {
                                    isFullscreen = false
                                    screen = Screen.Library
                                },
                                onWordTap = { word, wordIndex ->
                                    // Handle word tap: show toolbar (already handled in ReaderScreen via callback)
                                    // We'll also save the word index for TTS start
                                    // For now, just log
                                    Log.d("MainActivity", "Word tapped: $word at index $wordIndex")
                                },
                                ttsServiceBinder = ttsServiceBinder
                            )
                        }

                        is Screen.Settings -> SettingsScreen(
                            currentTheme = theme,
                            fontSize = fontSize,
                            fontName = fontName,
                            onThemeChange = { newTheme -> scope.launch { prefs.setTheme(newTheme) } },
                            onFontSizeChange = { newSize -> scope.launch { prefs.setFontSize(newSize) } },
                            onFontNameChange = { newName -> scope.launch { prefs.setFontName(newName) } },
                            onBack = { screen = Screen.Library }
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

    private fun bindTtsService() {
        ttsServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                ttsServiceBinder = service as? TtsService.LocalBinder
                Log.d("MainActivity", "TTS service connected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                ttsServiceBinder = null
                Log.d("MainActivity", "TTS service disconnected")
            }
        }
        val intent = Intent(this, TtsService::class.java)
        bindService(intent, ttsServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsServiceConnection?.let { unbindService(it) }
    }
}
