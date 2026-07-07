# Backlog AudioLex

Stand 2026-07-07: initiales Backlog aus der Konzept-Roadmap abgeleitet, Meilensteine (M0–M5) statt Sprints. Arbeitsreihenfolge `P0` → `P1` → `P2` → `P3`. Tags: `[KLÄRUNG]` = braucht Entscheid des Autors, `[PROP]` = Vorschlag über den aktuellen Scope hinaus.

## M0 – Gerüst & Governance

- [x] [P0] [Governance] Schlanke Governance aufgesetzt (AGENTS.md, CLAUDE.md, Backlog, Umsetzungslog, ADR-Struktur). Hinweis: wegrose-Apparat bewusst reduziert, siehe ADR-0002.
- [x] [P0] [ADR] Technologiewahl entschieden und dokumentiert. Hinweis: KMP + Compose Multiplatform statt nativ Android, siehe ADR-0001.
- [x] [P0] [Setup] KMP-Projektskeleton baut: `:core` (SRS + Audio-Mixer mit Tests) und `:composeApp` (Android + Desktop). Hinweis: 13 Unit-Tests grün (`:core:jvmTest`), Debug-APK gebaut, Desktop-Target kompiliert; JDK via Foojay-Resolver, Android SDK unter `~/Android/Sdk`.
- [x] [P0] [Setup] adb-Verbindung WSL2 → Galaxy A53 einrichten und Debug-APK installieren. Hinweis: WLAN-adb (Drahtlos-Debugging, Pairing + Connect), App installiert und startbar; Vorgehen im Umsetzungslog dokumentiert.
- [ ] [P2] [Setup] GitHub-Remote anlegen und pushen (sobald gewünscht).

## M1 – Audio-Grundgerüst

- [ ] [P0] [KLÄRUNG] Audioquelle für Testkorpus: TTS-Start (schnell, z. B. lokale Piper-Stimmen) vs. eigene Aufnahmen (mehrere Sprecher, natürliches Tempo). Empfehlung: TTS für M1, echte Aufnahmen parallel aufbauen.
- [ ] [P1] [Audio] WAV-Loader (PCM16) in `:core` implementieren + Tests.
- [ ] [P1] [Audio] Desktop-Sink verifizieren: hörbare Wiedergabe eines Testtons/-worts unter WSLg/PulseAudio.
- [ ] [P1] [Audio] Android-Sink auf Galaxy A53 verifizieren: AudioTrack-Lebenszyklus sauber (release nach Wiedergabe), Kanaltrennung real mit Hörgerät testen.
- [ ] [P1] [Audio] Kanalsteuerung links/rechts/beide + getrennte Pegel als Session-Parameter bis in die UI durchstechen.
- [ ] [P1] [Korpus] Minimaler Testkorpus 10–20 Wörter mit Metadaten (Silben, Kategorie) in `corpus-data/`.

## M2 – Lernmodus

- [ ] [P1] [Session] Sitzungssteuerung (Wortliste, Fortschritt, Wiederholungszahl vor Identifikation) als Paket `session` in `:core`.
- [ ] [P1] [UI] Lernmodus-Screen: Audio + Text simultan, Weiter/Wiederholen-Navigation.

## M3 – Prüfmodus + SRS

- [ ] [P1] [ADR] Persistenz-Entscheid bestätigen und umsetzen (ADR-0004: Room KMP vs. SQLDelight).
- [ ] [P1] [UI] Prüfmodus: verdeckte Karte, Aufdecken, 5-stufige Bewertung (Sofort/Bald/Später/Gut/Perfekt).
- [ ] [P1] [SRS] Fälligkeits-Persistenz + Review-Queue; `FixedIntervalScheduler` anbinden.

## M4 – Störgeräusche & Szenarien

- [ ] [P2] [KLÄRUNG] Störgeräusch-Quellen: selbst aufnehmen vs. freie Quellen (Lizenz auch bei nicht-kommerzieller Nutzung sauber dokumentieren).
- [ ] [P2] [Audio] Störgeräusch-Overlay mit SNR-Regler, mindestens 1 Szenario (Mixer ist vorbereitet: `noiseGainForSnr`).
- [ ] [P2] [Settings] Szenario-Presets Einfach/Schwierig/Fortgeschritten (`SettingsProfile`), Parameter-Persistenz.

## M5 – Wortkomplexität & Phonetik

- [ ] [P2] [Korpus] Silbenzahl-Filter + Kategorien (Alltag/Fremdwort/fremdsprachig).
- [ ] [P2] [KLÄRUNG] Phonetische Gruppierung: manuell kuratierte Minimalpaare (präzise, Pflegeaufwand) vs. algorithmische Distanz (z. B. Kölner Phonetik). Empfehlung: manuell kuratiert starten, Algorithmus als [PROP].

## Später / Vorschläge

- [ ] [P3] [PROP] [SRS] Automatische Bewertungsableitung (Reaktionszeit/Wiederholungszahl) statt manueller Wahl; ggf. FSRS statt fester Intervalltabelle.
- [ ] [P3] [PROP] [Produkt] Vokabeltrainer-Modus (generische Korpus-Nutzung für Fremdsprachen).
- [ ] [P3] [PROP] [Plattform] iOS-Target aktivieren (braucht macOS-Build-Host; Modulschnitt ist vorbereitet, siehe ADR-0001).
- [ ] [P3] [PROP] [Repo] Git-LFS o. ä. für `corpus-data/`, sobald echte Audiodateien versioniert werden sollen.
