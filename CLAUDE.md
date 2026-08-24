# CLAUDE.md

Projektkontext für Claude Code. Der verbindliche Arbeitsmodus (Backlog-Steuerung, Definition of Done, Konventionen, Source of Truth) steht in `AGENTS.md` — dort nachlesen, hier nicht dupliziert.

## Was AudioLex ist

Hörtrainings-App für den Autor selbst (ca. 80 % einseitiger Hörverlust, Hörgeräteträger). Das Problem ist neurologisch: Schall kommt an, wird aber nicht als Sprache decodiert. Training über zwei Modi: **Lernmodus** (Wort hören + Text sehen, baut die Assoziation auf) und **Prüfmodus** (Wort hören, verdeckte Karte, selbst bewerten → Spaced Repetition steuert Wiederholung).

## Architektur-Kurzfassung

- `:core` — KMP-Bibliothek (androidTarget + jvm), plattformfreie Logik in Paketen `srs`, `audio`, `corpus`, `session`, dazu `i18n` (UI-Textbestand, ADR-0015). Vollständig JVM-unit-testbar.
- `:composeApp` — Compose-Multiplatform-UI (Android + Desktop). Desktop ist das Dev-Target (nativ Debian, GNOME/Wayland).
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
- **Referenz-Trainings-Setup: BT-Hörgerät, linkes Ohr** (ADR-0007) — Stereo wird dort mono summiert; maßgeblich sind Pegel und Verständlichkeit am trainierten Ohr. Kanaltrennung links/rechts/beide (`StereoGain`) bleibt als Option für Alternativ-Setups (Kabel-Kopfhörer), ist über BT wirkungslos — die UI darf sie dort nicht als wirksam zeigen.
- **UI-Sprache Deutsch/Englisch (ADR-0015, seit v0.34.0):** Kein `strings.xml` — der Textbestand ist das getypte Interface `core/i18n/Strings.kt` mit `GermanStrings`/`EnglishStrings`; eine fehlende Übersetzung ist ein Compilerfehler. Screens lesen über `LocalStrings.current`. Neuer UI-Text = Interface erweitern + **beide** Kataloge füllen. Gewählte Sprache liegt in `AppSettings.uiLanguage`; Default `UiLanguage.SYSTEM` folgt der Gerätesprache (primärer Subtag, `de-AT` → Deutsch). Umschalter auf dem Startbildschirm, nicht in den Einstellungen. Ausgenommen: `DevPlaybackScreen` (Instrument, bleibt deutsch). **Der Korpus bleibt deutsch** — `UiLanguage` und `Word.language` sind verschiedene Dinge.
- **Wortkorpus generisch**: `Word` getrennt von `AudioRecording` (mehrere Sprecher pro Wort) und von `ReviewCard` (SRS-Zustand). Nichts Hörverlust-spezifisches ins Modell verdrahten.
- Störgeräusch-Overlay: Mischung über SNR (dB) — `noiseGainForSnr` im Mixer. Der Katalog hat zwei Hälften: gebündelt (`files/noise/`, versioniert) und die eigenen Geräusche des Nutzers (Aufnahme/WAV-Import, app-lokal). **Regel seit v0.33.0:** gebündelt werden nur Inhalte, deren Weitergabe erlaubt ist — praktisch eigene Aufnahmen; die drei zugekauften Loops sind weg (ADR-0014, ADR-0010 Nachträge). Leerer Katalog = sauberer Ton, kein Fehlerfall.
- **Ausgelieferte Audiodateien liegen im Repo** (68 Korpus-WAVs, seit v0.33.0): F-Droid baut aus dem Quelltext. Herkunft und Weitergaberecht gehören in die README des jeweiligen Ordners — ein Eintrag ohne Herkunftszeile ist ein Release-Blocker. Der Weg zur Aufnahme bei F-Droid: `docs/fdroid-anmeldung.md`.

## Stand & offene Klärungen

- Aktueller Stand: `docs/umsetzungslog.md` (oben) und `docs/backlog.md`.
- `[KLÄRUNG]`-Items im Backlog brauchen einen Entscheid des Autors (z. B. Audioquelle TTS vs. eigene Aufnahmen vor M1).
