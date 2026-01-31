#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AVD_NAME="focaccia_test"

# Kill any existing emulator
if adb devices 2>/dev/null | grep -q emulator; then
    echo "==> Killing existing emulator..."
    adb emu kill 2>/dev/null || true
    sleep 3
fi

# Launch emulator
echo "==> Starting emulator ($AVD_NAME)..."
emulator -avd "$AVD_NAME" -no-audio -no-snapshot -gpu swiftshader_indirect &
EMULATOR_PID=$!

# Wait for boot
echo "==> Waiting for device..."
adb wait-for-device
echo "==> Waiting for boot to complete..."
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
    sleep 3
done
echo "==> Boot complete."

# Build, install, launch
"$SCRIPT_DIR/deploy.sh"

echo ""
echo "Ready. Emulator PID: $EMULATOR_PID"
echo "To stop: adb emu kill"
