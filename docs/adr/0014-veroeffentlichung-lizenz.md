# ADR-0014: Veröffentlichungsrichtung, Lizenz und Asset-Politik

- **Status:** akzeptiert (Autor-Entscheide 2026-08-07)
- **Datum:** 2026-08-07

## Kontext

Am 2026-08-07 hat der Autor entschieden, AudioLex zu veröffentlichen — F-Droid vor Google Play (M6/M7 im Backlog). F-Droid baut selbst aus öffentlichem Quelltext und stellt damit drei Bedingungen, die als `[KLÄRUNG]`-Items im Backlog standen: für wen veröffentlicht wird, unter welcher Lizenz, und was mit den ausgelieferten Audiodateien geschieht. Zwei Befunde machten die Fragen akut: Im Repo ist keine Lizenzdatei („alle Rechte vorbehalten" schließt eine Aufnahme aus), und ein F-Droid-Build aus dem heutigen Repo wäre stumm, weil die Korpus-WAVs gitignoriert sind; zwei der drei Störgeräusch-Loops tragen zudem eine „nicht-kommerziell"-Klausel, die F-Droid nicht als freie Lizenz anerkennt.

Der Autor hat am selben Tag alle drei Fragen beantwortet. Wichtig für die Einordnung: **Die Veröffentlichung selbst steht nicht an.** Ziel ist ausdrücklich der Zustand, in dem Projekt und App veröffentlichungsbereit sind — nicht die Veröffentlichung. Release-Arbeit wird nicht priorisiert; laufende Arbeit soll lediglich keine neuen Release-Blocker aufbauen.

## Entscheidung

1. **Veröffentlichung für Dritte, nicht-kommerziell.** AudioLex wird über das Autorengerät hinaus Nutzern bereitgestellt, ohne finanzielle Interessen. Damit werden ein Erstnutzungspfad und selbsterklärende Texte grundsätzlich fällig — aber erst, wenn die Veröffentlichung konkret wird (siehe Punkt 4).

2. **Apache-2.0 für den Code.** Die Lizenz ist F-Droid-kompatibel und verbaut — anders als GPL — das offene iOS-Target nicht. Inhalte (Korpus, Audios) bleiben ausdrücklich außerhalb der Lizenz und tragen ihre eigene Herkunftsangabe; das folgt der Empfehlung des älteren Lizenz-`[PROP]`-Items, die hiermit übernommen wird. Das Einsetzen der LICENSE-Datei ist Teil der späteren Release-Arbeit, nicht dieses Entscheids.

3. **Kein nicht-freier Inhalt in einer Veröffentlichung.** Für die Audiodateien konkret:

   - **Störgeräusche:** Die drei gebündelten Loops (zweimal salamisound „nicht-kommerziell", einmal Pixabay) werden nicht mit veröffentlicht. Nutzer bringen eigene Geräusche selbst ein — das M4-Item heißt entsprechend „Eigene Störgeräusche aufnehmen, importieren und löschen": Direktaufnahme in der App **plus** Import vorhandener WAV-Dateien (Autor-Entscheid zweite Runde, 2026-08-07).
   - **Mitgelieferter Korpus:** Bleibt als Startbestand enthalten; seine WAVs werden jedoch **zur Build-Zeit per TTS erzeugt** (die lokale, reproduzierbare Piper-Pipeline, ADR-0006, `tools/generate_tts.py`) statt eingecheckt. Damit ist der vierte Weg aus dem Asset-Item („nur Texte veröffentlichen, in der App bearbeiten") **nicht** gewählt — er bleibt als eigenes, weiterhin blockiertes Backlog-Item stehen (keine Vertonung zur Laufzeit geklärt).
   - **Keine Nutzer-Korpora/Decks** werden mitgeliefert. Die Weitergabe-Idee aus dem Deck-`[PROP]` bleibt unberührt und ungebündelt.

4. **„Bereit, nicht veröffentlicht".** Release-Arbeit (LICENSE-Datei, öffentliches Repository, Tag-Konvention, Build-Integration der TTS-Erzeugung, F-Droid-Records) liegt, bis die Veröffentlichung konkret wird. Kriterium für alle Arbeit bis dahin: keine neuen Veröffentlichungs-Hindernisse aufbauen.

## Alternativen

Die Asset-Frage war im Backlog mit drei Wegen vorbereitet; der Autor hat eine Build-Variante von Weg 3 gewählt:

- **Weg 1: Audios ins Repo.** Verworfen — macht die „nicht-kommerziell"-Lizenz der salamisound-Loops sofort akut; diese Klausel ist mit F-Droid unvereinbar.
- **Weg 2: Ohne mitgelieferten Korpus ausliefern, nur Eigen-Korpus.** Verworfen als Auslieferungszustand — die App startete leer; der Autor will einen Startbestand behalten. (Die Eigen-Korpus-Fähigkeit ist davon unberührt und bleibt zentral.)
- **Weg 3: Frei lizenzierten Ersatzkorpus einchecken.** In der gewählten Build-Variante aufgegangen: statt Artefakte einzuchecken, wird die TTS-Erzeugung in den Build verlagert. Die Lizenzfrage wandert dabei von den WAVs zu den Piper-Stimmmodellen (siehe Konsequenzen).
- **Vierter Weg: Korpus-Texte in der App bearbeiten und vertonen.** Bleibt eigenständiges Backlog-Item. Blockiert, solange keine Vertonung zur Laufzeit geklärt ist — kein Teil dieses Entscheids.

## Konsequenzen

- **Der Auslieferungszustand der Störgeräusche ist „keine gebündelten Loops".** Das betrifft die Schärfung des M4-Störgeräusch-Items direkt: `noise.json` referenziert heute drei WAVs, die in einem Release-Build fehlen — der Katalog und die Störgeräusch-Sektion müssen mit ausschließlich selbst aufgenommenen Geräuschen (oder ganz ohne) funktionieren. Das ist der veröffentlichte Normalfall, kein Randzustand.
- **TTS im Build verlangt zwei Klärungen zur Release-Zeit:** die Lizenz der verwendeten Piper-Stimmmodelle (heute thorsten-medium) und die Integration der Erzeugung in den Build-/F-Droid-Pfad (F-Droid baut aus dem Quelltext — die Erzeugung muss dort laufen oder der Weg dorthin dokumentiert sein).
- **Zielgruppe Dritte** macht Erstnutzungspfad, Trainings-Erklärung und fehlermeldende Texte eventually fällig (Produkt-Item in M6) — gebunden an den Zeitpunkt „Veröffentlichung wird konkret", nicht an jetzt.
- **Nicht-kommerziell** hält M7 (Google Play) klein: Entwicklerkonto, Testpflichten und Gebühren bleiben gegen den F-Droid-Weg abgewogen (dortiges `[KLÄRUNG]`-Item).
- **Das Repo bleibt privat**, bis die Veröffentlichung konkret wird; der Zustand „alle Rechte vorbehalten" ist bis dahin gewollt und kein Versäumnis.
- **Belegpflicht bleibt:** Die F-Droid-Anforderungen selbst sind weiterhin nicht gegen die aktuelle F-Droid-Doku belegt (das M6-FOSS-Item macht das beim Schärfen) — dieser ADR hält die Autor-Entscheide fest, nicht externe Fakten.
