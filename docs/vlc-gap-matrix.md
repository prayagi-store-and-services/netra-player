# Netra Player vs VLC Android - gap matrix (T6)

Source review: 16 November 2026, against main `f145411171c85d3f011627470f5d438d1cd01e27` (v1.1.22/code 38). Implemented means code is present, not that every device or codec was tested. Test names below identify coverage in the tree; this source review did not run those tests. Pending PRs are not shipped features. No VLC branding, assets or code is copied.

| Area | Status on this branch | Source / test evidence and limits |
|---|---|---|
| Local video playback | Implemented | `PlaybackService` uses Media3 1.11.1. Actual playback depends on container and device decoders. |
| Local audio discovery | Implemented | `MainActivity.isPlayableMedia` accepts audio/video MIME types, with extension fallback only for missing/generic MIME; `MediaFolder` uses it. `MediaScopeTest` covers format filtering, not successful decoding. |
| Explicit audio-only mode for videos | Implemented | `AudioOnlyMode`, MainActivity control and service track selection preserve per-item choice. `AudioOnlyModeTest`, `AudioOnlyPersistenceTest` and `AudioOnlyModeDeviceTest` exist. Physical-device acceptance is NOT VERIFIED in this review. |
| 3D Stadium Sound | Implemented, device-dependent | `HeadphoneEffect` uses Android Virtualizer, recreates it for a new audio session and releases it on shutdown. `HeadphoneEffectTest` covers policy. Unsupported devices show unavailable; no universal spatial-audio claim. |
| Video audio playback, not conversion | Implemented | Explicit audio-only choice plus background service. No audio export/transcoding. |
| Containers/codecs breadth | Partial | Media3 extractors plus Android decoders. Extension recognition does not prove decoding. Compatibility-matrix work in PR79 is pending. |
| External SRT subtitles | Implemented | MainActivity subtitle picker and SubtitleConfiguration; device rendering not tested in this review. |
| External VTT/SSA/ASS | Missing here | Proposed PR76 is unmerged; its Android CI is red. No compatibility claim from that pending work. |
| Subtitle delay / styling controls | Missing | No controls found in this branch. |
| Folder library browsing | Implemented | `listFolderMedia`, name-sorted paged list, selected SAF folder only, 1000-item limit. |
| Interactive folder search / sort choices | Missing here | Proposed PR77 is unmerged; its Android CI is red. Existing alphabetical ordering is not an interactive sort control. |
| User-built playlists | Missing | No playlist editor or persistent user playlist store found. |
| Favorites | Missing here | Proposed PR77 has a store/UI, not present in this branch. |
| History / per-file resume | Partial | `LastPlayed` remembers one last item. PR77 per-file store/recents remain pending. |
| Playback speed control | Unverified | No app-owned control found; default Media3 control behavior has not been tested here. |
| Brightness / seek / volume gestures | Implemented | `PlayerGestures` and `PlayerGesturesTest`; physical interaction not tested here. |
| Picture-in-picture | Implemented | `PipSupport`, MainActivity integration, `PipSupportTest`; OEM behavior not tested here. |
| Background audio | Implemented | `PlaybackService`/MediaSessionService; audio-only device fixture covers lifecycle paths, not all physical headset or OEM behavior. |
| Equalizer / audio effects | Partial | Stadium Sound only; no app-owned multi-band equalizer found. |
| Android TV | Partial | TV layout branches and `TvTest`; full D-pad/device acceptance not verified. |
| Accessibility | Unverified | No complete TalkBack/keyboard audit in this review. |
| Network playback / NAS / SMB | Missing | App-owned input is SAF file/folder picking; no stream-entry or network-share browser found. |
| Diagnostics / media info | Missing here | PR79 panel and compatibility doc are unmerged. Existing `PlaybackErrorsTest` covers error-message policy, not diagnostics UI. |
| Chromecast | Missing | No Cast integration found. |

## Architecture refactor

This PR moves only `UpdateEntryActivity` and `PlayerClockTicks` from MainActivity to `ui/PlayerClock.kt`. The package, declarations and bodies are unchanged from current main, including the existing 33 ms visible clock. The non-exported manifest entry keeps the same fully qualified class name. `PlayerClockLifecycleTest` and `PlayerVisualTest` already reference these declarations. This move does not improve the clock's battery cost, change playback or complete a ViewModel split.

No permission, manifest, storage, version or release change is added by this PR. The current audio-only and Stadium Sound implementations are retained. Further refactors require separate review and tests.

## VLC comparison and license boundaries

The official VLC Android page describes library browsing, network streams, Android TV, subtitles, gestures and equalizer support. Those are comparison points, not promises for Netra Player. This is a gap inventory, not VLC parity certification. No VLC APK/device tests were run.

The VLC Android repository describes the app as GPLv2-or-later, effectively GPLv3 with its Android libraries, and the LibVLC engine separately as LGPLv2. No LibVLC integration is included. Any separate proof-of-concept must inspect the exact artifacts, modules and license obligations before use.

Sources re-read 16 November 2026:
- https://www.videolan.org/vlc/download-android.html
- https://github.com/videolan/vlc-android/blob/master/README.md
