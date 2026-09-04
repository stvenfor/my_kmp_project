#!/usr/bin/env bash
# Fail if feature packages import sibling feature packages (Spike I guard).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/composeApp/src/commonMain/kotlin/com.example.my_kmp_project/feature"
violations=0
while IFS= read -r -d '' file; do
  feat="$(echo "$file" | sed -n 's|.*/feature/\([^/]*\)/.*|\1|p')"
  [[ -z "$feat" ]] && continue
  while IFS= read -r line; do
    if [[ "$line" =~ feature\.([a-zA-Z0-9_]+) ]]; then
      other="${BASH_REMATCH[1]}"
      if [[ "$other" != "$feat" ]]; then
        echo "VIOLATION: $file imports feature.$other"
        violations=$((violations + 1))
      fi
    fi
  done < <(grep -E '^import com\.example\.my_kmp_project\.feature\.' "$file" || true)
done < <(find "$SRC" -name '*.kt' -print0)

if [[ "$violations" -gt 0 ]]; then
  echo "Found $violations feature→feature import(s). Use Navigator + component/core instead."
  exit 1
fi
echo "OK: no feature→feature imports."
