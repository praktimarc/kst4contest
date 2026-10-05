package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.i18n.LanguageStore
import kst4contest.view.i18n.LanguageStoreFactory
import kst4contest.view.i18n.SYSTEM_LANGUAGE
import org.junit.jupiter.api.Test
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class GuiOptionsTabStateTest {

    /*
     * Over lambdas rather than over the preferences, so switching the language in a test does
     * not write into the operator's home directory. What this file is about is the GUI tab's
     * booleans; the language is covered by LanguageStoreTest.
     */
    private var storedLanguage = SYSTEM_LANGUAGE

    private fun languageStore() = LanguageStore(
        storedLanguage = { storedLanguage },
        storeLanguage = { storedLanguage = it },
        systemDefault = Locale.UK,
    )

    @Test
    fun theLanguageIsWrittenThroughTheStoreAndReachesThePreference() {
        /*
         * Over the real factory rather than the lambda store above, because what this asserts
         * is the wiring: setting the language on the tab state has to land in the preference
         * that gets saved, and it has to do so through the store, which is what repaints the
         * open windows. A store wired to anything else would pass a weaker test.
         */
        val prefs = ChatPreferences()
        val state = GuiOptionsTabState(prefs, LanguageStoreFactory.create(prefs, Locale.UK))

        state.language = "de"

        assertEquals("de", state.language)
        assertEquals("de", prefs.guiOptions_language)
    }


    @Test
    fun `reading comes straight from the preferences`() {
        val prefs = ChatPreferences()
        prefs.setGUI_darkModeActiveByDefault(true)

        assertTrue(GuiOptionsTabState(prefs, languageStore()).gUI_darkModeActiveByDefault)
    }

    @Test
    fun `writing reaches the preferences at once`() {
        val prefs = ChatPreferences()
        val state = GuiOptionsTabState(prefs, languageStore())

        state.guiOptions_defaultFilterPublicMsgs = true
        state.guiOptions_showFreshCallHintInBandColumns = true

        assertTrue(prefs.isGuiOptions_defaultFilterPublicMsgs())
        assertTrue(prefs.isGuiOptions_showFreshCallHintInBandColumns())
    }

    @Test
    fun `the four startup filters are independent switches`() {
        // The JavaFX tab offered them as four separate checkboxes, not a choice of
        // one. Turning them into a radio group would take a combination away that
        // an operator may have configured.
        val prefs = ChatPreferences()
        val state = GuiOptionsTabState(prefs, languageStore())

        state.guiOptions_defaultFilterPublicMsgs = true
        state.guiOptions_defaultFilterPmToMe = true
        state.guiOptions_defaultFilterPmToOther = false
        state.guiOptions_defaultFilterNothing = false

        assertTrue(prefs.isGuiOptions_defaultFilterPublicMsgs())
        assertTrue(prefs.isGuiOptions_defaultFilterPmToMe())
        assertFalse(prefs.isGuiOptions_defaultFilterPmToOther())
        assertFalse(prefs.isGuiOptions_defaultFilterNothing())
    }
}
