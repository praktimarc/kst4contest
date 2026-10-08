package kst4contest.view.feed

import kst4contest.model.ChatMessage
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.DataTableState

/**
 * Fills the table of messages belonging to the selected station.
 *
 * Its own class for the reason it was nearly lost: the feed used to be a listener
 * registered two hundred lines inside a JavaFX table builder, so a reader asking "who
 * calls that builder" never discovered that deleting it would leave this table
 * permanently empty — with a green build and no compile error.
 *
 * Copies what it is handed. The caller's list is the live mirror of a roster and keeps
 * changing; a table holding a view of it would show rows that are no longer there.
 */
class SelectedStationMessagesFeed(
    private val target: DataTableState<ChatMessage>,
    private val dispatcher: UiDispatcher,
) {

    fun push(messages: List<ChatMessage>) {
        val copy = messages.toList()
        dispatcher.runOnUi { target.replaceRows(copy) }
    }
}
