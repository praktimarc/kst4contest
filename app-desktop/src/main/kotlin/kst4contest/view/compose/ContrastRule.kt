package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG AA for body text. Below this a pair is reported, never refused. */
const val CONTRAST_FLOOR = 4.5

/** The pairs that actually sit on top of each other, as the tab names them. */
enum class ContrastPair { TEXT_ON_WINDOW_SURFACE, TEXT_INSIDE_FIELD, ACCENT_ON_WINDOW_SURFACE, TEXT_ON_MENU_STRIP }

/**
 * One pair that reads badly.
 *
 * Carries the pair itself and not its name: this is a pure function, and a translated string
 * here would make every test that filters on it depend on whichever language ran last in the
 * shared JVM. The tab turns it into words.
 *
 * @param ratio what the pair measures now
 * @param shippedRatio what the same pair measures in the shipped sheet, so the tab can say
 *        how far the change moved it
 */
data class ContrastWarning(val what: ContrastPair, val ratio: Double, val shippedRatio: Double)

/**
 * The WCAG contrast ratio of two colours, between 1.0 and 21.0.
 *
 * Relative luminance and not brightness: the two are easy to confuse, and the JavaFX colour
 * derivation in this same package is a standing reminder that guessing a colour formula
 * produces values that look plausible and are wrong.
 */
fun contrastRatio(a: Color, b: Color): Double {
    val lighter = maxOf(relativeLuminance(a), relativeLuminance(b))
    val darker = minOf(relativeLuminance(a), relativeLuminance(b))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(colour: Color): Double {
    fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    return 0.2126 * channel(colour.red) +
            0.7152 * channel(colour.green) +
            0.0722 * channel(colour.blue)
}

/**
 * The pairs that actually sit on top of each other and read worse than they shipped.
 *
 * Two conditions, both required: under [CONTRAST_FLOOR], and worse than the same pair in the
 * shipped sheet. The second is there because the shipped sheets do not meet the floor
 * themselves — the evening accent measures 2.26 on its own surface — so an absolute rule
 * would warn about a client nobody has touched, which is how an operator learns to ignore
 * warnings. With this rule the shipped state never warns and an improvement never warns.
 *
 * Four pairs, and each one is read off `Theme.kt`'s Material mapping rather than off the role
 * names. The names mislead: `background` and `surface` are both `windowBackground`, with
 * `onBackground` and `onSurface` both `labelTextFill`, so almost every label in every window
 * sits on the window surface and not on `base` — `base` reaches the screen only through the
 * menu strip and the selection tint. And `primary` is `textAccent`, not `accent`, drawn as
 * text and as tab underlines. Measuring `labelTextFill` against `base` alone let an operator
 * black out every window and hear nothing.
 *
 * Four and not fifteen, deliberately: most pairs of six roles never meet on screen, and a
 * warning about a pair that cannot occur costs the same attention as a real one.
 */
fun contrastWarnings(resolved: JavaFxPalette, shipped: JavaFxPalette): List<ContrastWarning> {

    fun pairsOf(palette: JavaFxPalette) = listOf(
        /* onBackground/onSurface on background/surface: nearly every label in every window. */
        ContrastPair.TEXT_ON_WINDOW_SURFACE to (palette.labelTextFill to palette.windowBackground),
        /* onSurfaceVariant on surfaceVariant: inside a field, a list, and on every button. */
        ContrastPair.TEXT_INSIDE_FIELD to (palette.labelTextFill to palette.controlInnerBackground),
        /* primary as text and as a tab underline, on the window surface. */
        ContrastPair.ACCENT_ON_WINDOW_SURFACE to (palette.textAccent to palette.windowBackground),
        /* The menu strip paints base behind its titles; narrow, but it is real. */
        ContrastPair.TEXT_ON_MENU_STRIP to (palette.labelTextFill to palette.base),
    )

    val shippedRatios = pairsOf(shipped).associate { (what, colours) ->
        what to contrastRatio(colours.first, colours.second)
    }

    return pairsOf(resolved).mapNotNull { (what, colours) ->
        val ratio = contrastRatio(colours.first, colours.second)
        val shippedRatio = shippedRatios.getValue(what)

        if (ratio < CONTRAST_FLOOR && ratio < shippedRatio) {
            ContrastWarning(what, ratio, shippedRatio)
        } else {
            null
        }
    }
}
