package kst4contest.view.map;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Immutable manifest describing one locally installable terrain package.
 *
 * <p>This model is the authoritative description of the package contents after
 * download and before/after installation.</p>
 */
public record TerrainPackageManifest(
        int schemaVersion,
        String packageId,
        int packageVersion,
        String regionType,
        String regionId,
        double minLatitudeDeg,
        double maxLatitudeDeg,
        double minLongitudeDeg,
        double maxLongitudeDeg,
        String primaryDataset,
        String fallbackDataset,
        String packageBuiltAtUtc,
        String sourceAttribution,
        String derivedProductNotice,
        String disclaimerNotice,
        String packageSha256,
        List<TerrainTileMetadata> tiles
) {

    public TerrainPackageManifest {
        packageId = normalizeLower(packageId);
        regionType = normalizeLower(regionType);
        regionId = normalizeUpper(regionId);
        primaryDataset = normalizeLower(primaryDataset);
        fallbackDataset = normalizeLower(fallbackDataset);
        packageBuiltAtUtc = normalizeText(packageBuiltAtUtc);
        sourceAttribution = normalizeText(sourceAttribution);
        derivedProductNotice = normalizeText(derivedProductNotice);
        disclaimerNotice = normalizeText(disclaimerNotice);
        packageSha256 = normalizeLower(packageSha256);
        tiles = tiles == null ? List.of() : List.copyOf(tiles);

        if (schemaVersion < 0) {
            schemaVersion = 0;
        }

        if (packageVersion < 0) {
            packageVersion = 0;
        }
    }

    /**
     * Returns true if the manifest contains at least one usable tile.
     *
     * @return true if the manifest appears installable
     */
    public boolean hasUsableTiles() {
        return tiles.stream().anyMatch(TerrainTileMetadata::isUsable);
    }

    /**
     * Finds one tile metadata entry by canonical tile id.
     *
     * @param tileId canonical tile id
     * @return tile metadata if found
     */
    public Optional<TerrainTileMetadata> findTile(String tileId) {
        String normalizedTileId = normalizeUpper(tileId);

        return tiles.stream()
                .filter(tile -> tile.tileId().equals(normalizedTileId))
                .findFirst();
    }

    /**
     * Returns true if the manifest belongs to the given region.
     *
     * @param expectedRegionType region type
     * @param expectedRegionId region id
     * @return true if region type and id match
     */
    public boolean matchesRegion(String expectedRegionType, String expectedRegionId) {
        return regionType.equals(normalizeLower(expectedRegionType))
                && regionId.equals(normalizeUpper(expectedRegionId));
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