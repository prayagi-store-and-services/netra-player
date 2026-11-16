# Netra Player compatibility matrix (T11)

Evidence labels: **[unit]** = covered by a JVM unit test in this repo; **[media3-src]** = verified against the Media3 1.11.1 source tree (the exact version this app bundles); **[device-NOT-RUN]** = expected from Media3 documentation but NOT verified on a physical device.

Rule: container support is separate from codec support. A supported container with an unsupported codec fails with the plain-words error from `playbackErrorMessage` (mapped from the exact `PlaybackException` code - never guessed).

## Containers
| Container | Support | Evidence |
|---|---|---|
| MP4 / M4V | Supported | [media3-src] Mp4Extractor; [device-NOT-RUN] |
| Matroska / WebM (mkv/webm) | Supported | [media3-src] MatroskaExtractor; [device-NOT-RUN] |
| MP3 | Supported | [media3-src] Mp3Extractor; [device-NOT-RUN] |
| WAV / FLAC / OGG / AAC / AMR / MIDI | Supported (audio) | [media3-src] respective extractors in DefaultExtractorsFactory; [device-NOT-RUN] |
| AVI / TS / MOV / 3GP | Supported | [media3-src]; [device-NOT-RUN] |

## Codecs (decoder support depends on the PHONE's hardware - never inferred from the extension)
| Codec | Support | Evidence |
|---|---|---|
| H.264/AVC, AAC, MP3 | Effectively universal on Android; failure still surfaces as a clear error | [device-NOT-RUN] |
| H.265/HEVC, VP9, AV1, Opus, FLAC, AC3/E-AC3 | Device-dependent; Media3 falls back to software decoders only where bundled | [device-NOT-RUN] |
| DTS, TrueHD, WMV, RealMedia | Not claimed | [media3-src] no extractor/decoder |

## Subtitles (external, user-picked)
| Format | Support | Evidence |
|---|---|---|
| SubRip (.srt) | Supported | [media3-src] SubripParser; [unit] picker/mime mapping |
| WebVTT (.vtt) | Supported | [media3-src] WebVttParser; [unit] |
| SSA/ASS (.ssa/.ass) | Supported (Media3 rendering level; full ASS styling not claimed) | [media3-src] SsaParser; [unit] |
| TTML | Deliberately NOT accepted | [unit] rejection test |
Encoding: UTF-8 (incl. Hindi) and UTF-16 via BOM auto-detection [media3-src]; legacy non-UTF encodings are a known limit.

## Android API levels
| API | Behavior | Evidence |
|---|---|---|
| 26-28 | minSdk 26; all features expected; [device-NOT-RUN] per-level | [unit] minSdk gate |
| 29-33 | Scoped storage: picked files + persisted URI permissions used | [unit] |
| 34-36 | targetSdk 36; MediaSessionService foreground playback | [device-NOT-RUN] |

## Failure handling (all mapped from exact error codes, [unit] tested in PlaybackErrorsTest)
- File missing/moved -> "not found, pick again"
- Permission revoked -> "no longer has permission, pick again"
- Unsupported container/codec -> "this phone cannot play this kind of file"
- Malformed file -> "looks damaged"
- Decoder init/over-capability -> "too demanding for its hardware"
- Audio focus loss (call/other app) -> Media3 `setAudioAttributes(..., handleAudioFocus=true)` pauses and resumes [media3-src]; headphones unplugged -> pause via `setHandleAudioBecomingNoisy(true)` [media3-src]

## Diagnostics
The in-player "Media info" panel shows: display name, container hint (from the extension - labeled as a hint), resolver MIME, per-track type/codec/selection, and the last plain-words failure. It shows no file paths, no content, and sends nothing anywhere (local only, no telemetry).
