package kst4contest.view.map;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import kst4contest.controller.ChatController;
import kst4contest.locatorUtils.Location;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Synchronizes the application state with the station map window.
 *
 * Responsibilities:
 * - observes the visible filtered table content
 * - observes the central selection
 * - forwards marker clicks back into the main application
 * - triggers explicit DXCluster spots from the map detail panel
 */
public final class StationMapBridge {

    private final ExecutorService pathAnalysisExecutor = Executors.newSingleThreadExecutor(new PathAnalysisThreadFactory());
    private final AtomicLong pathAnalysisGeneration = new AtomicLong(0);

    private final ChatController chatController;
    private final TableView<ChatMember> chatMemberTable;
    private final StationMapView stationMapView;
    private final Consumer<ChatMember> focusChatMemberConsumer;





    private final MapCallsignRawSnapshotBuilder snapshotBuilder = new MapCallsignRawSnapshotBuilder();
//    private final OfflineDemManager offlineDemManager = new OfflineDemManager();
    private final PathAnalysisService pathAnalysisService;

    private String lastPathAnalysisRequestSignature = "";

    private final PauseTransition refreshCoalescer = new PauseTransition(Duration.seconds(1.0));

    public StationMapBridge(ChatController chatController,
                            TableView<ChatMember> chatMemberTable,
                            StationMapView stationMapView,
                            Consumer<ChatMember> focusChatMemberConsumer) {

        this.chatController = Objects.requireNonNull(chatController, "chatController");
        this.chatMemberTable = Objects.requireNonNull(chatMemberTable, "chatMemberTable");
        this.stationMapView = Objects.requireNonNull(stationMapView, "stationMapView");
        this.focusChatMemberConsumer = Objects.requireNonNull(focusChatMemberConsumer, "focusChatMemberConsumer");

        this.refreshCoalescer.setOnFinished(event -> refreshNow());

        this.pathAnalysisService = new GeometryOnlyPathAnalysisService(
                new OpenMeteoTerrainProfileProvider()
        );
    }

    public void install() {
        stationMapView.setOnCallsignRawSelected(this::handleMapCallsignSelection);
        stationMapView.setOnTriggerClusterSpot(this::handleExplicitClusterSpot);

        chatController.getLst_chatMemberSortedFilteredList().addListener(
                (ListChangeListener<ChatMember>) change -> scheduleRefresh()
        );

        chatController.getScoreService().selectedChatMemberProperty().addListener(
                (obs, oldValue, newValue) -> requestImmediateRefresh()
        );

        chatController.getChatPreferences().getActualQTF().addListener(
                (obs, oldValue, newValue) -> scheduleRefresh()
        );

        requestImmediateRefresh();
    }

    public void showWindow() {
        stationMapView.showWindow();
        requestImmediateRefresh();
    }

    public void hideWindow() {
        stationMapView.hideWindow();
    }

    public void toggleWindow() {
        if (stationMapView.isShowing()) {
            hideWindow();
        } else {
            showWindow();
        }
    }

    public void requestImmediateRefresh() {
        if (Platform.isFxApplicationThread()) {
            refreshNow();
        } else {
            Platform.runLater(this::refreshNow);
        }
    }

    public void focusSelectedCallsign() {
        showWindow();

        ChatMember selectedChatMember = chatController.getScoreService().getSelectedChatMember();
        if (selectedChatMember != null && selectedChatMember.getCallSignRaw() != null) {
            stationMapView.focusCallsignRaw(selectedChatMember.getCallSignRaw());
        }
    }

    public void applyThemeFromPreferences() {
        if (Platform.isFxApplicationThread()) {
            stationMapView.applyThemeFromPreferences();
        } else {
            Platform.runLater(stationMapView::applyThemeFromPreferences);
        }
    }

    private void scheduleRefresh() {
        if (Platform.isFxApplicationThread()) {
            refreshCoalescer.playFromStart();
        } else {
            Platform.runLater(() -> refreshCoalescer.playFromStart());
        }
    }

    private void refreshNow() {
        List<ChatMember> visibleChatMembers = new ArrayList<>(chatController.getLst_chatMemberSortedFilteredList());
        ChatMember selectedChatMember = chatController.getScoreService().getSelectedChatMember();

        List<MapCallsignRawSnapshot> snapshots = snapshotBuilder.buildSnapshots(visibleChatMembers, selectedChatMember);

        MapCallsignRawSnapshot selectedSnapshot = null;
        if (selectedChatMember != null && selectedChatMember.getCallSignRaw() != null) {
            String selectedCallsignRaw = normalizeCallsignRaw(selectedChatMember.getCallSignRaw());
            selectedSnapshot = snapshots.stream()
                    .filter(snapshot -> snapshot.callSignRaw().equals(selectedCallsignRaw))
                    .findFirst()
                    .orElse(null);
        }

        boolean filteredViewActive = visibleChatMembers.size() < chatController.getLst_chatMemberList().size();

        ChatPreferences preferences = chatController.getChatPreferences();

        stationMapView.refreshMap(
                snapshots,
                selectedSnapshot,
                preferences.getStn_loginLocatorMainCat(),
                preferences.getActualQTF().get(),
                preferences.getStn_antennaBeamWidthDeg(),
                preferences.getStn_maxQRBDefault(),
                filteredViewActive
        );

        requestPathAnalysisAsync(preferences.getStn_loginLocatorMainCat(), selectedSnapshot);
    }

    private void requestPathAnalysisAsync(String ownLocator6, MapCallsignRawSnapshot selectedSnapshot) {
        String normalizedOwnLocator6 = normalizeLocator6(ownLocator6);

        String requestSignature = buildPathAnalysisRequestSignature(normalizedOwnLocator6, selectedSnapshot);

        if (selectedSnapshot == null) {
            lastPathAnalysisRequestSignature = "";
        } else if (requestSignature.equals(lastPathAnalysisRequestSignature)) {
            return;
        } else {
            lastPathAnalysisRequestSignature = requestSignature;
        }

        if (selectedSnapshot == null) {
            long generation = pathAnalysisGeneration.incrementAndGet();
            Platform.runLater(() -> {
                if (generation == pathAnalysisGeneration.get()) {
                    stationMapView.setPathAnalysisResult(PathAnalysisResult.waitingForSelection(normalizedOwnLocator6));
                }
            });
            return;
        }

        String normalizedTargetLocator6 = normalizeLocator6(selectedSnapshot.locator6());
        String targetCallsignRaw = selectedSnapshot.callSignRaw();

        long generation = pathAnalysisGeneration.incrementAndGet();

        stationMapView.setPathAnalysisResult(
                PathAnalysisResult.loading(normalizedOwnLocator6, normalizedTargetLocator6, targetCallsignRaw)
        );

        pathAnalysisExecutor.submit(() -> {
            PathAnalysisResult result = buildPathAnalysisResult(normalizedOwnLocator6, selectedSnapshot);

            Platform.runLater(() -> {
                if (generation != pathAnalysisGeneration.get()) {
                    return;
                }
                stationMapView.setPathAnalysisResult(result);
            });
        });
    }

    public void dispose() {
        pathAnalysisExecutor.shutdownNow();
    }

    private void handleMapCallsignSelection(String callSignRaw) {
        System.out.println("########################### map selected callsign " + callSignRaw);

        ChatMember resolved = resolveBestChatMember(callSignRaw);
        if (resolved == null) {
            return;
        }

        Platform.runLater(() -> {
            chatController.getScoreService().setSelectedChatMember(resolved);

            chatMemberTable.getSelectionModel().select(resolved);
            chatMemberTable.scrollTo(resolved);

            focusChatMemberConsumer.accept(resolved);
            requestImmediateRefresh();
        });
    }

    private void handleExplicitClusterSpot(String callSignRaw) {
        ChatMember resolved = resolveBestChatMember(callSignRaw);
        if (resolved == null) {
            return;
        }

        if (chatController.getDxClusterServer() != null) {
            chatController.getDxClusterServer().broadcastSingleDXClusterEntryToLoggers(resolved);
        }
    }

    private ChatMember resolveBestChatMember(String callSignRaw) {
        String normalizedCallsignRaw = normalizeCallsignRaw(callSignRaw);
        if (normalizedCallsignRaw.isBlank()) {
            return null;
        }

        ChatMember selectedChatMember = chatController.getScoreService().getSelectedChatMember();
        if (selectedChatMember != null
                && normalizedCallsignRaw.equals(normalizeCallsignRaw(selectedChatMember.getCallSignRaw()))) {
            return selectedChatMember;
        }

        ChatMember visibleBest = chatMemberTable.getItems().stream()
                .filter(chatMember -> chatMember != null)
                .filter(chatMember -> normalizedCallsignRaw.equals(normalizeCallsignRaw(chatMember.getCallSignRaw())))
                .max(Comparator.comparingLong(ChatMember::getActivityTimeLastInEpoch))
                .orElse(null);

        if (visibleBest != null) {
            return visibleBest;
        }

        synchronized (chatController.getLst_chatMemberList()) {
            return chatController.getLst_chatMemberList().stream()
                    .filter(chatMember -> chatMember != null)
                    .filter(chatMember -> normalizedCallsignRaw.equals(normalizeCallsignRaw(chatMember.getCallSignRaw())))
                    .max(Comparator.comparingLong(ChatMember::getActivityTimeLastInEpoch))
                    .orElse(null);
        }
    }

    private String normalizeCallsignRaw(String callSignRaw) {
        if (callSignRaw == null) {
            return "";
        }
        return callSignRaw.trim().toUpperCase(Locale.ROOT);
    }

    private PathAnalysisResult buildPathAnalysisResult(String ownLocator6, MapCallsignRawSnapshot selectedSnapshot) {
        String normalizedOwnLocator6 = normalizeLocator6(ownLocator6);

        if (selectedSnapshot == null) {
            return PathAnalysisResult.waitingForSelection(normalizedOwnLocator6);
        }

        String normalizedTargetLocator6 = normalizeLocator6(selectedSnapshot.locator6());

        if (normalizedOwnLocator6.length() != 6) {
            return PathAnalysisResult.waitingForValidHomeLocator(normalizedOwnLocator6, normalizedTargetLocator6);
        }

        if (!selectedSnapshot.hasUsablePosition()) {
            return PathAnalysisResult.waitingForValidTarget(normalizedOwnLocator6, normalizedTargetLocator6);
        }

        Location homeLocation = new Location(normalizedOwnLocator6);
        double analysisFrequencyMHz = resolveAnalysisFrequencyMHz(selectedSnapshot);

        PathAnalysisRequest request = new PathAnalysisRequest(
                normalizedOwnLocator6,
                homeLocation.getLatitude().toDegrees(),
                homeLocation.getLongitude().toDegrees(),
                selectedSnapshot.callSignRaw(),
                normalizedTargetLocator6,
                selectedSnapshot.latitudeDeg(),
                selectedSnapshot.longitudeDeg(),
                analysisFrequencyMHz,
                chatController.getChatPreferences().getStn_pathAnalysisOwnAntennaHeightMeters(),
                chatController.getChatPreferences().getStn_pathAnalysisDefaultTargetAntennaHeightMeters(),
                PathGeometryUtils.DEFAULT_EFFECTIVE_EARTH_RADIUS_FACTOR,
                chatController.getChatPreferences().buildPathLinkBudgetSettings()
        );

        return pathAnalysisService.analyze(request);
    }

    private double resolveAnalysisFrequencyMHz(MapCallsignRawSnapshot selectedSnapshot) {
        if (selectedSnapshot == null) {
            return PathGeometryUtils.DEFAULT_ANALYSIS_FREQUENCY_MHZ;
        }

        return PathGeometryUtils.resolveAnalysisFrequencyMHz(selectedSnapshot.lastKnownFrequenciesByBand());
    }



    private String buildPathAnalysisRequestSignature(String ownLocator6, MapCallsignRawSnapshot selectedSnapshot) {
        if (selectedSnapshot == null) {
            return "";
        }

        double analysisFrequencyMHz = resolveAnalysisFrequencyMHz(selectedSnapshot);
        ChatPreferences preferences = chatController.getChatPreferences();

        return normalizeLocator6(ownLocator6)
                + "|"
                + selectedSnapshot.callSignRaw()
                + "|"
                + normalizeLocator6(selectedSnapshot.locator6())
                + "|"
                + String.format(Locale.US, "%.5f", selectedSnapshot.latitudeDeg())
                + "|"
                + String.format(Locale.US, "%.5f", selectedSnapshot.longitudeDeg())
                + "|"
                + String.format(Locale.US, "%.3f", analysisFrequencyMHz)
                + "|"
                + String.format(Locale.US, "%.1f", preferences.getStn_pathAnalysisOwnAntennaHeightMeters())
                + "|"
                + String.format(Locale.US, "%.1f", preferences.getStn_pathAnalysisDefaultTargetAntennaHeightMeters())
                + "|"
                + preferences.getStn_pathAnalysisDemRootDirectory()
                + "|"
                + String.format(Locale.US, "%.1f", preferences.getStn_pathAnalysisOwnTxPowerWatts())
                + "|"
                + String.format(Locale.US, "%.2f", preferences.getStn_pathAnalysisOwnAntennaGainDbi())
                + "|"
                + String.format(Locale.US, "%.1f", preferences.getStn_pathAnalysisDefaultTargetTxPowerWatts())
                + "|"
                + String.format(Locale.US, "%.2f", preferences.getStn_pathAnalysisDefaultTargetAntennaGainDbi());
    }

    private String normalizeLocator6(String locator) {
        if (locator == null) {
            return "";
        }
        return locator.trim().toUpperCase(Locale.ROOT);
    }

    private static final class PathAnalysisThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "station-map-path-analysis");
            thread.setDaemon(true);
            return thread;
        }
    }
}