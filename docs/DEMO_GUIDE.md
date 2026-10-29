# PixelCraft — 5-Minute Demo Guide

A script for presenting the project to your professor. Rehearse it once;
each step takes 30–60 seconds. Everything below maps to features listed in
the README, so nothing here needs a disclaimer.

## Before the demo

- [ ] Phone/emulator charged and unlocked, PixelCraft installed.
- [ ] Gallery contains: one landscape photo, one **portrait** photo taken
      with the phone camera (for the EXIF check), one very large image.
- [ ] Dark mode on the device (optional but shows off the theme).
- [ ] Android Studio open on `MainActivity.kt` and
      `image/ColorTransform.kt` for the code walkthrough.

## The script

**1. Hook (15 s).** "PixelCraft is an original, dark-themed photo editor
for Android — inspired by desktop tools like RawTherapee, but written
from scratch in Kotlin."

**2. Import (30 s).** Launch the app → note the clean empty state →
**Import photo** → pick the portrait camera photo. Point out it appears
correctly upright (EXIF handling) and that no permission dialog ever
appeared (system Photo Picker).

**3. Light + Color (60 s).** Light tab: push Brightness to +100, then
Contrast +50, Exposure -30. Color tab: Saturation -80, White balance +40.
Narrate: "Every change re-renders the full bitmap on a background thread
with a single-pass pixel pipeline — the UI never blocks."

**4. Filters (30 s).** Filters tab → Grayscale → then Warm. Show the
preset stays highlighted and mention: "presets set the sliders, so the
user can fine-tune afterwards — the implementation reuses the same color
engine."

**5. Transform (60 s).** Rotate right — note the progress overlay and the
preview updating. Then Crop: drag one corner, drag an edge, move the
rectangle (mention rule-of-thirds grid + minimum-size guard), Apply.
"Coordinates are converted from screen space to bitmap space and the crop
bakes into the base bitmap."

**6. Compare + Reset (30 s).** Tap **Compare** — untouched original with
the ORIGINAL badge; tap again to return. Tap **Reset** → confirm →
everything returns to the exact imported photo.

**7. Export (45 s).** Re-apply a quick look (one slider + one filter) →
**Save** → choose JPEG, drag quality, mention PNG disables it → Save →
show the success snackbar → open Google Photos and show
`Pictures/PixelCraft` containing the new image. Open it to prove edits
were baked in.

**8. Robustness (30 s).** Back-press with unsaved edits → discard dialog.
Rotate the device — state survives. Import the huge image — still fine
(down-sampling). "Errors are readable snackbars, never crashes."

**9. Code + tests (45 s).** Show the package split (`image/` is pure
Kotlin with 30+ JUnit assertions; `io/`; `ui/`). Run
`./gradlew testDebugUnitTest` live if you like — it passes in seconds.
Show `CONTRIBUTING.md` for the two-person branch workflow and the green
CI badge on GitHub.

## Likely professor questions

- **Why not use a library like GPUImage?** Requirement was Bitmap/Canvas
  APIs; the pipeline is original code and unit tested.
- **Why are images downsampled?** A 48 MP ARGB_8888 bitmap is ~192 MB —
  above the per-app heap; we keep a 2048 px editing copy.
- **Where do saved images go and why no permission?** MediaStore with
  `RELATIVE_PATH` on Android 10+; app-specific directory on older
  versions. Neither needs storage permission.
- **What happens to the original?** Never modified — three-bitmap model
  (source / base / edited) keeps compare and reset exact.
- **How do two people work on this?** Package-per-owner separation,
  feature branches, PRs with review, CI on every PR.
