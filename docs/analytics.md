# Analytics + A/B — FerrisFeed

Code: `data/.../data/Analytics.kt` (`Analytics`, `ExperimentFlags`).

## 1. What we log

| Event | Params | Why |
|---|---|---|
| `reel_impression` | reel_id, position, source | Funnel + feed-mix health |
| `reel_dwell` | reel_id, track, topic, dwell_ms, bucket (skip/glance/read/deep) | Scroll depth per topic |
| `reel_save` / `reel_unsave` | reel_id | Save rate = content value |
| `quiz_answer` | reel_id, topic, correct, grade, hook_variant | Accuracy per topic + A/B outcome |
| `recognition_start` | reel_id, topic | Speaker-opening capture started (TODO 24; no transcript) |
| `recognition_complete` | reel_id, topic, heard, latency_bucket (fast/normal/slow) | Capture completed; heard only, never content |
| `topic_tap` | topic, source (roadmap/directory/chip) | Taps per topic = which entry leads where |
| `topic_mastery` | topic, mastery_pct | Retention curve input |
| `streak_tick` | streak_days | Habit metric |

Source values: `daily_mix | search | topic_map | widget | resume`.

## 2. What we never log

Body text, code snippets, quiz options, notification content, IP-derived
location, advertising ID, contacts, or anything typed by the user. Reel IDs
(`rust-own-014`) are curriculum keys, not user data. Crash reports contain
stack traces + OS version only.

## 3. Backend + opt-out

- Production: Firebase Analytics + Crashlytics + RemoteConfig. Debug/FOSS
  flavors use `NoOpBackend` (in-memory list, asserted in tests).
- Settings -> Privacy -> "Usage analytics" toggle writes
  `analytics_opt_out` to DataStore. When set, `Analytics.allowed()` is
  false and **no event leaves the device** — not even queued. The toggle
  defaults to ON (logging enabled) but is asked about during onboarding
  with a one-screen plain-language explainer, not buried.
- Data retention: Firebase set to 14-month auto-delete; per-user export /
  delete honored via the Play Data Deletion path (see privacy policy).

## 4. A/B via RemoteConfig
`ExperimentFlags.fromRemoteConfig()` reads:

- `hook_style`: `curiosity_gap` (control) vs `compiler_error` vs
  `perf_claim`. Exposure = `quiz_answer.hook_variant`; outcome = quiz
  accuracy + dwell bucket for the same reel_id.
- `daily_mix_ratio`: `70_20_10` (control) vs `60_30_10`. Outcome = 7-day
  retention + weak-card recovery rate.

Rules: one primary metric per experiment, 95% significance, min 7 days,
no PII in variant assignment, and losing variants are deleted — not left
to rot as dead flags. Flags are documented in this file when added.

## 5. Weekly card-analytics digest (TODO 27b)

`CardAnalyticsDigest.summarize(events)` reads existing events only — no new
backend, no new collection. Input is the same event list `Analytics`
already emits (empty when opted out, so the digest honestly says "no
data"). Output: reels seen, quiz answer count + accuracy, recognition
start/heard counts, taps per topic, skip rate. Read the first data with
`behavioural-analytics` (27c); anything built to compare variants goes
through `a-b-test-design` first — the digest never assigns variants.
