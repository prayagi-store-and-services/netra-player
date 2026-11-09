# ATIF v3.0 - applicable parts for Netra Player

ATIF v3.0 (Absolute Truth & Intelligence Framework) is the owner's permanent master rule, issued 9 Nov 2026. Canonical full text: private governance copy (`netra-intruder`, `docs/ATIF-v3.0.md`). This file lists only the parts that apply to THIS app.

## Applicable parts and why

- Part 01, 02: playback capabilities and limits must be stated accurately.
- Part 04, 06: codec/track support is device-dependent; unsupported items show an error or Unavailable - never a guessed capability.
- Part 14: "offline" applies to local playback; the app's separate update/crash-report network paths are not hidden.
- Part 16: no login; local media stays local.
- Part 17, 20: README, release notes and site copy name the exact released build they describe.

## Data this app shows, and its status

- Playback of local video/MP3 via Media3: VERIFIED in source; device codec variance applies.
- Saved-position resume, queue controls, external .srt: as documented per released build.
- Update behaviour: described per exact version; unreleased source changes are never presented as shipped.

## Standing rule

Every description names the released version it was verified against. New playback features ship with evidence (CI +, where required, device testing) before the copy claims them.

## Source labels (owner-approved 9 Nov 2026)

Every user-visible data claim carries a short source label. UI strings stay short; the label for each claim lives in this file's "Data this app shows" list and in WHATS_NEW/release notes (e.g. "Source: Android BatteryManager"). In-app visible source labels are planned and ship only after the owner's UI preview.

## Stale data: OLD marking (owner-approved 9 Nov 2026)

Any dated or versioned claim that stops matching reality is marked "OLD (as of <date>)" at the next check and corrected in the same or a following PR. Stale claims are never silently edited and never left unmarked.

## Weekly audit (owner-approved 9 Nov 2026)

A weekly audit records each material claim as VERIFIED or NOT VERIFIED in `docs/ATIF-AUDIT.md`, newest first. Failures and NOT VERIFIED items are listed first in every audit and every report.
