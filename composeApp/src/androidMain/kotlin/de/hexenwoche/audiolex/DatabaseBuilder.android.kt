package de.hexenwoche.audiolex

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import de.hexenwoche.audiolex.core.persistence.AudioLexDatabase

/** Android: file in the app's private database dir (standard Room-KMP setup). */
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AudioLexDatabase> {
    val dbFile = context.applicationContext.getDatabasePath("audiolex.db")
    return Room.databaseBuilder<AudioLexDatabase>(context = context.applicationContext, name = dbFile.absolutePath)
}
