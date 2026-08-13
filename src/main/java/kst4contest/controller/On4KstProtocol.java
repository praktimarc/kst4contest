package kst4contest.controller;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds ON4KST port-23001 frames and rejects values that could break framing or
 * put the server into an invalid chat context.
 *
 * <p>All outbound protocol construction is concentrated here. User-controlled
 * values may therefore never introduce a field separator or a second line, and
 * category and locator validation happens before the frame reaches the socket.</p>
 */
final class On4KstProtocol {
    private static final Pattern LOCATOR_6 =
            Pattern.compile("^[A-Ra-r]{2}[0-9]{2}[A-Xa-x]{2}$");

    private On4KstProtocol() {
    }

    /**
     * Builds the initial authenticated login frame.
     *
     * @param callsign login callsign
     * @param password ON4KST password; never logged by this class
     * @param category primary chat category
     * @param clientName client identification sent to the server
     * @param lastMessageTimestamp earliest history timestamp to request
     * @return validated frame without CR/LF terminator
     */
    static String login(
            String callsign,
            String password,
            int category,
            String clientName,
            long lastMessageTimestamp
    ) {
        return "LOGINC|" + field(callsign, "callsign")
                + "|" + password(password)
                + "|" + category(category)
                + "|" + field(clientName, "client name")
                + "|25|0|1|" + Math.max(0L, lastMessageTimestamp) + "|0|";
    }

    /** Builds the settings-complete frame for the supplied chat category. */
    static String settingsDone(int category) {
        return "SDONE|" + category(category) + "|";
    }

    /** Builds the frame used to add a distinct second chat to the same session. */
    static String addChat(int category, long lastMessageTimestamp) {
        return "ACHAT|" + category(category)
                + "|25|10|2|" + Math.max(0L, lastMessageTimestamp)
                + "|0|";
    }

    /** Builds a category-qualified locator command after validating Maidenhead syntax. */
    static String setLocator(int category, String locator) {
        return command(category, "/SETLOC " + locator(locator));
    }

    /** Builds a category-qualified chat-name command. */
    static String setName(int category, String name) {
        return command(category, "/SETNAME " + field(name, "chat name"));
    }

    /** Builds the command that changes the operator state back to available. */
    static String back(int category) {
        return command(category, "/BACK");
    }

    /**
     * Wraps one validated slash command in an ON4KST message frame.
     *
     * @return frame without CR/LF terminator
     */
    static String command(int category, String command) {
        return "MSG|" + category(category) + "|0|"
                + messageText(command) + "|0|";
    }

    /**
     * Wraps one operator chat message in a category-qualified ON4KST frame.
     *
     * @return frame without CR/LF terminator
     */
    static String chatMessage(int category, String text) {
        return "MSG|" + category(category) + "|0|"
                + messageText(text) + "|0|";
    }

    /**
     * Removes trailing line terminators from a legacy raw frame while rejecting an
     * embedded line break that could inject a second server command.
     *
     * @param frame legacy raw frame, possibly with trailing CR/LF
     * @return exactly one normalized protocol line
     * @throws IllegalArgumentException if the value is {@code null} or contains an
     *                                  embedded line break
     */
    static String normalizeRawFrame(String frame) {
        if (frame == null) {
            throw new IllegalArgumentException("ON4KST frame must not be null");
        }

        int end = frame.length();
        while (end > 0) {
            char last = frame.charAt(end - 1);
            if (last != '\r' && last != '\n') {
                break;
            }
            end--;
        }

        String normalized = frame.substring(0, end);
        if (normalized.indexOf('\r') >= 0 || normalized.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    "ON4KST frame contains an embedded line break");
        }
        return normalized;
    }

    /**
     * Validates and normalizes a six-character Maidenhead locator.
     *
     * @return upper-case locator
     */
    static String locator(String locator) {
        String normalized = field(locator, "locator").toUpperCase(Locale.ROOT);
        if (!LOCATOR_6.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Locator must be a six-character Maidenhead locator: " + normalized);
        }
        return normalized;
    }

    /** Rejects message text containing an ON4KST field or line delimiter. */
    static String messageText(String text) {
        String value = field(text, "message text");
        if (value.indexOf('|') >= 0) {
            throw new IllegalArgumentException(
                    "Message text contains the ON4KST field separator '|'");
        }
        return value;
    }

    /**
     * Validates one required, non-password protocol field.
     *
     * @param value field value
     * @param label diagnostic label used in validation errors
     * @return trimmed value
     */
    static String field(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        if (value.indexOf('|') >= 0
                || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    label + " contains an ON4KST frame delimiter");
        }
        return value.trim();
    }

    private static String password(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        if (value.indexOf('|') >= 0
                || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    "password contains an ON4KST frame delimiter");
        }
        return value;
    }

    /**
     * Validates the category range supported by ON4KST.
     *
     * @return the unchanged category for convenient inline use
     */
    static int category(int category) {
        if (category < 1 || category > 12) {
            throw new IllegalArgumentException(
                    "Unsupported ON4KST chat category: " + category);
        }
        return category;
    }
}