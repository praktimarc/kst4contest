package kst4contest.view.map;

import java.util.Locale;

/**
 * Immutable input for one path analysis run.
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
        PathLinkBudgetSettings linkBudgetSettings
) {

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

        PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(effectiveEarthRadiusFactor);

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

    public boolean hasUsableFrequency() {
        return Double.isFinite(frequencyMHz) && frequencyMHz > 0.0;
    }
}