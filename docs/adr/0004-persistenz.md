# ADR-0004: Persistenz — Room KMP + DataStore (Vorschlag)

- **Status:** vorgeschlagen (Entscheid vor M3 bestätigen)
- **Datum:** 2026-07-07

## Kontext

Ab M3 (Prüfmodus) müssen `ReviewCard`-Fälligkeiten, Sitzungsergebnisse und Einstellungsprofile lokal persistiert werden — auf Android und Desktop, iOS-fähig. Keine Cloud (Konzept 4.5).

## Entscheidung

Vorschlag: **Room (ab 2.7, KMP-fähig)** mit `BundledSQLiteDriver` für strukturierte Daten (Words, ReviewCards, Sessions) und **DataStore (KMP)** für Einstellungen/Profile. Beides androidx-Ökosystem, konsistent zum übrigen Stack, Flow-Integration.

## Alternativen

- **SQLDelight:** ältester, sehr bewährter KMP-Weg, SQL als Quelle. Gleichwertig; gegen Room nur der zusätzliche Ökosystem-Bruch. Bleibt Fallback, falls Room-KSP im Multi-Target-Setup Reibung macht.
- **Reine Dateien (JSON):** reicht für Settings, aber Review-Queues (Abfragen nach Fälligkeit) wollen einen Index/SQL.

## Konsequenzen

- KSP-Konfiguration im KMP-Build kommt dazu (bekannte Fummelei, einmalig).
- Entscheid wird zu Beginn von M3 mit einem Spike bestätigt oder zugunsten SQLDelight gekippt — dieses ADR dann auf „akzeptiert" bzw. „abgelöst" setzen.
