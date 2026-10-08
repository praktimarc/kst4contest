package kst4contest.view.map;

import java.util.Locale;

/**
 * Summary of the dominant terrain obstruction and a rough single-knife-edge
 * diffraction estimate.
 *
 * <p>This is intentionally only a severity indicator. It does not replace a full
 * propagation model with multiple diffraction edges, clutter, refractivity,
 * troposcatter, aircraft scatter or ducting.</p>
 */
public record PathObstructionSummary(
        double analysisFrequencyMHz,

        int dominantObstructionSampleIndex,
        double dominantObstructionPathDistanceKm,
        double dominantObstructionHeightAboveLosMeters,
        double localFirstFresnelRadiusMeters,
        double obstructionFresnelRatio,
        double diffractionVParameter,
        double estimatedKnifeEdgeLossDb,

        int worstFresnelIntrusionSampleIndex,
        double worstFresnelIntrusionPathDistanceKm,
        double worstFresnelIntrusionMeters,
        double worstFresnelIntrusionRatio
) {

    public static PathObstructionSummary empty() {
        return new PathObstructionSummary(
                Double.NaN,
                -1,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1,
                Double.NaN,
                Double.NaN,
                Double.NaN
        );
    }

    public boolean hasDominantLosObstruction() {
        return dominantObstructionSampleIndex >= 0
                && Double.isFinite(dominantObstructionPathDistanceKm)
                && Double.isFinite(dominantObstructionHeightAboveLosMeters)
                && dominantObstructionHeightAboveLosMeters > 0.0;
    }

    public boolean hasFresnelIntrusion() {
        return worstFresnelIntrusionSampleIndex >= 0
                && Double.isFinite(worstFresnelIntrusionMeters)
                && worstFresnelIntrusionMeters > 0.0;
    }

    public String obstructionText() {
        if (hasDominantLosObstruction()) {
            return String.format(
                    Locale.US,
                    "%.1f km | %.1f m above LOS | %.2f Fresnel radii | knife-edge ≈ %.1f dB (%s)",
                    dominantObstructionPathDistanceKm,
                    dominantObstructionHeightAboveLosMeters,
                    obstructionFresnelRatio,
                    estimatedKnifeEdgeLossDb,
                    severityText()
            );
        }

        if (hasFresnelIntrusion()) {
            return String.format(
                    Locale.US,
                    "No LOS-blocking edge. Fresnel intrusion %.1f m at %.1f km (%.0f%%).",
                    worstFresnelIntrusionMeters,
                    worstFresnelIntrusionPathDistanceKm,
                    worstFresnelIntrusionRatio * 100.0
            );
        }

        return "No dominant LOS obstruction detected.";
    }

    public String severityText() {
        if (!Double.isFinite(estimatedKnifeEdgeLossDb)) {
            return "unknown";
        }

        if (estimatedKnifeEdgeLossDb < 6.0) {
            return "low";
        }

        if (estimatedKnifeEdgeLossDb < 15.0) {
            return "moderate";
        }

        if (estimatedKnifeEdgeLossDb < 25.0) {
            return "high";
        }

        return "severe";
    }
}