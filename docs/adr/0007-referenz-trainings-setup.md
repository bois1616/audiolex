# ADR-0007: Referenz-Trainings-Setup — BT-Hörgerät, linkes Ohr

- **Status:** akzeptiert
- **Datum:** 2026-07-08

## Kontext

Das Opus-Review (Befund 2, P0, `docs/reviews/2026-07-07-m1-audio-review.md`) hat aufgedeckt: Das Konzept behandelt Kanaltrennung (links/rechts/beide, getrennte Pegel) als zentrales Trainingsinstrument, aber im realen Setup erreicht BT-Audio nur das einzelne Hörgerät am linken Ohr — das gesunde rechte Ohr bekommt über diesen Pfad nie ein Signal, und das Hörgerät summiert Stereo zu Mono. Kanaltrennung ist dort strukturell wirkungslos. Zu klären war das Referenz-Trainings-Setup (Kabel-Kopfhörer? Lautsprecher? je Szenario unterschiedlich?), weil es Session-Parameter (M2), Presets (M4) und die Gewichtung des Kernfeatures bestimmt.

## Entscheidung

Entscheid des Autors vom 2026-07-08:

1. **Referenz-Trainings-Setup ist das BT-Hörgerät am linken (trainierten) Ohr.** Abspielgerät ist das Smartphone; das Galaxy A53 ist Testgerät, die App wird aber **nicht an dieses konkrete Gerät gebunden** (keine weiteren gerätespezifischen Verdrahtungen). Nachtrag: Der ursprünglich vermutete `swapStereoChannels`-Fix wurde per Re-Test-Protokoll widerlegt (Review Befund 1, ADR-0003) — das Gerät war nie fehlerhaft, der Fix selbst war der Bug und ist zurückgebaut.
2. **Kanaltrennung wird vom Kernfeature zur Setup-Option** für Alternativ-Hardware (z. B. Kabel-Kopfhörer): `StereoGain` bleibt im Mixer erhalten und getestet, wird aber nicht mehr als primäres UI-Feature nach M1/M2 durchgestochen, sondern als Einstellung geführt (M4).

## Alternativen

- **Kabel-Kopfhörer (USB-C) als Referenz** — der einzige Weg, auf dem links/rechts/beide wie ursprünglich konzipiert funktioniert. Verworfen als *Referenz*: Trainiert werden soll die reale Hörsituation, und die ist Hörgerät am linken Ohr. Bleibt als Alternativ-Setup möglich — genau deshalb bleibt der Mixer kanalfähig.
- **Je Szenario unterschiedlich** (BT für Alltagsnähe, Kabel für Kanaltrennungs-Übungen) — verworfen für den MVP: zwei Referenz-Setups verdoppeln den Verifikationsaufwand am Gerät. Kann später als Preset-Dimension zurückkommen.
- **Lautsprecher (Freifeld)** — keine Kontrolle darüber, welches Ohr was erreicht; als Referenz ungeeignet.

## Konsequenzen

- **Was im Referenz-Setup zählt:** Gesamtpegel und Verständlichkeit am linken Ohr, später SNR/Störgeräusch — nicht die Kanalwahl. Geräte-Verifikation (AGENTS.md §4.3) testet künftig gegen dieses Setup.
- Die App muss davon ausgehen, dass Stereo im Hörgerät **mono summiert** wird; Panning ist dort nicht wahrnehmbar. Die UI darf eine Kanalwahl ≠ „beide" bei BT-Ausgabe nicht als wirksam suggerieren (Hinweis, M4 Settings).
- Backlog-M1-Item „Kanalsteuerung bis in die UI durchstechen" wird herabgestuft und nach M4 (Settings) verschoben — löst zugleich die Review-Anmerkung „Backlog-Überschneidung auflösen".
- Formulierungen „Kanalsteuerung ist Kernfeature" in `SOUL.md`/`CLAUDE.md` werden angepasst: Kern ist „das Signal erreicht das trainierte Ohr verständlich und mit richtigem Pegel", Kanaltrennung ist das Werkzeug für Alternativ-Setups.
- Bewusst offen gehalten: Ändert sich die Versorgung (beidseitig, Kopfhörer-Training), ist nur UI-/Preset-Arbeit nötig — Mixer (`StereoGain`) und Datenmodell bleiben kanalfähig.
