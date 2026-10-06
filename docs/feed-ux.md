# FerrisFeed Feed UX — Doomscroll Spec

Covers TODO items 6 (feed UX) and 8 (feed engine behavior). The Compose
implementation is `FeedScreen.kt` + `FeedViewModel.kt`; the visuals are
`core-ui` cards. This doc is the interaction contract QA tests against.

## Layout — one reel, one screen

A full-screen `VerticalPager` shows exactly one reel per page:

1. **Header** — `TrackPill` + `LevelBadge` + read-time label (`55s read`).
   Read time = body words at 200 wpm + 3s per code line, minimum 20s.
2. **Explainer** — `ReelCard` (hook → body → takeaway), then `CodeCard` when the
   reel has code, then `TrapCard` (collapsed by default).
3. **Actions** — Like / Save icon buttons, `Deep Dive` outlined button,
   `Got it ✓` / `Still fuzzy` SRS buttons.
4. **Quiz variant** — swiping **left** on a reel flips an inner `HorizontalPager`
   to the `QuizCard` (MCQ / Tap-the-bug / Fill-blank). Swiping right returns.
   The quiz never replaces the explainer; it is a second face of the same page.

While the next pages compose, `ReelSkeleton` shimmer placeholders hold the exact
`ReelCard` proportions so there is no layout shift when content arrives.

## Gestures

| Gesture | Location | Effect |
|---|---|---|
| Vertical swipe | anywhere | Next / previous reel (`VerticalPager`). |
| Horizontal swipe left | reel page | Reveal quiz variant (inner pager page 1). |
| Horizontal swipe right | quiz page | Back to explainer (inner pager page 0). |
| Double-tap | reel body | Toggle **Save** (bookmark). Fires `FeedViewModel.onToggleSave`. |
| Long-press | reel body | **Peek** the quiz answer as an overlay. Shown while held, never graded. Release dismisses. |
| Tap `Deep Dive` | actions row | Open `ModalBottomSheet` with full body + code + trap + quiz. Predictive back returns to the same pager index. |
| Tap `Got it` / `Still fuzzy` | actions row | Grade the card for SRS (`recordGrade`). Advances the scheduler in `rust-core`. |

Nested scrolling rule: the inner horizontal pager only claims horizontal drags;
vertical drags always belong to the outer `VerticalPager`. Reel bodies scroll
vertically only when content exceeds the viewport.

## Motion — 300ms spring

- Pager snap: `spring(stiffness = Medium, damping = MediumBouncy)` targeting
  ~300ms settle. No custom fling velocity — stock pager physics.
- `ProgressRing` mastery sweep: `animateFloatAsState` to the new value on grade.
- `StreakFlame` pop: 1.0 → 1.25 scale spring when the streak increments.
- `TrapCard` expand: `AnimatedVisibility` expand/shrink, 300ms.
- Shimmer: 1200ms infinite linear sweep, shown only during prefetch.
- Target: 120 Hz scrolling on mid-range devices; `beyondViewportPageCount = 5`
  keeps the next 5 reels composed without overdraw.

## Haptics

- Quiz correct: `HapticFeedbackType.LongPress` tick (the "yes" tick).
- Quiz incorrect: `HapticFeedbackType.VirtualKey` buzz (no deny member exists
  across our resolved Compose versions, so VirtualKey stands in).
- Save via double-tap: light `TextHandleMove` tick is optional; no haptic on
  plain scroll to avoid fatigue.
- All haptics go through `LocalHapticFeedback` so system settings are honored;
  no custom vibrator calls.

## Feed engine (ViewModel contract)

- **Shuffle**: 70% due SRS cards (sorted by `dueAt`, oldest first), 20% new in
  `path_order`, 10% random review. At least one fresh card when any exist.
  Interleaved due/other so two due cards rarely sit back-to-back.
- **Prefetch**: on every page change the ViewModel resolves the next 5 reel ids
  (`getReel`) so code highlight + images are warm. UI keeps 5 offscreen pages.
- **Tracking**: page enter stamps `pageStartMs`; page exit logs
  `(reelId, dwellMs, skipped = dwell < 1500ms && !interacted)`.
- **Position**: current index + reel id hash debounced (400ms) into DataStore
  (`feed_last_index`, `feed_last_id_hash`); restored on cold start before the
  first frame where possible.

## Accessibility

- Every icon button has a content description (`Like`, `Save`, `Copy code`…).
- Quiz options are full-width 48dp+ tap targets with answer state announced via
  the explanation card, not color alone.
- Code font size slider (10–20sp) supports low-vision readers; TalkBack reads
  code line-by-line from the raw string, not the highlighted spans.
