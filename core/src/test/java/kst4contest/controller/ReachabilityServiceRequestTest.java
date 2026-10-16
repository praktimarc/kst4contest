package kst4contest.controller;

import kst4contest.locatorUtils.Location;
import kst4contest.model.ChatPreferences;
import kst4contest.view.map.LocatorHighestPointResolver;
import kst4contest.view.map.PathAnalysisRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How {@link ReachabilityService} turns the station preferences into a path-analysis
 * request, and which inputs reach the key that decides whether a cached result is reused.
 */
class ReachabilityServiceRequestTest {

    private static final String OWN = "JN49FK";
    private static final String TARGET = "JO50AE";
    private static final double TARGET_CENTRE_LAT = Location.extractLat(TARGET).toDegrees();
    private static final double TARGET_CENTRE_LON = Location.extractLon(TARGET).toDegrees();

    private static final LocatorHighestPointResolver.HighestPoint PEAK =
            new LocatorHighestPointResolver.HighestPoint(TARGET, TARGET_CENTRE_LAT + 0.01, TARGET_CENTRE_LON - 0.02, 640.0);

    private static final Function<String, LocatorHighestPointResolver.HighestPoint> NOTHING_CACHED = locator -> null;
    private static final Function<String, LocatorHighestPointResolver.HighestPoint> PEAK_CACHED =
            locator -> TARGET.equals(locator) ? PEAK : null;

    @TempDir
    Path directory;

    private ChatPreferences preferences() {
        return new ChatPreferences(directory.resolve("preferences.xml"));
    }

    private static PathAnalysisRequest build(ChatPreferences preferences,
                                             Function<String, LocatorHighestPointResolver.HighestPoint> cache) {
        return ReachabilityService.buildRequest(
                preferences, OWN, "DL0TEST", TARGET, TARGET_CENTRE_LAT, TARGET_CENTRE_LON, 144.3, cache);
    }

    @Test
    void withoutAnOwnHeightTheElevationModelDecides() {
        PathAnalysisRequest request = build(preferences(), NOTHING_CACHED);

        assertTrue(Double.isNaN(request.homeGroundElevationOverrideMeters()));
    }

    @Test
    void theOwnHeightAboveSeaLevelReachesTheRequest() {
        ChatPreferences preferences = preferences();
        preferences.setStn_pathAnalysisOwnHeightAslMeters(312.0);

        PathAnalysisRequest request = build(preferences, NOTHING_CACHED);

        assertEquals(312.0, request.homeGroundElevationOverrideMeters(), 1e-12);
        assertEquals(Location.extractLat(OWN).toDegrees(), request.fromLatitudeDeg(), 1e-12,
                "the own position stays the locator centre");
    }

    @Test
    void aCachedHighestPointMovesTheTarget() {
        PathAnalysisRequest request = build(preferences(), PEAK_CACHED);

        assertEquals(PEAK.latitudeDeg(), request.toLatitudeDeg(), 1e-12);
        assertEquals(PEAK.longitudeDeg(), request.toLongitudeDeg(), 1e-12);
        assertEquals(640.0, request.targetGroundElevationOverrideMeters(), 1e-12);
        assertEquals(TARGET, request.toLocator6());
        assertFalse(ReachabilityService.needsHighestPointLookup(request));
    }

    @Test
    void anUnresolvedSquareKeepsTheCentreForTheBackgroundLookup() {
        PathAnalysisRequest request = build(preferences(), NOTHING_CACHED);

        assertEquals(TARGET_CENTRE_LAT, request.toLatitudeDeg(), 1e-12);
        assertEquals(TARGET_CENTRE_LON, request.toLongitudeDeg(), 1e-12);
        assertTrue(ReachabilityService.needsHighestPointLookup(request));
    }

    @Test
    void switchedOffTheTargetStaysOnTheCentreEvenWhenThePeakIsKnown() {
        ChatPreferences preferences = preferences();
        preferences.setStn_pathAnalysisDxHighestPointInSquare(false);

        PathAnalysisRequest request = build(preferences, PEAK_CACHED);

        assertEquals(TARGET_CENTRE_LAT, request.toLatitudeDeg(), 1e-12);
        assertEquals(TARGET_CENTRE_LON, request.toLongitudeDeg(), 1e-12);
        assertTrue(Double.isNaN(request.targetGroundElevationOverrideMeters()));
    }

    @Test
    void theHighestPointIsOnByDefault() {
        assertTrue(preferences().isStn_pathAnalysisDxHighestPointInSquare());
    }

    @Test
    void theOwnHeightChangesTheKey() {
        ChatPreferences preferences = preferences();
        String withoutHeight = ReachabilityService.buildCalculationKey(build(preferences, NOTHING_CACHED), true);

        preferences.setStn_pathAnalysisOwnHeightAslMeters(312.0);
        String withHeight = ReachabilityService.buildCalculationKey(build(preferences, NOTHING_CACHED), true);

        preferences.setStn_pathAnalysisOwnHeightAslMeters(313.0);
        String withOtherHeight = ReachabilityService.buildCalculationKey(build(preferences, NOTHING_CACHED), true);

        assertNotEquals(withoutHeight, withHeight);
        assertNotEquals(withHeight, withOtherHeight);
    }

    /* One decimal used to be the key precision, so 123.04 and 123.00 m shared a result. */
    @Test
    void theOwnHeightIsKeyedAtFullPrecision() {
        ChatPreferences preferences = preferences();

        preferences.setStn_pathAnalysisOwnHeightAslMeters(123.00);
        String even = ReachabilityService.buildCalculationKey(build(preferences, NOTHING_CACHED), true);

        preferences.setStn_pathAnalysisOwnHeightAslMeters(123.04);
        String odd = ReachabilityService.buildCalculationKey(build(preferences, NOTHING_CACHED), true);

        assertNotEquals(even, odd);
    }

    @Test
    void theTargetGroundElevationIsKeyedAtFullPrecision() {
        LocatorHighestPointResolver.HighestPoint lower =
                new LocatorHighestPointResolver.HighestPoint(TARGET, PEAK.latitudeDeg(), PEAK.longitudeDeg(), 640.00);
        LocatorHighestPointResolver.HighestPoint higher =
                new LocatorHighestPointResolver.HighestPoint(TARGET, PEAK.latitudeDeg(), PEAK.longitudeDeg(), 640.04);

        assertNotEquals(
                ReachabilityService.buildCalculationKey(build(preferences(), locator -> lower), true),
                ReachabilityService.buildCalculationKey(build(preferences(), locator -> higher), true));
    }

    /*
     * Before its square is resolved, a request looks the same with the switch on or off:
     * both stand on the centre. Only the switch in the key keeps a centre result from
     * answering a request that asks for the highest point, and the other way round.
     */
    @Test
    void theSwitchChangesTheKeyEvenForTheSameCentreRequest() {
        PathAnalysisRequest request = build(preferences(), NOTHING_CACHED);

        assertNotEquals(
                ReachabilityService.buildCalculationKey(request, true),
                ReachabilityService.buildCalculationKey(request, false));
    }

    @Test
    void theResolvedTargetChangesTheKey() {
        ChatPreferences preferences = preferences();

        String centre = ReachabilityService.buildCalculationKey(build(preferences, NOTHING_CACHED), true);
        String peak = ReachabilityService.buildCalculationKey(build(preferences, PEAK_CACHED), true);

        assertNotEquals(centre, peak);
    }

    @Test
    void theSameInputsGiveTheSameKey() {
        ChatPreferences preferences = preferences();
        preferences.setStn_pathAnalysisOwnHeightAslMeters(312.0);

        assertEquals(
                ReachabilityService.buildCalculationKey(build(preferences, PEAK_CACHED), true),
                ReachabilityService.buildCalculationKey(build(preferences, PEAK_CACHED), true));
    }
}
