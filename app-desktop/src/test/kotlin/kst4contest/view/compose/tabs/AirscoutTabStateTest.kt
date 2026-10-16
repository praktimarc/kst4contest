package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.ImmediateDraft
import kst4contest.view.compose.SettingsDraft
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AirscoutTabStateTest(@TempDir private val tempDir: Path) {

    /*
     * The rules below are pinned with a draft that applies at once; what the draft itself
     * changes is pinned by the two tests that pass a real one.
     */
    private inner class Session(val draft: SettingsDraft = ImmediateDraft()) {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val state by lazy { AirscoutTabState(prefs, draft) }
    }

    @Test
    fun `reading comes straight from the preferences`() {
        val s = Session()
        s.prefs.setAirScout_asUDPListenerEnabled(true)
        s.prefs.setAirScout_asServerNameString("AS")
        s.prefs.setAirScout_asClientNameString("KST")
        s.prefs.setAirScout_asCommunicationPort(9872)
        s.prefs.setAirScout_autoBandSelectionEnabled(true)
        s.prefs.setAirScout_asBandString("1440000")

        assertTrue(s.state.airScout_asUDPListenerEnabled)
        assertEquals("AS", s.state.airScout_asServerNameString)
        assertEquals("KST", s.state.airScout_asClientNameString)
        assertEquals(9872, s.state.airScout_asCommunicationPort)
        assertTrue(s.state.airScout_autoBandSelectionEnabled)
        assertEquals("1440000", s.state.airScout_asBandString)
    }

    @Test
    fun `an edit waits for apply`() {
        val s = Session(SettingsDraft())
        s.prefs.setAirScout_asCommunicationPort(9872)

        s.state.airScout_asCommunicationPort = 9873
        s.state.airScout_asServerNameString = " AS3 "

        assertEquals(9872, s.prefs.getAirScout_asCommunicationPort(), "an edit took effect before Apply")
        assertEquals(9873, s.state.airScout_asCommunicationPort, "the tab shows the edit")
        assertEquals("AS3", s.state.airScout_asServerNameString, "the draft holds the trimmed value")

        s.draft.apply()

        assertEquals(9873, s.prefs.getAirScout_asCommunicationPort())
        assertEquals("AS3", s.prefs.getAirScout_asServerNameString())
    }

    @Test
    fun `a discarded edit changes nothing, and an invalid one never enters the draft`() {
        val s = Session(SettingsDraft())
        s.prefs.setAirScout_asBandString("1440000")

        s.state.airScout_asBandString = "-1"
        assertFalse(s.draft.hasPendingChanges, "a refused value reached the draft")

        s.state.airScout_asBandString = "4320000"
        s.draft.discard()

        assertEquals("1440000", s.prefs.getAirScout_asBandString())
    }

    @Test
    fun `writing reaches the preferences on apply`() {
        val s = Session()

        s.state.airScout_asUDPListenerEnabled = true
        s.state.airScout_asServerNameString = "AS2"
        s.state.airScout_asClientNameString = "KST2"
        s.state.airScout_asCommunicationPort = 9873
        s.state.airScout_autoBandSelectionEnabled = true
        s.state.airScout_asBandString = "4320000"

        assertTrue(s.prefs.isAirScout_asUDPListenerEnabled())
        assertEquals("AS2", s.prefs.getAirScout_asServerNameString())
        assertEquals("KST2", s.prefs.getAirScout_asClientNameString())
        assertEquals(9873, s.prefs.getAirScout_asCommunicationPort())
        assertTrue(s.prefs.isAirScout_autoBandSelectionEnabled())
        assertEquals("4320000", s.prefs.getAirScout_asBandString())
    }

    @Test
    fun `identifiers are stored trimmed`() {
        val s = Session()

        s.state.airScout_asServerNameString = "  AS  "
        s.state.airScout_asClientNameString = " KST "

        assertEquals("AS", s.prefs.getAirScout_asServerNameString())
        assertEquals("KST", s.prefs.getAirScout_asClientNameString())
    }

    @Test
    fun `an invalid identifier keeps the configured one instead of falling back`() {
        // The AirScout protocol carries these identifiers inside quoted fields:
        // empty, quotation marks and line breaks are what the JavaFX window rejected.
        // Guarding before the write matters because the ChatPreferences setter does
        // not keep the old value on bad input, it substitutes the factory default —
        // an unguarded keystroke would silently reset the operator's identifier.
        val s = Session()
        s.prefs.setAirScout_asServerNameString("AS7")
        s.prefs.setAirScout_asClientNameString("KST7")

        for (rejected in listOf("", "   ", "A\"S", "A\rS", "A\nS")) {
            s.state.airScout_asServerNameString = rejected
            s.state.airScout_asClientNameString = rejected
            assertFalse(AirscoutTabState.isValidIdentifier(rejected), "must be rejected: <$rejected>")
        }

        assertEquals("AS7", s.prefs.getAirScout_asServerNameString())
        assertEquals("KST7", s.prefs.getAirScout_asClientNameString())
    }

    @Test
    fun `a port outside the accepted range keeps the configured one`() {
        val s = Session()
        s.prefs.setAirScout_asCommunicationPort(9999)

        s.state.airScout_asCommunicationPort = 0
        s.state.airScout_asCommunicationPort = 65536

        assertEquals(9999, s.prefs.getAirScout_asCommunicationPort(),
            "the ChatPreferences setter would have substituted 9872 here")

        s.state.airScout_asCommunicationPort = 1
        assertEquals(1, s.prefs.getAirScout_asCommunicationPort())

        s.state.airScout_asCommunicationPort = 65535
        assertEquals(65535, s.prefs.getAirScout_asCommunicationPort(),
            "the JavaFX field accepted 1..65535; the range must not get narrower")
    }

    @Test
    fun `the band value is stored as the protocol spells it`() {
        val s = Session()
        s.prefs.setAirScout_asBandString("1440000")

        s.state.airScout_asBandString = " 04320000 "
        assertEquals("4320000", s.prefs.getAirScout_asBandString(),
            "the JavaFX field stored Long.toString of the parsed value")

        s.state.airScout_asBandString = "0"
        s.state.airScout_asBandString = "-1"
        s.state.airScout_asBandString = "144 MHz"
        assertEquals("4320000", s.prefs.getAirScout_asBandString(),
            "a value that is not a positive number was rejected, not stored — the "
                + "ChatPreferences setter would have substituted 1440000 here")
    }

    @Test
    fun `the forced band value is only editable while automatic selection is off`() {
        val s = Session()

        s.state.airScout_autoBandSelectionEnabled = true
        assertFalse(s.state.bandValueEditable,
            "the JavaFX band field was disabled while auto per station was selected")

        s.state.airScout_autoBandSelectionEnabled = false
        assertTrue(s.state.bandValueEditable)
    }
}
