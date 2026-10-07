# FerrisFeed Feed UX — Spec v2 (locked)

The Compose implementation is `FeedScreen.kt` + `FeedViewModel.kt`; the visuals
are the `core-ui` cards. This doc is the interaction contract QA tests against.
It replaces the v1 doomscroll spec: **one reel is ONE card**, the quiz is inline,
and quiz answers are the only grading signal.

## Layout — one reel, one vertically-swipeable screen

A full-screen `VerticalPager` shows exactly one reel per page. The page is a
single `Column`, top to bottom, **with no inner vertical scroll**:

1. **Unified info card** (`ReelCard`) — centered animated difficulty label →
   hook (headline) → body → takeaway as the closing line (track-colored, no
   boxed callout). The card's container is washed with the track color
   (orange Rust, sky-blue System Design) instead of carrying a pill/badge.
2. **Code card** (`CodeCard`) when the reel has code — header is language label
   + copy icon + flip icon (flip appears only when `output` is non-null).
   Fixed 12.5sp mono; horizontal scroll for long lines is allowed, vertical is
   not.
3. **Quiz inline** (`QuizCard`) — MCQ / tap-the-bug / fill-blank. Answering is
   the sole SRS/XP/streak signal (`FeedViewModel.onGrade`).

Like/save live on a right-edge action rail, vertically centered, 48dp targets.
There are no action rows inside the card, no bottom sheet, no peek overlay.

## Difficulty label

One small centered label replaces the old `TrackPill` + `LevelBadge` +
read-time header, mapped from the existing content `level`:

| Level | Label | Motion |
|---|---|---|
| 1 | easy | slow breathing scale pulse (~2s cycle) |
| 2 | medium | horizontal gradient shimmer sweep (~1.6s cycle) |
| 3+ | hard | ember flicker — fast small alpha jitter (~0.9s cycle) |

TalkBack reads "Difficulty: Medium" (semantics `contentDescription`).

## Gestures

| Gesture | Location | Effect |
|---|---|---|
| Vertical swipe | anywhere | Next / previous reel. Low positional threshold (`snapPositionalThreshold = 0.25`) so even a small swipe commits. |
| Double-tap | reel page | Toggle **Save** (bookmark). Invisible — no hints, no chrome. |
| Tap heart | right rail | Toggle **Like**. |
| Tap bookmark | right rail | Toggle **Save**. |
| Tap copy | code card header | Copy the code snippet to the clipboard. |
| Tap flip | code card header | Swap the card body between highlighted code and expected `output`. Hidden when `output` is null. |

There is no horizontal pager and no inner vertical scroll, so a vertical drag
always means "change reel" — the old nested-scroll fight is structurally gone.

## Motion — 300ms budget

- Pager snap: stock `PagerDefaults.flingBehavior` with a 0.25 positional
  threshold; `beyondViewportPageCount = 5` keeps the next 5 pages composed.
- Code flip: `animateFloatAsState(tween(300))` rotates the flip icon 180°.
- `ProgressRing` mastery sweep: `animateFloatAsState` to the new value.
- `StreakFlame` pop: 1.0 → 1.25 scale spring when the streak increments.
- Shimmer: 1200ms infinite linear sweep, shown only during prefetch/skeletons.

## Haptics

- Quiz correct: `HapticFeedbackType.LongPress` tick (the "yes" tick).
- Quiz incorrect: `HapticFeedbackType.Reject` (the "no" buzz).
- Save via double-tap: no haptic on plain scroll to avoid fatigue.
- All haptics go through `LocalHapticFeedback` so system settings are honored;
  no custom vibrator calls.

## Feed engine (ViewModel contract)

- **Shuffle**: 70% due SRS cards (sorted by `dueAt`, oldest first), 20% new in
  `path_order`, 10% random review. At least one fresh card when any exist.
  Interleaved due/other so two due cards rarely sit back-to-back.
- **Topic filter**: `setTopicFilter(topic)` narrows the whole queue to one
  roadmap topic (`Route.TopicFeed`); `null` restores the full mix. Called from
  the route, and `focusReel` still searches the full table as a fallback.
- **Prefetch**: on every page change the ViewModel resolves the next 5 reel ids
  (`getReel`) so code highlight + images are warm. UI keeps 5 offscreen pages.
- **Tracking**: page enter stamps `pageStartMs`; page exit logs
  `(reelId, dwellMs, skipped = dwell < 1500ms && !interacted)`.
- **Position**: current index + reel id hash debounced (400ms) into DataStore
  (`feed_last_index`, `feed_last_id_hash`); restored on cold start.
- **Empty vs loading**: seeding failures render an explicit empty state with a
  Retry button (`retryLoad()`), never an infinite skeleton.

## Path tab + browse (S7, TODO 22)

- Bottom nav is Feed + Path only. The Search tab and `Route.Search` are gone.
- The top of the Path screen is the browse section: query field + chips for
  **topic / track / level / has-code / has-quiz** + result rows. Results render
  as items of the same `LazyColumn` as the roadmap (one scroller; nothing
  nestable). A "Clear filters" chip appears whenever any chip is set.
- Topic-first IA: with an empty query and no chips the section shows a
  **Browse by topic** directory — every roadmap topic as a row with its reel
  count, entry level, and mastery. The topic chips in the row above come from
  the same live topic list, so a chip can never lead to an empty shelf.
- One topic identity everywhere (TODO 21): roadmap node, browse chip, result
  caption, and `Route.TopicFeed(topicId)` all address the exact stored topic
  string. Result rows caption their topic so a hit says where it lives.
- Selecting a track resets the topic chip (and vice versa is scoped per
  track), so the chip row never mixes topics across tracks.
- Roadmap nodes are real: one per topic with reels in Room
  (`ReelDao.countByTopic` → count + lowest level) with mastery from
  `ProgressStore` (2%/idle-day decay). A fresh install honestly shows 0%.
- Tapping a node or a directory row opens `Route.TopicFeed(topicId)` — that
  topic's reels only, quiz + info mixed, same card as the main feed. Browsing
  a topic via chips shows the same reels as flat rows instead.
- Empty results under an active query or scope say why and how to widen
  ("Nothing matches those filters yet — clear one to widen the net."), never
  a bare "0 results". The resting state is the directory, not a blank list.

## Accessibility

- Every icon button has a content description (`Like`, `Save`, `Copy code`,
  `Show output`…).
- Quiz options are full-width 48dp+ tap targets with answer state announced via
  the explanation card, not color alone.
- Difficulty is announced via semantics; track identity is not color-only
  because the label spells out difficulty and the row text names the track.
- Code is read by TalkBack line-by-line from the raw string, not the highlighted
  spans.
