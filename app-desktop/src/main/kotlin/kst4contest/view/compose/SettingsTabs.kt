package kst4contest.view.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kst4contest.controller.ChatController
import kst4contest.controller.ActiveOperatorProfile
import kst4contest.controller.OperatorProfileManagementService
import kst4contest.controller.OperatorProfilePaths
import kst4contest.model.ChatMember
import kst4contest.model.OperatorProfile
import kst4contest.view.compose.OperatorProfilePaletteFiles
import kst4contest.view.compose.tabs.AirscoutTab
import kst4contest.view.compose.tabs.AirscoutTabState
import kst4contest.view.compose.tabs.BeaconTab
import kst4contest.view.compose.tabs.BeaconTabState
import kst4contest.view.compose.tabs.ColoursTab
import kst4contest.view.compose.tabs.ColoursTabState
import kst4contest.view.compose.tabs.GuiOptionsTab
import kst4contest.view.compose.tabs.GuiOptionsTabState
import kst4contest.view.compose.tabs.LogSynchTab
import kst4contest.view.compose.tabs.LogSynchTabState
import kst4contest.view.compose.tabs.MessageHandlingTab
import kst4contest.view.compose.tabs.MessageHandlingTabState
import kst4contest.view.compose.tabs.NotificationTab
import kst4contest.view.compose.tabs.NotificationTabState
import kst4contest.view.compose.tabs.ProfilesTab
import kst4contest.view.compose.tabs.ProfilesTabState
import kst4contest.view.compose.tabs.ShortcutsTab
import kst4contest.view.compose.tabs.ShortcutsTabState
import kst4contest.view.compose.tabs.StationTab
import kst4contest.view.compose.tabs.StationTabState
import kst4contest.view.compose.tabs.TrxSynchTab
import kst4contest.view.compose.tabs.TrxSynchTabState
import kst4contest.view.compose.tabs.WorkedDatabaseTab
import kst4contest.view.compose.tabs.WorkedDatabaseTabState

/**
 * The work the settings window needs from the main window.
 *
 * These are the callbacks whose implementation lives in the JavaFX window and cannot
 * be reached from a ChatController: rebuilding controls, binding the own-QRG
 * follower and refreshing a table. Everything else the tabs need comes from the
 * controller, which is why it is not listed here.
 */
interface SettingsHost {

    /**
     * Rebuilds the button row above the message field from the shortcut list.
     *
     * The suffix keeps this distinct from the window's own private method of the same
     * purpose, which touches JavaFX directly; the implementation of this one has to hop
     * onto the JavaFX thread first, because the settings window runs on its own.
     */
    fun refreshShortcutButtonsFromSettings()

    /** Rebuilds the Ctrl+1..Ctrl+0 context menus from the snippet list. */
    fun refreshTextSnippetContextMenusFromSettings()

    /** The detected subnet broadcast address for Win-Test, or null. */
    fun detectWintestBroadcastAddress(): String?

    /** Attaches or detaches the follower that mirrors the own QRG into the station field. */
    fun applyOwnQrgFollower(enabled: Boolean)

    /**
     * Turns file recording of the whole session on or off, with the history recorder.
     *
     * Lives here and not on the controller because enabling it opens the error-log file
     * handler, which the application owns. The order — log before recorder when enabling,
     * recorder before log when disabling — is the one the JavaFX checkbox kept.
     */
    fun applyDebugModeToFile(enabled: Boolean)

    /** Re-reads the worked-stations table for display. */
    fun refreshWorkedStationsView()

    /**
     * Switches the application to another operator profile. The tab must not do this
     * itself: the switch reloads preferences and the worked database, which only the
     * application can sequence.
     */
    fun switchToOperatorProfile(profile: OperatorProfile)

    /**
     * Starts the ON4KST session — the work of the Connect button.
     *
     * @return null when the session was started, otherwise the reason it was not
     */
    fun connectFromSettings(): String?

    /** Drops the session and keeps the client running. */
    fun disconnectOnlyFromSettings()

    /** Drops the session and closes the client. */
    fun disconnectAndCloseChatFromSettings()

    /**
     * Persists the preferences.
     *
     * @return the file the settings went to, or null when it could not be written
     */
    fun savePreferencesFromSettings(): String?
}

/**
 * Builds the settings tabs in the order the JavaFX TabPane had them.
 *
 * The wiring lives here rather than in the Java window on purpose: every call that
 * reaches past ChatPreferences into the running session is made in one readable
 * place, and the foreign-write coverage test can see it. A tab that only wrote a
 * preference and skipped its session call would look correct in the file and do
 * nothing until the next restart.
 */
fun buildSettingsTabs(
    controller: ChatController,
    host: SettingsHost,
    notices: SettingsNotices,
    paletteStore: PaletteStore,
    languageStore: kst4contest.view.i18n.LanguageStore,
): List<SettingsTab> {

    val prefs = controller.chatPreferences

    val stationState = StationTabState(
        prefs,
        applyMainCategory = { category -> controller.setChatCategoryMain(category) },
        applySecondCategory = { category -> controller.setChatCategorySecondChat(category) },
    )

    val notificationState = NotificationTabState(
        prefs,
        controller.lstNotify_QSOSniffer_sniffedCallSignList,
        applyDxClusterServerEnabled = { enabled ->
            /*
             * Starting a server that nothing can reach yet is pointless, so the JavaFX
             * checkbox only touched the server while the session was logged in; the
             * flag alone decides what happens at the next login.
             */
            if (controller.isConnectedAndLoggedIn) {
                if (enabled) controller.startDxClusterServerIfEnabled() else controller.stopDxClusterServer()
            }
        },
        applyDxClusterServerPort = {
            if (controller.isConnectedAndLoggedIn && prefs.isNotify_dxClusterServerEnabled()) {
                controller.restartDxClusterServerIfEnabled()
            }
        },
        broadcastTestSpot = { spot -> deliverTestSpot(controller, spot) },
    )

    val shortcutsState = ShortcutsTabState(
        prefs,
        refreshShortcutButtons = host::refreshShortcutButtonsFromSettings,
        refreshTextSnippetContextMenus = host::refreshTextSnippetContextMenusFromSettings,
    )

    val beaconState = BeaconTabState(
        prefs,
        validateBeaconTemplate = { template -> controller.validateBeaconTemplate(template) },
        restartBeaconTimer = { controller.restartBeaconTimer() },
        mainCategoryName = categoryName(controller.chatCategoryMain, kst4contest.view.i18n.CurrentStrings.get().settingsCategoryMainFallback),
        secondCategoryName = categoryName(controller.chatCategorySecondChat, kst4contest.view.i18n.CurrentStrings.get().settingsCategorySecondFallback),
    )

    val logSynchState = LogSynchTabState(
        prefs,
        restartWintestListener = { controller.restartWintestUdpListenerIfEnabled() },
        stopWintestListener = { controller.stopWintestUdpListener() },
        detectWintestBroadcastAddress = host::detectWintestBroadcastAddress,
    )

    val workedDatabaseState = WorkedDatabaseTabState(
        resetInDatabase = { controller.dbHandler.resetWorkedDataInDB() },
        refreshGuiLists = {
            controller.resetWorkedAndQrvInfoInGuiLists()
            controller.refreshWorkedStateAndDatabaseListFromDatabase()
        },
        refreshView = host::refreshWorkedStationsView,
        workedRoster = controller.lst_DBBasedWkdCallSignList,
    )

    val profilesState = ProfilesTabState(
        OperatorProfileManagementService(),
        activeSelection = { ActiveOperatorProfile.get() },
        requestActivation = host::switchToOperatorProfile,
    )

    val reportRefusal: (String) -> Unit = notices::problem

    /*
     * The profile whose stylesheet the colours tab offers to write. Resolved here rather than
     * captured, because the settings window outlives nothing but is built once per profile.
     */
    fun activeProfileOrRoot(): OperatorProfile =
        ActiveOperatorProfile.get()?.profile
            ?: OperatorProfilePaths.buildRootProfile(prefs.stn_loginCallSign ?: "")

    /*
     * Order and titles are those of the JavaFX TabPane, character for character. The
     * comment there is explicit that operators navigate these tabs by muscle memory,
     * so a tidier order or a shorter title would be a regression, not an improvement.
     */
    return listOf(
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabStation }) { StationTab(stationState) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabLogSynch }) { LogSynchTab(logSynchState) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabTrxSynch }) { TrxSynchTab(TrxSynchTabState(prefs, host::applyOwnQrgFollower)) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabAirscout }) { AirscoutTab(AirscoutTabState(prefs)) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabNotification }) { NotificationTab(notificationState, reportRefusal) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabShortcuts }) { ShortcutsTab(shortcutsState, reportRefusal) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabBeacon }) { BeaconTab(beaconState, reportRefusal) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabMessageHandling }) { MessageHandlingTab(MessageHandlingTabState(prefs, host::applyDebugModeToFile)) },
        /*
         * Selecting this tab re-read the database in the JavaFX window; the list is a
         * snapshot and would otherwise show what was true when the window opened.
         */
        SettingsTab(
            { kst4contest.view.i18n.CurrentStrings.get().tabWorkedDatabase },
            onSelected = { controller.refreshWorkedStateAndDatabaseListFromDatabase() },
        ) { WorkedDatabaseTab(workedDatabaseState) },
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabGui }) { GuiOptionsTab(GuiOptionsTabState(prefs, languageStore)) },
        /*
         * Appended last on purpose so no established tab position shifts, the reason
         * the JavaFX tab gave for the same placement.
         */
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabProfiles }, onSelected = profilesState::refresh) { ProfilesTab(profilesState) },
        /*
         * Appended after Profiles for the same reason Profiles was appended after GUI. The
         * tab edits whichever design is in force, which is why it reads the flag through a
         * lambda rather than taking its value here: the operator can switch day/evening from
         * the main window's menu while this tab is open.
         */
        SettingsTab({ kst4contest.view.i18n.CurrentStrings.get().tabColours }) {
            /*
             * Remembered rather than rebuilt: the tab recomposes on every colour change, and
             * the design flag is read through a lambda precisely so the object itself does not
             * have to be rebuilt to follow a day/evening switch.
             */
            val coloursState = remember(paletteStore) {
                ColoursTabState(
                    paletteStore,
                    { prefs.isGUI_darkModeActive },
                    OperatorProfilePaletteFiles.of(activeProfileOrRoot()),
                )
            }
            ColoursTab(coloursState, reportRefusal, notices::info)
        },
    )
}

/**
 * Delivers the test spot and reports why it could not be delivered, in the order the
 * JavaFX button checked: session first, then a connected logger, then the delivery
 * itself. The messages are the ones the operator knows.
 */
private fun deliverTestSpot(controller: ChatController, spot: ChatMember): String? {
    if (!controller.isConnectedAndLoggedIn || !controller.chatPreferences.isNotify_dxClusterServerEnabled()) {
        return "Connect KST4Contest and enable the local DX Cluster server first."
    }

    val server = controller.dxClusterServer
        ?: return "Connect KST4Contest and enable the local DX Cluster server first."

    if (!server.hasConnectedClients()) {
        return "No DX Cluster client is connected to KST4Contest."
    }

    if (!server.broadcastSingleDXClusterEntryToLoggers(spot)) {
        return "The test spot could not be delivered."
    }

    return null
}

/** The label a chat category carries in the beacon tab, or a fallback when unset. */
private fun categoryName(category: kst4contest.model.ChatCategory?, fallback: String): String =
    category?.getChatCategoryName(category.categoryNumber) ?: fallback

/**
 * The bottom button row of the settings window.
 *
 * Built here because it needs the controller for its state and the host for its
 * actions, and because it belongs with the tabs it sits under. The `close` action comes
 * from the window itself: only the window can end its own Compose application.
 */
fun settingsButtons(
    controller: ChatController,
    host: SettingsHost,
    notices: SettingsNotices,
): @Composable (close: () -> Unit) -> Unit {
    val barState = ConnectionBarState(controller.chatPreferences, controller)

    return { close ->
        /*
         * The bar is rebuilt after every action so its labels and enabled states follow
         * the session. The JavaFX bar was told by onConnectionStateChanged instead.
         */
        var revision by remember { mutableStateOf(0) }

        @Suppress("UNUSED_EXPRESSION") revision

        ConnectionBar(
            state = barState,
            onConnect = {
                val refusal = host.connectFromSettings()
                if (refusal != null) notices.problem(refusal)
                revision++
            },
            onSave = {
                val target = host.savePreferencesFromSettings()
                if (target == null) {
                    notices.problem(
                        "The configuration file could not be written. The settings stay " +
                            "active for this session."
                    )
                } else {
                    notices.info(
                        "Settings are stored as default to the xml config file:\n" + target
                    )
                }
            },
            onApplyAndClose = close,
            onDisconnectAndCloseChat = host::disconnectAndCloseChatFromSettings,
            onDisconnectOnly = {
                host.disconnectOnlyFromSettings()
                revision++
            },
        )
    }
}
