#!/usr/bin/env python3
"""Audit the single execution ledger for unfinished in-scope items.

The sole execution ledger is sync-flutter-full-parity/tasks-ultra-fine-v2.md.
The old tasks-execution.md / tasks-fine.md / tasks-ultra-fine.md are historical
ledgers kept only for gap-checking; reading them here was the false green (585
`[x]` / 0 open) that hid the real v2 backlog.

Exit 0 only when the chosen ledger has no open `[ ]` / partial `[~]` items that
are not explicitly `[n/a]`. Real remaining > 0 -> exit 1. Used by gate Z.1.1.
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# 唯一执行账本（single source of truth）. Historical ledgers are NOT read by default.
MAIN_LEDGER = ROOT / "openspec/changes/sync-flutter-full-parity/tasks-ultra-fine-v2.md"


def audit(path: Path) -> int:
    if not path.is_file():
        print(f"MISSING {path}", file=sys.stderr)
        return 2
    text = path.read_text(encoding="utf-8")
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
        if s.startswith("- [ ]"):
            open_items.append(s)
        elif s.startswith("- [~]"):
            partial.append(s)

    remaining = len(open_items) + len(partial)
    try:
        shown = path.relative_to(ROOT)
    except ValueError:
        shown = path
    scope = "the sole execution ledger" if path == MAIN_LEDGER else f"inspection file {shown}"
    print(f"file: {shown}")
    print(f"[x]={done}  [n/a]={na}  [ ]={len(open_items)}  [~]={len(partial)}")
    if remaining > 0:
        print("\nUNFINISHED (first 40):")
        for row in (open_items + partial)[:40]:
            print(" ", row[:120])
        if remaining > 40:
            print(f"  ... +{remaining - 40} more")
        print(
            f"\nGATE Z.1.1 FAIL — {remaining} real remaining item(s) in {scope}; "
            "continue implementation; do not archive."
        )
        return 1
    print(f"\nGATE Z.1.1 PASS — no open/partial items in {scope}.")
    return 0


def main() -> int:
    # Default: the sole execution ledger (MAIN_LEDGER). An optional positional
    # argument lets a human inspect a historical ledger deliberately; it must
    # never be used to make the gate pass.
    path = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else MAIN_LEDGER
    return audit(path)


if __name__ == "__main__":
    sys.exit(main())
