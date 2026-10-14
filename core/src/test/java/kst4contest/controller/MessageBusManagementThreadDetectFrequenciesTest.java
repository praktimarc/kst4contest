package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import kst4contest.controller.MessageBusManagementThread.DetectedChatFrequency;
import kst4contest.model.Band;

/**
 * Unit tests for the pure frequency detection extracted from
 * {@link MessageBusManagementThread#smartFrequencyExtraction}. No mocks, no thread,
 * no preferences; the clock is an explicit parameter.
 */
class MessageBusManagementThreadDetectFrequenciesTest {

    private static final long NOW = 1_000_000_000_000L;
    private static final long THIRTY_MINUTES = 1_800_000L;
    private static final double EPSILON = 1e-9;

    private static List<DetectedChatFrequency> detect(String text) {
        return MessageBusManagementThread.detectFrequencies(text, Map.of(), null, NOW);
    }

    private static List<DetectedChatFrequency> detect(
            String text, Map<Band, Long> context, String fallbackPrefix) {
        return MessageBusManagementThread.detectFrequencies(text, context, fallbackPrefix, NOW);
    }

    private static void assertSingle(
            List<DetectedChatFrequency> result, Band band, double frequencyMHz) {
        assertEquals(1, result.size(), () -> "detections: " + result);
        assertEquals(band, result.get(0).band());
        assertEquals(frequencyMHz, result.get(0).frequencyMHz(), EPSILON);
    }

    // --- complete frequencies -----------------------------------------------

    @Test
    void detectsDottedFullFrequency() {
        List<DetectedChatFrequency> result = detect("pse qsy 144.300");

        assertSingle(result, Band.B_144, 144.300);
        assertFalse(result.get(0).derivedFromShortForm());
        assertEquals("144.300", result.get(0).rawMatch());
    }

    @Test
    void detectsCommaAsDecimalSeparator() {
        List<DetectedChatFrequency> result = detect("qrg 432,210");

        assertSingle(result, Band.B_432, 432.210);
        assertEquals("432.210", result.get(0).rawMatch());
    }

    @Test
    void detectsCompactDigitOnlyFrequency() {
        assertSingle(detect("pse try 10368100"), Band.B_10G, 10368.100);
    }

    @Test
    void detectsKstStyleSecondDecimalSeparator() {
        assertSingle(detect("cq on 144.300.03"), Band.B_144, 144.30003);
    }

    @Test
    void fullFrequencyIgnoresBandContextAndFallback() {
        Map<Band, Long> context = Map.of(Band.B_432, NOW);

        assertSingle(detect("144.300", context, "1296"), Band.B_144, 144.300);
    }

    @Test
    void ignoresFullFrequencyOutsideEverySupportedBand() {
        assertTrue(detect("try 146.500").isEmpty());
        assertTrue(detect("cq 28.500").isEmpty());
        assertSingle(detect("146.500 or 433.900"), Band.B_432, 433.900);
    }

    @Test
    void reportsAllFrequenciesInMessageOrder() {
        List<DetectedChatFrequency> result = detect("144.300 or 432.200");

        assertEquals(2, result.size());
        assertEquals(Band.B_144, result.get(0).band());
        assertEquals(Band.B_432, result.get(1).band());
    }

    /*
     * Pinned oddity: any decimal number between 50.0 and 54.0 (or 70.0 and 70.5)
     * is read as a frequency, so a distance such as "52,3 km" becomes 52.3 MHz.
     */
    @Test
    void pinsThatAPlainDecimalInsideTheSixMetreRangeCountsAsFrequency() {
        assertSingle(detect("qrb 52,3 km"), Band.B_50, 52.3);
    }

    // --- relative and bare values -------------------------------------------

    @Test
    void relativeValueWithoutContextUsesTheConfiguredFallback() {
        List<DetectedChatFrequency> result = detect("pse .210", Map.of(), "432");

        assertSingle(result, Band.B_432, 432.210);
        assertTrue(result.get(0).derivedFromShortForm());
        assertEquals("210", result.get(0).rawMatch());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "28", "abc"})
    void unknownFallbackPrefixFallsBackTo144(String prefix) {
        assertSingle(detect("pse .210", Map.of(), prefix), Band.B_144, 144.210);
    }

    @Test
    void missingFallbackPrefixFallsBackTo144() {
        assertSingle(detect("pse .210", null, null), Band.B_144, 144.210);
    }

    @Test
    void relativeValueWithCommaIsAccepted() {
        assertSingle(detect("pse ,210"), Band.B_144, 144.210);
    }

    @Test
    void bareValueNeedsFrequencyContext() {
        assertTrue(detect("ur 599 tnx").isEmpty());
        assertTrue(detect("144 tonight").isEmpty());
        assertSingle(detect("qrg 210"), Band.B_144, 144.210);
        assertSingle(detect("210 MHz"), Band.B_144, 144.210);
    }

    @Test
    void relativeValueUsesFreshSenderBandBeforeFallback() {
        Map<Band, Long> context = Map.of(Band.B_1296, NOW - 60_000L);

        assertSingle(detect("pse .210", context, "432"), Band.B_1296, 1296.210);
    }

    @Test
    void newestFreshBandWins() {
        Map<Band, Long> context = new HashMap<>();
        context.put(Band.B_144, NOW - 120_000L);
        context.put(Band.B_432, NOW - 60_000L);
        context.put(Band.B_1296, NOW - 180_000L);

        assertSingle(detect("pse .210", context, null), Band.B_432, 432.210);
    }

    @Test
    void contextExactlyThirtyMinutesOldIsStillUsed() {
        Map<Band, Long> context = Map.of(Band.B_432, NOW - THIRTY_MINUTES);

        assertSingle(detect("pse .210", context, "144"), Band.B_432, 432.210);
    }

    @Test
    void contextOlderThanThirtyMinutesIsIgnored() {
        Map<Band, Long> context = Map.of(Band.B_432, NOW - THIRTY_MINUTES - 1L);

        assertSingle(detect("pse .210", context, "144"), Band.B_144, 144.210);
    }

    @Test
    void contextBandIsSkippedWhenTheCompletedValueLeavesTheBand() {
        // 70.900 is outside 70.000-70.500, so the older 432 context is used.
        Map<Band, Long> context = new EnumMap<>(Band.class);
        context.put(Band.B_70, NOW - 1_000L);
        context.put(Band.B_432, NOW - 5_000L);

        assertSingle(detect("pse .900", context, "144"), Band.B_432, 432.900);
    }

    @Test
    void relativeValueOutsideTheFallbackBandIsDropped() {
        // 70.900 lies outside the 70 MHz band and nothing else is tried.
        assertTrue(detect("pse .900", Map.of(), "70").isEmpty());
    }

    @Test
    void earlierDetectionInTheSameMessageBecomesContextForALaterRelativeValue() {
        Map<Band, Long> context = Map.of(Band.B_432, NOW - 60_000L);

        List<DetectedChatFrequency> result =
                detect("10368.100 then .150", context, "144");

        assertEquals(2, result.size());
        assertEquals(Band.B_10G, result.get(1).band());
        assertEquals(10368.150, result.get(1).frequencyMHz(), EPSILON);
    }

    @Test
    void doesNotModifyTheGivenContext() {
        Map<Band, Long> context = new EnumMap<>(Band.class);
        context.put(Band.B_432, NOW - 60_000L);

        detect("10368.100 then .150", context, "144");

        assertEquals(Map.of(Band.B_432, NOW - 60_000L), context);
    }

    // --- edge cases ---------------------------------------------------------

    @Test
    void nullOrTextWithoutNumbersYieldsNothing() {
        assertTrue(MessageBusManagementThread.detectFrequencies(null, null, null, NOW).isEmpty());
        assertTrue(detect("gm all, tnx for qso").isEmpty());
    }

    @Test
    void callsignDigitsAndLocatorsAreNotFrequencies() {
        assertTrue(detect("DL144ABC in JO62QM").isEmpty());
    }
}
