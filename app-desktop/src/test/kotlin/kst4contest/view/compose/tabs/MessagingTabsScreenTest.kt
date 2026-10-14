package kst4contest.view.compose.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.Band
import kst4contest.model.ChatMember
import kst4contest.model.ChatPreferences
import kst4contest.observe.SimpleRoster
import kst4contest.view.i18n.LanguageStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * The tabs that decide what this station says and hears, drawn: Message handling, GUI,
 * Beacon and Notification (#107).
 *
 * Each is composed headless, shows its sections and key controls, and an edit made through
 * the drawn control reaches the preferences. The message-handling tab carries the
 * debug-to-file switch that the Compose redraw had dropped and `c8ce0e98` gave back.
 */
@OptIn(ExperimentalTestApi::class)
class MessagingTabsScreenTest {

    // ------------------------------------------------------------------ Message handling

    @Test
    fun messageHandlingWritesBothCategoriesAndAppliesTheFileRecording() = runComposeUiTest {
        val prefs = ChatPreferences().apply {
            isMessageHandling_autoAnswerEnabled = false
            isMessageHandling_debugModeToFileEnabled = false
        }
        val recording = mutableListOf<Boolean>()
        val state = MessageHandlingTabState(prefs) { recording += it }
        showTab { MessageHandlingTab(state) }

        onNodeWithText("Automatic answer to private messages").assertExists()
        onNodeWithText("Diagnostics").assertExists()

        toggle("Answer private messages automatically")
        assertTrue(prefs.isMessageHandling_autoAnswerEnabled)
        assertTrue(prefs.isMessageHandling_autoAnswerEnabledSecondCat, "the second category was left behind")

        type("Answer text", "qrv 432.200")
        assertEquals("qrv 432.200", prefs.messageHandling_autoAnswerTextMainCat)
        assertEquals("qrv 432.200", prefs.messageHandling_autoAnswerTextSecondCat)

        // The c8ce0e98 control: it writes the preference and asks the application to act.
        toggle("Record the whole session to file")
        assertTrue(prefs.isMessageHandling_debugModeToFileEnabled)
        assertEquals(listOf(true), recording)
    }

    // ------------------------------------------------------------------ GUI

    @Test
    fun guiOptionsWritesTheStartupChoicesAndTheLanguage() = runComposeUiTest {
        val prefs = ChatPreferences().apply { isGUI_darkModeActiveByDefault = false }
        var storedLanguage = "en"
        val languages = LanguageStore({ storedLanguage }, { storedLanguage = it }, Locale.UK)
        val state = GuiOptionsTabState(prefs, languages)
        showTab { GuiOptionsTab(state) }

        onNodeWithText("Startup design").assertExists()
        onNodeWithText("Message filters active at startup").assertExists()
        checkBeside("Start in the evening design").assertIsOff()

        toggle("Start in the evening design")
        assertTrue(prefs.isGUI_darkModeActiveByDefault)

        val before = prefs.isGuiOptions_defaultFilterPublicMsgs
        toggle("Public messages")
        assertEquals(!before, prefs.isGuiOptions_defaultFilterPublicMsgs)

        try {
            onNodeWithText("EN").performScrollTo().performClick()
            waitForIdle()
            onAllNodesWithText("DE").onLast().performClick()
            waitForIdle()

            assertEquals("de", storedLanguage, "the picked language was not stored")
            assertEquals("de", languages.language)
        } finally {
            // use() also sets the JVM-wide CurrentStrings; leave the next test the base language.
            languages.use("en")
        }
    }

    // ------------------------------------------------------------------ Beacon

    private class Beacon(val refuse: Boolean = false) {
        val prefs = ChatPreferences()
        var restarts = 0
        val refusals = mutableListOf<String>()
        val state = BeaconTabState(
            prefs,
            validateBeaconTemplate = { if (refuse) throw IllegalArgumentException("no variable %foo%") },
            restartBeaconTimer = { restarts++ },
            mainCategoryName = "Microwave",
            secondCategoryName = "144/432 MHz",
        )
    }

    @Test
    fun beaconShowsOneSectionPerCategoryAndCommitsOnEnter() = runComposeUiTest {
        val s = Beacon()
        showTab { BeaconTab(s.state) { s.refusals += it } }

        onNodeWithText("CQ beacon for Microwave").assertExists()
        onNodeWithText("CQ beacon for 144/432 MHz").assertExists()
        onNodeWithText("Shared interval").assertExists()

        typeAndCommit("Beacon message", "cq de dn9apw %MYQRG%", occurrence = 1)
        assertEquals("cq de dn9apw %MYQRG%", s.prefs.bcn_beaconTextSecondCat)

        typeAndCommit("Interval in whole minutes", "7")
        assertEquals(7, s.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertTrue(s.refusals.isEmpty(), "a valid entry was refused: ${s.refusals}")
    }

    /** A refused template is reported, not stored, and the field goes back to what is stored. */
    @Test
    fun beaconRefusesAnInvalidTemplateAndPutsTheFieldBack() = runComposeUiTest {
        val s = Beacon(refuse = true)
        s.prefs.bcn_beaconTextMainCat = "cq cq"
        showTab { BeaconTab(s.state) { s.refusals += it } }
        /*
         * Drawing the tab already commits each committed field once, because the field's
         * focus callback fires with "not focused" when it is first attached -- reported
         * under #107 as a defect, not pinned here. Only what this test types is counted.
         */
        s.refusals.clear()

        typeAndCommit("Beacon message", "cq %foo%")

        assertEquals("cq cq", s.prefs.bcn_beaconTextMainCat)
        assertEquals(1, s.refusals.size)
        fieldBeside("Beacon message").assertTextEquals("cq cq")
    }

    // ------------------------------------------------------------------ Notification

    private class Notification {
        val prefs = ChatPreferences().apply { isNotify_dxClusterServerEnabled = false }
        val monitored = SimpleRoster<String>()
        val serverEnabled = mutableListOf<Boolean>()
        val ports = mutableListOf<Int>()
        val spots = mutableListOf<ChatMember>()
        val refusals = mutableListOf<String>()
        val state = NotificationTabState(
            prefs,
            monitored,
            applyDxClusterServerEnabled = { serverEnabled += it },
            applyDxClusterServerPort = { ports += it },
            broadcastTestSpot = { spots += it; "no logger connected" },
        )
    }

    @Test
    fun notificationWritesThroughAndStartsTheClusterServer() = runComposeUiTest {
        val s = Notification()
        showTab { NotificationTab(s.state) { s.refusals += it } }

        listOf("Sounds", "Band upgrade hints", "Local DX Cluster server", "QSO monitoring")
            .forEach { onNodeWithText(it).assertExists() }

        val before = s.prefs.isNotify_playSimpleSounds
        toggle("Play simple sounds")
        assertEquals(!before, s.prefs.isNotify_playSimpleSounds)

        toggle("Run the DX Cluster server")
        assertTrue(s.prefs.isNotify_dxClusterServerEnabled)
        assertEquals(listOf(true), s.serverEnabled)

        typeAndCommit("Spotter callsign", "dn9apw")
        assertEquals("DN9APW", s.prefs.notify_DXCSrv_SpottersCallSign.get())

        val storedPort = s.prefs.notify_dxclusterServerPort
        typeAndCommit("TCP port", "0")
        assertEquals(1, s.refusals.size, "an impossible port was not refused")
        assertEquals(storedPort, s.prefs.notify_dxclusterServerPort)
        assertTrue(s.ports.isEmpty(), "the server was moved to a refused port")
    }

    @Test
    fun theFallbackBandAndTheTestSpotReachTheirTargets() = runComposeUiTest {
        val s = Notification()
        showTab { NotificationTab(s.state) { s.refusals += it } }

        val current = s.state.notify_optionalFrequencyPrefix
        onNodeWithText(current.prefix + " MHz").performScrollTo().performClick()
        waitForIdle()
        val other = Band.entries.first { it != current }
        onAllNodesWithText(other.prefix + " MHz").onLast().performClick()
        waitForIdle()
        assertEquals(other.prefix, s.prefs.notify_optionalFrequencyPrefix.get())

        onNodeWithText("Send a test spot").performScrollTo().performClick()
        waitForIdle()
        assertEquals("DO5AMF", s.spots.single().callSign)
        assertEquals(listOf("no logger connected"), s.refusals, "the outcome was not reported")
    }

    /** The add button asks for the callsign first; what is confirmed reaches the roster. */
    @Test
    fun aMonitoredCallsignAddedThroughTheDialogReachesTheRoster() = runComposeUiTest {
        val s = Notification()
        showTab { NotificationTab(s.state) { s.refusals += it } }

        onNodeWithText("Add monitored callsign").performScrollTo().performClick()
        waitForIdle()
        onNodeWithText("Add a callsign to the monitoring list").assertExists()

        // The dialog's field is the last text input on screen.
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("DL1ABC")
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(listOf("DL1ABC"), s.monitored.snapshot())
        assertFalse(s.refusals.isNotEmpty(), "the callsign was refused: ${s.refusals}")
    }
}
