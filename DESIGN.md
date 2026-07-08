# DESIGN — UI/UX-Konzeption

Dieses Dokument beschreibt die Gestalt der App: Screens, Navigation, visuelle Prinzipien. Die technische Architektur steht in `docs/architektur.md` und `docs/adr/` und wird hier nicht dupliziert. Der Schnitt der UI-Stories folgt dem Szenario-Katalog in `docs/szenarien.md` (SDD); die Haltung hinter allen Texten und Entscheidungen steht in `SOUL.md`.

## Leitprinzipien

1. **Audio-first.** Das Ohr ist der Hauptkanal, das Auge sekundär. Während der Wiedergabe zeigt der Screen wenig — nichts darf mit dem Hörreiz konkurrieren.
2. **Eine Handlung pro Schritt.** Im Training gibt es je Moment genau eine erwartete Aktion: hören → (aufdecken) → bewerten. Keine parallelen Entscheidungen, keine Menüs im Trainingsfluss.
3. **Kanal immer sichtbar.** Welches Ohr gerade angesteuert wird (l/r/beide, Pegel), ist während des Trainings jederzeit ablesbar — Kernfeature, kein verstecktes Setting.
4. **Große Ziele, eine Hand.** Die App wird am Handy einhändig bedient; Bewertungstasten und Aufdecken liegen daumenerreichbar in der unteren Hälfte.
5. **Ruhe statt Reiz.** Keine Animationen ohne Funktion, kein Konfetti, keine Badges. Der einzige inszenierte Moment ist das Aufdecken der Karte.
6. **Sofort trainierbar.** Vom App-Start bis zum ersten Wort höchstens zwei Antippen — zuletzt genutztes Preset und Kanal-Setup werden erinnert.

## Screenstruktur (Zielbild)

```text
Start           → Fällige Karten heute, Modus-Wahl (Lernen/Prüfen), Preset-Schnellwahl
Lernmodus       → Wort hören + Text sehen; Wiederholen / Weiter          (M2)
Prüfmodus       → verdeckte Karte → Aufdecken → 5 Bewertungstasten       (M3)
Einstellungen   → Kanal/Pegel, Störgeräusch/SNR (M4), Presets, Wortfilter (M5)
Statistik       → später; Umfang offen, siehe docs/szenarien.md (S-OFFEN-2)
```

Navigation flach: Start ist die Drehscheibe, Trainings-Screens sind Sackgassen mit „Beenden" zurück zur Drehscheibe. Maximal zwei Ebenen.

## Trainings-Screens im Detail

**Lernmodus (M2):** Zielwort groß und ruhig in der Bildmitte, immer an derselben Stelle — das Schriftbild ist die halbe Assoziation. Darunter: Wiederholen (Wort erneut abspielen) und Weiter. Fortschritt dezent („7 / 18"), Kanal-Badge am oberen Rand.

**Prüfmodus (M3):** Verdeckte Karte in konstanter Größe — die Silhouette darf die Wortlänge nicht verraten. Aufdecken über eine große Tippfläche (die Karte selbst). Danach fünf Bewertungstasten mit deutschen Labels und Intervall-Hinweis:

```text
[ Sofort ]  [ Bald ]  [ Später ]  [ Gut ]   [ Perfekt ]
   1 min      10 min     1 Tag     1 Woche    1 Monat
```

Die Tasten sind gleichwertig gestaltet (keine Ampelfarben Rot→Grün): die Skala steuert Wiederholung, sie benotet nicht (SOUL.md).

## Visuelle Prinzipien

- **Hell/Dunkel nach System**, Dark Mode gleichberechtigt gestaltet — Training findet auch abends statt.
- **Hoher Kontrast, große Typografie** für das Zielwort; Sekundäres (Fortschritt, Badges) tritt deutlich zurück.
- **Farbe trägt Bedeutung, nicht Dekoration**: Kanal-Kennzeichnung und Zustandsmeldungen dürfen Farbe nutzen, Schmuckfarben gibt es nicht.
- **UI-Texte Deutsch** (AGENTS.md §5), Ton nach SOUL.md.

## Komponenten (Zielbild)

| Komponente | Zweck |
| --- | --- |
| `WordCard` | Zielwort groß, positionsstabil (Lernmodus) |
| `RevealCard` | verdeckte Karte konstanter Größe, Tippfläche zum Aufdecken (Prüfmodus) |
| `RatingBar` | 5 gleichwertige Bewertungstasten mit Intervall-Hinweis |
| `ChannelBadge` | aktive Kanalwahl l/r/beide + Pegel, immer sichtbar im Training |
| `SessionProgress` | dezenter Fortschritt („7 / 18") |
| `NoiseControl` | Störszenario-Wahl + SNR-Regler (M4) |

## Entschieden

- Stack: Compose Multiplatform, Desktop als Dev-Target (ADR-0001) — keine UI-Bibliothek darüber hinaus.
- Die aktuelle Kanaltest-UI in `App.kt` bleibt als Smoke-Test, bis der Lernmodus-Screen (M2) sie ablöst.

## Noch offen

- [ ] Referenz-Trainings-Setup (einzelnes BT-Hörgerät vs. Kopfhörer) — P0 aus dem Opus-Review; bestimmt Default und Darstellung des `ChannelBadge`.
- [ ] Umfang der Statistik (nur „fällig heute" vs. Verlaufskurven) — im Konzept nicht ausgeführt.
- [ ] Bewertung automatisch vorschlagen (Reaktionszeit)? Backlog [P3] [PROP] — würde die `RatingBar` um einen Vorschlagszustand erweitern.
