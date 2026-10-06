# FerrisFeed — Doomscroll, but you get hired.

Offline-first Android feed of 60-second Rust / WASM / System Design reels
with quizzes, SRS, and streaks. Kotlin + Compose + Material 3 Expressive,
powered by a shared Rust core.

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17 (bundled with Studio is fine)
- Android SDK 36 (compileSdk; targetSdk stays 34, minSdk 26), NDK not required for the Android scaffold
- Rust toolchain only if you touch `rust-core/` (see that dir)

## Open in Android Studio

1. Open the folder `FerrisFeed/` (the one containing `settings.gradle.kts`).
2. Let Gradle sync with the version catalog in `gradle/libs.versions.toml`.
3. Select the `app` run configuration and run on a API 34 emulator or device.

## Build from the Command Line

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK (R8 + shrinkResources)
./gradlew :app:assembleRelease

# Unit tests
./gradlew test

# Connected Compose UI tests
./gradlew :app:connectedDebugAndroidTest
```

## Project Layout

```text
app/            MainActivity NavHost (feed/path/search), FerrisApp, DI, manifest
core-ui/        Design system — ReelCard, CodeCard, QuizCard, theme
feature-feed/   VerticalPager feed, prefetch, deep-dive sheet
feature-path/   Path DAG UI, placement test, search + topic map
data/           Room (reels + FTS + progress), DataStore, repositories, workers
rust-core/      Shared Rust crate ferris-core (UniFFI + wasm-pack) — owned by Rust agent
content/        Reel JSON packs (hook/body/code/takeaway/trap/quiz) — owned by content agent
docs/           vision.md, tech-stack.md
```

## Where the Rust Core Lives

- `rust-core/` holds the `ferris-core` crate: curriculum types, FSRS-lite
  scheduler, XP calculator, DAG resolver, UniFFI bindings, `wasm-bindgen`
  feature for the web preview.
- Android consumes it via UniFFI-generated Kotlin bindings. The `:app` and
  `:data` modules call into it for scheduling and grading; Room/DataStore
  remain the source of truth for persistence.
- Do not vendor Rust code into `app/` — keep the boundary clean.

## Release Notes

- `minSdk 26`, `targetSdk 34`, edge-to-edge, predictive back enabled.
- Release builds use R8 full mode + baseline profiles. Keep the APK < 25 MB.
- Curriculum JSON ships in APK assets and seeds Room on first launch; weekly
  packs sync via WorkManager.
