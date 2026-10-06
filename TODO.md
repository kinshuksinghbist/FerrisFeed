# FerrisFeed - Doomscroll to Learn Rust + WASM + System Design
### Android App Master TODO Plan

> Concept: TikTok-style vertical doomscroll, but every swipe is a 60-second Rust / WASM / System Design flashcard. Bite-sized, incremental, with code you can run inline. Built native for Android with Kotlin + Compose, powered by a shared Rust core that also compiles to WASM.

App Name: **FerrisFeed**
Tagline: "Doomscroll, but you get hired."
Platform: Android native (Kotlin + Jetpack Compose + Material 3 Expressive)
Core: Shared Rust crate `ferris-core` via UniFFI + WASM web preview via wasm-pack

---

## PHASE 0 - Product Vision & Tech Decisions

- [ ] **1. Lock product vision, name, and scope**
  Decide: `FerrisFeed` / `CrabScroll`. Tagline: "Doomscroll, but you get hired." Define MVP: offline feed + 300 reels + quizzes + streaks. V2: accounts, leaderboard. Non-goals for V1: user-generated content, comments.

- [ ] **2. Decide tech stack: Kotlin + Compose UI + Rust shared core via UniFFI + WASM web preview**
  UI: 100% Kotlin + Jetpack Compose + Material 3 Expressive. Logic: shared Rust crate `ferris-core` for curriculum model, SRS scheduling, quiz grading. Expose to Android via UniFFI / JNI. Expose same core to web preview via `wasm-pack`. This lets you dogfood Rust + WASM while teaching it. DB: Room + DataStore. MinSdk 26, edge-to-edge, predictive back.

## PHASE 1 - Android Project Scaffold

- [ ] **3. Scaffold Android project: Gradle, Compose BOM, Material3 Expressive, edge-to-edge, minSdk 26**
  Create Gradle version catalog, `:app`, `:core-ui`, `:feature-feed`, `:feature-path`, `:data` modules. Add Compose BOM, Navigation3, Hilt, Room, DataStore, WorkManager, Coil, kotlinx.serialization. Enable R8, baseline profiles, dynamic color.

- [ ] **4. Set up Rust core crate: curriculum types, SRS, quiz engine, UniFFI bindings + wasm-pack target**
  Create `rust-core/` with `cargo init --lib`. Types: `Reel { id, track, level, hook, body_md, code, language, takeaway, trap, quiz }`. Implement FSRS-lite scheduler, XP calculator, prerequisite DAG resolver. Add `uniffi` bindings + `wasm-bindgen` feature flag. CI: `cargo test + clippy + fmt`.

## PHASE 2 - Design System - Make It Unique, Android-Native

- [ ] **5. Define design language: dark Ferris terminal aesthetic, typography, color, motion, haptics**
  Theme: "Midnight Terminal + Warm Paper." Dark OLED `#0B0E14` background, Ferris orange `#FF6B35` primary, mint `#00D9A6` for correct, lavender for system design track. Fonts: System default for body for readability, `JetBrains Mono` for code. Rounded 28dp cards, soft shadows. Motion: 300ms spring swipe, haptic tick on quiz correct. Support Material You dynamic color toggle.

- [ ] **6. Design doomscroll feed UX: VerticalPager reels, double-tap save, long-press peek, bottom-sheet deep dive**
  Full-screen `VerticalPager`. One reel = one screen. Top: track pill + level + read time. Center: hook + body + code. Bottom: actions - Like/Save, "Deep Dive" bottom-sheet, "Got it / Still fuzzy" buttons that feed SRS. Gestures: double-tap to save, long-press to peek answer, swipe left for quiz variant.

- [ ] **7. Build design system components: ReelCard, CodeCard, QuizCard, PollCard, ProgressRing, StreakFlame**
  Build in `core-ui`: `ReelCard`, `CodeCard` with syntax highlight + copy + font-size slider, `QuizCard` MCQ / Tap-The-Bug / Fill-Blank, `TrapCard` for common compiler error, `ProgressRing`, `StreakFlame`, `TrackPill`, shimmer skeletons. All previewable in Compose previews, dark + light + dynamic.

## PHASE 3 - Feed Engine + Content Format

- [ ] **8. Build core feed engine: infinite pager, prefetch, shuffle, impression tracking, read-time estimate**
  Infinite `VerticalPager` with prefetch of next 5 reels, LRU cache for code highlighting. Shuffle algorithm: 70% due SRS cards + 20% new in path order + 10% random review. Track impressions, dwell time, skip rate. Save scroll position and restore instantly.

- [ ] **9. Define bite-size content schema: hook, 60-sec body, code snippet, takeaway, trap, quiz JSON**
  Every reel must fit this JSON, enforced by Rust validator: `hook` 1 line curiosity gap, `body` under 70 words markdown, `code` under 15 lines runnable, `takeaway` 1 sentence, `trap` 1 common mistake, `quiz` 1 question with explanation. Add lint script to reject long reels.

## PHASE 4 - Rust Curriculum (Beginner to Advanced)

- [ ] **10. Author Rust Beginner Track 1: toolchain, cargo, variables, mutability, types, functions, control flow**
  Write 35 reels: install rustup, `cargo new/run/build`, variables, mutability, shadowing, scalar types, functions, `if/let`, `loop/while/for`. Each with a "try to break it" code sample.

- [ ] **11. Author Rust Beginner Track 2: ownership, borrowing, slices, structs, enums, match, Option/Result intro**
  Write 45 reels: ownership moves, clone vs copy, borrowing `&`, mutable borrow, slices, structs, enums, `match`, `if let`, `Option`, `Result` intro. This is the make-or-break section - use visual memory diagrams as images.

- [ ] **12. Author Rust Beginner Track 3: vectors, strings, hashmaps, error handling with ?, modules, testing basics**
  Write 30 reels: `Vec`, `String` vs `&str`, `HashMap`, `?` operator, `panic!` vs `Result`, modules `mod/use`, `cargo test`, formatting `clippy/fmt`.

- [ ] **13. Author Rust Intermediate Track 1: generics, traits, lifetimes, closures, iterators, smart pointers Box/Rc/RefCell**
  Write 40 reels: generics, traits + derive, trait bounds, lifetimes `'a` with visual scope bars, closures `Fn/FnMut`, iterators + adapters, `Box`, `Rc`, `RefCell` with refcount diagrams.

- [ ] **14. Author Rust Intermediate Track 2: Arc/Mutex, channels, multithreading, async/await, tokio basics, futures**
  Write 35 reels: `Arc<Mutex<T>>`, `mpsc` channels, `thread::spawn/scope`, `Send/Sync`, `async/await`, `Future` polling mental model, Tokio runtime, `spawn`, `join!`, `select!`.

- [ ] **15. Author Rust Intermediate Track 3: macros, attributes, cargo workspaces, features, docs, benchmarking**
  Write 25 reels: declarative `macro_rules!`, derive macros, attributes, workspaces, features + `cfg`, docs + doctests, `criterion` benches.

- [ ] **16. Author Rust Advanced Track 1: unsafe, raw pointers, FFI, lifetimes variance, Pin, Send/Sync, memory layout**
  Write 30 reels: `unsafe` 5 superpowers, raw pointers, `*const/*mut`, FFI `extern C`, `Send/Sync` manual impl, `Pin/Unpin`, `repr(C)`, layout, aliasing, Miri.

- [ ] **17. Author Rust Advanced Track 2: advanced async (select, cancellation, backpressure), tokio internals, rayon**
  Write 25 reels: cancellation, `CancellationToken`, backpressure, bounded channels, `tokio::sync::{Semaphore,RwLock}`, `rayon` data parallelism vs async.

- [ ] **18. Author Rust Advanced Track 3: trait objects vs impl, GATs, const generics, type-state pattern, zero-cost abstractions**
  Write 25 reels: `dyn Trait` vs `impl Trait`, GATs, const generics, type-state builder pattern, `PhantomData`, zero-cost abstraction proof with `godbolt` assembly reel.

- [ ] **19. Author Rust Advanced Track 4: performance profiling, flamegraphs, jemalloc, SIMD, no_std/embedded intro**
  Write 20 reels: `cargo flamegraph`, `perf`, `jemalloc/mimalloc`, SIMD `std::simd`, `no_std` + embedded `cortex-m` teaser.

## PHASE 5 - WASM in Rust Track

- [ ] **20. Author WASM in Rust Track 1: what is WASM/WASI, linear memory, wasm-bindgen, wasm-pack hello world**
  Write 25 reels: What is WASM stack machine, linear memory, WAT teaser, WASI vs browser, `wasm-bindgen` hello, `wasm-pack build --target web`.

- [ ] **21. Author WASM in Rust Track 2: JS interop, web-sys/js-sys, DOM, fetch, canvas, serde-wasm-bindgen**
  Write 30 reels: `#[wasm_bindgen]` exports/imports, `js-sys`, `web-sys` DOM manipulation, `fetch` + `serde-wasm-bindgen`, canvas pixel loop, callbacks + closures leak trap.

- [ ] **22. Author WASM in Rust Track 3: WASI, wasmtime/wasmer, filesystem, CLI tools in WASM, component model/WIT**
  Write 25 reels: WASI filesystem/caps, `wasmtime` run `.wasm` CLI, `wasmer`, WASM Component Model + WIT `world` definition, `warg` registries.

- [ ] **23. Author WASM in Rust Track 4: bundling for web, vite/webpack, size optimization, wasm-opt, lazy loading on Android WebView**
  Write 20 reels: bundler with Vite, `wasm-opt -Oz`, `wee_alloc`, lazy-load WASM in Android WebView, measure `.wasm` size budget reel.

- [ ] **24. Author WASM in Rust Track 5: Rust+Yew/Leptos/Dioxus mini-apps compiled to WASM + embedded playground**
  Write 20 reels: Yew vs Leptos vs Dioxus hello apps compiled to WASM, embed live WASM demo in app via WebView that runs actual Rust-compiled examples offline.

## PHASE 6 - System Design Track

- [ ] **25. Author System Design Beginner: HTTP, REST, WebSockets, SQL vs NoSQL, caching, load balancer basics**
  Write 30 reels: HTTP lifecycle, REST vs gRPC vs GraphQL, WebSocket, SQL vs NoSQL picker, Redis caching patterns, LB + round-robin, CDN.

- [ ] **26. Author System Design Intermediate: consistent hashing, CAP/PACELC, sharding, replication, Kafka/queues, rate limiting**
  Write 35 reels: consistent hashing ring visual, CAP/PACELC, sharding key, leader/follower replication, Kafka partitions, token bucket rate limiter with Rust code, retry + idempotency.

- [ ] **27. Author System Design Advanced: distributed transactions, consensus Raft/Paxos, CRDTs, exactly-once, backpressure**
  Write 30 reels: 2PC/Saga, Raft election in 60 sec, CRDT G-Counter, exactly-once trap, backpressure + load shedding, fan-out timeline.

- [ ] **28. Author System Design in Rust: axum/actix architecture, tower middleware, bb8 pools, redis, postgres, observability**
  Write 30 reels: `axum` router + `tower` middleware stack, `bb8` + `deadpool` pools, Postgres `sqlx`, Redis cache-aside code, `tracing + OpenTelemetry`, Dockerfile multi-stage for Rust minimal image.

- [ ] **29. Author System Design Case Reels: URL shortener, Twitter feed, rate limiter, chat, object store in Rust**
  Write 25 reels as swipeable blueprints: URL shortener, Twitter home feed, global rate limiter, chat with WebSocket + presence, S3-like object store. Each: requirements -> API -> storage -> scale bottleneck -> Rust snippet.

## PHASE 7 - Learning Science & Interactivity

- [ ] **30. Build interactive code cards: syntax highlight, copy, runnable snippets via embedded WASM Rust playground**
  Syntax highlight with tree-sitter theme, one-tap copy, pinch to enlarge, "Run" button that executes via embedded WASM interpreter for beginner snippets or links to Rust Playground for advanced. Show compiler error as friendly card.

- [ ] **31. Build quiz/flashcard layer: MCQ, tap-the-bug, fill-blank, spaced repetition SM-2/FSRS in Rust core**
  Implement in Rust core: MCQ, tap-the-bug line picker, fill-blank. Grade locally. Implement FSRS: `Again/Hard/Got it` -> next due date. Due queue drives feed order. Show mastery % per topic that decays if you skip.

- [ ] **32. Build learning path engine: DAG prerequisites, placement test, daily mix, resume-where-left-off**
  DAG: Ownership -> Lifetimes -> Async -> Axum -> Rate Limiter. Placement test of 15 quizzes to skip beginner. Daily Mix: 7 new + 8 reviews + 5 weak spots. Resume bar: "Continue Ownership 62%."

- [ ] **33. Build progress system: XP, mastery per topic, streaks, heatmap, achievements, Ferris evolution**
  XP per reel + bonus for streak, heatmap calendar, Ferris mascot evolves Egg -> Crab -> Armored Crab. Achievements: "Borrow Checker Survivor", "WASM Summoner", "P99 Slayer."

- [ ] **34. Build search + topic map: full-text search, tags, beginner-to-advanced roadmap graph UI**
  Full-text Room FTS search, filter by track/level/has-code/has-quiz. Visual roadmap graph with nodes lighting up as you master prerequisites.

## PHASE 8 - Android Polish, Offline & Launch

- [ ] **35. Build offline-first: Room + DataStore, prepackaged SQLite curriculum, WorkManager sync, image/code cache**
  Prepack Room DB with all 400+ reels in APK asset. DataStore for progress. WorkManager to sync new packs weekly. Cache all code + images for airplane mode.

- [ ] **36. Build notifications + widgets: daily reel widget, streak reminder, Material You dynamic color support**
  Glance widget: "Reel of the Day" + streak count. Daily 9pm nudge with unsolved trap question. Full Material You icon + themed widget support.

- [ ] **37. Add delightful motion: shared-element transitions, skeleton shimmer, confetti on mastery, haptic ticks**
  Shared-element from feed to deep-dive sheet, confetti on level-up, skeleton shimmer while prefetching, predictive back to feed. Ensure 120Hz smooth `VerticalPager` with baseline profile.

- [ ] **38. Instrument analytics + A/B: scroll depth, dwell, quiz accuracy, retention, Firebase/RemoteConfig**
  Log dwell, save rate, quiz accuracy per topic. Use Firebase + RemoteConfig to A/B test hook wording. No PII, opt-out toggle.

- [ ] **39. Harden quality: unit tests for Rust core, Compose UI tests, baseline profiles, R8, accessibility audit**
  `cargo test` for SRS + DAG, JUnit for ViewModels, Compose UI tests for pager + quiz, screenshot tests for cards, accessibility TalkBack labels, minimum touch 48dp, R8 size audit under 25MB.

- [ ] **40. Prep launch: Play Store listing, screenshots, demo video, privacy policy, beta track + feedback loop**
  Play listing, 8 screenshots showing doomscroll + code + quiz, 30-sec demo video, privacy policy, open beta track, in-app "Suggest a reel / Report bug" sheet feeding GitHub issues.

---

## Content Count Target
- Rust Beginner: ~110 reels
- Rust Intermediate: ~100 reels
- Rust Advanced: ~100 reels
- WASM in Rust: ~120 reels
- System Design: ~120 reels
- Total MVP: 400-550 bite-size reels, each <90 sec

## Bite-Size Reel Schema (enforced)
```json
{
  "id": "rust-own-014",
  "track": "rust | wasm | system-design",
  "level": 1,
  "hook": "Why does this simple function not compile?",
  "body_md": "<=70 words",
  "code": "fn main() { ... }",
  "takeaway": "One sentence rule",
  "trap": "Common compiler error",
  "quiz": { "q": "...", "options": [], "answer": 0, "explain": "..." }
}
```

## Next Step
Say "start" to scaffold the Android project + Rust core + first 10 beginner reels.
