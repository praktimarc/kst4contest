package kst4contest.view.map;

import kst4contest.locatorUtils.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Grid sampling, selection, fallback and caching of {@link LocatorHighestPointResolver},
 * with an in-memory elevation provider that counts its calls instead of going online.
 */
class LocatorHighestPointResolverTest {

    private static final String SQUARE = "JO50AE";
    private static final int GRID_POINTS = LocatorHighestPointResolver.GRID_SIZE * LocatorHighestPointResolver.GRID_SIZE;
    private static final int CENTRE_INDEX = GRID_POINTS / 2;

    /** Answers from a function of the point index and records every request. */
    private static final class FakeElevationProvider implements TerrainElevationProvider {
        final List<List<PathGeometryUtils.GeoPoint>> calls = new ArrayList<>();
        Function<List<PathGeometryUtils.GeoPoint>, List<Double>> answer;

        FakeElevationProvider(Function<List<PathGeometryUtils.GeoPoint>, List<Double>> answer) {
            this.answer = answer;
        }

        @Override
        public List<Double> loadElevations(List<PathGeometryUtils.GeoPoint> points) {
            calls.add(List.copyOf(points));
            return answer.apply(points);
        }
    }

    private static List<Double> flat(int size, double elevation) {
        return new ArrayList<>(Collections.nCopies(size, elevation));
    }

    private static List<Double> flatWithPeak(int size, int peakIndex, double peak) {
        List<Double> elevations = flat(size, 100.0);
        elevations.set(peakIndex, peak);
        return elevations;
    }

    @Test
    void theGridHasTwentyFiveCellCentresInsideTheSquareWithTheCentreInTheMiddle() {
        List<PathGeometryUtils.GeoPoint> grid = LocatorHighestPointResolver.buildSampleGrid(SQUARE);

        assertEquals(GRID_POINTS, grid.size());
        for (PathGeometryUtils.GeoPoint point : grid) {
            assertEquals(SQUARE, Location.toMaidenhead(point.latitudeDeg(), point.longitudeDeg())
                    .toUpperCase(Locale.ROOT), "sample " + point + " left the square");
        }

        PathGeometryUtils.GeoPoint centre = grid.get(CENTRE_INDEX);
        assertEquals(Location.extractLat(SQUARE).toDegrees(), centre.latitudeDeg(), 1e-9);
        assertEquals(Location.extractLon(SQUARE).toDegrees(), centre.longitudeDeg(), 1e-9);
    }

    @Test
    void anInvalidLocatorHasNoGridAndAsksNothing() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> flat(points.size(), 0.0));
        LocatorHighestPointResolver resolver = new LocatorHighestPointResolver(provider);

        assertEquals(List.of(), LocatorHighestPointResolver.buildSampleGrid("JO50"));
        assertNull(resolver.resolve("JO50"));
        assertNull(resolver.resolve("ZZ99ZZ"));
        assertNull(resolver.resolve(null));
        assertEquals(0, provider.calls.size());
    }

    @Test
    void theHighestSampleIsChosenWithItsPositionAndElevation() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> flatWithPeak(points.size(), 7, 512.0));
        LocatorHighestPointResolver resolver = new LocatorHighestPointResolver(provider);

        LocatorHighestPointResolver.HighestPoint highest = resolver.resolve("jo50ae");

        PathGeometryUtils.GeoPoint expected = LocatorHighestPointResolver.buildSampleGrid(SQUARE).get(7);
        assertNotNull(highest);
        assertEquals(SQUARE, highest.locator6());
        assertEquals(expected.latitudeDeg(), highest.latitudeDeg(), 1e-12);
        assertEquals(expected.longitudeDeg(), highest.longitudeDeg(), 1e-12);
        assertEquals(512.0, highest.elevationMeters(), 1e-12);
    }

    @Test
    void allSamplesGoOutInOneBatch() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> flat(points.size(), 0.0));

        new LocatorHighestPointResolver(provider).resolve(SQUARE);

        assertEquals(1, provider.calls.size());
        assertEquals(GRID_POINTS, provider.calls.get(0).size());
    }

    @Test
    void aFlatSquareResolvesToItsCentre() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> flat(points.size(), 250.0));

        LocatorHighestPointResolver.HighestPoint highest = new LocatorHighestPointResolver(provider).resolve(SQUARE);

        assertNotNull(highest);
        assertEquals(Location.extractLat(SQUARE).toDegrees(), highest.latitudeDeg(), 1e-9);
        assertEquals(Location.extractLon(SQUARE).toDegrees(), highest.longitudeDeg(), 1e-9);
    }

    @Test
    void samplesWithoutDataAreSkipped() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> {
            List<Double> elevations = flatWithPeak(points.size(), 3, 300.0);
            elevations.set(20, Double.NaN);
            elevations.set(21, null);
            return elevations;
        });

        LocatorHighestPointResolver.HighestPoint highest = new LocatorHighestPointResolver(provider).resolve(SQUARE);

        assertNotNull(highest);
        assertEquals(300.0, highest.elevationMeters(), 1e-12);
    }

    @Test
    void aSquareWithoutAnyDataFails() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> flat(points.size(), Double.NaN));

        assertNull(new LocatorHighestPointResolver(provider).resolve(SQUARE));
    }

    @Test
    void aResolvedSquareIsCachedAndPeekable() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> flatWithPeak(points.size(), 0, 400.0));
        LocatorHighestPointResolver resolver = new LocatorHighestPointResolver(provider);

        assertNull(resolver.peekCached(SQUARE), "nothing cached before the first lookup");

        LocatorHighestPointResolver.HighestPoint first = resolver.resolve(SQUARE);
        LocatorHighestPointResolver.HighestPoint second = resolver.resolve(SQUARE);

        assertSame(first, second);
        assertSame(first, resolver.peekCached("jo50ae"));
        assertEquals(1, provider.calls.size(), "the square was asked for twice");
    }

    @Test
    void peekingNeverAsksTheProvider() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> flat(points.size(), 0.0));

        assertNull(new LocatorHighestPointResolver(provider).peekCached(SQUARE));
        assertEquals(0, provider.calls.size());
    }

    /*
     * A failed lookup falls back to the centre at the caller, and the square is not asked
     * again until the retry delay has passed, so an unreachable service is not hammered.
     */
    @Test
    void aFailedLookupIsNotRetriedBeforeTheDelay() {
        AtomicLong now = new AtomicLong(1_000_000L);
        FakeElevationProvider provider = new FakeElevationProvider(points -> List.of());
        LocatorHighestPointResolver resolver = new LocatorHighestPointResolver(provider, now::get);

        assertNull(resolver.resolve(SQUARE));
        assertNull(resolver.peekCached(SQUARE), "a failure is not a cached point");

        now.addAndGet(LocatorHighestPointResolver.FAILURE_RETRY_DELAY_MS - 1);
        assertNull(resolver.resolve(SQUARE));
        assertEquals(1, provider.calls.size(), "retried inside the delay");

        provider.answer = points -> flatWithPeak(points.size(), 24, 700.0);
        now.addAndGet(1);
        LocatorHighestPointResolver.HighestPoint retried = resolver.resolve(SQUARE);

        assertEquals(2, provider.calls.size());
        assertNotNull(retried);
        assertEquals(700.0, retried.elevationMeters(), 1e-12);
    }

    @Test
    void expiredFailuresAreDroppedWhenTheNextOneIsRecorded() {
        AtomicLong now = new AtomicLong(1_000_000L);
        FakeElevationProvider provider = new FakeElevationProvider(points -> List.of());
        LocatorHighestPointResolver resolver = new LocatorHighestPointResolver(provider, now::get);

        resolver.resolve("JO50AE");
        resolver.resolve("JO50AF");
        assertEquals(2, resolver.failedSquareCount());

        now.addAndGet(LocatorHighestPointResolver.FAILURE_RETRY_DELAY_MS);
        resolver.resolve("JO50AG");

        assertEquals(1, resolver.failedSquareCount(), "expired failures were kept");
    }

    @Test
    void theFailureListIsBounded() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> List.of());
        LocatorHighestPointResolver resolver = new LocatorHighestPointResolver(provider, () -> 1_000_000L);

        int squares = 0;
        outer:
        for (char field = 'A'; field <= 'R'; field++) {
            for (int digits = 0; digits < 100; digits++) {
                for (char sub = 'A'; sub <= 'X'; sub++) {
                    String locator = String.format(Locale.ROOT, "J%c%02dA%c", field, digits, sub);
                    resolver.resolve(locator);
                    if (++squares > LocatorHighestPointResolver.MAX_CACHED_SQUARES + 10) {
                        break outer;
                    }
                }
            }
        }

        assertEquals(LocatorHighestPointResolver.MAX_CACHED_SQUARES, resolver.failedSquareCount());
    }

    @Test
    void aThrowingProviderCountsAsAFailure() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> {
            throw new IllegalStateException("offline");
        });

        assertNull(new LocatorHighestPointResolver(provider).resolve(SQUARE));
    }

    @Test
    void anAnswerOfTheWrongLengthCountsAsAFailure() {
        FakeElevationProvider provider = new FakeElevationProvider(points -> flat(points.size() - 1, 900.0));

        assertNull(new LocatorHighestPointResolver(provider).resolve(SQUARE));
    }
}
