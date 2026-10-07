"""Backfill `topic_label` onto every non-WASM reel (TODO item 21).

Idempotent: reels that already carry a label keep it unless --force. Label
groups are nested by directory then file, so adding a pack first means adding
it to LABELS below — no auto-derived accidental labels.

Formatting guarantee: the repo deliberately carries several JSON styles
(pretty indent-2 / single-line compact, ASCII-escaped or literal unicode,
sorted or authored key order). For each pack we pick the dump settings whose
label-free re-serialization is byte-identical to the original and refuse to
write anything when none matches — the diff then shows exactly one inserted
`topic_label` key per reel and nothing else.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "content"

# Human topic labels (TODO 21): per-directory, per-file. Truthful over pretty:
# grab-bag `_extra` packs say so instead of inventing a theme.
LABELS: dict[str, dict[str, str]] = {
    "rust": {
        "advanced1_unsafe.json": "unsafe",
        "advanced2_async.json": "async",
        "advanced3_types.json": "traits",
        "advanced4_perf.json": "performance",
        "advanced5_extra.json": "advanced misc",
        "beginner1_toolchain.json": "toolchain",
        "beginner2_ownership.json": "ownership",
        "beginner3_collections.json": "collections",
        "beginner4_extra.json": "crates & tooling",
        "intermediate1_generics.json": "generics",
        "intermediate2_concurrency.json": "threads",
        "intermediate3_macros.json": "macros",
        "intermediate4_extra.json": "intermediate misc",
        "rust_mixed_drills.json": "drills",
    },
    "system-design": {
        "sd1_beginner.json": "fundamentals",
        "sd2_intermediate.json": "concepts",
        "sd3_advanced.json": "distributed",
        "sd4_rust.json": "rust backend",
        "sd5_cases.json": "app cases",
        "sd6_data.json": "data systems",
        "sd7_scale.json": "scaling",
        "sd8_rust_cases.json": "rust cases",
    },
}

SKIP_DIRS = {"wasm"}  # dormant: WASM reels are not seeded, not labelled


def serializer_variants(original: str) -> list[dict]:
    """Candidate dump settings matching the styles carried in this repo.

    Pretty packs end without a trailing newline; single-line ones end with
    one. Try every combination and let the byte-identity probe decide.
    """
    pretty = original.startswith("[\n")
    variants: list[dict] = []
    for ensure_ascii in (True, False):
        for sort_keys in (True, False):
            if pretty:
                variants.append({"ensure_ascii": ensure_ascii, "indent": 2, "sort_keys": sort_keys})
            else:
                variants.append({"ensure_ascii": ensure_ascii, "indent": None, "sort_keys": sort_keys})
                variants.append({
                    "ensure_ascii": ensure_ascii, "indent": None,
                    "sort_keys": sort_keys, "no_space": True,
                })
    return variants


def dumps(settings: dict, obj: object) -> str:
    """Bare serialization; trailing-newline handling is separate."""
    if settings["indent"] is None:
        seps = (",", ":") if settings.get("no_space") else (", ", ": ")
        return json.dumps(
            obj, ensure_ascii=settings["ensure_ascii"],
            separators=seps, sort_keys=settings["sort_keys"],
        )
    return json.dumps(
        obj, ensure_ascii=settings["ensure_ascii"],
        indent=settings["indent"], sort_keys=settings["sort_keys"],
    )


def main(force: bool = False) -> int:
    problems: list[str] = []
    labelled = 0
    files = sorted(
        p for p in CONTENT.rglob("*.json")
        if p.name != "schema.json" and p.parent.name not in SKIP_DIRS
    )
    for path in files:
        rel_dir = path.parent.name
        mapping = LABELS.get(rel_dir, {}).get(path.name)
        if mapping is None:
            problems.append(f"{path}: no label mapping for this pack — add it to LABELS")
            continue

        original = path.read_text(encoding="utf-8")
        reels = json.loads(original)
        if not isinstance(reels, list) or not reels:
            problems.append(f"{path}: expected a non-empty reel array")
            continue

        changed = 0
        for reel in reels:
            if "topic_label" in reel and not force:
                continue
            reel["topic_label"] = mapping
            labelled += 1
            changed += 1
        if changed == 0:
            continue

        # Guard: find dump settings that reproduce the file WITHOUT labels
        # byte-for-byte (allowing for the file's own trailing newline);
        # refuse to write when none does.
        probe = json.loads(original)
        for reel in probe:
            reel.pop("topic_label", None)
        ends_with_nl = original.endswith("\n")
        chosen: dict | None = None
        for settings in serializer_variants(original):
            candidate = dumps(settings, probe)
            if candidate == original or (ends_with_nl and candidate + "\n" == original):
                chosen = settings
                break
        if chosen is None:
            problems.append(f"{path}: re-dump would rewrite formatting — FIX before writing")
            continue

        path.write_text(dumps(chosen, reels) + ("\n" if ends_with_nl else ""), encoding="utf-8")
        print(f"ok {rel_dir}/{path.name}: {changed} label(s) written")

    if problems:
        print("\nFAILURES — nothing else was touched:")
        for p in problems:
            print(f"  - {p}")
        return 1
    print(f"\nbackfilled topic_label on {labelled} reels across {len(files)} packs (wasm untouched)")
    return 0


if __name__ == "__main__":
    sys.exit(main(force="--force" in sys.argv[1:]))
