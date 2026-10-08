package kst4contest.controller;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Formats local DX Cluster spots using the fixed-column layout emitted by
 * DXSpider and accepted by common logging programs.
 */
final class DXClusterSpotFormatter {

    /** Length of the DX Cluster line before BEL and CRLF framing. */
    /* package */
    static final int LINE_LENGTH = 75;
    /** One-based column in which the spotted callsign starts. */
    /* package */
    static final int DX_CALL_COLUMN = 27;
    /** Width of the fixed comment field. */
    /* package */
    static final int COMMENT_LENGTH = 30;
    /** One-based column in which the UTC time starts. */
    /* package */
    static final int TIME_COLUMN = 71;

    /** Zero-based exclusive end position of the frequency field. */
    private static final int FREQUENCY_END = 24;
    /** Maximum width of the spotted callsign field. */
    private static final int DX_CALL_LENGTH = 12;
    /** Required width of the HHMMZ time field. */
    private static final int TIME_LENGTH = 5;
    /** Minimum separator width between spotter and frequency. */
    private static final int MIN_FREQUENCY_GAP = 1;
    /** Wire framing appended to every formatted line. */
    private static final String PAYLOAD_SUFFIX = "\u0007\u0007\r\n";

    private DXClusterSpotFormatter() {
    }

    /** Builds the fixed 75-character payload line without wire framing. */
    /* package */
    static String formatLine(
            final String spotterCallSign,
            final String frequency,
            final String dxCallSign,
            final String comment,
            final String time
    ) {
        final String spotter = requireValue(
                spotterCallSign,
                "spotter callsign"
        )
                .toUpperCase(Locale.ROOT);
        final String frequencyValue = requireValue(frequency, "frequency");
        final String dxCall = requireValue(dxCallSign, "DX callsign")
                .toUpperCase(Locale.ROOT);
        final String timeValue = requireValue(time, "time");

        validateDxCall(dxCall);
        validateTime(timeValue);

        final String prefix = "DX de " + spotter + ":";
        final int frequencyPadding = calculateFrequencyPadding(
                prefix,
                frequencyValue
        );
        final String normalizedComment = normalizeComment(comment);

        final String line = prefix
                + " ".repeat(frequencyPadding)
                + frequencyValue
                + "  "
                + padRight(dxCall, DX_CALL_LENGTH)
                + " "
                + padRight(normalizedComment, COMMENT_LENGTH)
                + " "
                + timeValue;

        if (line.length() != LINE_LENGTH) {
            throw new IllegalStateException(
                    "DX Cluster formatter produced "
                            + line.length()
                            + " characters instead of "
                            + LINE_LENGTH
            );
        }

        return line;
    }

    /** Builds one complete ASCII spot payload including BEL and CRLF framing. */
    /* package */
    static byte[] formatPayload(
            final String spotterCallSign,
            final String frequency,
            final String dxCallSign,
            final String comment,
            final String time
    ) {
        return (formatLine(
                spotterCallSign,
                frequency,
                dxCallSign,
                comment,
                time
        ) + PAYLOAD_SUFFIX).getBytes(StandardCharsets.US_ASCII);
    }

    private static void validateDxCall(final String dxCall) {
        if (dxCall.length() > DX_CALL_LENGTH) {
            throw new IllegalArgumentException(
                    "DX callsign exceeds 12 characters: " + dxCall
            );
        }
    }

    private static void validateTime(final String time) {
        if (time.length() != TIME_LENGTH) {
            throw new IllegalArgumentException(
                    "DX Cluster time must contain exactly five characters"
            );
        }
    }

    private static int calculateFrequencyPadding(
            final String prefix,
            final String frequency
    ) {
        final int padding = FREQUENCY_END
                - prefix.length()
                - frequency.length();

        if (padding < MIN_FREQUENCY_GAP) {
            throw new IllegalArgumentException(
                    "Spotter callsign and frequency do not fit the DX Cluster prefix"
            );
        }

        return padding;
    }

    private static String normalizeComment(final String comment) {
        final String normalized = comment == null ? "" : comment.trim();

        return normalized.length() > COMMENT_LENGTH
                ? normalized.substring(0, COMMENT_LENGTH)
                : normalized;
    }

    private static String requireValue(
            final String value,
            final String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "DX Cluster " + fieldName + " is missing"
            );
        }

        return value.trim();
    }

    private static String padRight(final String value, final int length) {
        return value + " ".repeat(length - value.length());
    }
}
