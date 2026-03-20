package kst4contest.controller;

import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kst4contest.logic.PriorityCalculator;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;
import kst4contest.model.ContestSked;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
/**
 * Calculates priority scores off the JavaFX thread and publishes a small UI model.
 *
 * Design goals:
 * - No per-member Platform.runLater flooding.
 * - Score is computed once per callsignRaw (e.g. "SM6VTZ"), even if it exists in multiple chat categories.
 * - A routing hint (preferred ChatCategory) is kept using "last inbound category" if available.
 */
public final class ScoreService {

    public static final int DEFAULT_TOP_N = 15; //how many top places we have?

    /** Force a refresh at least every X ms (some scoring inputs are time dependent). */
    private static final long MAX_SNAPSHOT_AGE_MS = 10_000L;

    private final ChatController controller;
    private final PriorityCalculator priorityCalculator;

    private final AtomicBoolean recomputeRequested = new AtomicBoolean(true);
    private final AtomicReference<ScoreSnapshot> latestSnapshot = new AtomicReference<>(ScoreSnapshot.empty());

    // UI outputs
    private final ObservableList<TopCandidate> topCandidatesFx = FXCollections.observableArrayList();
    private final ReadOnlyDoubleWrapper selectedCallPriorityScore = new ReadOnlyDoubleWrapper(Double.NaN);
    private final LongProperty uiPulse = new SimpleLongProperty(0);

    private volatile String selectedCallSignRaw;
    private volatile long lastComputedEpochMs = 0L;
    private final int topN;

    private final ObjectProperty<ChatMember> selectedChatMember = new SimpleObjectProperty<>(null);


    public ScoreService(ChatController controller, PriorityCalculator priorityCalculator, int topN) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.priorityCalculator = Objects.requireNonNull(priorityCalculator, "priorityCalculator");
        this.topN = topN > 0 ? topN : DEFAULT_TOP_N;
    }

    public ObservableList<TopCandidate> getTopCandidatesFx() {
        return topCandidatesFx;
    }

    public ReadOnlyDoubleProperty selectedCallPriorityScoreProperty() {
        return selectedCallPriorityScore.getReadOnlyProperty();
    }

    /**
     * A lightweight UI invalidation signal that increments after every published snapshot.
     * Consumers can refresh small panels (timeline/toplist), but should avoid refreshing huge tables.
     */
    public LongProperty uiPulseProperty() {
        return uiPulse;
    }

    public ScoreSnapshot getLatestSnapshot() {
        return latestSnapshot.get();
    }

    /** Coalesced recompute request (safe to call frequently from other threads). */
    public void requestRecompute(String reason) {
        recomputeRequested.set(true);
    }

    /** Called by UI when selection changes. */
    public void setSelectedChatMember(ChatMember member) {

        // keep a central selection for UI actions (FurtherInfo buttons, timeline clicks, etc.)
        if (Platform.isFxApplicationThread()) {
            selectedChatMember.set(member);
        } else {
            Platform.runLater(() -> selectedChatMember.set(member));
        }

        selectedCallSignRaw = member == null ? null : normalizeCallRaw(member.getCallSignRaw());

        // Update score immediately from the latest snapshot
        if (Platform.isFxApplicationThread()) {
            updateSelectedScoreFromSnapshot(latestSnapshot.get());
        } else {
            Platform.runLater(() -> updateSelectedScoreFromSnapshot(latestSnapshot.get()));
        }
    }

    /**
     * Called periodically by the scheduler thread.
     * Recomputes only if explicitly requested or if the snapshot is too old.
     */
    public void tick() {
        long now = System.currentTimeMillis();

        boolean shouldRecompute = recomputeRequested.getAndSet(false) || (now - lastComputedEpochMs) > MAX_SNAPSHOT_AGE_MS;
        if (!shouldRecompute) return;

        try {

            // Apply "no reply" strikes (operator pinged via /cq but no inbound line arrived)
            controller.getStationMetricsService().evaluateNoReplyTimeouts(now, controller.getChatPreferences());

            recompute(now);
        } catch (Exception e) {
            System.err.println("[ScoreService] CRITICAL error while recomputing scores");
            e.printStackTrace();
        }
    }

    private void recompute(long nowEpochMs) {

        // Keep sked list clean (must happen on FX thread)
        controller.requestRemoveExpiredSkeds(nowEpochMs);

        final List<ChatMember> members = controller.snapshotChatMembers();
        final List<ContestSked> activeSkeds = controller.snapshotActiveSkeds();
        final ChatPreferences prefs = controller.getChatPreferences();
        final Map<String, ChatCategory> lastInbound = controller.snapshotLastInboundCategoryMap();

        StationMetricsService.Snapshot metricsSnapshot =
                controller.getStationMetricsService().snapshot(nowEpochMs, prefs);

        // 1) Choose one representative per callsignRaw
        Map<String, ChatMember> representativeByCallRaw = chooseRepresentativeMembers(members, lastInbound);

        // 2) Compute score once per callsignRaw
        Map<String, Double> scoreByCallRaw = new HashMap<>(representativeByCallRaw.size());
        Map<String, ChatCategory> preferredCategoryByCallRaw = new HashMap<>(representativeByCallRaw.size());
        List<TopCandidate> topAll = new ArrayList<>(representativeByCallRaw.size());

        for (Map.Entry<String, ChatMember> e : representativeByCallRaw.entrySet()) {
            String callRaw = e.getKey();
            ChatMember representative = e.getValue();
            if (representative == null) continue;

            double score = priorityCalculator.calculatePriority(
                    representative,
                    prefs,
                    activeSkeds,
                    metricsSnapshot,
                    nowEpochMs
            );

            scoreByCallRaw.put(callRaw, score);
            preferredCategoryByCallRaw.put(callRaw, representative.getChatCategory());

            topAll.add(new TopCandidate(callRaw, representative.getCallSign(), representative.getChatCategory(), score));
        }

        // 3) Build Top-N
        topAll.sort(Comparator.comparingDouble(TopCandidate::getScore).reversed());
        List<TopCandidate> topNList = topAll.size() <= topN ? topAll : new ArrayList<>(topAll.subList(0, topN));

        ScoreSnapshot snap = new ScoreSnapshot(
                nowEpochMs,
                Collections.unmodifiableMap(scoreByCallRaw),
                Collections.unmodifiableMap(preferredCategoryByCallRaw),
                Collections.unmodifiableList(topNList)
        );

        latestSnapshot.set(snap);
        lastComputedEpochMs = nowEpochMs;

        // 4) Publish to UI in ONE batched runLater
        Platform.runLater(() -> {
            topCandidatesFx.setAll(snap.getTopCandidates());
            updateSelectedScoreFromSnapshot(snap);
            uiPulse.set(uiPulse.get() + 1);
        });
    }

    /**
     * Picks one ChatMember object per callsignRaw.
     * Preference order:
     * 1) Variant in last inbound chat category (stable reply routing)
     * 2) Most recently active variant (fallback)
     */
    private Map<String, ChatMember> chooseRepresentativeMembers(
            List<ChatMember> members,
            Map<String, ChatCategory> lastInboundCategoryByCallRaw
    ) {
        Map<String, List<ChatMember>> byCallRaw = new HashMap<>();

        for (ChatMember m : members) {
            if (m == null) continue;
            String callRaw = normalizeCallRaw(m.getCallSignRaw());
            if (callRaw == null || callRaw.isEmpty()) continue;
            byCallRaw.computeIfAbsent(callRaw, k -> new ArrayList<>()).add(m);
        }

        Map<String, ChatMember> representative = new HashMap<>(byCallRaw.size());

        for (Map.Entry<String, List<ChatMember>> entry : byCallRaw.entrySet()) {
            String callRaw = entry.getKey();
            List<ChatMember> variants = entry.getValue();

            ChatCategory preferredCat = lastInboundCategoryByCallRaw.get(callRaw);
            ChatMember chosen = null;

            if (preferredCat != null) {
                for (ChatMember v : variants) {
                    if (v != null && v.getChatCategory() == preferredCat) {
                        chosen = v;
                        break;
                    }
                }
            }

            if (chosen == null) {
                chosen = variants.stream()
                        .filter(Objects::nonNull)
                        .max(Comparator.comparingLong(ChatMember::getActivityTimeLastInEpoch))
                        .orElse(null);
            }

            if (chosen != null) representative.put(callRaw, chosen);
        }

        return representative;
    }

    private void updateSelectedScoreFromSnapshot(ScoreSnapshot snap) {
        if (snap == null || selectedCallSignRaw == null) {
            selectedCallPriorityScore.set(Double.NaN);
            return;
        }
        Double v = snap.getScoreByCallSignRaw().get(selectedCallSignRaw);
        selectedCallPriorityScore.set(v == null ? Double.NaN : v);
    }

    private static String normalizeCallRaw(String callRaw) {
        if (callRaw == null) return null;
        return callRaw.trim().toUpperCase();
    }

    // ------------------------- DTOs -------------------------

    public static final class TopCandidate {
        private final String callSignRaw;
        private final String displayCallSign;
        private final ChatCategory preferredChatCategory;
        private final double score;

        public TopCandidate(String callSignRaw, String displayCallSign, ChatCategory preferredChatCategory, double score) {
            this.callSignRaw = callSignRaw;
            this.displayCallSign = displayCallSign;
            this.preferredChatCategory = preferredChatCategory;
            this.score = score;
        }

        public String getCallSignRaw() { return callSignRaw; }
        public String getDisplayCallSign() { return displayCallSign; }
        public ChatCategory getPreferredChatCategory() { return preferredChatCategory; }
        public double getScore() { return score; }
    }

    public static final class ScoreSnapshot {
        private final long computedAtEpochMs;
        private final Map<String, Double> scoreByCallSignRaw;
        private final Map<String, ChatCategory> preferredCategoryByCallSignRaw;
        private final List<TopCandidate> topCandidates;

        public ScoreSnapshot(long computedAtEpochMs,
                             Map<String, Double> scoreByCallSignRaw,
                             Map<String, ChatCategory> preferredCategoryByCallSignRaw,
                             List<TopCandidate> topCandidates) {
            this.computedAtEpochMs = computedAtEpochMs;
            this.scoreByCallSignRaw = scoreByCallSignRaw;
            this.preferredCategoryByCallSignRaw = preferredCategoryByCallSignRaw;
            this.topCandidates = topCandidates;
        }

        public static ScoreSnapshot empty() {
            return new ScoreSnapshot(System.currentTimeMillis(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
        }

        public long getComputedAtEpochMs() { return computedAtEpochMs; }
        public Map<String, Double> getScoreByCallSignRaw() { return scoreByCallSignRaw; }
        public Map<String, ChatCategory> getPreferredCategoryByCallSignRaw() { return preferredCategoryByCallSignRaw; }
        public List<TopCandidate> getTopCandidates() { return topCandidates; }
    }

    public ReadOnlyObjectProperty<ChatMember> selectedChatMemberProperty() {
        return selectedChatMember;
    }

    public ChatMember getSelectedChatMember() {
        return selectedChatMember.get();
    }
}
