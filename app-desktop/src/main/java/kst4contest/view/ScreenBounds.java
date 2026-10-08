package kst4contest.view;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * The screen area a window may actually use.
 *
 * <p>Replaces the former JavaFX screen lookup, which asked the primary screen for its
 * visual bounds. {@code GraphicsEnvironment.getMaximumWindowBounds()} is the AWT
 * equivalent: it excludes task bars and docks, which is exactly why the old code used the
 * visual bounds rather than the raw bounds.</p>
 */
public final class ScreenBounds {

    /** Used when there is no display at all, so a window still gets a usable size. */
    public static final double FALLBACK_WIDTH = 1234.0;

    /** Used when there is no display at all, so a window still gets a usable size. */
    public static final double FALLBACK_HEIGHT = 768.0;

    private ScreenBounds() {
        // Utility class.
    }

    /**
     * The usable width of the primary screen.
     *
     * @return the width in pixels, or {@link #FALLBACK_WIDTH} without a display
     */
    public static double availableWidth() {
        return widthOf(maximumWindowBounds());
    }

    /**
     * The usable height of the primary screen.
     *
     * @return the height in pixels, or {@link #FALLBACK_HEIGHT} without a display
     */
    public static double availableHeight() {
        return heightOf(maximumWindowBounds());
    }

    static double widthOf(final Rectangle bounds) {
        return bounds == null || bounds.width <= 0 ? FALLBACK_WIDTH : bounds.width;
    }

    static double heightOf(final Rectangle bounds) {
        return bounds == null || bounds.height <= 0 ? FALLBACK_HEIGHT : bounds.height;
    }

    private static Rectangle maximumWindowBounds() {

        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }

        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }
}
