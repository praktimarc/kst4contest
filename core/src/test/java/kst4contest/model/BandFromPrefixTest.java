package kst4contest.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the fallback-prefix lookup shared by the smart frequency parser and the
 * preferences UI. Restored from the frequency-context test file that sat under
 * {@code src/main} before #62 and was therefore never executed.
 */
class BandFromPrefixTest {

    @Test
    void resolvesOnlySupportedFallbackPrefixes() {
        assertEquals(Band.B_144, Band.fromPrefix("144"));
        assertEquals(Band.B_432, Band.fromPrefix(" 432 "));
        assertEquals(Band.B_10G, Band.fromPrefix("10368"));
        assertNull(Band.fromPrefix("999"));
        assertNull(Band.fromPrefix(null));
    }

    @Test
    void rejectsPrefixesThatAreNotSpelledExactly() {
        // The lookup is a trimmed string comparison, not a numeric one.
        assertNull(Band.fromPrefix(""));
        assertNull(Band.fromPrefix("144.0"));
        assertNull(Band.fromPrefix("0144"));
        assertNull(Band.fromPrefix("10G"));
    }

    @ParameterizedTest
    @EnumSource(Band.class)
    void everyBandResolvesFromItsOwnPrefix(Band band) {
        assertEquals(band, Band.fromPrefix(band.getPrefix()));
    }
}
