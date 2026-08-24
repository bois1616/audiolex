# DESIGN — UI/UX-Konzeption

Dieses Dokument beschreibt die Gestalt der App: Screens, Navigation, visuelle Prinzipien. Die technische Architektur steht in `docs/architektur.md` und `docs/adr/` und wird hier nicht dupliziert. Der Schnitt der UI-Stories folgt dem Szenario-Katalog in `docs/szenarien.md` (SDD); die Haltung hinter allen Texten und Entscheidungen steht in `SOUL.md`.

## Leitprinzipien

1. **Audio-first.** Das Ohr ist der Hauptkanal, das Auge sekundär. Während der Wiedergabe zeigt der Screen wenig — nichts darf mit dem Hörreiz konkurrieren.
2. **Eine Handlung pro Schritt.** Im Training gibt es je Moment genau eine erwartete Aktion: hören → (aufdecken) → bewerten. Keine parallelen Entscheidungen, keine Menüs im Trainingsfluss.
3. **Ausgabeweg immer sichtbar.** Referenz-Setup ist das BT-Hörgerät am linken Ohr (ADR-0007): Während des Trainings ist ablesbar, mit welchem Pegel trainiert wird; eine Kanalwahl (l/r/beide, nur bei Alternativ-Setups wirksam) wird bei BT-Ausgabe nicht als wirksam dargestellt.
4. **Große Ziele, eine Hand.** Die App wird am Handy einhändig bedient; Bewertungstasten und Aufdecken liegen daumenerreichbar in der unteren Hälfte.
5. **Ruhe statt Reiz.** Keine Animationen ohne Funktion, kein Konfetti, keine Badges. Der einzige inszenierte Moment ist das Aufdecken der Karte.
6. **Sofort trainierbar.** Vom App-Start bis zum ersten Wort höchstens zwei Antippen — zuletzt genutztes Preset und Kanal-Setup werden erinnert.

## Screenstruktur (Zielbild)

```text
Start           → Fällige Karten heute, Modus-Wahl (Lernen/Prüfen), Preset-Schnellwahl
                  unten ruhig: Sprachwahl, Kurzanleitung, Impressum, Version
Lernmodus       → Wort hören + Text sehen; Wiederholen / Weiter          (M2)
Prüfmodus       → verdeckte Karte → Aufdecken → 5 Bewertungstasten       (M3)
Einstellungen   → Kanal/Pegel, Störgeräusch/SNR (M4), Presets, Wortfilter (M5)
Statistik       → Sitzungsliste mit Datum/Uhrzeit, Kennzahlen je Sitzung   (M3, S12)
Kurzanleitung   → Modi, Bewertungsskala, Einstellungen erklärt             (ADR-0015)
```

Navigation flach: Start ist die Drehscheibe, Trainings-Screens sind Sackgassen mit „Beenden" zurück zur Drehscheibe. Maximal zwei Ebenen.

## Trainings-Screens im Detail

**Lernmodus (M2):** Zielwort groß und ruhig in der Bildmitte, immer an derselben Stelle — das Schriftbild ist die halbe Assoziation. Darunter: Wiederholen (Wort erneut abspielen) und Weiter. Fortschritt dezent („7 / 18"), Kanal-Badge am oberen Rand.

**Prüfmodus (M3):** Verdeckte Karte in konstanter Größe — die Silhouette darf die Wortlänge nicht verraten. Aufdecken über eine große Tippfläche (die Karte selbst). Danach fünf Bewertungstasten mit Intervall-Hinweis (Labels in der eingestellten UI-Sprache, ADR-0015):

```text
[ Sofort ]  [ Bald ]  [ Später ]  [ Gut ]   [ Perfekt ]     (de)
   1 min      10 min     1 Tag     1 Woche    1 Monat
[ Again  ]  [ Soon ]  [ Later  ]  [ Good ]  [ Perfect ]     (en)
   1 min      10 min     1 day     1 week     1 month
```

Die Tasten sind gleichwertig gestaltet (keine Ampelfarben Rot→Grün): die Skala steuert Wiederholung, sie benotet nicht (SOUL.md).

## Visuelle Prinzipien

- **Hell/Dunkel nach System**, Dark Mode gleichberechtigt gestaltet — Training findet auch abends statt.
- **Hoher Kontrast, große Typografie** für das Zielwort; Sekundäres (Fortschritt, Badges) tritt deutlich zurück.
- **Farbe trägt Bedeutung, nicht Dekoration**: Kanal-Kennzeichnung und Zustandsmeldungen dürfen Farbe nutzen, Schmuckfarben gibt es nicht.
- **UI-Texte Deutsch und Englisch** (ADR-0015), Ton nach SOUL.md — in beiden Sprachen derselbe: knapp und sachlich. Die Sprachwahl steht auf dem Startbildschirm, nicht in den Einstellungen: Wer die aktuelle Sprache nicht lesen kann, darf nicht raten müssen, welcher Knopf die Einstellungen öffnet. Sie sitzt dort in der ruhigen Zone unten, jede Sprache in sich selbst geschrieben („Deutsch", „English"), die aktive in der Akzentfarbe. Der **Korpus** bleibt deutsch.

## Komponenten (Zielbild)

| Komponente | Zweck |
| --- | --- |
| `WordCard` | Zielwort groß, positionsstabil (Lernmodus) |
| `RevealCard` | verdeckte Karte konstanter Größe, Tippfläche zum Aufdecken (Prüfmodus) |
| `RatingBar` | 5 gleichwertige Bewertungstasten mit Intervall-Hinweis |
| `ChannelBadge` | Ausgabeweg + Pegel, immer sichtbar im Training; Kanalwahl nur bei Alternativ-Setup als wirksam gezeigt (ADR-0007) |
| `SessionProgress` | dezenter Fortschritt („7 / 18") |
| `NoiseControl` | Störszenario-Wahl + SNR-Regler (M4) |

## Entschieden

- Stack: Compose Multiplatform, Desktop als Dev-Target (ADR-0001) — keine UI-Bibliothek darüber hinaus.
- Die aktuelle Kanaltest-UI in `App.kt` bleibt als Smoke-Test, bis der Lernmodus-Screen (M2) sie ablöst.
- Referenz-Trainings-Setup: BT-Hörgerät, linkes Ohr; Abspielgerät Smartphone, nicht ans A53 gebunden (ADR-0007). Kanaltrennung ist Setup-Option, nicht Kern.
- Statistik sitzungsbasiert: Liste abgeschlossener Sitzungen mit Datum/Uhrzeit, mehrere pro Tag (Szenario S12).
- Sitzungen enden sauber statt Pause/Resume (Szenario S5) — passt zur Sackgassen-Navigation mit „Beenden".
- Kein Screen hält Inhalt allein mit `Modifier.weight(1f)` am unteren Rand: Reicht die Höhe nicht, wird geschnitten statt gescrollt. Der Startbildschirm kombiniert seit v0.34.0 `heightIn(min = Viewport)` mit `Arrangement.SpaceBetween` in einer scrollenden Spalte — unten verankert, solange Platz ist, scrollend, sobald nicht. Dieselbe Fehlerklasse wie die dreimal unerreichbare „Zurück"-Taste.

## Noch offen

- [ ] Bewertung automatisch vorschlagen (Reaktionszeit)? Backlog [P3] [PROP] — würde die `RatingBar` um einen Vorschlagszustand erweitern.
- [ ] Konkrete Gestalt des BT-Hinweises, wenn Kanalwahl ≠ „beide" über BT aktiv ist (M4 Settings).
