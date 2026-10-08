package kst4contest.view;

import kst4contest.observe.UiDispatcher;

import javax.swing.Timer;
import java.util.Objects;

/**
 * Coalesces layout changes into selective preferences writes.
 *
 * <p>Dragging a column divider produces a stream of change events; writing the
 * preferences file on each would make the drag stutter. The write is therefore delayed
 * and restarted on every request, so a burst ends in exactly one write.</p>
 *
 * <p>The class knows nothing about where the layout goes: it takes the write action, not
 * the preferences. That is what lets the debounce be tested without touching the
 * operator's profile on disk.</p>
 */
public final class LayoutAutosave {

    private static final int SAVE_DELAY_MS = 750;

    private final Runnable writeLayout;
    private final UiDispatcher uiDispatcher;
    private final Timer saveDelay;
    private boolean pending;

    /**
     * @param writeLayout  persists the layout; called on the user-interface thread
     * @param uiDispatcher confines the pending state to one thread
     */
    public LayoutAutosave(final Runnable writeLayout, final UiDispatcher uiDispatcher) {
        this.writeLayout = Objects.requireNonNull(writeLayout, "writeLayout");
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher, "uiDispatcher");
        /*
         * A javax.swing.Timer and not the former JavaFX PauseTransition: it fires on the
         * AWT event thread, which is where the user interface lives now, and restart()
         * has the same meaning playFromStart() had. A PauseTransition would need the
         * JavaFX toolkit to be running, which it no longer is.
         */
        this.saveDelay = new Timer(SAVE_DELAY_MS, event -> flushPending());
        this.saveDelay.setRepeats(false);
    }

    /** Asks for a save, restarting the delay. Safe to call from any thread. */
    public void requestSave() {
        if (!uiDispatcher.isUiThread()) {
            uiDispatcher.runOnUi(this::requestSave);
            return;
        }

        pending = true;
        saveDelay.restart();
    }

    /** Writes a pending layout now. Does nothing when nothing is pending. */
    public void flushPending() {
        if (!pending) {
            return;
        }

        saveDelay.stop();
        pending = false;
        writeLayout.run();
    }

    /** Drops a pending layout without writing it. */
    public void cancelPending() {
        saveDelay.stop();
        pending = false;
    }
}
