package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.EditableListState

/**
 * State of the shortcuts-and-snippets tab: the two single-column lists from
 * `grdPnlShorts`.
 *
 * Both lists write through to their roster on every committed change. The JavaFX
 * tables did the same — their Add buttons called `mutate` on the roster directly —
 * and the shortcut buttons above the message field are rebuilt from the roster, so
 * a list that only wrote on Save would leave those buttons stale.
 *
 * @param refreshShortcutButtons rebuilds the button row above the message field,
 *        as `refreshShortcutButtons` did after every edit commit and move.
 * @param refreshTextSnippetContextMenus rebuilds the Ctrl+1..Ctrl+0 menus, the
 *        counterpart for the snippet list.
 */
class ShortcutsTabState(
    private val prefs: ChatPreferences,
    private val refreshShortcutButtons: () -> Unit,
    private val refreshTextSnippetContextMenus: () -> Unit,
) {

    val shortcuts = EditableListState(prefs.getLst_txtShortCutBtnList().snapshot())

    val snippets = EditableListState(prefs.getLst_txtSnipList().snapshot())

    /** Fills the shortcut roster and rebuilds the buttons built from it. */
    fun commitShortcuts() {
        shortcuts.commitTo(prefs.getLst_txtShortCutBtnList())
        refreshShortcutButtons()
    }

    /** Fills the snippet roster and rebuilds the context menus built from it. */
    fun commitSnippets() {
        snippets.commitTo(prefs.getLst_txtSnipList())
        refreshTextSnippetContextMenus()
    }

    companion object {
        /**
         * The text the JavaFX Add buttons inserted, kept verbatim: it tells the
         * operator both how to edit the entry and how to remove it again
         * (Kst4ContestApplication:11743 and :11797, identical in both lists).
         */
        const val NEW_ENTRY_TEMPLATE =
            "CHANGE THIS TEXT VIA DOUBLECLICK or remove by deleting all text. Then hit enter key"
    }
}
