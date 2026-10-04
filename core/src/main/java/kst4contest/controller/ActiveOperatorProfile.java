package kst4contest.controller;

import kst4contest.model.OperatorProfileSelection;

/**
 * Holds the operator profile the current runtime works with.
 *
 * <p>The state is deliberately static, which is safe here for reasons that did not apply
 * to the former static database connection: the value is immutable, it holds no live
 * resource, and it is set on the JavaFX Application Thread before anything reads it -
 * during startup, and again during a profile switch after the previous runtime has been
 * shut down completely.</p>
 */
public final class ActiveOperatorProfile {

    private static volatile OperatorProfileSelection currentSelection;

    private ActiveOperatorProfile() {
        // Utility class.
    }

    /**
     * Returns the active profile selection.
     *
     * @return the active selection, or null when startup has not resolved one yet
     */
    public static OperatorProfileSelection get() {
        return currentSelection;
    }

    /**
     * Sets the active profile selection.
     *
     * @param selection selection to activate
     */
    public static void set(final OperatorProfileSelection selection) {
        currentSelection = selection;
    }

    /**
     * Returns whether a profile has already been resolved for this runtime.
     *
     * @return true if a selection is present
     */
    public static boolean isInitialized() {
        return currentSelection != null;
    }
}
