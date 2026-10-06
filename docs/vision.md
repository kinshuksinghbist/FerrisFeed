# FerrisFeed — Product Vision

> **FerrisFeed — Doomscroll, but you get hired.**

FerrisFeed is a TikTok-style vertical feed where every swipe is a 60-second
Rust, WASM, or System Design flashcard. The goal is to turn dead time into
hirable skill with bite-size, runnable, testable lessons.

## Problem

Traditional courses are heavy and easy to abandon. Short-form video is
addictive but teaches nothing durable. Developers want daily reps that fit
in a commute or a queue, with real code and instant feedback.

## MVP Scope (V1)

The V1 milestone ships an **offline-first Android app** with:

- **Offline feed** — infinite `VerticalPager`, prefetch next 5, restore
  scroll position instantly, full airplane-mode support.
- **300+ reels at launch** — Rust beginner + intermediate, WASM intro,
  System Design beginner. Each reel is hook + <=70-word body + <=15-line
  runnable snippet + takeaway + trap + 1 quiz.
- **Quizzes + SRS** — MCQ, tap-the-bug, fill-blank. FSRS-lite scheduler in
  the shared Rust core drives 70% due / 20% new / 10% review ordering.
- **Streaks + XP + mastery** — DataStore-backed XP, per-topic mastery %,
  heatmap, streak flame, Ferris evolution Egg → Crab → Armored Crab.
- **Learning paths** — DAG prerequisites (Ownership → Lifetimes → Async →
  Axum → Rate Limiter), placement test, daily mix, resume bar.
- **Search + topic map** — Room FTS full-text search, filter by
  track / level / has-code / has-quiz, roadmap graph UI.

## Non-Goals for V1

Explicitly out of scope until V2:

- User-generated content (UGC) — all reels are curated and linted.
- Comments, follows, DMs, or social graph.
- Accounts, cloud sync, leaderboard (designed for, not built).
- iOS app — web preview via WASM only.

## Target User

Android-first junior-to-mid developer preparing for Rust or backend roles.
Has 5–15 minutes a day, wants visible progress and interview-ready recall.

## Success Criteria

- D7 retention driven by streak + daily mix.
- Median session: 12 reels + 4 quizzes.
- Quiz accuracy lift on reviewed topics within 14 days.
- APK < 25 MB, 120 Hz pager, TalkBack-clean.
