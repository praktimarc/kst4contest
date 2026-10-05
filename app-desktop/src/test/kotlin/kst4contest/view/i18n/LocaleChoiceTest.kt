package kst4contest.view.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Which language a stored setting and a system default add up to.
 *
 * A pure function on purpose: the alternative is a test that changes the JVM's default locale,
 * which is global state and leaks into whatever runs next.
 */
class LocaleChoiceTest {

    @Test
    fun anEmptySettingFollowsTheSystem() {
        assertEquals("de", languageFor(SYSTEM_LANGUAGE, Locale.GERMANY))
        assertEquals("en", languageFor(SYSTEM_LANGUAGE, Locale.UK))
    }

    @Test
    fun aSystemLanguageNobodyTranslatedFallsBackToEnglish() {
        // The spec's rule: default from the system language, falling back to English.
        assertEquals("en", languageFor(SYSTEM_LANGUAGE, Locale.FRANCE))
        assertEquals("en", languageFor(SYSTEM_LANGUAGE, Locale.JAPAN))
    }

    @Test
    fun anExplicitChoiceBeatsTheSystem() {
        assertEquals("de", languageFor("de", Locale.UK))
        assertEquals("en", languageFor("en", Locale.GERMANY))
    }

    @Test
    fun nonsenseInTheSettingFallsBackToEnglishRatherThanFailing() {
        /*
         * The stored value can be hand-edited in the preferences XML, or left over from a
         * build that carried a language this one does not.
         */
        assertEquals("en", languageFor("klingon", Locale.GERMANY))
        assertEquals("en", languageFor("de_DE_x", Locale.GERMANY))
        assertEquals("en", languageFor(null, Locale.UK))
    }

    @Test
    fun theSettingIsCaseInsensitiveBecauseAHandEditedFileMayShout() {
        assertEquals("de", languageFor("DE", Locale.UK))
    }

    @Test
    fun surroundingSpaceInAHandEditedSettingIsIgnored() {
        assertEquals("de", languageFor("  de  ", Locale.UK))
    }
}
