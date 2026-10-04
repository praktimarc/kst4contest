package kst4contest.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kst4contest.observe.ObservableRoster;
import kst4contest.observe.UiDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Mirrors an {@link ObservableRoster} into an {@link ObservableList} so the
 * existing {@code TableView} bindings keep working.
 *
 * <p>The mirror instance never changes: a {@code TableView} holds on to the
 * list it was handed. {@link #dispose()} unregisters the listener, which is
 * what keeps a discarded runtime from being held alive across a profile switch.
 *
 * <p>Besides the plain one-to-one mirror there is a derived form: a supplier
 * computes the visible content (filtered, ordered) and one or more rosters act
 * as the change sources. That is what replaces the former
 * {@code FilteredList}/{@code SortedList} chains, which recomputed themselves
 * whenever their source list changed.
 */
public final class FxRosterBinding<T> {

    private final ObservableList<T> mirror = FXCollections.observableArrayList();
    private final List<Runnable> unsubscribes = new ArrayList<>();
    private final Supplier<List<T>> derivation;
    private final UiDispatcher dispatcher;
    private volatile boolean disposed;

    private FxRosterBinding(Supplier<List<T>> derivation, UiDispatcher dispatcher) {
        this.derivation = derivation;
        this.dispatcher = dispatcher;
    }

    /** Mirrors the roster one to one. */
    public static <T> FxRosterBinding<T> mirror(ObservableRoster<T> source, UiDispatcher dispatcher) {
        FxRosterBinding<T> binding = new FxRosterBinding<>(source::snapshot, dispatcher);
        binding.subscribe(source);
        binding.applyNow();
        return binding;
    }

    /**
     * Mirrors whatever {@code derivation} computes and recomputes it whenever one
     * of the given rosters changes.
     *
     * @param dispatcher hands the mirror update to the user-interface thread
     * @param derivation computes the visible content; called on the UI thread
     * @param sources    rosters whose changes trigger a recomputation
     */
    public static <T> FxRosterBinding<T> derived(UiDispatcher dispatcher,
                                                 Supplier<List<T>> derivation,
                                                 ObservableRoster<?>... sources) {
        FxRosterBinding<T> binding = new FxRosterBinding<>(derivation, dispatcher);
        for (ObservableRoster<?> source : sources) {
            binding.subscribe(source);
        }
        binding.applyNow();
        return binding;
    }

    /**
     * Recomputes the mirror. Needed when something other than a source roster
     * changed the result, for example a newly set filter predicate.
     */
    public void refresh() {
        if (disposed) {
            return;
        }
        dispatcher.runOnUi(this::applyNow);
    }

    /** The list to hand to a {@code TableView}. Always the same instance. */
    public ObservableList<T> list() {
        return mirror;
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
         * An unchanged result must not produce a list change event. A TableView
         * reacts to setAll with selection and scroll churn, and the periodic
         * station-list refresh would otherwise fight the operator for the
         * selected row on every tick.
         *
         * This comparison also breaks a cycle: the table's sort policy calls
         * refresh(), which lands here, and setAll would ask the table to sort
         * again. Once the content is equal the chain stops.
         *
         * It rests on element identity. ChatMember declares
         * equals(ChatMember) — an overload, not an override of
         * equals(Object) — so List.equals falls back to reference comparison.
         * Turning that overload into a real override would make two different
         * rows with the same callsign compare equal here, and a genuine change
         * could be dropped. See ChatMember.
         */
        if (mirror.equals(computed)) {
            return;
        }
        mirror.setAll(computed);
    }
}
