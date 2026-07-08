# Backlog AudioLex

Stand 2026-07-07: initiales Backlog aus der Konzept-Roadmap abgeleitet, Meilensteine (M0–M5) statt Sprints. Arbeitsreihenfolge `P0` → `P1` → `P2` → `P3`. Tags: `[KLÄRUNG]` = braucht Entscheid des Autors, `[PROP]` = Vorschlag über den aktuellen Scope hinaus.

## M0 – Gerüst & Governance

- [x] [P0] [Governance] Schlanke Governance aufgesetzt (AGENTS.md, CLAUDE.md, Backlog, Umsetzungslog, ADR-Struktur). Hinweis: wegrose-Apparat bewusst reduziert, siehe ADR-0002.
- [x] [P0] [ADR] Technologiewahl entschieden und dokumentiert. Hinweis: KMP + Compose Multiplatform statt nativ Android, siehe ADR-0001.
- [x] [P0] [Setup] KMP-Projektskeleton baut: `:core` (SRS + Audio-Mixer mit Tests) und `:composeApp` (Android + Desktop). Hinweis: 13 Unit-Tests grün (`:core:jvmTest`), Debug-APK gebaut, Desktop-Target kompiliert; JDK via Foojay-Resolver, Android SDK unter `~/Android/Sdk`.
- [x] [P0] [Setup] adb-Verbindung WSL2 → Galaxy A53 einrichten und Debug-APK installieren. Hinweis: WLAN-adb (Drahtlos-Debugging, Pairing + Connect), App installiert und startbar; Vorgehen im Umsetzungslog dokumentiert.
- [x] [P1] [Governance] SDD-Grundlagen angelegt: `SOUL.md` (Haltung/Tonalität), `DESIGN.md` (UI/UX-Konzeption), `docs/szenarien.md` (Szenario-Katalog als Quelle der UI-ACs ab M2). Hinweis: 11 Szenarien aus Konzept §3 abgeleitet; 3 offene Szenarien brauchen Entscheid des Autors (Referenz-Trainings-Setup, Statistik-Umfang, Unterbrechungsverhalten) — siehe Katalog.
- [ ] [P2] [Setup] GitHub-Remote anlegen und pushen (sobald gewünscht).

## M1 – Audio-Grundgerüst

- [ ] [P0] [Review] Opus-Review vom 2026-07-07 abarbeiten: `docs/reviews/2026-07-07-m1-audio-review.md` — **vor weiterer Audio-/StereoGain-Arbeit**. Kernpunkte: (1) `swapStereoChannels`-Evidenz ist kontaminiert (Ursprungsbeobachtung physikalisch unmöglich, System-Balance rechts=0 nie zurückgesetzt, BT-Mono-Mix macht Swap unfalsifizierbar) → sauberes Re-Test-Protokoll, dann Fix behalten/entfernen/zum Setting machen; (2) Kanaltrennung über einzelnes BT-Hörgerät geht konzeptionell nicht → Referenz-Trainings-Setup als [KLÄRUNG] mit dem Autor; dazu kleinere Punkte (Log-Daten, Clone-Bootstrap, M2-Anforderungen). Hinweis (Update 2026-07-08): Befund 2 geklärt — Referenz-Setup ist BT-Hörgerät links, ADR-0007; ebenfalls erledigt: Playback-Queue als M2-Anforderung notiert, Backlog-Überschneidung Kanalsteuerung aufgelöst (nach M4 verschoben). Offen: Re-Test-Protokoll (Befund 1), Log-Daten, Clone-Bootstrap, App.kt-Trennung, zweite Stimme-Optionen, DoD-Drift.

- [x] [P0] [KLÄRUNG] Audioquelle für Testkorpus entschieden. Hinweis: lokales TTS (Piper), 2–3 Hochdeutsch-Stimmen (männlich/weiblich) für M1; Dialekte als vorbereitetes `locale`-Feld, Befüllung später — siehe ADR-0006.
- [x] [P1] [Korpus] `tools/generate_tts.py`: Piper via uv installiert, Generierungsskript schreibt WAV + `recordings.json`. Hinweis: native Piper-Rate 22050 Hz statt der ursprünglich angenommenen 48 kHz — ADR-0003 entsprechend korrigiert.
- [x] [P1] [Korpus] Minimaler Testkorpus 18 Wörter (thorsten-medium, 18 Aufnahmen) mit Metadaten (Silben, Kategorie, 2 Minimalpaare) in `words.json` + `recordings.json`. Hinweis: Audio selbst bleibt ungetrackt (`.gitignore`), nur Metadaten versioniert; Korpus liegt jetzt unter `composeApp/src/commonMain/composeResources/files/corpus/` statt Top-Level `corpus-data/` (Zugriff per `Res.readBytes(...)` auf jedem Target, auch Android ohne Repo-Dateisystempfad).
- [ ] [P1] [Korpus] [KLÄRUNG] Zweite (weibliche) Stimme fehlt noch. Hinweis: `kerstin-low` sprach isolierte Einzelwörter zu schnell/gestaucht (Bug vom Nutzer entdeckt: "Ball" klang unverständlich, voller Testsatz mit derselben Stimme war klar) — vermutlich Qualitätsstufen-Limit (Piper hat für deutsche Einzelsprecherinnen nur low/x_low, kein medium). Trägersatz-Workaround verworfen (unzuverlässiges Trimming). Optionen: bessere Piper-Stimme abwarten, andere TTS-Engine, oder echte Aufnahme — siehe ADR-0006.
- [x] [P1] [Audio] WAV-Loader (PCM16) in `:core` implementieren + Tests. Hinweis: `WavFile.decode` (chunk-walking, robust gegen Zusatz-Chunks wie `LIST`), 8 Tests (synthetische Fixtures + eine echte Piper-Datei als committtete Testressource `core/src/jvmTest/resources/`).
- [x] [P1] [Audio] Desktop-Sink verifizieren: hörbare Wiedergabe eines Testworts unter WSLg/PulseAudio. Hinweis: Java Sound (ALSA) findet unter WSL2 keine Line — Sink nutzt jetzt `paplay` als externen Prozess (javax.sound-Fallback bleibt für andere Umgebungen); Wiedergabe von "Ball" (thorsten) real bestätigt, klar verständlich.
- [x] [P1] [Audio] Android-Sink auf Galaxy A53 verifizieren: AudioTrack-Lebenszyklus sauber (release nach Wiedergabe), Kanaltrennung real mit Hörgerät testen. Hinweis: Wiedergabe funktioniert (erstes „nichts zu hören" war eine getrennte BT-Hörgerät-Lautstärkeeinstellung, kein Code-Fehler); dabei eine echte L/R-Kanalvertauschung auf diesem Gerät gefunden und behoben (`swapStereoChannels` in `AudioSink.android.kt`), bestätigt über zwei unabhängige Ausgabewege (BT-Hörgerät und Kabel-Kopfhörer, je mit zwei unterschiedlichen Wörtern pro Ohr getestet).

## M2 – Lernmodus

- [ ] [P1] [Session] Sitzungssteuerung (Wortliste, Fortschritt, Wiederholungszahl vor Identifikation) als Paket `session` in `:core`. Hinweis: Verlassen beendet die Sitzung sauber — kein Pause/Resume (Entscheid des Autors 2026-07-08, Szenario S5); braucht Playback-Queue mit Cancel-Semantik statt paralleler Coroutinen (Review-Hinweis, `Thread.sleep` im Sink ersetzen).
- [ ] [P1] [UI] Lernmodus-Screen: Audio + Text simultan, Weiter/Wiederholen-Navigation.

## M3 – Prüfmodus + SRS

- [ ] [P1] [ADR] Persistenz-Entscheid bestätigen und umsetzen (ADR-0004: Room KMP vs. SQLDelight).
- [ ] [P1] [UI] Prüfmodus: verdeckte Karte, Aufdecken, 5-stufige Bewertung (Sofort/Bald/Später/Gut/Perfekt).
- [ ] [P1] [SRS] Fälligkeits-Persistenz + Review-Queue; `FixedIntervalScheduler` anbinden.
- [ ] [P2] [UI] Sitzungshistorie: Liste abgeschlossener Sitzungen mit Datum/Uhrzeit (mehrere pro Tag), je Sitzung Modus + Kennzahlen. Hinweis: Entscheid des Autors 2026-07-08 (sitzungsbasierte Statistik, Szenario S12); braucht Session-Persistenz.

## M4 – Störgeräusche & Szenarien

- [ ] [P2] [KLÄRUNG] Störgeräusch-Quellen: selbst aufnehmen vs. freie Quellen (Lizenz auch bei nicht-kommerzieller Nutzung sauber dokumentieren).
- [ ] [P2] [Audio] Störgeräusch-Overlay mit SNR-Regler, mindestens 1 Szenario (Mixer ist vorbereitet: `noiseGainForSnr`).
- [ ] [P2] [Settings] Szenario-Presets Einfach/Schwierig/Fortgeschritten (`SettingsProfile`), Parameter-Persistenz.
- [ ] [P2] [Settings] Kanalwahl links/rechts/beide + getrennte Pegel als Setup-Option für Alternativ-Hardware (Kabel-Kopfhörer); bei BT-Ausgabe als wirkungslos kennzeichnen. Hinweis: per ADR-0007 herabgestuft (vormals M1/P1 „bis in die UI durchstechen") — Referenz-Setup ist BT-Hörgerät links, dort mono summiert; löst die Review-Anmerkung zur Backlog-Überschneidung.

## M5 – Wortkomplexität & Phonetik

- [ ] [P2] [Korpus] Silbenzahl-Filter + Kategorien (Alltag/Fremdwort/fremdsprachig).
- [ ] [P2] [KLÄRUNG] Phonetische Gruppierung: manuell kuratierte Minimalpaare (präzise, Pflegeaufwand) vs. algorithmische Distanz (z. B. Kölner Phonetik). Empfehlung: manuell kuratiert starten, Algorithmus als [PROP].
- [ ] [P2] [Korpus] Dialekt-Aufnahmen für Fortgeschritten-Modus beschaffen (`AudioRecording.locale` ist vorbereitet, ADR-0006): Quelle klären — dialektfähige TTS finden oder eigene Aufnahmen; ohne Dialekt-Pool bleibt der Fortgeschritten-Filter auf Hochdeutsch beschränkt.

## Später / Vorschläge

- [ ] [P3] [PROP] [SRS] Automatische Bewertungsableitung (Reaktionszeit/Wiederholungszahl) statt manueller Wahl; ggf. FSRS statt fester Intervalltabelle.
- [ ] [P3] [PROP] [Produkt] Vokabeltrainer-Modus (generische Korpus-Nutzung für Fremdsprachen).
- [ ] [P3] [PROP] [Plattform] iOS-Target aktivieren (braucht macOS-Build-Host; Modulschnitt ist vorbereitet, siehe ADR-0001).
- [ ] [P3] [PROP] [Repo] Git-LFS o. ä. für `corpus-data/`, sobald echte Audiodateien versioniert werden sollen.
