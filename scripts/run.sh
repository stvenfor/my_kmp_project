#!/usr/bin/env bash
# Dispatch to platform run scripts.
#
# Usage:
#   ./scripts/run.sh android [args...]
#   ./scripts/run.sh ios [args...]
#   ./scripts/run.sh harmony [args...]
#
# Examples:
#   ./scripts/run.sh android --avd Pixel_7_API_34
#   ./scripts/run.sh ios --device "iPhone 16"
#   ./scripts/run.sh harmony ohosArm64 127.0.0.1:5555
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

usage() {
  awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
  exit 0
}

if [[ $# -lt 1 ]]; then
  usage
fi

platform="$1"
shift || true

case "$platform" in
  -h|--help) usage ;;
  android|and|a) exec "$ROOT/scripts/run-android.sh" "$@" ;;
  ios|i) exec "$ROOT/scripts/run-ios.sh" "$@" ;;
  harmony|ohos|h) exec "$ROOT/scripts/run-harmony.sh" "$@" ;;
  *)
    echo "Unknown platform: $platform (use android|ios|harmony)" >&2
    usage
    ;;
esac
