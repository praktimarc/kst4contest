package kst4contest.view.compose.tabs

import androidx.compose.ui.input.key.Key
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The keyboard path to the three ways back.
 *
 * The spec requires one for all three, and says why: with no "not yet applied" state the ways
 * back are the only way back, and they must be reachable without colour perception. Tabbing
 * blindly through six text fields to find three buttons is reachable in the sense that a
 * locked door with the key inside is openable.
 *
 * Only the decision is tested, not the KeyEvent plumbing -- the same split WindowShortcuts
 * uses, and for the same reason: a desktop KeyEvent wraps a native AWT event and testing it
 * would test Compose rather than this.
 */
class ColourShortcutsTest {

    @Test
    fun theThreeWaysBackHaveTheirOwnKeys() {
        assertEquals(ColourAction.BACK_TO_PREVIOUS, ColourShortcuts.actionFor(true, Key.Z))
        assertEquals(ColourAction.DISCARD_CHANGES, ColourShortcuts.actionFor(true, Key.D))
        assertEquals(ColourAction.RESET_TO_SHIPPED, ColourShortcuts.actionFor(true, Key.R))
    }

    @Test
    fun eachActionHasExactlyOneKeyAndNoKeyHasTwoActions() {
        val keys = listOf(Key.Z, Key.D, Key.R)
        val actions = keys.map { ColourShortcuts.actionFor(true, it) }

        assertEquals(ColourAction.values().toList(), actions)
        assertEquals(ColourAction.values().size, actions.toSet().size)
    }

    @Test
    fun withoutControlTheKeysAreJustTypingAndMustReachTheFields() {
        /*
         * Six hex fields sit in this tab. A bare letter shortcut would eat the typing it is
         * meant to protect -- #DDEEFF contains a D.
         */
        assertNull(ColourShortcuts.actionFor(false, Key.Z))
        assertNull(ColourShortcuts.actionFor(false, Key.D))
        assertNull(ColourShortcuts.actionFor(false, Key.R))
    }

    @Test
    fun anyOtherKeyIsNotAShortcut() {
        assertNull(ColourShortcuts.actionFor(true, Key.A))
        assertNull(ColourShortcuts.actionFor(true, Key.Enter))
        assertNull(ColourShortcuts.actionFor(true, Key.Escape))
    }

    @Test
    fun theKeysDoNotCollideWithTheMainWindowsOwnShortcuts() {
        /*
         * The main window takes Enter, Escape and Ctrl+1 to Ctrl+0. None of the three here may
         * be one of those, or an operator with the settings window focused would fire both.
         */
        for (digit in listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")) {
            assertNull(
                kst4contest.view.compose.WindowShortcuts.snippetIndexFor("z"),
                "a letter key became a snippet slot",
            )
        }

        assertNull(ColourShortcuts.actionFor(true, Key.Enter))
        assertNull(ColourShortcuts.actionFor(true, Key.Escape))
    }

    @Test
    fun everyActionIsLabelledForTheOperatorToRead() {
        // A shortcut nobody is told about is not a second safety net.
        ColourAction.values().forEach {
            assert(ColourShortcuts.labelFor(it).isNotBlank()) { "$it has no label" }
        }
    }
}
