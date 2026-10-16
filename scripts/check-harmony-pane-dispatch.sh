#!/usr/bin/env bash
# M10.A.1.h regression guard: every `export struct Native…Pane` in
# NativeSecondaryPages.ets must be reachable through the ArkTS dispatch chain:
#
#   route → nativePaneIdFor() pane id → Index.ets render branch → struct (+ Index.ets import)
#
# Nothing is hardcoded per-pane; the guard derives the id↔struct mapping by parsing
# the dispatch tables themselves:
#   * struct defs : NativeSecondaryPages.ets  `export struct Native…Pane`
#   * mapped ids  : NativeFeatureCatalog.ets  `nativePaneIdFor()` `return '<id>'` literals
#   * render pairs: Index.ets  `this.nativePane === '<id>'` followed by the `Native…Pane(` it
#   * imports     : Index.ets  `from './NativeSecondaryPages'` import block
#
# Usage: bash scripts/check-harmony-pane-dispatch.sh   (no Gradle, no DevEco)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PAGES="$ROOT/harmonyApp/entry/src/main/ets/pages/NativeSecondaryPages.ets"
CATALOG="$ROOT/harmonyApp/entry/src/main/ets/pages/NativeFeatureCatalog.ets"
INDEX="$ROOT/harmonyApp/entry/src/main/ets/pages/Index.ets"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# (a) Distinct pane structs defined in the pages file.
grep -E '^export struct Native[A-Za-z0-9]+Pane' "$PAGES" | awk '{print $3}' | sort -u > "$TMP/defined.txt"
TOTAL=$(wc -l < "$TMP/defined.txt" | tr -d ' ')

# (b1) Pane ids actually returned by nativePaneIdFor ('' and the generic 'feature' fallback excluded).
awk '/^export function nativePaneIdFor/,/^}/' "$CATALOG" \
  | grep -oE "return '[A-Za-z0-9_]+'" \
  | sed -E "s/^return '//" | sed -E "s/'$//" \
  | grep -v '^feature$' | sort -u > "$TMP/mapped.txt"

# (b2) Render pairs "<pane-id> <StructName>" from Index.ets dispatch branches.
# `this.Native…Pane(` lines are @Builder method calls (e.g. NativeFeaturePane), not structs.
awk '
  match($0, /this\.nativePane === .[A-Za-z0-9_]+./) {
    id = substr($0, RSTART, RLENGTH)
    gsub(/this\.nativePane === /, "", id)
    gsub(/[^A-Za-z0-9_]/, "", id)
    cur = id
    next
  }
  cur != "" && /this\.Native[A-Za-z0-9]+Pane\(/ { cur = ""; next }
  cur != "" && match($0, /Native[A-Za-z0-9]+Pane\(/) {
    s = substr($0, RSTART, RLENGTH)
    sub(/\($/, "", s)
    print cur " " s
    cur = ""
    next
  }
' "$INDEX" | sort -u > "$TMP/pairs.txt"

# (b3) Structs imported by Index.ets from './NativeSecondaryPages'.
awk '
  /^[[:space:]]*import \{/ { buf = ""; inb = 1 }
  inb                   { buf = buf " " $0 }
  inb && /NativeSecondaryPages/ { print buf; inb = 0 }
' "$INDEX" | grep -oE 'Native[A-Za-z0-9]+Pane' | sort -u > "$TMP/imported.txt"

echo "Native pane dispatch guard"
echo "  defined structs : $TOTAL"
echo "  mapped pane ids : $(wc -l < "$TMP/mapped.txt" | tr -d ' ')"
echo "  render pairs    : $(cut -d' ' -f2 "$TMP/pairs.txt" | sort -u | wc -l | tr -d ' ') struct(s) rendered"
echo

FAILURES=""
while IFS= read -r struct; do
  [ -n "$struct" ] || continue
  ids="$(awk -v s="$struct" '$2 == s { printf "%s", (n++ ? "," : "") $1 }' "$TMP/pairs.txt")"
  problems=""
  if [ -z "$ids" ]; then
    problems="no Index.ets render branch"
  elif ! printf '%s\n' "$ids" | tr ',' '\n' | grep -Fxqf - "$TMP/mapped.txt" >/dev/null 2>&1; then
    problems="render id '$ids' is never returned by nativePaneIdFor"
  fi
  if ! grep -Fxq "$struct" "$TMP/imported.txt"; then
    problems="${problems:+$problems; }not imported in Index.ets"
  fi
  if [ -n "$problems" ]; then
    printf 'FAIL %-36s %s\n' "$struct" "$problems"
    FAILURES="$FAILURES$struct"$'\n'
  else
    printf 'OK   %-36s id=%s\n' "$struct" "$ids"
  fi
done < "$TMP/defined.txt"

# Symmetric check: a pane id returned by nativePaneIdFor with no render branch would
# open a blank overlay instead of its pane — same class of M10.A.1.h bug, other direction.
while IFS= read -r id; do
  [ -n "$id" ] || continue
  if ! cut -d' ' -f1 "$TMP/pairs.txt" | grep -Fxq "$id"; then
    printf 'FAIL mapped id %-29s has no Index.ets render branch\n' "$id"
    FAILURES="$FAILURES(id: $id)"$'\n'
  fi
done < "$TMP/mapped.txt"

echo
if [ -n "$FAILURES" ]; then
  echo "OFFENDERS:"
  printf '%s' "$FAILURES"
  echo "check-harmony-pane-dispatch: FAILED"
  exit 1
fi
echo "check-harmony-pane-dispatch: OK — all $TOTAL Native panes wired route → id → render branch → import"
