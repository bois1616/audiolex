package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.corpus.Word

/**
 * Pure progress tracking for Lernmodus (Konzept 3.1, Szenario S1/S2):
 * a linear walk through [words] where the current word can be repeated any
 * number of times before advancing or going back. No audio, no persistence
 * -- those are the caller's job (session/AudioSink, resp. M3's SRS
 * persistence). [words] is walked in whatever order the caller passes in;
 * shuffling for a mixed-but-fixed-per-session order (Autor-Requirement
 * 2026-07-12) is the caller's job (e.g. `words.shuffled()` once at session
 * start), not this class's -- it stays a deterministic linear walk either way.
 *
 * Leaving mid-list ends the session; there is no pause/resume state
 * (Szenario S5, decided 2026-07-08) -- the caller simply stops holding a
 * reference to it and starts a fresh [LearningSession] next time.
 */
data class LearningSession(
    val words: List<Word>,
    val currentIndex: Int = 0,
    val repeatCount: Int = 0,
) {
    init {
        require(words.isNotEmpty()) { "a session needs at least one word" }
        require(currentIndex in words.indices) { "currentIndex out of bounds" }
    }

    val currentWord: Word get() = words[currentIndex]
    val progress: Int get() = currentIndex + 1
    val total: Int get() = words.size
    val isLastWord: Boolean get() = currentIndex == words.lastIndex
    val isFirstWord: Boolean get() = currentIndex == 0

    /** Repeats the current word (Szenario S2): playback is the caller's job. */
    fun repeat(): LearningSession = copy(repeatCount = repeatCount + 1)

    /** Advances to the next word, resetting the repeat counter. Null once the list is exhausted. */
    fun advance(): LearningSession? =
        if (isLastWord) null else copy(currentIndex = currentIndex + 1, repeatCount = 0)

    /**
     * Moves one word back in the session's (possibly shuffled) order,
     * resetting the repeat counter, same as [advance]. Null on the first
     * word -- there is nothing before it to go back to (Autor-Requirement
     * 2026-07-12: "Vorheriges" steps back through the order actually seen
     * this session, not a random jump).
     */
    fun back(): LearningSession? =
        if (isFirstWord) null else copy(currentIndex = currentIndex - 1, repeatCount = 0)
}
