package de.hexenwoche.audiolex.core.i18n

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Sweeps every member of [Strings] across both catalogs and asserts that
 * something readable comes back (ADR-0015).
 *
 * A *missing* member is already a compile error -- that is the whole point
 * of the interface. What the compiler cannot catch is a member that exists
 * and returns nothing: an empty string literal, or a builder that drops its
 * argument on one branch. On a device that shows up as a blank label, which
 * is exactly the failure mode resource files are notorious for and this
 * design was chosen to avoid. So it gets a test rather than trust.
 *
 * Reflection is fine here because this is the jvm test source set, and it is
 * the only way to stay honest as the interface grows: a hand-written list of
 * members would fall behind the day someone adds one.
 */
class StringsCompletenessTest {

    @Test
    fun `every member of every catalog yields a non-blank string`() {
        val members = Strings::class.java.declaredMethods.sortedBy { it.name }
        assertTrue(members.size > 100, "suspiciously few members found: ${members.size}")

        for (catalog in listOf<Strings>(GermanStrings, EnglishStrings)) {
            for (member in members) {
                val arguments = member.parameterTypes.map { type ->
                    sampleFor(type)
                        ?: fail("${member.name}: no sample value for parameter type ${type.name}")
                }
                val value = member.invoke(catalog, *arguments.toTypedArray())

                assertTrue(
                    value is String && value.isNotBlank(),
                    "${catalog::class.simpleName}.${member.name} returned <$value>",
                )
            }
        }
    }

    /**
     * A usable argument per parameter type the interface actually uses.
     * Returns null for anything unknown, which fails the test loudly rather
     * than skipping the member -- a new parameter type should force a
     * decision here, not silently drop out of the sweep.
     */
    private fun sampleFor(type: Class<*>): Any? = when {
        type == Int::class.javaPrimitiveType -> 2
        type == String::class.java -> "Beispiel"
        type == List::class.java -> listOf("a", "b")
        type.isEnum -> type.enumConstants.first()
        else -> null
    }
}
