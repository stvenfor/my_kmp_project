#!/usr/bin/env python3
"""Audit parity-flutter-to-kmp/tasks-execution.md for unfinished in-scope items.

Exit 0 only when no unchecked `[ ]` / partial `[~]` remain on non-n/a lines
that are not explicitly platform-missing notes. Used by gate Z.1.1.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TASKS = ROOT / "openspec/changes/parity-flutter-to-kmp/tasks-execution.md"


def main() -> int:
    if not TASKS.is_file():
        print(f"MISSING {TASKS}", file=sys.stderr)
        return 2
    text = TASKS.read_text(encoding="utf-8")
    open_items: list[str] = []
    partial: list[str] = []
    done = na = 0
    for line in text.splitlines():
        s = line.strip()
        if not s.startswith("- ["):
            continue
        if s.startswith("- [n/a]"):
            na += 1
            continue
        if s.startswith("- [x]"):
            done += 1
            continue
        # platform missing allowed only if line says missing/registry
        if s.startswith("- [ ]") or s.startswith("- [~]"):
            body = s[6:].strip() if s.startswith("- [ ]") else s[6:].strip()
            if "missing" in body.lower() and ("registry" in body.lower() or "gap" in body.lower()):
                # still counts as open until explicitly [n/a] or done with registry row
                pass
            if s.startswith("- [~]"):
                partial.append(s)
            else:
                open_items.append(s)

    print(f"file: {TASKS.relative_to(ROOT)}")
    print(f"[x]={done}  [n/a]={na}  [ ]={len(open_items)}  [~]={len(partial)}")
    if open_items or partial:
        print("\nUNFINISHED (first 40):")
        for row in (open_items + partial)[:40]:
            print(" ", row[:120])
        if len(open_items) + len(partial) > 40:
            print(f"  ... +{len(open_items) + len(partial) - 40} more")
        print("\nGATE Z.1.1 FAIL — continue implementation; do not archive.")
        return 1
    print("\nGATE Z.1.1 PASS — no open/partial items.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
