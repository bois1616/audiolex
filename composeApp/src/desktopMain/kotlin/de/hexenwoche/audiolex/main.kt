package de.hexenwoche.audiolex

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.hexenwoche.audiolex.core.persistence.createAudioLexDatabase
import de.hexenwoche.audiolex.core.time.systemClock

fun main() {
    val database = createAudioLexDatabase(getDatabaseBuilder())
    val clock = systemClock()
    val ownCorpusDir = getOwnCorpusDir().absolutePath
    val ownNoiseDir = getOwnNoiseDir().absolutePath
    application {
        Window(onCloseRequest = ::exitApplication, title = "AudioLex") {
            App(
                database,
                clock,
                onExitApp = ::exitApplication,
                ownCorpusDir = ownCorpusDir,
                ownNoiseDir = ownNoiseDir,
            )
        }
    }
}
