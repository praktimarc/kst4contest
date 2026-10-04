package kst4contest.view.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kst4contest.model.PaletteOverrides
import kst4contest.model.PaletteRole

/**
 * The palette of both designs for the active profile, held as Compose state.
 *
 * **This is what makes a colour change reach every open window.** `Kst4ContestTheme` used to
 * take its palette from `remember(darkMode)`, so a change reached nothing until somebody
 * toggled day/evening. Reading [resolved] inside the theme turns every palette change into a
 * recomposition of every window that draws it.
 *
 * Built over four lambdas rather than over `ChatPreferences` so it can be tested without a
 * profile on disk: that constructor copies resources into the operator's home directory.
 *
 * **Where "immediately" and "survives a restart" part company:** every change writes through
 * to the preferences in memory at once, which is what recolours the windows, but the XML is
 * written by the existing `savePreferencesFromSettings`. That is deliberately the same
 * contract every other settings tab has — the JavaFX controls wrote as they changed and
 * "Save settings" only persisted — but it means a change made and never saved is gone after a
 * restart.
 *
 * @param shippedOf layer 1: the shipped sheet for a design, always complete
 * @param fileRolesOf layer 2: the roles the profile's own stylesheet names, if any
 * @param storedOverridesOf layer 3 as stored, encoded by [PaletteOverrides]
 * @param storeOverrides writes layer 3 back
 */
class PaletteStore(
    shippedOf: (darkMode: Boolean) -> JavaFxPalette,
    fileRolesOf: (darkMode: Boolean) -> Map<PaletteRole, Color>,
    storedOverridesOf: (darkMode: Boolean) -> String,
    private val storeOverrides: (darkMode: Boolean, encoded: String) -> Unit,
) {

    /*
     * Layers 1 and 2 are read once per design, on first use, and then held. They do not change
     * while the application runs -- the shipped sheet is a classpath resource, and both manuals
     * promise that edits to the profile's stylesheet take effect after a restart. Re-reading
     * them inside resolved() meant two CSS parses and a disk read per recomposition of every
     * window, on the UI thread, which in this codebase is never a detail.
     *
     * Only the overrides below stay live. The state read is the mechanism that repaints the
     * windows; re-parsing immutable input was never part of it.
     */
    private val shippedDaylight by lazy { shippedOf(false) }
    private val shippedEvening by lazy { shippedOf(true) }
    private val fileRolesDaylight by lazy { fileRolesOf(false) }
    private val fileRolesEvening by lazy { fileRolesOf(true) }

    /*
     * The encoded overrides per design, as state. Everything else derives from these two, so
     * there is exactly one place a change has to land for the windows to follow.
     */
    private val daylightOverrides = mutableStateOf(storedOverridesOf(false))
    private val eveningOverrides = mutableStateOf(storedOverridesOf(true))

    /** What [restoreSnapshot] goes back to, or null while nothing has been remembered. */
    private var snapshot: Pair<String, String>? = null

    /**
     * The palette in force for a design, with every role's provenance.
     *
     * Reads the override state, so a composable calling this recomposes when a colour
     * changes. That state read is the mechanism and must not be cached; the two immutable
     * layers under it are held, which is a different thing.
     */
    fun resolved(darkMode: Boolean): ResolvedPalette = resolvePalette(
        shipped = shipped(darkMode),
        fromFile = if (darkMode) fileRolesEvening else fileRolesDaylight,
        changed = changedRoles(darkMode),
    )

    /**
     * The shipped palette for a design, untouched by file or change.
     *
     * The lifeline reads this. A control drawn from [resolved] would disappear in exactly the
     * palette it exists to undo.
     *
     * @param darkMode the design
     * @return layer 1
     */
    fun shipped(darkMode: Boolean): JavaFxPalette =
        if (darkMode) shippedEvening else shippedDaylight

    /**
     * Sets one role of one design.
     *
     * An unusable colour is ignored rather than stored: an operator mistyping a hex value
     * must not end up with a black client. The tab reports the refusal.
     *
     * @param darkMode the design to change
     * @param role the role to set
     * @param colour a six-digit hex colour; anything else is ignored
     */
    fun set(darkMode: Boolean, role: PaletteRole, colour: String) {

        if (!PaletteOverrides.isValidColour(colour)) {
            return
        }

        val overrides = PaletteOverrides.parse(encoded(darkMode)).toMutableMap()
        overrides[role] = colour
        write(darkMode, PaletteOverrides.format(overrides))
    }

    /**
     * Sets every role of a design to its shipped colour, as an explicit change.
     *
     * Not the same as [discardChanges]: it has to beat a stylesheet file too, and only an
     * explicit override on all six roles does that. This is the action the stage's acceptance
     * criterion names — "exactly the shipped colours", file or no file.
     *
     * @param darkMode the design to reset
     */
    fun resetToShipped(darkMode: Boolean) {
        val shipped = shipped(darkMode)

        val overrides = PaletteRole.values().associateWith { role ->
            hexOf(shippedColourOf(shipped, role))
        }

        write(darkMode, PaletteOverrides.format(overrides))
    }

    /**
     * Empties layer 3, so the file or the shipped sheet applies again.
     *
     * @param darkMode the design to clear
     */
    fun discardChanges(darkMode: Boolean) {
        write(darkMode, "")
    }

    /** Remembers the overrides of both designs, for [restoreSnapshot]. */
    fun takeSnapshot() {
        snapshot = daylightOverrides.value to eveningOverrides.value
    }

    /**
     * Restores the overrides remembered by [takeSnapshot], including a change that was
     * already there when it was taken. Does nothing when nothing was remembered.
     *
     * Both designs, not only the one in force. [takeSnapshot] captures both, and an operator
     * who changed a colour, switched day/evening from the main window's menu, changed another
     * and then asked to go back to how it was means both -- restoring half of it would leave
     * a state they never chose and cannot name.
     */
    fun restoreSnapshot() {
        val remembered = snapshot ?: return
        write(darkMode = false, encoded = remembered.first)
        write(darkMode = true, encoded = remembered.second)
    }

    private fun changedRoles(darkMode: Boolean): Map<PaletteRole, Color> =
        PaletteOverrides.parse(encoded(darkMode)).mapValues { (_, hex) -> colourOf(hex) }

    private fun encoded(darkMode: Boolean): String =
        if (darkMode) eveningOverrides.value else daylightOverrides.value

    private fun write(darkMode: Boolean, encoded: String) {
        if (darkMode) {
            eveningOverrides.value = encoded
        } else {
            daylightOverrides.value = encoded
        }
        storeOverrides(darkMode, encoded)
    }

    private fun shippedColourOf(shipped: JavaFxPalette, role: PaletteRole): Color = when (role) {
        PaletteRole.SURFACE -> shipped.base
        PaletteRole.WINDOW_SURFACE -> shipped.windowBackground
        PaletteRole.FIELD_INTERIOR -> shipped.controlInnerBackground
        PaletteRole.TEXT -> shipped.labelTextFill
        PaletteRole.ACCENT -> shipped.accent
        PaletteRole.SEPARATOR -> shipped.separatorLine
    }
}

/**
 * A six-digit hex colour as a drawable colour.
 *
 * Only ever called with a value [PaletteOverrides.isValidColour] has already accepted, which
 * is why it does not have to answer what an unparsable value means.
 */
internal fun colourOf(hex: String): Color =
    Color(("ff" + hex.trim().removePrefix("#")).toLong(16))

/**
 * A colour as the six-digit hex form the preferences store.
 *
 * Alpha is dropped on purpose: the roles are opaque surfaces and text, and a stored colour
 * the operator cannot type back in would be a colour they cannot edit.
 */
internal fun hexOf(colour: Color): String = "#%02X%02X%02X".format(
    (colour.red * 255).toInt(),
    (colour.green * 255).toInt(),
    (colour.blue * 255).toInt(),
)

/**
 * The store for the windows below, or null where there is none.
 *
 * Null is not caution but a real case: the operator profile picker draws before a profile —
 * and therefore before a store — exists, and so do the tests of individual controls. Those
 * get the shipped palette.
 */
val LocalPaletteStore = staticCompositionLocalOf<PaletteStore?> { null }
