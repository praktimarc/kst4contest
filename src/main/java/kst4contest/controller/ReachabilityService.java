package kst4contest.controller;
import kst4contest.logic.BandOpportunityResolver;
import kst4contest.view.map.MapCallsignRawSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;
import kst4contest.locatorUtils.Location;
import kst4contest.model.Band;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;
import kst4contest.view.map.GeometryOnlyPathAnalysisService;

import kst4contest.view.map.OpenMeteoTerrainProfileProvider;
import kst4contest.view.map.PathAnalysisRequest;
import kst4contest.view.map.PathAnalysisResult;
import kst4contest.view.map.PathAnalysisService;
import kst4contest.view.map.PathGeometryUtils;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

/**
 * Central service for tropo/path reachability calculations.
 *
 * <p>This service is now the single calculation path for:
 * <ul>
 *     <li>the station table Tropo column</li>
 *     <li>the Tropo filter/sorter</li>
 *     <li>the station map path-analysis detail panel</li>
 * </ul>
 *
 * <p>The service always calculates a full {@link PathAnalysisResult}. When the
 * result contains a usable link budget, the bidirectional SSB margin is copied
 * into all matching {@link ChatMember} objects. The UI can then sort/filter by
 * a simple number while the map still receives the full path result.</p>
 */
public final class ReachabilityService {

    private static final long FAILED_ANALYSIS_RETRY_DELAY_MS = 5L * 60L * 1000L;

    private final ChatController chatController;
    private final PathAnalysisService pathAnalysisService;
    private final ExecutorService executor;

    /**
     * Prevents duplicate jobs for the same path while a calculation is already
     * queued or running.
     */
    private final Set<String> queuedCalculationKeys = ConcurrentHashMap.newKeySet();



    /**
     * Stores completed usable path-analysis results. Failed/no-budget results are
     * not permanently cached so transient terrain/network problems can be retried.
     */
    private final Map<String, PathAnalysisResult> pathAnalysisResultCache = new ConcurrentHashMap<>();

    /**
     * Temporary callback list for map requests waiting for an already running
     * background calculation.
     */
    private final Map<String, List<Consumer<PathAnalysisResult>>> pendingMapCallbacksByKey = new ConcurrentHashMap<>();

    /**
     * Prevents immediate retry loops for failed/no-budget analyses, especially
     * when a TableView cell repeatedly asks for the same value.
     */
    private final Map<String, Long> failedCalculationRetryAfterEpochMs = new ConcurrentHashMap<>();

    public ReachabilityService(ChatController chatController) {
        this.chatController = Objects.requireNonNull(chatController, "chatController");
        this.pathAnalysisService = new GeometryOnlyPathAnalysisService(new OpenMeteoTerrainProfileProvider());
        this.executor = Executors.newSingleThreadExecutor(new ReachabilityThreadFactory());
    }

    /**
     * Ensures that the automatically selected reachability band is calculated.
     *
     * @param member chatmember to evaluate
     */
    public void ensureAutoTropoMarginCalculated(ChatMember member) {
        ensureTropoMarginCalculated(member, resolveAutoBand(member));
    }

    /**
     * Ensures that a station/band SSB-margin value exists if it is already cached.
     *
     * <p>This method deliberately does not start online terrain analysis anymore.
     * It is safe to call from table cells and filters because it will not consume
     * API quota. Full calculations are now started only by explicit map/manual
     * requests.</p>
     *
     * @param member chatmember to evaluate
     * @param band band to evaluate
     */
    public void ensureTropoMarginCalculated(ChatMember member, Band band) {
        if (member == null || band == null) {
            return;
        }

        if (member.hasFiniteTropoSsbMarginDb(band)) {
            return;
        }

        PathAnalysisRequest request = buildRequestForChatMember(member, band);
        if (request == null) {
            return;
        }

        PathAnalysisResult cachedResult = pathAnalysisResultCache.get(buildCalculationKey(request));
        if (cachedResult != null) {
            storePathAnalysisResultInChatMembers(member, band, cachedResult);
        }
    }



    /**
     * Requests a full path analysis for the station map.
     *
     * <p>This is the only normal automatic entry point for online/full terrain
     * analysis. The map needs the full PathAnalysisResult, and the station table
     * needs the SSB margin. Both are produced here through the same calculation.</p>
     *
     * @param member best matching ChatMember, may be null when only a map snapshot exists
     * @param selectedSnapshot selected map snapshot
     * @param fxCallback callback executed on the JavaFX thread
     */
    public void requestPathAnalysisForMap(ChatMember member,
                                          MapCallsignRawSnapshot selectedSnapshot,
                                          Consumer<PathAnalysisResult> fxCallback) {

        String ownLocator6 = normalizeLocator6(chatController.getChatPreferences().getStn_loginLocatorMainCat());

        if (selectedSnapshot == null) {
            dispatchFxCallback(fxCallback, PathAnalysisResult.waitingForSelection(ownLocator6));
            return;
        }

        String targetLocator6 = normalizeLocator6(selectedSnapshot.locator6());

        if (ownLocator6.length() != 6) {
            dispatchFxCallback(fxCallback,
                    PathAnalysisResult.waitingForValidHomeLocator(ownLocator6, targetLocator6));
            return;
        }

        if (!selectedSnapshot.hasUsablePosition()) {
            dispatchFxCallback(fxCallback,
                    PathAnalysisResult.waitingForValidTarget(ownLocator6, targetLocator6));
            return;
        }

        double analysisFrequencyMHz = PathGeometryUtils.resolveAnalysisFrequencyMHz(
                selectedSnapshot.lastKnownFrequenciesByBand()
        );

        Band analysisBand = Band.fromFrequency(analysisFrequencyMHz);
        if (analysisBand != null && !isUsableAutomaticBand(member, analysisBand)) {
            analysisFrequencyMHz = Double.NaN;
            analysisBand = null;
        }

        if (!Double.isFinite(analysisFrequencyMHz)
                || analysisFrequencyMHz <= 0.0
                || analysisBand == null) {

            Band fallbackBand = resolveAutoBand(member);
            if (fallbackBand == null) {
                dispatchFxCallback(
                        fxCallback,
                        PathAnalysisResult.waitingForUsableBand(
                                ownLocator6,
                                targetLocator6,
                                selectedSnapshot.callSignRaw()
                        )
                );
                return;
            }

            analysisBand = fallbackBand;
            analysisFrequencyMHz = resolveAnalysisFrequencyForBand(member, fallbackBand);
        }

        PathAnalysisRequest request = buildRequest(
                ownLocator6,
                selectedSnapshot.callSignRaw(),
                targetLocator6,
                selectedSnapshot.latitudeDeg(),
                selectedSnapshot.longitudeDeg(),
                analysisFrequencyMHz
        );

        requestPathAnalysisAndStore(member, analysisBand, request, fxCallback);
    }



    /**
     * Starts a full on-demand reachability calculation for one station row.
     *
     * <p>Use this for explicit operator actions only. It may call the online terrain
     * API and therefore must not be used from TableView cell factories or automatic
     * chat-join processing.</p>
     *
     * @param member station to calculate
     * @param band selected reachability band
     */
    public void calculateSelectedStationOnDemand(ChatMember member, Band band) {
        if (member == null || band == null) {
            return;
        }

        PathAnalysisRequest request = buildRequestForChatMember(member, band);
        if (request == null) {
            member.setTropoSsbMarginDb(band, Double.NaN);
            chatController.fireUserListUpdate("Reachability unavailable");
            return;
        }

        requestPathAnalysisAndStore(member, band, request, null);
    }

    /**
     * Resolves the auto reachability band.
     *
     * <ol>
     *     <li>Use the lowest band detected in this session.</li>
     *     <li>If no session band exists and the station is in the microwave category, use 1296 MHz.</li>
     *     <li>Otherwise use 144 MHz.</li>
     * </ol>
     *
     * @param member member to inspect
     * @return resolved band
     */
    public Band resolveAutoBand(ChatMember member) {
        EnumSet<Band> enabledBands = getEnabledStationBands();
        if (enabledBands.isEmpty()) {
            return null;
        }

        List<ChatMember> variants = resolveCallsignVariants(member);
        BandOpportunityResolver.Resolution resolution =
                BandOpportunityResolver.resolve(variants, System.currentTimeMillis());

        EnumSet<Band> availableOfferedBands = resolution.getAvailableBands();
        availableOfferedBands.retainAll(enabledBands);

        if (!availableOfferedBands.isEmpty()) {
            return availableOfferedBands.stream()
                    .min(Comparator.comparingDouble(Band::getDefaultAnalysisFrequencyMHz))
                    .orElse(null);
        }

        // Known evidence exists, but every matching band is disabled or NOT QRV.
        if (resolution.hasBandEvidence()) {
            return null;
        }

        EnumSet<Band> fallbackBands = EnumSet.copyOf(enabledBands);
        fallbackBands.removeAll(resolution.getNotQrvBands());
        if (fallbackBands.isEmpty()) {
            return null;
        }

        if (member != null
                && member.getChatCategory() != null
                && member.getChatCategory().getCategoryNumber() == ChatCategory.MICROWAVE
                && fallbackBands.contains(Band.B_1296)) {
            return Band.B_1296;
        }

        if (member != null
                && member.getChatCategory() != null
                && member.getChatCategory().getCategoryNumber() == ChatCategory.FIFTYSEVENTYMHz) {
            if (fallbackBands.contains(Band.B_50)) {
                return Band.B_50;
            }
            if (fallbackBands.contains(Band.B_70)) {
                return Band.B_70;
            }
        }

        if (fallbackBands.contains(Band.B_144)) {
            return Band.B_144;
        }

        return fallbackBands.stream()
                .min(Comparator.comparingDouble(Band::getDefaultAnalysisFrequencyMHz))
                .orElse(null);
    }

    /**
     * Returns the active own bands configured in the station preferences. High bands
     * above 10 GHz are intentionally ignored for the first version.
     *
     * @return set of enabled bands
     */
    public EnumSet<Band> getEnabledStationBands() {
        return BandOpportunityResolver.getEnabledStationBands(
                chatController.getChatPreferences()
        );
    }

    private List<ChatMember> resolveCallsignVariants(ChatMember member) {
        if (member == null) {
            return List.of();
        }

        String rawCall = member.getCallSignRaw() != null
                ? member.getCallSignRaw()
                : member.getCallSign();

        List<ChatMember> variants = chatController.findActiveChatMembersByRawCall(rawCall);
        return variants.isEmpty() ? List.of(member) : variants;
    }

    /**
     * Verifies that an automatically selected map/snapshot frequency belongs to a
     * locally enabled band that is still available after NOT-QRV resolution.
     * Manual UI band overrides are handled separately and are not changed here.
     */
    private boolean isUsableAutomaticBand(ChatMember member, Band band) {
        if (band == null || !getEnabledStationBands().contains(band)) {
            return false;
        }

        if (member == null) {
            return true;
        }

        BandOpportunityResolver.Resolution resolution = BandOpportunityResolver.resolve(
                resolveCallsignVariants(member),
                System.currentTimeMillis()
        );

        return resolution.getAvailableBands().contains(band);
    }

    /**
     * Stops the background executor.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * Starts or reuses one shared path-analysis calculation.
     *
     * @param member matching ChatMember, may be null for map-only snapshots
     * @param band band under which the value is stored in ChatMember
     * @param request full path-analysis request
     * @param fxCallback optional map callback
     */
    private void requestPathAnalysisAndStore(ChatMember member,
                                             Band band,
                                             PathAnalysisRequest request,
                                             Consumer<PathAnalysisResult> fxCallback) {

        if (request == null || band == null) {
            return;
        }

        String calculationKey = buildCalculationKey(request);

        PathAnalysisResult cachedResult = pathAnalysisResultCache.get(calculationKey);
        if (cachedResult != null) {
            storePathAnalysisResultInChatMembers(member, band, cachedResult);
            dispatchFxCallback(fxCallback, cachedResult);
            return;
        }

        Long retryAfterEpochMs = failedCalculationRetryAfterEpochMs.get(calculationKey);
        if (retryAfterEpochMs != null && System.currentTimeMillis() < retryAfterEpochMs) {
            dispatchFxCallback(fxCallback, createNoProfileResult(
                    request,
                    "Previous path analysis failed or the terrain API limit was reached. Retry is delayed briefly."
            ));
            return;
        }

        addPendingCallback(calculationKey, fxCallback);

        if (!queuedCalculationKeys.add(calculationKey)) {
            return;
        }

        executor.submit(() -> {
            PathAnalysisResult result;

            try {
                result = pathAnalysisService.analyze(request);
                if (result == null) {
                    result = createNoProfileResult(request, "Path analysis returned no result.");
                }
            } catch (Exception exception) {
                result = createNoProfileResult(
                        request,
                        "Path analysis failed: " + exception.getMessage()
                );
            }

            boolean usableBudget = hasUsableLinkBudget(result);

            if (usableBudget) {
                pathAnalysisResultCache.put(calculationKey, result);
                failedCalculationRetryAfterEpochMs.remove(calculationKey);
            } else {
                failedCalculationRetryAfterEpochMs.put(
                        calculationKey,
                        System.currentTimeMillis() + FAILED_ANALYSIS_RETRY_DELAY_MS
                );
            }

            queuedCalculationKeys.remove(calculationKey);

            PathAnalysisResult finalResult = result;
            Runnable updateTask = () -> {
                storePathAnalysisResultInChatMembers(member, band, finalResult);
                dispatchAndClearPendingCallbacks(calculationKey, finalResult);
                chatController.fireUserListUpdate("Reachability calculated");
            };

            if (Platform.isFxApplicationThread()) {
                updateTask.run();
            } else {
                Platform.runLater(updateTask);
            }
        });
    }

    /**
     * Builds a path-analysis request from a ChatMember row.
     *
     * @param member station row
     * @param band selected reachability band
     * @return request or null if locators are not usable
     */
    private PathAnalysisRequest buildRequestForChatMember(ChatMember member, Band band) {
        String ownLocator6 = normalizeLocator6(chatController.getChatPreferences().getStn_loginLocatorMainCat());
        String targetLocator6 = normalizeLocator6(member.getQra());

        if (ownLocator6.length() != 6 || targetLocator6.length() != 6) {
            return null;
        }

        Location targetLocation = new Location(targetLocator6);
        double analysisFrequencyMHz = resolveAnalysisFrequencyForBand(member, band);

        return buildRequest(
                ownLocator6,
                member.getCallSignRaw(),
                targetLocator6,
                targetLocation.getLatitude().toDegrees(),
                targetLocation.getLongitude().toDegrees(),
                analysisFrequencyMHz
        );
    }

    /**
     * Builds the shared PathAnalysisRequest used by map and table/manual requests.
     */
    private PathAnalysisRequest buildRequest(String ownLocator6,
                                             String targetCallsignRaw,
                                             String targetLocator6,
                                             double targetLatitudeDeg,
                                             double targetLongitudeDeg,
                                             double analysisFrequencyMHz) {

        Location homeLocation = new Location(ownLocator6);

        return new PathAnalysisRequest(
                ownLocator6,
                homeLocation.getLatitude().toDegrees(),
                homeLocation.getLongitude().toDegrees(),
                targetCallsignRaw,
                targetLocator6,
                targetLatitudeDeg,
                targetLongitudeDeg,
                analysisFrequencyMHz,
                chatController.getChatPreferences().getStn_pathAnalysisOwnAntennaHeightMeters(),
                chatController.getChatPreferences().getStn_pathAnalysisDefaultTargetAntennaHeightMeters(),
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                chatController.getChatPreferences().buildPathLinkBudgetSettings()
        );
    }

    /**
     * Stores the SSB margin from a completed analysis in all matching ChatMember
     * objects. Matching uses raw callsign and locator.
     */
    private void storePathAnalysisResultInChatMembers(ChatMember primaryMember,
                                                      Band band,
                                                      PathAnalysisResult result) {

        if (band == null || result == null) {
            return;
        }

        double marginDb = hasUsableLinkBudget(result)
                ? result.linkBudgetSummary().bidirectionalSsbMarginDb()
                : Double.NaN;

        if (primaryMember != null) {
            primaryMember.setTropoSsbMarginDb(band, marginDb);
        }

        String resultCallSignRaw = normalizeCallsignRaw(result.toCallsignRaw());
        String resultLocator6 = normalizeLocator6(result.toLocator6());

        for (ChatMember member : chatController.snapshotChatMembers()) {
            if (member == null) {
                continue;
            }

            String memberCallSignRaw = normalizeCallsignRaw(member.getCallSignRaw());
            if (!resultCallSignRaw.isBlank() && !resultCallSignRaw.equals(memberCallSignRaw)) {
                continue;
            }

            String memberLocator6 = normalizeLocator6(member.getQra());
            if (!resultLocator6.isBlank() && !resultLocator6.equals(memberLocator6)) {
                continue;
            }

            member.setTropoSsbMarginDb(band, marginDb);
        }
    }

    /**
     * Resolves the analysis frequency from a map snapshot first, because the map
     * aggregates all visible ChatMember variants and often knows the best current
     * frequency per band.
     *
     * @param member fallback member
     * @param selectedSnapshot selected map snapshot
     * @return analysis frequency in MHz
     */
    private double resolveAnalysisFrequencyForSnapshot(ChatMember member, MapCallsignRawSnapshot selectedSnapshot) {
        if (selectedSnapshot != null) {
            double snapshotFrequencyMHz =
                    PathGeometryUtils.resolveAnalysisFrequencyMHz(selectedSnapshot.lastKnownFrequenciesByBand());

            if (Double.isFinite(snapshotFrequencyMHz) && snapshotFrequencyMHz > 0.0) {
                return snapshotFrequencyMHz;
            }
        }

        Band fallbackBand = member == null ? Band.B_144 : resolveAutoBand(member);
        return resolveAnalysisFrequencyForBand(member, fallbackBand);
    }

    /**
     * Resolves the analysis frequency for one member/band pair.
     *
     * <p>Preference order:
     * <ol>
     *     <li>knownActiveBands frequency for the band</li>
     *     <li>current displayed QRG if it belongs to the same band</li>
     *     <li>band default frequency</li>
     * </ol>
     */
    private double resolveAnalysisFrequencyForBand(ChatMember member, Band band) {
        if (member != null && member.getKnownActiveBands() != null) {
            ChatMember.ActiveFrequencyInfo activeFrequencyInfo = member.getKnownActiveBands().get(band);
            if (activeFrequencyInfo != null
                    && Double.isFinite(activeFrequencyInfo.frequency)
                    && activeFrequencyInfo.frequency > 0.0) {
                return activeFrequencyInfo.frequency;
            }
        }

        if (member != null && member.getFrequency() != null && member.getFrequency().getValue() != null) {
            double parsedFrequencyMHz = PathGeometryUtils.tryParseFrequencyMHz(member.getFrequency().getValue());
            if (Double.isFinite(parsedFrequencyMHz) && parsedFrequencyMHz > 0.0) {
                Band parsedBand = Band.fromFrequency(parsedFrequencyMHz);
                if (parsedBand == null || parsedBand == band) {
                    return parsedFrequencyMHz;
                }
            }
        }

        return band.getDefaultAnalysisFrequencyMHz();
    }

    /**
     * Adds a map callback to the pending callback list for one calculation key.
     *
     * @param calculationKey path key
     * @param fxCallback callback to add
     */
    private void addPendingCallback(String calculationKey, Consumer<PathAnalysisResult> fxCallback) {
        if (fxCallback == null) {
            return;
        }

        pendingMapCallbacksByKey
                .computeIfAbsent(
                        calculationKey,
                        ignored -> Collections.synchronizedList(new ArrayList<>())
                )
                .add(fxCallback);
    }

    /**
     * Dispatches and removes all callbacks waiting for one completed calculation.
     *
     * @param calculationKey path key
     * @param result completed result
     */
    private void dispatchAndClearPendingCallbacks(String calculationKey, PathAnalysisResult result) {
        List<Consumer<PathAnalysisResult>> callbacks = pendingMapCallbacksByKey.remove(calculationKey);
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }

        List<Consumer<PathAnalysisResult>> callbackSnapshot;
        synchronized (callbacks) {
            callbackSnapshot = new ArrayList<>(callbacks);
        }

        for (Consumer<PathAnalysisResult> callback : callbackSnapshot) {
            dispatchFxCallback(callback, result);
        }
    }

    /**
     * Executes a map callback on the JavaFX application thread.
     *
     * @param fxCallback callback to execute
     * @param result result to pass
     */
    private void dispatchFxCallback(Consumer<PathAnalysisResult> fxCallback, PathAnalysisResult result) {
        if (fxCallback == null) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            fxCallback.accept(result);
        } else {
            Platform.runLater(() -> fxCallback.accept(result));
        }
    }

    /**
     * Checks whether the result contains a usable link-budget summary.
     *
     * @param result path-analysis result
     * @return true if SSB margin can be read
     */
    private boolean hasUsableLinkBudget(PathAnalysisResult result) {
        return result != null
                && result.linkBudgetSummary() != null
                && result.linkBudgetSummary().hasUsableBudget();
    }

    /**
     * Creates a no-profile/no-budget result for failed service calculations.
     *
     * @param request original request
     * @param statusText status shown in the map detail panel
     * @return placeholder path result
     */
    private PathAnalysisResult createNoProfileResult(PathAnalysisRequest request, String statusText) {
        return PathAnalysisResult.noProfile(
                "Reachability",
                request.fromLocator6(),
                request.toLocator6(),
                request.toCallsignRaw(),
                Double.NaN,
                Double.NaN,
                request.homeAntennaHeightMeters(),
                request.targetAntennaHeightMeters(),
                request.frequencyMHz(),
                statusText
        );
    }

    /**
     * Builds a stable calculation key.
     *
     * <p>The key includes station, locators, frequency, antenna heights and link
     * budget settings. If any relevant input changes, a new calculation is allowed.</p>
     */
    private String buildCalculationKey(PathAnalysisRequest request) {
        return normalizeLocator6(request.fromLocator6())
                + "|"
                + normalizeCallsignRaw(request.toCallsignRaw())
                + "|"
                + normalizeLocator6(request.toLocator6())
                + "|"
                + String.format(Locale.US, "%.5f", request.toLatitudeDeg())
                + "|"
                + String.format(Locale.US, "%.5f", request.toLongitudeDeg())
                + "|"
                + String.format(Locale.US, "%.3f", request.frequencyMHz())
                + "|"
                + String.format(Locale.US, "%.1f", request.homeAntennaHeightMeters())
                + "|"
                + String.format(Locale.US, "%.1f", request.targetAntennaHeightMeters())
                + "|"
                + request.linkBudgetSettings();
    }

    private String normalizeCallsignRaw(String callSignRaw) {
        return callSignRaw == null ? "" : callSignRaw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLocator6(String locator) {
        return locator == null ? "" : locator.trim().toUpperCase(Locale.ROOT);
    }



    private static final class ReachabilityThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "reachability-service");
            thread.setDaemon(true);
            return thread;
        }
    }
}