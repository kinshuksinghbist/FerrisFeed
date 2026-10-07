# FerrisFeed — Tech Stack

> Spec v2: the app ships Rust + System Design only. WASM content stays
> dormant in `content/wasm/` (not seeded, rendered, or tinted).

Locked decisions for the MVP scaffold. Reasons are inline so future agents
do not relitigate them without new evidence.

## Language + UI

- **Kotlin + Jetpack Compose + Material 3 Expressive** — 100% Compose,
  no Views, no XML layouts. Expressive color schemes
  (`expressiveLightColorScheme` / `expressiveDarkColorScheme` where
  available on M3 1.3+, fallback to standard M3) with dynamic color
  toggle. Rounded 28dp cards, dark OLED `#0B0E14`, Ferris orange `#FF6B35`.
- **minSdk 26, targetSdk 35, edge-to-edge, predictive back** —
  minSdk 26 covers 98%+ devices while keeping `VerticalPager` + dynamic
  color simple. Edge-to-edge via `enableEdgeToEdge()` plus
  `android:enableOnBackInvokedCallback="true"` in the manifest.
  targetSdk 35 is Play's expectation for new listings (TODO 28a); the
  AGP 8.9.2 / Gradle 8.11.1 / compileSdk 36 triple stays locked.

## Modules

- **`:app`** — `MainActivity`, `FerrisApp`, DI wiring, NavHost,
  notifications, baseline profiles, R8.
- **`:core-ui`** — design system only: `ReelCard`, `CodeCard`,
  `QuizCard`, `TrapCard`, `ProgressRing`, `StreakFlame`, `TrackPill`,
  theme, shimmer. Previewable dark + light + dynamic.
- **`:feature-feed`** — vertical pager, prefetch, shuffle, impression
  tracking, deep-dive bottom sheet.
- **`:feature-path`** — DAG path engine UI, placement test, daily mix,
  search + topic map.
- **`:data`** — Room entities/DAOs (reels + FTS + progress), DataStore,
  repositories, WorkManager sync workers.

## Libraries (see `gradle/libs.versions.toml`)

- **Compose BOM 2025.04.01** — single source of truth for Compose +
  Material3 versions. Prevents drift across feature modules. Must stay on a
  BOM shipping UI 1.8.0+ (`Reject` haptic used by `QuizCard`).
- **Navigation3 (`androidx.navigation3`)** — type-safe backstack with
  `NavDisplay` + `@Serializable` routes. Chosen over Navigation2 because
  predictive back and adaptive layouts are first-class.
- **Hilt 2.52 + hilt-navigation-compose** — constructor injection for
  ViewModels, DAOs, repositories, workers. `HiltWorkerFactory` for
  WorkManager.
- **Room 2.6.1 + KSP** — `curriculum/*.json` in APK assets seeds
  the DB on first launch (`CurriculumSeeder`),
  FTS4 for search, `ProgressDao` for SRS state. KSP over KAPT for speed.
  Schema export to `data/schemas/` for review.
- **DataStore Preferences 1.1.1** — XP, streak, mastery, dynamic-color
  toggle, scroll-restore cursor. Proto not needed for MVP primitives.
- **WorkManager 2.9.0** — weekly pack sync + 9pm streak reminder.
  Constrained (unmetered + charging + idle) for pack downloads.
- **Coil 2.6.0 + okhttp-network** — memory (25%) + disk (256 MB) cache
  for memory-diagram images, airplane-mode friendly.
- **kotlinx.serialization 1.7.3** — reel JSON schema
  (`hook/body/code/takeaway/trap/quiz`), NavKeys, RemoteConfig payloads.
  Codegen via `kotlin-serialization` plugin, no reflection.

## Shared Rust Core

- **`rust-core/` crate `ferris-core`** — curriculum model, FSRS-lite,
  XP calculator, DAG resolver. Exposed to Android via UniFFI/JNI and to
  web preview via `wasm-pack`. This is why the app dogfoods Rust while
  teaching it. Android owns the scaffold; the Rust agent owns the crate.

## Performance + Release

- **R8 full mode + shrinkResources** — `isMinifyEnabled=true` on release,
  `consumer-rules.pro` per module, size budget < 25 MB.
- **Baseline profiles** — `androidx.baselineprofile` plugin, startup +
  `VerticalPager` scroll rules checked into
  `app/src/main/baselineProfiles/`.
- **Dynamic color** — Material You `dynamicLightColorScheme` /
  `dynamicDarkColorScheme` when the user opts in; Ferris palette otherwise.
