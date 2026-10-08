package kst4contest.observe;

/** An {@link ObservableValue} that can be written to. */
public interface MutableValue<T> extends ObservableValue<T> {

    /**
     * Replaces the value and notifies the listeners. Setting a value that
     * equals the current one notifies nobody.
     */
    void set(T value);
}
