# Motion — FerrisFeed

Motion budget: every animation must hold 120Hz on a Pixel 6 with the
baseline profile installed. If it drops frames in Macrobenchmark, it ships
off by default behind a "Reduce motion" respect flag.

## 1. Shared-element feed -> deep-dive sheet

- Feed pager item -> bottom-sheet deep dive uses Compose shared-element
  (`sharedElement {}` + `animateBounds`) on the hook text + track pill.
- The sheet expands from the tapped card's bounds (not a generic
  slide-up), so the eye never loses context. Duration 300ms,
  `spring(stiffness = 400f, dampingRatio = 0.9f)` — snappy, one gentle
  overshoot, no bounce loop.
- Predictive back: sheet participates in `PredictiveBackHandler`; swiping
  back previews the feed pager underneath at 0.7 scale + dim, and cancel
  returns to the sheet without losing scroll or quiz state.

## 2. Confetti on mastery

- Fires exactly once when a topic crosses 85% (MASTERED) or a Ferris stage
  evolves (Egg -> Crab -> Armored). Never on raw XP — XP is too frequent
  and confetti would become noise.
- Implementation: lightweight canvas particle burst (120 particles, 900ms,
  Ferris orange/mint/lavender), no third-party GIF/Lottie dependency.
- Paired with a haptic tick (`HapticFeedbackType.LongPress`) + the Ferris
  asset swap. Respects `ACCESSIBILITY reduce-motion`: shows a static badge
  pop instead.

## 3. Shimmer skeletons while prefetching

- Next-5 pager prefetch: unbound pages render `ReelCardSkeleton` (shimmer
  gradient sweep 1200ms, `rememberInfiniteTransition`) with the same
  28dp card geometry, so layout never jumps when content lands.
- Code blocks shimmer as 3-5 mono bars matching the snippet line count.
  Shimmer is disabled under battery saver.

## 4. Haptics

- Quiz correct: light tick. Quiz wrong: double-tick (not a buzz — wrong
  answers shouldn't feel punished). Save (double-tap): medium tick.
  Stage evolution: long-press pattern. All via `LocalHapticFeedback`,
  gated on system haptics setting.

## 5. 120Hz pager + baseline profile

- `VerticalPager` with `beyondViewportPageCount = 5`, LRU-highlighted code
  cache (max 20 entries), and `graphicsLayer {}` transforms only (no
  re-composition on scroll). Read-time + impression tracking is debounced
  off the composition path.
- `app/baseline-prof.txt` pins the hot path: pager, ReelCard, CodeCard,
  QuizCard, Room DAO reads, FSRS grading, DataStore reads. The release
  Macrobenchmark (`baselineProfile` Gradle task) regenerates it; CI fails
  if scroll jank (frame P99) regresses >10% vs. main.
