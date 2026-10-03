# Security notes - Netra Player

Netra Player is only a video and music player. It plays files that you choose on your phone. Files are opened through the Android file picker and are never uploaded.

## What the app connects to
- `https://github.com/prayagi-store-and-services/netra-player/releases/latest/download/latest.json` and the release APK, only for the "Check for update" button and the in-app update. The update is downloaded and then installed by you with the normal Android installer.
- Firestore REST (public project netra-ai-jan): once a day it adds +1 to the counter `netra_active/netra-player_<yyyyMMdd>`. No user ID, no location, no files, no device data. It is on by default and can be switched off in About.

## Permissions
- INTERNET: update check and the usage count.
- REQUEST_INSTALL_PACKAGES: installing an update you tapped.

## Update safety
- The updater only accepts releases from the prayagi-store-and-services organization, a valid vX.Y.Z tag and a 64-character sha256, and checks the downloaded size.
- Installer files are deleted after the update is installed or cancelled.

## Dependencies
- AndroidX Media3 (exoplayer, ui) 1.11.1.

No keys or tokens are stored in the app. Report problems through the Netra website contact form.
