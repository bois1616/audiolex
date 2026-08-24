package de.hexenwoche.audiolex.core.corpus

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The corpus language drawer (ADR-0016). The filter matters more than it
 * looks: entries that slip through it don't merely show up in a list, they
 * get seeded as SRS cards -- so an English sentence leaking into a German
 * round would sit in the author's real deck until he deletes it by hand.
 */
class CorpusLanguageTest {

    private fun word(id: String, language: String, kind: EntryKind = EntryKind.WORD) = Word(
        id = id,
        text = id,
        language = language,
        syllableCount = 1,
        category = WordCategory.EVERYDAY,
        kind = kind,
    )

    private fun recording(wordId: String, voiceId: String) = AudioRecording(
        id = "${wordId}__$voiceId",
        wordId = wordId,
        voiceId = voiceId,
        locale = "de-DE",
        fileRef = "raw/$wordId.wav",
    )

    private fun ownEntry(id: String, language: String, speaker: String = "Andy") = OwnEntry(
        id = id,
        text = id,
        speaker = speaker,
        language = language,
        fileName = "$id.wav",
        createdAtEpochMillis = 0,
    )

    @Test
    fun `region and script subtags file under the same drawer`() {
        for (tag in listOf("de", "de-DE", "de_AT", "DE", "de-CH-1901", " de ")) {
            assertTrue(CorpusLanguage.DEUTSCH.matches(tag), "tag: $tag")
            assertFalse(CorpusLanguage.ENGLISCH.matches(tag), "tag: $tag")
        }
        for (tag in listOf("en", "en-US", "en_GB", "EN")) {
            assertTrue(CorpusLanguage.ENGLISCH.matches(tag), "tag: $tag")
            assertFalse(CorpusLanguage.DEUTSCH.matches(tag), "tag: $tag")
        }
    }

    @Test
    fun `a language the app has no drawer for matches nothing`() {
        // Deliberately not a catch-all: the author's own framing is that the
        // app cannot know what is in a recording, so it must not pretend a
        // Chinese entry belongs in the German drawer.
        for (tag in listOf("zh-CN", "it-IT", "", "   ")) {
            assertFalse(CorpusLanguage.DEUTSCH.matches(tag), "tag: $tag")
            assertFalse(CorpusLanguage.ENGLISCH.matches(tag), "tag: $tag")
        }
    }

    @Test
    fun `the language filter drops other drawers with their recordings`() {
        val words = listOf(word("haus", "de-DE"), word("house", "en-US"))
        val recordings = listOf(recording("haus", "thorsten"), recording("house", "ljspeech"))

        val german = mergeCorpus(words, recordings, emptyList(), language = CorpusLanguage.DEUTSCH)

        assertContentEquals(listOf("haus"), german.words.map { it.id })
        // The recording has to go too -- a stray recording would still be
        // counted by speakerContingents and would show "ljspeech" as a
        // German speaker.
        assertContentEquals(listOf("haus"), german.recordings.map { it.wordId })
    }

    @Test
    fun `a null language keeps every drawer`() {
        val words = listOf(word("haus", "de-DE"), word("house", "en-US"))
        val recordings = listOf(recording("haus", "thorsten"), recording("house", "ljspeech"))

        val all = mergeCorpus(words, recordings, emptyList(), language = null)

        assertEquals(2, all.words.size)
        assertEquals(2, all.recordings.size)
    }

    @Test
    fun `own entries are filed by what the speaker declared, not by their content`() {
        // The whole point of ADR-0016: Andy says "German", so it is German,
        // whatever he actually said into the microphone.
        val own = listOf(ownEntry("own-1", "de-DE"), ownEntry("own-2", "en-US"))

        val german = mergeCorpus(emptyList(), emptyList(), own, language = CorpusLanguage.DEUTSCH)
        val english = mergeCorpus(emptyList(), emptyList(), own, language = CorpusLanguage.ENGLISCH)

        assertContentEquals(listOf("own-1"), german.words.map { it.id })
        assertContentEquals(listOf("own-2"), english.words.map { it.id })
    }

    @Test
    fun `an own entry written before the field existed counts as German`() {
        val legacy = OwnEntry(id = "own-old", text = "Haus", fileName = "own-old.wav", createdAtEpochMillis = 0)

        assertEquals("de-DE", legacy.language)
        assertContentEquals(
            listOf("own-old"),
            mergeCorpus(emptyList(), emptyList(), listOf(legacy), language = CorpusLanguage.DEUTSCH)
                .words.map { it.id },
        )
    }

    @Test
    fun `language and speaker filters combine without either swallowing the other`() {
        val own = listOf(
            ownEntry("own-de-andy", "de-DE", speaker = "Andy"),
            ownEntry("own-de-grete", "de-DE", speaker = "Grete"),
            ownEntry("own-en-andy", "en-US", speaker = "Andy"),
        )

        val german = mergeCorpus(
            emptyList(), emptyList(), own,
            excludedSpeakers = setOf("Grete"),
            language = CorpusLanguage.DEUTSCH,
        )

        assertContentEquals(listOf("own-de-andy"), german.words.map { it.id })
    }

    @Test
    fun `contingents are counted within the selected drawer`() {
        val own = listOf(
            ownEntry("own-de", "de-DE", speaker = "Andy"),
            ownEntry("own-en-1", "en-US", speaker = "Andy"),
            ownEntry("own-en-2", "en-US", speaker = "Andy"),
        )

        val germanContingents = mergeCorpus(emptyList(), emptyList(), own, language = CorpusLanguage.DEUTSCH)
            .speakerContingents()
        val englishContingents = mergeCorpus(emptyList(), emptyList(), own, language = CorpusLanguage.ENGLISCH)
            .speakerContingents()

        assertEquals(listOf(SpeakerContingent("Andy", wordCount = 1, sentenceCount = 0)), germanContingents)
        assertEquals(listOf(SpeakerContingent("Andy", wordCount = 2, sentenceCount = 0)), englishContingents)
    }
}
