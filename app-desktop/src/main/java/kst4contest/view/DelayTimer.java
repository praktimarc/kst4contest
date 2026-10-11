package kst4contest.view;

import javax.swing.Timer;
import java.util.Objects;

/**
 * A one-shot delay that can be restarted and stopped.
 *
 * <p>The thin seam between the two coalescing classes in this package and the
 * {@code javax.swing.Timer} they run on. In production the Swing timer is the only
 * implementation; tests substitute one whose firing they control, so "nothing ran" can
 * be asserted without waiting for a delay that might not have elapsed yet.</p>
 */
interface DelayTimer {

    /** Starts the delay, or starts it over if it is already running. */
    void restart();

    /** Drops a pending run. Idempotent. */
    void stop();

    /** Creates one-shot timers for a fixed delay and action. */
    interface Factory {
        /**
         * @param delayMs how long the timer waits after a {@link #restart()}
         * @param onFire  what runs once the delay elapses
         */
        DelayTimer create(int delayMs, Runnable onFire);
    }

    /**
     * The production factory: a {@code javax.swing.Timer}, which fires on the AWT event
     * thread where the user interface lives.
     */
    Factory SWING = (delayMs, onFire) -> new Swing(delayMs, onFire);

    /** {@code javax.swing.Timer} behind the interface. */
    final class Swing implements DelayTimer {

        private final Timer timer;

        Swing(final int delayMs, final Runnable onFire) {
            Objects.requireNonNull(onFire, "onFire");
            this.timer = new Timer(delayMs, event -> onFire.run());
            this.timer.setRepeats(false);
        }

        @Override
        public void restart() {
            timer.restart();
        }

        @Override
        public void stop() {
            timer.stop();
        }
    }
}
