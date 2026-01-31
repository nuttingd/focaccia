#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.focaccia.app"
SERVICE="$PACKAGE/$PACKAGE.AppBlockerAccessibilityService"

echo "==> Building debug APK..."
./gradlew assembleDebug -q

echo "==> Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "==> Enabling accessibility service..."
adb shell settings put secure enabled_accessibility_services "$SERVICE"
adb shell settings put secure accessibility_enabled 1

echo "==> Launching Focaccia..."
adb shell am start -n "$PACKAGE/.MainActivity"

echo "==> Done."
