package kst4contest.view;

import java.util.List;

/**
 * Selects the CSS style class used for a private-message table row.
 */
public final class PrivateMessageRowStyleResolver {

    /** Style used for messages sent by the local station. */
    public static final String OWN_STYLE_CLASS =
            "messageHighlightOwn-column";

    /** Upper inclusive age bounds for the private-message color levels. */
    private static final List<Long> AGE_LIMITS = List.of(
            30L,
            60L,
            90L,
            120L,
            180L,
            300L
    );

    /** Style classes corresponding to the configured age bounds. */
    private static final List<String> AGE_STYLES = List.of(
            "messageHighlight30-column",
            "messageHighlight60-column",
            "messageHighlight90-column",
            "messageHighlight120-column",
            "messageHighlight180-column",
            "messageHighlight300-column"
    );

    /** All private-message row classes managed by the row factory. */
    private static final List<String> MANAGED_STYLES = List.of(
            OWN_STYLE_CLASS,
            AGE_STYLES.get(0),
            AGE_STYLES.get(1),
            AGE_STYLES.get(2),
            AGE_STYLES.get(3),
            AGE_STYLES.get(4),
            AGE_STYLES.get(5)
    );

    private PrivateMessageRowStyleResolver() {
    }

    /**
     * Returns the complete set of private-message row classes managed by the
     * row factory.
     *
     * @return immutable list of managed style classes
     */
    public static List<String> knownStyleClasses() {
        return MANAGED_STYLES;
    }

    /**
     * Selects the private-message row class for the supplied message age.
     *
     * @param ownMessage whether the message was sent by the local station
     * @param ageSeconds message age in seconds
     * @return managed CSS class, or {@code null} after the five-minute window
     */
    public static String resolveStyleClass(
            final boolean ownMessage,
            final long ageSeconds
    ) {
        String styleClass = null;
        if (ownMessage) {
            styleClass = OWN_STYLE_CLASS;
        } else {
            for (int index = 0; index < AGE_LIMITS.size(); index++) {
                if (ageSeconds <= AGE_LIMITS.get(index)) {
                    styleClass = AGE_STYLES.get(index);
                    break;
                }
            }
        }

        return styleClass;
    }
}
