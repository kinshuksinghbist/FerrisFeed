# Quality Bar — FerrisFeed

Release checklist. CI (`android.yml`) enforces the automatable rows; the
rest are manual gates on the release issue template.

## 1. Rust core (`rust-core/`)

- `cargo test` — FSRS scheduler (stability up on Got it, down on Again),
  quiz grader (MCQ / tap-the-bug / fill-blank), DAG resolver (topo order,
  cycle rejection). Must be green.
- `cargo clippy -- -D warnings` and `cargo fmt --check`. No allows
  without a linked issue.

## 2. JVM unit tests (JUnit 5)

- `LearningPathEngineTest`: DAG acyclic, spine order, placement 11/15
  boundary, Daily Mix 7+8+5 counts, interleave pattern, resume label.
- `ProgressSystemTest`: XP math incl. streak cap, decay `0.98^days`,
  Ferris stage thresholds, all achievement predicates.
- `SearchRepositoryTest`: FTS sanitization (`toMatchQuery`), filter
  combinations, empty-query fallback. Room tests use in-memory DB.
- `ProgressStoreTest`: streak increment/continue/reset, heatmap cap,
  mastery record + decay. Fake DataStore, no device needed.

## 3. Compose UI tests

- `FeedPagerTest`: swipe advances exactly one reel, double-tap saves,
  scroll position restores after process death (StateRestorationTester).
- `QuizCardTest`: selecting correct option shows explanation + awards XP;
  "Still fuzzy" re-queues the card (Fake DAO asserts `nextDueMillis`).
- `SearchScreenTest`: typing filters FTS results; track/level chips apply.
- All tests run on API 26 + API 34 emulators (min + target).

## 4. Screenshot tests (Roborazzi)

- Golden set: ReelCard (dark/light/dynamic), CodeCard (rust/wat/toml),
  QuizCard (unanswered/correct/wrong), TrapCard, ProgressRing 62%,
  StreakFlame, TrackPill per track. Recorded at 411x891 (Pixel 6).
- Threshold 0.5% pixel diff; failures attach before/after PNGs in CI.
- Regenerate goldens only via `/shot-record` with reviewer approval.

## 5. Accessibility (TalkBack audit)

- Every interactive element has a content description ("Save reel",
  "Quiz option 2 of 4", "Mastery 62 percent, Ownership").
- Minimum touch target 48x48dp (verified by a lint rule + manual check
  on QuizCard options and pager actions).
- Contrast: body text >= 4.5:1 on OLED background in both themes.
- Full TalkBack pass on the feed + quiz + roadmap before every minor
  release; issues filed as P1.

## 6. Size + performance (R8 < 25 MB)

- R8 full mode + resource shrinking; APK analyzed by `bundletool`.
  Budget: base < 25 MB (universal APK), asset DB + WebP < 12 MB of that.
- Baseline profile (`app/baseline-prof.txt`) installed; Macrobenchmark
  asserts feed scroll P99 frame time and cold start on Pixel 6.
- 120Hz `VerticalPager` prefetch-5; no network on scroll path; strict
  `StrictMode` clean in debug.

## Release gate

`cargo test` + `./gradlew :app:assembleDebug :app:testDebugUnitTest` +
screenshot diff + TalkBack sheet + size report attached to the release
issue. Any red row blocks the beta track.
