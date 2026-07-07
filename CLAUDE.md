# CLAUDE.md

Projektkontext für Claude Code. Der verbindliche Arbeitsmodus (Backlog-Steuerung, Definition of Done, Konventionen, Source of Truth) steht in `AGENTS.md` — dort nachlesen, hier nicht dupliziert.

## Was AudioLex ist

Hörtrainings-App für den Autor selbst (ca. 80 % einseitiger Hörverlust, Hörgeräteträger). Das Problem ist neurologisch: Schall kommt an, wird aber nicht als Sprache decodiert. Training über zwei Modi: **Lernmodus** (Wort hören + Text sehen, baut die Assoziation auf) und **Prüfmodus** (Wort hören, verdeckte Karte, selbst bewerten → Spaced Repetition steuert Wiederholung).

## Architektur-Kurzfassung

- `:core` — KMP-Bibliothek (androidTarget + jvm), plattformfreie Logik in Paketen `srs`, `audio`, `corpus`, `session`. Vollständig JVM-unit-testbar.
- `:composeApp` — Compose-Multiplatform-UI (Android + Desktop). Desktop ist das Dev-Target unter WSL2.
- Audio: PCM-Mixing (Kanal-Pegel, Störgeräusch/SNR) in Common-Kotlin; nur die Ausgabe ist expect/actual (`AudioSink`: Android AudioTrack, Desktop javax.sound, iOS später AVAudioEngine).
- Details: `docs/architektur.md` · Entscheidungen: `docs/adr/`

## Befehle

```bash
./gradlew :core:jvmTest              # Kernlogik-Tests (schnellste Schleife)
./gradlew :composeApp:run            # Desktop-App starten
./gradlew :composeApp:assembleDebug  # Android-APK
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

## Domänenwissen

- **SRS-Skala (MVP, feste Intervalle, ADR-0005):** Sofort 1 min · Bald 10 min · Später 1 Tag · Gut 1 Woche · Perfekt 1 Monat. Implementiert in `FixedIntervalScheduler`; UI-Labels Deutsch, Code-Enum `ReviewRating` (AGAIN/SOON/LATER/GOOD/PERFECT).
- **Kanalsteuerung ist Kernfeature** (einseitiger Hörverlust): links/rechts/beide mit getrennten Pegeln — bei jedem Audio-Feature mitdenken (`StereoGain`).
- **Wortkorpus generisch**: `Word` getrennt von `AudioRecording` (mehrere Sprecher pro Wort) und von `ReviewCard` (SRS-Zustand). Nichts Hörverlust-spezifisches ins Modell verdrahten.
- Störgeräusch-Overlay: vorproduzierte Loops, Mischung über SNR (dB) — `noiseGainForSnr` im Mixer.

## Stand & offene Klärungen

- Aktueller Stand: `docs/umsetzungslog.md` (oben) und `docs/backlog.md`.
- `[KLÄRUNG]`-Items im Backlog brauchen einen Entscheid des Autors (z. B. Audioquelle TTS vs. eigene Aufnahmen vor M1).
