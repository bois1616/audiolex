package de.hexenwoche.audiolex

import androidx.room.Room
import androidx.room.RoomDatabase
import de.hexenwoche.audiolex.core.persistence.AudioLexDatabase
import java.io.File

/** Desktop: file in the user's data dir, so it survives across app runs. */
fun getDatabaseBuilder(): RoomDatabase.Builder<AudioLexDatabase> {
    val appDataDir = File(System.getProperty("user.home"), ".audiolex").apply { mkdirs() }
    val dbFile = File(appDataDir, "audiolex.db")
    return Room.databaseBuilder<AudioLexDatabase>(name = dbFile.absolutePath)
}
