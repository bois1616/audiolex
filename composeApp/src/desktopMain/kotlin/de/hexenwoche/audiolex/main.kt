package de.hexenwoche.audiolex

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.hexenwoche.audiolex.core.persistence.createAudioLexDatabase

fun main() {
    val database = createAudioLexDatabase(getDatabaseBuilder())
    application {
        Window(onCloseRequest = ::exitApplication, title = "AudioLex") {
            App(database)
        }
    }
}
