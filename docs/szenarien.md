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

- **S5 · Sitzung verlassen** (M2/M3, Unterbrechung)
  Als Trainierender möchte ich eine Sitzung jederzeit verlassen können (Anruf, Alltag), sodass sie **sauber endet**: bereits abgegebene Bewertungen sind persistiert, die Sitzung ist als abgeschlossen protokolliert, es gibt keinen Pause-/Fortsetzen-Zustand — der nächste Start ist eine neue Sitzung. (Entscheid des Autors 2026-07-08.)

### Audio-Setup

- **S6 · Trainiertes Ohr erreichen** (M1→M2, Kernanliegen)
  Als Trainierender möchte ich, dass das Training im Referenz-Setup (BT-Hörgerät, linkes Ohr — ADR-0007) verständlich und mit richtigem Pegel ankommt, sodass ich mich auf die Erkennung konzentriere statt auf die Technik. Kanalwahl links/rechts/beide (`StereoGain`) bleibt als Option für Alternativ-Setups (Kabel-Kopfhörer), ist über BT aber wirkungslos (Mono-Summierung) — die UI darf sie dort nicht als wirksam darstellen (M4).

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

- **S12 · Sitzungshistorie einsehen** (M3)
  Als Trainierender möchte ich abgeschlossene Sitzungen als Liste mit Datum und Uhrzeit sehen (auch mehrere pro Tag), je Sitzung mit Modus und Kennzahlen (Wörter, Bewertungsverteilung), sodass ich meinen Trainingsverlauf nachvollziehen kann. (Entscheid des Autors 2026-07-08; fachliche Basis ist die `Session`-Entität mit `parameterSnapshot` und `results[]`.)

## Geklärte Szenario-Fragen

Alle ursprünglich offenen Fragen (S-OFFEN-1…3) hat der Autor am 2026-07-08 entschieden:

| Frage | Entscheid |
| --- | --- |
| Referenz-Trainings-Setup | BT-Hörgerät, linkes Ohr; Abspielgerät Smartphone, nicht ans A53 gebunden — **ADR-0007**. Kanaltrennung wird Option statt Kern (→ S6). |
| Fortschritt/Statistik | Sitzungsbasiert: Liste mit Datum/Uhrzeit, mehrere Sitzungen pro Tag möglich (→ S12). |
| Unterbrechungsverhalten | Sauber beenden, kein Pause/Resume (→ S5). |
