# ADR-0011: Two equally supported output setups (hearing aid / stereo headphones), detected automatically

- **Status:** accepted (the author's decision 2026-08-06, the architecture from an Opus sharpening the same day) · **the detection rule corrected 2026-08-06 after the device test, see the addendum**
- **Date:** 2026-08-06

> **Addendum 2026-08-06 (device test v0.18.0): the detection asks about the routing, not about the connected devices.**
>
> The table below stays valid as a *classification of a single device type*. What was wrong was the layer above it: point 2 left open how several simultaneously reported devices are to be combined, and the doubt rule was consequently read as meaning that a hearing aid also wins against a stereo device present at the same time. The A53 test showed why that misses the main use case: the author's hearing aid is **connected** over Bluetooth essentially permanently. When he plugs headphones in, Android gives the wired output priority and the hearing aid goes silent — but it is still in the list of connected devices, so the app still reported "hearing aid". Exactly the channel work the headphones were bought for would have stayed locked permanently. (The author's finding: "the status line does not flip, but the headphones are detected and used with priority […] this is reversible.")
>
> **The corrected decision:** what governs is **where media audio would currently be routed**, not what is connected. The query is `AudioManager.getAudioDevicesForAttributes` with exactly the `AudioAttributes` that `AndroidAudioSink` sets for playback (`USAGE_MEDIA` + `CONTENT_TYPE_SPEECH`) — routing is attribute-dependent on Android, and a query with different attributes could answer for a different path than the one the user hears. If the result contains a stereo-capable device, the headphone setup applies, otherwise the hearing-aid setup. A precedence for the hearing aid is **no longer** needed and would be harmful: the routed set already *is* the answer.
>
> The API is public only from **Android 13 (API 33)** — an initial assumption of API 30 was refuted by the build's lint, so the guard follows the tool rather than the guess. Below that (at minSdk 29, so API 29–32) the original enumeration including the conservative precedence stays as the fallback; on the test device (API 36) the routing path always applies. The price of the correction: on devices between 29 and 32 the original misbehaviour remains. That is acceptable while the test device is the reference — were the app ever used seriously on an older device, the manual switch (see alternatives) would be the way out there.
>
> **What is to be learned from this:** the doubt rule ("hearing aid when in doubt") is right for *unknown device types* and stays. Applying it to the case "two known devices at once" was a transfer onto a question it was never meant to answer — there the right question is not "what is there?" but "what is active?".
>
> **An open limit:** the triggering callback only fires when devices appear or disappear. A rerouting without a device change (the system's output switcher) is not noticed.

> **Addendum 2026-08-27 (a tester report): the detection now says what it saw.**
>
> Under "consequences" stands a deliberately accepted risk: "the automatic detection can be wrong, and that is hard for the user to see through (locked controls with no apparent reason)." Exactly that case occurred on 2026-08-27 — the F-Droid tester chivalry reports an ineffective channel selection on a USB-C headset on a newer phone, while the same app does what it should on an older phone with a 3.5 mm jack. The author cannot reproduce it on his USB-C headset on the A53.
>
> That is precisely the gap the table above leaves open: a USB-C headset reports itself, depending on the descriptor, as `TYPE_USB_HEADSET` (22, in the table) **or** as `TYPE_USB_DEVICE` (11, which falls into the `else` branch and therefore onto the hearing aid). Which type someone else's phone reports cannot be guessed from here.
>
> **An addition, not a change to the rule:** `rememberOutputSetup` was extended into `rememberOutputDiagnosis` and, besides the setup, returns the reported device types in plain text including the constant (`USB_HEADSET (22)`). It is shown **only in the channel test** — that is the instrument, and the line is a diagnosis, not a setting. If it says "hearing aid detected" while the test below separates cleanly, the audio path is fine and the **classification** is wrong. The detection table itself stays untouched for now: first the reported type, then the correction. The manual override named under "alternatives" stays the follow-up should the table fundamentally not suffice.

## Context

On 2026-07-08 ADR-0007 fixed the **Bluetooth hearing aid on the left ear** as the reference training setup and thereby downgraded channel separation from a core feature to a "setup option for alternative hardware". The reason was compelling: over the ASHA/Bluetooth path the signal only reaches a hearing aid that sums stereo to mono — panning is not perceptible there, and a UI that offers a channel selection as effective would be lying.

On 2026-08-06 the author asked for an extension: the app should also run with (wired, at any rate stereo-capable) headphones — "that would lift the app to a learning mode and make it device-independent". That does not change the purpose of the training, but it does change the claim: no longer effectively tied to one piece of hardware.

The core already carries it. `StereoGain` (`BOTH`/`LEFT_ONLY`/`RIGHT_ONLY`, each with its own level) and `PcmBuffer.toStereoWithGain` have sat finished and unit-tested in `core/audio` since M1 and are used by no screen; `AndroidAudioSink` already sets the channel mask depending on `buffer.channels` (`CHANNEL_OUT_MONO`/`CHANNEL_OUT_STEREO`). ADR-0007 explicitly foresaw this: "should the supply change […], only UI/preset work is needed — the mixer and the data model stay channel-capable."

What was open was **how the app knows** which setup is currently active. Without that information it can only offer the channel selection unconditionally (and thereby feign effectiveness in the hearing-aid case, which ADR-0007 forbids) or hide it unconditionally (and thereby fail to serve the headphone case).

## Decision

**1. Two equally ranked setups instead of a reference plus an exception.** We model `OutputSetup { HOERGERAET, STEREO_KOPFHOERER }` as a named state. The hearing aid stays the setup the DoD's device tests run against (AGENTS.md §4.3) — the listening situation being trained does not change. But headphones are no longer a special case: they are a supported mode of operation with a training quality of their own, and only there is deliberate channel work possible at all.

**2. Automatic detection, no manual switching** (the author's decision 2026-08-06; for the alternatives considered see below). The detection rule follows the only question that matters in the subject matter — *do two separate signals reach two ears?* — rather than the device category as such:

| Detected output device | Setup | Reasoning |
| --- | --- | --- |
| `TYPE_WIRED_HEADPHONES`, `TYPE_WIRED_HEADSET`, `TYPE_USB_HEADSET` | stereo headphones | two transducers, one ear per channel |
| `TYPE_BLUETOOTH_A2DP` | stereo headphones | regular Bluetooth headphones; an ASHA hearing aid does **not** report as A2DP |
| `TYPE_HEARING_AID` | hearing aid | the reference case, summed to mono |
| BLE types (`TYPE_BLE_HEADSET` and similar) | hearing aid | ambiguous (LE-Audio earbuds *or* an LE-Audio hearing aid) — conservative, see consequences |
| speaker, earpiece, everything else | hearing aid | channel separation not controllable (ADR-0007: a free field is unsuitable) |

When in doubt, the hearing-aid setup applies. The error therefore always goes in the harmless direction: a locked channel selection, never a feigned one.

**3. The detection lives in `:composeApp`, not in `:core`.** On Android `AudioManager` needs a `Context`; `:core` stays context-free, as already decided for the database (ADR-0004, where `DatabaseBuilder.android.kt` sits in `:composeApp` for exactly this reason). `:core` contributes only the platform-free part: the enum and the `toStereoWithGain` arithmetic that already exists.

**4. The detected state is observable, not queried once.** Headphones get plugged in and out during operation; Android provides `AudioManager.registerAudioDeviceCallback` for that. The UI has to follow without the user re-entering a screen.

**5. The channel selection takes effect in the shared producer**, where the background noise is already mixed in (`NoiseMixing.kt`, ADR-0010) — after the decode and the noise mix, before handing over to the sink. The sink stays dumb (ADR-0003).

**6. On the desktop there is no detection.** Output runs through `paplay` there (ADR-0003); dependable device detection would only be available through additional external calls. The desktop target is the dev target, not a verification platform — it counts as stereo-capable across the board, documented rather than glossed over.

## Alternatives

- **A manual switch in the settings** ("output device: hearing aid / headphones"). No platform code, no misdetection, implementable immediately. Rejected by the author on 2026-08-06 in favour of the automatic detection — it stays the obvious fallback should the detection prove unreliable on the device.
- **Automatic detection with a manual override.** The best usability, but two states that can drift apart, and the largest implementation. Rejected as premature: first show whether the automatic detection carries.
- **Headphones as the new reference setup.** Rejected for the same reason as on 2026-07-08 in ADR-0007: what should be trained is the real listening situation, and that is the hearing aid. Headphones come in addition; they do not replace it.
- **Detection in `:core` with a `Context` passed through.** Rejected — it would break the context-freedom of `:core` that the project deliberately maintains in several places.

## Consequences

- **ADR-0007 is not superseded but extended** (an addendum noted there). Its core stays valid: the reference is the hearing aid, and what counts is the level and the intelligibility at the trained ear. What changes is the role of channel separation — from a "tool for alternative setups" to the second supported mode of operation.
- **The UI obligation from ADR-0007 remains and gets sharper**: a channel selection ≠ "both" must not appear effective in the hearing-aid setup. What is new is the opposite direction — the app visibly shows which setup it detected, so that a misdetection is noticeable to the user at all.
- **A deliberately accepted risk:** the automatic detection can be wrong, and that is hard for the user to see through (locked controls with no apparent reason). The conservative doubt rule limits the damage to "too little offered". Should that show up in the device test, the manual override is the documented follow-up.
- **The BLE ambiguity is an open flank.** LE-Audio earbuds are treated as the hearing-aid setup and thereby lose the channel selection. That is defensible for this app (the user wears a hearing aid), but it is a simplification, not a correct distinction.
- **New platform code on Android**, which existed nowhere before. It is limited to the detection; the audio path itself stays platform-free (ADR-0003).
- **The prototype character of the channel work ends.** `StereoGain` had been dead but tested code since M1 — with this ADR it becomes productive for the first time. The existing tests already cover the arithmetic.
