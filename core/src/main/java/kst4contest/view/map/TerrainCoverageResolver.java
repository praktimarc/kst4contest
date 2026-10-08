package kst4contest.view.map;

import kst4contest.locatorUtils.Location;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves which terrain packages and which 1° tiles are required for
 * a path analysis request.
 *
 * <p>This class is intentionally self-contained so it can later be reused
 * unchanged both in the desktop client and in a future Spring service.</p>
 */
public final class TerrainCoverageResolver {

    /**
     * The current package scheme groups terrain downloads by Maidenhead-4 region.
     */
    public static final String REGION_TYPE_MAIDENHEAD4 = "maidenhead4";

    /**
     * Fixed region set label for the first Europe-wide terrain service generation.
     */
    public static final String REGION_SET_EU = "eu";

    /**
     * Sampling step used to determine required coverage along a path.
     *
     * <p>This is intentionally finer than the final profile sampling so that
     * package/tile coverage is not missed on diagonal or border-crossing paths.</p>
     */
    private static final double COVERAGE_SAMPLE_STEP_KM = 10.0;

    /**
     * Safety minimum number of sampling points per path.
     */
    private static final int MIN_COVERAGE_SAMPLE_COUNT = 32;

    public TerrainCoverageResolver() {
    }

    /**
     * Resolves all package and tile requirements for the given path request.
     *
     * @param request immutable path analysis request
     * @return combined package/tile coverage selection
     */
    public TerrainCoverageSelection resolveCoverageForPath(PathAnalysisRequest request) {
        if (request == null || !request.hasUsableHome() || !request.hasUsableTarget()) {
            return TerrainCoverageSelection.empty();
        }

        return resolveCoverageForPath(
                request.fromLatitudeDeg(),
                request.fromLongitudeDeg(),
                request.toLatitudeDeg(),
                request.toLongitudeDeg()
        );
    }

    /**
     * Resolves all package and tile requirements for the given geographic path.
     *
     * @param fromLatitudeDeg source latitude in degrees
     * @param fromLongitudeDeg source longitude in degrees
     * @param toLatitudeDeg target latitude in degrees
     * @param toLongitudeDeg target longitude in degrees
     * @return combined package/tile coverage selection
     */
    public TerrainCoverageSelection resolveCoverageForPath(double fromLatitudeDeg,
                                                           double fromLongitudeDeg,
                                                           double toLatitudeDeg,
                                                           double toLongitudeDeg) {

        if (!areUsableCoordinates(fromLatitudeDeg, fromLongitudeDeg)
                || !areUsableCoordinates(toLatitudeDeg, toLongitudeDeg)) {
            return TerrainCoverageSelection.empty();
        }

        double totalDistanceKm = calculateGreatCircleDistanceKm(
                fromLatitudeDeg,
                fromLongitudeDeg,
                toLatitudeDeg,
                toLongitudeDeg
        );

        int coverageSampleCount = resolveCoverageSampleCount(totalDistanceKm);

        LinkedHashSet<String> requiredRegionIds = new LinkedHashSet<>();
        LinkedHashSet<String> requiredPackageIds = new LinkedHashSet<>();
        LinkedHashSet<String> requiredTileIds = new LinkedHashSet<>();

        for (int sampleIndex = 0; sampleIndex < coverageSampleCount; sampleIndex++) {
            double t = coverageSampleCount == 1
                    ? 0.0
                    : (double) sampleIndex / (double) (coverageSampleCount - 1);

            GeoPoint point = interpolateGreatCirclePoint(
                    fromLatitudeDeg,
                    fromLongitudeDeg,
                    toLatitudeDeg,
                    toLongitudeDeg,
                    t
            );

            if (!areUsableCoordinates(point.latitudeDeg(), point.longitudeDeg())) {
                continue;
            }

            String maidenhead4 = toMaidenhead4(point.latitudeDeg(), point.longitudeDeg());
            if (!maidenhead4.isBlank()) {
                requiredRegionIds.add(maidenhead4);
                requiredPackageIds.add(buildPackageId(REGION_SET_EU, maidenhead4, 1));
            }

            int southDeg = floorDegree(point.latitudeDeg());
            int westDeg = floorDegree(point.longitudeDeg());
            requiredTileIds.add(TerrainTileMetadata.buildTileId(southDeg, westDeg));
        }

        return new TerrainCoverageSelection(
                List.copyOf(requiredRegionIds),
                List.copyOf(requiredPackageIds),
                List.copyOf(requiredTileIds),
                totalDistanceKm,
                coverageSampleCount
        );
    }

    /**
     * Resolves only the required package ids for the given path.
     *
     * @param request immutable path analysis request
     * @return ordered distinct package ids
     */
    public List<String> resolveRequiredPackageIdsForPath(PathAnalysisRequest request) {
        return resolveCoverageForPath(request).packageIds();
    }

    /**
     * Resolves only the required tile ids for the given path.
     *
     * @param request immutable path analysis request
     * @return ordered distinct tile ids
     */
    public List<String> resolveRequiredTileIdsForPath(PathAnalysisRequest request) {
        return resolveCoverageForPath(request).tileIds();
    }

    /**
     * Builds the canonical package id for one region package.
     *
     * Example:
     * <ul>
     *     <li>terrain-eu-jo22-v1</li>
     * </ul>
     *
     * @param regionSet region set, e.g. "eu"
     * @param regionId region id, e.g. "JO22"
     * @param packageVersion package version
     * @return canonical package id
     */
    public static String buildPackageId(String regionSet, String regionId, int packageVersion) {
        String normalizedRegionSet = regionSet == null ? "" : regionSet.trim().toLowerCase(Locale.ROOT);
        String normalizedRegionId = regionId == null ? "" : regionId.trim().toLowerCase(Locale.ROOT);
        int normalizedPackageVersion = Math.max(0, packageVersion);

        return String.format(
                Locale.ROOT,
                "terrain-%s-%s-v%d",
                normalizedRegionSet,
                normalizedRegionId,
                normalizedPackageVersion
        );
    }

    /**
     * Returns the Maidenhead-4 region id for the given point.
     *
     * @param latitudeDeg latitude in degrees
     * @param longitudeDeg longitude in degrees
     * @return Maidenhead-4 id such as JO22, or empty string if conversion failed
     */
    public static String toMaidenhead4(double latitudeDeg, double longitudeDeg) {
        if (!areUsableCoordinates(latitudeDeg, longitudeDeg)) {
            return "";
        }

        String maidenhead6 = Location.toMaidenhead(latitudeDeg, longitudeDeg);
        if (maidenhead6 == null || maidenhead6.length() < 4) {
            return "";
        }

        return maidenhead6.substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private static int resolveCoverageSampleCount(double totalDistanceKm) {
        if (!Double.isFinite(totalDistanceKm) || totalDistanceKm <= 0.0) {
            return MIN_COVERAGE_SAMPLE_COUNT;
        }

        int computedSampleCount = (int) Math.ceil(totalDistanceKm / COVERAGE_SAMPLE_STEP_KM) + 1;
        return Math.max(MIN_COVERAGE_SAMPLE_COUNT, computedSampleCount);
    }

    private static int floorDegree(double value) {
        return (int) Math.floor(value);
    }

    private static boolean areUsableCoordinates(double latitudeDeg, double longitudeDeg) {
        return Double.isFinite(latitudeDeg)
                && Double.isFinite(longitudeDeg)
                && latitudeDeg >= -90.0
                && latitudeDeg <= 90.0
                && longitudeDeg >= -180.0
                && longitudeDeg <= 180.0;
    }

    /**
     * Great-circle distance in kilometers using the haversine formula.
     *
     * @param fromLatitudeDeg source latitude in degrees
     * @param fromLongitudeDeg source longitude in degrees
     * @param toLatitudeDeg target latitude in degrees
     * @param toLongitudeDeg target longitude in degrees
     * @return great-circle distance in kilometers
     */
    private static double calculateGreatCircleDistanceKm(double fromLatitudeDeg,
                                                         double fromLongitudeDeg,
                                                         double toLatitudeDeg,
                                                         double toLongitudeDeg) {

        double fromLatitudeRad = Math.toRadians(fromLatitudeDeg);
        double fromLongitudeRad = Math.toRadians(fromLongitudeDeg);
        double toLatitudeRad = Math.toRadians(toLatitudeDeg);
        double toLongitudeRad = Math.toRadians(toLongitudeDeg);

        double deltaLatitude = toLatitudeRad - fromLatitudeRad;
        double deltaLongitude = toLongitudeRad - fromLongitudeRad;

        double a = Math.sin(deltaLatitude / 2.0) * Math.sin(deltaLatitude / 2.0)
                + Math.cos(fromLatitudeRad) * Math.cos(toLatitudeRad)
                * Math.sin(deltaLongitude / 2.0) * Math.sin(deltaLongitude / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));

        return 6371.009 * c;
    }

    /**
     * Interpolates one point on the great-circle path between the given endpoints.
     *
     * @param fromLatitudeDeg source latitude in degrees
     * @param fromLongitudeDeg source longitude in degrees
     * @param toLatitudeDeg target latitude in degrees
     * @param toLongitudeDeg target longitude in degrees
     * @param t interpolation factor within [0, 1]
     * @return interpolated geographic point
     */
    private static GeoPoint interpolateGreatCirclePoint(double fromLatitudeDeg,
                                                        double fromLongitudeDeg,
                                                        double toLatitudeDeg,
                                                        double toLongitudeDeg,
                                                        double t) {

        double clampedT = clamp(t, 0.0, 1.0);

        if (clampedT <= 0.0) {
            return new GeoPoint(fromLatitudeDeg, normalizeLongitudeDeg(fromLongitudeDeg));
        }

        if (clampedT >= 1.0) {
            return new GeoPoint(toLatitudeDeg, normalizeLongitudeDeg(toLongitudeDeg));
        }

        double fromLatitudeRad = Math.toRadians(fromLatitudeDeg);
        double fromLongitudeRad = Math.toRadians(fromLongitudeDeg);
        double toLatitudeRad = Math.toRadians(toLatitudeDeg);
        double toLongitudeRad = Math.toRadians(toLongitudeDeg);

        double x1 = Math.cos(fromLatitudeRad) * Math.cos(fromLongitudeRad);
        double y1 = Math.cos(fromLatitudeRad) * Math.sin(fromLongitudeRad);
        double z1 = Math.sin(fromLatitudeRad);

        double x2 = Math.cos(toLatitudeRad) * Math.cos(toLongitudeRad);
        double y2 = Math.cos(toLatitudeRad) * Math.sin(toLongitudeRad);
        double z2 = Math.sin(toLatitudeRad);

        double dot = clamp(x1 * x2 + y1 * y2 + z1 * z2, -1.0, 1.0);
        double omega = Math.acos(dot);

        if (omega < 1e-12) {
            double latitudeDeg = fromLatitudeDeg + (toLatitudeDeg - fromLatitudeDeg) * clampedT;
            double longitudeDeg = normalizeLongitudeDeg(fromLongitudeDeg + (toLongitudeDeg - fromLongitudeDeg) * clampedT);
            return new GeoPoint(latitudeDeg, longitudeDeg);
        }

        double sinOmega = Math.sin(omega);
        double a = Math.sin((1.0 - clampedT) * omega) / sinOmega;
        double b = Math.sin(clampedT * omega) / sinOmega;

        double x = a * x1 + b * x2;
        double y = a * y1 + b * y2;
        double z = a * z1 + b * z2;

        double latitudeRad = Math.atan2(z, Math.sqrt(x * x + y * y));
        double longitudeRad = Math.atan2(y, x);

        return new GeoPoint(
                Math.toDegrees(latitudeRad),
                normalizeLongitudeDeg(Math.toDegrees(longitudeRad))
        );
    }

    private static double normalizeLongitudeDeg(double longitudeDeg) {
        double normalized = longitudeDeg % 360.0;

        if (normalized > 180.0) {
            normalized -= 360.0;
        } else if (normalized <= -180.0) {
            normalized += 360.0;
        }

        return normalized;
    }

    private static double clamp(double value, double minValue, double maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    /**
     * Combined path coverage selection.
     *
     * @param regionIds ordered distinct Maidenhead-4 region ids
     * @param packageIds ordered distinct package ids
     * @param tileIds ordered distinct 1° tile ids
     * @param totalDistanceKm great-circle path distance in kilometers
     * @param coverageSampleCount number of sampling points used for coverage resolution
     */
    public record TerrainCoverageSelection(
            List<String> regionIds,
            List<String> packageIds,
            List<String> tileIds,
            double totalDistanceKm,
            int coverageSampleCount
    ) {
        public TerrainCoverageSelection {
            regionIds = regionIds == null ? List.of() : List.copyOf(regionIds);
            packageIds = packageIds == null ? List.of() : List.copyOf(packageIds);
            tileIds = tileIds == null ? List.of() : List.copyOf(tileIds);

            if (coverageSampleCount < 0) {
                coverageSampleCount = 0;
            }
        }

        public static TerrainCoverageSelection empty() {
            return new TerrainCoverageSelection(List.of(), List.of(), List.of(), Double.NaN, 0);
        }

        public boolean hasCoverage() {
            return !packageIds.isEmpty() || !tileIds.isEmpty();
        }
    }

    /**
     * Small immutable geographic point for great-circle interpolation.
     *
     * @param latitudeDeg latitude in degrees
     * @param longitudeDeg longitude in degrees
     */
    private record GeoPoint(double latitudeDeg, double longitudeDeg) {
    }
}