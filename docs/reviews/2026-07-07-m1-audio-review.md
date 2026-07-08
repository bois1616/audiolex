# Review M1 Audio-Grundgerüst (Opus-Klasse, 2026-07-07)

Reviewer: Claude (Opus-Klasse), auf Wunsch des Autors nach Abschluss der Android-Sink-Verifikation.
Grundlage: `docs/umsetzungslog.md`, `docs/backlog.md`, Commits `977ad83…67ea1e2`.

**Auftrag an die nächste Sitzung (Sonnet):** Punkte in dieser Reihenfolge abarbeiten, Ergebnis je Punkt im Backlog/Log dokumentieren. Befund 1 und 2 vor jeder weiteren Feature-Arbeit an Audio/StereoGain.

---

## Befund 1 (P0): `swapStereoChannels` steht auf kontaminierter Evidenz — Fix ist wahrscheinlich falsch

Der „L/R-Vertauschungs-Bug auf dem Galaxy A53" (Commit `67ea1e2`) ist mit hoher Wahrscheinlichkeit **nicht existent**. Drei Gründe:

1. **Die Ursprungsbeobachtung ist physikalisch unmöglich.** Beim ersten Zwei-Wörter-Test („Ball links" → „Haus gehört") lief `playTwoWordsPerEar` mit `LEFT_ONLY = (1, 0)`: Die Haus-Samples wurden mit **0 multipliziert und waren im Buffer nicht vorhanden**. Egal wie das Gerät routet, mischt oder vertauscht — man kann kein Wort hören, dessen Samples null sind. Diese Beobachtung, der Keim der gesamten Swap-Theorie, muss ein Testfehler gewesen sein (falscher Button, Verwechslung beim schnellen Testen).

2. **Die System-Balance (rechter Kanal = 0) wurde mitten in der Diagnose gesetzt und nie kontrolliert zurückgesetzt.** Der Kabel-Kopfhörer-Test — die vermeintlich unabhängige Bestätigung — lief laut Nutzeraussage explizit *mit* diesem Setting (Samsung: Einstellungen → Eingabehilfe → Hörverbesserungen → Klangbalance; wirkt auf Kabel und BT). Das erklärt exakt beide Anomalien dieses Tests: „keine Überlagerung bei Beide" (rechter Kanal systemseitig stumm) und „seitenverkehrt" (mit aktivem Sink-Swap landet das Rechts-Wort auf dem einzig hörbaren linken Kanal). Der Test bestätigt nicht den Swap, sondern die stummgeschaltete Balance.

3. **Über das einzelne BT-Hörgerät ist der Swap unfalsifizierbar.** Der finale „alles korrekt"-Zustand (Ball→Ball, Haus→Haus, Beide→Überlagerung, alles links hörbar) ergibt sich bei Mono-Summierung im Hörgerät **mit und ohne Swap identisch** — bei „Ball links" ist ohnehin nur Ball im Signal.

Priori-Überlegung: Ein geräteweiter L/R-Tausch im Samsung-Audiostack würde jede Musik-/Video-App für jeden A53-Besitzer betreffen — so etwas überlebt keine OEM-QA.

### Aufgaben

- [x] **Sauberes Re-Test-Protokoll durchführen** (mit dem Autor):
  1. Klangbalance nachweislich auf Mitte (Einstellungen → Eingabehilfe → Hörverbesserungen), Screenshot/Bestätigung.
  2. Kabel-Kopfhörer (USB-C), kein BT.
  3. Ein Build mit **bekanntem** Swap-Zustand (dokumentieren, ob `swapStereoChannels` aktiv ist).
  4. Test: beide Ohrhörer **nacheinander physisch ans gesunde Ohr halten** — das umgeht die Einseitigkeit des Gehörs vollständig und macht „welche Muschel spielt welches Wort" objektiv prüfbar.
  → *Durchgeführt 2026-07-08: „Lautstärke getrennt anpassen" deaktiviert (gemeinsame Lautstärke statt möglicherweise verstelltem rechtem Kanal), USB-C-Kabel-Kopfhörer verbunden, Debug-Build mit einem Swap-Toggle im Dev-Screen (`createAudioSinkForSwapDiagnosis`, seitdem wieder entfernt) installiert. Ohrmuschel-Test mit Swap AN und AUS durchgeführt.*
- [x] **Konsequenz ziehen:** Reproduziert der Swap nicht → `swapStereoChannels` entfernen (inkl. Test, ADR-0003-Eintrag korrigieren, Log-Eintrag mit Richtigstellung). Reproduziert er doch → Befund hier dokumentiert widerlegen. → *Ergebnis: Swap AUS → „links"/„rechts" kommen korrekt aus der jeweiligen Muschel, sauber getrennt (kein Übersprechen). Swap AN → seitenvertauscht, wie erwartet, wenn eine funktionierende Kette künstlich vertauscht wird. Damit ist bestätigt: Befund 1 hatte recht, das Gerät war nie fehlerhaft. `swapStereoChannels` als Default entfernt (`AndroidAudioSink.swapChannels` jetzt `false`), Funktion selbst bleibt als Grundlage für ein künftiges Setting (siehe unten). ADR-0003 und Umsetzungslog korrigiert.*
- [x] **Unabhängig vom Ausgang:** „Kanäle tauschen" als **Nutzer-Setting** vorsehen (statt hart verdrahtetem Gerätefix) — für Hörgeräteträger eine legitime Barrierefreiheits-Option, und die Annahme bleibt revidierbar, ohne neu zu bauen. Als Backlog-Item (M4 Settings) einplanen, nicht sofort umsetzen. → *Erledigt: `createAudioSinkWithSwapOverride(Boolean)` als Grundgerüst in `AudioSink.android.kt` belassen, nicht in `createAudioSink()` verdrahtet; Backlog-Item M4 verweist darauf.*

---

## Befund 2 (P0, [KLÄRUNG]): Kanaltrennung über ein einzelnes BT-Hörgerät geht konzeptionell nicht

Wichtiger als Befund 1: BT-Audio erreicht **nur das Hörgerät** (linkes Ohr). Das gesunde rechte Ohr bekommt über diesen Pfad nie ein Signal. Das Kernfeature „links/rechts/beide mit getrennten Pegeln" — laut Konzept das zentrale Trainingsinstrument bei einseitigem Hörverlust — ist im BT-Setup strukturell wirkungslos.

### Aufgaben

- [x] Als eigenes `[KLÄRUNG]`-Item ins Backlog: **Was ist das Referenz-Trainings-Setup?** Optionen mit dem Autor klären: Kabel-Kopfhörer (über/unter dem Hörgerät getragen?), Lautsprecher, je Szenario unterschiedlich. Entscheidung beeinflusst M2 (Session-Parameter) und M4 (Presets, ggf. Warnhinweis in der App bei aktiver BT-Ausgabe). → *Erledigt 2026-07-08: Entscheid direkt mit dem Autor eingeholt (ohne Backlog-Umweg): Referenz-Setup ist das BT-Hörgerät am linken Ohr, Abspielgerät Smartphone (nicht ans A53 gebunden). Kanaltrennung wird Setup-Option für Alternativ-Hardware statt Kernfeature.*
- [x] Ergebnis als ADR oder ADR-Ergänzung festhalten (betrifft Audio-Pipeline und Produktkern). → *Erledigt: ADR-0007; SOUL.md/CLAUDE.md/DESIGN.md/Szenario-Katalog entsprechend angepasst.*

---

## Kleinere Hinweise (P1–P2)

- [ ] **Journal-Daten korrigieren:** Log-Einträge sagen 2026-07-08/07-09, alle Commits sind vom 07-07 (Abendsitzungen). Entweder Datumsangaben berichtigen oder als „Sitzung 2/3" führen — aktuell leidet die Nachvollziehbarkeit.
- [ ] **Frischer Clone ist nicht lauffähig:** WAVs sind gitignored; die App startet, aber jede Wiedergabe schlägt fehl. Entweder Bootstrap-Schritt („nach Klon: `cd tools && uv run generate_tts.py`") prominent in die README, oder die 18 Test-WAVs (~0,5 MB) einfach committen — bei der Größe billiger als die Piper-Pipeline vorauszusetzen.
- [x] **Playback-Nebenläufigkeit als M2-Anforderung notieren:** Schnelle Doppelklicks starten parallele Coroutinen → überlappende AudioTracks; `Thread.sleep` blockiert Default-Dispatcher-Threads. Für M2 (Session) braucht es eine Playback-Queue mit Cancel-Semantik — ins M2-Backlog-Item schreiben, nicht jetzt bauen. → *Erledigt 2026-07-08: im M2-Session-Item notiert.*
- [x] **`App.kt` wächst zum Dev-Screen:** Vor M2 die Diagnose-UI (Kanaltest) vom künftigen Lernmodus-Screen trennen, sonst wird der Smoke-Test zum Fundament. → *Erledigt 2026-07-08: Diagnose-UI nach `DevPlaybackScreen.kt` ausgelagert; `App.kt` ist jetzt nur noch schlanker Einstiegspunkt (Theme + Aufruf), der in M2 zum echten Navigation-Host wird. Build (`:core:jvmTest`, `:composeApp:assembleDebug`) grün.*
- [x] **Backlog-Überschneidung auflösen:** „Kanalsteuerung bis in die UI durchstechen" (M1) ist faktisch M2-Session-Parameter + M4-Settings. Zusammenführen, sonst wird es doppelt getrackt oder doppelt gebaut. → *Erledigt 2026-07-08: M1-Item entfällt; als ein M4-Settings-Item weitergeführt (per ADR-0007 ohnehin Option statt Kern).*
- [ ] **Zweite Stimme — Optionsliste ins bestehende [KLÄRUNG]-Item:** `de_DE-thorsten_emotional-medium` (8 Sprechvarianten, medium-Qualität, gleicher Sprecher) als Zwischenlösung für Varianz; alternativ andere Offline-TTS-Engines prüfen.
- [x] **DoD-Drift:** AGENTS.md verlangt `./gradlew build`, gelaufen sind immer nur Teiltasks. Entweder einmal `build` durchlaufen lassen (deckt Lint-/Release-Pfade auf) oder die DoD ehrlich auf die Teiltasks anpassen. → *Erledigt 2026-07-08: `./gradlew build` einmal komplett durchlaufen lassen (inkl. Release-Build, Lint, alle Tests) — BUILD SUCCESSFUL, 49s, keine Lint-Befunde. DoD-Formulierung war also nicht drift, nur ungeprüft; keine Anpassung nötig.*

---

## Positiv (keine Aktion nötig)

- Journal-Qualität überdurchschnittlich: Sackgassen (Kerstin-Workarounds, WSL2-Audio-Irrwege) ehrlich dokumentiert statt wegretuschiert.
- ADR-Disziplin funktioniert; Kette Entscheid → ADR → Backlog-Hinweis → Log ist durchgängig.
- Testwachstum 13 → 27 (inkl. `androidUnitTest`-Source-Set); echte Piper-Datei als Fixture ist ein guter Realitäts-Anker.
- Saubere Trennung „Code-Fehler vs. Umgebungsproblem" bei WSL2-Audio und BT-Lautstärke.
