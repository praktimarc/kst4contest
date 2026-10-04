package kst4contest.view.map;

import kst4contest.utils.ApplicationFileUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supplies map tiles, from memory, from disk, or from OpenStreetMap, in that order.
 *
 * Three rules it has to keep. The tile service asks callers to identify themselves and
 * not to re-download what they already have, which is what the disk cache is for — a
 * contest station restarts the client more than once an evening and the view it comes
 * back to is the view it left. The retired {@code a}/{@code b}/{@code c} subdomains are
 * not used. And a request that fails is counted and visible through
 * {@link #hasReachedTileSource()}, because the alternative is what the first version
 * did: return null quietly and leave the operator looking at an empty map with no idea
 * whether the problem was the network or the band.
 */
public final class TileFetcher {

    private static final int MEMORY_CACHE_MAX_TILES = 2048;

    /**
     * OpenStreetMap's tile usage policy allows a client at most two concurrent
     * downloads. Going wider fills the map faster and gets the application blocked.
     */
    public static final int MAX_CONCURRENT_REQUESTS = 2;

    /**
     * How long a tile that failed is left alone.
     *
     * The map re-requests every visible tile whenever the view changes, so without a
     * pause a server answering 429 would be asked again on every drag — which the same
     * usage policy calls abuse, and which is how a client gets blocked rather than
     * throttled.
     */
    private static final Duration DEFAULT_RETRY_PAUSE = Duration.ofSeconds(30);

    /**
     * OpenStreetMap's tile usage policy asks for an identifying agent. A generic one
     * gets the whole application blocked rather than throttled.
     */
    private static final String USER_AGENT = "kst4contest/1.0 amateur-radio-contest-tool";

    private static final String TILE_HOST = "https://tile.openstreetmap.org";

    /** Where a tile comes from when neither cache has it. Replaced in tests. */
    public interface TileSource {
        byte[] fetch(String url) throws IOException, InterruptedException;
    }

    private final TileSource source;
    private final Path cacheDirectory;
    private final ExecutorService executor;
    private final Map<String, byte[]> memoryCache;

    private final AtomicInteger failedRequests = new AtomicInteger();

    /*
     * Which happened more recently, an arrival or a failure. A single latched "we got a
     * tile once" flag cannot answer the question the warning is for: after a restart
     * without a network, the first tile out of the disk cache would set it and every
     * failure after that would be silent. Counters rather than timestamps, so the
     * comparison does not depend on clock resolution.
     */
    private final Duration retryPause;

    /** When each recently failed tile may be asked for again. */
    private final Map<String, Long> retryNotBeforeNanos = new java.util.concurrent.ConcurrentHashMap<>();

    private final AtomicInteger arrivals = new AtomicInteger();
    private final AtomicInteger lastArrivalAtFailureCount = new AtomicInteger(-1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TileFetcher() {
        this(defaultCacheDirectory(), new HttpTileSource());
    }

    public TileFetcher(Path cacheDirectory, TileSource source) {
        this(cacheDirectory, source, DEFAULT_RETRY_PAUSE);
    }

    public TileFetcher(Path cacheDirectory, TileSource source, Duration retryPause) {
        this.cacheDirectory = cacheDirectory;
        this.source = source;
        this.retryPause = retryPause;
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS, runnable -> {
            Thread thread = new Thread(runnable, "map-tile-fetch");
            thread.setDaemon(true);
            return thread;
        });

        this.memoryCache = Collections.synchronizedMap(
                new LinkedHashMap<>(MEMORY_CACHE_MAX_TILES, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                        return size() > MEMORY_CACHE_MAX_TILES;
                    }
                }
        );
    }

    /** The tile directory beside the rest of the application's files. */
    public static Path defaultCacheDirectory() {
        return Path.of(ApplicationFileUtils.getFilePath("kst4contest", "tile-cache"));
    }

    public static String tileUrl(int zoom, int x, int y) {
        return TILE_HOST + "/" + zoom + "/" + x + "/" + y + ".png";
    }

    /**
     * @return the tile, or null when it could not be had. Null is a gap in the map, not
     *         an error to act on; {@link #hasReachedTileSource()} is what tells the map
     *         whether to say something about it.
     */
    public CompletableFuture<byte[]> fetchTileAsync(int zoom, int x, int y) {
        String key = zoom + "/" + x + "/" + y;

        byte[] fromMemory = memoryCache.get(key);
        if (fromMemory != null) {
            return CompletableFuture.completedFuture(fromMemory);
        }

        return CompletableFuture.supplyAsync(() -> loadTile(zoom, x, y, key), executor);
    }

    private byte[] loadTile(int zoom, int x, int y, String key) {
        byte[] fromDisk = readFromDisk(zoom, x, y);
        if (fromDisk != null) {
            memoryCache.put(key, fromDisk);
            recordArrival();
            return fromDisk;
        }

        Long notBefore = retryNotBeforeNanos.get(key);
        if (notBefore != null && System.nanoTime() < notBefore) {
            /* Still inside the pause after a failure; do not ask again yet. */
            return null;
        }

        try {
            byte[] tile = source.fetch(tileUrl(zoom, x, y));
            if (tile == null || tile.length == 0) {
                recordFailure(key);
                return null;
            }

            memoryCache.put(key, tile);
            writeToDisk(zoom, x, y, tile);
            retryNotBeforeNanos.remove(key);
            recordArrival();
            return tile;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            recordFailure(key);
            return null;
        } catch (Exception unreachable) {
            /* Held back, not cached: after the pause the next pan tries again. */
            recordFailure(key);
            return null;
        }
    }

    /**
     * Whether the map should say that tiles are not arriving.
     *
     * True once a request has failed and nothing has arrived since — from the network
     * or from the disk cache. It therefore covers both the restart without a network
     * and the network lost halfway through an evening, and it clears itself as soon as
     * a tile comes in again.
     */
    public boolean isTileSourceUnavailable() {
        int failures = failedRequests.get();
        return failures > 0 && lastArrivalAtFailureCount.get() < failures;
    }

    /** Whether any tile has ever arrived, from either cache or the network. */
    public boolean hasReachedTileSource() {
        return arrivals.get() > 0;
    }

    public int failedRequestCount() {
        return failedRequests.get();
    }

    /**
     * Forgets a tile that turned out to be unusable, on disk as well as in memory.
     *
     * The caller is the only one who can tell: this class hands out bytes, and whether
     * they decode into an image is discovered one layer further up. Without this a
     * truncated file — the process died mid-write — is served again on every later run
     * and that square of the map stays blank for good.
     */
    public void discardTile(int zoom, int x, int y) {
        memoryCache.remove(zoom + "/" + x + "/" + y);
        try {
            Files.deleteIfExists(tilePath(zoom, x, y));
        } catch (Exception undeletable) {
            /* Then it stays, and the map has a gap rather than a crash. */
        }
    }

    /**
     * Stops the fetch threads.
     *
     * Not optional housekeeping: a live pool thread is a GC root, so an unclosed
     * fetcher holds its whole memory cache reachable. The map window builds one per
     * open, and an evening of opening and closing it would retain every one of them.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            executor.shutdownNow();
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    private void recordFailure(String key) {
        failedRequests.incrementAndGet();
        retryNotBeforeNanos.put(key, System.nanoTime() + retryPause.toNanos());
    }

    private void recordArrival() {
        arrivals.incrementAndGet();
        lastArrivalAtFailureCount.set(failedRequests.get());
    }

    private Path tilePath(int zoom, int x, int y) {
        return cacheDirectory.resolve(String.valueOf(zoom)).resolve(String.valueOf(x)).resolve(y + ".png");
    }

    private byte[] readFromDisk(int zoom, int x, int y) {
        try {
            Path path = tilePath(zoom, x, y);
            return Files.exists(path) ? Files.readAllBytes(path) : null;
        } catch (Exception unreadable) {
            /* A cache is an optimisation. An unreadable one is a cache miss. */
            return null;
        }
    }

    /**
     * Written under a temporary name and then moved into place.
     *
     * A plain write is not atomic. If the process dies partway through — battery, a
     * kill, an operating-system shutdown — a truncated file is left under the real name
     * and is served as a tile on every later run.
     */
    private void writeToDisk(int zoom, int x, int y, byte[] tile) {
        Path path = tilePath(zoom, x, y);
        Path partial = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.write(partial, tile);
            Files.move(partial, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception unwritable) {
            /* A read-only or full disk costs the cache, not the map. */
            try {
                Files.deleteIfExists(partial);
            } catch (Exception alsoUndeletable) {
                /* Nothing further to try; a stray temporary file is harmless. */
            }
        }
    }

    /** The real source: one HTTPS request per tile. */
    private static final class HttpTileSource implements TileSource {

        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        @Override
        public byte[] fetch(String url) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<byte[]> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new IOException("tile request answered " + response.statusCode());
            }
            return response.body();
        }
    }
}
