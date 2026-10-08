package kst4contest.observe;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectUiDispatcherTest {

    @Test
    void runsTheTaskImmediately() {
        AtomicInteger runs = new AtomicInteger();
        new DirectUiDispatcher().runOnUi(runs::incrementAndGet);
        assertEquals(1, runs.get());
    }

    @Test
    void reportsEveryThreadAsTheUiThread() {
        assertTrue(new DirectUiDispatcher().isUiThread());
    }

    @Test
    void aFailingTaskDoesNotPropagateToTheCaller() {
        AtomicBoolean reached = new AtomicBoolean();
        new DirectUiDispatcher().runOnUi(() -> { throw new IllegalStateException("boom"); });
        reached.set(true);
        assertTrue(reached.get(), "a failing UI task must not terminate the calling thread");
    }
}
