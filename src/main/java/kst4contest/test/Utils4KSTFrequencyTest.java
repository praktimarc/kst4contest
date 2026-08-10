package kst4contest.test;

import javafx.beans.property.SimpleStringProperty;
import kst4contest.controller.Utils4KST;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Utils4KSTFrequencyTest {

    @Test
    void convertsCompleteFrequenciesAcrossAllSupportedPrefixLengths() {
        assertEquals("50200.0", convert("50.200", "144"));
        assertEquals("70250.0", convert("70,250", "144"));
        assertEquals("144205.0", convert("144.205", "432"));
        assertEquals("432088.0", convert("432.088", "144"));
        assertEquals("1296338.0", convert("1296.338", "144"));
        assertEquals("10368100.0", convert("10368.100", "144"));
        assertEquals("24048100.0", convert("24048.100", "144"));
    }

    @Test
    void retainsSubKhzPrecision() {
        assertEquals("144205.2", convert("144.205.2", "432"));
        assertEquals("1296338.25", convert("1296.338.25", "144"));
    }

    @Test
    void convertsCompactLegacyFormats() {
        assertEquals("432088.0", convert("432088", "144"));
        assertEquals("10368100.0", convert("10368100", "144"));
        assertEquals("432088.2", convert("432088.2", "144"));
        assertEquals("432088.0", convert("432 088", "144"));
    }

    @Test
    void appliesFallbackOnlyToRelativeFrequencies() {
        assertEquals("432205.0", convert(".205", "432"));
        assertEquals("144205.0", convert("205", "144"));
        assertEquals("10368300.0", convert(".300", "10368"));
    }

    @Test
    void rejectsMissingOrUnsupportedValues() {
        assertEquals("", convert(null, "144"));
        assertEquals("", convert("", "144"));
        assertEquals("", convert("not-a-frequency", "144"));
        assertEquals("", convert("599", "144"));
        assertEquals("", convert(".205", null));
        assertEquals("", convert(".205", "invalid"));
    }

    private String convert(String frequency, String fallbackPrefix) {
        SimpleStringProperty fallback = fallbackPrefix == null
                ? null
                : new SimpleStringProperty(fallbackPrefix);

        return Utils4KST.normalizeFrequencyString(
                frequency,
                fallback
        );
    }
}