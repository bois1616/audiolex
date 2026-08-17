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

## Nachtrag 2026-08-17: Punkt 3 (Störgeräusche) ist umgesetzt — und drei belegte Befunde zum F-Droid-Weg

Der Autor hat am 2026-08-17 beauftragt, die lizenzgebundenen Störgeräusche zu entfernen und den Anmeldeweg zu F-Droid auszuarbeiten. Damit läuft ein Stück der Release-Arbeit an, die dieser ADR unter Punkt 4 auf „liegt, bis die Veröffentlichung konkret wird" gestellt hatte. Die Reihenfolge bleibt die des ADRs: erst der Zustand „bereit", die Veröffentlichung selbst ist nicht beauftragt.

**Umgesetzt (v0.32.0):** Die drei Loops sind aus dem Projekt entfernt, samt `noise.json`, Lizenztabelle und dem gebündelten Laden im Code (ADR-0010 Nachtrag). Beleg statt Behauptung: im gebauten APK sind **0** Dateien unter `files/noise/` und 71 Ressourcendateien (der Korpus). Nebenbefund derselben Prüfung, der es wert ist, festgehalten zu werden: Ein *inkrementeller* Build hätte die Loops weiter ausgeliefert — `copyDebugComposeResourcesToAndroidAssets` räumt entfernte Ressourcen nicht auf, die Dateien standen nach dem Löschen noch in `build/intermediates/assets/`. Nach dem Entfernen einer Compose-Ressource ist ein `clean` für dieses Modul Pflicht, sonst prüft man die Vergangenheit.

**Drei Befunde, gegen die aktuelle F-Droid-Doku belegt** (die Belegpflicht aus dem letzten Absatz dieses ADRs ist damit für diese Punkte erfüllt; Details, Quellen und Schrittfolge in `docs/fdroid-anmeldung.md`):

1. **Die Lizenz der Piper-Stimme ist unkritisch.** Das Modell `de_DE-thorsten-medium` liegt unter MIT, der zugrundeliegende Datensatz (Thorsten-Voice) unter CC0 — die offene Klärung aus den Konsequenzen dieses ADRs ist beantwortet. Die erzeugten Korpus-WAVs sind damit frei weitergebbar, und F-Droids Aufnahmepolitik lässt für nicht-funktionale Assets ohnehin den größeren Spielraum, verlangt aber ausdrücklich das Recht zur Weiterverbreitung.
2. **„TTS im Build" ist der riskante Teil des Entscheids, nicht der sichere.** F-Droid erlaubt vorgebaute Binärteile nur aus einer festen Liste von Quellen (Debian main, Maven Central/Google/Sonatype/JFrog/JitPack/Clojars, PyPI-Wheels, Rust/Go/Node); ein 63 MB großes Stimmodell von Hugging Face steht dort nicht. Die Erwartung „die Erzeugung läuft auf dem Buildserver" trägt in dieser Form nicht. Der praktikable Weg ist der, den dieser ADR unter Weg 3 als eingecheckte Variante beschreibt und verworfen hatte: die WAVs lokal erzeugen und einchecken — sie sind CC0/MIT-abgeleitet, also genau die unproblematischen Inhalte, um die es ging. **Das ist ein neuer Autor-Entscheid** und im Backlog als `[KLÄRUNG]` vermerkt, nicht hier vorweggenommen.
3. **Ein bisher unbekannter Blocker im Build.** `settings.gradle.kts` lädt das Plugin `org.gradle.toolchains.foojay-resolver-convention`, um sich eine JDK zu beschaffen. F-Droids Scanner führt genau dieses Plugin als „usual suspect" und bricht darauf ab — es lädt eine Java-Laufzeit aus einer nicht kontrollierten Quelle. Das gehört vor der Anmeldung aus dem Build, nicht in ein `scanignore`.

## Nachtrag 2026-08-17 (zweiter): Punkt 3 wird korrigiert — ausgelieferte Audios liegen im Repository

Der Autor hat die Deployment-Vorbereitung beauftragt und dabei zwei Entscheide getroffen, die Punkt 3 dieses ADRs ändern:

1. **„Wir arbeiten mit eigenen WAVs."** Die ausgelieferten Audiodateien werden **eingecheckt**, nicht zur Build-Zeit erzeugt. Damit ist die `[KLÄRUNG]` aus dem ersten Nachtrag entschieden, und zwar gegen die Build-Variante: Das Piper-Stimmodell (63 MB, Hugging Face) ist keine von F-Droid erlaubte Binärquelle, die erzeugten WAVs dagegen sind unproblematisch (Modell MIT, Datensatz CC0). Umgesetzt: 68 Korpus-WAVs im Index, 3,6 MB, `.gitignore`-Ausnahme gestrichen, Herkunft in `files/corpus/README.md` als Pflichtangabe. Das alte `[PROP]`-Git-LFS-Item ist damit gegenstandslos. Eigene Einsprachen des Autors und weiterer Stimmen (Grete) sollen als Beispiele dazukommen — Format und die drei Bedingungen (Einverständnis, Format, zwei JSON-Einträge) stehen in derselben README.
2. **Ein Störgeräusch wird mitgeliefert**, entgegen dem ersten Nachtrag: die Bus-Aufnahme des Autors. Nicht die Bündelung war das Problem, sondern die fremde Lizenz. Die Regel lautet jetzt „nur Inhalte, deren Weitergabe erlaubt ist", nicht „nichts" (ADR-0010 zweiter Nachtrag).

**Was mit diesem Nachtrag außerdem erledigt ist** (v0.33.0): LICENSE-Datei eingesetzt (Apache-2.0, Debians kanonischer Text, nur die Copyright-Zeile gefüllt), das Toolchain-Provisioning-Plugin aus `settings.gradle.kts` entfernt, Impressum-Satz „ohne öffentlichen Vertrieb" korrigiert, README für Fremde umgeschrieben samt Inhalte-Lizenztabelle, Store-Metadaten im Fastlane-Layout angelegt (de-DE + en-US, Icon 512×512 aus den Vektorpfaden gerastert), Rezeptur-Kopie unter `fdroid/`.

**Belegt am Artefakt, nicht behauptet:** Der Index wurde in ein leeres Verzeichnis exportiert (`git checkout-index --prefix`) und dort ohne `local.properties` und ohne die Autor-Bibliothek gebaut — das ist die Lage des Buildservers. `assembleRelease` läuft durch, das APK trägt 68 Korpus-WAVs und keinen der alten Loops. Zwei Funde dabei:

- **Der Scanner liest Kommentare mit.** Nach dem Entfernen des Plugins stand seine Kennung noch im erklärenden Kommentar derselben Datei. F-Droid sucht sie als Text in Gradle-Dateien — der Kommentar hätte denselben Abbruch ausgelöst wie das Plugin. Die Kennung steht jetzt nur noch in der Doku, die keine Gradle-Datei ist.
- **Der Toolchain-Wegfall trägt lokal.** `jvmToolchain(21)` löst gegen die installierte JDK 21 auf; `./gradlew build` bleibt grün, das Argument von damals („der Host hat nur eine JRE") ist überholt.

**Offen bleibt eine Lizenzfrage, die nur der Autor beantworten kann:** unter welcher Lizenz seine eigenen Aufnahmen (Bus-Geräusch, Einsprachen) weitergegeben werden. F-Droid verlangt für nicht-funktionale Assets ausdrücklich das Recht zur Weiterverbreitung, also braucht es eine Angabe. Vorschlag in der README: CC0-1.0. Bis zum Entscheid steht dort „noch festzulegen" — sichtbar, nicht stillschweigend.
