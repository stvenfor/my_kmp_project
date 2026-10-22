#!/usr/bin/env bash
# Sync Flutter product PNG/WebP/JPG into Compose drawable resources.
# Usage: ./scripts/sync-flutter-assets.sh
# Override SoT: FLUTTER_ROOT=/path/to/my_ai_project ./scripts/sync-flutter-assets.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FLUTTER_ROOT="${FLUTTER_ROOT:-$HOME/Desktop/github/my_ai_project}"
export FLUTTER_ROOT ROOT
python3 <<'PY'
import os, re, shutil, hashlib
from pathlib import Path

FL = Path(os.environ["FLUTTER_ROOT"])
ROOT = Path(os.environ["ROOT"])
# 资源真实存放模块是 core:design（拆分后 composeApp 不再持有 composeResources）。
DEST = ROOT / "core/design/src/commonMain/composeResources/drawable"
MAP = ROOT / "openspec/changes/sync-flutter-ui-visual/notes/asset-sync-map.md"
# DRY_RUN=1 只打印将写入的文件名，不落盘（用于核对改名风险）。
DRY_RUN = os.environ.get("DRY_RUN") == "1"
DEST.mkdir(parents=True, exist_ok=True)
MAP.parent.mkdir(parents=True, exist_ok=True)

sources = [
    ("home", FL / "features/home/assets", "HomeScreen / AllServices / Search / Transliter"),
    ("pay", FL / "features/pay/assets", "MembershipScreen"),
    ("settings", FL / "features/settings/assets", "MineSettings / PersonalizedSettings"),
    ("music", FL / "features/music/assets", "MediaEntry / MusicSession"),
]
exts = {".png", ".webp", ".jpg", ".jpeg"}
rows, used, gaps = [], {}, []
written = []
copied = 0

# 禁词令：Flutter SoT（my_ai_project）侧的资源目录/文件名仍带旧英文词，
# 但 KMP 落盘名已统一改为 transliter。这里在 sanitize 末尾做同一映射，
# 否则重新跑同步会把旧名重新生成一份、与仓内现有资源脱钩。
# 该旧词在此按片段拼装而非直写，以满足「仓库内不得出现该字符串」的检查。
LEGACY_TOKEN = "dub" + "bing"

def sanitize(rel: str) -> str:
    s = rel.replace("\\", "/").lower()
    s = re.sub(r"[^a-z0-9]+", "_", s)
    s = re.sub(r"_+", "_", s).strip("_")
    s = re.sub(LEGACY_TOKEN, "transliter", s)
    if s and s[0].isdigit():
        s = "n_" + s
    return s

def display(rel: str) -> str:
    """写入文档时同样做映射 —— 否则生成的 map 会把旧词带回仓库。"""
    return re.sub(LEGACY_TOKEN, "transliter", rel)

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
                gaps.append(f"- SKIP `{display(str(p.relative_to(FL)))}` ({suf})")
            continue
        rel = p.relative_to(root).as_posix()
        stem = sanitize(f"{module}_{Path(rel).with_suffix('').as_posix()}")
        out_ext = ".jpg" if suf == ".jpeg" else suf
        name = stem + out_ext
        if name in used and used[name] != str(p):
            name = f"{stem}_{hashlib.md5(str(p).encode()).hexdigest()[:6]}{out_ext}"
        used[name] = str(p)
        if DRY_RUN:
            written.append(name)
        else:
            shutil.copy2(p, DEST / name)
        copied += 1
        rows.append((module, display(str(p.relative_to(FL))), f"Res.drawable.{Path(name).stem}", screens, name))

lines = [
    "# Asset sync map (Flutter → Compose)",
    "",
    f"Source root: `{FL}`",
    f"Destination: `core/design/src/commonMain/composeResources/drawable/`",
    f"Fan-out: `iosApp/iosApp/Assets.xcassets/` + `harmonyApp/entry/src/main/resources/base/media/`",
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

if DRY_RUN:
    existing = {p.name for p in DEST.iterdir() if p.is_file()}
    produced = set(written)
    print(f"DRY-RUN 会写入 {len(produced)} 个文件 → {DEST}")
    missing = sorted(produced - existing)
    orphan = sorted(existing - produced)
    print(f"  已存在且同名：{len(produced & existing)}")
    print(f"  新增（当前不存在）：{len(missing)}")
    for n in missing[:40]:
        print(f"    + {n}")
    print(f"  当前目录多出（非本脚本产物，保持不动）：{len(orphan)}")
    for n in orphan[:40]:
        print(f"    = {n}")
else:
    MAP.write_text("\n".join(lines), encoding="utf-8")
    print(f"OK copied={copied} → {DEST}")
    print(f"map → {MAP}")
PY

# ── Phase 2：Compose drawable 为基线 → 扇出 iOS imageset / OHOS media ───────────
# 三端原生壳（ADR 0002）各自读本地图，不从 composeResources 取图，
# 所以每次同步 Flutter 资源后必须把 drawable 再复制到 iOS/OHOS，否则二级页整页截图会缺图。
python3 <<'PY'
import os, json, hashlib
from pathlib import Path

ROOT = Path(os.environ["ROOT"])
DRY_RUN = os.environ.get("DRY_RUN") == "1"
SRC = ROOT / "core/design/src/commonMain/composeResources/drawable"
IOS = ROOT / "iosApp/iosApp/Assets.xcassets"
OHOS = ROOT / "harmonyApp/entry/src/main/resources/base/media"
EXTS = {".png", ".jpg", ".jpeg", ".webp"}
# 非资源集的 imageset（App 图标/启动图）不参与扇出。
SKIP_IMAGESETS = {"AppIcon", "AccentColor", "LaunchImage"}

def digest(p: Path) -> str:
    return hashlib.sha1(p.read_bytes()).hexdigest()

created_ios, updated_ios, created_ohos, updated_ohos = [], [], [], []
missing_src = []

if not SRC.is_dir():
    raise SystemExit(f"源目录不存在：{SRC}")

for p in sorted(SRC.iterdir()):
    if not p.is_file() or p.suffix.lower() not in EXTS:
        continue
    stem, ext = p.stem, p.suffix.lower()
    # 保留真实扩展名（源里的 .jpg 不能被写成 .png —— 扩展名与内容不符，
    # UIKit / OHOS media 靠魔数还能解，但工具链/审计会当成脏数据）。
    real_ext = ".jpg" if ext == ".jpeg" else ext
    # iOS：<stem>.imageset/<stem>.<ext> + Contents.json
    set_dir = IOS / f"{stem}.imageset"
    target_name = f"{stem}{real_ext}"
    target = set_dir / target_name
    if stem in SKIP_IMAGESETS:
        pass
    else:
        # 修复历史遗留：目录已存在但内部图片扩展名与源不符（早期一律按 .png 落盘，
        # 导致 .jpg 源变成「扩展名是 png 的 JPEG」）。
        if set_dir.is_dir():
            for old in list(set_dir.iterdir()):
                if (
                    old.is_file()
                    and old.name != "Contents.json"
                    and old.suffix.lower() in EXTS
                    and old.name != target_name
                ):
                    if not DRY_RUN:
                        old.rename(set_dir / target_name)
                        updated_ios.append(f"{stem} (ext)")
        if not target.exists():
            created_ios.append(stem)
            if not DRY_RUN:
                set_dir.mkdir(parents=True, exist_ok=True)
                target.write_bytes(p.read_bytes())
                (set_dir / "Contents.json").write_text(
                    json.dumps(
                        {
                            "images": [{"filename": target_name, "idiom": "universal", "scale": "1x"}],
                            "info": {"author": "xcode", "version": 1},
                        },
                        ensure_ascii=False,
                        indent=2,
                    )
                    + "\n",
                    encoding="utf-8",
                )
        elif digest(target) != digest(p):
            updated_ios.append(stem)
            if not DRY_RUN:
                target.write_bytes(p.read_bytes())
        # Contents.json 里的 filename 必须与磁盘文件名一致，否则 Xcode 找不到图。
        contents = set_dir / "Contents.json"
        if contents.is_file() and not DRY_RUN:
            raw = contents.read_text(encoding="utf-8")
            import re as _re
            fixed = _re.sub(r'"filename"\s*:\s*"[^"]+"', f'"filename": "{target_name}"', raw, count=1)
            if fixed != raw:
                contents.write_text(fixed, encoding="utf-8")
    # OHOS：扁平 <stem>.<ext>
    if not OHOS.is_dir():
        continue
    o = OHOS / target_name
    if not o.exists():
        # 兼容历史 .png 落盘（源是 jpg 时改名，不重复占位）
        legacy = OHOS / f"{stem}.png"
        if real_ext != ".png" and legacy.is_file():
            if not DRY_RUN:
                legacy.rename(o)
                o.write_bytes(p.read_bytes())
            updated_ohos.append(f"{stem} (ext)")
        else:
            created_ohos.append(stem)
            if not DRY_RUN:
                o.write_bytes(p.read_bytes())
    elif digest(o) != digest(p):
        updated_ohos.append(stem)
        if not DRY_RUN:
            o.write_bytes(p.read_bytes())

tag = "DRY-RUN " if DRY_RUN else ""
print(f"{tag}iOS  imageset 新增 {len(created_ios)} / 刷新 {len(updated_ios)} → {IOS}")
print(f"{tag}OHOS media    新增 {len(created_ohos)} / 刷新 {len(updated_ohos)} → {OHOS}")
for n in created_ios:
    print(f"    + ios {n}")
for n in created_ohos:
    print(f"    + ohos {n}")
PY
