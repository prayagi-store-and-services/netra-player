## 1.1.21 - Audio formats and video audio mode

- Choose common audio files as well as video. File support still depends on Media3 and the device's decoders.
- For a video, tap "Listen as Audio" to keep its sound playing without the picture. "Play Video" switches back. The choice is saved per file and respected across foreground/background and queue changes.
- Player controls wrap on narrow screens so Tracks stays accessible.
- 3D Stadium Sound reattaches when the player gets a new audio session. The effect remains device-dependent and headphone-only.
- Existing gesture and picture-in-picture improvements from 1.1.19/1.1.20 are included. No new permissions or network endpoints. Audio-mode choices are stored on this device, not uploaded.

## 1.1.20 - Gesture controls on the player

While the player controls are hidden:
- Swipe up or down on the left half for screen brightness, on the right half for volume.
- Swipe left or right to seek (a full-width swipe moves 4 minutes).
- Double-tap the left half to jump back 10 seconds, the right half to jump forward 10 seconds.
- Single tap brings the controls back; while the controls are visible their buttons work as before.
Brightness returns to the system setting when you leave the player. Nothing new is collected or sent.
## 1.1.19 - Picture in picture for video

- A "PiP" button appears on the player controls while a video file is playing, on phones that support picture-in-picture.
- Tap it to keep the video playing in a small floating window while you use other apps. The controls hide inside the small window; tap the window to use the system PiP controls.
- Audio-only files do not get PiP. Nothing new is collected or sent.

## 1.1.18 - Cleaner update notices

- Update checks now happen at most every 6 hours instead of once a day, so new versions are noticed sooner.
- Settings no longer shows the long static update explainer; the update card appears only when a newer version is verified.

## 1.1.17
Headphone 3D stadium sound. When headphones (wired, USB or Bluetooth) are connected, a "3D stadium sound" toggle appears on the Play screen. If the phone does not support the effect, the app says so instead of pretending. The effect turns itself off when headphones are unplugged. New permission: modify audio settings (needed for the sound effect; no other use).

## 1.1.16.10
Automatic newer-version popup and notification. Metadata is checked at most once daily, subject to Android battery/network limits, not instantly. Update opens this app page in compatible Netra Ecosystem or the Eco website. No manual update check, APK download, installation or unknown-source permission request in this app. Notification denial is respected.

## 1.1.16.9
Metadata-only newer-version notice and exact Player update page in compatible Netra Ecosystem or the Eco website. Player no longer downloads, pre-saves or installs APKs. Old updater jobs are inert.
