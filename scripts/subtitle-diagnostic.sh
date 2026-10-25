#!/usr/bin/env bash
set -uo pipefail
mkdir -p app/build/device-diagnostics
adb logcat -b all -v threadtime > app/build/device-diagnostics/subtitle-logcat.txt 2>&1 &
logpid=$!
status=0
# A bounded diagnostic, never a replacement for the original full release gate.
gradle --no-daemon :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.prayagi.netraplayer.PlayerSubtitleDiagnosticTest,com.prayagi.netraplayer.PlayerVisualTest#trackSelectionRequiresApplyAndCancelPreservesPlayback' --stacktrace > app/build/device-diagnostics/subtitle-gradle.txt 2>&1 || status=$?
kill "$logpid" 2>/dev/null || true
wait "$logpid" 2>/dev/null || true
adb shell dumpsys activity lastanr > app/build/device-diagnostics/subtitle-lastanr.txt 2>&1 || true
adb shell uiautomator dump /data/local/tmp/subtitle-final.xml || true
adb pull /data/local/tmp/subtitle-final.xml app/build/device-diagnostics/ || true
adb pull /sdcard/Android/data/com.prayagi.netraplayer/files/subtitle-diagnostics app/build/device-diagnostics/ || true
printf 'targeted diagnostic exit=%s\n' "$status"
exit "$status"
