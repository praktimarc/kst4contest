package kst4contest.observe;

/**
 * Runs every task on the calling thread. Intended for tests and for headless
 * use; it is not a user-interface thread in any real sense.
 */
public class DirectUiDispatcher implements UiDispatcher {

    @Override
    public void runOnUi(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            System.err.println("[observe] UI task failed: " + e);
        }
    }

    @Override
    public boolean isUiThread() {
        return true;
    }
}
