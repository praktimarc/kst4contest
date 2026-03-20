package kst4contest.logic;

import kst4contest.controller.StationMetricsService;
import kst4contest.model.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Priority score calculation (off FX-thread).
 *
 * Notes:
 * - Score is computed once per callsignRaw by ScoreService.
 * - This calculator MUST be pure (no UI calls) and fast.
 */
public class PriorityCalculator {

    /** Max age for "known active bands" (derived from chat history). */
    private static final long RX_BANDS_MAX_AGE_MS = 30L * 60L * 1000L; // 30 minutes

    public double calculatePriority(ChatMember member,
                                    ChatPreferences prefs,
                                    List<ContestSked> activeSkeds,
                                    StationMetricsService.Snapshot metricsSnapshot,
                                    long nowEpochMs) {

        if (member == null || prefs == null) return 0.0;

        final String callRaw = normalize(member.getCallSignRaw());
        if (callRaw == null || callRaw.isBlank()) return 0.0;

        // --------------------------------------------------------------------
        // 1) HARD FILTER: reachable hardware + "already worked on all possible bands"
        // --------------------------------------------------------------------
        // --------------------------------------------------------------------
// 1) HARD FILTER: reachable hardware + "already worked on all possible bands"
// --------------------------------------------------------------------
        EnumSet<Band> myEnabledBands = getMyEnabledBands(prefs);

// "worked" for scoring is derived ONLY from per-band flags (worked144/432/...)
// IMPORTANT: ChatMember.worked is UI-only and NOT used in scoring.
        EnumSet<Band> workedBandsForScoring = getWorkedBands(member);

// Remaining bands that are:
// - recently offered by the station (from knownActiveBands history)
// - enabled at our station
// - NOT worked yet (per-band flags)
// If we do not know offered bands (history empty), this remains empty.
        EnumSet<Band> unworkedPossible = EnumSet.noneOf(Band.class);

        EnumSet<Band> stationOfferedBands = getStationOfferedBandsFromHistory(member, nowEpochMs);
        EnumSet<Band> possibleBands = stationOfferedBands.isEmpty()
                ? EnumSet.noneOf(Band.class) // unknown => don't hard-filter
                : EnumSet.copyOf(stationOfferedBands);

        if (!possibleBands.isEmpty()) {
            possibleBands.retainAll(myEnabledBands);
            if (possibleBands.isEmpty()) {
                // We know their bands, but none of them are enabled at our station.
                return 0.0;
            }

            unworkedPossible = EnumSet.copyOf(possibleBands);
            unworkedPossible.removeAll(workedBandsForScoring);

            // If already worked on all possible bands => no priority on them anymore (contest logic).
            if (unworkedPossible.isEmpty()) {
                return 0.0;
            }
        }

        // --------------------------------------------------------------------
        // 2) BASE SCORE
        // --------------------------------------------------------------------
        double score = 100.0;

//        if (!member.isWorked()) {
//            score += 200.0;
//        }

        //"worked" for scoring is derived ONLY from per-band flags (worked144/432/...)
//        EnumSet<Band> workedBandsForScoring = getWorkedBands(member);

        if (workedBandsForScoring.isEmpty()) {
            score += 200.0; // never worked on any supported band -> higher priority
        } else {
            score -= 150.0; // already worked on at least one band -> lower base priority
        }


        // Multi-band bonus: if they offer >1 possible band and we worked at least one, prefer them
        if (!possibleBands.isEmpty()) {
            int bandCount = possibleBands.size();
            score += (bandCount - 1) * 80.0;
        }

        // Optional: band-upgrade visibility boost
        // If the station is already worked on at least one band, but is still QRV on other unworked enabled band(s),
        // we can optionally add a boost so it remains visible in the list.
        if (prefs.isNotify_bandUpgradePriorityBoostEnabled()
                && !workedBandsForScoring.isEmpty()
                && !unworkedPossible.isEmpty()) {
            score += 180.0; // tuned visibility boost
        }

        // --------------------------------------------------------------------
        // 3) DISTANCE ("Goldilocks Zone")
        // --------------------------------------------------------------------
        double distKm = member.getQrb() == null ? 0.0 : member.getQrb();

        if (distKm > 0) {
            if (distKm < 200) {
                score *= 0.7;
            } else if (distKm > prefs.getStn_maxQRBDefault()) {
                score *= 0.3;
            } else {
                score *= 1.15;
            }
        }

        // --------------------------------------------------------------------
        // 4) AIRSCOUT BOOST
        // --------------------------------------------------------------------
        AirPlaneReflectionInfo apInfo = member.getAirPlaneReflectInfo();
        if (apInfo != null && apInfo.getAirPlanesReachableCntr() > 0) {
            score += 200;

            int nextMinutes = findNextAirplaneArrivingMinutes(apInfo);
            if (nextMinutes == 0) score += 120;
            else if (nextMinutes == 1) score += 60;
            else if (nextMinutes == 2) score += 30;
        }

        // --------------------------------------------------------------------
        // 5) BOOST IDEA #1: Beam direction match (within beamwidth)
        // --------------------------------------------------------------------
        if (member.getQTFdirection() != null) {
            double myAz = prefs.getActualQTF().getValue();
            double targetAz = member.getQTFdirection();
            double diff = minimalAngleDiffDeg(myAz, targetAz);

            double halfBeam = Math.max(1.0, prefs.getStn_antennaBeamWidthDeg()) / 2.0;
            if (diff <= halfBeam) {
                double centerFactor = 1.0 - (diff / halfBeam); // 1.0 center -> 0.0 edge
                score += 80.0 + (120.0 * centerFactor);
            }
        }

        // --------------------------------------------------------------------
        // 6) BOOST IDEA #3: Conversation momentum (recent inbound burst)
        // --------------------------------------------------------------------
        if (metricsSnapshot != null) {
            StationMetricsService.Snapshot.Metrics mx = metricsSnapshot.get(callRaw);
            if (mx != null) {
                long ageMs = mx.lastInboundEpochMs > 0 ? (nowEpochMs - mx.lastInboundEpochMs) : Long.MAX_VALUE;

                // "Active now" bonus
                if (ageMs < 60_000) score += 120;
                else if (ageMs < 3 * 60_000) score += 60;

                // Momentum bonus: multiple lines in the configured window
                int cnt = mx.inboundCountInWindow;
                if (cnt >= 6) score += 160;
                else if (cnt >= 4) score += 110;
                else if (cnt >= 2) score += 60;

                // Positive signal (configurable)
                if (mx.lastPositiveSignalEpochMs > 0 && (nowEpochMs - mx.lastPositiveSignalEpochMs) < 5 * 60_000) {
                    score += 120;
                }

                // Reply time: prefer fast responders
                if (mx.avgResponseTimeMs > 0) {
                    if (mx.avgResponseTimeMs < 60_000) score += 80;
                    else if (mx.avgResponseTimeMs < 3 * 60_000) score += 40;
                }

                // No-reply penalty (automatic failed attempt)
                if (mx.noReplyStrikes > 0) {
                    score /= (1.0 + (mx.noReplyStrikes * 0.6));
                }

                // Manual sked fail (path likely bad) => strong, permanent penalty until reset
                if (mx.manualSkedFailed) {
                    score *= 0.15;
                }
            }
        }

        // --------------------------------------------------------------------
        // 7) BOOST IDEA #4: Sked commitment ramp-up
        // --------------------------------------------------------------------
        if (activeSkeds != null && !activeSkeds.isEmpty()) {
            for (ContestSked sked : activeSkeds) {
                if (sked == null) continue;

                if (!callRaw.equals(normalize(sked.getTargetCallsign()))) continue;

                long seconds = sked.getTimeUntilSkedSeconds();

                // Imminent sked: absolute priority (T-3min..T+1min)
                if (seconds < 180 && seconds > -60) {
                    score += 5000;
                    continue;
                }

                // Ramp: 0..15 minutes before => up to +1200
                if (seconds >= 0 && seconds <= 15 * 60) {
                    double t = (15 * 60 - seconds) / (15.0 * 60.0); // 0.0..1.0
                    score += 300 + (900 * t);
                } else if (seconds > 15 * 60) {
                    score += 40;
                }
            }
        }

        // --------------------------------------------------------------------
        // 8) Legacy penalty: failed attempts in ChatMember
        // --------------------------------------------------------------------
        if (member.getFailedQSOAttempts() > 0) {
            score = score / (member.getFailedQSOAttempts() + 1);
        }

        return Math.max(0.0, score);
    }

    private static EnumSet<Band> getMyEnabledBands(ChatPreferences prefs) {
        EnumSet<Band> out = EnumSet.noneOf(Band.class);
        if (prefs.isStn_bandActive144()) out.add(Band.B_144);
        if (prefs.isStn_bandActive432()) out.add(Band.B_432);
        if (prefs.isStn_bandActive1240()) out.add(Band.B_1296);
        if (prefs.isStn_bandActive2300()) out.add(Band.B_2320);
        if (prefs.isStn_bandActive3400()) out.add(Band.B_3400);
        if (prefs.isStn_bandActive5600()) out.add(Band.B_5760);
        if (prefs.isStn_bandActive10G()) out.add(Band.B_10G);
        return out;
    }

    private static EnumSet<Band> getStationOfferedBandsFromHistory(ChatMember member, long nowEpochMs) {
        EnumSet<Band> out = EnumSet.noneOf(Band.class);
        Map<Band, ChatMember.ActiveFrequencyInfo> map = member.getKnownActiveBands();
        if (map == null || map.isEmpty()) return out;

        for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> e : map.entrySet()) {
            if (e == null || e.getKey() == null || e.getValue() == null) continue;
            long age = nowEpochMs - e.getValue().timestampEpoch;
            if (age <= RX_BANDS_MAX_AGE_MS) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    private static EnumSet<Band> getWorkedBands(ChatMember member) {
        EnumSet<Band> out = EnumSet.noneOf(Band.class);
        if (member.isWorked144()) out.add(Band.B_144);
        if (member.isWorked432()) out.add(Band.B_432);
        if (member.isWorked1240()) out.add(Band.B_1296);
        if (member.isWorked2300()) out.add(Band.B_2320);
        if (member.isWorked3400()) out.add(Band.B_3400);
        if (member.isWorked5600()) out.add(Band.B_5760);
        if (member.isWorked10G()) out.add(Band.B_10G);
        if (member.isWorked24G()) out.add(Band.B_24G);
        return out;
    }

    private static int findNextAirplaneArrivingMinutes(AirPlaneReflectionInfo apInfo) {
        try {
            if (apInfo.getRisingAirplanes() == null || apInfo.getRisingAirplanes().isEmpty()) return -1;

            int min = Integer.MAX_VALUE;
            for (AirPlane ap : apInfo.getRisingAirplanes()) {
                if (ap == null) continue;
                min = Math.min(min, ap.getArrivingDurationMinutes());
            }
            return min == Integer.MAX_VALUE ? -1 : min;
        } catch (Exception ignore) {
            return -1;
        }
    }

    private static double minimalAngleDiffDeg(double a, double b) {
        double diff = Math.abs((a - b) % 360.0);
        return diff > 180.0 ? 360.0 - diff : diff;
    }

    private static String normalize(String s) {
        if (s == null) return null;
        return s.trim().toUpperCase();
    }
}
