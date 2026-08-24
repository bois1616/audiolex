package de.hexenwoche.audiolex.core.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

/**
 * Language resolution (ADR-0015). The interesting part isn't the two
 * explicit choices -- it's what [UiLanguage.SYSTEM] does with the tags a
 * real device hands over, which is where a naive `== "de"` comparison would
 * quietly send every Austrian and Swiss device to English.
 */
class UiLanguageTest {

    @Test
    fun `an explicit choice ignores the device language`() {
        assertEquals(UiLanguage.DEUTSCH, UiLanguage.DEUTSCH.resolve(systemTag = "en"))
        assertEquals(UiLanguage.ENGLISCH, UiLanguage.ENGLISCH.resolve(systemTag = "de"))
    }

    @Test
    fun `SYSTEM follows a German device`() {
        assertEquals(UiLanguage.DEUTSCH, UiLanguage.SYSTEM.resolve(systemTag = "de"))
    }

    @Test
    fun `region and script subtags still count as German`() {
        for (tag in listOf("de-AT", "de_DE", "DE", "de-CH-1901", " de ")) {
            assertEquals(UiLanguage.DEUTSCH, UiLanguage.SYSTEM.resolve(systemTag = tag), "tag: $tag")
        }
    }

    @Test
    fun `an unknown or missing device language falls back to English`() {
        for (tag in listOf("en", "en-GB", "fr", "it-IT", "", "   ")) {
            assertEquals(UiLanguage.ENGLISCH, UiLanguage.SYSTEM.resolve(systemTag = tag), "tag: $tag")
        }
    }

    @Test
    fun `resolve never returns SYSTEM`() {
        for (language in UiLanguage.entries) {
            for (tag in listOf("de", "en", "xx", "")) {
                assertNotEquals(UiLanguage.SYSTEM, language.resolve(systemTag = tag))
            }
        }
    }

    @Test
    fun `stringsFor picks the catalog that resolve names`() {
        assertSame(GermanStrings, stringsFor(UiLanguage.DEUTSCH, systemTag = "en"))
        assertSame(EnglishStrings, stringsFor(UiLanguage.ENGLISCH, systemTag = "de"))
        assertSame(GermanStrings, stringsFor(UiLanguage.SYSTEM, systemTag = "de-AT"))
        assertSame(EnglishStrings, stringsFor(UiLanguage.SYSTEM, systemTag = "pt-BR"))
    }

    @Test
    fun `every selectable language has a name to show in the picker`() {
        val selectable = UiLanguage.entries - UiLanguage.SYSTEM

        assertEquals(listOf("Deutsch", "English"), selectable.map { it.nativeName })
    }
}
