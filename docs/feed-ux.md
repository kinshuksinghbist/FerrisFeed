# FerrisFeed UX — v3 ("Workbench")

Supersedes the P6/P7 passes (glass, dual design philosophies, floating rail,
speaker opening). Code is the source of truth; this note says *why*.

## 1. Shell

- Three tabs on a solid bottom bar: **Learn** (feed), **Path**, **You**.
  The bar sits under the content and never overlaps it; labels are always
  shown. It hides on pushed feeds (topic, single lesson), whose own top bar
  has a back button. There is exactly one top bar per screen, owned by the
  screen (the old shell-level stat bar + per-screen back chip produced two
  stacked back buttons on topic feeds).
- First run: three-step onboarding (what it is → track → experience). The
  answers set feed order only; nothing is ever hidden. Editable in You.

## 2. Learn feed

- Vertical pager, played in **sets of five**: lesson → quick check for each
  reel, then a **recap page** (score ring, XP earned, streak, daily goal,
  topics covered, up next). `FeedPaging` owns the mapping and is unit-tested.
- Top bar: streak · five set segments (green right / red wrong / ink
  current / hairline upcoming) · XP. Scoped feeds: back · topic title ·
  "Lesson n of N" · XP, with a thin topic progress meter.
- Pages scroll **only when content overflows**. Short lessons still advance
  with the smallest flick (nested scroll hands leftover drag to the pager);
  long ones and answered quizzes never clip their explanation.
- Lesson: track · topic tag and difficulty, hook (26sp), body with inline
  `code`/**bold**, code well (Output / Copy), "Key idea" takeaway. Save via
  the bookmark or a double-tap anywhere (burst shows only when saving).
- Quick check: options are **shuffled per reel** (stable by reel id) because
  authored answers cluster in early positions. One grade per reel. Feedback
  panel: verdict + XP, explanation, and the reel's **common trap** (authored
  for every reel, never shown before v3).
- Like was removed: nothing consumed it. Save is the one personal action.

## 3. Queue order (`QueueBuilder`)

Fresh reels follow the curriculum (unit → topic → level → authored order)
for the chosen track(s); both tracks alternate 3 Rust : 2 System Design per
set. Units below the learner's starting point move to the back. Due reviews
take at most two slots per set, after the new material. Topic feeds play the
topic in lesson order and open at the first unseen lesson.

## 4. Path

Track switch → one **Up next** card → numbered units, each a timeline of
topics (number / progress ring / check). Search is behind the header button:
keyword + track and level chips, results open the lesson inside its topic.
No hard locks: experienced learners must never be stuck behind basics.

## 5. You

Rank (XP stage) with progress to the next, streak week + today's goal,
lessons done / accuracy / checks taken, strongest and weakest topics (tap to
practise), saved lessons, learning focus.

## 6. Accessibility

- Every tappable ≥ 48dp; icon buttons carry descriptions; tabs and chips
  expose `Role` + `selected`; answer states use `stateDescription` and a
  polite live region; difficulty is announced as text.
- Colour is never the only signal: answer rows add check/cross badges, chips
  invert to solid ink when selected, topic nodes change shape by status.
- Track ink colours (`rustInk`, `sysDesignInk`) are the only coloured small
  text; raw track hues are for dots and fills.
- Reduce motion (animator scale 0) freezes skeleton breathing, hint bounce,
  shake and confetti; functional fades remain.
- Font scale > 1.3 drops the hook to `headlineSmall`; pages scroll anyway.
