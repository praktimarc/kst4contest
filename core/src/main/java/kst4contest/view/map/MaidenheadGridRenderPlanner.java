package kst4contest.view.map;

import static kst4contest.view.map.MaidenheadGridUtils.GridPrecision;

/**
 * Chooses a grid rendering strategy for the current viewport.
 *
 * Goals:
 * - keep the current zoom based progression as a baseline
 * - avoid overly dense grid rendering on unlucky viewport sizes
 * - make label visibility depend on actual on-screen cell size
 * - expose row/column strides so labels form a stable raster pattern
 */
public final class MaidenheadGridRenderPlanner {

    private static final int MAX_SUBSQUARE_CELLS = 4500;
    private static final int MAX_SQUARE_CELLS = 2500;

    private static final double MIN_SUBSQUARE_CELL_WIDTH_PX = 8.0;
    private static final double MIN_SUBSQUARE_CELL_HEIGHT_PX = 7.0;
    private static final double MIN_SQUARE_CELL_WIDTH_PX = 10.0;
    private static final double MIN_SQUARE_CELL_HEIGHT_PX = 9.0;

    private MaidenheadGridRenderPlanner() {
    }

    public static GridRenderPlan createPlan(int leafletZoom,
                                            double southLat,
                                            double westLon,
                                            double northLat,
                                            double eastLon,
                                            double viewportWidthPx,
                                            double viewportHeightPx) {

        double safeViewportWidthPx = Math.max(1.0, viewportWidthPx);
        double safeViewportHeightPx = Math.max(1.0, viewportHeightPx);

        GridPrecision requestedPrecision = MaidenheadGridUtils.precisionForZoom(leafletZoom);
        GridPrecision effectivePrecision = chooseEffectivePrecision(
                requestedPrecision,
                southLat,
                westLon,
                northLat,
                eastLon,
                safeViewportWidthPx,
                safeViewportHeightPx
        );

        double estimatedCellWidthPx = estimateCellWidthPx(effectivePrecision, westLon, eastLon, safeViewportWidthPx);
        double estimatedCellHeightPx = estimateCellHeightPx(effectivePrecision, southLat, northLat, safeViewportHeightPx);

        boolean showLabels = shouldShowLabels(effectivePrecision, estimatedCellWidthPx, estimatedCellHeightPx);
        int labelColumnStride = showLabels ? computeStride(estimatedCellWidthPx, desiredLabelWidthPx(effectivePrecision)) : Integer.MAX_VALUE;
        int labelRowStride = showLabels ? computeStride(estimatedCellHeightPx, desiredLabelHeightPx(effectivePrecision)) : Integer.MAX_VALUE;
        double labelFontSizePx = estimateLabelFontSizePx(effectivePrecision, estimatedCellWidthPx, estimatedCellHeightPx);

        return new GridRenderPlan(
                effectivePrecision,
                showLabels,
                labelRowStride,
                labelColumnStride,
                estimatedCellWidthPx,
                estimatedCellHeightPx,
                labelFontSizePx
        );
    }

    private static GridPrecision chooseEffectivePrecision(GridPrecision requestedPrecision,
                                                          double southLat,
                                                          double westLon,
                                                          double northLat,
                                                          double eastLon,
                                                          double viewportWidthPx,
                                                          double viewportHeightPx) {

        GridPrecision effectivePrecision = requestedPrecision;

        if (effectivePrecision == GridPrecision.SUBSQUARE_6
                && !canRenderSubsquareGrid(southLat, westLon, northLat, eastLon, viewportWidthPx, viewportHeightPx)) {
            effectivePrecision = GridPrecision.SQUARE_4;
        }

        if (effectivePrecision == GridPrecision.SQUARE_4
                && !canRenderSquareGrid(southLat, westLon, northLat, eastLon, viewportWidthPx, viewportHeightPx)) {
            effectivePrecision = GridPrecision.FIELD_2;
        }

        return effectivePrecision;
    }

    private static boolean canRenderSubsquareGrid(double southLat,
                                                  double westLon,
                                                  double northLat,
                                                  double eastLon,
                                                  double viewportWidthPx,
                                                  double viewportHeightPx) {

        double cellWidthPx = estimateCellWidthPx(GridPrecision.SUBSQUARE_6, westLon, eastLon, viewportWidthPx);
        double cellHeightPx = estimateCellHeightPx(GridPrecision.SUBSQUARE_6, southLat, northLat, viewportHeightPx);
        int estimatedCellCount = estimateVisibleCellCount(GridPrecision.SUBSQUARE_6, southLat, westLon, northLat, eastLon);

        return cellWidthPx >= MIN_SUBSQUARE_CELL_WIDTH_PX
                && cellHeightPx >= MIN_SUBSQUARE_CELL_HEIGHT_PX
                && estimatedCellCount <= MAX_SUBSQUARE_CELLS;
    }

    private static boolean canRenderSquareGrid(double southLat,
                                               double westLon,
                                               double northLat,
                                               double eastLon,
                                               double viewportWidthPx,
                                               double viewportHeightPx) {

        double cellWidthPx = estimateCellWidthPx(GridPrecision.SQUARE_4, westLon, eastLon, viewportWidthPx);
        double cellHeightPx = estimateCellHeightPx(GridPrecision.SQUARE_4, southLat, northLat, viewportHeightPx);
        int estimatedCellCount = estimateVisibleCellCount(GridPrecision.SQUARE_4, southLat, westLon, northLat, eastLon);

        return cellWidthPx >= MIN_SQUARE_CELL_WIDTH_PX
                && cellHeightPx >= MIN_SQUARE_CELL_HEIGHT_PX
                && estimatedCellCount <= MAX_SQUARE_CELLS;
    }

    private static int estimateVisibleCellCount(GridPrecision precision,
                                                double southLat,
                                                double westLon,
                                                double northLat,
                                                double eastLon) {

        double lonSpanDeg = Math.max(1e-6, eastLon - westLon);
        double latSpanDeg = Math.max(1e-6, northLat - southLat);

        int columns = Math.max(1, (int) Math.ceil(lonSpanDeg / precision.cellWidthDeg()));
        int rows = Math.max(1, (int) Math.ceil(latSpanDeg / precision.cellHeightDeg()));
        return columns * rows;
    }

    private static double estimateCellWidthPx(GridPrecision precision,
                                              double westLon,
                                              double eastLon,
                                              double viewportWidthPx) {

        double lonSpanDeg = Math.max(1e-6, eastLon - westLon);
        double visibleColumns = Math.max(1.0, lonSpanDeg / precision.cellWidthDeg());
        return viewportWidthPx / visibleColumns;
    }

    private static double estimateCellHeightPx(GridPrecision precision,
                                               double southLat,
                                               double northLat,
                                               double viewportHeightPx) {

        double latSpanDeg = Math.max(1e-6, northLat - southLat);
        double visibleRows = Math.max(1.0, latSpanDeg / precision.cellHeightDeg());
        return viewportHeightPx / visibleRows;
    }

    private static boolean shouldShowLabels(GridPrecision precision, double cellWidthPx, double cellHeightPx) {
        return switch (precision) {
            case FIELD_2 -> cellWidthPx >= 28.0 && cellHeightPx >= 14.0;
            case SQUARE_4 -> cellWidthPx >= 22.0 && cellHeightPx >= 14.0;
            case SUBSQUARE_6 -> cellWidthPx >= 18.0 && cellHeightPx >= 11.0;
        };
    }

    private static double desiredLabelWidthPx(GridPrecision precision) {
        return switch (precision) {
            case FIELD_2 -> 30.0;
            case SQUARE_4 -> 44.0;
            case SUBSQUARE_6 -> 56.0;
        };
    }

    private static double desiredLabelHeightPx(GridPrecision precision) {
        return switch (precision) {
            case FIELD_2 -> 18.0;
            case SQUARE_4 -> 18.0;
            case SUBSQUARE_6 -> 16.0;
        };
    }

    private static double estimateLabelFontSizePx(GridPrecision precision,
                                                  double cellWidthPx,
                                                  double cellHeightPx) {

        double minFontSizePx = switch (precision) {
            case FIELD_2 -> 14.0;
            case SQUARE_4 -> 11.5;
            case SUBSQUARE_6 -> 10.5;
        };

        double maxFontSizePx = switch (precision) {
            case FIELD_2 -> 18.0;
            case SQUARE_4 -> 15.0;
            case SUBSQUARE_6 -> 13.5;
        };

        double estimatedFontSizePx = Math.min(cellHeightPx * 0.55, cellWidthPx * 0.24);
        return clamp(estimatedFontSizePx, minFontSizePx, maxFontSizePx);
    }

    private static int computeStride(double cellSizePx, double desiredLabelSizePx) {
        return Math.max(1, (int) Math.ceil(desiredLabelSizePx / Math.max(1.0, cellSizePx)));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record GridRenderPlan(
            GridPrecision precision,
            boolean showLabels,
            int labelRowStride,
            int labelColumnStride,
            double estimatedCellWidthPx,
            double estimatedCellHeightPx,
            double labelFontSizePx
    ) {

        public boolean shouldShowLabel(MaidenheadGridUtils.GridCell cell) {
            if (!showLabels || cell == null) {
                return false;
            }

            return (cell.rowIndex() % labelRowStride) == 0
                    && (cell.columnIndex() % labelColumnStride) == 0;
        }
    }
}
