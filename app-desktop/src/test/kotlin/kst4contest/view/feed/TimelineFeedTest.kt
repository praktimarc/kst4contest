package kst4contest.view.feed

import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.TimelineState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * What fills the timeline above the send field.
 *
 * It has its own class because of how it was found: the feed sat in a method guarded by
 * a JavaFX field, inside window construction that is never shown, and deleting that
 * construction would have emptied the Compose timeline with a green build and no
 * compile error. A feed that can be constructed in a test cannot hide like that.
 */
class TimelineFeedTest {

    /** Runs inline and counts, so a test can tell "delivered" from "delivered twice". */
    private class CountingDispatcher(private val uiThread: Boolean = true) : UiDispatcher {
        var deliveries = 0
            private set

        override fun runOnUi(task: Runnable) {
            deliveries++
            task.run()
        }

        override fun isUiThread(): Boolean = uiThread
    }

    private fun sked(call: String) =
        kst4contest.model.ContestSked(call, 90.0, 60_000L, kst4contest.model.Band.B_144)

    @Test
    fun `it puts what it is given into the timeline`() {
        val target = TimelineState()

        TimelineFeed(target, CountingDispatcher()).push(listOf(sked("DL1ABC")), emptyList(), 229.0, 60.0)

        assertEquals(1, target.skeds.size)
        assertEquals(229.0, target.antennaAzimuth, 1e-9)
        assertEquals(60.0, target.beamWidthDeg, 1e-9)
    }

    /** Review Focus 2: two feeds writing one state is invisible until one goes stale. */
    @Test
    fun `one push is one delivery`() {
        val dispatcher = CountingDispatcher()

        TimelineFeed(TimelineState(), dispatcher).push(emptyList(), emptyList(), 0.0, 0.0)

        assertEquals(1, dispatcher.deliveries)
    }

    /** Review Focus 4: the controller's listeners fire on network threads. */
    @Test
    fun `it hands over rather than writing from the calling thread`() {
        val dispatcher = CountingDispatcher(uiThread = false)

        TimelineFeed(TimelineState(), dispatcher).push(emptyList(), emptyList(), 10.0, 5.0)

        assertTrue(dispatcher.deliveries > 0) { "the feed wrote without going through the dispatcher" }
    }

    /** Review Focus 3: empty is the normal state before the first score run. */
    @Test
    fun `an empty push is not an error`() {
        val target = TimelineState()

        TimelineFeed(target, CountingDispatcher()).push(emptyList(), emptyList(), 0.0, 0.0)

        assertEquals(0, target.skeds.size)
        assertEquals(0, target.candidates.size)
    }

    /** The caller hands over snapshots of state that keeps changing. */
    @Test
    fun `the timeline keeps its own copy`() {
        val target = TimelineState()
        val source = mutableListOf(sked("DL1ABC"))

        TimelineFeed(target, CountingDispatcher()).push(source, emptyList(), 0.0, 0.0)
        source.clear()

        assertEquals(1, target.skeds.size, "the timeline followed the caller's list")
    }
}
