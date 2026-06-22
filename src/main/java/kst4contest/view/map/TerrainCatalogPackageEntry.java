package kst4contest.view.map;

import java.util.List;
import java.util.Locale;

/**
 * Immutable catalog entry describing one downloadable terrain package.
 *
 * <p>This model is intentionally shared between the future server-side catalog
 * generation and the future desktop downloader.</p>
 */
public record TerrainCatalogPackageEntry(
        String packageId,
        String regionType,
        String regionId,
        int packageVersion,
        String downloadUrl,
        long sizeBytes,
        String sha256,
        double minLatitudeDeg,
        double maxLatitudeDeg,
        double minLongitudeDeg,
        double maxLongitudeDeg,
        List<String> tileIds,
        String sourceDataset,
        String sourceAttribution,
        String derivedProductNotice,
        String disclaimerNotice
) {

    public TerrainCatalogPackageEntry {
        packageId = normalizeLower(packageId);
        regionType = normalizeLower(regionType);
        regionId = normalizeUpper(regionId);
        downloadUrl = normalizeText(downloadUrl);
        sha256 = normalizeLower(sha256);
        tileIds = tileIds == null ? List.of() : tileIds.stream()
                                                .map(TerrainCatalogPackageEntry::normalizeUpper)
                                                .filter(value -> !value.isBlank())
                                                .distinct()
                                                .toList();
        sourceDataset = normalizeLower(sourceDataset);
        sourceAttribution = normalizeText(sourceAttribution);
        derivedProductNotice = normalizeText(derivedProductNotice);
        disclaimerNotice = normalizeText(disclaimerNotice);

        if (packageVersion < 0) {
            packageVersion = 0;
        }

        if (sizeBytes < 0L) {
            sizeBytes = 0L;
        }
    }

    /**
     * Returns true if the entry contains enough information for download/install logic.
     *
     * @return true if the entry is usable
     */
    public boolean isUsable() {
        return !packageId.isBlank()
                && !regionType.isBlank()
                && !regionId.isBlank()
                && !downloadUrl.isBlank();
    }

    /**
     * Returns true if the package matches the given region identifier.
     *
     * @param expectedRegionType region type, e.g. "maidenhead4"
     * @param expectedRegionId region id, e.g. "JO22"
     * @return true if both match
     */
    public boolean matchesRegion(String expectedRegionType, String expectedRegionId) {
        return regionType.equals(normalizeLower(expectedRegionType))
                && regionId.equals(normalizeUpper(expectedRegionId));
    }

    /**
     * Returns true if the bounding box covers the given point.
     *
     * @param latitudeDeg latitude in degrees
     * @param longitudeDeg longitude in degrees
     * @return true if the point lies inside the package coverage box
     */
    public boolean covers(double latitudeDeg, double longitudeDeg) {
        return Double.isFinite(latitudeDeg)
                && Double.isFinite(longitudeDeg)
                && latitudeDeg >= minLatitudeDeg
                && latitudeDeg <= maxLatitudeDeg
                && longitudeDeg >= minLongitudeDeg
                && longitudeDeg <= maxLongitudeDeg;
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