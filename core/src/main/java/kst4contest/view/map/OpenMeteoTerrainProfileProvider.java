package kst4contest.view.map;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Terrain provider using the Open-Meteo Elevation API.
 *
 * <p>Important API constraint: Open-Meteo accepts up to 100 coordinate pairs per
 * elevation request. Earlier KST4Contest profile calculations could ask for up
 * to 1201 samples, which forced many HTTP requests for one visible profile. This
 * provider intentionally clamps the online profile to one API request. The
 * offline Copernicus path can later keep using the denser local sample count.</p>
 *
 * <p>The provider keeps a small in-memory cache for the running session. This is
 * deliberately local and lightweight: it avoids repeated API calls when the user
 * re-selects the same station, but it does not replace the existing long-term
 * offline DEM/download architecture.</p>
 */
public final class OpenMeteoTerrainProfileProvider implements TerrainProfileProvider {

    private static final String SOURCE_NAME = "Open-Meteo Copernicus GLO-90";
    private static final String DEFAULT_BASE_URL = "https://api.open-meteo.com/v1/elevation";

    /**
     * Open-Meteo elevation requests accept up to 100 coordinate pairs. Keeping the
     * online provider at exactly one request per path makes limit behavior
     * predictable and avoids request bursts while clicking through stations.
     */
    private static final int MAX_COORDINATES_PER_REQUEST = 100;
    private static final int MAX_ONLINE_PROFILE_SAMPLE_COUNT = MAX_COORDINATES_PER_REQUEST;
    private static final int MIN_ONLINE_PROFILE_SAMPLE_COUNT = 2;

    private static final int MAX_CACHED_PROFILES = 256;

    /**
     * The free tier allows many more calls per minute, but this soft limiter keeps
     * accidental click/refresh bursts polite and easier to diagnose.
     */
    private static final Duration MINIMUM_REQUEST_INTERVAL = Duration.ofMillis(250);

    private static final Pattern ELEVATION_ARRAY_PATTERN =
            Pattern.compile("\\\"elevation\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);

    private static final Pattern ERROR_REASON_PATTERN =
            Pattern.compile("\\\"reason\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);

    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final String baseUrl;

    private final Map<RequestCacheKey, TerrainProfileData> profileCache =
            new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RequestCacheKey, TerrainProfileData> eldest) {
                    return size() > MAX_CACHED_PROFILES;
                }
            };

    private long lastRequestStartEpochMillis = 0L;

    public OpenMeteoTerrainProfileProvider() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(4))
                        .build(),
                Duration.ofSeconds(8),
                DEFAULT_BASE_URL
        );
    }

    public OpenMeteoTerrainProfileProvider(HttpClient httpClient,
                                           Duration requestTimeout,
                                           String baseUrl) {
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(8) : requestTimeout;
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl.trim();
    }

    @Override
    public TerrainProfileData loadProfile(TerrainProfileRequest request) {
        if (request == null || !request.hasUsableEndpoints() || request.requestedSampleCount() < 2) {
            return TerrainProfileData.empty(SOURCE_NAME);
        }

        int sampleCount = resolveOnlineSampleCount(request.requestedSampleCount());
        RequestCacheKey cacheKey = RequestCacheKey.from(request, sampleCount);

        TerrainProfileData cachedProfile = loadFromCache(cacheKey);
        if (cachedProfile != null) {
            return cachedProfile;
        }

        List<SamplePoint> samplePoints = buildSamplePoints(request, sampleCount);
        TerrainProfileData profileData = fetchProfile(samplePoints);

        if (profileData.hasUsableProfile()) {
            saveToCache(cacheKey, profileData);
        }

        return profileData;
    }

    private int resolveOnlineSampleCount(int requestedSampleCount) {
        return Math.max(
                MIN_ONLINE_PROFILE_SAMPLE_COUNT,
                Math.min(requestedSampleCount, MAX_ONLINE_PROFILE_SAMPLE_COUNT)
        );
    }

    private TerrainProfileData loadFromCache(RequestCacheKey cacheKey) {
        synchronized (profileCache) {
            return profileCache.get(cacheKey);
        }
    }

    private void saveToCache(RequestCacheKey cacheKey, TerrainProfileData profileData) {
        synchronized (profileCache) {
            profileCache.put(cacheKey, profileData);
        }
    }

    private TerrainProfileData fetchProfile(List<SamplePoint> samplePoints) {
        try {
            List<Double> elevations = fetchElevations(samplePoints);

            if (elevations.size() != samplePoints.size()) {
                return TerrainProfileData.empty(SOURCE_NAME + " returned an incomplete elevation array");
            }

            List<PathProfilePoint> profilePoints = new ArrayList<>(samplePoints.size());

            for (int i = 0; i < samplePoints.size(); i++) {
                SamplePoint samplePoint = samplePoints.get(i);
                double elevationMeters = elevations.get(i);

                if (!Double.isFinite(elevationMeters)) {
                    return TerrainProfileData.empty(SOURCE_NAME + " returned no-data elevation samples");
                }

                profilePoints.add(new PathProfilePoint(
                        samplePoint.distanceKm(),
                        samplePoint.latitudeDeg(),
                        samplePoint.longitudeDeg(),
                        elevationMeters
                ));
            }

            return new TerrainProfileData(profilePoints, SOURCE_NAME, false);
        } catch (Exception exception) {
            return TerrainProfileData.empty(SOURCE_NAME + " failed: " + buildShortFailureMessage(exception));
        }
    }

    private List<SamplePoint> buildSamplePoints(TerrainProfileRequest request, int sampleCount) {
        List<SamplePoint> points = new ArrayList<>(sampleCount);

        for (int i = 0; i < sampleCount; i++) {
            double t = sampleCount == 1 ? 0.0 : (double) i / (double) (sampleCount - 1);

            PathGeometryUtils.GeoPoint interpolatedPoint =
                    PathGeometryUtils.interpolateGreatCirclePoint(
                            request.fromLatitudeDeg(),
                            request.fromLongitudeDeg(),
                            request.toLatitudeDeg(),
                            request.toLongitudeDeg(),
                            t
                    );

            double latitudeDeg = interpolatedPoint.latitudeDeg();
            double longitudeDeg = interpolatedPoint.longitudeDeg();

            if (!Double.isFinite(latitudeDeg) || !Double.isFinite(longitudeDeg)) {
                return List.of();
            }

            points.add(new SamplePoint(
                    request.totalDistanceKm() * t,
                    latitudeDeg,
                    longitudeDeg
            ));
        }

        return points;
    }

    private List<Double> fetchElevations(List<SamplePoint> samplePoints) throws Exception {
        if (samplePoints == null || samplePoints.isEmpty()) {
            return List.of();
        }

        if (samplePoints.size() > MAX_COORDINATES_PER_REQUEST) {
            throw new IllegalArgumentException("Open-Meteo request would contain more than 100 coordinates");
        }

        waitForRateLimitSlot();

        String latitudeParameter = samplePoints.stream()
                .map(point -> formatCoordinate(point.latitudeDeg()))
                .collect(Collectors.joining(","));

        String longitudeParameter = samplePoints.stream()
                .map(point -> formatCoordinate(point.longitudeDeg()))
                .collect(Collectors.joining(","));

        String requestUrl = baseUrl
                + "?latitude=" + latitudeParameter
                + "&longitude=" + longitudeParameter;

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("User-Agent", "KST4Contest path-analysis")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() / 100 != 2) {
            String reason = extractErrorReason(response.body());
            throw new IOException("HTTP " + response.statusCode() + (reason.isBlank() ? "" : " - " + reason));
        }

        return parseElevationArray(response.body());
    }

    private synchronized void waitForRateLimitSlot() throws InterruptedException {
        long now = System.currentTimeMillis();
        long waitMillis = MINIMUM_REQUEST_INTERVAL.toMillis() - (now - lastRequestStartEpochMillis);

        if (waitMillis > 0L) {
            Thread.sleep(waitMillis);
        }

        lastRequestStartEpochMillis = System.currentTimeMillis();
    }

    private List<Double> parseElevationArray(String responseBody) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException("Empty response body");
        }

        Matcher elevationArrayMatcher = ELEVATION_ARRAY_PATTERN.matcher(responseBody);
        if (!elevationArrayMatcher.find()) {
            String reason = extractErrorReason(responseBody);
            throw new IOException("No elevation array found" + (reason.isBlank() ? "" : ": " + reason));
        }

        String arrayContent = elevationArrayMatcher.group(1).trim();
        if (arrayContent.isBlank()) {
            return List.of();
        }

        String[] parts = arrayContent.split("\\s*,\\s*");
        List<Double> elevations = new ArrayList<>(parts.length);

        for (String part : parts) {
            String value = part.trim();

            if (value.isBlank()
                    || "null".equalsIgnoreCase(value)
                    || "nan".equalsIgnoreCase(value)) {
                elevations.add(Double.NaN);
            } else {
                elevations.add(Double.parseDouble(value));
            }
        }

        return elevations;
    }

    private String extractErrorReason(String responseBody) {
        Matcher matcher = ERROR_REASON_PATTERN.matcher(responseBody == null ? "" : responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String buildShortFailureMessage(Exception exception) {
        if (exception == null) {
            return "unknown error";
        }

        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }

        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private String formatCoordinate(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private record SamplePoint(double distanceKm, double latitudeDeg, double longitudeDeg) {
    }

    private record RequestCacheKey(
            String fromLatitudeDeg,
            String fromLongitudeDeg,
            String toLatitudeDeg,
            String toLongitudeDeg,
            int sampleCount
    ) {
        private static RequestCacheKey from(TerrainProfileRequest request, int sampleCount) {
            return new RequestCacheKey(
                    normalizeCoordinate(request.fromLatitudeDeg()),
                    normalizeCoordinate(request.fromLongitudeDeg()),
                    normalizeCoordinate(request.toLatitudeDeg()),
                    normalizeCoordinate(request.toLongitudeDeg()),
                    sampleCount
            );
        }

        private static String normalizeCoordinate(double value) {
            return String.format(Locale.US, "%.6f", value);
        }
    }
}