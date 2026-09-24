#!/usr/bin/env bash
# Publish KMP OHOS binaries, assemble HAP, install and launch on Harmony emulator/device.
# Thin wrapper around runscript/runOhosApp-Mac.sh (Windows: runOhosApp-Win.bat).
#
# Usage:
#   ./scripts/run-harmony.sh
#   ./scripts/run-harmony.sh ohosArm64 127.0.0.1:5555
#   ./scripts/run-harmony.sh -m release
#   ./scripts/run-harmony.sh -p /absolute/path/to/harmonyApp
#
# Env:
#   DEVECO_PATH   DevEco Studio.app (default: /Applications/DevEco-Studio.app)
#   All flags are forwarded to runscript/runOhosApp-Mac.sh — run with -h for details.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" == "Darwin" ]]; then
  exec "$ROOT/runscript/runOhosApp-Mac.sh" "$@"
fi

if [[ -f "$ROOT/runscript/runOhosApp-Win.bat" ]]; then
  echo "On Windows use: runscript\\runOhosApp-Win.bat $*" >&2
  exit 1
fi

echo "Error: unsupported host OS for Harmony run script." >&2
exit 1
