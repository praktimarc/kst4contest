package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DXClusterSpotFormatterTest {

    @ParameterizedTest
    @MethodSource("supportedFrequencies")
    void keepsFixedColumnsAcrossSupportedFrequencies(
            String spotter,
            String frequency
    ) {
        String line = DXClusterSpotFormatter.formatLine(
                spotter,
                frequency,
                "DL5ASG",
                "JO51HK",
                "1234Z"
        );

        assertEquals(DXClusterSpotFormatter.LINE_LENGTH, line.length());
        assertEquals(
                "DL5ASG",
                line.substring(
                        DXClusterSpotFormatter.DX_CALL_COLUMN - 1,
                        DXClusterSpotFormatter.DX_CALL_COLUMN - 1 + 6
                )
        );
        assertEquals(
                "JO51HK",
                line.substring(39, 45)
        );
        assertEquals(
                "1234Z",
                line.substring(DXClusterSpotFormatter.TIME_COLUMN - 1)
        );
        assertEquals(
                frequency,
                line.substring(0, 24).trim().replaceFirst("^DX de .+?:\\s*", "")
        );
    }

    @Test
    void padsShortCommentsAndTruncatesLongCommentsToThirtyCharacters() {
        String shortLine = DXClusterSpotFormatter.formatLine(
                "DM5M",
                "144205.0",
                "DL5ASG",
                "JO51HK",
                "1234Z"
        );
        String longLine = DXClusterSpotFormatter.formatLine(
                "DM5M",
                "144205.0",
                "DL5ASG",
                "123456789012345678901234567890EXTRA",
                "1234Z"
        );

        assertEquals(
                "JO51HK" + " ".repeat(24),
                shortLine.substring(39, 69)
        );
        assertEquals(
                "123456789012345678901234567890",
                longLine.substring(39, 69)
        );
    }

    @Test
    void keepsVariableDxCallsignsWithoutMovingTheComment() {
        String twelveCharacterLine = DXClusterSpotFormatter.formatLine(
                "DO5AMF",
                "24048100.0",
                "ABCDEFGHIJKL",
                "JO51HK AP 1m/100%;4m/75%",
                "2359Z"
        );

        assertEquals("ABCDEFGHIJKL", twelveCharacterLine.substring(26, 38));
        assertEquals(
                "JO51HK AP 1m/100%;4m/75%" + " ".repeat(6),
                twelveCharacterLine.substring(39, 69)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> DXClusterSpotFormatter.formatLine(
                        "DO5AMF",
                        "144205.0",
                        "ABCDEFGHIJKLM",
                        "JO51HK",
                        "2359Z"
                )
        );
    }

    @Test
    void appendsExactlyTwoBellCharactersAndCrlf() {
        byte[] payload = DXClusterSpotFormatter.formatPayload(
                "DM5M",
                "50200.0",
                "DL5ASG",
                "JO51HK",
                "0000Z"
        );

        assertEquals(DXClusterSpotFormatter.LINE_LENGTH + 4, payload.length);
        assertEquals(7, payload[75]);
        assertEquals(7, payload[76]);
        assertEquals('\r', payload[77]);
        assertEquals('\n', payload[78]);
        assertEquals(
                75,
                new String(payload, 0, 75, StandardCharsets.US_ASCII).length()
        );
    }

    private static Stream<Arguments> supportedFrequencies() {
        return Stream.of(
                Arguments.of("DM5M", "50200.0"),
                Arguments.of("DO5AMF", "70250.0"),
                Arguments.of("DM5M", "144205.0"),
                Arguments.of("DO5AMF", "432088.0"),
                Arguments.of("DM5M", "1296338.0"),
                Arguments.of("DO5AMF", "10368100.0"),
                Arguments.of("DO5AMF", "24048100.0")
        );
    }
}
