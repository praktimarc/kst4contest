package kst4contest.view.map;

import kst4contest.locatorUtils.Location;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Finds the highest terrain point inside a six-character Maidenhead locator square.
 *
 * <p>A locator only says which square a station is in, not where in it. VHF/UHF contest
 * stations tend to operate from the high ground of their square rather than from its
 * geometric centre, so the path analysis can use the highest sampled point of the square
 * as the remote endpoint instead.</p>
 *
 * <p><b>Grid.</b> A six-character square spans 5' of longitude and 2.5' of latitude,
 * roughly 5.8 km x 4.6 km at 50 degrees north. It is sampled with a
 * {@value #GRID_SIZE} x {@value #GRID_SIZE} grid of cell centres, so the samples are
 * about 1.2 km x 0.9 km apart there and never sit on the border shared with the
 * neighbouring square. An odd grid size puts one sample exactly on the square centre,
 * which means the chosen point is never lower than the centre the analysis used before.
 * The 25 points fit into a single request of the online elevation service (limit 100
 * coordinates). The grid is a coarse search, not a summit finder: a broad hill usually
 * gets a sample reasonably close to its top, but a narrow ridge or peak between two
 * samples can be missed, and the chosen point is then only the highest of the samples.
 * A denser grid would not cost more requests, but would only refine a position that the
 * locator itself does not pin down anyway.</p>
 *
 * <p><b>Ties</b> are broken towards the square centre, so a flat square resolves to its
 * centre and the result is deterministic.</p>
 *
 * <p><b>Caching.</b> A resolved square is pure geometry and is kept in memory for the
 * running session (least recently used, at most {@value #MAX_CACHED_SQUARES} squares).
 * A failed lookup is remembered as well and not retried for
 * {@link #FAILURE_RETRY_DELAY_MS} milliseconds, so an unreachable service is not asked
 * again for every station of the same square. The caller falls back to the square centre
 * in the meantime. Expired failures are dropped whenever a failure is recorded, and the
 * failure list is bounded like the result cache.</p>
 *
 * <p>{@link #resolve(String)} may block on the network and must run on a background
 * thread; {@link #peekCached(String)} never blocks on it and is safe anywhere.</p>
 */
public final class LocatorHighestPointResolver {

    /** Samples per axis; see the class comment for why five. */
    public static final int GRID_SIZE = 5;

    static final int MAX_CACHED_SQUARES = 4096;

    /** Same delay the reachability service uses before it retries a failed analysis. */
    static final long FAILURE_RETRY_DELAY_MS = 5L * 60L * 1000L;

    private static final double SQUARE_HEIGHT_DEG = 2.5 / 60.0;
    private static final double SQUARE_WIDTH_DEG = 5.0 / 60.0;

    private static final Pattern LOCATOR6 = Pattern.compile("[A-R]{2}[0-9]{2}[A-X]{2}");

    private final TerrainElevationProvider elevationProvider;
    private final LongSupplier clock;

    private final Map<String, HighestPoint> resolvedSquares =
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, HighestPoint> eldest) {
                    return size() > MAX_CACHED_SQUARES;
                }
            };

    private final Map<String, Long> retryAfterEpochMsBySquare =
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                    return size() > MAX_CACHED_SQUARES;
                }
            };

    public LocatorHighestPointResolver(TerrainElevationProvider elevationProvider) {
        this(elevationProvider, System::currentTimeMillis);
    }

    LocatorHighestPointResolver(TerrainElevationProvider elevationProvider, LongSupplier clock) {
        this.elevationProvider = Objects.requireNonNull(elevationProvider, "elevationProvider");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * The highest sampled point of one locator square.
     *
     * @param locator6 the square, upper case
     * @param latitudeDeg latitude of the chosen sample
     * @param longitudeDeg longitude of the chosen sample
     * @param elevationMeters terrain elevation there, metres above mean sea level
     */
    public record HighestPoint(String locator6, double latitudeDeg, double longitudeDeg, double elevationMeters) {
    }

    /**
     * Returns the cached highest point of a square without touching the elevation service.
     *
     * @param locator6 six-character locator
     * @return the cached point, or {@code null} when the square has not been resolved yet,
     *         its lookup failed, or the locator is not a valid six-character locator
     */
    public HighestPoint peekCached(String locator6) {
        String square = normalize(locator6);
        if (square == null) {
            return null;
        }
        synchronized (resolvedSquares) {
            return resolvedSquares.get(square);
        }
    }

    /**
     * Returns the highest sampled point of a square, asking the elevation service once per
     * square and session. Blocks while the service answers.
     *
     * @param locator6 six-character locator
     * @return the highest point, or {@code null} when the locator is invalid or the lookup
     *         failed (now or within the retry delay); the caller then uses the square centre
     */
    public HighestPoint resolve(String locator6) {
        String square = normalize(locator6);
        if (square == null) {
            return null;
        }

        synchronized (resolvedSquares) {
            HighestPoint cached = resolvedSquares.get(square);
            if (cached != null) {
                return cached;
            }
            Long retryAfter = retryAfterEpochMsBySquare.get(square);
            if (retryAfter != null) {
                if (clock.getAsLong() < retryAfter) {
                    return null;
                }
                retryAfterEpochMsBySquare.remove(square);
            }
        }

        // The service is asked outside the lock so that peekCached never waits for it.
        HighestPoint resolved = lookUp(square);

        synchronized (resolvedSquares) {
            if (resolved == null) {
                long now = clock.getAsLong();
                retryAfterEpochMsBySquare.values().removeIf(retryAfter -> retryAfter <= now);
                retryAfterEpochMsBySquare.put(square, now + FAILURE_RETRY_DELAY_MS);
            } else {
                retryAfterEpochMsBySquare.remove(square);
                resolvedSquares.put(square, resolved);
            }
        }
        return resolved;
    }

    /** Number of squares currently held back after a failure; for tests. */
    int failedSquareCount() {
        synchronized (resolvedSquares) {
            return retryAfterEpochMsBySquare.size();
        }
    }

    /**
     * The sample grid of one square, row by row from south-west to north-east.
     *
     * @param locator6 six-character locator
     * @return {@value #GRID_SIZE} x {@value #GRID_SIZE} cell centres, or an empty list for
     *         an invalid locator
     */
    static List<PathGeometryUtils.GeoPoint> buildSampleGrid(String locator6) {
        String square = normalize(locator6);
        if (square == null) {
            return List.of();
        }

        double southLatitudeDeg = Location.extractLat(square).toDegrees() - SQUARE_HEIGHT_DEG / 2.0;
        double westLongitudeDeg = Location.extractLon(square).toDegrees() - SQUARE_WIDTH_DEG / 2.0;

        List<PathGeometryUtils.GeoPoint> grid = new ArrayList<>(GRID_SIZE * GRID_SIZE);
        for (int row = 0; row < GRID_SIZE; row++) {
            double latitudeDeg = southLatitudeDeg + SQUARE_HEIGHT_DEG * (row + 0.5) / GRID_SIZE;
            for (int column = 0; column < GRID_SIZE; column++) {
                double longitudeDeg = westLongitudeDeg + SQUARE_WIDTH_DEG * (column + 0.5) / GRID_SIZE;
                grid.add(new PathGeometryUtils.GeoPoint(latitudeDeg, longitudeDeg));
            }
        }
        return grid;
    }

    private HighestPoint lookUp(String square) {
        List<PathGeometryUtils.GeoPoint> grid = buildSampleGrid(square);

        final List<Double> elevations;
        try {
            elevations = elevationProvider.loadElevations(grid);
        } catch (RuntimeException exception) {
            return null;
        }

        if (elevations == null || elevations.size() != grid.size()) {
            return null;
        }

        int centreIndex = (GRID_SIZE / 2) * GRID_SIZE + GRID_SIZE / 2;
        PathGeometryUtils.GeoPoint centre = grid.get(centreIndex);

        int bestIndex = -1;
        double bestElevation = Double.NEGATIVE_INFINITY;
        double bestDistanceToCentre = Double.POSITIVE_INFINITY;

        for (int i = 0; i < grid.size(); i++) {
            Double elevation = elevations.get(i);
            if (elevation == null || !Double.isFinite(elevation)) {
                continue;
            }
            double distanceToCentre = squaredDistanceDeg(grid.get(i), centre);
            if (elevation > bestElevation
                    || (elevation == bestElevation && distanceToCentre < bestDistanceToCentre)) {
                bestIndex = i;
                bestElevation = elevation;
                bestDistanceToCentre = distanceToCentre;
            }
        }

        if (bestIndex < 0) {
            return null;
        }

        PathGeometryUtils.GeoPoint best = grid.get(bestIndex);
        return new HighestPoint(square, best.latitudeDeg(), best.longitudeDeg(), bestElevation);
    }

    private static double squaredDistanceDeg(PathGeometryUtils.GeoPoint a, PathGeometryUtils.GeoPoint b) {
        // Only used to order samples of one small square, so a flat approximation with the
        // longitude scaled by the square's own aspect is enough.
        double latitudeDelta = (a.latitudeDeg() - b.latitudeDeg()) / SQUARE_HEIGHT_DEG;
        double longitudeDelta = (a.longitudeDeg() - b.longitudeDeg()) / SQUARE_WIDTH_DEG;
        return latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta;
    }

    private static String normalize(String locator6) {
        if (locator6 == null) {
            return null;
        }
        String square = locator6.trim().toUpperCase(Locale.ROOT);
        return LOCATOR6.matcher(square).matches() ? square : null;
    }
}
