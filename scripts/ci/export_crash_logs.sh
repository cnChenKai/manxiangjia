#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <applicationId> [outputDir]"
  exit 1
fi

APP_ID="$1"
OUT_DIR="${2:-build-logs/device-crash-logs}"
mkdir -p "$OUT_DIR"

adb shell run-as "$APP_ID" ls files/crash-logs > /dev/null
adb shell run-as "$APP_ID" tar -cf /data/local/tmp/crash-logs.tar -C files crash-logs
adb pull /data/local/tmp/crash-logs.tar "$OUT_DIR/crash-logs.tar"
adb shell rm /data/local/tmp/crash-logs.tar

echo "Exported crash logs to $OUT_DIR/crash-logs.tar"
