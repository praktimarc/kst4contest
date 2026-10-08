package kst4contest.view;

import kst4contest.observe.UiDispatcher;

import java.awt.EventQueue;

/**
 * The {@link UiDispatcher} backed by the AWT event dispatch thread.
 *
 * <p>That is the thread Compose Desktop composes on, so this is where user-interface
 * state belongs. The contract is the one {@code JavaFxUiDispatcher} had, down to the
 * inline execution: a task handed over from the user-interface thread runs before
 * {@link #runOnUi(Runnable)} returns rather than queueing behind work that is already
 * pending. Several callers in {@code ChatController} ask {@link #isUiThread()} first and
 * rely on exactly that.</p>
 */
public class AwtUiDispatcher implements UiDispatcher {

    @Override
    public void runOnUi(Runnable task) {
        if (EventQueue.isDispatchThread()) {
            runGuarded(task);
        } else {
            EventQueue.invokeLater(() -> runGuarded(task));
        }
    }

    @Override
    public boolean isUiThread() {
        return EventQueue.isDispatchThread();
    }

    private static void runGuarded(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            // Must not tear down the AWT event dispatch thread.
            System.err.println("[observe] UI task failed: " + e);
        }
    }
}
