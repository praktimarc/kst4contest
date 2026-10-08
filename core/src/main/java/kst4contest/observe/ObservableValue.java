package kst4contest.observe;

import java.util.function.Consumer;

/**
 * A value that can be observed for changes.
 *
 * <p>Deliberately minimal: this replaces the JavaFX property types in the
 * domain layer without pulling in a reactive framework. {@code null} means the
 * value is unavailable, never a default.
 */
public interface ObservableValue<T> {

    /** The current value, or {@code null} when it is unavailable. */
    T get();

    /** Registers a listener that is called with the new value after each change. */
    void addListener(Consumer<T> listener);

    /** Removes a previously registered listener. Unknown listeners are ignored. */
    void removeListener(Consumer<T> listener);
}
