#!/usr/bin/env bash
set -uo pipefail
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
for name in gate-before gate-after gate-update gate-about gate-rollback; do
  adb pull /data/local/tmp/player-$name.png app/build/device-screenshots/$name.png || true
done
exit $test_status
