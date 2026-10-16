#!/usr/bin/env bash
# Fail if feature packages import sibling feature packages (Spike I guard),
# or if the shared `component/` layer imports upward into `feature/`.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG="$ROOT/composeApp/src/commonMain/kotlin/com.example.my_kmp_project"
SRC="$PKG/feature"
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

# core/ and component/ are below feature/: they may never import upward.
while IFS= read -r -d '' file; do
  while IFS= read -r line; do
    echo "VIOLATION: $file (lower layer) imports upward: $line"
    violations=$((violations + 1))
  done < <(grep -E '^import com\.example\.my_kmp_project\.(feature|app)\.' "$file" || true)
done < <(find "$PKG/component" "$PKG/core" -name '*.kt' -print0 2>/dev/null)

if [[ "$violations" -gt 0 ]]; then
  echo "Found $violations layer violation(s). Use app assembly + CrossFeatureScreens ports instead."
  exit 1
fi
echo "OK: no feature→feature imports."
