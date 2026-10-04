package kst4contest.view.compose

import kst4contest.model.OperatorProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.Timer

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

    /** Closes whatever window the picker opened, the way the operator's X would. */
    private fun disposeOpenWindowsAfter(delayMs: Int): Timer =
        Timer(delayMs) {
            Window.getWindows()
                .filter { it.isShowing && it.javaClass.name.contains("ComposeDialog") }
                .forEach { it.dispose() }
        }.apply { isRepeats = true }

    @Test
    fun opensAndReturnsWhenCalledOnTheEventThread() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        val closer = disposeOpenWindowsAfter(1_500)
        val worker = Executors.newSingleThreadExecutor()

        try {
            closer.start()
            // invokeAndWait so the call really happens ON the event thread, as the menu does.
            val call = worker.submit { EventQueue.invokeAndWait { OperatorProfilePickerWindow.showAndSelect(profiles(), "root") } }

            // The deadlock had no timeout of its own: without a fix this never completes.
            call.get(20, TimeUnit.SECONDS)
        } finally {
            closer.stop()
            worker.shutdownNow()
        }
    }

    @Test
    fun opensAndReturnsWhenCalledOffTheEventThread() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        val closer = disposeOpenWindowsAfter(1_500)

        try {
            closer.start()
            // The startup path: main() resolves the profile before any window exists.
            val chosen = OperatorProfilePickerWindow.showAndSelect(profiles(), "root")

            // Dismissed rather than confirmed, so the caller is told to quit.
            assertTrue(chosen.isEmpty, "a dismissed picker must report no choice")
        } finally {
            closer.stop()
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
