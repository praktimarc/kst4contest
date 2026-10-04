package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * The colours the Compose windows show come from here, and nothing that renders is
 * covered by a test. This class is where that gap is closed: the reader takes plain
 * text and gives plain values.
 */
class JavaFxStylesheetTest {

    private fun hex(color: Color): String = String.format(
        "#%02X%02X%02X",
        Math.round(color.red * 255),
        Math.round(color.green * 255),
        Math.round(color.blue * 255),
    )

    @Test
    fun `derive gives the values JavaFX gives`() {
        /*
         * Both halves matter. The expected strings were read out of JavaFX and are
         * asserted outright, so the reader still has to produce them if the delegation
         * is ever replaced. The cross-check below then proves the delegation itself
         * still works, which a JavaFX upgrade could break silently: the package is
         * internal and reached by reflection.
         */
        val expected = mapOf(
            ("#373E43" to 26.4) to "#525B61",
            ("#373E43" to 35.0) to "#5B646A",
            ("#373E43" to -20.0) to "#2C3236",
            ("#ECECEC" to 26.4) to "#F4F4F4",
            ("#ECECEC" to -20.0) to "#BDBDBD",
            ("#FFFFFF" to -30.0) to "#B2B2B2",
        )

        expected.forEach { (input, want) ->
            val (rgb, percent) = input

            /*
             * Compared with a tolerance of one step per channel. derive(white, -30%)
             * lands on 0.7 * 255 = 178.5 exactly, and the two paths round that half
             * step differently; a test that flips on a half step pins nothing useful.
             */
            assertChannelsClose(want, JavaFxStylesheet.derive(parse(rgb), percent),
                "derive($rgb, $percent%)")
            assertChannelsClose(want, parse(deriveWithJavaFx(rgb, percent)),
                "JavaFX itself no longer gives this; the pinned value is stale")
        }
    }

    @Test
    fun `the window background is not the base colour`() {
        val palette = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")

        assertEquals("#373E43", hex(palette.base))
        assertEquals("#525B61", hex(palette.windowBackground),
            "Modena paints a window with derive(-fx-base, 26.4%), not with -fx-base; "
                + "taking the base raw made the Compose window visibly darker than the "
                + "JavaFX one beside it",
        )
    }

    @Test
    fun `the evening sheet gives its declared colours`() {
        val palette = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")

        assertEquals("#373E43", hex(palette.base), "-fx-base")
        assertEquals("#1E74C6", hex(palette.accent), "-fx-accent")
        assertEquals("#D3D3D3", hex(palette.labelTextFill), ".label -fx-text-fill is lightgray")
        assertEquals("#3C3C3C", hex(palette.separatorLine), ".separator *.line")
        assertEquals("#FF0000", hex(palette.buttonPressedBorder), ".button:pressed")
        assertEquals(12f, palette.fontSizePx)
    }

    @Test
    fun `the control interior resolves its reference before deriving`() {
        val palette = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")

        assertEquals(
            hex(JavaFxStylesheet.derive(palette.base, 35.0)),
            hex(palette.controlInnerBackground),
            "-fx-control-inner-background is derive(-fx-base, 35%): the reference to "
                + "-fx-base has to be looked up in the same sheet first",
        )
    }

    @Test
    fun `the accent is the green of the field text, not the declared accent`() {
        val evening = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")
        val day = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")

        assertEquals("#1E74C6", hex(evening.accent),
            "-fx-accent stays what the sheet says; it drives the JavaFX selection bar")

        // green (#008000) and lightgreen (#90EE90), averaged.
        assertEquals("#48B748", hex(evening.textAccent),
            ".text-field .text is what the operator reads as the accent")
        assertEquals("#48B748", hex(day.textAccent),
            "both sheets declare the same rule, so the accent does not change with "
                + "the design")
    }

    @Test
    fun `the hover gradient keeps its stops in order`() {
        val palette = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")

        assertTrue(palette.buttonHoverGradient.size >= 2,
            "the sheet declares linear-gradient(#f0ff35, #a9ff00)")
        assertEquals("#F0FF35", hex(palette.buttonHoverGradient[0]))
        assertEquals("#A9FF00", hex(palette.buttonHoverGradient[1]))
    }

    @Test
    fun `the daylight sheet falls back to Modena`() {
        val palette = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")

        // It declares only a font size and a few button rules; everything else is
        // JavaFX's own default theme.
        assertEquals("#ECECEC", hex(palette.base))
        assertEquals("#0096C9", hex(palette.accent))
        assertEquals("#FFFFFF", hex(palette.controlInnerBackground))
        assertEquals("#000000", hex(palette.labelTextFill))
        assertEquals(12f, palette.fontSizePx)
    }

    @Test
    fun `the daylight sheet still carries its own button rules`() {
        val palette = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")

        assertEquals("#FF0000", hex(palette.buttonPressedBorder),
            "the daylight sheet declares .button:pressed too")
    }

    @Test
    fun `an absent sheet yields the defaults instead of failing`() {
        val palette = JavaFxStylesheet.read("/there-is-no-such-sheet.css")

        assertEquals("#ECECEC", hex(palette.base),
            "a missing stylesheet must not stop a window from opening")
    }


    /** Each channel within one step of the expected colour. */
    private fun assertChannelsClose(expected: String, actual: Color, message: String) {
        val want = parse(expected)

        listOf(
            Triple("red", want.red, actual.red),
            Triple("green", want.green, actual.green),
            Triple("blue", want.blue, actual.blue),
        ).forEach { (channel, wanted, got) ->
            assertEquals(
                Math.round(wanted * 255).toDouble(),
                Math.round(got * 255).toDouble(),
                1.0,
                "$message: $channel",
            )
        }
    }

    private fun deriveWithJavaFx(rgb: String, percent: Double): String {
        val fxColor = Class.forName("javafx.scene.paint.Color")
        val utils = Class.forName("com.sun.javafx.util.Utils")
        val source = fxColor.getMethod("web", String::class.java).invoke(null, rgb)
        val result = utils
            .getMethod("deriveColor", fxColor, Double::class.javaPrimitiveType)
            .invoke(null, source, percent / 100.0)

        return hex(
            Color(
                (fxColor.getMethod("getRed").invoke(result) as Double).toFloat(),
                (fxColor.getMethod("getGreen").invoke(result) as Double).toFloat(),
                (fxColor.getMethod("getBlue").invoke(result) as Double).toFloat(),
            )
        )
    }

    private fun parse(rgb: String): Color {
        val value = rgb.removePrefix("#").toLong(16)
        return Color(0xFF000000L or value)
    }
}
