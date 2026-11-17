#!/usr/bin/env bash
# Sync verified deep-dive markdown pages into Feishu wiki 「学习 / KMP学习」.
# Usage: bash docs/deep-dive/_verify/sync_feishu.sh [--dry-run|--audit-only]
set -uo pipefail

LARK=/Users/qpy/.workbuddy/binaries/node/cli-connector-packages/lib/node_modules/@larksuite/cli/bin/lark-cli
SPACE=7660024466872765661
PARENT=YO1nwr2RNiBmM2kjnxJcxBzCnyd
DOC_DIR=/Users/qpy/Desktop/github/my_kmp_project/docs/deep-dive
DRY="${1:-}"

# filename|exact wiki node title — these are the live page names, NOT display labels.
# Matching is by these strings plus _verify/page-map.tsv; a title that drifts creates a duplicate page.
MAP=(
  "09-build-and-config.md|09 构建与配置 —— settings / 根工程 / deployment / catalog / wrapper / OHOS 脚本 / composeApp"
  "10-core-base-platform-analytics.md|10 · core:base / core:platform / core:analytics 深度解析"
  "11-core-design-ui.md|11 · core:design 与 core:ui 深度文档"
  "12-core-storage-database-router.md|12 · core/storage · core/database · core/router 深度解析"
  "13-core-network-engine.md|13 · core/network 深度文档（引擎 / 基础设施层）"
  "14-core-network-endpoints.md|14 · 业务端点层与契约对账（core/network 消费侧）"
  "15-component-ports.md|15 · component —— ③ 垂直端口层（auth / chat / pay / push / realtime / routing / webview）"
  "16-feature-home.md|16 · feature:home 详解"
  "17-feature-mine-shell.md|17 · feature/mine + feature/shell 深度解析"
  "18-feature-auth-account-friend.md|18 · 登录鉴权域：feature/auth + core/account + feature/friend"
  "19-feature-media-community-chat.md|19 · 社交内容域全解：feature/media + feature/community + feature/chat"
  "20-feature-commerce-wallet-pay-ai.md|Deep dive 20 · feature/commerce · wallet · pay · ai"
  "21-feature-small-modules.md|21 — Feature 薄壳模块深读 + Feature 层整体盘点"
  "22-composeapp-assembly-routing.md|22 · composeApp 装配层与路由跳转全景"
  "23-native-shells.md|23 · 三端原生壳与产物投递（Android / iOS / HarmonyOS）"
  "24-gates-ci-adr.md|24 · 工程门禁、CI、决策记录与 parity 工作法"
  "25-platform-differences-matrix.md|25 · 三端差异性总表（target、依赖可用性、expect/actual、行为与产物）"
  "26-cold-start-and-first-frame.md|26 · 三端冷启动与首帧流程"
  "27-module-gradle-wiring.md|27 · 公共库与独立库的 Gradle 接线对照"
  "28-config-files-line-by-line.md|28 · 主要配置文件逐行解释（三端壳配置、CI、openspec、gitignore）"
)

# Post-publish render audit: compare what Feishu serves back against the source file.
# The Markdown importer intermittently drops a block chunk on large table-heavy pages, so a
# MANGLE verdict is repaired by re-overwriting the same doc (overwrite is idempotent) and re-reading.
# `</dev/null` keeps lark-cli from eating the loop's stdin while reading page-map.tsv.
render_audit() {
  cd "$DOC_DIR" || return 1
  local bad=0
  while IFS=$'\t' read -r file title node obj <&3; do
    [ -s "$file" ] || continue
    tmp=$(mktemp)
    verdict="MANGLE"; repairs=0
    for try in 1 2 3; do
      $LARK docs +fetch --doc "$obj" --doc-format markdown --scope full \
        --jq '.data.document.content' > "$tmp" 2>/dev/null </dev/null
      verdict=$(SRC="$file" GOT="$tmp" python3 - <<'PYCHK'
import os
src=open(os.environ["SRC"],encoding="utf-8").read()
got=open(os.environ["GOT"],encoding="utf-8").read()
print("OK" if got.count("/")>=src.count("/") else "MANGLE")
PYCHK
)
      [ "$verdict" = "OK" ] && break
      if [ "$try" -lt 3 ]; then
        $LARK docs +update --doc "$obj" --command overwrite --doc-format markdown \
          --content "@$file" --jq '{ok}' >/dev/null 2>&1 </dev/null
        repairs=$((repairs+1)); sleep 2
      fi
    done
    res=$(SRC="$file" GOT="$tmp" python3 - <<'PYFINAL'
import os
src=open(os.environ["SRC"],encoding="utf-8").read()
got=open(os.environ["GOT"],encoding="utf-8").read()
a,b=src.count("/"),got.count("/")
print("src=%d got=%d delta=%+d lines=%d/%d"%(a,b,b-a,src.count(chr(10)),got.count(chr(10))))
PYFINAL
)
    [ "$verdict" != "OK" ] && bad=$((bad+1))
    note=""; [ "$repairs" -gt 0 ] && note=" repaired=$repairs"
    echo "AUDIT $verdict $title -> $res$note"
    rm -f "$tmp"
  done 3< _verify/page-map.tsv
  echo "AUDIT-BAD $bad"
  return $((bad>0))
}

if [ "$DRY" = "--audit-only" ]; then
  echo "### render audit only"
  render_audit
  exit $?
fi

# Refuse to publish a book that fails the citation gate or the Feishu markdown gate.
if [ "$DRY" != "--dry-run" ]; then
  echo "### 0) acceptance gates"
  python3 "$(dirname "$0")/verify_docs.py" | grep -q "REVIEW\|FAIL" && { echo "GATE: verify_docs.py has non-PASS books"; exit 1; }
  python3 "$(dirname "$0")/check_markdown.py" || { echo "GATE: check_markdown.py found slash-italic risk"; exit 1; }
fi

node_list() { $LARK wiki +node-list --space-id "$SPACE" --parent-node-token "$PARENT" \
  --jq '.data.nodes[]|"\(.title)\t\(.node_token)\t\(.obj_token)"' 2>/dev/null </dev/null; }

echo "### 1) resolve file -> node mapping (page-map.tsv is the memory; titles only as fallback)"
cd "$DOC_DIR" || exit 1
SNAP=$(mktemp); [ -s _verify/page-map.tsv ] && cp _verify/page-map.tsv "$SNAP"
EXISTING=$(node_list)
: > _verify/page-map.tsv
for entry in "${MAP[@]}"; do
  file="${entry%%|*}"; title="${entry#*|}"
  if [ ! -s "$file" ]; then echo "SKIP(no file) $file"; continue; fi
  row=$(awk -F'\t' -v f="$file" '$1==f{print $3"\t"$4; exit}' "$SNAP")
  node="${row%%	*}"; obj="${row#*	}"
  if [ -n "$obj" ] && printf '%s\n' "$EXISTING" | grep -qF "$obj"; then
    echo "MAPPED $file -> $obj"
  else
    row=$(printf '%s\n' "$EXISTING" | grep -F "$title" | head -1 | awk -F'\t' '{print $2"\t"$3}')
    node="${row%%	*}"; obj="${row#*	}"
    if [ -z "$obj" ] || [ "$obj" = "$node" ]; then
      if [ "$DRY" = "--dry-run" ]; then echo "DRY create $title"; continue; fi
      $LARK wiki +node-create --space-id "$SPACE" --parent-node-token "$PARENT" --title "$title" --obj-type docx --jq '{ok,error}' | tail -3
      EXISTING=$(node_list)
      row=$(printf '%s\n' "$EXISTING" | grep -F "$title" | head -1 | awk -F'\t' '{print $2"\t"$3}')
      node="${row%%	*}"; obj="${row#*	}"
      echo "CREATED $title -> $obj"
    else
      echo "FOUND $file -> $obj"
    fi
  fi
  [ -n "$obj" ] && printf '%s\t%s\t%s\t%s\n' "$file" "$title" "$node" "$obj" >> _verify/page-map.tsv
done
rm -f "$SNAP"

echo "### 2) write bodies"
while IFS=$'\t' read -r file title node obj <&3; do
  [ -s "$file" ] || continue
  if [ "$DRY" = "--dry-run" ]; then echo "DRY write $title ($obj)"; continue; fi
  out=$($LARK docs +update --doc "$obj" --command overwrite --doc-format markdown --content "@$file" --jq '{ok,error}' 2>&1 </dev/null | tail -3 | tr '\n' ' ')
  echo "WRITE $title -> $out"
done 3< _verify/page-map.tsv

echo "### 3) page map"
column -t -s $'\t' _verify/page-map.tsv 2>/dev/null || cat _verify/page-map.tsv

echo "### 4) render audit (raw slash + line count vs source)"
render_audit
