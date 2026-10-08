package kst4contest.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The rules that decide whether a set of translations is usable.
 *
 * Pure functions, and therefore the part that can be wrong without anyone noticing: a build
 * break that never breaks looks exactly like a clean project. Every rule here has a failure
 * that reaches an operator -- a lost translation, a swallowed argument, or Kotlin that does
 * not compile with a message a contributor cannot read.
 */
class TranslationRulesTest {

    @Test
    fun whatTheBaseHasAndATranslationLacksIsMissing() {
        assertEquals(
            setOf("b"),
            missingKeys(base = setOf("a", "b"), other = setOf("a")),
        )
    }

    @Test
    fun whatATranslationHasAndTheBaseLacksIsAnOrphan() {
        // A contributor's typo. Silently keeping it would mean a translation nothing uses.
        assertEquals(
            setOf("typo"),
            orphanKeys(base = setOf("a"), other = setOf("a", "typo")),
        )
    }

    @Test
    fun aTextWithoutPlaceholdersNeedsNoArguments() {
        assertEquals(0, argumentCount("Save settings"))
        assertEquals(0, argumentCount(""))
    }

    @Test
    fun theArgumentCountIsTheHighestIndexPlusOneAndNotTheNumberOfOccurrences() {
        /*
         * The trap. "{1} {1}" occurs twice and needs two arguments, because MessageFormat
         * addresses them by index; "{0} {0}" occurs twice and needs one. Counting occurrences
         * makes the first case throw and the second swallow an argument.
         */
        assertEquals(1, argumentCount("Connect to {0}"))
        assertEquals(1, argumentCount("{0} and {0} again"))
        assertEquals(2, argumentCount("{1} {1}"))
        assertEquals(3, argumentCount("{2} {0}"))
    }

    @Test
    fun aFormattedPlaceholderStillCounts() {
        // MessageFormat allows {0,number} and {1,date,short}; both are arguments.
        assertEquals(1, argumentCount("Seen {0,number} times"))
        assertEquals(2, argumentCount("{0} at {1,date,short}"))
    }

    @Test
    fun somethingThatMerelyLooksLikeAPlaceholderIsNotOne() {
        // Braces appear in real text. Only a leading digit makes it an argument.
        assertEquals(0, argumentCount("Press {Ctrl} to stop"))
        assertEquals(0, argumentCount("100 {}"))
    }

    @Test
    fun aDottedKeyBecomesALowerCamelCaseIdentifier() {
        assertEquals("settingsSave", identifierFor("settings.save"))
        assertEquals("menuFile", identifierFor("menu.file"))
        assertEquals("guiBandColumnHints", identifierFor("gui.bandColumnHints"))
    }

    @Test
    fun aKeyWithUnderscoresOrDashesBecomesOneIdentifierToo() {
        assertEquals("guiStartupDesign", identifierFor("gui.startup_design"))
        assertEquals("guiStartupDesign", identifierFor("gui.startup-design"))
    }

    @Test
    fun anIdentifierNeverStartsWithADigit() {
        // A key may legitimately start with a number; a Kotlin identifier may not.
        assertTrue(identifierFor("2.metres").first().isLetter())
    }

    @Test
    fun twoKeysCollapsingToOneIdentifierAreReported() {
        /*
         * gui.startupDesign and gui.startup_design both become guiStartupDesign. Generating
         * both would produce Kotlin that does not compile, and the compiler's message names
         * the generated file -- which tells a contributor nothing about their properties file.
         */
        val collisions = identifierCollisions(listOf("gui.startupDesign", "gui.startup_design"))

        assertEquals(1, collisions.size)
        assertEquals(
            listOf("gui.startupDesign", "gui.startup_design"),
            collisions.getValue("guiStartupDesign").sorted(),
        )
    }

    @Test
    fun distinctKeysReportNoCollision() {
        assertTrue(identifierCollisions(listOf("a.b", "c.d")).isEmpty())
    }

    @Test
    fun aKeyThatWouldNotFormALegalIdentifierIsRejected() {
        /*
         * identifierFor strips only . _ -, so every other character a Properties file can
         * carry in a key -- a space, a brace, a quote, a newline decoded from a \uXXXX escape
         * -- would reach identifier position verbatim. Emitted there it does not merely fail
         * to compile: it breaks out of `val <name>: String get() = ...` into whatever the key
         * says, which is Kotlin that then runs. So the generated name is held to the shape of
         * an identifier before it is ever written.
         */
        val illegal = illegalIdentifierKeys(
            listOf("settings.save", "a b", "x\": String get() = \"\"", "init { pwn() }\n    val")
        )

        assertEquals(
            listOf("a b", "init { pwn() }\n    val", "x\": String get() = \"\""),
            illegal.sorted(),
        )
    }

    @Test
    fun ordinaryKeysFormLegalIdentifiersAndAreNotRejected() {
        // A digit-leading key is fine: identifierFor prefixes it, which is a letter again.
        assertTrue(
            illegalIdentifierKeys(listOf("settings.save", "gui.startup_design", "2.metres")).isEmpty()
        )
    }
}
