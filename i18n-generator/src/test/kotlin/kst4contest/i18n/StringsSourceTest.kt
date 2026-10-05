package kst4contest.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Turning translations into Kotlin.
 *
 * The dangerous part is the escaping. This writes Kotlin source, and in Kotlin `$` opens a
 * template: a translation containing one produces either source that does not compile or --
 * worse -- source that means something else. A translator has no reason to know that.
 */
class StringsSourceTest {

    private fun base(vararg pairs: Pair<String, String>) = Translation("en", mapOf(*pairs))

    @Test
    fun aPlainKeyBecomesAProperty() {
        val source = stringsSource(base("settings.save" to "Save settings"), emptyList())

        assertTrue(
            source.contains("val settingsSave: String get() = lookup(\"settings.save\")"),
            source,
        )
    }

    @Test
    fun aKeyWithAPlaceholderBecomesAFunctionWithThatManyArguments() {
        val source = stringsSource(base("c.to" to "Connect to {0}"), emptyList())

        assertTrue(source.contains("fun cTo(arg0: Any)"), source)
        assertFalse(source.contains("val cTo"), "a parameterised key became a property")
    }

    @Test
    fun theArityFollowsTheHighestIndexAndNotTheOccurrences() {
        val source = stringsSource(base("x" to "{1} {1}"), emptyList())

        assertTrue(source.contains("fun x(arg0: Any, arg1: Any)"), source)
    }

    @Test
    fun theBaseTextsAreEmittedAsAMap() {
        val source = stringsSource(base("a" to "one"), emptyList())

        assertTrue(source.contains("\"a\" to \"one\""), source)
    }

    @Test
    fun aTranslationIsEmittedWithOnlyTheKeysItHas() {
        /*
         * Incompleteness is allowed, so a translation carries only its own keys and the
         * lookup falls through to the base. Padding it with English would make a missing
         * translation indistinguishable from a deliberate English term.
         */
        val source = stringsSource(
            base("a" to "one", "b" to "two"),
            listOf(Translation("de", mapOf("a" to "eins"))),
        )

        assertTrue(source.contains("\"a\" to \"eins\""), source)
        assertFalse(source.contains("\"b\" to \"zwei\""), "de gained a key it does not have")

        // b belongs to the base table and to nothing else.
        assertEquals(1, source.split("\"b\" to ").size - 1, "b appears more than once")
    }

    // ------------------------------------------------------------------ escaping

    @Test
    fun aDollarSignIsEscapedBecauseKotlinWouldReadItAsATemplate() {
        assertEquals("\"5 \\\$ only\"", kotlinLiteral("5 \$ only"))
    }

    @Test
    fun quotesAndBackslashesAreEscaped() {
        assertEquals("\"say \\\"hi\\\"\"", kotlinLiteral("say \"hi\""))
        assertEquals("\"a\\\\b\"", kotlinLiteral("a\\b"))
    }

    @Test
    fun newlinesAndTabsBecomeEscapesRatherThanBreakingTheLiteral() {
        assertEquals("\"a\\nb\"", kotlinLiteral("a\nb"))
        assertEquals("\"a\\tb\"", kotlinLiteral("a\tb"))
    }

    @Test
    fun anUmlautStaysItselfBecauseTheGeneratedFileIsUtf8() {
        assertEquals("\"Grüße\"", kotlinLiteral("Grüße"))
    }

    @Test
    fun aTranslationFullOfHazardsSurvivesIntoTheSource() {
        // The end-to-end version of the escaping tests above.
        val source = stringsSource(base("x" to "a \$b \"c\" d\\e"), emptyList())

        assertTrue(source.contains("\\\$b"), source)
        assertTrue(source.contains("\\\"c\\\""), source)
        assertTrue(source.contains("d\\\\e"), source)
    }

    @Test
    fun aTextContainingACommentEndDoesNotBreakTheKdoc() {
        /*
         * The texts are echoed into a KDoc line so the call site shows what they say. A text
         * containing the comment terminator would end the comment early and leave the rest as
         * stray source.
         */
        val source = stringsSource(base("x" to "ends a comment */ here"), emptyList())

        /*
         * The KDoc line only. Inside the string literal in the table below, a comment
         * terminator is ordinary text and perfectly valid Kotlin -- asserting over the whole
         * file would be asserting that a harmless thing does not happen.
         *
         * Writing this comment is itself the demonstration: the first version quoted the
         * terminator and ended this comment three lines early.
         */
        val kdoc = source.lines().single { it.trimStart().startsWith("/** ends a comment") }

        assertTrue(kdoc.trimEnd().endsWith("*/"), kdoc)
        assertEquals(1, kdoc.split("*/").size - 1, "the KDoc line terminates twice: $kdoc")
    }
}
