package kst4contest.observe;

/**
 * Hands work over to the thread that owns the user interface.
 *
 * <p>This is the boundary the project has always had, named explicitly: worker
 * threads never mutate user-interface state themselves, they hand it over.
 * Before this interface existed the boundary was {@code Platform.runLater};
 * the rule is unchanged, only the dependency is gone.
 */
public interface UiDispatcher {

    /** Runs the task on the user-interface thread, now or later. */
    void runOnUi(Runnable task);

    /** Whether the calling thread is the user-interface thread. */
    boolean isUiThread();
}
