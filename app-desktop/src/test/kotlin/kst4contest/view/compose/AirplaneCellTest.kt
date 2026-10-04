package kst4contest.view.compose

import kst4contest.model.AirPlane
import kst4contest.model.AirPlaneReflectionInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The aircraft-reflection cell, shared by the station list and the directed messages.
 *
 * Both JavaFX tables built this text by hand and identically: the minutes until the
 * aeroplane arrives and how promising the path is, for at most the first two. The
 * Compose station list was reading AirPlaneReflectionInfo.toString() instead, which
 * yields "DL0ABC > OK1XYZ at <date> 3 planes: [...]" — a defect from Etappe 5a that this
 * test closes.
 */
class AirplaneCellTest {

    @Test
    fun `one aeroplane gives its minutes and its potential`() {
        val info = reflectionInfo(plane(minutes = 7, potential = 85))
        assertEquals("7 (85%)", AirplaneCell.format(info))
    }

    @Test
    fun `two aeroplanes are separated by a slash`() {
        val info = reflectionInfo(plane(7, 85), plane(12, 60))
        assertEquals("7 (85%) / 12 (60%)", AirplaneCell.format(info))
    }

    /** Only the first two fit in the column; the rest are in the tooltip's business. */
    @Test
    fun `a third aeroplane is not shown`() {
        val info = reflectionInfo(plane(7, 85), plane(12, 60), plane(20, 30))
        assertEquals("7 (85%) / 12 (60%)", AirplaneCell.format(info))
    }

    /**
     * "nil" and not an empty cell: the operator has to tell "no aeroplane is coming"
     * apart from "nothing has been computed for this station yet".
     */
    @Test
    fun `no aeroplanes reads nil`() {
        assertEquals("nil", AirplaneCell.format(reflectionInfo()))
    }

    @Test
    fun `an unset aeroplane list reads nil too`() {
        val info = AirPlaneReflectionInfo()
        info.risingAirplanes = null
        assertEquals("nil", AirplaneCell.format(info))
    }

    /** Nothing computed yet leaves the cell blank, as both JavaFX tables did. */
    @Test
    fun `no reflection info at all leaves the cell empty`() {
        assertEquals("", AirplaneCell.format(null))
    }

    // ---- the colour ---------------------------------------------------------

    /**
     * The colour is what an aircraft-scatter chance is spotted by; the numbers are read
     * afterwards. Both stylesheets use the same three: purple for a certain path, red for a
     * good one, orange for a fair one.
     */
    @Test
    fun `a certain path is purple, a good one red, a fair one orange`() {
        assertEquals(AirplaneCell.PURPLE_100, AirplaneCell.accentFor("7 (100%)")?.foreground)
        assertEquals(AirplaneCell.RED_75, AirplaneCell.accentFor("7 (75%)")?.foreground)
        assertEquals(AirplaneCell.ORANGE_50, AirplaneCell.accentFor("7 (50%)")?.foreground)
    }

    /**
     * Pinned as found: JavaFX matched the rendered string, so only exactly 100, 75 and 50
     * are coloured — 85% gets nothing. Colouring by threshold instead would light up cells
     * the operator has never seen lit, which changes what the column means at a glance.
     */
    @Test
    fun `a potential between the three steps is not coloured`() {
        assertNull(AirplaneCell.accentFor("7 (85%)"))
        assertNull(AirplaneCell.accentFor("7 (60%)"))
        assertNull(AirplaneCell.accentFor("7 (49%)"))
    }

    @Test
    fun `nothing coming is not coloured`() {
        assertNull(AirplaneCell.accentFor("nil"))
        assertNull(AirplaneCell.accentFor(""))
    }

    /** With two aeroplanes the better one decides, as the original's if-chain did. */
    @Test
    fun `the best of two potentials decides the colour`() {
        assertEquals(AirplaneCell.PURPLE_100, AirplaneCell.accentFor("7 (75%) / 12 (100%)")?.foreground)
        assertEquals(AirplaneCell.RED_75, AirplaneCell.accentFor("7 (50%) / 12 (75%)")?.foreground)
    }

    private fun reflectionInfo(vararg planes: AirPlane): AirPlaneReflectionInfo =
        AirPlaneReflectionInfo().apply { risingAirplanes = planes.toMutableList() }

    private fun plane(minutes: Int, potential: Int): AirPlane = AirPlane().apply {
        arrivingDurationMinutes = minutes
        this.potential = potential
    }
}
