# Security notes - Netra Player

Netra Player is only a video and music player. It plays files that you choose on your phone. Files are opened through the Android file picker and are never uploaded.

## Standard header (v1.1.1)
The header is the Netra standard: 56 dp, fixed, only the app name, the installed version and the live date and time. Everything else scrolls; only this header and the bottom bar stay fixed. No new permission, network call or library. Every shown value needs a real evidence source, otherwise "Unavailable".

## What the app connects to
- `https://github.com/prayagi-store-and-services/netra-player/releases/latest/download/latest.json` and the release APK, only for the "Check for update" button and the in-app update. The update is downloaded and then installed by you with the normal Android installer.
- Firestore REST (public project netra-ai-jan): once a day it adds +1 to the counter `netra_active/netra-player_<yyyyMMdd>`. No user ID, no location, no files, no device data. It is on by default and can be switched off in About.

## Permissions
- INTERNET: update check and the usage count.
- REQUEST_INSTALL_PACKAGES: installing an update you tapped.

## Version in the header and "What's new" (v1.1.0)
- The header now shows the installed version (read from Android's package info; "Unavailable" if Android does not return it). When "Check for update" finds a newer version, the release notes written for that version are shown before you tap Update. Notes come only from the release file in this repository's own releases; if there are none, nothing is shown. No new permission, no new network call, no new library.
- Rule for all Netra apps: every datum shown must be backed by real evidence; when none is available the app shows "Unavailable" and nothing is made up.

## Android TV (v1.0.1)
- The app also installs on Android TV (leanback launcher, no touchscreen needed). This adds no permission. It is built and unit-tested in CI but not yet tried on a real TV by us.

## Update safety
- The updater only accepts releases from the prayagi-store-and-services organization, a valid vX.Y.Z tag and a 64-character sha256, and checks the downloaded size.
- Installer files are deleted after the update is installed or cancelled.

## Dependencies
- AndroidX Media3 (exoplayer, ui) 1.11.1.

No keys or tokens are stored in the app. Report problems through the Netra website contact form.

## Crash report (version 1.1.2)

- If the app crashes, a short report is saved on the device. Nothing is sent by itself. The "Crash report" card has a "Send crash report" button: it first shows the exact text (app, app version, phone model, Android version, the crash trace with class names and code locations only, no exception messages) and sends only if you tap Send; "Share instead" lets you pick any app. If no crash is saved it says Unavailable. A send counts as done only when the forwarding service (formsubmit.co) confirms it. No name, email, location, files or device ID is included.

## Video and MP3 only (version 1.1.3)

- The file picker now offers video files and MP3 files only (before, it offered every audio type). A file that is picked from elsewhere is checked by its type, with the .mp3 name used when the phone gives no type, and anything else is refused with a message.
- No new permission, network call or library. Files are still opened through Android's own picker and never uploaded.

## Home screen widget and "Continue" (version 1.1.4)

- New home screen widget "Netra Player - Continue": shows the last video or MP3 you played and where it stopped, with the time it was saved. If nothing was played yet it shows "Unavailable". The widget has no timer and no background work: the app asks it to redraw only when a new position is saved (when you pick a file and when the app leaves the screen).
- New local storage (this phone only, not backed up because backup is off): the address Android gave for the file, its name, the stop position and the save time. Nothing is sent anywhere.
- The file picker now keeps read access to the file you picked (Android's "persistable" permission, requested through the same picker, no new app permission), so the "Continue" button can reopen it. If Android no longer allows it, the app says so and asks you to pick the file again.
- No new app permission, network call or library.

## Permissions list (version 1.1.5)

- New "Permissions" card on the Update screen: lists each permission the app uses (Internet, Install apps), the plain reason, and the live status read from Android when you open the screen (no timer). Tapping "Install apps" opens the Android page where you can allow or stop it. Internet is a normal permission that cannot be switched off from there, so it is shown as always allowed.
- No new permission, network call or library.
