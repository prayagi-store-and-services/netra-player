#!/usr/bin/env bash
set -uo pipefail
for attempt in $(seq 1 60); do adb shell test -d /sdcard/Download && break; sleep 1; done
adb shell mkdir -p /sdcard/Download/PlayerFixture
adb push /tmp/player-fixtures/. /sdcard/Download/PlayerFixture/
# Preserve emulator/system evidence. Never dismiss ANRs or alter app assertions.
mkdir -p app/build/device-diagnostics
adb shell dumpsys activity lastanr > app/build/device-diagnostics/lastanr-before.txt 2>&1 || true
adb shell dumpsys meminfo > app/build/device-diagnostics/meminfo-before.txt 2>&1 || true
adb logcat -b all -v threadtime > app/build/device-diagnostics/logcat.txt 2>&1 &
logcat_pid=$!
for name in a-video.mp4 b-video.mp4; do
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/PlayerFixture/$name
done
test_status=0
gradle --no-daemon :app:connectedDebugAndroidTest --stacktrace || test_status=$?
# Host-driven revocation kills the old process before fresh instrumentation starts.
PKG=com.prayagi.netraplayer
TEST="$PKG.test/androidx.test.runner.AndroidJUnitRunner"
for permission in READ_MEDIA_VIDEO READ_MEDIA_AUDIO READ_MEDIA_VISUAL_USER_SELECTED; do
  adb shell pm revoke "$PKG" android.permission.$permission || true
done
adb shell pm grant "$PKG" android.permission.READ_MEDIA_VISUAL_USER_SELECTED
for state in partial revoked; do
  if [ "$state" = revoked ]; then adb shell pm revoke "$PKG" android.permission.READ_MEDIA_VISUAL_USER_SELECTED; fi
  adb shell am instrument -w -e class com.prayagi.netraplayer.LibraryPermissionStateTest -e libraryState "$state" "$TEST" > /tmp/library-$state-result.txt
  cat /tmp/library-$state-result.txt
  grep -q 'OK (1 test)' /tmp/library-$state-result.txt || test_status=1
done
adb shell dumpsys activity lastanr > app/build/device-diagnostics/lastanr-after.txt 2>&1 || true
adb shell dumpsys activity activities > app/build/device-diagnostics/activities-after.txt 2>&1 || true
adb shell dumpsys meminfo > app/build/device-diagnostics/meminfo-after.txt 2>&1 || true
adb shell dumpsys cpuinfo > app/build/device-diagnostics/cpuinfo-after.txt 2>&1 || true
adb shell dumpsys window > app/build/device-diagnostics/window-after.txt 2>&1 || true
kill "$logcat_pid" 2>/dev/null || true
wait "$logcat_pid" 2>/dev/null || true
mkdir -p app/build/device-screenshots
adb pull /data/local/tmp/player-home.png app/build/device-screenshots/home.png || true
adb pull /data/local/tmp/player-tracks.png app/build/device-screenshots/tracks.png || true
adb pull /data/local/tmp/player-background.png app/build/device-screenshots/background.png || true
adb pull /data/local/tmp/player-loaded.png app/build/device-screenshots/loaded.png || true
adb pull /data/local/tmp/player-dark.png app/build/device-screenshots/dark.png || true
adb pull /data/local/tmp/player-fullscreen.png app/build/device-screenshots/fullscreen.png || true
adb pull /data/local/tmp/player-failure.png app/build/device-screenshots/failure.png || true
adb pull /sdcard/Android/data/com.prayagi.netraplayer/files/failure-ui.txt app/build/device-screenshots/failure-ui.txt || true
adb pull /data/local/tmp/player-folder.png app/build/device-screenshots/folder.png || true
adb pull /data/local/tmp/player-subtitle.png app/build/device-screenshots/subtitle.png || true
adb pull /data/local/tmp/player-folder-failure.png app/build/device-screenshots/folder-failure.png || true
adb pull /data/local/tmp/player-folder-failure.xml app/build/device-screenshots/folder-failure.xml || true
for name in video-fit-before video-after-fit-click video-fit video-crop video-stretch gate-failure gate-before gate-after gate-update gate-about gate-rollback; do
  adb pull /data/local/tmp/player-$name.png app/build/device-screenshots/$name.png || true
done
adb pull /data/local/tmp/player-gate-failure.xml app/build/device-diagnostics/gate-failure.xml || true
for file in folder-failure-ui resize-diagnostic; do
  adb pull /sdcard/Android/data/com.prayagi.netraplayer/files/$file.txt app/build/device-diagnostics/$file.txt || true
done
for name in library-folders library-media library-failure library-partial library-revoked; do
  adb pull /data/local/tmp/player-$name.png app/build/device-screenshots/$name.png || true
done
exit $test_status
