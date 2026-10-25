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
# Immutable owning fixtures. Host and device sizes/digests are mandatory before tests.
for code in 90 91; do
  if [ "$code" = 90 ]; then v=2.2.10; sha=af034734bfa2b69f901a613b58bff0c9eeb8acf11e6caf4989cd94699b729d52; size=17687136; else v=2.2.11; sha=493ce43f8b9ba50e1540bfdf4bdb0cedef80a3ea66f7e6eef1f339099c6c3a6f; size=17687140; fi
  file=/tmp/bspn-proof-$code.apk
  curl --fail --location --retry 2 "https://github.com/prayagi-store-and-services/-Battery-Sentinel-Pro-Netra/releases/download/v$v/app-release.apk" -o "$file" || exit 1
  echo "$sha  $file" | sha256sum --check --strict || exit 1
  test "$(stat -c%s "$file")" = "$size" || exit 1
  adb push "$file" /data/local/tmp/ || exit 1
  adb shell sha256sum /data/local/tmp/bspn-proof-$code.apk | grep -F "$sha" || exit 1
  test "$(adb shell stat -c%s /data/local/tmp/bspn-proof-$code.apk | tr -d '\r')" = "$size" || exit 1
done
test_status=0
gradle --no-daemon :app:connectedDebugAndroidTest --stacktrace || test_status=$?
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
for name in small-tracks-top small-tracks-bottom fullscreen-hidden fullscreen-shown video-fit-before video-after-fit-click video-fit video-crop video-stretch gate-failure gate-before gate-after gate-update gate-about gate-rollback; do
  adb pull /data/local/tmp/player-$name.png app/build/device-screenshots/$name.png || true
done
adb pull /data/local/tmp/player-gate-failure.xml app/build/device-diagnostics/gate-failure.xml || true
for file in folder-failure-ui resize-diagnostic; do
  adb pull /sdcard/Android/data/com.prayagi.netraplayer/files/$file.txt app/build/device-diagnostics/$file.txt || true
done
adb pull /data/local/tmp/player-update-choice.png app/build/device-screenshots/update-choice.png || true
adb pull /data/local/tmp/player-update-installer.png app/build/device-screenshots/update-installer.png || true
adb shell run-as com.prayagi.netraplayer cat files/positive-installer-ui.txt > app/build/device-diagnostics/positive-installer-ui.txt || true
exit $test_status
