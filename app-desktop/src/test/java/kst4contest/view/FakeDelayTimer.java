package kst4contest.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A {@link DelayTimer} that fires only when the test says so.
 *
 * <p>Models exactly what the Swing timer promises and nothing about wall time: a
 * {@code restart()} arms it, a {@code stop()} disarms it, and {@link #fire()} runs the
 * action once if it is armed. That turns "the run did not happen" from a wait-and-hope
 * into an assertion.</p>
 */
final class FakeDelayTimer implements DelayTimer {

    private final Runnable onFire;
    private boolean armed;
    private int restarts;

    FakeDelayTimer(final Runnable onFire) {
        this.onFire = onFire;
    }

    @Override
    public void restart() {
        armed = true;
        restarts++;
    }

    @Override
    public void stop() {
        armed = false;
    }

    /** What the Swing timer does when the delay elapses: run once, then disarm. */
    void fire() {
        if (!armed) {
            return;
        }
        armed = false;
        onFire.run();
    }

    boolean isArmed() {
        return armed;
    }

    int restarts() {
        return restarts;
    }

    /** Hands out one fake timer and keeps it for the test to fire. */
    static final class Factory implements DelayTimer.Factory {

        private FakeDelayTimer created;
        private int createdDelayMs;

        @Override
        public DelayTimer create(final int delayMs, final Runnable onFire) {
            created = new FakeDelayTimer(onFire);
            createdDelayMs = delayMs;
            return created;
        }

        FakeDelayTimer timer() {
            assertNotNull(created, "the class under test did not create its timer");
            return created;
        }

        int delayMs() {
            return createdDelayMs;
        }
    }
}
