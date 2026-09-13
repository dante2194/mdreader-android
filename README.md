# MD Reader (MVP)

A simplified Material 3 Markdown reader for Android, per the reduced scope:

- Open a `.md` file (via system file picker, or by opening one from another app)
- Read it with adjustable font size
- Light / Dark / System theme
- **Fullscreen is a toggle in the reader's top bar — off by default.** Tapping the
  fullscreen icon hides both the app bar and the system status/nav bars for a
  distraction-free view; tapping the exit icon (top-right) restores them.

## What was deliberately left out of this MVP
Custom font import, TTS, PDF/HTML export, bookmarks, cloud backup, and custom
accent colors were all in the original design proposal but are not implemented
here. The code is structured (see `ui/`, `data/`) so they can be added later
without restructuring.

## Project structure
```
app/src/main/java/com/mdreader/
├── MainActivity.kt         # navigation + fullscreen system-bar handling
├── data/
│   ├── local/FileReader.kt       # reads text from a content Uri
│   └── repository/PrefsRepository.kt  # DataStore: theme, font size, recent files
└── ui/
    ├── theme/Theme.kt      # Material 3 color schemes (light/dark)
    └── screens/
        ├── LibraryScreen.kt
        ├── ReaderScreen.kt
        └── SettingsScreen.kt
```

## Building
This is a standard Gradle Android project.

1. Open the folder in Android Studio (Hedgehog or newer), let it sync Gradle.
2. Run on a device/emulator (minSdk 24 / Android 7.0+).

Or from the command line, after generating the Gradle wrapper once in
Android Studio (`File > Sync`, which creates `gradlew`/`gradlew.bat`):

```
./gradlew assembleDebug
```

The wrapper jar/scripts aren't included in this zip — Android Studio will
generate them on first sync, or run `gradle wrapper` if you have Gradle
installed locally.

## Notes
- Markdown is rendered with [Markwon](https://noties.io/Markwon/) (`io.noties.markwon:core`).
- No Room database for this MVP — recent files are stored as a small set in
  Jetpack DataStore, which is simpler for a handful of entries.
