# AudioLex – Projektkonzept

**Arbeitstitel:** AudioLex
**Status:** Konzeptphase / Projektgerüst
**Datum:** 2026-07-07
**Autor:** Stephan
**Lizenz/Nutzung:** Zunächst nicht-kommerziell, keine Härtung, keine öffentliche Verteilung

---

## 1. Vision & Motivation

AudioLex ist eine Android-App zum Training des Sprachverständnisses nach einseitigem Hörverlust mit Hörgeräteversorgung. Das Kernproblem ist neurologisch, nicht akustisch: Schall kommt über das Hörgerät an, wird aber vom Gehirn (noch) nicht in Sprache decodiert. Das Ziel ist der Wiederaufbau der Zuordnung *Klang → Wort → Bedeutung* durch systematisches, wiederholtes und graduell schwieriger werdendes Hörtraining – methodisch angelehnt an Spaced-Repetition-Systeme (Anki-Prinzip), aber mit Audio statt Text als Trigger-Reiz.

Zwei komplementäre Trainingsmodi:

- **Modus 1 – Kopplung (Lernmodus):** Wort wird akustisch eingespielt und gleichzeitig als Text angezeigt. Baut die audio-visuelle Assoziation neu auf.
- **Modus 2 – Prüfung (Recall-Modus):** Wort wird akustisch eingespielt, Anzeige ist eine verdeckte Karte. Nutzer schätzt das Wort, deckt auf, bewertet die eigene Erkennungsleistung. Bewertung steuert die Wiederholfrequenz nach Spaced-Repetition-Logik.

---

## 2. Zielgruppe

Primär: der Autor selbst (Betroffener mit ca. 80% einseitigem Hörverlust, Hörgeräteträger). Sekundär denkbar (nicht Ziel der ersten Phase): andere Hörgeräteträger mit ähnlicher Reha-Notwendigkeit, ggf. auch als generisches Höralphabetisierungs-/Höralphabetisierungstool.

---

## 3. Kernfunktionen

### 3.1 Trainingsmodi

- Lernmodus (Audio + Text simultan)
- Prüfmodus (Audio, verdeckte Karte, Bewertung, Spaced Repetition)

### 3.2 Spaced-Repetition-Mechanik (Prüfmodus)

Bewertungsskala nach Erkennungsgrad, mit Intervallsteuerung analog SM-2/Anki:

| Bewertung | Intervall |
|---|---|
| Sofort | 1 Minute |
| Bald | 10 Minuten |
| Später | 1 Tag |
| Gut | 1 Woche |
| Perfekt | 1 Monat |

Offene Designfrage (siehe Abschnitt 8): feinere Bewertungsskala (z. B. „sofort erkannt“ / „nach X Wiederholungen erkannt“ / „nicht erkannt“) mit automatischer statt manueller Intervallwahl – näher am originalen SM-2-Verhalten. Für den MVP: manuelle Bewertung, spätere Iteration kann automatisieren.

### 3.3 Einstellbare Trainingsparameter

**Audio-Ausgabe**

- Kanalsteuerung: rechts / links / beide
- Lautstärke getrennt regelbar pro Kanal
- Störgeräusch-Overlay: Stärke einstellbar (dB-Verhältnis Signal/Rauschen)
- Störgeräusch-Szenarien: Sprachhintergrund/Kneipe (Stimmengewirr), Wetter (Wind/Regen), Verkehr — erweiterbar

**Wortsteuerung**

- Anzahl Wortwiederholungen vor erwarteter Identifikation (1×, 2×, 3×, …)
- Wortlänge nach Silbenzahl (1–2 / 3–4 / 5+ Silben)
- Wortkomplexität: deutsche Alltagswörter, Fremdwörter, andere Sprachen (Kategorien, erweiterbar)
- Phonetische Ähnlichkeit als eigene Dimension: gezielte Minimalpaare/Verwechslungswörter (z. B. Gnu/Kuh, Panther/Otter) als eigene Schwierigkeitsstufe

**Szenarien-Presets** (Kombination der obigen Parameter zu Voreinstellungen)

- Einfach: kurze, häufige Wörter, kein Störgeräusch, mehrfache Wiederholung erlaubt
- Schwierig: längere Wörter, moderates Störgeräusch, phonetisch aus einem breiteren Pool
- Fortgeschritten: gezielte phonetische Ähnlichkeitspaare, Störgeräusch-Szenarien aktiv, einmalige Wiedergabe vor Identifikation

### 3.4 Spätere Option (nicht MVP)

- Umnutzung der Spaced-Repetition-Engine für klassisches Vokabeltraining (Fremdsprachen) — Architektur sollte das von Anfang an nicht ausschließen (Wortkorpus-Modell generisch halten, nicht Hörverlust-spezifisch verdrahten).

---

## 4. Technische Architektur (Vorschlag für MVP)

### 4.1 Plattform

- Android, nativ. Sprache: Kotlin, UI: Jetpack Compose (moderner Standard, gut wartbar für Agenten-getriebene Entwicklung, gute Testbarkeit).
- Zielversion: aktuelle Android-API, Minimum SDK nach Bedarf (z. B. eigenes Testgerät als Referenz).

### 4.2 Kernkomponenten

```md
audiolex/
├── audio-engine/        # Wiedergabe, Kanalsteuerung, Lautstärke, Noise-Overlay
├── corpus/               # Wortkorpus-Verwaltung, Kategorisierung, Silben/Phonetik-Metadaten
├── srs-engine/            # Spaced-Repetition-Logik (SM-2-artig), Review-Scheduling
├── session/               # Trainingssitzung, Modus-Steuerung (Lernen/Prüfen)
├── settings/              # Parameter-Persistenz (Kanal, Lautstärke, Störgeräusch, Szenarien)
├── data/                  # lokale Persistenz (z. B. Room/SQLite), keine Cloud-Anbindung in Phase 1
└── ui/                    # Compose-Screens
```

### 4.3 Audio

- Wortkorpus: Priorität auf echten Sprachaufnahmen (mehrere Sprecher, natürliches Tempo) statt TTS – TTS als Fallback/Ergänzung für schnelle Erweiterung des Wortpools.
- Störgeräusch-Overlay: vorproduzierte Loop-Samples pro Szenario, gemischt mit einstellbarem Pegel zur Sprachspur.
- Kanaltrennung: Nutzung von `AudioTrack`/Stereo-Panning für gezielte Ohr-Ansteuerung (rechts/links/beide, getrennte Lautstärke).

### 4.4 Datenmodell (Entwurf)

- **Word**: id, text, audio_ref(s), silben_anzahl, sprache, kategorie (Alltag/Fremdwort/fremdsprachig), phonetische_gruppe (für Verwechslungspaare)
- **ReviewCard**: word_id, letzte_bewertung, naechste_faelligkeit, intervall_stufe, wiederholungs_zaehler
- **Session**: id, modus (lernen/pruefen), parameter_snapshot (Kanal, Lautstärke, Störgeräusch, Szenario), ergebnisse[]
- **SettingsProfile**: benannte Parameter-Presets (Einfach/Schwierig/Fortgeschritten + individuelle)

### 4.5 Datenschutz

Rein lokale Datenhaltung in Phase 1, keine Cloud-Synchronisation, kein Server-Backend nötig für MVP. DSGVO-Relevanz gering, da keine Übertragung personenbezogener Daten – dennoch von Anfang an sauber trennen (falls später Mehrbenutzer-/Cloud-Funktion dazukommt).

---

## 5. Entwicklungsmethodik – Agentengetrieben (100%)

Governance-Struktur analog zum bestehenden wegrose-Projekt, übertragen auf AudioLex:

- **AGENTS.md** – Regeln für agentische Codebasis-Analyse vor Planung („Pre-Tasking Considered Harmful“): Agent analysiert bestehenden Code/Struktur, bevor er plant oder implementiert.
- **CLAUDE.md** – projektspezifischer Kontext für Claude Code (Architekturentscheidungen, Konventionen, Domänenwissen zu Audio/SRS).
- **RSI.md** – Recursive-Self-Improvement-Prozess: Agent dokumentiert eigene Lernschritte, verbessert eigene Vorgehensweise iterativ.
- **EXP-Records** (YAML, schema-validiert) – strukturierte Erfahrungsaufzeichnung je Sprint/Task, für Nachvollziehbarkeit und späteres Lernen aus Entscheidungen.
- **PROP-System** – Vorschlagsmechanismus für Architekturänderungen, die über den aktuellen Sprint hinausgehen.
- **ADRs** (Architecture Decision Records) – jede wesentliche technische Entscheidung (z. B. Wahl Kotlin/Compose, Audio-Engine-Ansatz, SRS-Algorithmus) wird dokumentiert.
- **Sprint-Backlog-Governance** über GitHub Projects, Scrum-Rhythmus wie bei wegrose.

**Modellzuteilung** (wie gewohnt):

- Opus/Mythos-Tier: Architekturentscheidungen, RSI-Reflexion, mehrdeutige Designfragen (z. B. Wortkorpus-Strategie, SRS-Algorithmus-Feinschliff)
- Sonnet: klar spezifizierte Implementierung (UI-Screens, Audio-Engine-Bausteine, Datenmodell-Umsetzung)

**Dokumentformat:** Markdown (.md) als Standard für alle Projektdokumente, sofern nicht anders verlangt.

---

## 6. Repo- und Projektstruktur (Vorschlag)

```md
audiolex/
├── AGENTS.md
├── CLAUDE.md
├── RSI.md
├── docs/
│   ├── adr/                # Architecture Decision Records
│   ├── exp-records/         # Erfahrungsaufzeichnungen (YAML)
│   └── konzept/             # dieses Dokument + Weiterentwicklungen
├── app/                     # Android-App-Modul (Kotlin/Compose)
├── corpus-data/              # Wortkorpus, Audiodateien, Metadaten (ggf. separat via Git-LFS)
└── README.md
```

---

## 7. Roadmap (Sprint-Vorschlag für MVP)

**Sprint 0 – Setup**

- Repo-Grundgerüst, Governance-Dateien (AGENTS.md, CLAUDE.md, RSI.md)
- Android-Projekt-Skeleton (leeres Compose-Projekt, Build läuft)
- Erste ADR: Technologiewahl bestätigen

**Sprint 1 – Audio-Grundgerüst**

- Einfache Wiedergabe eines Testworts über AudioTrack
- Kanalsteuerung (links/rechts/beide) + getrennte Lautstärke
- Minimaler Wortkorpus (10–20 Testwörter, echte Aufnahmen oder TTS-Platzhalter)

**Sprint 2 – Lernmodus**

- UI: Wort abspielen + Text gleichzeitig anzeigen
- Session-Steuerung, Navigation durch Wortliste

**Sprint 3 – Prüfmodus + SRS-Kern**

- Verdeckte Karte, Aufdecken, Bewertung (5-stufig wie oben)
- SRS-Engine: Intervall-Scheduling, Persistenz der Fälligkeiten

**Sprint 4 – Störgeräusche & Szenarien**

- Störgeräusch-Overlay (mind. 1 Szenario), Pegel-Regler
- Szenario-Presets (Einfach/Schwierig/Fortgeschritten)

**Sprint 5 – Wortkomplexität & Phonetik**

- Silbenzahl-Filter, Kategorisierung (Alltag/Fremdwort/fremdsprachig)
- Phonetische Ähnlichkeitsgruppen (Minimalpaare) für „Fortgeschritten“

**Später (Backlog, nicht terminiert)**

- Automatische Bewertungsskala/Feinsteuerung der SRS-Intervalle
- Vokabeltraining-Modus (generische Wortkorpus-Nutzung für Fremdsprachen)
- Erweiterte Störgeräusch-Bibliothek

---

## 8. Offene Fragen zur Klärung vor/während Sprint 0

1. **Audioquelle für Wortkorpus:** Eigene Aufnahmen (z. B. Partner/Familie als Sprecher, verschiedene Stimmen) vs. TTS-Start für schnellen Prototyp?
2. **Störgeräusch-Samples:** Selbst produzieren/aufnehmen oder aus freien Quellen beziehen (Lizenzprüfung nötig, auch bei nicht-kommerzieller Nutzung sauber dokumentieren)?
3. **Bewertungsskala im Prüfmodus:** Bei der jetzigen 5-stufigen Skala (Sofort/Bald/Später/Gut/Perfekt) – soll die Wahl weiterhin manuell erfolgen, oder soll das System z. B. aus Reaktionszeit/Wiederholungszahl automatisch vorschlagen?
4. **Phonetische Gruppierung:** Manuell kuratierte Verwechslungspaare (höherer Pflegeaufwand, hohe Präzision) oder algorithmische Ähnlichkeitsschätzung (z. B. phonetische Distanzmaße)?
5. **Minimum SDK / Zielgerät:** Welches Android-Gerät/-Version ist die primäre Testumgebung?

---

## 9. Nächste Schritte

1. Repo anlegen, Governance-Grundgerüst (AGENTS.md, CLAUDE.md, RSI.md) erstellen.
2. Android-Projekt-Skeleton in VSCode/Codium mit Claude Code aufsetzen (Sprint 0).
3. Erste ADR zur Technologiewahl (Kotlin/Compose, lokale Persistenz, Audio-Engine-Ansatz) verfassen.
4. Offene Fragen aus Abschnitt 8 klären, bevor Sprint 1 beginnt.
