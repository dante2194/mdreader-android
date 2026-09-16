package com.mdreader.data.fonts

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes

/**
 * Registry of available fonts, both bundled and imported.
 */
data class FontInfo(
    val name: String,          // Display name in the picker
    val fileName: String,      // File name in assets/fonts/ or imported directory
    val isBundled: Boolean,    // Whether it's bundled (assets) or imported (filesDir)
    val typeface: Typeface? = null // Lazily loaded
)

class FontRegistry(private val context: Context) {

    // Bundled open-license fonts (alternatives to popular proprietary ones)
    val bundledFonts = listOf(
        FontInfo("EB Garamond", "EBGaramond-Regular.ttf", true),
        FontInfo("Libre Caslon", "LibreCaslon-Regular.ttf", true),
        FontInfo("Libre Baskerville", "LibreBaskerville-Regular.ttf", true),
        FontInfo("Tinos", "Tinos-Regular.ttf", true),
        FontInfo("Gelasio", "Gelasio-Regular.ttf", true),
        FontInfo("URW Gothic L", "URWGothic-Light.ttf", true), // Palatino alternative
        FontInfo("Cormorant Garamond", "CormorantGaramond-Regular.ttf", true),
        FontInfo("PT Serif", "PTSerif-Regular.ttf", true),
        FontInfo("Inter", "Inter-Regular.ttf", true),
        FontInfo("Arimo", "Arimo-Regular.ttf", true),
        FontInfo("DejaVu Sans", "DejaVuSans.ttf", true),
        FontInfo("Open Sans", "OpenSans-Regular.ttf", true),
        FontInfo("Carlito", "Carlito-Regular.ttf", true),
        // User uploaded fonts
        FontInfo("Styrene A Bold", "StyreneA-Bold.otf", true),
        FontInfo("Tiempos Text Bold", "TiemposText-Bold.ttf", true)
    )

    // Imported fonts will be loaded dynamically from context.filesDir/fonts/
    var importedFonts: List<FontInfo> = emptyList()
        private set

    fun loadImportedFonts() {
        val importedDir = context.filesDir.absolutePath + "/fonts/"
        val dir = java.io.File(importedDir)
        if (!dir.exists()) {
            importedFonts = emptyList()
            return
        }
        val files = dir.listFiles { _, name ->
            name.endsWith(".ttf") || name.endsWith(".otf")
        } ?: emptyArray()
        importedFonts = files.map { file ->
            FontInfo(
                name = file.nameWithoutExtension.replace('_', ' ').titlecase(),
                fileName = file.name,
                isBundled = false
            )
        }
    }

    fun getAllFonts(): List<FontInfo> = bundledFonts + importedFonts

    fun getTypeface(fontInfo: FontInfo): Typeface {
        return if (fontInfo.isBundled) {
            Typeface.createFromAsset(context.assets, "fonts/${fontInfo.fileName}")
        } else {
            Typeface.createFromFile(java.io.File(context.filesDir, "fonts/${fontInfo.fileName}"))
        }
    }
}
