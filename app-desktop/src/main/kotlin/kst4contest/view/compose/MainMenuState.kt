package kst4contest.view.compose

import kst4contest.view.i18n.Strings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.controller.On4KstConnectionState

/**
 * Which menu items can be used.
 *
 * Compose state, not plain properties, and deliberately so: a menu item that merely
 * looks usable is a worse place for a stale value than a form field. The operator
 * presses it in the middle of a contest and nothing happens, or the wrong thing does.
 * The JavaFX window drove the same flags from onConnectionStateChanged.
 */
class MainMenuState {

    /** Before the first callback there is no session, which reads as disconnected. */
    var connectionState: On4KstConnectionState by mutableStateOf(On4KstConnectionState.DISCONNECTED)

    private val attemptRunning: Boolean
        get() = connectionState.isConnectionAttemptActive

    /**
     * The Connect item's label, which names the chat the station will land in — and both when
     * the second one is on, so the operator sees before pressing it what they are logging
     * into. JavaFX built the same string in initMenuBar.
     */
    var connectLabel: String by mutableStateOf("Connect")

    val canConnect: Boolean
        get() = !attemptRunning

    /** Also while an attempt is still running: it can be given up. */
    val canDisconnect: Boolean
        get() = attemptRunning

    /**
     * Sending to the chat — the away state, the QRG as name — needs a session that is
     * really online. Logging in is not being in the chat, and a command sent then goes
     * nowhere.
     */
    val canUseChatActions: Boolean
        get() = connectionState.isOnline

    /**
     * Whether the operator is marked away in the chat.
     *
     * Compose state because the away item's own label is the ONLY place this is visible —
     * there is no separate indicator anywhere. JavaFX flipped the label inside the click
     * handler, which left it wrong whenever the state changed from elsewhere.
     */
    var awayFromChat: Boolean by mutableStateOf(false)

    /**
     * What the away item says. It names the next action rather than the current state,
     * which is what the JavaFX wording did.
     *
     * Takes the texts as a parameter: this is a plain state holder, not a composable, so the
     * caller reads the composition local and hands them over.
     */
    fun awayMenuLabel(strings: Strings): String =
        if (awayFromChat) strings.menuShowMeAsActive else strings.menuShowMeAsAway

    /** Never blocked: switching profiles tears the session down on its own. */
    val canSwitchProfile: Boolean = true

    /** Never blocked: an operator must always be able to leave. */
    val canExit: Boolean = true
}
