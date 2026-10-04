package kst4contest.view;

/**
 * The column-width rule the station and monitor tables are sized by.
 *
 * <p>What is left of the class that applied persisted leaf-column widths to a
 * {@code TableView} and performed one content-based initial sizing pass. The tables are
 * Compose now: {@code DataTableState} applies the stored widths and
 * {@code ColumnWidthStore} persists them, so the installation, the resize detection and
 * the measuring all went with the JavaFX table.</p>
 *
 * <p>The sizing rule itself did not. It survives because it is the one piece that decides
 * what the operator sees on a first run, and because several Compose column definitions
 * still name this class as the place their column ids were registered — a reader who
 * follows that reference should find the rule, not a gap.</p>
 */
public final class TableLayoutManager {

    /** Room for the cell's left and right padding, on top of the measured text. */
    private static final double CELL_HORIZONTAL_PADDING = 16.0;

    private TableLayoutManager() {
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    /**
     * Calculates a compact content width. Package-private for focused sizing tests.
     *
     * @param measuredWidth width the content needs without padding
     * @param minimum       narrowest acceptable column width
     * @param maximum       widest width an initial sizing pass may choose
     * @return the width to use, padding included and clamped into range
     */
    @SuppressWarnings("PMD.CommentDefaultAccessModifier")
    static double calculateInitialContentWidth(
            final double measuredWidth,
            final double minimum,
            final double maximum
    ) {
        return clamp(measuredWidth + CELL_HORIZONTAL_PADDING, minimum, maximum);
    }
}
