package de.hexenwoche.audiolex.core.corpus

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class WordSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `old format without kind deserializes as WORD`() {
        val oldJson = """
            { "id": "ball", "text": "Ball", "language": "de-DE",
              "syllableCount": 1, "category": "EVERYDAY" }
        """.trimIndent()

        val word = json.decodeFromString<Word>(oldJson)

        assertEquals(EntryKind.WORD, word.kind)
        assertEquals("ball", word.id)
        assertEquals(null, word.phoneticGroup)
    }

    @Test
    fun `new format with kind SENTENCE deserializes as SENTENCE`() {
        val newJson = """
            { "id": "satz-morgen", "text": "Am Morgen steht ein Bagger vor dem Haus.",
              "language": "de-DE", "syllableCount": 10, "category": "EVERYDAY",
              "kind": "SENTENCE" }
        """.trimIndent()

        val word = json.decodeFromString<Word>(newJson)

        assertEquals(EntryKind.SENTENCE, word.kind)
        assertEquals("satz-morgen", word.id)
    }

    @Test
    fun `roundtrip preserves kind`() {
        val sentence = Word(
            id = "satz-schiff",
            text = "Am Himmel taucht plötzlich ein Schiff auf.",
            language = "de-DE",
            syllableCount = 9,
            category = WordCategory.EVERYDAY,
            kind = EntryKind.SENTENCE,
        )

        val decoded = json.decodeFromString<Word>(json.encodeToString(Word.serializer(), sentence))

        assertEquals(sentence, decoded)
        assertEquals(EntryKind.SENTENCE, decoded.kind)
    }
}
