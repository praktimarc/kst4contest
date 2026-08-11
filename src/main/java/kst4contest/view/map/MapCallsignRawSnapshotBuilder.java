package kst4contest.view.map;

import kst4contest.locatorUtils.Location;
import kst4contest.logic.BandOpportunityResolver;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.Band;
import kst4contest.model.ChatMember;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import kst4contest.logic.FrequencyTextParser;

/**
 * Builds immutable map snapshots from the currently visible chat members.
 *
 * The aggregation key is callSignRaw because the map shall show exactly one marker
 * per base callsign, even if the same station exists in multiple chat categories.
 */
public final class MapCallsignRawSnapshotBuilder {

    public List<MapCallsignRawSnapshot> buildSnapshots(Collection<ChatMember> visibleChatMembers,
                                                       ChatMember selectedChatMember,
                                                       EnumSet<Band> selectedBands) {

        if (visibleChatMembers == null || visibleChatMembers.isEmpty()) {
            return List.of();
        }

        String selectedCallsignRaw = normalizeCallsignRaw(
                selectedChatMember == null ? null : selectedChatMember.getCallSignRaw()
        );

        Map<String, List<ChatMember>> groupedByCallsignRaw = new LinkedHashMap<>();

        for (ChatMember chatMember : visibleChatMembers) {
            if (chatMember == null) {
                continue;
            }

            String callSignRaw = normalizeCallsignRaw(chatMember.getCallSignRaw());
            if (callSignRaw.isBlank()) {
                continue;
            }

            groupedByCallsignRaw
                    .computeIfAbsent(callSignRaw, ignored -> new ArrayList<>())
                    .add(chatMember);
        }

        List<MapCallsignRawSnapshot> snapshots = new ArrayList<>(groupedByCallsignRaw.size());

        for (Map.Entry<String, List<ChatMember>> entry : groupedByCallsignRaw.entrySet()) {
            String callSignRaw = entry.getKey();
            List<ChatMember> variants = entry.getValue();

            ChatMember representative = chooseRepresentative(variants);
            if (representative == null) {
                continue;
            }

            String locator6 = findBestLocator6(variants);
            if (locator6.isBlank()) {
                continue;
            }

            Location location = new Location(locator6);

            long nowEpochMs = System.currentTimeMillis();
            BandOpportunityResolver.Resolution bandResolution =
                    BandOpportunityResolver.resolve(variants, nowEpochMs);

            EnumSet<Band> availableBands = bandResolution.getAvailableBands();
            LinkedHashMap<String, String> frequenciesByBand = collectLastKnownFrequenciesByBand(
                    variants,
                    availableBands,
                    nowEpochMs
            );

            String bandSummary = buildBandSummary(availableBands);
            boolean offersSelectedBand = !bandResolution
                    .getUnworkedEnabledBands(selectedBands)
                    .isEmpty();

            boolean warningToMyDirection = variants.stream().anyMatch(ChatMember::isInAngleAndRange);
            boolean worked = variants.stream().anyMatch(this::isWorkedAtAnyBand);
            boolean selected = callSignRaw.equals(selectedCallsignRaw);

            double qrbKm = representative.getQrb() != null ? representative.getQrb() : 0.0;
            double qtfDeg = representative.getQTFdirection() != null ? representative.getQTFdirection() : 0.0;

            int reachableAirplanes = variants.stream()
                    .map(ChatMember::getAirPlaneReflectInfo)
                    .mapToInt(this::extractReachableAirplanes)
                    .max()
                    .orElse(0);

            snapshots.add(new MapCallsignRawSnapshot(
                    callSignRaw,
                    bestDisplayCallsign(variants, representative),
                    locator6,
                    location.getLatitude().toDegrees(),
                    location.getLongitude().toDegrees(),
                    bandSummary,
                    frequenciesByBand,
                    offersSelectedBand,
                    warningToMyDirection,
                    worked,
                    selected,
                    qrbKm,
                    qtfDeg,
                    reachableAirplanes,
                    representative.getActivityTimeLastInEpoch()
            ));
        }

        snapshots.sort(Comparator.comparing(
                MapCallsignRawSnapshot::displayCallSign,
                String.CASE_INSENSITIVE_ORDER
        ));

        return List.copyOf(snapshots);
    }

    private ChatMember chooseRepresentative(List<ChatMember> variants) {
        return variants.stream()
                .filter(chatMember -> chatMember != null && normalizeLocator6(chatMember.getQra()).length() == 6)
                .max(Comparator.comparingLong(ChatMember::getActivityTimeLastInEpoch))
                .orElseGet(() -> variants.stream()
                        .filter(chatMember -> chatMember != null)
                        .max(Comparator.comparingLong(ChatMember::getActivityTimeLastInEpoch))
                        .orElse(null));
    }

    private String bestDisplayCallsign(List<ChatMember> variants, ChatMember representative) {
        if (representative != null && representative.getCallSign() != null && !representative.getCallSign().isBlank()) {
            return representative.getCallSign().trim().toUpperCase(Locale.ROOT);
        }

        for (ChatMember variant : variants) {
            if (variant != null && variant.getCallSign() != null && !variant.getCallSign().isBlank()) {
                return variant.getCallSign().trim().toUpperCase(Locale.ROOT);
            }
        }

        return representative == null ? "" : normalizeCallsignRaw(representative.getCallSignRaw());
    }

    private String findBestLocator6(List<ChatMember> variants) {
        return variants.stream()
                .sorted(Comparator.comparingLong(ChatMember::getActivityTimeLastInEpoch).reversed())
                .map(ChatMember::getQra)
                .map(this::normalizeLocator6)
                .filter(locator -> locator.length() == 6)
                .findFirst()
                .orElse("");
    }

    private LinkedHashMap<String, String> collectLastKnownFrequenciesByBand(
            List<ChatMember> variants,
            EnumSet<Band> availableBands,
            long nowEpochMs
    ) {

        Map<Band, FrequencyCandidate> latestByBand = new EnumMap<>(Band.class);

        for (ChatMember variant : variants) {
            if (variant == null) {
                continue;
            }

            for (Map.Entry<Band, ChatMember.ActiveFrequencyInfo> bandEntry
                    : variant.getKnownActiveBands().entrySet()) {

                Band band = bandEntry.getKey();
                ChatMember.ActiveFrequencyInfo activeFrequencyInfo = bandEntry.getValue();

                if (band == null
                        || activeFrequencyInfo == null
                        || availableBands == null
                        || !availableBands.contains(band)) {
                    continue;
                }

                long ageMs = nowEpochMs - activeFrequencyInfo.timestampEpoch;
                if (ageMs < 0L
                        || ageMs > BandOpportunityResolver.RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS) {
                    continue;
                }

                FrequencyCandidate previous = latestByBand.get(band);
                if (previous == null
                        || activeFrequencyInfo.timestampEpoch > previous.timestampEpochMs()) {

                    latestByBand.put(band, new FrequencyCandidate(
                            band,
                            formatFrequency(activeFrequencyInfo.frequency),
                            activeFrequencyInfo.timestampEpoch
                    ));
                }
            }

        }

        /*
         * A QRG explicitly contained in the current station name does not expire.
         *
         * Recent dynamic QRG evidence has priority. Therefore station-name QRGs fill
         * only bands for which no recent dynamic frequency is available.
         *
         * Different explicit QRGs for the same band are considered ambiguous and are
         * not reduced to one arbitrary run frequency.
         */
        Map<Band, Map<String, FrequencyTextParser.DetectedFrequency>>
                stationNameFrequenciesByBand =
                new EnumMap<>(Band.class);

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

                if (availableBands == null
                        || !availableBands.contains(band)) {
                    continue;
                }

                stationNameFrequenciesByBand
                        .computeIfAbsent(
                                band,
                                ignored -> new LinkedHashMap<>()
                        )
                        .putIfAbsent(
                                Double.toString(
                                        detectedFrequency.getFrequencyMHz()
                                ),
                                detectedFrequency
                        );
            }
        }

        for (Map.Entry<
                Band,
                Map<String, FrequencyTextParser.DetectedFrequency>>
                entry : stationNameFrequenciesByBand.entrySet()) {

            Band band = entry.getKey();

            /*
             * A recent QRG detected from chat always wins.
             */
            if (latestByBand.containsKey(band)) {
                continue;
            }

            /*
             * Do not guess when several different QRGs were published for the
             * same band.
             */
            if (entry.getValue().size() != 1) {
                continue;
            }

            FrequencyTextParser.DetectedFrequency detectedFrequency =
                    entry.getValue()
                            .values()
                            .iterator()
                            .next();

            latestByBand.put(
                    band,
                    new FrequencyCandidate(
                            band,
                            formatFrequency(
                                    detectedFrequency.getFrequencyMHz()
                            ),
                            Long.MIN_VALUE
                    )
            );
        }

        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        latestByBand.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.put(
                        toBandDisplayLabel(entry.getKey()),
                        entry.getValue().formattedFrequency()
                ));

        return ordered;
    }

    private String buildBandSummary(EnumSet<Band> availableBands) {
        if (availableBands == null || availableBands.isEmpty()) {
            return "";
        }

        List<String> labels = availableBands.stream()
                .sorted()
                .map(this::toBandDisplayLabel)
                .toList();
        return String.join(", ", labels);
    }

    private boolean isWorkedAtAnyBand(ChatMember member) {
        return member.isWorked()
                || member.isWorked50()
                || member.isWorked70()
                || member.isWorked144()
                || member.isWorked432()
                || member.isWorked1240()
                || member.isWorked2300()
                || member.isWorked3400()
                || member.isWorked5600()
                || member.isWorked10G()
                || member.isWorked24G()
                || member.isWorked47G()
                || member.isWorked76G();
    }

    private int extractReachableAirplanes(AirPlaneReflectionInfo airPlaneReflectionInfo) {
        return airPlaneReflectionInfo == null ? 0 : airPlaneReflectionInfo.getAirPlanesReachableCntr();
    }

    private String normalizeCallsignRaw(String callSignRaw) {
        if (callSignRaw == null) {
            return "";
        }
        return callSignRaw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLocator6(String locator) {
        if (locator == null) {
            return "";
        }

        String normalized = locator.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() >= 6) {
            normalized = normalized.substring(0, 6);
        }

        if (!normalized.matches("^[A-R]{2}[0-9]{2}[A-X]{2}$")) {
            return "";
        }

        return normalized;
    }

    private String toBandDisplayLabel(Band band) {
        return switch (band) {
            case B_50 -> "50";
            case B_70 -> "70";
            case B_144 -> "144";
            case B_432 -> "432";
            case B_1296 -> "1296";
            case B_2320 -> "2320";
            case B_3400 -> "3400";
            case B_5760 -> "5760";
            case B_10G -> "10368";
            case B_24G -> "24048";
        };
    }

    private String formatFrequency(double frequencyMHz) {
        return String.format(Locale.US, "%.3f", frequencyMHz);
    }

    private record FrequencyCandidate(Band band, String formattedFrequency, long timestampEpochMs) {
    }
}