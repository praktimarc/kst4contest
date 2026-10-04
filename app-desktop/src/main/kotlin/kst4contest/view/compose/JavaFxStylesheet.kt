package kst4contest.view.compose

import androidx.compose.ui.graphics.Color

/**
 * The colours the Compose windows take from the JavaFX stylesheets.
 *
 * @param fontSizePx the `-fx-font-size` of `.root`; both shipped sheets set 12px
 */
data class JavaFxPalette(
    val base: Color,
    /**
     * What a window is actually painted with. Modena's `-fx-background` is
     * `derive(-fx-base, 26.4%)`, not `-fx-base` itself: taking the base raw makes a
     * Compose window visibly darker than the JavaFX one beside it.
     */
    val windowBackground: Color,
    val accent: Color,
    /**
     * The colour the operator actually reads as the accent.
     *
     * `-fx-accent` only drives JavaFX's own selection bar, which barely shows. What is
     * seen instead is `.text-field .text`: both sheets fill the text inside every field
     * — and the table column headers of the evening sheet — with a green gradient. A
     * solid colour is needed here, so the gradient is collapsed to its midpoint.
     */
    val textAccent: Color,
    val controlInnerBackground: Color,
    val labelTextFill: Color,
    val separatorLine: Color,
    val buttonHoverGradient: List<Color>,
    val buttonPressedBorder: Color,
    val fontSizePx: Float,
)

/**
 * Reads the colours of a JavaFX stylesheet.
 *
 * This is deliberately not a CSS engine. It looks up the handful of declarations the
 * Compose windows need and understands only what those declarations actually use:
 * colour literals, named colours, references to another `-fx-` value in the same
 * sheet, `derive(colour, percent)` and the stops of a `linear-gradient`.
 *
 * **The source is the classpath resource, the same one JavaFX loads.** The copies
 * under the application directory are written at startup but never read back:
 * `applyThemeStylesheet` passes the bare file name to `Scene.getStylesheets()`, and
 * JavaFX resolves a relative stylesheet name against the classpath. Reading the copy
 * here would make the two windows disagree exactly when an operator edits it.
 *
 * The daylight sheet declares almost nothing and relies on JavaFX's own Modena
 * defaults, so every lookup falls back to the matching Modena value.
 */
object JavaFxStylesheet {

    /** Modena's own `-fx-background: derive(-fx-base, 26.4%)`. */
    private const val MODENA_BACKGROUND_DERIVE = 26.4

    /* Modena's defaults, used wherever a sheet declares nothing. */
    private val MODENA_BASE = Color(0xFFECECEC)
    private val MODENA_ACCENT = Color(0xFF0096C9)
    private val MODENA_CONTROL_INNER = Color(0xFFFFFFFF)
    private val MODENA_TEXT = Color(0xFF000000)

    private val NAMED_COLORS = mapOf(
        "white" to Color.White,
        "black" to Color.Black,
        "lightgray" to Color(0xFFD3D3D3),
        "lightgrey" to Color(0xFFD3D3D3),
        "gray" to Color(0xFF808080),
        "grey" to Color(0xFF808080),
        "darkgray" to Color(0xFFA9A9A9),
        "green" to Color(0xFF008000),
        "lightgreen" to Color(0xFF90EE90),
        "red" to Color(0xFFFF0000),
        "blue" to Color(0xFF0000FF),
        "yellow" to Color(0xFFFFFF00),
        "orange" to Color(0xFFFFA500),
        "transparent" to Color.Transparent,
    )

    /**
     * @param resourcePath classpath path of the sheet, for example
     *        `/KST4ContestDefaultEvening.css`
     */
    fun read(resourcePath: String): JavaFxPalette {
        val source = JavaFxStylesheet::class.java.getResource(resourcePath)?.readText().orEmpty()
        val blocks = parseBlocks(source)
        val root = blocks[".root"].orEmpty()

        val base = colorOf(root["-fx-base"], root) ?: MODENA_BASE
        val accent = colorOf(root["-fx-accent"], root) ?: MODENA_ACCENT

        return JavaFxPalette(
            base = base,
            windowBackground = colorOf(root["-fx-background"], root)
                ?: derive(base, MODENA_BACKGROUND_DERIVE),
            textAccent = midpointOf(
                gradientStops(blocks[".text-field .text"]?.get("-fx-fill"), root)
            ) ?: accent,
            accent = accent,
            controlInnerBackground = colorOf(root["-fx-control-inner-background"], root)
                ?: MODENA_CONTROL_INNER,
            labelTextFill = colorOf(blocks[".label"]?.get("-fx-text-fill"), root) ?: MODENA_TEXT,
            /* Modena draws the line one fifth darker than the base. */
            separatorLine = colorOf(blocks[".separator *.line"]?.get("-fx-background-color"), root)
                ?: derive(base, -20.0),
            buttonHoverGradient = gradientStops(
                blocks[".button:hover"]?.get("-fx-background-color"), root
            ),
            buttonPressedBorder = colorOf(blocks[".button:pressed"]?.get("-fx-border-color"), root)
                ?: accent,
            fontSizePx = root["-fx-font-size"]?.removeSuffix("px")?.trim()?.toFloatOrNull() ?: 12f,
        )
    }

    /**
     * JavaFX `derive(colour, percent)`, reimplemented.
     *
     * No JavaFX call and no reflection: this is the algorithm itself. The brightness it
     * derives from is not the HSB value but `0.3R + 0.59G + 0.11B`, and the positive
     * branch follows a staircase rather than a formula, which is why an earlier
     * approximation here was measurably wrong.
     *
     * Correctness is not argued, it is pinned: `JavaFxStylesheetTest` checks this against
     * values the real JavaFX colour-derivation routine produced, frozen into
     * `javafx-derive-reference.txt` so the check survives JavaFX being removed. That
     * file's header names the exact class the values came from.
     * Every grey and both stylesheet bases match. One saturated blue drifts by up to
     * three steps per channel. That is tolerable only because no saturated colour
     * reaches here: three of the four call sites pass the window background or
     * `-fx-base`, and the fourth is [parseColour]'s own `derive(...)` branch below,
     * which passes whatever a sheet names — and every `derive(...)` in both shipped
     * sheets derives from `-fx-base` alone. A hand-edited sheet could break that, so
     * Etappe 8 of the migration, which makes the sheets operator-editable, has to
     * revisit it.
     */
    fun derive(color: Color, percent: Double): Color {
        val baseBrightness = 0.3 * color.red + 0.59 * color.green + 0.11 * color.blue
        var calcBrightness = percent / 100.0

        if (calcBrightness > 0) {
            if (baseBrightness > 0.85) {
                calcBrightness *= 1.6
            } else if (baseBrightness > 0.6) {
                // no change
            } else if (baseBrightness > 0.5) {
                calcBrightness *= 0.9
            } else if (baseBrightness > 0.4) {
                calcBrightness *= 0.8
            } else if (baseBrightness > 0.3) {
                calcBrightness *= 0.7
            } else {
                calcBrightness *= 0.6
            }
        } else {
            if (baseBrightness < 0.2) {
                calcBrightness *= 0.6
            }
        }

        calcBrightness = calcBrightness.coerceIn(-1.0, 1.0)

        // RGBtoHSB
        val r = color.red.toDouble()
        val g = color.green.toDouble()
        val b = color.blue.toDouble()
        val cmax = maxOf(r, g, b)
        val cmin = minOf(r, g, b)
        val brightness = cmax
        val saturation = if (cmax != 0.0) (cmax - cmin) / cmax else 0.0
        var hue = 0.0

        if (saturation != 0.0) {
            val redc = (cmax - r) / (cmax - cmin)
            val greenc = (cmax - g) / (cmax - cmin)
            val bluec = (cmax - b) / (cmax - cmin)
            hue = when {
                r == cmax -> bluec - greenc
                g == cmax -> 2.0 + redc - bluec
                else -> 4.0 + greenc - redc
            }
            hue /= 6.0
            if (hue < 0) {
                hue += 1.0
            }
        }

        hue *= 360.0
        var derivedSat = saturation
        var derivedBri = brightness

        if (calcBrightness > 0) {
            derivedSat *= (1.0 - calcBrightness)
            derivedBri += (1.0 - derivedBri) * calcBrightness
        } else {
            derivedBri *= (calcBrightness + 1.0)
        }

        derivedSat = derivedSat.coerceIn(0.0, 1.0)
        derivedBri = derivedBri.coerceIn(0.0, 1.0)

        return Color.hsv(hue.toFloat(), derivedSat.toFloat(), derivedBri.toFloat(), color.alpha)
    }

    /** `selector { property: value; ... }`, comments removed. */
    private fun parseBlocks(source: String): Map<String, Map<String, String>> {
        val withoutComments = source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        val blocks = mutableMapOf<String, MutableMap<String, String>>()

        Regex("([^{}]+)\\{([^{}]*)\\}").findAll(withoutComments).forEach { match ->
            val selector = match.groupValues[1].trim().replace(Regex("\\s+"), " ")
            val declarations = blocks.getOrPut(selector) { mutableMapOf() }

            splitOnTopLevel(match.groupValues[2], ';').forEach { declaration ->
                val separator = declaration.indexOf(':')
                if (separator > 0) {
                    declarations[declaration.take(separator).trim()] =
                        declaration.substring(separator + 1).trim()
                }
            }
        }

        return blocks
    }

    /**
     * Resolves one value to a colour, or null when it is absent or is something this
     * reader does not need to understand.
     *
     * @param variables the `.root` declarations, where `-fx-` references are looked up
     */
    private fun colorOf(value: String?, variables: Map<String, String>, depth: Int = 0): Color? {
        val text = value?.trim()?.removeSuffix(";")?.trim() ?: return null

        /* A malformed sheet must not spin here: -fx-a: -fx-b; -fx-b: -fx-a; */
        if (depth > 8 || text.isEmpty()) {
            return null
        }

        NAMED_COLORS[text.lowercase()]?.let { return it }

        if (text.startsWith("#")) {
            return hexColor(text)
        }

        if (text.startsWith("-fx-")) {
            return colorOf(variables[text], variables, depth + 1)
        }

        if (text.startsWith("derive(")) {
            val arguments = splitOnTopLevel(text.removePrefix("derive(").dropLast(1), ',')
            if (arguments.size == 2) {
                val source = colorOf(arguments[0], variables, depth + 1) ?: return null
                val percent = arguments[1].removeSuffix("%").trim().toDoubleOrNull() ?: return null
                return derive(source, percent)
            }
            return null
        }

        if (text.contains("linear-gradient(")) {
            return gradientStops(text, variables).firstOrNull()
        }

        return null
    }

    /**
     * The colour stops of the first `linear-gradient` in a value. A stop may carry a
     * position (`green 0%`) or a direction (`from 0% 0% to 100% 200%`); only the
     * colours are wanted here.
     */
    private fun gradientStops(value: String?, variables: Map<String, String>): List<Color> {
        val text = value?.trim() ?: return emptyList()
        val start = text.indexOf("linear-gradient(")

        if (start < 0) {
            return emptyList()
        }

        var depth = 0
        var end = -1

        for (index in start + "linear-gradient".length until text.length) {
            when (text[index]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) {
                        end = index
                        break
                    }
                }
            }
        }

        if (end < 0) {
            return emptyList()
        }

        return splitOnTopLevel(text.substring(start + "linear-gradient(".length, end), ',')
            .filterNot { it.startsWith("from ") || it.startsWith("to ") }
            .mapNotNull { colorOf(it.split(Regex("\\s+")).first(), variables) }
    }

    /**
     * Collapses a gradient to one colour by averaging its ends. Compose paints solid
     * where JavaFX paints a gradient across the glyphs, and the average is what the eye
     * takes from the gradient anyway.
     */
    private fun midpointOf(stops: List<Color>): Color? {
        if (stops.isEmpty()) {
            return null
        }

        val first = stops.first()
        val last = stops.last()

        return Color(
            red = (first.red + last.red) / 2f,
            green = (first.green + last.green) / 2f,
            blue = (first.blue + last.blue) / 2f,
            alpha = 1f,
        )
    }

    /** Splits on a separator that is not inside brackets. */
    private fun splitOnTopLevel(text: String, separator: Char): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0

        text.forEachIndexed { index, character ->
            when (character) {
                '(' -> depth++
                ')' -> depth--
                separator -> if (depth == 0) {
                    parts.add(text.substring(start, index))
                    start = index + 1
                }
            }
        }

        parts.add(text.substring(start))
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun hexColor(text: String): Color? {
        val digits = text.removePrefix("#")

        val expanded = when (digits.length) {
            3 -> digits.toCharArray().joinToString("") { "$it$it" }
            6 -> digits
            else -> return null
        }

        val value = expanded.toLongOrNull(16) ?: return null
        return Color(0xFF000000L or value)
    }
}
