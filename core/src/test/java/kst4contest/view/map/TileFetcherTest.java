package kst4contest.view.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Getting map tiles, and admitting when it cannot.
 *
 * Three things were wrong with the first version. It asked the {@code a}/{@code b}/{@code c}
 * subdomains, which OpenStreetMap has retired. It kept tiles in memory only, so every
 * restart re-downloaded the same view from a service that asks callers not to. And a
 * failed request returned null and said nothing, which is why a map with no network
 * looked like a map with no stations.
 */
class TileFetcherTest {

    /** A source that records what it was asked for and answers from a script. */
    private static final class RecordingSource implements TileFetcher.TileSource {

        private final List<String> requestedUrls = new CopyOnWriteArrayList<>();
        private final byte[] answer;
        private final boolean fail;

        RecordingSource(byte[] answer, boolean fail) {
            this.answer = answer;
            this.fail = fail;
        }

        @Override
        public byte[] fetch(String url) throws IOException {
            requestedUrls.add(url);
            if (fail) {
                throw new IOException("no route to host");
            }
            return answer;
        }
    }

    private TileFetcher fetcherWith(Path cacheDirectory, RecordingSource source) {
        return new TileFetcher(cacheDirectory, source);
    }

    @Test
    void the_url_is_the_one_openstreetmap_still_serves() {
        String url = TileFetcher.tileUrl(6, 33, 21);

        assertEquals("https://tile.openstreetmap.org/6/33/21.png", url);
        assertFalse(url.contains("://a.") || url.contains("://b.") || url.contains("://c."),
                "the lettered subdomains are retired");
    }

    @Test
    void a_tile_is_fetched_once_and_then_answered_from_memory(@TempDir Path cache) throws Exception {
        RecordingSource source = new RecordingSource(new byte[]{1, 2, 3}, false);
        TileFetcher fetcher = fetcherWith(cache, source);

        assertArrayEquals(new byte[]{1, 2, 3}, fetcher.fetchTileAsync(6, 33, 21).get());
        assertArrayEquals(new byte[]{1, 2, 3}, fetcher.fetchTileAsync(6, 33, 21).get());

        assertEquals(1, source.requestedUrls.size());
    }

    @Test
    void a_tile_survives_a_restart(@TempDir Path cache) throws Exception {
        RecordingSource first = new RecordingSource(new byte[]{9, 9, 9}, false);
        fetcherWith(cache, first).fetchTileAsync(7, 1, 2).get();

        RecordingSource afterRestart = new RecordingSource(new byte[]{0}, false);
        byte[] tile = fetcherWith(cache, afterRestart).fetchTileAsync(7, 1, 2).get();

        assertArrayEquals(new byte[]{9, 9, 9}, tile);
        assertTrue(afterRestart.requestedUrls.isEmpty(), "the disk cache should have answered");
    }

    @Test
    void a_cached_tile_is_written_where_its_coordinates_say(@TempDir Path cache) throws Exception {
        fetcherWith(cache, new RecordingSource(new byte[]{4}, false)).fetchTileAsync(8, 5, 6).get();

        assertTrue(Files.exists(cache.resolve("8").resolve("5").resolve("6.png")));
    }

    @Test
    void a_failed_request_is_reported_rather_than_swallowed(@TempDir Path cache) throws Exception {
        TileFetcher fetcher = fetcherWith(cache, new RecordingSource(null, true));

        assertNull(fetcher.fetchTileAsync(6, 33, 21).get());
        assertFalse(fetcher.hasReachedTileSource(), "nothing has arrived, and the map should be able to say so");
        assertTrue(fetcher.failedRequestCount() > 0);
    }

    @Test
    void a_source_that_answers_is_reported_as_reachable(@TempDir Path cache) throws Exception {
        TileFetcher fetcher = fetcherWith(cache, new RecordingSource(new byte[]{1}, false));
        fetcher.fetchTileAsync(6, 33, 21).get();

        assertTrue(fetcher.hasReachedTileSource());
        assertEquals(0, fetcher.failedRequestCount());
    }

    /**
     * A failure must not be cached as an answer, or the tile never arrives at all.
     *
     * Retried after the pause, not on the next request: an immediate retry is what the
     * tile service's usage policy calls abuse, and the map re-requests every visible
     * tile on every change of view.
     */
    @Test
    void a_failure_is_not_remembered_as_an_answer(@TempDir Path cache) throws Exception {
        RecordingSource source = new RecordingSource(null, true);
        TileFetcher fetcher = new TileFetcher(cache, source, Duration.ofMillis(1));

        fetcher.fetchTileAsync(6, 33, 21).get();
        Thread.sleep(20);
        fetcher.fetchTileAsync(6, 33, 21).get();

        assertEquals(2, source.requestedUrls.size());
    }

    @Test
    void a_cache_directory_that_cannot_be_written_does_not_stop_the_map(@TempDir Path cache) throws Exception {
        Path notADirectory = cache.resolve("blocked");
        Files.writeString(notADirectory, "this is a file, not a directory");

        TileFetcher fetcher = fetcherWith(notADirectory, new RecordingSource(new byte[]{7}, false));

        assertArrayEquals(new byte[]{7}, fetcher.fetchTileAsync(6, 1, 1).get());
    }

    /**
     * The banner has to reach the one case it was written for: a restart without a
     * network and a cache that holds some of the view.
     *
     * A latched "we reached the source once" flag cannot do that. The first cached tile
     * sets it, every uncached tile then fails quietly, and the operator is left looking
     * at half a map with no explanation — which is the screenshot this whole step
     * started from.
     */
    @Test
    void a_cache_hit_does_not_silence_the_failures_around_it(@TempDir Path cache) throws Exception {
        fetcherWith(cache, new RecordingSource(new byte[]{1, 2, 3}, false)).fetchTileAsync(6, 1, 1).get();

        TileFetcher offline = fetcherWith(cache, new RecordingSource(null, true));
        offline.fetchTileAsync(6, 1, 1).get();
        offline.fetchTileAsync(6, 2, 2).get();

        assertTrue(offline.isTileSourceUnavailable(),
                "one cached tile must not hide that the rest cannot be had");
    }

    @Test
    void a_tile_arriving_again_clears_the_warning(@TempDir Path cache) throws Exception {
        TileFetcher fetcher = fetcherWith(cache, new RecordingSource(null, true));
        fetcher.fetchTileAsync(6, 1, 1).get();
        assertTrue(fetcher.isTileSourceUnavailable());

        TileFetcher recovered = fetcherWith(cache, new RecordingSource(new byte[]{5}, false));
        recovered.fetchTileAsync(6, 1, 1).get();

        assertFalse(recovered.isTileSourceUnavailable());
    }

    @Test
    void nothing_requested_yet_is_not_a_warning(@TempDir Path cache) {
        assertFalse(fetcherWith(cache, new RecordingSource(new byte[]{1}, false)).isTileSourceUnavailable());
    }

    /**
     * A tile that cannot be decoded has to be removable, or it is cached forever.
     *
     * The process can die mid-write; a truncated PNG then sits in the cache and is
     * served on every later run, so that square of the map is blank for good and there
     * is no way back except deleting the directory by hand.
     */
    @Test
    void a_tile_that_turned_out_to_be_unusable_can_be_thrown_away(@TempDir Path cache) throws Exception {
        TileFetcher fetcher = fetcherWith(cache, new RecordingSource(new byte[]{1, 2, 3}, false));
        fetcher.fetchTileAsync(9, 4, 5).get();
        assertTrue(Files.exists(cache.resolve("9").resolve("4").resolve("5.png")));

        fetcher.discardTile(9, 4, 5);

        assertFalse(Files.exists(cache.resolve("9").resolve("4").resolve("5.png")));

        RecordingSource again = new RecordingSource(new byte[]{7}, false);
        assertArrayEquals(new byte[]{7}, fetcherWith(cache, again).fetchTileAsync(9, 4, 5).get());
    }

    /** A half-written tile must never be visible under its real name. */
    @Test
    void a_tile_is_written_whole_or_not_at_all(@TempDir Path cache) throws Exception {
        fetcherWith(cache, new RecordingSource(new byte[]{1, 2, 3, 4}, false)).fetchTileAsync(9, 4, 5).get();

        try (var paths = Files.walk(cache)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")),
                    "a temporary file was left behind");
        }
        assertArrayEquals(new byte[]{1, 2, 3, 4}, Files.readAllBytes(cache.resolve("9").resolve("4").resolve("5.png")));
    }

    /** Closing has to stop the threads, or every window open leaks a pool. */
    @Test
    void closing_the_fetcher_stops_its_threads(@TempDir Path cache) {
        TileFetcher fetcher = fetcherWith(cache, new RecordingSource(new byte[]{1}, false));
        fetcher.fetchTileAsync(6, 1, 1).join();

        fetcher.close();

        assertTrue(fetcher.isClosed());
    }

    /**
     * The tile service caps a client at two concurrent downloads and treats sustained
     * retrying as abuse. This class quotes that policy in its own javadoc, so it had
     * better keep it: eight connections plus an immediate retry of every failure is how
     * an application gets blocked rather than throttled.
     */
    @Test
    void it_does_not_open_more_connections_than_the_tile_service_allows() {
        assertTrue(TileFetcher.MAX_CONCURRENT_REQUESTS <= 2,
                "OpenStreetMap's usage policy allows at most two concurrent downloads");
    }

    /**
     * A tile that just failed is not asked for again on the very next pan.
     *
     * The map re-requests every visible tile whenever the view changes, so without a
     * pause a server answering 429 would be hammered once per drag.
     */
    @Test
    void a_tile_that_just_failed_is_not_asked_for_again_immediately(@TempDir Path cache) throws Exception {
        RecordingSource source = new RecordingSource(null, true);
        TileFetcher fetcher = fetcherWith(cache, source);

        fetcher.fetchTileAsync(6, 33, 21).get();
        fetcher.fetchTileAsync(6, 33, 21).get();
        fetcher.fetchTileAsync(6, 33, 21).get();

        assertEquals(1, source.requestedUrls.size(),
                "the retries should have been held back rather than sent");
    }

    @Test
    void a_tile_is_asked_for_again_once_the_pause_is_over(@TempDir Path cache) throws Exception {
        RecordingSource source = new RecordingSource(null, true);
        TileFetcher fetcher = new TileFetcher(cache, source, Duration.ofMillis(1));

        fetcher.fetchTileAsync(6, 33, 21).get();
        Thread.sleep(20);
        fetcher.fetchTileAsync(6, 33, 21).get();

        assertEquals(2, source.requestedUrls.size());
    }

    /** A tile that arrives after a failure is not held back by the old failure. */
    @Test
    void a_pause_does_not_outlive_a_successful_fetch(@TempDir Path cache) throws Exception {
        TileFetcher failing = new TileFetcher(cache, new RecordingSource(null, true), Duration.ofMillis(1));
        failing.fetchTileAsync(6, 33, 21).get();
        Thread.sleep(20);

        RecordingSource working = new RecordingSource(new byte[]{3}, false);
        TileFetcher recovered = new TileFetcher(cache, working, Duration.ofMillis(1));

        assertArrayEquals(new byte[]{3}, recovered.fetchTileAsync(6, 33, 21).get());
    }
}
