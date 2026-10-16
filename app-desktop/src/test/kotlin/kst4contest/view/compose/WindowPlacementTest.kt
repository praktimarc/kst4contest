package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Point
import java.awt.Rectangle

/**
 * Where a secondary window opens (issue #59).
 *
 * The screens here are made up, which is the point: no test machine has a second
 * monitor left of the primary one, or one that was just unplugged.
 */
class WindowPlacementTest {

    /** 1920x1080 primary with a 40 px task bar at the bottom. */
    private val primary = ScreenArea(
        bounds = Rectangle(0, 0, 1920, 1080),
        usable = Rectangle(0, 0, 1920, 1040),
    )

    /** 2560x1440 to the right of the primary, without panels. */
    private val right = ScreenArea(
        bounds = Rectangle(1920, 0, 2560, 1440),
        usable = Rectangle(1920, 0, 2560, 1440),
    )

    /** 1280x1024 left of the primary, so with negative coordinates, and a 30 px top bar. */
    private val left = ScreenArea(
        bounds = Rectangle(-1280, 0, 1280, 1024),
        usable = Rectangle(-1280, 30, 1280, 994),
    )

    @Test
    fun `a window opens centred on the screen the main window is on`() {
        val main = Rectangle(2200, 100, 1200, 800)

        val placed = WindowPlacement.place(listOf(primary, right), main, null, 800, 600)

        assertEquals(Rectangle(1920 + 880, 420, 800, 600), placed)
    }

    @Test
    fun `the main window on the primary screen keeps the window there`() {
        val main = Rectangle(100, 100, 1200, 800)

        val placed = WindowPlacement.place(listOf(primary, right), main, null, 800, 600)

        assertEquals(Rectangle(560, 220, 800, 600), placed)
    }

    @Test
    fun `a screen left of the primary one works with negative coordinates`() {
        val main = Rectangle(-1200, 50, 1000, 700)

        val placed = WindowPlacement.place(listOf(primary, left), main, null, 800, 600)

        assertEquals(Rectangle(-1280 + 240, 30 + 197, 800, 600), placed)
    }

    @Test
    fun `a main window straddling two screens counts for the one holding its centre`() {
        val main = Rectangle(1500, 100, 1200, 800)

        val placed = WindowPlacement.place(listOf(primary, right), main, null, 800, 600)

        assertEquals(right.bounds, WindowPlacement.screenOf(listOf(primary, right), main).bounds)
        assertEquals(1920 + 880, placed!!.x)
    }

    @Test
    fun `a window larger than the screen is cut down to its usable area`() {
        val main = Rectangle(100, 100, 1200, 800)

        val placed = WindowPlacement.place(listOf(primary), main, null, 2500, 1300)

        assertEquals(Rectangle(0, 0, 1920, 1040), placed)
    }

    @Test
    fun `only the oversized dimension is cut`() {
        val main = Rectangle(-1200, 50, 1000, 700)

        val placed = WindowPlacement.place(listOf(primary, left), main, null, 800, 2000)

        assertEquals(Rectangle(-1280 + 240, 30, 800, 994), placed)
    }

    @Test
    fun `a window moved during the session reopens where it was moved to`() {
        val main = Rectangle(100, 100, 1200, 800)

        val placed = WindowPlacement.place(listOf(primary, right), main, Point(3000, 200), 800, 600)

        assertEquals(Rectangle(3000, 200, 800, 600), placed)
    }

    @Test
    fun `a remembered position on an unplugged screen falls back to the main window's screen`() {
        val main = Rectangle(100, 100, 1200, 800)

        /* Moved to the right-hand screen, which is no longer connected. */
        val placed = WindowPlacement.place(listOf(primary), main, Point(3000, 200), 800, 600)

        assertEquals(Rectangle(560, 220, 800, 600), placed)
    }

    @Test
    fun `a remembered position with the title bar above the screen falls back`() {
        val main = Rectangle(100, 100, 1200, 800)

        val placed = WindowPlacement.place(listOf(primary), main, Point(400, -200), 800, 600)

        assertEquals(Rectangle(560, 220, 800, 600), placed)
    }

    @Test
    fun `a remembered position with the title bar under the task bar falls back`() {
        val main = Rectangle(100, 100, 1200, 800)

        val placed = WindowPlacement.place(listOf(primary), main, Point(400, 1035), 800, 600)

        assertEquals(Rectangle(560, 220, 800, 600), placed)
    }

    @Test
    fun `a title bar that is mostly past the edge but still grabbable is pulled back in`() {
        val main = Rectangle(100, 100, 1200, 800)

        /* 120 px of the title bar remain on the primary screen: the spot counts, the overhang goes. */
        val placed = WindowPlacement.place(listOf(primary), main, Point(1800, 300), 800, 600)

        assertEquals(Rectangle(1920 - 800, 300, 800, 600), placed)
    }

    @Test
    fun `a remembered window hanging into the task bar is lifted above it`() {
        val main = Rectangle(100, 100, 1200, 800)

        val placed = WindowPlacement.place(listOf(primary), main, Point(400, 700), 800, 600)

        assertEquals(Rectangle(400, 1040 - 600, 800, 600), placed)
    }

    @Test
    fun `a remembered window that fits stays exactly where it was`() {
        val placed = WindowPlacement.place(listOf(primary, left), null, Point(-1100, 200), 800, 600)

        assertEquals(Rectangle(-1100, 200, 800, 600), placed)
    }

    @Test
    fun `a title bar with too little left to grab falls back`() {
        val main = Rectangle(100, 100, 1200, 800)

        /* Only 20 px of the title bar remain on the primary screen. */
        val placed = WindowPlacement.place(listOf(primary), main, Point(1900, 300), 800, 600)

        assertEquals(Rectangle(560, 220, 800, 600), placed)
    }

    @Test
    fun `a remembered window is cut down to the screen it is on`() {
        val placed = WindowPlacement.place(listOf(primary, left), null, Point(-1200, 100), 2000, 600)

        assertEquals(Rectangle(-1280, 100, 1280, 600), placed)
    }

    @Test
    fun `without a main window the platform decides`() {
        assertNull(WindowPlacement.place(listOf(primary, right), null, null, 800, 600))
    }

    @Test
    fun `without a display the platform decides`() {
        assertNull(WindowPlacement.place(emptyList(), Rectangle(0, 0, 100, 100), Point(10, 10), 800, 600))
    }

    @Test
    fun `a main window off every screen falls back to the first screen`() {
        val main = Rectangle(9000, 9000, 800, 600)

        assertEquals(primary, WindowPlacement.screenOf(listOf(primary, right), main))
    }

    @Test
    fun `empty insets leave the whole screen usable`() {
        val broken = ScreenArea(Rectangle(0, 0, 1920, 1080), Rectangle(0, 0, 0, 0))

        val placed = WindowPlacement.place(listOf(broken), Rectangle(0, 0, 100, 100), null, 800, 600)

        assertEquals(Rectangle(560, 240, 800, 600), placed)
    }

    @Test
    fun `a window manager's nudge is not taken for the operator's move`() {
        assertFalse(movedAway(Point(560, 220), Point(560, 252)))
        assertFalse(movedAway(Point(560, 220), Point(560 - MOVE_TOLERANCE_PX, 220)))
        assertTrue(movedAway(Point(560, 220), Point(560, 220 + MOVE_TOLERANCE_PX + 1)))
        assertTrue(movedAway(Point(560, 220), Point(3000, 220)))
    }
}
