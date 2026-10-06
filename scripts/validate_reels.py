#!/usr/bin/env python3
"""Validate FerrisFeed reels under content/.

Rules (TODO items 9 + content-schema.md):
  - hook: exactly 1 line, 10..140 chars
  - body_md: <= 70 words, non-empty
  - code: <= 15 lines (when present)
  - takeaway: 1 sentence-ish (1-2 terminators, <= 30 words)
  - trap: present, non-empty
  - quiz: valid per type (mcq / tap_bug / fill_blank, legacy aliases accepted)

Walks content/**/*.json, excluding content/schema.json itself.
Prints per-file errors and a summary. Exits 0 on success (or 0 files),
non-zero on any validation failure.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT_DIR = ROOT / "content"
SCHEMA_FILE = CONTENT_DIR / "schema.json"

VALID_TRACKS = {"rust", "wasm", "system-design"}
VALID_QUIZ_TYPES = {"mcq", "tap_bug", "fill_blank"}
ID_RE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")

# Non-fatal notices (e.g. missing language defaults to rust). Printed but do not fail.
WARNINGS: list[str] = []


def words(s: str) -> list[str]:
    return [w for w in re.split(r"\s+", s.strip()) if w]


def sentence_terminators(s: str) -> int:
    # Count sentence-ending punctuation clusters.
    return len(re.findall(r"[.!?]+", s.strip()))


def quiz_question(q: dict) -> str:
    return str(q.get("question", q.get("q", "")))


def quiz_explanation(q: dict) -> str:
    return str(q.get("explanation", q.get("explain", "")))


def validate_reel(data: object, path: Path) -> list[str]:
    errors: list[str] = []
    if not isinstance(data, dict):
        return [f"reel: must be a JSON object, got {type(data).__name__}"]

    # --- id ---
    rid = data.get("id", "")
    if not isinstance(rid, str) or not ID_RE.match(rid):
        errors.append(f"id: must be kebab-case [a-z0-9-]+, got {rid!r}")

    # --- track ---
    if data.get("track") not in VALID_TRACKS:
        errors.append(f"track: must be one of {sorted(VALID_TRACKS)}, got {data.get('track')!r}")

    # --- level ---
    level = data.get("level")
    if not isinstance(level, int) or not (1 <= level <= 4):
        errors.append(f"level: must be int 1..4, got {level!r}")

    # --- hook: 1 line ---
    hook = data.get("hook", "")
    if not isinstance(hook, str) or not hook.strip():
        errors.append("hook: missing or empty")
    else:
        if "\n" in hook.strip():
            errors.append("hook: must be exactly 1 line (no newlines)")
        if not (10 <= len(hook.strip()) <= 140):
            errors.append(f"hook: must be 10..140 chars, got {len(hook.strip())}")

    # --- body: <= 70 words ---
    body = data.get("body_md", "")
    if not isinstance(body, str) or not body.strip():
        errors.append("body_md: missing or empty")
    else:
        n = len(words(body))
        if n > 70:
            errors.append(f"body_md: must be <= 70 words, got {n}")

    # --- code: <= 15 lines ---
    code = data.get("code")
    if code is not None:
        if not isinstance(code, str):
            errors.append("code: must be a string or null")
        else:
            lines = code.splitlines()
            if len(lines) > 15:
                errors.append(f"code: must be <= 15 lines, got {len(lines)}")
            if code.strip() and not data.get("language"):
                # Warning only: TODO's minimal schema omits language; UI defaults to rust.
                WARNINGS.append(f"[{data.get('id', '?')}] language: missing with code present (defaults to rust)")

    # --- output: <= 15 lines, string or null (flip hidden when null) ---
    output = data.get("output", None)
    if output is not None:
        if not isinstance(output, str):
            errors.append("output: must be a string or null")
        elif len(output.splitlines()) > 15:
            errors.append(f"output: must be <= 15 lines, got {len(output.splitlines())}")

    # --- takeaway: 1 sentence-ish ---
    takeaway = data.get("takeaway", "")
    if not isinstance(takeaway, str) or not takeaway.strip():
        errors.append("takeaway: missing or empty")
    else:
        w = len(words(takeaway))
        if w > 30:
            errors.append(f"takeaway: must be <= 30 words (1 sentence-ish), got {w}")
        terms = sentence_terminators(takeaway)
        if terms == 0:
            errors.append("takeaway: must read as a sentence (missing . ! or ?)")
        elif terms > 2:
            errors.append(f"takeaway: must be 1 sentence-ish (<= 2 terminators), got {terms}")

    # --- trap: present ---
    trap = data.get("trap", "")
    if not isinstance(trap, str) or not trap.strip():
        errors.append("trap: missing or empty (one common mistake required)")

    # --- quiz ---
    quiz = data.get("quiz")
    if not isinstance(quiz, dict):
        errors.append("quiz: missing object")
        return errors

    qtext = quiz_question(quiz)
    if not qtext.strip():
        errors.append("quiz.question (or q): missing or empty")
    exp = quiz_explanation(quiz)
    if not exp.strip():
        errors.append("quiz.explanation (or explain): missing or empty")

    qtype = str(quiz.get("type", "mcq"))
    if qtype not in VALID_QUIZ_TYPES:
        errors.append(f"quiz.type: must be one of {sorted(VALID_QUIZ_TYPES)}, got {qtype!r}")
        return errors

    if qtype == "mcq":
        options = quiz.get("options", [])
        answer = quiz.get("answer", quiz.get("answerIndex", None))
        if not isinstance(options, list) or len(options) < 2:
            errors.append("quiz.options: mcq needs >= 2 options")
        if not isinstance(answer, int):
            errors.append("quiz.answer (or answerIndex): mcq needs integer index")
        elif isinstance(options, list) and options and not (0 <= answer < len(options)):
            errors.append(f"quiz.answer: index {answer} out of range for {len(options)} options")
    elif qtype == "tap_bug":
        lines = quiz.get("lines", quiz.get("codeLines", []))
        buggy = quiz.get("buggyLineIndex", quiz.get("buggy", None))
        if not isinstance(lines, list) or len(lines) < 2:
            errors.append("quiz.lines (or codeLines): tap_bug needs >= 2 lines")
        if not isinstance(buggy, int):
            errors.append("quiz.buggyLineIndex (or buggy): tap_bug needs integer index")
        elif isinstance(lines, list) and lines and not (0 <= buggy < len(lines)):
            errors.append(f"quiz.buggyLineIndex: index {buggy} out of range for {len(lines)} lines")
    elif qtype == "fill_blank":
        accepted = quiz.get("acceptedAnswers", quiz.get("answers", []))
        if not isinstance(accepted, list) or not accepted or not all(
            isinstance(a, str) and a.strip() for a in accepted
        ):
            errors.append("quiz.acceptedAnswers (or answers): fill_blank needs >= 1 non-empty answer")

    return errors


def collect_reel_files() -> list[Path]:
    if not CONTENT_DIR.exists():
        return []
    files = sorted(CONTENT_DIR.rglob("*.json"))
    # Exclude the JSON Schema itself — it is not a reel.
    return [f for f in files if f.resolve() != SCHEMA_FILE.resolve()]


def main() -> int:
    files = collect_reel_files()
    if not files:
        print("validate_reels: 0 reel files found under content/ — nothing to validate (OK).")
        print("  (Content agents have not landed reels yet; schema.json is excluded.)")
        return 0

    failed = 0
    passed = 0
    reel_total = 0
    for path in files:
        rel = path.relative_to(ROOT)
        try:
            raw_data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            print(f"FAIL {rel}: invalid JSON: {e}")
            failed += 1
            continue
        except OSError as e:
            print(f"FAIL {rel}: cannot read: {e}")
            failed += 1
            continue
        # A file may hold one reel object or an array of reels.
        if isinstance(raw_data, dict):
            reels: list[object] = [raw_data]
        elif isinstance(raw_data, list):
            reels = raw_data
            if not reels:
                print(f"FAIL {rel}: empty array (no reels)")
                failed += 1
                continue
        else:
            print(f"FAIL {rel}: top-level must be an object or array of objects")
            failed += 1
            continue
        file_errors: list[str] = []
        for idx, reel in enumerate(reels):
            prefix = f"[{idx}]" if len(reels) > 1 else ""
            reel_total += 1
            for e in validate_reel(reel, path):
                rid = reel.get("id", f"index {idx}") if isinstance(reel, dict) else f"index {idx}"
                file_errors.append(f"{prefix}[{rid}] {e}")
        if file_errors:
            print(f"FAIL {rel} ({len(reels)} reels):")
            for e in file_errors:
                print(f"  - {e}")
            failed += 1
        else:
            passed += 1

    total = passed + failed
    print(f"\nvalidate_reels: {passed}/{total} files passed, {failed} failed ({reel_total} reels checked).")
    if WARNINGS:
        print(f"warnings ({len(WARNINGS)}, non-fatal):")
        for w in WARNINGS[:20]:
            print(f"  ! {w}")
        if len(WARNINGS) > 20:
            print(f"  ! ... and {len(WARNINGS) - 20} more")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
