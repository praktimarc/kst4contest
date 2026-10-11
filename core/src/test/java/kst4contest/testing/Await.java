package kst4contest.testing;

import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Bounded polling for tests that wait on another thread.
 *
 * <p>Replaces fixed {@code Thread.sleep} pauses: a condition that is already true returns
 * at once, and one that never becomes true fails with a message instead of letting the
 * test pass or fail on the speed of the machine.</p>
 */
public final class Await {

    private static final long POLL_INTERVAL_MS = 10L;

    private Await() {
    }

    /**
     * Polls {@code condition} every 10 ms until it is true.
     *
     * @param timeout   how long to keep polling before failing
     * @param condition what must become true
     * @throws AssertionError when the timeout elapses first
     */
    public static void awaitUntil(Duration timeout, BooleanSupplier condition) {
        awaitUntil(timeout, "condition not met within " + timeout, condition);
    }

    /**
     * Polls {@code condition} every 10 ms until it is true.
     *
     * @param timeout   how long to keep polling before failing
     * @param message   what the failure should say when the timeout elapses
     * @param condition what must become true
     * @throws AssertionError when the timeout elapses first
     */
    public static void awaitUntil(Duration timeout, String message, BooleanSupplier condition) {
        awaitUntil(timeout, () -> message, condition);
    }

    /**
     * Polls {@code condition} every 10 ms until it is true.
     *
     * @param timeout   how long to keep polling before failing
     * @param message   builds the failure message at the moment of failure, so it can
     *                  describe the state that was actually seen
     * @param condition what must become true
     * @throws AssertionError when the timeout elapses first
     */
    public static void awaitUntil(Duration timeout, Supplier<String> message, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            if (condition.getAsBoolean()) {
                return;
            }
            if (System.nanoTime() >= deadline) {
                throw new AssertionError(message.get());
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting: " + message.get(), interrupted);
            }
        }
    }
}
