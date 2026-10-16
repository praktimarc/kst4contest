package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Normalisation in the {@link PathAnalysisRequest} constructor and the endpoint copy.
 */
class PathAnalysisRequestTest {

    private static PathAnalysisRequest withFactor(double effectiveEarthRadiusFactor) {
        return new PathAnalysisRequest(
                "JO50AA", 50.0, 10.0,
                "DL0TEST", "JO50AE", 50.2, 10.0,
                144.3, 10.0, 10.0,
                effectiveEarthRadiusFactor,
                PathLinkBudgetSettings.defaults()
        );
    }

    /*
     * The constructor called the sanitizer and dropped its result, so a broken k-factor
     * stayed in the request. Nothing computed with it, because the analysis service
     * sanitizes once more, but the request itself said something else than what was used.
     */
    @Test
    void anUnusableEarthRadiusFactorIsReplacedByTheDefault() {
        assertEquals(PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                withFactor(99.0).effectiveEarthRadiusFactor(), 1e-12);
        assertEquals(PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                withFactor(Double.NaN).effectiveEarthRadiusFactor(), 1e-12);
        assertEquals(PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                withFactor(0.1).effectiveEarthRadiusFactor(), 1e-12);
    }

    /*
     * Valid factors pass the sanitizer unchanged (it is the identity on 0.5 .. 10), so
     * assigning its result changes nothing for valid input.
     */
    @Test
    void aUsableEarthRadiusFactorIsKept() {
        for (double factor : new double[] {0.5, 1.0, 4.0 / 3.0, 2.5, 10.0}) {
            assertEquals(factor, withFactor(factor).effectiveEarthRadiusFactor(), 0.0, "k = " + factor);
        }
    }

    @Test
    void withoutOverridesBothGroundElevationsAreLeftToTheModel() {
        PathAnalysisRequest request = withFactor(4.0 / 3.0);

        assertTrue(Double.isNaN(request.homeGroundElevationOverrideMeters()));
        assertTrue(Double.isNaN(request.targetGroundElevationOverrideMeters()));
    }

    @Test
    void anInfiniteOverrideCountsAsNone() {
        PathAnalysisRequest request = new PathAnalysisRequest(
                "JO50AA", 50.0, 10.0,
                "DL0TEST", "JO50AE", 50.2, 10.0,
                144.3, 10.0, 10.0,
                4.0 / 3.0,
                PathLinkBudgetSettings.defaults(),
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        );

        assertTrue(Double.isNaN(request.homeGroundElevationOverrideMeters()));
        assertTrue(Double.isNaN(request.targetGroundElevationOverrideMeters()));
    }

    @Test
    void movingTheTargetKeepsEverythingElse() {
        PathAnalysisRequest request = new PathAnalysisRequest(
                "JO50AA", 50.0, 10.0,
                "DL0TEST", "JO50AE", 50.2, 10.0,
                144.3, 12.0, 8.0,
                1.0,
                PathLinkBudgetSettings.defaults(),
                312.0,
                Double.NaN
        );

        PathAnalysisRequest moved = request.withTargetEndpoint(50.21, 10.01, 480.0);

        assertEquals(50.21, moved.toLatitudeDeg(), 1e-12);
        assertEquals(10.01, moved.toLongitudeDeg(), 1e-12);
        assertEquals(480.0, moved.targetGroundElevationOverrideMeters(), 1e-12);
        assertEquals("JO50AE", moved.toLocator6());
        assertEquals("DL0TEST", moved.toCallsignRaw());
        assertEquals(312.0, moved.homeGroundElevationOverrideMeters(), 1e-12);
        assertEquals(12.0, moved.homeAntennaHeightMeters(), 1e-12);
        assertEquals(8.0, moved.targetAntennaHeightMeters(), 1e-12);
        assertEquals(1.0, moved.effectiveEarthRadiusFactor(), 1e-12);
        assertEquals(144.3, moved.frequencyMHz(), 1e-12);
    }
}
