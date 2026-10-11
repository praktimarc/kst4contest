package kst4contest.view;

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
 * The coalescer behind the station list.
 *
 * <p>Untested while it ran on a JavaFX {@code PauseTransition}, and both ways of being
 * wrong are invisible in a build: too eager and the table redraws on every message under
 * contest load, never and the table stops updating altogether.</p>
 *
 * <p>All but one test run on a {@link FakeDelayTimer} the test fires itself, so "it did
 * not run" is a fact and not a 400 ms guess. The one exception goes through the real
 * Swing timer so the default wiring stays covered.</p>
 */
class CoalescingTriggerTest {

    @Test
    void aBurstOfTriggersProducesOneRun() {
        AtomicInteger runs = new AtomicInteger();
        FakeDelayTimer.Factory timers = new FakeDelayTimer.Factory();
        CoalescingTrigger trigger = new CoalescingTrigger(120, runs::incrementAndGet, timers);

        // What a busy chat looks like: many updates, one intended redraw.
        for (int i = 0; i < 25; i++) {
            trigger.trigger();
        }
        assertEquals(0, runs.get(), "nothing may run before the delay elapses");

        timers.timer().fire();
        assertEquals(1, runs.get(), "a burst must coalesce into a single run");

        timers.timer().fire();
        assertEquals(1, runs.get(), "the timer is one-shot; a burst must not queue a second run");
    }

    /** The real Swing timer, end to end: the action has to arrive on the event thread. */
    @Test
    void theActionRunsOnTheEventThread() {
        AtomicBoolean onEventThread = new AtomicBoolean();
        AtomicBoolean ran = new AtomicBoolean();
        CoalescingTrigger trigger = new CoalescingTrigger(60, () -> {
            onEventThread.set(EventQueue.isDispatchThread());
            ran.set(true);
        });

        trigger.trigger();

        awaitUntil(Duration.ofSeconds(5), "the Swing timer never fired", ran::get);
        assertTrue(onEventThread.get(), "the action redraws tables and must be on the UI thread");
    }

    @Test
    void aLaterTriggerPostponesTheRun() {
        AtomicInteger runs = new AtomicInteger();
        FakeDelayTimer.Factory timers = new FakeDelayTimer.Factory();
        CoalescingTrigger trigger = new CoalescingTrigger(250, runs::incrementAndGet, timers);

        trigger.trigger();
        trigger.trigger();

        assertEquals(2, timers.timer().restarts(),
                "a new trigger must restart the delay, not let the old one fire");
        assertEquals(0, runs.get(), "nothing has elapsed yet, so nothing may have run");
        assertEquals(250, timers.delayMs());
    }

    @Test
    void cancellingBeforeTheDelayElapsesSuppressesTheRun() {
        AtomicInteger runs = new AtomicInteger();
        FakeDelayTimer.Factory timers = new FakeDelayTimer.Factory();
        CoalescingTrigger trigger = new CoalescingTrigger(120, runs::incrementAndGet, timers);

        trigger.trigger();
        trigger.cancel();
        assertFalse(timers.timer().isArmed(), "cancel must stop the timer");

        timers.timer().fire();
        assertEquals(0, runs.get(), "a cancelled trigger must not run; shutdown relies on it");
    }

    @Test
    void triggeringAgainAfterACancelStillWorks() {
        AtomicInteger runs = new AtomicInteger();
        FakeDelayTimer.Factory timers = new FakeDelayTimer.Factory();
        CoalescingTrigger trigger = new CoalescingTrigger(80, runs::incrementAndGet, timers);

        trigger.trigger();
        trigger.cancel();
        trigger.trigger();
        assertTrue(timers.timer().isArmed(), "a cancel must not disable the trigger for good");

        timers.timer().fire();
        assertEquals(1, runs.get());
    }
}
