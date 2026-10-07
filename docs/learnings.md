# FerrisFeed — Session Learnings (CI Green Run, Oct 2026)

This file exists so the next agent (human, Copilot, or otherwise) does not
re-fight battles this session already won. Each entry is a real CI failure
from the push → watch → fix loop, with root cause and the fix that worked.
Read this before touching build files, dependencies, or DI.

How the loop works (proven over ~10 cycles):
```bash
git push origin main
./scripts/ci-watch.sh main 900        # polls, dumps failing logs on red
RUN=$(gh run list --branch main --limit 1 --json databaseId --jq '.[0].databaseId')
gh run view $RUN --log-failed | grep -E "e:|error:" | sort -u   # real errors only
```

---

## 1. Never trust scaffolded Maven coordinates — verify them first

**Failures:** `Could not find io.coil-kt:coil-network-okhttp:2.6.0` and
`Could not find androidx.baselineprofile:baselineprofile-consumer:1.3.4`.

**Root cause:** The scaffolding subagent invented both artifacts. Neither has
ever existed on any Maven repository:
- `coil-network-okhttp` is a Coil **3.x** artifact (group `io.coil-kt.coil3`).
  Coil 2.x does OkHttp networking inside its core artifact.
- There is no `baselineprofile-consumer` library. The Gradle plugin version
  (1.3.4) is real; the library is not. The runtime counterpart is
  `androidx.profileinstaller:profileinstaller`.

**Fix:** Deleted the coil line (`coil-compose` already brings networking);
replaced the phantom with `androidx.profileinstaller:profileinstaller:1.3.1`.

**Rule:** Before adding ANY dependency, fetch its `maven-metadata.xml`
(`repo1.maven.org` for `io.*`/`com.*`, `dl.google.com/dl/android/maven2`
for `androidx.*`). A 404 there means the coordinate is fiction — do not
"fix" it by bumping the version.

## 2. Version catalog: never declare `libs` explicitly

**Failure:** `Multiple 'from' invocations … you can only call the 'from'
method a single time` on every Gradle invocation, including `gradle wrapper`.

**Root cause:** `settings.gradle.kts` contained
`versionCatalogs { create("libs") { from(files("gradle/libs.versions.toml")) } }`.
Gradle auto-loads `gradle/libs.versions.toml` as `libs`, so the explicit
`from(...)` counts as a second import and fails on Gradle 8.7+.

**Fix:** Delete the whole `versionCatalogs` block. Auto-discovery is the
entire mechanism; the explicit block is always wrong.

## 3. AGP + Gradle + compileSdk move as a locked triple

**Failure:** 27 `checkDebugAarMetadata` errors: Navigation3 alpha08 needs
compileSdk 36 + AGP 8.9.1+, Compose 1.9.0 needs compileSdk 35+, while the
project declared compileSdk 34 + AGP 8.6.1.

**Fix (all three together, never one alone):**
- AGP 8.6.1 → **8.9.2** (verified the POM exists on Google Maven first)
- `compileSdk` 34 → **36** in all five modules (`:app`, `:core-ui`,
  `:feature-feed`, `:feature-path`, `:data`). `targetSdk` stays 34 and
  `minSdk` stays 26 — compileSdk is independent of both.
- Gradle wrapper 8.10.2 → **8.11.1**, because AGP 8.9.2 refuses anything
  older (`Minimum supported Gradle version is 8.11.1`). Hit this locally
  right after the AGP bump.
- CI SDK install switched to `platforms;android-36` + `build-tools;36.0.0`
  to match.

**Rule:** When the AAR-metadata task complains, believe it and upgrade the
triple; do NOT downgrade pre-release libraries to older pre-releases.

## 4. The scaffold wrote manifests referencing resources it never created

**Failure:** AAPT `resource mipmap/ic_launcher / string/app_name /
mipmap/ic_launcher_round / style/Theme.FerrisFeed not found`.

**Root cause:** `app/src/main/` had code but zero `res/` directory, and all
four library modules (`core-ui`, `data`, `feature-feed`, `feature-path`)
had no `AndroidManifest.xml` at all (required for `com.android.library`).

**Fix:** Added `strings.xml`, `colors.xml`, `themes.xml` (framework
`NoActionBar` parent — the real theme lives in Compose, the manifest theme
only covers cold start), adaptive icons under `mipmap-anydpi-v26` (covers
everything since minSdk is 26), minimal `<manifest />` files (namespaces
come from build scripts), `consumer-rules.pro` stubs per library, and
`app/proguard-rules.pro` (referenced by the release build type — added
preemptively before it could fail next).

## 5. Compose API availability follows the RESOLVED version, not the docs

**Failures:** `Unresolved reference 'Reject'`, then after "fixing" it,
`Unresolved reference 'VirtualKey'` on `HapticFeedbackType`.

**Root cause:** `Confirm`/`Reject`/`VirtualKey`/etc. joined
`HapticFeedbackType` in Compose UI **1.8.0** (AOSP commit for bug 370482624).
BOM 2024.10.01 pins UI 1.7.x, so neither member existed on the compile
classpath. The scaffold author wrote code against newer docs.

**Fix:** Bumped BOM 2024.10.01 → **2025.04.01** (verified its POM pins UI
1.8.0), restored `Reject`. This also unified versions: the app module was
already getting UI 1.9.0 transitively via Navigation3, so modules were
compiling against two different Compose versions.

**Rule:** `docs/tech-stack.md` now records the constraint: never downgrade
the BOM below one shipping UI 1.8.0. When the compiler says a Compose
member is missing, check the BOM's UI version before doubting the code.

## 6. DataStore: `Preferences` reads, `MutablePreferences` writes

**Failure:** `No 'set' operator method providing array access` ×4 in
`ProgressStore.recordActivityLocked`.

**Root cause:** The helper declared its parameter as immutable `Preferences`
but assigned into it. Call sites inside `edit { }` were fine; only the
helper signature was wrong.

**Fix:** Parameter type → `MutablePreferences`. One-line change; check every
`*Locked(prefs: ...)` helper when you see this error.

## 7. Large-arity `combine()` does not resolve — nest instead

**Failure:** 7-flow `combine(...)` in `FeedViewModel` produced
`SuspendFunction7 ... was expected (Array<T>)` plus cascading inference
errors.

**Fix:** Restructured to nested 4+3+2 combines with two tiny private
`data class` holders. Arity ≤4 has existed in every coroutines version;
do not use 5+ arity heterogeneous combines in this codebase, ever.

## 8. Every module must declare what it imports

**Failures:** `Unresolved reference 'datastore'` / `intPreferencesKey` /
`DataStore` in `:feature-feed`; `Unresolved reference 'glance'` ×9 in
`Widget.kt`.

**Root cause:** Code imported DataStore and Glance APIs whose artifacts were
never added to the module's `dependencies`. The scaffold wrote imports
against a wishlist.

**Fix:** Added `datastore.preferences` to `:feature-feed`, and
`glance-appwidget` + `glance-material3` (both **1.2.0**, verified via
metadata — `GlanceTheme` lives in `glance-material3`, not `glance-appwidget`).

**Rule:** When you see `Unresolved reference '<top-level package>'`, check
the module's build file before the code. Cross-check used the same way for
sibling modules (only `:feature-feed` was missing it).

## 9. Glance APIs were partly hallucinated — use the documented patterns

**Failures (after adding the deps):**
- `Unresolved reference 'PreferencesGlanceState'` + `preferences` — no such
  type. Real pattern: `override val stateDefinition =
  PreferencesGlanceStateDefinition` plus `currentState<Preferences>()`.
- `actionStartActivity(Intent)` — no Intent overload exists in Glance 1.2.0
  (compiler suggested `ComponentName`). Glance actions must be
  activity/class/component based so hosts can serialize them. Fixed with
  `ComponentName(packageName, "com.ferrisfeed.app.MainActivity")`
  (string literal keeps `:feature-feed` decoupled from `:app`).
- Compose `Color` passed as Glance `TextStyle` color — Glance wants
  `androidx.glance.unit.ColorProvider`. Wrap at the definition site.

## 10. AppModule referenced an imagined package layout — inventory first

**Failure:** KSP/Hilt `error.NonExistentClass` on every binding:
`com.ferrisfeed.data.{local,progress,repo}.*`, plus `ProgressDao`,
`PathRepository`, `FeedRepositoryImpl` — none of which exist. The DI author
and the data author never agreed on packages.

**Fix:** Rewrote `AppModule` to reference only verified types
(`FerrisDatabase`, `ReelDao`, `ProgressStore(context)`,
`DefaultFeedRepository(local)`), dropped `provideProgressDao` (SRS columns
live on the reel rows via `ReelDao`) and `providePathRepository` (nothing
consumes it), and made the feed DataStore a real `@Singleton` on its own
file (`ferris_feed` — two DataStores must never share one file).

**Rule:** Before writing DI, run the inventory:
`grep -rn "^class\|^interface\|^object\|^data class" --include="*.kt"`
per module. If a type is not in the output, it does not exist no matter
what the docs claim. Companion gaps found the same way: the feed fetched
from Room through NOBODY — `FeedViewModel` only ever read the repository's
empty in-memory queue — so `RoomReelDataSource` (Room→UI models, quiz-JSON
parsing, DataStore-backed saved/liked, FSRS-lite-lite grading via
`ReelDao.applyReview`) had to be written, plus `allReels()/savedIds()/
likedIds()` on the repository interfaces, Hilt construction for the
ViewModel (`@HiltViewModel` + `@Inject`; `clock`/`random` became `var`s
because Dagger does not support default constructor args), and a
`focusReel()` for deep links.

## 11. MainActivity was written against imaginary screen APIs

**Would-be failures:** Wrong packages (`com.ferrisfeed.feature.feed` vs
real `com.ferrisfeed.feed`, `coreui.theme` vs real `coreui`) and wrong
call signatures (`FeedScreen(onOpenReel=…, onOpenPath=…)` vs real
`(viewModel, onRunCode, focusedReelId?)`, etc.).

**Fix:** Read each screen's real signature first, then wired the nav host
with Hilt (`hiltViewModel()`, field-injected `ReelDao` for search),
`defaultPathNodes()` for the path tab, Room FTS mapped to `SearchResult`
for search, and a Rust-playground intent for code runs. Note the codebase
has TWO `SearchFilters` classes (`:feature-feed` UI vs `:data` repo) —
the nav host maps between them with an import alias.

## 12. `contentOrNull` is not in kotlinx.serialization 1.7

**Failure:** `Unresolved reference 'contentOrNull'` in new code.

**Fix:** `runCatching { obj[key]?.jsonPrimitive?.content }.getOrNull()`
per key. (`intOrNull` DOES exist — inconsistent, just memorize it.)

## 13. CI workflow lessons (all fixed, do not regress)

- **No `gradlew` was committed.** CI ran `./gradlew` against nothing.
  Generated the wrapper with Gradle 8.10.2 (later 8.11.1, see §3) and
  committed `gradlew`, `gradlew.bat`, `gradle/wrapper/*`.
- **Node 20 deprecation:** `checkout@v4`, `setup-java@v4`,
  `upload-artifact@v4`, `setup-android@v3`,
  `gradle/wrapper-validation-action@v2` → `@v5`/`@v5`/`@v5`/`@v4`/
  `gradle/actions/wrapper-validation@v3`. Pinned `ubuntu-24.04`.
- **`setup-android@v3` installs the removed `tools` SDK package**
  (`sdkmanager failed with exit code 1`). Fixed by `@v4` plus an explicit
  `sdkmanager` step installing only `platform-tools`,
  `platforms;android-<compileSdk>`, `build-tools;<x>.0.0`. Nothing in the
  repo ever referenced `tools` — the failure came from the action's
  defaults.
- **Test-report upload warned "no files found"** while the build was red
  (nothing had run). Added `if-no-files-found: warn` so the warning cannot
  mask the real error again.
- A Copilot session works this repo in parallel: expect
  non-fast-forward push rejections → `git fetch` + `git rebase origin/main`
  (its merged change was one harmless `continue-on-error` line). Close its
  duplicate `copilot/fix-build-and-test-job-again` branch if still open.

## 14. Machine / auth notes (local Mac)
- `gh` CLI was authed as `kinshuk-bb`, invisible to the
  `kinshuksinghbist` private repo (API 404s). Fixed by granting access;
  if `gh run` 404s again, check `gh auth status` identity first.
- No-sudo JDK install: `brew install openjdk@17` (formula, no sudo) works;
  the `temurin@17` cask needs an interactive password and fails headless.
- Gradle spawns lingering daemons: verify Android changes with
  `./gradlew <task> --no-daemon`, and `pkill -f GradleDaemon` afterwards.
  Full `assembleDebug` needs the Android SDK — not installed locally, so
  local verification stops at `./gradlew help` (configuration) and CI
  proves the rest. Do NOT install the SDK to "verify faster"; the machine
  has 8 GB RAM and was already swapping.
- `contentOrNull`… see §12. `kotlinx.serialization` stays at 1.7.3.

## 15. Known gaps left for next session (CI is green, app is not done)

- **First-launch crash FIXED after this was written:** `createFromAsset`
  was removed; `:app:syncCurriculumAssets` mirrors `content/*.json` into
  generated assets and `:data CurriculumSeeder` populates Room on first
  `onCreate`, with `FeedViewModel` waiting up to 30s for the first rows.
  A release pipeline may still prebake a `.db` later — if it does, keep the
  seeder as fallback and never hand-write the SQLite (Room identity hash).
- Two deprecation warnings remain (`LocalClipboardManager` →
  `LocalClipboard`, `Icons.Filled.MenuBook` → AutoMirrored). Warnings only.
- Widget manifest registration (`ReelOfDayWidgetReceiver` +
  `appwidget-provider` XML) still listed as TODO in `Widget.kt` KDoc.
- `targetSdk` is still 34; Play Store now expects 35+ for new listings —
  product decision, flagged in `docs/play-listing.md` context.

## 16. Never write a glob like star-slash inside a block comment in .kts

Bisected 2026-10-06 across six variants: a `/** ... */` KDoc containing a
slash-star sequence (e.g. documenting a glob as `content/*.json`) breaks
`:app` configuration with phantom `compileSdk not specified` +
`hilt-android dependency not found` errors. Six runs agree: any variant
with the sequence fails, without it passes — including byte-identical code
differing only in comments. Probable mechanism is Kotlin nested-comment
handling swallowing subsequent script statements so the `android` block
half-evaluates. Two consequences: (a) keep ALL build-script comments
slash-star-free (spell out "star" or reword; `app/build.gradle.kts` carries
this warning inline), (b) `exclude()` belongs at Copy-task level anyway —
never nested inside `from(...) { }`. When a config error contradicts the
file in front of you, suspect comment corruption before logic, and bisect
with byte-exact variants (assert every test edit actually applied).

## 17. Verify APK contents, not just task execution + skeleton states

- `assets.srcDir(taskProvider)` does NOT pull the task into AGP's asset
  merge: the green build ran zero sync tasks and shipped zero curriculum
  files. Fix is an explicit
  `tasks.matching { merge.*Assets }.configureEach { dependsOn(sync) }`.
  Proof pattern that caught it: `gh run download` the APK and
  `unzip -l | grep assets/` — count the files, do not trust the task log.
- Related: a `Sync` from `content/` into `generated/curriculum/` merges at
  the APK ROOT (`assets/rust/...`, not `assets/curriculum/rust/...`).
  Code reading bundled assets must use the merged path; assert with the
  same unzip listing.
- UI lesson from the same incident: an empty feed rendered a zero-page
  pager (blank) while a stuck load rendered skeletons forever, and the user
  could not tell them apart. `FeedScreen` now has a dedicated empty state
  with retry, and `FeedViewModel.retryLoad()` re-attempts the wait.

## 18. Fused import lines are the recurring self-inflicted break (Spec v2 session)

**Failures:** two red CI runs, same shape:
`FeedViewModel.kt:14:55 Expecting a top level declaration` and
`CodeCard.kt:7:56 …` plus `imports are only allowed in the beginning of file`.

**Root cause:** an edit that replaced one import line glued the next one onto
it (`…MutableStateFlowimport kotlinx.coroutines.flow.SharingStarted`,
`…layout.Columnimport androidx.compose.foundation.layout.Row`). The column
numbers in the error are the giveaway: a single line producing a cascade of
"Expecting a top level declaration" errors.

**Fix:** split the line. **Rule:** after ANY edit that touches an import
block, re-read lines 1–30 of that file and assert them; when the compiler
points at one line with 6+ cascading errors, look for a fused line before
doubting anything else.

## 19. Compose 1.8 `LocalClipboard` is not a drop-in for the old manager

**Failure:** `CodeCard.kt:117:35 Unresolved reference 'setText'.`
`LocalClipboardManager.setText` is deprecated in UI 1.8, but
`LocalClipboard.current` (the replacement) has no `setText` — its `ClipEntry`
API is host-version sensitive.

**Fix:** use the platform clipboard, which has no deprecation and no
Compose-internal types:
`LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager`
+ `setPrimaryClip(ClipData.newPlainText("code", code))`. Both deprecation
warnings from §15 are now gone as a side effect.

## 20. Room projections from `@Query` need matching property names

`ReelDao.countByTopic` returns `List<TopicCount>`: the SQL alias must match the
Kotlin property (`COUNT(*) AS reelCount` → `val reelCount: Int`) and the class
must be a top-level POJO. Query-only projections like this give the path screen
real data (topic + reel count + `MIN(level)`) with **zero migration** — no new
column, no schema bump.

## 21. Never nest a `LazyColumn` inside a `verticalScroll` column

The Path screen now hosts search results above the roadmap. A `LazyColumn` of
results inside the old `verticalScroll(rememberScrollState())` column crashes at
measure time (infinite max height). **Fix:** the screen is ONE `LazyColumn` —
search section as an `item`, results as `items`, roadmap stages as
`itemsIndexed`. Same rule applies to any future "section above a list" work.

## 22. Navigation3 routes do not guarantee a per-route ViewModel

`hiltViewModel()` inside a `NavEntry` can resolve to a coarser
`ViewModelStoreOwner`, so ViewModel state may survive across routes and a
`SavedStateHandle`-based route argument cannot be relied on. **Pattern used
everywhere instead:** pass the route argument explicitly
(`FeedEntry(topicFilter = key.topicId)`) into an idempotent setter, and have the
setter return its rebuild `Job` so the caller can `join()` before doing
order-dependent work (`setTopicFilter(topic)?.join()` then `focusReel(id)` —
the new queue must exist before the focus lookup).

## 23. `JsonNull.content` is the literal string "null" (TODO 21 session)

kotlinx.serialization's `JsonNull` is a `JsonPrimitive`, so
`element.jsonPrimitive.content` on a JSON `"key": null` returns the string
`"null"` — the seeder would store the five-character word as a real value.
The seeder's text reader now short-circuits `JsonNull` explicitly (and wraps
`quiz` with `takeUnless { it is JsonNull }` before serializing), pinned by
`CurriculumSeederLabelTest`. **Rule:** when reading optional primitive fields
by hand, check `is JsonNull` before reading `content`.

## 24. Content packs carry several JSON house styles — never re-format them

The 22 seeded packs are pretty-printed OR single-line, ASCII-escaped OR
unicode-literal, sorted OR authored key order, with or without trailing
newline — and APK asset diffs must show only the new field.
`scripts/backfill_topic_labels.py` therefore tries serializer variants
(indent / separators / sort_keys / ensure_ascii) until the label-free re-dump
is byte-identical to the original and refuses to write when none matches.
**Rule:** any script that edits `content/` must prove the no-op round trip
per file before writing.

## 25. Redesign session (TODO 23–28): what the green run proved

- `rememberLauncherForActivityResult` lives in `androidx.activity:activity-compose`.
  A feature module using it must declare the dependency itself — the `:app`
  module having it does NOT leak it downstream. Same catalog entry, no new
  coordinate (learnings §1 still holds: it is already used by `:app`).
- Platform `SpeechRecognizer` needs no dependency and no new permission
  beyond `RECORD_AUDIO`. `EXTRA_PREFER_OFFLINE` is compile-safe on minSdk 26
  (constant inlined from compileSdk 36, ignored by old recognizers at
  runtime). Always `destroy()` the recognizer in a `DisposableEffect`
  keyed by reel id — pagers compose ahead and leak listeners otherwise.
- `GlanceAppWidgetManager.getGlanceIds(provider: Class<T>)` takes the
  WIDGET class (`ReelOfDayWidget::class.java`), not the receiver. Verified
  against the developer reference before writing `WidgetRefreshWorker`.
- `targetSdk` 34 → 35 is a one-line bump with the AGP 8.9.2 / Gradle 8.11.1 /
  compileSdk 36 triple untouched (learnings §3). Bump `tools:targetApi` in
  the manifest alongside it.
- Gating animation on state (`DifficultyLabel(animated)`, false until
  `Heard`) keeps motion as reward without blocking content: text composes
  immediately, the cue plays after capture. Same pattern applies to any
  future "celebrate after X" motion.
- Quiz option cards need `defaultMinSize(minHeight = 48.dp)` — short options
  otherwise ship sub-48dp targets. Rail IconButtons already use 48dp
  `Modifier.size` with 28dp glyphs; do not shrink them.

## 26. Two latent breaks the TODO 22 commit left (found closing out P5)

- A subjectless `when` with `//` comments between its entries failed
  `:feature-path` compilation (`138:68 Expecting a when-condition` on runs
  37582567262 + 37590339813 — red since the TODO 22 push, tests masked).
  Rewritten as a hoisted if-else caption val with comments above it
  (PathScreen results header). **Rule:** keep comments out from between
  `when` entries in this codebase; if the compiler points at an `else`
  line-end with no visible cause, rewrite as if-else instead of debating it.
- `:data SearchFilters` defaulted `maxLevel = 3` while the content schema
  runs 1..4 and `PathViewModel` maps unset levels to 1..4 — unfiltered
  browse silently hid every advanced reel, and `PathBrowseIaTest` (which
  expects 4) failed once compilation passed. Fixed the default to 4
  (+ `isEmpty`). **Rule:** level bounds live in exactly two places —
  the content schema doc and these two defaults — and all three must agree.

## 27. P6 Design-Studio Overhaul Learnings

- **Bundled Font Resources**:
  - Android AAPT requires font resource filenames under `res/font/` to be strictly lowercase alphanumeric with underscores (`[a-z0-9_]`). Hyphens or uppercase names (`SpaceGrotesk-Medium.ttf`) fail the resource merge task.
  - Core-UI namespace generates `R.font.*` seamlessly without new Maven dependencies.
- **Compose Foundation FlowRow is Stable in Compose 1.8+**:
  - `androidx.compose.foundation.layout.FlowRow` is fully stable and does not require experimental annotations (`@OptIn(ExperimentalLayoutApi::class)` is obsolete).
- **Navigation3 Transition Animation Spec Names**:
  - In Compose 1.8.x Navigation3 (`NavDisplay`), use `togetherWith` rather than legacy transition DSLs. Keep screen crossfade durations to 200–350ms.
- **GraphicsLayer for 120Hz Pager Transforms**:
  - Per-page 3D perspective transforms (`scale`, `alpha`, `rotationX`, `cameraDistance`) must read pager offset state exclusively inside the `Modifier.graphicsLayer { ... }` block to skip recomposition during vertical fling.
- **Accessibility `liveRegion` on Error Animations**:
  - When triggering error animations (such as horizontal shake on wrong MCQ or block selection), setting `liveRegion = LiveRegionMode.Polite` in Compose semantics ensures TalkBack immediately announces the state to the user without interrupting critical announcements.
- **Font Scale Safety Valve**:
  - Strict no-inner-vertical-scroll rule prevents nested scroll conflicts; however, when system `fontScale > 1.3f`, enabling `Modifier.verticalScroll(scrollState)` and scaling hook text to `headlineSmall` guarantees that large text is never clipped on low-height devices.

