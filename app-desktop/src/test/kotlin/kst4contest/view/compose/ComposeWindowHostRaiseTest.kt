package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Frame

/**
 * What a second press on an opener does to the window that is already open.
 *
 * `ComposeWindowHost.show` refused silently when the window was open, which is correct
 * about not building a second one and wrong about everything else: the JavaFX originals
 * built a fresh Stage per press and came forward as a side effect, so the operator who
 * clicks back to the main window and presses "more" again used to see the window and now
 * saw nothing. Six windows share this host, so all six had it.
 *
 * Only the rules are tested here, but there turned out to be two of them: what to do,
 * and what to write when de-iconifying. The first version of this file called the second
 * one untestable AWT and got it wrong in the one line it had excused itself from
 * covering. `toFront` really is not drivable — its outcome belongs to the window manager
 * and is not observable from the JVM — and that is the whole of what is uncovered.
 */
class ComposeWindowHostRaiseTest {

    @Test
    fun `an ordinary open window is brought forward`() {
        assertEquals(
            listOf(RaiseStep.TO_FRONT),
            raiseSteps(displayable = true, iconified = false),
        )
    }

    /**
     * The order is the point, not the content. `toFront` on an iconified frame leaves it
     * iconified, so restoring afterwards would raise a window the operator still cannot
     * see — the same dead button, one step further along.
     */
    @Test
    fun `a minimised window is restored before it is raised`() {
        assertEquals(
            listOf(RaiseStep.DEICONIFY, RaiseStep.TO_FRONT),
            raiseSteps(displayable = true, iconified = true),
        )
    }

    /**
     * A window on its way out is left alone. `close()` drops visibility and waits for the
     * thread; a press landing in that gap must not fight the close and must not raise a
     * frame that is about to be disposed.
     */
    @Test
    fun `a window that is no longer displayable is left alone`() {
        assertEquals(emptyList<RaiseStep>(), raiseSteps(displayable = false, iconified = false))
        assertEquals(emptyList<RaiseStep>(), raiseSteps(displayable = false, iconified = true))
    }

    /**
     * De-iconifying clears one bit; it does not assign a state.
     *
     * `extendedState` is a bit set — `NORMAL` is 0, `ICONIFIED` is 1, `MAXIMIZED_BOTH` is
     * 6 — so a window the operator maximised and then minimised holds 7. Writing
     * `Frame.NORMAL` there hands it back restored-down and drops a maximisation nobody
     * asked to drop. That is what the first version of `raise()` did, one line after
     * taking care to *detect* the mixed state with a mask rather than an equality.
     */
    @Test
    fun `restoring a maximised window keeps it maximised`() {
        assertEquals(Frame.MAXIMIZED_BOTH, deiconifiedState(Frame.MAXIMIZED_BOTH or Frame.ICONIFIED))
    }

    @Test
    fun `restoring a plain minimised window leaves it normal`() {
        assertEquals(Frame.NORMAL, deiconifiedState(Frame.ICONIFIED))
    }

    @Test
    fun `a state without the iconified bit is returned untouched`() {
        assertEquals(Frame.MAXIMIZED_BOTH, deiconifiedState(Frame.MAXIMIZED_BOTH))
        assertEquals(Frame.NORMAL, deiconifiedState(Frame.NORMAL))
    }
}
