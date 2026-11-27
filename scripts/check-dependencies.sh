#!/usr/bin/env bash
# 依赖治理门禁：
#   1) 仓库只能在 settings.gradle.kts 声明（FAIL_ON_PROJECT_REPOS）；
#   2) 模块构建脚本禁止硬编码 group:artifact:version；
#   3) 版本目录 key 不重复、version.ref 必须存在；
#   4) 未引用别名 / 未被引用的版本必须登记在 gradle/dependency-allowlist.txt（只减不增）。
#
# 用法：bash scripts/check-dependencies.sh
# 维护白名单：gradle/dependency-allowlist.txt，一行一个 key：
#   unused:<alias>            允许某个 library/plugin 别名暂不引用
#   version:<name>            允许某个 [versions] 条目暂不引用
#   coord:<group:artifact>    允许模块脚本里出现某个硬编码坐标（需写理由注释）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

python3 - "$ROOT" <<'PY'
import re
import sys
from pathlib import Path

root = Path(sys.argv[1])
catalog_path = root / "gradle/libs.versions.toml"
allowlist_path = root / "gradle/dependency-allowlist.txt"

errors = []
warnings = []


def rel(path: Path) -> str:
    return str(path.relative_to(root))


allow_unused = set()
allow_versions = set()
allow_coords = set()
if allowlist_path.is_file():
    for line in allowlist_path.read_text(encoding="utf-8").splitlines():
        text = line.strip()
        if not text or text.startswith("#"):
            continue
        if text.startswith("unused:"):
            allow_unused.add(text[len("unused:"):])
        elif text.startswith("version:"):
            allow_versions.add(text[len("version:"):])
        elif text.startswith("coord:"):
            allow_coords.add(text[len("coord:"):])
        else:
            errors.append(f"{rel(allowlist_path)}: 未知白名单前缀 -> {text}")

# ---------------------------------------------------------------- 1) repositories
for path in root.rglob("*.gradle.kts"):
    if any(part in {".gradle", ".kotlin", "build"} for part in path.parts):
        continue
    if path.name == "settings.gradle.kts":
        continue
    for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if re.search(r"^\s*repositories\s*\{", line):
            errors.append(
                f"{rel(path)}:{lineno}: 模块内不允许声明 repositories，请改到 settings.gradle.kts"
            )

# ---------------------------------------------------------------- 2) hard-coded coords
coord_pattern = re.compile(
    r"(implementation|api|compileOnly|runtimeOnly|debugImplementation|releaseImplementation|"
    r"testImplementation|androidTestImplementation|ksp)\s*\(\s*\"([^\"]+)\"\s*\)"
)
for path in root.rglob("*.gradle.kts"):
    if any(part in {".gradle", ".kotlin", "build"} for part in path.parts):
        continue
    for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if line.strip().startswith("//"):
            continue
        for match in coord_pattern.finditer(line):
            coord = match.group(2)
            parts = coord.split(":")
            # group:artifact 或 group:artifact:version；只拦带版本的三段式。
            if len(parts) >= 3 and not coord.startswith("$") and coord not in allow_coords:
                errors.append(
                    f"{rel(path)}:{lineno}: 硬编码依赖坐标 '{coord}'，请放进 libs.versions.toml"
                )

# ---------------------------------------------------------------- 3) catalog integrity
if not catalog_path.is_file():
    errors.append("gradle/libs.versions.toml 不存在")
else:
    raw = catalog_path.read_text(encoding="utf-8")
    section = None
    versions = {}
    libraries = {}
    plugins = {}
    seen = {}
    for lineno, line in enumerate(raw.splitlines(), 1):
        stripped = line.strip()
        if stripped.startswith("[") and stripped.endswith("]"):
            section = stripped[1:-1]
            continue
        if not stripped or stripped.startswith("#"):
            continue
        match = re.match(r"^([A-Za-z0-9_.-]+)\s*=\s*(.+)$", stripped)
        if not match or section not in {"versions", "libraries", "plugins"}:
            continue
        key, value = match.group(1), match.group(2)
        bucket = {"versions": versions, "libraries": libraries, "plugins": plugins}[section]
        if key in bucket:
            errors.append(f"{rel(catalog_path)}:{lineno}: [{section}] key 重复 -> {key}")
        bucket[key] = value
        seen[key] = lineno

    # version.ref 必须存在
    for section, bucket in (("libraries", libraries), ("plugins", plugins)):
        for key, value in bucket.items():
            ref = re.search(r"version\.ref\s*=\s*\"([^\"]+)\"", value)
            if ref and ref.group(1) not in versions:
                errors.append(
                    f"{rel(catalog_path)}:{seen[key]}: [{section}] {key} 引用了不存在的 version -> {ref.group(1)}"
                )

    # 使用检测：把别名转成 Kotlin accessor（- -> .）
    script_texts = []
    for path in root.rglob("*.gradle.kts"):
        if any(part in {".gradle", ".kotlin", "build"} for part in path.parts):
            continue
        script_texts.append(path.read_text(encoding="utf-8"))
    usage = "\n".join(script_texts)

    def used(alias: str, prefix: str) -> bool:
        accessor = prefix + alias.replace("-", ".")
        return re.search(re.escape(accessor) + r"(?![A-Za-z0-9_])", usage) is not None

    referenced_versions = set()
    for key, value in libraries.items():
        if used(key, "libs."):
            referenced_versions.update(re.findall(r"version\.ref\s*=\s*\"([^\"]+)\"", value))
        elif key not in allow_unused:
            errors.append(
                f"{rel(catalog_path)}: [libraries] 未引用别名 -> {key}（删除或登记 unused:{key}）"
            )
    for key, value in plugins.items():
        if used(key, "libs.plugins."):
            referenced_versions.update(re.findall(r"version\.ref\s*=\s*\"([^\"]+)\"", value))
        elif key not in allow_unused:
            errors.append(
                f"{rel(catalog_path)}: [plugins] 未引用别名 -> {key}（删除或登记 unused:{key}）"
            )

    for name in versions:
        direct = "libs.versions." + name.replace("-", ".")
        directly_used = re.search(re.escape(direct) + r"(?![A-Za-z0-9_])", usage) is not None
        if name not in referenced_versions and not directly_used and name not in allow_versions:
            errors.append(
                f"{rel(catalog_path)}: [versions] 条目未被任何 library/plugin 引用 -> {name}"
                f"（删除或登记 version:{name}）"
            )

# ---------------------------------------------------------------- result
for warning in warnings:
    print(f"WARN: {warning}")
if errors:
    for error in errors:
        print(f"ERROR: {error}")
    print(f"FAILED: {len(errors)} dependency governance issue(s).")
    sys.exit(1)
print("OK: dependency governance clean (repositories centralized, no hard-coded coordinates, catalog consistent).")
PY
