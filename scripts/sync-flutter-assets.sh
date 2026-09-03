#!/usr/bin/env bash
# Sync Flutter product PNG/WebP/JPG into Compose drawable resources.
# Usage: ./scripts/sync-flutter-assets.sh
# Override SoT: FLUTTER_ROOT=/path/to/my_ai_project ./scripts/sync-flutter-assets.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FLUTTER_ROOT="${FLUTTER_ROOT:-/Users/mac/Desktop/github/my_ai_project}"
export FLUTTER_ROOT ROOT
python3 <<'PY'
import os, re, shutil, hashlib
from pathlib import Path

FL = Path(os.environ["FLUTTER_ROOT"])
ROOT = Path(os.environ["ROOT"])
DEST = ROOT / "composeApp/src/commonMain/composeResources/drawable"
MAP = ROOT / "openspec/changes/sync-flutter-ui-visual/notes/asset-sync-map.md"
DEST.mkdir(parents=True, exist_ok=True)
MAP.parent.mkdir(parents=True, exist_ok=True)

sources = [
    ("home", FL / "features/home/assets", "HomeScreen / AllServices / Search / Dubbing"),
    ("pay", FL / "features/pay/assets", "MembershipScreen"),
    ("settings", FL / "features/settings/assets", "MineSettings / PersonalizedSettings"),
    ("music", FL / "features/music/assets", "MediaEntry / MusicSession"),
]
exts = {".png", ".webp", ".jpg", ".jpeg"}
rows, used, gaps = [], {}, []
copied = 0

def sanitize(rel: str) -> str:
    s = rel.replace("\\", "/").lower()
    s = re.sub(r"[^a-z0-9]+", "_", s)
    s = re.sub(r"_+", "_", s).strip("_")
    if s and s[0].isdigit():
        s = "n_" + s
    return s

for module, root, screens in sources:
    if not root.exists():
        gaps.append(f"- `{module}`: missing `{root}`")
        continue
    for p in sorted(root.rglob("*")):
        if not p.is_file():
            continue
        suf = p.suffix.lower()
        if suf not in exts:
            if suf in {".svg", ".json"}:
                gaps.append(f"- SKIP `{p.relative_to(FL)}` ({suf})")
            continue
        rel = p.relative_to(root).as_posix()
        stem = sanitize(f"{module}_{Path(rel).with_suffix('').as_posix()}")
        out_ext = ".jpg" if suf == ".jpeg" else suf
        name = stem + out_ext
        if name in used and used[name] != str(p):
            name = f"{stem}_{hashlib.md5(str(p).encode()).hexdigest()[:6]}{out_ext}"
        used[name] = str(p)
        shutil.copy2(p, DEST / name)
        copied += 1
        rows.append((module, str(p.relative_to(FL)), f"Res.drawable.{Path(name).stem}", screens, name))

lines = [
    "# Asset sync map (Flutter → Compose)",
    "",
    f"Source root: `{FL}`",
    f"Destination: `composeApp/src/commonMain/composeResources/drawable/`",
    f"Copied files: **{copied}**",
    f"Regenerate: `./scripts/sync-flutter-assets.sh`",
    "",
    "Excluded: bfui, DoKit, feature-package launcher sets.",
    "",
    "| module | flutter_path | compose_id | screens | file |",
    "|---|---|---|---|---|",
]
lines += [f"| {a} | `{b}` | `{c}` | {d} | `{e}` |" for a, b, c, d, e in rows]
lines += [
    "",
    "## Tab chrome",
    "",
    "Flutter tabs use CupertinoIcons (vector), not PNG. Mirror with Material icons in KMP.",
    "",
    "## Chat / Community",
    "",
    "No local image packs; network avatars + theme tokens.",
    "",
    "## Gaps / skips",
    "",
]
lines += gaps[:100] if gaps else ["- (none)"]
lines += ["", "## Fonts", "", "- System fonts + AppTheme metrics this wave.", ""]
MAP.write_text("\n".join(lines), encoding="utf-8")
print(f"OK copied={copied} → {DEST}")
print(f"map → {MAP}")
PY
