package de.hexenwoche.audiolex.core.corpus

import de.hexenwoche.audiolex.core.audio.WavFile
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the corpus that actually ships, not a fixture.
 *
 * Every entry here is a release blocker if it breaks: a `fileRef` pointing
 * at nothing means a word that plays silence on a stranger's phone, and
 * `recordings.json` is written by `tools/generate_tts.py` rather than by
 * hand -- a script that has already, once, quietly rendered files nobody
 * asked for. Since ADR-0016 the corpus also spans two languages, so an
 * empty drawer is a new way to ship a dead switch.
 *
 * This is the one test that reaches out of `:core` into `:composeApp`'s
 * resources. That coupling is deliberate and is the cheaper of two evils:
 * `:composeApp` has no test source set, and the alternative is no guard at
 * all on the files that get published.
 */
class CorpusIntegrityTest {

    private val corpusDir: File = run {
        // Walk up to the repo root instead of assuming the working
        // directory, so this survives being run from the module or the root.
        var dir = File(".").absoluteFile
        while (dir.parentFile != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        File(dir, "composeApp/src/commonMain/composeResources/files/corpus")
    }

    // Same leniency the app parses with, so this test can't pass on files
    // the app would choke on -- or fail on ones it happily reads.
    private val json = Json { ignoreUnknownKeys = true }

    private val words: List<Word> by lazy {
        json.decodeFromString<List<Word>>(File(corpusDir, "words.json").readText())
    }
    private val recordings: List<AudioRecording> by lazy {
        json.decodeFromString<List<AudioRecording>>(File(corpusDir, "recordings.json").readText())
    }

    @Test
    fun `the corpus directory is where this test thinks it is`() {
        assertTrue(File(corpusDir, "words.json").isFile, "not found: ${corpusDir.absolutePath}")
    }

    @Test
    fun `ids are unique on both sides`() {
        assertEquals(words.size, words.map { it.id }.toSet().size, "duplicate word id")
        assertEquals(recordings.size, recordings.map { it.id }.toSet().size, "duplicate recording id")
    }

    @Test
    fun `every recording points at a word that exists`() {
        val wordIds = words.map { it.id }.toSet()
        val orphans = recordings.filter { it.wordId !in wordIds }.map { it.id }

        assertTrue(orphans.isEmpty(), "recordings without a word: $orphans")
    }

    @Test
    fun `every word has at least one recording`() {
        // A word without audio is untrainable: the training screens surface
        // it as "Keine Aufnahme für ... gefunden" mid-session.
        val withAudio = recordings.map { it.wordId }.toSet()
        val mute = words.filter { it.id !in withAudio }.map { it.id }

        assertTrue(mute.isEmpty(), "words without any recording: $mute")
    }

    @Test
    fun `every referenced audio file exists and is in the corpus format`() {
        val missing = mutableListOf<String>()
        val wrongFormat = mutableListOf<String>()

        for (recording in recordings) {
            val file = File(corpusDir, recording.fileRef)
            if (!file.isFile) {
                missing += recording.fileRef
                continue
            }
            val buffer = WavFile.decode(file.readBytes())
            // 22050 Hz mono is not cosmetic: mixWithNoise refuses anything
            // else, so a stray stereo file would fail only once the noise
            // overlay is switched on.
            if (buffer.sampleRate != 22050 || buffer.channels != 1 || buffer.frameCount == 0) {
                wrongFormat += "${recording.fileRef} (${buffer.sampleRate} Hz, ${buffer.channels} ch, " +
                    "${buffer.frameCount} frames)"
            }
        }

        assertTrue(missing.isEmpty(), "missing audio files: $missing")
        assertTrue(wrongFormat.isEmpty(), "wrong format: $wrongFormat")
    }

    @Test
    fun `every language the app offers has something to train`() {
        // The "Schalter ohne Inhalt = totes UI" lesson from the sentence
        // arc, now applied per language (ADR-0016).
        for (language in CorpusLanguage.entries) {
            val drawer = mergeCorpus(words, recordings, emptyList(), language = language)
            assertTrue(drawer.words.isNotEmpty(), "no entries filed under $language")
            assertTrue(drawer.recordings.isNotEmpty(), "no recordings under $language")
        }
    }

    @Test
    fun `every entry is filed under a language the app can show`() {
        val orphaned = words.filter { word -> CorpusLanguage.entries.none { it.matches(word.language) } }

        assertTrue(orphaned.isEmpty(), "entries in a language with no drawer: ${orphaned.map { it.id to it.language }}")
    }

    @Test
    fun `both kinds exist in every language, so the Woerter-Saetze switch is never dead`() {
        for (language in CorpusLanguage.entries) {
            for (kind in EntryKind.entries) {
                val drawer = mergeCorpus(words, recordings, emptyList(), kind = kind, language = language)
                assertTrue(drawer.words.isNotEmpty(), "$language has no entries of kind $kind")
            }
        }
    }
}
