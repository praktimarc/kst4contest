package kst4contest.view.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Path analysis service that combines:
 * <ul>
 *     <li>terrain/profile loading from the configured provider chain</li>
 *     <li>adaptive profile sampling based on path length</li>
 *     <li>Earth-curvature-adjusted line-of-sight evaluation</li>
 *     <li>a symmetric first Fresnel hull around the direct path</li>
 *     <li>worst-intrusion extraction from exactly the same profile points that are rendered later</li>
 * </ul>
 *
 * <p>The main design goal is numeric/visual consistency:
 * the chart should never recalculate different geometry than the service.</p>
 */
public final class GeometryOnlyPathAnalysisService implements PathAnalysisService {

    private final TerrainProfileProvider terrainProfileProvider;

    public GeometryOnlyPathAnalysisService(TerrainProfileProvider terrainProfileProvider) {
        this.terrainProfileProvider = Objects.requireNonNull(terrainProfileProvider, "terrainProfileProvider");
    }

    @Override
    public PathAnalysisResult analyze(PathAnalysisRequest request) {
        if (request == null) {
            return PathAnalysisResult.waitingForSelection("");
        }

        String fromLocator6 = normalizeLocator6(request.fromLocator6());
        String toLocator6 = normalizeLocator6(request.toLocator6());
        String toCallsignRaw = normalizeCallsignRaw(request.toCallsignRaw());

        if (!request.hasUsableHome()) {
            return PathAnalysisResult.waitingForValidHomeLocator(fromLocator6, toLocator6);
        }

        if (!request.hasUsableTarget()) {
            return PathAnalysisResult.waitingForValidTarget(fromLocator6, toLocator6);
        }

        double distanceKm = PathGeometryUtils.calculateGreatCircleDistanceKm(
                request.fromLatitudeDeg(),
                request.fromLongitudeDeg(),
                request.toLatitudeDeg(),
                request.toLongitudeDeg()
        );

        double bearingDeg = PathGeometryUtils.calculateInitialBearingDeg(
                request.fromLatitudeDeg(),
                request.fromLongitudeDeg(),
                request.toLatitudeDeg(),
                request.toLongitudeDeg()
        );

        double analysisFrequencyMHz = request.hasUsableFrequency()
                ? request.frequencyMHz()
                : PathGeometryUtils.DEFAULT_ANALYSIS_FREQUENCY_MHZ;

        double homeAntennaHeightMeters = sanitizeAntennaHeightMeters(request.homeAntennaHeightMeters());
        double targetAntennaHeightMeters = sanitizeAntennaHeightMeters(request.targetAntennaHeightMeters());

        double effectiveEarthRadiusFactor =
                PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(request.effectiveEarthRadiusFactor());

        int requestedSampleCount = PathGeometryUtils.resolveAdaptiveSampleCount(distanceKm);

        TerrainProfileRequest terrainRequest = new TerrainProfileRequest(
                request.fromLatitudeDeg(),
                request.fromLongitudeDeg(),
                request.toLatitudeDeg(),
                request.toLongitudeDeg(),
                distanceKm,
                requestedSampleCount
        );

        final TerrainProfileData terrainProfileData;
        try {
            terrainProfileData = terrainProfileProvider.loadProfile(terrainRequest);
        } catch (Exception exception) {
            return PathAnalysisResult.noProfile(
                    "Terrain error",
                    fromLocator6,
                    toLocator6,
                    toCallsignRaw,
                    distanceKm,
                    bearingDeg,
                    homeAntennaHeightMeters,
                    targetAntennaHeightMeters,
                    analysisFrequencyMHz,
                    "Terrain provider failed: " + exception.getMessage()
            );
        }

        if (terrainProfileData == null || !terrainProfileData.hasUsableProfile()) {
            String sourceName = terrainProfileData == null ? "Unknown terrain source" : terrainProfileData.sourceName();

            return PathAnalysisResult.noProfile(
                    sourceName.isBlank() ? "No profile" : sourceName,
                    fromLocator6,
                    toLocator6,
                    toCallsignRaw,
                    distanceKm,
                    bearingDeg,
                    homeAntennaHeightMeters,
                    targetAntennaHeightMeters,
                    analysisFrequencyMHz,
                    buildNoProfileStatusText(sourceName, requestedSampleCount)
            );
        }

        List<PathProfilePoint> enrichedProfilePoints = buildEnrichedProfilePoints(
                terrainProfileData.profilePoints(),
                distanceKm,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                analysisFrequencyMHz,
                effectiveEarthRadiusFactor
        );

        PathHorizonSummary horizonSummary = buildHorizonSummary(
                enrichedProfilePoints,
                distanceKm,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                effectiveEarthRadiusFactor
        );


        ProfileSummary summary = summarizeProfile(enrichedProfilePoints);


        PathObstructionSummary obstructionSummary = buildObstructionSummary(
                enrichedProfilePoints,
                distanceKm,
                analysisFrequencyMHz,
                summary
        );

        PathLinkBudgetSummary linkBudgetSummary = buildLinkBudgetSummary(
                distanceKm,
                analysisFrequencyMHz,
                obstructionSummary,
                request.linkBudgetSettings()
        );

        PathPropagationAssessment propagationAssessment = buildPropagationAssessment(
                summary,
                obstructionSummary,
                linkBudgetSummary
        );

        String analysisMode = terrainProfileData.sourceName().isBlank()
                ? (terrainProfileData.synthetic() ? "Synthetic fallback" : "Terrain profile")
                : terrainProfileData.sourceName();

        String statusText = buildCompletedStatusText(
                terrainProfileData,
                summary,
                horizonSummary,
                obstructionSummary,
                linkBudgetSummary,
                propagationAssessment,
                enrichedProfilePoints.size(),
                requestedSampleCount,
                distanceKm,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                analysisFrequencyMHz
        );


        return PathAnalysisResult.completed(
                analysisMode,
                fromLocator6,
                toLocator6,
                toCallsignRaw,
                distanceKm,
                bearingDeg,
                homeAntennaHeightMeters,
                targetAntennaHeightMeters,
                analysisFrequencyMHz,
                summary.lineOfSightClear,
                summary.fresnelClear,
                summary.minimumLineOfSightClearanceMeters,
                summary.minimumLowerFresnelClearanceMeters,
                summary.worstFresnelIntrusionMeters,
                summary.worstFresnelIntrusionRatio,
                summary.worstFresnelDistanceKm,
                summary.worstFresnelSampleIndex,
                effectiveEarthRadiusFactor,
                horizonSummary,
                obstructionSummary,
                linkBudgetSummary,
                propagationAssessment,
                statusText,
                enrichedProfilePoints
        );
    }

    /**
     * Converts raw terrain profile samples into enriched samples that already contain:
     * <ul>
     *     <li>curvature-adjusted terrain</li>
     *     <li>direct LOS height</li>
     *     <li>upper/lower first Fresnel hull</li>
     *     <li>LOS and Fresnel clearances</li>
     * </ul>
     *
     * <p>Those values are later used unchanged by both the detail texts and the chart.</p>
     *
     * @param rawProfilePoints raw terrain profile points
     * @param totalDistanceKm total path length in kilometers
     * @param homeAntennaHeightMeters own antenna height in meters AGL
     * @param targetAntennaHeightMeters target antenna height in meters AGL
     * @param analysisFrequencyMHz analysis frequency in MHz
     * @return enriched immutable profile point list
     */
    private List<PathProfilePoint> buildEnrichedProfilePoints(List<PathProfilePoint> rawProfilePoints,
                                                              double totalDistanceKm,
                                                              double homeAntennaHeightMeters,
                                                              double targetAntennaHeightMeters,
                                                              double analysisFrequencyMHz,
                                                              double effectiveEarthRadiusFactor) {

        List<PathProfilePoint> enrichedPoints = new ArrayList<>(rawProfilePoints.size());

        PathProfilePoint firstRawPoint = rawProfilePoints.get(0);
        PathProfilePoint lastRawPoint = rawProfilePoints.get(rawProfilePoints.size() - 1);

        double startTerrainMeters = PathGeometryUtils.calculateCurvatureAdjustedElevationMeters(
                firstRawPoint,
                totalDistanceKm,
                effectiveEarthRadiusFactor
        );

        double endTerrainMeters = PathGeometryUtils.calculateCurvatureAdjustedElevationMeters(
                lastRawPoint,
                totalDistanceKm,
                effectiveEarthRadiusFactor
        );

        double startAntennaMeters = startTerrainMeters + homeAntennaHeightMeters;
        double endAntennaMeters = endTerrainMeters + targetAntennaHeightMeters;


        for (int sampleIndex = 0; sampleIndex < rawProfilePoints.size(); sampleIndex++) {
            PathProfilePoint rawPoint = rawProfilePoints.get(sampleIndex);

            double curvatureAdjustedElevationMeters =
                    PathGeometryUtils.calculateCurvatureAdjustedElevationMeters(
                            rawPoint,
                            totalDistanceKm,
                            effectiveEarthRadiusFactor
                    );

            double normalizedDistance = totalDistanceKm <= 0.0
                    ? 0.0
                    : Math.max(0.0, Math.min(1.0, rawPoint.distanceKm() / totalDistanceKm));

            double lineOfSightHeightMeters =
                    startAntennaMeters + (endAntennaMeters - startAntennaMeters) * normalizedDistance;

            double fresnelRadiusMeters = PathGeometryUtils.calculateFirstFresnelRadiusMeters(
                    rawPoint.distanceKm(),
                    totalDistanceKm,
                    analysisFrequencyMHz
            );

            if (!Double.isFinite(fresnelRadiusMeters)) {
                fresnelRadiusMeters = Double.NaN;
            }

            double fresnelUpperHeightMeters = Double.isFinite(fresnelRadiusMeters)
                    ? lineOfSightHeightMeters + fresnelRadiusMeters
                    : Double.NaN;

            double fresnelLowerHeightMeters = Double.isFinite(fresnelRadiusMeters)
                    ? lineOfSightHeightMeters - fresnelRadiusMeters
                    : Double.NaN;

            double lineOfSightClearanceMeters =
                    lineOfSightHeightMeters - curvatureAdjustedElevationMeters;

            double lowerFresnelClearanceMeters = Double.isFinite(fresnelLowerHeightMeters)
                    ? fresnelLowerHeightMeters - curvatureAdjustedElevationMeters
                    : Double.NaN;

            double fresnelIntrusionMeters = Double.isFinite(lowerFresnelClearanceMeters)
                    ? Math.max(0.0, curvatureAdjustedElevationMeters - fresnelLowerHeightMeters)
                    : 0.0;

            enrichedPoints.add(new PathProfilePoint(
                    sampleIndex,
                    rawPoint.distanceKm(),
                    rawPoint.latitudeDeg(),
                    rawPoint.longitudeDeg(),
                    rawPoint.elevationMeters(),
                    curvatureAdjustedElevationMeters,
                    lineOfSightHeightMeters,
                    fresnelUpperHeightMeters,
                    fresnelLowerHeightMeters,
                    lineOfSightClearanceMeters,
                    lowerFresnelClearanceMeters,
                    fresnelIntrusionMeters
            ));
        }

        return List.copyOf(enrichedPoints);
    }

    /**
     * Aggregates the LOS/Fresnel summary from the already enriched profile points.
     *
     * @param enrichedProfilePoints enriched profile point list
     * @return immutable summary
     */
    private ProfileSummary summarizeProfile(List<PathProfilePoint> enrichedProfilePoints) {
        boolean lineOfSightClear = true;
        boolean fresnelClear = true;

        double minimumLineOfSightClearanceMeters = Double.POSITIVE_INFINITY;
        double minimumLowerFresnelClearanceMeters = Double.POSITIVE_INFINITY;
        double worstFresnelIntrusionMeters = 0.0;
        double worstFresnelIntrusionRatio = 0.0;
        double worstFresnelDistanceKm = Double.NaN;
        int worstFresnelSampleIndex = -1;

        for (PathProfilePoint point : enrichedProfilePoints) {
            if (Double.isFinite(point.lineOfSightClearanceMeters())) {
                minimumLineOfSightClearanceMeters = Math.min(
                        minimumLineOfSightClearanceMeters,
                        point.lineOfSightClearanceMeters()
                );

                if (point.lineOfSightClearanceMeters() < 0.0) {
                    lineOfSightClear = false;
                }
            }

            if (Double.isFinite(point.lowerFresnelClearanceMeters())) {
                minimumLowerFresnelClearanceMeters = Math.min(
                        minimumLowerFresnelClearanceMeters,
                        point.lowerFresnelClearanceMeters()
                );
            }

            if (point.hasFresnelIntrusion()) {
                fresnelClear = false;
            }

            if (Double.isFinite(point.fresnelIntrusionMeters())
                    && point.fresnelIntrusionMeters() > worstFresnelIntrusionMeters) {

                worstFresnelIntrusionMeters = point.fresnelIntrusionMeters();
                worstFresnelDistanceKm = point.distanceKm();
                worstFresnelSampleIndex = point.sampleIndex();

                double localFresnelRadiusMeters =
                        point.lineOfSightHeightMeters() - point.fresnelLowerHeightMeters();

                if (Double.isFinite(localFresnelRadiusMeters) && localFresnelRadiusMeters > 0.0) {
                    worstFresnelIntrusionRatio =
                            point.fresnelIntrusionMeters() / localFresnelRadiusMeters;
                } else {
                    worstFresnelIntrusionRatio = 0.0;
                }
            }
        }

        if (Double.isInfinite(minimumLineOfSightClearanceMeters)) {
            minimumLineOfSightClearanceMeters = Double.NaN;
        }

        if (Double.isInfinite(minimumLowerFresnelClearanceMeters)) {
            minimumLowerFresnelClearanceMeters = Double.NaN;
        }

        return new ProfileSummary(
                lineOfSightClear,
                fresnelClear,
                minimumLineOfSightClearanceMeters,
                minimumLowerFresnelClearanceMeters,
                worstFresnelIntrusionMeters,
                worstFresnelIntrusionRatio,
                worstFresnelDistanceKm,
                worstFresnelSampleIndex
        );
    }


    /**
     * Builds horizon information from the already enriched profile points.
     *
     * <p>The simple radio horizon uses only antenna height and k-factor. The terrain
     * horizon is derived from the actual profile by finding the terrain point with
     * the highest apparent elevation angle from each endpoint.</p>
     *
     * @param enrichedProfilePoints enriched path profile
     * @param totalDistanceKm total path distance
     * @param homeAntennaHeightMeters own antenna height AGL
     * @param targetAntennaHeightMeters target antenna height AGL
     * @param effectiveEarthRadiusFactor k-factor
     * @return horizon summary
     */
    private PathHorizonSummary buildHorizonSummary(List<PathProfilePoint> enrichedProfilePoints,
                                                   double totalDistanceKm,
                                                   double homeAntennaHeightMeters,
                                                   double targetAntennaHeightMeters,
                                                   double effectiveEarthRadiusFactor) {

        double homeSimpleRadioHorizonKm = PathGeometryUtils.calculateRadioHorizonDistanceKm(
                homeAntennaHeightMeters,
                effectiveEarthRadiusFactor
        );

        double targetSimpleRadioHorizonKm = PathGeometryUtils.calculateRadioHorizonDistanceKm(
                targetAntennaHeightMeters,
                effectiveEarthRadiusFactor
        );

        double combinedSimpleRadioHorizonKm =
                Double.isFinite(homeSimpleRadioHorizonKm) && Double.isFinite(targetSimpleRadioHorizonKm)
                        ? homeSimpleRadioHorizonKm + targetSimpleRadioHorizonKm
                        : Double.NaN;

        TerrainHorizonCandidate homeTerrainHorizon = findHomeTerrainHorizon(
                enrichedProfilePoints,
                homeAntennaHeightMeters
        );

        TerrainHorizonCandidate targetTerrainHorizon = findTargetTerrainHorizon(
                enrichedProfilePoints,
                totalDistanceKm,
                targetAntennaHeightMeters
        );

        return new PathHorizonSummary(
                effectiveEarthRadiusFactor,

                homeSimpleRadioHorizonKm,
                targetSimpleRadioHorizonKm,
                combinedSimpleRadioHorizonKm,

                homeTerrainHorizon.pathDistanceKm(),
                homeTerrainHorizon.elevationAngleDeg(),
                homeTerrainHorizon.sampleIndex(),

                targetTerrainHorizon.pathDistanceKm(),
                targetTerrainHorizon.distanceFromEndpointKm(),
                targetTerrainHorizon.elevationAngleDeg(),
                targetTerrainHorizon.sampleIndex()
        );
    }

    private TerrainHorizonCandidate findHomeTerrainHorizon(List<PathProfilePoint> enrichedProfilePoints,
                                                           double homeAntennaHeightMeters) {

        if (enrichedProfilePoints == null || enrichedProfilePoints.size() < 3) {
            return TerrainHorizonCandidate.empty();
        }

        PathProfilePoint firstPoint = enrichedProfilePoints.get(0);
        double observerHeightMeters = firstPoint.curvatureAdjustedElevationMeters() + homeAntennaHeightMeters;

        TerrainHorizonCandidate bestCandidate = TerrainHorizonCandidate.empty();
        double bestAngleDeg = Double.NEGATIVE_INFINITY;

        for (int i = 1; i < enrichedProfilePoints.size() - 1; i++) {
            PathProfilePoint point = enrichedProfilePoints.get(i);

            if (!Double.isFinite(point.curvatureAdjustedElevationMeters())
                    || !Double.isFinite(point.distanceKm())
                    || point.distanceKm() <= 0.0) {
                continue;
            }

            double angleDeg = PathGeometryUtils.calculateElevationAngleDeg(
                    observerHeightMeters,
                    point.curvatureAdjustedElevationMeters(),
                    point.distanceKm()
            );

            if (Double.isFinite(angleDeg) && angleDeg > bestAngleDeg) {
                bestAngleDeg = angleDeg;
                bestCandidate = new TerrainHorizonCandidate(
                        point.sampleIndex(),
                        point.distanceKm(),
                        point.distanceKm(),
                        angleDeg
                );
            }
        }

        return bestCandidate;
    }

    private TerrainHorizonCandidate findTargetTerrainHorizon(List<PathProfilePoint> enrichedProfilePoints,
                                                             double totalDistanceKm,
                                                             double targetAntennaHeightMeters) {

        if (enrichedProfilePoints == null
                || enrichedProfilePoints.size() < 3
                || !Double.isFinite(totalDistanceKm)
                || totalDistanceKm <= 0.0) {
            return TerrainHorizonCandidate.empty();
        }

        PathProfilePoint lastPoint = enrichedProfilePoints.get(enrichedProfilePoints.size() - 1);
        double observerHeightMeters = lastPoint.curvatureAdjustedElevationMeters() + targetAntennaHeightMeters;

        TerrainHorizonCandidate bestCandidate = TerrainHorizonCandidate.empty();
        double bestAngleDeg = Double.NEGATIVE_INFINITY;

        for (int i = enrichedProfilePoints.size() - 2; i > 0; i--) {
            PathProfilePoint point = enrichedProfilePoints.get(i);

            double distanceFromTargetKm = totalDistanceKm - point.distanceKm();

            if (!Double.isFinite(point.curvatureAdjustedElevationMeters())
                    || !Double.isFinite(distanceFromTargetKm)
                    || distanceFromTargetKm <= 0.0) {
                continue;
            }

            double angleDeg = PathGeometryUtils.calculateElevationAngleDeg(
                    observerHeightMeters,
                    point.curvatureAdjustedElevationMeters(),
                    distanceFromTargetKm
            );

            if (Double.isFinite(angleDeg) && angleDeg > bestAngleDeg) {
                bestAngleDeg = angleDeg;
                bestCandidate = new TerrainHorizonCandidate(
                        point.sampleIndex(),
                        point.distanceKm(),
                        distanceFromTargetKm,
                        angleDeg
                );
            }
        }

        return bestCandidate;
    }

    private record TerrainHorizonCandidate(
            int sampleIndex,
            double pathDistanceKm,
            double distanceFromEndpointKm,
            double elevationAngleDeg
    ) {
        private static TerrainHorizonCandidate empty() {
            return new TerrainHorizonCandidate(-1, Double.NaN, Double.NaN, Double.NaN);
        }
    }

    /**
     * Builds a status text that clearly separates geometric/topographic evaluation
     * from radio-technical interpretation.
     *
     * @param terrainProfileData original terrain profile metadata
     * @param summary aggregated LOS/Fresnel summary
     * @param horizonSummary radio/terrain horizon summary
     * @param effectiveSampleCount number of samples actually used
     * @param requestedSampleCount requested sample count for this path
     * @param totalDistanceKm total path distance
     * @param homeAntennaHeightMeters own antenna height AGL
     * @param targetAntennaHeightMeters target antenna height AGL
     * @param analysisFrequencyMHz analysis frequency
     * @return human-readable multi-line status text
     */
    private String buildCompletedStatusText(TerrainProfileData terrainProfileData,
                                            ProfileSummary summary,
                                            PathHorizonSummary horizonSummary,
                                            PathObstructionSummary obstructionSummary,
                                            PathLinkBudgetSummary linkBudgetSummary,
                                            PathPropagationAssessment propagationAssessment,
                                            int effectiveSampleCount,
                                            int requestedSampleCount,
                                            double totalDistanceKm,
                                            double homeAntennaHeightMeters,
                                            double targetAntennaHeightMeters,
                                            double analysisFrequencyMHz) {

        String sourceName = terrainProfileData.sourceName().isBlank()
                ? "terrain source"
                : terrainProfileData.sourceName();

        StringBuilder status = new StringBuilder();

        status.append("Geometric / topographic evaluation\n");
        status.append("• Loaded ")
                .append(effectiveSampleCount)
                .append(" / ")
                .append(requestedSampleCount)
                .append(" samples from ")
                .append(sourceName)
                .append(".\n");

        status.append("• Earth/refraction model: ")
                .append(horizonSummary.effectiveEarthRadiusText())
                .append(".\n");

        status.append("• Direct line of sight: ")
                .append(summary.lineOfSightClear ? "clear" : "blocked")
                .append(" (minimum clearance ")
                .append(formatSignedMeters(summary.minimumLineOfSightClearanceMeters))
                .append(").\n");

        if (summary.fresnelClear) {
            status.append("• 1st Fresnel zone: clear")
                    .append(" (minimum lower clearance ")
                    .append(formatSignedMeters(summary.minimumLowerFresnelClearanceMeters))
                    .append(").\n");
        } else {
            status.append("• 1st Fresnel zone: intruded")
                    .append(" (worst intrusion ")
                    .append(formatUnsignedMeters(summary.worstFresnelIntrusionMeters))
                    .append(" at ")
                    .append(formatDistanceKm(summary.worstFresnelDistanceKm))
                    .append(", about ")
                    .append(String.format(Locale.US, "%.0f%%", summary.worstFresnelIntrusionRatio * 100.0))
                    .append(" of the local Fresnel radius).\n");
        }

        status.append("• Endpoint antenna heights: Home ")
                .append(String.format(Locale.US, "%.0f m AGL", homeAntennaHeightMeters))
                .append(", DX ")
                .append(String.format(Locale.US, "%.0f m AGL", targetAntennaHeightMeters))
                .append(".\n");

        status.append("• Simple radio horizon: ")
                .append(horizonSummary.simpleRadioHorizonText())
                .append(".\n");

        status.append("• Terrain horizon from profile: ")
                .append(horizonSummary.terrainHorizonText())
                .append(".\n");

        status.append("• Dominant obstruction / diffraction candidate: ")
                .append(obstructionSummary.obstructionText())
                .append(".\n");

        status.append("• Link budget: ")
                .append(linkBudgetSummary.ssbMarginText())
                .append(".\n");

        status.append("• Estimated RX power: ")
                .append(linkBudgetSummary.rxPowerText())
                .append(".\n");

        status.append("• Link-budget details: ")
                .append(linkBudgetSummary.linkBudgetDetailText())
                .append(".\n");

        status.append("• CW hint: ")
                .append(linkBudgetSummary.cwHintText())
                .append(".\n");

        status.append("• Propagation assessment: ")
                .append(propagationAssessment.shortText())
                .append(".\n");


        if (Double.isFinite(totalDistanceKm)
                && Double.isFinite(horizonSummary.combinedSimpleRadioHorizonKm())) {
            status.append("• Distance vs. simple radio horizon: ")
                    .append(formatDistanceKm(totalDistanceKm))
                    .append(" path distance vs. ")
                    .append(formatDistanceKm(horizonSummary.combinedSimpleRadioHorizonKm()))
                    .append(" combined simple horizon.\n");
        }

        if (Double.isFinite(analysisFrequencyMHz) && analysisFrequencyMHz > 0.0) {
            status.append("• Analysis frequency: ")
                    .append(String.format(Locale.US, "%.3f MHz", analysisFrequencyMHz))
                    .append(".\n");
        }

        status.append("\n");
        status.append("Radio-technical interpretation\n");

        status.append("• Assessment: ")
                .append(propagationAssessment.category())
                .append(" — ")
                .append(propagationAssessment.detailText())
                .append("\n");

        status.append("• Likely mechanisms: ")
                .append(propagationAssessment.likelyMechanisms())
                .append(".\n");

        if (summary.lineOfSightClear && summary.fresnelClear) {
            status.append("• The path is geometrically favorable. A direct tropospheric path is plausible.\n");
        } else if (summary.lineOfSightClear) {
            status.append("• The direct path is open, but the Fresnel zone is partly obstructed. Expect additional loss, not an automatic failure.\n");
        } else {
            status.append("• The path is geometrically blocked. This does not automatically mean that a VHF/UHF QSO is impossible.\n");

            if (obstructionSummary.hasDominantLosObstruction()) {
                status.append("• The dominant obstruction gives a rough single-knife-edge estimate of about ")
                        .append(String.format(Locale.US, "%.1f dB", obstructionSummary.estimatedKnifeEdgeLossDb()))
                        .append(" additional loss. Treat this as a severity hint, not as a final path prediction.\n");
            }

            status.append("• Possible mechanisms include diffraction over terrain, troposcatter, enhanced tropospheric refraction / tropo, and aircraft scatter.\n");
        }

        status.append("• Important: this is still not a full propagation model. It does not yet calculate diffraction loss, troposcatter probability, aircraft-scatter windows or ducting strength.\n");

        if (terrainProfileData.synthetic()) {
            status.append("• Synthetic fallback is active, so the result is only a rough approximation.\n");
        }

        return status.toString().trim();
    }

    private String formatSignedMeters(double value) {
        if (!Double.isFinite(value)) {
            return "-";
        }
        return String.format(Locale.US, "%+.1f m", value);
    }

    private String formatUnsignedMeters(double value) {
        if (!Double.isFinite(value)) {
            return "-";
        }
        return String.format(Locale.US, "%.1f m", value);
    }

    private String formatDistanceKm(double distanceKm) {
        if (!Double.isFinite(distanceKm)) {
            return "-";
        }
        return String.format(Locale.US, "%.1f km", distanceKm);
    }

    /**
     * Builds the status text when no usable terrain profile could be loaded.
     *
     * @param sourceName terrain source label
     * @param requestedSampleCount requested sample count for this path
     * @return human-readable status text
     */
    private String buildNoProfileStatusText(String sourceName, int requestedSampleCount) {
        if (sourceName == null || sourceName.isBlank()) {
            return "Terrain provider returned no usable profile for " + requestedSampleCount + " samples.";
        }
        return "Terrain provider returned no usable profile for "
                + requestedSampleCount
                + " samples: "
                + sourceName;
    }

    private String normalizeLocator6(String locator) {
        return locator == null ? "" : locator.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCallsignRaw(String callsignRaw) {
        return callsignRaw == null ? "" : callsignRaw.trim().toUpperCase(Locale.ROOT);
    }

    private double sanitizeAntennaHeightMeters(double antennaHeightMeters) {
        if (!Double.isFinite(antennaHeightMeters) || antennaHeightMeters < 0.0) {
            return 0.0;
        }
        return antennaHeightMeters;
    }

    /**
     * Internal immutable summary of LOS and Fresnel evaluation.
     */
    private static final class ProfileSummary {

        private final boolean lineOfSightClear;
        private final boolean fresnelClear;
        private final double minimumLineOfSightClearanceMeters;
        private final double minimumLowerFresnelClearanceMeters;
        private final double worstFresnelIntrusionMeters;
        private final double worstFresnelIntrusionRatio;
        private final double worstFresnelDistanceKm;
        private final int worstFresnelSampleIndex;

        private ProfileSummary(boolean lineOfSightClear,
                               boolean fresnelClear,
                               double minimumLineOfSightClearanceMeters,
                               double minimumLowerFresnelClearanceMeters,
                               double worstFresnelIntrusionMeters,
                               double worstFresnelIntrusionRatio,
                               double worstFresnelDistanceKm,
                               int worstFresnelSampleIndex) {
            this.lineOfSightClear = lineOfSightClear;
            this.fresnelClear = fresnelClear;
            this.minimumLineOfSightClearanceMeters = minimumLineOfSightClearanceMeters;
            this.minimumLowerFresnelClearanceMeters = minimumLowerFresnelClearanceMeters;
            this.worstFresnelIntrusionMeters = worstFresnelIntrusionMeters;
            this.worstFresnelIntrusionRatio = worstFresnelIntrusionRatio;
            this.worstFresnelDistanceKm = worstFresnelDistanceKm;
            this.worstFresnelSampleIndex = worstFresnelSampleIndex;
        }
    }

    /**
     * Builds an operator-facing propagation assessment from the geometric profile.
     *
     * <p>This method intentionally avoids "possible/impossible" wording. It gives
     * a practical severity estimate and suggests likely propagation mechanisms.</p>
     *
     * @param summary LOS/Fresnel summary
     * @param obstructionSummary dominant obstruction / diffraction summary
     * @return operator-facing propagation assessment
     */
    private PathPropagationAssessment buildPropagationAssessment(ProfileSummary summary,
                                                                 PathObstructionSummary obstructionSummary,
                                                                 PathLinkBudgetSummary linkBudgetSummary) {

        if (summary == null) {
            return PathPropagationAssessment.unknown();
        }

        double ssbMarginDb = linkBudgetSummary == null
                ? Double.NaN
                : linkBudgetSummary.bidirectionalSsbMarginDb();

        if (summary.lineOfSightClear && summary.fresnelClear) {
            if (Double.isFinite(ssbMarginDb) && ssbMarginDb < 0.0) {
                return new PathPropagationAssessment(
                        "Geometrically favorable, weak budget",
                        "Direct path possible, but SSB budget is weak",
                        String.format(
                                Locale.US,
                                "The path geometry is favorable, but the bidirectional SSB link margin is only %+.1f dB. CW may be more realistic.",
                                ssbMarginDb
                        ),
                        "Direct tropospheric path, CW if SSB is too weak",
                        2
                );
            }

            return PathPropagationAssessment.directFavorable();
        }

        if (summary.lineOfSightClear) {
            if (Double.isFinite(ssbMarginDb) && ssbMarginDb >= 6.0) {
                return new PathPropagationAssessment(
                        "Direct path lossy but budget-positive",
                        "Likely without aircraft scatter",
                        String.format(
                                Locale.US,
                                "The direct path is open, the Fresnel zone is obstructed, but the bidirectional SSB margin is still %+.1f dB.",
                                ssbMarginDb
                        ),
                        "Direct path with Fresnel loss, mild diffraction, tropo enhancement",
                        2
                );
            }

            return PathPropagationAssessment.directLossy(summary.worstFresnelIntrusionRatio);
        }

        if (linkBudgetSummary != null && linkBudgetSummary.hasUsableBudget()) {
            if (ssbMarginDb >= 10.0) {
                return new PathPropagationAssessment(
                        "Obstructed but budget-positive",
                        "Likely without aircraft scatter",
                        String.format(
                                Locale.US,
                                "The path is geometrically obstructed, but the bidirectional SSB link margin is still %+.1f dB after the rough diffraction estimate.",
                                ssbMarginDb
                        ),
                        "Terrain diffraction, tropo enhancement, troposcatter",
                        3
                );
            }

            if (ssbMarginDb >= 0.0) {
                return new PathPropagationAssessment(
                        "Obstructed but workable",
                        "Possible without aircraft scatter under good conditions",
                        String.format(
                                Locale.US,
                                "The path is geometrically obstructed, but the bidirectional SSB budget remains just positive at %+.1f dB. This is a candidate for diffraction/tropo, not a guaranteed QSO.",
                                ssbMarginDb
                        ),
                        "Terrain diffraction, tropo enhancement, troposcatter",
                        3
                );
            }

            if (linkBudgetSummary.bidirectionalCwMarginDb() >= 0.0) {
                return new PathPropagationAssessment(
                        "SSB marginal, CW possible",
                        "SSB weak; CW may still work",
                        String.format(
                                Locale.US,
                                "The bidirectional SSB margin is %+.1f dB, but the CW margin is %+.1f dB.",
                                ssbMarginDb,
                                linkBudgetSummary.bidirectionalCwMarginDb()
                        ),
                        "CW via diffraction/tropo, SSB only with better conditions",
                        4
                );
            }
        }

        if (obstructionSummary == null || !obstructionSummary.hasDominantLosObstruction()) {
            return PathPropagationAssessment.blockedNoLossEstimate();
        }

        double knifeEdgeLossDb = obstructionSummary.estimatedKnifeEdgeLossDb();

        if (!Double.isFinite(knifeEdgeLossDb)) {
            return PathPropagationAssessment.blockedNoLossEstimate();
        }

        if (knifeEdgeLossDb < 15.0) {
            return PathPropagationAssessment.diffractionPlausible(knifeEdgeLossDb);
        }

        if (knifeEdgeLossDb < 30.0) {
            return PathPropagationAssessment.obstructedNeedsHelp(knifeEdgeLossDb);
        }

        return PathPropagationAssessment.severelyObstructed(knifeEdgeLossDb);
    }

    /**
     * Finds the dominant LOS-blocking terrain obstruction and estimates a rough
     * single-knife-edge diffraction loss for that point.
     *
     * <p>The selected obstruction is the point with the highest estimated
     * single-knife-edge loss. This is intentionally a simple severity indicator,
     * not a full multi-edge propagation model.</p>
     *
     * @param enrichedProfilePoints enriched path profile
     * @param totalDistanceKm total path distance
     * @param analysisFrequencyMHz analysis frequency
     * @param summary existing LOS/Fresnel profile summary
     * @return obstruction summary
     */
    private PathObstructionSummary buildObstructionSummary(List<PathProfilePoint> enrichedProfilePoints,
                                                           double totalDistanceKm,
                                                           double analysisFrequencyMHz,
                                                           ProfileSummary summary) {

        if (enrichedProfilePoints == null
                || enrichedProfilePoints.size() < 3
                || !Double.isFinite(totalDistanceKm)
                || totalDistanceKm <= 0.0) {
            return PathObstructionSummary.empty();
        }

        PathObstructionCandidate bestCandidate = PathObstructionCandidate.empty();

        for (int i = 1; i < enrichedProfilePoints.size() - 1; i++) {
            PathProfilePoint point = enrichedProfilePoints.get(i);

            if (!Double.isFinite(point.lineOfSightClearanceMeters())
                    || !Double.isFinite(point.distanceKm())) {
                continue;
            }

            double heightAboveLosMeters = Math.max(0.0, -point.lineOfSightClearanceMeters());
            if (heightAboveLosMeters <= 0.0) {
                continue;
            }

            double distanceFromHomeKm = point.distanceKm();
            double distanceFromTargetKm = totalDistanceKm - point.distanceKm();

            if (distanceFromHomeKm <= 0.0 || distanceFromTargetKm <= 0.0) {
                continue;
            }

            double localFirstFresnelRadiusMeters = point.lineOfSightHeightMeters() - point.fresnelLowerHeightMeters();

            if (!Double.isFinite(localFirstFresnelRadiusMeters) || localFirstFresnelRadiusMeters <= 0.0) {
                localFirstFresnelRadiusMeters = PathGeometryUtils.calculateFirstFresnelRadiusMeters(
                        point.distanceKm(),
                        totalDistanceKm,
                        analysisFrequencyMHz
                );
            }

            double obstructionFresnelRatio =
                    Double.isFinite(localFirstFresnelRadiusMeters) && localFirstFresnelRadiusMeters > 0.0
                            ? heightAboveLosMeters / localFirstFresnelRadiusMeters
                            : Double.NaN;

            double vParameter = PathGeometryUtils.calculateKnifeEdgeVParameter(
                    heightAboveLosMeters,
                    distanceFromHomeKm,
                    distanceFromTargetKm,
                    analysisFrequencyMHz
            );

            double estimatedKnifeEdgeLossDb =
                    PathGeometryUtils.calculateSingleKnifeEdgeLossDb(vParameter);

            if (!Double.isFinite(estimatedKnifeEdgeLossDb)) {
                continue;
            }

            if (!bestCandidate.hasCandidate()
                    || estimatedKnifeEdgeLossDb > bestCandidate.estimatedKnifeEdgeLossDb()) {
                bestCandidate = new PathObstructionCandidate(
                        point.sampleIndex(),
                        point.distanceKm(),
                        heightAboveLosMeters,
                        localFirstFresnelRadiusMeters,
                        obstructionFresnelRatio,
                        vParameter,
                        estimatedKnifeEdgeLossDb
                );
            }
        }

        if (!bestCandidate.hasCandidate()) {
            return new PathObstructionSummary(
                    analysisFrequencyMHz,

                    -1,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,

                    summary.worstFresnelSampleIndex,
                    summary.worstFresnelDistanceKm,
                    summary.worstFresnelIntrusionMeters,
                    summary.worstFresnelIntrusionRatio
            );
        }

        return new PathObstructionSummary(
                analysisFrequencyMHz,

                bestCandidate.sampleIndex(),
                bestCandidate.pathDistanceKm(),
                bestCandidate.heightAboveLosMeters(),
                bestCandidate.localFirstFresnelRadiusMeters(),
                bestCandidate.obstructionFresnelRatio(),
                bestCandidate.diffractionVParameter(),
                bestCandidate.estimatedKnifeEdgeLossDb(),

                summary.worstFresnelSampleIndex,
                summary.worstFresnelDistanceKm,
                summary.worstFresnelIntrusionMeters,
                summary.worstFresnelIntrusionRatio
        );
    }

    private record PathObstructionCandidate(
            int sampleIndex,
            double pathDistanceKm,
            double heightAboveLosMeters,
            double localFirstFresnelRadiusMeters,
            double obstructionFresnelRatio,
            double diffractionVParameter,
            double estimatedKnifeEdgeLossDb
    ) {
        private static PathObstructionCandidate empty() {
            return new PathObstructionCandidate(
                    -1,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN
            );
        }

        private boolean hasCandidate() {
            return sampleIndex >= 0 && Double.isFinite(estimatedKnifeEdgeLossDb);
        }
    }

    private PathLinkBudgetSummary buildLinkBudgetSummary(double distanceKm,
                                                         double analysisFrequencyMHz,
                                                         PathObstructionSummary obstructionSummary,
                                                         PathLinkBudgetSettings settings) {

        PathLinkBudgetSettings safeSettings = settings == null
                ? PathLinkBudgetSettings.defaults()
                : settings;

        double ownTxPowerDbm = PathGeometryUtils.wattsToDbm(safeSettings.ownTxPowerWatts());
        double targetTxPowerDbm = PathGeometryUtils.wattsToDbm(safeSettings.targetTxPowerWatts());

        double freeSpacePathLossDb = PathGeometryUtils.calculateFreeSpacePathLossDb(
                distanceKm,
                analysisFrequencyMHz
        );

        double ownFeederLossDb = PathGeometryUtils.estimateFeederLossPerStationDb(
                analysisFrequencyMHz,
                safeSettings
        );

        double targetFeederLossDb = ownFeederLossDb;

        double diffractionLossDb = obstructionSummary != null
                && obstructionSummary.hasDominantLosObstruction()
                && Double.isFinite(obstructionSummary.estimatedKnifeEdgeLossDb())
                ? obstructionSummary.estimatedKnifeEdgeLossDb()
                : 0.0;

        double homeToTargetRxPowerDbm =
                ownTxPowerDbm
                        + safeSettings.ownAntennaGainDbi()
                        - ownFeederLossDb
                        - freeSpacePathLossDb
                        - diffractionLossDb
                        + safeSettings.targetAntennaGainDbi()
                        - targetFeederLossDb;

        double targetToHomeRxPowerDbm =
                targetTxPowerDbm
                        + safeSettings.targetAntennaGainDbi()
                        - targetFeederLossDb
                        - freeSpacePathLossDb
                        - diffractionLossDb
                        + safeSettings.ownAntennaGainDbi()
                        - ownFeederLossDb;

        double requiredSsbWithContestMarginDbm =
                safeSettings.requiredSsbSignalDbm() + safeSettings.contestMarginDb();

        double requiredCwWithContestMarginDbm =
                safeSettings.requiredCwSignalDbm() + safeSettings.contestMarginDb();

        double homeToTargetSsbMarginDb =
                homeToTargetRxPowerDbm - requiredSsbWithContestMarginDbm;

        double targetToHomeSsbMarginDb =
                targetToHomeRxPowerDbm - requiredSsbWithContestMarginDbm;

        double bidirectionalSsbMarginDb =
                Math.min(homeToTargetSsbMarginDb, targetToHomeSsbMarginDb);

        double homeToTargetCwMarginDb =
                homeToTargetRxPowerDbm - requiredCwWithContestMarginDbm;

        double targetToHomeCwMarginDb =
                targetToHomeRxPowerDbm - requiredCwWithContestMarginDbm;

        double bidirectionalCwMarginDb =
                Math.min(homeToTargetCwMarginDb, targetToHomeCwMarginDb);

        return new PathLinkBudgetSummary(
                analysisFrequencyMHz,

                ownTxPowerDbm,
                targetTxPowerDbm,
                safeSettings.ownAntennaGainDbi(),
                safeSettings.targetAntennaGainDbi(),

                ownFeederLossDb,
                targetFeederLossDb,
                freeSpacePathLossDb,
                diffractionLossDb,

                homeToTargetRxPowerDbm,
                targetToHomeRxPowerDbm,

                safeSettings.requiredSsbSignalDbm(),
                safeSettings.requiredCwSignalDbm(),
                safeSettings.contestMarginDb(),

                homeToTargetSsbMarginDb,
                targetToHomeSsbMarginDb,
                bidirectionalSsbMarginDb,

                homeToTargetCwMarginDb,
                targetToHomeCwMarginDb,
                bidirectionalCwMarginDb
        );
    }
}