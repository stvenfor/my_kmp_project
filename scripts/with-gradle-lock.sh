#!/usr/bin/env bash
# Same advisory lock scripts/verify-parity.sh uses, so an agent's narrow build
# cannot collide with another agent's daemon request (observed failure: 666s lock
# wait, then a spurious "other files are broken" attribution).
#
# Usage: scripts/with-gradle-lock.sh ./gradlew :feature:ai:testDebugUnitTest
set -uo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root" || exit 1
(( $# >= 1 )) || { echo "usage: $0 <command> [args…]" >&2; exit 2; }

lock_dir="${TMPDIR:-/tmp}/qoder-gradle-verify.lock"
lock_pid="$lock_dir/pid"
lock_deadline=$(( $(date +%s) + ${GRADLE_LOCK_WAIT:-3600} ))

while true; do
  if mkdir "$lock_dir" 2>/dev/null; then break; fi
  holder="$(cat "$lock_pid" 2>/dev/null || echo)"
  if [[ -n "$holder" ]] && ! kill -0 "$holder" 2>/dev/null; then
    rm -f "$lock_pid"; rmdir "$lock_dir" 2>/dev/null; continue
  fi
  if (( $(date +%s) >= lock_deadline )); then
    echo "with-gradle-lock: timed out waiting for gradle lock (holder pid=${holder:-unknown})" >&2
    exit 1
  fi
  echo "with-gradle-lock: gradle lock held by pid=${holder:-unknown}, retrying in 10s"
  sleep 10
done

echo "$$" > "$lock_pid"
cleanup() { rm -f "$lock_pid"; rmdir "$lock_dir" 2>/dev/null; }
trap cleanup EXIT

"$@"
status=$?
exit "$status"
