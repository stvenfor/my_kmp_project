#!/usr/bin/env bash
# Build, install, and launch Android debug on an emulator or device.
#
# Usage:
#   ./scripts/run-android.sh
#   ./scripts/run-android.sh -s emulator-5554
#   ./scripts/run-android.sh --avd Pixel_7_API_34
#   ./scripts/run-android.sh --assemble-only
#
# Env:
#   ANDROID_HOME / ANDROID_SDK_ROOT  SDK root (else local.properties sdk.dir)
#   ANDROID_SERIAL                   default adb device
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PKG="${ANDROID_APP_ID:-com.example.my_kmp_project}"
ACTIVITY="${ANDROID_ACTIVITY:-com.example.my_kmp_project.MainActivity}"
SERIAL=""
AVD=""
ASSEMBLE_ONLY=0
NO_BOOT=0
WAIT_SEC="${ANDROID_BOOT_WAIT:-120}"

usage() {
  awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
  exit 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s|--serial) SERIAL="$2"; shift 2 ;;
    --avd) AVD="$2"; shift 2 ;;
    --assemble-only) ASSEMBLE_ONLY=1; shift ;;
    --no-boot) NO_BOOT=1; shift ;;
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

resolve_sdk() {
  if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
    echo "$ANDROID_HOME"
    return
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" && -d "$ANDROID_SDK_ROOT" ]]; then
    echo "$ANDROID_SDK_ROOT"
    return
  fi
  if [[ -f "$ROOT/local.properties" ]]; then
    local dir
    dir="$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | head -1 | tr -d '\r' | sed 's/\\\\/\//g; s/\\:/:/g')"
    if [[ -n "$dir" && -d "$dir" ]]; then
      echo "$dir"
      return
    fi
  fi
  for cand in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    [[ -d "$cand" ]] && { echo "$cand"; return; }
  done
  echo ""
}

SDK="$(resolve_sdk)"
if [[ -z "$SDK" ]]; then
  echo "Error: Android SDK not found. Set ANDROID_HOME or sdk.dir in local.properties." >&2
  exit 1
fi
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
export PATH="$SDK/platform-tools:$SDK/emulator:$PATH"

ADB=(adb)
[[ -n "$SERIAL" ]] && ADB=(adb -s "$SERIAL")

echo "==> Android SDK: $SDK"
echo "==> Package: $PKG"

boot_emulator_if_needed() {
  if [[ "$NO_BOOT" -eq 1 ]]; then
    return
  fi
  if "${ADB[@]}" devices 2>/dev/null | awk 'NR>1 && $2=="device"{found=1} END{exit !found}'; then
    return
  fi
  local emu="$SDK/emulator/emulator"
  if [[ ! -x "$emu" ]]; then
    echo "Error: no adb device and emulator binary missing at $emu" >&2
    exit 1
  fi
  if [[ -z "$AVD" ]]; then
    AVD="$("$emu" -list-avds 2>/dev/null | head -1 || true)"
  fi
  if [[ -z "$AVD" ]]; then
    echo "Error: no AVD found. Create one in Android Studio or pass --avd NAME." >&2
    exit 1
  fi
  echo "==> Booting AVD: $AVD"
  "$emu" -avd "$AVD" -netdelay none -netspeed full >/tmp/kmp-android-emulator.log 2>&1 &
  local deadline=$((SECONDS + WAIT_SEC))
  until "${ADB[@]}" devices 2>/dev/null | awk 'NR>1 && $2=="device"{found=1} END{exit !found}'; do
    if (( SECONDS > deadline )); then
      echo "Error: emulator did not become ready within ${WAIT_SEC}s (see /tmp/kmp-android-emulator.log)" >&2
      exit 1
    fi
    sleep 2
  done
  "${ADB[@]}" wait-for-device
  # Wait until package manager is up
  deadline=$((SECONDS + WAIT_SEC))
  until "${ADB[@]}" shell getprop sys.boot_completed 2>/dev/null | grep -q 1; do
    if (( SECONDS > deadline )); then
      echo "Error: boot_completed timeout" >&2
      exit 1
    fi
    sleep 2
  done
}

boot_emulator_if_needed

echo "==> ./gradlew :composeApp:installDebug -PandroidOnly=true"
./gradlew :composeApp:installDebug -PandroidOnly=true

if [[ "$ASSEMBLE_ONLY" -eq 1 ]]; then
  echo "OK: installed (assemble-only; not launching)."
  exit 0
fi

echo "==> Launch $ACTIVITY"
"${ADB[@]}" shell am force-stop "$PKG" >/dev/null 2>&1 || true
"${ADB[@]}" shell am start -n "$PKG/$ACTIVITY" >/dev/null
echo "OK: Android app launched on $("${ADB[@]}" get-serialno 2>/dev/null || echo device)."
