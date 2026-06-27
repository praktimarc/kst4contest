package kst4contest.controller;

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

/**
 * Background service for station reachability values used by the station table.
 *
 * <p>The service performs the expensive full path analysis off the JavaFX thread,
 * stores the bidirectional SSB margin in the ChatMember, and leaves the UI with a
 * simple numeric value for sorting/filtering.</p>
 */
public final class ReachabilityService {

    private final ChatController chatController;
    private final PathAnalysisService pathAnalysisService;
    private final ExecutorService executor;

    /**
     * Prevents duplicate jobs for the same logical station/band while a calculation
     * is already queued or running.
     */
    private final Set<String> queuedCalculationKeys = ConcurrentHashMap.newKeySet();

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
     * Ensures that a station/band SSB-margin value exists. Existing finite values
     * and existing NaN failure markers are not recalculated automatically.
     *
     * @param member chatmember to evaluate
     * @param band band to evaluate
     */
    public void ensureTropoMarginCalculated(ChatMember member, Band band) {
        if (member == null || band == null) {
            return;
        }

        if (member.hasTropoSsbMarginDb(band)) {
            return;
        }

        String calculationKey = buildCalculationKey(member, band);
        if (!queuedCalculationKeys.add(calculationKey)) {
            return;
        }

        executor.submit(() -> {
            double marginDb = Double.NaN;
            try {
                marginDb = calculateSsbMarginDb(member, band);
            } catch (Exception exception) {
                System.out.println("[ReachabilityService, warning]: analysis failed for "
                        + member.getCallSignRaw() + " @" + band + ": " + exception.getMessage());
            }

            final double finalMarginDb = marginDb;
            Runnable storeResult = () -> {
                member.setTropoSsbMarginDb(band, finalMarginDb);
                chatController.fireUserListUpdate("Reachability calculated");
            };

            if (Platform.isFxApplicationThread()) {
                storeResult.run();
            } else {
                Platform.runLater(storeResult);
            }
        });
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
        if (member != null && member.getKnownActiveBands() != null && !member.getKnownActiveBands().isEmpty()) {
            return member.getKnownActiveBands().keySet().stream()
                    .filter(Objects::nonNull)
                    .min(Comparator.comparingDouble(Band::getDefaultAnalysisFrequencyMHz))
                    .orElse(Band.B_144);
        }

        if (member != null
                && member.getChatCategory() != null
                && member.getChatCategory().getCategoryNumber() == ChatCategory.MICROWAVE) {
            return Band.B_1296;
        }

        return Band.B_144;
    }

    /**
     * Returns the active own bands configured in the station preferences. High bands
     * above 10 GHz are intentionally ignored for the first version.
     *
     * @return set of enabled bands
     */
    public EnumSet<Band> getEnabledStationBands() {
        ChatPreferences preferences = chatController.getChatPreferences();
        EnumSet<Band> enabledBands = EnumSet.noneOf(Band.class);

        if (preferences == null) {
            return enabledBands;
        }

        if (preferences.isStn_bandActive144()) enabledBands.add(Band.B_144);
        if (preferences.isStn_bandActive432()) enabledBands.add(Band.B_432);
        if (preferences.isStn_bandActive1240()) enabledBands.add(Band.B_1296);
        if (preferences.isStn_bandActive2300()) enabledBands.add(Band.B_2320);
        if (preferences.isStn_bandActive3400()) enabledBands.add(Band.B_3400);
        if (preferences.isStn_bandActive5600()) enabledBands.add(Band.B_5760);
        if (preferences.isStn_bandActive10G()) enabledBands.add(Band.B_10G);

        return enabledBands;
    }

    /**
     * Stops the background executor.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    private double calculateSsbMarginDb(ChatMember member, Band band) {
        String ownLocator6 = normalizeLocator6(chatController.getChatPreferences().getStn_loginLocatorMainCat());
        String targetLocator6 = normalizeLocator6(member.getQra());

        if (ownLocator6.length() != 6 || targetLocator6.length() != 6) {
            return Double.NaN;
        }

        Location homeLocation = new Location(ownLocator6);
        Location targetLocation = new Location(targetLocator6);

        double analysisFrequencyMHz = resolveAnalysisFrequencyForBand(member, band);

        PathAnalysisRequest request = new PathAnalysisRequest(
                ownLocator6,
                homeLocation.getLatitude().toDegrees(),
                homeLocation.getLongitude().toDegrees(),
                member.getCallSignRaw(),
                targetLocator6,
                targetLocation.getLatitude().toDegrees(),
                targetLocation.getLongitude().toDegrees(),
                analysisFrequencyMHz,
                chatController.getChatPreferences().getStn_pathAnalysisOwnAntennaHeightMeters(),
                chatController.getChatPreferences().getStn_pathAnalysisDefaultTargetAntennaHeightMeters(),
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                chatController.getChatPreferences().buildPathLinkBudgetSettings()
        );

        PathAnalysisResult result = pathAnalysisService.analyze(request);
        if (result == null || result.linkBudgetSummary() == null || !result.linkBudgetSummary().hasUsableBudget()) {
            return Double.NaN;
        }

        return result.linkBudgetSummary().bidirectionalSsbMarginDb();
    }

    private double resolveAnalysisFrequencyForBand(ChatMember member, Band band) {
        if (member != null && member.getKnownActiveBands() != null) {
            ChatMember.ActiveFrequencyInfo activeFrequencyInfo = member.getKnownActiveBands().get(band);
            if (activeFrequencyInfo != null
                    && Double.isFinite(activeFrequencyInfo.frequency)
                    && activeFrequencyInfo.frequency > 0.0) {
                return activeFrequencyInfo.frequency;
            }
        }

        return band.getDefaultAnalysisFrequencyMHz();
    }

    private String buildCalculationKey(ChatMember member, Band band) {
        String callSignRaw = member.getCallSignRaw() == null ? "" : member.getCallSignRaw();
        String locator = member.getQra() == null ? "" : member.getQra();
        return callSignRaw.trim().toUpperCase() + "|" + locator.trim().toUpperCase() + "|" + band.name();
    }

    private String normalizeLocator6(String locator) {
        return locator == null ? "" : locator.trim().toUpperCase();
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