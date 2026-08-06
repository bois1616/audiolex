# ADR-0012: Eigen-Korpus — selbst eingesprochene Einträge als zweite, schreibbare Quelle

- **Status:** akzeptiert (Autor-Entscheide 2026-08-06, Architektur durch Opus-Schärfung am selben Tag) · **Entscheidung 1/2 revidiert am 2026-08-06 vor der Umsetzung, siehe Nachtrag**
- **Datum:** 2026-08-06

> **Nachtrag 2026-08-06: Die Metadaten liegen als JSON neben den Aufnahmen, nicht in der Datenbank.**
>
> Ursprünglich waren die Metadaten (Text, Eintragsart, Dateiname) für eine Room-Tabelle vorgesehen — konsistent zum übrigen Persistenz-Aufbau. Beim Schärfen von Batch B fiel auf, dass das mit einer bestehenden Festlegung kollidiert: `createAudioLexDatabase` setzt `fallbackToDestructiveMigration(true)`, **jede** Schema-Änderung löscht also alle Tabellen. Bisher war das folgenlos, weil nur SRS-Fälligkeiten und Einstellungen betroffen waren — beides regenerierbar oder trivial neu zu setzen. Eigene Aufnahmen sind das nicht: Sie sind der einzige Datenbestand der App, der **nicht wiederherstellbar** ist. Ein Versionssprung hätte die WAV-Dateien als Waisen zurückgelassen — vorhanden, aber ohne Text und Eintragsart wertlos. Ausgelöst wurde die Prüfung durch den Autor-Bericht, dass bereits eine zweite Person eingesprochen hat: Der Bestand ist nicht mehr hypothetisch.
>
> **Revidierte Entscheidung (Autor 2026-08-06):** Text, Eintragsart, Sprecher-Kontingent und Zeitstempel stehen in **einer JSON-Datei im selben Verzeichnis wie die WAVs** — dasselbe Muster wie beim mitgelieferten Korpus (`words.json` + `recordings.json` + WAVs). Room bleibt für SRS, Sitzungen und Einstellungen zuständig und behält dort seinen destruktiven Fallback; der Eigen-Korpus ist von Datenbank-Sprüngen schlicht nicht betroffen, weil er nicht in der Datenbank liegt.
>
> Was das zusätzlich löst: **Sicherung** ist das Kopieren eines Ordners (die unter „Konsequenzen" offen gelassene Frage), und der Bestand ist **selbstbeschreibend** — bei einer falschen Verschriftlichung genügt im Notfall ein Texteditor. Für ein System, dessen ausdrückliche Schwachstelle die Verschriftlichung ist (Punkt 5), ist das kein Nebeneffekt, sondern ein Sicherheitsnetz.
>
> Preis: keine Transaktionen und kein Abfrage-Optimierer. Bei einem Einzelnutzer-Bestand in der Größenordnung von zehn bis wenigen hundert Einträgen, der ohnehin komplett in den Speicher geladen wird, ist beides gegenstandslos. Die Datei wird bei jeder Änderung vollständig neu geschrieben.
>
> **Nachtrag 2026-08-06: Sprecher-Kontingente.** Der Autor hat eine zweite Person (weiblich) einsprechen lassen — mit gutem Ergebnis — und daraus zwei Folgerungen gezogen: Die seit Juli offene Frage nach einer **zweiten Stimme** (M1, an der TTS gescheitert) ist damit ohne TTS gelöst, und der **Dialekt** aus M5 lässt sich am einfachsten ebenso einsprechen. Vorgesehen sind daher benannte Kontingente (z. B. männlich, weiblich, Dialekt), nach denen sich später filtern lässt. Umgesetzt als **freies Textfeld** je Eintrag, nicht als festes Enum: Welche Kontingente entstehen, weiß heute niemand, und ein Enum müsste für jedes neue geändert werden. Mehrsprachigkeit ließe sich über denselben Weg abbilden — der Autor stellt das ausdrücklich zurück, das bestehende `Word.language`-Feld bleibt dafür der vorgesehene Ort (Sprach-Bogen).

## Kontext

Der Autor will eigene Wörter und Sätze selbst einsprechen und ihnen den Text manuell zuordnen — „jeder Benutzer kann seine eigenen Sätze/Wörter einsprechen und qualifizieren" (Requirement 2026-08-06). Der Nutzen liegt weniger in der Menge als in der **Stimmenvielfalt**: Der Korpus besteht heute aus 58 Einträgen einer einzigen synthetischen Stimme (thorsten-medium), und der Versuch, eine zweite TTS-Stimme zu ergänzen, ist im Juli an der Qualität gescheitert (kerstin-low, Backlog M1). Menschliche Aufnahmen lösen das ohne Umweg.

Das Datenmodell trägt das seit M1: `AudioRecording` ist von `Word` getrennt und erlaubt mehrere Sprecher je Eintrag (`voiceId`); ADR-0009 hat Sätze zu regulären Korpus-Einträgen gemacht. **Neu ist nicht das Modell, sondern der Weg der Daten.** Drei Dinge fehlen vollständig:

1. **Mikrofon-Eingang.** Es gibt nur die Ausgabe (`AudioSink` als expect/actual, Android `AudioTrack`, Desktop `paplay`). Ein Gegenstück existiert nicht.
2. **Ein schreibbarer Korpus.** `loadCorpus` liest ausschließlich aus den Compose-Resources — die werden zur Build-Zeit gepackt und sind zur Laufzeit unveränderlich. Eigene Aufnahmen können dort prinzipiell nicht landen.
3. **Eine Berechtigung.** Die App fordert heute **keine einzige** an, und die ausgelieferte Datenschutz-Seite (v0.16.0) sagt das ausdrücklich zu.

Autor-Entscheide, die den Rahmen setzen (2026-08-06): **Direkt-Aufnahme in der App** (nicht Import von außen) und die eigenen Aufnahmen bilden einen **eigenen Bereich neben dem mitgelieferten Korpus** — eigene Einträge mit eigenem Text, nicht zusätzliche Stimmen zu bestehenden Korpuswörtern.

Dazu eine ausdrückliche Feststellung des Autors, die diese Entscheidung mitprägt: „Das ist dann ein Garbage-in-Garbage-out-System. D. h., wenn ich die eingesprochenen Wörter oder Sätze nicht korrekt verschriftliche, bekomme ich auch falsche Ergebnisse."

## Entscheidung

**1. Der Eigen-Korpus ist eine zweite Quelle, keine Erweiterung der ersten.** Metadaten (Text, Eintragsart, Zeitstempel, Dateiname) kommen in eine neue Room-Tabelle, die Audiodaten als WAV-Dateien in ein app-privates Verzeichnis. Der mitgelieferte Korpus in den Compose-Resources bleibt unangetastet und read-only. `LoadedCorpus` bekommt beide Quellen; die Trainings-Screens wählen wie beim Wörter/Sätze-Schalter aus, womit sie arbeiten.

**2. Audio gehört ins Dateisystem, nicht in die Datenbank.** Room hält nur den Dateinamen. PCM-Daten als BLOB würden die Datenbank um Größenordnungen aufblähen und jeden Lesevorgang belasten, ohne einen Vorteil zu bieten. Den plattformabhängigen Pfad liefert der Entry-Point, wie schon bei der Datenbank (ADR-0004, `DatabaseBuilder.android.kt`/`.desktop.kt`) — `:core` bleibt kontextfrei.

**3. Der Mikrofon-Eingang spiegelt den `AudioSink`.** Ein `AudioSource` als expect/actual in `:core` (Android `AudioRecord`, Desktop `javax.sound` `TargetDataLine`). Aufgenommen wird in **PCM16, mono, 22050 Hz** — dasselbe Format wie der TTS-Korpus. Das ist keine Stilfrage: `mixWithNoise` verlangt gleiche Sample-Rate und Kanalzahl, und ADR-0010 schließt Resampling im Code aus. Eine Aufnahme in einem anderen Format wäre mit Störgeräusch nicht kombinierbar. Der vorhandene WAV-Writer wandert dafür von `jvmMain` nach `commonMain`.

**4. `RECORD_AUDIO` wird die einzige Berechtigung — und der Datenschutz-Text wird im selben Zug korrigiert.** Die heutige Zusage „fordert keine einzige Android-Berechtigung an" wird unwahr und ist anzupassen. Was **wahr bleibt** und deutlicher gesagt werden sollte: Es gibt weiterhin keine `INTERNET`-Berechtigung, Aufnahmen können das Gerät technisch nicht verlassen.

**5. „Garbage in, garbage out" wird als Systemgrenze benannt, nicht wegkonstruiert.** Die Zuordnung Ton → Text ist und bleibt manuell und liegt in der Verantwortung des Nutzers; eine automatische Gegenprüfung ist nicht vorgesehen (siehe Alternativen). Daraus folgen drei Anforderungen, die den Schaden begrenzen, ohne die Verantwortung zu verschieben:

- **Anhören vor dem Speichern.** Nach der Aufnahme sind Ton und eingegebener Text gleichzeitig zugänglich, bevor der Eintrag entsteht. Der wahrscheinlichste Fehler — vertippt, verhört, falsches Wort eingesprochen — fällt genau dort auf.
- **Nachträglich korrigierbar.** Der Text muss ohne Neuaufnahme änderbar sein, die Aufnahme ohne Textverlust wiederholbar, der Eintrag löschbar. Ein Tippfehler darf keinen Eintrag wertlos machen.
- **Fehler bleiben eingegrenzt.** Weil der Eigen-Korpus eine getrennte Quelle ist (Entscheidung 1), verunreinigt ein falscher Eintrag den kuratierten TTS-Korpus nicht.

**6. Eigene Einträge nehmen an beiden Trainingsmodi teil**, mit derselben SRS-Mechanik wie mitgelieferte (Karten via `allOrSeed`). Der Prüfmodus ist der Ort, an dem eine falsche Verschriftlichung am teuersten ist — dort wird die falsche Zuordnung eingeübt **und** in Fälligkeiten festgeschrieben. Das ist der Preis dafür, dass eigene Aufnahmen vollwertig sind; die Korrigierbarkeit aus Punkt 5 ist die Gegenmaßnahme.

## Alternativen

- **Import fertiger Audiodateien statt Aufnahme in der App.** Kein Mikrofon-Code, keine Berechtigung — dafür ein Medienbruch: extern aufnehmen, übertragen, zuordnen. Vom Autor am 2026-08-06 zugunsten der Direktaufnahme verworfen.
- **Eigene Aufnahmen als weitere `voiceId` zu bestehenden Korpuswörtern.** Das Modell trüge es, und es brächte Stimmenvielfalt zum selben Wort. Vom Autor verworfen: Er will eigene Inhalte, nicht Varianten vorgegebener. Nebeneffekt der Absage: Die Frage „welche von mehreren Aufnahmen wird gespielt?" stellt sich gar nicht erst.
- **Eigene Einträge in `words.json` schreiben.** Technisch unmöglich für die gepackte Ressource und konzeptionell falsch — es vermischte Kuratiertes mit Selbsterzeugtem in einer Datei, die unter Versionskontrolle steht.
- **Automatische Gegenprüfung per Spracherkennung.** Würde „garbage in" tatsächlich abfangen. Verworfen: Cloud-Erkennung verbietet sich (keine Netzberechtigung, ADR-Linie „strikt lokal"), und eine Offline-Erkennung wäre eine schwergewichtige neue Abhängigkeit für ein Nebenproblem. Bleibt als `[PROP]` denkbar, falls sich falsche Verschriftlichungen im Alltag als echtes Ärgernis erweisen.
- **Audio als BLOB in Room.** Verworfen, siehe Entscheidung 2.

## Konsequenzen

- **Die App fordert erstmals eine Berechtigung an.** Das ist eine sichtbare Zäsur für ein Projekt, dessen Datenschutz-Zusage bisher auf „gar keine" beruhte. Der Impressum-Text muss mit derselben Version ausgeliefert werden wie das Feature, sonst steht eine unwahre Zusage in der App.
- **Der Korpus wird zweiquellig.** `CorpusLoader` und die Screens müssen künftig zwei Herkünfte zusammenführen. Der Sprach-Bogen Batch B ergänzt an derselben Stelle einen Sprachfilter — beide Vorhaben fassen `loadCorpus` an, sie sollten nicht gleichzeitig laufen.
- **Nutzerdaten entstehen, die nicht im Repo liegen.** Anders als die gitignorierten Korpus-WAVs sind eigene Aufnahmen unwiederbringlich, wenn das Gerät verloren geht oder die App deinstalliert wird. Eine Sicherung ist **nicht** Teil dieses ADRs und bleibt eine offene Frage.
- **Die Qualität des Trainings hängt ab hier auch von der Sorgfalt beim Einsprechen.** Das ist gewollt, aber es verschiebt eine Fehlerquelle von der Software zum Nutzer — und Fehler dieser Art fallen erst im Prüfmodus auf, wo sie am meisten schaden.
- **Das Format ist festgelegt, nicht verhandelbar.** 22050 Hz mono PCM16 ist die Bedingung dafür, dass eigene Aufnahmen mit dem Störgeräusch-Overlay kombinierbar bleiben. Liefert `AudioRecord` auf einem Gerät diese Rate nicht, ist das ein echtes Problem und kein Detail. **Am Gerät geklärt (A53-Hörprobe 2026-08-06, v0.21.0):** Die scharfe Formatprüfung schlug nicht an, die Aufnahme ist verständlich und unverzerrt — der A53 liefert das geforderte Format. Der Vorbehalt ist damit für das Referenzgerät ausgeräumt; die Prüfung bleibt als Schutz für andere Geräte im Code.
