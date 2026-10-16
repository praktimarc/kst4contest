package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The ground elevation overrides of {@link PathAnalysisRequest} replace the terrain the
 * profile reports at the endpoints, and only there.
 *
 * <p>Scenario: 20 km due north from 50.0 N 10.0 E over terrain of 0 m, both antennas
 * 10 m above ground. The curvature correction is zero at both endpoints, so the line of
 * sight runs from ground + 10 m at the start to ground + 10 m at the end and is linear in
 * between.</p>
 */
class GeometryOnlyPathAnalysisServiceGroundOverrideTest {

    private static final double EARTH_RADIUS_KM = 6371.009;
    private static final double ANTENNA_HEIGHT_M = 10.0;

    /** Flat 0 m terrain with the requested number of samples along a meridian. */
    private static final class FlatTerrainProfileProvider implements TerrainProfileProvider {
        @Override
        public TerrainProfileData loadProfile(TerrainProfileRequest request) {
            int sampleCount = request.requestedSampleCount();
            List<PathProfilePoint> points = new ArrayList<>(sampleCount);
            for (int i = 0; i < sampleCount; i++) {
                double fraction = (double) i / (sampleCount - 1);
                points.add(new PathProfilePoint(
                        request.totalDistanceKm() * fraction,
                        request.fromLatitudeDeg() + (request.toLatitudeDeg() - request.fromLatitudeDeg()) * fraction,
                        request.fromLongitudeDeg(),
                        0.0));
            }
            return new TerrainProfileData(points, "Flat test terrain", false);
        }
    }

    private static PathAnalysisResult analyze(double homeGroundMeters, double targetGroundMeters) {
        double toLatitudeDeg = 50.0 + Math.toDegrees(20.0 / EARTH_RADIUS_KM);
        PathAnalysisRequest request = new PathAnalysisRequest(
                "JO50AA", 50.0, 10.0,
                "DL0TEST", "JO50AE", toLatitudeDeg, 10.0,
                144.3, ANTENNA_HEIGHT_M, ANTENNA_HEIGHT_M,
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                PathLinkBudgetSettings.defaults(),
                homeGroundMeters,
                targetGroundMeters
        );
        return new GeometryOnlyPathAnalysisService(new FlatTerrainProfileProvider()).analyze(request);
    }

    private static PathProfilePoint first(PathAnalysisResult result) {
        return result.profilePoints().get(0);
    }

    private static PathProfilePoint last(PathAnalysisResult result) {
        return result.profilePoints().get(result.profilePoints().size() - 1);
    }

    @Test
    void withoutOverridesTheModelTerrainIsUsed() {
        PathAnalysisResult result = analyze(Double.NaN, Double.NaN);

        assertEquals(0.0, first(result).elevationMeters(), 1e-9);
        assertEquals(ANTENNA_HEIGHT_M, first(result).lineOfSightHeightMeters(), 1e-9);
        assertEquals(ANTENNA_HEIGHT_M, last(result).lineOfSightHeightMeters(), 1e-9);
    }

    @Test
    void theOwnHeightAboveSeaLevelReplacesTheStartTerrain() {
        PathAnalysisResult result = analyze(100.0, Double.NaN);

        assertEquals(100.0, first(result).elevationMeters(), 1e-9);
        assertEquals(100.0, first(result).curvatureAdjustedElevationMeters(), 1e-9);
        assertEquals(110.0, first(result).lineOfSightHeightMeters(), 1e-9, "own antenna on the entered ground");
        assertEquals(0.0, last(result).elevationMeters(), 1e-9, "the far end stays on the model");
        assertEquals(ANTENNA_HEIGHT_M, last(result).lineOfSightHeightMeters(), 1e-9);

        PathProfilePoint midpoint = result.profilePoints().get(result.profilePoints().size() / 2);
        assertEquals(0.0, midpoint.elevationMeters(), 1e-9, "the path between stays on the model");
        assertEquals(60.0, midpoint.lineOfSightHeightMeters(), 1e-6, "LOS is linear between the antennas");
    }

    @Test
    void theTargetGroundElevationReplacesTheEndTerrain() {
        PathAnalysisResult result = analyze(Double.NaN, 50.0);

        assertEquals(0.0, first(result).elevationMeters(), 1e-9);
        assertEquals(50.0, last(result).elevationMeters(), 1e-9);
        assertEquals(60.0, last(result).lineOfSightHeightMeters(), 1e-9);
    }
}
