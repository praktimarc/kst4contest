package kst4contest.test;

import kst4contest.logic.PropagationFrequencyResolver;
import kst4contest.model.Band;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PropagationFrequencyResolverTest {

    private static final long NOW = 10_000_000L;


    @Test
    void exactStationNameQrgWinsOverBandAndCategoryFallback() {
        ChatMember station =
                station(
                        ChatCategory.VUHF,
                        "Phil 432.357"
                );

        PropagationFrequencyResolver.Resolution resolution =
                resolve(
                        List.of(station),
                        EnumSet.of(
                                Band.B_144,
                                Band.B_432
                        )
                );

        assertEquals(
                Band.B_432,
                resolution.getBand()
        );

        assertEquals(
                432.357,
                resolution.getAnalysisFrequencyMHz(),
                0.000_001
        );

        assertEquals(
                PropagationFrequencyResolver.Source.STATION_NAME_QRG,
                resolution.getSource()
        );
    }

    @Test
    void recentChatQrgWinsOverExactStationNameQrg() {
        ChatMember station =
                station(
                        ChatCategory.VUHF,
                        "Phil 432.357"
                );

        addCurrentQrg(
                station,
                Band.B_432,
                432.335,
                NOW - 1_000L
        );

        PropagationFrequencyResolver.Resolution resolution =
                resolve(
                        List.of(station),
                        EnumSet.of(Band.B_432)
                );

        assertEquals(
                432.335,
                resolution.getAnalysisFrequencyMHz(),
                0.000_001
        );

        assertEquals(
                PropagationFrequencyResolver.Source.CURRENT_QRG,
                resolution.getSource()
        );
    }

    @Test
    void multipleStationNameQrgsAreNotTreatedAsOneRunFrequency() {
        ChatMember station =
                station(
                        ChatCategory.EMEJT65,
                        "QRV 432.357 1296.210"
                );

        PropagationFrequencyResolver.Resolution resolution =
                resolve(
                        List.of(station),
                        EnumSet.of(
                                Band.B_432,
                                Band.B_1296
                        )
                );

        /*
         * Both bands are known, but no exact run QRG is guessed.
         * Existing station-name band selection therefore chooses the
         * lowest usable advertised band.
         */
        assertEquals(
                Band.B_432,
                resolution.getBand()
        );

        assertEquals(
                432.0,
                resolution.getAnalysisFrequencyMHz(),
                0.000_001
        );

        assertEquals(
                PropagationFrequencyResolver.Source.STATION_NAME,
                resolution.getSource()
        );
    }

    @Test
    void currentQrgWinsOverNameAndCategoryFallback() {
        ChatMember station = station(ChatCategory.MICROWAVE, "QRV 3cm");
        addCurrentQrg(station, Band.B_2320, 2320.175, NOW - 1_000L);

        PropagationFrequencyResolver.Resolution resolution = resolve(
                List.of(station),
                EnumSet.of(Band.B_1296, Band.B_2320, Band.B_10G)
        );

        assertEquals(Band.B_2320, resolution.getBand());
        assertEquals(2320.175, resolution.getAnalysisFrequencyMHz(), 0.000_001);
        assertEquals(PropagationFrequencyResolver.Source.CURRENT_QRG, resolution.getSource());
        assertEquals("23201750", resolution.getAirScoutBandValue());
    }

    @Test
    void mostRecentlyDetectedQrgWinsAcrossCategoryVariants() {
        ChatMember vhf = station(ChatCategory.VUHF, "");
        ChatMember microwave = station(ChatCategory.MICROWAVE, "");
        addCurrentQrg(vhf, Band.B_144, 144.210, NOW - 10_000L);
        addCurrentQrg(microwave, Band.B_1296, 1296.210, NOW - 1_000L);

        PropagationFrequencyResolver.Resolution resolution = resolve(
                List.of(vhf, microwave),
                EnumSet.of(Band.B_144, Band.B_432, Band.B_1296)
        );

        assertEquals(Band.B_1296, resolution.getBand());
        assertEquals(1296.210, resolution.getAnalysisFrequencyMHz(), 0.000_001);
    }

    @Test
    void microwaveFallsBackToLowestEnabledMicrowaveBand() {
        ChatMember station = station(ChatCategory.MICROWAVE, "");

        PropagationFrequencyResolver.Resolution resolution = resolve(
                List.of(station),
                EnumSet.of(Band.B_2320, Band.B_3400)
        );

        assertEquals(Band.B_2320, resolution.getBand());
        assertEquals(2320.0, resolution.getAnalysisFrequencyMHz(), 0.000_001);
        assertEquals(PropagationFrequencyResolver.Source.CHAT_CATEGORY, resolution.getSource());
    }

    @Test
    void supportedCategoriesUseTheirAgreedLowestFallbackBand() {
        assertEquals(
                Band.B_50,
                resolve(
                        List.of(station(ChatCategory.FIFTYSEVENTYMHz, "")),
                        EnumSet.of(Band.B_50, Band.B_70)
                ).getBand()
        );
        assertEquals(
                Band.B_144,
                resolve(
                        List.of(station(ChatCategory.VUHF, "")),
                        EnumSet.of(Band.B_144, Band.B_432)
                ).getBand()
        );
        assertEquals(
                Band.B_1296,
                resolve(
                        List.of(station(ChatCategory.MICROWAVE, "")),
                        EnumSet.of(Band.B_1296, Band.B_2320)
                ).getBand()
        );
        assertEquals(
                Band.B_144,
                resolve(
                        List.of(station(ChatCategory.EMEJT65, "")),
                        EnumSet.of(Band.B_144, Band.B_1296)
                ).getBand()
        );
    }

    @Test
    void vhfAndMicrowaveUse432MhzFallback() {
        ChatMember vhf = station(ChatCategory.VUHF, "");
        ChatMember microwave = station(ChatCategory.MICROWAVE, "");

        PropagationFrequencyResolver.Resolution resolution = resolve(
                List.of(vhf, microwave),
                EnumSet.of(Band.B_144, Band.B_432, Band.B_1296)
        );

        assertEquals(Band.B_432, resolution.getBand());
        assertEquals(432.0, resolution.getAnalysisFrequencyMHz(), 0.000_001);
        assertEquals(
                PropagationFrequencyResolver.Source.DUAL_CATEGORY_FALLBACK,
                resolution.getSource()
        );
        assertEquals("4320000", resolution.getAirScoutBandValue());
    }

    @Test
    void unsupportedChatCategoriesDoNotFallBackTo144Mhz() {
        for (int categoryNumber = ChatCategory.LOWBAND;
             categoryNumber <= ChatCategory.TENMeter;
             categoryNumber++) {
            ChatMember unsupported = station(categoryNumber, "QRV 2m");
            addCurrentQrg(unsupported, Band.B_144, 144.300, NOW - 1_000L);

            assertNull(
                    resolve(
                            List.of(unsupported),
                            EnumSet.of(Band.B_144, Band.B_432)
                    ),
                    "Category " + categoryNumber + " must be ignored"
            );
        }
    }

    @Test
    void manualNotQrvExclusionForcesNextUsableMicrowaveBand() {
        ChatMember station = station(ChatCategory.MICROWAVE, "");
        station.setQrv1240(false);

        PropagationFrequencyResolver.Resolution resolution = resolve(
                List.of(station),
                EnumSet.of(Band.B_1296, Band.B_2320)
        );

        assertEquals(Band.B_2320, resolution.getBand());
        assertEquals(2320.0, resolution.getAnalysisFrequencyMHz(), 0.000_001);
    }

    @Test
    void stationNameBandHintWinsOverCategoryFallback() {
        ChatMember station = station(ChatCategory.MICROWAVE, "QRV 3cm");

        PropagationFrequencyResolver.Resolution resolution = resolve(
                List.of(station),
                EnumSet.of(Band.B_1296, Band.B_10G)
        );

        assertEquals(Band.B_10G, resolution.getBand());
        assertEquals(10368.0, resolution.getAnalysisFrequencyMHz(), 0.000_001);
        assertEquals(PropagationFrequencyResolver.Source.STATION_NAME, resolution.getSource());
    }

    private PropagationFrequencyResolver.Resolution resolve(
            List<ChatMember> variants,
            EnumSet<Band> enabledBands
    ) {
        return PropagationFrequencyResolver.resolve(variants, enabledBands, NOW);
    }

    private ChatMember station(int categoryNumber, String name) {
        ChatMember station = new ChatMember();
        station.setCallSign("DL1ABC");
        station.setChatCategory(new ChatCategory(categoryNumber));
        station.setName(name);
        return station;
    }

    private void addCurrentQrg(ChatMember station,
                               Band band,
                               double frequencyMHz,
                               long timestampEpochMs) {
        station.addKnownFrequency(band, frequencyMHz);
        station.getKnownActiveBands().get(band).timestampEpoch = timestampEpochMs;
    }
}