package kst4contest.view.compose.tabs

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import kst4contest.view.compose.SettingsDraft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StationTabStateTest(@TempDir private val tempDir: Path) {

    private inner class Session {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        var mainApplied: ChatCategory? = null
        var secondApplied: ChatCategory? = null
        var secondWasApplied = false
        val draft = SettingsDraft()
        val state = StationTabState(
            prefs,
            draft,
            { mainApplied = it },
            { secondApplied = it; secondWasApplied = true },
        )
    }

    @Test
    fun `reading comes straight from the preferences`() {
        val s = Session()
        s.prefs.setStn_loginCallSign("DN9APW")
        s.prefs.setStn_on4kstServersPort(23001)

        assertEquals("DN9APW", s.state.stn_loginCallSign)
        assertEquals(23001, s.state.stn_on4kstServersPort)
    }

    @Test
    fun `a station setting waits in the draft until it is applied`() {
        val s = Session()

        s.state.stn_pstRotatorEnabled = true
        s.state.stn_qtfDefault = 135.0
        s.state.stn_on4kstServersPort = 23002

        assertFalse(s.prefs.isStn_pstRotatorEnabled(), "an edit took effect before Apply")
        assertNotEquals(23002, s.prefs.getStn_on4kstServersPort())
        assertTrue(s.state.stn_pstRotatorEnabled, "the tab shows the edited value")
        assertEquals(135.0, s.state.stn_qtfDefault, 0.001)

        s.draft.apply()

        assertTrue(s.prefs.isStn_pstRotatorEnabled())
        assertEquals(135.0, s.prefs.getStn_qtfDefault(), 0.001)
        assertEquals(23002, s.prefs.getStn_on4kstServersPort())
    }

    @Test
    fun `a discarded station setting leaves the preferences alone`() {
        val s = Session()
        s.prefs.setStn_bandActive144(true)

        s.state.stn_bandActive144 = false
        s.state.stn_bandActive432 = true
        s.draft.discard()

        assertTrue(s.prefs.isStn_bandActive144())
        assertFalse(s.prefs.isStn_bandActive432())
        assertTrue(s.state.stn_bandActive144, "after discarding the tab shows the stored value")
    }

    /*
     * The credentials stay immediate: they are locked while a session exists, and without
     * one Connect must use what the tab shows.
     */
    @Test
    fun `the login credentials still write through at once`() {
        val s = Session()

        s.state.stn_loginCallSign = "DO5AMF"
        s.state.stn_loginLocatorMainCat = "JO51IJ"
        s.state.stn_loginPassword = "secret"

        assertEquals("DO5AMF", s.prefs.getStn_loginCallSign())
        assertEquals("JO51IJ", s.prefs.getStn_loginLocatorMainCat())
        assertEquals("secret", s.prefs.getStn_loginPassword())
        assertFalse(s.draft.hasPendingChanges, "a credential went into the draft")
    }

    @Test
    fun `a band toggle round trips`() {
        val s = Session()

        s.state.stn_bandActive144 = true
        s.state.stn_bandActive432 = false
        s.draft.apply()

        assertTrue(s.prefs.isStn_bandActive144())
        assertEquals(false, s.prefs.isStn_bandActive432())
        assertTrue(s.state.stn_bandActive144)
    }

    @Test
    fun `selecting the main category writes both places it is held`() {
        val s = Session()
        val category = ChatCategory(2)

        s.state.selectMainCategory(category)

        assertEquals(2, s.prefs.getLoginChatCategoryMain().categoryNumber,
            "the stored setting must follow the selection")
        assertEquals(2, s.mainApplied?.categoryNumber,
            "so must the running session; the JavaFX window wrote both, and the "
                + "settings coverage test cannot see the ChatController write")
    }

    @Test
    fun `no second category is expressed as null`() {
        val s = Session()

        s.state.selectSecondCategory(null)

        assertNull(s.prefs.getLoginChatCategorySecond(),
            "the JavaFX window wrote null for 'no second chat'")
        assertTrue(s.secondWasApplied, "the running session must learn about it too")
        assertFalse(s.draft.hasPendingChanges, "a chat category went into the draft")
    }

    @Test
    fun `the second chat switch lives with the login settings`() {
        // It moved here from the GUI tab: the JavaFX checkbox stood on the Station tab
        // next to the category pickers, and it decides what the Connect button does.
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val draft = SettingsDraft()
        val state = StationTabState(prefs, draft, { }, { })

        state.loginToSecondChatEnabled = true
        draft.apply()

        assertTrue(prefs.isLoginToSecondChatEnabled())
        assertEquals(true, state.loginToSecondChatEnabled)
    }
}
