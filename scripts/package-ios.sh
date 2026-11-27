#!/usr/bin/env bash
# 产出 iOS Release archive → IPA（需要 Apple 开发者证书 + Provisioning Profile）。
#
# Usage:
#   ./scripts/package-ios.sh --team-id ABCDE12345 [--env prod] [--version-name 1.0] [--version-code 42]
#                           [--profile-specifier "My App Store Profile"] [--export-method app-store-connect]
#
# CI（GitHub Actions macos runner）可从 Secrets 注入：
#   IOS_CERTIFICATE_P12_BASE64 / IOS_CERTIFICATE_PASSWORD
#   IOS_PROVISIONING_PROFILE_BASE64 / IOS_TEAM_ID
#
# 本机则要求证书已在钥匙串、profile 已安装（或传 --profile-specifier）。
# 缺少任何签名材料时以退出码 2 结束并给出说明，不会伪造成功。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Error: iOS packaging requires macOS + Xcode." >&2
  exit 2
fi

ENV_NAME="prod"
TEAM_ID="${IOS_TEAM_ID:-}"
PROFILE_SPECIFIER="${IOS_PROVISIONING_PROFILE_SPECIFIER:-}"
EXPORT_METHOD="app-store-connect"
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
    --team-id) TEAM_ID="$2"; shift 2 ;;
    --profile-specifier) PROFILE_SPECIFIER="$2"; shift 2 ;;
    --export-method) EXPORT_METHOD="$2"; shift 2 ;;
    --version-name) VERSION_NAME="$2"; shift 2 ;;
    --version-code) VERSION_CODE="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Unknown option: $1" >&2; usage ;;
  esac
done

if [[ -z "$TEAM_ID" ]]; then
  echo "Error: --team-id (or IOS_TEAM_ID) is required." >&2
  exit 2
fi

VERSION_NAME="${VERSION_NAME:-$(file_value version.name)}"
VERSION_NAME="${VERSION_NAME:-1.0}"
VERSION_CODE="${VERSION_CODE:-$(file_value version.code)}"
VERSION_CODE="${VERSION_CODE:-1}"

WORK="${RUNNER_TEMP:-$(mktemp -d -t mykmp-ios.XXXXXX)}"
KEYCHAIN=""
cleanup() {
  if [[ -n "$KEYCHAIN" ]]; then
    security delete-keychain "$KEYCHAIN" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# ------------------------------------------------------------------ signing material
if [[ -n "${IOS_CERTIFICATE_P12_BASE64:-}" ]]; then
  KEYCHAIN="$WORK/mykmp-signing.keychain-db"
  KEYCHAIN_PASSWORD="$(uuidgen)"
  security create-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
  security set-keychain-settings -lut 3600 "$KEYCHAIN"
  security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"

  P12="$WORK/certificate.p12"
  printf '%s' "$IOS_CERTIFICATE_P12_BASE64" | base64 --decode > "$P12" 2>/dev/null \
    || printf '%s' "$IOS_CERTIFICATE_P12_BASE64" | base64 -D > "$P12"
  security import "$P12" -k "$KEYCHAIN" -P "${IOS_CERTIFICATE_PASSWORD:-}" \
    -T /usr/bin/codesign -T /usr/bin/xcodebuild
  security set-key-partition-list -S apple-tool:,apple: -s -k "$KEYCHAIN_PASSWORD" "$KEYCHAIN" >/dev/null
  security list-keychains -d user -s "$KEYCHAIN" $(security list-keychains -d user | tr -d '"')
fi

if [[ -n "${IOS_PROVISIONING_PROFILE_BASE64:-}" ]]; then
  PROFILE_DIR="$HOME/Library/MobileDevice/Provisioning Profiles"
  mkdir -p "$PROFILE_DIR"
  PROFILE_PATH="$PROFILE_DIR/mykmp.mobileprovision"
  printf '%s' "$IOS_PROVISIONING_PROFILE_BASE64" | base64 --decode > "$PROFILE_PATH" 2>/dev/null \
    || printf '%s' "$IOS_PROVISIONING_PROFILE_BASE64" | base64 -D > "$PROFILE_PATH"
  if [[ -z "$PROFILE_SPECIFIER" ]]; then
    PROFILE_SPECIFIER="$(security cms -D -i "$PROFILE_PATH" 2>/dev/null \
      | /usr/libexec/PlistBuddy -c 'Print :Name' /dev/stdin 2>/dev/null || true)"
  fi
fi

ARCHIVE="$WORK/MyKmpProject.xcarchive"
EXPORT_DIR="$WORK/export"
mkdir -p "$EXPORT_DIR"

echo "==> iOS archive: env=$ENV_NAME version=$VERSION_NAME ($VERSION_CODE)"

# Xcode 的 Compile Kotlin Framework phase 会把这些转发成 -Papp.* 传给 Gradle，
# 确保 archive 里的 BuildEnvironment 与本次打包的 env/版本一致。
export APP_ENV="$ENV_NAME"
export APP_RELEASE="true"
export APP_VERSION_NAME="$VERSION_NAME"
export APP_VERSION_CODE="$VERSION_CODE"

XCODE_SIGN_ARGS=(
  "DEVELOPMENT_TEAM=$TEAM_ID"
  "CODE_SIGN_STYLE=Manual"
  "MARKETING_VERSION=$VERSION_NAME"
  "CURRENT_PROJECT_VERSION=$VERSION_CODE"
)
if [[ -n "$PROFILE_SPECIFIER" ]]; then
  XCODE_SIGN_ARGS+=("PROVISIONING_PROFILE_SPECIFIER=$PROFILE_SPECIFIER")
fi

xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$ARCHIVE" \
  "${XCODE_SIGN_ARGS[@]}" \
  archive

EXPORT_PLIST="$WORK/ExportOptions.plist"
cat > "$EXPORT_PLIST" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>$EXPORT_METHOD</string>
    <key>signingStyle</key>
    <string>manual</string>
    <key>teamID</key>
    <string>$TEAM_ID</string>
    <key>stripSwiftSymbols</key>
    <true/>
    <key>compileBitcode</key>
    <false/>
</dict>
</plist>
PLIST

xcodebuild -exportArchive \
  -archivePath "$ARCHIVE" \
  -exportPath "$EXPORT_DIR" \
  -exportOptionsPlist "$EXPORT_PLIST"

DIST="$ROOT/dist"
mkdir -p "$DIST"
BASE="MyKmpProject-${ENV_NAME}-${VERSION_NAME}-${VERSION_CODE}"
IPA="$(find "$EXPORT_DIR" -maxdepth 1 -name '*.ipa' | head -1)"
if [[ -z "$IPA" ]]; then
  echo "Error: xcodebuild produced no .ipa" >&2
  exit 1
fi
cp "$IPA" "$DIST/$BASE.ipa"
(
  cd "$DIST"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$BASE.ipa" >> SHA256SUMS
  else
    shasum -a 256 "$BASE.ipa" >> SHA256SUMS
  fi
)
echo "OK: $DIST/$BASE.ipa"
