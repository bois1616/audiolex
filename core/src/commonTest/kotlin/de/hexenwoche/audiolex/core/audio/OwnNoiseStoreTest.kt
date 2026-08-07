package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Encode/parse rules of the own-noise metadata file (Backlog M4 "Eigene
 * Störgeräusche", AC1), mirroring `OwnCorpusStoreTest`: roundtrip, the
 * missing-file case and the defensive posture towards damaged content.
 */
class OwnNoiseStoreTest {

    @Test
    fun `roundtrip preserves all fields`() {
        val noises = listOf(
            OwnNoise(
                id = "noise-1000-1",
                label = "Straßencafé",
                fileName = "noise-1000-1.wav",
                createdAtEpochMillis = 1000L,
                source = OwnNoiseSource.AUFNAHME,
            ),
            OwnNoise(
                id = "noise-2000-2",
                label = "Regen am Fenster",
                fileName = "noise-2000-2.wav",
                createdAtEpochMillis = 2000L,
                source = OwnNoiseSource.IMPORT,
            ),
        )

        val decoded = parseOwnNoises(encodeOwnNoises(noises))

        assertEquals(noises, decoded)
    }

    @Test
    fun `missing file yields an empty collection`() {
        assertEquals(emptyList(), parseOwnNoises(null))
    }

    @Test
    fun `unknown JSON field is ignored`() {
        val withExtraField = """
            [ { "id": "noise-1", "label": "Baustelle", "fileName": "noise-1.wav",
                "createdAtEpochMillis": 42, "source": "AUFNAHME",
                "futureField": "irrelevant" } ]
        """.trimIndent()

        val noises = parseOwnNoises(withExtraField)

        assertEquals(listOf("noise-1"), noises.map { it.id })
    }

    @Test
    fun `corrupt JSON yields a quiet empty collection instead of throwing`() {
        val truncated = """[ { "id": "noise-1", "label": "Baustelle", "fileNa"""

        val noises = parseOwnNoises(truncated)

        assertTrue(noises.isEmpty())
    }

    @Test
    fun `valid JSON that is not an array of noises also yields an empty collection`() {
        val wrongShape = """{ "not": "a list" }"""

        val noises = parseOwnNoises(wrongShape)

        assertTrue(noises.isEmpty())
    }

    @Test
    fun `missing required field yields an empty collection, not a crash`() {
        // fileName is required and absent here -- decoding the whole array fails.
        val missingFileName = """[ { "id": "noise-1", "label": "Baustelle", "createdAtEpochMillis": 1 } ]"""

        val noises = parseOwnNoises(missingFileName)

        assertTrue(noises.isEmpty())
    }

    @Test
    fun `source defaults to AUFNAHME when omitted`() {
        val minimal = """[ { "id": "noise-1", "label": "Baustelle", "fileName": "noise-1.wav", "createdAtEpochMillis": 1 } ]"""

        val noises = parseOwnNoises(minimal)

        assertEquals(OwnNoiseSource.AUFNAHME, noises.single().source)
    }

    @Test
    fun `strict parse tells damaged apart from empty`() {
        // The backup reader needs this distinction (AC5): a damaged file must
        // not come through as "no noises".
        assertNull(parseOwnNoisesOrNull("""[ { "id": "noise-1", "labe"""))
        assertEquals(emptyList(), parseOwnNoisesOrNull("[]"))
        assertEquals(listOf("noise-1"), parseOwnNoisesOrNull(encodeOwnNoises(listOf(
            OwnNoise("noise-1", "Baustelle", "noise-1.wav", 1L),
        )))?.map { it.id })
    }
}
