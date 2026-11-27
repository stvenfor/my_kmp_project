#!/usr/bin/env bash
# 产出 Android signed release APK + AAB，验签并生成 SHA256SUMS。
#
# Usage:
#   ./scripts/package-android.sh [--env dev|test|staging|prod] [--version-name X] [--version-code N]
#
# 签名材料（任一来源，绝不入库）：
#   1) CI: ANDROID_KEYSTORE_BASE64 + ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_ALIAS / ANDROID_KEY_PASSWORD
#   2) 本机: local.properties 的 release.storeFile / release.storePassword / release.keyAlias / release.keyPassword
#   3) 直接导出 ANDROID_RELEASE_STORE_FILE 等环境变量
#
# 产物：dist/MyKmpProject-<env>-<versionName>-<versionCode>.{apk,aab} + SHA256SUMS
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

case "$ENV_NAME" in
  dev|test|staging|prod) ;;
  *) echo "Error: --env must be dev|test|staging|prod" >&2; exit 2 ;;
esac

VERSION_NAME="${VERSION_NAME:-$(file_value version.name)}"
VERSION_NAME="${VERSION_NAME:-1.0}"
if [[ -z "$VERSION_CODE" ]]; then
  if git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    VERSION_CODE="$(git -C "$ROOT" rev-list --count HEAD)"
  fi
fi
VERSION_CODE="${VERSION_CODE:-$(file_value version.code)}"
VERSION_CODE="${VERSION_CODE:-1}"

# ------------------------------------------------------------------ keystore
CLEANUP_KEYSTORE=""
cleanup() {
  [[ -n "$CLEANUP_KEYSTORE" && -f "$CLEANUP_KEYSTORE" ]] && rm -f "$CLEANUP_KEYSTORE"
}
trap cleanup EXIT

STORE_FILE="${ANDROID_RELEASE_STORE_FILE:-}"
if [[ -n "${ANDROID_KEYSTORE_BASE64:-}" ]]; then
  CLEANUP_KEYSTORE="$(mktemp -t mykmp-release.XXXXXX.jks)"
  if ! printf '%s' "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$CLEANUP_KEYSTORE" 2>/dev/null; then
    printf '%s' "$ANDROID_KEYSTORE_BASE64" | base64 -D > "$CLEANUP_KEYSTORE"
  fi
  STORE_FILE="$CLEANUP_KEYSTORE"
fi

local_prop() {
  local file="$ROOT/local.properties"
  [[ -f "$file" ]] || return 0
  sed -n "s/^$1=//p" "$file" | head -1 | tr -d '\r'
}

STORE_FILE="${STORE_FILE:-$(local_prop release.storeFile)}"
STORE_PASSWORD="${ANDROID_KEYSTORE_PASSWORD:-$(local_prop release.storePassword)}"
KEY_ALIAS="${ANDROID_KEY_ALIAS:-$(local_prop release.keyAlias)}"
KEY_PASSWORD="${ANDROID_KEY_PASSWORD:-$(local_prop release.keyPassword)}"

if [[ ! -f "$STORE_FILE" || -z "$STORE_PASSWORD" || -z "$KEY_ALIAS" || -z "$KEY_PASSWORD" ]]; then
  cat >&2 <<'MSG'
Error: release signing material is incomplete.
Provide one of:
  A) CI secrets: ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
  B) local.properties: release.storeFile / release.storePassword / release.keyAlias / release.keyPassword
  C) env: ANDROID_RELEASE_STORE_FILE, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
MSG
  exit 2
fi

echo "==> Android release: env=$ENV_NAME version=$VERSION_NAME ($VERSION_CODE)"

GRADLE_ARGS=(
  :composeApp:assembleRelease
  :composeApp:bundleRelease
  -PandroidOnly=true
  -Papp.release=true
  "-Papp.env=$ENV_NAME"
  "-Papp.versionName=$VERSION_NAME"
  "-Papp.versionCode=$VERSION_CODE"
  "-Prelease.storeFile=$STORE_FILE"
  "-Prelease.storePassword=$STORE_PASSWORD"
  "-Prelease.keyAlias=$KEY_ALIAS"
  "-Prelease.keyPassword=$KEY_PASSWORD"
)

if [[ -x "$ROOT/scripts/with-gradle-lock.sh" ]]; then
  ./scripts/with-gradle-lock.sh ./gradlew "${GRADLE_ARGS[@]}"
else
  ./gradlew "${GRADLE_ARGS[@]}"
fi

# ------------------------------------------------------------------ outputs
shopt -s nullglob
APKS=("$ROOT"/composeApp/build/outputs/apk/release/*.apk)
AABS=("$ROOT"/composeApp/build/outputs/bundle/release/*.aab)
shopt -u nullglob

if (( ${#APKS[@]} == 0 )); then
  echo "Error: no release APK produced" >&2
  exit 1
fi

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(local_prop sdk.dir)}}"
APKSIGNER="$(ls -1 "$SDK"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
if [[ -z "$APKSIGNER" || ! -x "$APKSIGNER" ]]; then
  echo "Error: apksigner not found under $SDK/build-tools" >&2
  exit 1
fi

DIST="$ROOT/dist"
mkdir -p "$DIST"
BASE="MyKmpProject-${ENV_NAME}-${VERSION_NAME}-${VERSION_CODE}"

cp "${APKS[0]}" "$DIST/$BASE.apk"
"$APKSIGNER" verify --print-certs "$DIST/$BASE.apk" >/dev/null
echo "OK: signature verified -> $DIST/$BASE.apk"

if (( ${#AABS[@]} > 0 )); then
  cp "${AABS[0]}" "$DIST/$BASE.aab"
  echo "OK: bundle -> $DIST/$BASE.aab"
fi

(
  cd "$DIST"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$BASE".* > SHA256SUMS
  else
    shasum -a 256 "$BASE".* > SHA256SUMS
  fi
)

echo "==> Artifacts:"
ls -lh "$DIST/$BASE".* "$DIST/SHA256SUMS"
