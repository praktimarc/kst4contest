package kst4contest.view.compose.tabs

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class StationTabStateTest {

    private class Session {
        val prefs = ChatPreferences()
        var mainApplied: ChatCategory? = null
        var secondApplied: ChatCategory? = null
        var secondWasApplied = false
        val state = StationTabState(
            prefs,
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
    fun `writing reaches the preferences at once, without a confirmation step`() {
        // The JavaFX window had no value-collecting confirmation: each control wrote
        // straight into ChatPreferences, and "Save settings" only persisted them.
        val s = Session()

        s.state.stn_loginCallSign = "DO5AMF"
        s.state.stn_pstRotatorEnabled = true
        s.state.stn_qtfDefault = 135.0

        assertEquals("DO5AMF", s.prefs.getStn_loginCallSign())
        assertTrue(s.prefs.isStn_pstRotatorEnabled())
        assertEquals(135.0, s.prefs.getStn_qtfDefault(), 0.001)
    }

    @Test
    fun `a band toggle round trips`() {
        val s = Session()

        s.state.stn_bandActive144 = true
        s.state.stn_bandActive432 = false

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
    }

    @Test
    fun `the second chat switch lives with the login settings`() {
        // It moved here from the GUI tab: the JavaFX checkbox stood on the Station tab
        // next to the category pickers, and it decides what the Connect button does.
        val prefs = ChatPreferences()
        val state = StationTabState(prefs, { }, { })

        state.loginToSecondChatEnabled = true

        assertTrue(prefs.isLoginToSecondChatEnabled())
        assertEquals(true, state.loginToSecondChatEnabled)
    }
}
