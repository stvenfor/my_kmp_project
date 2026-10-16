#!/usr/bin/env bash
# Collect per-class unit-test evidence from the Gradle XMLs produced by the LAST
# unfiltered run (scripts/verify-parity.sh). Filtered `--tests` runs wipe this
# directory, so evidence is only trustworthy straight after a full run.
#
# Usage: scripts/parity-evidence.sh [output.md]
set -uo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root" || exit 1

out="${1:-}"
python3 - <<'PY' ${out:+$out}
import datetime, pathlib, sys, xml.etree.ElementTree as ET

dirs = ["composeApp/build/test-results/testDebugUnitTest",
        "core/network/build/test-results/testDebugUnitTest"]
rows = []
for d in dirs:
    base = pathlib.Path(d)
    if not base.is_dir():
        continue
    for p in sorted(base.glob("TEST-*.xml")):
        try:
            a = ET.parse(p).getroot().attrib
        except ET.ParseError:
            rows.append((p.stem, -1, -1, -1, "UNPARSEABLE"))
            continue
        rows.append((a["name"], int(a["tests"]), int(a["failures"]),
                     int(a["errors"]), int(a.get("skipped", 0))))

if not rows:
    print("no test-results found — run `bash scripts/verify-parity.sh` first", file=sys.stderr)
    sys.exit(1)

t = sum(r[1] for r in rows)
f = sum(r[2] for r in rows)
e = sum(r[3] for r in rows)
s = sum(r[4] for r in rows if isinstance(r[4], int))
lines = ["# 单测证据（per-class）",
         "",
         f"采集时间：{datetime.date.today()}；来源 = 最近一次未过滤的 testDebugUnitTest 运行。",
         "",
         f"合计：**{t} tests, {f} failures, {e} errors, {s} skipped**，{len(rows)} 个测试类。",
         "",
         "| 测试类 | tests | failures | errors | skipped |",
         "|---|---|---|---|---|"]
for name, tt, ff, ee, sk in rows:
    flag = " ⚠" if (ff or ee) else ""
    lines.append(f"| `{name}`{flag} | {tt} | {ff} | {ee} | {sk} |")
text = "\n".join(lines) + "\n"
dest = sys.argv[1] if len(sys.argv) > 1 else None
if dest:
    pathlib.Path(dest).parent.mkdir(parents=True, exist_ok=True)
    pathlib.Path(dest).write_text(text, encoding="utf-8")
    print(f"wrote {dest}")
print(text)
sys.exit(0 if (f == 0 and e == 0) else 1)
PY
