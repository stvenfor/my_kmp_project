#!/usr/bin/env bash
# M10.A.2.i regression guard: every `struct Native…Page` in NativeSecondaryPages.swift
# must be reachable through the SwiftUI dispatch chain, and no known route may fall
# through to the `genericList` placeholder while a dedicated struct exists:
#
#   route/label → NativeRouteResolver.resolve() → `path == "…"` guard → Native…Page(
#
# Nothing is hardcoded per page: the route↔struct mapping and the resolver semantics are
# parsed from the sources themselves —
#   * struct defs   : NativeSecondaryPages.swift  `struct Native…Page: View`
#   * render pairs  : NativeFeatureCatalog.swift  `NativeFeatureHost` body — a
#                     `path == "/x"` / `pathOrLabel == "L"` guard plus the `Native…Page(`
#                     it opens on the following line
#   * resolver      : `NativeRouteResolver` — normalizePath if-rule chain + trailing
#                     `switch path` cases + labelToPath dict, replayed in source order
#   * delivered     : KNOWN Kotlin routes (AppRoutePath.kt / FeatureRoutes.kt), labelToPath
#                     keys+values, in-page `onOpen("…")` literals, ContentView toastOnlyKeys
#   * shell-owned   : iosApp ContentView.swift tab roots / Mine island / Compose secondaries
#                     / toast-only — those never reach NativeFeatureHost
#
# Usage: bash scripts/check-ios-page-dispatch.sh   (no Gradle, no Xcode)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS="$ROOT/iosApp/iosApp"
PAGES="$IOS/NativeSecondaryPages.swift"
CATALOG="$IOS/NativeFeatureCatalog.swift"
CONTENTVIEW="$IOS/ContentView.swift"
ROUTER="$ROOT/core/router/src/commonMain/kotlin/com.example.my_kmp_project/core/router"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# ---------------------------------------------------------------- (a) struct defs
grep -E '^struct Native[A-Za-z0-9]+Page: View' "$PAGES" \
  | sed -E 's/^struct ([A-Za-z0-9]+): View.*/\1/' | sort -u > "$TMP/defined.txt"
TOTAL=$(wc -l < "$TMP/defined.txt" | tr -d ' ')

# ---------------------------------------------------------------- (b) render pairs
awk '/^struct NativeFeatureHost/,/^}/' "$CATALOG" > "$TMP/host.swift"
awk '
  /path == "[^"]+"/ || /pathOrLabel == "[^"]+"/ {
    t = $0
    while (match(t, /path == "[^"]+"/)) {
      cur[++c] = substr(t, RSTART + 9, RLENGTH - 10); t = substr(t, RSTART + RLENGTH)
    }
    t = $0
    while (match(t, /pathOrLabel == "[^"]+"/)) {
      cur[++c] = "L" substr(t, RSTART + 16, RLENGTH - 17); t = substr(t, RSTART + RLENGTH)
    }
    next
  }
  c > 0 && match($0, /Native[A-Za-z0-9]+Page\(/) {
    s = substr($0, RSTART, RLENGTH); sub(/\($/, "", s)
    for (j = 1; j <= c; j++) print cur[j] "\t" s
    for (j = 1; j <= c; j++) cur[j] = ""
    c = 0
    next
  }
' "$TMP/host.swift" | sort -u > "$TMP/pairs.txt"

awk -F '\t' '$1 !~ /^L/ { print $1 }' "$TMP/pairs.txt" | sort -u > "$TMP/guards.txt"
awk -F '\t' '{ print $2 }' "$TMP/pairs.txt" | sort -u > "$TMP/rendered.txt"

# ---------------------------------------------------------------- resolver
cat > "$TMP/resolve.awk" <<'AWK'
# $1 = NativeFeatureCatalog.swift; stdin = one route/label per line → "<in>\t<resolved>"
NR == FNR {                       # pass 1: harvest the resolver tables only
  if ($0 ~ /^    private static func normalizePath/) { stage = 2; next }
  if ($0 ~ /^    static let labelToPath/) { stage = 1; next }
  if (stage == 1) {               # labelToPath dict entries: "label": "/path"
    if ($0 ~ /^    \]$/) { stage = 0; next }
    t = $0
    while (match(t, /"[^"]+"[[:space:]]*:[[:space:]]*"[^"]+"/)) {
      pair = substr(t, RSTART, RLENGTH); t = substr(t, RSTART + RLENGTH)
      match(pair, /"[^"]+"/); k = substr(pair, RSTART + 1, RLENGTH - 2)
      rest = substr(pair, RSTART + RLENGTH); match(rest, /"[^"]+"/)
      label[k] = substr(rest, RSTART + 1, RLENGTH - 2)
    }
    next
  }
  if (stage != 2) next
  if ($0 ~ /^[[:space:]]*case / && $0 ~ /:[[:space:]]*return "/) {   # normalizePath switch
    head = $0; sub(/:.*/, "", head); sub(/^[[:space:]]*case[[:space:]]*/, "", head)
    o = $0; sub(/.*return[[:space:]]*"/, "", o); sub(/".*/, "", o)
    t = head
    while (match(t, /"[^"]+"/)) {
      swp[++ns] = substr(t, RSTART + 1, RLENGTH - 2); swo[ns] = o
      t = substr(t, RSTART + RLENGTH)
    }
    next
  }
  start = nr + 1; t = $0
  while (match(t, /path == "[^"]+"/)) {
    nr++; kind[nr] = "eq"; pat[nr] = substr(t, RSTART + 9, RLENGTH - 10)
    t = substr(t, RSTART + RLENGTH)
  }
  t = $0
  while (match(t, /path\.hasPrefix\("[^"]+"\)/)) {
    nr++; kind[nr] = "pre"; pat[nr] = substr(t, RSTART + 16, RLENGTH - 18)
    t = substr(t, RSTART + RLENGTH)
  }
  if (nr >= start) {                       # rule header seen on this line
    if ($0 ~ /return path/) { for (j = start; j <= nr; j++) outv[j] = ID; next }
    if (match($0, /return "[^"]+"/)) {
      o = substr($0, RSTART + 8, RLENGTH - 9)
      for (j = start; j <= nr; j++) outv[j] = o
      next
    }
    open = start; next                     # `return` sits on a following line
  }
  if (open) {
    if ($0 ~ /return path/) o = ID
    else if (match($0, /return "[^"]+"/)) o = substr($0, RSTART + 8, RLENGTH - 9)
    else next
    for (j = open; j <= nr; j++) outv[j] = o
    open = 0
  }
  next
}
BEGIN { ID = "\001identity" }
function npath(p,   j, o) {                # normalizePath: if-chain, then switch, then identity
  for (j = 1; j <= nr; j++) {
    if (outv[j] == "") continue
    if (kind[j] == "eq") { if (p != pat[j]) continue }
    else if (substr(p, 1, length(pat[j])) != pat[j]) continue
    o = outv[j]
    return (o == ID) ? p : o
  }
  for (j = 1; j <= ns; j++) if (p == swp[j]) return swo[j]
  return p
}
                                # pass 2: replay NativeRouteResolver.resolve()
{
  p = $0; i = index(p, "://")
  if (i > 0) { p = substr(p, i + 3); if (substr(p, 1, 1) != "/") p = "/" p }
  i = index(p, "?"); if (i > 0) p = substr(p, 1, i - 1)
  i = index(p, "#"); if (i > 0) p = substr(p, 1, i - 1)
  if (substr(p, 1, 1) == "/") r = npath(p)
  else r = ($0 in label) ? label[$0] : $0
  print $0 "\t" r
}
AWK

# ---------------------------------------------------------------- delivered routes
grep -ohE '"/[^"]*"' "$ROUTER/AppRoutePath.kt" "$ROUTER/FeatureRoutes.kt" | tr -d '"' | sort -u > "$TMP/known.txt"
grep -ohE 'onOpen\?\("[^"]+"\)' "$IOS"/*.swift | sed -E 's/.*\("//; s/"\)//' | sort -u > "$TMP/onopen.txt"
awk '/toastOnlyKeys: Set<String> = \[/,/^        \]/' "$CONTENTVIEW" \
  | grep -ohE '"[^"]+"' | tr -d '"' | sort -u > "$TMP/toast.txt"
{ cat "$TMP/known.txt" "$TMP/onopen.txt" "$TMP/toast.txt"; } > "$TMP/inputs.txt"
awk '/^    static let labelToPath/,/^    \]$/' "$CATALOG" \
  | grep -ohE '"[^"]+"[[:space:]]*:[[:space:]]*"[^"]+"' \
  | sed -E 's/^"([^"]+)"[[:space:]]*:[[:space:]]*"([^"]+)"/\1\n\2/' >> "$TMP/inputs.txt"
sort -u "$TMP/inputs.txt" -o "$TMP/inputs.txt"

awk -f "$TMP/resolve.awk" "$CATALOG" "$TMP/inputs.txt" > "$TMP/resolved.txt"
awk -F '\t' '{ print $2 }' "$TMP/resolved.txt" | sort -u > "$TMP/producible.txt"
awk -F '\t' '$2 != $1' "$TMP/resolved.txt" > "$TMP/rewritten.txt"

# ---------------------------------------------------------------- shell ownership
{
  grep -E '^[[:space:]]*case ' "$CONTENTVIEW" | grep -ohE '"/[^"]*"' | tr -d '"'
  cat "$TMP/toast.txt"
  awk '/private func openOwnedRoute/,/private func openMineIslandWithSoftAuth/' "$CONTENTVIEW" \
    | grep -ohE '(route|key|path)\.hasPrefix\("[^"]+"\)' | sed -E 's/.*\("//; s/"\)//'
} | sort -u > "$TMP/owned.txt"

# ---------------------------------------------------------------- out-of-scope registry
# M10.A.2.i: these routes reach genericList because NO Native…Page struct models them;
# inventing SwiftUI for them is out of scope. Owning screen per target (evidence in
# openspec/changes/sync-flutter-full-parity/notes/ios-generic-list-2026-10-17.md).
cat > "$TMP/exempt.txt" <<'EXEMPT'
/bfui/course_info
/bfui/design_course
/bfui/feedback
/bfui/fitness_app
/bfui/glass_view
/bfui/help
/bfui/hotel_booking
/bfui/hotel_filters
/bfui/introduction_animation
/bfui/invite_friend
/bfui/mediterranean_diet
/bfui/my_diary
/bfui/navigation_drawer
/bfui/running_view
/bfui/training
/bfui/wave_view
/bfui/workout_view
/community/topic_select
/community/video_play
/login
/login/otp
/login/password
/media/entry
/pay
/register
/settings/bluetooth_demo
/settings/dialog_demo
/settings/im_debug
/settings/linking_debug
/settings/realtime_debug
EXEMPT

echo "Native page dispatch guard"
echo "  defined structs    : $TOTAL"
echo "  rendered structs   : $(wc -l < "$TMP/rendered.txt" | tr -d ' ')"
echo "  dispatch route ids : $(wc -l < "$TMP/guards.txt" | tr -d ' ')"
echo "  known Kotlin routes: $(wc -l < "$TMP/known.txt" | tr -d ' ')"
echo "  producible paths   : $(wc -l < "$TMP/producible.txt" | tr -d ' ')"
echo "  shell-owned paths  : $(wc -l < "$TMP/owned.txt" | tr -d ' ')"
echo

FAILURES=""

# (b) every defined struct must be instantiated AND carry at least one reachable guard:
#     a `path == "/x"` guard must be an *output* of resolve(); a `pathOrLabel == "L"` guard
#     is compared against the raw delivered key, so it must be a delivered key itself.
while IFS= read -r struct; do
  [ -n "$struct" ] || continue
  guards="$(awk -F '\t' -v s="$struct" '$2 == s { printf "%s ", $1 }' "$TMP/pairs.txt" | sed 's/ $//')"
  live="$(awk -F '\t' -v s="$struct" '
    FNR == 1 { f++ }
    f == 1 { prod[$1] = 1; next }
    f == 2 { key[$1] = 1; next }
    f == 3 && $2 == s {
      if ($1 ~ /^L/) { if (substr($1, 2) in key) { print $1; exit } }
      else if ($1 in prod) { print $1; exit }
    }
  ' "$TMP/producible.txt" "$TMP/inputs.txt" "$TMP/pairs.txt")"
  if [ -z "$guards" ]; then
    printf 'FAIL %-36s never instantiated in the NativeFeatureHost chain\n' "$struct"
    FAILURES="$FAILURES$struct"$'\n'
  elif [ -z "$live" ]; then
    printf 'FAIL %-36s guards [%s] never produced by NativeRouteResolver.resolve()\n' "$struct" "$guards"
    FAILURES="$FAILURES$struct"$'\n'
  else
    printf 'OK   %-36s route=%s\n' "$struct" "$live"
  fi
done < "$TMP/defined.txt"
echo

# (c) no known route may reach genericList while a dedicated struct exists for it.
while IFS= read -r route; do
  [ -n "$route" ] || continue
  resolved="$(awk -F '\t' -v r="$route" '$1 == r { print $2; exit }' "$TMP/resolved.txt")"
  [ -n "$resolved" ] || resolved="$route"
  if grep -Fxq "$resolved" "$TMP/guards.txt"; then continue; fi
  if grep -Fxq "$route" "$TMP/owned.txt"; then continue; fi
  if grep -Fxq "$resolved" "$TMP/owned.txt"; then continue; fi
  if grep -Fxq "$route" "$TMP/exempt.txt"; then continue; fi
  # A dedicated struct exists when some reachable guard is a strict path-prefix of this
  # route — i.e. the struct is written for this route family, only the mapping misses it.
  owner="$(awk -F '\t' -v l="$resolved" '
    NR == FNR { prod[$1] = 1; next }
    $1 !~ /^L/ && ($1 in prod) && length($1) < length(l) && substr(l, 1, length($1) + 1) == $1 "/" {
      print $1 "\t" $2; exit
    }
  ' "$TMP/producible.txt" "$TMP/pairs.txt")"
  if [ -n "$owner" ]; then
    printf 'FAIL route %-34s genericList although %s is dedicated to it (guard %s)\n' \
      "$route" "${owner#*	}" "${owner%%	*}"
  else
    printf 'FAIL route %-34s genericList and not declared out-of-scope\n' "$route"
  fi
  FAILURES="$FAILURES$route"$'\n'
done < "$TMP/known.txt"

echo
if [ -n "$FAILURES" ]; then
  echo "OFFENDERS:"
  printf '%s' "$FAILURES"
  echo "check-ios-page-dispatch: FAILED"
  exit 1
fi
echo "check-ios-page-dispatch: OK — all $TOTAL Native pages wired route → resolve() → guard → struct; $(wc -l < "$TMP/exempt.txt" | tr -d ' ') route(s) declared out-of-scope"
