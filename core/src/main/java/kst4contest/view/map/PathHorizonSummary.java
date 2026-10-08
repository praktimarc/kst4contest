package kst4contest.view.map;

import java.util.Locale;

/**
 * Immutable summary of simple radio-horizon and terrain-profile horizon data.
 *
 * <p>The simple radio horizon is based only on antenna height and effective
 * Earth radius. The terrain horizon is derived from the actual path profile and
 * therefore represents the highest apparent terrain angle from each endpoint.</p>
 */
public record PathHorizonSummary(
        double effectiveEarthRadiusFactor,

        double homeSimpleRadioHorizonKm,
        double targetSimpleRadioHorizonKm,
        double combinedSimpleRadioHorizonKm,

        double homeTerrainHorizonPathDistanceKm,
        double homeTerrainHorizonElevationAngleDeg,
        int homeTerrainHorizonSampleIndex,

        double targetTerrainHorizonPathDistanceKm,
        double targetTerrainHorizonDistanceFromTargetKm,
        double targetTerrainHorizonElevationAngleDeg,
        int targetTerrainHorizonSampleIndex
) {

    public PathHorizonSummary {
        effectiveEarthRadiusFactor =
                PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(effectiveEarthRadiusFactor);
    }

    public static PathHorizonSummary empty() {
        return new PathHorizonSummary(
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1
        );
    }

    public boolean hasHomeTerrainHorizon() {
        return homeTerrainHorizonSampleIndex >= 0
                && Double.isFinite(homeTerrainHorizonPathDistanceKm)
                && Double.isFinite(homeTerrainHorizonElevationAngleDeg);
    }

    public boolean hasTargetTerrainHorizon() {
        return targetTerrainHorizonSampleIndex >= 0
                && Double.isFinite(targetTerrainHorizonPathDistanceKm)
                && Double.isFinite(targetTerrainHorizonDistanceFromTargetKm)
                && Double.isFinite(targetTerrainHorizonElevationAngleDeg);
    }

    public String effectiveEarthRadiusText() {
        return String.format(
                Locale.US,
                "k = %.2f effective Earth radius",
                effectiveEarthRadiusFactor
        );
    }

    public String simpleRadioHorizonText() {
        if (!Double.isFinite(homeSimpleRadioHorizonKm)
                || !Double.isFinite(targetSimpleRadioHorizonKm)
                || !Double.isFinite(combinedSimpleRadioHorizonKm)) {
            return "-";
        }

        return String.format(
                Locale.US,
                "Home %.1f km | DX %.1f km | combined %.1f km",
                homeSimpleRadioHorizonKm,
                targetSimpleRadioHorizonKm,
                combinedSimpleRadioHorizonKm
        );
    }

    public String terrainHorizonText() {
        String homeText = hasHomeTerrainHorizon()
                ? String.format(
                Locale.US,
                "Home %.1f km / %+.2f°",
                homeTerrainHorizonPathDistanceKm,
                homeTerrainHorizonElevationAngleDeg
        )
                : "Home -";

        String targetText = hasTargetTerrainHorizon()
                ? String.format(
                Locale.US,
                "DX %.1f km from DX / %+.2f°",
                targetTerrainHorizonDistanceFromTargetKm,
                targetTerrainHorizonElevationAngleDeg
        )
                : "DX -";

        return homeText + " | " + targetText;
    }
}