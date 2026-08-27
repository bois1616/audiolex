# ADR-0002: Schlanke Governance statt vollem wegrose-Apparat

- **Status:** akzeptiert
- **Datum:** 2026-07-07

## Kontext

Das Konzept (Abschnitt 5) überträgt den vollen wegrose-Governance-Apparat: AGENTS.md, CLAUDE.md, RSI.md, schema-validierte EXP-Records (YAML), PROP-System, ADRs, Sprint-Scrum über GitHub Projects. AudioLex ist ein Solo-, Nicht-kommerziell-Projekt mit einem Autor; gefordert ist Nachvollziehbarkeit „in bekannter Art" (Backlog, Umsetzungslog) — Sprints ausdrücklich nicht als Dogma.

## Entscheidung

Wir verwenden den schlanken Satz:

- `AGENTS.md` (verbindlicher Arbeitsmodus) + `CLAUDE.md` (Kontext, ohne Duplikate)
- `docs/backlog.md` — P0–P3, Checkbox-Format mit `Hinweis:` bei Erledigung (wp_service-Stil)
- `docs/implementation-log.md` — Journal, neueste zuerst
- `docs/adr/` — Entscheidungen
- **Meilensteine M0–M5** statt Sprints; Reihenfolge darf begründet abweichen.

Ersatz für die entfallenden Artefakte: RSI-Erkenntnisse fließen als Log-Einträge bzw. in ADR-Konsequenzen ein; EXP-Records gehen im Umsetzungslog auf; PROP wird zum Backlog-Tag `[PROP]`; offener Klärungsbedarf zum Tag `[KLÄRUNG]`. Backlog dateibasiert statt GitHub Projects (kein Remote nötig, versioniert, direkt agentenbearbeitbar).

## Alternativen

- **Voller wegrose-Apparat:** bewährt im Team-/Kundenkontext, aber hier Zeremonie ohne Adressaten; Pflegeaufwand konkurriert mit Produktarbeit. Verworfen.

## Konsequenzen

- Weniger Dateien, ein Ort pro Frage („Wo steht was?" → AGENTS.md-Tabelle).
- Sollte das Projekt Mitwirkende bekommen oder auf GitHub Projects wechseln, lässt sich das Backlog 1:1 migrieren; EXP/RSI können bei Bedarf wieder ausgegliedert werden (abgelöst-Vermerk hier eintragen).
