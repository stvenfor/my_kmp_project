#!/usr/bin/env bash
# Serialized parity verification: one Gradle invocation, guarded by an advisory
# lock so concurrent agent builds cannot exhaust the daemon lock timeout.
#
# Usage: scripts/verify-parity.sh [--offline] [--android-only] [--static-only]
set -uo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root" || exit 1

offline=""
android_only=""
static_only=""
for arg in "$@"; do
  case "$arg" in
    --offline) offline="--offline" ;;
    --android-only) android_only="1" ;;
    --static-only) static_only="1" ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

lock_dir="${TMPDIR:-/tmp}/qoder-gradle-verify.lock"
lock_pid="$lock_dir/pid"
lock_deadline=$(( $(date +%s) + ${GRADLE_LOCK_WAIT:-3600} ))

acquire_lock() {
  while true; do
    if mkdir "$lock_dir" 2>/dev/null; then
      trap 'rmdir_lock' EXIT
      echo "$$" > "$lock_pid"
      return 0
    fi
    local holder
    holder="$(cat "$lock_pid" 2>/dev/null || echo)"
    if [[ -n "$holder" ]] && ! kill -0 "$holder" 2>/dev/null; then
      rm -f "$lock_pid" && rmdir "$lock_dir" 2>/dev/null && continue
    fi
    if (( $(date +%s) >= lock_deadline )); then
      echo "verify-parity: timed out waiting for gradle lock (holder pid=${holder:-unknown})" >&2
      return 1
    fi
    echo "verify-parity: gradle lock held by pid=${holder:-unknown}, retrying in 10s"
    sleep 10
  done
}

rmdir_lock() {
  rm -f "$lock_pid" 2>/dev/null
  rmdir "$lock_dir" 2>/dev/null
}

echo "## 1/3 static gates"
# Kotlin block comments nest, so a route path like `core/*` inside a KDoc opens a comment
# that never closes; the file then fails to parse and every symbol it declares cascades
# into "Unresolved reference" across the tree. A lexer that tracks comment depth catches
# it statically; two agents already lost build cycles to this.
python3 - "composeApp/src" "core" <<'PY' || exit 1
import pathlib, sys

def findings(text):
    """Lex one Kotlin file: unclosed comment/literal + bracket depth left at EOF."""
    out, stack = [], []
    pairs = {"}": "{", "]": "[", ")": "("}
    i, line, n = 0, 1, len(text)
    while i < n:
        c = text[i]
        if c == "\n":
            line += 1
            i += 1
        elif c == "/" and text.startswith("//", i):
            nl = text.find("\n", i)
            line += text.count("\n", i, nl if nl != -1 else n)
            i = n if nl == -1 else nl
        elif c == "/" and text.startswith("/*", i):
            start = line
            depth, i = 1, i + 2
            while i < n and depth:
                if text.startswith("/*", i):
                    depth += 1
                    i += 2
                elif text.startswith("*/", i):
                    depth -= 1
                    i += 2
                else:
                    if text[i] == "\n":
                        line += 1
                    i += 1
            if depth:
                out.append(f"{start}: unterminated block comment")
        elif c == '"':
            if text.startswith('"""', i):
                start, i, closed = line, i + 3, False
                while i < n:
                    if text[i] == '"':
                        j = i
                        while j < n and text[j] == '"':
                            j += 1
                        if j - i >= 3:
                            # Kotlin closes on the LAST three quotes of the run, so the
                            # """"…"""" idiom (raw string containing quotes) stays valid.
                            i, closed = j, True
                            break
                        i = j
                    else:
                        if text[i] == "\n":
                            line += 1
                        i += 1
                if not closed:
                    out.append(f"{start}: unterminated multiline string")
            else:
                start, i = line, i + 1
                while i < n and text[i] != '"':
                    if text[i] == "\\":
                        i += 1
                    elif text[i] == "\n":
                        break
                    i += 1
                if i >= n or text[i] != '"':
                    out.append(f"{start}: unterminated string")
                else:
                    i += 1
        elif c == "'":
            start, i = line, i + 1
            while i < n and text[i] != "'":
                if text[i] == "\\":
                    i += 1
                elif text[i] == "\n":
                    break
                i += 1
            if i >= n or text[i] != "'":
                out.append(f"{start}: unterminated char literal")
            i += 1
        elif c in "{[(":
            stack.append((c, line))
            i += 1
        elif c in "}])":
            if stack and stack[-1][0] == pairs[c]:
                stack.pop()
            else:
                out.append(f"{line}: '{c}' closes nothing")
            i += 1
        else:
            i += 1
    for c, ln in stack[:3]:
        out.append(f"{ln}: '{c}' never closed")
    return out

bad = []
for root in sys.argv[1:]:
    for p in sorted(pathlib.Path(root).rglob("*.kt")):
        if "/build/" in str(p):
            continue
        for h in findings(p.read_text(encoding="utf-8")):
            bad.append(f"{p}:{h}")
if bad:
    print("Kotlin lexer gate — unclosed constructs:", file=sys.stderr)
    for b in bad[:40]:
        print("  " + b, file=sys.stderr)
    if len(bad) > 40:
        print(f"  ... {len(bad) - 40} more", file=sys.stderr)
    print("usual causes: a nested /* opener (e.g. `feature/**`) inside a KDoc, "
          "or a source file truncated mid-write.", file=sys.stderr)
    sys.exit(1)
print("kotlin comment/bracket scan: OK")
PY

bash scripts/check-layer-deps.sh || exit 1

if [[ -n "$static_only" ]]; then
  echo "verify-parity: static gates only, gradle skipped"
  exit 0
fi

tasks=(":composeApp:compileDebugKotlinAndroid"
       ":composeApp:testDebugUnitTest"
       ":core:network:testDebugUnitTest")
if [[ -z "$android_only" ]]; then
  tasks+=(":composeApp:compileKotlinIosX64"
          ":composeApp:compileKotlinIosArm64"
          ":composeApp:compileKotlinIosSimulatorArm64"
          ":composeApp:compileKotlinOhosArm64"
          ":composeApp:compileKotlinOhosX64")
fi

echo "## 2/3 gradle (${#tasks[@]} tasks, offline=${offline:-off})"
acquire_lock || exit 1
set -o pipefail
# shellcheck disable=SC2086
./gradlew "${tasks[@]}" $offline --stacktrace 2>&1 | tee verify-parity.log
build_status=${PIPESTATUS[0]}
rmdir_lock
trap - EXIT

echo "## 3/3 result"
if (( build_status != 0 )); then
  echo "BUILD FAILED (exit $build_status) — last 60 lines:" >&2
  tail -n 60 verify-parity.log >&2
  exit "$build_status"
fi

grep -E "^e: |FAILURE:|Execution failed" verify-parity.log && exit 1
for d in composeApp core/network; do
  printf '%s test results: ' "$d"
  find "$d/build/test-results" -name "TEST-*.xml" 2>/dev/null \
    | xargs -I{} sed -n 's/.*<testsuite[^>]*tests="\([0-9]*\)".*failures="\([0-9]*\)".*/\1 tests, \2 failures/p;/<testsuite /{s/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*/\1 tests, \2 failures/p;q}/p' {} \; 2>/dev/null \
    | awk '{t+=$1; f+=$3} END {printf "tests=%d failures=%d\n", t, f}'
done
echo "verify-parity: OK"
