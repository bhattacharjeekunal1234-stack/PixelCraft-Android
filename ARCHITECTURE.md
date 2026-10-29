# PixelCraft Architecture

This document explains how PixelCraft is organized, why each decision was
made, and where each feature lives — useful both for the project report
and for contributors.

## 1. High-level shape

PixelCraft is a single-activity app following the MVVM pattern:

```
┌─────────────────────────────────────────────────────────────┐
│  MainActivity (thin: wiring only)                           │
│  ├── Toolbar (import / compare / reset / export)            │
│  ├── Image card (preview + crop overlay + badges)           │
│  └── TabLayout + ViewPager2                                 │
│        ├── LightTabFragment    (brightness/contrast/exposure)
│        ├── ColorTabFragment    (saturation/white balance)   │
│        ├── FiltersTabFragment  (4 presets)                  │
│        └── TransformTabFragment(rotate 90°, crop)           │
│                         │ LiveData                          │
│              ┌──────────▼──────────┐                       │
│              │   EditorViewModel   │  one instance, owned  │
│              │  (survives rotate)  │  by the Activity      │
│              └──────────┬──────────┘                       │
│         ┌───────────────┼────────────────┐                 │
│  ┌──────▼──────┐ ┌──────▼───────┐ ┌──────▼───────┐         │
│  │ image/      │ │ io/          │ │ ui/          │         │
│  │ pure Kotlin │ │ gallery in/  │ │ custom views │         │
│  │ pixel math  │ │ out          │ │              │         │
│  └─────────────┘ └──────────────┘ └──────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

The three packages are deliberately independent:

- `image/` imports **nothing from Android** except the Bitmap class used
  at its boundary — the math itself is plain Kotlin, which is why the
  color engine has JVM unit tests.
- `io/` knows how to get bytes in and out of the device; it never touches
  color math.
- `ui/` never touches files or pixels directly; it talks only to the
  ViewModel.

## 2. The three-bitmap model

`EditorViewModel` keeps three bitmaps:

| Bitmap | Meaning | Changes when |
|---|---|---|
| `sourceBitmap` | the photo exactly as imported | never |
| `baseBitmap` | source after crop / rotation | crop, rotate, reset |
| `editedBitmap` | base after color adjustments | every slider / filter |

Consequences:

- **Compare** is trivial and honest: showing `sourceBitmap` next to the
  current preview is a true before/after, including crops and rotations.
- **Reset** is O(1): drop the adjustment set, point `baseBitmap` back at
  `sourceBitmap`.
- Crop rectangles taken from the screen map 1:1 onto `baseBitmap`,
  because color adjustments never change pixel dimensions.

## 3. Why the color engine is one pass

Each slider drag re-renders the whole photo. Naively running each
adjustment as its own full-bitmap copy would allocate a new multi-megabyte
bitmap per step. Instead `ImageProcessor.apply` reads all pixels into one
`IntArray`, applies every active transform per pixel, and writes the
buffer back into a single result bitmap — one allocation per render.

Adjustment order (matching desktop editor behavior):
`exposure → brightness → contrast → saturation → white balance → filter`.

Slider events fire dozens of times per gesture; the ViewModel debounces
rendering with a 60 ms `Handler` delay and cancels stale renders (a
finished render is discarded if the sliders moved meanwhile), so dragging
stays smooth even on 12 MP photos.

## 4. Threading

- Decode (`ImageImporter`), save (`ImageExporter`), rotation and the
  color render all run on `Dispatchers.IO` / `Dispatchers.Default` via
  `viewModelScope` coroutines.
- The main thread only ever receives finished bitmaps through `LiveData`.
- While any of that runs, `EditorStatus.BUSY` shows a scrim + progress
  overlay and blocks re-entrant actions (double-tap import, save during
  save, etc.).

## 5. Import & export without permissions

- **Import** uses the system Photo Picker (`ActivityResultContracts.
  PickVisualMedia`), available on Android 11+ and backported by Google
  Play services — no `READ_EXTERNAL_STORAGE` permission and no
  permission-request UI anywhere in the app.
- Large photos are downsampled to ≤2048 px on the long edge with
  `inSampleSize` before decoding, so a 48 MP camera photo cannot exhaust
  the heap; EXIF orientation is then applied so portrait shots appear
  upright.
- **Export** on Android 10+ writes through `MediaStore` into
  `Pictures/PixelCraft` (pending-flag pattern, cleanup on failure) and on
  Android 9 and below into the app's own Pictures directory plus a media
  scanner scan. Both paths need no storage permission.

## 6. Crop overlay design

`CropOverlayView` is a custom `View` stacked in a wrapper that is sized
exactly like the preview bitmap, so overlay coordinates and displayed
bitmap pixels share one scale. The overlay converts its selection to real
bitmap coordinates by dividing by `viewWidth / bitmapWidth`. It supports
corner, edge and whole-rectangle dragging with a minimum crop size and
a rule-of-thirds grid, and blocks the parent ViewPager from stealing
horizontal drags (`requestDisallowInterceptTouchEvent`).

## 7. Version notes

The project pins Gradle 9.5.1 + AGP 8.13 + Kotlin 2.2.20 — a triple
checked against the official compatibility tables: AGP 8.x requires
Gradle ≤ 9.5 (Gradle 9.6 removed an internal API AGP 8 relies on), and
Gradle 9.1+ runs on the JDK bundled with current Android Studio releases.
If your studio installation is older, let Android Studio's AGP upgrade
assistant adjust the pair.
