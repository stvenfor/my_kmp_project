#!/usr/bin/env bash
# Generate harmonyApp/build-profile.json5 without keeping signing material in git.
#
#   harmonyApp/build-profile.base.json5   committed template (no secrets)
#   harmonyApp/signing.local.json         machine-private signing material (gitignored)
#   harmonyApp/build-profile.json5        generated output (gitignored)
#
# The generated profile keeps every non-signing field from an existing
# build-profile.json5 when it is valid JSON (so DevEco edits survive), and always
# replaces `app.signingConfigs` / product `signingConfig` from the private file.
# With no signing.local.json the result is a valid unsigned configuration:
# hvigor builds the HAP and simply skips signing.
#
# Usage: ./scripts/harmony-signing.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/harmonyApp"

BASE="$APP_DIR/build-profile.base.json5"
LOCAL="$APP_DIR/signing.local.json"
OUT="$APP_DIR/build-profile.json5"

if ! command -v python3 >/dev/null 2>&1; then
  echo "Error: python3 is required to merge the Harmony signing profile." >&2
  exit 1
fi

python3 - "$BASE" "$LOCAL" "$OUT" <<'PY'
import json
import os
import sys

base_path, local_path, out_path = sys.argv[1], sys.argv[2], sys.argv[3]


def load_strict(path):
    """Strict JSON only; JSON5 comments / trailing commas fall back to the template."""
    try:
        with open(path, encoding="utf-8") as handle:
            return json.load(handle)
    except (OSError, ValueError):
        return None


def expand(value):
    if isinstance(value, str):
        return os.path.expanduser(value.replace("${HOME}", os.path.expanduser("~")))
    if isinstance(value, list):
        return [expand(item) for item in value]
    if isinstance(value, dict):
        return {key: expand(item) for key, item in value.items()}
    return value


profile = load_strict(out_path) or load_strict(base_path)
if not profile:
    sys.exit(f"Error: cannot read {base_path}")

app = profile.setdefault("app", {})

configs = []
if os.path.isfile(local_path):
    with open(local_path, encoding="utf-8") as handle:
        configs = [
            expand(config)
            for config in (json.load(handle).get("signingConfigs") or [])
            if isinstance(config, dict)
        ]

app["signingConfigs"] = configs
for product in app.get("products", []):
    if configs:
        product["signingConfig"] = configs[0].get("name", "default")
    else:
        product.pop("signingConfig", None)

with open(out_path, "w", encoding="utf-8") as handle:
    json.dump(profile, handle, indent=2, ensure_ascii=False)
    handle.write("\n")

if configs:
    print(f"OK: {out_path} (signing: {len(configs)} config from signing.local.json)")
else:
    print(f"OK: {out_path} (no signing.local.json -> unsigned build profile)")
PY
