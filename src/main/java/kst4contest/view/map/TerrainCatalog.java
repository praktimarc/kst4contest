package kst4contest.view.map;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Immutable terrain catalog describing all downloadable terrain packages
 * currently offered by the service.
 *
 * <p>The catalog intentionally contains the required attribution/license texts
 * so they can later be shown both in the client UI and in service responses.</p>
 */
public record TerrainCatalog(
        int schemaVersion,
        int catalogVersion,
        String generatedAtUtc,
        String regionSet,
        String packageBaseUrl,
        String sourceAttribution,
        String licenseNotice,
        String disclaimerNotice,
        List<TerrainCatalogPackageEntry> packages
) {

    public TerrainCatalog {
        generatedAtUtc = normalizeText(generatedAtUtc);
        regionSet = normalizeLower(regionSet);
        packageBaseUrl = normalizeText(packageBaseUrl);
        sourceAttribution = normalizeText(sourceAttribution);
        licenseNotice = normalizeText(licenseNotice);
        disclaimerNotice = normalizeText(disclaimerNotice);
        packages = packages == null ? List.of() : List.copyOf(packages);

        if (schemaVersion < 0) {
            schemaVersion = 0;
        }

        if (catalogVersion < 0) {
            catalogVersion = 0;
        }
    }

    /**
     * Returns true if the catalog contains at least one package entry.
     *
     * @return true if the catalog is non-empty
     */
    public boolean hasPackages() {
        return !packages.isEmpty();
    }

    /**
     * Finds one package entry by its canonical package id.
     *
     * @param packageId canonical package id
     * @return matching catalog entry if found
     */
    public Optional<TerrainCatalogPackageEntry> findPackageById(String packageId) {
        String normalizedPackageId = normalizeLower(packageId);

        return packages.stream()
                .filter(entry -> entry.packageId().equals(normalizedPackageId))
                .findFirst();
    }

    /**
     * Finds one package entry by region type and region id.
     *
     * @param regionType region type, e.g. "maidenhead4"
     * @param regionId region id, e.g. "JO22"
     * @return matching package entry if found
     */
    public Optional<TerrainCatalogPackageEntry> findPackageByRegion(String regionType, String regionId) {
        String normalizedRegionType = normalizeLower(regionType);
        String normalizedRegionId = normalizeUpper(regionId);

        return packages.stream()
                .filter(entry -> entry.regionType().equals(normalizedRegionType))
                .filter(entry -> entry.regionId().equals(normalizedRegionId))
                .findFirst();
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