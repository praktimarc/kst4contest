package kst4contest.view.feed

import kst4contest.model.ChatMessage
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.DataTableState
import kst4contest.view.compose.RowKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * What fills the "messages of the selected station" table.
 *
 * Found the hard way: this feed was buried two hundred lines inside a JavaFX table
 * builder, so "who calls that builder" answered a different question than "is it safe
 * to delete". A feed with its own name and its own test cannot be lost that way.
 */
class SelectedStationMessagesFeedTest {

    private class CountingDispatcher : UiDispatcher {
        var deliveries = 0
            private set

        override fun runOnUi(task: Runnable) {
            deliveries++
            task.run()
        }

        override fun isUiThread(): Boolean = true
    }

    private fun emptyTable() =
        DataTableState<ChatMessage>(emptyList(), RowKeys.byReference(), "test")

    @Test
    fun `the messages it is given reach the table`() {
        val target = emptyTable()

        SelectedStationMessagesFeed(target, CountingDispatcher()).push(listOf(ChatMessage()))

        assertEquals(1, target.rows.size)
    }

    /** Review Focus 2. */
    @Test
    fun `one push is one delivery`() {
        val dispatcher = CountingDispatcher()

        SelectedStationMessagesFeed(emptyTable(), dispatcher).push(listOf(ChatMessage()))

        assertEquals(1, dispatcher.deliveries)
    }

    /** Review Focus 3: no station selected is the state the window opens in. */
    @Test
    fun `an empty push clears the table rather than failing`() {
        val target = emptyTable()
        val feed = SelectedStationMessagesFeed(target, CountingDispatcher())

        feed.push(listOf(ChatMessage()))
        feed.push(emptyList())

        assertEquals(0, target.rows.size)
    }

    /** The caller hands over a live mirror; the table must not follow it. */
    @Test
    fun `the table keeps its own copy`() {
        val target = emptyTable()
        val source = mutableListOf(ChatMessage())

        SelectedStationMessagesFeed(target, CountingDispatcher()).push(source)
        source.clear()

        assertEquals(1, target.rows.size, "the table followed the caller's list")
    }
}
