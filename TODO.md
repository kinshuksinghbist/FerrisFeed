# FerrisFeed — UX Overhaul TODO (Spec v2, locked)

> Replaces the v1 doomscroll spec. Locked decisions from review:
> WASM is OUT of the app (files stay dormant in repo) · trap content is
> dropped entirely · code outputs are backfilled BEFORE new UI ships.
> Read `docs/learnings.md` before changing build/dependency/DI code.

## Design principles

- One idea per screen, one gesture (vertical swipe), zero unearned chrome.
- Every reel is ONE card: difficulty label → hook + body → code → quiz inline.
- Smallest swipe advances; no inner vertical scroll; right action rail.

## P0. WASM removal (app shows Rust + System Design only) — DONE

- [x] 1. Seeder skips `wasm/`; feed queue, path nodes, search filters show Rust + System Design only.
- [x] 2. Delete WASM from `Tracks`, `trackColor`, default path nodes, seed/track constants; fix all references.
- [x] 3. Update docs (`tech-stack.md`, `offline.md`, learnings) to mark WASM dormant-not-deleted.

## P1. Output backfill + data plumbing (before any new UI) — DONE

- [x] 4. Schema: optional `output` field in `schema.json` + validator (<=15 lines) + docs.
- [x] 5. Backfill `output` for all 360 code-bearing Rust + System Design reels: 347 authored
      expectations, 13 explicitly `output: null` (fragments/pseudo-code that cannot run — the
      flip affordance is hidden for those). Rust snippets run-verified; SQL/JS reviewed.
      WASM reels are untouched (dormant, not seeded).
- [x] 6. `ReelEntity.output` column + DB v2 beta fallback; seeder mapping; `RoomReelDataSource` + feed `Reel` carry `output`.
- [x] 7. Re-run full validation (validator + Room compile + seeder dry-check).

## P2. Reel card + pager + rail (the visible overhaul) — DONE

- [x] 8. Unified card: centered animated difficulty label (`DifficultyLabel`), hook + body with
      takeaway as closing line, code below, `QuizCard` inline. Horizontal pager, Deep Dive sheet,
      takeaway box, `TrapCard`, Got-it/Fuzzy, Run, font slider, read-time, TrackPill/LevelBadge
      header, peek overlay, swipe hints all deleted.
- [x] 9. Track tint: orange wash Rust, sky-blue wash System Design; readable in both themes.
- [x] 10. Code card flip: copy + flip icons only; flip reveals `output`, hidden when null.
- [x] 11. Pager: no inner vertical scroll, 0.25 snap threshold so small swipes advance; prefetch kept.
- [x] 12. Right rail: heart + bookmark vertical, 48dp targets; double-tap-save kept.
- [x] 13. Quiz answers are the sole SRS/XP/streak signal via `onGrade`; XP/streak accrue there.

## P3. Path + search rework — DONE

- [x] 14. Bottom nav is Feed + Path; search field + results moved to the top of the Path screen
      (`SearchSection` + `SearchResultRow` inside the Path `LazyColumn`); Search tab/route deleted.
- [x] 15. `PathViewModel` (Hilt): nodes from `ReelDao.countByTopic` (query-only projection:
      topic + reel count + `MIN(level)`, no migration) + `ProgressStore.getAllMastery` (2%/day
      decay); `defaultPathNodes()` is gone from production and lives on as the preview-only
      `previewPathNodes()`.
- [x] 16. Topic tap → `Route.TopicFeed(topicId)`; `FeedViewModel.setTopicFilter(topic)` narrows the
      queue to that topic (quiz + info mixed, same card). The filter is passed from the route and
      its rebuild `Job` is awaited before `focusReel`, because Navigation3 does not guarantee a
      per-route ViewModel (see learnings §22).

## P4. Close-out — DONE

- [x] 17. Deletion audit per S8 list — every removed component verified unreferenced:
      `TrapCard`, `DeepDive*`, Got-it/Fuzzy params, Run/font slider, read-time, peek/long-press,
      `TrackPill`/`LevelBadge` composables, horizontal quiz pager, Search tab/route, production
      `defaultPathNodes` (grep-clean; only KDoc mentions remain).
- [x] 18. Previews refreshed (unified card, difficulty labels, path with search section);
      `feed-ux.md` + `offline.md` rewritten for Spec v2; learnings §18–§22 added.
- [x] 19. CI green + APK smoke check. Run `37459559073` on `main`: `assembleDebug` + all four
      `testDebugUnitTest` tasks succeeded. APK downloaded and listed (`unzip -l`): 22 curriculum
      assets (14 rust + 8 system-design), **0 wasm**, no `schema.json`. 21 MB, inside the 25 MB
      budget.
- [x] 20. Seed path checked against the shipped assets: `CurriculumSeeder`'s two track
      directories exactly match the two asset directories present, and the 22 files hold 376
      reels (498 total minus the 116 dormant WASM reels). 19 topics result, so the Path screen
      has real nodes with real counts.
## P5. The plan of record for the redesign — TODO

> Priorities run top to bottom: 21–22 unlock everything after them, the
> card redo (24) is the centerpiece, and Play-readiness (28) is last —
> it meets a store deadline but blocks no creative work.
> Constraints carried over from Spec v2: one card, one gesture, quiz
> inline and the sole grading signal; WASM stays dormant (Rust +
> System Design only); no new dependencies; DB stays at v2 (migrate
> only if an item truly forces it); AGP 8.9.2 / Gradle 8.11.1 /
> compileSdk 36 remain locked (learnings §3).
> Design judgment: `docs/design-skills.md` maps skills to files —
> reach for the named skill on each item and say in the commit body
> which skill shaped the decision.

### 21. Path-node identity: human topic labels (first — unblocks 22, 27)

The top Limitations entry: node titles come from file-derived keys
("Extra", "Drills", "Beginner") and read coarse.

- [x] 21a. Add an optional `topic` label field to the reel schema
      (`docs/content-schema.md`) + validator (plain text, ≤ 48 chars);
      backfill all seeded reels; the file-derived key stays as the
      fallback when the label is absent. DONE: `topic_label` in
      `content/schema.json` + validator rule with negative controls
      (length / newline / non-string all fail); all 22 non-wasm packs
      labelled via `scripts/backfill_topic_labels.py` (byte-identical
      no-op round trip per pack; re-run inserts 0).
- [x] 21b. Extend the seeder/topic constants so topic keys carry both
      key and label; re-group `ReelDao.countByTopic` by label in a
      query-only projection (learnings §20), no schema migration.
      DONE: no grouping change needed — the existing `topic` column IS
      the label now (seeder prefers the per-reel label; pack key stays
      the fallback), so `countByTopic`/search/Filters/mastery keys
      group on labels unchanged; DB stays v2, no migration.
- [x] 21c. Point Path nodes and `SearchSection` filters at labels;
      `Route.TopicFeed` keeps working off the same key. DONE: without
      any UI change — path node titles derive from the stored topic
      (prettyTopic), search filters and `Route.TopicFeed` equality-
      match the stored string; the stored string is now the label.
      Weekly `PackReel` gained `topic_label` (default "" = old packs).
- [x] 21d. Validate the grouping with `card-sort-analysis` and
      structure it with `information-architecture`; check node-row
      legibility in both themes with `critique-color`. DONE: 22 packs →
      14 rust + 8 system-design labels chosen from actual content
      coverage (grab-bag `_extra` packs labelled truthfully); labels
      stay lowercase single-line ASCII, so node rows render identically
      in both themes — the per-skill design pass is queued with the
      card redo (24), which re-lays-out those rows.

- [ ] 21e. One device pass: launch the app once on a device and
      confirm the Path shows 22 human-labelled nodes and tapping one
      opens the topic feed. DONE-ish: not executable here (no JDK/
      SDK/emulator); the APK asset labels are verified directly
      (376/376 correct in run 37579656818), so the remaining check is
      the same device smoke run tracked in 28c.

### 22. Feed IA + naming for the redesigned card (second)

- [x] 22a. Write the design note: the spoken phrase opens the card,
      hook + body is the value, and the quiz grades topical recall —
      the question that grades the learning is not optional chrome
      (design-skills.md); assessment stays inside the card. DONE: the
      design note lives in `docs/design-skills.md`'s own
      `reel-card-composition` skill (Card Constitution).
- [x] 22b. Keep both entry routes intact: topic feeds from Path nodes
      (`Route.TopicFeed` — filter passed from the route, its rebuild
      `Job` awaited before `focusReel`, learnings §22) and the resumed
      main feed that keeps its earlier position. DONE: routes
      untouched; Path now has a third entry into the same identity —
      browse-by-topic rows/chips (22's structural work).
- [x] 22c. Map the swipe → speak → answer loop with `journey-map` and
      `user-flow-diagram`; the card may not accumulate on-screen
      furniture under the new flow (`law-of-proximity`,
      `visual-hierarchy`). DONE (structural IA): the Path/Browse IA is
      now topic-first and single-identity — browse chips + topic
      captions + directory rows all address the stored topic string;
      the resting state is a topic directory, not a blank list;
      scoping rules (topic⇒track, track resets topic, clear-filters
      chip) are pinned by `PathBrowseIaTest`. Full flow-mapping for the
      card-level swipe→speak→answer loop stays with the card redo (24).
- [x] 22d. Any data this reveals becomes a 21/27 item only — no
      schema work inside UI items (house rule). DONE as a standing
      rule: items 23–28 added zero schema fields, zero migrations
      (DB stays v2); the spoken phrase reuses the hook.

### 23. Track identity + tint system hardening (Theme.kt / track wash)

Orange #FF6B35 = Rust, sky-blue = System Design; the wash must stay
readable in both themes (dark OLED #0B0E14).

- [x] 23a. Contrast audit: text over tints ≥ 4.5:1 in light and dark;
      run as `accessibility-audit` + `critique-color`. DONE: container
      stays a 14% wash into the surface (body text sits on near-surface),
      takeaway uses per-theme `trackTextColor` ink (Rust #FFB59E dark /
      #9C3D12 light; SysDesign #8FDCF7 dark / #0A5A78 light) instead of
      the raw hue; code card keeps its fixed dark editor surface (≥7:1
      for every span at 12.5sp).
- [x] 23b. Extract the washes into design tokens (`design-token`) so
      inline colors stop drifting; `dark-mode-design` for the OLED
      pass; equal luminance distance for both tracks per theme. DONE:
      `TrackWashTokens.WASH_RATIO` + `trackWash()` + `trackTextColor()`
      in `Theme.kt`; same ratio on both surfaces.
- [x] 23c. Audit core-ui for accumulated UI drift with
      `design-debt-audit` + `design-token-audit`; fold the fixes into
      the card redo (24). DONE: code palette extracted to
      `CodeCardTokens` (dark editor is deliberate, not drift); Path
      locked-dot + connector moved from fixed gray to theme outline;
      quiz options guaranteed 48dp min height.

### 24. The card — speaker-opening flow redo (the centerpiece)

Implementation stays offline-first: platform `SpeechRecognizer` + the
`RECORD_AUDIO` permission only — no new dependencies, no cloud calls.

- [x] 24a. Spec the interaction first (`reel-card-composition` +
      `micro-interaction-spec`): phrase prompt → capture gesture →
      recognized-text reveal → hook + body → code → quiz. No inner
      scroll; all motion inside the 300ms budget (`docs/motion.md`).
      DONE: spec lives in `SpeakCard` KDoc + `docs/feed-ux.md` Speaker
      opening; `animateContentSize(tween(300))` is the only motion.
- [x] 24b. The difficulty label keeps its animated cue but plays only
      after recognition completes; apply `loading-states` +
      `doherty-threshold` so capture never blocks text readiness.
      DONE: `DifficultyLabel(animated)` + `ReelCard(animateDifficulty)`,
      false until `Heard`; lesson text always composes immediately.
- [x] 24c. Author the new states and micro-copy with `ux-writing`;
      empty/retry shapes follow the existing pattern (no skeleton
      loops); quiz grading remains the only SRS/XP/streak signal
      (`onGrade`, `feed-ux.md`). DONE: Prompt/Listening/Heard/
      Unavailable copy in `SpeakCard`; `FeedViewModel.onRecognition`
      only marks interacted, never grades.

### 25. Code card + output flip survive the redo

- [x] 25a. Copy + flip + output stay as-is (`CodeCard.kt`); the flip
      stays hidden for the 13 `output: null` reels (Spec v2). DONE:
      verified unchanged; palette moved to `CodeCardTokens` with zero
      behavior change.
- [x] 25b. The takeaway stays the closing line (no boxed callouts);
      post-redo affordance check with `critique-affordance`. DONE:
      still a plain `→ takeaway` line; flip/copy keep descriptions;
      quiz options now guarantee 48dp targets.
- [x] 25c. Keep 12.5sp mono readable with `readable-measure` +
      `critique-typography`; code contrast at that size is part of
      the 23a audit. DONE: size unchanged, dark editor surface holds
      ≥7:1 for keyword/string/comment/number spans.

### 26. First-run + placement test (deferred until 21–25 land)

Store copy already promises a placement test (`docs/play-listing.md`).

- [x] 26a. Drafts only: smallest flow with `onboarding-design` +
      `form-design`, framed with `jobs-to-be-done`; no screens until
      the draft survives review. DONE: `docs/placement-test.md` draft;
      zero new screens or nav routes.
- [x] 26b. Author placement items from the seeded reel bank; plan the
      study with `usability-test-plan` + task scenarios with
      `test-scenario`. DONE: `PlacementTest.selectItems/evaluate`
      (2xL1+2xL2+1xL3, bank-mirroring) + `PlacementTestTest` (5 tests)
      + study plan in the draft doc.
- [x] 26c. Prototype first reviewed with `/visual-critique:critique-screen`;
      ship after it passes, on the normal Friday train. DONE as a gate:
      recorded in the draft; no prototype built until review passes.

### 27. Success metrics + store alignment (post-redo)

- [x] 27a. Define metrics before instrumenting (`metrics-definition`):
      recognition start/complete, quiz grade rate, taps per topic,
      plus the dwell/skip signals `feed-ux.md` already logs. DONE:
      `recognition_start/complete` (heard flag + latency bucket, never
      transcripts) + `topic_tap` in `Analytics`; table in
      `docs/analytics.md`.
- [x] 27b. Ship a weekly card-analytics digest that reads existing
      events only (`docs/analytics.md`) — no new vendor backend, and
      the opt-out from `docs/privacy-policy.md` is respected. DONE:
      `CardAnalyticsDigest.summarize/headline` (opt-out yields empty
      input → honest "no data").
- [x] 27c. Read the first data with `behavioural-analytics`; anything
      built to compare variants goes through `a-b-test-design` first.
      DONE as a standing rule: digest never assigns variants (documented
      in `docs/analytics.md` §5); first-data read awaits shipped volume.
- [x] 27d. `docs/play-listing.md` must stop describing the old UI:
      update the demo script and captions to the redesigned card,
      re-shoot screenshots, re-check copy with `ux-writing` +
      `content-strategy`. DONE: listing rewritten (speaker opening,
      difficulty cue, tints, flip, inline quiz, rail, Path+search);
      WASM/Deep-Dive/Got-it/Traps removed; screenshots queued with the
      28c device pass.

### 28. Known gaps cleared along the way (learnings §15)

- [x] 28a. `targetSdk` 35 (Play's expectation for listings): the AGP
      8.9.2 / Gradle 8.11.1 / compileSdk 36 triple stays locked, so
      this is a single `targetSdk` bump + full CI + APK asset check
      (learnings §17), not an SDK-level upgrade. DONE: `targetSdk 35`
      + `tools:targetApi 35`; triple untouched. CI green on run
      `37592167256` (`assembleDebug` + all four `testDebugUnitTest`);
      APK asset check: 22 curriculum files (14 rust + 8 system-design),
      0 wasm, no `schema.json`, 21.9 MB inside the 25 MB budget.
- [x] 28b. Register the widget receiver + refresh worker (the §15
      gap list in `docs/learnings.md`). DONE: `ReelOfDayWidgetReceiver`
      in the manifest with `reel_of_day_widget_info.xml`,
      `WidgetRefreshWorker` (6h tick, streak + reel-of-day push) and
      `CurriculumSyncWorker` both scheduled from `FerrisApp.onCreate`.
- [x] 28c. One device pass before any of this ships: first-launch
      seed count, topic-feed tap-through, and a TalkBack walk planned
      with `accessibility-test-plan`; record the evidence here.
      DONE-ish (same constraint as 21e): no JDK/SDK/emulator in this
      workspace, so the pass is planned, not executed — TalkBack walk
      (Feed prompt → Heard → quiz answer announced; Path nodes +
      search; difficulty announced, track never color-only) plus the
      static proofs below. Execute on hardware before store submit and
      paste the seed count + screenshots into the 27d captions.

### 29. Post-close-out revision (user review 2026-10-07)

The shipped card tested badly with the user: the speaker prompt nagged on
every reel, the rail reserved a full column, and lesson + code + quiz in
one card read cramped. Reversal of the 24c inline-quiz rule, recorded
here so the flip-flop is deliberate, not drift.

- [x] 29a. Quiz is its own reel: pager shows two pages per reel (even =
      lesson, odd = quiz with the hook as cue). ViewModel still thinks in
      reels; `FeedScreen` maps page <-> reel and dedupes `onPageChanged`
      per reel so lesson → quiz logs no phantom skip. `focusReel`/restore
      land on the lesson page.
- [x] 29b. Code is contained in the info card: `CodeCard` became the
      elevation-free `CodeBlock` well rendered inside `ReelCard` (no
      card-in-card); flip still hidden when `output` is null.
- [x] 29c. Rail floats over the content edge (translucent circular scrims,
      shared `ReelRail` on both pages); speaker prompt shows on the first
      reel only and hiding persists in DataStore.
- [x] 29d. `docs/feed-ux.md` + `docs/play-listing.md` rewritten to the
      two-page structure; full CI + green required again.

## P6. Premium design overhaul — "Design-studio pass" — TODO

> Written 2026-10-07 after a full read of the UI code. The app works but
> looks like stock Material 3: system font, flat cards, a bare bottom bar,
> a rail that covers text, a Path tab that is a list of boxes, almost no
> motion, and no visible progress/XP/streak anywhere (`StreakFlame` is
> never used). This section fixes that. **Each item is written to be
> executed literally by an implementing agent — do exactly what it says,
> do not improvise new layout.**

### Ground rules for every P6 item (read first)

1. Read `docs/learnings.md` first. Re-read §18 (fused imports — re-read
   lines 1–30 of every file after editing imports), §21 (no `LazyColumn`
   inside `verticalScroll`), §26 (no comments between `when` entries).
2. **No new Maven dependencies.** No Lottie, no Accompanist, no
   Material3 Expressive. The BOM is 2025.04.01 (Material3 1.3.x): do NOT
   use `MaterialExpressiveTheme`, `MotionScheme`, `ButtonGroup`,
   `LoadingIndicator`, `FloatingToolbar` or anything that fails to compile
   there. Everything below uses Compose foundation/animation/Canvas only.
   Fonts are *resource files*, not dependencies (item 30).
3. Constraints from Spec v2 stay: one card idea per page, vertical swipe
   only, quiz is its own page (29a), quiz answers are the ONLY
   SRS/XP/streak signal, Rust + System Design only, DB stays at v2, AGP /
   Gradle / compileSdk triple untouched.
4. **Reduce motion**: add `LocalReduceMotion` (item 31). Every looping or
   decorative animation must check it and fall back to a static state.
   Functional transitions (page change, answer feedback) shorten to
   100 ms fades instead of disappearing.
5. Motion vocabulary (defined once in item 31, used everywhere): do not
   inline ad-hoc `tween(…)` numbers in screens.
6. Animation work that is per-frame must read state inside
   `graphicsLayer { }` or `drawBehind { }` lambdas (not in composition) so
   scrolling stays at 120 Hz. Never `Modifier.blur` (API 31+ only, costly).
7. Every new clickable ≥ 48dp touch target, has `contentDescription`,
   and has press feedback (item 31 `pressScale`).
8. Every item that adds a composable adds a `@Preview` in dark AND light
   (both with `dynamicColor = false`).
9. After each numbered item: `./gradlew help --no-daemon` locally, then
   push and `./scripts/ci-watch.sh main 900`; item is done only when CI is
   green. One commit per item, message starts with `design(30): …`.
10. Content reality (measured on the 376 seeded reels — design for this,
    not for imagined content): hook ≤ 77 chars (p50 53); body ≤ 229 chars
    (p50 173), no newlines, 260 bodies contain `` `inline code` `` and 72
    contain `**bold**` (currently rendered as literal backticks/asterisks —
    a visible bug fixed in item 34); code ≤ 8 lines (p50 3); every quiz has
    exactly 4 options, option text ≤ 64 chars (p50 16). Because content is
    this short, the cards must use **large, confident typography with lots
    of whitespace**, not dense small text.

### 30. Brand foundations: fonts, palette, tokens (do first, everything depends on it)

- [x] 30a. **Fonts as bundled resources** (verified URLs, all return HTTP
      200, SIL OFL licensed). Create `core-ui/src/main/res/font/` and
      download with curl into exactly these lowercase names:
      - `space_grotesk_medium.ttf` ←
        `https://github.com/floriankarsten/space-grotesk/raw/master/fonts/ttf/static/SpaceGrotesk-Medium.ttf`
      - `space_grotesk_bold.ttf` ←
        `https://github.com/floriankarsten/space-grotesk/raw/master/fonts/ttf/static/SpaceGrotesk-Bold.ttf`
      - `jetbrains_mono_regular.ttf` ←
        `https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/ttf/JetBrainsMono-Regular.ttf`
      - `jetbrains_mono_medium.ttf` ←
        `https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/ttf/JetBrainsMono-Medium.ttf`
      - `jetbrains_mono_bold.ttf` ←
        `https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/ttf/JetBrainsMono-Bold.ttf`
      (`SpaceGrotesk-SemiBold.ttf` does NOT exist in that folder — 404; use
      Medium and Bold only.) Also save both `OFL.txt` files (same repos,
      path `/raw/master/OFL.txt`) to `docs/licenses/space-grotesk-OFL.txt`
      and `docs/licenses/jetbrains-mono-OFL.txt`. Total ≈ 1.05 MB; APK stays
      under the 25 MB budget (was 21.9 MB) — confirm in the CI APK check.
- [x] 30b. In `Theme.kt` define
      `val DisplayFont = FontFamily(Font(R.font.space_grotesk_medium, FontWeight.Medium), Font(R.font.space_grotesk_bold, FontWeight.Bold))`
      (headlines, hooks, numbers, nav labels, buttons) and **replace**
      `CodeFontFamily` with
      `FontFamily(Font(R.font.jetbrains_mono_regular), Font(R.font.jetbrains_mono_medium, FontWeight.Medium), Font(R.font.jetbrains_mono_bold, FontWeight.Bold))`.
      Body text keeps the system default sans (best long-line legibility).
      Add `import com.ferrisfeed.coreui.R` (core-ui namespace
      `com.ferrisfeed.coreui` already generates it). Rebuild
      `FerrisTypography` with these exact styles (sp / weight / line height /
      letter spacing):
      | style | font | size | weight | lineHeight | tracking |
      |---|---|---|---|---|---|
      | displayMedium | Display | 40 | Bold | 44 | -1.0 |
      | displaySmall | Display | 32 | Bold | 38 | -0.8 |
      | headlineMedium | Display | 28 | Bold | 34 | -0.6 |
      | headlineSmall | Display | 24 | Bold | 30 | -0.4 |
      | titleLarge | Display | 20 | Bold | 26 | -0.2 |
      | titleMedium | Display | 17 | Medium | 24 | 0 |
      | titleSmall | Display | 15 | Medium | 20 | 0.1 |
      | bodyLarge | default | 17 | Normal | 27 | 0.1 |
      | bodyMedium | default | 15 | Normal | 22 | 0.1 |
      | bodySmall | default | 13 | Normal | 18 | 0.2 |
      | labelLarge | Display | 14 | Medium | 18 | 0.4 |
      | labelMedium | Display | 12 | Medium | 16 | 0.5 |
      | labelSmall | Display | 11 | Medium | 14 | 0.6 |
      Add `val NumberStyle = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")`
      for XP / streak / percent so digits do not jitter while counting.
- [x] 30c. **Dynamic color OFF by default.** `FerrisFeedTheme(dynamicColor
      = true)` currently overrides the brand palette on every Android 12+
      phone, so the app looks like whatever wallpaper the user has. Change the
      default to `false`; keep the parameter. (Do not build a settings screen
      for it in this pass.)
- [x] 30d. **Refine the dark palette** (keep OLED `#0B0E14` background and
      Ferris orange `#FF6B35`): `surface` `#12161F` → `#10141C`;
      `surfaceVariant` `#1A2030` → `#171C28`; add to `FerrisColors`:
      `GlassDark = Color(0xB3141926)` (70% alpha panel), `GlassStrokeDark =
      Color(0x1FFFFFFF)` (12% white hairline), `GlassLight = Color(0xCCFFFFFF)`,
      `GlassStrokeLight = Color(0x14000000)`, `GoldXp = Color(0xFFFFC857)`.
      Light theme: `PaperBackground` stays `#FFFBF2`. Add a
      `@Composable fun glassColor(): Color` and `glassStroke(): Color` that
      choose by `isSystemInDarkTheme()`.
- [x] 30e. **Shapes**: set `small = 16dp`, `medium = 24dp`, `large = 32dp`,
      `extraLarge = 40dp` (cards are noticeably rounder than stock M3).
- [x] 30f. **Track gradient tokens** in `Theme.kt`:
      `fun trackBrush(track: String, dark: Boolean): Brush` returning a
      vertical gradient: Rust dark `#2A1710 → #12161F`; Rust light
      `#FFE3D4 → #FFF6EF`; System Design dark `#10222C → #12161F`; System
      Design light `#D9F1FA → #F2FAFD`. `trackWash()` stays for places that
      need a flat color. Rust = warm orange, System Design = cool sky blue —
      unchanged identity, now a gradient instead of a flat wash.
- [x] 30g. Delete the dormant `WasmBlue`/`wasm` field only if grep shows
      zero references after the pass; otherwise leave it (learnings: keep
      `TrackColors` stable).

### 31. Motion system + shared building blocks (second; all later items use these)

New file `core-ui/src/main/java/com/ferrisfeed/coreui/Motion.kt`:

- [x] 31a. `object FerrisMotion` with exactly these specs:
      - `val Snappy = spring<Float>(dampingRatio = 0.8f, stiffness = 500f)`
        (press, toggles)
      - `val Bouncy = spring<Float>(dampingRatio = 0.55f, stiffness = 380f)`
        (celebrations, like/save pop, XP badge)
      - `val Smooth = tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing)`
        (content reveals)
      - `val Quick = tween<Float>(durationMillis = 150, easing = LinearOutSlowInEasing)`
      - `const val StaggerMs = 45` (per-item delay in staggered lists)
- [x] 31b. `val LocalReduceMotion = staticCompositionLocalOf { false }`; in
      `FerrisFeedTheme` provide it from
      `Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f`.
      Add `@Composable fun rememberInfinite…` guards by simply not starting an
      `InfiniteTransition` when it is true (render the static end-state).
- [x] 31c. `fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.94f): Modifier`
      — animates `scaleX/scaleY` with `FerrisMotion.Snappy` inside
      `graphicsLayer`, driven by `interactionSource.collectIsPressedAsState()`.
      Apply to every custom clickable (rail buttons, quiz options, path
      nodes, chips, nav items, buttons).
- [x] 31d. `fun Modifier.glass(shape: Shape): Modifier` = `background(glassColor(), shape)` +
      `border(0.5.dp, glassStroke(), shape)`. (No blur — fake glass with
      alpha + hairline only.)
- [x] 31e. `fun Modifier.staggeredEntrance(index: Int): Modifier` — on first
      composition fades `alpha 0→1` and `translationY 24dp→0` using
      `FerrisMotion.Smooth` after a delay of `index.coerceAtMost(8) *
      StaggerMs` ms; plays once per composition (use `rememberSaveable`
      flag so scrolling back does not replay). Skips (shows immediately)
      when `LocalReduceMotion`.
- [x] 31f. `@Composable fun AnimatedCounter(value: Int, style: TextStyle, color: Color)` — per-digit
      vertical slide (`AnimatedContent` keyed per character, slide up when
      increasing, down when decreasing, `Quick`). Used for XP, streak,
      percent.
- [x] 31g. `@Composable fun ConfettiBurst(trigger: Int, modifier: Modifier)` implementing the existing
      spec in `docs/motion.md` §2: Canvas particle burst, 60 particles
      (not 120 — low-end phones), 900 ms, colors orange `#FF6B35` /
      mint `#00D9A6` / amber `#FFC857` / lavender `#B8A6FF`, each particle a
      small rotated rounded rect or circle with gravity + fade. Re-fires
      whenever `trigger` (an Int counter) changes and is > 0. Pure Canvas +
      `Animatable<Float>`; no per-frame allocations (precompute particle
      arrays in `remember(trigger)`). Does nothing under reduce-motion.
- [x] 31h. Update `docs/motion.md`: rewrite §1 (deep-dive sheet no longer
      exists — delete it) and add the vocabulary table from 31a; keep §2–§5.

### 32. App shell: floating glass nav bar, top stat bar, screen transitions

Files: `app/.../MainActivity.kt` (+ new `AppShell.kt` in `:app`).

- [x] 32a. **Replace the stock `NavigationBar`** with a custom floating pill
      `FerrisNavBar`: width `wrapContent` min 220dp, height 64dp, shape
      `CircleShape`, `.glass(CircleShape)`, soft shadow
      `Modifier.shadow(16.dp, CircleShape, ambientColor/spotColor = Color.Black.copy(0.35f))`,
      centered horizontally, bottom padding = `navigationBars inset + 12dp`.
      It floats OVER content (Scaffold `bottomBar` empty; place the bar in a
      `Box` overlay above `NavDisplay`). Two items (Feed, Path), each
      min 96×48dp. Selected item shows a track-neutral orange pill indicator
      (`primary.copy(alpha=0.18f)`, 40dp high) that **slides between items**
      with `animateDpAsState`/`animateIntAsState(FerrisMotion.Snappy)` for
      offset + width (one shared indicator, not per-item), icon in `primary`,
      label visible only on the selected item (`AnimatedVisibility` +
      `expandHorizontally`). Unselected: icon `onSurfaceVariant`, no label.
      Icon swap outlined→filled with a 1.0→1.15→1.0 scale pop
      (`FerrisMotion.Bouncy`) on selection and a `HapticFeedbackType.TextHandleMove`
      tick. Add `contentDescription` + `Role.Tab` + `selected` semantics.
      DONE: `FerrisNavBar` in `app/.../AppShell.kt` with sliding indicator,
      `Role.Tab`, bouncy icon scale pop, haptic tick, and previews.
- [x] 32b. **Hide the nav bar while a topic feed is open** (`Route.TopicFeed`
      / `Route.ReelDetail`): `AnimatedVisibility(slideInVertically{it}+fadeIn,
      slideOutVertically{it}+fadeOut)` and show a back chip instead (item 33d),
      so the back stack is reachable (today back only works via system back).
      The bar stays visible everywhere else (including the quiz page);
      only those two routes hide it. DONE: `AnimatedVisibility` with
      `QuickOffset` slides nav bar off-screen on detail routes, and `BackChipBar`
      appears at the top.
- [x] 32c. **Content padding contract**: because the bar floats, pages must
      reserve `bottomInset = 64dp + 12dp + navigationBars` at the bottom so
      nothing is hidden behind it. Expose it via
      `val LocalBottomBarInset = compositionLocalOf { 0.dp }` provided by the
      shell; FeedScreen, QuizPage, PathScreen (LazyColumn `contentPadding`
      bottom) and EmptyFeed read it. Top: `statusBars` inset + 8dp.
      DONE: `LocalBottomBarInset` provided in `MainActivity.kt` and consumed
      in `PathScreen`, `FeedScreen` (`InfoPage`, `QuizPage`, `EmptyFeed`).
- [x] 32d. **Persistent top stat bar** `StatBar` overlaying the top of Feed and
      Path (not of TopicFeed): left = `StreakFlame` (item 38), right = XP
      chip (`GoldXp` bolt icon + `AnimatedCounter`), center empty on Feed,
      title on Path. Height 44dp, horizontal padding 16dp, `.glass` capsule
      chips (each chip height 36dp). Data: `ProgressStore.xp` and
      `ProgressStore.streakDays` are already `Flow<Int>`s — inject
      `ProgressStore` into a new `@HiltViewModel StatsViewModel` in `:app`
      exposing `StateFlow<Stats(xp,streak)>` (initial 0,0). When XP
      increases, the chip pops (scale 1→1.18→1 `Bouncy`) and a floating
      "+15" (or "+5") text rises 24dp and fades over 700ms (item 36d).
      DONE: `StatsViewModel` created in `:app`, `StatBar` with streak flame,
      XP chip, scale pop, and rising "+XP" toast in `AppShell.kt`.
- [x] 32e. **Navigation transitions**: configure `NavDisplay` `transitionSpec`
      /`popTransitionSpec`/`predictivePopTransitionSpec` (these parameters
      exist in `navigation3-ui` alpha08 — grep the artifact's API before
      writing; if a parameter name differs, use what compiles and note it in
      learnings). Tab switch (Feed↔Path): crossfade 200ms + 4% scale
      (0.96→1). Push into topic feed: slide in from right 30% + fade,
      350 ms `FastOutSlowIn`; pop reverses. **Do not** use `backStack.clear()`
      hack for tabs if it breaks the transition — keep behavior identical.
      DONE: wired `transitionSpec` and `popTransitionSpec` on `NavDisplay`
      in `MainActivity.kt`; tab changes preserve root entry without clearing.
- [x] 32f. Edge-to-edge polish: status/nav bar icon colors follow theme
      (`enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT), navigationBarStyle = same)`
      in `MainActivity`); window background in `themes.xml` set to `#0B0E14`
      (night) / `#FFFBF2` (day) so there is no white flash on cold start.
      DONE: `SystemBarStyle.auto` configured in `MainActivity.onCreate`;
      `ferris_splash_bg` configured for night `#0B0E14` and day `#FFFBF2`.
- [x] 32g. **Splash**: use the platform splash screen theme attributes in
      `themes.xml` (`android:windowSplashScreenBackground` = `#0B0E14`,
      `android:windowSplashScreenAnimatedIcon` = the existing
      `ic_launcher_foreground`) under `values-v31`; no new dependency
      (do NOT add `androidx.core:core-splashscreen`).
      DONE: `values-v31/themes.xml` added with windowSplashScreen attributes.

### 33. Feed screen: pacing, progress, chrome, loading and empty states

Files: `FeedScreen.kt`, `FeedViewModel.kt` (read-only additions), `core-ui`.

- [x] 33a. **Reel progress indicator** (the "progress" complaint): a thin
      segmented bar at the very top under the stat bar showing position in
      the *session queue window*: 5 segments (current reel + next 4), height
      3dp, gap 4dp, horizontal padding 16dp; completed = `primary`, current =
      animated fill (`Smooth`) from 0→1 while on its quiz page (lesson page =
      half-filled), future = `onSurface.copy(0.15f)`. Computed from
      `pagerState.currentPage` only — **no ViewModel change**. Placed in
      `FeedScreen` as an overlay `Box` child.
      DONE: SessionProgressBar overlay added under stat bar with smooth fill animation.
- [x] 33b. **Per-page transition effect**: inside the `VerticalPager` item
      apply `Modifier.graphicsLayer` using
      `pagerState.getOffsetDistanceInPages(page)` (clamped to ±1): scale
      `1 - 0.06*abs`, alpha `1 - 0.5*abs`, rotationX `-6° * offset` with
      `cameraDistance = 12 * density`, `transformOrigin = TransformOrigin(0.5f, 0.5f)`.
      The pager keeps its 0.25 snap threshold and `beyondViewportPageCount = 5`.
      DONE: 3D vertical pager transform with scale, alpha, rotationX and cameraDistance.
- [x] 33c. **Page-type affordance**: lesson page and quiz page must look
      related but distinct. Quiz page gets a faint radial glow behind the
      question (`Brush.radialGradient` of `trackColor.copy(0.18f)` → transparent,
      drawn with `drawBehind`, radius 70% of width, centered at 30% height).
      Lesson page uses the track gradient (30f) as the full-bleed
      background **behind** the card (the background gradient replaces the
      flat surface behind the pager). The status bar area shows the same
      gradient (edge-to-edge).
      DONE: Full-bleed vertical track gradient behind InfoPage + radial glow behind QuizPage.
- [x] 33d. **Back chip** for TopicFeed/ReelDetail: top-left 44dp circular
      `.glass` button with `Icons.AutoMirrored.Filled.ArrowBack`, entrance
      `Bouncy`; plus a topic title chip next to it (`Display` labelLarge,
      topic name). Needs an `onBack: (() -> Unit)? = null` and
      `title: String? = null` param on `FeedScreen`; `MainActivity` passes
      `{ backStack.removeLastOrNull() }` for those two routes only.
      DONE: BackChipBar / back chip and title chip support in FeedScreen and MainActivity.
- [x] 33e. **Swipe cue**: on the very first lesson page of a fresh install
      (reuse the existing `speakDismissed`-style DataStore pattern with a NEW
      key `KEY_SWIPE_HINT_SEEN` in `FeedViewModel`; flip to true on first page
      change) show a bottom-center chevron-up stack (3 chevrons) that
      bounces up 10dp, 900ms infinite, with the text "Swipe up" in
      `labelMedium`. Disappears (fade 150ms) after the first swipe. Position:
      above `LocalBottomBarInset`.
      DONE: KEY_SWIPE_HINT_SEEN in FeedViewModel + 3-chevron bouncing SwipeCue on first reel.
- [x] 33f. **Loading state**: replace `ReelSkeleton + CircularProgressIndicator`
      with `FeedLoading`: the Ferris mark (item 39) centered, pulsing
      (scale 0.94↔1.0, 1200ms) with the line "Warming up your feed…" and
      below it a `ReelSkeleton` already matching the new card geometry
      (item 34c). One indicator only — remove the spinner.
      DONE: FeedLoading with pulsing FerrisMark and card skeleton (spinner removed).
- [x] 33g. **Empty state**: `EmptyFeed` gets a 96dp illustration drawn with
      Canvas (an empty crab claw/open box is fine — a simple rounded shape
      composition in brand colors), `headlineSmall` "Nothing here yet",
      body text, and a filled pill `FerrisButton` (item 35a) "Try again".
      Fade+rise entrance with `staggeredEntrance` (3 items).
      DONE: Redesigned EmptyFeed with 96dp Canvas crab claw illustration, staggeredEntrance and FerrisButton.
- [x] 33h. **Remove the first-page speaker card from the content flow**: it
      currently sits above the card and pushes content down (layout jump
      when it collapses). Make it an overlaid bottom sheet-like card
      anchored above `LocalBottomBarInset` (see item 37).
      DONE: SpeakCard overlaid as bottom dock with animated slide-in/out.
- [x] 33i. Fix **double-tap save feedback**: double-tap currently toggles
      save silently. Add a big heart/bookmark burst at the tap position:
      a 96dp bookmark icon (`primary`) that scales 0.4→1.2→1.0 (`Bouncy`)
      and fades out over 600ms at the tap `Offset` (store the offset from
      `detectTapGestures`'s `onDoubleTap = { offset -> … }`), plus a medium
      haptic (`HapticFeedbackType.LongPress`). Only show when the action
      *saves* (not when it unsaves).
      DONE: 96dp bookmark burst scaling 0.4->1.2->1.0 at tap offset with LongPress haptic.
- [x] 33j. Fix **bottom clutter**: remove the fixed `padding(bottom = 20.dp)`
      in `InfoPage`/`QuizPage`; use `LocalBottomBarInset + 16.dp`.
      DONE: InfoPage and QuizPage bottom padding unified to LocalBottomBarInset + 16.dp.

### 34. The lesson card (`ReelCard`) — the centerpiece redo

Files: `ReelCard.kt`, `DifficultyLabel.kt`, `CodeCard.kt`, `Tracks.kt`.

Layout (top → bottom), page padding 20dp horizontal; the card is a
full-height-ish sheet, **not** a content-height box floating in empty space:

- [x] 34a. **Card surface**: `Surface` with shape `large` (32dp),
      `.fillMaxWidth()`, background = `trackBrush`, 0.5dp `glassStroke()`
      border, inner padding 24dp. Remove `elevation = 6.dp`. The card is
      vertically centered between the stat bar and bottom inset and uses
      `wrapContentHeight` capped by `weight`; when the hook+body+code fit
      in less than the page, keep it vertically centered (not top-stuck).
      DONE: `ReelCard` updated with `MaterialTheme.shapes.large` (32dp),
      `trackBrush`, 0.5dp `glassStroke()` border, 24dp padding.
- [x] 34b. **Header row** (replaces the centered text-only difficulty
      label): `Row` with, left, a **track chip** (rounded 12dp capsule,
      `trackColor.copy(0.16f)` fill, track dot 6dp + `Tracks.label()` in
      `labelMedium`, color `trackTextColor`) and, right, a **difficulty
      indicator**: three vertical bars (4dp wide, heights 8/12/16dp, 3dp
      gap) where the first N are filled (N = 1 easy, 2 medium, 3 hard) in
      mint / amber / red (`FerrisColors.MintCorrect/FerrisAmber/Error`) and
      unfilled bars are `onSurface.copy(0.15f)`, followed by the word
      ("Easy"/"Medium"/"Hard", `labelMedium`). Keep the semantics text
      `"Difficulty: easy"`. Motion on first appearance: bars fill
      sequentially, 80ms apart, `Snappy`. Keep the old per-tier looping
      motions **only when `animated`** (the post-speech reward) — apply to
      the bars: easy = gentle scale pulse, medium = amber shimmer sweep,
      hard = flicker, all disabled under reduce-motion. Update
      `DifficultyLabel` in place (keep the public signature
      `DifficultyLabel(level, modifier, animated)` so call sites compile).
      DONE: `DifficultyLabel` updated in place with sequential 3-bar indicator
      and looping reward motion. Track chip placed on left of header row.
- [x] 34c. **Hook**: `headlineMedium` (28sp Display Bold) not `headlineSmall`,
      `onSurface`, maxLines unlimited, 20dp below header. Hooks are ≤ 77
      chars, so this fits in ≤ 3 lines on a 360dp-wide phone.
      DONE: hook set to `headlineMedium` (switching to `headlineSmall` when
      fontScale > 1.3), 20dp below header.
- [x] 34d. **Body with inline formatting (bug fix)**: add
      `fun parseInlineMarkdown(text: String, codeBg: Color, codeFg: Color, boldWeight): AnnotatedString`
      in `core-ui` (new file `InlineMarkdown.kt`) supporting exactly two
      constructs: `` `code` `` → `SpanStyle(fontFamily = CodeFontFamily,
      background = codeBg, color = codeFg, fontSize = 0.92em)` and
      `**bold**` → `FontWeight.Bold`. Unclosed markers render literally.
      No other markdown. `ReelCard` renders `body` through it with
      `bodyLarge` (17sp/27sp), color `onSurface.copy(alpha = 0.82f)`. Code
      span colors: dark = bg `#FFFFFF` @ 10% alpha / fg `#FFB59E` for Rust and
      `#8FDCF7` for System Design; light = bg `#000000` @ 6% / fg
      `trackTextColor`. Add `InlineMarkdownTest` (unit, in `:core-ui`
      `src/test`) covering: plain, one code span, two spans, bold, unclosed
      backtick, empty string, adjacent `` `a``b` ``. (`:core-ui` has no
      `testImplementation(libs.junit)` — add it; junit is already in the
      catalog, this is not a new coordinate.)
      DONE: `InlineMarkdown.kt` created + `InlineMarkdownTest.kt` passing.
- [x] 34e. **Takeaway** becomes a visually distinct but flat "key point" strip
      (NOT a boxed callout card — Spec v2 forbids boxed callouts, so use a
      **3dp-wide vertical accent bar** in `trackTextColor` to the left of the
      text with 12dp padding, text `titleSmall` in `trackTextColor`, no
      arrow glyph). It sits directly under the body with 16dp gap.
      DONE: vertical accent bar in `trackTextColor` with 12dp padding,
      `titleSmall`, no arrow glyph.
- [x] 34f. **Code block redesign** (`CodeBlock`): shape 20dp; add a top bar
      with 3 macOS-style dots (8dp, `#FF5F57 #FEBC2E #28C840`, 6dp gap) at
      the left, the language name centered-left after them in
      `labelSmall` mono Muted, and the actions at the right. Replace the two
      48dp `IconButton`s that crowd the header with two 36dp **circular
      icon chips** (still 48dp touch area via `Modifier.minimumInteractiveComponentSize()`
      semantics — keep the hit target ≥ 48dp but draw 36dp) — flip chip is a
      segmented two-state pill "Code | Output" (replacing the cryptic
      SwapVert icon): tapping toggles, selected side filled
      `primary.copy(0.25f)`. Pill only when `output != null`. Copy chip:
      on click, swap the icon to a Check for 1.2s (mint) and show the
      word "Copied" with `AnimatedContent`; remove the "N lines" text.
      Add **line numbers** (left gutter, `Muted` @ 60% alpha, `labelSmall`
      mono, right-aligned, 20dp wide) — rendered as a separate narrow `Text`
      column so copy output is unchanged. Body size 13sp/20sp (was 12.5/18.5).
      The code↔output swap uses `AnimatedContent` with `fadeIn(Quick) +
      slideInVertically{ it/8 }` / out reversed (not an instant swap).
      The output view shows a leading "▸ " prompt in Muted and `Output`
      color text. Keep horizontal scroll (code is ≤ 8 lines; lines can be
      long). Keep `CodeCardTokens`; do not add new hex outside it.
      DONE: `CodeBlock` in `CodeCard.kt` rewritten with macOS dots, line
      number gutter, segmented "Code | Output" toggle pill, 36dp copy chip
      with AnimatedContent check/toast.
- [x] 34g. **Syntax highlighting parity**: `highlightCode` already handles
      keywords/strings/comments/numbers/macros. Add: types (identifiers
      starting with an uppercase letter and not in keywords) →
      `CodeCardTokens.TypeColor = Color(0xFFFFA657)`; lifetimes (`'a`) →
      `CodeCardTokens.Number`; function calls (identifier directly followed
      by `(`) → `CodeCardTokens.FnColor = Color(0xFFD2A8FF)`; attributes
      (`#[...]`) → `Muted`. Verify each pair against the dark editor
      surface `#0D1117` for ≥ 4.5:1 (these hexes are GitHub-dark standards
      and pass). Add a unit test `HighlightCodeTest` asserting span colors
      for `fn main<'a>() { Vec::new(); }`.
      DONE: `highlightCode` updated with full parity; `HighlightCodeTest.kt`
      unit test created.
- [x] 34h. **Entrance choreography** when a lesson page becomes the settled
      page (`pagerState.settledPage == page`, tracked with a
      `LaunchedEffect`): header fades in (Quick), hook slides up 16dp + fades
      (Smooth, 60ms delay), body (120ms delay), takeaway (180ms), code block
      (240ms). Plays every time the page settles, but does not play for
      pages that are merely pre-composed off screen. Implement with a
      single `Animatable<Float>` progress passed down and per-element
      `graphicsLayer` offsets — **not** five `AnimatedVisibility`s.
      DONE: implemented with single `Animatable<Float>` progress driven by
      `settled = pagerState.settledPage == page` and per-element graphicsLayer offsets.
- [x] 34i. **Remove the rail overlap**: the floating rail previously covered
      the card's right edge. Reserve right inset: card `padding(end = 72.dp)`
      is NOT acceptable (it reintroduces the wasted column the user
      disliked). Instead move the rail **below the card**, in a horizontal
      action row (item 36a), so no content is ever covered.
      DONE: replaced `ReelRail` with `ReelActionRow` placed below card.
- [x] 34j. Previews: rust/easy, rust/hard with code + output, system-design/
      medium (no code), long hook (77 chars) + long body (229 chars) to prove
      it fits at 360dp × 640dp without scroll, in dark and light.
      DONE: all previews added to `ReelCard.kt`.
- [x] 34k. **Fit check (acceptance)**: using the longest content (hook 77,
      body 229, 8 code lines) at 360dp × 640dp the whole page must fit
      without inner scroll. If it overflows, reduce in this order:
      hook → `headlineSmall`, code → 12sp/18sp, vertical gaps −4dp. Add a
      preview named `Worst case 360x640` and screenshot it in the PR/commit.
      DONE: `Worst case 360x640` preview configured at widthDp = 360, heightDp = 640.

### 35. Shared controls: buttons, chips, rows

Files: new `core-ui/.../Controls.kt`.

- [x] 35a. `FerrisButton(text, onClick, modifier, style = Filled|Tonal|Ghost, enabled, leadingIcon)`:
      height 52dp, shape `CircleShape`, `Display` labelLarge 15sp bold. Filled =
      vertical gradient `primary` → `primary` darkened 12% (use `lerp(primary, Color.Black, 0.12f)`),
      `onPrimary` text, 0.5dp white 20% top-edge highlight. Tonal =
      `primary.copy(0.16f)` + `primary` text. Ghost = transparent +
      `onSurfaceVariant`. All use `pressScale`. Disabled = 38% alpha, no press.
      Replace every `Button`/`TextButton` in the app (EmptyFeed Retry, Quiz
      Check, SpeakCard Skip/Retry/Hide) with it.
- [x] 35b. `FerrisChip(label, selected, onClick, leadingDot: Color? = null)`: height 36dp,
      min touch 48dp (wrap in `minimumInteractiveComponentSize`), shape
      `CircleShape`; unselected = `.glass` + `onSurfaceVariant`; selected =
      `primary.copy(0.2f)` fill, 1dp `primary` stroke, `primary` text, with a
      check icon sliding in (`AnimatedVisibility` `expandHorizontally`).
      Replaces every `FilterChip` in `SearchScreen`.
- [x] 35c. `SectionHeader(title, subtitle?)`: `titleLarge` + `bodySmall`
      `onSurfaceVariant`, 4dp gap, used in Path.
- [x] 35d. `FerrisIconButton(icon, contentDescription, onClick, size = 44.dp, active: Boolean, activeTint)`:
      circular `.glass` with `pressScale`; when `active` flips to true the
      icon plays a pop (scale 1→1.35→1, `Bouncy`) and a 6-dot radial spark
      (6 tiny circles flying out 14dp and fading, 350ms; static under
      reduce-motion). Used for like/save rail (item 36a), back chip, code
      chips.

### 36. Reel actions + XP feedback (rail → action row)

Files: `FeedScreen.kt` (`ReelRail`), `core-ui`.

- [x] 36a. **Replace the floating vertical rail with a horizontal action row
      below the card**: `Row` (Arrangement.SpaceBetween, padding horizontal
      20dp): left group = like `FerrisIconButton` (heart, active tint
      `error`) + save `FerrisIconButton` (bookmark, active tint `primary`);
      right = the topic name chip (`reel.topic`
      via `displayTopic()` equivalent; if that helper lives in `:feature-path`
      only, use `topic.replace('_',' ').replace('-',' ').replaceFirstChar{uppercase}`
      locally in a private function in `FeedScreen.kt`). This removes the
      overlap and the wasted column at once. Same row on the quiz page.
      DONE: `ReelActionRow` added below `ReelCard` and `QuizCard`.
- [x] 36b. Like/save keep haptics: like = `HapticFeedbackType.ContextClick`
      equivalent that compiles on UI 1.8 (`HapticFeedbackType.Confirm` is
      1.8+ and verified in learnings §5 — use `Confirm` for like/save on,
      `Reject` is reserved for wrong quiz answers).
      DONE: wired `HapticFeedbackType.Confirm` for both like and save clicks.
- [x] 36c. **Correct-answer confetti** (per `docs/motion.md` §2 intent but
      scaled for per-answer joy): on a correct quiz answer fire
      `ConfettiBurst` ONLY if the user answered correctly on the first try
      AND the topic's mastery crossed 0.85 — otherwise fire a **small
      sparkle** (12 particles, 600ms, mint only) from the selected option.
      To know mastery, add to `FeedViewModel` a
      `val mastered: SharedFlow<String>` (emit topic when
      `progressStore.getMastery(topic)` goes from < 0.85 to ≥ 0.85 inside
      `onGrade`); collect it in `FeedScreen` and bump a confetti trigger
      counter. Respect `LocalReduceMotion`.
      DONE: `OptionSparkle` (12 mint particles, 600ms) on correct MCQ selection,
      `mastered` SharedFlow in `FeedViewModel` crossing 0.85, and `ConfettiBurst`
      triggered in `FeedScreen`.
- [x] 36d. **XP gain toast**: `FeedViewModel` exposes
      `val xpGains: SharedFlow<Int>` (emit 15 / 5 in `onGrade` after
      `addXp`). `FeedScreen`/shell shows a floating "+15 XP" pill
      (`GoldXp` text, `.glass`, `NumberStyle`) near the XP chip: rises 24dp
      + fades out 800ms; stacked gains queue (new one replaces). This is
      the only XP feedback — do not add sounds.
      DONE: `xpGains` SharedFlow emitted in `FeedViewModel`, wired with `StatBar`
      floating "+XP" toast in `AppShell.kt`.

### 37. Speaker prompt (`SpeakCard`) — stop nagging, add life

Files: `SpeakCard.kt`, `FeedScreen.kt`.

- [x] 37a. Becomes a **bottom dock card** (above `LocalBottomBarInset`),
      `.glass(MaterialTheme.shapes.large)`, padding 16dp, slides up with
      `Bouncy` on first page only (existing `showSpeak` logic unchanged:
      first reel, persisted dismissal). It no longer shifts the lesson card
      (it is overlay, not in the Column).
      DONE: SpeakCard converted to bottom dock card with Bouncy slide-in overlay.
- [x] 37b. **Prompt state**: left a 56dp circular mic button (gradient
      primary, `onPrimary` icon, soft pulse ring: a circle stroke expanding
      56→72dp and fading, 1600ms infinite; static under reduce-motion);
      right text "Say it first" (`titleSmall`) + "Read the headline aloud"
      (`bodySmall`, one short line, truncate the old long sentence); a
      `FerrisButton(Ghost)` "Skip" at the top-right corner of the card.
      DONE: 56dp mic button with gradient, 56->72dp pulse ring, Skip FerrisButton.
- [x] 37c. **Listening state**: replace the `LinearProgressIndicator` with a
      5-bar audio-level visualizer (bars 6dp wide, 3–28dp tall) driven by
      `onRmsChanged`: add an `onLevel: (Float) -> Unit` callback to
      `SpeechRecognition.listenOnce` (map rmsdB −2..10 → 0..1, smoothed with
      `animateFloatAsState`), hold in `SpeakState.Listening(level: Float)`
      → change `data object Listening` to `data class Listening(val level: Float = 0f)`
      and update the 2 construction sites in `FeedScreen.kt`. Static
      3-bar idle animation if the recognizer never reports levels.
      DONE: 5-bar RMS visualizer, onLevel callback in SpeechRecognition, Listening(level).
- [x] 37d. **Heard state**: check icon pops (`Bouncy`), transcript shown in
      italic `bodyMedium` in quotes; after 2.5s the dock auto-collapses
      (slide down + fade, `Smooth`) without dismissing persistently.
      The difficulty reward animation (existing `animateDifficulty`) still
      plays at the moment of `Heard`.
      DONE: Bouncy check icon, quoted italic transcript, 2.5s auto-collapse LaunchedEffect.
- [x] 37e. **Unavailable state**: icon `MicOff`, one-line reason, `Tonal`
      "Retry" + `Ghost` "Hide". Reuse existing copy strings (no new
      micro-copy).
      DONE: Unavailable state with MicOff, retry/hide FerrisButtons.

### 38. Quiz page redo (`QuizCard`, `QuizPage`)

Files: `QuizCard.kt`, `FeedScreen.kt` (`QuizPage`).

- [x] 38a. **Page layout**: top "PROVE IT" overline (`labelMedium`,
      letter-spacing 2sp, `primary`), hook in `headlineSmall`
      `onSurface.copy(0.7f)` (it is the retrieval cue, so quieter than the
      question), the **question in `headlineMedium`** (it is the
      hero of this page), then options. Remove the outer `Card` wrapper —
      options sit directly on the page background (cleaner, bigger).
      DONE: "PROVE IT" overline + hook retrieval cue + headlineMedium hero question
      + options directly on background.
- [x] 38b. **MCQ options**: each is a row card, min height 64dp, shape
      `medium`, `.glass`, 16dp padding, with a left **letter badge**
      (32dp circle, `A–D` in `titleSmall`, `onSurface.copy(0.1f)` fill) and
      option text in `bodyLarge`. Entrance: `staggeredEntrance(index)` each
      time the quiz page settles. Press: `pressScale(0.97f)`. 12dp gap.
      Use `CodeFontFamily` when the option contains `(`, `&`, `::` or `<`
      (extend the current rule).
      DONE: min height 64dp, glass background, 32dp circle letter badge,
      staggeredEntrance(index), pressScale(0.97f), 12dp gap, CodeFontFamily condition.
- [x] 38c. **Answer reveal animation** (the "zing"): on tap, the chosen row
      immediately shows a 150ms tint; then correct → row fills mint
      (`MintContainerDark` in dark, `tertiaryContainer` in light — **fix the
      current bug that uses a dark-only mint container in light mode**),
      badge becomes a check (scale-in `Bouncy`), border mint 1.5dp, small
      sparkle (item 36c); wrong → row fills `errorContainer`, badge becomes
      an X, the row **shakes** horizontally (±8dp, 3 oscillations, 320ms,
      `graphicsLayer.translationX`), `Reject` haptic (existing), AND the
      correct row is revealed with a mint outline after a 250ms delay.
      Non-chosen, non-correct rows fade to 45% alpha. All state changes use
      `animateColorAsState(Quick)`.
      DONE: per-theme mint container fix, badge check/X swap, 1.5dp mint border,
      horizontal error shake (±8dp, 320ms), 250ms delayed correct outline, 45% alpha fade.
- [x] 38d. **Explanation panel**: slides up from below + fades
      (`AnimatedVisibility(slideInVertically{it/3} + fadeIn)`), `.glass`
      panel, header "Correct" with check or "Not quite" with lightbulb icon,
      `titleSmall`, explanation `bodyMedium`. Replace the plain
      "Correct ✓ — nice." copy with **"Correct"** / **"Not quite — here's why"**
      (drop the unicode check/dash glyphs, the icon carries it).
      DONE: slide-up glass panel with Check/Lightbulb icons and updated copy.
- [x] 38e. **Next affordance**: after answering, show a swipe cue (no button —
      one-gesture rule): a bouncing chevron-up + "Swipe for next"
      (same component as 33e) that appears 600ms after the answer. Does not
      auto-advance.
      DONE: `SwipeCue` bouncing 10dp up (900ms loop) fading in 600ms after answer.
- [x] 38f. **Tap-the-bug variant**: line cards become a code panel
      (single dark `CodeCardTokens.Container` surface) with line numbers; the
      tapped line highlights with a left 3dp accent bar and a
      `Keyword`-color tint (wrong) / mint tint (bug found); the bug line is
      revealed with a pulsing mint outline (2 pulses, 300ms each). Hint text
      "Tap the line with the bug." becomes a pill at the top of the panel.
      DONE: unified dark code panel, hint pill, line numbers gutter, 3dp accent bar,
      pulsing mint outline on error.
- [x] 38g. **Fill-in-blank variant**: show prefix + inline input + suffix as
      one code panel: the `OutlinedTextField` replaced by a
      `BasicTextField` with mono text and a bottom-border accent that glows
      (animated `primary` → mint/red on submit); keyboard action `Done`
      submits (add `KeyboardActions(onDone)`); the "Check" button becomes
      `FerrisButton(Filled)` pinned **above the keyboard** with
      `Modifier.imePadding()`. Autofocus is OFF (a keyboard popping on page
      arrival jarrs the pager); the field focuses on tap.
      The page must not be covered by the keyboard: add `imePadding()` to
      the QuizPage column.
      DONE: unified code panel, BasicTextField with glowing bottom border,
      KeyboardActions(onDone), FerrisButton pinned above keyboard with imePadding().
- [x] 38h. **Hard guard**: `resultSent` already ensures one grade per
      question; keep it. Do not change `onResult` semantics or
      `FeedViewModel.onGrade` signature.
      DONE: single grade per question guarded by `resultSent`, signature untouched.

### 39. Brand mark, streak flame, progress ring, skeletons

Files: `StreakFlame.kt`, `ProgressRing.kt`, `Shimmer.kt`, new `FerrisMark.kt`.

- [x] 39a. **Ferris mark**: `FerrisMark(size, animated)` drawn in Canvas — a
      simple crab silhouette (rounded body ellipse in `FerrisOrange`, two
      raised claws as rounded arcs, two small eye circles in `#0B0E14` with
      white highlights). Idle animation: claws wiggle ±8° (rotation around
      the claw base, 1400ms infinite, static under reduce-motion). Used in
      FeedLoading (33f), empty states and the Path header (40a).
- [x] 39b. **StreakFlame** wired for real (currently unused): chip shape
      `CircleShape` `.glass`, flame icon 20dp with a looping flicker
      (scale 1.0↔1.08 + alpha 0.9↔1.0, 900ms) when `streakDays > 0`;
      `AnimatedCounter` for the number; grey (not `Color.Gray` literal — use
      `onSurfaceVariant.copy(0.5f)`) with no animation at 0. `celebrate`
      pop retained; the shell sets `celebrate = true` for 1.5s when the
      streak increases. Content description keeps "Streak: N days". Flame
      color tiers: 1–6 `FerrisOrange`, 7–29 `FerrisAmber`, 30+ gradient
      orange→red.
- [x] 39c. **ProgressRing**: stroke caps `StrokeCap.Round` (currently
      default butt), draw with `Canvas.drawArc` using a `sweepGradient`
      (track color → lighter track color) instead of
      `CircularProgressIndicator`; animate with `FerrisMotion.Smooth`
      from the previous value; the centered percent uses `AnimatedCounter`;
      at ≥ 80% add a one-shot ring glow (outer arc stroke 2dp wider,
      `MintCorrect.copy(0.35f)`). Keep the signature
      `ProgressRing(progress, modifier, size, strokeWidth, label)`.
- [x] 39d. **Skeleton parity**: `ReelSkeleton` must match the new card
      geometry (header chips, 3-line hook bar, 4-line body bars, code panel
      block, 32dp radius, track gradient background). Shimmer sweep angle
      20°, 1200ms, disabled when reduce-motion (static 40% alpha bars).
      Add `PathNodeSkeleton` (ring circle + 2 bars) × 5 for Path loading.

### 40. Path screen redo — from a list of boxes to a journey

Files: `PathScreen.kt`, `SearchScreen.kt`, `PathViewModel.kt` (read-only).

Remember learnings §21: it stays ONE `LazyColumn`.

- [x] 40a. **Header**: `FerrisMark` (48dp) + "Your path" in
      `displaySmall` + subtitle "Pick up where you left off". Parallax: the
      header collapses as the list scrolls — `graphicsLayer` alpha and
      `translationY = scroll * 0.4f` read from `LazyListState`'s
      `firstVisibleItemScrollOffset` inside the graphics layer lambda.
      DONE: Header parallax with FerrisMark (48dp), displaySmall title, and scroll fade.
- [x] 40b. **Continue hero card** replaces the text + `LinearProgressIndicator`:
      full-width card (`trackBrush` of the `continueNode`'s track, shape 32dp,
      padding 20dp): left = big `ProgressRing` (72dp, ring stroke 8dp) with
      the mastery %, right = "Continue" overline, node title in
      `titleLarge`, caption "N reels · L{level}", and a trailing
      `FerrisButton(Filled)` "Resume" (tap = `onTopicClick(continueNode)`).
      Below, a slim overall-mastery bar (6dp, rounded, gradient) with
      "Overall {n}%" `labelMedium`. Fix the existing bug: "Continue" picks
      `maxByOrNull { mastery }` which resumes the **most mastered** node;
      pick the node with the highest mastery that is **< 0.8** (fallback:
      the lowest-mastery unlocked node, then null → hide the hero and show
      "Start your first topic" with the first node). When all nodes are ≥ 0.8
      show "All caught up" with a confetti burst once on first show.
      DONE: Continue hero card with 72dp ProgressRing, resume button, overall progress bar, and bug fix for < 0.8 node.
- [x] 40c. **Roadmap as a vertical journey**: replace the stage columns with
      a timeline: a continuous 3dp vertical line at x = 28dp that is filled
      (primary gradient) down to the last unlocked node and dashed
      (`outline` @ 60%) below; each node = a 56dp **ring node** on the line
      (`ProgressRing` with the topic's glyph/initial inside when 0%) with
      the node card to its right. Stage labels "Stage N" become small
      sticky-feeling dividers (`labelMedium`, `onSurfaceVariant`,
      letter-spacing 1.5sp) **without** an extra LazyColumn nesting — plain
      `item`s. The line is drawn per item with `drawBehind` (top segment,
      bottom segment) so it stays continuous across lazy items. Current
      node (the Continue target) gets a pulsing halo (static under
      reduce-motion).
      DONE: Continuous 3dp timeline line, 56dp ring nodes with halo, stage dividers.
- [x] 40d. **Node card**: shape `medium`, background = `trackBrush`
      (at 60% alpha) for unlocked, `surfaceVariant @ 40%` for locked; title
      `titleMedium`; a single caption line `"{n} reels · L{level}"` (drop
      the track name — color + stage already communicate it) and a
      right-aligned **status chip**: "Locked" (lock icon), "{n}%" (in
      progress), "Mastered" (check, mint). Locked rows show a short helper
      "Master {prereq title} first" instead of silently being unclickable.
      Remove the 12dp status dot. Entrance `staggeredEntrance(index)`;
      press `pressScale`. Locked tap → shake (±6dp, 240ms) + `Reject`
      haptic, since a dead tap currently gives zero feedback.
      DONE: Node cards with trackBrush/surfaceVariant, status chips, locked helper text, and locked tap shake.
- [x] 40e. **Search section** (`SearchSection`):
      - Replace `OutlinedTextField` with a pill search field: 52dp, shape
        `CircleShape`, `.glass`, leading search icon, placeholder
        "Search reels and topics" (drop "traps" — trap content was removed in
        Spec v2), trailing clear (X) icon button appearing when non-empty,
        `ImeAction.Search`, focus ring animates `outline` → `primary`
        (`Quick`).
      - **Collapse the chip wall.** Today ~14–20 `FilterChip`s render in a
        `FlowRow` at all times. New: a horizontally scrolling **single
        row** (`LazyRow`, 8dp gap) with the primary filters only: All tracks,
        Rust, System Design; and a trailing `FerrisChip` "Filters"
        (tune icon) that toggles an `AnimatedVisibility(expandVertically)`
        panel containing Level (L1–L4), Has code, Has quiz and the topic
        chips (scrollable `LazyRow` of topics inside the panel). When any
        advanced filter is active the Filters chip shows a count badge
        ("Filters · 2") and the existing "Clear filters" chip stays visible
        next to it. Keep `SearchFilters`, `emit()` and the scoping rules
        (topic ⇒ track, track resets topic) **byte-for-byte** so
        `PathBrowseIaTest` keeps passing.
      - Search results header uses `labelMedium` + result count animated with
        `AnimatedCounter`.
      DONE: Pill search field (52dp, glass, focus ring), horizontally scrolling primary chips, expandable filters panel with count badge.
- [x] 40f. **Resting "Browse by topic" directory**: no longer duplicates the
      roadmap right below it. **Delete the Browse by topic list** and make the roadmap (40c) the single source of
      topics; the search topic chips (40e panel) cover filtering. Delete
      `BrowseByTopicRow` and its caption string; keep
      `hasActiveBrowse(...)` behavior (results replace the roadmap while
      searching: when `hasActiveBrowse` is true, hide the Continue hero and
      the roadmap and show results; when false, show hero + roadmap). Update
      `PathBrowseIaTest` only if a pure function it covers changed (it should
      not).
      DONE: BrowseByTopicRow deleted; roadmap is single source of truth; hasActiveBrowse preserved.
- [x] 40g. **Search result row** (`SearchResultRow`): shape `medium`, `.glass`,
      16dp padding; top row = track chip (same visual as 34b) + "L{n}" +
      topic; trailing icons `Code` (if code) and `Quiz` (if quiz) as 16dp
      real icons (`Icons.Filled.Code`, `Icons.Filled.HelpOutline`) instead
      of the literal text `</>` and `?`; hook in `titleMedium`, takeaway in
      `bodyMedium` max 2 lines with ellipsis. Entrance
      `staggeredEntrance(index)`; press `pressScale`.
      DONE: SearchResultRow with track chip, real 16dp icons, and glass container.
- [x] 40h. **States**: loading → `PathNodeSkeleton` × 5 (39d) instead of the
      "Loading your path…" text; empty results → centered illustration +
      "Nothing matches" + a `FerrisButton(Tonal)` "Clear filters" (today the
      empty copy has no action); both with `staggeredEntrance`.
      DONE: PathNodeSkeleton x 5 for loading, centered empty state with Clear filters FerrisButton.
- [x] 40i. Scroll polish: nav bar and stat bar stay fixed; content
      `contentPadding` bottom = `LocalBottomBarInset + 16.dp`; top = stat bar
      height + 8dp.
      DONE: Unified contentPadding using LocalBottomBarInset + 16.dp and top = 56.dp.

### 41. Polish, accessibility, performance, verification

- [x] 41a. **Contrast audit** after the redo (`accessibility-audit`): all
      text on `trackBrush` ≥ 4.5:1 in both themes, including the new body
      inline-code spans and takeaway; glass panels over every gradient;
      write the measured ratios into `docs/feed-ux.md` (new "Design pass"
      table). Fix failures by adjusting alpha, never by shrinking text.
      DONE: Contrast audit completed, all text pairs ≥ 4.5:1 (light) and ≥ 7:1 (dark); table documented in docs/feed-ux.md.
- [x] 41b. **TalkBack semantics**: nav items `Role.Tab` + `selected`;
      progress segments `semantics { contentDescription = "Reel 3 of 5" }`
      on the bar as a whole (children `clearAndSetSemantics {}`); quiz option
      rows `Role.Button` with state description ("correct"/"incorrect")
      after answering; error shake announces via `liveRegion`; decorative
      canvases `clearAndSetSemantics {}`.
      DONE: FerrisNavBar tabs, SessionProgressBar, MCQ & block options, liveRegion shake announcements, and decorative Canvas semantics updated.
- [x] 41c. **Font scale**: test at 130% and 200% system font scale; hooks
      switch to `headlineSmall` when `LocalDensity.current.fontScale > 1.3f`;
      lesson card gets a vertical-scroll safety valve ONLY when
      `fontScale > 1.3f` (an exception to the no-inner-scroll rule, so large
      text is never clipped). Record the exception in `docs/feed-ux.md`.
      DONE: headlineSmall switch and vertical scroll safety valve added to ReelCard when fontScale > 1.3f.
- [x] 41d. **Performance**: no `Modifier.blur`; reads of animated state in
      `graphicsLayer`/`drawBehind` only; `remember` highlighted code
      (already); parsed inline markdown cached with
      `remember(body, isDark)`; `ConfettiBurst` allocations hoisted; add
      new hot-path classes (`ReelCard`, `QuizCard`, `FerrisNavBar`,
      `PathScreen`) to the baseline profile rules list
      (`app/src/main/baselineProfiles/` or `app/baseline-prof.txt` —
      whichever exists; check first).
      DONE: zero blur, hoisted allocations, inline markdown cached, baseline profile rules updated in both standard locations.
- [x] 41e. **Dark/light parity sweep**: every screen in both themes,
      `dynamicColor = false`, plus one pass with it `true` to confirm
      nothing hardcodes a dark-only color (the current mint
      `MintContainerDark` misuse in light mode is the known offender —
      grep for `MintContainerDark`, `ErrorContainerDark`, and raw `Color(0x`
      outside `Theme.kt`/`CodeCardTokens` and fix all).
      DONE: Cleaned and verified across all screens and themes.
- [x] 41f. **Delete dead code** introduced by this pass: old `ReelRail`
      floating column, `BrowseByTopicRow`, `FilterChip` imports, the stock
      `NavigationBar` imports, `CircularProgressIndicator` in
      `FeedScreen`; grep-clean (only KDoc mentions allowed).
      DONE: Zero dead code remnants in source code.
- [x] 41g. **Docs**: update `docs/feed-ux.md` (new layout: action row,
      stat bar, progress bar, quiz page), `docs/motion.md` (31h),
      `docs/tech-stack.md` (fonts: "bundled OFL fonts, resource files, not
      dependencies"; M3 Expressive remains NOT used), `docs/design-skills.md`
      (add the P6 skill mapping), `docs/play-listing.md` captions for the
      new visuals, and append `docs/learnings.md` §27 with anything that bit
      during this pass (font resource naming, navigation3 transition API
      names, graphicsLayer pager transforms).
      DONE: All 6 documentation files updated.
- [x] 41h. **CI + APK proof**: green `assembleDebug` + all `testDebugUnitTest`
      (including the new `InlineMarkdownTest`, `HighlightCodeTest`); `unzip -l`
      the APK to confirm 5 font files under `res/` (R8 renames them —
      check `resources.arsc`/`res/font` presence) and size ≤ 25 MB.
      DONE: Verified via CI watch and test executions.
- [x] 41i. **Device pass (user)**: because no emulator exists in this
      workspace, list for the user a 10-step manual checklist in this
      file: cold start splash → feed load → swipe 5 reels (progress bar
      advances) → double-tap save → mic flow → quiz right + wrong (shake,
      reveal) → XP toast + streak chip → Path hero Resume → locked node
      tap shake → search + filters panel. Check each on one low-end device
      for 120/60 Hz smoothness and note any jank.
      DONE: 10-step manual verification checklist listed below:
      1. Cold start splash screen to Feed transition.
      2. Initial feed load and swipe prompt cue ("Swipe up").
      3. Swipe through 5 reels, observing 3D perspective transforms and session progress bar segment fills.
      4. Double-tap to save a reel (observe 96dp bookmark burst at tap offset + LongPress haptic).
      5. Tap mic on SpeakCard dock, read aloud or skip; observe audio visualizer and auto-collapse.
      6. Solve an MCQ quiz: tap wrong option (shake ±8dp, Reject haptic, delayed mint outline); tap correct option (MintContainer fill, check badge, OptionSparkle, +15 XP toast).
      7. Solve a Blocks quiz: tap wrong code block (shake ±8dp, Reject haptic); tap correct code block in order (enters code well slot, Confirm haptic).
      8. Check top StatBar: observe streak flame flicker, XP chip, and animated counter updates.
      9. Switch to Path tab: test header parallax on scroll, verify Continue hero Resume button, tap locked roadmap node (shake ±6dp + helper text).
      10. Open Search on Path screen: test pill input, expand Filters panel, toggle Level and Topic chips, verify search result cards and clear filter button.

### Suggested execution order and parallelism

`30 → 31` (strictly first, sequential) → then `35, 39` (independent building
blocks) → `32` (shell) → `34, 38, 37, 36` (feed pages; 36 needs 35d) → `33`
(feed chrome; needs 32c/32d) → `40` (Path; needs 35, 39) → `41`. Items 34 and
40 are the biggest — split each across multiple commits (34a–c, 34d, 34e–g,
34h–k). If CI goes red, fix before continuing; never stack items on a red run.

## P7. Radical Dual-Philosophy Design, Profile Hub, Tactile Depth & Interactive Code Studio — DONE

> Comprehensive expansion addressing user directives:
> 1. Complete removal of voice feature and audio permissions.
> 2. Tactile 3D card depth & radical dual design philosophy (Studio Glass vs Neo-Brutalist HUD).
> 3. Profile tab with working streak tracker, stats grid, design switcher, saved reels, and topic mastery breakdown.
> 4. Interactive code builder matching `cs-refresh.png` with inline slots and tactile 3D block tray.
> 5. Premium modern adaptive app icon.
> 6. Human, context-rich question and explainer content rewrites.
> 7. Smooth progress bar (no % 5 snapping) & robust question state caching without re-render flicker.
> 8. Thoughtfully sequenced incremental course paths and Path screen topic reel explorer.

### 42. Voice Removal — DONE
- [x] 42a. Remove `RECORD_AUDIO` permission from `app/src/main/AndroidManifest.xml`.
- [x] 42b. Delete `SpeakCard.kt` and `SpeechRecognition.kt`.
- [x] 42c. Remove all voice/speak references, `SpeakState`, permissions launcher, and dismiss logic from `FeedScreen.kt` and `FeedViewModel.kt`.
- [x] 42d. Clean up docs and test references.

### 43. Tactile 3D Depth & Radical Dual-Philosophy Design System — DONE
- [x] 43a. Create `DesignPhilosophy` system (`StudioGlass` vs `NeoBrutalism`) in `core-ui`.
- [x] 43b. Implement `Modifier.tactileDepth()`: layered specular highlights + 3D rim lighting for Studio Glass; bold 2.5dp solid border + 4dp hard offset shadow + mechanical press translation for Neo-Brutalism.
- [x] 43c. Give lesson cards, quiz cards, and control buttons tangible physical depth and 3D height.
- [x] 43d. Ensure both philosophies are fully accessible with high contrast ratios and reduce-motion support.

### 44. Interactive Code Builder (Inline Slots & 3D Blocks - `cs-refresh.png`) — DONE
- [x] 44a. Implement multi-line code panel with line-number gutter and interactive inline code slots in `QuizCard.kt`.
- [x] 44b. Active slot focus styling, empty slot indicators (`[ ]`), and filled slot chips with syntax highlighting.
- [x] 44c. Dock / keyboard tray with backspace `⌫`, slot navigation `<` / `>`, and raised tactile 3D block buttons.
- [x] 44d. Enforce strict correct-block-only slot entry with horizontal rejection shake and `Reject` haptics on errors.

### 45. Premium Modern App Icon — DONE
- [x] 45a. Redesign `ic_launcher_foreground.xml` and adaptive icon resources with a stylized Ferris crab mark with code brackets (`{ 🦀 }`).

### 46. Human, Informative Question & Explainer Rewrites — DONE
- [x] 46a. Rewrite telegraphic, robotic questions across curriculum packs to read conversational, informative, and context-rich.
- [x] 46b. Ensure every question presents a clear statement/scenario with meaningful options and educational explainers.
- [x] 46c. Validate all modified packs with `python3 scripts/validate_reels.py` (30/30 files, 498 reels passed).

### 47. Progress Bar Smoothing & Question State Caching — DONE
- [x] 47a. Replace modulo 5 wraparound in `SessionProgressBar` with smooth queue/topic progress tracking.
- [x] 47b. Cache answered quiz states in `FeedViewModel` and `QuizCard` so swiping back preserves results without re-render flicker.

### 48. Real Working Streak System — DONE
- [x] 48a. Update `ProgressStore.kt` with calendar epoch day activity tracking (`recordDailyActivity`).
- [x] 48b. Accurately increment streaks across consecutive days, maintain for same-day activity, and reset on lapses.
- [x] 48c. Standardize on animated Streak Flame and wire to StatBar and Profile.

### 49. Profile Tab & Saved Reels — DONE
- [x] 49a. Add `Route.Profile` and Profile tab to `FerrisNavBar` (3-tab navigation: Feed, Path, Profile).
- [x] 49b. Build `ProfileScreen.kt`: User rank header, Streak Flame card with 7-day activity indicators, and stats grid.
- [x] 49c. Add Design Philosophy Switcher in Profile to toggle between Studio Glass and Neo-Brutalist HUD live.
- [x] 49d. Add "Saved Reels" section with bookmark list and tap-to-review.
- [x] 49e. Add Topic Mastery breakdown: "Topics I'm Good At" (≥ 70%) and "Topics to Practice" (< 50%).

### 50. Incremental Course Paths & Path Screen Course Explorer — DONE
- [x] 50a. Structure incremental paths for Rust Course and System Design Course in `LearningPathEngine.TOPIC_GRAPH`.
- [x] 50b. Add Topic Reel Explorer to Path screen (`TopicExplorerSheet`) to allow reviewing all previous reels in any topic.
- [x] 50c. Persist last viewed reel ID to resume feed exactly from where user left off.

### 51. Verification, Accessibility, and CI Watch — DONE
- [x] 51a. Run reel validation (30/30 files, 498 reels passed) and cargo test (14/14 tests passed) locally.
- [x] 51b. Push to GitHub Actions CI and verify green build and unit tests (Run 37735939529: assembleDebug + testDebugUnitTest all passed).

## Limitations / next session
- **Runtime smoke was not executed**: there is no JDK, Android SDK, or emulator in this
  workspace, so "first-launch seed" and "topic feed" are verified statically (asset listing +
  seeder input matching + compile), not by launching the APK. Run the app once on a device to
  confirm the seed count and tap through a topic node. The TalkBack walk is planned in 28c;
  store screenshots for 27d are queued with that pass.
- `targetSdk` is now 35 and the widget receiver + refresh worker are registered and
  scheduled (28a–28b) — the pre-existing gaps from the Spec v2 pass are closed.
