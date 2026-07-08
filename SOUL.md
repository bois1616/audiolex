# SOUL — Seele des Projekts AudioLex

Dieses Dokument hält fest, wofür AudioLex steht und wie die App mit ihrem Nutzer spricht — unabhängig vom konkreten Feature. Es ergänzt `AGENTS.md` (Arbeitsmodus) und `docs/konzept/AudioLex-Konzept.md` (Fachkonzept); im Konfliktfall gilt das Konzept.

## Was ist das hier?

Eine Hörtrainings-App, die die Zuordnung *Klang → Wort → Bedeutung* wieder aufbaut. Das Problem ist neurologisch, nicht akustisch: Schall kommt über das Hörgerät an, wird aber vom Gehirn (noch) nicht als Sprache decodiert. AudioLex trainiert diese Decodierung — systematisch, wiederholt, graduell schwieriger.

## Wer der Nutzer ist

Stephan selbst: ca. 80 % einseitiger Hörverlust, Hörgeräteträger. Er ist Autor, Auftraggeber und in Phase 1 der einzige Nutzer. Daraus folgt eine klare Rollenverteilung: **Der Nutzer ist die Autorität über sein eigenes Gehör.** Die App liefert Struktur, Wiederholung und Schwierigkeitssteuerung — sie beurteilt nicht, was er „eigentlich hören müsste".

## Haltung — nicht verhandelbar

**Trainingsgerät, kein Medizinprodukt.** Keine Heilversprechen, keine Diagnosen, kein Therapiesprech. Die App plant Wiederholungen von Erkennungsübungen — mehr behauptet sie nicht.

**Selbstbewertung ohne Urteil.** Im Prüfmodus bewertet der Nutzer sich selbst. Die Skala (Sofort/Bald/Später/Gut/Perfekt) beschreibt, *wann das Wort wiederkommt* — nicht Erfolg oder Versagen. Es gibt kein „falsch", kein Rot, keine Fehlerquote als Anklage.

**Geduld ist eingebaut.** Neurologischer Wiederaufbau dauert Monate. Deshalb: kein Streak-Druck, keine Gamification-Tricks, keine Schuldgefühle nach Pausen. Spaced Repetition *ist* die Geduld — die App muss sie nicht inszenieren.

**Privat heißt privat.** Hörleistung ist ein Gesundheitsdatum. Alles bleibt lokal auf dem Gerät: kein Konto, keine Cloud, keine Telemetrie (AGENTS.md §5, Konzept 4.5).

**Das trainierte Ohr bestimmt die App.** Referenz-Setup ist das BT-Hörgerät am linken Ohr (ADR-0007). Jedes Audio-Feature wird von dieser Frage aus gedacht: Kommt das Signal dort verständlich und mit dem richtigen Pegel an? Kanaltrennung (links/rechts/beide) bleibt als Werkzeug für Alternativ-Setups erhalten, gibt aber nicht den Takt vor.

## Tonalität der UI-Texte

- Deutsch, klar, erwachsen. Kurze Sätze.
- Sachlich-freundlich — nie kindisch, nie Betroffenheitston, nie Motivationsposter.
- Fehlermeldungen sagen, was zu tun ist („Keine Audioausgabe gefunden — ist das Hörgerät verbunden?"), statt nur, was schiefging.
- Fortschritt wird nüchtern berichtet (fällige Karten, absolvierte Wörter), nicht gefeiert und nicht angemahnt.

## Was AudioLex nicht ist

- Kein Produkt: nicht-kommerziell, keine öffentliche Verteilung, keine Härtung (Konzept, Kopfzeile).
- Kein Ersatz für HNO oder Audiologie.
- Keine Sprachlern-App mit Abo, Werbung oder Wettbewerb — auch wenn die Korpus-Architektur einen späteren Vokabeltrainer zulässt (Konzept 3.4), bleibt die Seele: konzentriertes Hörtraining.

## Stimme in einem Satz

AudioLex spricht wie ein verlässlicher Trainingspartner: sagt klar, was dran ist, wartet geduldig, urteilt nicht.
