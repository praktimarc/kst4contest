package kst4contest.view.compose

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * The filter bar's rules.
 *
 * Its 25 toggles are not 25 states. Eighteen switch a predicate in the controller on
 * or off, eight are one mutually exclusive group that only writes a bearing into the
 * QTF field, and one colours a cell without filtering anything. Getting that wrong
 * would put a station in front of the operator that their filter excludes — or hide
 * one it does not.
 */
class StationFilterStateTest {

    @Test
    fun `nothing is filtered to begin with`() {
        assertTrue(StationFilterState().activeFilters.isEmpty())
    }

    @Test
    fun `a toggle switches exactly its own filter`() {
        val filters = StationFilterState()

        filters.onlyNewGrids = true

        assertEquals(setOf(StationFilter.ONLY_NEW_GRIDS), filters.activeFilters)
    }

    @Test
    fun `grid colouring is not a filter`() {
        val filters = StationFilterState()

        filters.gridColouring = true

        assertTrue(filters.activeFilters.isEmpty(),
            "it colours the locator cell; a station must not vanish because of it")
        assertTrue(filters.gridColouring)
    }

    @Test
    fun `each band hides only stations worked on that band`() {
        val filters = StationFilterState()

        filters.setHideWorkedOn(WorkedBandFilter.B_144, true)

        assertEquals(setOf(StationFilter.HIDE_WORKED_144), filters.activeFilters)

        filters.setHideWorkedOn(WorkedBandFilter.B_432, true)

        assertEquals(
            setOf(StationFilter.HIDE_WORKED_144, StationFilter.HIDE_WORKED_432),
            filters.activeFilters,
        )
    }

    @Test
    fun `a bearing button picks one direction and its degrees`() {
        val filters = StationFilterState()

        filters.toggleBearing(Bearing.NE)

        assertEquals(Bearing.NE, filters.bearing)
        assertEquals(45.0, filters.qtfDegrees)
    }

    @Test
    fun `the bearings are mutually exclusive, as their toggle group was`() {
        val filters = StationFilterState()
        filters.toggleBearing(Bearing.N)

        filters.toggleBearing(Bearing.SW)

        assertEquals(Bearing.SW, filters.bearing)
        assertEquals(225.0, filters.qtfDegrees)
    }

    /**
     * Pressing a direction switches the QTF filter on.
     *
     * Verified against the JavaFX toggle group rather than assumed: its
     * selectedToggleProperty listener does
     * `chatMemberTableFilterQtfEnableChkbx.setSelected(true)` whenever a direction is
     * selected, and `false` when the selection is cleared
     * (`Kst4ContestApplication.java:8758-8767`). An earlier version of this test claimed
     * the opposite and had to be taken on faith; it is pinned to the source now.
     */
    @Test
    fun `picking a bearing switches the QTF filter on, as the toggle group did`() {
        val filters = StationFilterState()

        filters.toggleBearing(Bearing.E)

        assertEquals(setOf(StationFilter.QTF), filters.activeFilters)
        assertEquals(90.0, filters.qtfDegrees)
    }

    /** Pressing the selected direction again clears it, and the filter with it. */
    @Test
    fun `pressing the same bearing again switches the QTF filter off`() {
        val filters = StationFilterState()
        filters.toggleBearing(Bearing.E)

        filters.toggleBearing(Bearing.E)

        assertNull(filters.bearing)
        assertTrue(filters.activeFilters.isEmpty())
    }

    @Test
    fun `the QTF filter can also be switched on without a direction`() {
        val filters = StationFilterState()

        filters.qtfEnabled = true

        assertEquals(setOf(StationFilter.QTF), filters.activeFilters)
    }

    @Test
    fun `the maximum QRB filter needs its own switch`() {
        val filters = StationFilterState()

        filters.maxQrbKm = 300.0
        assertTrue(filters.activeFilters.isEmpty(), "a distance alone filters nothing")

        filters.maxQrbEnabled = true
        assertEquals(setOf(StationFilter.MAX_QRB), filters.activeFilters)
    }

    @Test
    fun `a search text filters while it is there and stops when it is cleared`() {
        val filters = StationFilterState()

        filters.searchText = "DN9"
        assertEquals(setOf(StationFilter.SEARCH_TEXT), filters.activeFilters)

        filters.searchText = "   "
        assertTrue(filters.activeFilters.isEmpty(),
            "blank is not a search; leaving it would hide every station")
    }

    @Test
    fun `switching a filter off removes it again`() {
        val filters = StationFilterState()
        filters.hideInactive = true
        filters.onlyNewGrids = true

        filters.hideInactive = false

        assertEquals(setOf(StationFilter.ONLY_NEW_GRIDS), filters.activeFilters)
        assertFalse(filters.hideInactive)
    }
}
