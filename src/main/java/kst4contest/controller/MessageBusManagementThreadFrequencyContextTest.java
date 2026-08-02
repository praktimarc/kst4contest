//package kst4contest.controller;
//
//import kst4contest.model.Band;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//class MessageBusManagementThreadFrequencyContextTest {
//
//    private static final Pattern THREE_DIGIT_VALUE =
//            Pattern.compile("\\b\\d{3}\\b");
//
//    @ParameterizedTest
//    @ValueSource(strings = {
//            "qrg 210",
//            "QRG: 210",
//            "freq is 210",
//            "frequency = 210",
//            "on 210",
//            "210 MHz",
//            "210 qrg"
//    })
//    void acceptsBareThreeDigitValueWithFrequencyContext(String messageText) {
//        Matcher matcher = findThreeDigitValue(messageText);
//
//        assertTrue(
//                MessageBusManagementThread
//                        .hasExplicitBareFrequencyContext(
//                                messageText,
//                                matcher.start(),
//                                matcher.end()
//                        )
//        );
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {
//            "599",
//            "144",
//            "serial 210",
//            "score 210",
//            "worked 210 stations"
//    })
//    void rejectsBareThreeDigitValueWithoutFrequencyContext(String messageText) {
//        Matcher matcher = findThreeDigitValue(messageText);
//
//        assertFalse(
//                MessageBusManagementThread
//                        .hasExplicitBareFrequencyContext(
//                                messageText,
//                                matcher.start(),
//                                matcher.end()
//                        )
//        );
//    }
//
//    @Test
//    void resolvesOnlySupportedFallbackPrefixes() {
//        assertEquals(Band.B_144, Band.fromPrefix("144"));
//        assertEquals(Band.B_432, Band.fromPrefix(" 432 "));
//        assertEquals(Band.B_10G, Band.fromPrefix("10368"));
//        assertNull(Band.fromPrefix("999"));
//        assertNull(Band.fromPrefix(null));
//    }
//
//    private Matcher findThreeDigitValue(String messageText) {
//        Matcher matcher = THREE_DIGIT_VALUE.matcher(messageText);
//        assertTrue(matcher.find(), "Test message must contain a three-digit value");
//        assertNotNull(matcher.group());
//        return matcher;
//    }
//}