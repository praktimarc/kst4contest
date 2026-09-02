package kst4contest.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableLayoutManagerTest {

    @Test
    void contentWidthUsesCompactPadding() {
        assertEquals(116.0, TableLayoutManager.calculateInitialContentWidth(100.0, 24.0, 200.0));
    }

    @Test
    void contentWidthStillHonorsMinimumAndMaximum() {
        assertEquals(24.0, TableLayoutManager.calculateInitialContentWidth(0.0, 24.0, 200.0));
        assertEquals(200.0, TableLayoutManager.calculateInitialContentWidth(250.0, 24.0, 200.0));
    }
}
