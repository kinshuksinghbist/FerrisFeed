# FerrisFeed — UX Overhaul TODO (Spec v2, locked)

> Replaces the v1 doomscroll spec. Locked decisions from review:
> WASM is OUT of the app (files stay dormant in repo) · trap content is
> dropped entirely · code outputs are backfilled BEFORE new UI ships.
> Read `docs/learnings.md` before changing build/dependency/DI code.

## Design principles

- One idea per screen, one gesture (vertical swipe), zero unearned chrome.
- Every reel is ONE card: difficulty label → hook + body → code → quiz inline.
- Smallest swipe advances; no inner vertical scroll; right action rail.

## P0. WASM removal (app shows Rust + System Design only)

- [ ] 1. Seeder skips `wasm/`; feed queue, path nodes, search filters show Rust + System Design only.
- [ ] 2. Delete WASM from `Tracks`, `trackColor`, default path nodes, seed/track constants; fix all references.
- [ ] 3. Update docs (`tech-stack.md`, `offline.md`, learnings) to mark WASM dormant-not-deleted.

## P1. Output backfill + data plumbing (before any new UI)

- [ ] 4. Schema: optional `output` field in `schema.json` + validator (<=15 lines) + docs.
- [ ] 5. Backfill `output` for all 360 code-bearing Rust + System Design reels (Rust run-verified, rest reviewed; fragments get result/error block; unrunnable → null + log).
- [ ] 6. `ReelEntity.output` column + DB v2 beta fallback; seeder mapping; `RoomReelDataSource` + feed `Reel` carry `output`.
- [ ] 7. Re-run full validation (validator + Room compile + seeder dry-check).

## P2. Reel card + pager + rail (the visible overhaul)

- [ ] 8. Unified card: centered animated difficulty label (Easy breathing pulse / Medium shimmer / Hard ember flicker, from existing `level`), hook + body with takeaway as closing line, code below, `QuizCard` inline. Delete horizontal pager, Deep Dive sheet, takeaway box, `TrapCard` usage, Got-it/Fuzzy, Run, font slider, read-time, TrackPill/LevelBadge header, peek overlay, swipe hints.
- [ ] 9. Track tint: orange wash Rust, sky-blue wash System Design; readable in both themes.
- [ ] 10. Code card flip: copy + flip icons only; flip reveals `output`, hidden when null.
- [ ] 11. Pager: no inner vertical scroll, low-threshold snap so small swipes advance; prefetch kept.
- [ ] 12. Right rail: heart + bookmark vertical, 48dp targets; double-tap-save kept.
- [ ] 13. Quiz answers remain the sole SRS/XP/streak signal via existing `onGrade` path — verify XP still accrues with buttons gone.

## P3. Path + search rework

- [ ] 14. Bottom nav to Feed + Path; search field + results move to top of Path screen; Search tab/route deleted.
- [ ] 15. New `PathViewModel`: nodes from `topicsForTrack` + new `countByTopic` query (query-only, no migration) + `ProgressStore.getMastery` with decay; static demo nodes out of production path.
- [ ] 16. Topic tap → `Route.TopicFeed(topicId)` filtered feed (quiz+info mixed) via `SavedStateHandle` filter in `FeedViewModel`.

## P4. Close-out

- [ ] 17. Deletion audit — every removed component verified unreferenced.
- [ ] 18. Previews/screenshots refreshed; `feed-ux.md`, learnings updated; CI green + new APK smoke check (assets count + first-launch seed + topic feed).
