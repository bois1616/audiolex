# ADR-0005: SRS — feste Intervalltabelle hinter Scheduler-Interface

- **Status:** akzeptiert (für MVP)
- **Datum:** 2026-07-07

## Kontext

Der Prüfmodus steuert Wiederholungen nach Spaced-Repetition-Logik. Das Konzept nennt eine 5-stufige manuelle Bewertung mit festen Intervallen (Sofort 1 min, Bald 10 min, Später 1 Tag, Gut 1 Woche, Perfekt 1 Monat) und lässt offen, ob später ein adaptives Verfahren (SM-2-echt, FSRS) mit automatischer Bewertungsableitung folgt.

## Entscheidung

MVP: **feste Intervalltabelle, manuell bewertet** — implementiert als `FixedIntervalScheduler` hinter dem Interface `ReviewScheduler` (`:core`, Paket `srs`). Die Bewertung wählt das Intervall direkt; kein Ease-Faktor, kein Karteizustand über `repetitions` hinaus. Nachvollziehbarkeit schlägt Optimalität: der Nutzer (= Autor) soll das Systemverhalten unmittelbar verstehen.

## Alternativen

- **SM-2 vollständig (Ease-Faktor, adaptive Intervalle):** besser bei großen Kartenmengen, aber intransparenter; für einen Korpus im Hundert-Wörter-Bereich ohne Not. Später möglich.
- **FSRS:** modernster Ansatz, braucht Review-Historie als Trainingsdaten — genau die sammeln wir mit dem MVP ohnehin. Kandidat für später, als `[PROP]` im Backlog.

## Konsequenzen

- Algorithmuswechsel ist ein reiner Implementierungstausch hinter `ReviewScheduler`; das Datenmodell speichert Rohdaten (Bewertung, Zeitpunkt), nicht nur den nächsten Termin, damit spätere Verfahren die Historie nutzen können.
- Automatische Bewertungsableitung (Reaktionszeit/Wiederholungszahl) bleibt bewusst draußen — Backlog `[PROP]`.
