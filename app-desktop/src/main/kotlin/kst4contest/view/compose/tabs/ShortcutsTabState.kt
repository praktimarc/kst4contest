package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.EditableListState
import kst4contest.view.compose.SettingsDraft

/**
 * State of the shortcuts-and-snippets tab: the two single-column lists from
 * `grdPnlShorts`.
 *
 * Every committed change of a list is staged in the window's [SettingsDraft]. Applying
 * it fills the roster and rebuilds what is built from it -- the shortcut buttons above the
 * message field, the snippet menus -- once per apply, however many edits went before.
 *
 * @param refreshShortcutButtons rebuilds the button row above the message field,
 *        as `refreshShortcutButtons` did after every edit commit and move.
 * @param refreshTextSnippetContextMenus rebuilds the Ctrl+1..Ctrl+0 menus, the
 *        counterpart for the snippet list.
 */
class ShortcutsTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
    private val refreshShortcutButtons: () -> Unit,
    private val refreshTextSnippetContextMenus: () -> Unit,
) {

    val shortcuts = EditableListState(prefs.getLst_txtShortCutBtnList().snapshot())

    val snippets = EditableListState(prefs.getLst_txtSnipList().snapshot())

    /** Stages the shortcut list; applying it fills the roster and rebuilds the buttons. */
    fun commitShortcuts() {
        draft.stage(
            SHORTCUTS,
            shortcuts.texts.toList(),
            live = { prefs.getLst_txtShortCutBtnList().snapshot() },
            write = { shortcuts.commitTo(prefs.getLst_txtShortCutBtnList()) },
            effect = refreshShortcutButtons,
        )
    }

    /** Stages the snippet list; applying it fills the roster and rebuilds the menus. */
    fun commitSnippets() {
        draft.stage(
            SNIPPETS,
            snippets.texts.toList(),
            live = { prefs.getLst_txtSnipList().snapshot() },
            write = { snippets.commitTo(prefs.getLst_txtSnipList()) },
            effect = refreshTextSnippetContextMenus,
        )
    }

    companion object {
        /**
         * The text the JavaFX Add buttons inserted, kept verbatim: it tells the
         * operator both how to edit the entry and how to remove it again
         * (Kst4ContestApplication:11743 and :11797, identical in both lists).
         */
        const val NEW_ENTRY_TEMPLATE =
            "CHANGE THIS TEXT VIA DOUBLECLICK or remove by deleting all text. Then hit enter key"

        private const val SHORTCUTS = "ShortcutsTabState.shortcuts"
        private const val SNIPPETS = "ShortcutsTabState.snippets"
    }
}
