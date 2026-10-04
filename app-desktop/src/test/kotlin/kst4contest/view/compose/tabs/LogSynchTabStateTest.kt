package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class LogSynchTabStateTest {

    private class Session(detected: String? = null) {
        val prefs = ChatPreferences()
        var restarts = 0
        var stops = 0
        val detectedBroadcast = detected

        fun state() = LogSynchTabState(
            prefs,
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
    fun `writing reaches the preferences at once, without a confirmation step`() {
        // The JavaFX window had no value-collecting confirmation: each control wrote
        // straight into ChatPreferences, and "Save settings" only persisted them.
        val s = Session()
        val state = s.state()

        state.logsynch_fileBasedWkdCallInterpreterEnabled = true
        state.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = "/home/op/log.adi"
        state.logsynch_ucxUDPWkdCallListenerEnabled = true
        state.logsynch_ucxUDPWkdCallListenerPort = 12060
        state.logsynch_wintestNetworkPort = 9871

        assertTrue(s.prefs.isLogsynch_fileBasedWkdCallInterpreterEnabled())
        assertEquals("/home/op/log.adi", s.prefs.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly())
        assertTrue(s.prefs.isLogsynch_ucxUDPWkdCallListenerEnabled())
        assertEquals(12060, s.prefs.getLogsynch_ucxUDPWkdCallListenerPort())
        assertEquals(9871, s.prefs.getLogsynch_wintestNetworkPort())
    }

    @Test
    fun `the Win-Test station name and broadcast address are stored trimmed`() {
        // The JavaFX fields committed getText().trim(); a stray space in a station
        // name or a broadcast address is a protocol value, not decoration.
        val s = Session()
        val state = s.state()

        state.logsynch_wintestNetworkStationNameOfKST = "  KST  "
        state.logsynch_wintestNetworkBroadcastAddress = " 192.168.1.255 "

        assertEquals("KST", s.prefs.getLogsynch_wintestNetworkStationNameOfKST())
        assertEquals("192.168.1.255", s.prefs.getLogsynch_wintestNetworkBroadcastAddress())
    }

    @Test
    fun `enabling the Win-Test listener starts it and disabling stops it`() {
        val s = Session()
        val state = s.state()

        state.logsynch_wintestNetworkListenerEnabled = true

        assertTrue(s.prefs.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals(1, s.restarts, "the running listener must follow the setting")

        state.logsynch_wintestNetworkListenerEnabled = false

        assertFalse(s.prefs.isLogsynch_wintestNetworkListenerEnabled())
        assertEquals(1, s.stops)
    }

    @Test
    fun `a committed port change rebinds the listener only while it is enabled`() {
        // Deliberately not on every keystroke: the port write goes through at once,
        // but rebinding a UDP socket is bound to the commit, as the JavaFX field's
        // focus-lost handler did.
        val s = Session()
        val state = s.state()

        /*
         * Switched off explicitly: ChatPreferences ships
         * logsynch_wintestNetworkListenerEnabled = true (ChatPreferences.java:254), so
         * a fresh configuration already listens.
         */
        state.logsynch_wintestNetworkListenerEnabled = false
        val restartsAfterDisable = s.restarts

        state.logsynch_wintestNetworkPort = 9872
        assertEquals(restartsAfterDisable, s.restarts, "a keystroke must not rebind the socket")

        state.commitWintestNetworkPort()
        assertEquals(restartsAfterDisable, s.restarts, "a disabled listener has nothing to rebind")

        state.logsynch_wintestNetworkListenerEnabled = true
        val restartsAfterEnable = s.restarts
        state.logsynch_wintestNetworkPort = 9873
        state.commitWintestNetworkPort()

        assertEquals(9873, s.prefs.getLogsynch_wintestNetworkPort())
        assertEquals(restartsAfterEnable + 1, s.restarts)
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
