package kst4contest.view.map;

import java.util.Locale;

/**
 * Immutable input for one path analysis run.
 *
 * <p>The two ground elevation overrides replace the terrain elevation the profile
 * provider reports at the respective endpoint. {@code NaN} means "no override": the
 * elevation model decides, as it always did.</p>
 *
 * @param homeGroundElevationOverrideMeters own ground elevation above mean sea level
 *        entered by the operator, or {@code NaN}
 * @param targetGroundElevationOverrideMeters remote ground elevation already known to the
 *        caller (the highest point of its locator square), or {@code NaN}
 */
public record PathAnalysisRequest(
        String fromLocator6,
        double fromLatitudeDeg,
        double fromLongitudeDeg,
        String toCallsignRaw,
        String toLocator6,
        double toLatitudeDeg,
        double toLongitudeDeg,
        double frequencyMHz,
        double homeAntennaHeightMeters,
        double targetAntennaHeightMeters,
        double effectiveEarthRadiusFactor,
        PathLinkBudgetSettings linkBudgetSettings,
        double homeGroundElevationOverrideMeters,
        double targetGroundElevationOverrideMeters
) {

    /**
     * A request without ground elevation overrides.
     */
    public PathAnalysisRequest(String fromLocator6,
                               double fromLatitudeDeg,
                               double fromLongitudeDeg,
                               String toCallsignRaw,
                               String toLocator6,
                               double toLatitudeDeg,
                               double toLongitudeDeg,
                               double frequencyMHz,
                               double homeAntennaHeightMeters,
                               double targetAntennaHeightMeters,
                               double effectiveEarthRadiusFactor,
                               PathLinkBudgetSettings linkBudgetSettings) {
        this(
                fromLocator6,
                fromLatitudeDeg,
                fromLongitudeDeg,
                toCallsignRaw,
                toLocator6,
                toLatitudeDeg,
                toLongitudeDeg,
                frequencyMHz,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                effectiveEarthRadiusFactor,
                linkBudgetSettings,
                Double.NaN,
                Double.NaN
        );
    }

    public PathAnalysisRequest(String fromLocator6,
                               double fromLatitudeDeg,
                               double fromLongitudeDeg,
                               String toCallsignRaw,
                               String toLocator6,
                               double toLatitudeDeg,
                               double toLongitudeDeg,
                               double frequencyMHz,
                               double homeAntennaHeightMeters,
                               double targetAntennaHeightMeters) {
        this(
                fromLocator6,
                fromLatitudeDeg,
                fromLongitudeDeg,
                toCallsignRaw,
                toLocator6,
                toLatitudeDeg,
                toLongitudeDeg,
                frequencyMHz,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                PathLinkBudgetSettings.defaults()
        );
    }

    public PathAnalysisRequest {
        fromLocator6 = normalizeLocator(fromLocator6);
        toCallsignRaw = normalizeUpper(toCallsignRaw);
        toLocator6 = normalizeLocator(toLocator6);

        if (!Double.isFinite(frequencyMHz) || frequencyMHz <= 0.0) {
            frequencyMHz = Double.NaN;
        }

        if (!Double.isFinite(homeAntennaHeightMeters) || homeAntennaHeightMeters < 0.0) {
            homeAntennaHeightMeters = Double.NaN;
        }

        if (!Double.isFinite(targetAntennaHeightMeters) || targetAntennaHeightMeters < 0.0) {
            targetAntennaHeightMeters = Double.NaN;
        }

        effectiveEarthRadiusFactor = PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(effectiveEarthRadiusFactor);

        if (!Double.isFinite(homeGroundElevationOverrideMeters)) {
            homeGroundElevationOverrideMeters = Double.NaN;
        }

        if (!Double.isFinite(targetGroundElevationOverrideMeters)) {
            targetGroundElevationOverrideMeters = Double.NaN;
        }

        linkBudgetSettings = linkBudgetSettings == null
                ? PathLinkBudgetSettings.defaults()
                : linkBudgetSettings;
    }

    private static String normalizeLocator(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeUpper(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public boolean hasUsableHome() {
        return fromLocator6.length() == 6
                && Double.isFinite(fromLatitudeDeg)
                && Double.isFinite(fromLongitudeDeg);
    }

    public boolean hasUsableTarget() {
        return toLocator6.length() == 6
                && Double.isFinite(toLatitudeDeg)
                && Double.isFinite(toLongitudeDeg);
    }

    /**
     * Returns a copy whose remote endpoint is moved to the given position and ground
     * elevation. Locator and callsign stay, because the station is still the same.
     *
     * @param latitudeDeg new remote latitude
     * @param longitudeDeg new remote longitude
     * @param groundElevationMeters ground elevation there, or {@code NaN} for "ask the model"
     * @return the moved request
     */
    public PathAnalysisRequest withTargetEndpoint(double latitudeDeg,
                                                  double longitudeDeg,
                                                  double groundElevationMeters) {
        return new PathAnalysisRequest(
                fromLocator6,
                fromLatitudeDeg,
                fromLongitudeDeg,
                toCallsignRaw,
                toLocator6,
                latitudeDeg,
                longitudeDeg,
                frequencyMHz,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                effectiveEarthRadiusFactor,
                linkBudgetSettings,
                homeGroundElevationOverrideMeters,
                groundElevationMeters
        );
    }

    public boolean hasUsableFrequency() {
        return Double.isFinite(frequencyMHz) && frequencyMHz > 0.0;
    }
}