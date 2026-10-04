package kst4contest.view;

import javax.swing.Timer;
import java.util.Objects;

/**
 * Collapses a burst of requests into one run, a fixed delay after the last of them.
 *
 * <p>Replaces the JavaFX pause transition the station list used. A
 * {@code javax.swing.Timer} fires on the AWT event thread, which is where the user
 * interface lives, and {@code restart()} means exactly what that class's
 * {@code playFromStart()} meant. The old one needed a running JavaFX toolkit, which the
 * application no longer starts.</p>
 */
public final class CoalescingTrigger {

    private final Timer timer;

    /**
     * @param delayMs how long to wait after the last request
     * @param action  runs on the user-interface thread once the delay elapses
     */
    public CoalescingTrigger(final int delayMs, final Runnable action) {
        Objects.requireNonNull(action, "action");
        this.timer = new Timer(delayMs, event -> action.run());
        this.timer.setRepeats(false);
    }

    /** Requests a run, restarting the delay. */
    public void trigger() {
        timer.restart();
    }

    /** Drops a pending run. Idempotent, and the trigger stays usable afterwards. */
    public void cancel() {
        timer.stop();
    }
}
