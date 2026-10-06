# Learning Science — FerrisFeed

How FerrisFeed turns doomscrolling into durable Rust / WASM / system-design skill.
Companion code: `feature-path/.../LearningPathEngine.kt`, `ProgressSystem.kt`,
`rust-core` FSRS scheduler, `data/.../ProgressStore.kt`.

## 1. Spaced repetition (FSRS-lite)

Each reel with a quiz is a flashcard. After every review the user grades
themselves: **Again (1) / Hard (3) / Got it (4)** via the
"Still fuzzy / Got it" buttons. Grades feed an FSRS-lite scheduler
(full FSRS lives in `rust-core`; Room columns mirror its state):

- `stability` (days until recall drops to 90%), `difficulty` 1..10,
  `nextDueMillis`, `reps`, `lapses`.
- "Got it" raises stability multiplicatively; "Again" collapses stability
  and increments `lapses`, queuing the card as a weak spot.
- The feed is 70% due cards, so retrieval practice happens in context
  instead of a separate "review mode" users never open.

FSRS beats SM-2 here because stability is per-card and adapts to lapses:
borrow-checker traps that users keep missing stay due daily, while
`cargo new` basics quickly stretch to weeks.

## 2. Prerequisite DAG

Topics form a directed acyclic graph, not a flat list. The hero spine is:

```
Ownership -> Lifetimes -> Async -> Axum -> RateLimiter
```

`LearningPathEngine.TOPIC_GRAPH` holds the full graph (WASM and
system-design branches fork off Ownership / Axum). A topic unlocks when
**every** prerequisite has mastery >= 60%. The roadmap UI lights nodes up
as prerequisites clear, so learners see *why* Lifetimes is locked
("master Ownership first") instead of hitting a wall of `'a` errors.

Cycles are rejected by construction: `TOPIC_GRAPH` is a static list and
`unlockedTopics()` only reads forward edges. A unit test asserts the graph
is acyclic and the spine order is intact.

## 3. Placement test (skip beginner)

15 fixed quizzes spanning toolchain -> clippy (`PLACEMENT_REEL_IDS`).
Score >= 11/15 (73%) skips level-1 reels and starts at Ownership. The bar
is set so random guessing (~4/15) can never pass, but a working Rustacean
doesn't re-scroll `cargo new`. Results persist in `ProgressStore`
(`placement_done`, `placement_skip_beginner`).

## 4. Daily Mix: 7 new + 8 reviews + 5 weak

`buildDailyMix()` returns exactly 20 reels per session:

| Bucket | Count | Source |
|---|---|---|
| New | 7 | `getNewCardsInPathOrder`, filtered to unlocked topics |
| Reviews | 8 | `getDueCards(now)` (due SRS queue) |
| Weak spots | 5 | `getWeakCards` (low stability or recent lapse) |

Cards are **interleaved** (new, due, due, weak…) rather than blocked:
interleaving beats blocking for discrimination learning (e.g. `&str` vs
`String` vs `Vec<u8>` stop blurring together). Short buckets refill from
whatever is available, so day-one users get a full 20 even with no dues.

## 5. Resume-where-left-off

`ProgressStore` persists `(reelId, index)` on every page settle.
`resumeTopic()` prefers the last-seen topic while its mastery is < 85%,
otherwise recommends the lowest-mastery unlocked topic (spine order breaks
ties). The home bar reads "Continue Ownership 62%" (`resumeLabel()`),
deep-linking back to the exact pager position.

## 6. XP, streaks, heatmap — why these numbers

- **XP rewards retrieval, not scrolling.** Reel seen = 10 XP; quiz "Got it"
  = 15 XP; "Hard" = 8 XP; "Again" = 0. Skimming 20 reels with no quizzes
  earns ~200 XP; actually recalling earns ~400+. The incentive gradient
  points at the learning behavior.
- **Streak multiplier +5%/day, capped +50%.** Rewards daily return without
  letting a 30-day streak trivialize new content. Streaks increment only
  on distinct calendar days; gaps reset to 1 (see `ProgressStore`).
- **Mastery decays 2%/inactive-day** (`0.98^days`). Forgetting is real, and
  the UI should say so: skipping lifetimes for two weeks visibly drops its
  % and re-queues reviews. Decay applies on read, never destroys stored
  scores.
- **Heatmap 0..4 levels** (GitHub-style) from daily XP thresholds
  0 / 30 / 80 / 150. A full Daily Mix lands at level 3-4.
- **Ferris evolution** Egg (0 XP) -> Crab (500) -> Armored Crab (2000)
  maps to ~1 week / ~1 month of casual daily use. Cosmetic only — never
  gates content — so it motivates without punishing slow learners.
- **Achievements** (Borrow Checker Survivor, WASM Summoner, P99 Slayer,
  Crab Walk Week, Daily Mixer, Lifetimes Tamer) are mastery predicates,
  not grind counters. Each names a real skill milestone and is checked
  once per meaningful event, not polled.

## 7. What we deliberately don't do

No leaderboards, no social comparison, no XP for time-in-app in V1.
Leaderboards optimize for session length; we optimize for 7-day and
28-day retention of *recall accuracy per topic*, measured via quiz grades.
