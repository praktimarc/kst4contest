package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.model.AirPlane
import kst4contest.model.AirPlaneReflectionInfo

/**
 * The aircraft-reflection cell, shared by the station list and the directed messages.
 *
 * Both JavaFX tables built this text by hand and identically, which is why it lives in
 * one place now: minutes until the aeroplane is in the path, and how promising that path
 * is, for at most the first two.
 */
object AirplaneCell {

    /** How many fit in the column before it stops being readable. */
    private const val SHOWN_AIRPLANES = 2

    fun format(info: AirPlaneReflectionInfo?): String {
        // Nothing computed for this station yet. Both JavaFX cells ended up blank here, by
        // different routes: the directed-message one catches the NullPointerException, the
        // station-list one has no guard at all and lets it escape the value factory.
        val planes = info?.risingAirplanes ?: return if (info == null) "" else NOTHING_COMING

        if (planes.isEmpty()) return NOTHING_COMING

        return planes.take(SHOWN_AIRPLANES).joinToString(" / ") { plane ->
            "${plane.arrivingDurationMinutes} (${plane.potential}%)"
        }
    }

    /**
     * Not an empty cell: the operator has to tell "no aeroplane is coming" apart from
     * "nothing has been computed for this station yet".
     */
    private const val NOTHING_COMING = "nil"

    // ---- the order ----------------------------------------------------------

    /** Unknown or no aeroplane sorts after every real one. */
    private const val NO_PLANE = Int.MAX_VALUE

    /** The aeroplane the cell shows first; the parser already put the best one there. */
    private fun first(info: AirPlaneReflectionInfo?): AirPlane? = info?.risingAirplanes?.firstOrNull()

    /**
     * Ascending: the minutes the operator reads first, then the better potential first.
     *
     * Issue #87: without a comparator the table fell back to the rendered text, which put
     * "13 (75%)" above "3 (50%)". Only the first aeroplane counts, as it is the one the
     * operator reads and the one the colour marks; the second is in the cell for context.
     * Empty and "nil" cells carry [NO_PLANE]: like qrb and qtf, an absent value gets a
     * sentinel instead of a null check, here the maximum so the cell sorts last ascending.
     */
    val order: Comparator<AirPlaneReflectionInfo?> =
        compareBy<AirPlaneReflectionInfo?> { first(it)?.arrivingDurationMinutes ?: NO_PLANE }
            .thenByDescending { first(it)?.potential ?: 0 }

    /** `.table-cell-100PercentAP`, identical in both stylesheets: a certain path. */
    val PURPLE_100 = Color(0xFFF98AFF)

    /** `.table-cell-75PercentAP`: a good one. */
    val RED_75 = Color(0xFFFA6666)

    /** `.table-cell-50PercentAP`: a fair one. */
    val ORANGE_50 = Color(0xFFFA9F66)

    /**
     * The colour of the cell, which is what an aircraft-scatter chance is spotted by — the
     * numbers are read afterwards.
     *
     * Pinned as found: the JavaFX cell factories matched the RENDERED STRING, so only
     * exactly 100, 75 and 50 are coloured and 85% gets nothing. Colouring by threshold
     * instead would light up cells the operator has never seen lit, which changes what the
     * column means at a glance.
     */
    fun accentFor(renderedValue: String): CellAccent? = when {
        renderedValue.contains("100%") -> CellAccent(foreground = PURPLE_100)
        renderedValue.contains("75%") -> CellAccent(foreground = RED_75)
        renderedValue.contains("50%") -> CellAccent(foreground = ORANGE_50)
        else -> null
    }
}
