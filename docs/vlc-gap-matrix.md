# Netra Player vs VLC Android - gap matrix (T6)

Status labels: Implemented / Partial / Missing / Unverified - each with code or test evidence. No VLC branding, assets or code is copied; this is a capability comparison only.

| Area | Status | Evidence |
|---|---|---|
| Local video playback (MP4/MKV/etc.) | Implemented | Media3 1.11.1, `PlaybackService` (MediaSessionService) |
| Local audio playback | Implemented | picker accepts audio; [PR-A] widened formats |
| Audio-only mode for videos | Implemented [PR-B] | `AudioOnlyMode.kt`, per-item choice |
| 3D Stadium Sound (headphone effect) | Implemented | `HeadphoneEffect.kt` (USP; not in VLC) |
| Video-to-audio conversion | Implemented | USP feature (not in VLC mobile) |
| Containers/codecs breadth | Partial | Media3 extractors; no libVLC software codecs (DTS etc. unsupported) - see docs/compatibility-matrix.md |
| Subtitles: external SRT | Implemented | picker + SubtitleConfiguration |
| Subtitles: VTT/SSA/ASS | Implemented [PR-D] | verified against Media3 1.11.1 source |
| Subtitle sync-delay / styling | Missing | not implemented |
| Library browsing (folder) | Implemented | `MediaFolder.listFolderMedia` + paged list |
| Search / sort in folder | Implemented [PR-E] | query + order toggle |
| Playlists (user-built) | Missing | documented in [PR-E] scope |
| Favorites | Implemented [PR-E] | `Favorites.kt` |
| History / per-file resume | Implemented [PR-E] | `ResumeStore.kt` (was single-item only) |
| Playback speed control | Unverified | not found in code scan; treat as Missing unless found |
| Gestures (brightness/seek/volume) | Implemented | `PlayerGestures.kt` + PlayerGesturesTest |
| Picture-in-picture | Implemented | `PipSupport.kt` + PipSupportTest |
| Background audio | Implemented | `PlaybackService` + BackgroundPlaybackTest |
| Equalizer / audio effects | Partial | Stadium Sound only; no multi-band EQ |
| Android TV | Partial | TV layout branches (`tv` flag); TvTest |
| Accessibility (TalkBack labels etc.) | Unverified | no dedicated audit yet |
| Network playback (HTTP streams, NAS/SMB) | Missing | SAF file/folder picking only; no network sources |
| Diagnostics / media info | Implemented [PR-H] | `MediaDiagnostics.kt` + matrix doc |
| Chromecast | Missing | no cast code |

## Architecture refactor (incremental, no behavior change)
Step 1 (this PR): extract `UpdateEntryActivity` + `PlayerClockTicks` into `ui/` files; start the docs set. Next increments: track-choice dialog block, player controls row, home/recents card, then a ViewModel split. Each step ships only when CI is green.

## libVLC note
libVLC is considered ONLY as a separate proof-of-concept. VLC Android is GPL-2.0; any integration must check license obligations first. No integration in this wave.
