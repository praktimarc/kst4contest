package kst4contest.view.compose.tabs

import kst4contest.model.Band
import kst4contest.model.ChatMember
import kst4contest.model.ChatPreferences
import kst4contest.observe.SimpleRoster
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

class NotificationTabStateTest {

    private class Session {
        val prefs = ChatPreferences()
        val monitored = SimpleRoster<String>()
        var serverEnabledApplied: Boolean? = null
        var portApplied: Int? = null
        var spotSent: ChatMember? = null
        var spotRefusal: String? = null

        fun state() = NotificationTabState(
            prefs,
            monitored,
            applyDxClusterServerEnabled = { serverEnabledApplied = it },
            applyDxClusterServerPort = { portApplied = it },
            broadcastTestSpot = { spotSent = it; spotRefusal },
        )
    }

    @Test
    fun `the sound settings read from and write to the preferences at once`() {
        val s = Session()
        s.prefs.setNotify_playSimpleSounds(true)
        val state = s.state()

        assertTrue(state.notify_playSimpleSounds)

        state.notify_playSimpleSounds = false
        state.notify_playCWCallsignsOnRxedPMs = true
        state.notify_playVoiceCallsignsOnRxedPMs = true

        assertFalse(s.prefs.isNotify_playSimpleSounds())
        assertTrue(s.prefs.isNotify_playCWCallsignsOnRxedPMs())
        assertTrue(s.prefs.isNotify_playVoiceCallsignsOnRxedPMs())
    }

    @Test
    fun `the band-upgrade hints write through`() {
        val s = Session()
        val state = s.state()

        state.notify_bandUpgradeHintOnLogEnabled = true
        state.notify_bandUpgradePriorityBoostEnabled = true

        assertTrue(s.prefs.isNotify_bandUpgradeHintOnLogEnabled())
        assertTrue(s.prefs.isNotify_bandUpgradePriorityBoostEnabled())
    }

    @Test
    fun `enabling the DX cluster server also tells the running session`() {
        val s = Session()
        val state = s.state()

        state.enableDxClusterServer(true)

        assertTrue(s.prefs.isNotify_dxClusterServerEnabled())
        assertEquals(true, s.serverEnabledApplied,
            "the JavaFX checkbox started or stopped the server as well, and the "
                + "settings coverage test cannot see that call")
    }

    @Test
    fun `a valid port is stored and handed to the running session`() {
        val s = Session()
        val state = s.state()

        assertNull(state.commitDxClusterServerPort("7300"))

        assertEquals(7300, s.prefs.getNotify_dxclusterServerPort())
        assertEquals(7300, s.portApplied)
    }

    @Test
    fun `a port outside the TCP range is refused and nothing is written`() {
        val s = Session()
        s.prefs.setNotify_dxclusterServerPort(8000)
        val state = s.state()

        val refusal = state.commitDxClusterServerPort("70000")

        assertNotNull(refusal, "the JavaFX field alerted and restored the previous port")
        assertEquals(8000, s.prefs.getNotify_dxclusterServerPort())
        assertNull(s.portApplied, "an refused port must not restart the server")
    }

    @Test
    fun `a port that is not a number is refused`() {
        val s = Session()
        s.prefs.setNotify_dxclusterServerPort(8000)
        val state = s.state()

        assertNotNull(state.commitDxClusterServerPort("acht"))
        assertEquals(8000, s.prefs.getNotify_dxclusterServerPort())
    }

    @Test
    fun `an unchanged port is stored but does not restart the server`() {
        val s = Session()
        s.prefs.setNotify_dxclusterServerPort(8000)
        val state = s.state()

        assertNull(state.commitDxClusterServerPort("8000"))

        assertNull(s.portApplied,
            "the JavaFX field only restarted the server when the port actually changed")
    }

    @Test
    fun `a spotter callsign is upper-cased and stored`() {
        val s = Session()
        val state = s.state()

        assertNull(state.commitSpotterCallSign(" do5amf "))

        assertEquals("DO5AMF", s.prefs.getNotify_DXCSrv_SpottersCallSign().get())
        assertEquals("DO5AMF", state.notify_DXCSrv_SpottersCallSign)
    }

    @Test
    fun `a spotter callsign that is no callsign is refused`() {
        val s = Session()
        s.prefs.setNotify_DXCSrv_SpottersCallSign("DO5AMF")
        val state = s.state()

        assertNotNull(state.commitSpotterCallSign("not a call"))

        assertEquals("DO5AMF", s.prefs.getNotify_DXCSrv_SpottersCallSign().get())
    }

    @Test
    fun `the fallback band round trips through the stored prefix`() {
        val s = Session()
        val state = s.state()

        state.notify_optionalFrequencyPrefix = Band.B_432

        assertEquals("432", s.prefs.getNotify_optionalFrequencyPrefix().get())
        assertEquals(Band.B_432, state.notify_optionalFrequencyPrefix)
    }

    @Test
    fun `an unsupported stored prefix falls back to 144 and is written back`() {
        val s = Session()
        s.prefs.setNotify_optionalFrequencyPrefix("4711")

        val state = s.state()

        assertEquals(Band.B_144, state.notify_optionalFrequencyPrefix)
        assertEquals("144", s.prefs.getNotify_optionalFrequencyPrefix().get(),
            "the JavaFX window repaired the stored value the same way")
    }

    @Test
    fun `the monitoring list starts from the roster`() {
        val s = Session()
        s.monitored.setAll(listOf("DN9APW", "DO5AMF"))

        assertEquals(listOf("DN9APW", "DO5AMF"), s.state().monitoredCallSigns.texts)
    }

    @Test
    fun `a monitored callsign is appended as its base call`() {
        val s = Session()
        val state = s.state()

        assertNull(state.addMonitoredCallSign(" dn9apw-70 "))

        assertEquals(listOf("DN9APW"), state.monitoredCallSigns.texts,
            "monitoring works on the base call, so every SSID maps to the same entry")
        assertEquals(listOf("DN9APW"), s.monitored.snapshot(),
            "the JavaFX button added to the roster at once")
    }

    @Test
    fun `a monitored callsign that is no callsign is refused`() {
        val s = Session()
        val state = s.state()

        assertNotNull(state.addMonitoredCallSign("hello"))

        assertTrue(state.monitoredCallSigns.texts.isEmpty())
    }

    @Test
    fun `the same base call is not monitored twice`() {
        val s = Session()
        val state = s.state()
        state.addMonitoredCallSign("DN9APW")

        assertNotNull(state.addMonitoredCallSign("dn9apw-2"))

        assertEquals(listOf("DN9APW"), state.monitoredCallSigns.texts)
    }

    @Test
    fun `editing a monitored entry normalizes it and reaches the roster`() {
        val s = Session()
        s.monitored.setAll(listOf("DN9APW"))
        val state = s.state()

        state.monitoredCallSigns.commitAt(0, "do5amf-2")
        state.commitMonitoredCallSigns()

        assertEquals(listOf("DO5AMF"), s.monitored.snapshot())
    }

    @Test
    fun `an invalid edit of a monitored entry is refused and the roster is untouched`() {
        val s = Session()
        s.monitored.setAll(listOf("DN9APW"))
        val state = s.state()

        val outcome = state.monitoredCallSigns.commitAt(0, "still not a call")
        state.commitMonitoredCallSigns()

        assertTrue(outcome is kst4contest.view.compose.CommitOutcome.Refused)
        assertEquals(listOf("DN9APW"), s.monitored.snapshot())
    }

    @Test
    fun `clearing a monitored entry removes it from the roster`() {
        val s = Session()
        s.monitored.setAll(listOf("DN9APW", "DO5AMF"))
        val state = s.state()
        val rosterBefore = s.monitored

        state.monitoredCallSigns.commitAt(0, "")
        state.commitMonitoredCallSigns()

        assertEquals(listOf("DO5AMF"), s.monitored.snapshot())
        assertSame(rosterBefore, s.monitored,
            "the roster is filled, never replaced: a swap would orphan its listeners")
    }

    @Test
    fun `the test spot carries the values the JavaFX button used`() {
        val s = Session()
        val state = s.state()

        assertNull(state.sendTestSpot())

        val spot = s.spotSent
        assertNotNull(spot)
        assertEquals("DO5AMF", spot!!.callSign)
        assertEquals("300", spot.frequency.get())
        assertEquals("DXC test: You donated \$100!", spot.qra,
            "deliberate test data, kept verbatim")
    }

    @Test
    fun `a refused test spot reports why`() {
        val s = Session()
        s.spotRefusal = "No DX Cluster client is connected to KST4Contest."
        val state = s.state()

        assertEquals("No DX Cluster client is connected to KST4Contest.", state.sendTestSpot())
    }
}
