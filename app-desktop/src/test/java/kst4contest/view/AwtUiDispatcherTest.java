package kst4contest.view;

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contract the user interface depends on everywhere.
 *
 * <p>{@code core} hands over every user-interface change through this interface, so a
 * mistake here does not show up as a compile error but as an occasionally stale window.
 * The four rules below are what the specification names for this stage.</p>
 */
class AwtUiDispatcherTest {

    private final AwtUiDispatcher dispatcher = new AwtUiDispatcher();

    @Test
    void deliversWorkFromAForeignThreadToTheEventThread() throws Exception {
        AtomicBoolean ranOnEventThread = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);

        dispatcher.runOnUi(() -> {
            ranOnEventThread.set(EventQueue.isDispatchThread());
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS), "the task was never delivered");
        assertTrue(ranOnEventThread.get(), "the task must run on the AWT event thread");
    }

    @Test
    void runsInlineWhenAlreadyOnTheEventThread() throws Exception {
        AtomicInteger runs = new AtomicInteger();

        EventQueue.invokeAndWait(() -> {
            dispatcher.runOnUi(runs::incrementAndGet);
            // Inline means: already done when runOnUi returns, not queued behind us.
            assertEquals(1, runs.get(), "a task handed over on the UI thread must run at once");
        });

        assertEquals(1, runs.get());
    }

    @Test
    void reportsTheEventThreadAndOnlyThat() throws Exception {
        assertFalse(dispatcher.isUiThread(), "a test thread is not the UI thread");

        AtomicBoolean onEventThread = new AtomicBoolean();
        EventQueue.invokeAndWait(() -> onEventThread.set(dispatcher.isUiThread()));

        assertTrue(onEventThread.get(), "the AWT event thread must report itself as the UI thread");
    }

    @Test
    void aFailingTaskNeitherReachesTheCallerNorKillsTheEventThread() throws Exception {
        dispatcher.runOnUi(() -> {
            throw new IllegalStateException("boom");
        });

        CountDownLatch stillAlive = new CountDownLatch(1);
        dispatcher.runOnUi(stillAlive::countDown);

        assertTrue(stillAlive.await(5, TimeUnit.SECONDS),
                "the event thread must survive a failing UI task");
    }

    @Test
    void aFailingTaskHandedOverOnTheEventThreadDoesNotReachTheCaller() throws Exception {
        AtomicBoolean reachedTheLineAfter = new AtomicBoolean();

        EventQueue.invokeAndWait(() -> {
            dispatcher.runOnUi(() -> {
                throw new IllegalStateException("boom");
            });
            reachedTheLineAfter.set(true);
        });

        assertTrue(reachedTheLineAfter.get(),
                "an inline failure must not propagate into the caller");
    }
}
