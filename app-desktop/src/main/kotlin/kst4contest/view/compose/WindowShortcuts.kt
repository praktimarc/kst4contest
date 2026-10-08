package kst4contest.view.compose

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

/**
 * The keys that belong to the whole main window rather than to one control.
 *
 * JavaFX hung all of these on the Scene, and the tables even re-fired the send button from
 * their own key filter. An operator who has just clicked a station, a message row or a button
 * must still be able to press Enter and have the message go; and Ctrl+1…Ctrl+0 is how
 * snippets are used at all, because the hands are already on the keyboard.
 */
object WindowShortcuts {

    /**
     * Which snippet a typed character selects, or null if it is not one of the ten digits.
     *
     * Ctrl+1…Ctrl+9 are slots 0 to 8 and Ctrl+0 is the tenth, because the digit row reads
     * 1 to 9 and then 0.
     */
    fun snippetIndexFor(typed: String): Int? = when (typed) {
        "1" -> 0
        "2" -> 1
        "3" -> 2
        "4" -> 3
        "5" -> 4
        "6" -> 5
        "7" -> 6
        "8" -> 7
        "9" -> 8
        "0" -> 9
        else -> null
    }

    /**
     * Handles one key event for the window. Returns true when the event was consumed, which
     * is what keeps Enter out of the text and stops the key travelling further.
     *
     * @param send what Enter does, wherever the focus is
     * @param clear what Escape does
     * @param insertSnippet what Ctrl+<digit> does, with the slot it selected
     */
    fun handle(
        event: KeyEvent,
        send: () -> Unit,
        clear: () -> Unit,
        insertSnippet: (Int) -> Unit,
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        if (event.isCtrlPressed) {
            val index = snippetIndexFor(event.utf16CodePoint.toChar().toString())
                ?: return false
            insertSnippet(index)
            return true
        }

        return when (event.key) {
            Key.Enter, Key.NumPadEnter -> { send(); true }
            Key.Escape -> { clear(); true }
            else -> false
        }
    }
}
