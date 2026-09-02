package kst4contest.view;

/**
 * Pure tooltip decisions kept separate from JavaFX controls for unit testing.
 */
final class TruncatedTextTooltipSupport {

    private TruncatedTextTooltipSupport() {
    }

    static boolean isTextClipped(double requiredWidth, double availableWidth) {
        return requiredWidth > availableWidth + 1.0;
    }

    static String buildTooltipText(String fullText, boolean clipped, String functionalText) {
        boolean hasFunctionalText = functionalText != null && !functionalText.isBlank();
        if (!clipped && !hasFunctionalText) {
            return null;
        }
        if (!clipped) {
            return functionalText;
        }
        if (!hasFunctionalText) {
            return fullText;
        }
        return fullText + "\n\n" + functionalText;
    }
}
