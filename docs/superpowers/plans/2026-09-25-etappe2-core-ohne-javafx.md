# Etappe 2 — `core` von JavaFX befreien: Umsetzungsplan

> **Für agentische Bearbeiter:** ERFORDERLICHE UNTER-SKILL: superpowers:subagent-driven-development (empfohlen) oder superpowers:executing-plans, um diesen Plan Aufgabe für Aufgabe umzusetzen. Schritte nutzen Checkbox-Syntax (`- [ ]`).

**Ziel:** `core` compiliert und testet ohne JavaFX auf dem Klassenpfad. Die JavaFX-Oberfläche läuft unverändert weiter und beweist, dass die neue Schnittstelle trägt.

**Architektur:** Ein neues Paket `kst4contest.observe` in `core` ersetzt die JavaFX-Beobachtungstypen durch drei bewusst minimale Abstraktionen: `ObservableValue`/`MutableValue` für Einzelwerte, `ObservableRoster` für Sammlungen (snapshotbasiert, ohne inkrementelles Änderungsprotokoll) und `UiDispatcher` als Ersatz für `Platform.runLater`. `app-desktop` stellt die JavaFX-Implementierung von `UiDispatcher` bereit und spiegelt jedes `ObservableRoster` über eine einzige Adapterklasse in eine `ObservableList`, an der die `TableView` hängen bleiben.

**Tech-Stack:** Java 21, Gradle 9.7.1, JUnit 5, Mockito. Keine neuen Abhängigkeiten.

**Spec:** `docs/superpowers/specs/2026-09-25-compose-migration-design.md`

## Drei Präzisierungen gegenüber dem Spec

1. **Audio gehört nicht mehr hierher.** Das Spec nennt für Etappe 2 die Verlagerung von `javafx.scene.media` nach `core/audio`. Nach der Modulaufteilung aus Etappe 1 liegt `javafx.scene.media` ausschließlich in `app-desktop/…/Kst4ContestApplication.java` (`:33`, `:34`, `:6599`, `:6611`); `core/…/utils/PlayAudioUtils.java` benutzt nur `jlayer`. Für das Ziel „`core` ohne JavaFX" ist hier nichts zu tun. Der Ersatz des `MediaPlayer` wandert zu Etappe 6, die JavaFX ganz entfernt.

2. **Zwei JavaFX-Klassen in `core` sind toter Code.** `utils/BoundedDequeObservableList.java` (168 Zeilen, `extends ObservableListBase`) und `utils/TestAudioPlayerUtils.java` (19 Zeilen, `extends Application`) werden von keiner anderen Datei referenziert. Sie werden gelöscht, nicht portiert. Die JavaFX-Importe in `controller/StartChat.java` (`:6`, `:7`) hängen nur an einer auskommentierten Zeile (`:26`).

3. **Die Kopplung ist kleiner als die Importliste.** 15 Dateien in `core` importieren JavaFX, aber elf davon nur für einen einzelnen `Platform.runLater`, eine `SimpleStringProperty` als Wertbehälter oder eine `ObservableList`, die wie eine gewöhnliche Liste benutzt wird. Der eigentliche Aufwand steckt in `ChatController`, `ScoreService` und `ChatPreferences`.

## Globale Randbedingungen

- Java 21, Gradle 9.7.1. Keine neuen Abhängigkeiten in `core`.
- **Kein Verhalten ändert sich.** Diese Etappe ist eine Umstellung von Typen, keine fachliche Änderung.
- Die Invarianten aus dem Spec-Abschnitt „Invarianten" gelten unverändert: Chat-Identität samt Kategorie, `NOT-QRV`-Vorrang, `null` bedeutet unbekannt, Protokoll-Framing, `preferences.xml` Version 7, Operator-Profile, SQLite-Schema.
- **Die Bündelung bleibt.** `flushPendingChatMessagesToUi`, das Coalescing der Mitgliederliste und jede vorhandene Entprellung werden übernommen, nicht vereinfacht. Sie sind Leistungsschutz bei mehreren tausend ON4KST-Nutzern.
- Die Regel aus `AGENTS.md` gilt wortgleich weiter, nur heißt die Grenze jetzt `UiDispatcher`: Worker-Threads mutieren keine UI-gebundenen Sammlungen; UI-sichtbare Änderungen laufen über den Dispatcher.
- Kommentare und Javadoc auf Englisch; Commit-Nachrichten knapp auf Englisch.
- `core/build.gradle.kts` verliert seinen `javafx`-Block erst in der letzten Aufgabe. Bis dahin compiliert `core` weiter mit JavaFX und die Umstellung bleibt schrittweise prüfbar.

## Prüfschwerpunkte

Fünf Fehlerklassen, die kein Übersetzungslauf zeigt. Jede bekommt ihren Test in der zuständigen Aufgabe.

1. **Die Stationsliste steht anders da als vorher.** `SortedList` fügte einzelne Elemente per Binärsuche ein und landete bei Gleichstand an undefinierter Position, ohne bereits sichtbare Zeilen zu bewegen; über viele Zu- und Abgänge driftete der Gleichstandsblock so von der Ankunftsreihenfolge weg. Eine Sortierung, die den Block bei jeder Änderung vollständig neu ableitet, liefert dort zwangsläufig eine andere — nämlich deterministische — Reihenfolge. Zu prüfen ist deshalb nicht Gleichheit mit vorher, sondern dass die neue Reihenfolge bei Gleichstand der Ankunftsreihenfolge folgt und bei einem Spaltenwechsel nicht die vorherige Spalte nachwirkt. → Aufgabe 10.
2. **Listener feuern auf dem falschen Thread.** Ein `ObservableRoster`, das seine Listener synchron im mutierenden Thread aufruft, bringt Worker-Threads direkt an die UI — genau das, was `AGENTS.md` verbietet. → Aufgaben 3 und 4.
3. **Die Bündelung geht verloren.** Wird pro Änderung ein Snapshot erzeugt und ein Listener gerufen, entsteht bei mehreren tausend Nutzern eine Flut. Das fällt erst unter Last auf, nicht im Test. → Aufgabe 11.
4. **`frequency` wird zu `""` statt `null`.** `StringProperty` liefert ohne gesetzten Wert `null` über `get()`. Ein `MutableValue<String>` mit Vorgabewert `""` würde aus „unbekannte Frequenz" eine leere, aber vorhandene machen — und verstößt gegen „`null` bedeutet nicht verfügbar". → Aufgabe 8.
5. **Listener überleben den Profilwechsel.** Der Laufzeit-Neuaufbau baut eine neue `Kst4ContestApplication`; Adapter, die sich nicht abmelden, halten die alte Instanz am Leben. Vor Etappe 1 waren genau solche Lecks vorhanden. → Aufgabe 11.

---

## Aufgabe 1: Toten JavaFX-Code entfernen

**Dateien:**
- Löschen: `core/src/main/java/kst4contest/utils/BoundedDequeObservableList.java`
- Löschen: `core/src/main/java/kst4contest/utils/TestAudioPlayerUtils.java`
- Ändern: `core/src/main/java/kst4contest/controller/StartChat.java`

**Schnittstellen:**
- Erzeugt: nichts. Reine Entfernung.

- [ ] **Schritt 1: Nachweisen, dass die Klassen ungenutzt sind**

Ausführen:
```bash
grep -rn "BoundedDequeObservableList\|TestAudioPlayerUtils" core/src app-desktop/src --include="*.java" | grep -v "utils/BoundedDequeObservableList.java\|utils/TestAudioPlayerUtils.java"
```
Erwartet: keine Ausgabe.

Gibt es Treffer, werden die Klassen doch benutzt. Dann diese Aufgabe überspringen, im Ledger als `Ruling:` festhalten und mit Aufgabe 2 weitermachen.

- [ ] **Schritt 2: Löschen**

```bash
git rm core/src/main/java/kst4contest/utils/BoundedDequeObservableList.java
git rm core/src/main/java/kst4contest/utils/TestAudioPlayerUtils.java
```

- [ ] **Schritt 3: Tote Importe in `StartChat` prüfen**

Ausführen: `grep -n "SimpleStringProperty\|ObservableStringValue" core/src/main/java/kst4contest/controller/StartChat.java`
Erwartet: drei Zeilen — zwei Importe und eine auskommentierte Verwendung.

Wird einer der Typen in nicht auskommentiertem Code benutzt, bleiben die Importe stehen und die Datei wird in Aufgabe 7 behandelt. Als `Ruling:` festhalten.

- [ ] **Schritt 4: Importe entfernen**

In `core/src/main/java/kst4contest/controller/StartChat.java` die beiden Zeilen

```java
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
```

löschen. Die auskommentierte Zeile bleibt unverändert stehen — sie ist Dokumentation der früheren Umsetzung.

- [ ] **Schritt 5: Übersetzen und testen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 208 Testfälle grün.

- [ ] **Schritt 6: Zählstand prüfen**

Ausführen: `grep -rl "javafx" core/src/main/java | wc -l`
Erwartet: `12` — von 15 bleiben zwölf Dateien.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Remove the unused JavaFX classes from core"
```

---

## Aufgabe 2: `ObservableValue` und `MutableValue`

**Dateien:**
- Anlegen: `core/src/main/java/kst4contest/observe/ObservableValue.java`
- Anlegen: `core/src/main/java/kst4contest/observe/MutableValue.java`
- Anlegen: `core/src/main/java/kst4contest/observe/SimpleValue.java`
- Test: `core/src/test/java/kst4contest/observe/SimpleValueTest.java`

**Schnittstellen:**
- Erzeugt: `ObservableValue<T>` mit `T get()`, `void addListener(Consumer<T>)`, `void removeListener(Consumer<T>)`; `MutableValue<T> extends ObservableValue<T>` mit `void set(T)`; `SimpleValue<T> implements MutableValue<T>` mit Konstruktoren `SimpleValue()` und `SimpleValue(T initial)`. Konsumiert von den Aufgaben 7, 8, 9 und 12.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

`core/src/test/java/kst4contest/observe/SimpleValueTest.java`:

```java
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
```

- [ ] **Schritt 2: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.observe.SimpleValueTest'`
Erwartet: FAIL — Übersetzungsfehler, `SimpleValue` existiert nicht.

- [ ] **Schritt 3: Die Schnittstellen schreiben**

`core/src/main/java/kst4contest/observe/ObservableValue.java`:

```java
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
```

`core/src/main/java/kst4contest/observe/MutableValue.java`:

```java
package kst4contest.observe;

/** An {@link ObservableValue} that can be written to. */
public interface MutableValue<T> extends ObservableValue<T> {

    /**
     * Replaces the value and notifies the listeners. Setting a value that
     * equals the current one notifies nobody.
     */
    void set(T value);
}
```

- [ ] **Schritt 4: Die Umsetzung schreiben**

`core/src/main/java/kst4contest/observe/SimpleValue.java`:

```java
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
```

- [ ] **Schritt 5: Test laufen lassen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.observe.SimpleValueTest'`
Erwartet: PASS, 7 Testfälle.

- [ ] **Schritt 6: Commit**

```bash
git add core/src/main/java/kst4contest/observe core/src/test/java/kst4contest/observe
git commit -m "Add the observable value abstraction to core"
```

---

## Aufgabe 3: `ObservableRoster`

Deckt Prüfschwerpunkt 2 ab.

**Dateien:**
- Anlegen: `core/src/main/java/kst4contest/observe/ObservableRoster.java`
- Anlegen: `core/src/main/java/kst4contest/observe/SimpleRoster.java`
- Test: `core/src/test/java/kst4contest/observe/SimpleRosterTest.java`

**Schnittstellen:**
- Konsumiert: nichts aus Aufgabe 2.
- Erzeugt: `ObservableRoster<T>` mit `List<T> snapshot()`, `int size()`, `void addListener(Consumer<List<T>>)`, `void removeListener(Consumer<List<T>>)`; `SimpleRoster<T> implements ObservableRoster<T>` mit zusätzlich `void add(T)`, `void addAll(Collection<? extends T>)`, `void remove(T)`, `void setAll(Collection<? extends T>)`, `void clear()` und `void mutate(Consumer<List<T>>)`. Konsumiert von den Aufgaben 6, 9, 11 und 12.

`mutate` ist der Sammelweg: es übergibt die interne Liste zur Bearbeitung und feuert danach **einen** Listener-Durchlauf. Damit bleibt die Bündelung aus `ChatController` erhalten, statt pro Element einen Snapshot zu erzeugen.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

`core/src/test/java/kst4contest/observe/SimpleRosterTest.java`:

```java
package kst4contest.observe;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
```

- [ ] **Schritt 2: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.observe.SimpleRosterTest'`
Erwartet: FAIL — `SimpleRoster` existiert nicht.

- [ ] **Schritt 3: Die Schnittstelle schreiben**

`core/src/main/java/kst4contest/observe/ObservableRoster.java`:

```java
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

    /** Registers a listener that is called with the new content after each change. */
    void addListener(Consumer<List<T>> listener);

    /** Removes a previously registered listener. Unknown listeners are ignored. */
    void removeListener(Consumer<List<T>> listener);
}
```

- [ ] **Schritt 4: Die Umsetzung schreiben**

`core/src/main/java/kst4contest/observe/SimpleRoster.java`:

```java
package kst4contest.observe;

import java.util.ArrayList;
import java.util.Collection;
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

    @Override
    public List<T> snapshot() {
        synchronized (lock) {
            return List.copyOf(items);
        }
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
            snapshot = List.copyOf(items);
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
```

- [ ] **Schritt 5: Test laufen lassen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.observe.SimpleRosterTest'`
Erwartet: PASS, 8 Testfälle.

- [ ] **Schritt 6: Commit**

```bash
git add core/src/main/java/kst4contest/observe core/src/test/java/kst4contest/observe
git commit -m "Add the observable roster abstraction to core"
```

---

## Aufgabe 4: `UiDispatcher`

Deckt Prüfschwerpunkt 2 ab.

**Dateien:**
- Anlegen: `core/src/main/java/kst4contest/observe/UiDispatcher.java`
- Anlegen: `core/src/main/java/kst4contest/observe/DirectUiDispatcher.java`
- Anlegen: `app-desktop/src/main/java/kst4contest/view/JavaFxUiDispatcher.java`
- Test: `core/src/test/java/kst4contest/observe/DirectUiDispatcherTest.java`

**Schnittstellen:**
- Erzeugt: `UiDispatcher` mit `void runOnUi(Runnable)` und `boolean isUiThread()`; `DirectUiDispatcher` (führt sofort im aufrufenden Thread aus, für Tests); `JavaFxUiDispatcher` in `app-desktop` (delegiert an `Platform.runLater` und `Platform.isFxApplicationThread`). Konsumiert von den Aufgaben 5, 11 und 12.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

`core/src/test/java/kst4contest/observe/DirectUiDispatcherTest.java`:

```java
package kst4contest.observe;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectUiDispatcherTest {

    @Test
    void runsTheTaskImmediately() {
        AtomicInteger runs = new AtomicInteger();
        new DirectUiDispatcher().runOnUi(runs::incrementAndGet);
        assertEquals(1, runs.get());
    }

    @Test
    void reportsEveryThreadAsTheUiThread() {
        assertTrue(new DirectUiDispatcher().isUiThread());
    }

    @Test
    void aFailingTaskDoesNotPropagateToTheCaller() {
        AtomicBoolean reached = new AtomicBoolean();
        new DirectUiDispatcher().runOnUi(() -> { throw new IllegalStateException("boom"); });
        reached.set(true);
        assertTrue(reached.get(), "a failing UI task must not terminate the calling thread");
    }
}
```

- [ ] **Schritt 2: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.observe.DirectUiDispatcherTest'`
Erwartet: FAIL — `DirectUiDispatcher` existiert nicht.

- [ ] **Schritt 3: Die Schnittstelle und die Testumsetzung schreiben**

`core/src/main/java/kst4contest/observe/UiDispatcher.java`:

```java
package kst4contest.observe;

/**
 * Hands work over to the thread that owns the user interface.
 *
 * <p>This is the boundary the project has always had, named explicitly: worker
 * threads never mutate user-interface state themselves, they hand it over.
 * Before this interface existed the boundary was {@code Platform.runLater};
 * the rule is unchanged, only the dependency is gone.
 */
public interface UiDispatcher {

    /** Runs the task on the user-interface thread, now or later. */
    void runOnUi(Runnable task);

    /** Whether the calling thread is the user-interface thread. */
    boolean isUiThread();
}
```

`core/src/main/java/kst4contest/observe/DirectUiDispatcher.java`:

```java
package kst4contest.observe;

/**
 * Runs every task on the calling thread. Intended for tests and for headless
 * use; it is not a user-interface thread in any real sense.
 */
public class DirectUiDispatcher implements UiDispatcher {

    @Override
    public void runOnUi(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            System.err.println("[observe] UI task failed: " + e);
        }
    }

    @Override
    public boolean isUiThread() {
        return true;
    }
}
```

- [ ] **Schritt 4: Test laufen lassen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.observe.DirectUiDispatcherTest'`
Erwartet: PASS, 3 Testfälle.

- [ ] **Schritt 5: Die JavaFX-Umsetzung schreiben**

`app-desktop/src/main/java/kst4contest/view/JavaFxUiDispatcher.java`:

```java
package kst4contest.view;

import javafx.application.Platform;
import kst4contest.observe.UiDispatcher;

/** The {@link UiDispatcher} backed by the JavaFX application thread. */
public class JavaFxUiDispatcher implements UiDispatcher {

    @Override
    public void runOnUi(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            runGuarded(task);
        } else {
            Platform.runLater(() -> runGuarded(task));
        }
    }

    @Override
    public boolean isUiThread() {
        return Platform.isFxApplicationThread();
    }

    private static void runGuarded(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            // Must not tear down the JavaFX application thread.
            System.err.println("[observe] UI task failed: " + e);
        }
    }
}
```

- [ ] **Schritt 6: Übersetzen und testen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 208 + 18 = 226 Testfälle grün.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Add the UI dispatcher boundary and its JavaFX implementation"
```

---

## Aufgabe 5: `Platform.runLater` durch `UiDispatcher` ersetzen

**Dateien:**
- Ändern: `core/src/main/java/kst4contest/controller/ReachabilityService.java` (`:380`, `:383`, `:586`, `:589`)
- Ändern: `core/src/main/java/kst4contest/controller/ReadUDPbyUCXMessageThread.java` (`:587`, `:590`)
- Ändern: `core/src/main/java/kst4contest/controller/ReadUDPByWintestThread.java` (`:571`)
- Ändern: `core/src/main/java/kst4contest/controller/SkedReminderService.java` (`:3`)
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

**Schnittstellen:**
- Konsumiert: `UiDispatcher`, `JavaFxUiDispatcher` aus Aufgabe 4.
- Erzeugt: die drei Klassen nehmen einen `UiDispatcher` im Konstruktor entgegen.

`SkedReminderService` importiert `Platform`, benutzt es aber an keiner Stelle — dort wird nur der Import entfernt.

- [ ] **Schritt 1: Die toten und echten Stellen bestätigen**

Ausführen:
```bash
grep -c "Platform\." core/src/main/java/kst4contest/controller/SkedReminderService.java
grep -n "Platform\." core/src/main/java/kst4contest/controller/ReachabilityService.java core/src/main/java/kst4contest/controller/ReadUDPbyUCXMessageThread.java core/src/main/java/kst4contest/controller/ReadUDPByWintestThread.java
```
Erwartet: `0` für `SkedReminderService`; sieben Zeilen für die übrigen drei.

- [ ] **Schritt 2: Toten Import entfernen**

In `SkedReminderService.java` die Zeile `import javafx.application.Platform;` löschen.

- [ ] **Schritt 3: Die drei Klassen auf `UiDispatcher` umstellen**

In jeder der drei Dateien:

1. `import javafx.application.Platform;` durch `import kst4contest.observe.UiDispatcher;` ersetzen.
2. Ein Feld `private final UiDispatcher uiDispatcher;` ergänzen und im Konstruktor setzen. Hat die Klasse mehrere Konstruktoren, bekommt jeder den Parameter; gibt es einen, der ohne auskommt, bekommt er `new DirectUiDispatcher()` als Vorgabe **nicht** — der Aufrufer muss entscheiden.
3. Jedes Muster

```java
if (Platform.isFxApplicationThread()) {
    task.run();
} else {
    Platform.runLater(task);
}
```

durch den einen Aufruf `uiDispatcher.runOnUi(task);` ersetzen. `JavaFxUiDispatcher.runOnUi` enthält genau diese Fallunterscheidung bereits.

4. Ein alleinstehendes `Platform.runLater(x)` wird zu `uiDispatcher.runOnUi(x)`.

- [ ] **Schritt 4: Die Erzeugungsstellen in der Oberfläche nachziehen**

Ausführen:
```bash
grep -n "new ReachabilityService\|new ReadUDPbyUCXMessageThread\|new ReadUDPByWintestThread" core/src app-desktop/src -r
```

Jeder Fundstelle den Dispatcher mitgeben. In `Kst4ContestApplication` einmalig ein Feld anlegen:

```java
private final UiDispatcher uiDispatcher = new JavaFxUiDispatcher();
```

und es an die Konstruktoren durchreichen.

- [ ] **Schritt 5: Übersetzen und testen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 226 Testfälle grün.

- [ ] **Schritt 6: Anwendung starten**

Ausführen: `./gradlew :app-desktop:run`
Erwartet: Hauptfenster erscheint. Verbindungsanzeige, Sked-Erinnerung und die UDP-Empfänger verhalten sich unverändert.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Route UI hand-offs in core through the UI dispatcher"
```

---

## Aufgabe 6: `ObservableList` als gewöhnliche Liste

**Dateien:**
- Ändern: `core/src/main/java/kst4contest/model/AirPlaneReflectionInfo.java` (`:3`, `:10`, `:44`, `:48`)
- Ändern: `core/src/main/java/kst4contest/controller/ReadUDPbyAirScoutMessageThread.java` (`:12`, `:13`, `:278`)

`risingAirplanes` wird nirgends beobachtet — es ist eine Ergebnisliste. Sie wird zu `List<AirPlane>`.

- [ ] **Schritt 1: Nachweisen, dass niemand darauf hört**

Ausführen: `grep -rn "getRisingAirplanes()\." core/src app-desktop/src`
Erwartet: nur Aufrufe wie `.size()`, `.get(…)`, `.isEmpty()`, `.stream()` — **kein** `.addListener(`.

Findet sich ein `addListener`, wird das Feld stattdessen ein `SimpleRoster<AirPlane>` aus Aufgabe 3. Als `Ruling:` festhalten.

- [ ] **Schritt 2: Umstellen**

In `AirPlaneReflectionInfo.java`: `import javafx.collections.ObservableList;` durch `import java.util.List;` ersetzen und die drei Vorkommen von `ObservableList<AirPlane>` zu `List<AirPlane>` ändern.

In `ReadUDPbyAirScoutMessageThread.java`: beide JavaFX-Importe entfernen, `import java.util.ArrayList;` und `import java.util.List;` ergänzen (falls nicht vorhanden), und `:278` ändern von

```java
ObservableList<AirPlane> airplaneList = FXCollections.observableArrayList();
```

zu

```java
List<AirPlane> airplaneList = new ArrayList<>();
```

- [ ] **Schritt 3: Übersetzen und testen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 226 Testfälle grün.

- [ ] **Schritt 4: Commit**

```bash
git add -A
git commit -m "Use a plain list for the AirScout reflection result"
```

---

## Aufgabe 7: `SimpleStringProperty` als Wertbehälter

**Dateien:**
- Ändern: `core/src/main/java/kst4contest/controller/Utils4KST.java` (`:3`, `:123`)

**Schnittstellen:**
- Konsumiert: `ObservableValue` aus Aufgabe 2.

`MessageBusManagementThread` (`:15`, `:1435`, `:1475`) wird **nicht** hier behandelt: seine beiden `new SimpleStringProperty(...)` sind Argumente für `ChatMember.setFrequency` und fallen mit Aufgabe 8.

- [ ] **Schritt 1: Die Verwendung ansehen**

Ausführen: `sed -n '115,135p' core/src/main/java/kst4contest/controller/Utils4KST.java`

Der Parameter `optionalPrefix` wird gelesen, nicht beobachtet.

- [ ] **Schritt 2: Umstellen**

`import javafx.beans.property.SimpleStringProperty;` durch `import kst4contest.observe.ObservableValue;` ersetzen und den Parametertyp von `SimpleStringProperty` auf `ObservableValue<String>` ändern. Lesende Zugriffe `optionalPrefix.get()` und `optionalPrefix.getValue()` werden beide zu `optionalPrefix.get()`.

- [ ] **Schritt 3: Aufrufer nachziehen**

Ausführen: `grep -rn "optionalPrefix\|notify_optionalFrequencyPrefix" core/src app-desktop/src | grep -v "Utils4KST.java"`

Jeder Aufrufer übergibt heute die `SimpleStringProperty` aus `ChatPreferences:296`. Diese wird in Aufgabe 9 zu einem `MutableValue<String>`, das `ObservableValue<String>` erfüllt. Bis dahin wird an der Aufrufstelle ein `new SimpleValue<>(prefs.getNotify_optionalFrequencyPrefix().get())` übergeben.

- [ ] **Schritt 4: Übersetzen und testen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 226 Testfälle grün.

- [ ] **Schritt 5: Commit**

```bash
git add -A
git commit -m "Take an observable value instead of a JavaFX property in Utils4KST"
```

---

## Aufgabe 8: `ChatMember.frequency`

Deckt Prüfschwerpunkt 4 ab.

**Dateien:**
- Ändern: `core/src/main/java/kst4contest/model/ChatMember.java` (`:9`-`:12`, `:42`, `:510`, `:516`, `:616`)
- Ändern: `core/src/main/java/kst4contest/controller/MessageBusManagementThread.java` (`:15`, `:1435`, `:1475`)
- Ändern: alle weiteren Aufrufstellen von `getFrequency()` / `setFrequency(…)`
- Test: `core/src/test/java/kst4contest/model/ChatMemberFrequencyTest.java`

**Schnittstellen:**
- Konsumiert: `MutableValue`, `SimpleValue` aus Aufgabe 2.
- Erzeugt: `ChatMember.getFrequency()` liefert `MutableValue<String>`.

Es gibt 31 Aufrufstellen, davon sieben lesende (`.get()` zweimal, `.getValue()` fünfmal). `SimpleValue.get()` ersetzt beide.

- [ ] **Schritt 1: Den Test schreiben, der das heutige Verhalten festschreibt**

`core/src/test/java/kst4contest/model/ChatMemberFrequencyTest.java`:

```java
package kst4contest.model;

import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMemberFrequencyTest {

    @Test
    void aFreshMemberHasNoFrequency() {
        ChatMember member = new ChatMember();
        assertNotNull(member.getFrequency(), "the holder itself must exist");
        assertNull(member.getFrequency().get(),
                "an unknown frequency must stay unavailable, never become an empty string or a default band");
    }

    @Test
    void theFrequencyCanBeSetAndReadBack() {
        ChatMember member = new ChatMember();
        member.setFrequency(new SimpleValue<>("144.300"));
        assertEquals("144.300", member.getFrequency().get());
    }

    @Test
    void clearingTheFrequencyReturnsToUnavailable() {
        ChatMember member = new ChatMember();
        member.setFrequency(new SimpleValue<>("144.300"));
        member.getFrequency().set(null);
        assertNull(member.getFrequency().get());
    }
}
```

- [ ] **Schritt 2: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.model.ChatMemberFrequencyTest'`
Erwartet: FAIL — `SimpleValue` passt nicht auf `StringProperty`.

- [ ] **Schritt 3: `ChatMember` umstellen**

Die vier JavaFX-Importe (`:9`-`:12`) durch

```java
import kst4contest.observe.MutableValue;
import kst4contest.observe.SimpleValue;
```

ersetzen. `:42` wird zu

```java
	MutableValue<String> frequency = new SimpleValue<>();
```

`getFrequency()` (`:510`) liefert `MutableValue<String>`, `setFrequency` (`:516`) nimmt `MutableValue<String>`, und die Neuanlage bei `:616` wird zu `frequency = new SimpleValue<>();`.

> `SimpleValue` ohne Argument setzt `null`, genau wie `new SimpleStringProperty()`. Das ist der Punkt aus Prüfschwerpunkt 4: ein Vorgabewert `""` würde aus „unbekannt" ein „bekannt und leer" machen.

Die Importe `SimpleBooleanProperty`/`BooleanProperty` (`:9`, `:10`) prüfen: werden sie noch benutzt?

Ausführen: `grep -n "BooleanProperty" core/src/main/java/kst4contest/model/ChatMember.java`
Erwartet: nur die auskommentierte Zeile `:22`. Dann entfallen beide Importe ersatzlos.

- [ ] **Schritt 4: Die Aufrufstellen nachziehen**

Ausführen: `grep -rn "etFrequency(" core/src app-desktop/src --include="*.java" | grep -v "model/ChatMember.java"`

In `MessageBusManagementThread` (`:1435`, `:1475`) wird `new SimpleStringProperty(x)` zu `new SimpleValue<>(x)` und der Import `:15` entsprechend getauscht. Jedes `.getValue()` auf dem Ergebnis von `getFrequency()` wird zu `.get()`.

- [ ] **Schritt 5: Tests laufen lassen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 229 Testfälle grün.

- [ ] **Schritt 6: Anwendung prüfen**

Ausführen: `./gradlew :app-desktop:run`

Im laufenden Programm die Stationsliste ansehen.
Erwartet: Die Spalte mit der Frequenz zeigt für Stationen ohne bekannte QRG **nichts**, nicht `0` und nicht `144`.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Hold the chat member frequency in an observable value"
```

---

## Aufgabe 9: `ChatPreferences`

**Dateien:**
- Ändern: `core/src/main/java/kst4contest/model/ChatPreferences.java` (`:26`, `:35`, `:36`, `:228`, `:266`, `:268`, `:296`, `:298`, `:307`, `:308`, `:324`, `:325` und die zugehörigen Zugriffsmethoden)
- Ändern: die Aufrufstellen in `app-desktop`
- Test: `core/src/test/java/kst4contest/model/ChatPreferencesObservableTest.java`

**Schnittstellen:**
- Konsumiert: `MutableValue`, `SimpleValue`, `SimpleRoster` aus den Aufgaben 2 und 3.
- Erzeugt: `actualQTF` wird `MutableValue<Double>`, `MYQRGFirstCat`/`MYQRGSecondCat`/`notify_optionalFrequencyPrefix`/`notify_DXCSrv_SpottersCallSign` werden `MutableValue<String>`, die vier Listen werden `SimpleRoster<String>`.

**Was unverändert bleibt:** Das `preferences.xml`-Format. Version 7, dieselben Elementnamen, dieselben Vorgabewerte. Diese Aufgabe ändert Typen im Speicher, nicht auf der Platte.

- [ ] **Schritt 1: Den Test schreiben, der die Vorgabewerte festschreibt**

`core/src/test/java/kst4contest/model/ChatPreferencesObservableTest.java`:

```java
package kst4contest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPreferencesObservableTest {

    @Test
    void theDefaultsMatchTheFormerJavaFxProperties() {
        ChatPreferences prefs = new ChatPreferences();

        assertEquals(360.0, prefs.getActualQTF().get(), 0.0001,
                "actualQTF defaulted to 360 as a SimpleDoubleProperty");
        assertEquals("144", prefs.getNotify_optionalFrequencyPrefix().get());
        assertEquals("DO5AMF", prefs.getNotify_DXCSrv_SpottersCallSign().get());
    }

    @Test
    void theQrgValuesStartUnavailable() {
        ChatPreferences prefs = new ChatPreferences();

        assertNull(prefs.getMYQRGFirstCat().get(),
                "an unset own QRG must stay unavailable rather than becoming a fixed band");
        assertNull(prefs.getMYQRGSecondCat().get());
    }

    @Test
    void theListSettingsStartEmpty() {
        ChatPreferences prefs = new ChatPreferences();

        assertTrue(prefs.getLstNotify_QSOSniffer_sniffedWordsList().snapshot().isEmpty());
        assertTrue(prefs.getLstNotify_QSOSniffer_sniffedPrefixLocList().snapshot().isEmpty());
        assertTrue(prefs.getLst_txtSnipList().snapshot().isEmpty());
        assertTrue(prefs.getLst_txtShortCutBtnList().snapshot().isEmpty());
    }
}
```

- [ ] **Schritt 2: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.model.ChatPreferencesObservableTest'`
Erwartet: FAIL — die Rückgabetypen passen nicht.

- [ ] **Schritt 3: Die Felder umstellen**

Die drei JavaFX-Importe (`:26`, `:35`, `:36`) durch

```java
import kst4contest.observe.MutableValue;
import kst4contest.observe.SimpleRoster;
import kst4contest.observe.SimpleValue;
```

ersetzen. Dann:

```java
	MutableValue<Double> actualQTF = new SimpleValue<>(360.0);
	MutableValue<String> MYQRGFirstCat = new SimpleValue<>();
	MutableValue<String> MYQRGSecondCat = new SimpleValue<>();
	MutableValue<String> notify_optionalFrequencyPrefix = new SimpleValue<>("144");
	MutableValue<String> notify_DXCSrv_SpottersCallSign = new SimpleValue<>("DO5AMF");
	SimpleRoster<String> lstNotify_QSOSniffer_sniffedWordsList = new SimpleRoster<>();
	SimpleRoster<String> lstNotify_QSOSniffer_sniffedPrefixLocList = new SimpleRoster<>();
	SimpleRoster<String> lst_txtSnipList = new SimpleRoster<>();
	SimpleRoster<String> lst_txtShortCutBtnList = new SimpleRoster<>();
```

Die Kommentare an den Feldern bleiben unverändert stehen.

> `actualQTF` war ein `DoubleProperty` mit primitivem `double`. Als `MutableValue<Double>` kann es `null` werden. Es darf nicht: der Vorgabewert 360 bleibt, und keine Stelle setzt `null`. Wer `getActualQTF().get()` auspackt, bekommt weiterhin einen Wert.

- [ ] **Schritt 4: Die Zugriffsmethoden nachziehen**

Ausführen: `grep -n "Property()\|getActualQTF\|getMYQRG\|getNotify_\|getLst_txt\|getLstNotify_" core/src/main/java/kst4contest/model/ChatPreferences.java`

Alle `…Property()`-Methoden im JavaFX-Stil entfallen; die `get…()`-Methoden liefern den `MutableValue` beziehungsweise den `SimpleRoster`. Die Setter, die heute eine `ObservableList<String>` entgegennehmen, nehmen jetzt eine `Collection<String>` und rufen `setAll(…)` auf dem vorhandenen Roster auf — **nicht** das Feld ersetzen, sonst verlieren angemeldete Listener ihre Quelle.

- [ ] **Schritt 5: Lese- und Schreibpfad von `preferences.xml` prüfen**

Ausführen: `grep -rn "actualQTF\|MYQRGFirstCat\|lst_txtSnipList" core/src/main/java/kst4contest/model/ChatPreferences.java | grep -iE "element|getTextContent|appendChild|setTextContent"`

Jede dieser Stellen liest oder schreibt den Wert. Sie werden auf `.get()` beziehungsweise `.set(…)` umgestellt. Die XML-Elementnamen und die Reihenfolge bleiben unangetastet.

- [ ] **Schritt 6: Die Oberfläche nachziehen**

Ausführen: `grep -rn "getActualQTF\|getMYQRG\|getNotify_optionalFrequencyPrefix\|getNotify_DXCSrv_SpottersCallSign\|getLst_txtSnipList\|getLst_txtShortCutBtnList\|getLstNotify_QSOSniffer" app-desktop/src`

Für einfache Lesezugriffe genügt `.get()`. Wo eine `ObservableList` an ein Bedienelement gebunden war (`ListView.setItems`, `ComboBox.setItems`), wird der Adapter aus Aufgabe 11 benutzt; bis dahin genügt `FXCollections.observableArrayList(roster.snapshot())` an der Bindungsstelle. Solche Stellen als `Ruling:` notieren, damit Aufgabe 11 sie findet.

- [ ] **Schritt 7: Tests laufen lassen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 232 Testfälle grün.

- [ ] **Schritt 8: Einstellungen im laufenden Programm prüfen**

Ausführen: `./gradlew :app-desktop:run`

Einstellungsfenster öffnen, unter **Messagehandling** einen Textbaustein ergänzen, **Save Settings** drücken, Programm beenden und neu starten.
Erwartet: Der Baustein ist noch da. `~/.praktiKST/preferences.xml` enthält ihn, und die Konfigurationsversion steht weiterhin auf 7.

- [ ] **Schritt 9: Commit**

```bash
git add -A
git commit -m "Hold the observable preferences in the core abstractions"
```

---

## Aufgabe 10: Stationsliste filtern und sortieren als reine Funktion

Deckt Prüfschwerpunkt 1 ab. Das ist der fachliche Kern dieser Etappe.

**Dateien:**
- Anlegen: `core/src/main/java/kst4contest/controller/ChatMemberListView.java`
- Test: `core/src/test/java/kst4contest/controller/ChatMemberListViewTest.java`

**Schnittstellen:**
- Erzeugt: `ChatMemberListView.compute(List<ChatMember> members, List<Predicate<ChatMember>> filters, Comparator<ChatMember> order)` liefert `List<ChatMember>` — unveränderlich. Konsumiert von Aufgabe 11.

Heute leisten das `FilteredList` (`ChatController:1766`) und `SortedList` (`:1767`). Beide sind JavaFX-Typen und deshalb nicht ohne laufendes Toolkit testbar. Die reine Funktion macht dieses Verhalten **erstmals prüfbar** — das ist der eigentliche Gewinn der Etappe.

**Verhalten, das erhalten bleiben muss:** `FilteredList` verknüpft mehrere Prädikate nicht selbst; `ChatController` setzt ein zusammengesetztes. Die neue Funktion verknüpft die Liste mit UND. Eine leere Prädikatliste lässt alles durch. Ein `null`-Komparator lässt die Reihenfolge unverändert.

**Bewusst geändertes Verhalten:** `SortedList` fügte per Binärsuche ein und liess bei Gleichstand die Position davon abhängen, in welcher Reihenfolge Stationen historisch eingetroffen und wieder gegangen waren. Die neue Funktion sortiert bei jeder Änderung die kanonische Liste vollständig neu, also stabil über der Ankunftsreihenfolge. Das ist deterministisch und reproduzierbar, wo die alte Position undefiniert war.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

`core/src/test/java/kst4contest/controller/ChatMemberListViewTest.java`:

```java
package kst4contest.controller;

import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatMemberListViewTest {

    private static ChatMember member(String call) {
        ChatMember m = new ChatMember();
        m.setCallSign(call);
        return m;
    }

    private static List<String> calls(List<ChatMember> members) {
        return members.stream().map(ChatMember::getCallSign).toList();
    }

    @Test
    void withoutFiltersOrOrderTheInputIsReturnedUnchanged() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"), member("DL1ABC"));

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(), null);

        assertEquals(List.of("DN9APW", "DO5AMF", "DL1ABC"), calls(result));
    }

    @Test
    void severalFiltersAreCombinedWithAnd() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"), member("DL1ABC"));
        Predicate<ChatMember> startsWithD = m -> m.getCallSign().startsWith("D");
        Predicate<ChatMember> endsWithF = m -> m.getCallSign().endsWith("F");

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(startsWithD, endsWithF), null);

        assertEquals(List.of("DO5AMF"), calls(result));
    }

    @Test
    void anEmptyFilterListLetsEverythingThrough() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"));

        assertEquals(2, ChatMemberListView.compute(input, List.of(), null).size());
    }

    @Test
    void theOrderIsStableForEqualRanks() {
        List<ChatMember> input = List.of(member("BBB"), member("AAA"), member("CCC"), member("AAA"));
        Comparator<ChatMember> allEqual = (a, b) -> 0;

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(), allEqual);

        assertEquals(List.of("BBB", "AAA", "CCC", "AAA"), calls(result),
                "equal ranks must keep their incoming order, the way SortedList did");
    }

    @Test
    void theComparatorDecidesTheOrder() {
        List<ChatMember> input = List.of(member("CCC"), member("AAA"), member("BBB"));

        List<ChatMember> result = ChatMemberListView.compute(
                input, List.of(), Comparator.comparing(ChatMember::getCallSign));

        assertEquals(List.of("AAA", "BBB", "CCC"), calls(result));
    }

    @Test
    void filteringHappensBeforeOrdering() {
        List<ChatMember> input = List.of(member("CCC"), member("AAA"), member("BBB"));
        Predicate<ChatMember> notB = m -> !m.getCallSign().equals("BBB");

        List<ChatMember> result = ChatMemberListView.compute(
                input, List.of(notB), Comparator.comparing(ChatMember::getCallSign));

        assertEquals(List.of("AAA", "CCC"), calls(result));
    }

    @Test
    void theResultIsImmutable() {
        List<ChatMember> result = ChatMemberListView.compute(List.of(member("DN9APW")), List.of(), null);

        assertThrows(UnsupportedOperationException.class, () -> result.add(member("DL1ABC")));
    }

    @Test
    void aFilterThrowingDoesNotDiscardTheWholeList() {
        List<ChatMember> input = List.of(member("DN9APW"), member("DO5AMF"));
        Predicate<ChatMember> broken = m -> { throw new IllegalStateException("boom"); };

        List<ChatMember> result = ChatMemberListView.compute(input, List.of(broken), null);

        assertEquals(List.of("DN9APW", "DO5AMF"), calls(result),
                "a broken filter must not empty the station list mid-contest");
    }
}
```

- [ ] **Schritt 2: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.controller.ChatMemberListViewTest'`
Erwartet: FAIL — `ChatMemberListView` existiert nicht.

- [ ] **Schritt 3: Die Umsetzung schreiben**

`core/src/main/java/kst4contest/controller/ChatMemberListView.java`:

```java
package kst4contest.controller;

import kst4contest.model.ChatMember;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Derives the visible station list from the canonical one.
 *
 * <p>This replaces the JavaFX {@code FilteredList}/{@code SortedList} pair that
 * used to sit in {@link ChatController}. Being a pure function, the filtering
 * and ordering rules are testable without a running toolkit for the first time.
 */
public final class ChatMemberListView {

    private ChatMemberListView() {
    }

    /**
     * @param members the canonical list; never modified
     * @param filters combined with AND; an empty list lets everything through
     * @param order   {@code null} keeps the incoming order; sorting is stable
     * @return an immutable result list
     */
    public static List<ChatMember> compute(List<ChatMember> members,
                                           List<Predicate<ChatMember>> filters,
                                           Comparator<ChatMember> order) {
        List<ChatMember> result = new ArrayList<>(members.size());
        for (ChatMember member : members) {
            if (matches(member, filters)) {
                result.add(member);
            }
        }
        if (order != null) {
            // List.sort is stable, which is what SortedList guaranteed.
            result.sort(order);
        }
        return List.copyOf(result);
    }

    private static boolean matches(ChatMember member, List<Predicate<ChatMember>> filters) {
        for (Predicate<ChatMember> filter : filters) {
            try {
                if (!filter.test(member)) {
                    return false;
                }
            } catch (RuntimeException e) {
                // A broken filter must not empty the station list during a contest.
                System.err.println("[station list] filter failed, keeping the member: " + e);
            }
        }
        return true;
    }
}
```

- [ ] **Schritt 4: Test laufen lassen**

Ausführen: `./gradlew :core:test --tests 'kst4contest.controller.ChatMemberListViewTest'`
Erwartet: PASS, 8 Testfälle.

- [ ] **Schritt 5: Commit**

```bash
git add core/src/main/java/kst4contest/controller/ChatMemberListView.java core/src/test/java/kst4contest/controller/ChatMemberListViewTest.java
git commit -m "Derive the visible station list with a pure function"
```

---

## Aufgabe 11: `ChatController` und der Adapter in der Oberfläche

Deckt die Prüfschwerpunkte 3 und 5 ab. Das ist die größte Aufgabe dieses Plans.

**Dateien:**
- Ändern: `core/src/main/java/kst4contest/controller/ChatController.java` (Importe `:14`-`:20`, Felder und rund 30 öffentliche Methoden)
- Ändern: `core/src/main/java/kst4contest/controller/MessageBusManagementThread.java` (`:16`, `:577`)
- Anlegen: `app-desktop/src/main/java/kst4contest/view/FxRosterBinding.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Test: `app-desktop/src/test/java/kst4contest/view/FxRosterBindingTest.java`

**Schnittstellen:**
- Konsumiert: `SimpleRoster`, `ObservableRoster` (Aufgabe 3), `UiDispatcher` (Aufgabe 4), `ChatMemberListView.compute` (Aufgabe 10).
- Erzeugt: `FxRosterBinding.mirror(ObservableRoster<T>, UiDispatcher)` liefert eine `ObservableList<T>`, die dem Roster folgt, plus `void dispose()` zum Abmelden.

Die Oberfläche greift heute an rund 15 Stellen auf diese Listen zu: fünfmal `.size()`, einmal `.stream()`, einmal `.addListener(`, dazu acht Rohübergaben an `TableView.setItems` und Hilfsmethoden.

- [ ] **Schritt 1: Den Test für den Adapter schreiben**

`app-desktop/src/test/java/kst4contest/view/FxRosterBindingTest.java`:

```java
package kst4contest.view;

import javafx.collections.ObservableList;
import kst4contest.observe.DirectUiDispatcher;
import kst4contest.observe.SimpleRoster;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
```

- [ ] **Schritt 2: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.FxRosterBindingTest'`
Erwartet: FAIL — `FxRosterBinding` existiert nicht.

- [ ] **Schritt 3: Den Adapter schreiben**

`app-desktop/src/main/java/kst4contest/view/FxRosterBinding.java`:

```java
package kst4contest.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kst4contest.observe.ObservableRoster;
import kst4contest.observe.UiDispatcher;

import java.util.List;
import java.util.function.Consumer;

/**
 * Mirrors an {@link ObservableRoster} into an {@link ObservableList} so the
 * existing {@code TableView} bindings keep working.
 *
 * <p>The mirror instance never changes: a {@code TableView} holds on to the
 * list it was handed. {@link #dispose()} unregisters the listener, which is
 * what keeps a discarded runtime from being held alive across a profile switch.
 */
public final class FxRosterBinding<T> {

    private final ObservableRoster<T> source;
    private final ObservableList<T> mirror;
    private final Consumer<List<T>> listener;
    private boolean disposed;

    private FxRosterBinding(ObservableRoster<T> source, UiDispatcher dispatcher) {
        this.source = source;
        this.mirror = FXCollections.observableArrayList(source.snapshot());
        this.listener = snapshot -> dispatcher.runOnUi(() -> {
            if (!disposed) {
                mirror.setAll(snapshot);
            }
        });
        source.addListener(listener);
    }

    public static <T> FxRosterBinding<T> mirror(ObservableRoster<T> source, UiDispatcher dispatcher) {
        return new FxRosterBinding<>(source, dispatcher);
    }

    /** The list to hand to a {@code TableView}. Always the same instance. */
    public ObservableList<T> list() {
        return mirror;
    }

    /** Unregisters from the source and empties the mirror. Idempotent. */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        source.removeListener(listener);
        mirror.clear();
    }
}
```

- [ ] **Schritt 4: Test laufen lassen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.FxRosterBindingTest'`
Erwartet: PASS, 4 Testfälle.

- [ ] **Schritt 5: Die Felder in `ChatController` umstellen**

Die sieben `ObservableList`-Felder werden `SimpleRoster`:

```java
	private final SimpleRoster<ContestSked> activeSkeds = new SimpleRoster<>();
	private final SimpleRoster<ChatMessage> lst_globalChatMessageList = new SimpleRoster<>();
	private final SimpleRoster<ChatMember> chatMemberList = new SimpleRoster<>();
	private final SimpleRoster<Predicate<ChatMember>> lst_chatMemberListFilterPredicates = new SimpleRoster<>();
	private final SimpleRoster<ClusterMessage> lst_clusterMemberList = new SimpleRoster<>();
	private final SimpleRoster<ChatMember> lst_DBBasedWkdCallSignList = new SimpleRoster<>();
	private final SimpleRoster<String> lstNotify_QSOSniffer_sniffedCallSignList = new SimpleRoster<>();
```

`SimpleRoster` ist selbst synchronisiert; die bisherige Hülle `FXCollections.synchronizedObservableList(chatMemberList)` entfällt, und `lst_chatMemberList` wird zum selben Feld wie `chatMemberList`.

Die `FilteredList`/`SortedList`-Felder entfallen ersatzlos. An ihre Stelle tritt:

```java
	/** The visible station list, derived on demand. */
	public List<ChatMember> visibleChatMembers() {
		return ChatMemberListView.compute(
				chatMemberList.snapshot(),
				lst_chatMemberListFilterPredicates.snapshot(),
				chatMemberOrder);
	}
```

wobei `chatMemberOrder` der Komparator ist, den `SortedList` heute gesetzt bekommt. Ermitteln mit:

Ausführen: `grep -rn "setComparator\|comparatorProperty" core/src app-desktop/src`

- [ ] **Schritt 6: Die Bündelung erhalten**

Ausführen: `grep -n "flushPending\|runLater\|coalesc\|pending" core/src/main/java/kst4contest/controller/ChatController.java`

Jede gefundene Sammel- oder Entprellstelle bleibt. Der Unterschied: statt am Ende `Platform.runLater(this::flushPendingChatMessagesToUi)` zu rufen, sammelt die Methode in eine lokale Liste und übergibt sie mit **einem** `roster.mutate(list -> { list.clear(); list.addAll(gesammelt); })`. Damit feuert ein Sammelvorgang genau einen Listener-Durchlauf.

> Prüfschwerpunkt 3: Wird hier pro Nachricht ein `roster.add(…)` gerufen, entsteht bei mehreren tausend Nutzern eine Snapshot-Flut. Das zeigt sich nicht im Test, sondern erst unter Last.

- [ ] **Schritt 7: Die öffentlichen Methoden umstellen**

Ausführen: `grep -nE "public .*(ObservableList|FilteredList|SortedList)" core/src/main/java/kst4contest/controller/ChatController.java`

Jede Rückgabe wird zu `ObservableRoster<…>`. Setter, die eine `ObservableList` entgegennahmen, nehmen eine `Collection<…>` und rufen `setAll(…)` auf dem vorhandenen Roster, statt das Feld zu ersetzen.

Die Methoden `getLst_toAllMessageList`, `getLst_toMeMessageList`, `getLst_toOtherMessageList` und `getLst_selectedCallSignInfofilteredMessageList` gaben `FilteredList` zurück, deren Prädikat von außen gesetzt wurde (`Kst4ContestApplication:1426`, `:1460`, `:1483`, `:1507`). Sie werden zu je einem Paar aus Setzer und Abfrage in `ChatController`, das mit dem gespeicherten Prädikat aus `lst_globalChatMessageList.snapshot()` filtert:

```java
	public void setToAllMessageFilter(Predicate<ChatMessage> filter) { this.toAllMessageFilter = filter; }
	public List<ChatMessage> toAllMessages() { return filtered(toAllMessageFilter); }

	public void setToMeMessageFilter(Predicate<ChatMessage> filter) { this.toMeMessageFilter = filter; }
	public List<ChatMessage> toMeMessages() { return filtered(toMeMessageFilter); }

	public void setToOtherMessageFilter(Predicate<ChatMessage> filter) { this.toOtherMessageFilter = filter; }
	public List<ChatMessage> toOtherMessages() { return filtered(toOtherMessageFilter); }

	public void setSelectedCallSignInfoFilter(Predicate<ChatMessage> filter) { this.selectedCallSignInfoFilter = filter; }
	public List<ChatMessage> selectedCallSignInfoMessages() { return filtered(selectedCallSignInfoFilter); }

	private List<ChatMessage> filtered(Predicate<ChatMessage> filter) {
		if (filter == null) {
			return lst_globalChatMessageList.snapshot();
		}
		return lst_globalChatMessageList.snapshot().stream().filter(filter).toList();
	}
```

Ein `null`-Prädikat lässt alles durch, genau wie `FilteredList.setPredicate(null)` — `ChatController:3057` verlässt sich darauf.

- [ ] **Schritt 8: Die Oberfläche nachziehen**

Ausführen: `grep -rn "getLst_chatMemberList\|getLst_chatMemberSortedFilteredList\|getLst_chatMemberListFiltered\|getLst_globalChatMessageList\|getLst_clusterMemberList\|getActiveSkeds\|getLstNotify_QSOSniffer_sniffedCallSignList" app-desktop/src`

In `Kst4ContestApplication` je ein Feld für die Spiegel anlegen und in `shutdownRuntime()` abräumen:

```java
	private final List<FxRosterBinding<?>> rosterBindings = new ArrayList<>();

	private <T> ObservableList<T> mirrorOf(ObservableRoster<T> roster) {
		FxRosterBinding<T> binding = FxRosterBinding.mirror(roster, uiDispatcher);
		rosterBindings.add(binding);
		return binding.list();
	}
```

und in `shutdownRuntime()`:

```java
		rosterBindings.forEach(FxRosterBinding::dispose);
		rosterBindings.clear();
```

> Prüfschwerpunkt 5: Ohne dieses Abräumen hält ein Spiegel die verworfene `Kst4ContestApplication` am Leben. Genau solche Lecks gab es vor den Aufräumarbeiten in `shutdownRuntime()` schon einmal.

`.size()` wird zu `.size()` auf dem Roster, `.stream()` zu `.snapshot().stream()`, `.addListener(…)` zu `roster.addListener(snapshot -> …)`. Rohübergaben an `TableView.setItems` werden zu `setItems(mirrorOf(roster))`.

Die Stationsliste bekommt `chatcontroller.visibleChatMembers()` statt der `SortedList`; nach jeder Änderung an Mitgliedern oder Filtern wird sie neu berechnet und über den Spiegel gesetzt.

- [ ] **Schritt 8b: `MessageBusManagementThread` von `ObservableList` lösen**

Die Methode `checkListForChatMemberIndexByCallSign` nimmt heute eine `ObservableList<ChatMember>` entgegen (`MessageBusManagementThread:577`). Sie liest die Liste nur.

Ausführen: `grep -n "ObservableList\|checkListForChatMemberIndexByCallSign" core/src/main/java/kst4contest/controller/MessageBusManagementThread.java`

Den Import `javafx.collections.ObservableList` entfernen, `import java.util.List;` ergänzen falls nicht vorhanden, und die Signatur ändern zu:

```java
	private int checkListForChatMemberIndexByCallSign(List<ChatMember> list, ChatMember lookForThis) {
```

Die Aufrufer übergeben künftig `roster.snapshot()`.

> Das ist die Stelle, an der `AGENTS.md` ausdrücklich verlangt, dass dieser Thread keine UI-gebundene Sammlung anfasst. Mit einem Snapshot statt einer lebenden Liste ist das strukturell erfüllt statt nur verabredet.

- [ ] **Schritt 9: Übersetzen und testen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 244 Testfälle grün (232 nach Aufgabe 9, plus 8 aus Aufgabe 10 und 4 aus Schritt 1).

- [ ] **Schritt 10: Verhalten im laufenden Programm prüfen**

Ausführen: `./gradlew :app-desktop:run`

Erwartet, der Reihe nach:
- Die Stationsliste füllt sich und steht in derselben Reihenfolge wie vor der Umstellung.
- Ein Filter in der Filterleiste verkleinert die Liste und hebt sie beim Zurücksetzen wieder auf.
- Die drei Chat-Reiter zeigen ihre jeweiligen Nachrichten getrennt.
- Ein Profilwechsel über das Einstellungsfenster funktioniert, und die Stationsliste des neuen Profils füllt sich.

- [ ] **Schritt 11: Commit**

```bash
git add -A
git commit -m "Hold the controller collections in observable rosters"
```

---

## Aufgabe 12: `ScoreService`

**Dateien:**
- Ändern: `core/src/main/java/kst4contest/controller/ScoreService.java` (Importe `:3`-`:9`, `:20`-`:22`; `:45`, `:62`, `:66`, `:74`, `:91`, `:100`, `:195`, `:352`)
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

**Schnittstellen:**
- Konsumiert: `SimpleRoster`, `SimpleValue`, `UiDispatcher`.
- Erzeugt: `getTopCandidatesFx()` heißt künftig `topCandidates()` und liefert `ObservableRoster<TopCandidate>`; `selectedCallPriorityScoreProperty()` wird `ObservableValue<Double> selectedCallPriorityScore()`; `uiPulseProperty()` wird `ObservableValue<Long> uiPulse()`; `selectedChatMemberProperty()` wird `ObservableValue<ChatMember> selectedChatMember()`.

Diese Klasse ist bereits auf die Trennung gebaut — es gibt ein Snapshot-Modell, und der Klassenkommentar hält „No per-member Platform.runLater flooding" fest. Diese Eigenschaft bleibt erhalten.

- [ ] **Schritt 1: Die heutige Bündelung ansehen**

Ausführen: `sed -n '85,110p;185,205p' core/src/main/java/kst4contest/controller/ScoreService.java`

Notieren, an welchen Stellen gebündelt wird. Diese Stellen werden übernommen, nicht vereinfacht.

- [ ] **Schritt 2: Umstellen**

Die zehn JavaFX-Importe durch

```java
import kst4contest.observe.MutableValue;
import kst4contest.observe.ObservableRoster;
import kst4contest.observe.ObservableValue;
import kst4contest.observe.SimpleRoster;
import kst4contest.observe.SimpleValue;
import kst4contest.observe.UiDispatcher;
```

ersetzen. `topCandidatesFx` wird `SimpleRoster<TopCandidate>`, die drei Properties werden `SimpleValue`. Jedes

```java
if (Platform.isFxApplicationThread()) { … } else { Platform.runLater(() -> …); }
```

wird zu `uiDispatcher.runOnUi(() -> …)`; der Dispatcher kommt über den Konstruktor.

- [ ] **Schritt 3: Die Oberfläche nachziehen**

Ausführen: `grep -rn "getTopCandidatesFx\|selectedCallPriorityScoreProperty\|uiPulseProperty\|selectedChatMemberProperty" app-desktop/src`

`getTopCandidatesFx()` wird zu `mirrorOf(scoreService.topCandidates())` aus Aufgabe 11. Bindungen wie `label.textProperty().bind(x.selectedCallPriorityScoreProperty())` werden zu einem Listener:

```java
scoreService.selectedCallPriorityScore().addListener(value ->
		uiDispatcher.runOnUi(() -> label.setText(formatScore(value))));
```

- [ ] **Schritt 4: Übersetzen und testen**

Ausführen: `./gradlew build`
Erwartet: BUILD SUCCESSFUL, 244 Testfälle grün. Diese Aufgabe fügt keine Tests hinzu; die Zahl muss unverändert bleiben.

- [ ] **Schritt 5: Prüfen**

Ausführen: `./gradlew :app-desktop:run`

Eine Station in der Liste auswählen.
Erwartet: Die Prioritätsanzeige und die Top-Prioritätenliste aktualisieren sich wie zuvor.

- [ ] **Schritt 6: Commit**

```bash
git add -A
git commit -m "Publish the score service state through the core abstractions"
```

---

## Aufgabe 13: JavaFX aus `core` entfernen

**Dateien:**
- Ändern: `core/build.gradle.kts`
- Ändern: `docs/PROJECT_CONTEXT.md`, `AGENTS.md`

- [ ] **Schritt 1: Nachweisen, dass kein JavaFX mehr referenziert wird**

Ausführen: `grep -rn "javafx" core/src`
Erwartet: keine Ausgabe.

Bleibt ein Treffer, ist eine Datei übersehen worden. Sie wird nach demselben Muster umgestellt; als `Ruling:` festhalten.

- [ ] **Schritt 2: Den JavaFX-Block entfernen**

In `core/build.gradle.kts` den `alias(libs.plugins.javafx)`-Eintrag im `plugins`-Block und den gesamten `javafx { … }`-Block löschen, samt des Kommentars, der auf Etappe 2 verweist.

- [ ] **Schritt 3: Der Härtetest**

Ausführen: `./gradlew clean build`
Erwartet: BUILD SUCCESSFUL. `core` übersetzt **ohne JavaFX auf dem Klassenpfad** — das ist das Ziel dieser Etappe.

Ausführen: `./gradlew :core:dependencies --configuration compileClasspath | grep -c javafx`
Erwartet: `0`.

- [ ] **Schritt 4: Die Anwendung vollständig prüfen**

Ausführen: `./gradlew :app-desktop:packageImage && ./app-desktop/build/jpackage/praktiKST/bin/praktiKST`

Erwartet, der Reihe nach:
- Hauptfenster erscheint, Stationsliste füllt sich.
- Die drei Chat-Reiter zeigen getrennte Nachrichten.
- Filterleiste verkleinert und erweitert die Stationsliste.
- Einstellungsfenster öffnet, alle zwölf Reiter sind bedienbar, **Save Settings** schreibt `preferences.xml` in Version 7.
- Das Kartenfenster zeigt Kacheln und Stationsmarker.
- Ein Profilwechsel im laufenden Betrieb funktioniert ohne doppelte Listener.

- [ ] **Schritt 5: Dokumentation nachziehen**

In `docs/PROJECT_CONTEXT.md` unter „Current Architecture" ergänzen, dass `core` keine UI-Technologie mehr kennt und die Beobachtungsgrenze `kst4contest.observe` heißt. Den Abschnitt „JavaFX/threading" umbenennen in „Threading" und `Platform.runLater` durch `UiDispatcher` ersetzen; die Aussage, `ObservableList` sei eine UI-Projektion, wird zur Aussage über `ObservableRoster`.

In `AGENTS.md` im Abschnitt „Java and JavaFX architecture" dieselben Begriffe angleichen. Die inhaltliche Regel bleibt wortgleich: Worker-Threads mutieren keine UI-gebundenen Sammlungen.

- [ ] **Schritt 6: Commit**

```bash
git add -A
git commit -m "Drop the JavaFX dependency from core"
```

---

## Abnahme der Etappe

Etappe 2 ist abgeschlossen, wenn alles davon zutrifft:

- [ ] `grep -rn "javafx" core/src` liefert keine Ausgabe.
- [ ] `./gradlew :core:dependencies --configuration compileClasspath | grep -c javafx` liefert `0`.
- [ ] `./gradlew clean build` läuft durch; alle Testfälle grün, darunter die neuen für `observe`, `ChatMemberListView`, `ChatMember.frequency`, `ChatPreferences` und `FxRosterBinding`.
- [ ] Das jpackage-Image startet und zeigt Stationsliste, Chat-Reiter, Filter, Einstellungsfenster, Karte und Profilwechsel unverändert.
- [ ] Die Stationsliste folgt bei gleichem Rang der Ankunftsreihenfolge, und ein Spaltenwechsel ordnet ranggleiche Zeilen nicht nach der vorher gewählten Spalte. (Nicht: „dieselbe Reihenfolge wie vorher" — `SortedList` hatte dort keine definierte Reihenfolge.)
- [ ] Eine Station ohne bekannte QRG zeigt weiterhin nichts an, nicht `0` und nicht `144`.
- [ ] `preferences.xml` bleibt in Version 7 und behält unbekannte Knoten.

---

# Stand 2026-09-25: angehalten nach Aufgabe 11

Aufgaben 1 bis 11 sind umgesetzt, Commit `9ca5c7bf`. `./gradlew clean build` grün,
**249 Testfälle, 0 Fehler**. Nichts gepusht. `core` enthält JavaFX nur noch in
`controller/ScoreService.java`. Nicht begonnen: Aufgabe 12 und 13.

Aufgabe 11 wurde von einem Subagenten umgesetzt und von einem zweiten, frischen
Prüfer nachgeprüft. Das vollständige Ledger liegt unter
`.superpowers/sdd/2026-09-25-etappe2-core-ohne-javafx/progress.md` — **git-ignoriert**,
also flüchtig. Was davon zählt, steht deshalb hier.

## Offen: ein Fehler zum Beheben

**Die Infofläche der gewählten Station wird bei jedem Chat-Zu- und -Abgang
zurückgesetzt.** `chatMemberListBinding` hängt direkt am kanonischen Roster; die
Schutzflagge `programmaticChatMemberSelectionChange`
(`Kst4ContestApplication.java:1718-1723`) umschliesst nur den ausdrücklichen
`refresh()`, nicht den vom Roster ausgelösten Weg. Der Auswahl-Listener (`:8124`)
läuft deshalb ungeschützt durch, `selectedCallSignFurtherInfoPane` wird neu gebaut
und die Radiofilter springen auf die Vorgabe (`:1657-1659`).

Auswahl, Index und Scrollposition bleiben erhalten — gemessen bei 3000 Zeilen und
50 `setAll`-Durchläufen, Scrollposition in alter und neuer Fassung identisch. Es ist
also kein Datenverlust, aber eine laufende Sichtstörung: der Arbeitsgang „Station
wählen, Infofläche lesen, `/cq` schicken" wird auf einem gut besuchten Server alle
paar Sekunden unterbrochen. Verstösst gegen `AGENTS.md`, „Do not change ... selection,
focus, sorting, tab choice or prefilled text as an incidental side effect".

**Vorgeschlagener Fix:** im Auswahl-Listener früh abbrechen, wenn
`isSameLogicalChatMember(selectedCallSignInfoStageChatMember, newSelectedMember)`.

Nebenbefund: die Schutzflagge ist für den Filterknopf-Pfad wirkungslos, weil der
Spiegel beim geschützten Aufruf bereits gleich ist und `applyNow` früh zurückkehrt.

## Korrektur: die Prämisse zur Sortierung war falsch

Dieser Plan behauptet an drei Stellen, `SortedList` habe bei Gleichstand die
Ankunftsreihenfolge erhalten. **Das stimmt nicht.** `SortedList` fügte einzelne
Elemente per Binärsuche ein und landete bei Gleichstand an undefinierter Position;
gemessen ergab sich `A3, A4, A2, A1`. Über viele Zu- und Abgänge driftete der
Gleichstandsblock von der Ankunftsreihenfolge weg — und genau das sah dem Operator
wie springende Zeilen aus.

`ChatMemberListView.compute` leitet den Block bei jeder Änderung vollständig aus der
kanonischen Liste ab, also immer in Ankunftsreihenfolge. Das ist deterministisch und
damit **besser** als vorher, aber eben nicht „dieselbe Reihenfolge wie vor der
Umstellung". Die `setSortPolicy`-Verdrahtung wurde gemessen und ist korrekt
(`sortOrderSize=1`, Sortierpfeil bleibt, zweiter Klick schlägt durch,
`getSortOrder().clear()` stellt die kanonische Ordnung her, keine Rekursion).

**Zu korrigieren ist der Text, nicht der Code**, an drei Stellen:
1. Prüfschwerpunkt 1 oben in dieser Datei
2. Aufgabe 10, Absatz „Verhalten, das erhalten bleiben muss" („sortiert **stabil**")
3. Abnahmekriterium „Die Stationsliste steht in derselben Reihenfolge wie vor der
   Umstellung, auch bei gleichem Rang"
4. Testkommentar in `ChatMemberListViewTest.theOrderIsStableForEqualRanks`
   („the way SortedList did")

Neue Formulierung: *bei Gleichstand gilt die Ankunftsreihenfolge; das ersetzt die
undefinierte Einfügeposition der `SortedList` bewusst.*

## Kleinere Befunde des Prüfers — abgearbeitet 2026-09-27 (Commit 121ddd5c)

- `List.copyOf` verträgt kein `null`; `runGuarded` würde eine NPE verschlucken und
  der Spiegel erstarrte still. Nachgeprüft: `null` kann die Mitgliederliste nicht
  erreichen, `addOrUpdateActiveChatMember` bricht bei `null`-Schlüssel ab.
- Bündelung nur schreibseitig: jeder Nachrichtenreiter filtert den ganzen Speicher
  neu, ~1,7 ms statt ~0,1 ms je Schub. Unsichtbar, aber gemessen die ~17-fache Arbeit.
- `Kst4ContestApplication.java:7745` sortiert mehrere tausend Mitglieder nur für eine
  Zahl im Statustext.
- `:2440` und `:2450` verdrahten die Ordnung doppelt; harmlos nur, weil der
  `equals`-Vergleich in `FxRosterBinding:112` die Rekursion bricht — Kommentar fehlt.
- `ChatMember.equals(ChatMember)` ist eine Überladung, keine Überschreibung; der
  Spiegelvergleich hängt daran, dass `List.equals` deshalb auf Identität fällt.
- `clearChatMemberListFilterPredicates` prüft `size() == 0`; `ObservableRoster` hat
  kein `isEmpty()`.

## Vom Prüfer ohne Beanstandung bestätigt

Bündelung (ein `mutate` je Schub in beiden Flush-Methoden und im Kategorie-Austausch,
kein Pro-Element-Pfad), Lebensdauer der Spiegel (20 Aufrufstellen; die wiederholt
laufende wird entsorgt und aus `rosterBindings` entfernt; `shutdownRuntime()` räumt
vollständig ab; kein Roster wird unter laufender Bindung ausgetauscht).
Selbst nachgeprüft: `filtered(null)` liefert den vollen Schnappschuss, die
`setPredicate(null)`-Semantik ist erhalten; Prädikate 18-mal hinzugefügt, 18-mal
entfernt.

## Nur von Hand prüfbar

1. Spaltenköpfe der Stationsliste durchklicken, besonders Spalten mit vielen
   Gleichständen (Worked, Kategorie, NOT-QRV). Sortierpfeil und Richtungswechsel.
2. Auswahlerhalt unter Last — durch die Messungen oben weitgehend entkräftet,
   aber nie mit der Maus erlebt.

---

# Nachtrag 2026-09-27: zwei Fehler, die erst der Lasttest zeigte

Geprüft mit `src/kstsimulator.py` (777 Stationen in Kategorie 2), `LOGIN_LOGOUT_INTERVAL`
vorübergehend von 60 auf 3 Sekunden gesetzt. Beide Fehler betrafen den Tastaturfokus
und waren durch Lesen nicht zu finden — drei Leseläufe von zwei Agenten und dem
Umsetzer sind daran vorbeigegangen.

## 1. Der Fokus wurde beim Tippen gestohlen (Regression aus Etappe 2)

`prepareCqTextForCallsign` endet in `requestFocus()` auf dem Sendefeld. Aufgerufen
wurde es, sobald das Auswahlmodell eine Änderung meldete — und ein Spiegelwechsel
meldet bei jedem Chat-Zu- und -Abgang `[null, dieselbeStation]`. Wer in `Radio2` oder
`QRB` tippte, verlor bei dieser Rate alle 3 Sekunden den Fokus.

Behoben: die `/cq`-Vorbereitung läuft nur noch, wenn die Station wirklich wechselt
oder der Operator selbst eine Zeile gewählt hat. Der Parameter `previousMember` ist
entfallen — der Vergleich dagegen war die Ursache, denn im Zwischenzustand ist er
`null` und sieht wie ein Wechsel aus.

**Der Fix vom Vormittag war unvollständig:** er sicherte nur den Neuaufbau der
Infofläche. Begründung damals: `forceOverwrite=false` schütze getippten Text. Das
stimmt — aber der Fokus ist eine zweite Sache, und `AGENTS.md` nennt ihn ausdrücklich
neben dem Text.

## 2. Ein Klick in die Userlist holte den Fokus nicht (Altbestand)

`requestFocus()` lief innerhalb der Mausereignis-Verarbeitung; die `TableView` holt
sich den Fokus danach im Rahmen ihrer Standardbehandlung und macht die Anforderung
still zunichte. Behoben durch `Platform.runLater`.

Der Fehler ist alt. Er war unsichtbar, weil der nächste Chat-Zugang den Fokus
Sekunden später ins Sendefeld zog — dasselbe Verhalten wie Fehler 1, nur zufällig in
die gewünschte Richtung. Fehler 1 abzuschalten machte ihn sichtbar.

**Wichtig für spätere Etappen:** dort steht bewusst `Platform.runLater` und nicht
`uiDispatcher.runOnUi`. Der Dispatcher führt die Aufgabe inline aus, wenn der Aufrufer
schon auf dem UI-Thread ist — hier wäre das genau der Fall und würde nichts
verschieben. Ein „Aufräumen" auf den Dispatcher bringt den Fehler zurück.

## Was der Lasttest sonst belegt hat

- Speicher stabil über eine Minute unter Volllast: 1220 → 1218 → 1223 MB. Kein
  Listener-Leck.
- Keine einzige `[observe]`-Meldung. Dort würde protokolliert, wenn ein Listener in
  den neuen Abstraktionen wirft — nichts wurde verschluckt.
- Verbindungsabbruch des Simulators mitten im Betrieb: sauber protokolliert,
  Wiederverbindung von selbst, kein abgestorbener Thread.
- Sortierung, Auswahlerhalt und der Fix an der Infofläche wurden vom Operator
  bedient und bestätigt. Damit sind die beiden Handgriffe aus dem Haltepunkt erledigt.


---

# Nachtrag 2026-09-27: die kleinen Befunde sind erledigt

Commit `121ddd5c`. 255 Testfälle, 0 Fehler; jpackage-Image gebaut und gestartet.

| Befund | Erledigt durch |
|---|---|
| Vier Reiter filtern je Schub den ganzen Speicher neu | `SimpleRoster` hält den Schnappschuss zwischen, bis sich der Inhalt ändert. Die Kopie entsteht einmal je Änderung statt einmal je Leser. |
| `List.copyOf` verträgt kein `null` | `SimpleRoster.snapshot()` und `ChatMemberListView.compute` nutzen `Collections.unmodifiableList`. Die JavaFX-Liste trug `null` mit; eine NPE wäre im Listener-Schutz verschluckt worden und der Spiegel hätte stillschweigend aufgehört zu folgen. |
| `visibleChatMembers().size()` sortiert für eine Zahl | Neu `ChatMemberListView.countMatching` und `ChatController.visibleChatMemberCount()`; der Statustext zählt, statt zu sortieren. |
| `size() == 0` statt `isEmpty()` | `ObservableRoster.isEmpty()` als Standardmethode. |
| Rekursionsbremse in `FxRosterBinding` unkommentiert | Der Kommentar nennt jetzt beides: das Unterdrücken unnötiger Listenereignisse **und** das Brechen der Kette Sortierpolitik → `refresh()` → `setAll` → Sortierung. |
| `ChatMember.equals` ist Überladung, keine Überschreibung | An beiden Stellen vermerkt. Wer die Signatur „repariert", lässt zwei verschiedene Zeilen mit demselben Rufzeichen gleich erscheinen — dann kann `FxRosterBinding` eine echte Änderung verwerfen. |
| Voll qualifizierte Typen statt Importe | `ObservableRoster` ist jetzt importiert. `kst4contest.observe.ObservableValue` **bleibt** qualifiziert, mit Begründung im Code: `javafx.beans.value.ObservableValue` besitzt in dieser Klasse den einfachen Namen. |

Sechs neue Tests: zwischengespeicherter Schnappschuss, `null` im Roster, `isEmpty`,
`countMatching` gegen `compute`, ein werfendes Prädikat beim Zählen, `null` in
`compute`.
