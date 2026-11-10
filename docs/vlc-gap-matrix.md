# Netra Player vs VLC Android - gap matrix (T6)

Status labels: Implemented / Partial / Missing / Unverified. Implemented describes source presence, not device verification. This branch contains the baseline plus the extraction only; references to other PRs describe pending work, not features shipped by this branch. No VLC branding, assets or code is copied; this is a capability comparison only.

| Area | Status | Evidence |
|---|---|---|
| Local video playback (MP4/MKV/etc.) | Implemented | Media3 1.11.1, `PlaybackService` (MediaSessionService) |
| Local audio playback | Implemented | Baseline picker accepts MP3; widening is pending PR-A |
| Audio-only mode for videos | Partial (pending PR-B) | `AudioOnlyMode.kt`, per-item choice |
| 3D Stadium Sound (headphone effect) | Implemented | `HeadphoneEffect.kt`; Android Virtualizer support varies by device |
| Video audio playback, not file conversion | Partial | Existing background audio; explicit persisted mode is pending PR-B. No audio export/transcoding. |
| Containers/codecs breadth | Partial | Media3 extractors plus Android device decoders; no universal codec claim. Proposed PR-H adds a separate matrix. |
| Subtitles: external SRT | Implemented | picker + SubtitleConfiguration |
| Subtitles: VTT/SSA/ASS | Partial (pending PR-D) | verified against Media3 1.11.1 source |
| Subtitle sync-delay / styling | Missing | not implemented |
| Library browsing (folder) | Implemented | `MediaFolder.listFolderMedia` + paged list |
| Search / sort in folder | Partial (pending PR-E) | query + order toggle |
| Playlists (user-built) | Missing | documented in [PR-E] scope |
| Favorites | Partial (pending PR-E) | `Favorites.kt` |
| History / per-file resume | Partial (pending PR-E) | `ResumeStore.kt` (was single-item only) |
| Playback speed control | Unverified | not found in code scan; treat as Missing unless found |
| Gestures (brightness/seek/volume) | Implemented | `PlayerGestures.kt` + PlayerGesturesTest |
| Picture-in-picture | Implemented | `PipSupport.kt` + PipSupportTest |
| Background audio | Implemented | `PlaybackService` + BackgroundPlaybackTest |
| Equalizer / audio effects | Partial | Stadium Sound only; no multi-band EQ |
| Android TV | Partial | TV layout branches (`tv` flag); TvTest |
| Accessibility (TalkBack labels etc.) | Unverified | no dedicated audit yet |
| Network playback (HTTP streams, NAS/SMB) | Missing | SAF file/folder picking only; no network sources |
| Diagnostics / media info | Missing on this branch (pending PR-H) | `MediaDiagnostics.kt` + matrix doc |
| Chromecast | Missing | no cast code |

## Architecture refactor (incremental, no behavior change)
Step 1 (this PR): extract `UpdateEntryActivity` + `PlayerClockTicks` into `ui/` files; start the docs set. Next increments: track-choice dialog block, player controls row, home/recents card, then a ViewModel split. Each step ships only when CI is green.

## libVLC note
libVLC is considered ONLY as a separate proof-of-concept. VideoLAN lists the Android app as GPLv2-or-later (effectively GPLv3 with its Android libraries), and the LibVLC engine separately as LGPLv2. Any proof-of-concept must inspect the exact chosen artifacts, bundled modules and their license obligations first; do not conflate app and engine licenses. No integration in this wave.

## VLC comparison sources (read 2026-11-10)
- https://www.videolan.org/vlc/download-android.html documents VLC Android library/search/history, network streams and NAS, Android TV, subtitles, gestures, equalizer and software/hardware decoding. Those are comparison targets, not promises for Netra.
- https://github.com/videolan/vlc-android/blob/master/README.md documents LibVLC renderer casting and the distinct Android app / engine licenses.
- VLC capabilities not explicitly documented above (including detailed accessibility and per-feature behavior) remain unverified here. No VLC APK/device tests were run.
