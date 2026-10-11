package kst4contest.view.compose

import kst4contest.model.OperatorProfile
import kst4contest.testing.Await.awaitUntil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * The picker must be openable from the user-interface thread.
 *
 * Since the dispatcher moved to the AWT event thread, `ComposeMenuActions.switchOperatorProfile`
 * delivers the File menu's profile switch there. The picker's former thread-plus-latch
 * construction deadlocked against that: a `CountDownLatch.await()` does not pump events, while
 * every continuation of the window's own `application { }` — composition, the button callbacks,
 * even the return that would release the latch — is posted to that same thread by skiko's
 * SwingDispatcher. The window never appeared and the client froze for good.
 */
class OperatorProfilePickerWindowTest {

    private fun profiles() = listOf(
        OperatorProfile().apply { profileId = "root"; displayName = "Root" },
        OperatorProfile().apply { profileId = "second"; displayName = "Second" },
    )

    /** The picker's dialog, once it is on screen, or null while it is not. */
    private fun showingPickerDialog(): Window? =
        Window.getWindows().firstOrNull { it.isShowing && it.javaClass.name.contains("ComposeDialog") }

    /**
     * Closes the picker the way the operator's X would, as soon as it is showing.
     *
     * Runs on its own thread because both callers block the test thread: one inside
     * `invokeAndWait`, the other inside the modal `showAndSelect` itself. The dispose is
     * handed to the event thread, which a modal dialog keeps pumping. Waiting for the dialog
     * to appear replaces a fixed 1.5 s timer that was long enough on a fast machine only.
     */
    private fun disposePickerOnceShowing(): Future<*> =
        closer.submit {
            awaitUntil(Duration.ofSeconds(10), "the picker's dialog never appeared") {
                showingPickerDialog() != null
            }
            EventQueue.invokeLater { showingPickerDialog()?.dispose() }
        }

    private val closer = Executors.newSingleThreadExecutor()

    @Test
    fun opensAndReturnsWhenCalledOnTheEventThread() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        val worker = Executors.newSingleThreadExecutor()

        try {
            val closing = disposePickerOnceShowing()
            // invokeAndWait so the call really happens ON the event thread, as the menu does.
            val call = worker.submit { EventQueue.invokeAndWait { OperatorProfilePickerWindow.showAndSelect(profiles(), "root") } }

            // The deadlock had no timeout of its own: without a fix this never completes.
            call.get(20, TimeUnit.SECONDS)
            closing.get(1, TimeUnit.SECONDS)
        } finally {
            worker.shutdownNow()
            closer.shutdownNow()
        }
    }

    @Test
    fun opensAndReturnsWhenCalledOffTheEventThread() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        try {
            val closing = disposePickerOnceShowing()
            // The startup path: main() resolves the profile before any window exists.
            val chosen = OperatorProfilePickerWindow.showAndSelect(profiles(), "root")

            // Dismissed rather than confirmed, so the caller is told to quit.
            assertTrue(chosen.isEmpty, "a dismissed picker must report no choice")
            closing.get(1, TimeUnit.SECONDS)
        } finally {
            closer.shutdownNow()
        }
    }

    @Test
    fun theSelectionRulesAreUnchanged() {
        val state = OperatorProfilePickerState(profiles(), "second")

        assertEquals("second", state.selected?.profileId, "the preselected profile must win")
        assertTrue(state.canConfirm)

        state.select(profiles()[0])
        assertEquals("root", state.selected?.profileId)
    }
}
