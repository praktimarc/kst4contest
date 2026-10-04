package kst4contest.view;

import javafx.application.Platform;
import kst4contest.observe.UiDispatcher;

/** The {@link UiDispatcher} backed by the JavaFX application thread. */
public class JavaFxUiDispatcher implements UiDispatcher {

    @Override
    public void runOnUi(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            runGuarded(task);
        } else {
            Platform.runLater(() -> runGuarded(task));
        }
    }

    @Override
    public boolean isUiThread() {
        return Platform.isFxApplicationThread();
    }

    private static void runGuarded(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            // Must not tear down the JavaFX application thread.
            System.err.println("[observe] UI task failed: " + e);
        }
    }
}
