package kst4contest.service.path;

/**
 * Utility methods for Fresnel zone calculations.
 *
 * <p>This helper intentionally contains only pure mathematical functions.
 * It has no dependency on UI code or terrain providers.</p>
 */
public final class FresnelMathUtils {

    /**
     * Speed of light in vacuum in meters per second.
     */
    public static final double SPEED_OF_LIGHT_METERS_PER_SECOND = 299_792_458.0;

    private FresnelMathUtils() {
        // Utility class
    }

    /**
     * Computes the wavelength in meters for the given frequency.
     *
     * @param frequencyHz signal frequency in Hz
     * @return wavelength in meters, or 0 if the input is invalid
     */
    public static double computeWavelengthMeters(final double frequencyHz) {
        if (frequencyHz <= 0.0) {
            return 0.0;
        }
        return SPEED_OF_LIGHT_METERS_PER_SECOND / frequencyHz;
    }

    /**
     * Computes the radius of the first Fresnel zone at a specific point
     * along the path.
     *
     * <p>Formula:
     * r = sqrt(lambda * d1 * d2 / (d1 + d2))</p>
     *
     * @param frequencyHz signal frequency in Hz
     * @param distanceFromTxMeters distance from TX to the current point in meters
     * @param totalPathDistanceMeters full TX-to-RX path length in meters
     * @return Fresnel radius in meters, or 0 at invalid inputs / path ends
     */
    public static double computeFirstFresnelRadiusMeters(
            final double frequencyHz,
            final double distanceFromTxMeters,
            final double totalPathDistanceMeters) {

        if (frequencyHz <= 0.0 || totalPathDistanceMeters <= 0.0) {
            return 0.0;
        }

        final double clampedDistanceFromTxMeters = Math.max(
                0.0,
                Math.min(distanceFromTxMeters, totalPathDistanceMeters)
        );

        final double distanceFromPointToRxMeters = totalPathDistanceMeters - clampedDistanceFromTxMeters;

        if (clampedDistanceFromTxMeters <= 0.0 || distanceFromPointToRxMeters <= 0.0) {
            return 0.0;
        }

        final double wavelengthMeters = computeWavelengthMeters(frequencyHz);

        return Math.sqrt(
                wavelengthMeters
                        * clampedDistanceFromTxMeters
                        * distanceFromPointToRxMeters
                        / totalPathDistanceMeters
        );
    }
}