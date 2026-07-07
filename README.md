# AudioLex

Hörtrainings-App zum Wiederaufbau der Zuordnung **Klang → Wort → Bedeutung** nach einseitigem Hörverlust mit Hörgeräteversorgung. Methodisch angelehnt an Spaced-Repetition-Systeme (Anki-Prinzip), aber mit Audio statt Text als Trigger-Reiz.

**Status:** Gerüstphase (M0) · nicht-kommerziell · rein lokale Datenhaltung

## Stack

Kotlin Multiplatform + Compose Multiplatform (siehe [ADR-0001](docs/adr/0001-tech-stack-kmp-compose.md)):

| Ziel | Zweck |
| --- | --- |
| Android (minSdk 29) | Primäre Zielplattform, Testgerät Galaxy A53 / Android 16 |
| Desktop (JVM) | Entwicklungs- und Verifikations-Target unter WSL2, kein Emulator nötig |
| iOS | Option, Modulschnitt vorbereitet, nicht aktiviert (braucht macOS-Host) |

## Build & Run

```bash
./gradlew :core:jvmTest              # Unit-Tests der Kernlogik
./gradlew :composeApp:run            # Desktop-App starten
./gradlew :composeApp:assembleDebug  # Android-APK bauen
./gradlew build                      # alles
```

Voraussetzungen: JDK 21, Android SDK (Pfad in `local.properties`).

## Projektnavigation

- Verbindlicher Arbeitsmodus: [AGENTS.md](AGENTS.md)
- Konzept/Vision: [docs/konzept/AudioLex-Konzept.md](docs/konzept/AudioLex-Konzept.md)
- Architektur: [docs/architektur.md](docs/architektur.md)
- Entscheidungen: [docs/adr/](docs/adr/)
- Aufgaben: [docs/backlog.md](docs/backlog.md)
- Umsetzungsjournal: [docs/umsetzungslog.md](docs/umsetzungslog.md)
