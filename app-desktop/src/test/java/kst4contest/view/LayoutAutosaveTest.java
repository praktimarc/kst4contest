package kst4contest.view;

import kst4contest.observe.UiDispatcher;
import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The debounce behind the column widths.
 *
 * <p>Untested while it ran on a JavaFX {@code PauseTransition}, and the failure mode is
 * silent: a timer that never fires means the operator's column widths are simply not
 * saved, with nothing to see until the next start.</p>
 */
class LayoutAutosaveTest {

    private final UiDispatcher dispatcher = new AwtUiDispatcher();

    @Test
    void aRequestFromAForeignThreadEventuallyWrites() throws Exception {
        CountDownLatch written = new CountDownLatch(1);
        LayoutAutosave autosave = new LayoutAutosave(written::countDown, dispatcher);

        autosave.requestSave();

        assertTrue(written.await(5, TimeUnit.SECONDS),
                "a save requested off the user-interface thread must still happen");
    }

    @Test
    void burstsOfRequestsCollapseIntoOneWrite() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        CountDownLatch written = new CountDownLatch(1);
        LayoutAutosave autosave = new LayoutAutosave(() -> {
            writes.incrementAndGet();
            written.countDown();
        }, dispatcher);

        // What dragging a column divider looks like: many events, one intended save.
        for (int i = 0; i < 20; i++) {
            autosave.requestSave();
        }

        assertTrue(written.await(5, TimeUnit.SECONDS), "the burst never produced a write");
        Thread.sleep(300);
        assertEquals(1, writes.get(), "a burst must collapse into a single write");
    }

    @Test
    void cancellingBeforeTheDelayElapsesSuppressesTheWrite() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        LayoutAutosave autosave = new LayoutAutosave(writes::incrementAndGet, dispatcher);

        EventQueue.invokeAndWait(() -> {
            autosave.requestSave();
            autosave.cancelPending();
        });

        Thread.sleep(1_200);
        assertEquals(0, writes.get(), "a cancelled save must not be written");
    }

    @Test
    void flushingWritesAtOnceAndOnlyOnce() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        LayoutAutosave autosave = new LayoutAutosave(writes::incrementAndGet, dispatcher);

        EventQueue.invokeAndWait(() -> {
            autosave.requestSave();
            autosave.flushPending();
            // A second flush has nothing left to write; shutdownRuntime calls flush then cancel.
            autosave.flushPending();
        });

        assertEquals(1, writes.get(), "flushing a pending save must write exactly once");
    }
}
