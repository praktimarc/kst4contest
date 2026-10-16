package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.awt.Point

/**
 * Which reported positions of an opened window count as the operator's move (issue #59).
 */
class MoveTrackerTest {

    private val opened = Point(560, 220)

    private fun offset(dx: Int, dy: Int = 0) = Point(opened.x + dx, opened.y + dy)

    @Test
    fun `a nudge around the placed position is not remembered`() {
        val tracker = MoveTracker(opened)

        assertNull(tracker.observe(offset(0, 30)))
        assertNull(tracker.observe(offset(-MOVE_TOLERANCE_PX)))
    }

    @Test
    fun `a move away from the placed position is remembered`() {
        val tracker = MoveTracker(opened)

        assertEquals(offset(100), tracker.observe(offset(100)))
    }

    /** The review case: open at O, drag to O+100, drag back to O+10. */
    @Test
    fun `a move back to near the opening spot replaces the far one`() {
        val tracker = MoveTracker(opened)

        assertEquals(offset(100), tracker.observe(offset(100)))
        assertEquals(offset(10), tracker.observe(offset(10)))
        assertEquals(opened, tracker.observe(opened))
    }

    @Test
    fun `an unplaced window takes the platform's placement jump as its anchor`() {
        val tracker = MoveTracker(null)

        /* Provisional position, then the window manager's real placement on another screen. */
        assertNull(tracker.observe(Point(0, 0)))
        assertNull(tracker.observe(Point(2400, 300)))
        /* A decoration nudge settles it. */
        assertNull(tracker.observe(Point(2400, 330)))
        /* Now the operator drags. */
        assertEquals(Point(2600, 330), tracker.observe(Point(2600, 330)))
    }

    @Test
    fun `an unplaced window settled by the start of a drag remembers the rest of it`() {
        val tracker = MoveTracker(null)

        assertNull(tracker.observe(Point(500, 300)))
        assertNull(tracker.observe(Point(510, 300)))
        assertNull(tracker.observe(Point(540, 300)))
        assertEquals(Point(700, 300), tracker.observe(Point(700, 300)))
        assertEquals(Point(520, 300), tracker.observe(Point(520, 300)))
    }
}
