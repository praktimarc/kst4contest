package kst4contest.controller;

import kst4contest.logic.SignalDetector;
import kst4contest.model.ChatPreferences;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread-safe metrics store keyed by normalized callsignRaw (e.g. "SM6VTZ").
 *
 * Purpose:
 * - Provide inputs for scoring (momentum, reply time, no-reply strikes, manual sked-fail, positive signals).
 * - Decouple MessageBus / TX from ScoreService (only data flows, no UI calls here).
 */
public final class StationMetricsService {

    /** /cq <CALL> ... */
    private static final Pattern OUTBOUND_CQ_PATTERN = Pattern.compile("(?i)^\\s*/cq\\s+([A-Z0-9/]+)\\b.*");

    /** Rolling window timestamps for momentum scoring. */
    private static final int MAX_STORED_INBOUND_TIMESTAMPS = 32;

    private final ConcurrentHashMap<String, StationMetrics> byCallRaw = new ConcurrentHashMap<>();

    /**
     * Called when the operator sends a message.
     * If it is a "/cq CALL ..." message, this arms a pending ping for response-time / no-reply tracking.
     */
    public Optional<String> tryRecordOutboundCq(String messageText, long nowEpochMs) {
        if (messageText == null) return Optional.empty();

        Matcher m = OUTBOUND_CQ_PATTERN.matcher(messageText.trim());
        if (!m.matches()) return Optional.empty();

        String callRaw = normalizeCallRaw(m.group(1));
        if (callRaw == null || callRaw.isBlank()) return Optional.empty();

        StationMetrics metrics = byCallRaw.computeIfAbsent(callRaw, k -> new StationMetrics());
        synchronized (metrics) {
            metrics.pendingCqSentAtEpochMs = nowEpochMs;
            metrics.lastOutboundCqEpochMs = nowEpochMs;
        }
        return Optional.of(callRaw);
    }

    /**
     * Called for EVERY inbound line from a station (CH or PM).
     * "Any line counts as activity"
     */
    public void onInboundMessage(String senderCallSignRaw,
                                 long nowEpochMs,
                                 String messageText,
                                 ChatPreferences prefs,
                                 String ownCallSignRaw) {

        String callRaw = normalizeCallRaw(senderCallSignRaw);
        if (callRaw == null || callRaw.isBlank()) return;

        // ignore own echoed messages
        if (ownCallSignRaw != null && callRaw.equalsIgnoreCase(normalizeCallRaw(ownCallSignRaw))) return;

        StationMetrics metrics = byCallRaw.computeIfAbsent(callRaw, k -> new StationMetrics());
        synchronized (metrics) {
            metrics.lastInboundEpochMs = nowEpochMs;

            // rolling timestamps (momentum)
            metrics.recentInboundEpochMs.addLast(nowEpochMs);
            while (metrics.recentInboundEpochMs.size() > MAX_STORED_INBOUND_TIMESTAMPS) {
                metrics.recentInboundEpochMs.removeFirst();
            }

            // positive signal detection (extendable by prefs)
            if (messageText != null && prefs != null) {
                if (SignalDetector.containsPositiveSignal(messageText, prefs.getNotify_positiveSignalsPatterns())) {
                    metrics.lastPositiveSignalEpochMs = nowEpochMs;
                }
            }

            // response time measurement: any inbound line ends a pending ping
            if (metrics.pendingCqSentAtEpochMs > 0) {
                long rttMs = Math.max(0, nowEpochMs - metrics.pendingCqSentAtEpochMs);
                metrics.pendingCqSentAtEpochMs = 0;

                // EWMA for response time (stable, no spikes)
                final double alpha = 0.25;
                if (metrics.avgResponseTimeMs <= 0) {
                    metrics.avgResponseTimeMs = rttMs;
                } else {
                    metrics.avgResponseTimeMs = alpha * rttMs + (1.0 - alpha) * metrics.avgResponseTimeMs;
                }
            }
        }
    }

    /**
     * Called periodically (e.g. from ScoreService.tick()).
     * Applies a "no reply" strike if the pending ping is older than prefs timeout.
     */
    public void evaluateNoReplyTimeouts(long nowEpochMs, ChatPreferences prefs) {
        if (prefs == null) return;
        long timeoutMs = Math.max(1, prefs.getNotify_noReplyPenaltyMinutes()) * 60_000L;

        for (Map.Entry<String, StationMetrics> e : byCallRaw.entrySet()) {
            StationMetrics metrics = e.getValue();
            if (metrics == null) continue;

            synchronized (metrics) {
                if (metrics.pendingCqSentAtEpochMs <= 0) continue;

                long age = nowEpochMs - metrics.pendingCqSentAtEpochMs;
                if (age >= timeoutMs) {
                    metrics.pendingCqSentAtEpochMs = 0;
                    metrics.noReplyStrikes++;
                    metrics.lastNoReplyStrikeEpochMs = nowEpochMs;
                }
            }
        }
    }

    /** Manual sked fail: permanent until reset. */
    public void markManualSkedFail(String callSignRaw) {
        String callRaw = normalizeCallRaw(callSignRaw);
        if (callRaw == null || callRaw.isBlank()) return;

        StationMetrics metrics = byCallRaw.computeIfAbsent(callRaw, k -> new StationMetrics());
        synchronized (metrics) {
            metrics.manualSkedFailed = true;
            metrics.manualSkedFailCount++;
        }
    }

    public void resetManualSkedFail(String callSignRaw) {
        String callRaw = normalizeCallRaw(callSignRaw);
        if (callRaw == null || callRaw.isBlank()) return;

        StationMetrics metrics = byCallRaw.computeIfAbsent(callRaw, k -> new StationMetrics());
        synchronized (metrics) {
            metrics.manualSkedFailed = false;
            metrics.manualSkedFailCount = 0;
        }
    }

    public boolean isManualSkedFailed(String callSignRaw) {
        String callRaw = normalizeCallRaw(callSignRaw);
        if (callRaw == null || callRaw.isBlank()) return false;

        StationMetrics metrics = byCallRaw.get(callRaw);
        if (metrics == null) return false;

        synchronized (metrics) {
            return metrics.manualSkedFailed;
        }
    }

    /** Immutable snapshot for scoring */
    public Snapshot snapshot(long nowEpochMs, ChatPreferences prefs) {
        long momentumWindowMs = (prefs != null ? prefs.getNotify_momentumWindowSeconds() : 180) * 1000L;

        Snapshot snap = new Snapshot(nowEpochMs, momentumWindowMs);
        for (Map.Entry<String, StationMetrics> e : byCallRaw.entrySet()) {
            String callRaw = e.getKey();
            StationMetrics m = e.getValue();
            if (m == null) continue;

            synchronized (m) {
                snap.byCallRaw.put(callRaw, new Snapshot.Metrics(
                        m.lastInboundEpochMs,
                        countRecent(m.recentInboundEpochMs, nowEpochMs, momentumWindowMs),
                        m.avgResponseTimeMs,
                        m.noReplyStrikes,
                        m.manualSkedFailed,
                        m.manualSkedFailCount,
                        m.lastPositiveSignalEpochMs
                ));
            }
        }
        return snap;
    }

    private static int countRecent(Deque<Long> timestamps, long nowEpochMs, long windowMs) {
        if (timestamps == null || timestamps.isEmpty()) return 0;
        int cnt = 0;
        for (Long t : timestamps) {
            if (t == null) continue;
            if (nowEpochMs - t <= windowMs) cnt++;
        }
        return cnt;
    }

    private static String normalizeCallRaw(String s) {
        if (s == null) return null;
        return s.trim().toUpperCase();
    }

    private static final class StationMetrics {
        long lastInboundEpochMs;
        long lastOutboundCqEpochMs;

        long pendingCqSentAtEpochMs; // 0 = none
        int noReplyStrikes;
        long lastNoReplyStrikeEpochMs;

        double avgResponseTimeMs; // EWMA
        final Deque<Long> recentInboundEpochMs = new ArrayDeque<>();

        long lastPositiveSignalEpochMs;

        boolean manualSkedFailed;
        int manualSkedFailCount;
    }

    public static final class Snapshot {
        private final long snapshotEpochMs;
        private final long momentumWindowMs;
        private final ConcurrentHashMap<String, Metrics> byCallRaw = new ConcurrentHashMap<>();

        private Snapshot(long snapshotEpochMs, long momentumWindowMs) {
            this.snapshotEpochMs = snapshotEpochMs;
            this.momentumWindowMs = momentumWindowMs;
        }

        public Metrics get(String callSignRaw) {
            if (callSignRaw == null) return null;
            return byCallRaw.get(normalizeCallRaw(callSignRaw));
        }

        public long getSnapshotEpochMs() {
            return snapshotEpochMs;
        }

        public long getMomentumWindowMs() {
            return momentumWindowMs;
        }

        public static final class Metrics {
            public final long lastInboundEpochMs;
            public final int inboundCountInWindow;
            public final double avgResponseTimeMs;
            public final int noReplyStrikes;
            public final boolean manualSkedFailed;
            public final int manualSkedFailCount;
            public final long lastPositiveSignalEpochMs;

            public Metrics(long lastInboundEpochMs,
                           int inboundCountInWindow,
                           double avgResponseTimeMs,
                           int noReplyStrikes,
                           boolean manualSkedFailed,
                           int manualSkedFailCount,
                           long lastPositiveSignalEpochMs) {

                this.lastInboundEpochMs = lastInboundEpochMs;
                this.inboundCountInWindow = inboundCountInWindow;
                this.avgResponseTimeMs = avgResponseTimeMs;
                this.noReplyStrikes = noReplyStrikes;
                this.manualSkedFailed = manualSkedFailed;
                this.manualSkedFailCount = manualSkedFailCount;
                this.lastPositiveSignalEpochMs = lastPositiveSignalEpochMs;
            }
        }
    }
}
