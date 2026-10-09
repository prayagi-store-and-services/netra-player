# Security notes - Netra Player

## Background audio (v1.1.12)
- Playback lives in an Android MediaSessionService. Locking the screen/backgrounding disables the video track while audio keeps playing. Media3 supplies media controls in the notification. Audio focus is respected; unplugging headphones pauses playback.
- New library: androidx.media3:media3-session 1.11.1. New normal permissions: FOREGROUND_SERVICE and FOREGROUND_SERVICE_MEDIA_PLAYBACK. Existing notification permission is explained in Settings.
- The session accepts this app and Android-trusted media controllers, not arbitrary apps. No file content is uploaded and no new remote endpoint is added.
- Service stops/release behaviour follows Media3 when playback is no longer ongoing. Device audio/notification behaviour must be checked separately from compile/unit checks.


## Track choices (v1.1.11)
- Audio and subtitle choices list only supported tracks returned by Media3 for the selected file. Unknown languages are labelled unavailable, never guessed. Automatic selection and subtitles off are available.
- No new permission, dependency, network request or stored data. Media files stay on the phone.
- Release requires green build/tests. Device validation is separate from build checks; no device crash-free percentage is claimed.


## Downloads fix and download list (v1.1.10)
- Download progress now counts up (percent downloaded) and the time left no longer rises. A finished update file stays in a Downloads list with Install and Delete, and is deleted automatically once that version is installed. Partial or unknown files are cleaned.
- Files stay in the app's private cache folder only. No new permission, library or network call.

## Update alert (v1.1.9)
- Every 6 hours (network needed) the app checks its own public GitHub release and, when a newer version exists, shows one notification. Tapping it downloads the build, checks size and SHA-256, and opens the Android installer. The notification is silent.
- New permission: POST_NOTIFICATIONS (asked once on Android 13+; if refused, no notification is shown). New library: AndroidX WorkManager work-runtime-ktx 2.10.0. No new server, no personal data.

## Clearer playback errors (v1.1.8)
- When a file cannot be played, the message now says the real reason Android reports: file not found, permission lost, format not supported by this phone, damaged file, or too demanding for the phone. Other errors keep the old generic message. Nothing is guessed.
- No new permission, library or network call. The message is shown on screen only; nothing is sent anywhere.

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

## Festival banner (version 1.1.6)

- A card at the top of the app shows today's festival (India calendar, bundled in the app, from timeanddate.com India 2026-2027) or "coming soon" for a festival within 3 days, with the live date and time. India's Independence Day (15 August) is shown too. It has no death anniversaries and no other country's days. After 2027 there is no data, so no banner is shown and nothing is invented. A date marked "may differ by a day" says so.
- It works offline. No new permission, network call or library.

## Real video player screen (version 1.1.7)
- With no file chosen, the big empty black box is gone. You see one clear "Open video or MP3 file" button and, if you played something before, a "Continue" button with the real saved position.
- Once a file is open, the video shows at 16:9 with the standard Media3 controls: play and pause, a seek bar, current time and total length, back and forward jump buttons, and a full-screen button. The controls hide after 4 seconds and come back on a tap.
- Full screen hides the header, footer and banner, turns the phone to landscape and hides the system bars (swipe to see them). Back or the full-screen button leaves full screen. Playback continues through the turn.
- An MP3 shows "Audio" and the file name in the player area. A file with no video track is detected from the file itself, not guessed.
- No new permission, library or network call. The activity now handles screen turns itself (configChanges) so playback is not restarted when the phone turns.

## Update input hardening (version 1.1.12)
- Notification-triggered downloads use a non-exported activity reached by the app's immutable PendingIntent. The exported launcher ignores update extras from other apps.
- Update metadata is limited to 1 MiB while reading, not after allocating the complete response. Three-part and four-part numeric release tags are accepted; path separators and extra components are refused.
- Shared Firestore rules, server rate limits and FormSubmit controls require separate server evidence. Local once-per-day counter preferences are not a server security control.
- Dependency vulnerability alerts were inactive at audit time. No claim of zero vulnerable dependencies is made from that state.
