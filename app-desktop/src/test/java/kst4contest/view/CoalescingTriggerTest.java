package kst4contest.view;

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coalescer behind the station list.
 *
 * <p>Untested while it ran on a JavaFX {@code PauseTransition}, and both ways of being
 * wrong are invisible in a build: too eager and the table redraws on every message under
 * contest load, never and the table stops updating altogether.</p>
 */
class CoalescingTriggerTest {

    @Test
    void aBurstOfTriggersProducesOneRun() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch ran = new CountDownLatch(1);
        CoalescingTrigger trigger = new CoalescingTrigger(120, () -> {
            runs.incrementAndGet();
            ran.countDown();
        });

        // What a busy chat looks like: many updates, one intended redraw.
        for (int i = 0; i < 25; i++) {
            trigger.trigger();
        }

        assertTrue(ran.await(5, TimeUnit.SECONDS), "the burst never produced a run");
        Thread.sleep(300);
        assertEquals(1, runs.get(), "a burst must coalesce into a single run");
    }

    @Test
    void theActionRunsOnTheEventThread() throws Exception {
        AtomicBoolean onEventThread = new AtomicBoolean();
        CountDownLatch ran = new CountDownLatch(1);
        CoalescingTrigger trigger = new CoalescingTrigger(60, () -> {
            onEventThread.set(EventQueue.isDispatchThread());
            ran.countDown();
        });

        trigger.trigger();

        assertTrue(ran.await(5, TimeUnit.SECONDS));
        assertTrue(onEventThread.get(), "the action redraws tables and must be on the UI thread");
    }

    @Test
    void aLaterTriggerPostponesTheRun() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CoalescingTrigger trigger = new CoalescingTrigger(250, runs::incrementAndGet);

        trigger.trigger();
        Thread.sleep(150);
        trigger.trigger();
        Thread.sleep(150);

        // 300 ms have passed, but the second trigger restarted the wait at 150 ms.
        assertEquals(0, runs.get(),
                "a new trigger must restart the delay, not let the old one fire");
    }

    @Test
    void cancellingBeforeTheDelayElapsesSuppressesTheRun() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CoalescingTrigger trigger = new CoalescingTrigger(120, runs::incrementAndGet);

        trigger.trigger();
        trigger.cancel();
        Thread.sleep(400);

        assertEquals(0, runs.get(), "a cancelled trigger must not run; shutdown relies on it");
    }

    @Test
    void triggeringAgainAfterACancelStillWorks() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch ran = new CountDownLatch(1);
        CoalescingTrigger trigger = new CoalescingTrigger(80, () -> {
            runs.incrementAndGet();
            ran.countDown();
        });

        trigger.trigger();
        trigger.cancel();
        trigger.trigger();

        assertTrue(ran.await(5, TimeUnit.SECONDS),
                "a cancel must not disable the trigger for good");
        assertEquals(1, runs.get());
    }
}
