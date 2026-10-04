package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.model.PaletteRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The three layers and which one wins, role by role.
 *
 * The provenance is not decoration. Two sources that can decide the same colour is a place
 * where nobody can later say why a colour is what it is, and saying so per role is the one
 * thing that makes the arrangement defensible -- so it is pinned as hard as the colours.
 */
class PaletteResolutionTest {

    private val shipped = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")

    private val red = Color(0xFFFF0000)
    private val green = Color(0xFF00FF00)

    @Test
    fun withNothingSetEveryRoleComesFromTheShippedSheet() {
        val resolved = resolvePalette(shipped, emptyMap(), emptyMap())

        assertEquals(PaletteRole.values().toSet(), resolved.roles.keys)
        resolved.roles.forEach { (role, it) ->
            assertEquals(PaletteSource.SHIPPED, it.source, "$role")
        }
        assertEquals(shipped.base, resolved.palette.base)
        assertEquals(shipped.accent, resolved.palette.accent)
    }

    @Test
    fun aFileOverridesTheShippedSheetForTheRolesItNames() {
        val resolved = resolvePalette(shipped, mapOf(PaletteRole.SURFACE to red), emptyMap())

        assertEquals(red, resolved.roles[PaletteRole.SURFACE]!!.colour)
        assertEquals(PaletteSource.FILE, resolved.roles[PaletteRole.SURFACE]!!.source)

        // And only those roles.
        assertEquals(PaletteSource.SHIPPED, resolved.roles[PaletteRole.ACCENT]!!.source)
        assertEquals(shipped.accent, resolved.roles[PaletteRole.ACCENT]!!.colour)
    }

    @Test
    fun aChangeOverridesBothTheFileAndTheShippedSheet() {
        val resolved = resolvePalette(
            shipped,
            mapOf(PaletteRole.SURFACE to red),
            mapOf(PaletteRole.SURFACE to green),
        )

        assertEquals(green, resolved.roles[PaletteRole.SURFACE]!!.colour)
        assertEquals(PaletteSource.CHANGED, resolved.roles[PaletteRole.SURFACE]!!.source)
    }

    @Test
    fun theResolvedRolesReachThePaletteTheWindowsDraw() {
        val resolved = resolvePalette(
            shipped,
            emptyMap(),
            mapOf(
                PaletteRole.SURFACE to red,
                PaletteRole.WINDOW_SURFACE to green,
                PaletteRole.FIELD_INTERIOR to red,
                PaletteRole.TEXT to green,
                PaletteRole.ACCENT to red,
                PaletteRole.SEPARATOR to green,
            ),
        )

        assertEquals(red, resolved.palette.base)
        assertEquals(green, resolved.palette.windowBackground)
        assertEquals(red, resolved.palette.controlInnerBackground)
        assertEquals(green, resolved.palette.labelTextFill)
        assertEquals(red, resolved.palette.accent)
        assertEquals(green, resolved.palette.separatorLine)
    }

    @Test
    fun theDerivedFieldsStayWithTheShippedSheet() {
        // The hover gradient, the pressed border and the font size are not roles the operator
        // sets here; they follow the shipped sheet rather than becoming further decisions.
        val resolved = resolvePalette(shipped, emptyMap(), mapOf(PaletteRole.SURFACE to red))

        assertEquals(shipped.buttonHoverGradient, resolved.palette.buttonHoverGradient)
        assertEquals(shipped.buttonPressedBorder, resolved.palette.buttonPressedBorder)
        assertEquals(shipped.fontSizePx, resolved.palette.fontSizePx)
    }

    @Test
    fun aChosenAccentIsAlsoTheAccentTheOperatorActuallySees() {
        /*
         * Theme.kt maps Material's primary onto textAccent and not onto accent, because
         * -fx-accent only paints JavaFX's selection bar while the green of .text-field .text
         * is what reads as the accent. Leave textAccent on the sheet's green and setting the
         * accent changes almost nothing visible -- a setting written through and not drawn,
         * which is precisely the fault class the drawing-layer tests exist for.
         */
        val resolved = resolvePalette(shipped, emptyMap(), mapOf(PaletteRole.ACCENT to red))

        assertEquals(red, resolved.palette.accent)
        assertEquals(red, resolved.palette.textAccent)
    }

    @Test
    fun anUntouchedAccentLeavesTheSheetsOwnTextAccentAlone() {
        // Without an override the sheet's gradient midpoint stands; it is not the accent.
        val resolved = resolvePalette(shipped, emptyMap(), mapOf(PaletteRole.SURFACE to red))

        assertEquals(shipped.textAccent, resolved.palette.textAccent)
    }

    @Test
    fun aFileRestatingTheShippedAccentDoesNotRepaintTheTextAccent() {
        /*
         * The root profile's stylesheet is written by copyResourceIfRequired at startup and
         * is the shipped sheet, so layer 2 declares the accent on every existing installation
         * with nobody having chosen anything. Keying the text accent on provenance rather
         * than on the value repainted the evening primary from green to blue on upgrade.
         */
        val evening = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")

        val resolved = resolvePalette(
            evening,
            mapOf(PaletteRole.ACCENT to evening.accent),
            emptyMap(),
        )

        assertEquals(PaletteSource.FILE, resolved.roles[PaletteRole.ACCENT]!!.source)
        assertEquals(
            evening.textAccent,
            resolved.palette.textAccent,
            "a file that merely restates the shipped accent changed the accent that is seen",
        )
    }

    @Test
    fun resettingToTheShippedAccentRestoresTheShippedTextAccent() {
        /*
         * resetToShipped writes all six roles as explicit overrides, because it has to beat a
         * stylesheet file. If the text accent follows provenance, the one button the spec
         * names as producing "exactly the shipped colours" produces something else -- and
         * disagrees with "discard my changes", which clears layer 3 and does restore it.
         */
        val resolved = resolvePalette(
            shipped,
            emptyMap(),
            mapOf(PaletteRole.ACCENT to shipped.accent),
        )

        assertEquals(PaletteSource.CHANGED, resolved.roles[PaletteRole.ACCENT]!!.source)
        assertEquals(shipped.textAccent, resolved.palette.textAccent)
    }

    @Test
    fun theWindowSurfaceIsNotDraggedAlongByTheSurface() {
        /*
         * The reason these are two roles rather than one: the sheets state the window
         * surface separately, and an operator who sets only the control surface must not
         * have the window surface changed under them.
         */
        val resolved = resolvePalette(shipped, emptyMap(), mapOf(PaletteRole.SURFACE to red))

        assertEquals(red, resolved.palette.base)
        assertEquals(shipped.windowBackground, resolved.palette.windowBackground)
    }
}
