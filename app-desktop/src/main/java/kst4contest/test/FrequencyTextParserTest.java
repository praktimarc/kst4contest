package kst4contest.test;

import kst4contest.logic.FrequencyTextParser;
import kst4contest.model.Band;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequencyTextParserTest {

    @Test
    void detectsExplicitStationNameFrequencies() {
        List<FrequencyTextParser.DetectedFrequency> detected =
                FrequencyTextParser.findExplicitFrequencies(
                        "Phil 432.357"
                );

        assertEquals(1, detected.size());
        assertEquals(
                Band.B_432,
                detected.get(0).getBand()
        );
        assertEquals(
                432.357,
                detected.get(0).getFrequencyMHz(),
                0.000_001
        );
    }

    @Test
    void detectsCommaAndMicrowaveFrequencies() {
        List<FrequencyTextParser.DetectedFrequency> detected =
                FrequencyTextParser.findExplicitFrequencies(
                        "QRV 1296,210 / 10368.100"
                );

        assertEquals(2, detected.size());

        assertEquals(
                Band.B_1296,
                detected.get(0).getBand()
        );

        assertEquals(
                Band.B_10G,
                detected.get(1).getBand()
        );
    }

    @Test
    void detectsFiftyAndSeventyMhzFrequencies() {
        assertEquals(
                Band.B_50,
                FrequencyTextParser
                        .findExplicitFrequencies("50.150")
                        .get(0)
                        .getBand()
        );

        assertEquals(
                Band.B_70,
                FrequencyTextParser
                        .findExplicitFrequencies("70.200")
                        .get(0)
                        .getBand()
        );
    }

    @Test
    void ignoresRelativeAndAmbiguousValues() {
        assertTrue(
                FrequencyTextParser
                        .findExplicitFrequencies(
                                "Mike .180"
                        )
                        .isEmpty()
        );

        assertTrue(
                FrequencyTextParser
                        .findExplicitFrequencies(
                                "Mike 180"
                        )
                        .isEmpty()
        );

        assertTrue(
                FrequencyTextParser
                        .findExplicitFrequencies(
                                "David 1.2"
                        )
                        .isEmpty()
        );

        assertTrue(
                FrequencyTextParser
                        .findExplicitFrequencies(
                                "RST 599"
                        )
                        .isEmpty()
        );
    }

    @Test
    void normalizesSubKhzNotation() {
        FrequencyTextParser.DetectedFrequency detected =
                FrequencyTextParser
                        .findExplicitFrequencies(
                                "144.300.03"
                        )
                        .get(0);

        assertEquals(Band.B_144, detected.getBand());
        assertEquals(
                144.30003,
                detected.getFrequencyMHz(),
                0.000_001
        );
    }
}