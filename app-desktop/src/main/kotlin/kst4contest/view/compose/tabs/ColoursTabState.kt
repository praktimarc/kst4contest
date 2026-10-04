package kst4contest.view.compose.tabs

import kst4contest.model.PaletteRole
import kst4contest.view.compose.ContrastWarning
import kst4contest.view.compose.OperatorProfilePaletteFiles
import kst4contest.view.compose.JavaFxPalette
import kst4contest.view.compose.PaletteSource
import kst4contest.view.compose.PaletteStore
import kst4contest.view.compose.contrastWarnings
import kst4contest.view.compose.hexOf

/**
 * One row of the colours tab: a role, what it is set to, and where that came from.
 *
 * @param label the role's name as the operator reads it
 * @param hex the colour in force, in the one form the field accepts
 * @param source why it is that colour — the mitigation the spec calls mandatory
 */
data class RoleRow(
    val role: PaletteRole,
    val label: String,
    val hex: String,
    val source: PaletteSource,
)

/**
 * The colours tab as a typed facade over the palette store.
 *
 * Writes through immediately and has no apply(), like every settings tab: the JavaFX controls
 * wrote as they changed and "Save settings" only persisted.
 *
 * @param store the active profile's palette
 * @param darkModeNow which design the operator is looking at; the tab always edits that one
 * @param paletteFiles where this profile's own stylesheets live, for the hand-editing route
 */
class ColoursTabState(
    private val store: PaletteStore,
    private val darkModeNow: () -> Boolean,
    private val paletteFiles: OperatorProfilePaletteFiles,
) {

    /**
     * The six rows, in a fixed order, for the design in force.
     *
     * @return one row per role, always all six
     */
    fun roles(): List<RoleRow> {
        val resolved = store.resolved(darkModeNow())

        return PaletteRole.values().map { role ->
            val it = resolved.roles.getValue(role)
            RoleRow(role = role, label = LABELS.getValue(role), hex = hexOf(it.colour), source = it.source)
        }
    }

    /**
     * Sets one role of the design in force.
     *
     * An unusable value is refused rather than stored: a mistyped hex value must not leave
     * the operator with a black client. The refusal is returned rather than pushed, which is
     * what `Form.committed` expects — it puts the field back and shows the reason.
     *
     * @param role the role to set
     * @param text what the operator typed
     * @return null when it was accepted, otherwise why it was not
     */
    fun set(role: PaletteRole, text: String): String? {
        val before = store.resolved(darkModeNow()).roles.getValue(role).colour

        store.set(darkModeNow(), role, text)

        if (store.resolved(darkModeNow()).roles.getValue(role).colour != before) {
            return null
        }

        /*
         * Setting a role to the colour it already had is indistinguishable from a refusal
         * here, and saying so would be wrong -- so the value itself decides.
         */
        if (kst4contest.model.PaletteOverrides.isValidColour(text)) {
            return null
        }

        return "'$text' is not a colour. Six hexadecimal digits with a leading hash, "
                .plus("for example #3C7A4B. ${LABELS.getValue(role)} is unchanged.")
    }

    /**
     * The pairs that now read worse than the shipped sheet makes them.
     *
     * @return the warnings to show, empty in the shipped state
     */
    fun warnings(): List<ContrastWarning> = contrastWarnings(
        resolved = store.resolved(darkModeNow()).palette,
        shipped = store.shipped(darkModeNow()),
    )

    /**
     * The shipped palette of the design in force.
     *
     * The lifeline is drawn from this and never from [roles]: a rescue control that took the
     * operator's palette would disappear in exactly the palette it exists to undo.
     *
     * @return layer 1
     */
    fun shipped(): JavaFxPalette = store.shipped(darkModeNow())

    /** Remembers the palette in force, for [restoreRemembered]. Called when the tab opens. */
    fun rememberCurrentState() = store.takeSnapshot()

    /** The first way back: the palette as it was when the tab was opened, both designs. */
    fun restoreRemembered() = store.restoreSnapshot()

    /** The second way back: empty this design's changes, so file or shipping applies again. */
    fun discardChanges() = store.discardChanges(darkModeNow())

    /** The third way back, and the one that beats a stylesheet file as well. */
    fun resetToShipped() = store.resetToShipped(darkModeNow())

    /**
     * The path of this design's own stylesheet, shown so the operator knows where to look.
     *
     * @return absolute path, whether or not the file exists
     */
    fun stylesheetPath(): String = paletteFiles.fileFor(darkModeNow()).path

    /**
     * Whether this design's own stylesheet is there.
     *
     * @return true when a file exists at [stylesheetPath]
     */
    fun stylesheetExists(): Boolean = paletteFiles.fileFor(darkModeNow()).isFile

    /**
     * Writes the shipped stylesheet into the profile so it can be edited by hand.
     *
     * Never replaces a file that is already there: that file is the operator's own work.
     *
     * @return what happened, for the window's notice bar
     */
    fun exportStylesheet(): StylesheetExportOutcome =
        exportShippedStylesheet(paletteFiles.fileFor(darkModeNow()), darkModeNow())

    private companion object {

        /*
         * One place for the six names. Stage 9 moves the interface text into a resource
         * bundle; keeping them together is what makes that a single move.
         */
        /*
         * Each label says where the colour is actually painted, because the role names alone
         * mislead: "Surface" is -fx-base, which reaches the screen only through the menu
         * strip and the selected-row tint, while "Window surface" is what the windows are
         * filled with. An operator reaching for "Surface" to darken the client and seeing
         * almost nothing happen would reasonably conclude the control is broken.
         */
        val LABELS = mapOf(
            PaletteRole.SURFACE to "Surface (menu strip, selected rows)",
            PaletteRole.WINDOW_SURFACE to "Window surface (the windows themselves)",
            PaletteRole.FIELD_INTERIOR to "Field interior (fields, lists, buttons)",
            PaletteRole.TEXT to "Text",
            PaletteRole.ACCENT to "Accent",
            PaletteRole.SEPARATOR to "Separator line",
        )
    }
}
