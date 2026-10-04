package kst4contest.view

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class BuildInfoTest {

    @Test
    fun `an unstamped build reports a version that cannot be mistaken for a release`() {
        // Tests run from class directories, where no jar manifest exists.
        assertEquals(BuildInfo.UNKNOWN_VERSION, BuildInfo.version)
        assertFalse(BuildInfo.version.first().isDigit(),
                "an unstamped version must not look like a release number")
    }

    @Test
    fun `the window title carries the version`() {
        val title = BuildInfo.windowTitle("KST4Contest")
        assertTrue(title.startsWith("KST4Contest "))
        assertTrue(title.endsWith(BuildInfo.version))
    }
}
