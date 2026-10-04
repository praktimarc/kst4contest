package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class GuiOptionsTabStateTest {

    @Test
    fun `reading comes straight from the preferences`() {
        val prefs = ChatPreferences()
        prefs.setGUI_darkModeActiveByDefault(true)

        assertTrue(GuiOptionsTabState(prefs).gUI_darkModeActiveByDefault)
    }

    @Test
    fun `writing reaches the preferences at once`() {
        val prefs = ChatPreferences()
        val state = GuiOptionsTabState(prefs)

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
        val state = GuiOptionsTabState(prefs)

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
