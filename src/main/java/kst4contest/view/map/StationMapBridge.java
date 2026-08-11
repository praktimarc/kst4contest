package kst4contest.view.map;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import kst4contest.controller.ChatController;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import kst4contest.model.Band;
import java.util.function.Predicate;

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


    private final AtomicLong pathAnalysisGeneration = new AtomicLong(0);

    private final ChatController chatController;
    private final TableView<ChatMember> chatMemberTable;
    private final StationMapView stationMapView;
    private final Consumer<ChatMember> focusChatMemberConsumer;
    private final Supplier<Band> reachabilityBandOverrideSupplier;





    private final MapCallsignRawSnapshotBuilder snapshotBuilder = new MapCallsignRawSnapshotBuilder();


    private String lastPathAnalysisRequestSignature = "";

    private final PauseTransition refreshCoalescer = new PauseTransition(Duration.seconds(1.0));

    public StationMapBridge(ChatController chatController,
                            TableView<ChatMember> chatMemberTable,
                            StationMapView stationMapView,
                            Consumer<ChatMember> focusChatMemberConsumer,
                            Supplier<Band> reachabilityBandOverrideSupplier) {

        this.chatController = Objects.requireNonNull(chatController, "chatController");
        this.chatMemberTable = Objects.requireNonNull(chatMemberTable, "chatMemberTable");
        this.stationMapView = Objects.requireNonNull(stationMapView, "stationMapView");
        this.focusChatMemberConsumer = Objects.requireNonNull(
                focusChatMemberConsumer,
                "focusChatMemberConsumer"
        );
        this.reachabilityBandOverrideSupplier = Objects.requireNonNull(
                reachabilityBandOverrideSupplier,
                "reachabilityBandOverrideSupplier"
        );

        this.refreshCoalescer.setOnFinished(event -> refreshNow());
    }

    public void install() {
        stationMapView.setOnCallsignRawSelected(this::handleMapCallsignSelection);
        stationMapView.setOnTriggerClusterSpot(this::handleExplicitClusterSpot);

        stationMapView.setOnResetView(this::handleMapReset);

        chatController.getLst_chatMemberSortedFilteredList().addListener(
                (ListChangeListener<ChatMember>) change -> scheduleRefresh()
        );

        chatController.getScoreService().selectedChatMemberProperty().addListener(
                (obs, oldValue, newValue) -> requestImmediateRefresh()
        );

        chatController.getChatPreferences().getActualQTF().addListener(
                (obs, oldValue, newValue) -> scheduleRefresh()
        );

        chatController.getLst_chatMemberListFilterPredicates().addListener(
                (ListChangeListener<Predicate<ChatMember>>) change -> requestImmediateRefresh()
        );

        requestImmediateRefresh();
    }

    private void handleMapReset() {
        Runnable resetAction = () -> {
            /*
             * Ignore an analysis result that may still arrive for the
             * previously selected station.
             */
            pathAnalysisGeneration.incrementAndGet();
            lastPathAnalysisRequestSignature = "";

            /*
             * Clear the application's central station selection.
             */
            chatController.getScoreService().setSelectedChatMember(null);

            /*
             * Keep the main station table synchronized with the central selection.
             */
            chatMemberTable.getSelectionModel().clearSelection();

            requestImmediateRefresh();
        };

        if (Platform.isFxApplicationThread()) {
            resetAction.run();
        } else {
            Platform.runLater(resetAction);
        }
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

    /**
     * Forces the currently selected map path to be requested again.
     *
     * <p>This is used by the explicit "Calc selected" action after the operator
     * changed the reachability band. Merely changing the ComboBox still does not
     * trigger terrain analysis.</p>
     */
    public void requestSelectedPathAnalysisRefresh() {
        lastPathAnalysisRequestSignature = "";
        requestImmediateRefresh();
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
        EnumSet<Band> selectedBands = chatController.getReachabilityService().getEnabledStationBands();

        List<MapCallsignRawSnapshot> snapshots = snapshotBuilder.buildSnapshots(
                visibleChatMembers,
                selectedChatMember,
                selectedBands
        );

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

    /**
     * Requests the selected station path analysis through the central reachability
     * service.
     *
     * <p>The map no longer owns a separate PathAnalysisService. This ensures that
     * the map detail panel, the Tropo table column and the station filters all use
     * exactly the same path/link-budget calculation.</p>
     *
     * @param ownLocator6 own locator from preferences
     * @param selectedSnapshot selected map marker snapshot
     */
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

        ChatMember selectedMember = resolveBestChatMember(targetCallsignRaw);

        Band requestedBandOverride = reachabilityBandOverrideSupplier.get();

        chatController.getReachabilityService().requestPathAnalysisForMap(
                selectedMember,
                selectedSnapshot,
                requestedBandOverride,
                result -> {
                    if (generation != pathAnalysisGeneration.get()) {
                        return;
                    }
                    stationMapView.setPathAnalysisResult(result);
                }
        );

    }

    /**
     * Invalidates pending map callbacks.
     *
     * <p>The actual calculation executor is owned by ReachabilityService now, so
     * this bridge no longer shuts down any path-analysis thread directly.</p>
     */
    public void dispose() {
        pathAnalysisGeneration.incrementAndGet();
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

            /*
             * A map click is an explicit operator action. Clear the signature so a
             * newly selected reachability band is honored even when the same station
             * is clicked again.
             */
            lastPathAnalysisRequestSignature = "";
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


    private double resolveAnalysisFrequencyMHz(MapCallsignRawSnapshot selectedSnapshot) {
        if (selectedSnapshot == null) {
            return Double.NaN;
        }

        ChatMember selectedMember = resolveBestChatMember(selectedSnapshot.callSignRaw());
        var resolution = chatController.getReachabilityService()
                .resolveAutomaticPropagationFrequency(selectedMember);

        return resolution == null
                ? Double.NaN
                : resolution.getAnalysisFrequencyMHz();
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
                + selectedSnapshot.bandSummary()
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


}