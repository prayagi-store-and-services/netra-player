#!/usr/bin/env bash
set -uo pipefail
for attempt in $(seq 1 60); do adb shell test -d /sdcard/Download && break; sleep 1; done
adb shell mkdir -p /sdcard/Download/PlayerFixture
adb push /tmp/player-fixtures/. /sdcard/Download/PlayerFixture/
test_status=0
gradle --no-daemon :app:connectedDebugAndroidTest --stacktrace || test_status=$?
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
exit $test_status
