package kst4contest.view.compose.tabs

import androidx.compose.ui.graphics.Color
import kst4contest.view.compose.JavaFxStylesheet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kst4contest.view.compose.CONTRAST_FLOOR
import kst4contest.view.compose.contrastRatio

/**
 * The colours of the one control that refuses the operator's palette.
 *
 * The fault this guards against closes itself in: an operator who sets text and surface to
 * the same black can no longer see the buttons that would undo it. So this control is drawn
 * from the shipped sheet, always, and that is checked here against a palette in which every
 * role has been changed.
 *
 * The composable itself takes the shipped palette as a required parameter and never reads
 * LocalJavaFxPalette, so it has no way to reach the resolved palette at all. These tests
 * cover the colour decision; that it cannot be fed the wrong palette is structural.
 */
class LifelineColoursTest {

    private val shipped = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")

    @Test
    fun theColoursComeFromThePaletteItIsGiven() {
        val colours = lifelineColours(shipped)

        assertEquals(shipped.base, colours.container)
        assertEquals(shipped.labelTextFill, colours.content)
    }

    @Test
    fun aFullyBlackedOutPaletteGivesDifferentColoursThanTheShippedOne() {
        /*
         * The palette an operator locks themselves out with. If the lifeline ever read it,
         * these two would agree and the button would be invisible exactly when needed.
         */
        val blackedOut = shipped.copy(
            base = Color.Black,
            windowBackground = Color.Black,
            controlInnerBackground = Color.Black,
            labelTextFill = Color.Black,
            accent = Color.Black,
            separatorLine = Color.Black,
        )

        assertNotEquals(lifelineColours(shipped), lifelineColours(blackedOut))
    }

    @Test
    fun theShippedColoursAreReadableAgainstEachOther() {
        /*
         * A lifeline has to be legible on its own terms. Both shipped sheets pass here, which
         * is not true of every pair in them -- the accent does not -- so this is a real check
         * and not a restatement.
         */
        for (sheet in listOf("/KST4ContestDefaultDay.css", "/KST4ContestDefaultEvening.css")) {
            val colours = lifelineColours(JavaFxStylesheet.read(sheet))
            val ratio = contrastRatio(colours.content, colours.container)

            assertTrue(ratio >= CONTRAST_FLOOR, "$sheet gives the lifeline a ratio of $ratio")
        }
    }

    @Test
    fun theBorderIsVisibleAgainstTheContainer() {
        // The border is what makes it read as a control rather than as a patch of colour.
        val colours = lifelineColours(shipped)

        assertNotEquals(colours.container, colours.border)
    }
}
