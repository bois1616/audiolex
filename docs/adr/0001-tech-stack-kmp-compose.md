# ADR-0001: Kotlin Multiplatform + Compose Multiplatform

- **Status:** akzeptiert
- **Datum:** 2026-07-07

## Kontext

Das Konzept schlägt „Android nativ (Kotlin, Jetpack Compose)" vor, verlangt aber zugleich, die Apple-Option nicht auszuschließen. Ein nativer Android-Build macht iOS später zum vollständigen Rewrite. Zusätzlich läuft die Entwicklung zu 100 % agentengetrieben unter WSL2, wo ein Android-Emulator mühsam ist — der Agent braucht einen schnellen, lokalen Weg, Verhalten selbständig zu verifizieren.

## Entscheidung

Wir verwenden **Kotlin Multiplatform (KMP)** mit **Compose Multiplatform (CMP)** als UI:

- Targets jetzt: **Android** (primär, minSdk 29 / Testgerät Galaxy A53, Android 16) und **Desktop/JVM** (Entwicklungs- und Verifikations-Target).
- iOS wird nicht aktiviert, aber der Schnitt ist darauf ausgelegt: Logik in `commonMain`, Plattformzugriffe (Audio-Ausgabe) hinter expect/actual.
- Versionen zentral in `gradle/libs.versions.toml` (Kotlin 2.1.x, CMP 1.8.x, AGP 8.7.x, Gradle 8.11).

## Alternativen

- **Nativ Android (Konzeptvorschlag):** schnellster Android-MVP, aber iOS = Rewrite und jede Verifikation braucht Gerät/Emulator. Verworfen.
- **Flutter:** ein Codebase inkl. Linux-Desktop, sehr agentenfreundlich; aber Dart statt Kotlin (Konzeptpräferenz) und die feine Audio-Kontrolle (kanalgetrennte Pegel, SNR-Mixing) bräuchte eigene Platform-Channels. Verworfen.

## Konsequenzen

- Der Agent kann bauen, Tests fahren und die App am Desktop starten, ohne Emulator — kürzeste Feedback-Schleife.
- iOS später: Targets im Gradle-Build ergänzen + `AudioSink`-actual mit AVAudioEngine; braucht einen macOS-Build-Host.
- Toolchain ist schwerer als ein reines Android-Projekt (CMP-Plugin, Multi-Target-Konfiguration); Versionskompatibilität Kotlin↔CMP↔AGP muss bei Upgrades beachtet werden.
- Audio-Verhalten auf dem Desktop ist nur eine Näherung — hörgeräterelevante Tests finden immer auf dem Gerät statt (siehe AGENTS.md, Definition of Done).
