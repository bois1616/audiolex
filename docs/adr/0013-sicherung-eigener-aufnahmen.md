# ADR-0013: Sicherung eigener Aufnahmen als ZIP im Dokumentenverzeichnis — und Abschalten des automatischen System-Backups

- **Status:** akzeptiert (Autor-Entscheid 2026-08-06)
- **Datum:** 2026-08-06

## Kontext

ADR-0012 hat die Sicherung eigener Aufnahmen ausdrücklich offen gelassen: „Eine Sicherung ist **nicht** Teil dieses ADRs und bleibt eine offene Frage." Mit den Batches A–D (v0.21.0–v0.25.0) ist der Eigen-Korpus vom Rohtest zum Trainingsinhalt geworden. Damit wächst der Bestand, und er ist weiterhin **der einzige Datenbestand der App, der sich nicht wiederherstellen lässt**: SRS-Fälligkeiten säen sich neu, der mitgelieferte Korpus liegt im Repository, Einstellungen sind in einer Minute wieder gesetzt. Eine Aufnahme mit einer Person, die gerade nicht neben einem steht, ist weg.

**Der auslösende Befund (2026-08-06):** Bei der Prüfung, wie eine Sicherung ohne Netzzugriff überhaupt aussehen kann, fiel auf, dass im `AndroidManifest.xml` `android:allowBackup="true"` steht — ohne jede Backup-Regel. Damit ist Androids Auto-Backup aktiv: App-private Dateien, also `files/eigene-aufnahmen/` samt WAVs und Metadaten-JSON sowie die Room-Datenbank, werden bei aktiviertem Google-Backup bis 25 MB in das Google-Konto des Nutzers übertragen. Die App braucht dafür keine `INTERNET`-Berechtigung; die Übertragung erledigt ein Systemdienst.

**Folge:** Der Satz im Impressum — „Eine Internet-Berechtigung gibt es weiterhin nicht — Aufnahmen können das Gerät technisch nicht verlassen" — ist **derzeit nicht zutreffend**. Fairerweise gehört dazu: Ab Android 9 verschlüsselt die Google-Sicherung mit einem aus der Bildschirmsperre abgeleiteten Schlüssel, Google selbst kommt also nicht an die Inhalte. Aber die Daten verlassen das Gerät, und der Nutzer hat das nie entschieden. Das Projekt hat seine Datenschutzzusagen bisher **belegt statt behauptet** (der Mikrofon-Text wurde in Batch A im selben Batch nachgezogen wie die Berechtigung, gegen das Manifest geprüft) — dieser Anspruch verlangt, den Widerspruch aufzulösen, nicht ihn zu erklären.

## Entscheidung

**1. Die Sicherung ist ausdrücklich und vom Nutzer ausgelöst, nicht automatisch.** Eine **ZIP-Datei** mit dem gesamten Eigen-Korpus (Metadaten-JSON + alle WAVs) wird ins **Dokumentenverzeichnis** geschrieben. Was danach mit der Datei geschieht — auf einen Stick kopieren, per Kabel an den Rechner ziehen, manuell in eine Cloud laden — ist Sache des Nutzers und ausdrücklich **nicht** Aufgabe der App. Autor-Entscheid: „Backup als Zip ins Dokumentenverzeichnis. Weitere Speicherung kann z. B. per Cloud Upload durch den User manuell erfolgen."

**2. Das Dokumentenverzeichnis, nicht der App-Speicher.** Der Ablageort ist bewusst außerhalb des app-privaten Bereichs: Eine Sicherung, die mit der Deinstallation verschwindet, ist keine. Auf Android ab API 29 (minSdk ist 29) kann eine App ohne jede Berechtigung eigene Dateien dorthin schreiben; die Datei bleibt nach einer Deinstallation liegen und ist in der Dateien-App sichtbar.

**3. `allowBackup` wird abgeschaltet — aber erst zusammen mit dem Export, nicht davor.** Nach Entscheidung 1 hat der Nutzer einen selbstbestimmten Weg; das automatische, ungefragte Hochladen entfällt und die Zusage im Impressum stimmt wieder. **Die Reihenfolge ist Teil der Entscheidung:** Das System-Backup ist heute das einzige — wenn auch ungewollte — Sicherheitsnetz. Es abzuschalten, bevor der Export existiert, verschlechtert die Lage. Beides gehört in dieselbe Version.

**4. Keine Verschlüsselung der ZIP.** Autor-Einschätzung: „Die Daten sind harmlos genug, dass man sie nicht extra verschlüsseln müsste." Es handelt sich um selbst gesprochene Alltagswörter und -sätze zu Übungszwecken. Eine Passwortabfrage wäre eine Hürde bei jedem Export und ein Verlustrisiko obendrein (vergessenes Passwort = verlorene Sicherung), ohne einem realen Angriffsszenario zu begegnen. Wo die Datei landet, bleibt damit eine echte Entscheidung des Nutzers — was der Punkt der Sache ist.

**5. Wiederherstellung gehört dazu.** Eine Sicherung, die sich nicht zurückspielen lässt, schützt nichts — sie erzeugt nur das Gefühl, geschützt zu sein. Der Import liest eine ZIP und **führt zusammen**: Einträge mit unbekannter id kommen hinzu, bereits vorhandene ids werden übersprungen. Das ist konfliktfrei möglich, weil ids kollisionsfrei erzeugt werden (`own-<Zeitstempel>-<Zufall>`, ADR-0012); es braucht weder Konfliktdialog noch Überschreib-Semantik. Ein Import fügt nur hinzu und löscht nie — die zerstörungsfreie Richtung ist die richtige Vorgabe, wenn auf beiden Seiten unwiederbringliche Daten liegen.

**6. Nur der Eigen-Korpus wird gesichert.** Nicht die Datenbank. SRS-Fälligkeiten und Sitzungshistorie sind Verlaufsdaten, die sich neu bilden; Einstellungen sind in einer Minute wieder gesetzt. Aufnahme und Verschriftlichung sind es nicht. Das hält die Sicherung klein, verständlich und frei von Schema-Fragen — eine gesicherte Datenbank müsste bei jedem künftigen Schema-Sprung mitgepflegt werden, ein ZIP mit WAVs und einer JSON-Datei nicht.

## Alternativen

- **Storage Access Framework mit Auswahldialog** (`ACTION_CREATE_DOCUMENT`). Der Nutzer wählt bei jedem Export das Ziel — auch einen Cloud-Anbieter, ohne dass die App davon erführe. Sauberste Trennung, aber ein Dialog bei jeder Sicherung. Verworfen zugunsten des einfacheren festen Ablageorts; für den Import ist ein Auswahldialog dagegen unvermeidlich (die App kann nicht raten, welche ZIP gemeint ist).
- **`allowBackup="true"` belassen und stattdessen den Impressum-Text korrigieren.** Wäre ehrlich und billiger. Verworfen: Das Projekt hat „strikt lokal" nicht als Beschreibung, sondern als Eigenschaft verstanden, die durch das Fehlen der Netzberechtigung erzwungen wird. Eine stillschweigende Übertragung ins Google-Konto widerspricht dem, auch wenn sie verschlüsselt ist.
- **Automatische Sicherung durch die App selbst** (etwa bei jedem Start ins Dokumentenverzeichnis). Verworfen: erzeugt unbemerkt wachsende Dateien und nimmt dem Nutzer die Entscheidung ab, die dieses ADR ihm ausdrücklich zurückgeben will.
- **Verschlüsselte ZIP.** Siehe Entscheidung 4 — verworfen auf Autor-Einschätzung des Inhalts.
- **Die Datenbank mitsichern.** Siehe Entscheidung 6 — verworfen zugunsten von Schlankheit und Wartungsfreiheit.

## Konsequenzen

- **Die Datenschutz-Zusage wird wieder wahr, und zwar erzwungen statt behauptet.** Mit `allowBackup="false"` und ohne `INTERNET`-Berechtigung kann weder die App noch das System die Aufnahmen von sich aus übertragen. Der Impressum-Text muss mit derselben Version ausgeliefert werden wie die Änderung — dieselbe Pflicht wie bei der Mikrofon-Berechtigung in ADR-0012.
- **Die Verantwortung wandert zum Nutzer, sichtbar.** Wer nie exportiert, hat nach dieser Änderung *weniger* Schutz als vorher (das automatische Backup fällt weg). Das ist gewollt und muss in der App erkennbar sein — eine Sicherung, von der niemand weiß, ist keine.
- **Die ZIP liegt unverschlüsselt im Dokumentenverzeichnis** und ist damit für andere Apps mit Dateizugriff und für jeden am angeschlossenen Rechner lesbar. Bewusst hingenommen (Entscheidung 4).
- **Der Import kann nichts kaputtmachen, aber auch nichts reparieren.** Er fügt hinzu und überschreibt nie. Eine versehentlich gelöschte *und* seither neu angelegte id lässt sich damit nicht zurückholen — dieser Fall existiert praktisch nicht, weil ids nie wiederverwendet werden.
- **Ein Gerätewechsel ist ab hier durchführbar**, ohne dass Aufnahmen verloren gehen: exportieren, Datei mitnehmen, auf dem neuen Gerät importieren. Das war vorher nicht möglich.

## Nachtrag 2026-08-07: Entscheidung 6 war zur Hälfte falsch — die Sitzungshistorie gehört in die Sicherung

**Anlass:** Der Gerätetest zu AC8 (exportieren, deinstallieren, neu installieren, importieren) hat die Sicherung bewiesen — und dabei die Sitzungshistorie des Autors gelöscht. Wiederhergestellt wurde nur der Eigen-Korpus, wie Entscheidung 6 es vorsieht. Der Autor bemerkte den Verlust unmittelbar danach („Die Sitzungshistorie ist zurückgesetzt. Wird diese mitgesichert beim Backup?"). Die konkreten Sitzungen waren verzichtbar (Autor 2026-08-07: „Bisherige Verläufe sind irrelevant"), die Begründung dahinter ist es nicht.

**Der Fehler:** Entscheidung 6 begründet den Ausschluss der Datenbank mit „SRS-Fälligkeiten und Sitzungshistorie sind Verlaufsdaten, die sich neu bilden". Das fasst zwei ungleiche Dinge unter ein Wort:

- **SRS-Fälligkeiten bilden sich tatsächlich neu.** Ein paar Runden Bewertung, und der Zustand ist wieder da. Ihr Verlust kostet Terminierung, keine Information. Für sie bleibt Entscheidung 6 richtig.
- **Die Sitzungshistorie bildet sich nicht neu.** Sie ist die Aufzeichnung dessen, was tatsächlich geschehen ist — wann geübt wurde, wie bewertet wurde, wie sich das über Wochen verändert. Bei einem Training, dessen Zweck eine langsame neurologische Veränderung über Monate ist, ist genau das die Größe, an der sich Fortschritt überhaupt ablesen lässt. Sie ist so unwiederbringlich wie eine Aufnahme, nur unauffälliger: Ihr Fehlen tut nicht sofort weh, sondern erst nach einem Jahr.

Das Wort „Verlaufsdaten" hat den Fehler getragen. Es klingt nach „vorübergehend", meint hier aber „Aufzeichnung eines Verlaufs" — das Gegenteil.

**Korrigierte Entscheidung 6:** Gesichert wird, was sich nicht neu bildet: **der Eigen-Korpus und die Sitzungshistorie**. Nicht gesichert werden SRS-Karten und Einstellungen — beide stellen sich durch Benutzung bzw. in einer Minute wieder her, und beide hingen an der eigentlichen Begründung von Entscheidung 6, die insoweit gültig bleibt: die Sicherung klein und frei von Schema-Fragen zu halten.

**Nicht die Datenbank sichern, sondern die Historie.** Der Weg bleibt derselbe wie beim Eigen-Korpus: eine JSON-Datei im Archiv (`sitzungen/verlauf.json`), keine `.db`-Datei. Eine mitgesicherte Datenbank müsste bei jedem Schema-Sprung mitgepflegt werden und wäre nur mit passender Room-Version lesbar; eine Liste von Sitzungsaufzeichnungen ist beides nicht. Der Archiv-Aufbau trägt das ohne Änderung — er wurde am selben Tag bewusst mit einem Ordner je Inhaltsart geschnitten.

**Zusammengeführt wird über den Startzeitpunkt, nicht über die Datenbank-id.** `SessionEntity.id` ist `autoGenerate` und damit gerätelokal: Auf zwei Geräten trägt dieselbe Zahl verschiedene Sitzungen. Als Identität dient `startedAtEpochMillis` — zwei Sitzungen desselben Nutzers beginnen nicht in derselben Millisekunde. Damit gilt dieselbe Regel wie für Aufnahmen: unbekannter Startzeitpunkt kommt hinzu, bekannter wird übersprungen, nichts wird überschrieben oder gelöscht.

**Was das über den Prozess sagt:** Der Fehler stand seit dem 2026-08-06 im ADR und ist niemandem aufgefallen, weil die Formulierung plausibel klang. Bemerkt wurde er erst, als der Test die Daten wirklich löschte. Zur Lehre gehört auch der zweite Teil: Vor dem zerstörenden Schritt wurde der Eigen-Korpus vom Gerät gezogen, die Datenbank nicht — obwohl es derselbe Handgriff gewesen wäre. Wer einen Test fährt, der Daten löscht, zieht vorher **alles**, nicht das, was er für wichtig hält.
