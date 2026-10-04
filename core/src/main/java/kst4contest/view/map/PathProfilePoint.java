package kst4contest.view.map;

import java.util.Locale;

/**
 * Immutable terrain sample plus optional derived radio-path geometry.
 *
 * <p>The first four properties are the raw terrain profile payload:
 * <ul>
 *     <li>distance along the path in kilometers</li>
 *     <li>sample latitude in degrees</li>
 *     <li>sample longitude in degrees</li>
 *     <li>terrain elevation in meters above mean sea level</li>
 * </ul>
 *
 * <p>The remaining properties are optional derived values filled by
 * {@link GeometryOnlyPathAnalysisService}. They are all expressed in the
 * same chart/analysis space as the curvature-adjusted terrain profile:
 * <ul>
 *     <li>curvature-adjusted terrain elevation</li>
 *     <li>direct line-of-sight height</li>
 *     <li>upper / lower first Fresnel hull</li>
 *     <li>LOS and Fresnel clearances</li>
 *     <li>worst-intrusion support values</li>
 * </ul>
 */
public final class PathProfilePoint {

    private final int sampleIndex;
    private final double distanceKm;
    private final double latitudeDeg;
    private final double longitudeDeg;
    private final double elevationMeters;

    private final double curvatureAdjustedElevationMeters;
    private final double lineOfSightHeightMeters;
    private final double fresnelUpperHeightMeters;
    private final double fresnelLowerHeightMeters;
    private final double lineOfSightClearanceMeters;
    private final double lowerFresnelClearanceMeters;
    private final double fresnelIntrusionMeters;

    /**
     * Creates a raw terrain profile sample without derived LOS/Fresnel values.
     *
     * <p>This constructor is used by terrain providers and cache deserialization.
     * Derived geometry values remain unavailable until the path analysis service
     * enriches the profile.</p>
     *
     * @param distanceKm distance from the path origin in kilometers
     * @param latitudeDeg sample latitude in degrees
     * @param longitudeDeg sample longitude in degrees
     * @param elevationMeters terrain elevation in meters above mean sea level
     */
    public PathProfilePoint(double distanceKm,
                            double latitudeDeg,
                            double longitudeDeg,
                            double elevationMeters) {
        this(
                -1,
                distanceKm,
                latitudeDeg,
                longitudeDeg,
                elevationMeters,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN
        );
    }

    /**
     * Creates a terrain profile sample enriched with derived radio-path geometry.
     *
     * @param sampleIndex zero-based sample index in the analyzed path
     * @param distanceKm distance from the path origin in kilometers
     * @param latitudeDeg sample latitude in degrees
     * @param longitudeDeg sample longitude in degrees
     * @param elevationMeters raw terrain elevation in meters above mean sea level
     * @param curvatureAdjustedElevationMeters terrain elevation plus Earth bulge in meters
     * @param lineOfSightHeightMeters direct path height in the same chart space
     * @param fresnelUpperHeightMeters upper first Fresnel hull in the same chart space
     * @param fresnelLowerHeightMeters lower first Fresnel hull in the same chart space
     * @param lineOfSightClearanceMeters direct LOS clearance against curvature-adjusted terrain
     * @param lowerFresnelClearanceMeters lower Fresnel clearance against curvature-adjusted terrain
     * @param fresnelIntrusionMeters terrain intrusion into the lower Fresnel hull
     */
    public PathProfilePoint(int sampleIndex,
                            double distanceKm,
                            double latitudeDeg,
                            double longitudeDeg,
                            double elevationMeters,
                            double curvatureAdjustedElevationMeters,
                            double lineOfSightHeightMeters,
                            double fresnelUpperHeightMeters,
                            double fresnelLowerHeightMeters,
                            double lineOfSightClearanceMeters,
                            double lowerFresnelClearanceMeters,
                            double fresnelIntrusionMeters) {

        this.sampleIndex = sampleIndex;
        this.distanceKm = distanceKm;
        this.latitudeDeg = latitudeDeg;
        this.longitudeDeg = longitudeDeg;
        this.elevationMeters = elevationMeters;
        this.curvatureAdjustedElevationMeters = curvatureAdjustedElevationMeters;
        this.lineOfSightHeightMeters = lineOfSightHeightMeters;
        this.fresnelUpperHeightMeters = fresnelUpperHeightMeters;
        this.fresnelLowerHeightMeters = fresnelLowerHeightMeters;
        this.lineOfSightClearanceMeters = lineOfSightClearanceMeters;
        this.lowerFresnelClearanceMeters = lowerFresnelClearanceMeters;
        this.fresnelIntrusionMeters = fresnelIntrusionMeters;
    }

    public int sampleIndex() {
        return sampleIndex;
    }

    public double distanceKm() {
        return distanceKm;
    }

    public double latitudeDeg() {
        return latitudeDeg;
    }

    public double longitudeDeg() {
        return longitudeDeg;
    }

    public double elevationMeters() {
        return elevationMeters;
    }

    public double curvatureAdjustedElevationMeters() {
        return curvatureAdjustedElevationMeters;
    }

    public double lineOfSightHeightMeters() {
        return lineOfSightHeightMeters;
    }

    public double fresnelUpperHeightMeters() {
        return fresnelUpperHeightMeters;
    }

    public double fresnelLowerHeightMeters() {
        return fresnelLowerHeightMeters;
    }

    public double lineOfSightClearanceMeters() {
        return lineOfSightClearanceMeters;
    }

    public double lowerFresnelClearanceMeters() {
        return lowerFresnelClearanceMeters;
    }

    public double fresnelIntrusionMeters() {
        return fresnelIntrusionMeters;
    }

    /**
     * Returns true if enriched LOS/Fresnel geometry is available.
     *
     * @return true if this sample was enriched by the path analysis service
     */
    public boolean hasDerivedGeometry() {
        return Double.isFinite(curvatureAdjustedElevationMeters)
                || Double.isFinite(lineOfSightHeightMeters)
                || Double.isFinite(fresnelUpperHeightMeters)
                || Double.isFinite(fresnelLowerHeightMeters);
    }

    /**
     * Returns true if the terrain blocks the direct line of sight at this sample.
     *
     * @return true if the LOS clearance is negative
     */
    public boolean isLineOfSightBlocked() {
        return Double.isFinite(lineOfSightClearanceMeters) && lineOfSightClearanceMeters < 0.0;
    }

    /**
     * Returns true if the terrain intrudes into the lower Fresnel hull.
     *
     * @return true if Fresnel intrusion is positive
     */
    public boolean hasFresnelIntrusion() {
        return Double.isFinite(fresnelIntrusionMeters) && fresnelIntrusionMeters > 0.0;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "PathProfilePoint{index=%d, distanceKm=%.3f, lat=%.6f, lon=%.6f, elevation=%.2f, curvature=%.2f, los=%.2f, fresnelLower=%.2f, intrusion=%.2f}",
                sampleIndex,
                distanceKm,
                latitudeDeg,
                longitudeDeg,
                elevationMeters,
                curvatureAdjustedElevationMeters,
                lineOfSightHeightMeters,
                fresnelLowerHeightMeters,
                fresnelIntrusionMeters
        );
    }
}