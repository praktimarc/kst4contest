package kst4contest.view.feed

import kst4contest.controller.On4KstConnectionState
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.MainWindowSurroundings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * What fills the connection indicator in the Compose status bar.
 *
 * The JavaFX method this came out of did two things in one breath: it wrote the Compose
 * state and then touched a JavaFX tooltip. Deleting the construction that creates the
 * tooltip would have left live callers running into the second half.
 */
class ConnectionStateFeedTest {

    private class CountingDispatcher : UiDispatcher {
        var deliveries = 0
            private set

        override fun runOnUi(task: Runnable) {
            deliveries++
            task.run()
        }

        override fun isUiThread(): Boolean = true
    }

    @Test
    fun `the state and its detail reach the status bar`() {
        val target = MainWindowSurroundings()

        ConnectionStateFeed(target, CountingDispatcher())
            .push(On4KstConnectionState.ONLINE, "logged in as DN9APW")

        assertEquals(On4KstConnectionState.ONLINE, target.connectionState)
        assertEquals("logged in as DN9APW", target.connectionDetail)
    }

    /**
     * Review Focus 3. The JavaFX method substituted DISCONNECTED for an absent state
     * and the state's own name for a blank detail; an operator reading "null" in a
     * status bar learns nothing.
     */
    @Test
    fun `a missing state reads as disconnected`() {
        val target = MainWindowSurroundings()

        ConnectionStateFeed(target, CountingDispatcher()).push(null, null)

        assertEquals(On4KstConnectionState.DISCONNECTED, target.connectionState)
        assertEquals("DISCONNECTED", target.connectionDetail)
    }

    @Test
    fun `a blank detail reads as the state itself`() {
        val target = MainWindowSurroundings()

        ConnectionStateFeed(target, CountingDispatcher())
            .push(On4KstConnectionState.CONNECTING, "   ")

        assertEquals("CONNECTING", target.connectionDetail)
    }

    /** Review Focus 2. */
    @Test
    fun `one push is one delivery`() {
        val dispatcher = CountingDispatcher()

        ConnectionStateFeed(MainWindowSurroundings(), dispatcher)
            .push(On4KstConnectionState.ONLINE, "x")

        assertEquals(1, dispatcher.deliveries)
    }
}
