package kst4contest.view.compose

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * The second net. SettingsFieldCoverageTest only covers ChatPreferences setters,
 * so a write to any other object slips through it — and the JavaFX settings window
 * had five such writes:
 *
 * - setChatCategoryMain and setChatCategorySecondChat on ChatController, called on
 *   every category selection alongside the ChatPreferences write. Dropping them
 *   leaves the running session on the old category while the stored setting says
 *   otherwise.
 * - setCallSign, setFrequency and setQra on the own ChatMember, kept in step with
 *   the station fields.
 *
 * The list was derived mechanically: the setters called in the settings range that
 * ChatPreferences does not declare, intersected with the setters the project's own
 * classes declare, which drops JavaFX control setters like setDisable.
 */
class SettingsForeignWriteCoverageTest {

    @Test
    fun `every write outside ChatPreferences is still performed`() {
        val expected = File("src/test/resources/settings-foreign-writes.txt")
            .readLines().map { it.trim() }.filter { it.isNotEmpty() }

        val composeDir = File("src/main/kotlin/kst4contest/view/compose")
        val sources = if (composeDir.isDirectory) {
            composeDir.walkTopDown().filter { it.extension == "kt" }
                .joinToString("\n") { it.readText() }
        } else {
            ""
        }

        /*
         * A name counts as covered when it appears as a call or as the target of a
         * function reference. The category writes reach ChatController through the
         * callbacks StationTabState takes, so the wiring in SettingsWindow is what
         * satisfies them.
         */
        val missing = expected.filterNot { sources.contains("$it(") || sources.contains("::$it") }

        assertTrue(missing.isEmpty(),
            "writes outside ChatPreferences not performed by any Compose source " +
                "(${missing.size} of ${expected.size}):\n" + missing.joinToString("\n") { "  $it" })
    }
}
