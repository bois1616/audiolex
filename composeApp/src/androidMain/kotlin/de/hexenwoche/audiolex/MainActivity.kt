package de.hexenwoche.audiolex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.hexenwoche.audiolex.core.persistence.createAudioLexDatabase
import de.hexenwoche.audiolex.core.time.systemClock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = createAudioLexDatabase(getDatabaseBuilder(applicationContext))
        val clock = systemClock()
        val ownCorpusDir = getOwnCorpusDir(applicationContext).absolutePath
        setContent {
            App(database, clock, onExitApp = { finishAndRemoveTask() }, ownCorpusDir = ownCorpusDir)
        }
    }
}
