package kst4contest.view.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reference cases for {@link PathGeometryUtils} against ITU-R P.525 / P.526 and
 * textbook values.
 *
 * <p>Every expected value is stated in the test method's Javadoc together with its
 * source, the equation and the tolerance. Where the project deliberately deviates from
 * the standard (the 32.44 constant of the free-space loss) the code's value is pinned
 * and the standard's value is documented next to it. Nothing here was derived by
 * running the code under test.</p>
 *
 * <p>Context: issue #108.</p>
 */
class PathGeometryUtilsReferenceTest {

    private static final double SPEED_OF_LIGHT_M_PER_S = 299_792_458.0;

    /**
     * Great-circle distance and initial bearing, Veness worked example.
     *
     * <p>Source: Chris Veness, "Calculate distance, bearing and more between
     * Latitude/Longitude points" (movable-type.co.uk/scripts/latlong.html): from
     * 50 03 59 N, 005 42 53 W to 58 38 38 N, 003 04 12 W the haversine distance is
     * 968.9 km on R = 6371 km and the initial bearing is 9.12 degrees. Recomputed on the
     * project's R = 6371.009 km the distance is 968.855 km. Tolerance: 0.1 km, 0.01 deg.</p>
     */
    @Test
    void greatCircleDistanceAndBearingMatchTheVenessWorkedExample() {
        double fromLat = 50.0 + 3.0 / 60.0 + 59.0 / 3600.0;
        double fromLon = -(5.0 + 42.0 / 60.0 + 53.0 / 3600.0);
        double toLat = 58.0 + 38.0 / 60.0 + 38.0 / 3600.0;
        double toLon = -(3.0 + 4.0 / 60.0 + 12.0 / 3600.0);

        assertEquals(968.855,
                PathGeometryUtils.calculateGreatCircleDistanceKm(fromLat, fromLon, toLat, toLon),
                0.1, "Veness distance on R = 6371.009 km");
        assertEquals(9.1198,
                PathGeometryUtils.calculateInitialBearingDeg(fromLat, fromLon, toLat, toLon),
                0.01, "Veness initial bearing");
    }

    /**
     * First Fresnel zone radius, ITU-R P.526-15 section 2.1 eq. (2).
     *
     * <p>R_n = sqrt(n * lambda * d1 * d2 / (d1 + d2)) with n = 1 and lambda = c / f,
     * c = 299 792 458 m/s. For d1 = d2 = 50 km at 144.3 MHz this is 227.9015 m, for
     * d1 = 10 km, d2 = 40 km at 432.2 MHz it is 74.4926 m. Tolerance: 1e-3 m against the
     * literal, relative 1e-6 against the equation evaluated in the test.</p>
     */
    @Test
    void firstFresnelRadiusMatchesP526Equation2() {
        double symmetric = PathGeometryUtils.calculateFirstFresnelRadiusMeters(50.0, 100.0, 144.3);
        double asymmetric = PathGeometryUtils.calculateFirstFresnelRadiusMeters(10.0, 50.0, 432.2);

        assertEquals(227.9015, symmetric, 1e-3, "R1 for d1 = d2 = 50 km at 144.3 MHz");
        assertEquals(74.4926, asymmetric, 1e-3, "R1 for d1 = 10 km, d2 = 40 km at 432.2 MHz");

        double expectedSymmetric = fresnelRadiusEq2(50_000.0, 50_000.0, 144.3);
        double expectedAsymmetric = fresnelRadiusEq2(10_000.0, 40_000.0, 432.2);
        assertEquals(expectedSymmetric, symmetric, expectedSymmetric * 1e-6, "R1 vs eq. (2), symmetric");
        assertEquals(expectedAsymmetric, asymmetric, expectedAsymmetric * 1e-6, "R1 vs eq. (2), asymmetric");
    }

    /**
     * First Fresnel zone radius against the practical form, ITU-R P.526-15 eq. (3).
     *
     * <p>R_1 = 550 * sqrt(d1 * d2 / ((d1 + d2) * f)) with d in km and f in MHz. The
     * constant 550 carries the rounding of sqrt(c) in these units, so the two forms
     * differ by about 0.45 percent (228.928 m vs 227.902 m, 74.828 m vs 74.493 m).
     * Tolerance: 0.5 percent.</p>
     */
    @Test
    void firstFresnelRadiusAgreesWithP526Equation3WithinHalfAPercent() {
        assertRelative(fresnelRadiusEq3(50.0, 50.0, 144.3),
                PathGeometryUtils.calculateFirstFresnelRadiusMeters(50.0, 100.0, 144.3),
                0.005, "R1 vs eq. (3), symmetric");
        assertRelative(fresnelRadiusEq3(10.0, 40.0, 432.2),
                PathGeometryUtils.calculateFirstFresnelRadiusMeters(10.0, 50.0, 432.2),
                0.005, "R1 vs eq. (3), asymmetric");
    }

    /**
     * Recommended clearance is 60 percent of the first Fresnel radius.
     *
     * <p>Source: the usual planning rule (ITU-R P.530, "0.6 F1 clearance"). Expected
     * 0.6 * 227.9015 m = 136.7409 m. Tolerance: 1e-3 m.</p>
     */
    @Test
    void recommendedClearanceIsSixtyPercentOfTheFirstFresnelRadius() {
        assertEquals(136.7409,
                PathGeometryUtils.calculateRecommendedFresnelClearanceMeters(50.0, 100.0, 144.3),
                1e-3, "0.6 * R1 for d1 = d2 = 50 km at 144.3 MHz");
    }

    /**
     * Earth bulge below the straight line between the endpoints.
     *
     * <p>h = d1 * d2 / (2 * k * R) with R = 6 371 009 m. For d1 = d2 = 50 km: 147.1509 m
     * at k = 4/3 and 196.2013 m at k = 1. Tolerance: 1e-3 m. The k = 4/3 value is also
     * checked against the effective Earth radius of 8500 km that ITU-R P.834 and the IEV
     * quote for the standard atmosphere (147.0588 m), within 0.1 percent.</p>
     */
    @Test
    void earthBulgeMatchesTheGeometricFormulaAndThe8500KmEffectiveRadius() {
        double bulgeK43 = PathGeometryUtils.calculateEarthBulgeMeters(50.0, 100.0, 4.0 / 3.0);
        double bulgeK1 = PathGeometryUtils.calculateEarthBulgeMeters(50.0, 100.0, 1.0);

        assertEquals(147.1509, bulgeK43, 1e-3, "bulge at k = 4/3");
        assertEquals(196.2013, bulgeK1, 1e-3, "bulge at k = 1");
        assertEquals(bulgeK43, PathGeometryUtils.calculateEarthBulgeMeters(50.0, 100.0), 1e-9,
                "two-argument overload uses k = 4/3");

        double bulge8500Km = 50_000.0 * 50_000.0 / (2.0 * 8_500_000.0);
        assertRelative(bulge8500Km, bulgeK43, 0.001, "bulge vs 8500 km effective radius");
    }

    /**
     * Radio horizon distance for an antenna height of 100 m.
     *
     * <p>d = sqrt(2 * k * R * h) with R = 6 371 009 m: 41.2181 km at k = 4/3 and
     * 35.6960 km at k = 1. Tolerance: 1e-3 km. The textbook approximations
     * d = 4.12 * sqrt(h) (radio, k = 4/3) and d = 3.57 * sqrt(h) (optical, k = 1), d in
     * km and h in m, are met within 0.1 percent.</p>
     */
    @Test
    void radioHorizonMatchesSqrtTwoKRHAndTheTextbookApproximations() {
        double horizonK43 = PathGeometryUtils.calculateRadioHorizonDistanceKm(100.0, 4.0 / 3.0);
        double horizonK1 = PathGeometryUtils.calculateRadioHorizonDistanceKm(100.0, 1.0);

        assertEquals(41.2181, horizonK43, 1e-3, "horizon at k = 4/3");
        assertEquals(35.6960, horizonK1, 1e-3, "horizon at k = 1");
        assertRelative(4.12 * Math.sqrt(100.0), horizonK43, 0.001, "horizon vs 4.12 sqrt(h)");
        assertRelative(3.57 * Math.sqrt(100.0), horizonK1, 0.001, "horizon vs 3.57 sqrt(h)");
    }

    /**
     * Single knife-edge diffraction loss, ITU-R P.526-15 eq. (31).
     *
     * <p>J(nu) = 6.9 + 20 log10(sqrt((nu - 0.1)^2 + 1) + nu - 0.1) for nu greater than
     * -0.78, otherwise 0. Expected: J(0) = 6.033, J(1) = 13.926, J(2.4) = 20.539,
     * J(-0.5) = 1.959, J(-1) = 0. Tolerance: 0.01 dB.</p>
     *
     * <p>At exactly nu = -0.78 the formula gives 0.004 dB; the code compares with
     * {@code v <= -0.78} and answers 0 there. That is pinned here, not endorsed: the step
     * is 0.004 dB and of no practical consequence, but the boundary belongs to the
     * zero branch in this implementation.</p>
     */
    @Test
    void knifeEdgeLossMatchesP526Equation31() {
        assertEquals(6.033, PathGeometryUtils.calculateSingleKnifeEdgeLossDb(0.0), 0.01, "J(0)");
        assertEquals(13.926, PathGeometryUtils.calculateSingleKnifeEdgeLossDb(1.0), 0.01, "J(1)");
        assertEquals(20.539, PathGeometryUtils.calculateSingleKnifeEdgeLossDb(2.4), 0.01, "J(2.4)");
        assertEquals(1.959, PathGeometryUtils.calculateSingleKnifeEdgeLossDb(-0.5), 0.01, "J(-0.5)");
        assertEquals(0.0, PathGeometryUtils.calculateSingleKnifeEdgeLossDb(-1.0), 0.0, "J(-1)");
        // Pinned: the boundary nu = -0.78 falls into the zero branch (v <= -0.78).
        assertEquals(0.0, PathGeometryUtils.calculateSingleKnifeEdgeLossDb(-0.78), 0.0, "J(-0.78)");
    }

    /**
     * Fresnel-Kirchhoff diffraction parameter, ITU-R P.526-15 eq. (26) in SI units.
     *
     * <p>nu = h * sqrt(2 * (d1 + d2) / (lambda * d1 * d2)). For h = 50 m, d1 = d2 = 25 km
     * at 144.3 MHz (lambda = 2.0776 m): nu = 0.4388. Tolerance: 1e-4.</p>
     */
    @Test
    void knifeEdgeVParameterMatchesP526Equation26() {
        assertEquals(0.4388,
                PathGeometryUtils.calculateKnifeEdgeVParameter(50.0, 25.0, 25.0, 144.3),
                1e-4, "nu for h = 50 m, d1 = d2 = 25 km, 144.3 MHz");
    }

    /**
     * Free-space basic transmission loss for 100 km at 144.3 MHz.
     *
     * <p>The code uses L = 32.44 + 20 log10(d/km) + 20 log10(f/MHz), which gives
     * 115.625 dB; that value is pinned (tolerance 0.001 dB) as decided in #108.</p>
     *
     * <p>For the record: ITU-R P.525-5 eq. (6) writes the constant as 32.4 (115.585 dB
     * here), and the exact constant 20 log10(4 pi * 1e9 / c) = 32.4478 gives 115.633 dB.
     * The project keeps 32.44; the spread of 0.05 dB is far below the uncertainty of
     * the rest of the link budget.</p>
     */
    @Test
    void freeSpacePathLossUsesTheProjectConstant32Point44() {
        double lossDb = PathGeometryUtils.calculateFreeSpacePathLossDb(100.0, 144.3);

        assertEquals(115.625, lossDb, 0.001, "FSPL 100 km / 144.3 MHz with 32.44");

        // Documented, not asserted as the project's answer: the standard's variants.
        double p525Variant = 32.4 + 20.0 * Math.log10(100.0) + 20.0 * Math.log10(144.3);
        double exactConstant = 20.0 * Math.log10(4.0 * Math.PI * 1e9 / SPEED_OF_LIGHT_M_PER_S);
        double exactVariant = exactConstant + 20.0 * Math.log10(100.0) + 20.0 * Math.log10(144.3);
        assertEquals(115.585, p525Variant, 0.001, "P.525-5 eq. (6) with 32.4");
        assertEquals(115.633, exactVariant, 0.001, "exact constant 32.4478");
    }

    /**
     * Watts to dBm: P_dBm = 10 log10(P_W * 1000).
     *
     * <p>750 W = 58.751 dBm, 100 W = 50.000 dBm. Tolerance: 1e-3 dB.</p>
     */
    @Test
    void wattsToDbmMatchesTheDefinition() {
        assertEquals(58.751, PathGeometryUtils.wattsToDbm(750.0), 1e-3, "750 W");
        assertEquals(50.0, PathGeometryUtils.wattsToDbm(100.0), 1e-3, "100 W");
    }

    /**
     * Adaptive sample count: ceil(d / 0.5 km) + 1, clamped to 121 .. 1201.
     *
     * <p>0 km (non-positive) yields the minimum 121, 50 km computes 101 and is clamped
     * to 121, 100 km gives 201, 1000 km computes 2001 and is clamped to 1201.</p>
     */
    @Test
    void adaptiveSampleCountFollowsTheHalfKilometreRuleWithClamping() {
        assertEquals(121, PathGeometryUtils.resolveAdaptiveSampleCount(0.0), "0 km");
        assertEquals(121, PathGeometryUtils.resolveAdaptiveSampleCount(50.0), "50 km");
        assertEquals(201, PathGeometryUtils.resolveAdaptiveSampleCount(100.0), "100 km");
        assertEquals(1201, PathGeometryUtils.resolveAdaptiveSampleCount(1000.0), "1000 km");
    }

    /**
     * Effective Earth radius factor sanitising: values outside 0.5 .. 10 fall back to 4/3.
     *
     * <p>0.4 and 11 are replaced by 4/3, 1.0 is kept. Tolerance: 1e-12.</p>
     */
    @Test
    void effectiveEarthRadiusFactorOutsideTheAcceptedRangeFallsBackToFourThirds() {
        assertEquals(4.0 / 3.0, PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(0.4), 1e-12, "0.4");
        assertEquals(4.0 / 3.0, PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(11.0), 1e-12, "11");
        assertEquals(1.0, PathGeometryUtils.sanitizeEffectiveEarthRadiusFactor(1.0), 1e-12, "1.0");
    }

    /** ITU-R P.526-15 eq. (2) with n = 1, distances in metres. */
    private static double fresnelRadiusEq2(double d1Meters, double d2Meters, double frequencyMHz) {
        double wavelengthMeters = SPEED_OF_LIGHT_M_PER_S / (frequencyMHz * 1e6);
        return Math.sqrt(wavelengthMeters * d1Meters * d2Meters / (d1Meters + d2Meters));
    }

    /** ITU-R P.526-15 eq. (3): distances in km, frequency in MHz, result in metres. */
    private static double fresnelRadiusEq3(double d1Km, double d2Km, double frequencyMHz) {
        return 550.0 * Math.sqrt(d1Km * d2Km / ((d1Km + d2Km) * frequencyMHz));
    }

    private static void assertRelative(double expected, double actual, double relativeTolerance, String message) {
        assertEquals(expected, actual, Math.abs(expected) * relativeTolerance, message);
    }
}
