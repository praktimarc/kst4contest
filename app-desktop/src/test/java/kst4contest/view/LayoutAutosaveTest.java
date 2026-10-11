package kst4contest.view;

import kst4contest.observe.UiDispatcher;
import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static kst4contest.testing.Await.awaitUntil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The debounce behind the column widths.
 *
 * <p>Untested while it ran on a JavaFX {@code PauseTransition}, and the failure mode is
 * silent: a timer that never fires means the operator's column widths are simply not
 * saved, with nothing to see until the next start.</p>
 *
 * <p>The negative checks run on a {@link FakeDelayTimer} the test fires itself, on the
 * event thread as the Swing timer would, so "nothing was written" is a fact rather than a
 * 1.2 s guess. The first test goes through the real Swing timer so the default wiring
 * stays covered.</p>
 */
class LayoutAutosaveTest {

    private final UiDispatcher dispatcher = new AwtUiDispatcher();

    /** The real Swing timer, end to end, from a thread that is not the event thread. */
    @Test
    void aRequestFromAForeignThreadEventuallyWrites() {
        AtomicBoolean written = new AtomicBoolean();
        LayoutAutosave autosave = new LayoutAutosave(() -> written.set(true), dispatcher);

        autosave.requestSave();

        awaitUntil(Duration.ofSeconds(5),
                "a save requested off the user-interface thread must still happen",
                written::get);
    }

    @Test
    void burstsOfRequestsCollapseIntoOneWrite() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        FakeDelayTimer.Factory timers = new FakeDelayTimer.Factory();
        LayoutAutosave autosave = new LayoutAutosave(writes::incrementAndGet, dispatcher, timers);

        // What dragging a column divider looks like: many events, one intended save.
        EventQueue.invokeAndWait(() -> {
            for (int i = 0; i < 20; i++) {
                autosave.requestSave();
            }
        });
        assertEquals(20, timers.timer().restarts(), "every request must restart the delay");
        assertEquals(0, writes.get(), "nothing may be written before the delay elapses");

        EventQueue.invokeAndWait(timers.timer()::fire);
        assertEquals(1, writes.get(), "a burst must collapse into a single write");

        EventQueue.invokeAndWait(timers.timer()::fire);
        assertEquals(1, writes.get(), "the timer is one-shot; a burst must not queue a second write");
    }

    @Test
    void cancellingBeforeTheDelayElapsesSuppressesTheWrite() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        FakeDelayTimer.Factory timers = new FakeDelayTimer.Factory();
        LayoutAutosave autosave = new LayoutAutosave(writes::incrementAndGet, dispatcher, timers);

        EventQueue.invokeAndWait(() -> {
            autosave.requestSave();
            autosave.cancelPending();
        });
        assertFalse(timers.timer().isArmed(), "cancelling must stop the timer");

        EventQueue.invokeAndWait(timers.timer()::fire);
        assertEquals(0, writes.get(), "a cancelled save must not be written");
    }

    @Test
    void flushingWritesAtOnceAndOnlyOnce() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        FakeDelayTimer.Factory timers = new FakeDelayTimer.Factory();
        LayoutAutosave autosave = new LayoutAutosave(writes::incrementAndGet, dispatcher, timers);

        EventQueue.invokeAndWait(() -> {
            autosave.requestSave();
            autosave.flushPending();
            // A second flush has nothing left to write; shutdownRuntime calls flush then cancel.
            autosave.flushPending();
        });

        assertEquals(1, writes.get(), "flushing a pending save must write exactly once");
        assertFalse(timers.timer().isArmed(), "a flush must also stop the pending timer");
        assertTrue(timers.timer().restarts() > 0, "the request must have armed the timer first");
    }
}
