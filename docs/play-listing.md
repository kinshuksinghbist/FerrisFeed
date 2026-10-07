# Play Store Listing — FerrisFeed

> TODO 27d: describes the REDESIGNED card (Spec v2 + speaker opening), not
> the old UI. Shaped by `ux-writing` + `content-strategy`: one promise per
> line, verbs first, no feature that no longer ships.

## Title

FerrisFeed — Learn Rust in 60 Seconds

## Short description (80 chars)

Say it, read it, prove it. Bite-size Rust + system design, offline.

## Full description

**Say it, read it, prove it.**

FerrisFeed is a vertical feed where every swipe is one 60-second Rust or
system-design lesson — spoken first, read second, proven with a quiz.

- One card per idea: say the headline aloud, read the hook + short body
  with the takeaway as the closing line, flip the code to see its output,
  answer the quiz inline. No inner scroll, no second screen.
- Small centered difficulty cue: easy breathes, medium shimmers, hard
  flickers. Track identity is the card tint itself — orange wash for Rust,
  sky-blue wash for system design — readable in light and dark.
- Real code on every code card: fixed readable mono, copy with one tap,
  flip to reveal the authored expected output. Cards without a runnable
  result simply hide the flip.
- The quiz is the only grade: answering schedules your next review,
  grows XP, and keeps your streak. No self-report buttons.
- Heart + save on the right rail (48dp targets, double-tap to save),
  like reels anywhere.
- Path tab with real progress: one node per topic with live reel counts
  and mastery that decays when you skip. Tap a node for that topic's feed
  only. Search lives at the top of Path — query + topic/track/level chips
  over the same roadmap, never a separate tab.
- Fully offline: the whole curriculum ships in the app. Airplane-mode
  friendly, no account needed.
- Streaks, mastery rings, and a Ferris mascot that evolves from Egg to
  Armored Crab as you learn.

Free, no ads, no account. Optional anonymous usage stats (opt-out anytime).

Made by Rust learners, for Rust learners. Crab on. 🦀

## Screenshot captions (8)

1. "One swipe, one idea: say the headline, then read."
2. "Difficulty at a glance: easy breathes, medium shimmers, hard flickers."
3. "Rust glows orange, system design glows blue — no badges needed."
4. "Real code, one tap to copy, flip to see the output."
5. "The quiz lives in the card. Answering is the grade."
6. "Your path: live nodes with real counts and mastery."
7. "Search lives in Path: topics, tracks, and filters in one place."
8. "Fully offline. Airplane mode is a feature, not a bug."

## 30-second demo script

- 0:00-0:05 — Cold open on the feed mid-scroll. VO: "Say it, read it,
  prove it." Tap the mic, read the headline, "Heard you" appears.
- 0:05-0:12 — Swipe twice (ownership reel -> borrow reel). Point out the
  difficulty cue motion + track tint. Small swipe still advances.
- 0:12-0:18 — Code card: tap copy, tap flip to reveal output. Quiz card:
  tap the answer, explanation appears, difficulty cue plays.
- 0:18-0:24 — Path tab: search field + chips on top, "Browse by topic"
  directory, tap a node into its topic-only feed. Resume bar + mastery.
- 0:24-0:30 — Airplane-mode toggle ON, keep scrolling. End card: app
  icon + "FerrisFeed — free Rust + system design, offline."

## Privacy policy summary (listing short text)

No account, no ads, no sale of data. Optional anonymous usage stats
(reel views, quiz accuracy, voice-capture counts — never transcripts or
audio) with in-app opt-out. Full policy in-app and
at ferrisfeed.app/privacy.

## Beta track + feedback loop

- Open beta track (`ferrisfeed-beta`) ships every Friday from `main`.
- In-app "Suggest a reel / Report bug" sheet (Settings + long-press any
  reel) prefills app version, OS, and reel ID into a GitHub issue form
  (`anomalyco/FerrisFeed` — bug / content-suggestion templates).
- Top-voted suggestions are authored into the next weekly pack and
  credited in the reel takeaway. Crash clusters from Crashlytics are
  linked back to the originating issue automatically.
