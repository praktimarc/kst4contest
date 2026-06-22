package kst4contest.view.map;

import java.util.Locale;

/**
 * Operator-facing propagation assessment derived from geometric path analysis.
 *
 * <p>This class deliberately avoids binary "possible/impossible" wording.
 * VHF/UHF paths can work despite blocked geometric LOS due to diffraction,
 * troposcatter, enhanced tropospheric refraction, ducting or aircraft scatter.</p>
 */
public record PathPropagationAssessment(
        String category,
        String shortText,
        String detailText,
        String likelyMechanisms,
        int severityLevel
) {

    public static PathPropagationAssessment unknown() {
        return new PathPropagationAssessment(
                "Unknown",
                "No propagation assessment available.",
                "No usable terrain/profile data is available.",
                "-",
                0
        );
    }

    public static PathPropagationAssessment directFavorable() {
        return new PathPropagationAssessment(
                "Direct path favorable",
                "Direct path likely",
                "The geometric line of sight and the first Fresnel zone are clear. A direct tropospheric path is plausible.",
                "Direct tropospheric path",
                1
        );
    }

    public static PathPropagationAssessment directLossy(double fresnelIntrusionRatio) {
        return new PathPropagationAssessment(
                "Direct path lossy",
                "Direct path plausible, but lossy",
                String.format(
                        Locale.US,
                        "The direct line of sight is clear, but the first Fresnel zone is obstructed by about %.0f%% of the local Fresnel radius. Expect additional loss.",
                        fresnelIntrusionRatio * 100.0
                ),
                "Direct path with Fresnel loss, possible mild diffraction",
                2
        );
    }

    public static PathPropagationAssessment diffractionPlausible(double knifeEdgeLossDb) {
        return new PathPropagationAssessment(
                "Diffraction plausible",
                "Obstructed, diffraction may still be plausible",
                String.format(
                        Locale.US,
                        "The direct geometric path is blocked, but the rough single-knife-edge estimate is moderate at about %.1f dB. A QSO may still be possible with sufficient antennas, power and conditions.",
                        knifeEdgeLossDb
                ),
                "Terrain diffraction, tropo enhancement",
                3
        );
    }

    public static PathPropagationAssessment obstructedNeedsHelp(double knifeEdgeLossDb) {
        return new PathPropagationAssessment(
                "Obstructed",
                "Obstructed, enhanced propagation likely required",
                String.format(
                        Locale.US,
                        "The geometric path is blocked and the rough single-knife-edge estimate is high at about %.1f dB. Direct diffraction alone may be weak; tropo enhancement or scatter mechanisms become more relevant.",
                        knifeEdgeLossDb
                ),
                "Diffraction, troposcatter, tropo enhancement, aircraft scatter",
                4
        );
    }

    public static PathPropagationAssessment severelyObstructed(double knifeEdgeLossDb) {
        return new PathPropagationAssessment(
                "Severely obstructed",
                "Severely obstructed, special propagation probably required",
                String.format(
                        Locale.US,
                        "The geometric path is strongly blocked. The rough single-knife-edge estimate is about %.1f dB, so a normal direct path is unlikely. This does not mean impossible on VHF/UHF, but special propagation is probably needed.",
                        knifeEdgeLossDb
                ),
                "Aircraft scatter, tropo ducting/enhancement, troposcatter, strong diffraction only in exceptional cases",
                5
        );
    }

    public static PathPropagationAssessment blockedNoLossEstimate() {
        return new PathPropagationAssessment(
                "Blocked",
                "Geometrically blocked",
                "The geometric line of sight is blocked. No reliable diffraction-loss estimate is available for this profile.",
                "Diffraction, tropo enhancement, troposcatter, aircraft scatter",
                4
        );
    }
}