# PixelCraft 📸

A modern, dark-themed photo editor for Android, built as a college
Photography subject project. Import a photo, adjust light and color,
apply filters, crop and rotate, compare before/after, and export the
result to your gallery as JPEG or PNG.

| | |
|---|---|
| **Platform** | Android 7.0+ (API 24), optimized and tested on current Android |
| **Language** | Kotlin |
| **UI** | XML layouts, Material 3 dark theme, ViewBinding |
| **Image engine** | Android Bitmap + Canvas APIs, pure-Kotlin pixel math |
| **Status** | MVP complete — all listed features are implemented |

## Features

- **Gallery import** via the system Photo Picker (no storage permission
  needed), with automatic down-sampling of huge camera photos and EXIF
  orientation correction.
- **Light adjustments** — brightness, contrast, exposure.
- **Color adjustments** — saturation, white balance (warm ↔ cool).
- **One-tap filters** — Original, Grayscale, Warm, Cool. Presets set the
  sliders, so you can fine-tune after applying one.
- **Crop** with a draggable rule-of-thirds overlay (corners, edges and
  whole-rectangle move) and **90° step rotation**.
- **Before/after compare** — tap the compare button to flip between the
  untouched original and the current edit; an ORIGINAL badge marks it.
- **Reset** everything with one confirmed action.
- **Export** as JPEG (with adjustable quality) or PNG into
  `Pictures/PixelCraft` in the gallery, correctly indexed so other apps
  see it immediately.
- **Robustness** — unreadable files, out-of-memory cases and save
  failures produce readable error snackbars, never crashes.

## Getting started

1. Install [Android Studio](https://developer.android.com/studio) (a
   current version — the project targets AGP 8.13 / Gradle 9.5.1).
2. **File → Open** and select this project folder.
3. Let Gradle sync finish (first sync downloads dependencies).
4. Pick a device: a physical phone with USB debugging, or an emulator
   from **Device Manager**.
5. Press **Run ▶**.

### Build and test from the terminal

```bash
./gradlew assembleDebug          # build the APK
./gradlew testDebugUnitTest      # run the unit tests
./gradlew installDebug           # install on the connected device
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Project structure

```
app/src/main/java/com/pixelcraft/editor/
├── MainActivity.kt          # single screen: toolbar, preview, tabs
├── EditorViewModel.kt       # state + debounce + background rendering
├── image/                   # pure-Kotlin engine (no Android imports)
│   ├── ColorTransform.kt    #   per-pixel math, unit tested on the JVM
│   ├── Adjustments.kt       #   slider state, -100..100 per parameter
│   ├── FilterType.kt        #   the four presets
│   └── ImageProcessor.kt    #   bitmap application, single pass
├── io/                      # gallery in / out
│   ├── ImageImporter.kt     #   decode + downsample + EXIF fix
│   └── ImageExporter.kt     #   MediaStore / file export
└── ui/
    ├── CropOverlayView.kt   # interactive crop rectangle
    ├── EditorPagerAdapter.kt# the four tool tabs
    └── tabs/                # Light / Color / Filters / Transform

app/src/test/                # JUnit tests for the color engine
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the design rationale and
[CONTRIBUTING.md](CONTRIBUTING.md) for the two-contributor workflow.

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — how the code is organized and why
- [CONTRIBUTING.md](CONTRIBUTING.md) — branches, PRs, file ownership
- [TESTING.md](TESTING.md) — unit tests + the manual test checklist
- [docs/DEMO_GUIDE.md](docs/DEMO_GUIDE.md) — 5-minute professor demo script

## Screenshots

_(Add screenshots here after the first run: open the emulator, take
screenshots of the editor, filters and crop screens, drop them into a
`/screenshots` folder and reference them like
`![Editor](screenshots/editor.png)`.)_

## License

Released under the [MIT License](LICENSE). All artwork in
`app/src/main/res/drawable/` is original vector work created for this
project.
