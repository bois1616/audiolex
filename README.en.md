# AudioLex

*[Deutsche Fassung](README.md) — the project's documentation is otherwise German.*

Hearing training for word recognition. The app plays a word, you match it, and how long until it comes back depends on how well it landed — spaced repetition like Anki, but with sound as the stimulus instead of writing.

It is built for the case where hearing and understanding come apart: after one-sided hearing loss the sound arrives but is no longer reliably recognised as speech. A hearing aid makes things louder, not clearer; the path from sound to word has to be practised again.

Non-commercial, no account, no advertising. There is no internet permission, so vocabulary, ratings and session history stay on the device — not as a promise, but because the app is technically unable to transmit anything.

AudioLex is a practice tool, not a medical product. It replaces neither an audiologist nor an ENT examination.

## What the app does

- **Learning mode** — hear the word, read along. Builds the link between what you hear and what it means.
- **Exam mode** — hear the word, card covered, rate yourself. Five steps from "again right away" to "in a month".
- **Your own recordings** — speak words and sentences yourself and write the text to go with them. Several speakers can be kept apart and switched on or off individually; a familiar voice is a different exercise from a stranger's.
- **Background noise** — lay a loop under the speech and set the distance in decibels (−5 to +20 dB). One bus interior is bundled; you can record or import your own as WAV.
- **Training levels** — Easy, Hard and Advanced as one-tap presets for the noise pair.
- **Channel choice** — left, right, both, effective with stereo headphones. A Bluetooth hearing aid sums stereo to mono; the app detects that and shows the choice as ineffective there.
- **Backup** — your recordings, your noises and your session history as a ZIP in your documents, at the press of a button.
- **German or English interface** — switchable right on the start screen; without a choice of your own it follows the device language. A quick guide sits next to it and follows the same setting ([ADR-0015](docs/adr/0015-ui-lokalisierung.md), German).
- **Training language, kept separate** — what you practise is chosen in the settings. Training German while reading the app in English is a legitimate combination. Your own recordings get a language when you create them; it says where the entry shows up, not what is spoken in it ([ADR-0016](docs/adr/0016-korpus-sprache.md), German).

Bundled are 72 German words and sentences — 68 from free speech synthesis, four spoken by real voices — plus 20 English examples and one background noise recorded inside a bus.

## Licence

The **code** is under Apache-2.0, see [LICENSE](LICENSE).

The **content** (audio files, corpus texts) sits explicitly outside the code licence and carries its own provenance, as decided in [ADR-0014](docs/adr/0014-veroeffentlichung-lizenz.md) (German). Whatever ships has to be redistributable:

| Content | Origin | Redistribution |
| --- | --- | --- |
| 68 synthetic recordings (`voiceId: thorsten`) | Generated locally with [Piper](https://github.com/rhasspy/piper), voice `de_DE-thorsten-medium` — model MIT, dataset [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) CC0 | CC0-1.0 |
| 20 synthetic recordings (`voiceId: ljspeech`) | Generated locally with Piper, voice `en_US-ljspeech-high` — model MIT, dataset [LJ Speech](https://keithito.com/LJ-Speech-Dataset/) public domain | CC0-1.0 |
| 4 demo recordings (`voiceId: stephan`, `grete`) | Recorded by the author and a second speaker inside the app | CC0-1.0; the second speaker's consent is on file with the author (2026-08-17) |
| German sentence entries (`satz-*`) | Freely paraphrased after Douglas Adams, "The Hitchhiker's Guide to the Galaxy", ch. 1 — no verbatim quotes ([ADR-0009](docs/adr/0009-satz-korpus-modell.md), German) | own text |
| English entries (`en-*`, `satz-en-*`) | Freely composed, neither translations nor based on a source | own text |
| Bundled background noise (`files/noise/bus.wav`) | The author's own recording, bus interior | CC0-1.0 |

Third-party licensed audio does not belong in this repository. Three purchased noise loops were removed in August 2026 for exactly that reason.

## Stack

Kotlin Multiplatform + Compose Multiplatform ([ADR-0001](docs/adr/0001-tech-stack-kmp-compose.md), German):

| Target | Purpose |
| --- | --- |
| Android (minSdk 29) | Primary platform, test device Galaxy A53 / Android 16 |
| Desktop (JVM) | Development and verification target, no emulator needed |
| iOS | An option, module split prepared, not enabled (needs a macOS host) |

Dependencies: Kotlin/kotlinx, AndroidX (Activity, Room, SQLite), Compose Multiplatform. No Play Services, no Firebase, no analytics, no advertising.

## Build & run

```bash
./gradlew :core:jvmTest              # unit tests of the core logic
./gradlew :composeApp:run            # start the desktop app
./gradlew :composeApp:assembleDebug  # build the Android APK
./gradlew build                      # everything
```

Requirements: JDK 21 (installed, not downloaded — there is deliberately no toolchain provisioning plugin), Android SDK with its path in `local.properties`.

A fresh clone builds completely: the corpus audio is versioned. To regenerate it:

```bash
cd tools
uv run python -m piper.download_voices --download-dir voices \
  de_DE-thorsten-medium en_US-ljspeech-high
uv run generate_tts.py        # renders only what is missing; --force redoes everything
```

Details: [tools/generate_tts.py](tools/generate_tts.py), [ADR-0006](docs/adr/0006-audioquelle-tts.md) (German).

## Repository map

The documentation is written in German, including the architecture decision records. This file and the app's English interface are the exceptions.

- Binding working mode: [AGENTS.md](AGENTS.md)
- Concept and vision: [docs/konzept/AudioLex-Konzept.md](docs/konzept/AudioLex-Konzept.md)
- Architecture: [docs/architecture.md](docs/architecture.md)
- Decisions: [docs/adr/](docs/adr/)
- Backlog: [docs/backlog.md](docs/backlog.md)
- Implementation journal: [docs/implementation-log.md](docs/implementation-log.md)
- Route to F-Droid inclusion: [docs/fdroid-anmeldung.md](docs/fdroid-anmeldung.md)
