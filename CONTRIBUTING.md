# Contributing to PixelCraft

This project is designed for **two contributors** working in parallel with
minimal merge friction. The layout separates concerns into file families
that rarely need to change at the same time.

## Branch model

```
main ── stable, demo-ready code only
 ├── feature/light-adjustments     (Contributor A)
 ├── feature/filters-and-pipeline  (Contributor A)
 ├── feature/gallery-import-export (Contributor B)
 └── feature/crop-rotate-compare   (Contributor B)
```

- `main` is protected in spirit: work lands there **only through pull
  requests**, after the other contributor reviewed them.
- One feature per branch, named `feature/<what-it-does>`.
- Keep branches short-lived; rebase onto `main` (`git pull --rebase origin
  main`) instead of merging `main` into your branch when you need fresh
  changes.

## File ownership (conflict avoidance)

| Area | Files | Default owner |
|---|---|---|
| Image engine + unit tests | `image/*`, `app/src/test/*` | Contributor A |
| Import / export | `io/*` | Contributor B |
| Screens & tabs | `MainActivity.kt`, `EditorViewModel.kt`, `ui/tabs/*` | Contributor B |
| Crop overlay & adapter | `ui/CropOverlayView.kt`, `ui/EditorPagerAdapter.kt` | Contributor A |
| Layouts, strings, themes | `res/layout/*`, `res/values/*`, `res/menu/*` | Contributor B |
| Vector icons & drawables | `res/drawable/*` | Contributor A |
| Gradle files | `*.gradle.kts`, `gradle-wrapper.properties` | **Shared — change only with agreement** |

Rules of thumb:

- **Never rename shared ids** (`viewPager`, `imagePreview`, tab array…)
  without announcing it in the PR description — both halves touch them.
- New strings go in `res/values/strings.xml`; to avoid colliding there,
  add your strings in your own feature commit and resolve trivially if
  both edited the same region.
- `EditorViewModel.kt` is the most-shared file. Add new public functions
  at the *bottom* of their section rather than editing neighbors' code.

## Pull request checklist

Before opening a PR:

1. `./gradlew testDebugUnitTest` passes.
2. `./gradlew assembleDebug` builds.
3. You manually exercised the feature on the emulator.
4. No leftover debug logging, no commented-out code, no TODO markers
   claiming features that do not exist.

PR description template:

```markdown
## What
<one paragraph>

## How to test
1. Import a photo
2. <step>… 

## Screenshots
<before/after>
```

## Commit style

Small, imperative commits:

```
Add exposure slider to Light tab
Fix EXIF rotation for portrait photos
Handle OOM when decoding very large images
```

## Reporting bugs

Open a GitHub issue with: device/emulator + Android version, steps to
reproduce, expected vs actual behavior, and a screenshot or Logcat
excerpt if available.
