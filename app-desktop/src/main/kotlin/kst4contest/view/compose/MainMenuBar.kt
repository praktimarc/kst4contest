package kst4contest.view.compose

import androidx.compose.runtime.Composable
import kst4contest.controller.On4KstConnectionState
import kst4contest.utils.PlatformUtils
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar

/**
 * What the menu bar does when something is picked.
 *
 * An interface rather than a dozen lambdas: every window is meant to carry this same bar, and a
 * parameter list that long is misread sooner or later.
 */
interface MainMenuActions {
    fun connect()
    fun disconnect()
    fun switchOperatorProfile()
    fun exitApplication()
    fun setQrgAsNameInChat()
    fun toggleAwayState()
    fun toggleSettingsWindow()
    fun toggleMonitorWindow()
    fun toggleStationMap()
    fun useDarkDesign()
    fun useDefaultDesign()
    fun openDonationPage()
    fun openHomepage()
    fun openNewsgroup()
    /** The JavaFX item and its handler are both commented out; kept so the bar can say so. */
    fun openChangelog()

    /** OV3T runs the aeroplane feed the AirScout predictions come from. */
    fun openOv3tDonationPage()
    fun contactAuthor()
    fun showAbout()
}

/**
 * The menu bar, declared once and called by every window.
 *
 * Compose has no shared bar across windows: `MenuBar` lives in the scope of a `Window`
 * and macOS shows it in the system menu bar while that window is active. The JavaFX
 * side arranged the same thing the other way round — `installSharedSystemMenuBar`
 * mirrored the main window's menus into an invisible bar in every other window — and
 * this is the Compose shape of it: one function each window can call.
 *
 * The labels of the two window toggles are asked for at the moment the bar is built,
 * because a Compose window can be closed from its own frame without telling the menu.
 */
@Composable
fun FrameWindowScope.Kst4ContestMenuBar(
    state: MainMenuState,
    actions: MainMenuActions,
    settingsWindowOpen: Boolean,
    monitorWindowOpen: Boolean,
    /**
     * The connection state, for the read-only macOS menu. On macOS the status bar hides its
     * badge because JavaFX moved the indicator into the system menu bar, so without this
     * menu a macOS operator would have no connection indicator at all.
     */
    connectionState: On4KstConnectionState? = null,
    connectionDetail: String? = null,
) {
    MenuBar {
        Menu("File") {
            Item(state.connectLabel, enabled = state.canConnect, onClick = actions::connect)
            Item("Disconnect", enabled = state.canDisconnect, onClick = actions::disconnect)
            Item("Switch operator profile...", onClick = actions::switchOperatorProfile)
            Item("Exit + disconnect", onClick = actions::exitApplication)
        }

        Menu("Options") {
            Item(
                "Set QRG as name in Chat (main category)",
                enabled = state.canUseChatActions,
                onClick = actions::setQrgAsNameInChat,
            )
            /*
             * This label is the only place the AFK state is visible anywhere in the
             * application, so it follows the state rather than being flipped inside the
             * click handler as JavaFX did.
             */
            Item(
                state.awayMenuLabel,
                enabled = state.canUseChatActions,
                onClick = actions::toggleAwayState,
            )
            Item(
                if (settingsWindowOpen) "hide options" else "Show options",
                onClick = actions::toggleSettingsWindow,
            )
        }

        Menu("Windows") {
            Item(
                if (monitorWindowOpen) "Hide cluster / stranger QSOs"
                else "Show cluster / stranger QSOs",
                onClick = actions::toggleMonitorWindow,
            )
            Item(
                if (settingsWindowOpen) "hide options" else "show options",
                onClick = actions::toggleSettingsWindow,
            )
            Separator()
            Item("Show / hide station map", onClick = actions::toggleStationMap)
            Separator()
            Item("Use dark mode design", onClick = actions::useDarkDesign)
            Item("Use default mode design", onClick = actions::useDefaultDesign)
        }

        Menu("Info") {
            Item("Donate for kst4Contest development via PayPal", onClick = actions::openDonationPage)
            Item("Donate for OV3T´s plane feed service", onClick = actions::openOv3tDonationPage)
            Item("Visit DARC X08-Homepage", onClick = actions::openHomepage)
            Item("Join kst4Contest newsgroup", onClick = actions::openNewsgroup)
            Item("Contact the author using default mail app", onClick = actions::contactAuthor)
            Separator()
            Item("About...", onClick = actions::showAbout)
        }

        /*
         * The connection indicator, for macOS only: the system menu bar is where it lives
         * there, because the status bar's badge is hidden on that platform. Read-only — the
         * single item is disabled, exactly as menuItemConnectionStateDetailMacOs is.
         */
        if (PlatformUtils.isMacOs()) {
            Menu(ConnectionIndicator.macOsMenuTitle(connectionState)) {
                Item(
                    ConnectionIndicator.detailFor(connectionState, connectionDetail),
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}
