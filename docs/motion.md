# Motion — FerrisFeed

Motion budget: every animation must hold 120Hz on a Pixel 6 with the
baseline profile installed. If it drops frames in Macrobenchmark, it ships
off by default behind a "Reduce motion" respect flag.

## 1. Motion vocabulary (`FerrisMotion`)

Defined centrally in `Motion.kt` (P6 31a). Never inline ad-hoc `tween(...)` numbers in screens.

| Token | Spec | Primary Use |
|---|---|---|
| `Snappy` | `spring(dampingRatio = 0.8f, stiffness = 500f)` | Press feedback (`pressScale`), toggles, active indicators |
| `Bouncy` | `spring(dampingRatio = 0.55f, stiffness = 380f)` | Celebrations, like/save pop, XP badge, icon bursts |
| `Smooth` | `tween(durationMillis = 350, easing = FastOutSlowInEasing)` | Content reveals, card settlement, page transitions |
| `Quick` | `tween(durationMillis = 150, easing = LinearOutSlowInEasing)` | Micro-interactions, counter digit slides, color transitions |
| `StaggerMs` | `45ms` | Per-item entrance delay for staggered lists (`staggeredEntrance`) |

All looping and decorative animations respect `LocalReduceMotion` (bound to system `ANIMATOR_DURATION_SCALE == 0f`). Functional transitions fall back to 100ms quick fades.

## 2. Page-type and 3D Pager Transitions

Inside `FeedScreen.kt`'s `VerticalPager`, each page applies 3D perspective transforms inside `Modifier.graphicsLayer` using `pagerState.getOffsetDistanceInPages(page)`:
- Scale: `1f - 0.06f * abs(offset)`
- Alpha: `1f - 0.5f * abs(offset)`
- RotationX: `-6f * offset`
- Camera distance: `12 * density`
- Transform origin: `TransformOrigin(0.5f, 0.5f)`

Because all transforms run inside `graphicsLayer`, no re-composition occurs during scrolls, maintaining 120Hz frame rates.

## 3. Error Shake & Rejection Kinetics

1. **MCQ Error Shake**:
   - Tapping an incorrect MCQ option drives `translationX` with keyframes: ±8dp amplitude across 3 oscillations over 320ms (`-8f at 40ms`, `8f at 90ms`, `-6f at 150ms`, `6f at 210ms`, `-3f at 265ms`, `0f at 320ms`).
   - Accompanied by `HapticFeedbackType.Reject` and announced to screen readers via `liveRegion = LiveRegionMode.Polite`.
2. **Click-to-Code Block Assembly Shake**:
   - Tapping an incorrect code block triggers horizontal rejection shake (±8dp, 320ms) with `Reject` haptic feedback. Only the correct block enters the active slot.
3. **Locked Path Node Shake**:
   - Tapping a locked roadmap node shakes the node card (±6dp, 240ms) with `Reject` haptic feedback, while highlighting the prerequisite helper text.

## 4. Celebrations, Sparkles & Confetti

1. **Option Sparkle** (`OptionSparkle`):
   - Regular correct quiz answers trigger a localized sparkle burst of 12 mint particles (600ms) originating from the chosen card.
2. **Mastery Confetti** (`ConfettiBurst`):
   - When a topic's mastery crosses 85% on a first-try correct answer, a 60-particle canvas confetti burst (900ms) fires across the screen in brand colors (Ferris Orange, Mint, Amber, Lavender).
   - All particle physics compute off-composition and render via Canvas.
3. **XP Gain Toast**:
   - Correct answers award +15 XP (+5 on retry), causing a "+15 XP" glass pill to rise 24dp and fade out over 800ms next to the StatBar XP chip.

## 5. Shimmer Skeletons

- Next-5 pager prefetch: unbound pages render `ReelSkeleton` (32dp card geometry, header chips, 3-line hook, 4-line body, code panel) with a 20° angled shimmer sweep (1200ms).
- Path roadmap uses `PathNodeSkeleton` (56dp ring + 2 text bars) × 5 while topics load.
- Skeletons fall back to static 40% alpha bars under `LocalReduceMotion`.

## 6. Haptics Hierarchy

- Confirm / Positive (`HapticFeedbackType.Confirm`): Like tap, Save tap, Correct block placed.
- Reject / Error (`HapticFeedbackType.Reject`): MCQ incorrect choice, Block placement rejection, Locked roadmap node tap.
- Save double-tap: `HapticFeedbackType.LongPress` at the gesture offset.
- Navigation tab change: `HapticFeedbackType.TextHandleMove`.

## 7. Baseline Profile Hot Paths

`app/baseline-prof.txt` and `app/src/main/baselineProfiles/baseline-prof.txt` pin the hot execution paths:
- `FeedScreenKt` / `FeedPager`
- `ReelCardKt`
- `CodeCardKt` / `CodeBlock`
- `QuizCardKt` / `McqBody` / `BlocksBody`
- `AppShellKt` / `FerrisNavBar`
- `PathScreenKt`
- Room DAO reads and FSRS scheduling logic.
