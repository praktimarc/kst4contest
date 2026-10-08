package kst4contest.view.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * The application theme for Compose windows.
 *
 * Release 1.50 gave the JavaFX side one base font size per operator profile.
 * Compose windows take the same value so a Compose window next to a JavaFX one
 * does not look scaled differently.
 *
 * @param darkMode the operator's configured day/evening design
 * @param baseFontSizeSp the configured base font size, in scale-independent points
 */
@Composable
fun Kst4ContestTheme(
    darkMode: Boolean,
    baseFontSizeSp: Float = 0f,
    content: @Composable () -> Unit,
) {
    val store = LocalPaletteStore.current

    val palette = if (store != null) {
        /*
         * Read from the store's state, not remembered: that read is what makes a palette
         * change recompose every open window. remember(darkMode) kept the palette until
         * somebody toggled day/evening, which is why a colour change used to reach nothing.
         */
        store.resolved(darkMode).palette
    } else {
        /*
         * No store yet. Not caution: the operator profile picker composes before a profile,
         * and therefore before a store, exists.
         */
        remember(darkMode) { JavaFxStylesheet.shipped(darkMode) }
    }

    val fontSize = if (baseFontSizeSp > 0f) baseFontSizeSp else palette.fontSizePx
    val typography = MaterialTheme.typography.scaledTo(fontSize)

    MaterialTheme(
        colorScheme = palette.toColorScheme(darkMode),
        typography = typography,
    ) {
        CompositionLocalProvider(LocalJavaFxPalette provides palette, content = content)
    }
}


/**
 * The palette in force, for the few places that need a colour Material has no slot
 * for — the button hover gradient and its pressed border.
 */
val LocalJavaFxPalette = staticCompositionLocalOf {
    JavaFxStylesheet.shipped(darkMode = false)
}

/**
 * Maps the stylesheet onto Material's colour roles.
 *
 * Only the roles the settings windows actually paint with are taken from the sheet;
 * the rest keep Material's defaults, which nothing in these windows shows.
 */
private fun JavaFxPalette.toColorScheme(darkMode: Boolean): ColorScheme {
    val defaults = if (darkMode) darkColorScheme() else lightColorScheme()

    /*
     * The tint behind a selected row. Mixing the accent into the base keeps it legible
     * in both designs, where a fixed colour would disappear into one of them.
     */
    val selection = blend(accent, base, 0.30f)

    /*
     * The sheet's own separator colour is #3C3C3C on a #373E43 surface — a brightness
     * difference of 0.002, which is no line at all. Where the declared colour cannot be
     * told apart from the surface it is replaced by a step away from that surface, so
     * the section rules are actually visible. Making them visible is the point: they
     * were asked for because a bare line of text did not read as a division.
     */
    val declaredLine = separatorLine
    val contrast = brightness(declaredLine) - brightness(windowBackground)
    val visibleLine = if (contrast > -0.08f && contrast < 0.08f) {
        JavaFxStylesheet.derive(windowBackground, if (darkMode) 45.0 else -25.0)
    } else {
        declaredLine
    }

    return defaults.copy(
        /*
         * The green of .text-field .text, not -fx-accent: the blue accent only paints
         * JavaFX's selection bar, while the green is what the operator sees on column
         * headers and inside every field, and therefore what reads as the accent.
         */
        primary = textAccent,
        onPrimary = contrastingText(textAccent),
        background = windowBackground,
        onBackground = labelTextFill,
        surface = windowBackground,
        onSurface = labelTextFill,
        /* The interior of a text field: JavaFX sets -fx-control-inner-background. */
        surfaceVariant = controlInnerBackground,
        onSurfaceVariant = labelTextFill,
        secondaryContainer = selection,
        onSecondaryContainer = labelTextFill,
        outline = visibleLine,
        outlineVariant = visibleLine,
    )
}

/** The brightness JavaFX measures with: 0.3R + 0.59G + 0.11B. */
private fun brightness(color: Color): Float =
    0.3f * color.red + 0.59f * color.green + 0.11f * color.blue

/** [amount] of [foreground] over [background]. */
private fun blend(foreground: Color, background: Color, amount: Float): Color = Color(
    red = foreground.red * amount + background.red * (1f - amount),
    green = foreground.green * amount + background.green * (1f - amount),
    blue = foreground.blue * amount + background.blue * (1f - amount),
    alpha = 1f,
)

/**
 * Black or white, whichever reads on the given colour. Uses the same brightness
 * measure JavaFX does, so the decision matches what the rest of the application
 * would make.
 *
 * Internal rather than private because the menu row needs it too: once the operator can
 * choose the accent, a menu title drawn in any fixed colour on an accent background can
 * collapse into it.
 */
internal fun contrastingText(background: Color): Color =
    if (brightness(background) > 0.5f) Color.Black else Color.White

/**
 * Scales every text style by the factor that turns the body size into
 * [targetBodySizeSp]. A value of zero or less is ignored, so a missing or
 * malformed configured size keeps the default design rather than collapsing it.
 */
internal fun Typography.scaledTo(targetBodySizeSp: Float): Typography {
    if (targetBodySizeSp <= 0f) {
        return this
    }
    val factor = targetBodySizeSp / bodyMedium.fontSize.value
    if (factor == 1f) {
        return this
    }
    fun TextStyle.scaled() = copy(fontSize = fontSize.value.times(factor).sp)
    return copy(
        displayLarge = displayLarge.scaled(), displayMedium = displayMedium.scaled(),
        displaySmall = displaySmall.scaled(), headlineLarge = headlineLarge.scaled(),
        headlineMedium = headlineMedium.scaled(), headlineSmall = headlineSmall.scaled(),
        titleLarge = titleLarge.scaled(), titleMedium = titleMedium.scaled(),
        titleSmall = titleSmall.scaled(), bodyLarge = bodyLarge.scaled(),
        bodyMedium = bodyMedium.scaled(), bodySmall = bodySmall.scaled(),
        labelLarge = labelLarge.scaled(), labelMedium = labelMedium.scaled(),
        labelSmall = labelSmall.scaled(),
    )
}
