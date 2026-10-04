package kst4contest.view.map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helper methods for path geometry and radio-path calculations.
 *
 * <p>This class intentionally centralizes:
 * <ul>
 *     <li>great-circle geometry</li>
 *     <li>Earth curvature calculations</li>
 *     <li>Fresnel calculations</li>
 *     <li>adaptive profile sampling heuristics</li>
 *     <li>default/fallback frequency handling</li>
 *     <li>tolerant frequency parsing from UI/chat strings</li>
 * </ul>
 */
public final class PathGeometryUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_009.0;
    private static final double EARTH_RADIUS_KM = EARTH_RADIUS_METERS / 1000.0;
    private static final double SPEED_OF_LIGHT_METERS_PER_SECOND = 299_792_458.0;

    /**
     * Default effective Earth radius factor used for VHF/UHF path geometry.
     *
     * <p>k = 4/3 is the common standard-atmosphere approximation. It bends the
     * radio path slightly with the atmosphere and therefore reduces the apparent
     * Earth bulge compared with pure optical geometry.</p>
     *
     * <p>This is still only a geometric/refraction approximation. It does not model
     * troposcatter, aircraft scatter, ducting or diffraction loss numerically.</p>
     */
    public static final double DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR = 4.0 / 3.0;

    /**
     * Central fallback frequency for path analysis when no station frequency
     * could be extracted from the chat member / aggregated marker data.
     *
     * Easy to change later if users mainly work on another band.
     */
    public static final double DEFAULT_ANALYSIS_FREQUENCY_MHZ = 144.0;

    /**
     * Common practical recommendation: at least 60% of the first Fresnel zone
     * should remain clear.
     */
    public static final double DEFAULT_FRESNEL_CLEARANCE_FACTOR = 0.60;

    /**
     * Current sampling heuristic for terrain/profile analysis.
     *
     * <p>The active goal is to sample approximately every 0.5 km while
     * keeping the UI responsive. A later batch/service variant can use
     * different limits.</p>
     */
    public static final double DEFAULT_TARGET_SAMPLE_STEP_KM = 0.5;
    public static final int MIN_PROFILE_SAMPLE_COUNT = 121;
    public static final int MAX_PROFILE_SAMPLE_COUNT = 1201;

    private static final Pattern FREQUENCY_MHZ_PATTERN =
            Pattern.compile("(?i)(\\d{2,6}(?:[\\.,]\\d+)?)\\s*(?:mhz)?");

    private PathGeometryUtils() {
    }

    /**
     * Great-circle distance in kilometers using the haversine formula.
     *
     * @param fromLatitudeDeg source latitude in degrees
     * @param fromLongitudeDeg source longitude in degrees
     * @param toLatitudeDeg target latitude in degrees
     * @param toLongitudeDeg target longitude in degrees
     * @return great-circle distance in kilometers
     */
    public static double calculateGreatCircleDistanceKm(double fromLatitudeDeg,
                                                        double fromLongitudeDeg,
                                                        double toLatitudeDeg,
                                                        double toLongitudeDeg) {

        if (!Double.isFinite(fromLatitudeDeg)
                || !Double.isFinite(fromLongitudeDeg)
                || !Double.isFinite(toLatitudeDeg)
                || !Double.isFinite(toLongitudeDeg)) {
            return Double.NaN;
        }

        double fromLatitudeRad = Math.toRadians(fromLatitudeDeg);
        double fromLongitudeRad = Math.toRadians(fromLongitudeDeg);
        double toLatitudeRad = Math.toRadians(toLatitudeDeg);
        double toLongitudeRad = Math.toRadians(toLongitudeDeg);

        double deltaLatitude = toLatitudeRad - fromLatitudeRad;
        double deltaLongitude = toLongitudeRad - fromLongitudeRad;

        double a = Math.sin(deltaLatitude / 2.0) * Math.sin(deltaLatitude / 2.0)
                + Math.cos(fromLatitudeRad) * Math.cos(toLatitudeRad)
                * Math.sin(deltaLongitude / 2.0) * Math.sin(deltaLongitude / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Initial great-circle bearing in degrees from north.
     *
     * @param fromLatitudeDeg source latitude in degrees
     * @param fromLongitudeDeg source longitude in degrees
     * @param toLatitudeDeg target latitude in degrees
     * @param toLongitudeDeg target longitude in degrees
     * @return initial bearing in degrees within [0, 360)
     */
    public static double calculateInitialBearingDeg(double fromLatitudeDeg,
                                                    double fromLongitudeDeg,
                                                    double toLatitudeDeg,
                                                    double toLongitudeDeg) {

        if (!Double.isFinite(fromLatitudeDeg)
                || !Double.isFinite(fromLongitudeDeg)
                || !Double.isFinite(toLatitudeDeg)
                || !Double.isFinite(toLongitudeDeg)) {
            return Double.NaN;
        }

        double fromLatitudeRad = Math.toRadians(fromLatitudeDeg);
        double toLatitudeRad = Math.toRadians(toLatitudeDeg);
        double deltaLongitudeRad = Math.toRadians(toLongitudeDeg - fromLongitudeDeg);

        double y = Math.sin(deltaLongitudeRad) * Math.cos(toLatitudeRad);
        double x = Math.cos(fromLatitudeRad) * Math.sin(toLatitudeRad)
                - Math.sin(fromLatitudeRad) * Math.cos(toLatitudeRad) * Math.cos(deltaLongitudeRad);

        double bearingDeg = Math.toDegrees(Math.atan2(y, x));

        return normalizeBearingDeg(bearingDeg);
    }

    /**
     * Interpolates one point on the great-circle path between two endpoints.
     *
     * <p>This uses spherical linear interpolation (slerp) on the unit sphere.
     * It avoids the path distortion that appears when latitude and longitude
     * are interpolated independently.</p>
     *
     * @param fromLatitudeDeg source latitude in degrees
     * @param fromLongitudeDeg source longitude in degrees
     * @param toLatitudeDeg target latitude in degrees
     * @param toLongitudeDeg target longitude in degrees
     * @param t interpolation factor in [0, 1]
     * @return interpolated great-circle point
     */
    public static GeoPoint interpolateGreatCirclePoint(double fromLatitudeDeg,
                                                       double fromLongitudeDeg,
                                                       double toLatitudeDeg,
                                                       double toLongitudeDeg,
                                                       double t) {

        if (!Double.isFinite(fromLatitudeDeg)
                || !Double.isFinite(fromLongitudeDeg)
                || !Double.isFinite(toLatitudeDeg)
                || !Double.isFinite(toLongitudeDeg)
                || !Double.isFinite(t)) {
            return new GeoPoint(Double.NaN, Double.NaN);
        }

        double clampedT = clamp(t, 0.0, 1.0);

        if (clampedT <= 0.0) {
            return new GeoPoint(fromLatitudeDeg, normalizeLongitudeDeg(fromLongitudeDeg));
        }

        if (clampedT >= 1.0) {
            return new GeoPoint(toLatitudeDeg, normalizeLongitudeDeg(toLongitudeDeg));
        }

        double fromLatitudeRad = Math.toRadians(fromLatitudeDeg);
        double fromLongitudeRad = Math.toRadians(fromLongitudeDeg);
        double toLatitudeRad = Math.toRadians(toLatitudeDeg);
        double toLongitudeRad = Math.toRadians(toLongitudeDeg);

        double x1 = Math.cos(fromLatitudeRad) * Math.cos(fromLongitudeRad);
        double y1 = Math.cos(fromLatitudeRad) * Math.sin(fromLongitudeRad);
        double z1 = Math.sin(fromLatitudeRad);

        double x2 = Math.cos(toLatitudeRad) * Math.cos(toLongitudeRad);
        double y2 = Math.cos(toLatitudeRad) * Math.sin(toLongitudeRad);
        double z2 = Math.sin(toLatitudeRad);

        double dot = clamp(x1 * x2 + y1 * y2 + z1 * z2, -1.0, 1.0);
        double omega = Math.acos(dot);

        if (omega < 1e-12) {
            double latitudeDeg = fromLatitudeDeg + (toLatitudeDeg - fromLatitudeDeg) * clampedT;
            double longitudeDeg = normalizeLongitudeDeg(fromLongitudeDeg + (toLongitudeDeg - fromLongitudeDeg) * clampedT);
            return new GeoPoint(latitudeDeg, longitudeDeg);
        }

        double sinOmega = Math.sin(omega);
        double a = Math.sin((1.0 - clampedT) * omega) / sinOmega;
        double b = Math.sin(clampedT * omega) / sinOmega;

        double x = a * x1 + b * x2;
        double y = a * y1 + b * y2;
        double z = a * z1 + b * z2;

        double latitudeRad = Math.atan2(z, Math.sqrt(x * x + y * y));
        double longitudeRad = Math.atan2(y, x);

        return new GeoPoint(
                Math.toDegrees(latitudeRad),
                normalizeLongitudeDeg(Math.toDegrees(longitudeRad))
        );
    }

    /**
     * Resolves an adaptive terrain/profile sample count from the full path distance.
     *
     * <p>Current heuristic:
     * <ul>
     *     <li>target spacing about 0.5 km</li>
     *     <li>minimum 121 samples</li>
     *     <li>maximum 1201 samples</li>
     * </ul>
     *
     * @param totalDistanceKm full path distance in kilometers
     * @return clamped sample count
     */
    public static int resolveAdaptiveSampleCount(double totalDistanceKm) {
        if (!Double.isFinite(totalDistanceKm) || totalDistanceKm <= 0.0) {
            return MIN_PROFILE_SAMPLE_COUNT;
        }

        int computedSampleCount = (int) Math.ceil(totalDistanceKm / DEFAULT_TARGET_SAMPLE_STEP_KM) + 1;

        return clampInt(computedSampleCount, MIN_PROFILE_SAMPLE_COUNT, MAX_PROFILE_SAMPLE_COUNT);
    }

    /**
     * Returns the Earth bulge above the straight endpoint chord at one point
     * along the path.
     *
     * <p>Distances are given along the path in kilometers.</p>
     *
     * <p>Approximation:
     * bulge = d1 * d2 / (2 * R_eff)</p>
     */
    public static double calculateEarthBulgeMeters(double distanceFromStartKm,
                                                   double totalDistanceKm) {
        return calculateEarthBulgeMeters(
                distanceFromStartKm,
                totalDistanceKm,
                DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR
        );
    }

    public static double calculateEarthBulgeMeters(double distanceFromStartKm,
                                                   double totalDistanceKm,
                                                   double effectiveEarthRadiusFactor) {

        if (!Double.isFinite(distanceFromStartKm)
                || !Double.isFinite(totalDistanceKm)
                || !Double.isFinite(effectiveEarthRadiusFactor)
                || totalDistanceKm <= 0.0
                || effectiveEarthRadiusFactor <= 0.0) {
            return 0.0;
        }

        double clampedDistanceFromStartKm = Math.max(0.0, Math.min(distanceFromStartKm, totalDistanceKm));
        double distanceToTargetKm = totalDistanceKm - clampedDistanceFromStartKm;

        double distanceFromStartMeters = clampedDistanceFromStartKm * 1000.0;
        double distanceToTargetMeters = distanceToTargetKm * 1000.0;
        double effectiveEarthRadiusMeters = EARTH_RADIUS_METERS * effectiveEarthRadiusFactor;

        return (distanceFromStartMeters * distanceToTargetMeters) / (2.0 * effectiveEarthRadiusMeters);
    }

    /**
     * Returns terrain height plus Earth curvature using the default effective Earth
     * radius factor.
     *
     * @param point terrain/profile point
     * @param totalDistanceKm full path distance in kilometers
     * @return curvature-adjusted terrain height in meters
     */
    public static double calculateCurvatureAdjustedElevationMeters(PathProfilePoint point,
                                                                   double totalDistanceKm) {
        return calculateCurvatureAdjustedElevationMeters(
                point,
                totalDistanceKm,
                DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR
        );
    }

    /**
     * Returns terrain height plus Earth curvature using the given effective Earth
     * radius factor.
     *
     * <p>k = 1.0 means optical/geometric Earth curvature. k = 4/3 is the common
     * standard radio-refraction approximation for VHF/UHF path previews.</p>
     *
     * @param point terrain/profile point
     * @param totalDistanceKm full path distance in kilometers
     * @param effectiveEarthRadiusFactor k-factor for effective Earth radius
     * @return curvature-adjusted terrain height in meters
     */
    public static double calculateCurvatureAdjustedElevationMeters(PathProfilePoint point,
                                                                   double totalDistanceKm,
                                                                   double effectiveEarthRadiusFactor) {
        if (point == null || !Double.isFinite(point.elevationMeters())) {
            return Double.NaN;
        }

        return point.elevationMeters()
                + calculateEarthBulgeMeters(
                point.distanceKm(),
                totalDistanceKm,
                sanitizeEffectiveEarthRadiusFactor(effectiveEarthRadiusFactor)
        );
    }

    /**
     * Calculates the simple radio horizon distance from antenna height above ground.
     *
     * <p>The result is based on the effective Earth radius model:
     * d = sqrt(2 * R_eff * h)</p>
     *
     * <p>This is a local tangent-horizon approximation. It is useful as an operator
     * hint, but it is not a complete propagation prediction.</p>
     *
     * @param antennaHeightMeters antenna height above local ground in meters
     * @param effectiveEarthRadiusFactor k-factor for effective Earth radius
     * @return radio horizon distance in kilometers
     */
    public static double calculateRadioHorizonDistanceKm(double antennaHeightMeters,
                                                         double effectiveEarthRadiusFactor) {
        if (!Double.isFinite(antennaHeightMeters) || antennaHeightMeters < 0.0) {
            return Double.NaN;
        }

        double sanitizedFactor = sanitizeEffectiveEarthRadiusFactor(effectiveEarthRadiusFactor);
        double effectiveEarthRadiusMeters = EARTH_RADIUS_METERS * sanitizedFactor;

        return Math.sqrt(2.0 * effectiveEarthRadiusMeters * antennaHeightMeters) / 1000.0;
    }

    /**
     * Calculates the apparent elevation angle from an observer height to a target
     * height over a given distance.
     *
     * @param observerHeightMeters observer height in the already curvature-adjusted profile space
     * @param targetHeightMeters target height in the same profile space
     * @param distanceKm distance between observer and target in kilometers
     * @return elevation angle in degrees
     */
    public static double calculateElevationAngleDeg(double observerHeightMeters,
                                                    double targetHeightMeters,
                                                    double distanceKm) {
        if (!Double.isFinite(observerHeightMeters)
                || !Double.isFinite(targetHeightMeters)
                || !Double.isFinite(distanceKm)
                || distanceKm <= 0.0) {
            return Double.NaN;
        }

        double distanceMeters = distanceKm * 1000.0;
        return Math.toDegrees(Math.atan2(targetHeightMeters - observerHeightMeters, distanceMeters));
    }


    /**
     * Sanitizes the effective Earth radius factor.
     *
     * <p>Values outside a practical range are folded back to the standard default
     * so broken preferences or malformed future XML values cannot destabilize path
     * analysis.</p>
     *
     * @param effectiveEarthRadiusFactor raw k-factor
     * @return usable k-factor
     */
    public static double sanitizeEffectiveEarthRadiusFactor(double effectiveEarthRadiusFactor) {
        if (!Double.isFinite(effectiveEarthRadiusFactor)
                || effectiveEarthRadiusFactor < 0.5
                || effectiveEarthRadiusFactor > 10.0) {
            return DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR;
        }

        return effectiveEarthRadiusFactor;
    }

    /**
     * Converts a frequency in MHz to wavelength in meters.
     *
     * @param frequencyMHz signal frequency in MHz
     * @return wavelength in meters
     */
    public static double calculateWavelengthMeters(double frequencyMHz) {
        if (!Double.isFinite(frequencyMHz) || frequencyMHz <= 0.0) {
            return Double.NaN;
        }
        return SPEED_OF_LIGHT_METERS_PER_SECOND / (frequencyMHz * 1_000_000.0);
    }

    /**
     * First Fresnel zone radius at one point along the path.
     *
     * @param distanceFromStartKm distance from TX in kilometers
     * @param totalDistanceKm total path distance in kilometers
     * @param frequencyMHz signal frequency in MHz
     * @return first Fresnel radius in meters
     */
    public static double calculateFirstFresnelRadiusMeters(double distanceFromStartKm,
                                                           double totalDistanceKm,
                                                           double frequencyMHz) {

        if (!Double.isFinite(distanceFromStartKm)
                || !Double.isFinite(totalDistanceKm)
                || !Double.isFinite(frequencyMHz)
                || totalDistanceKm <= 0.0
                || frequencyMHz <= 0.0) {
            return Double.NaN;
        }

        double clampedDistanceFromStartKm = Math.max(0.0, Math.min(distanceFromStartKm, totalDistanceKm));
        double distanceToTargetKm = totalDistanceKm - clampedDistanceFromStartKm;

        double d1Meters = clampedDistanceFromStartKm * 1000.0;
        double d2Meters = distanceToTargetKm * 1000.0;

        if (d1Meters <= 0.0 || d2Meters <= 0.0) {
            return 0.0;
        }

        double wavelengthMeters = calculateWavelengthMeters(frequencyMHz);
        if (!Double.isFinite(wavelengthMeters) || wavelengthMeters <= 0.0) {
            return Double.NaN;
        }

        return Math.sqrt((wavelengthMeters * d1Meters * d2Meters) / (d1Meters + d2Meters));
    }

    /**
     * Recommended minimum clearance, currently 60% of the first Fresnel zone.
     *
     * @param distanceFromStartKm distance from TX in kilometers
     * @param totalDistanceKm total path distance in kilometers
     * @param frequencyMHz signal frequency in MHz
     * @return recommended clearance in meters
     */
    public static double calculateRecommendedFresnelClearanceMeters(double distanceFromStartKm,
                                                                    double totalDistanceKm,
                                                                    double frequencyMHz) {

        double firstFresnelRadiusMeters =
                calculateFirstFresnelRadiusMeters(distanceFromStartKm, totalDistanceKm, frequencyMHz);

        if (!Double.isFinite(firstFresnelRadiusMeters)) {
            return Double.NaN;
        }

        return firstFresnelRadiusMeters * DEFAULT_FRESNEL_CLEARANCE_FACTOR;
    }

    /**
     * Tries to extract a MHz frequency from strings such as:
     * <ul>
     *     <li>"144.300"</li>
     *     <li>"144,300"</li>
     *     <li>"144.300 MHz"</li>
     *     <li>"QRG 432.174 MHz"</li>
     * </ul>
     *
     * @param rawText free-form raw text
     * @return parsed frequency in MHz or NaN
     */
    public static double tryParseFrequencyMHz(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Double.NaN;
        }

        Matcher matcher = FREQUENCY_MHZ_PATTERN.matcher(rawText.trim());
        if (!matcher.find()) {
            return Double.NaN;
        }

        String numericText = matcher.group(1).replace(',', '.');

        try {
            double parsedFrequencyMHz = Double.parseDouble(numericText);
            return parsedFrequencyMHz > 0.0 ? parsedFrequencyMHz : Double.NaN;
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }



    /**
     * Small immutable geographic point used by great-circle interpolation.
     *
     * @param latitudeDeg latitude in degrees
     * @param longitudeDeg longitude in degrees
     */
    public record GeoPoint(double latitudeDeg, double longitudeDeg) {
    }

    private static double normalizeLongitudeDeg(double longitudeDeg) {
        if (!Double.isFinite(longitudeDeg)) {
            return Double.NaN;
        }

        double normalized = longitudeDeg % 360.0;
        if (normalized > 180.0) {
            normalized -= 360.0;
        } else if (normalized <= -180.0) {
            normalized += 360.0;
        }
        return normalized;
    }

    private static double normalizeBearingDeg(double bearingDeg) {
        if (!Double.isFinite(bearingDeg)) {
            return Double.NaN;
        }

        double normalized = bearingDeg % 360.0;
        if (normalized < 0.0) {
            normalized += 360.0;
        }
        return normalized;
    }

    private static double clamp(double value, double minValue, double maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private static int clampInt(int value, int minValue, int maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }


    /**
     * Calculates the Fresnel-Kirchhoff diffraction parameter v for a single
     * obstruction.
     *
     * <p>Positive height means the obstruction is above the direct line of sight.
     * This method is useful for a rough single-knife-edge severity estimate.</p>
     *
     * @param heightAboveLosMeters obstruction height above direct LOS in meters
     * @param distanceFromHomeKm distance from home endpoint to obstruction
     * @param distanceFromTargetKm distance from target endpoint to obstruction
     * @param frequencyMHz analysis frequency in MHz
     * @return diffraction parameter v
     */
    public static double calculateKnifeEdgeVParameter(double heightAboveLosMeters,
                                                      double distanceFromHomeKm,
                                                      double distanceFromTargetKm,
                                                      double frequencyMHz) {
        if (!Double.isFinite(heightAboveLosMeters)
                || !Double.isFinite(distanceFromHomeKm)
                || !Double.isFinite(distanceFromTargetKm)
                || distanceFromHomeKm <= 0.0
                || distanceFromTargetKm <= 0.0) {
            return Double.NaN;
        }

        double wavelengthMeters = calculateWavelengthMeters(frequencyMHz);
        if (!Double.isFinite(wavelengthMeters) || wavelengthMeters <= 0.0) {
            return Double.NaN;
        }

        double d1Meters = distanceFromHomeKm * 1000.0;
        double d2Meters = distanceFromTargetKm * 1000.0;

        return heightAboveLosMeters
                * Math.sqrt(2.0 * (d1Meters + d2Meters) / (wavelengthMeters * d1Meters * d2Meters));
    }

    /**
     * Estimates the additional diffraction loss for a single knife edge.
     *
     * <p>This is a rough operator-facing indicator. It must not be interpreted as a
     * complete path-loss model.</p>
     *
     * @param v Fresnel-Kirchhoff diffraction parameter
     * @return estimated additional loss in dB
     */
    public static double calculateSingleKnifeEdgeLossDb(double v) {
        if (!Double.isFinite(v)) {
            return Double.NaN;
        }

        if (v <= -0.78) {
            return 0.0;
        }

        return 6.9 + 20.0 * Math.log10(
                Math.sqrt(Math.pow(v - 0.1, 2.0) + 1.0) + v - 0.1
        );
    }

    /**
     * Converts RF power from watts to dBm.
     *
     * @param watts power in watts
     * @return power in dBm
     */
    public static double wattsToDbm(double watts) {
        if (!Double.isFinite(watts) || watts <= 0.0) {
            return Double.NaN;
        }

        return 10.0 * Math.log10(watts * 1000.0);
    }

    /**
     * Calculates free-space path loss.
     *
     * @param distanceKm path distance in kilometers
     * @param frequencyMHz frequency in MHz
     * @return free-space path loss in dB
     */
    public static double calculateFreeSpacePathLossDb(double distanceKm, double frequencyMHz) {
        if (!Double.isFinite(distanceKm)
                || !Double.isFinite(frequencyMHz)
                || distanceKm <= 0.0
                || frequencyMHz <= 0.0) {
            return Double.NaN;
        }

        return 32.44 + 20.0 * Math.log10(distanceKm) + 20.0 * Math.log10(frequencyMHz);
    }

    /**
     * Estimates per-station feeder loss from a simple VHF baseline and a frequency
     * dependent increase.
     *
     * <p>The result is capped because a linear MHz-based feeder heuristic becomes
     * unrealistic on microwave bands where transverters are often placed near the
     * antenna. Later this should become a per-band user setting.</p>
     *
     * @param frequencyMHz frequency in MHz
     * @param settings link-budget settings
     * @return estimated per-station feeder loss in dB
     */
    public static double estimateFeederLossPerStationDb(double frequencyMHz,
                                                        PathLinkBudgetSettings settings) {
        PathLinkBudgetSettings safeSettings = settings == null
                ? PathLinkBudgetSettings.defaults()
                : settings;

        double baseLossDb = safeSettings.vhfFeederLossPerStationDb();

        if (!Double.isFinite(frequencyMHz) || frequencyMHz <= 0.0) {
            return baseLossDb;
        }

        double additionalLossDb = Math.max(0.0, (frequencyMHz - 144.0) / 200.0)
                * safeSettings.feederLossIncreaseDbPer200MHz();

        double estimatedLossDb = baseLossDb + additionalLossDb;

        return Math.min(estimatedLossDb, safeSettings.maxEstimatedFeederLossPerStationDb());
    }
}