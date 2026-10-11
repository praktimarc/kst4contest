package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The path-analysis pipeline of {@link GeometryOnlyPathAnalysisService} on a flat
 * terrain profile, with every expected value computed from the underlying formulas
 * rather than from the code.
 *
 * <p>Scenario: 20.000 km due north from 50.0 N 10.0 E, both antennas 10 m above a
 * terrain of 0 m everywhere, 144.3 MHz, k = 4/3, default link-budget settings. A path
 * along a meridian makes the haversine distance exactly R times the latitude difference,
 * so the 20 km are hit to floating-point precision. The in-memory terrain provider hands
 * back the requested number of samples (121 for 20 km, so the midpoint is sample 60 at
 * exactly 10.000 km) with elevation 0.</p>
 *
 * <p>Expected midpoint values (R = 6 371 009 m, c = 299 792 458 m/s):</p>
 * <pre>
 *   bulge      = d1 d2 / (2 k R)        = 1e4 * 1e4 / (2 * 4/3 * 6371009) = 5.886 m
 *   LOS height = 10 m (both endpoints have bulge 0 and antenna 10 m)
 *   clearance  = 10 - 5.886                                              = 4.114 m
 *   lambda     = c / 144.3e6                                             = 2.0776 m
 *   R1         = sqrt(lambda d1 d2 / (d1 + d2)) = sqrt(2.0776 * 5000)   = 101.921 m
 *   intrusion  = bulge - (LOS - R1) = 5.886 - (10 - 101.921)            = 97.807 m
 * </pre>
 * <p>Tolerances: 0.05 m for bulge and clearance, 0.1 m for Fresnel radius and intrusion,
 * 0.01 dB for losses.</p>
 *
 * <p>Context: issue #108.</p>
 */
class GeometryOnlyPathAnalysisServiceProfileTest {

    private static final double EARTH_RADIUS_KM = 6371.009;
    private static final double TOTAL_DISTANCE_KM = 20.0;
    private static final double ANTENNA_HEIGHT_M = 10.0;
    private static final double FREQUENCY_MHZ = 144.3;
    private static final int MIDPOINT_INDEX = 60;

    private static final double EXPECTED_MIDPOINT_BULGE_M = 5.886;
    private static final double EXPECTED_MIDPOINT_LOS_CLEARANCE_M = 4.114;
    private static final double EXPECTED_MIDPOINT_FRESNEL_RADIUS_M = 101.921;
    private static final double EXPECTED_MIDPOINT_INTRUSION_M = 97.807;

    /**
     * Terrain provider that answers a flat profile of the requested sample count.
     * Points are spaced uniformly along the path; latitude is interpolated linearly,
     * which is exact for a meridian.
     */
    private static final class FlatTerrainProfileProvider implements TerrainProfileProvider {
        @Override
        public TerrainProfileData loadProfile(TerrainProfileRequest request) {
            int sampleCount = request.requestedSampleCount();
            List<PathProfilePoint> points = new ArrayList<>(sampleCount);
            for (int i = 0; i < sampleCount; i++) {
                double fraction = (double) i / (sampleCount - 1);
                double distanceKm = request.totalDistanceKm() * fraction;
                double latitudeDeg = request.fromLatitudeDeg()
                        + (request.toLatitudeDeg() - request.fromLatitudeDeg()) * fraction;
                points.add(new PathProfilePoint(distanceKm, latitudeDeg, request.fromLongitudeDeg(), 0.0));
            }
            return new TerrainProfileData(points, "Flat test terrain", false);
        }
    }

    private static PathAnalysisResult analyzeFlatPath() {
        double fromLatitudeDeg = 50.0;
        double longitudeDeg = 10.0;
        double toLatitudeDeg = fromLatitudeDeg + Math.toDegrees(TOTAL_DISTANCE_KM / EARTH_RADIUS_KM);

        PathAnalysisRequest request = new PathAnalysisRequest(
                "JO50AA",
                fromLatitudeDeg,
                longitudeDeg,
                "DL0TEST",
                "JO50AE",
                toLatitudeDeg,
                longitudeDeg,
                FREQUENCY_MHZ,
                ANTENNA_HEIGHT_M,
                ANTENNA_HEIGHT_M,
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                PathLinkBudgetSettings.defaults()
        );
        return new GeometryOnlyPathAnalysisService(new FlatTerrainProfileProvider()).analyze(request);
    }

    /**
     * The scenario is what the Javadoc says: 20.000 km, 121 samples, midpoint at 10 km.
     *
     * <p>Sample count: ceil(20 / 0.5) + 1 = 41, clamped to the minimum of 121.
     * Tolerance: 1e-6 km for the distance.</p>
     */
    @Test
    void scenarioIsTwentyKilometresWithTheMidpointAtSampleSixty() {
        PathAnalysisResult result = analyzeFlatPath();

        assertTrue(result.hasUsableProfile(), "profile usable");
        assertEquals(TOTAL_DISTANCE_KM, result.distanceKm(), 1e-6, "path distance");
        assertEquals(121, result.profilePoints().size(), "sample count");
        assertEquals(10.0, result.profilePoints().get(MIDPOINT_INDEX).distanceKm(), 1e-9, "midpoint distance");
    }

    /**
     * Earth bulge at the midpoint: d1 d2 / (2 k R) = 5.886 m at k = 4/3.
     *
     * <p>On a flat profile the curvature-adjusted elevation is the bulge itself.
     * Tolerance: 0.05 m.</p>
     */
    @Test
    void midpointBulgeIsFivePointNineMetres() {
        PathProfilePoint midpoint = analyzeFlatPath().profilePoints().get(MIDPOINT_INDEX);

        assertEquals(EXPECTED_MIDPOINT_BULGE_M, midpoint.curvatureAdjustedElevationMeters(), 0.05,
                "curvature-adjusted elevation at 10 km");
    }

    /**
     * Line of sight: 10 m antenna height minus 5.886 m bulge leaves 4.114 m, so the
     * direct path is free and the minimum clearance of the whole profile is the midpoint
     * value. Tolerance: 0.05 m.
     */
    @Test
    void lineOfSightIsFreeWithFourMetresClearanceAtTheMidpoint() {
        PathAnalysisResult result = analyzeFlatPath();
        PathProfilePoint midpoint = result.profilePoints().get(MIDPOINT_INDEX);

        assertEquals(ANTENNA_HEIGHT_M, midpoint.lineOfSightHeightMeters(), 1e-9, "LOS height");
        assertEquals(EXPECTED_MIDPOINT_LOS_CLEARANCE_M, midpoint.lineOfSightClearanceMeters(), 0.05,
                "LOS clearance at 10 km");
        assertTrue(result.lineOfSightClear(), "line of sight clear");
        assertEquals(EXPECTED_MIDPOINT_LOS_CLEARANCE_M, result.minimumLineOfSightClearanceMeters(), 0.05,
                "minimum LOS clearance");
    }

    /**
     * First Fresnel zone: R1 = sqrt(lambda d1 d2 / (d1 + d2)) = 101.921 m at the midpoint,
     * far more than the 4.114 m of clearance, so the zone is intruded by
     * 5.886 - (10 - 101.921) = 97.807 m at 10 km. Tolerance: 0.1 m.
     */
    @Test
    void fresnelZoneIsIntrudedByNinetyEightMetresAtTheMidpoint() {
        PathAnalysisResult result = analyzeFlatPath();
        PathProfilePoint midpoint = result.profilePoints().get(MIDPOINT_INDEX);

        double localFresnelRadiusM = midpoint.lineOfSightHeightMeters() - midpoint.fresnelLowerHeightMeters();
        assertEquals(EXPECTED_MIDPOINT_FRESNEL_RADIUS_M, localFresnelRadiusM, 0.1, "R1 at 10 km");
        assertEquals(EXPECTED_MIDPOINT_INTRUSION_M, midpoint.fresnelIntrusionMeters(), 0.1, "intrusion at 10 km");

        assertFalse(result.fresnelClear(), "Fresnel zone intruded");
        assertEquals(EXPECTED_MIDPOINT_INTRUSION_M, result.worstFresnelIntrusionMeters(), 0.1, "worst intrusion");
        assertEquals(10.0, result.worstFresnelDistanceKm(), 1e-9, "worst intrusion distance");
        assertEquals(MIDPOINT_INDEX, result.worstFresnelSampleIndex(), "worst intrusion sample");
        assertEquals(EXPECTED_MIDPOINT_INTRUSION_M / EXPECTED_MIDPOINT_FRESNEL_RADIUS_M,
                result.worstFresnelIntrusionRatio(), 1e-3, "worst intrusion ratio");
    }

    /**
     * No knife edge: the obstruction search only considers samples that rise above the
     * line of sight (h greater than 0). On a flat profile with free LOS there is none, so
     * there is no dominant obstruction and no knife-edge loss estimate.
     */
    @Test
    void noKnifeEdgeCandidateOnAFreeLineOfSight() {
        PathObstructionSummary obstruction = analyzeFlatPath().obstructionSummary();

        assertFalse(obstruction.hasDominantLosObstruction(), "dominant obstruction");
        assertEquals(-1, obstruction.dominantObstructionSampleIndex(), "obstruction sample index");
        assertTrue(Double.isNaN(obstruction.estimatedKnifeEdgeLossDb()), "knife-edge loss is NaN");
        assertEquals(EXPECTED_MIDPOINT_INTRUSION_M, obstruction.worstFresnelIntrusionMeters(), 0.1,
                "obstruction summary carries the worst Fresnel intrusion");
    }

    /**
     * Pinned, not endorsed: a partial Fresnel intrusion without LOS blockage adds 0 dB to
     * the link budget.
     *
     * <p>The link budget takes its diffraction term only from a dominant LOS obstruction.
     * Here the first Fresnel zone is intruded by 96 percent of its radius yet the direct
     * path is open, so the code books 0 dB. ITU-R P.526 eq. (31) would still give a loss
     * for negative nu down to -0.78 (here nu = -0.057, about 5.5 dB; in general 0 .. 6 dB
     * for an open but Fresnel-obstructed path). Tracked as a separate issue; this test
     * records the current behaviour. Tolerance: 0.01 dB.</p>
     */
    @Test
    void partialFresnelIntrusionWithoutLosBlockageContributesZeroDiffractionLoss() {
        PathLinkBudgetSummary budget = analyzeFlatPath().linkBudgetSummary();

        assertEquals(0.0, budget.diffractionLossDb(), 0.01, "diffraction loss in the link budget");
    }
}
