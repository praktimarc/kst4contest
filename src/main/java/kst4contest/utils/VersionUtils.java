package kst4contest.utils;

public final class VersionUtils {

    private VersionUtils() {
    }

    /**
     * Compares numeric release versions such as 1.41, 1.41.1 and 1.41.10.
     *
     * Pre-release and build suffixes are ignored because the update feed
     * currently publishes stable releases only.
     */
    public static int compareStableVersions(String left, String right) {
        int[] leftParts = parseVersion(left);
        int[] rightParts = parseVersion(right);
        int partCount = Math.max(leftParts.length, rightParts.length);

        for (int index = 0; index < partCount; index++) {
            int leftPart = index < leftParts.length ? leftParts[index] : 0;
            int rightPart = index < rightParts.length ? rightParts[index] : 0;

            int comparison = Integer.compare(leftPart, rightPart);
            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }

    private static int[] parseVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version must not be empty");
        }

        String normalized = version.trim();

        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }

        int hyphenIndex = normalized.indexOf('-');
        int plusIndex = normalized.indexOf('+');
        int suffixIndex;

        if (hyphenIndex < 0) {
            suffixIndex = plusIndex;
        } else if (plusIndex < 0) {
            suffixIndex = hyphenIndex;
        } else {
            suffixIndex = Math.min(hyphenIndex, plusIndex);
        }

        if (suffixIndex >= 0) {
            normalized = normalized.substring(0, suffixIndex);
        }

        String[] textParts = normalized.split("\\.");
        int[] numericParts = new int[textParts.length];

        for (int index = 0; index < textParts.length; index++) {
            numericParts[index] = Integer.parseInt(textParts[index]);
        }

        return numericParts;
    }
}