package kst4contest.view.map;

import java.util.List;
import java.util.Locale;

/**
 * Immutable UI-facing result for one path analysis run.
 *
 * <p>This result intentionally combines:
 * <ul>
 *     <li>basic path metadata used by the detail panel</li>
 *     <li>the enriched path profile used by the preview chart</li>
 *     <li>formatted state helpers for concise UI binding</li>
 * </ul>
 *
 * <p>The chart and the numeric evaluation both consume the same already
 * computed profile data to avoid drift between visual and numeric output.</p>
 */
public record PathAnalysisResult(
        String analysisMode,
        String fromLocator6,
        String toLocator6,
        String toCallsignRaw,
        double distanceKm,
        double bearingDeg,
        double homeAntennaHeightMeters,
        double targetAntennaHeightMeters,
        double analysisFrequencyMHz,
        boolean lineOfSightClear,
        boolean fresnelClear,
        double minimumLineOfSightClearanceMeters,
        double minimumLowerFresnelClearanceMeters,
        double worstFresnelIntrusionMeters,
        double worstFresnelIntrusionRatio,
        double worstFresnelDistanceKm,
        int worstFresnelSampleIndex,
        double effectiveEarthRadiusFactor,
        PathHorizonSummary horizonSummary,
        PathObstructionSummary obstructionSummary,
        PathPropagationAssessment propagationAssessment,
        String statusText,
        List<PathProfilePoint> profilePoints,
        PathLinkBudgetSummary linkBudgetSummary
) {

    public PathAnalysisResult {
        analysisMode = normalizeText(analysisMode);
        fromLocator6 = normalizeUpper(fromLocator6);
        toLocator6 = normalizeUpper(toLocator6);
        toCallsignRaw = normalizeUpper(toCallsignRaw);
        statusText = normalizeText(statusText);
        profilePoints = profilePoints == null ? List.of() : List.copyOf(profilePoints);
        effectiveEarthRadiusFactor =
                PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(effectiveEarthRadiusFactor);

        horizonSummary = horizonSummary == null ? PathHorizonSummary.empty() : horizonSummary;
        obstructionSummary = obstructionSummary == null ? PathObstructionSummary.empty() : obstructionSummary;

        propagationAssessment = propagationAssessment == null
                ? PathPropagationAssessment.unknown()
                : propagationAssessment;

        linkBudgetSummary = linkBudgetSummary == null
                ? PathLinkBudgetSummary.empty()
                : linkBudgetSummary;
    }

    public PathAnalysisResult(String analysisMode,
                              String fromLocator6,
                              String toLocator6,
                              String toCallsignRaw,
                              double distanceKm,
                              double bearingDeg,
                              double homeAntennaHeightMeters,
                              double targetAntennaHeightMeters,
                              double analysisFrequencyMHz,
                              boolean lineOfSightClear,
                              boolean fresnelClear,
                              double minimumLineOfSightClearanceMeters,
                              double minimumLowerFresnelClearanceMeters,
                              double worstFresnelIntrusionMeters,
                              double worstFresnelIntrusionRatio,
                              double worstFresnelDistanceKm,
                              int worstFresnelSampleIndex,
                              String statusText,
                              List<PathProfilePoint> profilePoints) {
        this(
                analysisMode,
                fromLocator6,
                toLocator6,
                toCallsignRaw,
                distanceKm,
                bearingDeg,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                analysisFrequencyMHz,
                lineOfSightClear,
                fresnelClear,
                minimumLineOfSightClearanceMeters,
                minimumLowerFresnelClearanceMeters,
                worstFresnelIntrusionMeters,
                worstFresnelIntrusionRatio,
                worstFresnelDistanceKm,
                worstFresnelSampleIndex,
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                PathHorizonSummary.empty(),
                PathObstructionSummary.empty(),
                PathPropagationAssessment.unknown(),
                statusText,
                profilePoints,
                PathLinkBudgetSummary.empty()
        );
    }

    /**
     * Creates a placeholder result shown while no target station is selected.
     *
     * @param fromLocator6 normalized own locator if known
     * @return waiting result
     */
    public static PathAnalysisResult waitingForSelection(String fromLocator6) {
        return new PathAnalysisResult(
                "Waiting",
                fromLocator6,
                "",
                "",
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                false,
                false,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1,
                "Select a station to prepare path and terrain analysis.",
                List.of()
        );
    }

    /**
     * Creates a placeholder result shown while analysis is currently running.
     *
     * @param fromLocator6 own locator
     * @param toLocator6 target locator if known
     * @param toCallsignRaw target callsign if known
     * @return loading result
     */
    public static PathAnalysisResult loading(String fromLocator6,
                                             String toLocator6,
                                             String toCallsignRaw) {
        return new PathAnalysisResult(
                "Loading",
                fromLocator6,
                toLocator6,
                toCallsignRaw,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                false,
                false,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1,
                "Loading terrain/profile data and evaluating the path.",
                List.of()
        );
    }

    /**
     * Creates a placeholder result shown when the own locator is not usable.
     *
     * @param fromLocator6 own locator candidate
     * @param toLocator6 target locator candidate
     * @return waiting result with home-locator warning
     */
    public static PathAnalysisResult waitingForValidHomeLocator(String fromLocator6,
                                                                String toLocator6) {
        return new PathAnalysisResult(
                "Waiting",
                fromLocator6,
                toLocator6,
                "",
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                false,
                false,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1,
                "Own locator is missing or invalid. Path analysis cannot start yet.",
                List.of()
        );
    }

    /**
     * Creates a placeholder result shown when the selected target has no usable position.
     *
     * @param fromLocator6 own locator
     * @param toLocator6 target locator candidate
     * @return waiting result with target-position warning
     */
    public static PathAnalysisResult waitingForValidTarget(String fromLocator6,
                                                           String toLocator6) {
        return new PathAnalysisResult(
                "Waiting",
                fromLocator6,
                toLocator6,
                "",
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                false,
                false,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1,
                "Selected station has no usable locator/position for path analysis.",
                List.of()
        );
    }

    /**
     * Creates a placeholder when automatic band resolution has no usable result.
     */
    public static PathAnalysisResult waitingForUsableBand(String fromLocator6,
                                                          String toLocator6,
                                                          String toCallsignRaw) {
        return new PathAnalysisResult(
                "Waiting",
                fromLocator6,
                toLocator6,
                toCallsignRaw,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                false,
                false,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1,
                "No usable automatic band is available. "
                        + "Check own enabled bands and the station's NOT-QRV tags.",
                List.of()
        );
    }

    /**
     * Creates a finished result without a usable terrain profile.
     *
     * @param analysisMode source/mode text
     * @param fromLocator6 own locator
     * @param toLocator6 target locator
     * @param toCallsignRaw target callsign
     * @param distanceKm path distance in kilometers
     * @param bearingDeg initial bearing in degrees
     * @param homeAntennaHeightMeters own antenna height in meters AGL
     * @param targetAntennaHeightMeters target antenna height in meters AGL
     * @param analysisFrequencyMHz analysis frequency in MHz
     * @param statusText human-readable status text
     * @return finished result without profile points
     */
    public static PathAnalysisResult noProfile(String analysisMode,
                                               String fromLocator6,
                                               String toLocator6,
                                               String toCallsignRaw,
                                               double distanceKm,
                                               double bearingDeg,
                                               double homeAntennaHeightMeters,
                                               double targetAntennaHeightMeters,
                                               double analysisFrequencyMHz,
                                               String statusText) {
        return new PathAnalysisResult(
                analysisMode,
                fromLocator6,
                toLocator6,
                toCallsignRaw,
                distanceKm,
                bearingDeg,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                analysisFrequencyMHz,
                false,
                false,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                -1,
                statusText,
                List.of()
        );
    }

    /**
     * Creates a fully evaluated result.
     *
     * @param analysisMode source/mode text
     * @param fromLocator6 own locator
     * @param toLocator6 target locator
     * @param toCallsignRaw target callsign
     * @param distanceKm path distance in kilometers
     * @param bearingDeg initial bearing in degrees
     * @param homeAntennaHeightMeters own antenna height in meters AGL
     * @param targetAntennaHeightMeters target antenna height in meters AGL
     * @param analysisFrequencyMHz analysis frequency in MHz
     * @param lineOfSightClear true if the direct path is clear
     * @param fresnelClear true if the lower Fresnel hull is not intruded
     * @param minimumLineOfSightClearanceMeters minimum LOS clearance in meters
     * @param minimumLowerFresnelClearanceMeters minimum lower Fresnel clearance in meters
     * @param worstFresnelIntrusionMeters maximum lower Fresnel intrusion in meters
     * @param worstFresnelIntrusionRatio intrusion divided by local Fresnel radius
     * @param worstFresnelDistanceKm distance of the worst intrusion from TX in kilometers
     * @param worstFresnelSampleIndex sample index of the worst intrusion
     * @param statusText human-readable status text
     * @param profilePoints enriched profile points
     * @return completed path analysis result
     */
    public static PathAnalysisResult completed(String analysisMode,
                                               String fromLocator6,
                                               String toLocator6,
                                               String toCallsignRaw,
                                               double distanceKm,
                                               double bearingDeg,
                                               double homeAntennaHeightMeters,
                                               double targetAntennaHeightMeters,
                                               double analysisFrequencyMHz,
                                               boolean lineOfSightClear,
                                               boolean fresnelClear,
                                               double minimumLineOfSightClearanceMeters,
                                               double minimumLowerFresnelClearanceMeters,
                                               double worstFresnelIntrusionMeters,
                                               double worstFresnelIntrusionRatio,
                                               double worstFresnelDistanceKm,
                                               int worstFresnelSampleIndex,
                                               String statusText,
                                               List<PathProfilePoint> profilePoints) {
        return new PathAnalysisResult(
                analysisMode,
                fromLocator6,
                toLocator6,
                toCallsignRaw,
                distanceKm,
                bearingDeg,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                analysisFrequencyMHz,
                lineOfSightClear,
                fresnelClear,
                minimumLineOfSightClearanceMeters,
                minimumLowerFresnelClearanceMeters,
                worstFresnelIntrusionMeters,
                worstFresnelIntrusionRatio,
                worstFresnelDistanceKm,
                worstFresnelSampleIndex,
                statusText,
                profilePoints
        );
    }

    /**
     * Creates a fully evaluated result including horizon/refraction metadata.
     *
     * @return completed path analysis result
     */
    public static PathAnalysisResult completed(String analysisMode,
                                               String fromLocator6,
                                               String toLocator6,
                                               String toCallsignRaw,
                                               double distanceKm,
                                               double bearingDeg,
                                               double homeAntennaHeightMeters,
                                               double targetAntennaHeightMeters,
                                               double analysisFrequencyMHz,
                                               boolean lineOfSightClear,
                                               boolean fresnelClear,
                                               double minimumLineOfSightClearanceMeters,
                                               double minimumLowerFresnelClearanceMeters,
                                               double worstFresnelIntrusionMeters,
                                               double worstFresnelIntrusionRatio,
                                               double worstFresnelDistanceKm,
                                               int worstFresnelSampleIndex,
                                               double effectiveEarthRadiusFactor,
                                               PathHorizonSummary horizonSummary,
                                               PathObstructionSummary obstructionSummary,
                                               PathLinkBudgetSummary linkBudgetSummary,
                                               PathPropagationAssessment propagationAssessment,
                                               String statusText,
                                               List<PathProfilePoint> profilePoints) {
        return new PathAnalysisResult(
                analysisMode,
                fromLocator6,
                toLocator6,
                toCallsignRaw,
                distanceKm,
                bearingDeg,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                analysisFrequencyMHz,
                lineOfSightClear,
                fresnelClear,
                minimumLineOfSightClearanceMeters,
                minimumLowerFresnelClearanceMeters,
                worstFresnelIntrusionMeters,
                worstFresnelIntrusionRatio,
                worstFresnelDistanceKm,
                worstFresnelSampleIndex,
                effectiveEarthRadiusFactor,
                horizonSummary,
                obstructionSummary,
                propagationAssessment,
                statusText,
                profilePoints,
                linkBudgetSummary = linkBudgetSummary == null
                        ? PathLinkBudgetSummary.empty()
                        : linkBudgetSummary
        );
    }

    /**
     * Returns true if the result contains a profile usable for charting/evaluation.
     *
     * @return true if at least two profile points are available
     */
    public boolean hasUsableProfile() {
        return profilePoints.size() >= 2;
    }

    /**
     * Returns the dominant obstruction / diffraction candidate text.
     *
     * @return obstruction summary text
     */
    public String obstructionText() {
        return obstructionSummary == null
                ? "-"
                : obstructionSummary.obstructionText();
    }

    /**
     * Returns a short UI text for direct LOS state.
     *
     * @return LOS summary text
     */
    public String losText() {
        if (!hasUsableProfile()) {
            return "-";
        }

        return lineOfSightClear
                ? String.format(Locale.US, "Geometric LOS clear (min %+,.1f m)", minimumLineOfSightClearanceMeters)
                : String.format(Locale.US, "Geometric LOS blocked (min %+,.1f m)", minimumLineOfSightClearanceMeters);
    }

    /**
     * Returns a short UI text for the minimum LOS clearance.
     *
     * @return worst direct-path clearance text
     */
    public String worstClearanceText() {
        if (!hasUsableProfile() || !Double.isFinite(minimumLineOfSightClearanceMeters)) {
            return "-";
        }
        return String.format(Locale.US, "%.1f m", minimumLineOfSightClearanceMeters);
    }

    /**
     * Returns a short UI text for the analysis frequency.
     *
     * @return frequency text or "-"
     */
    public String analysisFrequencyText() {
        if (!Double.isFinite(analysisFrequencyMHz) || analysisFrequencyMHz <= 0.0) {
            return "-";
        }
        return String.format(Locale.US, "%.3f MHz", analysisFrequencyMHz);
    }

    /**
     * Returns a short UI text for the Fresnel state.
     *
     * @return Fresnel summary text
     */
    public String fresnelText() {
        if (!hasUsableProfile()) {
            return "-";
        }

        if (fresnelClear) {
            return String.format(
                    Locale.US,
                    "1st Fresnel clear (min %+,.1f m)",
                    minimumLowerFresnelClearanceMeters
            );
        }

        return String.format(
                Locale.US,
                "1st Fresnel intruded (worst %.1f m)",
                worstFresnelIntrusionMeters
        );
    }

    /**
     * Returns a short UI text for the worst Fresnel intrusion.
     *
     * @return worst Fresnel intrusion text
     */
    public String worstFresnelClearanceText() {
        if (!hasUsableProfile()) {
            return "-";
        }

        if (Double.isFinite(worstFresnelIntrusionMeters) && worstFresnelIntrusionMeters > 0.0) {
            return String.format(
                    Locale.US,
                    "%.1f m @ %.1f km (%.0f%%)",
                    worstFresnelIntrusionMeters,
                    worstFresnelDistanceKm,
                    worstFresnelIntrusionRatio * 100.0
            );
        }

        if (Double.isFinite(minimumLowerFresnelClearanceMeters)) {
            return String.format(
                    Locale.US,
                    "No intrusion (min %.1f m)",
                    minimumLowerFresnelClearanceMeters
            );
        }

        return "-";
    }


    /**
     * Returns the effective Earth radius / refraction model text used by this
     * analysis result.
     *
     * @return k-factor text
     */
    public String effectiveEarthRadiusText() {
        return horizonSummary == null
                ? "-"
                : horizonSummary.effectiveEarthRadiusText();
    }

    /**
     * Returns a concise simple radio-horizon summary for both endpoint antenna
     * heights.
     *
     * @return radio-horizon summary
     */
    public String radioHorizonText() {
        return horizonSummary == null
                ? "-"
                : horizonSummary.simpleRadioHorizonText();
    }

    /**
     * Returns the actual terrain horizon derived from the current terrain profile.
     *
     * @return terrain-horizon summary
     */
    public String terrainHorizonText() {
        return horizonSummary == null
                ? "-"
                : horizonSummary.terrainHorizonText();
    }

    /**
     * Returns the distance as formatted text.
     *
     * @return distance text or "-"
     */
    public String distanceText() {
        if (!Double.isFinite(distanceKm) || distanceKm < 0.0) {
            return "-";
        }
        return String.format(Locale.US, "%.1f km", distanceKm);
    }

    /**
     * Returns the initial path bearing as formatted text.
     *
     * @return bearing text or "-"
     */
    public String bearingText() {
        if (!Double.isFinite(bearingDeg)) {
            return "-";
        }
        return String.format(Locale.US, "%.0f°", bearingDeg);
    }

    /**
     * Returns a concise endpoint summary with ground level and configured antenna heights.
     *
     * @return endpoint/antenna summary text
     */
    public String endpointSummaryText() {
        PathProfilePoint firstPoint = firstProfilePoint();
        PathProfilePoint lastPoint = lastProfilePoint();

        boolean hasHomeGround = firstPoint != null && Double.isFinite(firstPoint.elevationMeters());
        boolean hasTargetGround = lastPoint != null && Double.isFinite(lastPoint.elevationMeters());

        boolean hasHomeAntenna = Double.isFinite(homeAntennaHeightMeters);
        boolean hasTargetAntenna = Double.isFinite(targetAntennaHeightMeters);

        String homeText;
        if (hasHomeGround && hasHomeAntenna) {
            homeText = String.format(
                    Locale.US,
                    "Home %.0f m ASL + %.0f m AGL",
                    firstPoint.elevationMeters(),
                    homeAntennaHeightMeters
            );
        } else if (hasHomeAntenna) {
            homeText = String.format(Locale.US, "Home +%.0f m AGL", homeAntennaHeightMeters);
        } else {
            homeText = "Home -";
        }

        String targetText;
        if (hasTargetGround && hasTargetAntenna) {
            targetText = String.format(
                    Locale.US,
                    "DX %.0f m ASL + %.0f m AGL",
                    lastPoint.elevationMeters(),
                    targetAntennaHeightMeters
            );
        } else if (hasTargetAntenna) {
            targetText = String.format(Locale.US, "DX +%.0f m AGL", targetAntennaHeightMeters);
        } else {
            targetText = "DX -";
        }

        return homeText + " | " + targetText;
    }


    /**
     * Returns the operator-facing propagation assessment.
     *
     * @return short propagation assessment text
     */
    public String propagationAssessmentText() {
        return propagationAssessment == null
                ? "-"
                : propagationAssessment.shortText();
    }

    /**
     * Returns the likely propagation mechanisms suggested by the current assessment.
     *
     * @return mechanism summary
     */
    public String propagationMechanismsText() {
        return propagationAssessment == null
                ? "-"
                : propagationAssessment.likelyMechanisms();
    }

    /**
     * Returns a numeric severity level from 0 to 5.
     *
     * @return severity level
     */
    public int propagationSeverityLevel() {
        return propagationAssessment == null
                ? 0
                : propagationAssessment.severityLevel();
    }

    private PathProfilePoint firstProfilePoint() {
        return profilePoints.isEmpty() ? null : profilePoints.get(0);
    }

    private PathProfilePoint lastProfilePoint() {
        return profilePoints.isEmpty() ? null : profilePoints.get(profilePoints.size() - 1);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public String linkBudgetText() {
        return linkBudgetSummary == null
                ? "-"
                : linkBudgetSummary.ssbMarginText();
    }

    public String linkBudgetRxPowerText() {
        return linkBudgetSummary == null
                ? "-"
                : linkBudgetSummary.rxPowerText();
    }

    public String linkBudgetDetailText() {
        return linkBudgetSummary == null
                ? "-"
                : linkBudgetSummary.linkBudgetDetailText();
    }

    public String cwHintText() {
        return linkBudgetSummary == null
                ? "-"
                : linkBudgetSummary.cwHintText();
    }
}