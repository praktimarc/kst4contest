package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.ImmediateDraft
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.i18n.LanguageStore
import kst4contest.view.i18n.LanguageStoreFactory
import kst4contest.view.i18n.SYSTEM_LANGUAGE
import org.junit.jupiter.api.Test
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GuiOptionsTabStateTest(@TempDir private val tempDir: Path) {

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
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val draft = SettingsDraft()
        val state = GuiOptionsTabState(prefs, draft, LanguageStoreFactory.create(prefs, Locale.UK))

        state.language = "de"

        assertEquals("de", state.language)
        assertEquals("de", prefs.guiOptions_language, "the language switches at once, without Apply")
        assertFalse(draft.hasPendingChanges)
    }


    @Test
    fun `reading comes straight from the preferences`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        prefs.setGUI_darkModeActiveByDefault(true)

        assertTrue(GuiOptionsTabState(prefs, SettingsDraft(), languageStore()).gUI_darkModeActiveByDefault)
    }

    @Test
    fun `an edit waits for apply, and discarding drops it`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val draft = SettingsDraft()
        val state = GuiOptionsTabState(prefs, draft, languageStore())

        state.gUI_darkModeActiveByDefault = !prefs.isGUI_darkModeActiveByDefault()
        state.guiOptions_defaultFilterPublicMsgs = true
        state.guiOptions_showFreshCallHintInBandColumns = true

        assertFalse(prefs.isGuiOptions_defaultFilterPublicMsgs(), "an edit took effect before Apply")
        assertTrue(state.guiOptions_defaultFilterPublicMsgs, "the tab shows the edit")

        val darkBefore = prefs.isGUI_darkModeActiveByDefault()
        draft.discard()
        assertEquals(darkBefore, prefs.isGUI_darkModeActiveByDefault())

        state.guiOptions_defaultFilterPublicMsgs = true
        state.guiOptions_showFreshCallHintInBandColumns = true
        draft.apply()

        assertTrue(prefs.isGuiOptions_defaultFilterPublicMsgs())
        assertTrue(prefs.isGuiOptions_showFreshCallHintInBandColumns())
    }

    @Test
    fun `the four startup filters are independent switches`() {
        // The JavaFX tab offered them as four separate checkboxes, not a choice of
        // one. Turning them into a radio group would take a combination away that
        // an operator may have configured.
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val state = GuiOptionsTabState(prefs, ImmediateDraft(), languageStore())

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
