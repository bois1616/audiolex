package de.hexenwoche.audiolex.core.i18n

import de.hexenwoche.audiolex.core.audio.OutputSetup
import de.hexenwoche.audiolex.core.audio.OwnNoiseSource
import de.hexenwoche.audiolex.core.corpus.EntryKind
import de.hexenwoche.audiolex.core.settings.ChannelMode
import de.hexenwoche.audiolex.core.settings.CorpusMode
import de.hexenwoche.audiolex.core.settings.SettingsProfile
import de.hexenwoche.audiolex.core.settings.ThemeMode
import de.hexenwoche.audiolex.core.srs.ReviewRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The parts of a catalog that are logic rather than wording (ADR-0015):
 * the enum mappings, the plural forms and the list joiner. A missing
 * translation is already a compile error, so these tests aim at what the
 * compiler cannot see -- two enum entries mapped to the same label, a
 * plural that ignores its count, a joiner that drops an item.
 */
class StringsTest {

    private val catalogs = listOf(GermanStrings, EnglishStrings)

    @Test
    fun `enum mappings are total and free of collisions`() {
        for (strings in catalogs) {
            assertDistinct(ReviewRating.entries.map { strings.ratingLabel(it) })
            assertDistinct(ReviewRating.entries.map { strings.intervalHint(it) })
            assertDistinct(SettingsProfile.entries.map { strings.profileLabel(it) })
            assertDistinct(ThemeMode.entries.map { strings.themeModeLabel(it) })
            assertDistinct(CorpusMode.entries.map { strings.corpusModeLabel(it) })
            assertDistinct(ChannelMode.entries.map { strings.channelModeLabel(it) })
            assertDistinct(OutputSetup.entries.map { strings.outputSetupDetected(it) })
            assertDistinct(EntryKind.entries.map { strings.kindLabel(it) })
            assertDistinct(OwnNoiseSource.entries.map { strings.noiseSourceLabel(it) })
        }
    }

    @Test
    fun `plural forms actually distinguish one from many`() {
        for (strings in catalogs) {
            assertDistinct(listOf(strings.wordCount(1), strings.wordCount(2)))
            assertDistinct(listOf(strings.sentenceCount(1), strings.sentenceCount(2)))
            assertDistinct(listOf(strings.channelCount(1), strings.channelCount(2)))
        }
    }

    @Test
    fun `counts carry their number`() {
        for (strings in catalogs) {
            val counted = listOf(
                strings.wordCount(7),
                strings.sentenceCount(7),
                strings.channelCount(7),
                strings.recordingCount(7),
                strings.noiseCount(7),
                strings.sessionCount(7),
                strings.entryCount(7),
                strings.cardsRatedSentence(7),
                strings.recorderFinished(7),
            )

            for (text in counted) assertTrue("7" in text, "no count in: $text")
        }
    }

    @Test
    fun `the SNR label signs a positive value and keeps the minus on a negative one`() {
        for (strings in catalogs) {
            assertTrue("+5" in strings.snrLabel(5), strings.snrLabel(5))
            assertTrue("-5" in strings.snrLabel(-5), strings.snrLabel(-5))
            assertTrue("+0" in strings.snrLabel(0), strings.snrLabel(0))
        }
    }

    @Test
    fun `joinLast keeps every part and only conjoins the last one`() {
        assertEquals("a", GermanStrings.joinLast(listOf("a")))
        assertEquals("", GermanStrings.joinLast(emptyList()))
        assertEquals("a und b", GermanStrings.joinLast(listOf("a", "b")))
        assertEquals("a, b und c", GermanStrings.joinLast(listOf("a", "b", "c")))
        assertEquals("a and b", EnglishStrings.joinLast(listOf("a", "b")))
        assertEquals("a, b and c", EnglishStrings.joinLast(listOf("a", "b", "c")))
    }

    @Test
    fun `an unknown session mode is shown as it was stored rather than swallowed`() {
        for (strings in catalogs) {
            assertEquals("LERNMODUS", strings.sessionModeLabel("LERNMODUS"))
            assertTrue(strings.sessionModeLabel("PRUEFMODUS").isNotBlank())
        }
    }

    @Test
    fun `interpolated texts carry the value they were given`() {
        for (strings in catalogs) {
            assertTrue("Haus" in strings.noRecordingFound("Haus"))
            assertTrue("Haus" in strings.noRecordingAvailable("Haus"))
            assertTrue("Haus" in strings.deleteConfirmBody("Haus"))
            assertTrue("k-1" in strings.wordForCardNotFound("k-1"))
            assertTrue("bus.wav" in strings.recordingFileMissing("bus.wav"))
            assertTrue("bus.wav" in strings.fileChosen("bus.wav"))
            assertTrue("boom" in strings.playbackFailed("boom"))
            assertTrue("boom" in strings.corpusLoadFailed("boom"))
            assertTrue("boom" in strings.recorderFailed("boom"))
            assertTrue("12:00" in strings.recordingLabelSuggestion("12:00"))
            assertTrue("Dokumente" in strings.backupSaved("x", "Dokumente"))
            assertTrue("44100" in strings.wavWrongFormat(44100, 2))
        }
    }

    private fun assertDistinct(labels: List<String>) {
        for (label in labels) assertTrue(label.isNotBlank(), "blank label in $labels")
        assertEquals(labels.size, labels.toSet().size, "duplicate labels: $labels")
    }
}
