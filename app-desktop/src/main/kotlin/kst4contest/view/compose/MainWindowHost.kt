package kst4contest.view.compose

import kst4contest.model.Band
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import java.util.function.Consumer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Opens the Compose main window.
 *
 * Deliberately a sibling of the JavaFX window rather than its replacement for now: the two
 * are meant to run side by side so the operator can compare them against a live session,
 * which is the only way the differences of the last three Etappen came to light.
 */
object MainWindowHost {

    private val host = ComposeWindowHost("main-window")

    @JvmStatic
    val isOpen: Boolean
        get() = host.isOpen

    @JvmStatic
    fun applyDarkMode(darkMode: Boolean) = host.applyDarkMode(darkMode)

    @JvmStatic
    fun close() = host.close()

    /**
     * @param state everything the window draws; replacing it is what a profile switch will be
     * @param profileName the profile part of the title; the chat part is read from the state
     */
    private var currentState by androidx.compose.runtime.mutableStateOf<MainWindowState?>(null)
    private var currentActions by androidx.compose.runtime.mutableStateOf<MainMenuActions?>(null)
    private var currentSkedBands by androidx.compose.runtime.mutableStateOf<(() -> List<Band>)?>(null)
    private var currentOpenInBrowser by androidx.compose.runtime.mutableStateOf<Consumer<String>?>(null)
    private var currentOnShowOnMap by androidx.compose.runtime.mutableStateOf<(() -> Unit)?>(null)
    private var currentOnShowPathInAirScout by androidx.compose.runtime.mutableStateOf<(() -> Unit)?>(null)
    private var currentOnStationSelected by androidx.compose.runtime.mutableStateOf<((ChatMember) -> Unit)?>(null)
    private var currentOnDirectedMessageClicked by androidx.compose.runtime.mutableStateOf<((ChatMessage) -> Unit)?>(null)
    private var currentIsOwnMessage by androidx.compose.runtime.mutableStateOf<((ChatMessage) -> Boolean)?>(null)
    private var currentMessageAgeSeconds by androidx.compose.runtime.mutableStateOf<((ChatMessage) -> Long)?>(null)
    private var currentOnFiltersChanged by androidx.compose.runtime.mutableStateOf<(() -> Unit)?>(null)
    private var currentOnShowMorePriorities by androidx.compose.runtime.mutableStateOf<(() -> Unit)?>(null)
    private var currentOnCandidateClicked by androidx.compose.runtime.mutableStateOf<((TimelineCandidate) -> Unit)?>(null)

    @JvmStatic
    fun show(
        state: MainWindowState,
        actions: MainMenuActions,
        /**
         * The profile name for the title, or null for the only profile. The rest of the
         * title is what the chat is doing, and that is read from the state during
         * composition -- as the JavaFX window did, where the title carried the whole
         * connection line.
         */
        profileName: String?,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        /**
         * The palette of the active profile, handed to the theme. Null draws the shipped
         * palette.
         */
        paletteStore: PaletteStore? = null,
        /**
         * The interface language of the active profile, handed to the window. Null draws the
         * base language.
         */
        languageStore: kst4contest.view.i18n.LanguageStore? = null,
        widthDp: Float,
        heightDp: Float,
        onResized: (Float, Float) -> Unit,
        /**
         * Runs the application's quit flow. This is the main window: its X — and a tiling
         * window manager's close, the same request — has to ask whether to disconnect and
         * then end the process, the way the File menu's Exit does. Closing it must not
         * leave the client running with the chat connected and nothing to see it in.
         */
        onCloseRequest: () -> Unit,
        skedBands: () -> List<Band>,
        openInBrowser: Consumer<String>,
        onShowOnMap: () -> Unit,
        onShowPathInAirScout: () -> Unit,
        onStationSelected: (ChatMember) -> Unit,
        onDirectedMessageClicked: (ChatMessage) -> Unit,
        isOwnMessage: (ChatMessage) -> Boolean,
        messageAgeSeconds: (ChatMessage) -> Long,
        onFiltersChanged: () -> Unit,
        onShowMorePriorities: () -> Unit,
        onCandidateClicked: (TimelineCandidate) -> Unit,
    ) {
        currentState = state
        currentActions = actions
        currentSkedBands = skedBands
        currentOpenInBrowser = openInBrowser
        currentOnShowOnMap = onShowOnMap
        currentOnShowPathInAirScout = onShowPathInAirScout
        currentOnStationSelected = onStationSelected
        currentOnDirectedMessageClicked = onDirectedMessageClicked
        currentIsOwnMessage = isOwnMessage
        currentMessageAgeSeconds = messageAgeSeconds
        currentOnFiltersChanged = onFiltersChanged
        currentOnShowMorePriorities = onShowMorePriorities
        currentOnCandidateClicked = onCandidateClicked

        host.show(
            title = {
                /*
                 * Read inside the composition, so the title follows the connection the way
                 * the JavaFX one did: "Connected to: 2: 144/432 MHz as DN5PW in JO50JP
                 * (73 users online, 73 shown), 29 messages total." A captured string would
                 * say "disconnected" for the life of the session.
                 */
                val surroundings = currentState?.surroundings
                MainWindowFrame.title(
                    chatState = MainWindowFrame.chatState(
                        surroundings?.connectionState,
                        surroundings?.connectionDetail,
                    ),
                    profileName = profileName,
                )
            },
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            paletteStore = paletteStore,
            languageStore = languageStore,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
            onCloseRequest = onCloseRequest,
        ) { _ ->
            val s = currentState ?: return@show
            val a = currentActions ?: return@show
            MainWindow(
                state = s,
                actions = a,
                skedBands = currentSkedBands?.invoke() ?: emptyList(),
                openInBrowser = currentOpenInBrowser ?: Consumer { _ -> },
                onShowOnMap = currentOnShowOnMap ?: {},
                onShowPathInAirScout = currentOnShowPathInAirScout ?: {},
                onStationSelected = currentOnStationSelected ?: {},
                onDirectedMessageClicked = currentOnDirectedMessageClicked ?: {},
                isOwnMessage = currentIsOwnMessage ?: { false },
                messageAgeSeconds = currentMessageAgeSeconds ?: { 0L },
                onFiltersChanged = currentOnFiltersChanged ?: {},
                onShowMorePriorities = currentOnShowMorePriorities ?: {},
                onCandidateClicked = currentOnCandidateClicked ?: {},
            )
        }
    }
}
