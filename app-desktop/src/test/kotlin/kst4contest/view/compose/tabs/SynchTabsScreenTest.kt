package kst4contest.view.compose.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The three integration tabs, drawn: Log synch, TRX synch and Airscout (#107).
 *
 * All three draw from a facade with no snapshot state in it and count a revision on every
 * write so the control redraws. That device is what these tests watch: a write that
 * reached the preferences and not the screen is the class-1 fault of `83e23f55`.
 *
 * The "Choose..." button of the log file is never pressed here: it opens a modal AWT file
 * dialog, which has no place in a headless test.
 */
@OptIn(ExperimentalTestApi::class)
class SynchTabsScreenTest {

    // ------------------------------------------------------------------ Log synch

    private class LogSynch(detected: String? = "192.168.1.255") {
        val prefs = ChatPreferences().apply {
            // Both listeners default to on; start from off so a tick is a visible change.
            isLogsynch_ucxUDPWkdCallListenerEnabled = false
            isLogsynch_wintestNetworkListenerEnabled = false
        }
        var restarts = 0
        var stops = 0
        val state = LogSynchTabState(prefs, { restarts++ }, { stops++ }, { detected })
    }

    @Test
    fun logSynchShowsItsSectionsAndTheDetectedBroadcastAddress() = runComposeUiTest {
        val s = LogSynch()
        showTab { LogSynchTab(s.state) }

        onNodeWithText("File polling for worked callsigns").assertExists()
        onNodeWithText("Network-based QSO synchronization (N1MM/QARTEST/UCXLog/DXLog)").assertExists()
        onNodeWithText("Win-Test Network-Listener").assertExists()
        onNodeWithText("Choose...").assertExists()
        fieldBeside("UDP broadcast address for Win-Test (default = internet interface broadcast)")
            .assertTextEquals("192.168.1.255")
    }

    @Test
    fun logSynchWritesThroughAndStartsTheWinTestListener() = runComposeUiTest {
        val s = LogSynch()
        showTab { LogSynchTab(s.state) }

        toggle("Process QSO messages from N1MM+, QARTEST, UCXLog and DXLog.net")
        assertTrue(s.prefs.isLogsynch_ucxUDPWkdCallListenerEnabled)
        checkBeside("Process QSO messages from N1MM+, QARTEST, UCXLog and DXLog.net").assertIsOn()

        type("Shared UDP port for QSO and TRX messages [default 12060]:", "12061")
        assertEquals(12061, s.prefs.logsynch_ucxUDPWkdCallListenerPort)

        val winTestPort = "UDP-Port for Win-Test listener (default is 9871)"
        // The port field is greyed out until the listener is switched on; greyed out, it
        // offers no text input, so it is found by the port it shows.
        onNodeWithText(s.prefs.logsynch_wintestNetworkPort.toString()).assertIsNotEnabled()
        toggle("Receive Win-Test network based UDP log messages")
        assertTrue(s.prefs.isLogsynch_wintestNetworkListenerEnabled)
        assertEquals(1, s.restarts, "switching the listener on did not start it")

        type(winTestPort, "9872")
        assertEquals(9872, s.prefs.logsynch_wintestNetworkPort)

        // A refused entry is kept on screen as typed, and nothing is written.
        type(winTestPort, "98x")
        fieldBeside(winTestPort).assertTextEquals("98x")
        assertEquals(9872, s.prefs.logsynch_wintestNetworkPort)

        type("KST station name in Win-Test network (src of SKED packets)", " KST2 ")
        assertEquals("KST2", s.prefs.logsynch_wintestNetworkStationNameOfKST)
    }

    // ------------------------------------------------------------------ TRX synch

    @Test
    fun trxSynchWritesThroughAndSwitchesTheQrgFollower() = runComposeUiTest {
        val prefs = ChatPreferences()
        val follower = mutableListOf<Boolean>()
        val state = TrxSynchTabState(prefs) { follower += it }
        showTab { TrxSynchTab(state) }

        onNodeWithText("Receive UCXLog TRX info").assertExists()
        onNodeWithText("What an enabled source does").assertExists()

        toggle("Update MYQRG from RadioInfo messages received on the shared log-sync port")
        assertTrue(prefs.isTrxSynch_ucxLogUDPListenerEnabled)
        assertEquals(listOf(true), follower)

        toggle("Use pass frequency from Win-Test STATUS (instead of own QRG)")
        assertTrue(prefs.isLogsynch_wintestUsePassQrg)

        type("Win-Test station name filter (e.g. STN1, empty = accept all)", "STN1 ")
        assertEquals("STN1", prefs.logsynch_wintestNetworkStationNameOfWintestClient1)
    }

    // ------------------------------------------------------------------ Airscout

    @Test
    fun airscoutWritesValidEntriesAndKeepsAnInvalidOneOffThePreferences() = runComposeUiTest {
        // The link defaults to on; start from off so a tick is a visible change.
        val prefs = ChatPreferences().apply { isAirScout_asUDPListenerEnabled = false }
        val state = AirscoutTabState(prefs)
        showTab { AirscoutTab(state) }

        onNodeWithText("AirScout UDP communication").assertExists()
        onNodeWithText("Queried frequency").assertExists()

        toggle("Enable AirScout UDP integration (AirScout 0.9.9.5 or newer)")
        assertTrue(prefs.isAirScout_asUDPListenerEnabled)

        val port = "AirScout UDP port [9872] — reconnect after changing:"
        type(port, "9873")
        assertEquals(9873, prefs.airScout_asCommunicationPort)

        type(port, "70000")
        fieldBeside(port).assertTextEquals("70000")
        assertEquals(9873, prefs.airScout_asCommunicationPort, "an out-of-range port was written")

        type("AirScout server identifier [AS]:", "AS2")
        assertEquals("AS2", prefs.airScout_asServerNameString)
    }
}
