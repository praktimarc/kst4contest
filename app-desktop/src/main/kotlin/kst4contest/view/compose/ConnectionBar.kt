package kst4contest.view.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import kst4contest.view.i18n.LocalStrings
import kst4contest.view.i18n.Strings
import kst4contest.controller.ChatController
import kst4contest.model.ChatPreferences

/**
 * The connection state the button bar shows. Derived from the controller rather than
 * held, because the session also connects and drops outside this window.
 */
enum class ConnectionState { DISCONNECTED, CONNECTED_NOT_LOGGED_IN, LOGGED_IN }

/**
 * The button bar at the bottom of the settings window.
 *
 * This bar is the reason the settings window exists at startup: in the JavaFX client
 * it carried the Connect button, which is the one place where `ChatController.execute`
 * is called and therefore the only way into the chat. Saving and closing sit next to
 * it because that is where the operator has always found them.
 */
class ConnectionBarState(
    private val prefs: ChatPreferences,
    private val controller: ChatController,
) {

    val connectionState: ConnectionState
        get() = when {
            controller.isConnectedAndLoggedIn -> ConnectionState.LOGGED_IN
            controller.isConnectedAndNOTLoggedIn -> ConnectionState.CONNECTED_NOT_LOGGED_IN
            else -> ConnectionState.DISCONNECTED
        }

    /**
     * The label of the Connect button, which names the chat the station will land in —
     * and both chats when the second one is enabled, so the operator sees before
     * pressing it what they are logging into.
     *
     * Takes the texts as a parameter because this is a plain property holder and not a
     * composable: it cannot read the composition local itself, and the caller can.
     */
    fun connectButtonText(strings: Strings): String {
            val main = prefs.getLoginChatCategoryMain()
                ?: return strings.connectionConnect

            /*
             * Formatted rather than concatenated: in German the chat name comes first ("Mit
             * 144 MHz verbinden"), which no amount of "Connect to " + name can produce.
             */
            var label = strings.connectionConnectTo(main.getChatCategoryName(main.categoryNumber))
            val second = prefs.getLoginChatCategorySecond()

            if (prefs.isLoginToSecondChatEnabled() && second != null) {
                label += " & " + second.getChatCategoryName(second.categoryNumber)
            }

            return label
        }

    /** Only a disconnected session can connect; the JavaFX button locked itself. */
    val canConnect: Boolean
        get() = connectionState != ConnectionState.LOGGED_IN

    /** Disconnecting needs a session that is actually logged in. */
    val canDisconnect: Boolean
        get() = connectionState == ConnectionState.LOGGED_IN
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionBar(
    state: ConnectionBarState,
    onConnect: () -> Unit,
    onSave: () -> Unit,
    onApplyAndClose: () -> Unit,
    onDisconnectAndCloseChat: () -> Unit,
    onDisconnectOnly: () -> Unit,
) {
    /*
     * Order as in the JavaFX HBox: connect, save, apply, disconnect and close, and
     * disconnect only. Operators reach for these by position.
     */
    /*
     * A wrapping row, not a plain one. Five buttons do not fit a narrow window, and a
     * plain Row does not wrap: it squeezes the last children instead. A button squeezed
     * to a few pixels wraps its own label to one letter per line and grows into a tall
     * coloured bar that eats the whole window height — which is exactly what happened.
     */
    val strings = LocalStrings.current

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
        verticalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
    ) {
        Form.button(
            state.connectButtonText(strings),
            enabled = state.canConnect,
            onClick = onConnect,
        )
        Form.button(strings.settingsSave, onClick = onSave)
        Form.button(strings.settingsApplyAndClose, onClick = onApplyAndClose)
        Form.button(strings.connectionDisconnectAndCloseChat, onClick = onDisconnectAndCloseChat)
        Form.button(
            strings.connectionDisconnect,
            enabled = state.canDisconnect,
            onClick = onDisconnectOnly,
        )
    }
}
