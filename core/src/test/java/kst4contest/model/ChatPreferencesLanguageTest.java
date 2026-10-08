package kst4contest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The interface language is stored per profile, like every other GUI preference. */
class ChatPreferencesLanguageTest {

    @Test
    void theDefaultIsTheSystemLanguage() {
        // Empty means "follow the system", which is what the spec asks for on a first start.
        assertEquals("", new ChatPreferences().getGuiOptions_language());
    }

    @Test
    void aChosenLanguageIsKept() {
        ChatPreferences prefs = new ChatPreferences();

        prefs.setGuiOptions_language("de");

        assertEquals("de", prefs.getGuiOptions_language());
    }

    @Test
    void aNullSetterValueBecomesEmpty() {
        ChatPreferences prefs = new ChatPreferences();

        prefs.setGuiOptions_language(null);

        assertEquals("", prefs.getGuiOptions_language());
    }
}
