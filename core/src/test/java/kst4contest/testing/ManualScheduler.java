package kst4contest.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link ScheduledExecutorService} whose delays the test releases by hand.
 *
 * <p>Immediate work ({@code execute}, {@code submit}) runs on a real single thread, as
 * it does in production, so the class under test keeps its threading. Delayed work is
 * recorded instead of timed: {@link #runDelayedTasks()} runs everything scheduled so far
 * and {@link #runPeriodicTask()} runs the one fixed-rate task, each on that same thread
 * and each waited for. A test therefore triggers "five seconds later" itself rather than
 * sleeping through it.</p>
 */
public final class ManualScheduler extends AbstractExecutorService
        implements ScheduledExecutorService {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "manual-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private final List<Runnable> delayed = new CopyOnWriteArrayList<>();
    private final AtomicReference<Runnable> periodic = new AtomicReference<>();

    /** Whether at least one delayed task is waiting to be released. */
    public boolean hasDelayedTasks() {
        return !delayed.isEmpty();
    }

    /**
     * Runs every delayed task recorded so far, in scheduling order, on the worker thread,
     * and returns once they have all finished. Tasks they schedule stay pending.
     */
    public void runDelayedTasks() throws Exception {
        List<Runnable> due = new ArrayList<>(delayed);
        delayed.removeAll(due);
        for (Runnable task : due) {
            runOnWorker(task);
        }
    }

    /** Runs the fixed-rate task once, on the worker thread, and waits for it. */
    public void runPeriodicTask() throws Exception {
        Runnable task = periodic.get();
        if (task == null) {
            throw new AssertionError("no fixed-rate task has been scheduled");
        }
        runOnWorker(task);
    }

    private void runOnWorker(Runnable task) throws Exception {
        try {
            worker.submit(task).get(10, TimeUnit.SECONDS);
        } catch (ExecutionException failed) {
            throw new AssertionError("scheduled task failed", failed.getCause());
        } catch (TimeoutException tooSlow) {
            throw new AssertionError("scheduled task did not finish within 10 s", tooSlow);
        }
    }

    @Override
    public void execute(Runnable command) {
        worker.execute(command);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        RecordedTask<Void> task = new RecordedTask<>(command, delay, unit);
        delayed.add(task);
        return task;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("not used by the classes under test");
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (!periodic.compareAndSet(null, command)) {
            throw new UnsupportedOperationException("only one fixed-rate task is supported");
        }
        return new RecordedTask<>(command, initialDelay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("not used by the classes under test");
    }

    @Override
    public void shutdown() {
        worker.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        delayed.clear();
        return worker.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return worker.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return worker.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return worker.awaitTermination(timeout, unit);
    }

    /** A recorded delay: a plain future whose "remaining delay" is what was asked for. */
    private static final class RecordedTask<V> extends FutureTask<V> implements ScheduledFuture<V> {

        private final long delayNanos;

        RecordedTask(Runnable command, long delay, TimeUnit unit) {
            super(command, null);
            this.delayNanos = unit.toNanos(delay);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delayNanos, TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(delayNanos, other.getDelay(TimeUnit.NANOSECONDS));
        }
    }
}
