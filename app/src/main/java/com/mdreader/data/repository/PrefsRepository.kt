package com.mdreader.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mdreader.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mdreader_prefs")

/**
 * Small wrapper around DataStore. Kept deliberately simple for the MVP:
 * theme choice, font size, and a short list of recently opened file URIs.
 */
class PrefsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val RECENT_FILES = stringSetPreferencesKey("recent_files")
    }

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
    }

    val fontSize: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.FONT_SIZE] ?: 16f
    }

    val recentFiles: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENT_FILES]?.toList() ?: emptyList()
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size }
    }

    suspend fun addRecentFile(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_FILES]?.toMutableSet() ?: mutableSetOf()
            current.add(uri)
            // Keep the list small
            prefs[Keys.RECENT_FILES] = current.toList().takeLast(10).toSet()
        }
    }
}
