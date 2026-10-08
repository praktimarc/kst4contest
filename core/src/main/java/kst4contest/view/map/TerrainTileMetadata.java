package kst4contest.view.map;

import java.util.Locale;

/**
 * Immutable metadata for one internal terrain tile.
 *
 * <p>This model intentionally describes the runtime-ready tile after ingestion
 * into the KST4Contest terrain format, not the original upstream GeoTIFF.</p>
 */
public record TerrainTileMetadata(
        String tileId,
        String fileName,
        int southDeg,
        int westDeg,
        int width,
        int height,
        int arcSecondResolution,
        short noDataValue,
        String sourceDataset,
        String sha256
) {

    public TerrainTileMetadata {
        tileId = normalizeUpper(tileId);
        fileName = normalizeText(fileName);
        sourceDataset = normalizeText(sourceDataset);
        sha256 = normalizeLower(sha256);

        if (width < 0) {
            width = 0;
        }

        if (height < 0) {
            height = 0;
        }

        if (arcSecondResolution < 0) {
            arcSecondResolution = 0;
        }
    }

    /**
     * Returns the northern edge of the covered 1° x 1° geocell.
     *
     * @return north edge latitude in degrees
     */
    public int northDeg() {
        return southDeg + 1;
    }

    /**
     * Returns the eastern edge of the covered 1° x 1° geocell.
     *
     * @return east edge longitude in degrees
     */
    public int eastDeg() {
        return westDeg + 1;
    }

    /**
     * Returns true if this metadata appears complete enough for installation/runtime.
     *
     * @return true if the essential fields are usable
     */
    public boolean isUsable() {
        return !tileId.isBlank()
                && !fileName.isBlank()
                && width > 0
                && height > 0
                && arcSecondResolution > 0;
    }

    /**
     * Returns true if the tile covers the given geographic sample point.
     *
     * <p>The tile is interpreted as the 1° x 1° cell
     * [southDeg, southDeg+1) x [westDeg, westDeg+1).</p>
     *
     * @param latitudeDeg sample latitude in degrees
     * @param longitudeDeg sample longitude in degrees
     * @return true if the point lies inside this tile
     */
    public boolean covers(double latitudeDeg, double longitudeDeg) {
        return Double.isFinite(latitudeDeg)
                && Double.isFinite(longitudeDeg)
                && latitudeDeg >= southDeg
                && latitudeDeg < northDeg()
                && longitudeDeg >= westDeg
                && longitudeDeg < eastDeg();
    }

    /**
     * Builds the canonical internal tile id for one 1° x 1° cell.
     *
     * Examples:
     * <ul>
     *     <li>N51_E007</li>
     *     <li>N52_W003</li>
     *     <li>S01_E010</li>
     * </ul>
     *
     * @param southDeg southern cell boundary in degrees
     * @param westDeg western cell boundary in degrees
     * @return canonical tile id
     */
    public static String buildTileId(int southDeg, int westDeg) {
        String latPrefix = southDeg >= 0 ? "N" : "S";
        String lonPrefix = westDeg >= 0 ? "E" : "W";

        return String.format(
                Locale.ROOT,
                "%s%02d_%s%03d",
                latPrefix,
                Math.abs(southDeg),
                lonPrefix,
                Math.abs(westDeg)
        );
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}