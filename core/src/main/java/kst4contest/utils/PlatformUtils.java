package kst4contest.utils;

import java.util.Locale;

/**
 * Small helpers for operating-system specific UI behaviour.
 */
public final class PlatformUtils {

    private PlatformUtils() {
    }

    /**
     * @return true if the running JVM reports macOS as operating system
     */
    public static boolean isMacOs() {
        return isMacOs(System.getProperty("os.name"));
    }

    /**
     * Checks an {@code os.name} value for macOS. A missing value is treated as "not macOS".
     */
    public static boolean isMacOs(String osName) {
        if (osName == null) {
            return false;
        }
        return osName.toLowerCase(Locale.ROOT).startsWith("mac");
    }
}
