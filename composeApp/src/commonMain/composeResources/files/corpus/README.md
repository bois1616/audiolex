# corpus

Wortkorpus für AudioLex: Audiodateien und Metadaten, als Compose-Resource unter `composeApp` (nicht als eigenständiger Top-Level-Ordner), damit alle Targets — inklusive Android, das keinen repo-relativen Dateisystempfad hat — per `Res.readBytes(...)` einheitlich darauf zugreifen (siehe `docs/architektur.md`).

- **Metadaten** (`words.json`, `recordings.json`) und **Audiodateien** sind versioniert. Seit v0.33.0 gilt das auch für die WAVs: F-Droid baut aus dem Quelltext, ein Buildserver ohne sie liefert eine stumme App (Autor-Entscheid 2026-08-17, `docs/fdroid-anmeldung.md` Schritt 3).
- Format: WAV, PCM16, 22050 Hz, mono — native Piper-Ausgaberate (ADR-0003, ADR-0006).
- Herkunft und Weitergaberecht jeder Datei gehören hierher. Was nicht weitergegeben werden darf, gehört nicht in dieses Verzeichnis.
- Erzeugt über `tools/generate_tts.py` (`uv run generate_tts.py` aus `tools/`).

## Herkunft der Aufnahmen

| Sprecher (`voiceId`) | Einträge | Herkunft | Lizenz / Weitergabe |
| --- | --- | --- | --- |
| `thorsten` | 68 | Lokal erzeugt mit Piper, Stimmodell `de_DE-thorsten-medium` | Modell MIT, Datensatz [Thorsten-Voice](https://github.com/thorstenMueller/Thorsten-Voice) CC0 — CC0-1.0 |
| `Stephan (Beispiel)` | 3 | Eigene Einsprachen des Autors, in der App aufgenommen (2026-08-07), vom Gerät übernommen | CC0-1.0 (Autor-Entscheid 2026-08-17) |
| `Grete (Beispiel)` | 1 | Einsprache einer zweiten Sprecherin, in der App aufgenommen (2026-08-07) | CC0-1.0; **Einverständnis der Sprecherin liegt dem Autor vor** (bestätigt 2026-08-17) |

**Warum der Zusatz „(Beispiel)" im `voiceId` steht** (A53-Befund 2026-08-17): Die Kontingent-Liste zeigt den `voiceId` wörtlich an, und dieselben Aufnahmen liegen auf dem Gerät des Autors auch als *eigener* Korpus — dort mit `speaker: "Stephan"` bzw. `"Grete"`. Ohne Unterscheidung standen vier Sprecher in der Liste, die sich nur durch Groß-/Kleinschreibung unterschieden („Stephan" und „stephan"). Der Zusatz trennt die mitgelieferte Demo sichtbar von einer echten eigenen Stimme — auf jedem Gerät, nicht nur auf dem des Autors. Dateinamen und Aufnahme-Ids bleiben schlank (`…__stephan.wav`); nur das angezeigte Kontingent trägt den Zusatz.

Die vier Einsprachen sind ausdrücklich **Demo-Beispiele** (Autor-Entscheid 2026-08-17), keine kuratierte Sammlung: Ihre Texte („Test zwei", „Glenkill") stammen aus der Erprobung der Aufnahmefunktion. Sie bilden ein eigenes Kontingent je Stimme und lassen sich in den Einstellungen abschalten, ohne den synthetischen Bestand anzutasten — genau dafür gibt es die Kontingente. Wortlaut unverändert übernommen; angepasst wurden nur Groß-/Kleinschreibung und Satzzeichen an den Stil des übrigen Korpus.

**Offene Kleinigkeit:** Die Einsprachen sind als `locale: de-DE` eingetragen, weil sich der Akzent nicht am Schreibtisch feststellen lässt. Sind sie österreichisch gefärbt, gehört `de-AT` hinein und die Dateien in ein `raw/de-AT/`. Ein Satz des Autors genügt dafür.

Das Stimmodell selbst liegt **nicht** im Repo (`tools/voices/`, gitignoriert, 63 MB) und wird bei Bedarf per Skript geholt. Ausgeliefert werden nur die erzeugten WAVs.

## Herkunft der Satz-Einträge

Die `satz-*`-Einträge (`"kind": "SENTENCE"`) sind motivisch an Douglas Adams, „Per Anhalter durch die Galaxis" (Kapitel 1), angelehnt, aber **frei paraphrasiert** — keine wörtlichen Zitate stehen im Repo (ADR-0009). Kriterium ist akustische Erkennbarkeit beim Mitlesen, nicht Werktreue.

## Eigene Aufnahmen mitliefern

Eingesprochene Wörter und Sätze (Autor, Grete, weitere Stimmen) können als zusätzliches Kontingent mitgeliefert werden — die App behandelt `thorsten` und jede andere Stimme gleich, es gibt keinen Sonderstatus (ADR-0012 Nachtrag).

Nötig ist dreierlei:

1. **Einverständnis der aufgenommenen Person.** Eine Stimme ist ein personenbezogenes Datum; wer eine fremde Aufnahme veröffentlicht, braucht deren Zustimmung. Das gilt für den mitgelieferten Korpus genauso wie für die Deck-Weitergabe, über die der Backlog nachdenkt — nur ist es hier bindend, weil die Datei in einem öffentlichen Repository landet.
2. **Format.** WAV, PCM16, 22050 Hz, mono. Umwandeln:

   ```bash
   ffmpeg -y -i <quelle> -ac 1 -ar 22050 -c:a pcm_s16le \
     composeApp/src/commonMain/composeResources/files/corpus/raw/de-DE/<wortId>__<sprecher>.wav
   ```

3. **Zwei Einträge je Aufnahme.** In `words.json` das Wort (nur einmal je Wort, unabhängig von der Sprecherzahl), in `recordings.json` die Aufnahme:

   ```json
   { "id": "ball", "text": "Ball", "language": "de-DE", "syllableCount": 1, "category": "EVERYDAY" }
   ```

   ```json
   {
     "id": "ball__grete",
     "wordId": "ball",
     "voiceId": "grete",
     "locale": "de-DE",
     "fileRef": "raw/de-DE/ball__grete.wav"
   }
   ```

   `source` bleibt weg — der Standard `MITGELIEFERT` ist richtig für alles in diesem Verzeichnis. Für einen Dialekt gehört das passende Subtag in `locale` (z. B. `de-AT`), nicht in den Sprechernamen.

Danach die Tabelle oben um die Stimme ergänzen. Ein Eintrag ohne Herkunftszeile ist ein Release-Blocker, kein Schönheitsfehler.
