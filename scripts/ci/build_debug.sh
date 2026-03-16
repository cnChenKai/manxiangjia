#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT_DIR/build-logs"
mkdir -p "$LOG_DIR"

KOTLIN_LOG="$LOG_DIR/compileDebugKotlin.log"
ASSEMBLE_LOG="$LOG_DIR/assembleDebug.log"

cd "$ROOT_DIR"

./gradlew :data-local:compileDebugKotlin --stacktrace | tee "$KOTLIN_LOG"
./gradlew assembleDebug --stacktrace | tee "$ASSEMBLE_LOG"

APK_PATH=$(find "$ROOT_DIR" -path "*/build/outputs/apk/debug/*.apk" | head -n 1 || true)

if [[ -z "$APK_PATH" ]]; then
  echo "No debug APK found. Checked */build/outputs/apk/debug/*.apk"
  exit 1
fi

echo "Debug APK: $APK_PATH"
echo "Build logs: $LOG_DIR"
