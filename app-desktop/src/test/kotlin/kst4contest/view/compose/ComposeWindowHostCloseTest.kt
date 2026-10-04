package kst4contest.view.compose

import androidx.compose.material3.Text
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Closing a window from the user-interface thread has to work.
 *
 * Since the dispatcher moved to the AWT event thread, every window toggle in the Windows
 * menu — and the whole teardown in `shutdownRuntime` — calls [ComposeWindowHost.close]
 * from that thread. The window's own `application { }` can only advance as continuations
 * posted to the same thread, so a plain `Thread.join` there can never be satisfied: the
 * interface froze for the timeout and `close()` returned with the window still registered
 * as open. The documented consequence is the worst one: a profile switch then found the
 * stale flag, raised the dead runtime's window, and the rebuilt profile got no settings
 * window at all — the window that carries the Connect button.
 */
class ComposeWindowHostCloseTest {

    private fun openHost(name: String): ComposeWindowHost {
        val host = ComposeWindowHost(name)
        host.show(
            title = "close test",
            darkMode = false,
            baseFontSizeSp = 12f,
            widthDp = 200f,
            heightDp = 120f,
        ) { Text("close test") }

        // Wait for the window to really exist, so the close has something to close.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (!host.isOpen && System.nanoTime() < deadline) {
            Thread.sleep(25)
        }
        assertTrue(host.isOpen, "the window never opened, so the close proves nothing")
        /* Let the first frame land; closing mid-composition is a different test. */
        Thread.sleep(600)
        return host
    }

    @Test
    fun closingFromTheEventThreadCompletesAndClearsTheOpenFlag() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        val host = openHost("close-from-edt")
        val worker = Executors.newSingleThreadExecutor()

        try {
            // invokeAndWait so close() really runs ON the event thread, as the menu does.
            worker.submit { EventQueue.invokeAndWait { host.close() } }
                .get(20, TimeUnit.SECONDS)

            assertFalse(
                host.isOpen,
                "close() must not return while the window still counts as open: the next "
                        + "show() would raise the dead window instead of building a new one",
            )
        } finally {
            worker.shutdownNow()
            host.close()
        }
    }

    @Test
    fun theWindowManagerCloseRunsTheHostsOwnCloseRequestWhenOneIsGiven() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        /*
         * The main window's close has to run the application's quit flow, not just close
         * the window. Compose's default `onCloseRequest = ::exitApplication` ends that one
         * window's `application { }` and leaves the process running with the chat still
         * connected and no main window — which is what a tiling window manager's close
         * produced: the window vanished and gradle kept reporting the task as executing.
         */
        val asked = java.util.concurrent.CountDownLatch(1)
        val host = ComposeWindowHost("close-request-override")
        host.show(
            title = "close request test",
            darkMode = false,
            baseFontSizeSp = 12f,
            widthDp = 200f,
            heightDp = 120f,
            onCloseRequest = { asked.countDown() },
        ) { Text("close request test") }

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (!host.isOpen && System.nanoTime() < deadline) {
            Thread.sleep(25)
        }
        assertTrue(host.isOpen, "the window never opened")
        Thread.sleep(600)

        try {
            /*
             * What the window manager sends: a close request on the window itself. Matched
             * by title rather than by type — the suite runs in one JVM and other tests'
             * Compose windows can still be showing, so a type filter sent the event to the
             * wrong window and this latch waited for ever.
             */
            EventQueue.invokeAndWait {
                val mine = java.awt.Window.getWindows()
                    .filterIsInstance<java.awt.Frame>()
                    .filter { it.isShowing && it.title == "close request test" }
                assertTrue(mine.isNotEmpty(), "this test's own window was not found")
                mine.forEach {
                    it.dispatchEvent(
                        java.awt.event.WindowEvent(it, java.awt.event.WindowEvent.WINDOW_CLOSING)
                    )
                }
            }

            assertTrue(
                asked.await(10, TimeUnit.SECONDS),
                "the host must route a close request to the handler it was given",
            )
        } finally {
            host.close()
        }
    }

    @Test
    fun closingFromAForeignThreadStillWorks() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        val host = openHost("close-from-worker")

        host.close()

        assertFalse(host.isOpen, "a close off the event thread must still wait for the window")
    }
}
