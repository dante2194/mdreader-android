package com.mdreader.data.fonts

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.graphics.Typeface as ComposeTypeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.io.File

/**
 * Registry of available fonts.
 * Bundled = the two actual TTF/OTF files currently in app-private assets.
 * Imported = fonts the user has added via the import feature.
 * Conceptual = font names displayed in Settings UI; real Compose typeface depends on availability.
 */
object FontRegistry {
    private const val TAG = "FontRegistry"

    /** Fonts that physically exist in app-private `assets/fonts/`. */
    data class BundledFont(
        val id: String,
        val displayName: String,
        val cssFamilyName: String,
        val assetFileName: String,
        val assetPath: String, // "fonts/filename"
    )

    /** A user-imported TTF/OTF file. */
    data class ImportedFont(
        val id: String,
        val filePath: String, // "filesDir/fonts/filename"
        val displayName: String,
        val cssFamilyName: String,
    )

    /** Conceptual font name shown in Settings; real Compose typeface may fall back to system. */
    data class ConceptFont(
        val id: String,
        val displayName: String,
        val cssFamilyName: String,
    )

    // Two actually bundled fonts
    val bundledFonts = listOf(
        BundledFont("styrene_a_bold", "Styrene A Bold", "Styrene A Bold", "StyreneA-Bold.otf",
            "fonts/StyreneA-Bold.otf"),
        BundledFont("tiempos_text_bold", "Tiempos Text Bold", "Tiempos Text Bold", "TiemposText-Bold.ttf",
            "fonts/TiemposText-Bold.ttf")
    )

    // Conceptual fonts that appear in the Settings font picker
    // Their real typeface at runtime will be the closest available, or the user imports one.
    val conceptFonts = listOf(
        ConceptFont("garamond", "Garamond", "Garamond"),
        ConceptFont("caslon", "Caslon", "Caslon"),
        ConceptFont("baskerville", "Baskerville", "Baskerville"),
        ConceptFont("times_new_roman", "Times New Roman", "Times New Roman"),
        ConceptFont("georgia", "Georgia", "Georgia"),
        ConceptFont("palatino", "Palatino", "Palatino"),
        ConceptFont("minion", "Minion", "Minion"),
        ConceptFont("century_schoolbook", "Century Schoolbook", "Century Schoolbook"),
        ConceptFont("helvetica", "Helvetica", "Helvetica"),
        ConceptFont("arial", "Arial", "Arial"),
        ConceptFont("verdana", "Verdana", "Verdana"),
        ConceptFont("open_sans", "Open Sans", "Open Sans"),
        ConceptFont("calibri", "Calibri", "Calibri"),
        ConceptFont("old_english", "Old English", "Old English"),
        ConceptFont("roboto", "Roboto", "Roboto"),
        ConceptFont("system_sf", "System (San Francisco-like)", "System"),
    )

    /** Get a BundledFont by id; returns null if not found. */
    fun getBundledById(id: String): BundledFont? = bundledFonts.firstOrNull { it.id == id }

    /** Resolve a Compose Typeface from a bundled font's asset path. */
    fun resolveTypefaceFromBundled(context: Context, bundled: BundledFont): ComposeTypeface? {
        return try {
            val tf = Typeface.createFromAsset(context.assets, bundled.assetPath)
            ComposeTypeface(tf)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bundled typeface ${bundled.id}", e)
            null
        }
    }

    /** Get all imported font paths from DataStore. Must be called with dataStore sync. */
    fun getImportedFontPaths(context: Context): List<String> {
        return try {
            val key = androidx.datastore.preferences.core.stringSetPreferencesKey("imported_fonts")
            context.dataStore.data.awaits().let { prefs ->
                prefs[key]?.toList() ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Add a font file path to the imported set in DataStore. */
    suspend fun addImportedFont(context: Context, filePath: String) {
        val key = androidx.datastore.preferences.core.stringSetPreferencesKey("imported_fonts")
        context.dataStore.edit {
            val current = (it[key] ?: mutableSetOf()).toMutableSet()
            current.add(filePath)
            it[key] = current.toList().takeLast(20).toSet()
        }
    }

    /** Remove a font path from the imported set. */
    suspend fun removeImportedFont(context: Context, filePath: String) {
        val key = androidx.datastore.preferences.core.stringSetPreferencesKey("imported_fonts")
        context.dataStore.edit {
            val current = (it[key] ?: mutableSetOf()).toMutableSet()
            current.remove(filePath)
            it[key] = current.toList().takeLast(20).toSet()
        }
    }

    /** Build a combined list: bundled (real Typeface-resolvable) + imported + conceptual. */
    fun getAllFontEntries(context: Context, importedPaths: List<String>): List<Object> {
        val result = mutableListOf<Object>()

        // Bundled - can actually provide Typeface for Compose
        for (b in bundledFonts) {
            result.add(
                fontEntry(b, "bundled") { resolveTypefaceFromBundled(context, b) }
            )
        }

        // Imported - displayed but Typeface will be null/fallback (loaded via CSS @font-face in WebView)
        for (path in importedPaths) {
            val file = java.io.File(path)
            if (!file.exists()) continue
            val name = file.nameWithoutExtension
                .replace("_", " ")
                .replace("-", " ")
            result.add(fontEntry(
                id = "imported_${file.name}",
                displayName = name,
                cssFamilyName = name,
                typeface = null,
                kind = "imported"
            ))
        }

        // Conceptual - just UI entries; typeface will default to system/roboto
        for (c in conceptFonts) {
            result.add(fontEntry(
                id = "concept_${c.id}",
                displayName = c.displayName,
                cssFamilyName = c.cssFamilyName,
                typeface = null,
                kind = "concept"
            ))
        }

        return result
    }

    /** Create a FontEntry (when kind = "bundled", typeface is resolved; otherwise null). */
    private data class FontEntry(
        val id: String,
        val displayName: String,
        val cssFamilyName: String,
        val typeface: ComposeTypeface?,
        val kind: String,
    )

    private fun fontEntry(
        id: String,
        displayName: String,
        cssFamilyName: String,
        typeface: ComposeTypeface?,
        kind: String,
    ): FontEntry = FontEntry(id, displayName, cssFamilyName, typeface, kind)
}
