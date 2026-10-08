package kst4contest.view.compose

import kst4contest.model.UpdateInformation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * When the update window opens, and what it says.
 *
 * The decision matters in both directions: a window that opens without an update is a
 * daily annoyance, and one that stays shut when an update exists defeats the whole
 * mechanism.
 */
class UpdateWindowStateTest {

    private fun semantic(version: String) = UpdateInformation().apply {
        latestSemanticVersionOnServer = version
    }

    private fun numeric(number: Double) = UpdateInformation().apply {
        latestVersionNumberOnServer = number
    }

    private fun state(information: UpdateInformation?) =
        UpdateWindowState(information, currentVersion = "1.50.0", currentVersionNumber = 1.50)

    @Test
    fun `a newer semantic version on the server is an update`() {
        assertTrue(state(semantic("1.51.0")).updateAvailable)
        assertTrue(state(semantic("2.0.0")).updateAvailable)
    }

    @Test
    fun `the same or an older semantic version is not`() {
        assertFalse(state(semantic("1.50.0")).updateAvailable,
            "the running version is not an update to itself")
        assertFalse(state(semantic("1.49.9")).updateAvailable,
            "a server rolled back must not push the operator downwards")
    }

    @Test
    fun `without a semantic version the numeric comparison decides`() {
        val older = numeric(1.49)
        val newer = numeric(1.51)

        assertFalse(older.hasSemanticVersion(), "this is the path the test is about")
        assertFalse(state(older).updateAvailable)
        assertTrue(state(newer).updateAvailable)
    }

    @Test
    fun `absent update information is not an update`() {
        // The check runs at startup. A download that failed must not keep the
        // application from coming up, and must not claim an update either.
        assertFalse(state(null).updateAvailable)
    }

    @Test
    fun `the version lines carry the product name, not the directory name`() {
        val state = state(semantic("1.51.0"))

        assertEquals("KST4Contest 1.50.0", state.installedVersion)
        assertEquals("KST4Contest 1.51.0", state.latestVersion)
    }

    @Test
    fun `a change log row becomes a heading with its lines`() {
        val information = semantic("1.51.0").apply {
            changeLog = arrayListOf(
                arrayOf("1.51.0", "Compose settings window", "Tabs sized to their titles"),
                arrayOf("1.50.0", "Gradle build"),
            )
        }

        val sections = state(information).changeLog

        assertEquals(2, sections.size)
        assertEquals("1.51.0", sections[0].title)
        assertEquals(
            listOf("Compose settings window", "Tabs sized to their titles"),
            sections[0].entries,
        )
        assertEquals(listOf("Gradle build"), sections[1].entries)
    }

    @Test
    fun `a heading without lines is kept`() {
        val information = semantic("1.51.0").apply {
            changeLog = arrayListOf(arrayOf("1.51.0"))
        }

        val sections = state(information).changeLog

        assertEquals(1, sections.size,
            "a version that brought no listed change is still worth showing as released")
        assertTrue(sections[0].entries.isEmpty())
    }

    @Test
    fun `known bugs read the same way as the change log`() {
        val information = semantic("1.51.0").apply {
            bugList = arrayListOf(arrayOf("Profile switch", "The confirmation opens behind"))
        }

        val sections = state(information).knownBugs

        assertEquals("Profile switch", sections[0].title)
        assertEquals(listOf("The confirmation opens behind"), sections[0].entries)
    }

    @Test
    fun `an empty update information yields empty text rather than null`() {
        val state = state(UpdateInformation())

        assertEquals("", state.majorChanges)
        assertEquals("", state.adminMessage)
        assertEquals("", state.releasePageUrl)
        assertTrue(state.changeLog.isEmpty())
        assertTrue(state.knownBugs.isEmpty())
    }
}
