package kst4contest.view.compose.tabs

import androidx.compose.ui.graphics.Color
import kst4contest.model.PaletteRole
import kst4contest.view.compose.JavaFxStylesheet
import kst4contest.view.compose.PaletteSource
import kst4contest.view.compose.OperatorProfilePaletteFiles
import kst4contest.view.compose.PaletteStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The colours tab as a typed facade over the store.
 *
 * Built over a store made of lambdas, so nothing here touches the operator's home directory.
 * What is pinned is what the tab shows and what it refuses: a role's colour, where that
 * colour came from, and the fact that nonsense typed into a field changes nothing.
 */
class ColoursTabStateTest {

    private val stored = mutableMapOf(false to "", true to "")
    private var fileRoles: Map<PaletteRole, Color> = emptyMap()
    private var darkMode = false

    private fun state(): ColoursTabState {
        val store = PaletteStore(
            shippedOf = { dark ->
                JavaFxStylesheet.read(
                    if (dark) "/KST4ContestDefaultEvening.css" else "/KST4ContestDefaultDay.css"
                )
            },
            fileRolesOf = { fileRoles },
            storedOverridesOf = { dark -> stored.getValue(dark) },
            storeOverrides = { dark, encoded -> stored[dark] = encoded },
        )
        return ColoursTabState(
            store,
            { darkMode },
            OperatorProfilePaletteFiles(
                dayFile = File("/nonexistent/day.css"),
                eveningFile = File("/nonexistent/evening.css"),
            ),
        )
    }

    @Test
    fun everyRoleIsShownWithItsColourAndItsProvenance() {
        val tab = state()

        val rows = tab.roles()

        assertEquals(PaletteRole.values().toList(), rows.map { it.role })
        rows.forEach {
            assertEquals(PaletteSource.SHIPPED, it.source, "${it.role}")
            assertTrue(it.hex.matches(Regex("#[0-9A-F]{6}")), "${it.role} shows '${it.hex}'")
            assertTrue(it.label.isNotBlank(), "${it.role} has no label")
        }
    }

    @Test
    fun aSetColourIsShownAsChanged() {
        val tab = state()

        val refusal = tab.set(PaletteRole.ACCENT, "#FF0000")

        val row = tab.roles().single { it.role == PaletteRole.ACCENT }
        assertEquals("#FF0000", row.hex)
        assertEquals(PaletteSource.CHANGED, row.source)
        assertNull(refusal, "a valid colour was refused: $refusal")
    }

    @Test
    fun aRoleTakenFromTheFileSaysSo() {
        /*
         * The mitigation the spec calls mandatory: with two sources that can decide the same
         * colour, "why is this colour what it is" has to be answerable per role.
         */
        fileRoles = mapOf(PaletteRole.ACCENT to Color(0xFF00FF00))
        val tab = state()

        assertEquals(
            PaletteSource.FILE,
            tab.roles().single { it.role == PaletteRole.ACCENT }.source,
        )
    }

    @Test
    fun nonsenseTypedIntoAFieldChangesNothingAndIsReported() {
        val tab = state()
        val before = tab.roles().single { it.role == PaletteRole.TEXT }.hex

        /*
         * The refusal is returned rather than pushed, which is the contract Form.committed
         * expects: it puts the field back to the stored value and shows the reason.
         */
        for (nonsense in listOf("rot", "", "#GGG", "#1234567", "ECECEC", "   ")) {
            val refusal = tab.set(PaletteRole.TEXT, nonsense)
            assertTrue(
                refusal != null && refusal.isNotBlank(),
                "'$nonsense' was accepted or refused without a reason",
            )
        }

        assertEquals(before, tab.roles().single { it.role == PaletteRole.TEXT }.hex)
        assertEquals(PaletteSource.SHIPPED, tab.roles().single { it.role == PaletteRole.TEXT }.source)
    }

    @Test
    fun settingARoleToTheColourItAlreadyHasIsNotARefusal() {
        /*
         * set() decides by comparing the colour before and after, so a valid colour that
         * happens to equal the current one changes nothing -- and reporting that as "'#ECECEC'
         * is not a colour" would be a lie the operator cannot argue with. The field commits on
         * focus loss, so this happens whenever somebody tabs through without editing.
         */
        val tab = state()
        val unchanged = tab.roles().single { it.role == PaletteRole.SURFACE }.hex

        assertNull(tab.set(PaletteRole.SURFACE, unchanged), "an unchanged colour was refused")
        assertNull(tab.set(PaletteRole.SURFACE, unchanged.lowercase()), "case alone was refused")
    }

    @Test
    fun theRowsFollowTheActiveDesign() {
        val tab = state()
        tab.set(PaletteRole.ACCENT, "#FF0000")

        darkMode = true

        // The evening design has its own overrides; the daylight change must not show here.
        assertEquals(
            PaletteSource.SHIPPED,
            tab.roles().single { it.role == PaletteRole.ACCENT }.source,
        )
    }

    @Test
    fun theWarningsDescribeTheResolvedPaletteAgainstTheShippedOne() {
        val tab = state()
        assertTrue(tab.warnings().isEmpty(), "the untouched state warns: ${tab.warnings()}")

        tab.set(PaletteRole.TEXT, "#FFFFFF")

        // White label text on the light window surface is unreadable and worse than shipped.
        assertTrue(
            tab.warnings().any { it.what.contains("Text on the window surface") },
            "no warning for white on light grey: ${tab.warnings()}",
        )
    }

    @Test
    fun theLifelineColoursComeFromTheShippedSheet() {
        /*
         * The reason this accessor exists at all. A tab reading its rescue colours from the
         * resolved palette would draw them in exactly the palette they exist to undo.
         */
        val tab = state()
        val shippedBase = tab.shipped().base

        PaletteRole.values().forEach { tab.set(it, "#000000") }

        assertEquals(shippedBase, tab.shipped().base)
        assertFalse(shippedBase == Color.Black, "the shipped base is black, so this proves nothing")
    }

    @Test
    fun theThreeWaysBackDoTheThreeDifferentThings() {
        fileRoles = mapOf(PaletteRole.ACCENT to Color(0xFF00FF00))
        val tab = state()

        // Back to how it was when the tab opened, including a change already present.
        tab.set(PaletteRole.ACCENT, "#FF0000")
        tab.rememberCurrentState()
        tab.set(PaletteRole.ACCENT, "#0000FF")
        tab.restoreRemembered()
        assertEquals("#FF0000", tab.roles().single { it.role == PaletteRole.ACCENT }.hex)

        // Discard my changes: the file applies again.
        tab.discardChanges()
        assertEquals(
            PaletteSource.FILE,
            tab.roles().single { it.role == PaletteRole.ACCENT }.source,
        )

        // Back to shipped: beats the file too.
        tab.resetToShipped()
        val row = tab.roles().single { it.role == PaletteRole.ACCENT }
        assertEquals(PaletteSource.CHANGED, row.source)
        assertEquals(tab.shipped().accent, kst4contest.view.compose.colourOf(row.hex))
    }

    @Test
    fun theFileRouteNamesThisDesignsOwnStylesheet() {
        val tab = state()

        assertTrue(tab.stylesheetPath().endsWith("day.css"), tab.stylesheetPath())
        assertFalse(tab.stylesheetExists(), "a nonexistent file was reported as present")

        darkMode = true

        assertTrue(tab.stylesheetPath().endsWith("evening.css"), tab.stylesheetPath())
    }
}
