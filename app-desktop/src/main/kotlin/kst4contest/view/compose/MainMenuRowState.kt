package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Which menu of the in-window menu row is currently down.
 *
 * Compose state, because the row redraws when it changes. One menu at a time: two dropped
 * at once is a thing no menu bar does.
 *
 * [anyOpen] is the part that is not cosmetic. `MainWindow` hangs `WindowShortcuts.handle`
 * on the window's `onPreviewKeyEvent`, and a preview runs *before* its children — so
 * without asking this first, pressing Escape to dismiss a menu would also clear the
 * operator's half-typed message.
 */
class MainMenuRowState {

    /** The title of the open menu, or null when none is. */
    var openMenu: String? by mutableStateOf(null)
        private set

    /** Whether a menu is down, and the window keys should therefore stay out of the way. */
    val anyOpen: Boolean
        get() = openMenu != null

    /** Opens the named menu, or closes it when it is the one already open. */
    fun toggle(title: String) {
        openMenu = if (openMenu == title) null else title
    }

    /** Closes whatever is open. Idempotent; a dismissal from outside lands here too. */
    fun close() {
        openMenu = null
    }

    /**
     * Runs a picked item's action and closes the menu it came from.
     *
     * Closing first would be wrong in the one case that matters: the quit item puts up a
     * modal dialog, and the row must not still be drawing a dropdown underneath it.
     */
    fun pick(action: () -> Unit) {
        openMenu = null
        action()
    }
}
