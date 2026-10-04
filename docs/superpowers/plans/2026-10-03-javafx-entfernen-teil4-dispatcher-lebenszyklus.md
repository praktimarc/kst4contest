# Umsetzungsplan — JavaFX entfernen, Teil 4: Dispatcher, Dialoge, Lebenszyklus

> **Für ausführende Agenten:** Dieser Plan setzt die Etappen 4 und 5 der Spezifikation in
> einem Zug um. Schritte sind als Kästchen (`- [ ]`) geführt. Nach jeder Aufgabe ein
> Commit; nach den mit **GUI-ABNAHME** markierten Aufgaben die Anwendung starten und von
> Marc bedienen lassen, bevor es weitergeht.

**Ziel:** `Kst4ContestApplication` startet ohne `javafx.application.Application`, stellt UI-Arbeit
über den AWT-Ereignisfaden zu und zeigt alle Dialoge als Compose-Fenster.

**Architektur:** Der `UiDispatcher`-Vertrag bleibt unverändert; nur die Umsetzung wechselt von
`Platform.runLater` auf `EventQueue.invokeLater` — den Faden, auf dem Compose ohnehin komponiert.
Weil `ComposeMenuActions` jede Menüaktion über diesen Dispatcher schickt, müssen die zehn
JavaFX-`Alert`-Aufrufe und der `new Stage()` des Profilwechsels **vor** dem Tausch fallen; sonst
laufen sie auf dem falschen Faden und werfen. Die Dialoge werden modale `ComposeDialog`-Fenster
(ein `JDialog` aus der Compose-AWT-Brücke), weil ein modaler `JDialog` auf dem Ereignisfaden eine
geschachtelte Ereignisschleife startet und damit genau die `showAndWait`-Semantik liefert, die die
Ablaufsteuerung dieser Dialoge braucht.

**Werkzeuge:** Java 21, Gradle Wrapper, Kotlin 2.2.0, Compose Multiplatform 1.8.2, JUnit 5.

**Spezifikation:** `docs/superpowers/specs/2026-10-01-javafx-entfernen-design.md`, Etappen 4 und 5.
**Lebendbefund:** `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`.
**Vorgänger:** `docs/superpowers/plans/2026-10-02-javafx-entfernen-teil3.md`.

## Projektweite Vorgaben

- Kommunikation mit Marc auf **Deutsch**; Quelltextkommentare und Javadoc **ausschließlich
  Englisch**. Commit-Nachrichten knappes Englisch.
- `./gradlew clean build`, Java 21. Zwei Module: `core` (toolkit-frei) und `app-desktop`.
- **Nicht aus Exit-Code 0 auf grün schließen.** Nach jeder Aufgabe die XML unter
  `*/build/test-results/test/TEST-*.xml` auswerten. Grundlinie: **800 Tests, 0 Fehler**.
- **Signierte Commits.** Schlägt das Signieren mit `Couldn't get agent socket` fehl, ist der
  Bitwarden-SSH-Agent gesperrt — Marc entsperren lassen, **nicht** unsigniert committen.
- Kein Push, kein Merge, kein Tag, kein Release, kein Versions-Bump.
- Die Schnittstelle `kst4contest.observe.UiDispatcher` bleibt unverändert — nur ihre Umsetzung
  wird getauscht.
- `core` bekommt in diesem Plan **keine** Änderung. Kein `java.awt`-Import in `core`.
- Keine neue Produktionsabhängigkeit. `ComposeDialog` liegt in `ui-desktop-1.8.2.jar`, das
  bereits über `compose.desktop.currentOs` auf dem Klassenpfad ist.
- **Nicht anfassen:** `TableLayoutManager.java` (wird von sechs Compose-Dateien benutzt, verliert
  seine JavaFX-Importe erst in Etappe 6), die 91 MP3-Dateien, `PlayAudioUtils`, die Kartendateien
  `StationMapView`/`StationMapBridge`/`MapHtmlResources`, `MessageTextTableCell`,
  `TruncatedTextTableCell`, `PathProfileChart`, `TimelineView`, `FxRosterBinding`, der
  `org.openjfx`-Block in `app-desktop/build.gradle.kts` und `gradle/libs.versions.toml`. Das ist
  alles Etappe 6.

## Prüfschwerpunkte

Fünf Eingaben beziehungsweise Zustände, die die Spezifikation voraussetzt, aber nicht als Test
benennt. Jede Zeile bekommt ihren Test in der Aufgabe, der der Code gehört.

1. **Blockierender Dialog vom Ereignisfaden aus.** Ein modaler Dialog, der vom AWT-Faden geöffnet
   wird, muss den Aufrufer anhalten **und** sich trotzdem zeichnen. Täte er das nicht, stünde die
   Anwendung beim ersten Profilwechsel. → Aufgabe 1 (Sonde) und Aufgabe 3.
2. **Zustellung vom Fremdfaden, wenn noch kein Fenster offen ist.** `MessageBusManagementThread`
   stellt ab der ersten Verbindung zu, und das kann vor dem ersten gezeichneten Fenster passieren.
   Der Ereignisfaden muss dann trotzdem anlaufen. → Aufgabe 2.
3. **Eine geworfene Ausnahme in einer UI-Aufgabe.** Reißt sie den Ereignisfaden, steht die ganze
   Oberfläche — nicht nur die eine Aktualisierung. → Aufgabe 2.
4. **`mailto:` gegen `http:`.** `getHostServices().showDocument` behandelte beides gleich,
   `java.awt.Desktop` nicht: Mail will `Desktop.mail(URI)`, Web will `Desktop.browse(URI)`. Ein
   `browse("mailto:…")` öffnet auf Linux nichts. → Aufgabe 5.
5. **Gespeicherte Fenstergröße von einem größeren Bildschirm.** Die Korrektur auf den aktuell
   verfügbaren Bereich muss den Wechsel von `javafx.stage.Screen` auf
   `GraphicsEnvironment.getMaximumWindowBounds()` überleben, sonst startet die Anwendung größer
   als der Schirm. → Aufgabe 6.

---

## Dateiübersicht

**Neu:**

| Datei | Verantwortung |
|---|---|
| `app-desktop/src/main/java/kst4contest/view/AwtUiDispatcher.java` | `UiDispatcher` auf dem AWT-Ereignisfaden |
| `app-desktop/src/test/java/kst4contest/view/AwtUiDispatcherTest.java` | Vertrag des Dispatchers |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/ComposeAlert.kt` | Der Dialogbaustein: Beschreibung, Knöpfe, modales Fenster |
| `app-desktop/src/test/kotlin/kst4contest/view/compose/ComposeAlertTest.kt` | Die toolkit-freien Regeln des Bausteins |
| `app-desktop/src/main/java/kst4contest/view/ExternalDocuments.java` | Ersatz für `HostServices.showDocument` |
| `app-desktop/src/test/java/kst4contest/view/ExternalDocumentsTest.java` | Schema-Entscheidung Mail gegen Web |
| `app-desktop/src/main/java/kst4contest/view/ScreenBounds.java` | Verfügbarer Bildschirmbereich über AWT |
| `app-desktop/src/test/java/kst4contest/view/ScreenBoundsTest.java` | Begrenzung der gespeicherten Größe |

**Geändert:** `Kst4ContestApplication.java` (Hauptlast), `ApplicationRuntimeLauncher.java`,
`LayoutAutosave.java`, `Main.java`, `GuiUtils.java`, `app-desktop/build.gradle.kts` (nur
`mainClass`, falls nötig).

**Gelöscht:** `app-desktop/src/main/java/kst4contest/view/JavaFxUiDispatcher.java` sowie die in
Aufgabe 11 benannten toten Methoden.

---

## Aufgabe 1: Sonde — modaler `ComposeDialog` vom Ereignisfaden

**Wegwerfcode.** Das Ergebnis ist eine Antwort, kein Baustein. Die Datei wird am Ende der Aufgabe
wieder gelöscht; nur die Antwort wandert in diesen Plan und in die Übergabe.

**Die Frage:** Hält ein modaler `androidx.compose.ui.awt.ComposeDialog` den Aufrufer an und
zeichnet sich trotzdem — sowohl wenn er vom AWT-Ereignisfaden geöffnet wird als auch von einem
Fremdfaden? Davon hängt ab, ob die vier blockierenden Dialoge (Aufgabe 3) ihre Ablaufsteuerung
behalten können.

**Dateien:**
- Anlegen (und am Ende löschen): `app-desktop/src/test/kotlin/kst4contest/view/compose/ComposeDialogProbeTest.kt`

- [ ] **Schritt 1: Die Sonde schreiben**

```kotlin
package kst4contest.view.compose

import androidx.compose.material3.Text
import androidx.compose.ui.awt.ComposeDialog
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.awt.Dialog
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Timer
import kotlin.test.assertTrue

/** THROWAWAY probe for the Etappe 4/5 plan. Delete once the question is answered. */
class ComposeDialogProbeTest {

    private fun probe(label: String): Pair<Long, Boolean> {
        val composed = AtomicBoolean(false)
        val dialog = ComposeDialog(null, Dialog.ModalityType.APPLICATION_MODAL)
        dialog.size = Dimension(300, 150)
        dialog.setContent {
            composed.set(true)
            Text("probe $label")
        }
        /* Closes the dialog from the event thread, which is the only thread that may. */
        Timer(1500) { dialog.dispose() }.apply { isRepeats = false }.start()

        val startedAt = System.nanoTime()
        dialog.isVisible = true
        val blockedMs = (System.nanoTime() - startedAt) / 1_000_000

        return blockedMs to composed.get()
    }

    @Test
    fun blocksAndRendersFromTheEventThread() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        var result: Pair<Long, Boolean>? = null
        EventQueue.invokeAndWait { result = probe("edt") }

        println("[probe] from EDT: blocked=${result!!.first}ms composed=${result!!.second}")
        assertTrue(result!!.first >= 1_000, "a modal dialog must block its caller")
        assertTrue(result!!.second, "the dialog must have composed while blocking")
    }

    @Test
    fun blocksAndRendersFromAForeignThread() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display")

        val result = probe("worker")

        println("[probe] from worker: blocked=${result.first}ms composed=${result.second}")
        assertTrue(result.first >= 1_000, "a modal dialog must block its caller")
        assertTrue(result.second, "the dialog must have composed while blocking")
    }
}
```

Der Konstruktor wird als `ComposeDialog(owner: Window?, modalityType: Dialog.ModalityType)`
angenommen. Weicht er in 1.8.2 ab, die tatsächliche Signatur nehmen:

```bash
javap -cp ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.compose.ui/ui-desktop/1.8.2/*/ui-desktop-1.8.2.jar \
      androidx.compose.ui.awt.ComposeDialog | head -30
```

- [ ] **Schritt 2: Sonde laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*ComposeDialogProbeTest*' --info
```

Erwartung: beide Tests grün, in der Ausgabe `blocked=~1500ms composed=true` für beide Fäden.

- [ ] **Schritt 3: Das Ergebnis festhalten**

Ergebnis in `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md` oben als kurze Notiz
eintragen: ob der modale `ComposeDialog` trägt, mit den gemessenen Zahlen.

**Scheitert die Sonde** — der Aufruf kehrt sofort zurück, oder `composed` bleibt falsch, oder es
hängt —, ist der Plan ab Aufgabe 3 hinfällig. Dann **anhalten und Marc fragen**, nicht ausweichen.
Der Ausweichweg wäre, die vier blockierenden Dialoge auf Rückrufe umzubauen; das ändert die
Ablaufsteuerung und ist eine eigene Entscheidung, die die Spezifikation ausdrücklich Marc überlässt
(„Die Dialoge sind Ablaufsteuerung, keine Dekoration").

- [ ] **Schritt 4: Sonde löschen und committen**

```bash
rm app-desktop/src/test/kotlin/kst4contest/view/compose/ComposeDialogProbeTest.kt
git add docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Record the modal ComposeDialog probe result for the dispatcher stage"
```

---

## Aufgabe 2: `AwtUiDispatcher`

Die neue Umsetzung entsteht, wird aber noch nicht verdrahtet. Keine Verhaltensänderung.

**Dateien:**
- Anlegen: `app-desktop/src/main/java/kst4contest/view/AwtUiDispatcher.java`
- Anlegen: `app-desktop/src/test/java/kst4contest/view/AwtUiDispatcherTest.java`

**Schnittstellen:**
- Nutzt: `kst4contest.observe.UiDispatcher` mit `void runOnUi(Runnable)` und `boolean isUiThread()`.
- Liefert: `public class AwtUiDispatcher implements UiDispatcher` mit öffentlichem
  Standardkonstruktor. Aufgabe 9 ersetzt damit `new JavaFxUiDispatcher()`.

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwtUiDispatcherTest {

    private final AwtUiDispatcher dispatcher = new AwtUiDispatcher();

    @Test
    void deliversWorkFromAForeignThreadToTheEventThread() throws Exception {
        AtomicBoolean ranOnEventThread = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);

        dispatcher.runOnUi(() -> {
            ranOnEventThread.set(EventQueue.isDispatchThread());
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS), "the task was never delivered");
        assertTrue(ranOnEventThread.get(), "the task must run on the AWT event thread");
    }

    @Test
    void runsInlineWhenAlreadyOnTheEventThread() throws Exception {
        AtomicInteger runs = new AtomicInteger();

        EventQueue.invokeAndWait(() -> {
            dispatcher.runOnUi(runs::incrementAndGet);
            // Inline means: already done when runOnUi returns, not queued behind us.
            assertEquals(1, runs.get(), "a task handed over on the UI thread must run at once");
        });

        assertEquals(1, runs.get());
    }

    @Test
    void reportsTheEventThreadAndOnlyThat() throws Exception {
        assertFalse(dispatcher.isUiThread(), "a test thread is not the UI thread");

        AtomicBoolean onEventThread = new AtomicBoolean();
        EventQueue.invokeAndWait(() -> onEventThread.set(dispatcher.isUiThread()));

        assertTrue(onEventThread.get(), "the AWT event thread must report itself as the UI thread");
    }

    @Test
    void aFailingTaskNeitherReachesTheCallerNorKillsTheEventThread() throws Exception {
        dispatcher.runOnUi(() -> { throw new IllegalStateException("boom"); });

        CountDownLatch stillAlive = new CountDownLatch(1);
        dispatcher.runOnUi(stillAlive::countDown);

        assertTrue(stillAlive.await(5, TimeUnit.SECONDS),
                "the event thread must survive a failing UI task");
    }

    @Test
    void aFailingTaskHandedOverOnTheEventThreadDoesNotReachTheCaller() throws Exception {
        AtomicBoolean reachedTheLineAfter = new AtomicBoolean();

        EventQueue.invokeAndWait(() -> {
            dispatcher.runOnUi(() -> { throw new IllegalStateException("boom"); });
            reachedTheLineAfter.set(true);
        });

        assertTrue(reachedTheLineAfter.get(),
                "an inline failure must not propagate into the caller");
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*AwtUiDispatcherTest*'
```

Erwartung: Übersetzungsfehler, `AwtUiDispatcher` existiert nicht.

- [ ] **Schritt 3: Die Umsetzung schreiben**

```java
package kst4contest.view;

import kst4contest.observe.UiDispatcher;

import java.awt.EventQueue;

/**
 * The {@link UiDispatcher} backed by the AWT event dispatch thread.
 *
 * <p>That is the thread Compose Desktop composes on, so this is where user-interface
 * state belongs. The contract is the one {@code JavaFxUiDispatcher} had, down to the
 * inline execution: a task handed over from the user-interface thread runs before
 * {@link #runOnUi(Runnable)} returns rather than queueing behind work that is already
 * pending. Several callers in {@code ChatController} ask {@link #isUiThread()} first and
 * rely on exactly that.
 */
public class AwtUiDispatcher implements UiDispatcher {

    @Override
    public void runOnUi(Runnable task) {
        if (EventQueue.isDispatchThread()) {
            runGuarded(task);
        } else {
            EventQueue.invokeLater(() -> runGuarded(task));
        }
    }

    @Override
    public boolean isUiThread() {
        return EventQueue.isDispatchThread();
    }

    private static void runGuarded(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            // Must not tear down the AWT event dispatch thread.
            System.err.println("[observe] UI task failed: " + e);
        }
    }
}
```

- [ ] **Schritt 4: Tests laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*AwtUiDispatcherTest*'
```

Erwartung: 5 Tests grün.

- [ ] **Schritt 5: Ganzen Build prüfen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
```

Erwartung: `tests=805 failures=0 errors=0` (800 plus die fünf neuen).

- [ ] **Schritt 6: Committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/AwtUiDispatcher.java \
        app-desktop/src/test/java/kst4contest/view/AwtUiDispatcherTest.java
git commit -m "Add the AWT event-thread UiDispatcher with its contract tests"
```

---

## Aufgabe 3: Der Dialogbaustein `ComposeAlert`

Ein Baustein, der die drei Dinge liefert, die die neun `Alert`-Stellen brauchen: einen
blockierenden Dialog mit Antwort, einen blockierenden Dialog ohne Antwort und einen
nicht-blockierenden Hinweis. Noch keine Umstellung der Aufrufer.

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/ComposeAlert.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/compose/ComposeAlertTest.kt`

**Schnittstellen:**
- Nutzt: `Kst4ContestTheme(darkMode: Boolean, baseFontSizeSp: Float, content: @Composable () -> Unit)`
  aus `Theme.kt`, `Form.button(text, enabled, onClick)` aus `FormControls.kt`, `Density` aus
  `Density.kt`.
- Liefert, von Java aus aufrufbar:
  - `ComposeAlert.show(title: String, header: String?, body: String, darkMode: Boolean)` — kehrt
    sofort zurück.
  - `ComposeAlert.confirm(title: String, header: String?, body: String, confirmText: String, cancelText: String, darkMode: Boolean): Boolean` —
    blockiert, liefert `true` für den Bestätigungsknopf.
  - `ComposeAlert.acknowledge(title: String, header: String?, body: String, darkMode: Boolean)` —
    blockiert bis „OK".
  - `ComposeAlert.showWithLink(title: String, header: String?, body: String, linkText: String, linkTarget: String, darkMode: Boolean)` —
    kehrt sofort zurück, der Verweis geht über `ExternalDocuments.open` aus Aufgabe 5. Diese
    Überladung wird erst in Aufgabe 5 fertiggestellt; hier entsteht sie ohne Verweiszeile und
    bekommt sie dort.

- [ ] **Schritt 1: Den fehlschlagenden Test für die toolkit-freien Regeln schreiben**

Getestet wird, was ohne Bildschirm entscheidbar ist: die Knopfzeile. Das Zeichnen selbst deckt
die GUI-Abnahme ab — dasselbe Verhältnis wie bei `raiseSteps`/`deiconifiedState` in
`ComposeWindowHost.kt`.

```kotlin
package kst4contest.view.compose

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeAlertTest {

    @Test
    fun anAcknowledgementHasOneButtonAndItConfirms() {
        val buttons = alertButtons(confirmText = "OK", cancelText = null)

        assertEquals(listOf(AlertButton("OK", confirming = true)), buttons)
    }

    @Test
    fun aConfirmationOffersTheConfirmingButtonFirst() {
        val buttons = alertButtons(confirmText = "Switch profile", cancelText = "Cancel")

        assertEquals(
            listOf(
                AlertButton("Switch profile", confirming = true),
                AlertButton("Cancel", confirming = false),
            ),
            buttons,
        )
    }

    @Test
    fun closingTheWindowCountsAsNotConfirming() {
        // The JavaFX Alert returned Optional.empty when dismissed, and every caller
        // treated that as "do not proceed". The window close button must do the same.
        assertTrue(!dismissalConfirms())
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*ComposeAlertTest*'
```

Erwartung: Übersetzungsfehler, `alertButtons`/`AlertButton`/`dismissalConfirms` existieren nicht.

- [ ] **Schritt 3: `ComposeAlert.kt` schreiben**

```kotlin
package kst4contest.view.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Dialog
import java.awt.Dimension
import java.awt.EventQueue
import androidx.compose.ui.awt.ComposeDialog

/** One button of an alert, and whether pressing it means "go ahead". */
internal data class AlertButton(val text: String, val confirming: Boolean)

/**
 * The button row of an alert.
 *
 * The confirming button comes first because that is the order the JavaFX dialogs showed
 * and the operator's muscle memory for the profile switch sits on the left-hand button.
 */
internal fun alertButtons(confirmText: String, cancelText: String?): List<AlertButton> =
    if (cancelText == null) {
        listOf(AlertButton(confirmText, confirming = true))
    } else {
        listOf(AlertButton(confirmText, confirming = true), AlertButton(cancelText, confirming = false))
    }

/**
 * What closing the window without pressing a button means.
 *
 * The JavaFX Alert returned an empty Optional and every caller read that as "do not
 * proceed"; `closeWindowEvent` would otherwise quit the client when the operator
 * dismisses the question.
 */
internal fun dismissalConfirms(): Boolean = false

/**
 * The replacement for the JavaFX {@code Alert}.
 *
 * A modal {@link ComposeDialog} rather than a Compose `application { }` window on its own
 * thread: a modal AWT dialog shown from the event thread runs a nested event pump, so it
 * blocks its caller *and* keeps drawing. The thread-plus-latch pattern that
 * [OperatorProfilePickerWindow] uses cannot do that — once the dispatcher hands menu
 * actions to the event thread, blocking it with a latch would deadlock against the window
 * that is trying to draw on the very same thread.
 */
object ComposeAlert {

    /** The font size the JavaFX dialogs had; the settings windows use the same value. */
    private const val DIALOG_FONT_SIZE_SP = 12f

    private val DIALOG_SIZE = Dimension(520, 260)

    /** Shows the alert and returns at once. For notices nothing waits on. */
    @JvmStatic
    @JvmOverloads
    fun show(title: String, header: String?, body: String, darkMode: Boolean = false) {
        EventQueue.invokeLater {
            open(title, header, body, confirmText = "OK", cancelText = null,
                darkMode = darkMode, modal = false)
        }
    }

    /** Shows the alert and blocks until the operator acknowledges it. */
    @JvmStatic
    @JvmOverloads
    fun acknowledge(title: String, header: String?, body: String, darkMode: Boolean = false) {
        open(title, header, body, confirmText = "OK", cancelText = null,
            darkMode = darkMode, modal = true)
    }

    /**
     * Asks a yes/no question and blocks until it is answered.
     *
     * @return true only when the operator pressed the confirming button; dismissing the
     *         window counts as a no, the way an empty Optional did.
     */
    @JvmStatic
    @JvmOverloads
    fun confirm(
        title: String,
        header: String?,
        body: String,
        confirmText: String,
        cancelText: String,
        darkMode: Boolean = false,
    ): Boolean = open(title, header, body, confirmText, cancelText, darkMode, modal = true)

    private fun open(
        title: String,
        header: String?,
        body: String,
        confirmText: String,
        cancelText: String?,
        darkMode: Boolean,
        modal: Boolean,
    ): Boolean {
        var answer = dismissalConfirms()

        val dialog = ComposeDialog(
            null,
            if (modal) Dialog.ModalityType.APPLICATION_MODAL else Dialog.ModalityType.MODELESS,
        )
        dialog.title = title
        dialog.size = DIALOG_SIZE
        dialog.setLocationRelativeTo(null)
        /*
         * Above everything else on purpose. These dialogs are asked for from Compose
         * windows, which are not their owner; without this the question opens behind the
         * window that asked it and the operator sees nothing happen at all. The JavaFX
         * profile-switch confirmation needed the same and said so.
         */
        dialog.isAlwaysOnTop = true

        dialog.setContent {
            Kst4ContestTheme(darkMode = darkMode, baseFontSizeSp = DIALOG_FONT_SIZE_SP) {
                AlertContent(
                    header = header,
                    body = body,
                    buttons = alertButtons(confirmText, cancelText),
                    onPressed = { pressed ->
                        answer = pressed.confirming
                        dialog.dispose()
                    },
                )
            }
        }

        dialog.isVisible = true
        return answer
    }
}

@Composable
private fun AlertContent(
    header: String?,
    body: String,
    buttons: List<AlertButton>,
    onPressed: (AlertButton) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (header != null) {
                Text(header, style = MaterialTheme.typography.titleMedium)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                /* Scrolls rather than clips: onSimpleLogFileCreated shows five paragraphs. */
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                buttons.forEach { button ->
                    Form.button(button.text) { onPressed(button) }
                }
            }
        }
    }
}
```

Geprüft, bevor der Block geschrieben wurde: `Density.BUTTON_GAP` ist `6.dp`, und `Form.button` hat
die Signatur `fun button(text: String, enabled: Boolean = true, onClick: () -> Unit)`
(`FormControls.kt:367`). Beide so benutzen, keine eigenen Maße einführen.

- [ ] **Schritt 4: Tests laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*ComposeAlertTest*'
```

Erwartung: 3 Tests grün.

- [ ] **Schritt 5: Ganzen Build prüfen und committen**

```bash
./gradlew clean build
git add app-desktop/src/main/kotlin/kst4contest/view/compose/ComposeAlert.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/ComposeAlertTest.kt
git commit -m "Add the Compose alert dialog, modal so it keeps showAndWait semantics"
```

---

## Aufgabe 4: Die vier blockierenden Dialoge umstellen — **GUI-ABNAHME**

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
  — `resolveOperatorProfileIfRequired` (um `:2431`), `showOperatorProfileSwitchDialog` (um `:2479`),
  `confirmOperatorProfileSwitch` (um `:2532`), `closeWindowEvent` (um `:3083`)

Die Zeilennummern sind Stand heute und verschieben sich mit jeder Änderung — nach dem
Methodennamen suchen, nicht nach der Zahl.

- [ ] **Schritt 1: `resolveOperatorProfileIfRequired` umstellen**

Ersetzen:

```java
		if (bootstrap.getStartupWarning() != null) {
			Alert startupWarning = new Alert(AlertType.WARNING);
			startupWarning.setTitle("Operator profile");
			startupWarning.setHeaderText("The requested operator profile was not found.");
			startupWarning.setContentText(bootstrap.getStartupWarning());
			startupWarning.showAndWait();
		}
```

durch:

```java
		if (bootstrap.getStartupWarning() != null) {
			ComposeAlert.acknowledge(
					"Operator profile",
					"The requested operator profile was not found.",
					bootstrap.getStartupWarning());
		}
```

- [ ] **Schritt 2: `showOperatorProfileSwitchDialog` umstellen**

Ersetzen:

```java
		if (selectableProfiles.size() < 2) {
			Alert noProfilesYet = new Alert(AlertType.INFORMATION);
			noProfilesYet.setTitle("Operator profiles");
			noProfilesYet.setHeaderText("Only one operator profile is configured.");
			noProfilesYet.setContentText(
					"Additional profiles are created in the settings window on the "
							+ "\"Profiles\" tab. Each profile keeps its own settings and layout, "
							+ "and can either share the station worked database or use its own.");
			noProfilesYet.showAndWait();
			return;
		}
```

durch:

```java
		if (selectableProfiles.size() < 2) {
			ComposeAlert.acknowledge(
					"Operator profiles",
					"Only one operator profile is configured.",
					"Additional profiles are created in the settings window on the "
							+ "\"Profiles\" tab. Each profile keeps its own settings and layout, "
							+ "and can either share the station worked database or use its own.",
					chatcontroller.getChatPreferences().isGUI_darkModeActive());
			return;
		}
```

- [ ] **Schritt 3: `confirmOperatorProfileSwitch` umstellen**

Die Methode schrumpft auf den Aufruf. Der `alwaysOnTop`-Block entfällt, weil `ComposeAlert` das
selbst macht; der Kommentar, der die Begründung trägt, wandert mit nach `ComposeAlert.kt` — er
steht dort bereits.

```java
	private boolean confirmOperatorProfileSwitch(OperatorProfile targetProfile) {

		return ComposeAlert.confirm(
				"Switch operator profile",
				"Switch to \"" + targetProfile.getDisplayName() + "\"?",
				"The ON4KST connection is closed and all windows are rebuilt with the "
						+ "settings and layout of the selected profile.\n\n"
						+ "Unsaved settings of the current profile are lost. Window sizes, "
						+ "divider and column widths are saved automatically.",
				"Switch profile",
				"Cancel",
				chatcontroller.getChatPreferences().isGUI_darkModeActive());
	}
```

- [ ] **Schritt 4: `closeWindowEvent` umstellen**

Die Methode nimmt heute ein `javafx.stage.WindowEvent` entgegen, das sie nie benutzt — beide
Aufrufer übergeben `null`. Der Parameter fällt mit, sonst bleibt ein JavaFX-Import nur für ihn
stehen.

```java
	/**
	 * Asks whether the chat connection may be given up, and quits when it may.
	 *
	 * <p>Blocking on purpose: the answer decides whether the application exits. The former
	 * {@code WindowEvent} parameter is gone with the JavaFX stage; both callers passed null
	 * and the body never read it.</p>
	 */
	private void closeWindowEvent() {
		System.out.println("Window close request ...");

		boolean quit = ComposeAlert.confirm(
				"Quit application",
				null,
				"Do you want to disconnect from the Chat?",
				"Yes",
				"Cancel",
				chatcontroller.getChatPreferences().isGUI_darkModeActive());

		if (quit) {
			System.out.println("closewindowevent: exiting the application");

			// Routed through the launcher so the runtime that is actually live
			// releases its resources. After a profile switch that is no longer the
			// instance JavaFX would call stop() on.
			ApplicationRuntimeLauncher.exitApplication();
		}
	}
```

Beide Aufrufstellen mitziehen: `menuActionExit()` ruft `closeWindowEvent()`, und die Stelle um
`:4668` (`uiDispatcher.runOnUi(() -> closeWindowEvent(null))`) wird
`uiDispatcher.runOnUi(this::closeWindowEvent)`.

- [ ] **Schritt 5: Import ergänzen**

In `Kst4ContestApplication.java` bei den übrigen Importen:

```java
import kst4contest.view.compose.ComposeAlert;
```

- [ ] **Schritt 6: Bauen und Testzahl prüfen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
```

Erwartung: `tests=808 failures=0 errors=0`.

- [ ] **Schritt 7: GUI-ABNAHME durch Marc**

```bash
./gradlew :app-desktop:run
```

Zu prüfen: Profilwechsel über das Menü (Bestätigung erscheint **vor** dem Hauptfenster, nicht
dahinter; „Cancel" lässt die Sitzung stehen; „Switch profile" baut sie neu auf); Menü → Exit
(Frage erscheint, „Cancel" bricht ab, „Yes" beendet); Start mit `--profile nichtvorhanden`
(Startwarnung erscheint und blockiert, bis sie bestätigt ist).

- [ ] **Schritt 8: Committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Turn the four blocking JavaFX alerts into modal Compose dialogs"
```

---

## Aufgabe 5: Die nicht-blockierenden Hinweise und `getHostServices`

Beides in einer Aufgabe, weil `onSimpleLogFileCreated` einen Hinweis **mit** Verweis zeigt und
damit beides braucht.

**Dateien:**
- Anlegen: `app-desktop/src/main/java/kst4contest/view/ExternalDocuments.java`
- Anlegen: `app-desktop/src/test/java/kst4contest/view/ExternalDocumentsTest.java`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/ComposeAlert.kt`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

**Schnittstellen:**
- Liefert: `ExternalDocuments.open(String address)` — der Ersatz für
  `getHostServices().showDocument(address)` an allen sieben Stellen.
- Liefert: `ExternalDocuments.isMailAddress(String address)` — die testbare Entscheidung dahinter.
- Liefert: `ComposeAlert.showWithLink(title, header, body, linkText, linkTarget, darkMode)`.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```java
package kst4contest.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalDocumentsTest {

    @Test
    void aMailtoAddressGoesToTheMailClient() {
        assertTrue(ExternalDocuments.isMailAddress("mailto:praktimarc+kst4contest@gmail.com"));
    }

    @Test
    void theSchemeIsReadWithoutRegardToCase() {
        assertTrue(ExternalDocuments.isMailAddress("MAILTO:someone@example.org"));
    }

    @Test
    void webAddressesGoToTheBrowser() {
        assertFalse(ExternalDocuments.isMailAddress("https://ko-fi.com/praktimarc"));
        assertFalse(ExternalDocuments.isMailAddress("http://www.x08.de"));
    }

    @Test
    void anAddressThatMentionsMailtoLaterIsStillAWebAddress() {
        // Would be a mail client launch for a perfectly ordinary link otherwise.
        assertFalse(ExternalDocuments.isMailAddress("https://example.org/?to=mailto:x@y.z"));
    }

    @Test
    void nothingIsNotAMailAddress() {
        assertFalse(ExternalDocuments.isMailAddress(null));
        assertFalse(ExternalDocuments.isMailAddress("   "));
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*ExternalDocumentsTest*'
```

Erwartung: Übersetzungsfehler, `ExternalDocuments` existiert nicht.

- [ ] **Schritt 3: `ExternalDocuments` schreiben**

```java
package kst4contest.view;

import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens an address in whatever the desktop uses for it.
 *
 * <p>Replaces {@code Application.getHostServices().showDocument(...)}, which went away
 * with {@code extends Application}. JavaFX treated web and mail addresses alike; AWT does
 * not, so the scheme is decided here: {@code Desktop.mail} for mail, {@code Desktop.browse}
 * for everything else. A {@code browse("mailto:...")} silently opens nothing on Linux.</p>
 */
public final class ExternalDocuments {

    private static final Logger LOGGER = Logger.getLogger(ExternalDocuments.class.getName());

    private static final String MAIL_SCHEME = "mailto:";

    private ExternalDocuments() {
        // Utility class.
    }

    /** Whether the address names a mail recipient rather than a document. */
    public static boolean isMailAddress(final String address) {

        if (address == null) {
            return false;
        }

        return address.trim().toLowerCase(Locale.ROOT).startsWith(MAIL_SCHEME);
    }

    /**
     * Opens the address, or logs why it could not be opened.
     *
     * <p>A failure here is never worth terminating anything: the operator asked for a
     * web page, not for a state change.</p>
     *
     * @param address an http/https address or a mailto address
     */
    public static void open(final String address) {

        if (address == null || address.isBlank()) {
            return;
        }

        try {
            URI target = URI.create(address.trim());

            if (!Desktop.isDesktopSupported()) {
                LOGGER.log(Level.WARNING, "No desktop integration available for {0}", address);
                return;
            }

            Desktop desktop = Desktop.getDesktop();

            if (isMailAddress(address)) {
                desktop.mail(target);
            } else {
                desktop.browse(target);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not open " + address, e);
        }
    }
}
```

- [ ] **Schritt 4: Tests laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*ExternalDocumentsTest*'
```

Erwartung: 5 Tests grün.

- [ ] **Schritt 5: `showWithLink` in `ComposeAlert.kt` ergänzen**

In `ComposeAlert` hinzufügen:

```kotlin
    /**
     * Shows a notice with one clickable line below the text, and returns at once.
     *
     * Only `onSimpleLogFileCreated` needs this; the JavaFX version put a Hyperlink into
     * the dialog pane. Kept as its own entry point rather than a nullable parameter on
     * [show], so the ordinary notice stays a three-argument call.
     */
    @JvmStatic
    @JvmOverloads
    fun showWithLink(
        title: String,
        header: String?,
        body: String,
        linkText: String,
        linkTarget: String,
        darkMode: Boolean = false,
    ) {
        EventQueue.invokeLater {
            val dialog = ComposeDialog(null, Dialog.ModalityType.MODELESS)
            dialog.title = title
            dialog.size = DIALOG_SIZE
            dialog.setLocationRelativeTo(null)
            dialog.isAlwaysOnTop = true
            dialog.setContent {
                Kst4ContestTheme(darkMode = darkMode, baseFontSizeSp = DIALOG_FONT_SIZE_SP) {
                    AlertContent(
                        header = header,
                        body = body,
                        buttons = alertButtons("OK", null),
                        onPressed = { dialog.dispose() },
                        link = linkText to linkTarget,
                    )
                }
            }
            dialog.isVisible = true
        }
    }
```

und `AlertContent` um den optionalen Verweis erweitern:

```kotlin
@Composable
private fun AlertContent(
    header: String?,
    body: String,
    buttons: List<AlertButton>,
    onPressed: (AlertButton) -> Unit,
    link: Pair<String, String>? = null,
) {
```

sowie innerhalb des scrollenden `Column`, nach dem `Text(body, ...)`:

```kotlin
                if (link != null) {
                    Text(
                        link.first,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { ExternalDocuments.open(link.second) },
                    )
                }
```

Zusätzliche Importe in `ComposeAlert.kt`: `androidx.compose.foundation.clickable` und
`kst4contest.view.ExternalDocuments`.

- [ ] **Schritt 6: Die fünf nicht-blockierenden `Alert`-Stellen umstellen**

`alertWindowEvent` (statisch, wird auch von `ApplicationRuntimeLauncher` gerufen — die
Entwurfsfarbe ist dort nicht erreichbar, also bleibt der Standardwert):

```java
	public static void alertWindowEvent(String warning) {
		System.out.println("Alert due to ... " + warning);
		ComposeAlert.show("WARNING", null, warning);
	}
```

`onSimpleLogFileCreated` — der Textkörper bleibt Wort für Wort erhalten, der `Hyperlink` wird die
Verweiszeile:

```java
	@Override
	public void onSimpleLogFileCreated(Path filePath) {
		ComposeAlert.showWithLink(
				"Simplelogfile created",
				"The selected Simplelogfile did not exist and has been created",
				"File: " + filePath + "\n\n"
						+ "First check whether you need the Simplelogfile integration. If your logging "
						+ "application provides a supported network interface, use that interface for "
						+ "band and locator information.\n\n"
						+ "If you use Simplelogfile, configure your logging application to write its live log to this file. "
						+ "Then log a test QSO and verify that the callsign is marked as worked "
						+ "in KST4Contest within one minute.\n\n"
						+ "Before each contest, verify that the logging application writes the current "
						+ "contest log to this exact file. KST4Contest does not reset Simplelogfile-derived "
						+ "Worked marks automatically when a new contest starts.",
				"Open the Simplelogfile manual",
				SIMPLE_LOG_MANUAL_URL,
				chatcontroller.getChatPreferences().isGUI_darkModeActive());
	}
```

`menuActionConnect` — beide Zweige:

```java
	void menuActionConnect() {
		String call = chatcontroller.getChatPreferences().getStn_loginCallSign();
		String pass = chatcontroller.getChatPreferences().getStn_loginPassword();
		boolean darkMode = chatcontroller.getChatPreferences().isGUI_darkModeActive();

		if (call == null || call.isBlank() || pass == null || pass.isBlank()) {
			ComposeAlert.show(
					"Cannot connect",
					"Login credentials missing",
					"Please configure your callsign and password in Settings first.",
					darkMode);
			return;
		}

		try {
			chatcontroller.execute();
		} catch (InterruptedException | IOException e) {
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception", e);
			ComposeAlert.show(
					"Connection failed",
					null,
					"Could not connect: " + e.getMessage(),
					darkMode);
		}
	}
```

`menuActionShowAbout`:

```java
	void menuActionShowAbout() {
		ComposeAlert.show(
				"About kst4contest",
				"kst4Contest " + ApplicationConstants.APPLICATION_CURRENT_VERSION
						+ ": ON4KST Chatclient by DO5AMF and DN9APW",
				chatcontroller.getChatPreferences().getProgramVersion(),
				chatcontroller.getChatPreferences().isGUI_darkModeActive());
	}
```

- [ ] **Schritt 7: Die tote `showUserInputErrorWindow` löschen**

Die statische Methode um `:454` hat keinen Aufrufer mehr (geprüft über das ganze Modul). Sie fällt
samt ihrem `Alert`.

- [ ] **Schritt 8: Die sieben `getHostServices()`-Stellen umstellen**

Jeweils `getHostServices().showDocument(X)` → `ExternalDocuments.open(X)`:
`menuActionOpenDonationPage`, `menuActionOpenHomepage`, `menuActionOpenNewsgroup`,
`menuActionOpenOv3tDonationPage`, `menuActionContactAuthor`, die Stelle um `:4000`
(`address -> runOnUi(() -> getHostServices().showDocument(address))` →
`address -> runOnUi(() -> ExternalDocuments.open(address))`) und die um `:4413`. Die Stelle in
`onSimpleLogFileCreated` ist mit Schritt 6 bereits weg.

- [ ] **Schritt 9: Bauen, Testzahl prüfen, GUI-ABNAHME**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
./gradlew :app-desktop:run
```

Erwartung: `tests=813 failures=0 errors=0`. In der Anwendung: Info → Über, Info → Spende,
Info → Autor kontaktieren (Mailprogramm, nicht Browser), Verbinden ohne hinterlegtes Rufzeichen
(Hinweis erscheint), QRZ-Verweis im Stationsbereich.

- [ ] **Schritt 10: Committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/ExternalDocuments.java \
        app-desktop/src/test/java/kst4contest/view/ExternalDocumentsTest.java \
        app-desktop/src/main/kotlin/kst4contest/view/compose/ComposeAlert.kt \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Replace the remaining alerts and HostServices with Compose and AWT"
```

---

## Aufgabe 6: Bildschirmgröße über AWT

**Dateien:**
- Anlegen: `app-desktop/src/main/java/kst4contest/view/ScreenBounds.java`
- Anlegen: `app-desktop/src/test/java/kst4contest/view/ScreenBoundsTest.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` (um `:3978`)

**Schnittstellen:**
- Liefert: `ScreenBounds.availableHeight()` und `ScreenBounds.availableWidth()`, beide `double`.
- `MainWindowFrame.INSTANCE.startupSize(storedSize, screenHeight, screenWidth)` bleibt unverändert;
  nur woher die beiden Zahlen kommen, ändert sich.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```java
package kst4contest.view;

import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class ScreenBoundsTest {

    @Test
    void theAvailableAreaIsPositive() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");

        assertTrue(ScreenBounds.availableWidth() > 0);
        assertTrue(ScreenBounds.availableHeight() > 0);
    }

    @Test
    void headlessFallsBackToASizeTheWindowCanActuallyUse() {
        // Packaging and CI run headless. Returning zero would hand Compose a 0x0 window.
        assertEquals(ScreenBounds.FALLBACK_WIDTH, ScreenBounds.widthOf(null));
        assertEquals(ScreenBounds.FALLBACK_HEIGHT, ScreenBounds.heightOf(null));
    }

    @Test
    void aKnownBoundIsPassedThrough() {
        java.awt.Rectangle bounds = new java.awt.Rectangle(0, 0, 2560, 1400);

        assertEquals(2560.0, ScreenBounds.widthOf(bounds));
        assertEquals(1400.0, ScreenBounds.heightOf(bounds));
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*ScreenBoundsTest*'
```

- [ ] **Schritt 3: `ScreenBounds` schreiben**

```java
package kst4contest.view;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * The screen area a window may actually use.
 *
 * <p>Replaces {@code javafx.stage.Screen.getPrimary().getVisualBounds()}.
 * {@code GraphicsEnvironment.getMaximumWindowBounds()} is the AWT equivalent: it excludes
 * task bars and docks, which is why the JavaFX code used the visual bounds rather than the
 * raw bounds.</p>
 */
public final class ScreenBounds {

    /** Used when there is no display at all, so a window still gets a usable size. */
    public static final double FALLBACK_WIDTH = 1234.0;

    /** Used when there is no display at all, so a window still gets a usable size. */
    public static final double FALLBACK_HEIGHT = 768.0;

    private ScreenBounds() {
        // Utility class.
    }

    /** The usable width of the primary screen, or {@link #FALLBACK_WIDTH} without one. */
    public static double availableWidth() {
        return widthOf(maximumWindowBounds());
    }

    /** The usable height of the primary screen, or {@link #FALLBACK_HEIGHT} without one. */
    public static double availableHeight() {
        return heightOf(maximumWindowBounds());
    }

    static double widthOf(final Rectangle bounds) {
        return bounds == null || bounds.width <= 0 ? FALLBACK_WIDTH : bounds.width;
    }

    static double heightOf(final Rectangle bounds) {
        return bounds == null || bounds.height <= 0 ? FALLBACK_HEIGHT : bounds.height;
    }

    private static Rectangle maximumWindowBounds() {

        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }

        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }
}
```

- [ ] **Schritt 4: Die Aufrufstelle umstellen**

Um `:3978`, in der Methode, die das Compose-Hauptfenster öffnet:

```java
		double[] storedSize = chatcontroller.getChatPreferences().getGUIscn_ChatwindowMainSceneSizeHW();
		MainWindowSize size = MainWindowFrame.INSTANCE.startupSize(
				storedSize, ScreenBounds.availableHeight(), ScreenBounds.availableWidth());
```

Die Zeile `javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();` fällt.

- [ ] **Schritt 5: Die beiden toten Bildschirmhelfer löschen**

`getScreenAwareMainSceneSizeHW(double[])` und `ensureStageFitsPrimaryScreen(Stage)` haben keinen
Aufrufer mehr. Beide löschen, zusammen mit `GuiUtils.applyApplicationIcon` **nur dann**, wenn nach
Aufgabe 9 kein `primaryStage` mehr existiert — das gehört nach Aufgabe 10, hier nur die beiden
Bildschirmhelfer.

- [ ] **Schritt 6: Bauen, prüfen, committen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
git add app-desktop/src/main/java/kst4contest/view/ScreenBounds.java \
        app-desktop/src/test/java/kst4contest/view/ScreenBoundsTest.java \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Take the startup window size from AWT instead of javafx.stage.Screen"
```

Erwartung: `tests=816 failures=0 errors=0`.

---

## Aufgabe 7: Die lebenden `Platform`-Aufrufe auf den Dispatcher umhängen

Noch zeigt der Dispatcher auf JavaFX — diese Aufgabe ändert kein Verhalten, sie führt die
Zustellung nur vollständig über die Schnittstelle. Das ist die Voraussetzung dafür, dass der
Tausch in Aufgabe 9 **eine** Reihenfolge behält statt zweier Fäden.

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/LayoutAutosave.java`

- [ ] **Schritt 1: Die Stellen in `Kst4ContestApplication` umschreiben**

Alle `Platform.runLater(X)` werden `uiDispatcher.runOnUi(X)`, alle
`Platform.isFxApplicationThread()` werden `uiDispatcher.isUiThread()`. Betroffen sind nach
heutigem Stand (nach Methodennamen suchen, nicht nach Zeilennummern):

| Methode | Was dort steht |
|---|---|
| `applySelectedCallSignInfoFilter` | `Platform.runLater(() -> { … selectedStationMessagesFeed.push(…) })` |
| die Stelle mit dem Fokus-Kommentar um `:1132` | `Platform.runLater(() -> { txt_chatMessageUserInput.requestFocus(); … })` |
| `updateTimelineVisuals` (Wächter um `:1665`) | `if (!Platform.isFxApplicationThread()) { Platform.runLater(this::updateTimelineVisuals); return; }` |
| `updateConnectionStateIndicator` (Wächter um `:2104`) | dito, mit `updateConnectionStateIndicator(state, detail)` |
| `showBlinkingBandUpgradeIndicator` um `:2195` | `Platform.runLater(() -> showBlinkingBandUpgradeIndicator(…))` |
| der Sked-Erinnerer in `start(…)` um `:2845` | `Platform.runLater(() -> showBlinkingSkedWarnIndicator(text))` |
| das Ende von `start(…)` um `:2898` | `Platform.runLater(this::updateTimelineVisuals)` |
| `onThreadStateChanged`-Umfeld um `:3172`, `:3177`, `:3196` | drei `Platform.runLater(…)` |
| die Stelle um `:3303` | `Platform.runLater(() -> { … forceRedraw() … })` |
| die Stelle um `:4179` | `Platform.runLater(() -> { … selectedStationMessagesFeed.push(…) })` |
| die Stelle um `:4988` mit dem anonymen `Runnable` | `Platform.runLater(new Runnable() { … })` |

**Nicht** anfassen: `Platform.exit()` um `:2439` und `Platform.setImplicitExit(false)` um `:2786` —
die gehören zum Lebenszyklus und fallen in Aufgabe 10.

Der Kommentar um `:4982` („Platform.runLater() resets the flag after the current JavaFX event
cycle") beschreibt danach das Falsche. Er wird zu: „Handing the reset to the user-interface thread
lets the current event cycle finish first, so a …". Die Aussage bleibt, der Toolkit-Name
verschwindet.

Ebenso der Kommentar um `:1128` („on purpose and not uiDispatcher: the dispatcher runs the task
inline when …"). **Diesen zuerst lesen** — er begründet ausdrücklich, warum dort *nicht* der
Dispatcher steht. Trifft die Begründung weiter zu, bleibt die Stelle wie sie ist und bekommt nur
einen Zusatz, dass sie bewusst an `Platform` hängt; dann wandert sie in Aufgabe 10 mit auf
`EventQueue.invokeLater`. Trifft sie nicht mehr zu, wird sie hier mit umgestellt und der Kommentar
gelöscht. **Diese Entscheidung in der Commit-Nachricht festhalten.**

- [ ] **Schritt 2: `LayoutAutosave` umhängen**

`LayoutAutosave.requestSave` hat heute `Platform.isFxApplicationThread()`/`Platform.runLater`
fest eingebaut. Die Klasse bekommt den Dispatcher über den Konstruktor, statt das Toolkit zu
kennen:

```java
    private final UiDispatcher uiDispatcher;

    public LayoutAutosave(final ChatPreferences preferences, final UiDispatcher uiDispatcher) {
        this.preferences = preferences;
        this.uiDispatcher = uiDispatcher;
    }
```

und in `requestSave`:

```java
        if (!uiDispatcher.isUiThread()) {
            uiDispatcher.runOnUi(this::requestSave);
            return;
        }
```

Die einzige Erzeugungsstelle ist `Kst4ContestApplication` um `:2808`:
`layoutAutosave = new LayoutAutosave(chatcontroller.getChatPreferences(), uiDispatcher);`.
Einen Test für `LayoutAutosave` gibt es nicht (geprüft: `grep -rn "new LayoutAutosave" app-desktop/src`
findet nur diese eine Zeile), also ist kein weiterer Aufrufer anzupassen.

- [ ] **Schritt 3: Bauen und Testzahl prüfen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
```

Erwartung: `tests=816 failures=0 errors=0`, unverändert.

- [ ] **Schritt 4: Committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        app-desktop/src/main/java/kst4contest/view/LayoutAutosave.java
git commit -m "Route the live Platform.runLater sites through the UiDispatcher"
```

---

## Aufgabe 8: Das letzte JavaFX am Profilwechsel lösen

`ApplicationRuntimeLauncher.switchProfile` ruft `nextRuntime.start(new Stage())`. `new Stage()`
wirft außerhalb des JavaFX-Fadens — das ist die Stelle, die den Tausch in Aufgabe 9 sonst sofort
zerlegt. Sie fällt hier, noch mit laufendem JavaFX, damit der Tausch allein für sich steht.

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java`

**Schnittstellen:**
- Liefert: `Kst4ContestApplication.startRuntime()` ohne Parameter — der gesamte heutige Rumpf von
  `start(Stage)`, ohne die vier Bühnenzeilen. Aufgabe 10 macht daraus den einzigen Einstieg.
- `start(Stage)` bleibt in dieser Aufgabe als `@Override` bestehen und ruft nur noch
  `startRuntime()`, nachdem es die Bühne gesetzt hat.

- [ ] **Schritt 1: `start(Stage)` aufteilen**

```java
	@Override
	public void start(Stage primaryStage) throws InterruptedException, IOException, URISyntaxException {
		/*
		 * Still here only because this class still extends Application. The stage is not
		 * shown and carries nothing but the application icon; stage 5 removes both.
		 */
		ownPrimaryStage = primaryStage;
		Platform.setImplicitExit(false);
		primaryStage.setOnCloseRequest(closeRequest -> {
			closeRequest.consume();
			ApplicationRuntimeLauncher.exitApplication();
		});
		GuiUtils.applyApplicationIcon(primaryStage);

		startRuntime();
	}

	/**
	 * Builds the running application: profile, chat controller, listeners and windows.
	 *
	 * <p>Toolkit-free on purpose. A profile switch calls this on a fresh instance, and it
	 * must not need a JavaFX stage to do so.</p>
	 */
	public void startRuntime() throws InterruptedException, IOException, URISyntaxException {

		if (!resolveOperatorProfileIfRequired()) {
			return;
		}

		ApplicationRuntimeLauncher.setCurrent(this);

		// … the rest of today's start(Stage) body, unchanged, from
		// ApplicationFileUtils.copyResourceIfRequired(…) down to the final
		// uiDispatcher.runOnUi(this::updateTimelineVisuals);
	}
```

Der `resolveOperatorProfileIfRequired`-Wächter und `setCurrent(this)` wandern mit nach
`startRuntime()`, weil der Profilwechsel beides braucht.

- [ ] **Schritt 2: `switchProfile` ohne Bühne**

In `ApplicationRuntimeLauncher`:

```java
        try {
            nextRuntime.startRuntime();
        } catch (Exception e) {
```

Der Import `javafx.stage.Stage` fällt damit aus `ApplicationRuntimeLauncher`.

- [ ] **Schritt 3: Bauen und GUI-ABNAHME**

```bash
./gradlew clean build && ./gradlew :app-desktop:run
```

Zu prüfen: Profilwechsel über Menü **und** über die Einstellungen → Profile. Nach dem Wechsel
müssen Hauptfenster, Einstellungsfenster und Monitorfenster auf dem neuen Profil stehen, und die
Verbindung muss sich neu aufbauen lassen.

- [ ] **Schritt 4: Committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java
git commit -m "Split startRuntime out of start(Stage) so a profile switch needs no stage"
```

---

## Aufgabe 9: Der Tausch — **GUI-ABNAHME**

Eine Zeile. Hier und nur hier ändert sich, auf welchem Faden die Oberfläche läuft.

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` (um `:204`)
- Löschen: `app-desktop/src/main/java/kst4contest/view/JavaFxUiDispatcher.java`

- [ ] **Schritt 1: Vor der Löschung das Durchlauf-Skript laufen lassen**

```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py
```

- [ ] **Schritt 2: Den Dispatcher tauschen**

```java
	/** Hands worker-thread results over to the AWT event dispatch thread, where Compose draws. */
	private final UiDispatcher uiDispatcher = new AwtUiDispatcher();
```

Die Javadoc von `runOnUi(Runnable)` direkt darunter spricht noch vom JavaFX-Faden — mit anpassen.

- [ ] **Schritt 3: `JavaFxUiDispatcher` löschen**

```bash
git rm app-desktop/src/main/java/kst4contest/view/JavaFxUiDispatcher.java
```

- [ ] **Schritt 4: Bauen und Testzahl prüfen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
```

Erwartung: `tests=816 failures=0 errors=0`.

- [ ] **Schritt 5: GUI-ABNAHME durch Marc — die gründlichste dieses Plans**

```bash
./gradlew :app-desktop:run
```

Zu prüfen, weil jede dieser Bahnen über den getauschten Dispatcher läuft:
- Verbinden, Stationsliste füllt sich und aktualisiert sich laufend
- Eine Station auswählen; der Stationsbereich und die Nachrichtentabs folgen
- Private Nachricht senden und empfangen
- Die Verbindungsanzeige und die drei Statusanzeigen wechseln ihre Farbe
- Sked anlegen; Zeitstrahl und Erinnerungsstreifen erscheinen
- Bandwechsel-Hinweis und Sked-Warnung
- Karte öffnen, Station anzeigen, schließen
- Monitorfenster öffnen und schließen
- Entwurf hell/dunkel umschalten; alle offenen Fenster folgen
- Spaltenbreiten ziehen, Anwendung beenden und neu starten — die Breiten müssen stehen

**Das ist die Etappe, in der ein Fehler sich als sporadisch stehende Oberfläche zeigt, nicht als
Ausnahme.** Lieber eine Sitzung lang betreiben als fünf Minuten klicken.

- [ ] **Schritt 6: Committen**

```bash
git add -A app-desktop/src/main/java/kst4contest/view/
git commit -m "Move UI delivery to the AWT event thread and delete the JavaFX dispatcher"
```

---

## Aufgabe 10: Lebenszyklus ohne `javafx.application.Application` — **GUI-ABNAHME**

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Main.java`
- Ändern: `app-desktop/src/main/java/kst4contest/view/GuiUtils.java`

- [ ] **Schritt 1: `init()`, `start(Stage)` und `stop()` entfernen**

`init()` las die Kommandozeile über `getParameters()`. Das übernimmt `main`:

```java
	public static void main(String[] args) {
		setupFileLogging();

		CommandLineOptions.remember(CommandLineOptions.parse(
				args == null ? null : java.util.List.of(args)));

		try {
			new Kst4ContestApplication().startRuntime();
		} catch (Exception startupProblem) {
			LOGGER.log(java.util.logging.Level.SEVERE, "Could not start KST4Contest", startupProblem);
			ComposeAlert.acknowledge(
					"KST4Contest",
					"KST4Contest could not be started.",
					String.valueOf(startupProblem.getMessage()));
			System.exit(1);
		}
	}
```

`start(Stage)` fällt ganz; sein Rest (die vier Bühnenzeilen aus Aufgabe 8) fällt mit. `stop()`
fällt ebenfalls — es wurde nur von JavaFX gerufen. Damit die Mittel auch bei einem Abbruch von
außen freigegeben werden, bekommt `startRuntime()` am Ende:

```java
		/*
		 * Replaces Application.stop(), which only JavaFX ever called. Covers a SIGTERM and
		 * a closing terminal; the ordinary exit still goes through
		 * ApplicationRuntimeLauncher.exitApplication().
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownRuntime, "kst4contest-shutdown"));
```

Die Klassendeklaration verliert `extends Application`:

```java
public class Kst4ContestApplication implements StatusUpdateListener, SettingsHost {
```

- [ ] **Schritt 2: Der Prozess muss am Leben bleiben**

`launch(args)` blockierte bisher. Danach hält ihn offen, dass `ComposeWindowHost.show` je Fenster
einen gewöhnlichen (nicht-Daemon-)`Thread` startet und `startRuntime()` vor seiner Rückkehr
`openSettingsWindow()` und `openComposeMainWindowIfRequested()` ruft. **Das ist eine Annahme, die
zu prüfen ist**, nicht eine, die man glaubt: nach Schritt 5 muss die Anwendung ohne Terminal
weiterlaufen. Tut sie das nicht, bekommt `main` am Ende ein `Thread.currentThread().join()` nach
dem Start — dann aber mit einem Kommentar, der sagt warum.

- [ ] **Schritt 3: `Platform.exit()` entfernen**

In `ApplicationRuntimeLauncher.exitApplication()` und `.switchProfile(...)` fällt je ein
`Platform.exit();`; `System.exit(…)` bleibt. In `resolveOperatorProfileIfRequired` ebenso. Damit
verliert `ApplicationRuntimeLauncher` seinen letzten JavaFX-Import.

In `Kst4ContestApplication` fällt `Platform.setImplicitExit(false)` — es gab es nur, weil JavaFX
den Prozess beim letzten geschlossenen Fenster beendet hätte. Der Kommentar darüber beschreibt
genau das und fällt mit.

- [ ] **Schritt 4: `ownPrimaryStage` und das Anwendungssymbol**

`ownPrimaryStage` fällt samt der `Stage[]`-Schleife in `closeOwnedStages()`; die Methode behält nur
noch die Compose-Fenster. `GuiUtils.applyApplicationIcon(Stage)` verliert seinen letzten Aufrufer —
die Compose-Fenster setzen ihr Symbol selbst über `applicationIcon()` in `ComposeWindowHost.kt`.
Die Methode aus `GuiUtils` löschen; `GuiUtils.isCallSignSyntax` bleibt, es wird von
`NotificationTabState.kt` benutzt. Die dadurch frei werdenden JavaFX-Importe aus `GuiUtils`
entfernen. `GuiUtils.triggerUpdate` mit seinem `Platform`-Paar hat nur noch einen auskommentierten
Aufrufer in `ReadUDPbyUCXMessageThread` — **nicht löschen**, nur die beiden `Platform`-Zeilen auf
`EventQueue` umstellen; das Löschen toter Methoden ist Etappe 6 und braucht den Lebendbefund.

- [ ] **Schritt 5: Bauen, starten, GUI-ABNAHME**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
./gradlew :app-desktop:run
```

Zu prüfen: Start ohne Argumente; Start mit `--profile <name>`; Start mit einem Profilnamen, den es
nicht gibt (Startwarnung); Profilwechsel; Beenden über Menü → Exit; Beenden über das
Fensterkreuz des Hauptfensters; Anwendungssymbol in der Fensterleiste; die Anwendung läuft weiter,
nachdem das startende Terminal geschlossen wurde.

- [ ] **Schritt 6: Committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/
git commit -m "Start the application from a plain main instead of javafx Application"
```

---

## Aufgabe 11: Aufräumen und Bilanz

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Ändern: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Schritt 1: Verwaiste Importe entfernen**

Nur die, die durch diesen Plan frei geworden sind. Kandidaten:
`javafx.application.Application`, `javafx.application.Platform`, `javafx.stage.Stage`,
`javafx.stage.Screen`, `javafx.stage.Window`, `javafx.stage.WindowEvent`,
`javafx.stage.FileChooser` (war schon vorher unbenutzt), `javafx.scene.control.Alert.AlertType`.
Der Übersetzer sagt, welche wirklich frei sind — raten verbietet sich, die Datei benutzt
`javafx.scene.control.*`.

```bash
./gradlew :app-desktop:compileJava
```

- [ ] **Schritt 2: Bilanz ziehen**

```bash
grep -c "^import javafx" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
grep -rl "javafx\." core/src/main app-desktop/src/main | sort
wc -l app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Zielwert für die Dateiliste: höchstens noch die Etappe-6-Dateien, also
`ComposeMenuActions`, `FxRosterBinding`, `GuiUtils` (falls Restimporte), `Kst4ContestApplication`,
`LayoutAutosave` (sollte leer sein), `map/PathProfileChart`, `map/StationMapBridge`,
`map/StationMapView`, `MessageTextTableCell`, `TableLayoutManager`, `TimelineView`,
`TruncatedTextTableCell`, `view/compose/JavaFxStylesheet.kt`, `test/MessageVariableResolverTest`.
`ApplicationRuntimeLauncher` und `JavaFxUiDispatcher` müssen **verschwunden** sein.

- [ ] **Schritt 3: Lebendbefund fortschreiben**

Oben in `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md` eine Abschlussnotiz für Teil 4
eintragen: das Sondenergebnis aus Aufgabe 1, die Entscheidung aus Aufgabe 7 Schritt 1 zum
Kommentar bei `:1128`, die neue Dateiliste und die Testzahl.

- [ ] **Schritt 4: Abschlussbau und Commit**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*failures="\([0-9]*\)".*errors="\([0-9]*\)".*/\1 \2 \3/' | awk '{t+=$1; f+=$2; e+=$3} END {print "tests="t" failures="f" errors="e}'
git add -A
git commit -m "Drop the JavaFX imports freed by the dispatcher and lifecycle stage"
```

---

## Was die Spezifikation für diese Etappen nennt und schon erledigt ist

Drei Punkte aus Etappe 5 der Spezifikation kommen in diesem Plan nicht als Aufgabe vor, weil sie
seit dem Schreiben der Spezifikation durch Teil 3 beziehungsweise die Kartenetappe erledigt sind.
Beim Lesen soll das nicht wie eine Lücke aussehen:

- **Das Kandidatenfenster** (in der Spezifikation „die einzige JavaFX-Bühne, die tatsächlich
  erscheint", `:4855`–`:4860`) ist ein Compose-Fenster:
  `app-desktop/src/main/kotlin/kst4contest/view/compose/TopPriorityCandidatesWindow.kt`. Weder
  `Scene` noch `stage.show()` stehen dort noch.
- **Die macOS-Systemmenüleiste.** `installSharedSystemMenuBar` und `initMenuBar` sind mit Teil 3
  gefallen (Commit `319daeaf`); die Menüleiste liegt in `MainMenuBar.kt`. Erhalten bleibt nur die
  reine Zeichenkettenfunktion `macOsConnectionStateMenuTitle` samt ihrem Test — macOS-Verhalten
  ist auf Linux nicht entscheidbar, und die Spezifikation hält das offen.
- **Die Karte.** `menuActionToggleStationMap` ruft `StationMapWindow.INSTANCE.toggle(...)`, also
  Compose. `StationMapView`/`StationMapBridge` haben keinen lebenden Aufrufer mehr und werden in
  Etappe 6 gelöscht, nicht hier.

## Was unverändert bleibt

- Die Fachlogik in `core` und sämtliche Protokollformate. `core` bekommt in diesem Plan keine
  einzige Änderung.
- Die Schnittstelle `UiDispatcher` und `DirectUiDispatcher`.
- Der Wortlaut jeder Dialogmeldung. Umgestellt wird die Technik, nicht der Text.
- Die Ablaufsteuerung: was heute blockiert, blockiert nachher.
- `ObservableRoster` ist der Speicher, die Listen sind Spiegel; kein Worker-Faden fasst
  UI-Sammlungen an; Zoom, Auswahl, Fokus, Sortierung und vorbelegter Text ändern sich nicht als
  Nebeneffekt.
- Die 91 MP3-Dateien und die CW-Ausgabe in `PlayAudioUtils`.
- Der `org.openjfx`-Block im Build — JavaFX bleibt bis Etappe 6 auf dem Klassenpfad.

## Bekannte Fallstricke

- **`ComposeStationMapDrawingTest` ist flaky.** Flackert unter paralleler Build-Last sporadisch
  rot. Bei einem roten Lauf zuerst isoliert nachprüfen, bevor man eine echte Regression annimmt.
- **PMD/SpotBugs** melden rund 4448 vorbestehende Befunde in `app-desktop:main` und brechen den
  Build nicht. Nur neu hinzugekommene melden.
- **Nach jeder GUI-ABNAHME-Aufgabe anhalten.** Die beiden Fehler der letzten Runde — eine
  `NullPointerException` und ein totes Menü — waren im grünen Build unsichtbar.
