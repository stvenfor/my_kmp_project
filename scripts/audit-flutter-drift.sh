#!/usr/bin/env bash
# M14.3 · 双仓 drift 巡检（KMP 侧对齐 my_ai_project 上游 SoT）
#
# 三轴 diff：
#   (a) Flutter HEAD vs notes/flutter-baseline.md 里记录的 baseline_head
#   (b) Dart 端点集合 (`/api/v1/…` + `/im/…` 字符串字面量) vs Kotlin 端点集合
#   (c) Dart 路由路径 (`commons/wys_router/lib/src/route/route_path.dart`)
#       vs Kotlin `core/router/**` 路由常量集合
#
# 输出人类可读报告；任何一轴存在 drift 时退出码非零。
# 网络零依赖；只读 Flutter 仓库；只有显式 `--record` 才更新 baseline。
#
# 用法：
#   bash scripts/audit-flutter-drift.sh [--record] [--flutter <abs-path>]
#
# 环境变量：
#   FLUTTER_REPO  覆盖 Flutter 仓库路径（默认 /Users/qpy/Desktop/github/my_ai_project）
#
# 约束：
#   - 只跑 git rev-parse / git log / find / grep / python3，绝不写 Flutter；
#   - `--record` 只改本仓 `openspec/.../notes/flutter-baseline.md` 的 baseline_head 行。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FLUTTER_ROOT="${FLUTTER_REPO:-/Users/qpy/Desktop/github/my_ai_project}"
BASELINE_NOTE="$ROOT/openspec/changes/sync-flutter-full-parity/notes/flutter-baseline.md"
RECORD=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --record) RECORD=1; shift ;;
    --flutter) FLUTTER_ROOT="${2:?--flutter needs a path}"; shift 2 ;;
    -h|--help) sed -n '1,26p' "$0"; exit 0 ;;
    *) echo "audit-flutter-drift: unknown arg: $1" >&2; exit 2 ;;
  esac
done

[[ -d "$FLUTTER_ROOT" ]] || { echo "Flutter repo not found: $FLUTTER_ROOT (set FLUTTER_REPO)" >&2; exit 2; }
[[ -f "$BASELINE_NOTE" ]] || { echo "baseline note missing: $BASELINE_NOTE" >&2; exit 2; }
[[ -d "$FLUTTER_ROOT/.git" ]] || { echo "Flutter repo is not a git checkout: $FLUTTER_ROOT" >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 required" >&2; exit 2; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Embedded resolver: turns quoted path literals (including `$CONST/…` composition)
# into a sorted-unique set. Handles same-file `const val BASE = "/api/v1/mall"` and
# `const val CART = "$BASE/cart"` so composed endpoints are compared, not their raw parts.
cat > "$TMP/resolve.py" <<'PY'
import sys, re
mode = sys.argv[1]          # 'ep' | 'route'
files = [l.rstrip("\n") for l in sys.stdin if l.strip()]
def_quot = re.compile(r"""([A-Za-z_]\w*)\s*(?::\s*\w+\s*)?=\s*(["'])((?:[^"'\\\n]|\\.)*?)\2""")
str_lit  = re.compile(r"""(["'])((?:[^"'\\\n]|\\.)*)\1""")
def resolve(s, consts):
    for _ in range(2):
        for k in sorted(consts, key=len, reverse=True):
            s = s.replace("${%s}" % k, consts[k]).replace("$%s" % k, consts[k])
    s = re.sub(r"\$\{[^}]*\}", ":v", s)
    s = re.sub(r"\$\w+", ":v", s)
    s = re.sub(r"\{\w+\}", ":v", s)
    return s.rstrip("/")
def extract(text):
    consts = {}
    for m in def_quot.finditer(text):
        n, v = m.group(1), m.group(3)
        if "$" not in v and "{" not in v: consts[n] = v
    for _ in range(4):
        for m in def_quot.finditer(text):
            n, v = m.group(1), m.group(3)
            if n in consts: continue
            r = resolve(v, consts)
            if "$" not in r and "{" not in r and ":" not in r:
                consts[n] = r
    out = set()
    for m in str_lit.finditer(text):
        s = resolve(m.group(2), consts)
        if not s.startswith("/"): continue
        if s in ("/", "/:v"): continue
        if mode == "ep":
            if s.startswith("/api/v1") or s == "/im" or s.startswith("/im/"):
                out.add(s)
        else:
            if s.startswith("/api") or s.startswith("/im"): continue
            if len(s) > 1: out.add(s)
    return out
res = set()
for f in files:
    try: res |= extract(open(f, encoding="utf-8").read())
    except Exception: pass
for p in sorted(res): print(p)
PY

resolve_paths() {  # $1 = 'ep'|'route'; stdin = file list; stdout = sorted paths
  python3 "$TMP/resolve.py" "$1"
}

dart_kt_ep_files() {  # Kotlin production .kt files
  find "$ROOT/core" "$ROOT/feature" "$ROOT/component" "$ROOT/composeApp/src" \
       -type f -name '*.kt' 2>/dev/null \
    | grep -vE '(commonTest|androidUnitTest|jvmTest|nativeTest|iosTest|ohosTest|UnitTest|Test\.kt|/test/)' \
    || true
}

drift_total=0
section() { printf '\n=== %s ===\n' "$1"; }

# --- (a) HEAD drift ------------------------------------------------------------------------

section "(a) Flutter HEAD vs recorded baseline"
current_head="$(git -C "$FLUTTER_ROOT" rev-parse HEAD)"
current_short="$(git -C "$FLUTTER_ROOT" rev-parse --short=7 HEAD)"
current_subject="$(git -C "$FLUTTER_ROOT" log -1 --pretty=%s)"
baseline_head="$(sed -nE 's/^baseline_head:[[:space:]]*([0-9a-f]+).*/\1/p' "$BASELINE_NOTE" | head -1)"
printf '  current Flutter HEAD : %s (%s) — %s\n' "$current_short" "$current_head" "$current_subject"
if [[ -z "$baseline_head" ]]; then
  printf '  recorded baseline    : <none> (add `baseline_head: <sha>` to %s)\n' "$BASELINE_NOTE"
  drift_total=$((drift_total + 1))
else
  printf '  recorded baseline    : %s\n' "${baseline_head:0:7}"
  if [[ "$baseline_head" == "$current_head" ]]; then
    printf '  → aligned\n'
  else
    ahead="$(git -C "$FLUTTER_ROOT" rev-list --count "${baseline_head}..HEAD" 2>/dev/null || echo unknown)"
    printf '  → DRIFT: Flutter is %s commit(s) ahead of baseline\n' "$ahead"
    drift_total=$((drift_total + 1))
    if (( RECORD )); then
      sed -i.bak -E "s|^baseline_head:[[:space:]]*[0-9a-f]+.*|baseline_head: ${current_head}|" "$BASELINE_NOTE"
      rm -f "${BASELINE_NOTE}.bak"
      printf '  → recorded. baseline_head updated to %s\n' "$current_short"
    else
      printf '  → run with --record to freeze this HEAD as the new baseline\n'
    fi
  fi
fi

# --- (b) endpoint drift --------------------------------------------------------------------

section "(b) /api/v1 + /im endpoint paths (Dart vs Kotlin)"
dart_ep="$TMP/dart_ep.txt"; kotlin_ep="$TMP/kotlin_ep.txt"
find "$FLUTTER_ROOT" -type f -name '*.dart' 2>/dev/null \
  | grep -vE '(_test\.dart|/test/|\.g\.dart$)' \
  | resolve_paths ep > "$dart_ep"
dart_kt_ep_files | resolve_paths ep > "$kotlin_ep"
dart_n=$(wc -l < "$dart_ep" | tr -d ' ')
kotlin_n=$(wc -l < "$kotlin_ep" | tr -d ' ')
missing_ep="$TMP/missing_ep.txt"; extra_ep="$TMP/extra_ep.txt"
comm -23 "$dart_ep" "$kotlin_ep" > "$missing_ep"
comm -13 "$dart_ep" "$kotlin_ep" > "$extra_ep"
m_n=$(wc -l < "$missing_ep" | tr -d ' ')
e_n=$(wc -l < "$extra_ep" | tr -d ' ')
printf '  Dart   : %s unique paths\n' "$dart_n"
printf '  Kotlin : %s unique paths\n' "$kotlin_n"
printf '  → Missing in Kotlin (Flutter has, KMP does not) : %s\n' "$m_n"
if (( m_n > 0 )); then sed -E 's/^/      /' "$missing_ep"; drift_total=$((drift_total + 1)); fi
printf '  → Extra   in Kotlin (KMP has, Flutter does not) : %s\n' "$e_n"
if (( e_n > 0 )); then sed -E 's/^/      /' "$extra_ep"; drift_total=$((drift_total + 1)); fi

# --- (c) route drift -----------------------------------------------------------------------

section "(c) Route paths (Dart router table vs Kotlin core/router)"
dart_rt="$TMP/dart_rt.txt"; kotlin_rt="$TMP/kotlin_rt.txt"
route_file="$FLUTTER_ROOT/commons/wys_router/lib/src/route/route_path.dart"
if [[ -f "$route_file" ]]; then printf '%s\n' "$route_file" | resolve_paths route > "$dart_rt"
else : > "$dart_rt"; fi
find "$ROOT/core/router/src/commonMain" -type f -name '*.kt' 2>/dev/null \
  | resolve_paths route > "$kotlin_rt"
dart_rn=$(wc -l < "$dart_rt" | tr -d ' ')
kotlin_rn=$(wc -l < "$kotlin_rt" | tr -d ' ')
missing_rt="$TMP/missing_rt.txt"; extra_rt="$TMP/extra_rt.txt"
comm -23 "$dart_rt" "$kotlin_rt" > "$missing_rt"
comm -13 "$dart_rt" "$kotlin_rt" > "$extra_rt"
mr_n=$(wc -l < "$missing_rt" | tr -d ' ')
er_n=$(wc -l < "$extra_rt" | tr -d ' ')
printf '  Dart   route table : %s unique paths\n' "$dart_rn"
printf '  Kotlin core/router : %s unique paths\n' "$kotlin_rn"
printf '  → Missing in Kotlin : %s\n' "$mr_n"
if (( mr_n > 0 )); then sed -E 's/^/      /' "$missing_rt"; drift_total=$((drift_total + 1)); fi
printf '  → Extra   in Kotlin : %s\n' "$er_n"
if (( er_n > 0 )); then sed -E 's/^/      /' "$extra_rt"; drift_total=$((drift_total + 1)); fi

# --- summary -------------------------------------------------------------------------------

section "summary"
if (( drift_total == 0 )); then
  printf 'no drift across (a) HEAD · (b) endpoints · (c) routes\n'
  exit 0
else
  printf 'drift axes flagged: %s (see sections above)\n' "$drift_total"
  printf 'headlines: endpoints missing=%s extra=%s · routes missing=%s extra=%s\n' \
    "$m_n" "$e_n" "$mr_n" "$er_n"
  exit 1
fi
