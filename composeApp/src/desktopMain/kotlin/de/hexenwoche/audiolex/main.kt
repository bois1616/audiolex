package de.hexenwoche.audiolex

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.hexenwoche.audiolex.core.persistence.createAudioLexDatabase
import de.hexenwoche.audiolex.core.time.systemClock

fun main() {
    val database = createAudioLexDatabase(getDatabaseBuilder())
    val clock = systemClock()
    val ownCorpusDir = getOwnCorpusDir().absolutePath
    val ownNoiseDir = getOwnNoiseDir().absolutePath
    application {
        // Phone proportions rather than Compose's 800x600 default: the
        // desktop target exists to stand in for the A53 (AGENTS.md §7), and
        // a landscape window does not show what a phone screen shows. 440x900
        // is close to the A53's ~411x914dp. The layouts survive either shape
        // -- every screen scrolls -- but a dev target you have to scroll to
        // see the whole hub is a poor stand-in for a device where it fits.
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(width = 440.dp, height = 900.dp),
            title = "AudioLex",
        ) {
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
