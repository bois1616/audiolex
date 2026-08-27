# ADR-0004: Persistence — Room KMP + DataStore

- **Status:** accepted (confirmed by a spike 2026-07-10)
- **Date:** 2026-07-07

## Context

From M3 (exam mode) on, `ReviewCard` due dates, session results and settings profiles have to be persisted locally — on Android and desktop, ready for iOS. No cloud (concept 4.5).

## Decision

The proposal: **Room (from 2.7, KMP-capable)** with the `BundledSQLiteDriver` for structured data (words, review cards, sessions) and **DataStore (KMP)** for settings and profiles. Both are androidx ecosystem, consistent with the rest of the stack, with Flow integration.

## Alternatives

- **SQLDelight:** the oldest and very well-proven KMP route, with SQL as the source. Equivalent; the only thing against it versus Room is the additional break in the ecosystem. It stays the fallback should Room's KSP cause friction in the multi-target setup.
- **Plain files (JSON):** enough for settings, but review queues (queries by due date) want an index or SQL.

## Consequences

- KSP configuration in the KMP build joins the picture (known fiddliness, one-off).
- **Confirmed by the spike (2026-07-10):** Room 2.8.4 + `sqlite-bundled` 2.7.0 + KSP `2.1.21-2.0.2` (matching the project's Kotlin version 2.1.21 exactly) ran in the `:core` multi-target build (`androidTarget` + `jvm`) without friction — no fallback to SQLDelight needed. The setup: the `com.google.devtools.ksp` and `androidx.room` Gradle plugins in `core/build.gradle.kts`, `ksp{Android,Jvm}` dependency declarations for the Room compiler (the dependency key follows the Kotlin target name, here `kspJvm`, **not** `kspDesktop` — this project's JVM target is simply `jvm()`, without a named suffix). The `RoomDatabaseConstructor` pattern: an `expect object` in `commonMain` with `@ConstructedBy`, and **no** `actual object` of your own — the Room KSP compiler generates that per target itself, and a manually added `actual` collides with it. The builder factories (`Room.databaseBuilder`/`Room.inMemoryDatabaseBuilder` + `BundledSQLiteDriver`) sit as ordinary top-level functions in `jvmMain`, with no `expect`/`actual` needed, since they need no shared signature with Android (Android would take a `Context` parameter, which is a matter for the real schema, not for this spike). A real roundtrip test (`SpikeDatabaseTest`, `:core:jvmTest`) writes and reads through a real file-based SQLite DB and reopens it in a second `Room` instance, to prove that it really persisted to disk and is not just held in the first instance's process memory. `./gradlew build` (including `:composeApp:assembleDebug`, which pulls Room in for `androidTarget`) fully green.
- The spike's throwaway artefacts (`SpikeCardEntity`/`SpikeCardDao`/`SpikeDatabase` in `core/.../persistence/`) stay until the real schema item, then get replaced or deleted — no production code.
