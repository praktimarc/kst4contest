package kst4contest.view.compose

import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window

/**
 * One connected screen: its whole area and the part a window may use.
 *
 * @param bounds the screen in the shared desktop coordinate space. A screen left of or
 *        above the primary one has negative coordinates.
 * @param usable [bounds] minus task bars, docks and panels
 */
data class ScreenArea(val bounds: Rectangle, val usable: Rectangle)

/**
 * Where a secondary window opens, decided without touching AWT (issue #59).
 *
 * The rule: a window the operator moved during this session reopens where it was moved to,
 * as long as its title bar can still be reached on a connected screen. Otherwise it opens
 * centred on the screen the main window is on. Either way its size is cut down to the
 * usable area of the screen it lands on, so no button sits past the edge.
 *
 * Before this, every secondary window got the platform's default position, which on a
 * multi-monitor desk is the primary screen — the station map opened on a screen the
 * operator was not looking at, or on one that was switched off.
 *
 * Pure on purpose: the rule is the part that can be wrong, and it needs screens that no
 * test machine has. [WindowPlacementOnScreen] feeds it the real screens.
 */
object WindowPlacement {

    /**
     * How much of the window's top edge counts as the title bar. A remembered position
     * is only kept when this strip can be grabbed, because a window whose title bar is
     * off-screen cannot be moved back by the operator.
     */
    const val TITLE_BAR_HEIGHT = 32

    /**
     * How wide a piece of the title bar must be visible to count as reachable. Narrower
     * windows need their whole width.
     */
    const val MIN_VISIBLE_TITLE_WIDTH = 100

    /**
     * Where the window opens and how big it is.
     *
     * @param screens the connected screens; empty when there is no display
     * @param mainWindow the main window's frame bounds, null while it does not exist
     * @param remembered where the operator moved this window to during the session, or null
     * @param width the size the window asks for
     * @param height the size the window asks for
     * @return the frame bounds to open with, or null to leave placement to the platform —
     *         which is the behaviour before the main window exists
     */
    fun place(
        screens: List<ScreenArea>,
        mainWindow: Rectangle?,
        remembered: Point?,
        width: Int,
        height: Int,
    ): Rectangle? {
        if (screens.isEmpty()) {
            return null
        }

        if (remembered != null) {
            val screen = screenShowingTitleBar(screens, remembered, width)
            if (screen != null) {
                val usable = usableArea(screen)
                return Rectangle(
                    remembered.x,
                    remembered.y,
                    clamp(width, usable.width),
                    clamp(height, usable.height),
                )
            }
        }

        if (mainWindow == null) {
            return null
        }

        return centredOn(usableArea(screenOf(screens, mainWindow)), width, height)
    }

    /**
     * The window centred on [usable], cut down to fit it.
     */
    fun centredOn(usable: Rectangle, width: Int, height: Int): Rectangle {
        val w = clamp(width, usable.width)
        val h = clamp(height, usable.height)
        return Rectangle(
            usable.x + (usable.width - w) / 2,
            usable.y + (usable.height - h) / 2,
            w,
            h,
        )
    }

    /**
     * The screen a window is on: the one holding its centre, else the one it overlaps
     * most, else the first. The centre decides first because a window straddling two
     * screens is "on" the one the operator sees most of it on, and the centre says so
     * without an area comparison in the common case.
     */
    fun screenOf(screens: List<ScreenArea>, window: Rectangle): ScreenArea {
        val centre = Point(window.x + window.width / 2, window.y + window.height / 2)
        screens.firstOrNull { it.bounds.contains(centre) }?.let { return it }

        return screens.maxByOrNull { overlapArea(it.bounds, window) }
            ?.takeIf { overlapArea(it.bounds, window) > 0 }
            ?: screens.first()
    }

    /**
     * The screen on which the title bar of a window at [position] can be grabbed, or null
     * when it cannot be grabbed on any — the screen was unplugged, or the window was
     * pushed past an edge.
     */
    private fun screenShowingTitleBar(
        screens: List<ScreenArea>,
        position: Point,
        width: Int,
    ): ScreenArea? {
        val titleBar = Rectangle(position.x, position.y, width.coerceAtLeast(1), TITLE_BAR_HEIGHT)
        val neededWidth = minOf(MIN_VISIBLE_TITLE_WIDTH, titleBar.width)
        val neededHeight = TITLE_BAR_HEIGHT / 2

        return screens
            .filter {
                val visible = usableArea(it).intersection(titleBar)
                !visible.isEmpty && visible.width >= neededWidth && visible.height >= neededHeight
            }
            .maxByOrNull { overlapArea(usableArea(it), titleBar) }
    }

    /**
     * The usable area, or the whole screen when the insets leave nothing — a broken inset
     * report must not shrink every window to a single pixel.
     */
    private fun usableArea(screen: ScreenArea): Rectangle =
        if (screen.usable.isEmpty) screen.bounds else screen.usable

    private fun clamp(wanted: Int, available: Int): Int =
        wanted.coerceAtMost(available).coerceAtLeast(1)

    private fun overlapArea(a: Rectangle, b: Rectangle): Long {
        val overlap = a.intersection(b)
        return if (overlap.isEmpty) 0L else overlap.width.toLong() * overlap.height
    }
}

/**
 * The AWT side of [WindowPlacement]: the real screens and the real main window.
 *
 * Kept thin so that what no headless test can see is only reading the screens and
 * handing them over. Call it on the AWT event thread, like everything that touches a
 * window.
 */
object WindowPlacementOnScreen {

    /**
     * The main window's frame, published by its host while it is open. Read only for its
     * bounds; nothing here moves it.
     */
    @Volatile
    var mainWindow: Window? = null

    /** The connected screens, the default one first. Empty without a display. */
    fun connectedScreens(): List<ScreenArea> = runCatching {
        if (GraphicsEnvironment.isHeadless()) {
            return emptyList()
        }

        val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val toolkit = Toolkit.getDefaultToolkit()
        val default = environment.defaultScreenDevice

        environment.screenDevices
            .sortedByDescending { it == default }
            .map { device ->
                val configuration = device.defaultConfiguration
                val bounds = configuration.bounds
                val insets = toolkit.getScreenInsets(configuration)
                ScreenArea(
                    bounds = bounds,
                    usable = Rectangle(
                        bounds.x + insets.left,
                        bounds.y + insets.top,
                        bounds.width - insets.left - insets.right,
                        bounds.height - insets.top - insets.bottom,
                    ),
                )
            }
    }.getOrDefault(emptyList())

    /**
     * The main window's bounds, or null while there is no main window on screen.
     */
    fun mainWindowBounds(): Rectangle? =
        mainWindow?.takeIf { it.isDisplayable && it.isShowing }?.bounds

    /**
     * Where a secondary window opens, from the live screens and main window.
     *
     * @return null to leave placement to the platform
     */
    fun place(remembered: Point?, width: Int, height: Int): Rectangle? =
        WindowPlacement.place(connectedScreens(), mainWindowBounds(), remembered, width, height)

    /**
     * Centres a dialog on the main window's screen, cut down to fit.
     *
     * Before the main window exists — the profile picker at startup — the dialog is
     * centred the way it always was, on the primary screen.
     */
    @JvmStatic
    fun centreOnMainWindowScreen(dialog: Window) {
        val placed = place(null, dialog.width, dialog.height)
        if (placed == null) {
            dialog.setLocationRelativeTo(null)
        } else {
            dialog.bounds = placed
        }
    }
}
