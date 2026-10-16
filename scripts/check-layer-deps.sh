#!/usr/bin/env bash
# Layer gate. Three axes, all of them must stay clean:
#
#   1) feature → feature：同一个 feature 目录/模块只能看到自己。跨模块 UI 走
#      app 装配 + CrossFeatureScreens 端口。
#   2) 低层（core/*、component/）不得向上 import feature/ 或 app/。
#   3) 模块级依赖：feature 拆成 Gradle 模块后，光查 import 不够 ——
#      `implementation(projects.feature.X)` 本身就是违规，必须在 build.gradle.kts 层拦掉。
#
# 拆分是渐进的，所以每条规则都同时覆盖「还在 :composeApp 里」和「已下沉成模块」两种形态。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG=com.example.my_kmp_project
violations=0

report() { echo "VIOLATION: $*"; violations=$((violations + 1)); }

# ---- 1) feature → feature (source imports) ----
while IFS= read -r -d '' file; do
  feat="$(echo "$file" | sed -n 's|.*/feature/\([^/]*\)/.*|\1|p')"
  [[ -z "$feat" ]] && continue
  while IFS= read -r line; do
    if [[ "$line" =~ feature\.([a-zA-Z0-9_]+) ]]; then
      other="${BASH_REMATCH[1]}"
      [[ "$other" != "$feat" ]] && report "$file imports feature.$other"
    fi
  done < <(grep -E "^import $PKG\.feature\." "$file" || true)
done < <(find "$ROOT/composeApp/src" "$ROOT/feature" -name '*.kt' -print0 2>/dev/null)

# ---- 2) lower layers must not import upward ----
# 只看 core/ 与 component/ 两种形态：还在 :composeApp 里的包，以及已下沉的 core/* 模块。
while IFS= read -r -d '' file; do
  while IFS= read -r line; do
    report "$file (lower layer) imports upward: $line"
  done < <(grep -E "^import $PKG\.(feature|app)\." "$file" || true)
done < <(find "$ROOT/composeApp/src" "$ROOT/core" "$ROOT/feature" \
              \( -path '*/core/*' -o -path '*/component/*' \) -name '*.kt' -print0 2>/dev/null)

# ---- 3) module-level: a feature module may not depend on a sibling feature module ----
while IFS= read -r -d '' gradle; do
  while IFS= read -r line; do
    report "$gradle declares a feature→feature module dependency: ${line//[$'\t']/ }"
  done < <(grep -E 'projects\.feature\.' "$gradle" || true)
done < <(find "$ROOT/feature" -name build.gradle.kts -print0 2>/dev/null)

# ---- 3b) core modules may not depend on feature modules ----
while IFS= read -r -d '' gradle; do
  while IFS= read -r line; do
    report "$gradle (core layer) depends on a feature module: ${line//[$'\t']/ }"
  done < <(grep -E 'projects\.feature\.' "$gradle" || true)
done < <(find "$ROOT/core" -name build.gradle.kts -print0 2>/dev/null)

if [[ "$violations" -gt 0 ]]; then
  echo "Found $violations layer violation(s). Use app assembly + CrossFeatureScreens ports instead."
  exit 1
fi
echo "OK: layer gate clean (no feature→feature imports, no upward imports, no feature→feature module deps)."
