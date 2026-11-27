#!/usr/bin/env bash
# 产出 HarmonyOS signed release HAP。
#
# Usage:
#   ./scripts/package-harmony.sh [--env dev|test|staging|prod] [--version-name 1.0] [--version-code 42]
#
# 前置：
#   - macOS + DevEco Studio（默认 /Applications/DevEco-Studio.app，可用 DEVECO_HOME 覆盖）
#   - harmonyApp/signing.local.json（本机私有签名材料，已 gitignore；模板见 signing.local.example.json）
#   - 会先 publishReleaseBinariesToHarmonyApp（-Papp.release=true -Papp.env=...）
#
# 产物：dist/MyKmpProject-<env>-<versionName>-<versionCode>.hap + SHA256SUMS
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_NAME="prod"
VERSION_NAME=""
VERSION_CODE=""

ENV_FILE="$ROOT/gradle/app.env.properties"
file_value() {
  [[ -f "$ENV_FILE" ]] || return 0
  sed -n "s/^$1=//p" "$ENV_FILE" | head -1 | tr -d '\r'
}

usage() {
  awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
  exit 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env) ENV_NAME="$2"; shift 2 ;;
    --version-name) VERSION_NAME="$2"; shift 2 ;;
    --version-code) VERSION_CODE="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Unknown option: $1" >&2; usage ;;
  esac
done

VERSION_NAME="${VERSION_NAME:-$(file_value version.name)}"
VERSION_NAME="${VERSION_NAME:-1.0}"
VERSION_CODE="${VERSION_CODE:-$(file_value version.code)}"
VERSION_CODE="${VERSION_CODE:-1}"

DEVECO_HOME="${DEVECO_HOME:-/Applications/DevEco-Studio.app/Contents}"
HVIGORW="$DEVECO_HOME/tools/hvigor/bin/hvigorw.js"
if [[ ! -f "$HVIGORW" ]]; then
  echo "Error: DevEco hvigorw not found at $HVIGORW (set DEVECO_HOME)." >&2
  exit 2
fi
if [[ ! -f "$ROOT/harmonyApp/signing.local.json" ]]; then
  echo "Error: harmonyApp/signing.local.json is required for a signed release HAP." >&2
  echo "Copy harmonyApp/signing.local.example.json and fill in local signing material." >&2
  exit 2
fi

export DEVECO_SDK_HOME="$DEVECO_HOME/sdk"
export PATH="$DEVECO_SDK_HOME/default/openharmony/toolchains:$DEVECO_HOME/tools/node/bin:$DEVECO_HOME/tools/ohpm/bin:$DEVECO_HOME/tools/hvigor/bin:$PATH"

# ------------------------------------------------------------------ app.json5 version
APP_JSON="$ROOT/harmonyApp/AppScope/app.json5"
BACKUP="$(mktemp -t app.json5.XXXXXX)"
cp "$APP_JSON" "$BACKUP"
restore_app_json() { cp "$BACKUP" "$APP_JSON"; rm -f "$BACKUP"; }
trap restore_app_json EXIT

python3 - "$APP_JSON" "$VERSION_NAME" "$VERSION_CODE" <<'PY'
import json
import sys

path, version_name, version_code = sys.argv[1], sys.argv[2], int(sys.argv[3])
with open(path, encoding="utf-8") as handle:
    data = json.load(handle)
data["app"]["versionName"] = version_name
data["app"]["versionCode"] = version_code
with open(path, "w", encoding="utf-8") as handle:
    json.dump(data, handle, indent=2, ensure_ascii=False)
    handle.write("\n")
PY

echo "==> Harmony release: env=$ENV_NAME version=$VERSION_NAME ($VERSION_CODE)"

# ------------------------------------------------------------------ signing + publish + HAP
bash "$ROOT/scripts/harmony-signing.sh"

./scripts/with-gradle-lock.sh ./gradlew :composeApp:publishReleaseBinariesToHarmonyApp \
  -Papp.release=true \
  "-Papp.env=$ENV_NAME" \
  "-Papp.versionName=$VERSION_NAME" \
  "-Papp.versionCode=$VERSION_CODE"

cd "$ROOT/harmonyApp"
node "$HVIGORW" --sync -p product=default -p buildMode=release --analyze=normal --parallel --incremental --daemon
node "$HVIGORW" --mode module -p module=entry@default -p product=default -p buildMode=release \
  -p requiredDeviceType=phone assembleHap --analyze=normal --parallel --incremental --daemon

SIGNED_HAP="$ROOT/harmonyApp/entry/build/default/outputs/default/entry-default-signed.hap"
if [[ ! -f "$SIGNED_HAP" ]]; then
  echo "Error: signed HAP not produced at $SIGNED_HAP (check signing.local.json)." >&2
  exit 2
fi

DIST="$ROOT/dist"
mkdir -p "$DIST"
BASE="MyKmpProject-${ENV_NAME}-${VERSION_NAME}-${VERSION_CODE}"
cp "$SIGNED_HAP" "$DIST/$BASE.hap"
(
  cd "$DIST"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$BASE.hap" >> SHA256SUMS
  else
    shasum -a 256 "$BASE.hap" >> SHA256SUMS
  fi
)
echo "OK: $DIST/$BASE.hap"
