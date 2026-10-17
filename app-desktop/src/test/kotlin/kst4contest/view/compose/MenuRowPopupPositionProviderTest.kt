package kst4contest.view.compose

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The menu row's own popup placement, as pure arithmetic.
 *
 * Material3's DropdownMenuPositionProvider refuses any y closer than 48 dp to the window's
 * top edge, which pushed every menu of the bar down to that margin. This provider must not:
 * a menu-bar menu starts flush under its anchor, whatever the distance to the window edge.
 */
class MenuRowPopupPositionProviderTest {

    private val window = IntSize(800, 600)
    private val menu = IntSize(200, 150)

    private fun positionOf(
        anchor: IntRect,
        window: IntSize = this.window,
        direction: LayoutDirection = LayoutDirection.Ltr,
        menu: IntSize = this.menu,
    ) = MenuRowPopupPositionProvider.calculatePosition(
        anchorBounds = anchor,
        windowSize = window,
        layoutDirection = direction,
        popupContentSize = menu,
    )

    /** The regression itself: an anchor at the very top gets its menu directly below it. */
    @Test
    fun opensFlushUnderAnAnchorAtTheTopOfTheWindow() {
        val position = positionOf(IntRect(0, 0, 40, 30))
        assertEquals(IntOffset(0, 30), position)
    }

    @Test
    fun staysInsideTheWindowWhenTheAnchorIsNearTheRightEdge() {
        val position = positionOf(IntRect(760, 0, 800, 30))
        assertEquals(IntOffset(600, 30), position)
    }

    @Test
    fun opensAboveTheAnchorWhenThereIsNoRoomBelow() {
        val position = positionOf(IntRect(0, 560, 40, 590))
        assertEquals(IntOffset(0, 410), position)
    }

    /** No room on either side: pinned to the window's bottom edge rather than clipped. */
    @Test
    fun staysInsideAWindowTooSmallForTheMenu() {
        val position = positionOf(IntRect(0, 40, 40, 70), window = IntSize(800, 100), menu = IntSize(200, 90))
        assertEquals(IntOffset(0, 10), position)
    }

    @Test
    fun alignsTheRightEdgesInRightToLeftLayouts() {
        val position = positionOf(IntRect(600, 0, 640, 30), direction = LayoutDirection.Rtl)
        assertEquals(IntOffset(440, 30), position)
    }
}
