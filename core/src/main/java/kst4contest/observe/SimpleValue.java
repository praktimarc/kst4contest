package kst4contest.observe;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The default {@link MutableValue}. Safe to read and write from any thread;
 * listeners run on the thread that calls {@link #set}, so anything touching the
 * user interface must hand over through {@link UiDispatcher}.
 */
public class SimpleValue<T> implements MutableValue<T> {

    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
    private volatile T value;

    public SimpleValue() {
        this(null);
    }

    public SimpleValue(T initial) {
        this.value = initial;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(T newValue) {
        if (Objects.equals(value, newValue)) {
            return;
        }
        value = newValue;
        for (Consumer<T> listener : listeners) {
            try {
                listener.accept(newValue);
            } catch (RuntimeException e) {
                // A broken listener must not keep the remaining ones from running,
                // and must not terminate the thread that produced the value.
                System.err.println("[observe] value listener failed: " + e);
            }
        }
    }

    @Override
    public void addListener(Consumer<T> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeListener(Consumer<T> listener) {
        listeners.remove(listener);
    }
}
