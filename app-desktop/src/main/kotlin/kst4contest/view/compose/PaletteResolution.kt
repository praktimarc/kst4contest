package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.model.PaletteRole

/** Which of the three layers a colour in force came from. */
enum class PaletteSource { SHIPPED, FILE, CHANGED }

/** One role's colour, and where it came from. */
data class ResolvedRole(val colour: Color, val source: PaletteSource)

/**
 * The palette in force, and the provenance of every role in it.
 *
 * @param roles always all six, so the settings tab can show a source for each
 * @param palette what the windows draw
 */
data class ResolvedPalette(
    val roles: Map<PaletteRole, ResolvedRole>,
    val palette: JavaFxPalette,
)

/**
 * Resolves the three layers, role by role: the later layer wins.
 *
 * The shipped sheet is the floor and always complete. A file and a change each carry only
 * the roles they name — which is why [JavaFxStylesheet.declaredRoles] exists rather than
 * `read`: a file saying only `-fx-base` must override exactly one role.
 *
 * Fields that are not roles — the hover gradient, the pressed border, the font size — stay
 * with the shipped sheet. They are derived values rather than decisions an operator wants to
 * make one by one.
 *
 * The exception is `textAccent`, which follows the accent role whenever that role's colour
 * actually differs from the shipped one: Material's primary is mapped onto it rather than onto
 * `accent`, because `-fx-accent` paints only JavaFX's selection bar while the green of
 * `.text-field .text` is what an operator reads as the accent. Left on the sheet's own value,
 * a chosen accent would change almost nothing visible; keyed on provenance instead of on the
 * value, an accent nobody chose would repaint it.
 */
fun resolvePalette(
    shipped: JavaFxPalette,
    fromFile: Map<PaletteRole, Color>,
    changed: Map<PaletteRole, Color>,
): ResolvedPalette {

    val shippedColour = mapOf(
        PaletteRole.SURFACE to shipped.base,
        PaletteRole.WINDOW_SURFACE to shipped.windowBackground,
        PaletteRole.FIELD_INTERIOR to shipped.controlInnerBackground,
        PaletteRole.TEXT to shipped.labelTextFill,
        PaletteRole.ACCENT to shipped.accent,
        PaletteRole.SEPARATOR to shipped.separatorLine,
    )

    val roles = PaletteRole.values().associateWith { role ->
        when {
            changed.containsKey(role) -> ResolvedRole(changed.getValue(role), PaletteSource.CHANGED)
            fromFile.containsKey(role) -> ResolvedRole(fromFile.getValue(role), PaletteSource.FILE)
            else -> ResolvedRole(shippedColour.getValue(role), PaletteSource.SHIPPED)
        }
    }

    fun colourOf(role: PaletteRole) = roles.getValue(role).colour

    /*
     * Keyed on the value and deliberately not on the provenance. The root profile's own
     * stylesheet is the one copyResourceIfRequired writes at startup -- the shipped sheet
     * itself -- so on every existing installation layer 2 declares the accent without anybody
     * having chosen anything. Asking "does it come from a file" repainted the evening primary
     * from green to blue on upgrade, and made resetToShipped disagree with discardChanges
     * about what shipped looks like.
     */
    val chosenAccent = roles.getValue(PaletteRole.ACCENT).colour.takeIf { it != shipped.accent }

    return ResolvedPalette(
        roles = roles,
        palette = shipped.copy(
            base = colourOf(PaletteRole.SURFACE),
            windowBackground = colourOf(PaletteRole.WINDOW_SURFACE),
            controlInnerBackground = colourOf(PaletteRole.FIELD_INTERIOR),
            labelTextFill = colourOf(PaletteRole.TEXT),
            accent = colourOf(PaletteRole.ACCENT),
            textAccent = chosenAccent ?: shipped.textAccent,
            separatorLine = colourOf(PaletteRole.SEPARATOR),
        ),
    )
}
