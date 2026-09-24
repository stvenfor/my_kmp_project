#!/usr/bin/env bash
# Build and launch the iOS app on the Simulator.
#
# Usage:
#   ./scripts/run-ios.sh
#   ./scripts/run-ios.sh --device "iPhone 16"
#   ./scripts/run-ios.sh --udid <SIMULATOR_UDID>
#   ./scripts/run-ios.sh --build-only
#
# Env:
#   IOS_SIM_DEVICE   default simulator name (default: first available iPhone)
#   IOS_BUNDLE_ID    override bundle id (default from Config.xcconfig)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PROJECT="$ROOT/iosApp/iosApp.xcodeproj"
SCHEME="${IOS_SCHEME:-iosApp}"
CONFIG="${IOS_CONFIGURATION:-Debug}"
DEVICE_NAME="${IOS_SIM_DEVICE:-}"
UDID=""
BUILD_ONLY=0
DERIVED="$ROOT/build/ios-sim"

usage() {
  awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
  exit 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE_NAME="$2"; shift 2 ;;
    --udid) UDID="$2"; shift 2 ;;
    --build-only) BUILD_ONLY=1; shift ;;
    -h|--help) usage ;;
    -*)
      echo "Unknown option: $1" >&2
      usage
      ;;
    *)
      echo "Unexpected argument: $1" >&2
      usage
      ;;
  esac
done

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Error: iOS simulator scripts require macOS." >&2
  exit 1
fi
if [[ ! -d "$PROJECT" ]]; then
  echo "Error: missing $PROJECT" >&2
  exit 1
fi

resolve_bundle_id() {
  if [[ -n "${IOS_BUNDLE_ID:-}" ]]; then
    echo "$IOS_BUNDLE_ID"
    return
  fi
  local cfg="$ROOT/iosApp/Configuration/Config.xcconfig"
  local id
  id="$(sed -n 's/^PRODUCT_BUNDLE_IDENTIFIER=//p' "$cfg" | head -1 | tr -d '\r')"
  # TEAM_ID is empty in Config.xcconfig by default
  id="${id//\$(TEAM_ID)/}"
  echo "$id"
}

BUNDLE_ID="$(resolve_bundle_id)"

pick_simulator() {
  if [[ -n "$UDID" ]]; then
    echo "$UDID"
    return
  fi
  if [[ -n "$DEVICE_NAME" ]]; then
    local id
    id="$(xcrun simctl list devices available 2>/dev/null \
      | awk -v name="$DEVICE_NAME" '
          $0 ~ name {
            if (match($0, /\(([A-F0-9-]{36})\)/)) {
              print substr($0, RSTART+1, RLENGTH-2)
              exit
            }
          }')"
    if [[ -z "$id" ]]; then
      echo "Error: simulator named '$DEVICE_NAME' not found (available iPhones below)." >&2
      xcrun simctl list devices available 2>/dev/null | grep -E "iPhone" >&2 || true
      exit 1
    fi
    echo "$id"
    return
  fi
  # Prefer an already-booted iPhone, else first available iPhone.
  local booted
  booted="$(xcrun simctl list devices available 2>/dev/null \
    | awk '/iPhone/ && /(Booted)/ {
        if (match($0, /\(([A-F0-9-]{36})\)/)) {
          print substr($0, RSTART+1, RLENGTH-2)
          exit
        }
      }')"
  if [[ -n "$booted" ]]; then
    echo "$booted"
    return
  fi
  xcrun simctl list devices available 2>/dev/null \
    | awk '/iPhone/ {
        if (match($0, /\(([A-F0-9-]{36})\)/)) {
          print substr($0, RSTART+1, RLENGTH-2)
          exit
        }
      }'
}

SIM_UDID="$(pick_simulator)"
if [[ -z "$SIM_UDID" ]]; then
  echo "Error: no available iPhone simulator. Create one in Xcode → Settings → Platforms." >&2
  exit 1
fi

SIM_NAME="$(xcrun simctl list devices 2>/dev/null | grep "$SIM_UDID" | head -1 | sed -E 's/^[[:space:]]+//; s/ \(.*//')"
echo "==> Simulator: ${SIM_NAME:-unknown} ($SIM_UDID)"
echo "==> Bundle: $BUNDLE_ID"
echo "==> Scheme: $SCHEME ($CONFIG)"

STATE="$(xcrun simctl list devices 2>/dev/null | grep "$SIM_UDID" | head -1 || true)"
if ! grep -q "(Booted)" <<<"$STATE"; then
  echo "==> Booting simulator"
  open -a Simulator >/dev/null 2>&1 || true
  xcrun simctl boot "$SIM_UDID" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$SIM_UDID" -b
fi

DEST="platform=iOS Simulator,id=$SIM_UDID"
mkdir -p "$DERIVED"

echo "==> xcodebuild (embeds ComposeApp via Gradle run script)"
xcodebuild \
  -project "$PROJECT" \
  -scheme "$SCHEME" \
  -configuration "$CONFIG" \
  -destination "$DEST" \
  -derivedDataPath "$DERIVED" \
  -quiet \
  build

APP="$(find "$DERIVED/Build/Products" -type d -name '*.app' | head -1)"
if [[ -z "$APP" || ! -d "$APP" ]]; then
  echo "Error: built .app not found under $DERIVED/Build/Products" >&2
  exit 1
fi
echo "==> App: $APP"

if [[ "$BUILD_ONLY" -eq 1 ]]; then
  echo "OK: built (build-only; not installing)."
  exit 0
fi

echo "==> Install + launch"
xcrun simctl uninstall "$SIM_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
xcrun simctl install "$SIM_UDID" "$APP"
xcrun simctl launch "$SIM_UDID" "$BUNDLE_ID" >/dev/null
echo "OK: iOS app launched on ${SIM_NAME:-$SIM_UDID}."
