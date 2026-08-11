package kst4contest.logic;

import kst4contest.model.Band;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Selects one realistic propagation frequency for a station.
 *
 * <p>The same resolution is used by AirScout and by the internal path analysis.
 * Only the chat categories supported by these features participate. This keeps
 * unrelated KST categories from silently falling back to 144 MHz.</p>
 */
public final class PropagationFrequencyResolver {

//    private static final double DUAL_VUHF_MICROWAVE_FALLBACK_MHZ = 430.0;

    private PropagationFrequencyResolver() {
    }

    /** Explains why a frequency was selected. */
    public enum Source {
        CURRENT_QRG,
        STATION_NAME_QRG,
        STATION_NAME,
        DUAL_CATEGORY_FALLBACK,
        CHAT_CATEGORY
    }

    /**
     * Resolves the frequency from all active category variants of one base
     * callsign.
     *
     * <ol>
     *     <li>Most recently detected QRG</li>
     *     <li>Lowest band explicitly named by the station</li>
     *     <li>432 MHz if the station is present in VUHF and Microwave</li>
     *     <li>Lowest usable fallback band of the supported chat category</li>
     * </ol>
     *
     * <p>Locally disabled and manually excluded bands are never selected.</p>
     *
     * @param variants active category variants of one callsign
     * @param enabledBands bands enabled for the local station
     * @param nowEpochMs current time used for the QRG age check
     * @return one resolution, or {@code null} if no safe choice exists
     */
    public static Resolution resolve(Collection<ChatMember> variants,
                                     EnumSet<Band> enabledBands,
                                     long nowEpochMs) {

        if (variants == null || variants.isEmpty()
                || enabledBands == null || enabledBands.isEmpty()) {
            return null;
        }

        List<ChatMember> supportedVariants = variants.stream()
                .filter(PropagationFrequencyResolver::isSupportedVariant)
                .toList();

        if (supportedVariants.isEmpty()) {
            return null;
        }

        BandOpportunityResolver.Resolution opportunityResolution =
                BandOpportunityResolver.resolve(supportedVariants, nowEpochMs);

        EnumSet<Band> usableBands = EnumSet.copyOf(enabledBands);
        usableBands.removeAll(opportunityResolution.getNotQrvBands());

        if (usableBands.isEmpty()) {
            return null;
        }

        FrequencyCandidate latestQrg = findLatestQrg(
                supportedVariants,
                usableBands,
                nowEpochMs
        );

        if (latestQrg != null) {
            return new Resolution(
                    latestQrg.band,
                    latestQrg.frequencyMHz,
                    Source.CURRENT_QRG
            );
        }

        FrequencyCandidate stationNameQrg =
                findUniqueStationNameQrg(
                        supportedVariants,
                        usableBands
                );

        if (stationNameQrg != null) {
            return new Resolution(
                    stationNameQrg.band,
                    stationNameQrg.frequencyMHz,
                    Source.STATION_NAME_QRG
            );
        }

        EnumSet<Band> nameBands = EnumSet.noneOf(Band.class);
        for (ChatMember variant : supportedVariants) {
            nameBands.addAll(
                    BandOpportunityResolver.detectBandsFromStationName(variant.getName())
            );
        }
        nameBands.retainAll(usableBands);

        Band nameBand = lowestBand(nameBands);
        if (nameBand != null) {
            return new Resolution(
                    nameBand,
                    nameBand.getDefaultAnalysisFrequencyMHz(),
                    Source.STATION_NAME
            );
        }

        EnumSet<SupportedCategory> categories = collectSupportedCategories(supportedVariants);

        if (categories.contains(SupportedCategory.VUHF)
                && categories.contains(SupportedCategory.MICROWAVE)
                && usableBands.contains(Band.B_432)) {
            return new Resolution(
                    Band.B_432,
                    Band.B_432.getDefaultAnalysisFrequencyMHz(),
                    Source.DUAL_CATEGORY_FALLBACK
            );
        }

        EnumSet<Band> categoryBands = EnumSet.noneOf(Band.class);
        for (SupportedCategory category : categories) {
            categoryBands.addAll(category.fallbackBands);
        }
        categoryBands.retainAll(usableBands);

        Band categoryBand = lowestBand(categoryBands);
        if (categoryBand == null) {
            return null;
        }

        return new Resolution(
                categoryBand,
                categoryBand.getDefaultAnalysisFrequencyMHz(),
                Source.CHAT_CATEGORY
        );
    }

    private static FrequencyCandidate findLatestQrg(List<ChatMember> variants,
                                                    EnumSet<Band> usableBands,
                                                    long nowEpochMs) {
        FrequencyCandidate latest = null;

        for (ChatMember variant : variants) {
            for (var entry : variant.getKnownActiveBands().entrySet()) {
                Band band = entry.getKey();
                ChatMember.ActiveFrequencyInfo info = entry.getValue();

                if (band == null || info == null || !usableBands.contains(band)) {
                    continue;
                }

                long ageMs = nowEpochMs - info.timestampEpoch;
                if (ageMs < 0L
                        || ageMs > BandOpportunityResolver.RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS
                        || !Double.isFinite(info.frequency)
                        || !band.isPlausible(info.frequency)) {
                    continue;
                }

                if (latest == null || info.timestampEpoch > latest.timestampEpochMs) {
                    latest = new FrequencyCandidate(
                            band,
                            info.frequency,
                            info.timestampEpoch
                    );
                }
            }
        }

        return latest;
    }


    /**
     * Resolves one exact station-name QRG only when the evidence is unambiguous.
     *
     * <p>The same QRG repeated in several category variants counts only once.
     * If different explicit QRGs are advertised, no run frequency is guessed and
     * the caller continues with normal band-name/category resolution.</p>
     */
    private static FrequencyCandidate findUniqueStationNameQrg(
            List<ChatMember> variants,
            EnumSet<Band> usableBands
    ) {
        java.util.LinkedHashMap<String, FrequencyCandidate> uniqueCandidates =
                new java.util.LinkedHashMap<>();

        for (ChatMember variant : variants) {
            if (variant == null) {
                continue;
            }

            for (FrequencyTextParser.DetectedFrequency detectedFrequency
                    : FrequencyTextParser.findExplicitFrequencies(
                    variant.getName()
            )) {

                Band band =
                        detectedFrequency.getBand();

                if (!usableBands.contains(band)) {
                    continue;
                }

                double frequencyMHz =
                        detectedFrequency.getFrequencyMHz();

                String key =
                        band.name()
                                + "|"
                                + Double.toString(frequencyMHz);

                uniqueCandidates.putIfAbsent(
                        key,
                        new FrequencyCandidate(
                                band,
                                frequencyMHz,
                                Long.MIN_VALUE
                        )
                );

                /*
                 * More than one different exact QRG means that we cannot safely
                 * identify one run frequency.
                 */
                if (uniqueCandidates.size() > 1) {
                    return null;
                }
            }
        }

        return uniqueCandidates.size() == 1
                ? uniqueCandidates.values()
                  .iterator()
                  .next()
                : null;
    }


    private static boolean isSupportedVariant(ChatMember member) {
        if (member == null || member.getChatCategory() == null) {
            return false;
        }

        int categoryNumber = member.getChatCategory().getCategoryNumber();
        return categoryNumber == ChatCategory.FIFTYSEVENTYMHz
                || categoryNumber == ChatCategory.VUHF
                || categoryNumber == ChatCategory.MICROWAVE
                || categoryNumber == ChatCategory.EMEJT65;
    }

    private static EnumSet<SupportedCategory> collectSupportedCategories(
            List<ChatMember> variants
    ) {
        EnumSet<SupportedCategory> categories = EnumSet.noneOf(SupportedCategory.class);

        for (ChatMember variant : variants) {
            int categoryNumber = variant.getChatCategory().getCategoryNumber();

            if (categoryNumber == ChatCategory.FIFTYSEVENTYMHz) {
                categories.add(SupportedCategory.FIFTY_SEVENTY);
            } else if (categoryNumber == ChatCategory.VUHF) {
                categories.add(SupportedCategory.VUHF);
            } else if (categoryNumber == ChatCategory.MICROWAVE) {
                categories.add(SupportedCategory.MICROWAVE);
            } else if (categoryNumber == ChatCategory.EMEJT65) {
                categories.add(SupportedCategory.EME);
            }
        }

        return categories;
    }

    private static Band lowestBand(Collection<Band> bands) {
        if (bands == null || bands.isEmpty()) {
            return null;
        }

        return bands.stream()
                .min(Comparator.comparingDouble(Band::getDefaultAnalysisFrequencyMHz))
                .orElse(null);
    }

    private enum SupportedCategory {
        FIFTY_SEVENTY(EnumSet.of(Band.B_50, Band.B_70)),
        VUHF(EnumSet.of(Band.B_144, Band.B_432)),
        MICROWAVE(EnumSet.of(
                Band.B_1296,
                Band.B_2320,
                Band.B_3400,
                Band.B_5760,
                Band.B_10G,
                Band.B_24G
        )),
        EME(EnumSet.of(
                Band.B_144,
                Band.B_432,
                Band.B_1296,
                Band.B_2320,
                Band.B_3400,
                Band.B_5760,
                Band.B_10G,
                Band.B_24G
        ));

        private final EnumSet<Band> fallbackBands;

        SupportedCategory(EnumSet<Band> fallbackBands) {
            this.fallbackBands = fallbackBands;
        }
    }

    private static final class FrequencyCandidate {

        private final Band band;
        private final double frequencyMHz;
        private final long timestampEpochMs;

        private FrequencyCandidate(Band band,
                                   double frequencyMHz,
                                   long timestampEpochMs) {
            this.band = band;
            this.frequencyMHz = frequencyMHz;
            this.timestampEpochMs = timestampEpochMs;
        }
    }

    /** Immutable selected band/frequency pair. */
    public static final class Resolution {

        private final Band band;
        private final double analysisFrequencyMHz;
        private final Source source;

        private Resolution(Band band,
                           double analysisFrequencyMHz,
                           Source source) {
            this.band = band;
            this.analysisFrequencyMHz = analysisFrequencyMHz;
            this.source = source;
        }

        public Band getBand() {
            return band;
        }

        public double getAnalysisFrequencyMHz() {
            return analysisFrequencyMHz;
        }

        public Source getSource() {
            return source;
        }

        /**
         * Converts MHz to AirScout's 100-Hz protocol unit.
         *
         * @return integer protocol value, for example 1442100 for 144.210 MHz
         */
        public String getAirScoutBandValue() {
            return Long.toString(Math.round(analysisFrequencyMHz * 10_000.0));
        }
    }
}