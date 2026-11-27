#!/usr/bin/env bash
# Layer gate. Six axes, all of them must stay clean:
#
#   1) feature → feature：同一个 feature 目录/模块只能看到自己。跨模块 UI 走
#      app 装配 + CrossFeatureScreens 端口。
#   2) 低层（core/*、component/）不得向上 import feature/ 或 app/。
#   3) 模块级依赖：feature 拆成 Gradle 模块后，光查 import 不够 ——
#      `implementation(projects.feature.X)` 本身就是违规，必须在 build.gradle.kts 层拦掉。
#   4) feature 不得新增全局可变状态 `object *Store`（应改为注入的仓库/状态对象）。
#   5) feature 不得直接调用 `NetworkFacade.api()` / `AccountFacade.current()`（应注入接口）。
#   6) feature 不得默认构造 `XxxGateway()`（应在 AppGraph 装配）。
#
# 4–6 的存量违规登记在 scripts/layer-deps-baseline.txt；新增即失败，白名单只减不增。
# 维护白名单：bash scripts/check-layer-deps.sh --print-baseline（补跑后人工 review）。
#
# 拆分是渐进的，所以每条规则都同时覆盖「还在 :composeApp 里」和「已下沉成模块」两种形态。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG=com.example.my_kmp_project
violations=0

PRINT_BASELINE=""
for arg in "$@"; do
  case "$arg" in
    --print-baseline) PRINT_BASELINE="1" ;;
    *)
      echo "unknown argument: $arg (supported: --print-baseline)" >&2
      exit 2
      ;;
  esac
done

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
# 只看 core/ 与 component/ 两种形态：还在 :composeApp 里的包、已下沉的 core/* 模块，
# 以及顶层 :component 模块（component/）。
while IFS= read -r -d '' file; do
  while IFS= read -r line; do
    report "$file (lower layer) imports upward: $line"
  done < <(grep -E "^import $PKG\.(feature|app)\." "$file" || true)
done < <(find "$ROOT/composeApp/src" "$ROOT/core" "$ROOT/feature" "$ROOT/component" \
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

# ---- 3c) the component module may not depend on feature modules either ----
while IFS= read -r -d '' gradle; do
  while IFS= read -r line; do
    report "$gradle (component layer) depends on a feature module: ${line//[$'\t']/ }"
  done < <(grep -E 'projects\.feature\.' "$gradle" || true)
done < <(find "$ROOT/component" -name build.gradle.kts -print0 2>/dev/null)

# ---------------------------------------------------------------------------
# 4–6) DI 方向门禁：feature 不得绕过接口直接摸单例 / 默认构造 / 新增全局可变状态。
#
# 白名单 key = `规则|相对路径|去缩进行内容`；重复行按出现次数计。
# 新增（当前次数 > 白名单次数）即失败；次数变少只提示删旧行，保持「只减不增」。
# ---------------------------------------------------------------------------
BASELINE="$ROOT/scripts/layer-deps-baseline.txt"
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

current="$tmpdir/current.txt"
: > "$current"

# collect_rule <rule-id> <regex>：扫描 feature/**/*.kt，跳过注释行（//、*、/*）。
collect_rule() {
  local rule="$1" pattern="$2" file rel raw text
  while IFS= read -r -d '' file; do
    rel="${file#"$ROOT"/}"
    while IFS= read -r raw; do
      text="${raw#*:}"
      text="$(printf '%s' "$text" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//')"
      [[ -z "$text" ]] && continue
      [[ "$text" == //* || "$text" == \** || "$text" == /\** ]] && continue
      printf '%s|%s|%s\n' "$rule" "$rel" "$text" >> "$current"
    done < <(grep -nE "$pattern" "$file" || true)
  done < <(find "$ROOT/feature" -name '*.kt' -not -path '*/build/*' -print0 2>/dev/null)
}

# R4：全局可变 object *Store 只能存在于 core/component 或显式白名单里。
collect_rule "R4-object-store" '(^|[^A-Za-z0-9_])object[[:space:]]+[A-Za-z0-9_]*Store([^A-Za-z0-9_]|$)'
# R5：直接取网络 / 会话单例，绕过 ApiClientProvider / SessionRepository 注入。
collect_rule "R5-network-facade" 'NetworkFacade[[:space:]]*\.[[:space:]]*api[[:space:]]*\('
collect_rule "R5-account-facade" 'AccountFacade[[:space:]]*\.[[:space:]]*current[[:space:]]*\('
# R6：默认构造具体 Gateway（应统一在 AppGraph 装配）。
collect_rule "R6-gateway-default" '=[[:space:]]*[A-Z][A-Za-z0-9_]*Gateway[[:space:]]*\('

if [[ -n "$PRINT_BASELINE" ]]; then
  sort "$current"
  exit 0
fi

# 输出 `key<TAB>count`：key 里可能含空格，用 awk 只剥离行首计数、不重建字段，
# 避免 BSD sed 对 replacement 里 \t 的解释差异。
count_keys() {
  sort "$1" | uniq -c \
    | awk '{ count = $1; sub(/^[[:space:]]*[0-9]+[[:space:]]/, ""); print $0 "\t" count }'
}

count_keys "$current" > "$tmpdir/current_counts.txt"
: > "$tmpdir/baseline_keys.txt"
if [[ -f "$BASELINE" ]]; then
  sed -E 's/[[:space:]]+$//' "$BASELINE" \
    | grep -vE '^[[:space:]]*(#|$)' \
    >> "$tmpdir/baseline_keys.txt" || true
fi
count_keys "$tmpdir/baseline_keys.txt" > "$tmpdir/baseline_counts.txt"

while IFS=$'\t' read -r key count; do
  [[ -z "$key" ]] && continue
  allowed="$(awk -F $'\t' -v k="$key" '$1 == k { print $2 }' "$tmpdir/baseline_counts.txt")"
  allowed="${allowed:-0}"
  if (( count > allowed )); then
    report "[$key] 出现 $count 次，白名单仅 $allowed 次 —— 新增违规（改为构造注入）"
  elif (( count < allowed )); then
    echo "NOTE: 白名单过期 $((allowed - count)) 行：[$key] —— 请删除，保持只减不增"
  fi
done < "$tmpdir/current_counts.txt"

while IFS=$'\t' read -r key count; do
  [[ -z "$key" ]] && continue
  if ! awk -F $'\t' -v k="$key" '$1 == k { found = 1 } END { exit !found }' "$tmpdir/current_counts.txt"; then
    echo "NOTE: 白名单条目当前已无违规：[$key] —— 请删除，保持只减不增"
  fi
done < "$tmpdir/baseline_counts.txt"

if [[ "$violations" -gt 0 ]]; then
  echo "Found $violations layer violation(s). Use app assembly + CrossFeatureScreens ports / injected contracts instead."
  exit 1
fi
echo "OK: layer gate clean (no feature→feature imports, no upward imports, no feature→feature module deps, no new feature globals/singleton calls/gateway constructions)."
