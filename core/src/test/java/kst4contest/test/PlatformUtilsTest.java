package kst4contest.test;

import kst4contest.utils.PlatformUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformUtilsTest {

    @Test
    void macOsNamesAreDetected() {
        assertTrue(PlatformUtils.isMacOs("Mac OS X"));
        assertTrue(PlatformUtils.isMacOs("macOS"));
    }

    @Test
    void otherOrMissingNamesAreNotMacOs() {
        assertFalse(PlatformUtils.isMacOs("Windows 11"));
        assertFalse(PlatformUtils.isMacOs("Linux"));
        assertFalse(PlatformUtils.isMacOs(""));
        assertFalse(PlatformUtils.isMacOs(null));
    }
}
