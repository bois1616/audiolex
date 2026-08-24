# ADR-0015: UI-Sprache Deutsch und Englisch, umschaltbar auf dem Startbildschirm

- **Status:** akzeptiert (Autor-Auftrag 2026-08-24)
- **Datum:** 2026-08-24

## Kontext

Bis v0.33.6 war die App einsprachig deutsch, und das war ausdrücklich so festgelegt: AGENTS.md §5 („UI-Texte Deutsch"), DESIGN.md unter „Visuelle Prinzipien", und im Backlog dreimal als Nicht-Ziel notiert — beim Sprach-Bogen Batch A und B sowie bei der Impressum-Seite. Der Grund war gut: Die App ist für einen einzelnen Nutzer gebaut, und eine zweite Sprache, die niemand liest, ist Ballast, der bei jeder Textänderung mitgepflegt werden will.

Seit dem 17. August liegt die App bei F-Droid. Damit stimmt die Prämisse nicht mehr. Der Store-Eintrag existiert bereits zweisprachig (`fastlane/metadata/android/{de-DE,en-US}/`), die App dahinter aber nicht — wer die englische Beschreibung liest und installiert, landet in einer deutschen Oberfläche.

Der Autor hat am 2026-08-24 zwei Dinge beauftragt: Lokalisierung nach Englisch mit Sprachauswahl auf der Eingangsmaske, und eine kurze Anleitung, ebenfalls von der Hauptseite aus erreichbar und der eingestellten Sprache folgend.

Die technische Randbedingung, die die Lösung bestimmt hat: Compose Multiplatform 1.8.2 löst die Sprache von Compose Resources aus der Plattform-Locale auf und bietet keine unterstützte Möglichkeit, sie zur Laufzeit zu überschreiben. Eine Sprachwahl *in* der App hätte also gegen das Framework gearbeitet.

## Entscheidung

**1. Der Textbestand ist ein getyptes Kotlin-Interface, keine Ressourcendatei.** `core/i18n/Strings.kt` deklariert jeden Text als `val` oder `fun`; `GermanStrings` und `EnglishStrings` implementieren es. Eine fehlende Übersetzung ist damit ein Compilerfehler, kein leeres Label auf dem Gerät. Das ist der Punkt, an dem sich diese Lösung von `strings.xml` unterscheidet — und bei zwei Fassungen in der Hand einer Person ist genau diese Garantie mehr wert als das Werkzeug drumherum.

**2. Der Katalog liegt in `:core`, nicht in `:composeApp`.** Das ist die unsaubere Stelle dieser Entscheidung: UI-Wortlaut in einem Modul, das als „plattformfreie Logik" beschrieben ist. Ausschlaggebend war die Testbarkeit — `:composeApp` hat kein Test-Sourceset, `:core:jvmTest` läuft in jeder DoD-Schleife. Der Katalog hängt von nichts aus Compose ab; er besteht aus Zeichenketten und `when`-Abbildungen über Domänen-Enums, und diese Abbildungen sind der Teil, der einen Test verdient.

**3. Die Sprache ist eine persistierte Einstellung mit `SYSTEM` als Vorgabe.** `UiLanguage { SYSTEM, DEUTSCH, ENGLISCH }` folgt dem Muster von `ThemeMode`. `SYSTEM` löst über den primären Sprach-Subtag des Geräts auf: `de`, `de-AT`, `de_DE` ergeben Deutsch, alles andere Englisch. Damit ist das Update auf einem deutschen Gerät unsichtbar — die Spalte kommt mit `'SYSTEM'` dazu, das Gerät sagt `de`, nichts ändert sich —, und ein englisches Gerät bekommt Englisch, ohne dass jemand etwas einstellt. DB-Schema v8 → v9, getragen von `MIGRATION_8_9` (`ALTER TABLE ... ADD COLUMN`), nicht vom destruktiven Fallback.

**4. Die Auswahl steht auf dem Startbildschirm, nicht in den Einstellungen.** Zwei Textschalter, „Deutsch" und „English", jeder in seiner eigenen Sprache geschrieben und der aktive in der Akzentfarbe. Der Grund ist zwingender als Bequemlichkeit: Wer die aktuelle Sprache nicht lesen kann, darf nicht raten müssen, welcher von fünf deutschen Knöpfen die Einstellungen öffnet. Sie sitzt trotzdem unten in der ruhigen Zone, nicht über den Trainingsknöpfen — sie wird einmal pro Installation angefasst, und der Startbildschirm hat die Aufgabe, in zwei Antippen zum ersten Wort zu führen (DESIGN.md Leitprinzip 6).

`SYSTEM` ist bewusst **keine** anwählbare Option. Es ist der Anfangszustand, keine Wahl, die es anzubieten lohnt; ein dritter Knopf würde auf einem Trainings-Startbildschirm zum Nachdenken über Locale-Vererbung einladen. Welche Sprache `SYSTEM` gerade ergibt, zeigt die Akzentfarbe ohnehin. Ein Antippen schreibt immer eine konkrete Sprache: Wer gesagt hat, was er will, soll nicht zusätzlich vom Gerät überstimmt werden.

**5. Die Kurzanleitung ist ein Screen, kein Verweis auf die README.** Sie erklärt die beiden Modi, die fünf Bewertungsstufen und was die Einstellungen tun — und sie folgt der Sprachwahl. Die README ist deutsch und für jemanden geschrieben, der die App *baut*, nicht für jemanden, der sie benutzt.

**6. Der Wortschatz bleibt deutsch.** Übersetzt ist die Bedienung, nicht das Trainingsmaterial. Die Sprache des Korpus ist eine eigene, zurückgestellte Frage (Backlog „Sprach-Bogen", `Word.language`); `UiLanguage` und `Word.language` dürfen nicht zusammenwachsen, sonst kann man später nicht mehr auf Englisch üben, was man auf Deutsch bedient.

## Alternativen

**Compose Resources (`composeResources/values/strings.xml` + `values-en/`).** Das naheliegende Werkzeug und in einer reinen Android-App die richtige Antwort. Zwei Gründe dagegen: In 1.8.2 gibt es keinen unterstützten Weg, die Locale zur Laufzeit zu überschreiben — die geforderte Sprachwahl in der App wäre auf `Locale.setDefault`-Tricks hinausgelaufen, die je Plattform anders brechen. Und eine fehlende Übersetzung fällt erst auf dem Gerät auf. Bei 150 Texten in zwei Fassungen ohne Übersetzerteam ist der Compiler die bessere Kontrolle.

**Nur Englisch, Deutsch abschaffen.** Halbiert den Pflegeaufwand. Verworfen: Der Hauptnutzer ist der Autor, das Trainingsmaterial ist deutsch, und die Texte im Impressum sind auf deutsches Recht hin formuliert.

**Der Systemsprache folgen, ohne Auswahl.** Wäre die kleinste Lösung und hätte ohne die Auswahl auch keinen Persistenz- und Migrationsaufwand gehabt. Der Autor hat die Auswahl ausdrücklich verlangt; sie ist außerdem das Einzige, was auf einem Gerät hilft, dessen Systemsprache man nicht ändern will.

**Alles übersetzen, auch den Dev-Kanaltest.** Verworfen: Der Kanaltest ist ein Instrument, erreichbar nur über einen langen Druck auf die Versionszeile, den niemand zufällig findet. Sein Wortlaut bleibt deutsch; nur der Weg zurück nutzt geteilte UI und folgt der Sprache.

## Konsequenzen

**Leichter:** Zwei Fassungen können nicht auseinanderlaufen, ohne dass der Build es merkt. Die Umschaltung wirkt sofort und ohne Neustart, weil ein `CompositionLocal` ausgetauscht wird und nicht die Plattform-Locale. Eine dritte Sprache wäre eine Datei und ein Enum-Eintrag — der Compiler zählt die fehlenden Texte auf.

**Schwerer:** Jeder neue UI-Text kostet jetzt drei Zeilen an drei Stellen statt einer im Screen. Das ist der Preis der Compilerprüfung und beabsichtigt, aber es ist Reibung.

**Bewusste Schuld:** Der Wortlaut der Oberfläche liegt in `:core`. Sollte `:composeApp` je ein Test-Sourceset bekommen, gehört der Katalog dorthin verschoben; bis dahin ist das der Preis für Tests in der schnellen Schleife.

**Zwei Regeln sind überholt.** AGENTS.md §5 und DESIGN.md sagten „UI-Texte Deutsch". Sie sind auf „Deutsch und Englisch, beide Fassungen gleichrangig gepflegt" geändert. Die drei Nicht-Ziel-Vermerke im Backlog gelten für die *Korpus*-Sprache weiter, für die UI-Sprache nicht mehr; sie sind entsprechend annotiert.

**Nicht erledigt und nicht behauptet:** Die Übersetzung stammt von der KI, die sie geschrieben hat, und ist von keinem englischen Muttersprachler gegengelesen. Fachlich heikel sind die Rechtstexte im Impressum — die englische Fassung ist eine Verständnishilfe, verbindlich bleibt die deutsche.
