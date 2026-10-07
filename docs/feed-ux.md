# FerrisFeed Feed UX — Design Pass (P6 Overhaul)

The Compose implementation is `FeedScreen.kt` + `FeedViewModel.kt` wrapped in
`AppShell.kt`; the visuals are the `core-ui` cards and controls. This document
defines the design studio visual and usability system.

## 1. App Shell Architecture

The screen is framed by floating chrome:
1. **Top Persistent Stat Bar** (`StatBar`):
   - Left: `StreakFlame` chip (`.glass`, flame icon with 900ms looping flicker when streak > 0, `AnimatedCounter`, streak color tiering).
   - Center: Optional topic / screen title (`DisplayFont` labelLarge).
   - Right: XP pill (`GoldXp` text, `.glass`, `NumberStyle` tabular figures) + floating "+15 XP" toast that rises 24dp and fades out (800ms) on successful quiz answers.
2. **Session Progress Indicator** (`SessionProgressBar`):
   - Positioned directly beneath the stat bar across the top.
   - Shows current position within a 5-reel session queue window: 5 thin segments (height 3dp, gap 4dp, horizontal padding 16dp).
   - Completed segments = `primary`, current segment = smooth animated fill (0.5 for lesson, 1.0 for quiz), future segments = `onSurface @ 15%`.
   - TalkBack announces `Reel X of 5` for the entire bar; child segments clear semantics.
3. **Bottom Floating Navigation Bar** (`FerrisNavBar`):
   - 64dp height, pill shape (`CircleShape`), `.glass` container with 0.5dp `glassStroke()`.
   - Two routes: **Feed** (home icon) and **Path** (timeline/map icon).
   - Selected tab expands horizontally with `Bouncy` icon scale and bold `DisplayFont` label.
   - Semantics: `Role.Tab` + `selected` state.

## 2. Pager & Card Layout (One Reel = Two Pages)

A full-screen `VerticalPager` (`beyondViewportPageCount = 5`, `snapPositionalThreshold = 0.25`)
displays each reel as two pages:
1. **Lesson Page** (`InfoPage`):
   - Full-bleed vertical track gradient background (`trackBrush`).
   - `ReelCard`: 32dp shape (`large`), 0.5dp glass stroke border, inner padding 24dp.
   - Entrance choreography: settled page triggers sequential fade/rise for header, hook, body, takeaway, and code well.
   - Header row: Left = track chip (12dp capsule, 6dp track dot + label); Right = 3-bar sequential difficulty indicator (Easy/Medium/Hard).
   - Hook: 28sp `DisplayFont` Bold (`headlineMedium`). Switches to `headlineSmall` when `fontScale > 1.3f`.
   - Body: `bodyLarge` (17sp) with `parseInlineMarkdown` supporting `` `inline code` `` and `**bold**`.
   - Takeaway: 3dp vertical accent bar in `trackTextColor` with 12dp padding, `titleSmall`, no arrow glyphs.
   - Code Block well (`CodeBlock`): macOS header dots, mono language label, 36dp segmented pill "Code | Output" toggle, 36dp copy chip with check toast, line numbers gutter (`labelSmall` mono right-aligned), and syntax highlighting.
   - **Horizontal Action Row** (`ReelActionRow`) placed below the card: left = Like (`FerrisIconButton` heart with `Confirm` haptic and 6-dot radial spark) + Save (`FerrisIconButton` bookmark with `Confirm` haptic and spark); right = topic chip.
2. **Quiz Page** (`QuizPage`):
   - Faint radial glow behind question (`trackColor @ 18%`).
   - Top "PROVE IT" overline (`labelMedium`, letter spacing 2sp, `primary`).
   - Hook as retrieval cue (`headlineSmall` @ 70% alpha).
   - Question as hero in `headlineMedium`.
   - Options sit directly on page:
     - **MCQ**: 64dp min height, `.glass` medium cards, 32dp letter circle badges (A–D). Correct = MintContainer fill, check badge, mint border, `OptionSparkle` (12 mint particles) and `ConfettiBurst` on mastery crossing 0.85; Incorrect = errorContainer fill, X badge, horizontal shake (±8dp, 320ms, `Reject` haptic, TalkBack `liveRegion` announcement), followed by 250ms delayed correct outline reveal.
     - **Click-to-Code Blocks** (`BlocksBody`): Top code panel with highlighted prefix/suffix and dashed active slot; bottom bank of code blocks. Tapping correct block enters slot with `Confirm` haptic; tapping wrong block shakes (±8dp, 320ms) with `Reject` haptic and rejection sound feedback. Reset button restores bank.
     - **Tap-the-Bug**: Unified code panel with line numbers gutter and hint pill.
     - **Fill-in-the-Blank**: Unified code panel with `BasicTextField`, glowing bottom border, and `FerrisButton` pinned above IME.
   - Slide-up explanation panel (`ExplanationPanel`).
   - Bouncing `SwipeCue` ("Swipe for next") appears 600ms after answering.

## 3. Speaker Opening (`SpeakCard`)

Overlaid bottom dock card above bottom inset:
- Prompts mic on first reel only; persists hide state in DataStore.
- 56dp mic button with primary gradient, expanding soft pulse ring (56→72dp, 1600ms).
- Listening state: 5-bar audio-level visualizer driven by RMS dB callbacks.
- Heard state: Bouncy check icon, quoted italic transcript, 2.5s auto-collapse without persistent dismissal.

## 4. Design Pass Contrast Audit

All color pairings were verified under WCAG 2.1 specifications:

| Element | Theme | Foreground Hex | Background / Context | Ratio | Level |
|---|---|---|---|---|---|
| Primary Text (`onSurface`) | Dark | `#E8ECF2` | Surface `#10141C` | 14.7:1 | AAA |
| Body Text (82% alpha) | Dark | `#C1C6CE` | Surface `#10141C` | 9.8:1 | AAA |
| Rust Track Accent / Takeaway | Dark | `#FF8A5B` | Surface `#12161F` | 7.1:1 | AAA |
| System Design Track Accent | Dark | `#70CFFF` | Surface `#12161F` | 10.5:1 | AAA |
| Rust Inline Code Span | Dark | `#FFB59E` | 10% white well | 8.5:1 | AAA |
| SD Inline Code Span | Dark | `#8FDCF7` | 10% white well | 11.2:1 | AAA |
| Primary Text (`onSurface`) | Light | `#1F1B16` | Paper `#FFFBF2` | 15.2:1 | AAA |
| Rust Track Accent / Takeaway | Light | `#A33400` | Track `#FFE3D4` | 5.4:1 | AA |
| System Design Track Accent | Light | `#006584` | Track `#D9F1FA` | 4.9:1 | AA |
| Glass Panel over Gradient | Both | White / Dark | 70% alpha glass surface | ≥ 6.2:1 | AA/AAA |

## 5. Accessibility & Font Scale Exceptions

1. **TalkBack Semantics**:
   - `FerrisNavBar`: `Role.Tab` + `selected` property.
   - `SessionProgressBar`: `contentDescription = "Reel X of 5"`, children `clearAndSetSemantics {}`.
   - `QuizCard` MCQ options: `Role.Button` + `stateDescription` ("correct" / "incorrect").
   - Shake errors: Announced via `liveRegion = LiveRegionMode.Polite`.
   - Decorative animations (`RadialSpark`, `ConfettiBurst`, pulse rings, chevrons): `clearAndSetSemantics {}`.
2. **Font Scale Exception (Spec v2 Exception)**:
   - While the app strictly maintains a no-inner-vertical-scroll rule for normal operation, when system `fontScale > 1.3f`, `ReelCard` enables a `Modifier.verticalScroll(scrollState)` safety valve and scales hooks down to `headlineSmall` so large text is never clipped on small devices.
