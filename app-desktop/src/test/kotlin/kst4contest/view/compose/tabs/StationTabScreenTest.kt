package kst4contest.view.compose.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.runtime.mutableStateOf
import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.MainWindowSurroundings
import kst4contest.view.feed.ConnectionStateFeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kst4contest.view.compose.awaitNode
import kst4contest.view.compose.awaitGone
import kst4contest.view.compose.inDialog
import kst4contest.view.compose.inPopup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The Station tab, drawn (#107).
 *
 * [StationTabStateTest] pins the facade; this pins that the drawn controls show what the
 * profile holds and that editing one writes through, which is the class of fault the
 * settings window shipped repeatedly with a green build (`83e23f55`, Etappe 7).
 */
@OptIn(ExperimentalTestApi::class)
class StationTabScreenTest {

    /*
     * Never the no-argument ChatPreferences: it resolves the operator's real profile and
     * seeds preferences.xml there when it is missing. A file in a temporary directory is
     * never read or written by these tests, it only gives the preferences a home.
     */
    @TempDir
    lateinit var directory: Path

    private fun preferences(): ChatPreferences = ChatPreferences(directory.resolve("preferences.xml"))

    private class Session(isolated: ChatPreferences) {
        val prefs = isolated.apply {
            stn_loginCallSign = "DN9APW"
            stn_on4kstServersPort = 23001
            isStn_bandActive144 = true
            isStn_bandActive10G = false
        }
        val mainApplied = mutableListOf<ChatCategory?>()
        val link = mutableStateOf(On4KstConnectionState.DISCONNECTED)
        val state = StationTabState(prefs, { mainApplied += it }, { }, { link.value })
    }

    @Test
    fun theTabShowsItsSectionsAndWhatTheProfileHolds() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        listOf("Own station", "Chat category", "ON4KST server", "Active bands", "Defaults",
            "PSTRotator", "Path analysis defaults").forEach { onNodeWithText(it).assertExists() }

        fieldBeside("Callsign").assertTextEquals("DN9APW")
        fieldBeside("Server port").assertTextEquals("23001")
        checkBeside("144 MHz").assertIsOn()
        checkBeside("10 GHz").assertIsOff()
    }

    @Test
    fun editingTheTextFieldsWritesThroughToThePreferences() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        type("Callsign", "DO5AMF")
        type("Locator", "JO51IJ")
        type("Server host name", "www.on4kst.info")

        assertEquals("DO5AMF", s.prefs.stn_loginCallSign)
        assertEquals("JO51IJ", s.prefs.stn_loginLocatorMainCat)
        assertEquals("www.on4kst.info", s.prefs.stn_on4kstServersDns)
        fieldBeside("Callsign").assertTextEquals("DO5AMF")
    }

    /** A number field writes only what parses; a half-typed entry leaves the stored value. */
    @Test
    fun theNumberFieldsWriteOnlyWhatParses() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        type("Server port", "23000")
        assertEquals(23000, s.prefs.stn_on4kstServersPort)

        type("Server port", "23x")
        assertEquals(23000, s.prefs.stn_on4kstServersPort, "an unparsable port was written")

        type("QTF", "135,5")
        assertEquals(135.5, s.prefs.stn_qtfDefault, 1e-9)
    }

    @Test
    fun aBandCheckboxWritesThrough() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }

        toggle("10 GHz")
        assertTrue(s.prefs.isStn_bandActive10G)
        checkBeside("10 GHz").assertIsOn()

        toggle("144 MHz")
        assertFalse(s.prefs.isStn_bandActive144)
    }

    @Test
    fun choosingTheMainCategoryWritesAndAppliesIt() = runComposeUiTest {
        val s = Session(preferences())
        s.prefs.loginChatCategoryMain = ChatCategory(2)
        showTab { StationTab(s.state) }

        val target = s.state.availableCategories.first { it.categoryNumber == 3 }
        val current = s.state.describeCategory(s.prefs.loginChatCategoryMain)
        // The main category is the upper of the two pickers; the second may show the same name.
        onAllNodesWithText(current).onFirst().performScrollTo().performClick()
        waitForIdle()
        awaitNode(inPopup(s.state.describeCategory(target))).performClick()
        waitForIdle()

        assertEquals(3, s.prefs.loginChatCategoryMain?.categoryNumber)
        assertEquals(3, s.mainApplied.single()?.categoryNumber, "the running session was not told")
    }

    /*
     * The credentials are locked while a session exists or is being made: the running
     * connection would not pick up a change, so an edit would look applied and be ignored
     * until the next login. The lock used to be a plain field nothing ever wrote.
     */
    @Test
    fun theLoginFieldsAreLockedWhileConnectedOrConnecting() = runComposeUiTest {
        val s = Session(preferences())
        showTab { StationTab(s.state) }
        val credentials = listOf("Callsign", "Name, main category", "Name, second category", "Locator", "Password")

        credentials.forEach { fieldBeside(it).assertIsEnabled() }

        runOnIdle { s.link.value = On4KstConnectionState.CONNECTING }
        waitForIdle()
        credentials.forEach { fieldBeside(it).assertIsNotEnabled() }

        runOnIdle { s.link.value = On4KstConnectionState.ONLINE }
        waitForIdle()
        credentials.forEach { fieldBeside(it).assertIsNotEnabled() }
        fieldBeside("Server host name").assertIsEnabled()

        runOnIdle { s.link.value = On4KstConnectionState.DISCONNECTED }
        waitForIdle()
        credentials.forEach { fieldBeside(it).assertIsEnabled() }
    }

    /*
     * The application hands the tab `surroundings.connectionState`, which ConnectionStateFeed
     * fills from the controller's callback. The application class itself cannot be built in
     * a test -- it needs a controller with its database, the windows and the update service --
     * so this drives the same chain from the feed on: a pushed state reaches the drawn tab
     * without anything rebuilding it.
     */
    @Test
    fun theLoginFieldsFollowWhatTheConnectionFeedPushes() = runComposeUiTest {
        val surroundings = MainWindowSurroundings()
        val immediately = object : UiDispatcher {
            override fun runOnUi(task: Runnable) = task.run()
            override fun isUiThread(): Boolean = true
        }
        val feed = ConnectionStateFeed(surroundings, immediately)
        val state = StationTabState(preferences(), { }, { }, { surroundings.connectionState })
        showTab { StationTab(state) }

        fieldBeside("Callsign").assertIsEnabled()

        runOnIdle { feed.push(On4KstConnectionState.AUTHENTICATING, null) }
        waitForIdle()
        fieldBeside("Callsign").assertIsNotEnabled()

        // An absent state reads as DISCONNECTED in the feed, and unlocks the fields.
        runOnIdle { feed.push(null, null) }
        waitForIdle()
        fieldBeside("Callsign").assertIsEnabled()
    }
}
