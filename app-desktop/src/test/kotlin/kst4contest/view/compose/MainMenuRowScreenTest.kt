package kst4contest.view.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.controller.On4KstConnectionState
import kst4contest.model.PaletteRole
import kst4contest.view.i18n.LanguageStore
import kst4contest.view.i18n.LocalStrings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * The in-window menu row, drawn and clicked (#107).
 *
 * [MainMenuModelTest] covers the bar as data. This covers what the operator actually meets:
 * a title that opens, an item that runs its own action, a label that follows the state while
 * the row is on screen. Two regressions are the reason it exists:
 *
 * - `fe4215bc` (Remove JavaFX, in `main` as `2a5c89b9`): the sixteen menu actions still hung
 *   on JavaFX `MenuItem`s that had been deleted, so picking them in the Compose menu did
 *   nothing. Found by a GUI acceptance run, never by the build. They were moved onto their
 *   own `menuAction…` methods behind [MainMenuActions].
 * - `83e23f55` (Draw the menu bar in Compose, in `main` as `2a5c89b9`): Compose's `MenuBar`
 *   is a Swing `JMenuBar` that no theme reaches, so Linux showed the default Metal menu above
 *   a themed window. The row is now Compose and reads the stylesheet palette.
 *
 * Headless: only a composition, no window.
 */
@OptIn(ExperimentalTestApi::class)
class MainMenuRowScreenTest {

    private class RecordingActions : MainMenuActions {
        val calls = mutableListOf<String>()
        override fun connect() { calls += "connect" }
        override fun disconnect() { calls += "disconnect" }
        override fun switchOperatorProfile() { calls += "switchOperatorProfile" }
        override fun exitApplication() { calls += "exitApplication" }
        override fun setQrgAsNameInChat() { calls += "setQrgAsNameInChat" }
        override fun toggleAwayState() { calls += "toggleAwayState" }
        override fun toggleSettingsWindow() { calls += "toggleSettingsWindow" }
        override fun toggleMonitorWindow() { calls += "toggleMonitorWindow" }
        override fun toggleStationMap() { calls += "toggleStationMap" }
        override fun useDarkDesign() { calls += "useDarkDesign" }
        override fun useDefaultDesign() { calls += "useDefaultDesign" }
        override fun openDonationPage() { calls += "openDonationPage" }
        override fun openHomepage() { calls += "openHomepage" }
        override fun openNewsgroup() { calls += "openNewsgroup" }
        override fun openChangelog() { calls += "openChangelog" }
        override fun openOv3tDonationPage() { calls += "openOv3tDonationPage" }
        override fun contactAuthor() { calls += "contactAuthor" }
        override fun showAbout() { calls += "showAbout" }
    }

    private val rowTag = "menu-row"

    private fun ComposeUiTest.showRow(
        state: MainMenuState,
        actions: MainMenuActions,
        rowState: MainMenuRowState = MainMenuRowState(),
        settingsOpen: () -> Boolean = { false },
        monitorOpen: () -> Boolean = { false },
    ) {
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                Kst4ContestMenuRow(
                    state = state,
                    actions = actions,
                    settingsWindowOpen = settingsOpen(),
                    monitorWindowOpen = monitorOpen(),
                    rowState = rowState,
                    modifier = Modifier.testTag(rowTag),
                )
            }
        }
    }

    private fun onlineState() = MainMenuState().apply {
        connectionState = On4KstConnectionState.ONLINE
        connectLabel = "Connect to 144/432 MHz"
    }

    /**
     * Every item, picked through the drawn row, reaches its own action and nobody else's.
     * This is the `fe4215bc` fault in the form the operator saw it: the item was there and
     * picking it did nothing.
     */
    @Test
    fun everyItemPickedInTheRowRunsItsOwnAction() = runComposeUiTest {
        val actions = RecordingActions()
        val state = onlineState()
        val rowState = MainMenuRowState()
        showRow(state, actions, rowState)

        val expected = listOf(
            Triple("File", "Disconnect", null),
            Triple("File", "Switch operator profile...", "switchOperatorProfile"),
            Triple("File", "Exit + disconnect", "exitApplication"),
            Triple("Options", "Set QRG as name in Chat (main category)", "setQrgAsNameInChat"),
            Triple("Options", "Show me as AWAY FROM chat!", "toggleAwayState"),
            Triple("Options", "Show options", "toggleSettingsWindow"),
            Triple("Windows", "Show cluster / stranger QSOs", "toggleMonitorWindow"),
            Triple("Windows", "Show / hide station map", "toggleStationMap"),
            Triple("Windows", "Use dark mode design", "useDarkDesign"),
            Triple("Windows", "Use default mode design", "useDefaultDesign"),
            Triple("Info", "Donate for kst4Contest development via PayPal", "openDonationPage"),
            Triple("Info", "Donate for OV3T´s plane feed service", "openOv3tDonationPage"),
            Triple("Info", "Visit DARC X08-Homepage", "openHomepage"),
            Triple("Info", "Join kst4Contest newsgroup", "openNewsgroup"),
            Triple("Info", "Contact the author using default mail app", "contactAuthor"),
            Triple("Info", "About...", "showAbout"),
        )

        expected.filter { it.third != null }.forEach { (menu, item, action) ->
            onNodeWithText(menu).performClick()
            waitForIdle()
            onNodeWithText(item).performClick()
            waitForIdle()

            assertEquals(action, actions.calls.lastOrNull(), "'$menu > $item' ran the wrong action")
            assertEquals(null, rowState.openMenu, "'$menu' stayed down after '$item' was picked")
        }

        assertEquals(
            expected.count { it.third != null },
            actions.calls.size,
            "an item ran more than one action, or one ran none",
        )
    }

    /** Connected: Connect is greyed out and Disconnect works; the drawn row says so. */
    @Test
    fun theConnectItemsFollowTheConnectionStateWhileTheMenuIsDown() = runComposeUiTest {
        val actions = RecordingActions()
        val state = MainMenuState().apply { connectLabel = "Connect to 144/432 MHz" }
        showRow(state, actions)

        onNodeWithText("File").performClick()
        waitForIdle()

        onNodeWithText("Connect to 144/432 MHz").assertIsEnabled()
        onNodeWithText("Disconnect").assertIsNotEnabled()

        // The state changes from elsewhere -- the connection callback -- with the menu down.
        runOnIdle { state.connectionState = On4KstConnectionState.CONNECTING }
        waitForIdle()

        onNodeWithText("Connect to 144/432 MHz").assertIsNotEnabled()
        onNodeWithText("Disconnect").assertIsEnabled()

        onNodeWithText("Connect to 144/432 MHz").performClick()
        waitForIdle()
        assertTrue(actions.calls.isEmpty(), "a disabled item ran its action: ${actions.calls}")
    }

    /**
     * The away item's label is the only place the AFK state is shown at all. JavaFX flipped
     * it inside the click handler, which left it wrong whenever the state changed from
     * elsewhere; here it must follow the state, drawn.
     */
    @Test
    fun theAwayLabelFollowsTheStateAndNotTheClick() = runComposeUiTest {
        val actions = RecordingActions()
        val state = onlineState()
        showRow(state, actions)

        onNodeWithText("Options").performClick()
        waitForIdle()
        onNodeWithText("Show me as AWAY FROM chat!").performClick()
        waitForIdle()

        // The click alone changes nothing: the action did not touch the state.
        onNodeWithText("Options").performClick()
        waitForIdle()
        onNodeWithText("Show me as AWAY FROM chat!").assertExists()

        runOnIdle { state.awayFromChat = true }
        waitForIdle()

        onNodeWithText("Show me as ACTIVE in chat!").assertExists()
        onNodeWithText("Show me as AWAY FROM chat!").assertDoesNotExist()
    }

    /** Both toggles name the next action, and follow windows that close on their own. */
    @Test
    fun theWindowTogglesFollowWhetherTheWindowIsOpen() = runComposeUiTest {
        var monitorOpen by mutableStateOf(false)
        showRow(onlineState(), RecordingActions(), monitorOpen = { monitorOpen })

        onNodeWithText("Windows").performClick()
        waitForIdle()
        onNodeWithText("Show cluster / stranger QSOs").assertExists()

        runOnIdle { monitorOpen = true }
        waitForIdle()

        onNodeWithText("Hide cluster / stranger QSOs").assertExists()
        onNodeWithText("Show cluster / stranger QSOs").assertDoesNotExist()
    }

    /** A second click on an open title closes it again, the way a menu bar behaves. */
    @Test
    fun clickingAnOpenTitleClosesIt() = runComposeUiTest {
        val rowState = MainMenuRowState()
        showRow(onlineState(), RecordingActions(), rowState)

        onNodeWithText("Info").performClick()
        waitForIdle()
        assertEquals("Info", rowState.openMenu)
        onNodeWithText("About...").assertExists()

        onNodeWithText("Info").performClick()
        waitForIdle()
        assertEquals(null, rowState.openMenu)
        onNodeWithText("About...").assertDoesNotExist()
    }

    /**
     * The `83e23f55` fault: the bar must be drawn in the palette, and a change of the palette
     * must reach it while it is on screen. Measured on the rendered row, to the right of the
     * last title, where nothing but the row's own background is drawn.
     */
    @Test
    fun theRowIsDrawnInThePaletteAndFollowsAChange() = runComposeUiTest {
        var stored = ""
        val store = PaletteStore(
            shippedOf = { JavaFxStylesheet.read("/KST4ContestDefaultDay.css") },
            fileRolesOf = { emptyMap() },
            storedOverridesOf = { stored },
            storeOverrides = { _, encoded -> stored = encoded },
        )

        setContent {
            CompositionLocalProvider(LocalPaletteStore provides store) {
                Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                    Kst4ContestMenuRow(
                        state = onlineState(),
                        actions = RecordingActions(),
                        settingsWindowOpen = false,
                        monitorWindowOpen = false,
                        rowState = MainMenuRowState(),
                        modifier = Modifier.testTag(rowTag),
                    )
                }
            }
        }
        waitForIdle()

        assertEquals(store.shipped(false).base, rightEdgeColour(), "the row is not in the shipped base")

        runOnIdle { store.set(darkMode = false, role = PaletteRole.SURFACE, colour = "#FF0000") }
        waitForIdle()

        assertEquals(Color(0xFFFF0000), rightEdgeColour(), "a palette change did not reach the row")
    }

    private fun ComposeUiTest.rightEdgeColour(): Color {
        val pixels = onNodeWithTag(rowTag).captureToImage().toPixelMap()
        return pixels[pixels.width - 2, pixels.height / 2]
    }

    /** A language picked after the first draw reaches the titles and the items. */
    @Test
    fun aLanguageChosenWhileTheRowIsShownReachesIt() = runComposeUiTest {
        var storedLanguage = "en"
        val languages = LanguageStore(
            storedLanguage = { storedLanguage },
            storeLanguage = { storedLanguage = it },
            systemDefault = Locale.UK,
        )

        setContent {
            CompositionLocalProvider(LocalStrings provides languages.strings) {
                Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                    Kst4ContestMenuRow(
                        state = onlineState(),
                        actions = RecordingActions(),
                        settingsWindowOpen = false,
                        monitorWindowOpen = false,
                        rowState = MainMenuRowState(),
                    )
                }
            }
        }

        onNodeWithText("File").assertExists()

        try {
            runOnIdle { languages.use("de") }
            waitForIdle()

            onNodeWithText("Datei").performClick()
            waitForIdle()
            onNodeWithText("Trennen").assertExists()
            onNodeWithText("File").assertDoesNotExist()
        } finally {
            // use() also sets the JVM-wide CurrentStrings; leave the next test the base language.
            languages.use("en")
        }
    }
}
