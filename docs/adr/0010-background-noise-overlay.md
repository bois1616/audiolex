# ADR-0010: The background-noise overlay (SNR) — mixed in the shared producer, a free source, a persisted setting

- **Status:** accepted (the author's decisions 2026-07-19, the architecture from an Opus sharpening the same day)
- **Date:** 2026-07-19

## Context

The heart of the training is decoding speech under harder conditions; a background-noise overlay with an adjustable signal-to-noise ratio (SNR) is the next step (backlog M4, concept §3). The mixer has been prepared since M1 but unused: `mixWithNoise`, `noiseGainForSnr` and `rms()` in `core/audio/Mixer.kt` are platform-free and unit-tested. Three questions were open: **where** in the audio path the mixing happens, **where** the noise comes from, and **how and where** the SNR is controlled and stored.

The author's decisions of 2026-07-19 set the frame: the noise comes from a **free source** (freely licensed, CC0 for instance), the SNR slider sits in the **settings**, and the noise takes effect in **both** training modes (learning and exam).

## Decision

1. **Mix in the shared producer, not in the sink.** Both training screens play through `queue.play { … }` — the producer decodes the WAV into a `PcmBuffer` and returns it. The noise is mixed in **there**, after the decode and before the return (`mixWithNoise(speech, noise, noiseGainForSnr(...))`). That way the mixing applies equally to Android and desktop, and the `AudioSink` stays a dumb output (ADR-0003). The Android sink's 180 ms pre-roll silence (`withLeadingSilence`) is prepended after the mixing and stays untouched.

2. **The noise as a free, locally obtained resource — the source library separate from the bundled resource.** The author keeps his raw audio library under `resources/sounds/` (the repo root, gitignored) — background noises now, own word/sentence recordings later. For the app the loops are converted from there to **22050 Hz mono PCM16** (`ffmpeg -t 20 -ac 1 -ar 22050 -c:a pcm_s16le …`; the speech format, since `mixWithNoise` requires the same sample rate and channel count; the app only decodes WAV, no MP3) and placed under `composeApp/src/commonMain/composeResources/files/noise/` — only from there does Compose load them at runtime on every target (`Res.readBytes`), and a repo-root directory is not reachable on Android. Those WAVs stay **gitignored** like the corpus WAVs; what is versioned is only `noise.json` (scenario metadata: `id`/`label`/`fileRef`/`source`/`license`) and `README.md` (origin/licence/conversion). The first stock (2026-07-19): three scenarios (`verkehr`, `strassenbahn`, `restaurant`) from salamisound.de/pixabay.com, free for non-commercial use, trimmed to 20 s. If a file is missing, the feature is audibly ineffective (the fallback: clean speech) but nothing breaks.

3. **The SNR as a persisted app setting, in both modes.** `AppSettings`/`SettingsEntity` gain `noiseEnabled: Boolean = false` and `snrDb: Int = 10` (DB version 4 → 5, the destructive fallback carrying it as with the previous bumps). A `Switch` + `Slider` in `EinstellungenScreen` control both; the values are passed to both screens through `App()` by the same load/save pattern as `themeMode`/`corpusMode`. Both modes share the same setting — no per-mode separation.

4. **No resampling in code.** Aligning the sample rate is a documented ffmpeg step, not a runtime resampler. A defensive `PcmBuffer.toMono()` catches a file accidentally stored in stereo; with a differing sample rate the noise is left out of the playback (no crash) rather than resampled with aliasing.

## Alternatives

- **Generating the noise in code (pink/white noise):** rejected in favour of the author's choice of a "free source" — generated broadband noise would be asset- and licence-free, deterministically testable and without a procurement step, but it sounds less realistic than a real babble of voices. Noted as a possible route for a later, additional scenario.
- **Mixing in the platform-specific sink:** rejected — it would duplicate the logic across Android and desktop and violate the ADR-0003 boundary "no business logic below the sink".
- **A runtime resampler in `:core`:** rejected for now — downsampling without a low-pass produces aliasing (a spectral shift), and the ffmpeg step is one-off and trivial. On real need, a `[PROP]` of its own.
- **The SNR as an in-session control rather than a setting:** rejected (the author's decision "in the settings") — central persistence, and it composes cleanly with the presets item later.
- **A continuous noise bed across word boundaries:** out of scope — the noise restarts at the beginning of the loop for every playback. Accepted for a first scenario; a continuous bed would be a rebuild of its own (the sink would then hold state).

## Consequences

- **Easier:** no new audio layer, and the prepared mixer finally gets used; the mixing sits in one place for both platforms; the setting docks onto the existing `SettingsEntity` foundation; the presets level (M4) finds its main lever (`noiseEnabled`/`snrDb`) already there.
- **Harder / deliberate debt:** a content dependency — the loops are converted locally from `resources/sounds/` (like the corpus WAVs), so on a fresh checkout they are inert until then. The noise is not continuous across words (it restarts at the beginning of the loop per playback); the bundled WAVs are trimmed to 20 s, because the per-word mixing only uses the beginning anyway. The destructive DB migration discards the test device's SRS cards on the bump (an accepted prototype state, no real user installations).
- **Distinguishing own recordings:** `resources/sounds/` is a **build-time source library** (the author puts raw files there, they get converted and taken into the bundle). Words and sentences recorded inside the app at runtime, wanted later (backlog M2 "record your own words/sentences", `[→Opus]`), are something else: user content that has to be stored and imported app-locally on the device, **not** through the Compose resources path packed at build time. A repo directory does not appear automatically in the running app.

## Addendum 2026-08-17: The bundled loops are gone — the catalogue consists only of the user's own noises

Point 2 of this ADR is superseded. The three loops (`verkehr`, `strassenbahn`, `restaurant`) from salamisound.de/pixabay.com are removed from the project (v0.32.0, the author's commission 2026-08-17). The reason is the direction towards publication: "free for non-commercial use" is not a free licence, and ADR-0014 fixes the shipped state as "no bundled loops". What stood here — the source library `resources/sounds/`, the ffmpeg conversion into `files/noise/`, a versioned `noise.json` plus a `README.md` with a licence table — describes a route that no longer exists.

What takes its place:

- **The catalogue is the user's own collection of noises** (v0.31.0, recorded or imported as WAV, app-local on the device). `loadAllNoiseScenarios` reads only there; `loadNoiseScenarios` (the bundled branch including `Res.readBytes`) is deleted, and `files/noise/` no longer exists. The requirement 22050 Hz / mono / PCM16 applies unchanged — it is now checked at import instead of assured at conversion.
- **An empty catalogue is the normal state of a fresh installation**, not the error case. Point 4 (the fallback to clean speech) carries that unchanged; the settings now say it too (the line "No sound yet — playback stays clean." instead of an empty radio group).
- **`NoiseScenario` lost `source`/`license`** and is no longer `@Serializable`: both fields carried the origin of foreign loops, and without `noise.json` the type is deserialised nowhere. There is no audio left in the app whose licence would have to travel with it.
- **No default scenario any more.** `AppSettings.noiseScenario` is empty on a fresh installation instead of `"restaurant"`; installations from before v0.32.0 carry the old id and run into the existing resolution "unknown id → the first catalogue entry" (or clean speech while the catalogue is empty). Only the Kotlin default changed, not the column — the schema version stays 8.

The author's own recordings (the converted WAVs) still sit in `resources/sounds/` next to their MP3 sources: gitignored, outside every build path, importable on the device for personal use. What was rejected is shipping them, not using them privately.

## Addendum 2026-08-17 (the second, a few hours after the first): bundled loops yes — but only own recordings

The first addendum of that day deleted the bundled catalogue along with its mechanism. The author then decided: **one background noise has to come along in any case**, namely his own recording from the bus ("not ideal, but legal"). So the mechanism returns; the licence blocker does not.

What that changes and what it does not:

- **The catalogue has two halves again**, bundled first and own noises after (`loadBundledNoiseScenarios` + `loadAllNoiseScenarios`, with the branch in `loadNoiseBuffer` on the id prefix `eigen-`). Exactly the structure from before v0.32.0.
- **The selection rule for "what may sit here" is the new part**, not the code: only content that may be redistributed — in practice, own recordings. `files/noise/README.md` lists the origin and licence per file; an entry without an origin line is a release blocker.
- **The WAVs are versioned now** instead of gitignored (like the corpus, ADR-0014 second addendum). The reason is the same: F-Droid builds from source.
- **The parsing moves into `:core`** (`parseNoiseCatalog` in `NoiseScenario.kt`, defensive like `parseCorpus`), and only the `Res.readBytes` stays in composeApp. That makes the bundled half unit-tested (`NoiseCatalogTest`, 5 cases) before a single file even exists — before, it was only "it works".
- **`source`/`license` stay gone from the data type.** The origin stands in the README, where a reviewer reads it; a field no code reads would only have hidden it worse.
- **No default scenario in the code.** The resolution "unknown or empty id → the first catalogue entry" makes the bundled entry the preselection automatically. No id is hard-wired anywhere.

The state at the close of this addendum: `noise.json` is an empty array, because the bus recording only sits on the test device. The app therefore starts with clean sound, and the settings say so (the line from the first addendum). Putting it in place is one file plus one JSON entry, with the instructions in `files/noise/README.md`.
