# CLAUDE.md

Project context for Claude Code. The binding working mode (backlog control, definition of done, conventions, source of truth) lives in `AGENTS.md` — read it there, it is not duplicated here.

## What AudioLex is

A hearing-training app for the author himself (roughly 80 % one-sided hearing loss, hearing-aid wearer). The problem is neurological: the sound arrives but is not decoded as speech. Training runs in two modes: **learning mode** (hear the word, see the text — this builds the association) and **exam mode** (hear the word, card stays covered, rate yourself → spaced repetition decides when it returns).

## Architecture in short

- `:core` — KMP library (androidTarget + jvm), platform-free logic in the packages `srs`, `audio`, `corpus`, `session`, plus `i18n` (UI text catalogue, ADR-0015). Fully unit-testable on the JVM.
- `:composeApp` — Compose Multiplatform UI (Android + desktop). Desktop is the dev target (native Debian, GNOME/Wayland).
- Audio: PCM mixing (channel levels, background noise/SNR) in common Kotlin; only the output is expect/actual (`AudioSink`: Android AudioTrack, desktop javax.sound, iOS AVAudioEngine later).
- Details: `docs/architecture.md` · decisions: `docs/adr/`

## Commands

```bash
./gradlew :core:jvmTest              # core logic tests (fastest loop)
./gradlew :composeApp:run            # start the desktop app
./gradlew :composeApp:assembleDebug  # Android APK
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

## Domain knowledge

- **SRS scale (MVP, fixed intervals, ADR-0005):** Again 1 min · Soon 10 min · Later 1 day · Good 1 week · Perfect 1 month. Implemented in `FixedIntervalScheduler`; the code enum is `ReviewRating` (AGAIN/SOON/LATER/GOOD/PERFECT), the UI labels come from the text catalogue in both languages.
- **Reference training setup: Bluetooth hearing aid, left ear** (ADR-0007) — stereo is summed to mono there; what counts is the level and the intelligibility at the trained ear. Channel separation left/right/both (`StereoGain`) remains an option for alternative setups (wired headphones) and has no effect over Bluetooth — the UI must not show it as effective there.
- **UI language German/English (ADR-0015, since v0.34.0):** no `strings.xml` — the text catalogue is the typed interface `core/i18n/Strings.kt` with `GermanStrings`/`EnglishStrings`; a missing translation is a compile error. Screens read through `LocalStrings.current`. New UI text = extend the interface + fill **both** catalogues. The chosen language lives in `AppSettings.uiLanguage`; the default `UiLanguage.SYSTEM` follows the device language (primary subtag, `de-AT` → German). The picker sits on the start screen, not in the settings. No exceptions any more: the channel test (`DevPlaybackScreen`) was German until v0.36.0 and speaks both languages since the F-Droid tester report of 2026-08-27 (ADR-0015 addendum) — it stays reachable only by a long press on the version line.
- **The corpus language (ADR-0016, since v0.35.0) is a different thing from the UI language.** `CorpusLanguage { DEUTSCH, ENGLISCH }` in `core/corpus`, the setting is `AppSettings.corpusLanguage`, chosen under "Training language" in the settings. The language is a **classification by whoever created the entry**, not a check of its content — `OwnEntry.language` is picked when the entry is created and only says where the entry shows up. Matched on the primary subtag (`primaryLanguageSubtag`, `de-AT` → German). The filter sits in `mergeCorpus` **before** `kind` and `excludedSpeakers`, because the corpus turns into SRS cards through `allOrSeed` — an entry in the wrong language that slips through would otherwise land in the real deck. Background noises stay language-free.
- **A headset with a microphone produces crosstalk** (author's device test 2026-08-27, which explains the finding left open on 2026-08-06): the mic picks up the ear that is playing and the other side reproduces it faintly — the selected channel is clearly louder, but the silenced one is not silent. **Not an app bug**: `perEarStereo`/`toStereoWithGain` put exact zeroes there (unit-tested). For clean channel checks use headphones **without** a microphone; the channel test says so itself since v0.36.2, and since v0.37.0 the settings say it too when a channel ≠ both is chosen and the routed output type implies a microphone (`OutputDiagnosis.headsetHasMicrophone`). **Confirmed independently on 2026-08-28** by the F-Droid tester on different hardware: with only one earpiece in the ear, the unused one plays into the mic and the other side reproduces it.
- **Keep the word corpus generic**: `Word` is separate from `AudioRecording` (several speakers per word) and from `ReviewCard` (SRS state). Nothing hearing-loss specific gets wired into the model.
- Background-noise overlay: mixed by SNR (dB) — `noiseGainForSnr` in the mixer. The catalogue has two halves: bundled (`files/noise/`, versioned) and the user's own sounds (recording/WAV import, app-local). **Rule since v0.33.0:** only content that may be redistributed gets bundled — in practice the author's own recordings; the three purchased loops are gone (ADR-0014, ADR-0010 addenda). An empty catalogue means clean speech, not an error.
- **Shipped audio files live in the repo** (92 corpus WAVs: 68 German TTS, 20 English TTS, 4 human recordings; since v0.33.0): F-Droid builds from source. Origin and redistribution rights belong in that folder's README — an entry without an origin line is a release blocker. The path to inclusion in F-Droid: `docs/fdroid-anmeldung.md` (German).

## Current state & open questions

- Where things stand: `docs/implementation-log.md` (top) and `docs/backlog.md`.
- `[KLÄRUNG]` items in the backlog need a decision from the author — do not resolve them alone.
