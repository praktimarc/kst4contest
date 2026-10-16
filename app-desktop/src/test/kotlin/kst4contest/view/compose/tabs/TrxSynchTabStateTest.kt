package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.SettingsDraft
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TrxSynchTabStateTest(@TempDir private val tempDir: Path) {

    private inner class Session {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val followerCalls = mutableListOf<Boolean>()
        val draft = SettingsDraft()
        val secondRotatorCalls = mutableListOf<Unit>()
        val state by lazy {
            TrxSynchTabState(prefs, draft, applySecondRotator = { secondRotatorCalls.add(Unit) }) { followerCalls.add(it) }
        }
    }

    @Test
    fun `the SO2R switch and the second Win-Test station wait for apply and restart rotator 2 once`() {
        val s = Session()

        s.state.trxSynch_so2rEnabled = true
        s.state.logsynch_wintestNetworkStationNameOfWintestClient2 = "  STN2 "

        assertFalse(s.prefs.isTrxSynch_so2rEnabled())
        s.draft.apply()

        assertTrue(s.prefs.isTrxSynch_so2rEnabled())
        assertEquals("STN2", s.prefs.getLogsynch_wintestNetworkStationNameOfWintestClient2())
        assertEquals(1, s.secondRotatorCalls.size)
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
    fun `an edit waits for apply`() {
        val s = Session()

        s.state.trxSynch_ucxLogUDPListenerEnabled = true
        s.state.logsynch_wintestUsePassQrg = true

        assertFalse(s.prefs.isTrxSynch_ucxLogUDPListenerEnabled(), "an edit took effect before Apply")
        assertTrue(s.state.trxSynch_ucxLogUDPListenerEnabled, "the tab shows the edit")
        assertTrue(s.followerCalls.isEmpty(), "the follower moved before Apply")

        s.draft.apply()

        assertTrue(s.prefs.isTrxSynch_ucxLogUDPListenerEnabled())
        assertTrue(s.prefs.isLogsynch_wintestUsePassQrg())
    }

    @Test
    fun `a discarded edit changes nothing`() {
        val s = Session()

        s.state.trxSynch_ucxLogUDPListenerEnabled = true
        s.draft.discard()

        assertFalse(s.prefs.isTrxSynch_ucxLogUDPListenerEnabled())
        assertTrue(s.followerCalls.isEmpty())
    }

    @Test
    fun `the Win-Test station name filter is stored trimmed, and an empty filter is allowed`() {
        // Empty means "accept all" — the JavaFX label said so explicitly.
        val s = Session()

        s.state.logsynch_wintestNetworkStationNameOfWintestClient1 = "  STN1 "
        s.draft.apply()
        assertEquals("STN1", s.prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1())

        s.state.logsynch_wintestNetworkStationNameOfWintestClient1 = "   "
        s.draft.apply()
        assertEquals("", s.prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1())
    }

    @Test
    fun `the own-QRG follower follows whether any source is enabled, once per apply`() {
        val s = Session()

        // Both sources in one apply: one call, with the finished state.
        s.state.trxSynch_ucxLogUDPListenerEnabled = true
        s.state.logsynch_wintestQrgSyncEnabled = false
        s.draft.apply()
        assertEquals(listOf(true), s.followerCalls)

        // One source off is not all sources off: the follower must stay attached.
        s.state.logsynch_wintestQrgSyncEnabled = true
        s.draft.apply()
        s.state.trxSynch_ucxLogUDPListenerEnabled = false
        s.draft.apply()
        assertEquals(listOf(true, true, true), s.followerCalls)

        s.state.logsynch_wintestQrgSyncEnabled = false
        s.draft.apply()
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
        /*
         * Read from the translation rather than from a constant beside it: one sentence, one
         * place. What this asserts is a content requirement on the English text, which is
         * where that text now lives.
         */
        val hint = kst4contest.view.i18n.Translations.BY_LANGUAGE
            .getValue(kst4contest.view.i18n.Translations.BASE_LANGUAGE)
            .getValue(TrxSynchTabState.QRG_SOURCE_HINT_KEY)
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
