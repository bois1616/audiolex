# Szenario-Katalog AudioLex (SDD)

Stand 2026-07-08. UI-Stories werden **entlang von Szenarien geschnitten** (Scenario-Driven Development), nicht entlang von Entitäten: „Wort im Prüfmodus erkennen und bewerten" ist ein Szenario, „ReviewCard-CRUD" ist keins. Jedes Szenario wird bei der Umsetzung zur Quelle der Akzeptanzkriterien seiner Story. Haltung dahinter: `SOUL.md` · Gestalt: `DESIGN.md` · Technik: `docs/architektur.md`.

Format:

```text
Als Trainierender möchte ich [in Zustand Z] [Absicht erreichen],
sodass [beobachtbares Ergebnis].
```

Ein Szenario ist verlaufsorientiert (ein Weg durch die App), zustandsbehaftet (an einen fachlichen/technischen Zustand gebunden) und beobachtbar (endet sicht- oder hörbar, nicht in einem internen Zustand). Es gibt nur eine Rolle: den Trainierenden.

## Pflichtprüfung je UI-Story

Vor jeder Trainings-UI-Story prüfen, welche dieser Verlaufsklassen zutreffen — zutreffende werden zu Akzeptanzkriterien, nicht zutreffende explizit begründet:

| Verlaufsklasse | Leitfrage |
| --- | --- |
| **Happy Path** | Der erfolgreiche Hauptverlauf der Absicht. |
| **Leer-Zustand** | Nichts fällig / Korpus leer — was sieht der Nutzer, welche Aktion bleibt? |
| **Audio-Ausgabe gestört** | Sink schlägt fehl, Hörgerät getrennt, Pegel „stumm" — Meldung statt stiller Fehlschlag. |
| **Fehlerzustand** | Datei fehlt/korrupt, Persistenz-Fehler — Meldung statt Absturz. |
| **Unterbrechung** | App verlassen oder Anruf mitten in der Sitzung — definierter Wiedereinstieg. |

**Strukturell nicht zutreffend** (entfällt ohne Einzelbegründung, solange Phase 1 gilt): *Offline/Degraded* (App ist vollständig lokal, kein Backend), *fehlende Berechtigung* (Single-User, keine Rollen), *Nebenläufigkeit* (ein Nutzer, ein Gerät). Wiedervorlage, falls Cloud-Sync oder Mehrbenutzer je kommen (Konzept 4.5).

## Katalog

### Training

- **S1 · Lernsitzung durchlaufen** (M2, Happy Path)
  Als Trainierender möchte ich im Lernmodus eine Sitzung starten und Wort für Wort gleichzeitig hören und lesen, sodass sich die Assoziation Klang → Schriftbild aufbaut und ich am Ende sehe, wie viele Wörter ich durchlaufen habe.

- **S2 · Wort wiederholt anhören** (M2)
  Als Trainierender möchte ich ein noch nicht verstandenes Wort beliebig oft erneut abspielen, bevor ich weitergehe, sodass ich das Tempo selbst bestimme. (Konfigurierbare Wiederholungszahl vor Identifikation: Konzept 3.3.)

- **S3 · Wort im Prüfmodus erkennen und bewerten** (M3, Happy Path)
  Als Trainierender möchte ich bei fälligen Karten ein Wort nur hören, meine Vermutung gegen die aufgedeckte Karte prüfen und fünfstufig bewerten, sodass die nächste Fälligkeit sichtbar gesetzt wird und das nächste Wort folgt.

- **S4 · Nichts fällig** (M3, Leer-Zustand)
  Als Trainierender möchte ich bei leerer Review-Queue sehen, wann die nächste Karte fällig ist und was ich stattdessen tun kann (Lernmodus), sodass die App nie in einer Sackgasse endet.

- **S5 · Sitzung unterbrechen** (M2/M3, Unterbrechung) — *Verhalten noch zu entscheiden, siehe S-OFFEN-3*
  Als Trainierender möchte ich eine Sitzung jederzeit verlassen können (Anruf, Alltag), sodass beim Wiedereinstieg ein definierter, nachvollziehbarer Zustand herrscht — bereits abgegebene Bewertungen gehen nicht verloren.

### Audio-Setup

- **S6 · Trainiertes Ohr ansteuern** (M1→M2, Kernfeature)
  Als Trainierender mit einseitigem Hörverlust möchte ich vor der Sitzung Kanal (links/rechts/beide) und Pegel je Ohr einstellen und die aktive Wahl während des Trainings sehen, sodass das Signal nachweislich auf dem trainierten Ohr ankommt. (Abhängig von der Klärung des Referenz-Setups, S-OFFEN-1.)

- **S7 · Keine hörbare Ausgabe** (M1→M2, Audio gestört)
  Als Trainierender möchte ich bei fehlgeschlagener oder stummer Wiedergabe (Sink-Fehler, Hörgerät getrennt, Geräte-Lautstärke) eine verständliche Meldung mit nächstem Schritt bekommen, sodass ich nicht rätsle, ob App, Gerät oder Ohr die Ursache ist. (Reale Erfahrung aus M1: „kein Ton" war eine getrennte BT-Lautstärkeeinstellung.)

- **S8 · Schwierigkeit über Störgeräusch steigern** (M4)
  Als Trainierender möchte ich ein Störszenario (Kneipe/Wetter/Verkehr) mit einstellbarem SNR zuschalten, sodass das Training realen Hörsituationen näherkommt und die Einstellung im Sitzungsprotokoll nachvollziehbar ist.

- **S9 · Preset wählen** (M4)
  Als Trainierender möchte ich mit einem Antippen ein Preset (Einfach/Schwierig/Fortgeschritten) aktivieren, statt Einzelparameter zu justieren, sodass die Sitzung sofort mit stimmigen Parametern startet.

### Rahmen

- **S10 · Erststart** (M2, Leer-/Initialzustand)
  Als Trainierender möchte ich beim ersten Start ohne Einrichtung direkt zu einem funktionierenden Trainingszustand kommen (Korpus geladen, sinnvolle Defaults für Kanal/Pegel), sodass die erste Sitzung ohne Konfigurationshürde möglich ist.

- **S11 · Wortauswahl eingrenzen** (M5)
  Als Trainierender möchte ich den Wortpool nach Silbenzahl, Kategorie und phonetischer Ähnlichkeit (Minimalpaare) filtern, sodass ich gezielt an meiner aktuellen Schwierigkeitsgrenze trainiere.

## Offene Szenarien (nicht spezifizierbar ohne Entscheid)

| Nr. | Lücke | Blockiert durch |
| --- | --- | --- |
| S-OFFEN-1 | **Referenz-Trainings-Setup**: Über welche Hardware (einzelnes BT-Hörgerät? Kopfhörer?) wird Kanaltrennung real trainiert? Bestimmt S6 und die Defaults in S10. | P0-Item Opus-Review, `docs/reviews/2026-07-07-m1-audio-review.md` [KLÄRUNG] |
| S-OFFEN-2 | **Fortschritt einsehen**: Ob und wie der Nutzer Trainingsverlauf/Statistik sieht, ist im Konzept nicht ausgeführt. | Entscheid des Autors (Umfang Statistik) |
| S-OFFEN-3 | **Unterbrechungsverhalten** (S5): Sitzung pausieren und fortsetzen vs. sauber beenden und neu starten? | Entscheid des Autors vor M2-Umsetzung |
