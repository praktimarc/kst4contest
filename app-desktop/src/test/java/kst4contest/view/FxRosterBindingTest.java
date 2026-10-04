package kst4contest.view;

import javafx.collections.ObservableList;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleRoster;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxRosterBindingTest {

    @Test
    void theMirrorStartsWithTheCurrentContent() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");

        FxRosterBinding<String> binding = FxRosterBinding.mirror(roster, new DirectUiDispatcher());

        assertEquals(List.of("DN9APW"), List.copyOf(binding.list()));
    }

    @Test
    void theMirrorFollowsLaterChanges() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        FxRosterBinding<String> binding = FxRosterBinding.mirror(roster, new DirectUiDispatcher());

        roster.add("DN9APW");
        roster.add("DO5AMF");

        assertEquals(List.of("DN9APW", "DO5AMF"), List.copyOf(binding.list()));
    }

    @Test
    void theMirrorIsTheSameInstanceThroughout() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        FxRosterBinding<String> binding = FxRosterBinding.mirror(roster, new DirectUiDispatcher());
        ObservableList<String> list = binding.list();

        roster.add("DN9APW");

        assertTrue(list == binding.list(),
                "a TableView holds on to the instance it was given; it must not be replaced");
    }

    @Test
    void disposeStopsFollowingAndReleasesTheListener() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        FxRosterBinding<String> binding = FxRosterBinding.mirror(roster, new DirectUiDispatcher());

        binding.dispose();
        roster.add("DN9APW");

        assertTrue(binding.list().isEmpty(),
                "a disposed binding must not keep the old runtime alive across a profile switch");
    }

    @Test
    void anUnchangedResultProducesNoListChangeEvent() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");
        FxRosterBinding<String> binding = FxRosterBinding.mirror(roster, new DirectUiDispatcher());

        AtomicInteger changes = new AtomicInteger();
        binding.list().addListener((javafx.collections.ListChangeListener<String>) change -> changes.incrementAndGet());

        binding.refresh();

        assertEquals(0, changes.get(),
                "an unchanged station list must not make the TableView drop its selection");
    }

    @Test
    void aDerivedBindingAppliesTheDerivationAndFollowsItsSources() {
        SimpleRoster<String> members = new SimpleRoster<>();
        SimpleRoster<String> filters = new SimpleRoster<>();
        members.add("DO5AMF");
        members.add("DN9APW");

        FxRosterBinding<String> binding = FxRosterBinding.derived(
                new DirectUiDispatcher(),
                () -> members.snapshot().stream()
                        .filter(call -> filters.snapshot().isEmpty() || filters.snapshot().contains(call))
                        .sorted(Comparator.naturalOrder())
                        .toList(),
                members, filters);

        assertEquals(List.of("DN9APW", "DO5AMF"), List.copyOf(binding.list()));

        filters.add("DO5AMF");
        assertEquals(List.of("DO5AMF"), List.copyOf(binding.list()),
                "a change of a source roster must recompute the derived content");

        members.add("DL1ABC");
        assertEquals(List.of("DO5AMF"), List.copyOf(binding.list()));
    }

    @Test
    void refreshRecomputesWithoutASourceChange() {
        SimpleRoster<String> members = new SimpleRoster<>();
        members.add("DN9APW");
        members.add("DO5AMF");
        String[] wanted = { "DN9APW" };

        FxRosterBinding<String> binding = FxRosterBinding.derived(
                new DirectUiDispatcher(),
                () -> members.snapshot().stream().filter(call -> call.equals(wanted[0])).toList(),
                members);

        assertEquals(List.of("DN9APW"), List.copyOf(binding.list()));

        wanted[0] = "DO5AMF";
        binding.refresh();

        assertEquals(List.of("DO5AMF"), List.copyOf(binding.list()),
                "a newly set filter must be visible without waiting for the next roster change");
    }

    @Test
    void aDisposedDerivedBindingDetachesFromEverySource() {
        SimpleRoster<String> members = new SimpleRoster<>();
        SimpleRoster<String> filters = new SimpleRoster<>();

        FxRosterBinding<String> binding = FxRosterBinding.derived(
                new DirectUiDispatcher(), members::snapshot, members, filters);

        binding.dispose();
        members.add("DN9APW");
        filters.add("DN9APW");

        assertTrue(binding.list().isEmpty());
    }
}
