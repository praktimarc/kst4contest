package kst4contest.view.compose

import kst4contest.model.PaletteRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Which roles a stylesheet actually names.
 *
 * Not the same question as `read`, and conflating the two is the trap of this stage: `read`
 * fills a missing role from the Modena defaults, so a file saying only `-fx-base` would
 * silently override all six. A layer needs presence, not a complete palette.
 *
 * How far apart the two questions are is visible in the shipped sheets themselves -- the
 * daylight one states no colour at all, and `read` still returns a full palette for it.
 */
class StylesheetDeclaredRolesTest {

    private fun shipped(name: String): String =
        JavaFxStylesheet::class.java.getResource(name)!!.readText()

    @Test
    fun aSheetNamingOneRoleDeclaresOnlyThatRole() {
        // The accent, deliberately: nothing is derived from it, so one named means one role.
        val declared = JavaFxStylesheet.declaredRoles(".root { -fx-accent: #0096C9; }")

        assertEquals(setOf(PaletteRole.ACCENT), declared.keys)
    }

    @Test
    fun aRoleStatedOutsideRootIsFoundAtItsOwnSelector() {
        /*
         * Three of the six live outside .root. A reader that only looked there would never
         * find the text colour or the separator line -- which is half the palette.
         */
        val declared = JavaFxStylesheet.declaredRoles(
            """
            .label { -fx-text-fill: #112233; }
            .separator *.line { -fx-background-color: #445566; }
            """.trimIndent()
        )

        assertEquals(setOf(PaletteRole.TEXT, PaletteRole.SEPARATOR), declared.keys)
    }

    @Test
    fun aSheetStatingTheBaseWithoutTheBackgroundStatesTheDerivedWindowSurfaceToo() {
        /*
         * What JavaFX itself does: Modena's -fx-background is derive(-fx-base, 26.4%), so a
         * sheet that states -fx-base and says nothing about -fx-background has stated the
         * window surface as well. Without this the most natural hand edit is nearly inert --
         * an operator sets -fx-base to #303030, restarts, and the windows stay light grey,
         * because base reaches the screen only through the menu strip and the selection tint.
         *
         * The file layer only. In the settings tab the two stay independent, which is the
         * whole reason the spec keeps them as separate roles: six fields are visible there and
         * the operator can set both.
         */
        val declared = JavaFxStylesheet.declaredRoles(".root { -fx-base: #303030; }")

        assertEquals(setOf(PaletteRole.SURFACE, PaletteRole.WINDOW_SURFACE), declared.keys)
        assertEquals(
            JavaFxStylesheet.derive(declared.getValue(PaletteRole.SURFACE), 26.4),
            declared.getValue(PaletteRole.WINDOW_SURFACE),
        )
    }

    @Test
    fun aStatedBackgroundIsNotReplacedByADerivedOne() {
        val declared = JavaFxStylesheet.declaredRoles(
            ".root { -fx-base: #303030; -fx-background: #112233; }"
        )

        assertEquals(colourOf("#112233"), declared.getValue(PaletteRole.WINDOW_SURFACE))
    }

    @Test
    fun aSheetWithNoBaseDerivesNoWindowSurface() {
        val declared = JavaFxStylesheet.declaredRoles(".label { -fx-text-fill: #112233; }")

        assertEquals(setOf(PaletteRole.TEXT), declared.keys)
    }

    @Test
    fun theSamePropertyUnderAnotherSelectorIsNotARole() {
        // -fx-background-color appears a dozen times in the evening sheet. One is the role.
        val declared = JavaFxStylesheet.declaredRoles(
            ".scroll-bar { -fx-background-color: #445566; } .button:hover { -fx-text-fill: red; }"
        )

        assertTrue(declared.isEmpty(), "found roles where there are none: ${declared.keys}")
    }

    @Test
    fun aSheetNamingNothingDeclaresNothing() {
        assertTrue(JavaFxStylesheet.declaredRoles("").isEmpty())
        assertTrue(JavaFxStylesheet.declaredRoles("/* just a comment */").isEmpty())
        assertTrue(JavaFxStylesheet.declaredRoles(".button { -fx-padding: 2px; }").isEmpty())
    }

    @Test
    fun theShippedDaylightSheetStatesNoColourAtAll() {
        /*
         * Measured, not assumed: its .root carries only -fx-font-size, and every colour in
         * the daylight design comes from JavaFX's Modena defaults by way of `read`. If this
         * ever starts declaring a role, the daylight palette has gained a second source and
         * the resolver's layer 1 needs looking at.
         */
        val declared = JavaFxStylesheet.declaredRoles(shipped("/KST4ContestDefaultDay.css"))

        assertTrue(declared.isEmpty(), "the daylight sheet now declares ${declared.keys}")
    }

    @Test
    fun theShippedEveningSheetStatesEveryRole() {
        /*
         * It states the surface, the field interior, the accent, the label text and the
         * separator line outright, and the window surface by implication -- it names -fx-base
         * and no -fx-background, which is how JavaFX derives one. This pins the
         * selector-and-property mapping against a real file: get a selector wrong and a role
         * drops out without any other test noticing.
         */
        val declared = JavaFxStylesheet.declaredRoles(shipped("/KST4ContestDefaultEvening.css"))

        assertEquals(PaletteRole.values().toSet(), declared.keys)
    }

    @Test
    fun aMissingFileDeclaresNothingRatherThanThrowing() {
        val absent = File("/nonexistent/kst4contest/does-not-exist.css")

        assertTrue(JavaFxStylesheet.declaredRolesOfFile(absent).isEmpty())
    }

    @Test
    fun anUnreadableOrNonsenseFileDeclaresNothingRatherThanThrowing() {
        /*
         * A client that will not start because of a broken colour file is worse than one in
         * the wrong colours. Half-written files and files that are not CSS at all both exist,
         * and this one is hand-edited by the operator.
         */
        val nonsense = File.createTempFile("kst4contest-palette", ".css")
        try {
            nonsense.writeText("this is not css { -fx-base")
            assertTrue(JavaFxStylesheet.declaredRolesOfFile(nonsense).isEmpty())
        } finally {
            nonsense.delete()
        }

        val directory = File.createTempFile("kst4contest-palette-dir", "").let {
            it.delete()
            it.mkdirs()
            it
        }
        try {
            assertTrue(JavaFxStylesheet.declaredRolesOfFile(directory).isEmpty())
        } finally {
            directory.delete()
        }
    }
}
