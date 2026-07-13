package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.corpus.Word
import de.hexenwoche.audiolex.core.corpus.WordCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningSessionTest {

    private fun word(id: String) = Word(id, id, "de-DE", 1, WordCategory.EVERYDAY)

    private val threeWords = listOf(word("a"), word("b"), word("c"))

    @Test
    fun startsAtFirstWordWithNoRepeats() {
        val session = LearningSession(threeWords)
        assertEquals("a", session.currentWord.id)
        assertEquals(1, session.progress)
        assertEquals(3, session.total)
        assertEquals(0, session.repeatCount)
    }

    @Test
    fun repeatKeepsSameWordAndIncrementsCounter() {
        val session = LearningSession(threeWords).repeat().repeat()
        assertEquals("a", session.currentWord.id)
        assertEquals(2, session.repeatCount)
    }

    @Test
    fun advanceMovesToNextWordAndResetsRepeatCount() {
        val session = LearningSession(threeWords).repeat().advance()
        assertEquals("b", session?.currentWord?.id)
        assertEquals(0, session?.repeatCount)
        assertEquals(2, session?.progress)
    }

    @Test
    fun advancePastLastWordReturnsNull() {
        var session: LearningSession? = LearningSession(threeWords)
        repeat(2) { session = session?.advance() }
        assertTrue(session?.isLastWord == true)
        assertNull(session?.advance())
    }

    @Test
    fun rejectsEmptyWordList() {
        assertFailsWith<IllegalArgumentException> { LearningSession(emptyList()) }
    }

    @Test
    fun backOnFirstWordReturnsNull() {
        val session = LearningSession(threeWords)
        assertTrue(session.isFirstWord)
        assertNull(session.back())
    }

    @Test
    fun backMovesToPreviousWordAndResetsRepeatCount() {
        val session = LearningSession(threeWords, currentIndex = 2).repeat().back()
        assertEquals("b", session?.currentWord?.id)
        assertEquals(0, session?.repeatCount)
        assertEquals(2, session?.progress)
    }

    @Test
    fun advanceThenBackReturnsToOriginalWord() {
        val session = LearningSession(threeWords).advance()?.back()
        assertEquals("a", session?.currentWord?.id)
        assertTrue(session?.isFirstWord == true)
    }
}
