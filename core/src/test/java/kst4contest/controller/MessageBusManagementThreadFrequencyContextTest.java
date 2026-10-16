package kst4contest.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the context rule that decides whether a bare three-digit value such as
 * {@code 210} is read as a frequency. Restored from the test file that sat under
 * {@code src/main} before #62 and was therefore never executed.
 */
class MessageBusManagementThreadFrequencyContextTest {

    private static final Pattern THREE_DIGIT_VALUE =
            Pattern.compile("\\b\\d{3}\\b");

    @ParameterizedTest
    @ValueSource(strings = {
            "qrg 210",
            "QRG: 210",
            "freq is 210",
            "frequency = 210",
            "on 210",
            "pse 210",
            "at 210",
            "210 MHz",
            "210 qrg",
            "210 freq"
    })
    void acceptsBareThreeDigitValueWithFrequencyContext(String messageText) {
        Matcher matcher = findThreeDigitValue(messageText);

        assertTrue(
                MessageBusManagementThread.hasExplicitBareFrequencyContext(
                        messageText,
                        matcher.start(),
                        matcher.end()
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "599",
            "144",
            "serial 210",
            "score 210",
            "worked 210 stations",
            "210 stations worked"
    })
    void rejectsBareThreeDigitValueWithoutFrequencyContext(String messageText) {
        Matcher matcher = findThreeDigitValue(messageText);

        assertFalse(
                MessageBusManagementThread.hasExplicitBareFrequencyContext(
                        messageText,
                        matcher.start(),
                        matcher.end()
                )
        );
    }

    @Test
    void contextWindowIsLimitedToNearbyText() {
        // "qrg" is present but more than 32 characters before the value.
        String messageText =
                "qrg will follow later in this long sentence about nothing 210";
        Matcher matcher = findThreeDigitValue(messageText);

        assertFalse(
                MessageBusManagementThread.hasExplicitBareFrequencyContext(
                        messageText,
                        matcher.start(),
                        matcher.end()
                )
        );
    }

    @Test
    void rejectsInvalidIndices() {
        assertFalse(MessageBusManagementThread
                .hasExplicitBareFrequencyContext(null, 0, 3));
        assertFalse(MessageBusManagementThread
                .hasExplicitBareFrequencyContext("qrg 210", -1, 3));
        assertFalse(MessageBusManagementThread
                .hasExplicitBareFrequencyContext("qrg 210", 5, 4));
        assertFalse(MessageBusManagementThread
                .hasExplicitBareFrequencyContext("qrg 210", 4, 8));
    }

    private Matcher findThreeDigitValue(String messageText) {
        Matcher matcher = THREE_DIGIT_VALUE.matcher(messageText);
        assertTrue(matcher.find(), "Test message must contain a three-digit value");
        assertNotNull(matcher.group());
        return matcher;
    }
}
