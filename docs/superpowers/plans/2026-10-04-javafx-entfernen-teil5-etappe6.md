# Umsetzungsplan — JavaFX entfernen, Teil 5: Etappe 6, die Reste und der Build

> **Für ausführende Agenten:** Schritte sind als Kästchen (`- [ ]`) geführt. Nach jeder Scheibe
> ein Commit und ein `./gradlew clean build` mit Auswertung der Test-XML. Nach den mit
> **GUI-ABNAHME** markierten Scheiben die Anwendung starten und von Marc bedienen lassen.

**Ziel:** `grep -rl "javafx\." core/src/main app-desktop/src/main` liefert nichts, und der
`org.openjfx`-Block ist aus dem Build und dem Versionskatalog verschwunden.

**Architektur:** Zwei Stellen benutzen JavaFX noch lebend und werden ersetzt, bevor gelöscht wird:
der 300-ms-Entpreller der Benutzerliste (`javafx.animation.PauseTransition` → `javax.swing.Timer`)
und `FxRosterBinding` (`ObservableList` → gewöhnliche Liste plus Rückruf). Alles übrige ist ein
Schatten, der nie in einer Szene hängt und nur sich selbst füttert; er fällt in Teilscheiben,
damit eine später gefundene Regression einer Scheibe zuzuordnen ist.

**Werkzeuge:** Java 21, Gradle Wrapper, Kotlin 2.2.0, Compose Multiplatform 1.8.2, JUnit 5.

**Spezifikation:** `docs/superpowers/specs/2026-10-01-javafx-entfernen-design.md`, Etappe 6.
**Lebendbefund:** `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`.
**Vorgänger:** `docs/superpowers/plans/2026-10-03-javafx-entfernen-teil4-dispatcher-lebenszyklus.md`.

## Projektweite Vorgaben

- Kommunikation mit Marc auf **Deutsch**; Quelltextkommentare und Javadoc **ausschließlich
  Englisch**. Commit-Nachrichten knappes Englisch.
- `./gradlew clean build`, Java 21. **Nicht aus Exit-Code 0 auf grün schließen** — die XML unter
  `*/build/test-results/test/TEST-*.xml` auswerten. Grundlinie: **835 Tests, 0 Fehler**.
- **Signierte Commits.** Schlägt das Signieren mit `Couldn't get agent socket` fehl, ist der
  Bitwarden-SSH-Agent gesperrt — Marc entsperren lassen, **nicht** unsigniert committen.
- **Immer mit Pfadangabe committen** (`git commit -- <pfade>`). Dieser Arbeitsbaum trägt
  vorbestehende gestapelte und ungestapelte Änderungen von vor der Sitzung; ein nackter
  `git commit` nimmt sie mit.
- Kein Push, kein Merge, kein Tag, kein Release, kein Versions-Bump. Keine neue Abhängigkeit.
- **Nicht anfassen:** `selectedCallSignInfoStageChatMember` (gewöhnliches Feld, von
  `ComposeChatInputActions.panelSelection()` gelesen), die 91 MP3-Dateien, `PlayAudioUtils`,
  `UiDispatcher`/`AwtUiDispatcher`/`DirectUiDispatcher`, `macOsConnectionStateMenuTitle` samt Test,
  `JavaFxStylesheet.kt` samt eingefrorener `derive`-Tabelle (der Name beschreibt die Herkunft der
  Farben und bleibt), `StationMapClusterer`/`StationMapStatusText` in `core`, und die
  **Compose**-Dateien `compose/TimelineView.kt` und `compose/map/PathProfileChart.kt`.

## Die Namensfalle, die diesen Plan sonst zerlegt

Vier Namen existieren **zweimal** — einmal JavaFX, einmal Compose. Gelöscht wird nur die
JavaFX-Fassung. Wer nach dem bloßen Namen greift, löscht die lebende:

| Name | **löschen** | **behalten** |
|---|---|---|
`TimelineView` | `app-desktop/src/main/java/kst4contest/view/TimelineView.java` | `app-desktop/src/main/kotlin/kst4contest/view/compose/TimelineView.kt` |
`PathProfileChart` | `app-desktop/src/main/java/kst4contest/view/map/PathProfileChart.java` | `app-desktop/src/main/kotlin/kst4contest/view/compose/map/PathProfileChart.kt` |

Dazu: `MapHtmlResources` und `TileProxyServer` existieren nur einmal. Die Treffer für
`MapHtmlResources` und `StationMapView` in `core/src/test` und in `StationMapWindow.kt` sind
**Kommentare**, die beschreiben, woher eine Formulierung stammt — geprüft, nicht angenommen. Sie
bleiben stehen; sie dokumentieren Herkunft.

## Prüfschwerpunkte

Fünf Dinge, die diese Etappe stillschweigend brechen kann. Jedes bekommt seinen Test in der
Scheibe, der der Code gehört.

1. **Der Entpreller muss nach dem Tausch noch entprellen.** `core` ruft `onUserListUpdated` bei
   jeder Listenänderung; ohne Zusammenfassung zeichnet die Stationstabelle bei Contest-Last
   dauernd neu. Fällt der Timer ganz aus, aktualisiert sie **nie** — und beides sieht im Build
   gleich aus. → S1.
2. **Der Nachrichtenbereich der ausgewählten Station muss weiter folgen.** `FxRosterBinding`
   speist ihn, und die Gleichheitsprüfung darin verhindert, dass eine unveränderte Liste ein
   Änderungsereignis auslöst. Fällt sie weg, kämpft die Tabelle bei jedem Takt mit dem Bediener um
   die ausgewählte Zeile. → S2.
3. **`tableSelection()` muss weiter `null` liefern, nicht werfen.** `ComposeChatInputActions`
   liest die gelöschte Tabelle; die lebende Sendezeile ruft das bei jedem Schnipsel und jeder
   Kurztaste. → S3a.
4. **Die Zeitstrahl-Kandidaten müssen Feld für Feld gleich bleiben.** Die zehn Werte werden heute
   1:1 umkopiert; eine vertauschte Reihenfolge im Konstruktor fällt nicht auf, weil fünf davon
   Zahlen sind. → S3d.
5. **Die Anwendung muss ohne gestartetes JavaFX-Toolkit hochkommen.** Die Startmethode fällt erst
   mit dem letzten Steuerelement; fällt sie zu früh oder zu spät, ist der Start kaputt
   beziehungsweise bleibt eine tote Abhängigkeit. → S6.

---

## S1 — Der Entpreller der Benutzerliste

**Dateien:**
- Anlegen: `app-desktop/src/main/java/kst4contest/view/CoalescingTrigger.java`
- Anlegen: `app-desktop/src/test/java/kst4contest/view/CoalescingTriggerTest.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
  (`onUserListUpdated`, das Feld `userListRefreshCoalescer`, `stopAnimation`, `shutdownRuntime`)

**Schnittstellen:**
- Liefert: `new CoalescingTrigger(int delayMs, Runnable action)` mit `void trigger()` und
  `void cancel()`. `trigger()` startet die Wartezeit neu; `action` läuft auf dem
  AWT-Ereignisfaden.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```java
package kst4contest.view;

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coalescer behind the station list.
 *
 * <p>Untested while it ran on a JavaFX {@code PauseTransition}, and both ways of being wrong
 * are invisible in a build: too eager and the table redraws on every message under contest
 * load, never and the table stops updating altogether.</p>
 */
class CoalescingTriggerTest {

    @Test
    void aBurstOfTriggersProducesOneRun() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch ran = new CountDownLatch(1);
        CoalescingTrigger trigger = new CoalescingTrigger(120, () -> {
            runs.incrementAndGet();
            ran.countDown();
        });

        // What a busy chat looks like: many updates, one intended redraw.
        for (int i = 0; i < 25; i++) {
            trigger.trigger();
        }

        assertTrue(ran.await(5, TimeUnit.SECONDS), "the burst never produced a run");
        Thread.sleep(300);
        assertEquals(1, runs.get(), "a burst must coalesce into a single run");
    }

    @Test
    void theActionRunsOnTheEventThread() throws Exception {
        AtomicBoolean onEventThread = new AtomicBoolean();
        CountDownLatch ran = new CountDownLatch(1);
        CoalescingTrigger trigger = new CoalescingTrigger(60, () -> {
            onEventThread.set(EventQueue.isDispatchThread());
            ran.countDown();
        });

        trigger.trigger();

        assertTrue(ran.await(5, TimeUnit.SECONDS));
        assertTrue(onEventThread.get(), "the action redraws tables and must be on the UI thread");
    }

    @Test
    void aLaterTriggerPostponesTheRun() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CoalescingTrigger trigger = new CoalescingTrigger(250, runs::incrementAndGet);

        trigger.trigger();
        Thread.sleep(150);
        trigger.trigger();
        Thread.sleep(150);

        // 300 ms have passed but the second trigger restarted the wait at 150 ms.
        assertEquals(0, runs.get(), "a new trigger must restart the delay, not let the old one fire");
    }

    @Test
    void cancellingBeforeTheDelayElapsesSuppressesTheRun() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CoalescingTrigger trigger = new CoalescingTrigger(120, runs::incrementAndGet);

        trigger.trigger();
        trigger.cancel();
        Thread.sleep(400);

        assertEquals(0, runs.get(), "a cancelled trigger must not run; shutdown relies on it");
    }

    @Test
    void triggeringAgainAfterACancelStillWorks() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch ran = new CountDownLatch(1);
        CoalescingTrigger trigger = new CoalescingTrigger(80, () -> {
            runs.incrementAndGet();
            ran.countDown();
        });

        trigger.trigger();
        trigger.cancel();
        trigger.trigger();

        assertTrue(ran.await(5, TimeUnit.SECONDS), "a cancel must not disable the trigger for good");
        assertEquals(1, runs.get());
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*CoalescingTriggerTest*'
```

Erwartung: Übersetzungsfehler, `CoalescingTrigger` existiert nicht.

- [ ] **Schritt 3: Die Umsetzung schreiben**

```java
package kst4contest.view;

import javax.swing.Timer;
import java.util.Objects;

/**
 * Collapses a burst of requests into one run, a fixed delay after the last of them.
 *
 * <p>Replaces the {@code javafx.animation.PauseTransition} the station list used. A
 * {@code javax.swing.Timer} fires on the AWT event thread, which is where the user
 * interface lives, and {@code restart()} means exactly what {@code playFromStart()} meant.
 * A PauseTransition would need a running JavaFX toolkit, which the application no longer
 * has.</p>
 */
public final class CoalescingTrigger {

    private final Timer timer;

    /**
     * @param delayMs how long to wait after the last request
     * @param action  runs on the user-interface thread once the delay elapses
     */
    public CoalescingTrigger(final int delayMs, final Runnable action) {
        Objects.requireNonNull(action, "action");
        this.timer = new Timer(delayMs, event -> action.run());
        this.timer.setRepeats(false);
    }

    /** Requests a run, restarting the delay. */
    public void trigger() {
        timer.restart();
    }

    /** Drops a pending run. Idempotent, and the trigger stays usable afterwards. */
    public void cancel() {
        timer.stop();
    }
}
```

- [ ] **Schritt 4: Tests laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*CoalescingTriggerTest*'
```

Erwartung: 5 Tests grün.

- [ ] **Schritt 5: `onUserListUpdated` umstellen**

Das Feld `private Animation userListRefreshCoalescer;` wird
`private CoalescingTrigger userListRefreshCoalescer;`. Die Methode wird:

```java
	@Override
	public void onUserListUpdated(String reason) {
		uiDispatcher.runOnUi(() -> {
			pendingUserListUpdateReason = reason;

			if (userListRefreshCoalescer == null) {
				userListRefreshCoalescer = new CoalescingTrigger(
						USER_LIST_REFRESH_DELAY_MS,
						() -> {
							forceChatMemberFilterRefresh();

							if (composeMainWindowState != null) {
								composeMainWindowState.getStations().forceRedraw();
							}

							refreshStationMapIfVisible();

							System.out.println(
									"KST4Capp, UI Update Trigger: " + pendingUserListUpdateReason);
						});
			}

			userListRefreshCoalescer.trigger();
		});
	}
```

Der `tbl_chatMember.refresh()`-Aufruf fällt hier mit: er zeichnet eine Tabelle neu, die keine
Zeilen hat, und das Feld fällt in S3b. Dazu die Konstante bei den übrigen Konstanten der Klasse:

```java
	/** The delay the JavaFX PauseTransition used, kept so the feel does not change. */
	private static final int USER_LIST_REFRESH_DELAY_MS = 300;
```

- [ ] **Schritt 6: `shutdownRuntime` und `stopAnimation` nachziehen**

In `shutdownRuntime` wird

```java
		stopAnimation(userListRefreshCoalescer);
		userListRefreshCoalescer = null;
```

zu

```java
		if (userListRefreshCoalescer != null) {
			userListRefreshCoalescer.cancel();
			userListRefreshCoalescer = null;
		}
```

`private static void stopAnimation(Animation)` verliert damit seinen letzten Aufrufer und wird
gelöscht. Prüfen mit `grep -n "stopAnimation" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
— sind weitere Aufrufer da, bleibt die Methode und nur dieser eine Aufruf wechselt.

- [ ] **Schritt 7: Bauen und Testzahl prüfen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
```

Erwartung: `tests=840 failures=0 errors=0`.

- [ ] **Schritt 8: Committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/CoalescingTrigger.java \
        app-desktop/src/test/java/kst4contest/view/CoalescingTriggerTest.java \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Coalesce station-list updates with a Swing timer instead of a PauseTransition" -- \
        app-desktop/src/main/java/kst4contest/view/CoalescingTrigger.java \
        app-desktop/src/test/java/kst4contest/view/CoalescingTriggerTest.java \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

---

## S2 — `FxRosterBinding` ohne JavaFX

Die Klasse ist lebend: `selectedCallSignInfoMessageBinding` speist den Nachrichtenbereich der
ausgewählten Station. `SelectedStationMessagesFeed.push` nimmt ein gewöhnliches
`List<ChatMessage>` und kopiert es sofort (`SelectedStationMessagesFeed.kt:23-26`), die
`ObservableList` wird also nie als beobachtbare Liste benutzt. Statt eines Listeners bekommt die
Klasse einen Rückruf.

**Dateien:**
- Umbenennen und ändern: `app-desktop/src/main/java/kst4contest/view/FxRosterBinding.java`
  → `app-desktop/src/main/java/kst4contest/view/RosterMirror.java`
- Umbenennen und ändern: `app-desktop/src/test/java/kst4contest/view/FxRosterBindingTest.java`
  → `app-desktop/src/test/java/kst4contest/view/RosterMirrorTest.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

**Schnittstellen:**
- Liefert: `RosterMirror.mirror(ObservableRoster<T>, UiDispatcher)` und
  `RosterMirror.derived(UiDispatcher, Supplier<List<T>>, ObservableRoster<?>...)`, dazu
  `List<T> list()`, `void refresh()`, `void dispose()` und neu
  `void onChanged(Consumer<List<T>> listener)`.

- [ ] **Schritt 1: Den bestehenden Test lesen, bevor etwas angefasst wird**

```bash
cat app-desktop/src/test/java/kst4contest/view/FxRosterBindingTest.java
```

Er deckt den Vertrag schon ab. Jede Zusicherung darin muss nach der Umstellung weiter gelten;
nur `ObservableList` wird zu `List`. Die Datei wird mit `git mv` umbenannt, damit die Geschichte
erhalten bleibt.

- [ ] **Schritt 2: Den bestehenden No-Event-Test umschreiben und einen zweiten ergänzen**

`ObservableRoster` ist eine **Schnittstelle**; die Tests benutzen `SimpleRoster` aus
`kst4contest.observe` als echte Umsetzung, nicht als Attrappe. Die bestehende Zusicherung
`anUnchangedResultProducesNoListChangeEvent` prüft heute über einen JavaFX-`ListChangeListener`.
Sie wird auf den Rückruf umgeschrieben — gleiche Aussage, neuer Weg:

```java
    @Test
    void anUnchangedResultNotifiesNobody() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");
        RosterMirror<String> mirror = RosterMirror.mirror(roster, new DirectUiDispatcher());

        AtomicInteger notifications = new AtomicInteger();
        mirror.onChanged(rows -> notifications.incrementAndGet());

        mirror.refresh();
        mirror.refresh();

        /*
         * The equality check is not an optimisation. The selected-station message pane
         * replaces its rows on every notification, and the periodic refresh would fight
         * the operator for the selected row on every tick.
         */
        assertEquals(0, notifications.get(),
                "an unchanged station list must not make the message pane drop its selection");
    }
```

Dazu neu, weil bisher nichts prüft, dass eine **echte** Änderung ankommt — der Fall, in dem der
Nachrichtenbereich stehen bliebe:

```java
    @Test
    void aRealChangeNotifiesOnceWithTheNewContent() {
        SimpleRoster<String> roster = new SimpleRoster<>();
        roster.add("DN9APW");
        RosterMirror<String> mirror = RosterMirror.mirror(roster, new DirectUiDispatcher());

        List<List<String>> seen = new java.util.ArrayList<>();
        mirror.onChanged(seen::add);

        roster.add("DO5AMF");

        assertEquals(1, seen.size(), "a real change must notify exactly once");
        assertEquals(List.of("DN9APW", "DO5AMF"), seen.get(0));
    }
```

Die übrigen fünf Zusicherungen der Datei bleiben inhaltlich gleich; nur
`ObservableList`/`List.copyOf(binding.list())` wird `List.copyOf(mirror.list())` und der
`javafx.collections`-Import fällt.

- [ ] **Schritt 3: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*RosterMirrorTest*'
```

Erwartung: Übersetzungsfehler, `RosterMirror` existiert nicht.

- [ ] **Schritt 4: Umbenennen und umstellen**

```bash
git mv app-desktop/src/main/java/kst4contest/view/FxRosterBinding.java \
       app-desktop/src/main/java/kst4contest/view/RosterMirror.java
git mv app-desktop/src/test/java/kst4contest/view/FxRosterBindingTest.java \
       app-desktop/src/test/java/kst4contest/view/RosterMirrorTest.java
```

In `RosterMirror.java`: die beiden `javafx.collections`-Importe fallen,
`private final ObservableList<T> mirror = FXCollections.observableArrayList();` wird
`private final List<T> mirror = new ArrayList<>();`, `list()` liefert
`Collections.unmodifiableList(mirror)`, und `applyNow()` endet statt mit `mirror.setAll(computed)`
mit:

```java
        if (mirror.equals(computed)) {
            return;
        }

        mirror.clear();
        mirror.addAll(computed);

        List<T> snapshot = List.copyOf(mirror);
        for (Consumer<List<T>> listener : changeListeners) {
            listener.accept(snapshot);
        }
```

Der lange Kommentar über der Gleichheitsprüfung **bleibt wörtlich erhalten** — er trägt die
Begründung und den Hinweis auf `ChatMember.equals(ChatMember)` als Überladung. Nur die Erwähnung
von `setAll` und `TableView` wird auf den Rückruf umformuliert. Dazu neu:

```java
    private final List<Consumer<List<T>>> changeListeners = new ArrayList<>();

    /**
     * Registers a listener for real content changes.
     *
     * <p>Replaces the {@code ListChangeListener} a {@code TableView} used to attach. It is
     * called on the user-interface thread with an immutable snapshot, and only when the
     * content actually changed.</p>
     */
    public void onChanged(Consumer<List<T>> listener) {
        changeListeners.add(Objects.requireNonNull(listener, "listener"));
    }
```

Die Klassen-Javadoc verliert ihre `TableView`- und `ObservableList`-Sätze und benennt stattdessen
die Compose-Senke.

- [ ] **Schritt 5: Die Aufrufstellen nachziehen**

In `Kst4ContestApplication`:
- `FxRosterBinding` → `RosterMirror` überall (Feld `rosterBindings`, `mirrorOf`, `derivedBinding`,
  `chatMemberListBinding`, `selectedCallSignInfoMessageBinding`, der `dispose`-Aufruf in
  `shutdownRuntime`).
- `private <T> javafx.collections.ObservableList<T> mirrorOf(...)` wird
  `private <T> java.util.List<T> mirrorOf(...)`.
- Der `ListChangeListener` bei `:1402` wird der Rückruf:

```java
		selectedCallSignInfoMessageBinding.onChanged(rows -> {
			if (selectedStationMessagesFeed != null) {
				selectedStationMessagesFeed.push(rows);
			}
		});
```

- Die drei verbleibenden `ObservableList`-Erwähnungen in `applySelectedCallSignInfoFilter`, bei
  `:3431` (`insertTextSnippet`) und `:1905` (`refreshShortcutButtons`) werden `List`. Beide
  letzteren fallen in S3 ganz; hier nur den Typ anpassen, damit es übersetzt.

- [ ] **Schritt 6: Bauen, Testzahl prüfen, committen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
git add -A app-desktop/src/main/java/kst4contest/view/ app-desktop/src/test/java/kst4contest/view/
git commit -m "Mirror rosters into a plain list with a callback instead of an ObservableList" -- \
        app-desktop/src/main/java/kst4contest/view/ app-desktop/src/test/java/kst4contest/view/
```

Erwartung: `tests=842 failures=0 errors=0`.

---

## S3 — Der Schatten, in vier Teilscheiben

Jede Teilscheibe für sich gebaut und committet. Reihenfolge ist nicht beliebig: S3a nimmt der
Sendezeile ihre JavaFX-Quelle, bevor S3b die Tabelle löscht, auf die sie zeigt.

### S3a — Die Sendezeile — **GUI-ABNAHME**

**Belegt:** `ChatInputState.kt:176-187` ist eine vollständige Nachbildung mit derselben
Überschreibschutz-Regel und eigenem `lastAutoPreparedSendText`; `ChatInputState.kt:190-191`
nimmt dieselbe Drei-Stufen-Auswahl. Die Zeile `// Sync with JavaFX state just in case` bei
`:4110` sagt, dass der JavaFX-Zweig nur mitgeschrieben wird.

**Dateien:** `Kst4ContestApplication.java`

- [ ] **Schritt 1: Den Test für die Auswahl nach dem Löschen schreiben**

`ComposeChatInputActions.tableSelection()` liest die Tabelle, die in S3b fällt. Nach dieser
Scheibe muss sie `null` liefern, nicht werfen. In
`app-desktop/src/test/java/kst4contest/view/ComposeChatInputActionsTest.java`:

```java
package kst4contest.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The selection bridge the live Compose send line asks on every snippet and shortcut.
 *
 * <p>The JavaFX station table it used to consult never had rows after the window was
 * disconnected, so this always answered null; it must keep doing that rather than throw
 * once the table is gone.</p>
 */
class ComposeChatInputActionsTest {

    @Test
    void thereIsNoTableSelectionAnyMore() {
        ComposeChatInputActions actions = new ComposeChatInputActions(new Kst4ContestApplication());

        assertNull(actions.tableSelection(), "the station table is gone; this must answer null");
    }

    @Test
    void anUnbuiltRuntimeHasNoPanelOrScoreSelection() {
        ComposeChatInputActions actions = new ComposeChatInputActions(new Kst4ContestApplication());

        assertNull(actions.panelSelection());
        assertNull(actions.scoreSelection(), "no chat controller yet, so no score selection");
    }
}
```

**Achtung:** `new Kst4ContestApplication()` erzeugt die Steuerelementfelder. Solange welche
übrig sind, braucht dieser Test ein gestartetes JavaFX-Toolkit und schlägt sonst mit
`Toolkit not initialized` fehl. Darum steht der Test hier und nicht früher: nach S3a ist
`txt_chatMessageUserInput` weg, und nach S3d ist kein Steuerelement mehr im Konstruktor. Schlägt
er in dieser Scheibe noch fehl, **nicht** das Toolkit im Test starten — den Test nach S3d
verschieben und das in der Commit-Nachricht festhalten.

- [ ] **Schritt 2: Laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*ComposeChatInputActionsTest*'
```

- [ ] **Schritt 3: Den JavaFX-Zweig der Sendezeile löschen**

Löschen: die Felder `txt_chatMessageUserInput`, `sendButton`, `lastAutoPreparedSendText`,
`lastAutoPreparedCqTargetCallsign`, `lastAutoPreparedCqTargetCategory`, und die Methoden
`prepareCqTextForCallsign` (beide Überladungen), `canOverwriteSendTextWithAutoPreparedText`,
`appendResolvedMessageText`, `insertTextSnippet`, `resolveSnippetIndex` sowie der
`EventHandler`-`handle`-Block um `:2309`, der nur diese Felder beschreibt.

Vor jeder Löschung die Aufrufer prüfen:

```bash
P=app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
for m in prepareCqTextForCallsign canOverwriteSendTextWithAutoPreparedText \
         appendResolvedMessageText insertTextSnippet resolveSnippetIndex; do
  echo "--- $m ---"; grep -rn "\b$m\b" app-desktop/src core/src --include="*.java" --include="*.kt"
done
```

Hat eine davon einen Aufrufer außerhalb der Löschliste, bleibt sie stehen und verliert nur ihren
JavaFX-Teil — die „gemischte Methode an ihrer Naht trennen", die der Lebendbefund als zentrale
Falle beschreibt. In `focusChatMemberAndPrepareCq` fällt nur der `prepareCqTextForCallsign`-Aufruf;
die Methode selbst ist lebend (`:924`, `:1584`) und setzt
`selectedCallSignInfoStageChatMember`, was bleiben muss. Bei `:4110` fällt die Zeile
`prepareCqTextForCallsign(receiverCallsign, message.getChatCategory(), false);` samt dem Kommentar
`// Sync with JavaFX state just in case`; `chatInput.prepareCq(...)` darüber bleibt.

- [ ] **Schritt 4: Bauen, Testzahl prüfen, GUI-ABNAHME**

```bash
./gradlew clean build && ./gradlew :app-desktop:run --args="--profile default"
```

Zu prüfen: Station auswählen → `/cq RUFZEICHEN ` erscheint vorbelegt im Eingabefeld und der Fokus
liegt darin; eigenen Text tippen, dann eine andere Station wählen → der getippte Text wird
**nicht** überschrieben; Schnipsel über Strg+1 bis Strg+0; die Kurztastenknöpfe; eine private
Nachricht senden.

- [ ] **Schritt 5: Committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/ app-desktop/src/test/java/kst4contest/view/
git commit -m "Delete the shadow send line; the Compose chat input is the only one left" -- \
        app-desktop/src/main/java/kst4contest/view/ app-desktop/src/test/java/kst4contest/view/
```

### S3b — Die Stationstabelle

**Belegt:** `onUserListUpdated` rief nur `refresh()` darauf, nie `setItems` — die Tabelle hat
keine Zeilen. `getEffectiveSelectedChatMember` hat sie als mittlere von drei Quellen, und dieser
Zweig kann nie feuern.

**Dateien:** `Kst4ContestApplication.java`, `ComposeChatInputActions.java`

- [ ] **Schritt 1: `tbl_chatMember` und die Zellenfabriken löschen**

Löschen: das Feld `tbl_chatMember`, die Methoden `refreshChatMemberTable`,
`createBandStatusCellFactory`, `applyQrgUiFormatting`, `applyTruncatedTextCells`,
`focusChatMemberAndPrepareCq`s Tabellenhälfte (`:950`–`:955`), `moveSelectedTableEntry`,
`markOperatorChatMemberSelectionIntent` samt dem Feld
`operatorInitiatedChatMemberSelectionChange`, und der Tabellenzweig in
`getEffectiveSelectedChatMember`. Jede davon vorher wie in S3a auf Aufrufer prüfen.

`getEffectiveSelectedChatMember` behält zwei Quellen:

```java
	private ChatMember getEffectiveSelectedChatMember() {
		if (selectedCallSignInfoStageChatMember != null) {
			return selectedCallSignInfoStageChatMember;
		}

		/*
		 * The JavaFX station table used to sit between these two. It never received rows
		 * after the window was disconnected, so this branch could not fire.
		 */
		if (chatcontroller != null
				&& chatcontroller.getScoreService() != null
				&& chatcontroller.getScoreService().getSelectedChatMember() != null) {
			return chatcontroller.getScoreService().getSelectedChatMember();
		}

		return null;
	}
```

- [ ] **Schritt 2: `ComposeChatInputActions.tableSelection()` nachziehen**

```java
    @Override
    public ChatMember tableSelection() {
        /*
         * The JavaFX station table is gone. Kept rather than removed from the interface
         * because ChatInputState's three-step lookup is the documented selection order and
         * a Compose station table may fill this in again.
         */
        return null;
    }
```

- [ ] **Schritt 3: Bauen, Testzahl prüfen, committen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
git add app-desktop/src/main/java/kst4contest/view/
git commit -m "Delete the never-populated JavaFX station table and its cell factories" -- \
        app-desktop/src/main/java/kst4contest/view/
```

### S3c — Die QRG- und QTF-Felder und der Rest der Steuerelemente

**Dateien:** `Kst4ContestApplication.java`

- [ ] **Schritt 1: Die restlichen Felder und ihre Methoden löschen**

Löschen: `txt_ownqrgMainCategory`, `txt_ownqrgSecondCategory`, `txt_myQTF`,
`flwPane_textSnippets`, `selectedCallSignInfoBorderPane`, `MYQRGButton`, `MYCALLSetQRGButton`,
und die Methoden `attachOwnQrgFollower`, `detachOwnQrgFollower`, `attachQtfFollower`,
`detachQtfFollower`, `attachPriorityScoreFollower`, `refreshShortcutButtons`, `buttonFactory`,
`createDoublePreferenceTextField`, `generateLabeledSeparator`, `createArrow`, sowie die Felder
`ownQrgFollower` und `qtfFollower`.

**Eine gemischte Methode:** `applyOwnQrgFollower(boolean)` ist ein `SettingsHost`-Override, den
die Compose-Einstellungen rufen. Sie ruft nur noch `attachOwnQrgFollower`/`detachOwnQrgFollower`,
die beide fallen. Sie wird eine No-op mit Begründung, nach dem Muster von
`refreshTextSnippetContextMenusFromSettings`:

```java
	/**
	 * No-op since the JavaFX own-QRG text field was removed with the never-shown window.
	 *
	 * <p>The Compose chat input reads the own QRG from preferences when it needs it, so there
	 * is no follower left to attach. The Compose settings still call this through the shared
	 * interface, and an empty body is honest about there being nothing to do.</p>
	 */
	@Override
	public void applyOwnQrgFollower(boolean enabled) {
		// intentionally empty: no JavaFX text field left to follow
	}
```

**Vorher prüfen**, dass die Compose-Sendezeile die eigene QRG wirklich selbst liest:

```bash
grep -rn "getMYQRGFirstCat\|ownQrg" app-desktop/src/main/kotlin/kst4contest/view/compose/ | head
```

Liest sie sie **nicht**, ist die No-op falsch — dann **anhalten und Marc fragen**, weil die
MYQRG-Knöpfe der Sendezeile sonst leer einfügen.

Ebenso `refreshShortcutButtonsFromSettings`, das `refreshShortcutButtons` ruft: dieselbe
Behandlung, mit derselben Vorprüfung gegen `ChatInputState`.

- [ ] **Schritt 2: Bauen, Testzahl prüfen, committen**

```bash
./gradlew clean build
git add app-desktop/src/main/java/kst4contest/view/
git commit -m "Delete the shadow QRG, QTF and shortcut controls" -- \
        app-desktop/src/main/java/kst4contest/view/
```

### S3d — Der Zeitstrahl-DTO — **GUI-ABNAHME**

`TimelineView.CandidateEvent` ist ein reines Zwischen-DTO: `buildTimelinePriorityCandidateEvents`
baut es, und `updateTimelineVisuals` kopiert es Feld für Feld in
`kst4contest.view.compose.TimelineCandidate`. Der Builder baut künftig direkt den Compose-Typ, und
die Umkopierschleife fällt — ein Übersetzungsschritt weniger.

**Dateien:** `Kst4ContestApplication.java`

- [ ] **Schritt 1: Die Feldreihenfolge festnageln, bevor etwas umgebaut wird**

```bash
grep -n "class TimelineCandidate" -A14 app-desktop/src/main/kotlin/kst4contest/view/compose/TimelineState.kt
sed -n '1645,1660p' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Die zehn Werte in der heutigen Umkopierschleife sind die Sollreihenfolge:
`callSignRaw`, `displayCallSign`, `preferredChatCategory`, `timeUntilMs`, `minuteBucket`,
`laneIndex`, `targetAzimuth`, `score`, `opportunityPotentialPercent`, `tooltipText`. Fünf davon
sind Zahlen, eine Vertauschung fällt nicht auf — darum zuerst der Test.

- [ ] **Schritt 2: Den Test schreiben, der die Reihenfolge festhält**

In `app-desktop/src/test/kotlin/kst4contest/view/compose/TimelineCandidateFieldsTest.kt`:

```kotlin
package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pins the field order of the timeline candidate.
 *
 * The JavaFX builder produced its own DTO and copied it across field by field; the builder
 * now constructs this type directly. Five of the ten values are numbers, so a swapped pair
 * would compile, run, and quietly draw the wrong marker at the wrong minute.
 */
class TimelineCandidateFieldsTest {

    @Test
    fun theTenValuesKeepTheirPositions() {
        val category = ChatCategory.values().first()

        val candidate = TimelineCandidate(
            "DL1ABC", "DL1ABC/P", category, 90_000L, 3, 1, 215.0, 42.5, 77, "tooltip",
        )

        assertEquals("DL1ABC", candidate.callSignRaw)
        assertEquals("DL1ABC/P", candidate.displayCallSign)
        assertEquals(category, candidate.preferredChatCategory)
        assertEquals(90_000L, candidate.timeUntilMs)
        assertEquals(3, candidate.minuteBucket)
        assertEquals(1, candidate.laneIndex)
        assertEquals(215.0, candidate.targetAzimuth)
        assertEquals(42.5, candidate.score)
        assertEquals(77, candidate.opportunityPotentialPercent)
        assertEquals("tooltip", candidate.tooltipText)
    }
}
```

Geprüft, bevor der Block geschrieben wurde (`TimelineState.kt:39-50`): die Reihenfolge ist
`callSignRaw: String`, `displayCallSign: String`, `preferredChatCategory: ChatCategory?`,
`timeUntilMs: Long`, `minuteBucket: Int`, `laneIndex: Int`, `targetAzimuth: Double`,
`score: Double`, `opportunityPotentialPercent: Int`, `tooltipText: String?`. **Die dritte Stelle
ist eine `ChatCategory`, keine Zahl** — ein Punkt, an dem eine Vertauschung mit `minuteBucket`
oder `laneIndex` nicht einmal übersetzen würde, während eine Vertauschung der drei Zahlen
untereinander lautlos durchgeht. Import: `kst4contest.model.ChatCategory`.

- [ ] **Schritt 3: Laufen lassen — er muss grün sein**

```bash
./gradlew :app-desktop:test --tests '*TimelineCandidateFieldsTest*'
```

Dieser Test ist bewusst **kein** RED-Test: er beschreibt bestehendes Verhalten, das der Umbau
erhalten muss. Wird er rot, stimmt die angenommene Reihenfolge nicht und der Umbau wäre falsch
geworden — dann die Erwartungen an die Deklaration anpassen, nicht die Deklaration an den Test.

- [ ] **Schritt 4: Den Builder umstellen**

`buildTimelinePriorityCandidateEvents()` gibt `List<TimelineCandidate>` zurück, die beiden
`out.add(new TimelineView.CandidateEvent(...))`-Stellen bauen `new TimelineCandidate(...)` mit
derselben Werteliste, der `Comparator` am Ende wird
`.comparingInt(TimelineCandidate::getMinuteBucket).thenComparingInt(TimelineCandidate::getLaneIndex)`
— die aus Kotlin erzeugten Zugriffsmethoden prüfen, ein `data class` liefert `getMinuteBucket()`.
In `updateTimelineVisuals` fällt die Umkopierschleife, und `timelineFeed.push(...)` bekommt die
Liste direkt.

- [ ] **Schritt 5: `TimelineView.java` löschen**

```bash
grep -rn "kst4contest.view.TimelineView\|TimelineView\." app-desktop/src core/src --include="*.java" --include="*.kt" | grep -v "compose"
git rm app-desktop/src/main/java/kst4contest/view/TimelineView.java
```

Der `grep` muss leer sein. **Nur die Java-Datei**, nicht `compose/TimelineView.kt`.

- [ ] **Schritt 6: Bauen, GUI-ABNAHME, committen**

```bash
./gradlew clean build && ./gradlew :app-desktop:run --args="--profile default"
```

Zu prüfen: Sked anlegen → Marke erscheint auf dem Zeitstrahl, an der richtigen Minute, mit dem
richtigen Hinweistext; Prioritätskandidaten erscheinen; Rotordrehung ändert die Darstellung.

```bash
git add -A app-desktop/src/main/java/kst4contest/view/ app-desktop/src/test/kotlin/kst4contest/view/compose/
git commit -m "Build timeline candidates as the Compose type and delete the JavaFX timeline" -- \
        app-desktop/src/main/java/kst4contest/view/ app-desktop/src/test/kotlin/kst4contest/view/compose/
```

---

## S4 — Die Kartendateien und der Kachel-Umleitungsserver

**Dateien zum Löschen:**
- `app-desktop/src/main/java/kst4contest/view/map/StationMapView.java`
- `app-desktop/src/main/java/kst4contest/view/map/StationMapBridge.java`
- `app-desktop/src/main/java/kst4contest/view/map/MapHtmlResources.java`
- `app-desktop/src/test/java/kst4contest/test/MapHtmlResourcesContractTest.java`
- `app-desktop/src/main/java/kst4contest/view/map/PathProfileChart.java` (**nicht** die `.kt`)
- `app-desktop/src/main/java/kst4contest/view/MessageTextTableCell.java`
- `app-desktop/src/main/java/kst4contest/view/TruncatedTextTableCell.java`
- `app-desktop/src/main/resources/web/leaflet/leaflet.css` und `leaflet.js`
- `core/src/main/java/kst4contest/view/map/TileProxyServer.java`

- [ ] **Schritt 1: Prüfen, dass nichts mehr darauf zeigt**

```bash
for c in StationMapView StationMapBridge MapHtmlResources MessageTextTableCell \
         TruncatedTextTableCell TileProxyServer; do
  echo "--- $c ---"
  grep -rn "\b$c\b" app-desktop/src core/src --include="*.java" --include="*.kt" \
    | grep -v "/$c\.java:" | grep -vE "^\S+: *\*|^\S+: *//"
done
echo "--- PathProfileChart, nur die Java-Nutzer ---"
grep -rn "kst4contest.view.map.PathProfileChart" app-desktop/src core/src
echo "--- Leaflet-Ressourcen ---"
grep -rn "leaflet" app-desktop/src/main --include="*.java" --include="*.kt"
```

Jede Ausgabe muss leer sein oder ein Kommentar, der nur die Herkunft einer Formulierung
beschreibt. Kommentare bleiben stehen. Zeigt echter Code noch hin, **anhalten**: dann ist etwas
lebend, was dieser Plan für tot hält, und das gehört in den Lebendbefund, bevor es gelöscht wird.

- [ ] **Schritt 2: Löschen**

```bash
git rm app-desktop/src/main/java/kst4contest/view/map/StationMapView.java \
       app-desktop/src/main/java/kst4contest/view/map/StationMapBridge.java \
       app-desktop/src/main/java/kst4contest/view/map/MapHtmlResources.java \
       app-desktop/src/test/java/kst4contest/test/MapHtmlResourcesContractTest.java \
       app-desktop/src/main/java/kst4contest/view/map/PathProfileChart.java \
       app-desktop/src/main/java/kst4contest/view/MessageTextTableCell.java \
       app-desktop/src/main/java/kst4contest/view/TruncatedTextTableCell.java \
       core/src/main/java/kst4contest/view/map/TileProxyServer.java
git rm -r app-desktop/src/main/resources/web/leaflet/
```

- [ ] **Schritt 3: Den `jdk.jsobject`-Eintrag im Build prüfen**

`app-desktop/build.gradle.kts` listet in `modules(...)` `jdk.jsobject` mit der Begründung „carries
netscape.javascript for the map bridge". Die Brücke fällt in diesem Schritt:

```bash
grep -rn "netscape.javascript" app-desktop/src core/src
```

Ist das leer, fällt `"jdk.jsobject"` aus der `modules(...)`-Liste und der Teilsatz aus dem
Kommentar darüber. `jdk.unsupported` **bleibt** — der Kommentar nennt dafür die Marlin-Begründung,
die mit JavaFX fällt, aber `sun.misc.Unsafe` wird auch von anderen Abhängigkeiten benutzt; das zu
entscheiden gehört nicht in diese Etappe. Den Kommentar entsprechend berichtigen.

- [ ] **Schritt 4: Bauen, Testzahl prüfen, committen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
```

Die Testzahl **sinkt** hier um die Tests von `MapHtmlResourcesContractTest`. Die Zahl vorher
notieren und die Differenz in der Commit-Nachricht nennen, damit ein späterer Leser den Rückgang
nicht für einen Verlust hält.

```bash
git add -A app-desktop/src core/src app-desktop/build.gradle.kts
git commit -m "Delete the JavaFX WebView map, its Leaflet resources and the tile proxy" -- \
        app-desktop/src core/src app-desktop/build.gradle.kts
```

---

## S5 — `TableLayoutManager` und die Testdatei im Hauptquellbaum

- [ ] **Schritt 1: `TableLayoutManager` entschlacken — die Klasse bleibt**

```bash
grep -n "javafx\|Platform\." app-desktop/src/main/java/kst4contest/view/TableLayoutManager.java
grep -rn "TableLayoutManager" app-desktop/src/main/kotlin | head
```

Sechs Compose-Dateien benutzen diese Klasse. Es bleibt, was keine JavaFX-Typen berührt —
insbesondere `calculateInitialContentWidth`, das `TableLayoutManagerTest` abdeckt. Die
`column(...)`-Methoden mit `TableColumn`-Parametern und die beiden `Platform.runLater`-Aufrufe in
`sizePendingColumns` fallen mit ihren JavaFX-Importen. Gibt es nach dem Entschlacken keinen
JavaFX-freien Rest außer `calculateInitialContentWidth`, dann **nicht** die Klasse löschen,
sondern auf diese eine Methode zusammenschrumpfen und die Klassen-Javadoc neu schreiben: die
Compose-Dateien verweisen namentlich darauf.

- [ ] **Schritt 2: Die Testdatei an ihren Platz schieben**

```bash
git mv app-desktop/src/main/java/kst4contest/test/MessageVariableResolverTest.java \
       app-desktop/src/test/java/kst4contest/test/MessageVariableResolverTest.java
grep -n "javafx" app-desktop/src/test/java/kst4contest/test/MessageVariableResolverTest.java
```

Der `javafx`-Import darin fällt, wenn er unbenutzt ist. Danach prüfen, ob weitere Testklassen im
Hauptquellbaum liegen, die den Kommentar in `app-desktop/build.gradle.kts` begründen
(„11 test classes live under src/main/java and need JUnit and Mockito on the main compile
classpath"):

```bash
grep -rln "@Test" app-desktop/src/main/java | sed 's/^/noch im main-Baum: /'
```

Ist die Liste leer, können `implementation(libs.junit.jupiter.api)` und
`implementation(libs.mockito.core.compile)` samt Kommentar aus dem Build fallen. Ist sie nicht
leer, bleiben sie, und die Zahl im Kommentar wird berichtigt.

- [ ] **Schritt 3: Bauen, Testzahl prüfen, committen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
git add -A app-desktop/src app-desktop/build.gradle.kts
git commit -m "Strip JavaFX from TableLayoutManager and move the stray test to src/test" -- \
        app-desktop/src app-desktop/build.gradle.kts
```

Die Testzahl **steigt** hier um die Tests von `MessageVariableResolverTest`: im Hauptquellbaum
wurden sie nie ausgeführt.

---

## S6 — Den Build befreien — **GUI-ABNAHME**

- [ ] **Schritt 1: Das Zielkriterium prüfen, bevor der Build angefasst wird**

```bash
grep -rn "javafx" core/src/main app-desktop/src/main || echo "LEER — Zielkriterium erfüllt"
```

Sind noch Treffer da, gehören sie in eine der vorigen Scheiben und nicht hierher. Treffer in
Kommentaren, die die **Herkunft** beschreiben, sind zulässig, solange sie nicht `javafx.`
schreiben; sonst umformulieren, wie es Teil 4 für `ScreenBounds`, `GuiUtils` und
`ComposeMenuActions` getan hat.

- [ ] **Schritt 2: Die Toolkit-Startmethode entfernen**

```bash
grep -n "startJavaFxToolkitForTheRemainingDetachedControls\|Platform\." \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Die Methode und ihr Aufruf in `main` fallen, dazu der Import `javafx.application.Platform`. Das
ist die Stelle, auf die die Javadoc der Methode verweist („Both this method and its call go when
Etappe 6 removes the controls").

- [ ] **Schritt 3: `org.openjfx` aus dem Build streichen**

In `app-desktop/build.gradle.kts`: die Zeile `alias(libs.plugins.javafx)` im `plugins`-Block, der
ganze `javafx { ... }`-Block, und
`implementation("org.openjfx:javafx-swing:${libs.versions.javafx.get()}")`.

In `gradle/libs.versions.toml`: `javafx = "21.0.5"`, `javafxPlugin = "0.1.0"` und
`javafx = { id = "org.openjfx.javafxplugin", version.ref = "javafxPlugin" }`.

- [ ] **Schritt 4: Bauen und beweisen, dass JavaFX weg ist**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
./gradlew :app-desktop:dependencies --configuration runtimeClasspath | grep -i javafx \
  || echo "LEER — JavaFX ist nicht mehr auf dem Klassenpfad"
```

Erwartung: Build grün, und der zweite Befehl sagt „LEER". **Das ist der Beweis der Spezifikation**
— ab hier kann nichts mehr versehentlich JavaFX benutzen, weil es nicht da ist.

- [ ] **Schritt 5: `JavaFxStylesheetTest` gegen den leeren Klassenpfad prüfen**

```bash
./gradlew :app-desktop:test --tests '*JavaFxStylesheetTest*' --info 2>&1 | grep -iE "skip|assum|javafx|PASS|FAIL"
```

Etappe 3 hat die `derive`-Werte als Daten eingecheckt, genau damit dieser Test das Entfernen
überlebt. Ruft er noch `com.sun.javafx.util.Utils` per Reflexion, muss der Zweig jetzt sauber
übersprungen werden, statt stillschweigend gegen sich selbst zu prüfen. Fällt er aus oder wird er
grün, ohne etwas zu prüfen, **anhalten und Marc sagen** — das wäre der Verlust des Farbpins, den
die Spezifikation ausdrücklich schützt.

- [ ] **Schritt 6: GUI-ABNAHME, die letzte**

```bash
./gradlew :app-desktop:run
```

Der vollständige Durchgang, weil ab hier kein JavaFX mehr einspringt: Profilwähler, Verbinden,
Stationsliste, Auswahl, private Nachricht, Zeitstrahl, Sked, Karte, Monitorfenster,
Einstellungsfenster, Entwurf hell/dunkel, alle Dialoge, Profilwechsel, Beenden über Menü und
Fensterkreuz, Spaltenbreiten über einen Neustart hinweg.

- [ ] **Schritt 7: Committen**

```bash
git add -A app-desktop/build.gradle.kts gradle/libs.versions.toml app-desktop/src
git commit -m "Remove the org.openjfx dependency; KST4Contest no longer uses JavaFX" -- \
        app-desktop/build.gradle.kts gradle/libs.versions.toml app-desktop/src
```

---

## S7 — Nachdokumentation

- [ ] **Schritt 1: Die Dokumente nachziehen**

```bash
grep -rn "JavaFX\|javafx" AGENTS.md CLAUDE.md docs/PROJECT_CONTEXT.md README.md 2>/dev/null
```

Zu berichtigen, weil es nach S6 falsch ist: `AGENTS.md` nennt KST4Contest einen „Java/JavaFX
desktop client" (Zeile 7) und „Java 21 / JavaFX 21.x" unter „Build and verification"; dazu die
Sätze über die JavaFX-`ObservableList` und die WebView-/Leaflet-Umgehung, die mit S4 ihren
Gegenstand verloren hat. `docs/PROJECT_CONTEXT.md` und `README.md` entsprechend.

Nicht zu berichtigen: die Spezifikationen und Pläne unter `docs/superpowers/` — die beschreiben
den Weg und sollen den Vorzustand nennen.

- [ ] **Schritt 2: Lebendbefund abschließen**

Abschlussnotiz für Etappe 6 oben in `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`:
der Beweis aus S6 Schritt 4, die endgültige Testzahl, die Zeilenzahl von
`Kst4ContestApplication`, und was von den neun Schattenfeldern wie ersetzt wurde.

- [ ] **Schritt 3: Dokumentationswirkung einschätzen**

Nach `AGENTS.md` („Documentation and durable project context") eine kurze Einschätzung, ob
nutzersichtbares Verhalten betroffen ist. Betroffen sind die Dialoge (Compose statt JavaFX, andere
Knopfreihenfolge auf macOS, keine Symbole mehr) und die Karte (Compose-Canvas statt WebView). Die
betreffenden Abschnitte unter `github_docs/` in **beiden** Sprachen suchen und abgleichen;
veraltete Bildschirmfotos benennen, **nicht** erfinden.

```bash
grep -rln "WebView\|Leaflet\|JavaFX" github_docs/ | head
```

- [ ] **Schritt 4: Committen**

```bash
git add AGENTS.md docs/PROJECT_CONTEXT.md README.md docs/superpowers/notes/ github_docs/
git commit -m "Update the binding docs and the manual for a KST4Contest without JavaFX" -- \
        AGENTS.md docs/PROJECT_CONTEXT.md README.md docs/superpowers/notes/ github_docs/
```

---

## Was unverändert bleibt

- Die Fachlogik in `core` und sämtliche Protokollformate. `core` verliert nur `TileProxyServer`.
- `UiDispatcher`, `AwtUiDispatcher`, `DirectUiDispatcher`.
- `selectedCallSignInfoStageChatMember` und `ComposeChatInputActions.panelSelection()`.
- `JavaFxStylesheet.kt` samt eingefrorener `derive`-Tabelle und `JavaFxStylesheetTest`.
- `macOsConnectionStateMenuTitle` und sein Test.
- Die 91 MP3-Dateien und die CW-Ausgabe in `PlayAudioUtils`.
- `StationMapClusterer` und `StationMapStatusText` in `core`.
- Verpackungs- und Vertriebsidentität: `packageName`, `bundleID`, der `kst4contest`-Paketname.

## Bekannte Fallstricke

- **`ComposeStationMapDrawingTest` ist flaky.** Bei einem roten Lauf zuerst isoliert nachprüfen.
- **PMD/SpotBugs** melden rund 4448 vorbestehende Befunde und brechen den Build nicht. Nur neue
  melden.
- **Vier Klassennamen sind doppelt belegt.** Siehe die Tabelle oben. `git rm` nach dem bloßen
  Namen löscht die lebende Compose-Fassung.
- **`new Kst4ContestApplication()` in einem Test** braucht ein gestartetes JavaFX-Toolkit, solange
  noch ein Steuerelementfeld im Konstruktor steht. Das ist der Grund für die Reihenfolge in S3.
- **Die Testzahl bewegt sich in beide Richtungen:** S4 senkt sie (gelöschter Kartentest), S5 hebt
  sie (die Testdatei wird erstmals ausgeführt). Jede Änderung in der Commit-Nachricht nennen.
