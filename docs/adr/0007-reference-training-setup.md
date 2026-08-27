# ADR-0007: The reference training setup — Bluetooth hearing aid, left ear

- **Status:** accepted · **extended by ADR-0011** (2026-08-06)
- **Date:** 2026-07-08

> **Addendum 2026-08-06 (ADR-0011):** the core of this ADR stays valid — the reference setup is the Bluetooth hearing aid on the left ear, what counts is the level and the intelligibility at the trained ear, and the UI must not suggest a channel selection is effective there. What changed is the role of channel separation: from a "setup option for alternative hardware" (decision 2 below) it becomes a **second, equally supported output setup** with a training quality of its own, which the app detects automatically. The details, the detection rule and the risks deliberately accepted are in ADR-0011. The sentence noted under "consequences" — "should the supply change […], only UI/preset work is needed" — proved accurate: `StereoGain` and the data model did not have to be touched.

## Context

The Opus review (finding 2, P0, `docs/reviews/2026-07-07-m1-audio-review.md`) uncovered this: the concept treats channel separation (left/right/both, separate levels) as a central training instrument, but in the real setup Bluetooth audio only reaches the single hearing aid on the left ear — the healthy right ear never gets a signal over that path, and the hearing aid sums stereo to mono. Channel separation is structurally ineffective there. What needed settling was the reference training setup (wired headphones? speakers? different per scenario?), because it determines session parameters (M2), presets (M4) and how much weight the core feature carries.

## Decision

The author's decision of 2026-07-08:

1. **The reference training setup is the Bluetooth hearing aid on the left (trained) ear.** The playback device is the smartphone; the Galaxy A53 is the test device, but the app is **not tied to that particular device** (no further device-specific wiring). An addendum: the `swapStereoChannels` fix originally suspected was refuted by the re-test protocol (review finding 1, ADR-0003) — the device was never faulty, the fix itself was the bug and has been rolled back.
2. **Channel separation moves from a core feature to a setup option** for alternative hardware (wired headphones, say): `StereoGain` stays in the mixer and stays tested, but is no longer driven through as a primary UI feature after M1/M2 and is carried as a setting instead (M4).

## Alternatives

- **Wired headphones (USB-C) as the reference** — the only route on which left/right/both works as originally conceived. Rejected as the *reference*: what should be trained is the real listening situation, and that is the hearing aid on the left ear. It stays possible as an alternative setup — which is exactly why the mixer stays channel-capable.
- **Different per scenario** (Bluetooth for everyday realism, wired for channel-separation exercises) — rejected for the MVP: two reference setups double the verification effort on the device. It can come back later as a preset dimension.
- **Speakers (free field)** — no control over which ear receives what; unsuitable as a reference.

## Consequences

- **What counts in the reference setup:** the overall level and the intelligibility at the left ear, later the SNR and the background noise — not the channel selection. Device verification (AGENTS.md §4.3) tests against this setup from now on.
- The app has to assume that stereo is **summed to mono** in the hearing aid; panning is not perceptible there. The UI must not suggest that a channel selection ≠ "both" is effective on Bluetooth output (a note, M4 settings).
- The M1 backlog item "drive channel control all the way into the UI" is downgraded and moved behind M4 (settings) — which also resolves the review remark "resolve the backlog overlap".
- Phrasings like "channel control is a core feature" in `SOUL.md`/`CLAUDE.md` are adjusted: the core is "the signal reaches the trained ear intelligibly and at the right level", and channel separation is the tool for alternative setups.
- Deliberately kept open: should the supply change (both sides, headphone training), only UI/preset work is needed — the mixer (`StereoGain`) and the data model stay channel-capable.
