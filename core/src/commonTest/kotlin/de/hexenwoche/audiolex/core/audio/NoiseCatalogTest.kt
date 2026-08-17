package de.hexenwoche.audiolex.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the bundled noise catalog parse ([parseNoiseCatalog], the
 * `files/noise/noise.json` half of the scenario catalog). The point of having
 * this in `:core` rather than inline in composeApp is exactly this test: the
 * bundled half can be proven to work without any Compose resource existing --
 * which matters right now, because the catalog ships empty until the author's
 * own recording is put in (`files/noise/README.md`).
 */
class NoiseCatalogTest {

    @Test
    fun `parses a catalog entry`() {
        val catalog = parseNoiseCatalog(
            """[{ "id": "bus", "label": "Bus", "fileRef": "bus.wav" }]""",
        )

        assertEquals(1, catalog.size)
        assertEquals(NoiseScenario(id = "bus", label = "Bus", fileRef = "bus.wav"), catalog.single())
    }

    @Test
    fun `parses several entries in file order`() {
        val catalog = parseNoiseCatalog(
            """
            [
              { "id": "bus", "label": "Bus", "fileRef": "bus.wav" },
              { "id": "cafe", "label": "Café", "fileRef": "cafe.wav" }
            ]
            """.trimIndent(),
        )

        // File order is load-bearing: the first entry is what an unknown or
        // empty stored id resolves to, so it is the effective default.
        assertEquals(listOf("bus", "cafe"), catalog.map { it.id })
    }

    @Test
    fun `an empty catalog is a valid state, not an error`() {
        assertTrue(parseNoiseCatalog("[]").isEmpty())
    }

    @Test
    fun `ignores unknown fields`() {
        // A hand-written noise.json may carry provenance or a comment key;
        // that must not make the entry disappear.
        val catalog = parseNoiseCatalog(
            """[{ "id": "bus", "label": "Bus", "fileRef": "bus.wav", "kommentar": "eigene Aufnahme" }]""",
        )

        assertEquals("bus", catalog.single().id)
    }

    @Test
    fun `broken json yields an empty catalog instead of throwing`() {
        for (broken in listOf("", "   ", "[", "{}", """[{ "id": "bus" }]""", "not json at all")) {
            assertTrue(parseNoiseCatalog(broken).isEmpty(), "should be empty for: $broken")
        }
    }
}
