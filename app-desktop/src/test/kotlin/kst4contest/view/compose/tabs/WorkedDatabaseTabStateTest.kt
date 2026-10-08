package kst4contest.view.compose.tabs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class WorkedDatabaseTabStateTest {

    private class Recorder(private val resetResult: Int) {
        var resetCalls = 0
        var guiRefreshed = false
        var refreshCalls = 0
        val state = WorkedDatabaseTabState(
            resetInDatabase = { resetCalls++; resetResult },
            refreshGuiLists = { guiRefreshed = true },
            refreshView = { refreshCalls++ },
        )
    }

    @Test
    fun `a reset asks before it touches anything`() {
        val r = Recorder(resetResult = 5)

        r.state.requestReset()

        assertTrue(r.state.confirmationPending,
            "the JavaFX button showed a confirmation dialog first")
        assertEquals(0, r.resetCalls, "nothing may be reset before the operator confirms")
    }

    @Test
    fun `cancelling a reset changes nothing`() {
        val r = Recorder(resetResult = 5)
        r.state.requestReset()

        r.state.cancelReset()

        assertFalse(r.state.confirmationPending)
        assertEquals(0, r.resetCalls)
        assertFalse(r.guiRefreshed)
    }

    @Test
    fun `confirming resets the database and then the lists, in that order`() {
        val r = Recorder(resetResult = 12)
        r.state.requestReset()

        r.state.confirmReset()

        assertEquals(1, r.resetCalls)
        assertTrue(r.guiRefreshed, "the runtime lists must follow the database")
        assertEquals(12, r.state.lastAffectedLines)
        assertFalse(r.state.confirmationPending)
    }

    @Test
    fun `a failed reset reports an error and leaves the lists alone`() {
        // resetWorkedDataInDB returns a negative number when it could not confirm a
        // reset. The JavaFX handler stopped there and did not touch the GUI lists.
        val r = Recorder(resetResult = -1)
        r.state.requestReset()

        r.state.confirmReset()

        assertTrue(r.state.resetFailed)
        assertFalse(r.guiRefreshed,
            "clearing the runtime lists after a failed database reset would show the "
                + "operator a state the database does not have")
    }

    @Test
    fun `refreshing is a plain action`() {
        val r = Recorder(resetResult = 0)

        r.state.refresh()

        assertEquals(1, r.refreshCalls)
    }
}
