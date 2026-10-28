# Netra Player

Android local video and MP3 player by Prayagi Team, part of [Netra Ecosystem](https://prayagi-store-and-services.github.io/netra-eco/).

## Public release: v1.1.16.8

[Release v1.1.16.8](https://github.com/prayagi-store-and-services/netra-player/releases/tag/v1.1.16.8), versionCode 30, is the current published build checked for this description.

- Open a video or MP3 file through Android's file picker, or browse a selected folder with previous/next queue controls.
- Continue the last selected file from its saved position. Playback runs in a Media3 service, with Android media controls and background audio subject to Android limits.
- Choose supported audio/subtitle tracks and open an external `.srt` subtitle file. Unsupported files or tracks show an error or Unavailable; codec support depends on the device.
- Local playback works offline. No login is required. Updates and optional crash reports or usage counts have separate network paths; local playback does not mean the entire app has no network activity.
- The published v1.1.16.8 still contains the older update check and verified APK background-download path. Android confirmation is required to install. Do not describe it as Eco-only updating.

## Unreleased main: v1.1.16.9

The newer source changes updating to a metadata-only notice and an exact Player page handoff in compatible Netra Ecosystem or on its website. Legacy updater jobs are inert in that source. These changes are not presented as shipped until its signed release is published and verified. This README change does not publish an APK.

## Description maintenance

Whenever a feature or architecture changes, update the app/store/site/Eco description, README and release notes against the released source. Keep unreleased work, automated checks and physical-device validation separate. No phone-testing, codec-coverage or performance result is claimed by this description.

## Build

Kotlin, Jetpack Compose and AndroidX Media3 ExoPlayer. `./gradlew testDebugUnitTest assembleRelease`.

Use reviewable PRs and green checks at the exact reviewed head, not direct main pushes. Audit the signed APK's identity, certificate and contents before publication. See SECURITY.md and WHATS_NEW.md for the release and data boundaries.
