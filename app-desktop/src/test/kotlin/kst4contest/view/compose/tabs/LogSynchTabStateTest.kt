package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.SettingsDraft
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LogSynchTabStateTest(@TempDir private val tempDir: Path) {

    private inner class Session(detected: String? = null) {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        var restarts = 0
        var stops = 0
        val detectedBroadcast = detected
        val draft = SettingsDraft()

        fun state() = LogSynchTabState(
            prefs,
            draft,
            restartWintestListener = { restarts++ },
            stopWintestListener = { stops++ },
            detectWintestBroadcastAddress = { detectedBroadcast },
        )
    }

    @Test
    fun `reading comes straight from the preferences`() {
        val s = Session()
        s.prefs.setLogsynch_fileBasedWkdCallInterpreterEnabled(true)
        s.prefs.setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly("/tmp/wkd.txt")
        s.prefs.setLogsynch_ucxUDPWkdCallListenerEnabled(true)
        s.prefs.setLogsynch_ucxUDPWkdCallListenerPort(12060)
        s.prefs.setLogsynch_wintestNetworkPort(9871)
        s.prefs.setLogsynch_wintestNetworkStationNameOfKST("KST")
        val state = s.state()

        assertTrue(state.logsynch_fileBasedWkdCallInterpreterEnabled)
        assertEquals("/tmp/wkd.txt", state.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly)
        assertTrue(state.logsynch_ucxUDPWkdCallListenerEnabled)
        assertEquals(12060, state.logsynch_ucxUDPWkdCallListenerPort)
        assertEquals(9871, state.logsynch_wintestNetworkPort)
        assertEquals("KST", state.logsynch_wintestNetworkStationNameOfKST)
    }

    @Test
    fun `an edit waits for apply, the chosen log file does not`() {
        val s = Session()
        s.prefs.setLogsynch_ucxUDPWkdCallListenerPort(12060)
        s.prefs.setLogsynch_fileBasedWkdCallInterpreterEnabled(false)
        val state = s.state()

        state.logsynch_fileBasedWkdCallInterpreterEnabled = true
        state.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = "/home/op/log.adi"
        state.logsynch_ucxUDPWkdCallListenerEnabled = true
        state.logsynch_ucxUDPWkdCallListenerPort = 12061
        state.logsynch_wintestNetworkPort = 9879

        // The file chooser is an action: the chosen path is written when chosen.
        assertEquals("/home/op/log.adi", s.prefs.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly())
        assertFalse(s.prefs.isLogsynch_fileBasedWkdCallInterpreterEnabled(), "an edit took effect before Apply")
        assertEquals(12060, s.prefs.getLogsynch_ucxUDPWkdCallListenerPort())
        assertEquals(12061, state.logsynch_ucxUDPWkdCallListenerPort, "the tab shows the edit")

        s.draft.apply()

        assertTrue(s.prefs.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertTrue(s.prefs.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertEquals(12061, s.prefs.getLogsynch_ucxUDPWkdCallListenerPort())
        assertEquals(9879, s.prefs.getLogsynch_wintestNetworkPort())
    }

    @Test
    fun `a discarded edit changes nothing and touches no listener`() {
        val s = Session()
        val state = s.state()
        val enabled = s.prefs.isLogsynch_wintestNetworkListenerEnabled()

        state.logsynch_wintestNetworkListenerEnabled = !enabled
        state.logsynch_wintestNetworkPort = 9879
        s.draft.discard()

        assertEquals(enabled, s.prefs.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals(0, s.restarts + s.stops)
    }

    @Test
    fun `the Win-Test station name and broadcast address are stored trimmed`() {
        // The JavaFX fields committed getText().trim(); a stray space in a station
        // name or a broadcast address is a protocol value, not decoration.
        val s = Session()
        s.prefs.setLogsynch_wintestNetworkStationNameOfKST("KST")
        val state = s.state()

        state.logsynch_wintestNetworkStationNameOfKST = "KST "
        assertFalse(s.draft.hasPendingChanges, "a trailing space is no change")

        state.logsynch_wintestNetworkStationNameOfKST = "  KST2  "
        state.logsynch_wintestNetworkBroadcastAddress = " 192.168.1.255 "
        s.draft.apply()

        assertEquals("KST2", s.prefs.getLogsynch_wintestNetworkStationNameOfKST())
        assertEquals("192.168.1.255", s.prefs.getLogsynch_wintestNetworkBroadcastAddress())
    }

    @Test
    fun `applying the Win-Test switch starts or stops the listener`() {
        val s = Session()
        s.prefs.setLogsynch_wintestNetworkListenerEnabled(false)
        val state = s.state()

        state.logsynch_wintestNetworkListenerEnabled = true
        assertEquals(0, s.restarts, "the listener followed the box before Apply")
        s.draft.apply()

        assertTrue(s.prefs.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals(1, s.restarts, "the running listener must follow the setting")

        state.logsynch_wintestNetworkListenerEnabled = false
        s.draft.apply()

        assertFalse(s.prefs.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals(1, s.stops)
    }

    @Test
    fun `an applied port change rebinds the listener only while it is enabled`() {
        val s = Session()
        /*
         * Switched off explicitly: ChatPreferences ships
         * logsynch_wintestNetworkListenerEnabled = true (ChatPreferences.java:254), so
         * a fresh configuration already listens.
         */
        s.prefs.setLogsynch_wintestNetworkListenerEnabled(false)
        val state = s.state()

        state.logsynch_wintestNetworkPort = 9872
        s.draft.apply()
        assertEquals(0, s.restarts, "a disabled listener has nothing to rebind")

        s.prefs.setLogsynch_wintestNetworkListenerEnabled(true)
        state.logsynch_wintestNetworkPort = 9873
        assertEquals(0, s.restarts, "a keystroke must not rebind the socket")
        s.draft.apply()

        assertEquals(9873, s.prefs.getLogsynch_wintestNetworkPort())
        assertEquals(1, s.restarts)
    }

    /*
     * Switch and port in one apply: one restart, on the new port. Two restarts in a row can
     * fail on the port the first has not released yet.
     */
    @Test
    fun `switch and port applied together rebind once, on the new port`() {
        val s = Session()
        s.prefs.setLogsynch_wintestNetworkListenerEnabled(false)
        var portAtRestart = -1
        val state = LogSynchTabState(
            s.prefs, s.draft,
            restartWintestListener = { s.restarts++; portAtRestart = s.prefs.getLogsynch_wintestNetworkPort() },
            stopWintestListener = { s.stops++ },
            detectWintestBroadcastAddress = { null },
        )

        state.logsynch_wintestNetworkListenerEnabled = true
        state.logsynch_wintestNetworkPort = 9874
        s.draft.apply()

        assertEquals(1, s.restarts)
        assertEquals(9874, portAtRestart)
    }

    @Test
    fun `a still-default broadcast address is replaced by the detected one`() {
        val s = Session(detected = "192.168.178.255")
        s.prefs.setLogsynch_wintestNetworkBroadcastAddress("255.255.255.255")

        s.state()

        assertEquals("192.168.178.255", s.prefs.getLogsynch_wintestNetworkBroadcastAddress(),
            "the JavaFX window auto-detected the subnet broadcast while the stored "
                + "value was still the catch-all default")
    }

    @Test
    fun `a broadcast address the operator chose is never overwritten`() {
        val s = Session(detected = "192.168.178.255")
        s.prefs.setLogsynch_wintestNetworkBroadcastAddress("10.0.0.255")

        s.state()

        assertEquals("10.0.0.255", s.prefs.getLogsynch_wintestNetworkBroadcastAddress())
    }

    @Test
    fun `a failed detection leaves the default in place`() {
        val s = Session(detected = null)
        s.prefs.setLogsynch_wintestNetworkBroadcastAddress("255.255.255.255")

        s.state()

        assertEquals("255.255.255.255", s.prefs.getLogsynch_wintestNetworkBroadcastAddress())
    }
}
