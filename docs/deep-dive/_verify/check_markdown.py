#!/usr/bin/env python3
"""Preflight for Feishu markdown import: find text the importer will mangle.

Feishu's markdown importer honours single-slash emphasis, so a bare path list
like `base/network/router` outside a code span renders as italic run-on text
(this is what broke wiki page 03). Same risk from bare `_x_` emphasis.
"""
import glob
import os
import re
import sys

DOC_DIR = "/Users/qpy/Desktop/github/my_kmp_project/docs/deep-dive"
FENCE = re.compile(r"^\s*(```|~~~)")
CODE = re.compile(r"(`+)(?:(?!`).|\n)*?\1")
# italic pair: /.../ with no space inside, not preceded by a word char (avoid http://)
SLASH_EMP = re.compile(r"(?<![^\s|,;:()\[`=])/(?=\S)([^|\n`]{1,120}?)/(?![\w/])")
UNDER_EMP = re.compile(r"(?<![^\s|,;:()\[`=])_(?=\S)([^|\n`_]{1,80}?)_(?![\w])")


def scan(path):
    hits = []
    fenced = False
    with open(path, encoding="utf-8") as fh:
        for no, line in enumerate(fh, 1):
            if FENCE.match(line):
                fenced = not fenced
                continue
            if fenced:
                continue
            if line.lstrip().startswith("|"):
                for span in CODE.findall(line):
                    if re.search(r"(?<!\\)\|", span):
                        hits.append((no, "pipe-in-table-cell", span[:60]))
            plain = CODE.sub(" ", line)
            for rx, kind in ((SLASH_EMP, "slash-italic"), (UNDER_EMP, "underscore-italic")):
                for m in rx.finditer(plain):
                    hits.append((no, kind, m.group(0)[:60]))
    return hits


def main():
    files = sorted(glob.glob(os.path.join(DOC_DIR, "*.md")))
    bad = 0
    for f in files:
        hits = scan(f)
        if not hits:
            continue
        bad += 1
        print(f"{os.path.basename(f)}  ({len(hits)} hits)")
        for no, kind, txt in hits[:8]:
            print(f"    L{no} {kind}: {txt!r}")
    print(f"\nsummary: {bad}/{len(files)} files need quoting fixes")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
