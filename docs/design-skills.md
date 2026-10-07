# Design Skills — the Designer Skills Pack, kept in-repo

This file is our working copy of the **[Designer Skills Pack](https://github.com/Owl-Listener/designer-skills)**
by MC Dean: 111 design skills and 34 commands, written to be read and applied by
an AI agent rather than skimmed by a human. It is here so that any agent working
on FerrisFeed — and the person reviewing the result — can reach for the same
design judgment without leaving the repo.

Skills are **nouns** (knowledge: "use when…"), commands are **verbs**
(workflows: `/ui-design:color-palette`). You don't invoke a skill; you name it
when the work calls for it, e.g. "use `critique-typography` on this card."

## Provenance

| | |
| --- | --- |
| Source | https://github.com/Owl-Listener/designer-skills |
| Pinned commit | `9a6930cf84a822eb458624bd11c61aac5bbdf224` (2026-09-05, `main`) |
| Marketplace version | 2.0.0 |
| Author | MC Dean — [MC Percolates](https://marieclairedean.substack.com/) |
| Licence | MIT (full text at the bottom of this file) |

What's included here is the **complete skill catalogue with each skill's verbatim
`description`** — the "what it produces / use when / use this instead" contract,
which is the part you need in order to pick the right skill. The full bodies
(300 KB of judgment: principles, tables, worked examples) stay upstream; nothing
here is paraphrased, so a description always matches the body you find there.

To install the bodies so an agent can read them in full:

```
/plugin marketplace add Owl-Listener/designer-skills     # Claude Code
/plugin install ux-strategy@designer-skills              # then install per plugin
```

Gemini CLI users: clone the repo and copy `.gemini/extensions/` into the project.

## Which skill for which part of FerrisFeed

The pack is generic; this table is our mapping. It points at the files and docs
the skill applies to, so the next agent doesn't have to re-derive it.

| Area / file | Reach for |
| --- | --- |
| The reel card itself — `core-ui/ReelCard.kt`, `docs/feed-ux.md` | `reel-card-composition` (ours, below), `visual-hierarchy`, `law-of-proximity`, `law-of-common-region` |
| Difficulty cue — `core-ui/DifficultyLabel.kt` | `animation-principles`, `micro-interaction-spec`, `motion-system` |
| Track wash + palette — `core-ui/Theme.kt` (`FerrisColors`, dark OLED `#0B0E14`, Ferris orange `#FF6B35`) | `color-system`, `dark-mode-design`, `design-token`, `critique-color` |
| Code card — `core-ui/CodeCard.kt` | `typography-scale`, `readable-measure`, `critique-typography` |
| Pager physics, swipe thresholds — `FeedScreen.kt`, `docs/motion.md` | `gesture-patterns`, `doherty-threshold`, `jakobs-law` (feed = familiar vertical swipe) |
| Right action rail (48dp targets, double-tap save) | `fitts-law`, `feedback-patterns` |
| Inline quiz + grading — `core-ui/QuizCard.kt`, `FeedViewModel.onGrade` | `state-machine`, `feedback-patterns`, `peak-end-rule`, `zeigarnik-effect` |
| Empty / loading / seeding states | `loading-states`, `doherty-threshold`, `ux-writing` |
| Nav: Feed + Path tabs, topic feeds (`Route.TopicFeed`) | `navigation-patterns`, `information-architecture`, `user-flow-diagram` |
| Path roadmap + embedded search — `PathScreen.kt`, `PathViewModel.kt` | `information-architecture`, `search-ux`, `visual-hierarchy` |
| Placement test / first-run (when it lands) | `onboarding-design`, `form-design`, `jobs-to-be-done` |
| Streak reminder + sync notifications | `ux-writing`, `feedback-patterns`, `peak-end-rule` |
| Accessibility: both themes, TalkBack, contrast | `accessibility-audit`, `critique-color`, `accessibility-test-plan` |
| Pre-ship review of a screen (screenshot in hand) | `/visual-critique:critique-screen`, `critique-affordance`, `critique-information-density` |
| Component specs for ReelCard / CodeCard / QuizCard | `component-spec`, `documentation-template`, `naming-convention` |
| Handoff notes into `docs/` | `handoff-spec`, `design-qa-checklist` |
| Play listing / store copy — `docs/play-listing.md` | `ux-writing`, `content-strategy` |
| Accumulated UI drift (e.g. inline colours vs tokens) | `design-debt-audit`, `design-token-audit` |

Two rules from the pack worth repeating because our codebase violates them most
easily: **one idea per screen, one gesture** (if a card needs an inner scroll, the
card is doing too much) and **the question that grades the learning is not
optional chrome** — assessment belongs inside the card, not behind a second
screen.

## The 111 skills, by plugin

Verbatim `description` fields from the pinned commit. Plugin blurbs are from the
pack's marketplace manifest.

### `design-research` — 14 skills, 4 commands

User research skills for designers: personas, empathy maps, journey maps, interview scripts, usability testing, card sorting, behavioural analytics, and reconciling data with research.

- **`affinity-diagram`** — Cluster many qualitative data points into themes and insight statements. Use when synthesising across multiple sessions or sources. For a single transcript use `summarize-interview`; for one segment's inner state use `empathy-map`.
- **`behavioural-analytics`** — Read funnels, retention curves, and event data as a designer — separating a design problem from a tracking artefact. Use when handed product data you did not design and asked why people drop off. For choosing what to measure, use `metrics-definition` (ux-strategy); for running a controlled test, use `a-b-test-design` (prototyping-testing).
- **`card-sort-analysis`** — Analyse open or closed card sort results into a proposed grouping and label set. Use after running a sort study. For turning that evidence into a full structure, use `information-architecture` (ux-strategy).
- **`diary-study-plan`** — Design a diary study — prompts, cadence, duration, participant criteria, and analysis frame. Use when behaviour unfolds over days or weeks. For a single-session study, use `usability-test-plan`.
- **`empathy-map`** — Build a Says, Thinks, Does, Feels map for one user or segment. Use when sharing user understanding quickly. For a composite archetype with goals and behaviours use `user-persona`; for cross-session themes use `affinity-diagram`.
- **`interview-script`** — Write a structured interview guide — warm-up, core exploration, and wrap-up. Use before running interviews. For analysing what comes back, use `summarize-interview`.
- **`jobs-to-be-done`** — Map functional, emotional, and social jobs with outcome expectations. Use when reframing decisions around motivation rather than features. For who the user is, use `user-persona`.
- **`journey-map`** — Map one persona's end-to-end experience with stages, touchpoints, emotions, and pain points. Use when improving an existing experience. For the multi-channel ecosystem use `experience-map` (ux-strategy); for screen-level paths use `user-flow-diagram` (prototyping-testing).
- **`qual-quant-triangulation`** — Reconcile what the numbers say with what users say, and design the study that settles it rather than restates it. Use when behavioural data and research findings point different ways. For reading the data on its own, use `behavioural-analytics`; for synthesising interviews on their own, use `affinity-diagram`.
- **`research-repository`** — Build a repository that makes findings findable, reusable, and cumulative across teams. Use when the same research keeps getting redone. For synthesising one study, use `affinity-diagram`.
- **`summarize-interview`** — Turn one interview transcript into themes, supporting quotes, and action items. Use immediately after a session. For synthesising many sessions at once, use `affinity-diagram`.
- **`survey-design`** — Design unbiased survey instruments — question wording, scales, and sampling — to measure attitudes at scale. Use when you need quantitative breadth. For behavioural experiments, use `a-b-test-design` (prototyping-testing).
- **`usability-test-plan`** — Design a usability study — research questions, methodology, participant criteria, metrics, and facilitation guide. Use when planning the study as a whole. For writing the task scenarios inside it, use `test-scenario` (prototyping-testing).
- **`user-persona`** — Build research-grounded personas with goals, frustrations, and behavioural patterns. Use when decisions need a consistent user reference. For one session's emotional snapshot use `empathy-map`; for motivation framing use `jobs-to-be-done`.

Commands: `/design-research:discover`, `/design-research:interview`, `/design-research:synthesize`, `/design-research:test-plan`

### `design-systems` — 11 skills, 3 commands

Design system skills: component specs, design tokens, naming conventions, spacing and grid systems, accessibility standards, and documentation.

- **`accessibility-audit`** — Audit an existing interface against WCAG, producing findings with severity ratings and remediation steps. Use when you have a design or build to assess now. Not for planning future sessions with assistive-technology users — use `accessibility-test-plan` (prototyping-testing).
- **`component-spec`** — Specify one component — props, states, variants, accessibility, and usage rules. Use when defining a library component. For the reusable doc scaffold use `documentation-template`; for a problem-solution pattern use `pattern-library`.
- **`design-system-governance`** — Define how the system evolves — contribution model, versioning, deprecation, and change management. Use when multiple teams contribute. For driving uptake use `design-system-adoption` (designer-toolkit); for design file history use `version-control-strategy` (design-ops).
- **`design-token`** — Define and organise tokens for colour, spacing, type, and elevation with naming and usage rules. Use when establishing the token layer. For auditing existing usage use `design-token-audit` (designer-toolkit); for multi-brand mapping use `theming-system`.
- **`documentation-template`** — Generate a reusable documentation scaffold for components, patterns, or guidelines. Use when standardising how the system is documented. For the content of one component's spec, use `component-spec`.
- **`icon-system`** — Specify an icon system — grid, sizing, stroke weight, naming, categories, and implementation. Use when standardising iconography. For broader illustration, use `illustration-style` (ui-design).
- **`localization-design`** — Design for multiple languages, writing directions, and cultural contexts — text expansion, RTL mirroring, and locale formats. Use when shipping beyond one locale. For the words themselves, use `ux-writing` (designer-toolkit).
- **`motion-system`** — Define motion tokens — durations, easing vocabulary, and reduced-motion handling — for consistency product-wide. Use when standardising motion across a system. For crafting one specific animation, use `animation-principles` (interaction-design).
- **`naming-convention`** — Establish naming rules for components, tokens, and layers with patterns and worked examples. Use when names are inconsistent or being set. For what the tokens actually contain, use `design-token`.
- **`pattern-library`** — Structure a pattern entry — problem context, solution, usage examples, and related patterns. Use when documenting a recurring solution rather than a component. For a single component's API, use `component-spec`.
- **`theming-system`** — Design theming architecture — brand variants, dark mode, and high-contrast — mapped through token layers. Use when one system must serve multiple themes. For a single palette use `color-system` (ui-design); for dark mode craft use `dark-mode-design` (ui-design).

Commands: `/design-systems:audit-system`, `/design-systems:create-component`, `/design-systems:tokenize`

### `ux-strategy` — 12 skills, 3 commands

UX strategy skills: information architecture, content strategy, navigation patterns, user flows, task analysis, and competitive UX audits.

- **`business-design`** — Read financials, map competitive landscapes, and argue design decisions in the language of value. Use when defending design to commercial stakeholders. For the live negotiation itself, use `design-negotiation` (designer-toolkit).
- **`competitive-analysis`** — Compare UX patterns, features, strengths, and gaps across rival products. Use when you need to know what others actually do. For deliberately adopting their conventions, use `jakobs-law` (interaction-design).
- **`content-strategy`** — Define what content a product needs, how it is structured, and who owns it. Use when content itself is the problem. For the words in the interface use `ux-writing` (designer-toolkit); for structural hierarchy use `information-architecture`.
- **`design-brief`** — Write a project brief — problem space, constraints, audience, and success criteria. Use at kickoff for one specific project. For long-horizon aspiration use `north-star-vision`; for reusable decision rules use `design-principles`.
- **`design-principles`** — Define actionable principles that resolve trade-offs when the team disagrees. Use when the same decisions keep getting relitigated. For a single project's framing, use `design-brief`.
- **`experience-map`** — Map the full ecosystem of touchpoints, channels, and relationships across a service. Use when the experience spans more than one product. For one persona's linear journey use `journey-map` (design-research); for backstage operations use `service-blueprint`.
- **`information-architecture`** — Design content structure, hierarchy, labelling, and the navigation model. Use when organising what exists. For the UI that exposes it use `navigation-patterns` (interaction-design); for user-generated grouping evidence use `card-sort-analysis` (design-research).
- **`metrics-definition`** — Define UX metrics and KPIs that connect design decisions to measurable outcomes. Use when choosing what to measure. For presenting the results afterwards, use `design-impact-reporting` (design-ops).
- **`north-star-vision`** — Articulate a long-horizon product vision that aligns teams and anchors strategy. Use when direction is contested or absent. For near-term project scope, use `design-brief`.
- **`opportunity-framework`** — Identify, score, and prioritise design opportunities against impact and effort. Use when there are more ideas than capacity. For framing the one you choose, use `design-brief`.
- **`service-blueprint`** — Map service delivery across frontstage actions, backstage processes, and supporting systems. Use when staff and operations are part of the experience. For the customer-visible layer only, use `experience-map`.
- **`stakeholder-alignment`** — Build alignment artifacts — responsibility matrices, decision rights, and communication plans. Use when unclear ownership stalls decisions. For persuading in the moment, use `design-negotiation` (designer-toolkit).

Commands: `/ux-strategy:benchmark`, `/ux-strategy:frame-problem`, `/ux-strategy:strategize`

### `ui-design` — 19 skills, 5 commands

UI design skills: color palettes, typography systems, layout grids, responsive breakpoints, dark mode specs, visual hierarchy, and brand alignment.

- **`aesthetic-usability`** — Apply the Aesthetic-Usability Effect — polished, consistent interfaces are perceived as more usable and forgive minor friction. Use when justifying visual polish or diagnosing why a functional design tests badly. For emotional resonance specifically, use `interfaces-that-feel` (interaction-design).
- **`color-system`** — Build a product colour system — tonal scales, semantic roles, and contrast compliance. Use when defining or rebuilding colour from scratch. For dark-mode adaptation use `dark-mode-design`; for chart palettes use `data-visualization`; for multi-brand token architecture use `theming-system` (design-systems).
- **`dark-mode-design`** — Adapt an existing palette to dark mode — surface elevation, contrast rebalancing, and desaturation rules. Use when you already have a light palette to translate. For building the base palette first, use `color-system`.
- **`data-visualization`** — Select chart types and design data encodings — marks, axes, labels, and accessible chart styling. Use when presenting data graphically. Owns chart selection and encoding only; the categorical colour ramp itself belongs to `color-system`.
- **`illustration-style`** — Define an illustration style guide — visual language, colour usage, and application rules. Use when commissioning or standardising illustration. For icons, use `icon-system` (design-systems).
- **`law-of-closure`** — Apply the Law of Closure — the eye completes implied shapes from partial forms. Use when reducing visual weight by dropping borders or letting negative space suggest structure. For explicit containers, use `law-of-common-region`.
- **`law-of-common-region`** — Apply the Law of Common Region — a shared container, background, or border groups elements regardless of spacing. Use when grouping must survive a tight layout. For grouping by spacing alone, use `law-of-proximity`.
- **`law-of-continuity`** — Apply the Law of Continuity — the eye follows alignment and unbroken paths. Use when sequencing steps, aligning content, or designing carousels and timelines. For grouping rather than sequencing, use `law-of-proximity`.
- **`law-of-figure-ground`** — Apply the Law of Figure-Ground — establish which layer is foreground and actionable versus background. Use when designing modals, overlays, and depth. For emphasising one element among peers, use `von-restorff-effect`.
- **`law-of-proximity`** — Apply the Law of Proximity — spatial closeness groups elements more strongly than any other cue. Use when spacing alone must carry grouping. For grouping via containers use `law-of-common-region`; via shared appearance use `law-of-similarity`.
- **`law-of-similarity`** — Apply the Law of Similarity — shared colour, shape, or size signals that elements belong to one category. Use when signalling relationships across distance. For grouping by position, use `law-of-proximity`.
- **`layout-grid`** — Define a responsive grid — columns, gutters, margins, and breakpoint behaviour. Use when establishing page structure. For the spacing scale inside components use `spacing-system`; for cross-device behaviour use `responsive-design`.
- **`platform-conventions`** — Design to iOS and Android conventions — what each OS mandates, where they diverge, and when to unify. Use when shipping native apps. For breakpoint adaptation use `responsive-design`; for matching competitor patterns use `jakobs-law` (interaction-design).
- **`readable-measure`** — Set line length and measure for comfortable reading across type sizes and breakpoints. Use when tuning body text. Covers measure only — for the full size and weight scale, use `typography-scale`.
- **`responsive-design`** — Design layouts and interactions that adapt across screen sizes and input methods. Use when one design must serve many viewports. For the underlying column grid use `layout-grid`; for OS-specific patterns use `platform-conventions`.
- **`spacing-system`** — Create a spacing scale from a base unit with rules for when each step applies. Use when standardising padding and margins. For page-level columns and gutters, use `layout-grid`.
- **`typography-scale`** — Create a modular type scale with size, weight, and line-height relationships. Use when establishing typographic structure. For line length only use `readable-measure`; for judging type on an existing screen use `critique-typography` (visual-critique).
- **`visual-hierarchy`** — Establish hierarchy through size, weight, colour, spacing, and position so the eye lands in the intended order. Use when composing new work. For judging an existing screen, use `critique-visual-hierarchy` (visual-critique).
- **`von-restorff-effect`** — Apply the Von Restorff Effect — the element that differs from its neighbours is the one remembered. Use when a single action must dominate. For overall ordering rather than single-element emphasis, use `visual-hierarchy`.

Commands: `/ui-design:color-palette`, `/ui-design:design-screen`, `/ui-design:platform-audit`, `/ui-design:responsive-audit`, `/ui-design:type-system`

### `interaction-design` — 22 skills, 5 commands

Interaction design skills: micro-interactions, animation principles, state machines, gesture patterns, error handling UX, and feedback patterns.

- **`animation-principles`** — Apply animation principles — easing, staging, follow-through — to one specific UI motion. Use when tuning how an animation feels. For product-wide duration and easing tokens use `motion-system` (design-systems); for a full interaction spec use `micro-interaction-spec`.
- **`conversational-ux`** — Design voice and conversational interfaces — dialog flows, error recovery, and persona. Use when the interface speaks and listens rather than being tapped. For graphical input collection, use `form-design`.
- **`doherty-threshold`** — Apply the Doherty Threshold — keep system response under 400ms to preserve user flow. Use when diagnosing perceived slowness or setting a performance budget. For what to show during unavoidable waits, use `loading-states`.
- **`error-handling-ux`** — Design error prevention, detection, and recovery across a product — message content, placement, and escape routes. Use when errors span multiple flows. For validation inside a single form, use `form-design`.
- **`feedback-patterns`** — Design confirmations, status updates, and notifications that tell users an action registered. Use when the system must acknowledge success or change. For waiting states use `loading-states`; for failures use `error-handling-ux`.
- **`fitts-law`** — Apply Fitts's Law — target acquisition time depends on size and distance. Use when sizing and positioning controls, especially for touch. For how many controls to show at once, use `hicks-law`.
- **`form-design`** — Design a form end to end — field order, grouping, validation, and completion. Use when the artifact is a form. For product-wide error strategy use `error-handling-ux`; for first-run signup use `onboarding-design`.
- **`gesture-patterns`** — Design gesture interactions for touch and pointer — swipe, drag, long-press, and their discoverability. Use when input is gestural. For OS-standard gestures on iOS and Android, use `platform-conventions` (ui-design).
- **`hicks-law`** — Apply Hick's Law — decision time grows with the number of simultaneous choices. Use when a screen offers too many options at once. For how many items survive in memory afterwards, use `millers-law`.
- **`interfaces-that-feel`** — Apply an emotional resonance lens to a UI that is technically correct but flat, prescribing changes at the copy, motion, and interaction layer. Use when a design tests fine but lands cold. For the polish-perception argument, use `aesthetic-usability` (ui-design).
- **`jakobs-law`** — Apply Jakob's Law — users expect your product to work like the others they already use. Use when deciding whether to innovate on a familiar pattern. For OS-mandated conventions specifically, use `platform-conventions` (ui-design).
- **`loading-states`** — Design waiting experiences — spinners, skeletons, optimistic updates, and progressive reveal. Use when content takes time to arrive. For the latency budget itself use `doherty-threshold`; for success confirmation use `feedback-patterns`.
- **`micro-interaction-spec`** — Specify one micro-interaction completely — trigger, rules, feedback, loops, and modes. Use when handing a single interaction to engineering. For motion craft alone use `animation-principles`; for multi-state components use `state-machine`.
- **`millers-law`** — Apply Miller's Law — chunk information into groups of about four to fit working memory. Use when grouping fields, menu items, or steps. For reducing the number of choices offered, use `hicks-law`.
- **`navigation-patterns`** — Select and design a navigation pattern — tabs, drawer, hierarchy, or hub — matched to product structure and user tasks. Use when choosing how users move between sections. For the underlying content structure, use `information-architecture` (ux-strategy).
- **`onboarding-design`** — Design the first-run experience — activation path, progressive disclosure, and time to first value. Use for a user's very first session. For the mechanics of the signup form itself, use `form-design`.
- **`peak-end-rule`** — Apply the Peak-End Rule — a flow is remembered by its most intense moment and its last. Use when designing completion, celebration, or cancellation moments. For sustaining engagement mid-flow, use `zeigarnik-effect`.
- **`search-ux`** — Design search — query input, zero results, refinement, and result presentation. Use when users retrieve rather than browse. For browse structure, use `navigation-patterns`.
- **`serial-position-effect`** — Apply the Serial Position Effect — first and last items in a sequence are recalled best. Use when ordering menus, lists, and steps. For emphasising one item regardless of its position, use `von-restorff-effect` (ui-design).
- **`state-machine`** — Model component behaviour as explicit states, events, and transitions. Use when a component has many interacting states that must be exhaustive. For the feel and feedback of a single interaction, use `micro-interaction-spec`.
- **`teslers-law`** — Apply Tesler's Law — every process has irreducible complexity that someone must absorb. Use when deciding whether the product or the user carries it. For reducing apparent choice, use `hicks-law`.
- **`zeigarnik-effect`** — Apply the Zeigarnik Effect — incomplete tasks stay mentally active. Use when designing progress indicators, saved drafts, and return hooks. For the emotional shape of the ending, use `peak-end-rule`.

Commands: `/interaction-design:design-form`, `/interaction-design:design-interaction`, `/interaction-design:design-onboarding`, `/interaction-design:error-flow`, `/interaction-design:map-states`

### `prototyping-testing` — 10 skills, 5 commands

Prototyping and testing skills: wireframe specs, usability heuristics, heuristic evaluations, accessibility audits, A/B test design, and benchmark analysis.

- **`a-b-test-design`** — Design an A/B experiment — hypothesis, variants, primary metric, and sample size. Use when a change can be measured quantitatively at scale. For observing behaviour qualitatively, use `test-scenario`.
- **`accessibility-test-plan`** — Plan accessibility testing — assistive technologies, participant criteria, WCAG coverage, and session protocol. Use when scheduling testing with real AT users. Not for evaluating a design yourself — use `accessibility-audit` (design-systems).
- **`click-test-plan`** — Design first-click and click tests for findability and navigation. Use when testing whether people can locate something. For full task-based observation, use `test-scenario`.
- **`concept-selection`** — Choose between competing concepts against criteria fixed in advance, and record what each rejected concept was testing. Use when several directions are alive and one has to win. For picking which problem to work on, use `opportunity-framework` (ux-strategy); for deciding by production traffic, use `a-b-test-design`.
- **`heuristic-evaluation`** — Run an expert review against Nielsen's heuristics and domain criteria, with severity ratings. Use when you need findings without recruiting participants. For a facilitated team feedback session, use `design-critique` (design-ops).
- **`parallel-concepts`** — Build several genuinely different solutions to the same problem at once, spread across what the user does rather than how it looks. Use when one direction is on the table and the team is about to refine it by default. For choosing between the concepts afterwards, use `concept-selection`.
- **`prototype-strategy`** — Choose prototype fidelity and method to match the design question and the decision at stake. Use before building a prototype. For what to test once it exists, use `test-scenario`.
- **`test-scenario`** — Write realistic usability task scenarios with success criteria and facilitation notes. Use when you have a study and need the tasks. For the surrounding study design, use `usability-test-plan` (design-research).
- **`user-flow-diagram`** — Diagram screen-level paths, decision points, and branch logic. Use when specifying how a feature is traversed. For the emotional end-to-end arc, use `journey-map` (design-research).
- **`wireframe-spec`** — Specify wireframe layout — content priority, component placement, and annotation. Use when defining structure before visual design. For grid mechanics, use `layout-grid` (ui-design).

Commands: `/prototyping-testing:evaluate`, `/prototyping-testing:experiment`, `/prototyping-testing:explore-options`, `/prototyping-testing:prototype-plan`, `/prototyping-testing:test-plan`

### `design-ops` — 9 skills, 3 commands

Design operations skills: handoff specs, design critique facilitation, design sprint planning, team workflows, QA checklists, and design debt audits.

- **`design-critique`** — Facilitate a structured team critique — framing, feedback rules, and actionable outcomes. Use when running a session with people in the room. For a solo expert review, use `heuristic-evaluation` (prototyping-testing).
- **`design-debt-audit`** — Inventory and prioritise accumulated design inconsistencies across a product. Use when drift has built up over time. For token coverage specifically use `design-token-audit` (designer-toolkit); for WCAG gaps use `accessibility-audit` (design-systems).
- **`design-impact-reporting`** — Communicate design's contribution to business and user outcomes in stakeholder language. Use when reporting results upward. For choosing the metrics in the first place, use `metrics-definition` (ux-strategy).
- **`design-qa-checklist`** — Build a QA checklist for verifying that a build matches the design. Use at implementation review. For the spec engineers build from, use `handoff-spec`.
- **`design-review-process`** — Establish review gates — criteria, checkpoints, and approval flow. Use when work ships without consistent review. For running one individual session, use `design-critique`.
- **`design-sprint-plan`** — Plan and facilitate a design sprint from challenge framing through prototype testing. Use when compressing discovery into days. For ongoing team cadence, use `team-workflow`.
- **`handoff-spec`** — Write the implementation handoff — measurements, behaviours, assets, states, and edge cases. Use when engineering picks up the work. For verifying the result afterwards use `design-qa-checklist`; for reusable library components use `component-spec` (design-systems).
- **`team-workflow`** — Design the team's operating rhythm — task management, collaboration rituals, and tooling. Use when the day-to-day cadence needs structure. For a time-boxed sprint, use `design-sprint-plan`.
- **`version-control-strategy`** — Define version control for design files, components, and libraries — branching, naming, and release. Use when file history is chaotic. For design system contribution rules, use `design-system-governance` (design-systems).

Commands: `/design-ops:handoff`, `/design-ops:plan-sprint`, `/design-ops:setup-workflow`

### `designer-toolkit` — 7 skills, 4 commands

Designer utility skills: design rationale, case study structure, design presentations, UX writing, design system adoption, and design negotiation — plus the /start-here router.

- **`case-study`** — Craft a portfolio case study with narrative arc, process evidence, and outcomes. Use when telling a project's story to an external audience. For an internal stakeholder deck, use `presentation-deck`.
- **`design-negotiation`** — Advocate for design quality, scope, and timeline with partners and leadership using evidence and shared goals. Use in the conversation itself. For the commercial vocabulary behind it, use `business-design` (ux-strategy).
- **`design-rationale`** — Write rationale connecting decisions to user needs, business goals, and principles. Use when a decision needs defending in writing. For a live conversation, use `design-negotiation`.
- **`design-system-adoption`** — Create adoption strategy and enablement materials to drive design system usage. Use when the system exists but teams ignore it. For contribution and versioning rules, use `design-system-governance` (design-systems).
- **`design-token-audit`** — Audit token usage across a product for coverage, drift, and hard-coded values. Use when tokens exist and you suspect they are being bypassed. For defining tokens in the first place, use `design-token` (design-systems).
- **`presentation-deck`** — Structure a design presentation for a specific audience and decision. Use when presenting internally. For a portfolio narrative use `case-study`; for the written argument use `design-rationale`.
- **`ux-writing`** — Write interface copy — microcopy, error messages, empty states, and CTAs. Use when the words are the deliverable. For content structure and ownership, use `content-strategy` (ux-strategy).

Commands: `/designer-toolkit:build-presentation`, `/designer-toolkit:start-here`, `/designer-toolkit:write-case-study`, `/designer-toolkit:write-rationale`

### `visual-critique` — 7 skills, 2 commands

Visual critique skills: hierarchy analysis, brand consistency checks against mood/voice/tokens, composition evaluation, and typography audits — with a /critique-screen command that compiles a prioritised fix list.

- **`critique-affordance`** — Critique a rendered screen's affordances — what looks clickable, state visibility, CTA clarity, and action discoverability. Use when reviewing an existing screen. For sizing and positioning targets in new work, use `fitts-law` (interaction-design).
- **`critique-brand-consistency`** — Critique a rendered screen against mood.md, voice.md, and tokens.md. Use when those brand files exist and you are checking compliance. For defining the visual language itself, use `illustration-style` (ui-design).
- **`critique-color`** — Critique a rendered screen's colour — contrast ratios, palette coherence, and semantic meaning. Use when reviewing one screen. For a product-wide WCAG audit use `accessibility-audit` (design-systems); for building the palette use `color-system` (ui-design).
- **`critique-composition`** — Critique a rendered screen's composition — balance, whitespace, rhythm, and gestalt grouping. Use when a layout feels off but hierarchy is fine. For emphasis and eye flow specifically, use `critique-visual-hierarchy`.
- **`critique-information-density`** — Critique a rendered screen's density — cognitive load, content prioritisation, scanning patterns, and progressive disclosure. Use when a screen feels overwhelming. For the underlying choice-count principle, use `hicks-law` (interaction-design).
- **`critique-typography`** — Critique a rendered screen's typography — scale usage, readability, consistency, and token compliance. Use when reviewing type on a screen. For defining the scale itself, use `typography-scale` (ui-design).
- **`critique-visual-hierarchy`** — Critique a rendered screen's hierarchy — entry point, eye flow, weight distribution, and emphasis. Use when attention lands in the wrong place. For establishing hierarchy in new work, use `visual-hierarchy` (ui-design).

Commands: `/visual-critique:critique-screen`, `/visual-critique:critique-ux`

## Our own skill: `reel-card-composition`

Added to this repo by the coding agent (Codebuff/Buffy) on 2026-10-06, written in the
pack's format so it can be lifted straight into `interaction-design` upstream as a PR.

```markdown
---
name: reel-card-composition
description: Compose a feed card that teaches one idea and grades it in place — difficulty cue, hook, body, optional code, and the question, in a single non-scrolling column. Use when a swipe card must carry lesson and assessment together. For the motion between cards, use `micro-interaction-spec`; for waits inside a card, use `loading-states`.
---
# Reel Card Composition

You are an expert in single-screen, swipe-native teaching cards — the format where
one idea is delivered and tested before the user's thumb can move on.

## What You Do

You compose one card that carries a complete learning unit: a difficulty cue, a
hook, a short body, optional code, and the question that grades comprehension —
in a fixed vertical order, with no inner scrolling and no second screen. You
decide what gets cut so the card fits a phone viewport, and you decide how the
card signals track and difficulty without spending header chrome. You do not
design the motion between cards (that is `micro-interaction-spec`) and you do not
choose the learning sequence itself.

## The Card Constitution

One idea, one gesture, zero unearned chrome. Every element must justify its
height. The order below is load-bearing: difficulty first calibrates attention,
the hook buys the next two seconds, the body pays it off, code shows the claim in
the wild, and the question converts reading into retrieval.

| Slot | Carries | Fails when |
| --- | --- | --- |
| Difficulty cue | Expected effort, so the reader can consent to it | It is a label with no motion, or motion too subtle to read peripherally |
| Hook | One line that names a concrete surprise or stake | It summarises the topic instead of provoking it |
| Body | Under ~70 words of prose; the takeaway is the closing line, not a box | It teaches two ideas, or repeats the hook |
| Code | One runnable-in-principle snippet, ≤15 lines, fixed mono size | It is pseudocode, or it needs a paragraph of setup |
| Question | Exactly the retrieval the body enables | It tests recall of a detail the body never stated |

### What gets cut, in order

When the card overflows, cut in this order: redundant header chrome (track pill,
level badge, read-time) → decorative containers (callout boxes, separators,
cards inside cards) → secondary actions inside the content column → prose. Never
cut the question. A card without its assessment is a slide, and slides are not
retrievable.

## Signals Without Chrome

Track identity and difficulty are the two facts a reader needs before committing
to the card, and both can be carried without a single label:

- **Track** — a low-alpha wash of the container fill (a ~14% blend keeps text
  readable in both light and dark). Colour carries identity; text carries the
  lesson.
- **Difficulty** — one small centred cue whose *motion* encodes the tier: slow
  breath for easy, a shimmer sweep for medium, a fast ember flicker for hard.
  Motion is read peripherally in a way a label is not. Announce the same value to
  screen readers so the encoding is never colour- or motion-only.

## Assessment Inline

The question belongs inside the card, directly under the material it tests. This
is the structural guarantee that the reader's answer is evidence about *this*
idea, and it removes the nested-navigation bug where "next" means two different
things on two different faces of a page.

- Grade on the answer alone. Remove the redundant "got it / still fuzzy"
  self-report buttons: self-report is a feeling, an answer is a measurement.
- Reward the answer with XP, streak, and scheduling updates — the same event, one
  write path.
- Keep the rail (like, save, share) outside the content column so it never
  competes with the question for the thumb.
- Enforce ≥48dp targets on that rail; a 28dp glyph inside a 48dp hit area is the
  difference between a save and a scroll.

## Best Practices

- Compose at the smallest supported viewport first. If the card needs an inner
  scroll, the card is doing too much — cut, don't scroll.
- Put the answer-checking affordance where the reading ends, not below the fold of
  a nested scroller.
- Show the expected result only when it exists: an explicit "no output authored"
  state is a hidden button, not an empty panel.
- Verify with a real screenshot at both themes before shipping; a wash that works
  on a dark surface frequently fails on paper.
- Do not stack two teaching units in one card to save a swipe. The swipe is the
  cheap part; the confusion is not.
```

## Licence and attribution

The catalogue above is reproduced from the Designer Skills Pack, MIT licensed.
Copyright and permission notice, as required by that licence:

```
MIT License

Copyright (c) 2026 MC Dean

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

`reel-card-composition` is this repo's own work and carries no third-party claim.

### Keeping this current

Re-sync by diffing the pinned commit against `main` upstream and re-extracting the
`description` frontmatter (skills live at `<plugin>/skills/<skill-name>/SKILL.md`).
Update the pinned SHA row when you do, so a reader can tell how stale this is.
