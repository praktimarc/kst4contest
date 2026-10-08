package kst4contest.observe;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleRosterTest {

    @Test
    void snapshotReflectsTheCurrentContent() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");
        roster.add("DO5AMF");

        assertEquals(List.of("DN9APW", "DO5AMF"), roster.snapshot());
        assertEquals(2, roster.size());
    }

    @Test
    void theSnapshotIsImmutable() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");

        List<String> snapshot = roster.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("DL1ABC"));
    }

    @Test
    void theSnapshotDoesNotChangeWhenTheRosterDoes() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");
        List<String> taken = roster.snapshot();

        roster.add("DO5AMF");

        assertEquals(List.of("DN9APW"), taken, "a handed-out snapshot must stay as it was");
    }

    @Test
    void eachMutationNotifiesOnceWithTheNewContent() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        List<List<String>> seen = new ArrayList<>();
        roster.addListener(seen::add);

        roster.add("DN9APW");
        roster.add("DO5AMF");

        assertEquals(2, seen.size());
        assertEquals(List.of("DN9APW"), seen.get(0));
        assertEquals(List.of("DN9APW", "DO5AMF"), seen.get(1));
    }

    @Test
    void mutateNotifiesOnceForTheWholeBatch() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        AtomicInteger calls = new AtomicInteger();
        roster.addListener(list -> calls.incrementAndGet());

        roster.mutate(list -> {
            for (int i = 0; i < 500; i++) {
                list.add("CALL" + i);
            }
        });

        assertEquals(1, calls.get(), "a batch must wake listeners once, not per element");
        assertEquals(500, roster.size());
    }

    @Test
    void setAllReplacesTheContentAndNotifiesOnce() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("OLD");
        AtomicInteger calls = new AtomicInteger();
        roster.addListener(list -> calls.incrementAndGet());

        roster.setAll(List.of("DN9APW", "DO5AMF"));

        assertEquals(1, calls.get());
        assertEquals(List.of("DN9APW", "DO5AMF"), roster.snapshot());
    }

    @Test
    void aListenerThrowingDoesNotStopTheOthers() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        List<List<String>> seen = new ArrayList<>();
        roster.addListener(list -> { throw new IllegalStateException("boom"); });
        roster.addListener(seen::add);

        roster.add("DN9APW");

        assertEquals(1, seen.size());
    }

    @Test
    void concurrentWritersDoNotLoseEntries() throws Exception {
        SimpleRoster<Integer> roster = new SimpleRoster<>();
        int threads = 8;
        int perThread = 250;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int base = t * perThread;
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        roster.add(base + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS), "writer threads did not finish");

        assertEquals(threads * perThread, roster.size());
    }

    @Test
    void repeatedSnapshotsShareOneCopyUntilTheRosterChanges() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");

        List<String> first = roster.snapshot();
        List<String> second = roster.snapshot();
        assertSame(first, second,
                "four message tabs read the snapshot per batch; copying it each time is wasted work");

        roster.add("DO5AMF");
        assertNotSame(first, roster.snapshot(), "a change must produce a fresh snapshot");
    }

    @Test
    void nullEntriesSurviveASnapshot() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add(null);
        roster.add("DN9APW");

        List<String> snapshot = roster.snapshot();
        assertEquals(2, snapshot.size());
        assertNull(snapshot.get(0),
                "the JavaFX list this replaces carried nulls; throwing here would freeze the mirror silently");
    }

    @Test
    void isEmptyReportsTheContent() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        assertTrue(roster.isEmpty());
        roster.add("DN9APW");
        assertFalse(roster.isEmpty());
    }
}
