# Backlog AudioLex

Stand 2026-07-07: initiales Backlog aus der Konzept-Roadmap abgeleitet, Meilensteine (M0–M5) statt Sprints. Arbeitsreihenfolge `P0` → `P1` → `P2` → `P3`. Tags: `[KLÄRUNG]` = braucht Entscheid des Autors, `[PROP]` = Vorschlag über den aktuellen Scope hinaus.

Modell-Richtwert je offenem Item (AGENTS.md §6): `[→Sonnet]` = fertig spezifiziert (Akzeptanzkriterien + Nicht-Ziele stehen im Item), direkt umsetzbar · `[→Opus]` = Architektur-/Design-Entscheid nötig · `[→Opus→Sonnet]` = erst schärfen (ACs + Nicht-Ziele ergänzen), dann umsetzen. Ohne Tag: braucht den Autor. Faustregel: Ein Item ohne ausformulierte ACs ist nie Sonnet-reif — der nächste Schritt heißt dann „schärfen", nicht „umsetzen".

## M0 – Gerüst & Governance

- [x] [P0] [Governance] Schlanke Governance aufgesetzt (AGENTS.md, CLAUDE.md, Backlog, Umsetzungslog, ADR-Struktur). Hinweis: wegrose-Apparat bewusst reduziert, siehe ADR-0002.
- [x] [P0] [ADR] Technologiewahl entschieden und dokumentiert. Hinweis: KMP + Compose Multiplatform statt nativ Android, siehe ADR-0001.
- [x] [P0] [Setup] KMP-Projektskeleton baut: `:core` (SRS + Audio-Mixer mit Tests) und `:composeApp` (Android + Desktop). Hinweis: 13 Unit-Tests grün (`:core:jvmTest`), Debug-APK gebaut, Desktop-Target kompiliert; JDK via Foojay-Resolver, Android SDK unter `~/Android/Sdk`.
- [x] [P0] [Setup] adb-Verbindung WSL2 → Galaxy A53 einrichten und Debug-APK installieren. Hinweis: WLAN-adb (Drahtlos-Debugging, Pairing + Connect), App installiert und startbar; Vorgehen im Umsetzungslog dokumentiert.
- [x] [P1] [Governance] SDD-Grundlagen angelegt: `SOUL.md` (Haltung/Tonalität), `DESIGN.md` (UI/UX-Konzeption), `docs/szenarien.md` (Szenario-Katalog als Quelle der UI-ACs ab M2). Hinweis: 11 Szenarien aus Konzept §3 abgeleitet; 3 offene Szenarien brauchen Entscheid des Autors (Referenz-Trainings-Setup, Statistik-Umfang, Unterbrechungsverhalten) — siehe Katalog.
- [ ] [P2] [Setup] GitHub-Remote anlegen und pushen (sobald gewünscht).

## M1 – Audio-Grundgerüst

- [x] [P0] [Review] Opus-Review vom 2026-07-07 abarbeiten: `docs/reviews/2026-07-07-m1-audio-review.md` — **vor weiterer Audio-/StereoGain-Arbeit**. Hinweis (Update 2026-07-08): Alle 9 Punkte erledigt, inklusive Befund 1 (Re-Test-Protokoll, siehe eigenes Item direkt unten) und Befund 2 (Referenz-Setup, ADR-0007).

- [x] [P0] [KLÄRUNG] Audioquelle für Testkorpus entschieden. Hinweis: lokales TTS (Piper), 2–3 Hochdeutsch-Stimmen (männlich/weiblich) für M1; Dialekte als vorbereitetes `locale`-Feld, Befüllung später — siehe ADR-0006.
- [x] [P1] [Korpus] `tools/generate_tts.py`: Piper via uv installiert, Generierungsskript schreibt WAV + `recordings.json`. Hinweis: native Piper-Rate 22050 Hz statt der ursprünglich angenommenen 48 kHz — ADR-0003 entsprechend korrigiert.
- [x] [P1] [Korpus] Minimaler Testkorpus 18 Wörter (thorsten-medium, 18 Aufnahmen) mit Metadaten (Silben, Kategorie, 2 Minimalpaare) in `words.json` + `recordings.json`. Hinweis: Audio selbst bleibt ungetrackt (`.gitignore`), nur Metadaten versioniert; Korpus liegt jetzt unter `composeApp/src/commonMain/composeResources/files/corpus/` statt Top-Level `corpus-data/` (Zugriff per `Res.readBytes(...)` auf jedem Target, auch Android ohne Repo-Dateisystempfad).
- [ ] [P1] [Korpus] [KLÄRUNG] Zweite (weibliche) Stimme fehlt noch. Hinweis: `kerstin-low` sprach isolierte Einzelwörter zu schnell/gestaucht (Bug vom Nutzer entdeckt: "Ball" klang unverständlich, voller Testsatz mit derselben Stimme war klar) — vermutlich Qualitätsstufen-Limit (Piper hat für deutsche Einzelsprecherinnen nur low/x_low, kein medium). Trägersatz-Workaround verworfen (unzuverlässiges Trimming). Optionen (Review-Ergänzung 2026-07-08):
  1. **`de_DE-thorsten_emotional-medium`** (medium-Qualität, 8 Sprechvarianten desselben Sprechers) als Zwischenlösung für hörbare Varianz — bleibt männlich/gleicher Sprecher, löst also nicht den Wunsch nach einer zweiten Stimme im eigentlichen Sinn, aber medium-Qualität ist sofort verfügbar und ohne die Kerstin-Stauchung.
  2. **Andere Offline-TTS-Engine prüfen** (z. B. Coqui TTS/XTTS) auf bessere deutsche Frauenstimmen in Einzelwort-Qualität — ungetestet, zusätzlicher Tooling-Aufwand.
  3. Bessere Piper-Stimme abwarten oder echte Aufnahme — siehe ADR-0006.
  Noch kein Entscheid des Autors; Umsetzung folgt erst nach Klärung.
- [x] [P1] [Audio] WAV-Loader (PCM16) in `:core` implementieren + Tests. Hinweis: `WavFile.decode` (chunk-walking, robust gegen Zusatz-Chunks wie `LIST`), 8 Tests (synthetische Fixtures + eine echte Piper-Datei als committtete Testressource `core/src/jvmTest/resources/`).
- [x] [P1] [Audio] Desktop-Sink verifizieren: hörbare Wiedergabe eines Testworts unter WSLg/PulseAudio. Hinweis: Java Sound (ALSA) findet unter WSL2 keine Line — Sink nutzt jetzt `paplay` als externen Prozess (javax.sound-Fallback bleibt für andere Umgebungen); Wiedergabe von "Ball" (thorsten) real bestätigt, klar verständlich.
- [x] [P1] [Audio] Android-Sink auf Galaxy A53 verifizieren: AudioTrack-Lebenszyklus sauber (release nach Wiedergabe), Kanaltrennung real mit Hörgerät testen. Hinweis: Wiedergabe funktioniert (erstes „nichts zu hören" war eine getrennte BT-Hörgerät-Lautstärkeeinstellung, kein Code-Fehler). Ursprünglich hier eine vermeintliche L/R-Kanalvertauschung gefunden und mit `swapStereoChannels` „behoben" — dieser Befund war falsch (kontaminierte Evidenz), siehe Re-Test-Protokoll unten. Aktueller Stand: Kanaltrennung ohne Swap korrekt (links→links, rechts→rechts, sauber getrennt).
- [x] [P0] [Review] **Re-Test-Protokoll `swapStereoChannels`** (Opus-Review Befund 1) durchgeführt 2026-07-08. Hinweis: Klangbalance-Sonderregelung deaktiviert (gemeinsame statt getrennter Lautstärke), USB-C-Kabel-Kopfhörer statt BT, Debug-Build mit explizitem Swap-Toggle im Dev-Screen. Ohrmuschel-Test (Kopfhörermuscheln nacheinander ans gesunde Ohr gehalten, umgeht die Einseitigkeit des Gehörs): Swap AUS → „links"/„rechts" kommen korrekt und sauber getrennt aus der jeweiligen Muschel; Swap AN → seitenvertauscht. Ergebnis: Das Gerät war nie fehlerhaft, `swapStereoChannels` war selbst der Bug. Fix zurückgebaut (`AndroidAudioSink` wendet Swap nicht mehr standardmäßig an), Funktion + Tests bleiben als Grundlage für ein mögliches künftiges Setting (`createAudioSinkWithSwapOverride`, siehe M4-Item), ADR-0003 und Umsetzungslog korrigiert.
- [ ] [P2] [Settings] [→Opus→Sonnet] „Kanäle tauschen" als Nutzer-Setting statt hart verdrahtetem Gerätefix (Opus-Review Befund 1) — legitime Barrierefreiheits-Option für Hörgeräteträger. Hinweis: Grundgerüst existiert bereits (`createAudioSinkWithSwapOverride` in `AudioSink.android.kt`, nicht verdrahtet); Umsetzung als UI-Setting nach Bedarf.

## M2 – Lernmodus

- [x] [P1] [Session] Sitzungssteuerung (Wortliste, Fortschritt, Wiederholungszahl vor Identifikation) als Paket `session` in `:core`. Hinweis: `LearningSession` (linearer Fortschritt, Repeat/Advance, Szenario S1/S2) + `PlaybackQueue` (neuer `play()` bricht den vorigen ab) implementiert und getestet. Dabei echten Abbruch-Bug in `AudioSink` gefunden und behoben: `play()` ist jetzt `suspend fun`, Android/Desktop reagieren kooperativ auf Cancellation statt die Wiedergabe im Hintergrund zu Ende laufen zu lassen (`Thread.sleep`/`paplay-waitFor` waren nicht abbrechbar) — siehe ADR-0003 und Umsetzungslog 2026-07-09. Verlassen beendet die Sitzung sauber, kein Pause/Resume (Entscheid des Autors 2026-07-08, Szenario S5).
- [ ] [P1] [UI] [→Sonnet] Navigations-Gerüst: `App.kt` wird zur flachen Drehscheibe (DESIGN.md „Screenstruktur"). Umfang: Start-Screen mit primärem Einstieg „Lernmodus starten" und sekundärem Zugang „Kanaltest (Dev)" zum bestehenden `DevPlaybackScreen` (bleibt als Smoke-Test erhalten, DESIGN.md „Entschieden"); Trainings-Screens sind Sackgassen mit „Beenden" zurück zum Start. Nicht-Ziele: kein Navigation-Framework/-Library — ein simpler Screen-Zustand (`mutableStateOf` + `when`) reicht bei maximal zwei Ebenen; keine Buttons für Prüfmodus/Einstellungen/Statistik anlegen (kommen mit M3/M4, keine toten Stubs).
- [x] [P1] [Audio] `WavFile.decode` gegen korrupte Eingaben absichern. Hinweis: Guard prüft jetzt `chunkSize >= 0 && chunkDataStart + chunkSize <= bytes.size` direkt nach dem Chunk-Header (statt erst beim nächsten Offset-Sprung zu scheitern), zusätzlich `fmt`-Chunk-Mindestgröße (16 Byte) gegen abgeschnittene Header. 3 neue Tests (negative/übergroße `data`-Chunk-Größe, abgeschnittener `fmt`-Chunk), alle 10 `WavFileTest`-Fälle grün.
- [ ] [P1] [UI] [→Sonnet] Lernmodus-Screen (Szenarien S1, S2, S5, S7; Gestalt: DESIGN.md „Trainings-Screens im Detail"). Vorhandene Bausteine verwenden: `LearningSession` (immutable, `advance()` liefert `null` am Ende), `PlaybackQueue` (Abspielen **ausschließlich** hierüber — nicht `sink.play()` direkt aus dem Screen launchen; `DevPlaybackScreen` ist dafür keine Vorlage, er stammt von vor `PlaybackQueue`), Korpus-Laden per `Res.readBytes` auf `words.json`/`recordings.json` (Muster in `DevPlaybackScreen`), `WavFile.decode`, `createAudioSink()`. Akzeptanzkriterien (Pflichtprüfung `docs/szenarien.md`):
  - Happy Path (S1): Sitzung enthält alle Korpuswörter in Korpusreihenfolge; beim Eintritt in ein Wort spielt es automatisch einmal, der Text steht dabei groß und positionsstabil in der Bildmitte; „Weiter" wechselt zum nächsten Wort; nach dem letzten Wort ein Abschlusszustand („18 Wörter durchlaufen") mit „Zurück zum Start".
  - Wiederholen (S2): „Wiederholen" spielt das aktuelle Wort erneut über `PlaybackQueue.play` (bricht eine noch laufende Wiedergabe hörbar ab); Fortschritt dezent als „7 / 18".
  - Unterbrechung (S5): „Beenden" jederzeit möglich → `PlaybackQueue.stop()`, zurück zum Start; laufende Wiedergabe verstummt sofort; kein Pause/Resume-Zustand.
  - Audio gestört / Fehlerzustand (S7): Exception beim Laden oder Abspielen (fehlende Datei, korruptes WAV, Sink-Fehler) → verständliche deutsche Meldung im Screen statt stillem Fehlschlag oder Absturz.
  - Leer-Zustand: Korpus leer oder aktuelles Wort ohne Aufnahme → Meldung mit Ausweg (zurück zum Start), keine leere Sitzung starten.
  - Nicht-Ziele: keine Persistenz/SRS (M3), keine Einstellungen und keine Wiederholungs-Obergrenze (Konzept 3.3 „konfigurierbar" braucht Settings → M4), kein `ChannelBadge` (braucht Ausgabeweg-Erkennung, kommt mit den M4-Settings — DESIGN-Leitprinzip 3 wird dort eingelöst), kein Mischen der Wortreihenfolge (M5 Wortfilter).
  - Verifikation: Desktop (`./gradlew :composeApp:run`) hörbar durchspielen inkl. Doppel-Tap auf „Wiederholen"; Fehlerpfad provozieren (z. B. WAV temporär wegbenennen).
- [ ] [P1] [Gerätetest] Wiedergabe-Abbruch real auf dem Galaxy A53 hören (nach dem Lernmodus-Screen; braucht den Autor, ~5 min): im Lernmodus „Wiederholen" zweimal schnell antippen — die erste Wiedergabe muss hörbar abbrechen statt zu Ende zu spielen; zusätzlich einmal mitten im Wort „Beenden". Hinweis: Codeseitig umgesetzt und unit-getestet (Commit `5525848`), die Hörprobe ist der offene Rest der DoD vom 2026-07-09; BT-Latenz des Hörgeräts einkalkulieren.

## M3 – Prüfmodus + SRS

- [ ] [P1] [ADR] [→Opus] Persistenz-Entscheid bestätigen und umsetzen (ADR-0004: Room KMP vs. SQLDelight).
- [ ] [P1] [UI] [→Opus→Sonnet] Prüfmodus: verdeckte Karte, Aufdecken, 5-stufige Bewertung (Sofort/Bald/Später/Gut/Perfekt). Hinweis: vor Umsetzung analog zum Lernmodus-Screen schärfen (Szenarien S3/S4/S5, DESIGN.md `RevealCard`/`RatingBar`, Bausteine, Nicht-Ziele); braucht den Persistenz-Entscheid (Item oben).
- [ ] [P1] [SRS] [→Sonnet] Fälligkeits-Persistenz + Review-Queue; `FixedIntervalScheduler` anbinden. Hinweis: erst nach ADR-0004 (Item oben) — der Entscheid legt die Umsetzung fest, danach ist das klar spezifizierte Anbindungsarbeit.
- [ ] [P2] [UI] [→Opus→Sonnet] Sitzungshistorie: Liste abgeschlossener Sitzungen mit Datum/Uhrzeit (mehrere pro Tag), je Sitzung Modus + Kennzahlen. Hinweis: Entscheid des Autors 2026-07-08 (sitzungsbasierte Statistik, Szenario S12); braucht Session-Persistenz.

## M4 – Störgeräusche & Szenarien

- [ ] [P2] [KLÄRUNG] Störgeräusch-Quellen: selbst aufnehmen vs. freie Quellen (Lizenz auch bei nicht-kommerzieller Nutzung sauber dokumentieren).
- [ ] [P2] [Audio] [→Opus→Sonnet] Störgeräusch-Overlay mit SNR-Regler, mindestens 1 Szenario (Mixer ist vorbereitet: `noiseGainForSnr`).
- [ ] [P2] [Settings] [→Opus→Sonnet] Szenario-Presets Einfach/Schwierig/Fortgeschritten (`SettingsProfile`), Parameter-Persistenz.
- [ ] [P2] [Settings] [→Opus→Sonnet] Kanalwahl links/rechts/beide + getrennte Pegel als Setup-Option für Alternativ-Hardware (Kabel-Kopfhörer); bei BT-Ausgabe als wirkungslos kennzeichnen. Hinweis: per ADR-0007 herabgestuft (vormals M1/P1 „bis in die UI durchstechen") — Referenz-Setup ist BT-Hörgerät links, dort mono summiert; löst die Review-Anmerkung zur Backlog-Überschneidung.

## M5 – Wortkomplexität & Phonetik

- [ ] [P2] [Korpus] [→Opus→Sonnet] Silbenzahl-Filter + Kategorien (Alltag/Fremdwort/fremdsprachig).
- [ ] [P2] [KLÄRUNG] Phonetische Gruppierung: manuell kuratierte Minimalpaare (präzise, Pflegeaufwand) vs. algorithmische Distanz (z. B. Kölner Phonetik). Empfehlung: manuell kuratiert starten, Algorithmus als [PROP].
- [ ] [P2] [Korpus] Dialekt-Aufnahmen für Fortgeschritten-Modus beschaffen (`AudioRecording.locale` ist vorbereitet, ADR-0006): Quelle klären — dialektfähige TTS finden oder eigene Aufnahmen; ohne Dialekt-Pool bleibt der Fortgeschritten-Filter auf Hochdeutsch beschränkt.

## Später / Vorschläge

- [ ] [P3] [PROP] [SRS] Automatische Bewertungsableitung (Reaktionszeit/Wiederholungszahl) statt manueller Wahl; ggf. FSRS statt fester Intervalltabelle.
- [ ] [P3] [PROP] [Produkt] Vokabeltrainer-Modus (generische Korpus-Nutzung für Fremdsprachen).
- [ ] [P3] [PROP] [Plattform] iOS-Target aktivieren (braucht macOS-Build-Host; Modulschnitt ist vorbereitet, siehe ADR-0001).
- [ ] [P3] [PROP] [Repo] Git-LFS o. ä. für `corpus-data/`, sobald echte Audiodateien versioniert werden sollen.
