package kst4contest.logic;

import kst4contest.model.Band;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common parser for explicit amateur-radio frequencies embedded in text.
 *
 * <p>This parser deliberately handles only complete frequencies such as
 * 144.300, 432.357 or 10368.100. Relative forms such as ".210" or ambiguous
 * bare values such as "210" require additional message context and remain the
 * responsibility of the chat-message parser.</p>
 */
public final class FrequencyTextParser {

    /*
     * Examples:
     * 50.150
     * 144.300
     * 432,357
     * 10368.100
     * 144.300.03
     *
     * At least two digits are required before the decimal separator. This
     * intentionally prevents "1.2" from being interpreted as a frequency.
     */
    private static final Pattern EXPLICIT_FREQUENCY_PATTERN = Pattern.compile(
            "(?<![A-Z0-9])"
                    + "(\\d{2,5}[.,]\\d{1,3}(?:[.,]\\d{1,3})?)"
                    + "(?!\\d)",
            Pattern.CASE_INSENSITIVE
    );

    private FrequencyTextParser() {
    }

    /**
     * Finds all distinct explicit frequencies that fall into one of the bands
     * supported by {@link Band}.
     */
    public static List<DetectedFrequency> findExplicitFrequencies(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Matcher matcher = EXPLICIT_FREQUENCY_PATTERN.matcher(text);

        Map<String, DetectedFrequency> uniqueFrequencies =
                new LinkedHashMap<>();

        while (matcher.find()) {
            DetectedFrequency detected =
                    parseExplicitFrequency(matcher.group(1));

            if (detected == null) {
                continue;
            }

            String uniqueKey =
                    detected.getBand().name()
                            + "|"
                            + Double.toString(
                            detected.getFrequencyMHz()
                    );

            uniqueFrequencies.putIfAbsent(
                    uniqueKey,
                    detected
            );
        }

        return List.copyOf(
                new ArrayList<>(uniqueFrequencies.values())
        );
    }

    /**
     * Parses one complete frequency.
     *
     * @return detected band/frequency or {@code null} when the value is invalid
     *         or outside the supported amateur bands
     */
    public static DetectedFrequency parseExplicitFrequency(
            String rawFrequency
    ) {
        if (rawFrequency == null || rawFrequency.isBlank()) {
            return null;
        }

        String normalized =
                normalizeFrequencyString(
                        rawFrequency
                                .trim()
                                .replace(',', '.')
                );

        try {
            double frequencyMHz =
                    Double.parseDouble(normalized);

            if (!Double.isFinite(frequencyMHz)) {
                return null;
            }

            Band band =
                    Band.fromFrequency(frequencyMHz);

            if (band == null) {
                return null;
            }

            return new DetectedFrequency(
                    band,
                    frequencyMHz,
                    rawFrequency.trim()
            );

        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Normalizes KST-style frequency strings with an optional second decimal
     * separator.
     *
     * Examples:
     * 144.300.03 -> 144.30003
     * 144.300    -> 144.300
     */
    private static String normalizeFrequencyString(
            String rawInput
    ) {
        int firstDotIndex = rawInput.indexOf('.');

        if (firstDotIndex < 0) {
            return rawInput;
        }

        String decimalPart =
                rawInput.substring(firstDotIndex + 1);

        if (!decimalPart.contains(".")) {
            return rawInput;
        }

        decimalPart =
                decimalPart.replace(".", "");

        return rawInput.substring(0, firstDotIndex + 1)
                + decimalPart;
    }

    /**
     * Immutable result of one explicit-frequency detection.
     */
    public static final class DetectedFrequency {

        private final Band band;
        private final double frequencyMHz;
        private final String sourceText;

        private DetectedFrequency(
                Band band,
                double frequencyMHz,
                String sourceText
        ) {
            this.band = band;
            this.frequencyMHz = frequencyMHz;
            this.sourceText = sourceText;
        }

        public Band getBand() {
            return band;
        }

        public double getFrequencyMHz() {
            return frequencyMHz;
        }

        public String getSourceText() {
            return sourceText;
        }
    }
}