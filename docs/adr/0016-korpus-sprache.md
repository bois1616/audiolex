# ADR-0016: Korpus-Sprache als Einordnung durch den Sprecher, getrennt von der UI-Sprache

- **Status:** akzeptiert (Autor-Entscheid 2026-08-24)
- **Datum:** 2026-08-24

## Kontext

ADR-0015 hat die Oberfläche zweisprachig gemacht und dabei ausdrücklich offengelassen, was trainiert wird: „Der Wortschatz bleibt deutsch." Genau das hat der Autor am selben Tag beauftragt zu ändern — englische Beispielsätze, und ein Korpus, der nach Sprache qualifiziert ist.

Das Modell trägt die halbe Antwort schon: `Word.language` und `AudioRecording.locale` sind seit M1 BCP-47-Felder und stehen überall auf `"de-DE"`. Für den mitgelieferten Korpus ist Sprachqualifizierung also reine Datenarbeit. Die Lücke war `OwnEntry` — eingesprochene Einträge kannten nur `speaker`, keine Sprache. Sprecher und Sprache sind aber zwei Achsen: Andy kann heute Deutsch und morgen Englisch einsprechen.

Zwei ältere Festlegungen mussten dabei fallen. Der Backlog führte „Sprach-Bogen Batch A/B" seit dem 2026-08-07 als zurückgestellt. Und der Autor hatte am 2026-07-29 `en_US-lessac-medium` als englische Stimme gewählt — das war vor ADR-0014 und vor der Aufnahme bei F-Droid, also bevor „ausgeliefert wird nur, was weitergegeben werden darf" galt.

## Entscheidung

**1. Die Sprache ist eine Einordnung des Erzeugers, keine Aussage über den Inhalt.** Wer einen Eintrag anlegt, wählt die Sprache; das entscheidet ausschließlich, **wo der Eintrag erscheint und verwendet wird**. Spricht Andy seine Einträge als Deutsch ein und mischt später einen englischen Satz dazwischen, bleibt der unter Deutsch. In den Worten des Autors: „Kann auch chinesisch oder Buschmann sein, könnten wir eh nicht verhindern."

Das ist keine Bequemlichkeit, sondern die einzige ehrliche Bauart. Die App sieht den Text nur als Zeichenkette, die jemand getippt hat, und das Audio ist ihr vollständig undurchsichtig. Eine Erkennung wäre manchmal falsch — und im Fehlerfall nicht erklärbar.

**2. `CorpusLanguage` ist die kleine, geschlossene Menge an Schubladen**, die die UI anbietet (`DEUTSCH`, `ENGLISCH`), abgeglichen gegen die BCP-47-Tags über den **primären Subtag**: `de`, `de-AT`, `de_DE` fallen in dieselbe Schublade. Ein Tag ohne Schublade — jemandes `zh-CN` — passt in keine und wird nicht angezeigt. Das ist das ehrliche Ergebnis; ein Auffangbecken „Sonstige" würde eine Ordnung vortäuschen, die es nicht gibt.

Gespeichert wird in `OwnEntry` der **Tag**, nicht der Enum-Name: Das ist Nutzerdatei, und ein Eintrag aus einer späteren Version mit mehr Sprachen muss weiter parsen, statt die ganze Datei zu sprengen (Muster `noiseScenario`).

**3. Der Sprachfilter greift vor allen anderen.** In `mergeCorpus` fällt eine fremde Schublade samt ihrer Aufnahmen weg, bevor `kind` und `excludedSpeakers` überhaupt laufen. Der Grund ist nicht Ordnung, sondern Datenschutz vor sich selbst: Der Korpus wird über `allOrSeed` zu SRS-Karten. Ein englischer Eintrag, der durchrutscht, säße als Karte im echten Stapel des Autors, bis er ihn von Hand löscht.

**4. Getrennt von der UI-Sprache**, wie ADR-0015 es angelegt hat. Deutsch trainieren mit englischer Oberfläche ist eine zulässige Kombination. Eine Kopplung würde jemandem das Trainingsmaterial umstellen, der nur die Beschriftungen lesen wollte. Zwei Einstellungen, zwei Orte: UI-Sprache auf dem Startbildschirm, Trainingssprache in den Einstellungen — mit einer Zeile darunter, die den Unterschied benennt.

**5. Englische Stimme: `en_US-ljspeech-high`.** Die Lizenz hat entschieden, nicht der Klang. LJ Speech ist gemeinfrei (LibriVox-Aufnahmen, Buchtexte von 1884–1964), und die MODEL_CARD sagt „Trained from scratch" — das Modell erbt nichts. Die Stufe „high" folgt dem kerstin-Befund aus M1: Die Kernübung dieser App ist das isolierte Einzelwort, und dort stauchen niedrige Stufen. Gemessen an der ersten Ausgabe: „bread" 0,53 s, „cow" 0,50 s, gegen thorstens „Ball" 0,52 s.

**6. Störgeräusche bleiben sprachfrei** (Autor: „Für Background Noise ist die Sprache egal"). `NoiseScenario` hat kein Sprachfeld und bekommt keins.

## Alternativen

**Sprache aus dem Inhalt erkennen.** Verworfen, siehe Punkt 1 — und der Autor hat die Frage ohnehin anders gestellt: Er wollte ein Auswahlfeld, keine Automatik.

**`en_US-lessac-medium` (Entscheid 2026-07-29).** Trainiert auf Blizzard 2013, dessen Lizenz Nutzung auf „Research Purposes" beschränkt und die Entwicklung von Sprachsynthese-Produkten ausdrücklich ausschließt. Mit ADR-0014 unvereinbar. **`ryan`** und **`hfc_female`/`hfc_male`**: CC BY-**NC**-SA — dieselbe Nicht-kommerziell-Klausel, wegen der die drei zugekauften Störgeräusch-Loops im August entfernt wurden; `hfc` ist zusätzlich aus `lessac` feinabgestimmt.

**`en_US-libritts_r-medium`** (CC BY 4.0) bleibt der Rückfall: frei, aber ein 904-Sprecher-Modell — man müsste eine Sprecher-Id wählen, die Qualität schwankt über Vorleser hinweg, und es gibt es nur als „medium".

**Sprache an die UI-Sprache koppeln.** Wäre eine Einstellung weniger. Verworfen: Der Hauptnutzer trainiert Deutsch; ihm beim Umschalten der Beschriftungen den Wortschatz zu wechseln, wäre ein Nebeneffekt, den niemand bestellt hat.

**Sprecher-Ausschluss je Sprache** (`Map<Sprache, Set<Sprecher>>` statt einer flachen Menge). Verworfen als Aufwand ohne Anlass — siehe Konsequenzen.

## Konsequenzen

**Leichter:** Eine dritte Sprache ist eine Zeile in `CorpusLanguage`, eine Zeile in `tools/generate_tts.py` und Daten. Der Generator rendert nur, was fehlt, und ordnet jede Stimme ihrer Sprache zu; der Ordner `raw/<locale>/` folgt daraus.

**Schwerer:** Zwei Sprach-Einstellungen sind erklärungsbedürftig. Die Hinweiszeile unter der Trainingssprache ist der Preis dafür.

**Bewusst in Kauf genommen:** `excludedSpeakers` bleibt eine flache Menge über alle Sprachen. Wer „Andy" in der deutschen Ansicht abwählt, hat ihn auch in der englischen abgewählt. Das kann überraschen; ein Ausschluss je Sprache wäre aber neue Persistenzform und neue Migration für einen Fall, den es noch nicht gibt. Wenn er auftritt, ist das der Anlass, es zu ändern.

**Ebenfalls bewusst:** Ein falsch einsortierter Eintrag bleibt falsch einsortiert, bis jemand ihn neu anlegt — eine nachträgliche Sprachänderung an bestehenden Einträgen gibt es in dieser Version nicht. Bestehende Einträge stehen per kotlinx-Default auf `de-DE`, was für alle vier heute existierenden sachlich richtig ist.

**Nicht getan und nicht behauptet:** Die zwanzig englischen Einträge sind eine Kostprobe, keine kuratierte Sammlung — zehn Wörter, zehn Sätze, frei formuliert. Und die Hörprobe hat bisher nur die Dauer gemessen; ob die Stimme für das Training taugt, entscheidet das Ohr des Autors.
