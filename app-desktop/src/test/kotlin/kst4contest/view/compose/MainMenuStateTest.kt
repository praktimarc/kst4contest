package kst4contest.view.compose

import kst4contest.view.i18n.Strings
import kst4contest.view.i18n.Translations
import kst4contest.controller.On4KstConnectionState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Which menu items can be used, and when.
 *
 * A menu item that merely looks usable is a worse place for a stale value than a form
 * field: the operator presses it in the middle of a contest and nothing happens, or
 * the wrong thing does. The JavaFX window drove these from onConnectionStateChanged.
 */
class MainMenuStateTest {

    @Test
    fun `disconnected - connect is offered, disconnect is not`() {
        val menu = MainMenuState()

        menu.connectionState = On4KstConnectionState.DISCONNECTED

        assertTrue(menu.canConnect)
        assertFalse(menu.canDisconnect)
    }

    @Test
    fun `while connecting, connect is no longer offered`() {
        val menu = MainMenuState()

        menu.connectionState = On4KstConnectionState.AUTHENTICATING

        assertFalse(menu.canConnect, "a second attempt while one is running helps nobody")
        assertTrue(menu.canDisconnect, "the attempt can be given up")
    }

    @Test
    fun `online - disconnect yes, connect no`() {
        val menu = MainMenuState()

        menu.connectionState = On4KstConnectionState.ONLINE

        assertFalse(menu.canConnect)
        assertTrue(menu.canDisconnect)
    }

    @Test
    fun `the chat actions need a session that is really online`() {
        val menu = MainMenuState()

        menu.connectionState = On4KstConnectionState.AUTHENTICATING
        assertFalse(menu.canUseChatActions,
            "logging in is not being in the chat; /AWAY would go nowhere")

        menu.connectionState = On4KstConnectionState.ONLINE
        assertTrue(menu.canUseChatActions)
    }

    @Test
    fun `switching profiles and leaving are always possible`() {
        val menu = MainMenuState()
        menu.connectionState = On4KstConnectionState.DISCONNECTED

        assertTrue(menu.canSwitchProfile)
        assertTrue(menu.canExit, "an operator must always be able to leave")
    }

    @Test
    fun `an unknown state is treated as disconnected`() {
        val menu = MainMenuState()

        assertTrue(menu.canConnect, "before the first callback there is no session")
        assertFalse(menu.canDisconnect)
    }

    /**
     * The away item's own label is the only place the AFK state is visible at all — there
     * is no other indicator for it. It names the next action: away when active, active when
     * away. JavaFX flipped setText() inside the handler; here it follows the state, so it
     * is right even when the state changes from elsewhere.
     */
    /*
     * The base language's texts, built here rather than taken from a store: this test asserts
     * over the real English wording, and a store would make it depend on whichever language
     * ran last in the shared JVM.
     */
    private fun baseStrings(): Strings {
        val base = Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE)
        return Strings { key -> base[key] ?: key }
    }

    @Test
    fun `the away item says what pressing it will do`() {
        val state = MainMenuState()

        val strings = baseStrings()

        state.awayFromChat = false
        assertEquals("Show me as AWAY FROM chat!", state.awayMenuLabel(strings))

        state.awayFromChat = true
        assertEquals("Show me as ACTIVE in chat!", state.awayMenuLabel(strings))
    }

    /** Nobody is away before the session exists. */
    @Test
    fun `a fresh session is not away`() {
        assertFalse(MainMenuState().awayFromChat)
    }
}
