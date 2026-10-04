package kst4contest.view.compose

/**
 * One line of a menu.
 *
 * Toolkit-free on purpose: the bar is drawn two different ways — Compose's `MenuBar`,
 * which is a `javax.swing.JMenuBar` under the covers and only used on macOS, and the
 * in-window Compose row everywhere else. Declaring the menus twice is how one renderer
 * quietly loses an item that the other gained, so they both read this instead.
 */
sealed interface MenuEntry {

    /**
     * @param enabled whether it can be pressed; a menu item that merely looks usable is
     *        worse than a disabled one, because the operator presses it mid-contest and
     *        either nothing happens or the wrong thing does
     */
    data class Item(
        val label: String,
        val enabled: Boolean,
        val onClick: () -> Unit,
    ) : MenuEntry

    data object Separator : MenuEntry
}

/** One menu of the bar: its title and what drops down from it. */
data class MenuSpec(val title: String, val entries: List<MenuEntry>)

/**
 * The whole bar, as data.
 *
 * A plain function of the state rather than a composable, which is what finally makes the
 * structure, the dynamic labels and the enabled states testable: while this lived inside
 * Compose's `MenuBar` it took a running window to look at, and none of it was covered.
 *
 * @param settingsWindowOpen asked for at build time, not held in [MainMenuState]: a Compose
 *        window can be closed from its own frame without telling the menu
 * @param monitorWindowOpen the same for the cluster / stranger-QSO window
 */
fun mainMenuModel(
    state: MainMenuState,
    actions: MainMenuActions,
    settingsWindowOpen: Boolean,
    monitorWindowOpen: Boolean,
): List<MenuSpec> = listOf(
    MenuSpec(
        "File",
        listOf(
            MenuEntry.Item(state.connectLabel, state.canConnect, actions::connect),
            MenuEntry.Item("Disconnect", state.canDisconnect, actions::disconnect),
            MenuEntry.Item(
                "Switch operator profile...",
                state.canSwitchProfile,
                actions::switchOperatorProfile,
            ),
            MenuEntry.Item("Exit + disconnect", state.canExit, actions::exitApplication),
        ),
    ),
    MenuSpec(
        "Options",
        listOf(
            MenuEntry.Item(
                "Set QRG as name in Chat (main category)",
                state.canUseChatActions,
                actions::setQrgAsNameInChat,
            ),
            /*
             * This label is the only place the AFK state is visible anywhere in the
             * application, so it follows the state rather than being flipped inside the
             * click handler as JavaFX did.
             */
            MenuEntry.Item(
                state.awayMenuLabel,
                state.canUseChatActions,
                actions::toggleAwayState,
            ),
            MenuEntry.Item(
                if (settingsWindowOpen) "hide options" else "Show options",
                enabled = true,
                actions::toggleSettingsWindow,
            ),
        ),
    ),
    MenuSpec(
        "Windows",
        listOf(
            MenuEntry.Item(
                if (monitorWindowOpen) "Hide cluster / stranger QSOs"
                else "Show cluster / stranger QSOs",
                enabled = true,
                actions::toggleMonitorWindow,
            ),
            MenuEntry.Item(
                if (settingsWindowOpen) "hide options" else "show options",
                enabled = true,
                actions::toggleSettingsWindow,
            ),
            MenuEntry.Separator,
            MenuEntry.Item("Show / hide station map", enabled = true, actions::toggleStationMap),
            MenuEntry.Separator,
            MenuEntry.Item("Use dark mode design", enabled = true, actions::useDarkDesign),
            MenuEntry.Item("Use default mode design", enabled = true, actions::useDefaultDesign),
        ),
    ),
    MenuSpec(
        "Info",
        listOf(
            MenuEntry.Item(
                "Donate for kst4Contest development via PayPal",
                enabled = true,
                actions::openDonationPage,
            ),
            MenuEntry.Item(
                "Donate for OV3T´s plane feed service",
                enabled = true,
                actions::openOv3tDonationPage,
            ),
            MenuEntry.Item("Visit DARC X08-Homepage", enabled = true, actions::openHomepage),
            MenuEntry.Item("Join kst4Contest newsgroup", enabled = true, actions::openNewsgroup),
            MenuEntry.Item(
                "Contact the author using default mail app",
                enabled = true,
                actions::contactAuthor,
            ),
            MenuEntry.Separator,
            MenuEntry.Item("About...", enabled = true, actions::showAbout),
        ),
    ),
)
