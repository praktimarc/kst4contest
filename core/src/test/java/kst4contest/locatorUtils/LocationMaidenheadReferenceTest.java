package kst4contest.locatorUtils;

import kst4contest.view.map.PathGeometryUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reference cases for the Maidenhead and great-circle maths in {@link Location}.
 *
 * <p>None of the expected values below was derived from the code under test. The
 * Maidenhead centres follow from the definition of the locator system, the distances
 * and bearings from an independent spherical computation (haversine and atan2, see
 * {@link #expectedDistanceAndBearingOfLocatorPairs}), both on the IUGG mean radius
 * R = 6371.009 km that {@code Location} declares. Behaviour that is merely pinned
 * (not endorsed) is marked as such in the test method's Javadoc.</p>
 *
 * <p>Context: issue #108.</p>
 */
class LocationMaidenheadReferenceTest {

    /** Tolerance for a subsquare centre: well below the 1e-7 degree print precision. */
    private static final double CENTRE_TOLERANCE_DEG = 1e-6;

    /** Tolerance for values that the code rounds to 0.01 km / 0.01 degrees. */
    private static final double ROUNDED_TOLERANCE = 0.005;

    /**
     * Locator to subsquare centre, by definition.
     *
     * <p>Source: Wikipedia, "Maidenhead Locator System". A field spans 20 degrees of
     * longitude and 10 degrees of latitude, a square 2 by 1 degrees, a subsquare 5 by
     * 2.5 arc minutes. The centre of a subsquare is therefore</p>
     * <pre>
     *   lon = -180 + 20 * F1 + 2 * S1 + (5/60) * X1 + (5/60)/2
     *   lat =  -90 + 10 * F2 +     S2 + (2.5/60) * X2 + (2.5/60)/2
     * </pre>
     * <p>with F = field letter index (A = 0), S = square digit, X = subsquare letter
     * index. Tolerance: 1e-6 degrees.</p>
     */
    @Test
    void locatorIsResolvedToTheCentreOfItsSubsquare() {
        assertCentre("JO62QM", 52.5208333, 13.3750000);
        assertCentre("JN49FK", 49.4375000, 8.4583333);
        assertCentre("JO50JP", 50.6458333, 10.7916667);
        assertCentre("AA00AA", -89.9791667, -179.9583333);
        assertCentre("RR99XX", 89.9791667, 179.9583333);
    }

    /**
     * Round trip from the subsquare centre back to the locator.
     *
     * <p>The coordinates are the JO62QM centre of the previous test. The expected string
     * uses a lower-case subsquare: that is what {@link Location#toMaidenhead(double, double)}
     * produces today and is pinned here, not endorsed. The locator system itself is
     * case-insensitive; the ON4KST convention is upper case.</p>
     */
    @Test
    void centreCoordinatesRoundTripToTheLocatorWithLowerCaseSubsquare() {
        assertEquals("JO62qm", Location.toMaidenhead(52.5208333, 13.375));
    }

    /**
     * Distance and bearing between locator centres against an independent spherical
     * computation.
     *
     * <p>Expected values were computed with Python 3 ({@code python3 -I}) on the subsquare
     * centres of the previous test, R = 6371.009 km, using</p>
     * <pre>
     *   a     = sin^2(dphi/2) + cos(phi1) * cos(phi2) * sin^2(dlambda/2)
     *   d     = 2 * R * atan2(sqrt(a), sqrt(1 - a))                        (haversine)
     *   theta = atan2(sin(dlambda) * cos(phi2),
     *                 cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dlambda))
     *   bearing = (deg(theta) + 360) mod 360
     * </pre>
     * <p>giving JO50JP to JO31AA 336.1207 km / 278.963 deg, JO50JP to JN49FK
     * 214.0343 km / 232.018 deg, JO62QM to JN58TD 502.0447 km / 195.002 deg.
     * {@link Location#getDistanceKmByTwoLocatorStrings} and {@link Location#getBearing}
     * round to two decimals, so the rounded values are pinned with a tolerance of
     * 0.005 km and 0.005 degrees.</p>
     */
    @Test
    void expectedDistanceAndBearingOfLocatorPairs() {
        assertDistanceAndBearing("JO50JP", "JO31AA", 336.12, 278.96);
        assertDistanceAndBearing("JO50JP", "JN49FK", 214.03, 232.02);
        assertDistanceAndBearing("JO62QM", "JN58TD", 502.04, 195.00);
    }

    /**
     * The two distance implementations in the project agree.
     *
     * <p>{@link Location} uses the spherical law of cosines, {@link PathGeometryUtils}
     * the haversine formula; both declare R = 6371.009 km. On the same pair of subsquare
     * centres the unrounded results must agree within 0.005 km (the law of cosines loses
     * precision only for distances far below one kilometre).</p>
     */
    @Test
    void lawOfCosinesAndHaversineAgreeOnTheSamePair() {
        Location from = new Location("JO50JP");
        Location to = new Location("JN58TD");

        double lawOfCosinesKm = from.getDistanceKm(to);
        double haversineKm = PathGeometryUtils.calculateGreatCircleDistanceKm(
                from.getLatitude().toDegrees(),
                from.getLongitude().toDegrees(),
                to.getLatitude().toDegrees(),
                to.getLongitude().toDegrees()
        );

        assertEquals(haversineKm, lawOfCosinesKm, ROUNDED_TOLERANCE,
                "law of cosines vs haversine on JO50JP -> JN58TD");
    }

    /**
     * Bearing between identical points in {@link Location}: NaN.
     *
     * <p>Pinned, not endorsed. The bearing between a point and itself is undefined;
     * {@code Location.getBearing} answers {@code Double.NaN}.</p>
     */
    @Test
    void locationBearingOfIdenticalPointsIsNaN() {
        Location point = new Location("JO62QM");

        assertTrue(Double.isNaN(Location.getBearing(point, new Location("JO62QM"))),
                "Location.getBearing for identical points");
    }

    /**
     * Bearing between identical points in {@link PathGeometryUtils}: 0 degrees.
     *
     * <p>Pinned, not endorsed. For identical coordinates both atan2 arguments are zero
     * and {@code atan2(0, 0)} is 0, so {@code calculateInitialBearingDeg} answers 0 where
     * {@code Location.getBearing} answers NaN. The two implementations disagree on the
     * undefined case; this test records the current answer.</p>
     */
    @Test
    void pathGeometryBearingOfIdenticalPointsIsZero() {
        double bearingDeg = PathGeometryUtils.calculateInitialBearingDeg(
                52.5208333, 13.375, 52.5208333, 13.375);

        assertEquals(0.0, bearingDeg, 1e-12, "PathGeometryUtils bearing for identical points");
    }

    private static void assertCentre(String locator, double expectedLatDeg, double expectedLonDeg) {
        Location location = new Location(locator);
        assertEquals(expectedLatDeg, location.getLatitude().toDegrees(), CENTRE_TOLERANCE_DEG,
                locator + " latitude");
        assertEquals(expectedLonDeg, location.getLongitude().toDegrees(), CENTRE_TOLERANCE_DEG,
                locator + " longitude");
    }

    private static void assertDistanceAndBearing(String fromLocator,
                                                 String toLocator,
                                                 double expectedRoundedKm,
                                                 double expectedRoundedBearingDeg) {
        Location helper = new Location();

        assertEquals(expectedRoundedKm,
                helper.getDistanceKmByTwoLocatorStrings(fromLocator, toLocator),
                ROUNDED_TOLERANCE,
                fromLocator + " -> " + toLocator + " distance");
        assertEquals(expectedRoundedBearingDeg,
                helper.getBearingOfTwoLocatorStrings(fromLocator, toLocator),
                ROUNDED_TOLERANCE,
                fromLocator + " -> " + toLocator + " bearing");
    }
}
