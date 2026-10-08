package kst4contest.view.compose.tabs

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** One of the three ways back, in the order the tab shows them. */
enum class ColourAction { BACK_TO_PREVIOUS, DISCARD_CHANGES, RESET_TO_SHIPPED }

/**
 * The keyboard path to the three ways back.
 *
 * The spec asks for one for all three, and says why: there is no "not yet applied" state, so
 * the ways back are the only way back, and they have to be reachable without colour
 * perception. Default focus traversal technically reaches the buttons, but tabbing blindly
 * past six hex fields to find them is not a rescue.
 *
 * Control is required on all three. Six text fields live in this tab and `#DDEEFF` contains a
 * D: a bare letter would swallow the typing it exists to protect.
 *
 * Split the same way [kst4contest.view.compose.WindowShortcuts] is — a pure decision plus a
 * thin event adapter — because a desktop `KeyEvent` wraps a native AWT event, and a test that
 * built one would be testing Compose.
 */
object ColourShortcuts {

    /**
     * The action a key combination means, or null when it means nothing here.
     *
     * @param ctrlPressed whether control was held
     * @param key the key pressed
     * @return the action, or null to let the key travel on to the fields
     */
    fun actionFor(ctrlPressed: Boolean, key: Key): ColourAction? {

        if (!ctrlPressed) {
            return null
        }

        return when (key) {
            Key.Z -> ColourAction.BACK_TO_PREVIOUS
            Key.D -> ColourAction.DISCARD_CHANGES
            Key.R -> ColourAction.RESET_TO_SHIPPED
            else -> null
        }
    }

    /**
     * How the shortcut is written where the operator can read it.
     *
     * Shown in the tab beside each button: a shortcut nobody is told about is not a second
     * safety net, and this one exists for an operator who cannot read the buttons.
     *
     * @param action the way back
     * @return the key combination as text
     */
    fun labelFor(action: ColourAction): String = when (action) {
        ColourAction.BACK_TO_PREVIOUS -> "Ctrl+Z"
        ColourAction.DISCARD_CHANGES -> "Ctrl+D"
        ColourAction.RESET_TO_SHIPPED -> "Ctrl+R"
    }

    /**
     * Handles one key event for the colours tab.
     *
     * @param event the key event
     * @param perform runs the action the keys named
     * @return true when the event was consumed and must travel no further
     */
    fun handle(event: KeyEvent, perform: (ColourAction) -> Unit): Boolean {

        if (event.type != KeyEventType.KeyDown) {
            return false
        }

        val action = actionFor(event.isCtrlPressed, event.key) ?: return false
        perform(action)
        return true
    }
}
