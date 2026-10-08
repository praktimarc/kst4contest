package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Which menu of the in-window row is open.
 *
 * Small, but it carries one thing that is not cosmetic: [MainMenuRowState.anyOpen] is what
 * keeps the window-wide keys out of the way while a menu is down. `MainWindow` hangs
 * `WindowShortcuts.handle` on the window's `onPreviewKeyEvent`, and a preview runs *before*
 * its children — so without this, pressing Escape to dismiss a menu would first clear the
 * operator's half-typed message.
 */
class MainMenuRowStateTest {

    @Test
    fun nothingIsOpenToStartWith() {
        val state = MainMenuRowState()

        assertNull(state.openMenu)
        assertFalse(state.anyOpen)
    }

    @Test
    fun aTitleOpensItsOwnMenu() {
        val state = MainMenuRowState()

        state.toggle("File")

        assertEquals("File", state.openMenu)
        assertTrue(state.anyOpen)
    }

    @Test
    fun theSameTitleAgainClosesIt() {
        val state = MainMenuRowState()
        state.toggle("File")

        state.toggle("File")

        assertNull(state.openMenu, "pressing the open menu's own title must close it")
        assertFalse(state.anyOpen)
    }

    @Test
    fun anotherTitleSwitchesRatherThanStacking() {
        val state = MainMenuRowState()
        state.toggle("File")

        state.toggle("Info")

        // Two menus down at once is a thing no menu bar does.
        assertEquals("Info", state.openMenu)
    }

    @Test
    fun closeAlwaysEndsUpClosedAndIsIdempotent() {
        val state = MainMenuRowState()
        state.toggle("Windows")

        state.close()
        state.close()

        assertNull(state.openMenu)
        assertFalse(state.anyOpen)
    }

    @Test
    fun anItemBeingPickedClosesTheMenu() {
        val state = MainMenuRowState()
        state.toggle("Options")
        var ran = false

        state.pick { ran = true }

        assertTrue(ran, "the action must run")
        assertNull(state.openMenu, "a picked item must close the menu it came from")
    }
}
