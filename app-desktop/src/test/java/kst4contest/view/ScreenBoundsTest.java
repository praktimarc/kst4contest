package kst4contest.view;

import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The startup size correction has to survive the move off {@code javafx.stage.Screen}.
 *
 * <p>A stored size from a larger monitor is reduced to what the current screen offers;
 * that rule lives in {@code MainWindowFrame.startupSize}, and this class only has to hand
 * it honest numbers. Returning zero would give Compose a 0x0 window.</p>
 */
class ScreenBoundsTest {

    @Test
    void theAvailableAreaIsPositive() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        assertTrue(ScreenBounds.availableWidth() > 0);
        assertTrue(ScreenBounds.availableHeight() > 0);
    }

    @Test
    void headlessFallsBackToASizeTheWindowCanActuallyUse() {
        // Packaging and CI run headless. Returning zero would hand Compose a 0x0 window.
        assertEquals(ScreenBounds.FALLBACK_WIDTH, ScreenBounds.widthOf(null));
        assertEquals(ScreenBounds.FALLBACK_HEIGHT, ScreenBounds.heightOf(null));
    }

    @Test
    void aKnownBoundIsPassedThrough() {
        Rectangle bounds = new Rectangle(0, 0, 2560, 1400);

        assertEquals(2560.0, ScreenBounds.widthOf(bounds));
        assertEquals(1400.0, ScreenBounds.heightOf(bounds));
    }

    @Test
    void aDegenerateBoundFallsBackRatherThanReportingZero() {
        Rectangle nothing = new Rectangle(0, 0, 0, 0);

        assertEquals(ScreenBounds.FALLBACK_WIDTH, ScreenBounds.widthOf(nothing));
        assertEquals(ScreenBounds.FALLBACK_HEIGHT, ScreenBounds.heightOf(nothing));
    }
}
