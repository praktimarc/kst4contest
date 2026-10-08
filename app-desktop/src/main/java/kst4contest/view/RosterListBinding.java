package kst4contest.view;

import kst4contest.observe.ObservableRoster;
import kst4contest.observe.UiDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Mirrors an {@link ObservableRoster} into a plain list the user interface can read.
 *
 * <p>The mirror instance never changes: callers hold on to the list they were handed, so
 * {@link #list()} returns the same unmodifiable view every time. {@link #dispose()}
 * unregisters the listener, which is what keeps a discarded runtime from being held alive
 * across a profile switch.</p>
 *
 * <p>Besides the plain one-to-one mirror there is a derived form: a supplier computes the
 * visible content (filtered, ordered) and one or more rosters act as the change sources.
 * That is what replaced the former {@code FilteredList}/{@code SortedList} chains, which
 * recomputed themselves whenever their source list changed.</p>
 *
 * <p>Named a binding and not a mirror on purpose: {@code kst4contest.view.compose.RosterMirror}
 * already exists and mirrors a {@code SimpleRoster} into Compose state. Two classes of the same
 * name doing the same job in sibling packages is a reader's trap.</p>
 *
 * <p>This was an {@code ObservableList} while a {@code TableView} was bound to it. The
 * Compose panes take a snapshot instead — {@code SelectedStationMessagesFeed.push} copies
 * what it is given — so the only thing the observable collection still provided was the
 * change notification, which is now {@link #onChanged(Consumer)}.</p>
 */
public final class RosterListBinding<T> {

    private final List<T> mirror = new ArrayList<>();

    /**
     * Handed out by {@link #list()}. A field and not a fresh wrapper per call, so the
     * instance a caller kept stays the instance it keeps seeing.
     */
    private final List<T> readOnlyView = Collections.unmodifiableList(mirror);

    private final List<Consumer<List<T>>> changeListeners = new ArrayList<>();
    private final List<Runnable> unsubscribes = new ArrayList<>();
    private final Supplier<List<T>> derivation;
    private final UiDispatcher dispatcher;
    private volatile boolean disposed;

    private RosterListBinding(Supplier<List<T>> derivation, UiDispatcher dispatcher) {
        this.derivation = derivation;
        this.dispatcher = dispatcher;
    }

    /** Mirrors the roster one to one. */
    public static <T> RosterListBinding<T> mirror(ObservableRoster<T> source, UiDispatcher dispatcher) {
        RosterListBinding<T> binding = new RosterListBinding<>(source::snapshot, dispatcher);
        binding.subscribe(source);
        binding.applyNow();
        return binding;
    }

    /**
     * Mirrors whatever {@code derivation} computes and recomputes it whenever one of the
     * given rosters changes.
     *
     * @param dispatcher hands the mirror update to the user-interface thread
     * @param derivation computes the visible content; called on the UI thread
     * @param sources    rosters whose changes trigger a recomputation
     */
    public static <T> RosterListBinding<T> derived(UiDispatcher dispatcher,
                                              Supplier<List<T>> derivation,
                                              ObservableRoster<?>... sources) {
        RosterListBinding<T> binding = new RosterListBinding<>(derivation, dispatcher);
        for (ObservableRoster<?> source : sources) {
            binding.subscribe(source);
        }
        binding.applyNow();
        return binding;
    }

    /**
     * Registers a listener for real content changes.
     *
     * <p>Replaces the {@code ListChangeListener} a {@code TableView} used to attach. It is
     * called on the user-interface thread, with an immutable snapshot, and only when the
     * content actually changed — see {@link #applyNow()}.</p>
     */
    public void onChanged(Consumer<List<T>> listener) {
        changeListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Recomputes the mirror. Needed when something other than a source roster changed the
     * result, for example a newly set filter predicate.
     */
    public void refresh() {
        if (disposed) {
            return;
        }
        dispatcher.runOnUi(this::applyNow);
    }

    /** The list to read. Always the same instance, and not modifiable by the caller. */
    public List<T> list() {
        return readOnlyView;
    }

    /** Unregisters from every source and empties the mirror. Idempotent. */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (Runnable unsubscribe : unsubscribes) {
            unsubscribe.run();
        }
        unsubscribes.clear();
        changeListeners.clear();
        mirror.clear();
    }

    private <S> void subscribe(ObservableRoster<S> source) {
        Consumer<List<S>> listener = snapshot -> refresh();
        source.addListener(listener);
        unsubscribes.add(() -> source.removeListener(listener));
    }

    private void applyNow() {
        if (disposed) {
            return;
        }
        List<T> computed = derivation.get();
        /*
         * An unchanged result must not notify. The selected-station message pane replaces
         * its rows on every notification, and the periodic station-list refresh would
         * otherwise fight the operator for the selected row on every tick.
         *
         * This comparison also breaks a cycle: a consumer that reacts by asking for a
         * refresh would land here again, and notifying unconditionally would ask it to
         * react again. Once the content is equal the chain stops.
         *
         * It rests on element identity. ChatMember declares equals(ChatMember) — an
         * overload, not an override of equals(Object) — so List.equals falls back to
         * reference comparison. Turning that overload into a real override would make two
         * different rows with the same callsign compare equal here, and a genuine change
         * could be dropped. See ChatMember.
         */
        if (mirror.equals(computed)) {
            return;
        }

        mirror.clear();
        mirror.addAll(computed);

        List<T> snapshot = List.copyOf(mirror);
        for (Consumer<List<T>> listener : changeListeners) {
            listener.accept(snapshot);
        }
    }
}
