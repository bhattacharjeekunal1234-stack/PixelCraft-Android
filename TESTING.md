# Testing PixelCraft

Two layers: fast JVM unit tests for the color engine, and a manual test
checklist for everything that needs Android (files, gallery, touch).

## Unit tests

Located in `app/src/test/java/com/pixelcraft/editor/image/` and runnable
on any machine in seconds — no emulator needed.

| Test class | Covers |
|---|---|
| `ColorTransformTest` | brightness, contrast, exposure, saturation, white balance, grayscale, warm/cool — identity at 0, clamping at extremes, fixed points, symmetry |
| `AdjustmentsTest` | neutral defaults, clamping of out-of-range slider values, copy-API isolation |

Run them:

```bash
./gradlew testDebugUnitTest
```

In Android Studio: right-click `app/src/test` → **Run Tests**, or click
the gutter arrow next to any test class. Results appear in the **Test
Results** tool window; HTML report in
`app/build/reports/tests/testDebugUnitTest/`.

### What is deliberately *not* unit tested

Bitmap operations (`ImageProcessor.apply`, crop, rotate) need a real
`Bitmap` implementation and belong in an instrumented test
(`androidTest`) — a natural extension if you want to grow the project.

## Manual test checklist

Run through this before the demo, after every big change, and when
testing a teammate's pull request.

### Import
- [ ] Import from the Photo Picker — photo appears centered in the card.
- [ ] Import a portrait photo taken with the phone camera — it must show
      **upright** (EXIF check).
- [ ] Import a very large photo (screenshot, 12 MP+ camera shot) — no
      crash, loads within a couple of seconds.
- [ ] Cancel the picker — nothing changes, no error shown.

### Adjustments (Light + Color tabs)
- [ ] Each slider changes the preview in the correct direction.
- [ ] Value labels show `-100`…`+100` and step by 1.
- [ ] Dragging a slider does **not** swipe the tab pager.
- [ ] Extreme settings (all +100) never crash and never corrupt colors.
- [ ] Switching tabs does not reset sliders.

### Filters
- [ ] Grayscale / Warm / Cool visibly change the photo.
- [ ] The tapped filter stays highlighted; Original clears the highlight.
- [ ] A preset followed by slider tweaks combines both effects.
- [ ] After Reset, the highlighted preset returns to Original.

### Transform
- [ ] Rotate right 4× returns to the original orientation; image stays
      sharp (no quality loss after 4 rotations).
- [ ] Crop: drag a corner inward, drag an edge, move the whole rectangle;
      Apply cuts exactly the selected area (check against the grid).
- [ ] Crop: Cancel leaves the photo untouched.
- [ ] Crop a tiny area — a readable error snackbar appears, no crash.
- [ ] Rotate/crop with sliders at non-default values keeps the color
      edits applied afterwards.

### Compare / Reset
- [ ] Compare shows the untouched original with the ORIGINAL badge;
      tapping again returns to the edit.
- [ ] Reset restores the exact imported photo and zeroes all sliders.
- [ ] Reset with nothing edited shows the "nothing to reset" snackbar.

### Export
- [ ] Save as JPEG with quality ~90 — file appears in Gallery →
      Pictures/PixelCraft immediately.
- [ ] Save as PNG (quality slider visibly disabled) — also appears.
- [ ] The saved file matches the preview **including crop, rotation,
      filter and sliders**.
- [ ] Compare the saved file size: JPEG at low quality is clearly smaller.

### Robustness
- [ ] Press Back with unsaved edits — confirm dialog appears; with no
      edits the app exits immediately.
- [ ] Rotate the device mid-edit — sliders, preview and bitmap all
      survive (ViewModel).
- [ ] During import/save the progress overlay shows and buttons are
      inert; no double imports or double saves.
- [ ] Airplane-mode test: everything works — the app never needs network.
