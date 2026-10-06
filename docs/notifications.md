# Notifications + Widget — FerrisFeed

## 1. Daily 9pm nudge

- Channel: `ferris_nudge` (importance DEFAULT, no sound by default).
- Schedule: exact-inexact daily alarm via WorkManager
  (`NudgeWorker`, 21:00 local, `setInitialDelay` from now). Respects
  Do-Not-Disturb; never vibrates through it.
- Copy rotates among unsolved trap questions, never generic "come back!":
  - "That `E0382 use of moved value` — can you fix it in 60 seconds?"
  - "Your Lifetimes mastery slipped to 58%. One reel will fix it."
  - "Streak at 6 days. One Daily Mix makes it a week."
- Tapping deep-links to `ferrisfeed://reel/<id>` (the specific trap reel),
  not the home screen. If the reel was mastered since scheduling, falls
  back to the Daily Mix head.
- Frequency cap: max 1/day, auto-skipped if the user already completed a
  Daily Mix that day. Opt-out toggle in Settings + system channel; the
  toggle also cancels pending work (`NudgeWorker.cancel()`).
- Permission: `POST_NOTIFICATIONS` requested contextually after the 3rd
  session (never on first launch), with a rationale sheet explaining the
  9pm trap-question format.

## 2. Glance widget: Reel of the Day + streak

Implementation: `feature-feed/.../feed/Widget.kt` (`ReelOfDayWidget`).

- Size: 2x2 / 4x2 responsive. Shows track pill (RUST/WASM/SYS), hook (max
  3 lines), streak flame count. Tap opens the reel deep link.
- Data: `WidgetReelProvider` resolves the Daily Mix head (or lowest
  stability due card) on a 6h WorkManager tick + on `ACTION_BOOT_COMPLETED`.
  State is stored via `updateAppWidgetState` (DataStore-backed), so the
  widget renders the last known reel offline.
- Themed: `GlanceTheme` colors, Material You dynamic tint on the track
  pill. Dark-mode widget background `#0B0E14` at 92% opacity.

## 3. Material You icon + dynamic color

- Adaptive launcher icon: `mipmap-anydpi-v26/ic_launcher.xml` with
  monochrome `ic_launcher_monochrome.xml` (Ferris silhouette) for themed
  icons on Android 13+.
- In-app dynamic color toggle (Settings -> Appearance): when ON, seed
  color comes from wallpaper (`dynamicDarkColorScheme` /
  `dynamicLightColorScheme`); when OFF, fixed Ferris palette (orange
  `#FF6B35` primary, OLED `#0B0E14` background). Code cards keep
  `JetBrains Mono` + fixed terminal colors either way for readability.
- Widget + notification icon both provide monochrome variants so themed
  icons never fall back to a white box.
