package de.hexenwoche.audiolex.core.persistence

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

/** jvm/desktop builder: file-backed so a real roundtrip survives process restarts. */
fun createSpikeDatabase(path: String): SpikeDatabase =
    Room.databaseBuilder<SpikeDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

/** In-memory variant for tests -- no file left behind. */
fun createInMemorySpikeDatabase(): SpikeDatabase =
    Room.inMemoryDatabaseBuilder<SpikeDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()

internal fun tempDbPath(): String = File.createTempFile("spike-", ".db").apply { delete() }.absolutePath
