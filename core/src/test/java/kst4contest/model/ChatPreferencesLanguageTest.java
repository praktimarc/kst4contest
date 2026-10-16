package kst4contest.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;

/** The interface language is stored per profile, like every other GUI preference. */
class ChatPreferencesLanguageTest {

    @TempDir
    Path tempDir;

    @Test
    void theDefaultIsTheSystemLanguage() {
        // Empty means "follow the system", which is what the spec asks for on a first start.
        assertEquals("", new ChatPreferences(tempDir.resolve("preferences.xml")).getGuiOptions_language());
    }

    @Test
    void aChosenLanguageIsKept() {
        ChatPreferences prefs = new ChatPreferences(tempDir.resolve("preferences.xml"));

        prefs.setGuiOptions_language("de");

        assertEquals("de", prefs.getGuiOptions_language());
    }

    @Test
    void aNullSetterValueBecomesEmpty() {
        ChatPreferences prefs = new ChatPreferences(tempDir.resolve("preferences.xml"));

        prefs.setGuiOptions_language(null);

        assertEquals("", prefs.getGuiOptions_language());
    }
}
