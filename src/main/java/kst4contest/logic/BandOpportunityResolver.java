package kst4contest.logic;

import kst4contest.model.Band;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Resolves band availability and band-upgrade opportunities from one or more
 * active {@link ChatMember} variants of the same base callsign.
 *
 * <p>The resolver deliberately separates a band hint from an exact frequency:
 * {@code knownActiveBands} remains the source for detected QRGs with timestamps,
 * while the station name may add a band without inventing a frequency.</p>
 *
 * <p>A manual NOT-QRV flag always overrides automatic evidence. Worked flags are
 * evaluated separately because an offered band may still be useful for display,
 * even when it is no longer a band-upgrade opportunity.</p>
 */
public final class BandOpportunityResolver {

    public static final long RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS = 30L * 60L * 1000L;

    private static final Map<Band, Pattern> STATION_NAME_BAND_PATTERNS = createStationNameBandPatterns();

    private BandOpportunityResolver() {
    }

    /**
     * Resolves the common band state using the application-wide 30-minute window
     * for frequency evidence. Name-derived band hints remain valid while the
     * ChatMember is present in the active chat model.
     */
    public static Resolution resolve(Collection<ChatMember> variants, long nowEpochMs) {
        return resolve(variants, nowEpochMs, RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS);
    }

    /**
     * Resolves offered, worked and manually excluded bands across all supplied
     * category/callsign variants.
     */
    public static Resolution resolve(Collection<ChatMember> variants,
                                     long nowEpochMs,
                                     long dynamicEvidenceMaxAgeMs) {

        EnumSet<Band> offeredBands = EnumSet.noneOf(Band.class);
        EnumSet<Band> workedBands = EnumSet.noneOf(Band.class);
        EnumSet<Band> notQrvBands = EnumSet.noneOf(Band.class);

        if (variants == null) {
            return new Resolution(offeredBands, workedBands, notQrvBands);
        }

        for (ChatMember member : variants) {
            if (member == null) {
                continue;
            }

            collectRecentFrequencyBands(
                    member,
                    offeredBands,
                    nowEpochMs,
                    dynamicEvidenceMaxAgeMs
            );
            offeredBands.addAll(detectBandsFromStationName(member.getName()));
            collectWorkedBands(member, workedBands);
            collectNotQrvBands(member, notQrvBands);
        }

        return new Resolution(offeredBands, workedBands, notQrvBands);
    }

    /**
     * Returns the bands enabled in the local station setup. Bands above 10 GHz
     * remain excluded because the current preferences do not provide active-band
     * flags for them.
     */
    public static EnumSet<Band> getEnabledStationBands(ChatPreferences preferences) {
        EnumSet<Band> enabledBands = EnumSet.noneOf(Band.class);
        if (preferences == null) {
            return enabledBands;
        }

        if (preferences.isStn_bandActive50()) enabledBands.add(Band.B_50);
        if (preferences.isStn_bandActive70()) enabledBands.add(Band.B_70);
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
     * Detects explicit amateur-band designators in a station name.
     *
     * <p>The boundary rules are intentionally stricter than simple token splitting.
     * In particular, the trailing {@code 2} in {@code 1.2 cm} must not be mistaken
     * for the 2 m band.</p>
     */
    public static EnumSet<Band> detectBandsFromStationName(String stationName) {
        EnumSet<Band> detectedBands = EnumSet.noneOf(Band.class);
        if (stationName == null || stationName.isBlank()) {
            return detectedBands;
        }

        for (Map.Entry<Band, Pattern> entry : STATION_NAME_BAND_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(stationName).find()) {
                detectedBands.add(entry.getKey());
            }
        }

        return detectedBands;
    }

    private static void collectRecentFrequencyBands(ChatMember member,
                                                    EnumSet<Band> target,
                                                    long nowEpochMs,
                                                    long dynamicEvidenceMaxAgeMs) {
        if (member.getKnownActiveBands() == null || member.getKnownActiveBands().isEmpty()) {
            return;
        }

        for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> entry
                : member.getKnownActiveBands().entrySet()) {

            Band band = entry.getKey();
            ChatMember.ActiveFrequencyInfo info = entry.getValue();
            if (band == null || info == null) {
                continue;
            }

            long ageMs = nowEpochMs - info.timestampEpoch;
            boolean ageAccepted = dynamicEvidenceMaxAgeMs <= 0L
                    ? ageMs >= 0L
                    : ageMs >= 0L && ageMs <= dynamicEvidenceMaxAgeMs;

            if (ageAccepted) {
                target.add(band);
            }
        }
    }

    private static void collectWorkedBands(ChatMember member, EnumSet<Band> target) {
        if (member.isWorked50()) target.add(Band.B_50);
        if (member.isWorked70()) target.add(Band.B_70);
        if (member.isWorked144()) target.add(Band.B_144);
        if (member.isWorked432()) target.add(Band.B_432);
        if (member.isWorked1240()) target.add(Band.B_1296);
        if (member.isWorked2300()) target.add(Band.B_2320);
        if (member.isWorked3400()) target.add(Band.B_3400);
        if (member.isWorked5600()) target.add(Band.B_5760);
        if (member.isWorked10G()) target.add(Band.B_10G);
        if (member.isWorked24G()) target.add(Band.B_24G);
    }

    private static void collectNotQrvBands(ChatMember member, EnumSet<Band> target) {
        if (!member.isQrv50()) target.add(Band.B_50);
        if (!member.isQrv70()) target.add(Band.B_70);
        if (!member.isQrv144()) target.add(Band.B_144);
        if (!member.isQrv432()) target.add(Band.B_432);
        if (!member.isQrv1240()) target.add(Band.B_1296);
        if (!member.isQrv2300()) target.add(Band.B_2320);
        if (!member.isQrv3400()) target.add(Band.B_3400);
        if (!member.isQrv5600()) target.add(Band.B_5760);
        if (!member.isQrv10G()) target.add(Band.B_10G);
        // There is currently no persisted NOT-QRV flag for 24 GHz.
    }

    private static Map<Band, Pattern> createStationNameBandPatterns() {
        Map<Band, Pattern> patterns = new EnumMap<>(Band.class);

        // Bare "70" and bare "6" are already claimed by the 70cm/6cm shorthand below
        // (their "CM" suffix is optional), so 4m/6m must require an explicit MHz/"M"
        // suffix here to avoid misreading a cm-band shorthand as 70/50 MHz.
        patterns.put(Band.B_50, bandPattern("50(?:\\s*MHZ)?|6\\s*M"));
        patterns.put(Band.B_70, bandPattern("70\\s*MHZ|4\\s*M"));
        patterns.put(Band.B_144, bandPattern("144(?:\\s*MHZ)?|2(?:\\s*M)?"));
        patterns.put(Band.B_432, bandPattern("432(?:\\s*MHZ)?|70(?:\\s*CM)?"));
        patterns.put(Band.B_1296, bandPattern("1296(?:\\s*MHZ)?|23(?:\\s*CM)?"));
        patterns.put(Band.B_2320, bandPattern("(?:2300|2320)(?:\\s*MHZ)?|13(?:\\s*CM)?"));
        patterns.put(Band.B_3400, bandPattern("3400(?:\\s*MHZ)?|9(?:\\s*CM)?"));
        patterns.put(Band.B_5760, bandPattern("(?:5600|5760)(?:\\s*MHZ)?|6(?:\\s*CM)?"));
        patterns.put(Band.B_10G, bandPattern("10368(?:\\s*MHZ)?|10\\s*G(?:HZ)?|3(?:\\s*CM)?"));
        patterns.put(Band.B_24G, bandPattern("24048(?:\\s*MHZ)?|24\\s*G(?:HZ)?|1[.,]2(?:\\s*CM)?"));

        return Collections.unmodifiableMap(patterns);
    }

    private static Pattern bandPattern(String alternatives) {
        return Pattern.compile(
                "(?<![A-Z0-9.,])(?:" + alternatives + ")(?![A-Z0-9.,])",
                Pattern.CASE_INSENSITIVE
        );
    }

    /** Immutable result of one callsign-wide band resolution. */
    public static final class Resolution {

        private final EnumSet<Band> offeredBands;
        private final EnumSet<Band> workedBands;
        private final EnumSet<Band> notQrvBands;

        private Resolution(EnumSet<Band> offeredBands,
                           EnumSet<Band> workedBands,
                           EnumSet<Band> notQrvBands) {
            this.offeredBands = copyOf(offeredBands);
            this.workedBands = copyOf(workedBands);
            this.notQrvBands = copyOf(notQrvBands);
        }

        /** Returns all recent/name-derived bands before NOT-QRV is applied. */
        public EnumSet<Band> getOfferedBands() {
            return copyOf(offeredBands);
        }

        public EnumSet<Band> getWorkedBands() {
            return copyOf(workedBands);
        }

        public EnumSet<Band> getNotQrvBands() {
            return copyOf(notQrvBands);
        }

        /** Returns offered bands after manual NOT-QRV exclusions. */
        public EnumSet<Band> getAvailableBands() {
            EnumSet<Band> availableBands = copyOf(offeredBands);
            availableBands.removeAll(notQrvBands);
            return availableBands;
        }

        /** Returns offered, QRV, enabled and not-yet-worked bands. */
        public EnumSet<Band> getUnworkedEnabledBands(EnumSet<Band> enabledBands) {
            EnumSet<Band> opportunities = getAvailableBands();
            if (enabledBands == null || enabledBands.isEmpty()) {
                opportunities.clear();
                return opportunities;
            }

            opportunities.retainAll(enabledBands);
            opportunities.removeAll(workedBands);
            return opportunities;
        }

        public boolean hasBandEvidence() {
            return !offeredBands.isEmpty();
        }

        private static EnumSet<Band> copyOf(EnumSet<Band> source) {
            return source == null || source.isEmpty()
                    ? EnumSet.noneOf(Band.class)
                    : EnumSet.copyOf(source);
        }
    }
}
