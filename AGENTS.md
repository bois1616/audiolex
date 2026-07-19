# AGENTS.md

Verbindlicher Arbeitsmodus für dieses Repo. Gilt für alle Agenten und Modelle.

## 1) Projekt in einem Satz

AudioLex ist eine Hörtrainings-App (Android zuerst, iOS-Option offen) zum Wiederaufbau der Zuordnung Klang → Wort → Bedeutung nach einseitigem Hörverlust. Vision und Fachkonzept: `docs/konzept/AudioLex-Konzept.md`.

## 2) Arbeitsweise

- **Backlog-getrieben**: Reihenfolge `P0` → `P1` → `P2` → `P3` aus `docs/backlog.md`. Nichts in Arbeit nehmen, was nicht im Backlog steht oder explizit beauftragt wurde.
- **Analyse vor Plan**: bestehenden Code und Doku lesen, bevor geplant oder implementiert wird.
- **Meilensteine statt Sprint-Dogma**: M0–M5 strukturieren die Arbeit; Reihenfolge darf begründet abweichen.
- **ADRs**: jede wesentliche technische Entscheidung als ADR in `docs/adr/` (Template dort). Vorschläge über den aktuellen Scope hinaus als Backlog-Item mit Tag `[PROP]`.
- **Klärungsbedarf**: Items mit Tag `[KLÄRUNG]` brauchen einen Entscheid des Autors — nicht eigenmächtig auflösen, sondern nachfragen.

## 3) Source of Truth

| Was | Wo |
| --- | --- |
| Konzept/Vision | `docs/konzept/AudioLex-Konzept.md` |
| Haltung & Tonalität | `SOUL.md` |
| UI/UX-Konzeption | `DESIGN.md` |
| Szenario-Katalog (SDD, Quelle der UI-ACs) | `docs/szenarien.md` |
| Architektur | `docs/architektur.md` |
| Entscheidungen | `docs/adr/` |
| Aufgaben & Prioritäten | `docs/backlog.md` |
| Umsetzungsjournal | `docs/umsetzungslog.md` |
| Claude-Kontext | `CLAUDE.md` |

## 4) Definition of Done

1. Build grün: `./gradlew build` (mindestens `:core:jvmTest` und `:composeApp:assembleDebug`).
2. Neue Logik in `:core` hat Unit-Tests.
3. Sichtbares/hörbares Verhalten am Desktop-Target verifiziert (`./gradlew :composeApp:run`); Audio- und Gerätethemen zusätzlich auf dem Testgerät (Galaxy A53, Android 16).
4. Backlog-Item abgehakt mit kurzem `Hinweis:` zum Ergebnis.
5. Eintrag in `docs/umsetzungslog.md` (neueste zuerst; Datum, fetter Titel, was + warum + wie verifiziert).
6. **Version hochgezählt (je Batch, seit 2026-07-13):** `VERSION_NAME` in `composeApp/.../AppVersion.kt` eine Minor-Stufe (z. B. `0.5.0` → `0.6.0`), `VERSION_CODE` +1. Patch (`0.5.1`) bleibt Hotfixes vorbehalten. Diese Datei ist die Single Source of Truth — der Gradle-`versionName`/`versionCode` und die StartScreen-Anzeige lesen daraus, nirgends doppelt pflegen. Die neue Version im Umsetzungslog-Eintrag und in der Commit-Message nennen, damit klar ist, welche Version ein Gerätetest prüft. **Bündeln (seit 2026-07-19):** Ein Batch darf mehrere zusammenhängende Items in **eine** Version/Commit bündeln — Richtschnur ist „eine Version je *kohärenter* Änderung", nicht zwingend eine je Item. Verwandtes (gleiches Subsystem, oder ein Schwung Gerätetest-Findings) läuft dann in einer Umsetzung; nur eigenständige Brocken (z. B. ein Audio-Feature vs. ein UI-Fix) bekommen eigene Versionen.
7. Commit mit prägnanter deutscher Message.

## 5) Konventionen

- **Sprache**: Doku und Commits Deutsch; Code, Bezeichner und Code-Kommentare Englisch; UI-Texte Deutsch.
- **Namensraum**: `de.hexenwoche.audiolex`
- **Kein Netzwerk-/Cloud-Code in Phase 1** — Datenhaltung strikt lokal.
- **Keine neuen Gradle-Module ohne ADR**: Komponenten wachsen zuerst als Pakete in `:core` (srs, audio, corpus, session), Modul-Split erst bei echtem Bedarf.
- **Audiodateien** nicht ins Repo committen, solange die Versionierungsstrategie offen ist (Backlog `[PROP]` Git-LFS); Metadaten (JSON/MD) sind ok.
- **Korpus generisch halten**: nichts Hörverlust-spezifisches ins Datenmodell verdrahten (spätere Vokabeltrainer-Nutzung, Konzept 3.4).

## 6) Modellzuteilung (Richtwert)

- **Opus-Klasse**: Architektur, mehrdeutige Designfragen, ADR-Entwürfe, Prozess-Reflexion, Backlog-Schärfung.
- **Sonnet-Klasse**: klar spezifizierte Implementierung, Tests, Refactoring.
- **Je offenem Backlog-Item als Tag vermerkt**: `[→Sonnet]`, `[→Opus]` oder `[→Opus→Sonnet]` (erst schärfen, dann umsetzen); Items ohne Tag brauchen den Autor. Entscheidend ist der Spezifikationsgrad, nicht die Item-Größe: ein Item ohne Akzeptanzkriterien und Nicht-Ziele ist nie Sonnet-reif — Sonnet-Sessions setzen nur `[→Sonnet]`-Items um und schärfen nicht selbst nach.
- **Effizienz bei mehreren `[→Sonnet]`-Items:** eine zusammenhängende Kette in **einer** Agent-Session abarbeiten (Kontext einmal lesen statt je Item kalt neu), pro Commit nur die DoD-Mindest-Tasks bauen (`:core:jvmTest` + `:composeApp:assembleDebug`), den vollen `./gradlew build` einmal am Ende. Kein voller Rebuild + kalter Agent-Neustart je Kleinst-Item. (Hintergrund-Agenten sind in dieser Umgebung unzuverlässig durchgelaufen — synchron delegieren.)

## 7) Umgebung

- Entwicklung unter WSL2 (Debian), JDK 21, Android SDK unter `~/Android/Sdk` (Pfad in `local.properties`, nicht versioniert).
- Schnelle Iteration über das **Desktop-Target**, nicht über den Emulator.
- Gerätetest: Samsung Galaxy A53 (SM-A536B/DS, Android 16) via adb über WLAN oder USB (usbipd).
- Deployment aufs Gerät über das `Makefile` im Repo-Root (`make pair` / `make deploy PORT=...`, Details `make help`); Standard-IP 192.168.178.42, bei DHCP-Drift `IP=...` überschreiben. ASHA-/BT-Audio-Diagnose am Gerät: `adb logcat | grep bluetooth-asha` — Signatur `SendStart ... volume=0x80` = systemseitig stummer Hörgerät-Volume-State (kein App-Bug), `volume=0x0` = gesund (Beweiskette: Umsetzungslog 2026-07-18 Abend).
