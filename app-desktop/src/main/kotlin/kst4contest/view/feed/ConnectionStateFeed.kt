package kst4contest.view.feed

import kst4contest.controller.On4KstConnectionState
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.MainWindowSurroundings

/**
 * Fills the connection indicator in the Compose status bar.
 *
 * Separated from `updateConnectionStateIndicator()`, which wrote this state and then
 * touched a JavaFX tooltip in the same method. The tooltip belongs to the window that
 * is never shown; its live callers do not, and they must not be taken down with it.
 *
 * The substitutions are the JavaFX method's own: an absent state reads as DISCONNECTED
 * and an absent detail as the state's name, because "null" in a status bar tells an
 * operator nothing.
 */
class ConnectionStateFeed(
    private val target: MainWindowSurroundings,
    private val dispatcher: UiDispatcher,
) {

    fun push(state: On4KstConnectionState?, detail: String?) {
        val effectiveState = state ?: On4KstConnectionState.DISCONNECTED
        val effectiveDetail = if (detail.isNullOrBlank()) effectiveState.name else detail

        dispatcher.runOnUi {
            target.connectionState = effectiveState
            target.connectionDetail = effectiveDetail
        }
    }
}
