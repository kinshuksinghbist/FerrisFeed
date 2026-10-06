# FerrisFeed Content Schema — Reel JSON Spec

Every swipe in the feed is one **reel**: a 60-second flashcard with runnable code
and one quiz. This document is the contract between content authors (human or
agent), `scripts/validate_reels.py`, `content/schema.json`, and the Android
renderers (`ReelCard`, `CodeCard`, `QuizCard`, `TrapCard`).

## Reel object

| Field | Type | Required | Rule |
|---|---|---|---|
| `id` | string | yes | Kebab-case, unique. e.g. `rust-own-014`. |
| `track` | string | yes | One of `rust`, `wasm`, `system-design`. |
| `level` | int | yes | 1 (beginner) to 4 (advanced). |
| `hook` | string | yes | **Exactly 1 line**, 10–140 chars. Curiosity gap, not a summary. |
| `body_md` | string | yes | Markdown-lite explainer, **≤ 70 words**. Plain text + `code` spans only. |
| `code` | string \| null | no | Runnable snippet, **≤ 15 lines**. Omit or null when the reel needs no code. |
| `language` | string | when `code` present | e.g. `rust`, `toml`, `sql`. |
| `takeaway` | string | yes | **1 sentence-ish**: ≤ 30 words, 1–2 sentence terminators. The rule to remember. |
| `trap` | string | yes | The single most common mistake or compiler error. Never empty. |
| `trap_compiler_message` | string \| null | no | Verbatim `rustc` / tool error shown in the expanded trap card. |
| `tags` | string[] | no | Free-form, e.g. `["ownership", "borrowck"]`. Used by search. |
| `path_order` | int | no | Position in path order for the 20% fresh-card slice. Defaults to 0. |
| `quiz` | object | yes | Exactly one question, see below. |

## Quiz object

`quiz.type` is one of `mcq`, `tap_bug`, `fill_blank` (defaults to `mcq`).
Legacy aliases are accepted by the validator and the app: `q` → `question`,
`explain` → `explanation`, `answerIndex` → `answer`, `codeLines` → `lines`,
`buggy` → `buggyLineIndex`, `answers` → `acceptedAnswers`.

- Common: `question` (non-empty), `explanation` (non-empty, shown after answering).
- `mcq`: `options` (2–5 strings), `answer` (valid index into `options`).
- `tap_bug`: `lines` (≥ 2 code lines), `buggyLineIndex` (valid index into `lines`).
- `fill_blank`: `prefix`, `suffix` (either may be empty), `acceptedAnswers` (≥ 1
  non-empty string, matched case-insensitively).

## Example — MCQ

```json
{
  "id": "rust-own-014",
  "track": "rust",
  "level": 1,
  "hook": "Why does this simple function not compile?",
  "body_md": "Ownership moves values by default. Passing a String to a function moves it, so the caller cannot use it after. Borrow with & to keep ownership.",
  "code": "fn takes(s: String) {}\nfn main() {\n    let s = String::from(\"hi\");\n    takes(s);\n}",
  "language": "rust",
  "takeaway": "Move by default; borrow with & to keep ownership.",
  "trap": "Using s after takes(s) — the value was moved.",
  "trap_compiler_message": "error[E0382]: borrow of moved value: `s`",
  "tags": ["ownership", "moves"],
  "path_order": 14,
  "quiz": {
    "type": "mcq",
    "question": "What happens when s is passed to takes(s)?",
    "options": ["s is moved", "s is copied", "s is borrowed", "Nothing"],
    "answer": 0,
    "explanation": "String is not Copy, so by-value passing moves ownership."
  }
}
```

## Example — Tap-the-bug

```json
{
  "id": "rust-borrow-021",
  "track": "rust",
  "level": 1,
  "hook": "Two mutable borrows — which line does rustc reject?",
  "body_md": "You may have many shared borrows or one mutable borrow, never both at once. The second &mut while the first is live is rejected.",
  "code": "let mut v = vec![1, 2];\nlet a = &mut v;\nlet b = &mut v;",
  "language": "rust",
  "takeaway": "One mutable borrow at a time.",
  "trap": "Holding two &mut to the same value alive simultaneously.",
  "quiz": {
    "type": "tap_bug",
    "question": "Tap the line that fails to compile.",
    "lines": ["let mut v = vec![1, 2];", "let a = &mut v;", "let b = &mut v;"],
    "buggyLineIndex": 2,
    "explanation": "Second mutable borrow while `a` is still live."
  }
}
```

## Example — Fill-blank

```json
{
  "id": "rust-vec-009",
  "track": "rust",
  "level": 1,
  "hook": "What single operator borrows without moving?",
  "body_md": "Prefix a place expression with & to borrow it. The owner keeps the value and you get a shared reference.",
  "takeaway": "Use & to borrow a value you do not own.",
  "trap": "Forgetting & and accidentally moving the value.",
  "quiz": {
    "type": "fill_blank",
    "question": "Fill the blank to borrow s.",
    "prefix": "takes(",
    "suffix": "s);",
    "acceptedAnswers": ["&s", "& s"],
    "explanation": "&s creates a shared borrow; takes(&s) leaves s usable."
  }
}
```

## Validation

Run `python3 scripts/validate_reels.py` from the repo root. It walks
`content/**/*.json` (excluding `content/schema.json`), enforces the word/line/
sentence budgets above, and exits non-zero on any failure. `content/schema.json`
additionally gives editors structural completion, but the word budgets live in the
script because JSON Schema cannot count words.
