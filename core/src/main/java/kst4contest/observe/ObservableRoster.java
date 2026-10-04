package kst4contest.observe;

import java.util.List;
import java.util.function.Consumer;

/**
 * An observable collection that hands out immutable snapshots.
 *
 * <p>There is deliberately no incremental change protocol: consumers receive
 * the whole new content and decide for themselves what to do with it. That
 * matches how a declarative user interface redraws, and it keeps the domain
 * layer testable without a running toolkit.
 *
 * <p>Listeners run on the thread that performed the mutation. Anything that
 * touches the user interface must hand over through {@link UiDispatcher}.
 */
public interface ObservableRoster<T> {

    /** An immutable view of the current content. */
    List<T> snapshot();

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    /** Registers a listener that is called with the new content after each change. */
    void addListener(Consumer<List<T>> listener);

    /** Removes a previously registered listener. Unknown listeners are ignored. */
    void removeListener(Consumer<List<T>> listener);
}
