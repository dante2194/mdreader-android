package com.mdreader.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mdreader.ui.theme.AppTheme
import com.mdreader.data.ai.AiPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "mdreader_prefs")

/**
 * Small wrapper around DataStore. Now includes:
 * theme choice, font size, imported fonts, AI settings, and per-file progress.
 */
class PrefsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val FONT_NAME = stringPreferencesKey("font_name") // name of the selected font
        val IMPORTED_FONTS = stringSetPreferencesKey("imported_fonts") // set of file names
        val API_KEY = stringPreferencesKey("api_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val SELECTED_MODEL_NAME = stringPreferencesKey("selected_model_name")
        val AI_PRESETS = stringPreferencesKey("ai_presets") // JSON list of AiPreset
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val TTS_BACKGROUND = intPreferencesKey("tts_background") // 0=off, 1=on
        val RECENT_FILES = stringSetPreferencesKey("recent_files")
    }

    // Existing flows
    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
    }

    val fontSize: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.FONT_SIZE] ?: 16f
    }

    val fontName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.FONT_NAME]
    }

    val importedFonts: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.IMPORTED_FONTS] ?: emptySet()
    }

    val recentFiles: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENT_FILES]?.toList() ?: emptyList()
    }

    val apiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.API_KEY]
    }

    val selectedModel: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_MODEL]
    }

    val selectedModelName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_MODEL_NAME]
    }

    val aiPresets: Flow<List<AiPreset>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.AI_PRESETS]
        if (json == null || json.isEmpty()) defaultPresets()
        else {
            try {
                Json.decodeFromString<List<AiPreset>>(json)
            } catch (e: Exception) {
                defaultPresets()
            }
        }
    }

    val ttsRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.TTS_RATE] ?: 1.0f
    }

    val ttsPitch: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.TTS_PITCH] ?: 1.0f
    }

    val ttsBackground: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.TTS_BACKGROUND] == 1
    }

    // --- Setters ---

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size }
    }

    suspend fun setFontName(name: String?) {
        context.dataStore.edit { it[Keys.FONT_NAME] = name }
    }

    suspend fun setImportedFont(fileName: String, add: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.IMPORTED_FONTS]?.toMutableSet() ?: mutableSetOf()
            if (add) {
                current.add(fileName)
            } else {
                current.remove(fileName)
            }
            prefs[Keys.IMPORTED_FONTS] = current
        }
    }

    suspend fun addRecentFile(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_FILES]?.toMutableSet() ?: mutableSetOf()
            current.add(uri)
            prefs[Keys.RECENT_FILES] = current
        }
    }

    suspend fun setApiKey(key: String?) {
        context.dataStore.edit { it[Keys.API_KEY] = key }
    }

    suspend fun setSelectedModel(modelId: String?, modelName: String?) {
        context.dataStore.edit {
            it[Keys.SELECTED_MODEL] = modelId
            it[Keys.SELECTED_MODEL_NAME] = modelName
        }
    }

    suspend fun setAiPresets(presets: List<AiPreset>) {
        val json = Json.encodeToString(presets)
        context.dataStore.edit { it[Keys.AI_PRESETS] = json }
    }

    suspend fun setTtsRate(rate: Float) {
        context.dataStore.edit { it[Keys.TTS_RATE] = rate }
    }

    suspend fun setTtsPitch(pitch: Float) {
        context.dataStore.edit { it[Keys.TTS_PITCH] = pitch }
    }

    suspend fun setTtsBackground(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TTS_BACKGROUND] = if (enabled) 1 else 0 }
    }

    // --- Default AI presets ---
    private fun defaultPresets(): List<AiPreset> {
        return listOf(
            AiPreset("Simplify", "Make this text simpler to understand: {copied text}"),
            AiPreset("Modernize", "Update this text to use contemporary language: {copied text}"),
            AiPreset("Fact check", "Verify the factual accuracy of this text: {copied text}"),
            AiPreset("Explain", "Explain this text in simple terms: {copied text}"),
            AiPreset("Summarize", "Provide a concise summary of this text: {copied text}"),
            AiPreset("Translate", "Translate this text to English: {copied text}"),
            AiPreset("Ask", "Answer questions about this text: {copied text}")
        )
    }

    // --- Per-file progress (resume position) ---
    // We'll store a JSON map: uriHash -> { fraction, wordOffset, totalWords, updatedAt }
    private val PROGRESS_MAP = stringPreferencesKey("progress_map")

    @Serializable
    private data class ProgressRecord(
        val fraction: Float, // scroll fraction (0-1)
        val wordOffset: Int, // word index at which to resume
        val totalWords: Int, // total words in the document (for progress calculation)
        val updatedAt: Long  // timestamp
    )

    suspend fun saveProgress(uri: String, fraction: Float, wordOffset: Int, totalWords: Int) {
        val hash = uri.hashCode().toString()
        val record = ProgressRecord(fraction, wordOffset, totalWords, System.currentTimeMillis())
        context.dataStore.edit { prefs ->
            val currentJson = prefs[PROGRESS_MAP] ?: "{}"
            val map = try {
                Json.decodeFromString<Map<String, ProgressRecord>>(currentJson)
            } catch (e: Exception) {
                mapOf()
            }
            val updated = map + (hash to record)
            prefs[PROGRESS_MAP] = Json.encodeToString(updated)
        }
    }

    suspend fun loadProgress(uri: String): ProgressRecord? {
        val hash = uri.hashCode().toString()
        return context.dataStore.data.map { prefs ->
            val json = prefs[PROGRESS_MAP]
            if (json == null || json.isEmpty()) return@map null
            try {
                val map = Json.decodeFromString<Map<String, ProgressRecord>>(json)
                map[hash]
            } catch (e: Exception) {
                null
            }
        }.first() // first emission
    }

    // --- For home progress bar: we need to know total words and read words for each recent file ---
    // We'll reuse the progress map but also store the last known totalWords and wordOffset (read so far).
    // For the home screen, we can compute percent = wordOffset / totalWords * 100
    suspend fun getRecentFileProgress(uri: String): Pair<Int, Int>? { // (wordOffset, totalWords)
        return loadProgress(uri)?.let { record ->
            record.wordOffset to record.totalWords
        }
    }
}
