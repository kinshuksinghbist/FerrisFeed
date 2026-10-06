# Offline-First — FerrisFeed

FerrisFeed works fully in airplane mode from first launch. No account, no
network, no spinner: 400+ reels ship inside the APK.

## 1. First-launch seeding (current path)

- `:app:syncCurriculumAssets` mirrors validated repo-root `content/**/*.json`
  (minus `schema.json`) into generated APK assets. The merge flattens to the
  APK asset ROOT (`assets/rust/…`, `assets/system-design/…`), which is the
  path `CurriculumSeeder` lists — never `assets/curriculum/`.
- Only Rust and System Design ship (Spec v2): `content/wasm/**` stays in the
  repo but is **dormant** — not synced into seeding, not seeded, not rendered,
  not searchable. Do not re-add without a spec change.
- On first DB creation, `FerrisDatabase` fires `CurriculumSeeder`, which
  parses the JSON on IO and inserts ~500 rows, then the feed observes the
  table. No prepackaged SQLite: a hand-built `.db` must carry Room's exact
  schema identity hash, and a missing/stale one crashes on EVERY launch
  instead of degrading — seeding lets Room own its schema.
- Upgrade path (optional, later): content JSON -> SQLite generator script ->
  `createFromAsset` + auto-migration. Keep the seeder as the fallback even
  then; `exportSchema = true` stays on either way.

Asset budget: text for 550 reels is ~2-3 MB SQLite; illustrations are
WebP in `assets/img/` (~5-8 MB). Total offline payload stays under ~12 MB,
inside the <25 MB R8 budget (see docs/quality.md).

## 2. DataStore for progress

`ProgressStore` (DataStore Preferences) holds everything user-specific:

- total XP, streak days, last active day, active-day set (capped 400),
  per-day XP (heatmap), per-topic mastery JSON, scroll position
  `(reelId, index)`, placement flags, analytics opt-out, last sync time.

DataStore — not Room — because progress is key-value, needs no relations,
and must survive curriculum DB swaps without migration coupling.

## 3. Weekly sync (WorkManager)

`CurriculumSyncWorker` runs every 7 days:

- Constraints: unmetered network, battery not low, storage not low.
- Fetches `packs/manifest.json`, downloads packs newer than `last_sync`,
  inserts **only unknown reel IDs** (INSERT IGNORE semantics).
- Existing rows are never overwritten: the user's SRS columns
  (`nextDueMillis`, `stability`, `reps`, `lapses`) live in the same row,
  so REPLACE-upserts would wipe scheduling. Content corrections ship with
  the next full asset DB instead.
- Exponential backoff (30 min), max 3 attempts, then surfaces "Sync
  failed — will retry" in Settings. Manual "Check for new reels" triggers
  the same worker one-shot.

## 4. Airplane-mode image/code cache

- All reel illustrations are bundled WebP assets (`imageAsset`), never
  remote URLs for V1 content. No network, no cache miss, ever.
- Weekly-pack images (V2 packs only) are warmed into the HTTP disk cache
  by the sync worker (`warmImageCache`) before the user can open them.
- Code snippets are inline strings in the reel row — rendering never
  needs the network. There is no in-app interpreter and no Run button
  (Spec v2): `output` on the reel is the authored expected result and the
  code card simply flips to reveal it offline.
- Coil disk cache size: 100 MB, `CachePolicy.ENABLED` offline-first.

## 5. Offline behavior matrix

| Feature | Offline | Notes |
|---|---|---|
| Feed scroll + pager | Yes | Room only |
| Code highlight + copy | Yes | Bundled |
| Quiz + grading + SRS | Yes | rust-core local |
| FTS search + filters | Yes | Prebuilt FTS4 |
| Progress / streak / XP | Yes | DataStore |
| Daily Mix | Yes | Computed locally |
| Widget Reel of Day | Yes | Last synced state |
| New curriculum packs | No | Queued to next sync |
| Firebase analytics | No | Dropped when opted-out/offline, never queued with PII |
| Crash reports | Deferred | Cached, uploaded on reconnect |

## 6. Testing offline

- `FerrisDatabase.inMemory()` for hermetic tests; asset DB verified by a
  release check (`countAll() >= 400`, every reel has hook/body/takeaway,
  FTS index non-empty).
- Manual QA: fresh install -> airplane mode -> scroll 50 reels, answer 5
  quizzes, kill process, relaunch, confirm resume position + streak intact.
