# Architektur AudioLex

Stand 2026-07-07. Entscheidungen mit Begründung: `docs/adr/`. Dieses Dokument beschreibt das Zielbild und den aktuellen Schnitt.

## Zielbild

Eine lokale Trainings-App ohne Backend. Sämtliche Fachlogik (SRS, Korpus, Session, Audio-Mixing) ist plattformfrei und JVM-testbar; Plattformcode beschränkt sich auf UI-Einstieg und Audio-Ausgabe. Android ist die Zielplattform, Desktop das Entwicklungsvehikel, iOS die vorbereitete Option.

## Module

```text
audiolex/
├── composeApp/                  # Compose-Multiplatform-UI
│   └── src/
│       ├── commonMain/          # Screens, Navigation, ViewModel-Schicht
│       ├── androidMain/         # MainActivity, Manifest
│       └── desktopMain/         # main() für das Dev-Target
├── core/                        # KMP-Bibliothek: plattformfreie Fachlogik
│   └── src/
│       ├── commonMain/kotlin/de/hexenwoche/audiolex/core/
│       │   ├── srs/             # ReviewRating, ReviewCard, ReviewScheduler
│       │   ├── audio/           # PcmBuffer, Mixer, AudioSink (expect)
│       │   ├── corpus/          # Word, AudioRecording, Kategorien
│       │   └── session/         # (ab M2) Sitzungssteuerung
│       ├── commonTest/          # Unit-Tests (laufen als jvmTest)
│       ├── androidMain/         # actual: AudioTrack-Sink
│       └── jvmMain/             # actual: javax.sound-Sink
├── corpus-data/                 # Wortkorpus: Metadaten versioniert, Audio nicht (s. .gitignore)
└── docs/                        # Konzept, ADRs, Backlog, Umsetzungslog
```

**Modul-Disziplin** (AGENTS.md): Komponenten wachsen als Pakete in `:core`; ein Split in eigene Gradle-Module (z. B. `:core:srs`) erst, wenn Build-Zeiten oder Abhängigkeitsgrenzen es erzwingen — dann per ADR. Das hält die Gradle-Konfiguration klein und die Grenzen trotzdem sichtbar.

**Abhängigkeitsrichtung:** `composeApp` → `core`. Nie umgekehrt; `core` kennt weder Compose noch Android-UI.

## Audio-Pipeline (ADR-0003)

```text
WAV-Datei (Korpus)          WAV-Loop (Störgeräusch)
      │                            │
      ▼                            ▼
  PcmBuffer ──── mixWithNoise(noiseGainForSnr) ────┐
                                                   ▼
                                    toStereoWithGain(StereoGain)   ← Kanalwahl l/r/beide,
                                                   │                  Pegel pro Ohr
                                                   ▼
                                        AudioSink (expect/actual)
                                        Android: AudioTrack
                                        Desktop: javax.sound
                                        iOS:     AVAudioEngine (später)
```

Alles oberhalb des Sinks ist deterministisch und unit-getestet; der Sink erhält fertiges Stereo-PCM16.

## Datenmodell (Persistenz ab M3, ADR-0004)

| Entität | Kern-Felder | Zweck |
| --- | --- | --- |
| `Word` | id, text, language, syllableCount, category, phoneticGroup? | generischer Korpus-Eintrag, nicht hörverlust-spezifisch |
| `AudioRecording` | id, wordId, speaker, fileRef | n Aufnahmen pro Wort (mehrere Sprecher) |
| `ReviewCard` | wordId, dueAt, lastRating, repetitions | SRS-Zustand, getrennt vom Wort |
| `Session` | id, mode, parameterSnapshot, results[] | Nachvollziehbarkeit einer Trainingseinheit |
| `SettingsProfile` | name, Kanal/Pegel/SNR/Szenario/Wortfilter | benannte Presets (Einfach/Schwierig/Fortgeschritten) |

Review-Historie wird roh gespeichert (Bewertung + Zeitpunkt), nicht nur der nächste Termin — hält den Weg zu FSRS/Auto-Bewertung offen (ADR-0005).

## Teststrategie

1. **Unit (Kern, schnellste Schleife):** `./gradlew :core:jvmTest` — SRS-Scheduling, Mixer/SNR, später WAV-Loader und Session-Logik. Läuft ohne Audiogerät und ohne Android SDK.
2. **Desktop-Sichtprüfung:** `./gradlew :composeApp:run` — UI-Verhalten, Abläufe; Audio unter WSLg nur als Näherung.
3. **Gerätetest (verbindlich für Audio):** Debug-APK auf Galaxy A53, real mit Hörgerät — Kanaltrennung, Pegel, Störgeräusch-Empfinden sind nur dort beurteilbar.

## Plattform-Matrix

| Baustein | Android | Desktop (JVM) | iOS (Option) |
| --- | --- | --- | --- |
| UI | Compose MP | Compose MP | Compose MP (nicht aktiviert) |
| Fachlogik `:core` | commonMain | commonMain | commonMain |
| Audio-Ausgabe | AudioTrack | javax.sound | AVAudioEngine (offen) |
| Persistenz (ab M3) | Room KMP/Bundled SQLite | dito | dito |

iOS-Aktivierung = Gradle-Targets ergänzen + ein `AudioSink`-actual + macOS-Build-Host; keine Architekturänderung.
