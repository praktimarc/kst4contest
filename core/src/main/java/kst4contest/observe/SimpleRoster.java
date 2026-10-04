package kst4contest.observe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The default {@link ObservableRoster}. Writes are serialised on the roster
 * itself; reads hand out an immutable copy taken under the same lock.
 */
public class SimpleRoster<T> implements ObservableRoster<T> {

    private final Object lock = new Object();
    private final List<T> items = new ArrayList<>();
    private final List<Consumer<List<T>>> listeners = new CopyOnWriteArrayList<>();

    /**
     * The last handed-out snapshot, reused until the content changes. Several
     * consumers read the same roster per batch — four message tabs, for one — and
     * copying the whole store for each of them is wasted work.
     */
    private volatile List<T> snapshotCache;

    @Override
    public List<T> snapshot() {
        List<T> cached = snapshotCache;
        if (cached != null) {
            return cached;
        }
        synchronized (lock) {
            if (snapshotCache == null) {
                snapshotCache = newSnapshot();
            }
            return snapshotCache;
        }
    }

    /*
     * Collections.unmodifiableList and not List.copyOf: the JavaFX list this
     * replaces carried null entries, and List.copyOf rejects them. A NullPointerException
     * raised here would be swallowed by the listener guard and the mirror would stop
     * updating without a word.
     */
    private List<T> newSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    @Override
    public int size() {
        synchronized (lock) {
            return items.size();
        }
    }

    public void add(T item) {
        mutate(list -> list.add(item));
    }

    public void addAll(Collection<? extends T> newItems) {
        mutate(list -> list.addAll(newItems));
    }

    public void remove(T item) {
        mutate(list -> list.remove(item));
    }

    public void setAll(Collection<? extends T> newItems) {
        mutate(list -> {
            list.clear();
            list.addAll(newItems);
        });
    }

    public void clear() {
        mutate(List::clear);
    }

    /**
     * Applies several changes and notifies listeners once. This is what keeps
     * a batch of several thousand chat members from producing a snapshot and a
     * listener round per element.
     */
    public void mutate(Consumer<List<T>> change) {
        Objects.requireNonNull(change, "change");
        List<T> snapshot;
        synchronized (lock) {
            change.accept(items);
            snapshot = newSnapshot();
            snapshotCache = snapshot;
        }
        notifyListeners(snapshot);
    }

    private void notifyListeners(List<T> snapshot) {
        for (Consumer<List<T>> listener : listeners) {
            try {
                listener.accept(snapshot);
            } catch (RuntimeException e) {
                // A broken listener must not keep the remaining ones from running,
                // and must not terminate the message-processing thread.
                System.err.println("[observe] roster listener failed: " + e);
            }
        }
    }

    @Override
    public void addListener(Consumer<List<T>> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeListener(Consumer<List<T>> listener) {
        listeners.remove(listener);
    }
}
