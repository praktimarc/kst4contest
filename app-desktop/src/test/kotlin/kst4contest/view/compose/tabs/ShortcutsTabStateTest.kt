package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

class ShortcutsTabStateTest {

    private class Session {
        val prefs = ChatPreferences()
        var shortcutRefreshes = 0
        var snippetRefreshes = 0

        fun state() = ShortcutsTabState(
            prefs,
            refreshShortcutButtons = { shortcutRefreshes++ },
            refreshTextSnippetContextMenus = { snippetRefreshes++ },
        )
    }

    @Test
    fun `both lists start from the stored rosters`() {
        val s = Session()
        s.prefs.getLst_txtShortCutBtnList().setAll(listOf("cq", "tnx"))
        s.prefs.getLst_txtSnipList().setAll(listOf("qrv 144", "73"))

        val state = s.state()

        assertEquals(listOf("cq", "tnx"), state.shortcuts.texts)
        assertEquals(listOf("qrv 144", "73"), state.snippets.texts)
    }

    @Test
    fun `a new shortcut lands at the top and reaches the roster at once`() {
        val s = Session()
        s.prefs.getLst_txtShortCutBtnList().setAll(listOf("alt"))
        val state = s.state()
        val rosterBefore = s.prefs.getLst_txtShortCutBtnList()

        state.shortcuts.addAtTop(ShortcutsTabState.NEW_ENTRY_TEMPLATE)
        state.commitShortcuts()

        assertEquals(
            listOf(ShortcutsTabState.NEW_ENTRY_TEMPLATE, "alt"),
            s.prefs.getLst_txtShortCutBtnList().snapshot(),
        )
        assertSame(rosterBefore, s.prefs.getLst_txtShortCutBtnList(),
            "the roster is filled, never replaced: a swap would orphan its listeners")
    }

    @Test
    fun `committing the shortcuts rebuilds the shortcut buttons`() {
        val s = Session()
        val state = s.state()

        state.shortcuts.addAtTop("cq")
        state.commitShortcuts()

        assertTrue(s.shortcutRefreshes > 0,
            "the buttons above the message field are built from this list")
        assertEquals(0, s.snippetRefreshes, "the snippet menus are a different list")
    }

    @Test
    fun `a cleared shortcut disappears from the roster`() {
        val s = Session()
        s.prefs.getLst_txtShortCutBtnList().setAll(listOf("cq", "tnx"))
        val state = s.state()

        state.shortcuts.commitAt(0, "   ")
        state.commitShortcuts()

        assertEquals(listOf("tnx"), s.prefs.getLst_txtShortCutBtnList().snapshot(),
            "clearing the text was how the operator removed an entry")
    }

    @Test
    fun `moving a shortcut reorders the roster`() {
        val s = Session()
        s.prefs.getLst_txtShortCutBtnList().setAll(listOf("a", "b"))
        val state = s.state()

        state.shortcuts.select(0)
        state.shortcuts.moveSelected(1)
        state.commitShortcuts()

        assertEquals(listOf("b", "a"), s.prefs.getLst_txtShortCutBtnList().snapshot())
    }

    @Test
    fun `a new snippet lands at the top and reaches its own roster`() {
        val s = Session()
        s.prefs.getLst_txtSnipList().setAll(listOf("alt"))
        val state = s.state()

        state.snippets.addAtTop(ShortcutsTabState.NEW_ENTRY_TEMPLATE)
        state.commitSnippets()

        assertEquals(
            listOf(ShortcutsTabState.NEW_ENTRY_TEMPLATE, "alt"),
            s.prefs.getLst_txtSnipList().snapshot(),
        )
        assertTrue(s.snippetRefreshes > 0,
            "the Ctrl+1..Ctrl+0 context menus are built from this list")
        assertEquals(0, s.shortcutRefreshes)
    }

    @Test
    fun `the placeholder text of a new entry is the one the JavaFX button inserted`() {
        assertEquals(
            "CHANGE THIS TEXT VIA DOUBLECLICK or remove by deleting all text. Then hit enter key",
            ShortcutsTabState.NEW_ENTRY_TEMPLATE,
        )
    }
}
