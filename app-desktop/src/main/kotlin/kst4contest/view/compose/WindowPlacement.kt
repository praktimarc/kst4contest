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
 * A window position kept across restarts, together with the screen it was on.
 *
 * @param position the window's top-left corner in desktop coordinates
 * @param screen the bounds of the screen the window was on when it was stored
 */
data class StoredWindowPosition(val position: Point, val screen: Rectangle)

/**
 * Where window positions are kept across restarts, when the operator opted in.
 *
 * The application sets it per profile; [load] answers null and [save] does nothing while
 * the opt-in is off, so the hosts need not know about the setting.
 */
interface WindowPositionMemory {
    fun load(windowId: String): StoredWindowPosition?
    fun save(windowId: String, stored: StoredWindowPosition)
}

/**
 * Where a secondary window opens, decided without touching AWT (issue #59).
 *
 * The rule: a window the operator moved during this session reopens where it was moved to,
 * as long as its title bar can still be reached on a connected screen — shifted just far
 * enough to lie wholly within that screen's usable area. Otherwise it opens centred on the
 * screen the main window is on. Either way its size is cut down to the usable area of the
 * screen it lands on, so no button sits past the edge.
 *
 * The cut-down size is only the opening size. The host does not write it back, so the
 * stored size survives a stay on a smaller screen; a resize the operator makes there is
 * an ordinary resize and is saved.
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
                return keptWithin(usableArea(screen), remembered, width, height)
            }
        }

        if (mainWindow == null) {
            return null
        }

        return centredOn(usableArea(screenOf(screens, mainWindow)), width, height)
    }

    /**
     * Where a window opens from a position stored in an earlier run.
     *
     * Only on the very screen it was stored on: same place in the desktop, same size. A
     * changed layout -- a monitor unplugged, rearranged or switched to another resolution --
     * makes the stored position meaningless, and the caller falls back to the session rule.
     * On that screen the window is kept within the usable area, like a session position.
     *
     * @return the frame bounds to open with, or null when the stored screen is not connected
     */
    fun restore(screens: List<ScreenArea>, stored: StoredWindowPosition, width: Int, height: Int): Rectangle? {
        val screen = screens.firstOrNull { it.bounds == stored.screen } ?: return null
        return keptWithin(usableArea(screen), stored.position, width, height)
    }

    /**
     * The window cut down to the usable area of the screen it is on, or null when it fits.
     *
     * For a window that is already open and was moved onto a smaller screen -- a laptop
     * panel, a 640x480 field monitor. Opening sizes are cut by [place] and [restore]; this
     * covers the move. Shifted the least distance into the usable area, like [keptWithin].
     */
    fun fitted(screens: List<ScreenArea>, window: Rectangle): Rectangle? {
        if (screens.isEmpty()) {
            return null
        }
        val usable = usableArea(screenOf(screens, window))
        if (window.width <= usable.width && window.height <= usable.height) {
            return null
        }
        return keptWithin(usable, window.location, window.width, window.height)
    }

    /**
     * A minimum size no larger than the smallest usable area: AWT enforces the minimum even when it does
     * not fit the screen, which leaves the window's edges unreachable.
     */
    fun minimumWithin(screens: List<ScreenArea>, width: Int, height: Int): java.awt.Dimension {
        if (screens.isEmpty()) {
            return java.awt.Dimension(width, height)
        }
        val areas = screens.map(::usableArea)
        return java.awt.Dimension(
            clamp(width, areas.minOf { it.width }),
            clamp(height, areas.minOf { it.height }),
        )
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
     * The window at [position], cut down to fit [usable] and shifted the least distance
     * that puts it wholly inside. The cut comes first, so the shift always has room.
     */
    fun keptWithin(usable: Rectangle, position: Point, width: Int, height: Int): Rectangle {
        val w = clamp(width, usable.width)
        val h = clamp(height, usable.height)
        return Rectangle(
            position.x.coerceIn(usable.x, usable.x + usable.width - w),
            position.y.coerceIn(usable.y, usable.y + usable.height - h),
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

    /**
     * The positions kept across restarts, set by the application for the active profile.
     * Null in tests and before startup, which means nothing is restored or stored.
     */
    @JvmStatic
    @Volatile
    var memory: WindowPositionMemory? = null

    /**
     * Where a window opens from its stored position, or null when none applies.
     */
    fun restore(windowId: String, width: Int, height: Int): Rectangle? {
        val stored = memory?.load(windowId) ?: return null
        return WindowPlacement.restore(connectedScreens(), stored, width, height)
    }

    /**
     * The minimum size a window may enforce, capped to the smallest connected screen. The
     * smallest rather than the current one, because the window may be moved there later
     * and must then still be cut down to fit.
     */
    fun minimumSizeOnScreens(width: Int, height: Int): java.awt.Dimension =
        WindowPlacement.minimumWithin(connectedScreens(), width, height)

    /**
     * The window cut down to the screen it is now on, or null when it fits.
     */
    fun fitted(window: Rectangle): Rectangle? = WindowPlacement.fitted(connectedScreens(), window)

    /**
     * Stores the position the operator moved a window to, with the screen it is now on.
     */
    fun store(windowId: String, position: Point, width: Int, height: Int) {
        val target = memory ?: return
        val screens = connectedScreens()
        if (screens.isEmpty()) {
            return
        }
        val screen = WindowPlacement.screenOf(screens, Rectangle(position.x, position.y, width, height))
        target.save(windowId, StoredWindowPosition(position, screen.bounds))
    }

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
