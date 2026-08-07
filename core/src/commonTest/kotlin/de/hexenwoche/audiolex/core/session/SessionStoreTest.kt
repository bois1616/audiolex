package de.hexenwoche.audiolex.core.session

import de.hexenwoche.audiolex.core.srs.ReviewRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The session history's backup format and merge rule (ADR-0013 Nachtrag
 * 2026-08-07). The history is irreplaceable in a way the SRS state is not --
 * it records what happened, and nothing regenerates it.
 */
class SessionStoreTest {

    private fun session(startedAt: Long, rated: Int = 3) = Session(
        startedAtEpochMillis = startedAt,
        zoneId = "Europe/Vienna",
        mode = "PRUEFMODUS",
        ratedCount = rated,
        ratingCounts = mapOf(
            ReviewRating.AGAIN to 1,
            ReviewRating.GOOD to 1,
            ReviewRating.PERFECT to 1,
        ),
    )

    @Test
    fun `roundtrip preserves every field including the rating counts`() {
        val sessions = listOf(session(1_000L), session(2_000L, rated = 7))

        assertEquals(sessions, parseSessionsOrNull(encodeSessions(sessions)))
    }

    @Test
    fun `damaged history reports unreadable instead of an empty list`() {
        // Same distinction as parseOwnCorpusOrNull: "0 Sitzungen übernommen"
        // for a broken file would claim the history had been empty.
        assertNull(parseSessionsOrNull("""[ { "startedAtEpochMillis": 1, "zoneI"""))
        assertNull(parseSessionsOrNull("""{ "not": "a list" }"""))
    }

    @Test
    fun `an empty history is not the same as a damaged one`() {
        assertEquals(emptyList(), parseSessionsOrNull("[]"))
    }

    @Test
    fun `an unknown rating key does not make an older backup unreadable`() {
        // Forward compatibility: a ReviewRating added later must not turn
        // today's archives into garbage on an older build, and vice versa.
        val withExtra = """
            [ { "startedAtEpochMillis": 1, "zoneId": "Europe/Vienna", "mode": "PRUEFMODUS",
                "ratedCount": 1, "ratingCounts": { "GOOD": 1 }, "futureField": 42 } ]
        """.trimIndent()

        val parsed = parseSessionsOrNull(withExtra)

        assertEquals(1, parsed?.size)
        assertEquals(1, parsed?.single()?.ratingCounts?.get(ReviewRating.GOOD))
    }

    @Test
    fun `merge adds unknown start times and skips known ones`() {
        val existing = listOf(session(1_000L), session(2_000L))
        val fromBackup = listOf(session(1_000L), session(3_000L), session(4_000L))

        val toAdd = sessionsToAdd(fromBackup, existing)

        assertEquals(listOf(3_000L, 4_000L), toAdd.map { it.startedAtEpochMillis })
    }

    @Test
    fun `merging a backup into the device it came from adds nothing`() {
        val sessions = listOf(session(1_000L), session(2_000L))

        assertTrue(sessionsToAdd(sessions, sessions).isEmpty())
    }

    @Test
    fun `identity is the start time, not the Room id`() {
        // SessionEntity.id is autoGenerate and device-local: the same number
        // means different sessions on two devices. Two Session values that
        // differ in everything but the start time must still count as known.
        val existing = listOf(session(1_000L, rated = 3))
        val sameMomentDifferentCounts = listOf(session(1_000L, rated = 99))

        assertTrue(sessionsToAdd(sameMomentDifferentCounts, existing).isEmpty())
    }
}
