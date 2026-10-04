package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class TrxSynchTabStateTest {

    private class Session {
        val prefs = ChatPreferences()
        val followerCalls = mutableListOf<Boolean>()
        val state by lazy { TrxSynchTabState(prefs) { followerCalls.add(it) } }
    }

    @Test
    fun `reading comes straight from the preferences`() {
        val s = Session()
        s.prefs.setTrxSynch_ucxLogUDPListenerEnabled(true)
        s.prefs.setLogsynch_wintestQrgSyncEnabled(true)
        s.prefs.setLogsynch_wintestUsePassQrg(true)
        s.prefs.setLogsynch_wintestNetworkStationNameOfWintestClient1("STN1")

        assertTrue(s.state.trxSynch_ucxLogUDPListenerEnabled)
        assertTrue(s.state.logsynch_wintestQrgSyncEnabled)
        assertTrue(s.state.logsynch_wintestUsePassQrg)
        assertEquals("STN1", s.state.logsynch_wintestNetworkStationNameOfWintestClient1)
    }

    @Test
    fun `writing reaches the preferences at once, without a confirmation step`() {
        val s = Session()

        s.state.trxSynch_ucxLogUDPListenerEnabled = true
        s.state.logsynch_wintestQrgSyncEnabled = true
        s.state.logsynch_wintestUsePassQrg = true

        assertTrue(s.prefs.isTrxSynch_ucxLogUDPListenerEnabled())
        assertTrue(s.prefs.isLogsynch_wintestQrgSyncEnabled())
        assertTrue(s.prefs.isLogsynch_wintestUsePassQrg())
    }

    @Test
    fun `the Win-Test station name filter is stored trimmed, and an empty filter is allowed`() {
        // Empty means "accept all" — the JavaFX label said so explicitly.
        val s = Session()

        s.state.logsynch_wintestNetworkStationNameOfWintestClient1 = "  STN1 "
        assertEquals("STN1", s.prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1())

        s.state.logsynch_wintestNetworkStationNameOfWintestClient1 = "   "
        assertEquals("", s.prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1())
    }

    @Test
    fun `the own-QRG follower follows whether any source is enabled`() {
        val s = Session()

        s.state.trxSynch_ucxLogUDPListenerEnabled = true
        assertEquals(listOf(true), s.followerCalls)

        s.state.logsynch_wintestQrgSyncEnabled = true
        assertEquals(listOf(true, true), s.followerCalls)

        // One source off is not all sources off: the follower must stay attached.
        s.state.trxSynch_ucxLogUDPListenerEnabled = false
        assertEquals(listOf(true, true, true), s.followerCalls)

        s.state.logsynch_wintestQrgSyncEnabled = false
        assertEquals(listOf(true, true, true, false), s.followerCalls)
    }

    @Test
    fun `an enabled source alone is never presented as a frequency`() {
        // PROJECT_CONTEXT: automatic QRG updates need an enabled source AND valid
        // incoming RadioInfo or Win-Test STATUS data. The tab must not let an
        // operator read "source on" as "my QRG is being kept current".
        val s = Session()

        s.state.trxSynch_ucxLogUDPListenerEnabled = true

        assertTrue(s.state.anyQrgSourceEnabled)
        val hint = TrxSynchTabState.QRG_SOURCE_HINT
        assertTrue(hint.contains("valid", ignoreCase = true) && hint.contains("data", ignoreCase = true),
            "the hint must say that incoming data is required as well, not just a source: <$hint>")
        assertTrue(hint.contains("RadioInfo") && hint.contains("STATUS"),
            "naming both sources tells the operator what data he is waiting for: <$hint>")
    }

    @Test
    fun `no enabled source means no automatic QRG at all`() {
        val s = Session()

        /*
         * Both sources have to be switched off explicitly: ChatPreferences ships
         * logsynch_wintestQrgSyncEnabled = true (ChatPreferences.java:258), so a fresh
         * configuration already has one source enabled.
         */
        s.state.logsynch_wintestQrgSyncEnabled = false
        s.state.trxSynch_ucxLogUDPListenerEnabled = false

        assertFalse(s.state.anyQrgSourceEnabled)
    }

    @Test
    fun `a fresh configuration already has the Win-Test source enabled`() {
        // Pins the shipped default rather than assuming it, which is what made the
        // test above wrong on the first attempt.
        assertTrue(Session().state.logsynch_wintestQrgSyncEnabled)
        assertFalse(Session().state.trxSynch_ucxLogUDPListenerEnabled)
    }
}
