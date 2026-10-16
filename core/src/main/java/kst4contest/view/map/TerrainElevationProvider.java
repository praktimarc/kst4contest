package kst4contest.view.map;

import java.util.List;

/**
 * Abstraction for looking up terrain elevations at arbitrary points.
 *
 * <p>{@link TerrainProfileProvider} only samples along a great circle between two
 * endpoints. Some questions need a set of points that is not a path, such as a grid
 * over a locator square, and a provider that can answer many points in one request
 * should be asked for them together instead of point by point.</p>
 */
public interface TerrainElevationProvider {

    /**
     * Loads the terrain elevation for every point.
     *
     * <p>Implementations may block on network or disk access, so this must not be called
     * on the user-interface thread.</p>
     *
     * @param points points to look up
     * @return elevations in metres above mean sea level, in the order of {@code points};
     *         {@code NaN} for a point without data. An empty list means the lookup failed
     *         as a whole.
     */
    List<Double> loadElevations(List<PathGeometryUtils.GeoPoint> points);
}
