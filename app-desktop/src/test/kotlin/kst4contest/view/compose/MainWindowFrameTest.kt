package kst4contest.view.compose

import kst4contest.controller.On4KstConnectionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The frame around the main window: how big it opens, and what its title says.
 *
 * Pure arithmetic and string building, which is why it is tested here rather than looked
 * at. The clamp exists because a window remembered from a larger monitor opens
 * unusable — its buttons off-screen, and no way to reach them.
 */
class MainWindowFrameTest {

    @Test
    fun `a remembered size is used as it is when it fits`() {
        val size = MainWindowFrame.startupSize(
            stored = doubleArrayOf(768.0, 1234.0),
            screenHeight = 1440.0,
            screenWidth = 2560.0,
        )
        assertEquals(768.0, size.heightDp)
        assertEquals(1234.0, size.widthDp)
    }

    /** The margin leaves room for the window decoration and the screen edge. */
    @Test
    fun `a window remembered from a larger screen is cut down to fit`() {
        val size = MainWindowFrame.startupSize(
            stored = doubleArrayOf(1400.0, 3000.0),
            screenHeight = 1080.0,
            screenWidth = 1920.0,
        )
        assertEquals(1040.0, size.heightDp)
        assertEquals(1880.0, size.widthDp)
    }

    @Test
    fun `no remembered size at all opens at the default`() {
        val size = MainWindowFrame.startupSize(null, screenHeight = 1440.0, screenWidth = 2560.0)
        assertEquals(768.0, size.heightDp)
        assertEquals(1234.0, size.widthDp)
    }

    @Test
    fun `a half written size falls back per dimension`() {
        val size = MainWindowFrame.startupSize(
            stored = doubleArrayOf(0.0, 1500.0),
            screenHeight = 1440.0,
            screenWidth = 2560.0,
        )
        assertEquals(768.0, size.heightDp, "a stored zero height should fall back")
        assertEquals(1500.0, size.widthDp, "a usable stored width should survive")
    }

    @Test
    fun `an unusable stored array falls back entirely`() {
        assertEquals(768.0, MainWindowFrame.startupSize(doubleArrayOf(600.0), 1440.0, 2560.0).heightDp)
        assertEquals(
            768.0,
            MainWindowFrame.startupSize(doubleArrayOf(Double.NaN, Double.NaN), 1440.0, 2560.0).heightDp,
        )
    }

    /** A screen smaller than the margin must still yield a window with a size. */
    @Test
    fun `an absurdly small screen still yields a positive size`() {
        val size = MainWindowFrame.startupSize(doubleArrayOf(768.0, 1234.0), 20.0, 20.0)
        assertTrue(size.heightDp > 0.0)
        assertTrue(size.widthDp > 0.0)
    }

    // ---- the title ---------------------------------------------------------

    @Test
    fun `the title carries the chat state`() {
        assertEquals(
            "Connected to: 2 as DN9APW",
            MainWindowFrame.title(chatState = "Connected to: 2 as DN9APW", profileName = null),
        )
    }

    /** The root profile is the only one, so naming it would only add noise. */
    @Test
    fun `the root profile is not named in the title`() {
        assertEquals("DISCONNECTED!", MainWindowFrame.title("DISCONNECTED!", profileName = null))
    }

    @Test
    fun `a named profile is appended so two windows can be told apart`() {
        assertEquals(
            "DISCONNECTED! - Contest station",
            MainWindowFrame.title("DISCONNECTED!", profileName = "Contest station"),
        )
    }

    // ---- the chat state line ----------------------------------------------

    /**
     * What the title says while the link is not up. The operator reads this to tell a
     * scheduled reconnect from a dead session, so the wording is pinned.
     */
    @Test
    fun `every connection state has its own offline wording`() {
        assertEquals("CONNECTION LOST – reconnect scheduled", MainWindowFrame.offlineChatState(On4KstConnectionState.RECONNECT_WAIT))
        assertEquals("Connecting to ON4KST…", MainWindowFrame.offlineChatState(On4KstConnectionState.CONNECTING))
        assertEquals("Connected – authenticating with ON4KST…", MainWindowFrame.offlineChatState(On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT))
        assertEquals("Connected – authenticating with ON4KST…", MainWindowFrame.offlineChatState(On4KstConnectionState.AUTHENTICATING))
        assertEquals("Connected – synchronizing ON4KST chat data…", MainWindowFrame.offlineChatState(On4KstConnectionState.SYNCING_MAIN_CHAT))
        assertEquals("Connected – synchronizing ON4KST chat data…", MainWindowFrame.offlineChatState(On4KstConnectionState.SYNCING_SECOND_CHAT))
        assertEquals("DISCONNECTED!", MainWindowFrame.offlineChatState(On4KstConnectionState.DISCONNECTED))
        assertEquals("DISCONNECTED!", MainWindowFrame.offlineChatState(On4KstConnectionState.STOPPING))
    }

    @Test
    fun `no connection state at all reads as disconnected`() {
        assertEquals("DISCONNECTED!", MainWindowFrame.offlineChatState(null))
    }

    /**
     * The title carries the whole connection line while the chat is usable.
     *
     * The JavaFX window did that -- "Connected to: 2: 144/432 MHz as DN5PW (Testing) in
     * JO50JP (73 users online, 73 shown), 29 messages total." -- and the Compose window
     * opened with a fixed "KST4Contest (Compose)" instead, which told a tiling window
     * manager nothing and told the operator less.
     */
    @Test
    fun `the title carries the live connection line once the chat is usable`() {
        val detail = "Connected to: 2: 144/432 MHz  as DN5PW (Testing) in JO50JP " +
            "(73 users online, 73 shown), 29 messages total."

        assertEquals(detail, MainWindowFrame.chatState(On4KstConnectionState.ONLINE, detail))
    }

    @Test
    fun `an unusable chat says what it is doing instead`() {
        assertEquals(
            MainWindowFrame.offlineChatState(On4KstConnectionState.CONNECTING),
            MainWindowFrame.chatState(On4KstConnectionState.CONNECTING, "ignored while connecting"),
        )
    }

    @Test
    fun `a blank detail falls through rather than leaving the title empty`() {
        // The feed substitutes the state name for a blank detail, but it may not have run yet.
        assertEquals(
            MainWindowFrame.offlineChatState(On4KstConnectionState.ONLINE),
            MainWindowFrame.chatState(On4KstConnectionState.ONLINE, "   "),
        )
        assertEquals(
            MainWindowFrame.offlineChatState(null),
            MainWindowFrame.chatState(null, null),
        )
    }
}
