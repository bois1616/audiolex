# AudioLex architecture

As of 2026-07-07. Decisions with their reasoning: `docs/adr/`. This document describes the target picture and the current cut.

## Target picture

A local training app with no backend. All domain logic (SRS, corpus, session, audio mixing) is platform-free and testable on the JVM; platform code is limited to the UI entry point and audio output. Android is the target platform, desktop is the development vehicle, iOS is the prepared option.

## Modules

```text
audiolex/
├── composeApp/                  # Compose Multiplatform UI
│   └── src/
│       ├── commonMain/          # screens, navigation (flat, state in remember)
│       │   └── composeResources/files/corpus/  # word corpus, metadata and
│       │                         #   audio both versioned (ADR-0014: F-Droid
│       │                         #   builds from source) — as a Compose
│       │                         #   resource, so Android reads it through
│       │                         #   Res.readBytes(...) exactly like desktop
│       ├── androidMain/         # MainActivity, manifest
│       └── desktopMain/         # main() for the dev target
├── core/                        # KMP library: platform-free domain logic
│   └── src/
│       ├── commonMain/kotlin/de/hexenwoche/audiolex/core/
│       │   ├── srs/             # ReviewRating, ReviewCard, ReviewScheduler
│       │   ├── audio/           # PcmBuffer, Mixer, AudioSink (expect), WavFile
│       │   ├── corpus/          # Word, AudioRecording, categories, CorpusLanguage
│       │   ├── i18n/            # UI text catalogue DE/EN, UiLanguage (ADR-0015)
│       │   └── session/         # session control
│       ├── commonTest/          # unit tests (run as jvmTest)
│       ├── androidMain/         # actual: AudioTrack sink
│       └── jvmMain/             # actual: javax.sound sink
└── docs/                        # concept, ADRs, backlog, implementation log
```

**Module discipline** (AGENTS.md): components grow as packages inside `:core`; a split into separate Gradle modules (say `:core:srs`) waits until build times or dependency boundaries force it — and then via an ADR. That keeps the Gradle configuration small while the boundaries stay visible.

**Direction of dependency:** `composeApp` → `core`. Never the other way round; `core` knows neither Compose nor Android UI.

## Audio pipeline (ADR-0003)

```text
WAV file (corpus)           WAV loop (background noise)
      │                            │
      ▼                            ▼
  PcmBuffer ──── mixWithNoise(noiseGainForSnr) ────┐
                                                   ▼
                                    toStereoWithGain(StereoGain)   ← channel choice l/r/both,
                                                   │                  level per ear
                                                   ▼
                                        AudioSink (expect/actual)
                                        Android: AudioTrack
                                        Desktop: javax.sound
                                        iOS:     AVAudioEngine (later)
```

Everything above the sink is deterministic and unit-tested; the sink receives finished stereo PCM16.

## Data model (persistence from M3, ADR-0004)

| Entity | Core fields | Purpose |
| --- | --- | --- |
| `Word` | id, text, language, syllableCount, category, phoneticGroup? | generic corpus entry, nothing hearing-loss specific |
| `AudioRecording` | id, wordId, voiceId, locale, fileRef | n recordings per word (several speakers/voices); `locale` carries standard/region/dialect (ADR-0006) and is filterable independently of `Word.language` |
| `ReviewCard` | wordId, dueAt, lastRating, repetitions | SRS state, kept separate from the word |
| `Session` | id, mode, parameterSnapshot, results[] | traceability of one training unit |
| `SettingsProfile` | name, channel/level/SNR/scenario/word filter | named presets (easy/hard/advanced) |

Review history is stored raw (rating plus timestamp), not just the next due date — which keeps the path to FSRS or automatic rating open (ADR-0005).

## Deliberate limits (what the MVP does without)

- **Session state does not survive a configuration change or process death** (author's decision 2026-08-05, backlog "code quality"): all screen state lives in `remember` without a `Saver` or ViewModel — a deliberate decision, not an open gap. Rotation or a system kill ends the running round; it starts fresh next time. Ratings already given and SRS due dates are persisted immediately (ADR-0004) and survive. A ViewModel/SavedState layer would be comparatively cheap given the flat navigation, but it carries no value in this phase — it stays a backlog proposal in case interrupted sessions turn out to be a real nuisance.

## Test strategy

1. **Unit (core, fastest loop):** `./gradlew :core:jvmTest` — SRS scheduling, mixer/SNR, WAV loading, session logic. Runs without an audio device and without the Android SDK.
2. **Desktop sight check:** `./gradlew :composeApp:run` — UI behaviour and flows; audio on the computer is an approximation only.
3. **Device test (binding for audio):** debug APK on the Galaxy A53, for real with the hearing aid — channel separation, levels and how the background noise feels can only be judged there.

## Platform matrix

| Building block | Android | Desktop (JVM) | iOS (option) |
| --- | --- | --- | --- |
| UI | Compose MP | Compose MP | Compose MP (not enabled) |
| Domain logic `:core` | commonMain | commonMain | commonMain |
| Audio output | AudioTrack | javax.sound | AVAudioEngine (open) |
| Persistence (from M3) | Room KMP/bundled SQLite | same | same |

Enabling iOS = add the Gradle targets, one `AudioSink` actual, and a macOS build host; no architectural change.
