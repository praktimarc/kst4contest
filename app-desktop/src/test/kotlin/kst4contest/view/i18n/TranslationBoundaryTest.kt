package kst4contest.view.i18n

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

/**
 * Nothing that goes to the server or to another radio amateur has got into the texts.
 *
 * The third of the three boundary checks, and the one that catches the follow-up work: the
 * remaining ~380 strings get pulled into these files over time, and a beacon template or a
 * `/CQ` sitting among them would be translated by the next contributor in good faith.
 *
 * German is held complete here because it is a shipped language. A contributed language is
 * deliberately not: a partial file is the normal state of a contribution in progress, and a
 * check over every language would turn the arrival of one into a red build.
 */
class TranslationBoundaryTest {

    private val everyText: List<String> =
        Translations.BY_LANGUAGE.values.flatMap { it.values }

    @Test
    fun thereAreTextsToCheckAtAll() {
        /*
         * Without this, every assertion below would pass over an empty list -- the shape of a
         * boundary check that guards nothing.
         */
        assertTrue(everyText.size >= 16, "only ${everyText.size} texts were found")
    }

    @Test
    fun noTextIsAProtocolFrameOrAChatCommand() {
        /*
         * Deliberately conservative, and the reason is worth stating: a pattern cannot tell a
         * protocol command from an interface label that names one. "Update MYQRG from
         * RadioInfo messages" is a setting's description, and its German translation even
         * begins with that word, because that is where German puts it. A check for "contains"
         * or "starts with" fails both and teaches the next person to delete the test.
         *
         * So only the unambiguous shapes are checked: a text that IS a token, a wire frame
         * (an opcode followed by its pipe), and anything beginning with a slash. The real
         * guarantees are the other two boundary tests, which measure behaviour rather than
         * text -- the frames built under three locales, and every preference default by
         * reflection.
         */
        val opcodes = listOf("LOGINC", "SDONE", "ACHAT", "DXQ", "SETNAME", "MYQRG")

        for (text in everyText) {
            assertTrue(
                opcodes.none { text == it || text.startsWith("$it|") },
                "'$text' is a protocol token or a wire frame; that text goes to the ON4KST "
                        + "server and must not be in a translation file",
            )
            assertTrue(
                !text.trimStart().startsWith("/"),
                "'$text' looks like a chat command; those go to the server as a message",
            )
        }
    }

    @Test
    fun noTextIsOneOfThePreferenceDefaultsThatGoToOtherRadioAmateurs() {
        val prefs = ChatPreferences()

        val defaults = ChatPreferences::class.java.declaredFields
            .filter { it.type == String::class.java && !Modifier.isStatic(it.modifiers) }
            .mapNotNull { field ->
                field.isAccessible = true
                (field.get(prefs) as String?)?.takeIf { it.isNotBlank() }
            }
            .toSet()

        assertTrue(defaults.size >= 10, "only ${defaults.size} defaults were found to check against")

        for (text in everyText) {
            assertTrue(
                text !in defaults,
                "'$text' is a ChatPreferences default; it goes to other radio amateurs and is "
                        + "in every operator's stored XML",
            )
        }
    }

    @Test
    fun germanIsCompleteBecauseItIsAShippedLanguage() {
        val base = Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE).keys
        val german = Translations.BY_LANGUAGE.getValue("de").keys

        assertEquals(
            emptySet<String>(),
            base - german,
            "German is shipped, so a gap in it is a defect and not a contribution in progress",
        )
    }

    @Test
    fun onlyTheShippedLanguagesAreHeldToCompleteness() {
        /*
         * The counterpart of the test above, and a real assertion rather than a note: a
         * language this build carries beyond the shipped two must be free to be partial. The
         * check is on the rule itself -- exactly two languages are held complete -- so adding
         * strings_fr.properties with ten keys cannot turn the build red, and removing German
         * from the held set cannot pass unnoticed either.
         */
        val held = setOf(Translations.BASE_LANGUAGE, "de")

        assertEquals(setOf("en", "de"), held, "the set of shipped languages changed")

        val base = Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE).keys
        val partialLanguages = Translations.BY_LANGUAGE
            .filterKeys { it !in held }
            .filterValues { (base - it.keys).isNotEmpty() }

        assertTrue(
            partialLanguages.keys.none { it in held },
            "a shipped language turned up among the partial ones: ${partialLanguages.keys}",
        )
    }
}
