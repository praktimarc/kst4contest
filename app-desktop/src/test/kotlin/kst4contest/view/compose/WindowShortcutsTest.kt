package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Enter, Escape and Ctrl+1…Ctrl+0 belong to the whole window, not to the message field.
 *
 * JavaFX hung all three on the SCENE (7455-7474), and the tables even re-fired the send
 * button from their own key filter. An operator who has just clicked a station, a message row
 * or a button must still be able to press Enter and have the message go — and Ctrl+1…Ctrl+0
 * is how snippets are used at all, since the operator's hands are on the keyboard.
 */
class WindowShortcutsTest {

    @Test
    fun `control and the digit keys map to the ten snippet slots`() {
        assertEquals(0, WindowShortcuts.snippetIndexFor("1"))
        assertEquals(1, WindowShortcuts.snippetIndexFor("2"))
        assertEquals(8, WindowShortcuts.snippetIndexFor("9"))
    }

    /** Ctrl+0 is the tenth, not the first — the row of digits reads 1…9 then 0. */
    @Test
    fun `control zero is the tenth slot`() {
        assertEquals(9, WindowShortcuts.snippetIndexFor("0"))
    }

    @Test
    fun `any other key is not a snippet shortcut`() {
        assertNull(WindowShortcuts.snippetIndexFor("a"))
        assertNull(WindowShortcuts.snippetIndexFor(""))
        assertNull(WindowShortcuts.snippetIndexFor("12"))
    }

    /** All ten slots are reachable and none is reachable twice. */
    @Test
    fun `the ten digits cover the ten slots exactly once`() {
        val indices = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            .mapNotNull { WindowShortcuts.snippetIndexFor(it) }

        assertEquals((0..9).toList(), indices)
    }
}
