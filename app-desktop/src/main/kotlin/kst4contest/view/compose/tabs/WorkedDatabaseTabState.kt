package kst4contest.view.compose.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.model.ChatMember
import kst4contest.observe.ObservableRoster

/**
 * The Workedstn database tab. It holds no settings at all — the JavaFX tab offered
 * two actions, "Refresh worked database" and "Reset worked, NOT-QRV and grid data".
 *
 * Neither coverage net can see this tab: both check setter names, and an action is
 * not a setter. That is why its behaviour is pinned by the tests around this class
 * instead.
 *
 * The actions are passed in rather than taken from a ChatController so the order
 * they run in stays testable without opening a database.
 *
 * @param resetInDatabase clears the flags and returns the affected row count, or a
 *   negative number when no successful reset could be confirmed
 * @param refreshGuiLists drops the worked and NOT-QRV marks from the runtime lists
 * @param refreshView re-reads the worked database for display
 * @param workedRoster the callsigns read from the worked database; the JavaFX tab
 *   showed them in a table with one column per band, which is the only place the
 *   operator can see what the database actually holds
 */
class WorkedDatabaseTabState(
    private val resetInDatabase: () -> Int,
    private val refreshGuiLists: () -> Unit,
    private val refreshView: () -> Unit,
    private val workedRoster: ObservableRoster<ChatMember>? = null,
) {

    /**
     * The rows to show. A snapshot, taken when the tab is built and again on every
     * refresh and reset, exactly as the JavaFX table behaved: it mirrored the roster
     * and was told to redraw after each of those actions.
     */
    var workedStations: List<ChatMember> by mutableStateOf(workedRoster?.snapshot() ?: emptyList())
        private set

    private fun readRoster() {
        workedStations = workedRoster?.snapshot() ?: emptyList()
    }

    var confirmationPending: Boolean by mutableStateOf(false)
        private set

    var resetFailed: Boolean by mutableStateOf(false)
        private set

    var lastAffectedLines: Int? by mutableStateOf(null)
        private set

    /** Opens the confirmation. Nothing is touched until [confirmReset]. */
    fun requestReset() {
        confirmationPending = true
        resetFailed = false
    }

    fun cancelReset() {
        confirmationPending = false
    }

    /**
     * Resets the database first and the runtime lists only afterwards. On failure the
     * lists are left alone: clearing them would show the operator a state the database
     * does not have.
     */
    fun confirmReset() {
        confirmationPending = false
        val affected = resetInDatabase()
        if (affected < 0) {
            resetFailed = true
            return
        }
        lastAffectedLines = affected
        refreshGuiLists()

        /*
         * The rows are re-read after a successful reset, or the table would keep
         * showing entries the database no longer has.
         */
        readRoster()
    }

    fun dismissError() {
        resetFailed = false
    }

    fun refresh() {
        refreshView()
        readRoster()
    }
}
