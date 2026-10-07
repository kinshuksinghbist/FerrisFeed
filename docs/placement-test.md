# Placement Test — Draft Only (TODO 26)

> Status: DRAFT. No screens ship from this item (26a). The draft below must
> survive review before any UI is built; the prototype then goes through
> `/visual-critique:critique-screen` and ships on the normal Friday train
> (26c). Shaped by `onboarding-design` + `form-design`, framed with
> `jobs-to-be-done`.

## The job, not the feature

An experienced Rust dev hires first-run to **skip what they already know**,
not to prove they are advanced. A beginner hires it to **start without
fear of a quiz that punishes them on day one**. The smallest flow that does
both jobs:

1. One screen of plain-language promise ("60-second cards, quiz is the only
   grade, offline, no account") + analytics opt-out (privacy policy §12).
2. Optional 5-question placement (2x L1, 2x L2, 1x L3 from the seeded bank
   — `PlacementTest.selectItems`). Skippable with one tap ("Start at the
   beginning").
3. Result in one line ("Start at Level 2 — basics skipped") with a visible
   "Retake / start over" path. Writes `ProgressStore.setPlacementComplete`
   only.

No streak, no XP, no mastery writes during placement: placement measures,
it does not grade.

## Items come from the bank (26b)

`PlacementTest` in `:feature-path` is the rule, unit-tested in
`PlacementTestTest`. Bank = seeded reels with quizzes, lowest `orderIndex`
per (level, topic), distinct topics preferred. Scoring: 4–5 correct skips
beginner; 3 correct skips only with the L3 stretch; anything less starts at
L1. The rule is pure Kotlin so the review argues about numbers.

## Study plan (26b, `usability-test-plan` + `test-scenario`)

- 5 participants: 2 never-touched-Rust, 2 wrote-some-Rust, 1 ships-Rust.
- Tasks: (1) complete first-run without help, (2) place out of beginner if
  experienced, (3) find where to retake. Success: all finish < 3 min, all
  can name where the quiz grade goes, none feels punished by a wrong answer.
- Facilitation: think-aloud, no help on wording; note every re-read.

## Prototype gate (26c)

Build the prototype ONLY after this draft is approved. Review with
`/visual-critique:critique-screen` (affordance + density + hierarchy),
then ship on the Friday train — never as a hotfix.
