# ADR-0004: Persistenz — Room KMP + DataStore

- **Status:** akzeptiert (Spike 2026-07-10 bestätigt)
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
- **Spike bestätigt (2026-07-10):** Room 2.8.4 + `sqlite-bundled` 2.7.0 + KSP `2.1.21-2.0.2` (exakt zur Projekt-Kotlin-Version 2.1.21 passend) liefen im `:core`-Multi-Target-Build (`androidTarget` + `jvm`) ohne Reibung — kein Fallback auf SQLDelight nötig. Setup: `com.google.devtools.ksp`- und `androidx.room`-Gradle-Plugin in `core/build.gradle.kts`, `ksp{Android,Jvm}`-Dependency-Deklarationen für den Room-Compiler (Dependency-Key richtet sich nach dem Kotlin-Target-Namen, hier `kspJvm`, **nicht** `kspDesktop` — das JVM-Target dieses Projekts heißt schlicht `jvm()`, ohne benannten Zusatz). `RoomDatabaseConstructor`-Pattern: `expect object` in `commonMain` mit `@ConstructedBy`, **kein** eigenes `actual object` schreiben — der Room-KSP-Compiler generiert das je Target selbst; ein manuell ergänztes `actual` kollidiert damit. Builder-Fabriken (`Room.databaseBuilder`/`Room.inMemoryDatabaseBuilder` + `BundledSQLiteDriver`) liegen als normale Top-Level-Funktionen in `jvmMain`, kein `expect`/`actual` nötig, da sie keine gemeinsame Signatur mit Android brauchen (Android bekäme einen `Context`-Parameter, das ist Sache des echten Schemas, nicht dieses Spikes). Realer Roundtrip-Test (`SpikeDatabaseTest`, `:core:jvmTest`) schreibt/liest über eine echte Datei-SQLite-DB, öffnet sie in einer zweiten `Room`-Instanz erneut, um zu beweisen, dass tatsächlich auf Platte persistiert wurde und nicht nur im Prozessspeicher der ersten Instanz gehalten wird. `./gradlew build` (inkl. `:composeApp:assembleDebug`, zieht Room für `androidTarget` mit) komplett grün.
- Wegwerf-Artefakte des Spikes (`SpikeCardEntity`/`SpikeCardDao`/`SpikeDatabase` in `core/.../persistence/`) bleiben bis zum echten Schema-Item stehen, dann ersetzt/gelöscht — kein Produktionscode.
