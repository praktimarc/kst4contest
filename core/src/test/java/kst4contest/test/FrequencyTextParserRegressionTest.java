package kst4contest.test;

import kst4contest.logic.FrequencyTextParser;
import kst4contest.model.Band;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequencyTextParserRegressionTest {

    @ParameterizedTest
    @MethodSource("compactFrequenciesAcrossSupportedBands")
    void detectsCompactFrequenciesAcrossSupportedBands(
            String compactFrequency,
            Band expectedBand,
            double expectedFrequencyMHz
    ) {
        FrequencyTextParser.DetectedFrequency detected =
                FrequencyTextParser.findExplicitFrequencies(
                        "QRV " + compactFrequency
                ).get(0);

        assertEquals(expectedBand, detected.getBand());
        assertEquals(
                expectedFrequencyMHz,
                detected.getFrequencyMHz(),
                0.000_001
        );
    }

    @Test
    void detectsReferenceFrequencyInStationName() {
        List<FrequencyTextParser.DetectedFrequency> detected =
                FrequencyTextParser.findExplicitFrequencies(
                        "Operator 144307"
                );

        assertEquals(1, detected.size());
        assertEquals(Band.B_144, detected.get(0).getBand());
        assertEquals(
                144.307,
                detected.get(0).getFrequencyMHz(),
                0.000_001
        );
    }

    @Test
    void rejectsCompactValuesOutsideSupportedBandRanges() {
        assertTrue(
                FrequencyTextParser.findExplicitFrequencies(
                        "146100 434100 99999"
                ).isEmpty()
        );
    }

    @Test
    void keepsBareThreeDigitValuesOutOfCompleteFrequencyDetection() {
        assertTrue(
                FrequencyTextParser.findExplicitFrequencies(
                        "210 599 144"
                ).isEmpty()
        );
    }

    private static Stream<Arguments> compactFrequenciesAcrossSupportedBands() {
        return Stream.of(
                Arguments.of("50278", Band.B_50, 50.278),
                Arguments.of("70200", Band.B_70, 70.200),
                Arguments.of("145500", Band.B_144, 145.500),
                Arguments.of("432100", Band.B_432, 432.100),
                Arguments.of("1296100", Band.B_1296, 1296.100),
                Arguments.of("2320100", Band.B_2320, 2320.100),
                Arguments.of("3400100", Band.B_3400, 3400.100),
                Arguments.of("5760100", Band.B_5760, 5760.100),
                Arguments.of("10368100", Band.B_10G, 10368.100),
                Arguments.of("24048100", Band.B_24G, 24048.100)
        );
    }
}
