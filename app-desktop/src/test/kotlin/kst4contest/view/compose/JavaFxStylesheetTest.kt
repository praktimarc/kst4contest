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
         * A handful of values spelled out, so the most-used derivations are readable
         * here rather than only in a data file. The full comparison against JavaFX
         * lives in `derive matches the frozen JavaFX reference`.
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
            /*
             * No live JavaFX comparison here. It lives in the frozen reference data
             * instead (javafx-derive-reference.txt), because a reflection call into
             * com.sun.javafx stops working when JavaFX is removed and would take the
             * only real check with it.
             */
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

    /** The reference pairs, as `(rgbHex, percent, expectedRgbHex)`. */
    private fun referencePairs(): List<Triple<String, Double, String>> {
        val resource = javaClass.getResourceAsStream("/javafx-derive-reference.txt")
            ?: error("javafx-derive-reference.txt is missing from the test resources")

        /*
         * Comment lines begin with "# "; a data line begins with "#" followed straight
         * by hex. The header's format line contains semicolons too, so filtering on
         * those would read it as data.
         */
        return resource.bufferedReader().readLines()
            .filterNot { it.isBlank() || it.startsWith("# ") }
            .map { line ->
                val (rgb, percent, expected) = line.split(";")
                Triple(rgb, percent.toDouble(), expected)
            }
    }

    /**
     * The one colour this reimplementation does not reproduce exactly.
     *
     * Found by widening the reference data past the greys the old pin covered. Every
     * grey and both stylesheet bases match JavaFX to within the rounding tolerance;
     * this saturated blue drifts by up to three steps per channel.
     *
     * Why that is tolerable, checked rather than assumed. derive() has four callers:
     * `Theme.kt:86` and `JavaFxStylesheet` lines 96 and 106 all pass the window
     * background or `-fx-base`, which are near grey. The fourth is the CSS parser at
     * `JavaFxStylesheet:249`, which passes whatever colour a sheet names inside a
     * `derive(...)` expression — and **every** such expression in both shipped sheets
     * derives from `-fx-base` and nothing else. So no saturated colour reaches derive()
     * today.
     *
     * That is a fact about the sheets as they are, not a guarantee. Etappe 8 of the
     * migration makes the colours operator-editable; a sheet with `derive(#4da6ff, 20%)`
     * in it would walk straight into this. Revisit then.
     *
     * Named rather than removed from the data: the data records what JavaFX does, and
     * hiding the pairs that disagree would turn the record into a flattering one.
     */
    private val knownDivergentColour = "#4da6ff"

    /**
     * derive() against the values JavaFX itself produced.
     *
     * Against checked-in data rather than a live reflection call, because the live call
     * disappears with JavaFX and would take the only real check with it — leaving a
     * test that compares derive() to derive().
     */
    @Test
    fun `derive matches the frozen JavaFX reference`() {
        val pairs = referencePairs()

        assertTrue(pairs.size >= 100) { "only ${pairs.size} reference pairs; the file looks truncated" }

        pairs.filterNot { (rgb, _, _) -> rgb == knownDivergentColour }
            .forEach { (rgb, percent, expected) ->
                assertChannelsClose(
                    expected,
                    JavaFxStylesheet.derive(parse(rgb), percent),
                    "derive($rgb, $percent%)",
                )
            }
    }

    /**
     * The known divergence, pinned so it cannot grow unnoticed.
     *
     * Three steps per channel is small enough to leave alone for a colour derive() is
     * never called with. It is not small enough to stop measuring: a later change that
     * widened it, or that started calling derive() with saturated colours, should fail
     * here rather than go unseen.
     */
    @Test
    fun `the known divergence from JavaFX stays within three steps`() {
        val divergent = referencePairs().filter { (rgb, _, _) -> rgb == knownDivergentColour }

        assertTrue(divergent.isNotEmpty()) { "the reference data no longer covers $knownDivergentColour" }

        divergent.forEach { (rgb, percent, expected) ->
            val got = JavaFxStylesheet.derive(parse(rgb), percent)
            val want = parse(expected)
            listOf(
                "red" to (want.red to got.red),
                "green" to (want.green to got.green),
                "blue" to (want.blue to got.blue),
            ).forEach { (channel, values) ->
                val (wanted, actual) = values
                val delta = Math.abs(Math.round(wanted * 255) - Math.round(actual * 255))
                assertTrue(delta <= 3) {
                    "derive($rgb, $percent%): $channel is $delta steps from JavaFX, was at most 3"
                }
            }
        }
    }

    /**
     * Writes the reference data the frozen test compares against.
     *
     * A generator, not an assertion: it runs once, while JavaFX is still on the
     * classpath, and its output is checked in. Kept rather than deleted so the
     * provenance of the data stays readable — the values come from
     * com.sun.javafx.util.Utils.deriveColor and from nowhere else, which is the whole
     * point of pinning them.
     *
     * To regenerate: remove @Disabled, run, restore @Disabled.
     */
    @org.junit.jupiter.api.Disabled("generator; run by hand while JavaFX is present")
    @Test
    fun `generate the JavaFX reference data`() {
        /*
         * The greys are chosen for where they land on the brightness staircase
         * deriveColor switches on — 0.2, 0.3, 0.4, 0.5, 0.6 and 0.85 of
         * 0.3R + 0.59G + 0.11B — and two of them are there because mutation testing
         * showed the first set left holes:
         *
         *   #e0e0e0 (0.878) sits above the 0.85 step but, unlike white and #f4f4f4,
         *   has room left to brighten, so changing that branch's 1.6 factor moves it
         *   further than the rounding tolerance. Without it, 1.6 could become 1.5 and
         *   every test stayed green.
         *
         *   #606060 (0.376) and #5a5a5a (0.353) sit between the 0.3 and 0.4 steps,
         *   which nothing else covered: the 0.4 threshold could be moved to 0.35
         *   unnoticed.
         */
        val colours = listOf(
            "#ffffff", "#000000", "#ececec", "#373e43", "#1d1d1d", "#4da6ff",
            "#2b2b2b", "#808080", "#333333", "#4d4d4d", "#666666", "#999999",
            "#d9d9d9", "#f4f4f4", "#63067a", "#ff9900",
            "#e0e0e0", "#eeeeee", "#606060", "#5a5a5a", "#707070", "#454545",
        )
        val percents = listOf(-80.0, -50.0, -35.0, -30.0, -10.0, 10.0, 26.4, 35.0, 50.0, 80.0)

        val lines = buildList {
            for (rgb in colours) {
                for (percent in percents) {
                    add("$rgb;$percent;${deriveWithJavaFx(rgb, percent)}")
                }
            }
        }

        val target = java.io.File("src/test/resources/javafx-derive-reference.txt")
        target.parentFile.mkdirs()
        target.writeText(
            "# Generated from com.sun.javafx.util.Utils.deriveColor by\n" +
                "# JavaFxStylesheetTest.`generate the JavaFX reference data`.\n" +
                "# Format: <rgbHex>;<percent>;<expectedRgbHex>\n" +
                lines.joinToString("\n") + "\n"
        )
        println("wrote ${lines.size} pairs to ${target.absolutePath}")
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
