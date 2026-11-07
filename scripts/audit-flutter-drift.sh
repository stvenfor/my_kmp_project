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

# comm 依据 locale 的 collation 逐行比较，而 resolve.py 用 Python 的码点序 sorted()。
# en_US.UTF-8 下 `/` 与 `_` 的折叠规则不同，会让同一路径（如 /classroom/homework_stats）
# 同时落进 missing 和 extra。统一成字节序，让 comm 与 resolve.py 的排序一致。
export LC_ALL=C

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

# 移植期译名别名（同一块屏幕）：KMP core/router 沿用旧名 transliter，Flutter
# route_path.dart 用 dubbing。仅用于 route 比较前归一，不改任何常量定义；
# Kotlin 常量 / route inventory / 原生分发脚本三侧均保持原样、彼此一致（不做统一改名）。
# 显式逐对列出，避免把未来新增的含 transliter 路由误判为同一路由。
ROUTE_ALIASES = {
    "/classroom/homework/transliter": "/classroom/homework/dubbing",
    "/home/transliter_feed": "/home/dubbing_feed",
    "/video/transliter/videos": "/video/dubbing/videos",
    "/video/transliter/videos/detail": "/video/dubbing/videos/detail",
    "/video/transliter/works": "/video/dubbing/works",
    "/video/transliter/works/detail": "/video/dubbing/works/detail",
}
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
        if mode == "route":
            s = ROUTE_ALIASES.get(s, s)
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

# --- 书面 allowlist --------------------------------------------------------------------
# 每行 "path|理由"。只豁免**精确列举**的既有差异；任何未登记的新差异仍然计入 drift
# （不是放宽阈值、不是删除真实差异）。完整分类与逐条理由见 notes/route-inventory.md
# 与 notes/backend-contract-matrix.md。
ALLOWLIST="$TMP/allowlist.tsv"
cat > "$ALLOWLIST" <<'ALLOW'
# ---- (b) endpoint：KMP 有、Flutter 无（3 条）----
/api/v1/im/friends/requests/:v/respond|后端真实路由 im_routes.go:22；Flutter im_friend_api.dart:159 也调用，resolver 不识别 Dart 的 Class.CONST 插值而漏采，误报为 KMP extra，非真实 drift（见 matrix）。
/api/v1/mall|MallApi.kt:232 私有组合前缀常量，仅用于拼 /api/v1/mall/* 子路径；后端 mall 组无裸路由、Flutter 用完整字面量，非真实端点（见 matrix 非端点常量节）。
/api/v1/user/huawei/login|后端真实路由 user_routes.go:21（华为登录），KMP AuthApiContracts.kt:31 已登记；Flutter d7876a1 未接入，KMP 对齐后端、领先 Flutter（extended/产品确认）。
# ---- (c) route：KMP core/router 原生页，Flutter route_path.dart 无对应条目（33 条）----
/ai|KMP 内容域聚合前缀（ProductRouteDispatch.kt:128），Flutter 按具体页登记；extended/产品确认。
/app|KMP 深链装配前缀（DeepLinkRouter.kt:142 removePrefix /app），Flutter 无此前缀；extended/产品确认。
/auth|KMP 鉴权域前缀（ProductRouteDispatch.kt:119），Flutter 用 /login /register；extended/产品确认。
/auth/login|KMP AppRoutes.kt:14 登录入口，Flutter 为 /login；extended/产品确认。
/auth/register|KMP AppRoutes.kt:15 注册入口，Flutter 为 /register；extended/产品确认。
/classroom|KMP 课堂域聚合前缀（DeepLinkRouter.kt:105），Flutter 按具体页登记；extended/产品确认。
/community/comment|KMP FeatureRoutes.kt:207 社区评论页，Flutter 未单列；extended/产品确认。
/community/image_preview|KMP FeatureRoutes.kt:204 看图页，Flutter 未单列；extended/产品确认。
/community/topic_select|KMP FeatureRoutes.kt:206 选话题页，Flutter 未单列；extended/产品确认。
/community/video_play|KMP FeatureRoutes.kt:205 看视频页，Flutter 未单列；extended/产品确认。
/media|KMP 媒体域聚合前缀（ProductRouteDispatch.kt:130），Flutter 按具体页登记；extended/产品确认。
/media/entry|KMP FeatureRoutes.kt:163 媒体入口，Flutter 未单列；extended/产品确认。
/mine/about|KMP AppRoutes.kt:33 / MineIslandRoute.kt:23 关于页，Flutter 用 MineIsland 内部导航；extended/产品确认。
/mine/business|KMP FeatureRoutes.kt:149 / ProductRouteDispatch.kt:44 商家页，Flutter 未单列；extended/产品确认。
/mine/business_card|KMP ProductRouteDispatch.kt:42 名片页，Flutter 未单列；extended/产品确认。
/mine/buy_qa|KMP FeatureRoutes.kt:148 购买问答，Flutter 未单列；extended/产品确认。
/mine/cooperation|KMP FeatureRoutes.kt:106/149 合作页，Flutter 未单列；extended/产品确认。
/mine/fan_group|KMP ProductRouteDispatch.kt:47 粉丝群，Flutter 未单列；extended/产品确认。
/mine/feedback|KMP FeatureRoutes.kt:105 反馈页，Flutter 未单列；extended/产品确认。
/mine/invite|KMP FeatureRoutes.kt:108 邀请页，Flutter 未单列；extended/产品确认。
/mine/poster|KMP FeatureRoutes.kt:99 海报页，Flutter 未单列；extended/产品确认。
/mine/qa|KMP FeatureRoutes.kt:148 问答页，Flutter 未单列；extended/产品确认。
/mine/reminder|KMP FeatureRoutes.kt:107 提醒页，Flutter 未单列；extended/产品确认。
/mine/reminders|KMP FeatureRoutes.kt:150 提醒列表，Flutter 未单列；extended/产品确认。
/mine/settings|KMP FeatureRoutes.kt:101 设置（legacy 别名），Flutter 为 /settings；extended/产品确认。
/mine/shop_qr|KMP FeatureRoutes.kt:94 店铺收款码，Flutter 未单列；extended/产品确认。
/mine/sms_template|KMP FeatureRoutes.kt:146 短信模板（单数别名），Flutter 未单列；extended/产品确认。
/mine/sms_templates|KMP FeatureRoutes.kt:93 短信模板，Flutter 未单列；extended/产品确认。
/mine/store_qr|KMP FeatureRoutes.kt:147 门店收款码，Flutter 未单列；extended/产品确认。
/music|KMP 音乐域前缀（DeepLinkRouter.kt:104 / ProductRouteDispatch.kt:129），Flutter 按页登记；extended/产品确认。
/privacy/consent|KMP AppRoutes.kt:10 隐私同意页，Flutter 无同名条目；extended/产品确认。
/scan|KMP ProductRouteDispatch.kt:96/118 扫码入口，Flutter route_path.dart 未登记；extended/产品确认。
/splash|KMP AppRoutes.kt:6 启动页；Flutter 用根路由 / 表达；extended/产品确认。
ALLOW

# 把一份 comm diff 拆成「真实 drift」($2) 与「书面登记(带理由打印)」($3)；
# 已登记项不计 drift，未登记项照旧计入 —— 只豁免精确列举的路径。
allow_n=0
allowlist_filter() {  # $1=diff-in; $2=real-out; $3=allowlisted-report-out
  local n
  n=$(python3 - "$ALLOWLIST" "$1" "$2" "$3" <<'PY'
import sys
allow = {}
for line in open(sys.argv[1], encoding="utf-8"):
    line = line.rstrip("\n")
    if not line or line.startswith("#"):
        continue
    p, _, r = line.partition("|")
    allow[p.strip()] = r.strip()
diffs = [l.rstrip("\n") for l in open(sys.argv[2], encoding="utf-8") if l.strip()]
real = [p for p in diffs if p not in allow]
listed = [(p, allow[p]) for p in diffs if p in allow]
open(sys.argv[3], "w").write("\n".join(real) + ("\n" if real else ""))
with open(sys.argv[4], "w") as f:
    if listed:
        f.write("    allowlisted (书面登记, 不计 drift): %d\n" % len(listed))
        for p, r in listed:
            f.write("      %s  <- %s\n" % (p, r))
print(len(listed))
PY
)
  allow_n=$((allow_n + n))
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
missing_ep_real="$TMP/missing_ep_real.txt"; extra_ep_real="$TMP/extra_ep_real.txt"
missing_ep_allow="$TMP/missing_ep_allow.txt"; extra_ep_allow="$TMP/extra_ep_allow.txt"
allowlist_filter "$missing_ep" "$missing_ep_real" "$missing_ep_allow"
allowlist_filter "$extra_ep" "$extra_ep_real" "$extra_ep_allow"
m_n=$(wc -l < "$missing_ep_real" | tr -d ' ')
e_n=$(wc -l < "$extra_ep_real" | tr -d ' ')
printf '  Dart   : %s unique paths\n' "$dart_n"
printf '  Kotlin : %s unique paths\n' "$kotlin_n"
printf '  → Missing in Kotlin (Flutter has, KMP does not) : %s\n' "$m_n"
if (( m_n > 0 )); then sed -E 's/^/      /' "$missing_ep_real"; drift_total=$((drift_total + 1)); fi
cat "$missing_ep_allow"
printf '  → Extra   in Kotlin (KMP has, Flutter does not) : %s\n' "$e_n"
if (( e_n > 0 )); then sed -E 's/^/      /' "$extra_ep_real"; drift_total=$((drift_total + 1)); fi
cat "$extra_ep_allow"

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
missing_rt_real="$TMP/missing_rt_real.txt"; extra_rt_real="$TMP/extra_rt_real.txt"
missing_rt_allow="$TMP/missing_rt_allow.txt"; extra_rt_allow="$TMP/extra_rt_allow.txt"
allowlist_filter "$missing_rt" "$missing_rt_real" "$missing_rt_allow"
allowlist_filter "$extra_rt" "$extra_rt_real" "$extra_rt_allow"
mr_n=$(wc -l < "$missing_rt_real" | tr -d ' ')
er_n=$(wc -l < "$extra_rt_real" | tr -d ' ')
printf '  Dart   route table : %s unique paths\n' "$dart_rn"
printf '  Kotlin core/router : %s unique paths\n' "$kotlin_rn"
printf '  → Missing in Kotlin : %s\n' "$mr_n"
if (( mr_n > 0 )); then sed -E 's/^/      /' "$missing_rt_real"; drift_total=$((drift_total + 1)); fi
cat "$missing_rt_allow"
printf '  → Extra   in Kotlin : %s\n' "$er_n"
if (( er_n > 0 )); then sed -E 's/^/      /' "$extra_rt_real"; drift_total=$((drift_total + 1)); fi
cat "$extra_rt_allow"

# --- summary -------------------------------------------------------------------------------

section "summary"
if (( drift_total == 0 )); then
  printf 'no unallowlisted drift across (a) HEAD · (b) endpoints · (c) routes\n'
  printf 'documented allowlist: %s item(s) with written reasons (see sections above + notes/route-inventory.md)\n' "$allow_n"
  printf 'headlines (unallowlisted): endpoints missing=%s extra=%s · routes missing=%s extra=%s\n' \
    "$m_n" "$e_n" "$mr_n" "$er_n"
  exit 0
else
  printf 'drift axes flagged: %s (see sections above)\n' "$drift_total"
  printf 'headlines: endpoints missing=%s extra=%s · routes missing=%s extra=%s (unallowlisted only)\n' \
    "$m_n" "$e_n" "$mr_n" "$er_n"
  exit 1
fi
