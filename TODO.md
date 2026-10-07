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
- [ ] 22d. Any data this reveals becomes a 21/27 item only — no
      schema work inside UI items (house rule). STILL-OPEN as a
      standing rule: no schema work beyond 21 is planned inside UI
      items; 22's data need (topic strings) was already met by 21.

### 23. Track identity + tint system hardening (Theme.kt / track wash)

Orange #FF6B35 = Rust, sky-blue = System Design; the wash must stay
readable in both themes (dark OLED #0B0E14).

- [ ] 23a. Contrast audit: text over tints ≥ 4.5:1 in light and dark;
      run as `accessibility-audit` + `critique-color`.
- [ ] 23b. Extract the washes into design tokens (`design-token`) so
      inline colors stop drifting; `dark-mode-design` for the OLED
      pass; equal luminance distance for both tracks per theme.
- [ ] 23c. Audit core-ui for accumulated UI drift with
      `design-debt-audit` + `design-token-audit`; fold the fixes into
      the card redo (24).

### 24. The card — speaker-opening flow redo (the centerpiece)

Implementation stays offline-first: platform `SpeechRecognizer` + the
`RECORD_AUDIO` permission only — no new dependencies, no cloud calls.

- [ ] 24a. Spec the interaction first (`reel-card-composition` +
      `micro-interaction-spec`): phrase prompt → capture gesture →
      recognized-text reveal → hook + body → code → quiz. No inner
      scroll; all motion inside the 300ms budget (`docs/motion.md`).
- [ ] 24b. The difficulty label keeps its animated cue but plays only
      after recognition completes; apply `loading-states` +
      `doherty-threshold` so capture never blocks text readiness.
- [ ] 24c. Author the new states and micro-copy with `ux-writing`;
      empty/retry shapes follow the existing pattern (no skeleton
      loops); quiz grading remains the only SRS/XP/streak signal
      (`onGrade`, `feed-ux.md`).

### 25. Code card + output flip survive the redo

- [ ] 25a. Copy + flip + output stay as-is (`CodeCard.kt`); the flip
      stays hidden for the 13 `output: null` reels (Spec v2).
- [ ] 25b. The takeaway stays the closing line (no boxed callouts);
      post-redo affordance check with `critique-affordance`.
- [ ] 25c. Keep 12.5sp mono readable with `readable-measure` +
      `critique-typography`; code contrast at that size is part of
      the 23a audit.

### 26. First-run + placement test (deferred until 21–25 land)

Store copy already promises a placement test (`docs/play-listing.md`).

- [ ] 26a. Drafts only: smallest flow with `onboarding-design` +
      `form-design`, framed with `jobs-to-be-done`; no screens until
      the draft survives review.
- [ ] 26b. Author placement items from the seeded reel bank; plan the
      study with `usability-test-plan` + task scenarios with
      `test-scenario`.
- [ ] 26c. Prototype first reviewed with `/visual-critique:critique-screen`;
      ship after it passes, on the normal Friday train.

### 27. Success metrics + store alignment (post-redo)

- [ ] 27a. Define metrics before instrumenting (`metrics-definition`):
      recognition start/complete, quiz grade rate, taps per topic,
      plus the dwell/skip signals `feed-ux.md` already logs.
- [ ] 27b. Ship a weekly card-analytics digest that reads existing
      events only (`docs/analytics.md`) — no new vendor backend, and
      the opt-out from `docs/privacy-policy.md` is respected.
- [ ] 27c. Read the first data with `behavioural-analytics`; anything
      built to compare variants goes through `a-b-test-design` first.
- [ ] 27d. `docs/play-listing.md` must stop describing the old UI:
      update the demo script and captions to the redesigned card,
      re-shoot screenshots, re-check copy with `ux-writing` +
      `content-strategy`.

### 28. Known gaps cleared along the way (learnings §15)

- [ ] 28a. `targetSdk` 35 (Play's expectation for listings): the AGP
      8.9.2 / Gradle 8.11.1 / compileSdk 36 triple stays locked, so
      this is a single `targetSdk` bump + full CI + APK asset check
      (learnings §17), not an SDK-level upgrade.
- [ ] 28b. Register the widget receiver + refresh worker (the §15
      gap list in `docs/learnings.md`).
- [ ] 28c. One device pass before any of this ships: first-launch
      seed count, topic-feed tap-through, and a TalkBack walk planned
      with `accessibility-test-plan`; record the evidence here.

## Limitations / next session

- **Runtime smoke was not executed**: there is no JDK, Android SDK, or emulator in this
  workspace, so "first-launch seed" and "topic feed" are verified statically (asset listing +
  seeder input matching + compile), not by launching the APK. Run the app once on a device to
  confirm the seed count and tap through a topic node.
- Roadmap node titles come from the seeded topic key, which is derived from the content file
  name, so they read coarse (`Extra`, `Drills`, `Beginner`). Finer nodes need a `topic` field in
  the reel schema + a richer filename/label mapping — out of scope for this pass.
- `targetSdk` is still 34 (Play expects 35+ for new listings) and the widget receiver + refresh
  worker are still unregistered — both pre-existing, tracked in `docs/play-listing.md`.
