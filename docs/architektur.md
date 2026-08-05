# Architektur AudioLex

Stand 2026-07-07. Entscheidungen mit Begründung: `docs/adr/`. Dieses Dokument beschreibt das Zielbild und den aktuellen Schnitt.

## Zielbild

Eine lokale Trainings-App ohne Backend. Sämtliche Fachlogik (SRS, Korpus, Session, Audio-Mixing) ist plattformfrei und JVM-testbar; Plattformcode beschränkt sich auf UI-Einstieg und Audio-Ausgabe. Android ist die Zielplattform, Desktop das Entwicklungsvehikel, iOS die vorbereitete Option.

## Module

```text
audiolex/
├── composeApp/                  # Compose-Multiplatform-UI
│   └── src/
│       ├── commonMain/          # Screens, Navigation (flach, State in remember)
│       │   └── composeResources/files/corpus/  # Wortkorpus (Metadaten
│       │                         #   versioniert, Audio nicht, s. .gitignore) —
│       │                         #   als Compose-Resource, damit Android per
│       │                         #   Res.readBytes(...) genauso zugreift wie Desktop
│       ├── androidMain/         # MainActivity, Manifest
│       └── desktopMain/         # main() für das Dev-Target
├── core/                        # KMP-Bibliothek: plattformfreie Fachlogik
│   └── src/
│       ├── commonMain/kotlin/de/hexenwoche/audiolex/core/
│       │   ├── srs/             # ReviewRating, ReviewCard, ReviewScheduler
│       │   ├── audio/           # PcmBuffer, Mixer, AudioSink (expect), WavFile
│       │   ├── corpus/          # Word, AudioRecording, Kategorien
│       │   └── session/         # (ab M2) Sitzungssteuerung
│       ├── commonTest/          # Unit-Tests (laufen als jvmTest)
│       ├── androidMain/         # actual: AudioTrack-Sink
│       └── jvmMain/             # actual: javax.sound-Sink
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
| `AudioRecording` | id, wordId, voiceId, locale, fileRef | n Aufnahmen pro Wort (mehrere Sprecher/Stimmlagen); `locale` trägt Standard/Region/Dialekt (ADR-0006), unabhängig von `Word.language` filterbar |
| `ReviewCard` | wordId, dueAt, lastRating, repetitions | SRS-Zustand, getrennt vom Wort |
| `Session` | id, mode, parameterSnapshot, results[] | Nachvollziehbarkeit einer Trainingseinheit |
| `SettingsProfile` | name, Kanal/Pegel/SNR/Szenario/Wortfilter | benannte Presets (Einfach/Schwierig/Fortgeschritten) |

Review-Historie wird roh gespeichert (Bewertung + Zeitpunkt), nicht nur der nächste Termin — hält den Weg zu FSRS/Auto-Bewertung offen (ADR-0005).

## Bewusste Grenzen (MVP-Verzicht)

- **Session-State überlebt keine Konfigurationsänderung/Prozess-Tod** (Autor-Entscheid 2026-08-05, Backlog „Code-Qualität"): Alle Screen-States leben in `remember` ohne `Saver`/ViewModel — eine bewusste Entscheidung, keine offene Lücke. Rotation oder System-Kill beendet die laufende Runde; sie startet beim nächsten Öffnen neu. Bereits abgegebene Bewertungen und SRS-Fälligkeiten sind sofort persistiert (ADR-0004) und bleiben erhalten. Eine ViewModel-/SavedState-Schicht wäre bei der flachen Navigation vergleichsweise billig, trägt aber in dieser Phase keinen Nutzwert — sie bleibt ein Backlog-Vorschlag für den Fall, dass Sitzungs-Unterbrechungen im Alltag wirklich stören.

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
