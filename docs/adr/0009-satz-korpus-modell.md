# ADR-0009: Sätze als Eintragsart des generischen Korpus-Eintrags (kein eigener Satz-Typ)

- **Status:** akzeptiert (Autor-Entscheide 2026-07-18, Modellfolge durch Opus-Schärfung am selben Tag)
- **Datum:** 2026-07-18

## Kontext

Der Autor will neben Einzelwörtern auch ganze Sätze trainieren (Backlog M2 „Ganze Sätze als Schwierigkeitsstufe", P1/„Jetzt machen"). Das Korpusmodell (`Word`/`AudioRecording`, `core/corpus`) war faktisch auf Einzelwörter zugeschnitten (`syllableCount`, `phoneticGroup` für Minimalpaare), die Klassendoku von `Word` aber bereits bewusst generisch gehalten (Konzept 3.4: spätere Vokabeltrainer-Nutzung). Offen war die Modellfrage: eigener Typ `Sentence` parallel zu `Word`, oder ein generischer Eintrag mit Unterscheidungsmerkmal? Autor-Entscheide vom 2026-07-18, die den Rahmen setzen: Sätze werden **wie Wörter behandelt** (gleiche subjektive Bewertung im Prüfmodus, gleiche SRS-Logik); der erste Satzkorpus kommt **per TTS** (Piper, ADR-0006), nicht als Eigenaufnahme; Inhalte müssen **nicht originalgetreu** sein — Kriterium ist „mitlesbar und erkennbar", kurze Sätze, **kein Gedächtnistraining**.

## Entscheidung

1. **Ein generischer Korpus-Eintrag mit Eintragsart.** `Word` bekommt ein Feld `kind: EntryKind { WORD, SENTENCE }` mit kotlinx-serialization-Default `WORD` — bestehende `words.json` ohne `kind` bleibt gültig, keine Migration. Es gibt **keinen** neuen Typ `Sentence`.
2. **SRS unverändert.** `ReviewCard.wordId` referenziert jeden Korpus-Eintrag (Wort oder Satz); Fälligkeit, Bewertung (fünf Stufen, ADR-0005) und Intervalle sind identisch. Seeding (`allOrSeed`) erfasst Sätze automatisch wie Wörter.
3. **TTS-Pipeline unverändert.** `tools/generate_tts.py` liest `words.json` und vertont `text` — Satz-Einträge laufen durch denselben Piper-Lauf, kein neues Tooling.
4. **Korpus-Modus als einfache Einstellung, nicht als Preset.** Die Wahl Wörter/Sätze wird eine persistierte App-Einstellung (`CorpusMode` in `AppSettings`/`SettingsEntity`), bewusst **kein** `SettingsProfile`-Konstrukt (das Presets-Item bleibt eigenständig und ist damit entkoppelt).
5. **Inhalte paraphrasiert, nicht zitiert.** Erster Satzkorpus: kurze Sätze (≤ 8 Wörter), motivisch angelehnt an „Per Anhalter durch die Galaxis" (Kap. 1), aber frei formuliert — Kriterium ist akustische Erkennbarkeit beim Mitlesen, nicht Werktreue. Herkunft wird im Korpus-README dokumentiert.

## Alternativen

- **Eigener Typ `Sentence` parallel zu `Word`:** verworfen — verdoppelt Lade-Pfade, SRS-Seeding und Screen-Logik für ein Verhalten, das laut Autor-Entscheid identisch sein soll; ein Unterscheidungsfeld trägt dieselbe Information bei einem Bruchteil der Änderungsfläche.
- **`Word` in `CorpusEntry` umbenennen:** vorerst verworfen — kosmetischer Groß-Diff ohne Verhaltensänderung; die Klassendoku sagt bereits „generic corpus entry" und wird um den Satz-Bezug ergänzt. Bleibt als optionale Aufräum-Notiz, kein Item.
- **Satz-Modus als Preset-Stufe (`SettingsProfile` Einfach/Fortgeschritten):** verworfen — ein binärer Inhalts-Schalter ist ehrlicher als ein Preset-Konstrukt, das noch nicht existiert; koppelt den Satz-Bogen nicht mehr an das Presets-Item.
- **Eigenaufnahmen (menschlich) als erster Satzkorpus:** vom Autor verschoben — TTS zuerst; das Einsprechen-Item bleibt für später offen, seine Aufnahme-Weg-Frage (App-Mikrofon vs. Import) ist damit vorerst nicht dringlich.

## Konsequenzen

- **Leichter:** keine neue Modell-Hierarchie; SRS, Seeding und TTS-Tooling unberührt; Sätze sind nach zwei kleinen Batches (Modell+Inhalt, dann Modus-Schalter+Anzeige) in beiden Modi nutzbar; alte `words.json` bleibt kompatibel; Copyright-Frage entschärft (keine wörtlichen Buchzitate im Repo).
- **Schwerer / bewusste Schulden:** `syllableCount` und `phoneticGroup` sind wort-spezifisch und für Sätze bedeutungsarm — Konvention: `syllableCount` = Gesamtsilben des Satzes (hält numerische Filter nutzbar), `phoneticGroup` = `null`. Der Typname `Word` wird semantisch ungenau (über die Doku abgefedert). Die Anzeige in Lern- und Prüfmodus muss mehrzeilig umbrechen können (bisher shrink-to-fit einzeilig) — eigener Umsetzungs-Batch; DESIGN.md-Prinzip „positionsstabil" wird dahin gelesen, dass Wörter unverändert einzeilig/groß bleiben und nur Sätze innerhalb derselben stabilen Fläche umbrechen.
