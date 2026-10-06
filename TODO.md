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
- [ ] 19. CI green + APK smoke check (asset count + first-launch seed + topic feed) on the
      Spec v2 commits.
