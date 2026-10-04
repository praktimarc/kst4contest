package kst4contest.observe;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleValueTest {

    @Test
    void anUnsetValueIsNullRatherThanEmpty() {
        SimpleValue<String> value = new SimpleValue<>();
        assertNull(value.get(), "an unset value must stay unavailable, not become an empty string");
    }

    @Test
    void getReturnsTheInitialValue() {
        assertEquals("144.300", new SimpleValue<>("144.300").get());
    }

    @Test
    void setNotifiesListenersWithTheNewValue() {
        SimpleValue<String> value = new SimpleValue<>();
        List<String> seen = new ArrayList<>();
        value.addListener(seen::add);

        value.set("432.200");

        assertEquals(List.of("432.200"), seen);
    }

    @Test
    void setToAnEqualValueDoesNotNotify() {
        SimpleValue<String> value = new SimpleValue<>("144.300");
        List<String> seen = new ArrayList<>();
        value.addListener(seen::add);

        value.set("144.300");

        assertTrue(seen.isEmpty(), "an unchanged value must not wake listeners");
    }

    @Test
    void settingBackToNullNotifiesAndReportsUnavailable() {
        SimpleValue<String> value = new SimpleValue<>("144.300");
        List<String> seen = new ArrayList<>();
        value.addListener(seen::add);

        value.set(null);

        assertEquals(1, seen.size());
        assertNull(seen.get(0));
        assertNull(value.get());
    }

    @Test
    void removedListenersStopBeingCalled() {
        SimpleValue<String> value = new SimpleValue<>();
        List<String> seen = new ArrayList<>();
        Consumer<String> listener = seen::add;
        value.addListener(listener);
        value.removeListener(listener);

        value.set("50.200");

        assertTrue(seen.isEmpty());
    }

    @Test
    void aListenerThrowingDoesNotStopTheOthers() {
        SimpleValue<String> value = new SimpleValue<>();
        List<String> seen = new ArrayList<>();
        value.addListener(v -> { throw new IllegalStateException("boom"); });
        value.addListener(seen::add);

        value.set("1296.200");

        assertEquals(List.of("1296.200"), seen,
                "one failing listener must not keep the others from being notified");
    }
}
