package kst4contest.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TruncatedTextTableCellTest {

    @Test
    void plainFullTextTooltipIsShownOnlyForClippedText() {
        assertNull(TruncatedTextTooltipSupport.buildTooltipText("complete", false, null));
        assertEquals("complete",
                TruncatedTextTooltipSupport.buildTooltipText("complete", true, null));
    }

    @Test
    void functionalTooltipRemainsAndCombinesWithClippedValue() {
        assertEquals("Worked status",
                TruncatedTextTooltipSupport.buildTooltipText("X", false, "Worked status"));
        assertEquals("Long value\n\nWorked status",
                TruncatedTextTooltipSupport.buildTooltipText("Long value", true, "Worked status"));
    }

    @Test
    void clippingComparisonUsesAvailableRenderedWidth() {
        assertFalse(TruncatedTextTooltipSupport.isTextClipped(100, 100));
        assertTrue(TruncatedTextTooltipSupport.isTextClipped(102, 100));
    }
}
