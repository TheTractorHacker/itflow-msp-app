#!/usr/bin/env bash
# Installs the MINIFIED release build on a running emulator/device and launches it. Fails if the
# process dies or logs a fatal exception. Debug builds are not minified, so R8 problems such as the
# v1.32.0 startup crash (WorkDatabase constructor stripped) only show up here.
#
#   scripts/release-smoke.sh [path-to-app-release-unsigned.apk]
#
# Needs: adb on PATH (or ANDROID_HOME), build-tools (zipalign, apksigner), a booted device.
set -euo pipefail
cd "$(dirname "$0")/.."

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
ADB="${ADB:-$(command -v adb || echo "$SDK/platform-tools/adb")}"
BT="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)"
APK="${1:-app/build/outputs/apk/release/app-release-unsigned.apk}"
PKG=com.foleyit.itflow
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

[ -f "$APK" ] || { echo "No release APK at $APK (run ./gradlew assembleRelease first)"; exit 2; }
"$BT/zipalign" -f 4 "$APK" "$TMP/aligned.apk"
# Throwaway signature: this only proves the build starts, it is never published.
"$BT/apksigner" sign --ks app/debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out "$TMP/signed.apk" "$TMP/aligned.apk"

"$ADB" wait-for-device
"$ADB" uninstall "$PKG" >/dev/null 2>&1 || true
"$ADB" install -r "$TMP/signed.apk" >/dev/null
"$ADB" logcat -c
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 12

if ! "$ADB" shell pidof "$PKG" >/dev/null; then
  echo "FAIL: $PKG is not running after launch"; "$ADB" logcat -d -b crash | grep -v crash_dump | head -40; exit 1
fi
if "$ADB" logcat -d -b crash | grep -q "FATAL EXCEPTION.*\|Process: $PKG"; then
  echo "FAIL: fatal exception in the release build"; "$ADB" logcat -d -b crash | head -40; exit 1
fi
echo "OK: minified release build launched and stayed up"
