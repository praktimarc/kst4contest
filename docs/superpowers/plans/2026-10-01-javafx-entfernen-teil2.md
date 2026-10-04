# JavaFX entfernen, Teil 2: Entflechten, bis der tote Aufbau wirklich tot ist

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die vier Stellen, an denen der nie angezeigte JavaFX-Aufbau die Compose-Oberfläche speist, aus ihm herauslösen — und die eine Scheibe löschen, deren Unschädlichkeit beweisbar ist.

**Architecture:** Jede Zuführung bekommt eine eigene, testbare Einheit, die den Compose-Zustand füllt und nichts von JavaFX weiß. Danach ruft der JavaFX-Aufbau sie nur noch, statt sie zu besitzen, und kann in Teil 3 fallen, ohne etwas mitzunehmen. Das Kandidatenfenster wird nicht entflochten, sondern portiert: es ist die einzige `Stage`, die dieser Quelltext selbst erzeugt.

**Tech Stack:** Java 21, Kotlin 2.2, Gradle, JUnit 5, Compose Multiplatform 1.8.2, JavaFX 21 (noch vorhanden).

**Spec:** `docs/superpowers/specs/2026-10-01-javafx-entfernen-design.md`

**Befund, auf dem jede Aufgabe aufsetzt:** `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

## Warum nur das

Die Spezifikation hat sechs Etappen. Dieser Plan ist der erste Teil von Etappe 2 und
deckt die fünf Entflechtungen plus die erste Löschscheibe ab. Die restlichen sieben
Löschscheiben hängen daran, dass diese fünf gelandet sind; `UiDispatcher`,
Lebenszyklus, die zehn Dialoge und der Build sind Etappe 4 bis 6.

Nach diesem Plan ist der tote Aufbau zum ersten Mal wirklich tot — vorher ist jede
Löschung ein Risiko, das kein Test und kein Übersetzer abfängt.

## Zwei Dinge, die beim Schreiben dieses Plans fehlten

1. **Fünf Beobachtungen am laufenden Programm** aus Teil 1, Aufgabe 4, stehen aus
   (Abschnitt „Was hier nicht geprüft werden konnte" im Befund). Betroffen ist vor
   allem Aufgabe 2: das Kandidatenfenster wird aus dem Quelltext nachgebaut, nicht aus
   der Anschauung. Der Quelltext ist vollständig gelesen und in Aufgabe 2 wiedergegeben,
   aber niemand hat das alte Fenster dabei gesehen.
2. **macOS.** `installSharedSystemMenuBar` steigt auf Linux bei `:6075` sofort aus. Was
   dort an der JavaFX-Menüleiste sichtbar hängt, entscheidet Aufgabe 6 nicht.

## Global Constraints

- Kommunikation mit dem Benutzer auf Deutsch; Quelltextkommentare und Javadoc
  ausschließlich auf Englisch (`AGENTS.md`).
- Keine neue Produktionsabhängigkeit ohne vorherige Freigabe (`AGENTS.md`).
- Kein Push, Merge, Tag oder Release. Die Commit-Schritte unten sind durch die Freigabe
  dieses Plans gedeckt, nichts darüber hinaus.
- **Nicht** aus Exit-Code 0 auf „alle Tests grün" schließen — die XML-Ergebnisse unter
  `*/build/test-results/` lesen (`AGENTS.md`).
- Ausgangslage: `./gradlew clean build` → exit 0, 769 Tests, 0 Fehler.
- Signierte Commits sind konfiguriert. Schlägt das Signieren fehl, läuft der
  Bitwarden-Agent nicht; das ist kein Grund, unsigniert zu committen.
- `ObservableRoster` ist der Speicher, die Listen sind Spiegel. Kein Worker-Faden fasst
  UI-Sammlungen an. Zustellung nur über `UiDispatcher` (`AGENTS.md`).
- Zoom, Auswahl, Fokus, Sortierung und vorbelegter Text ändern sich nicht als
  Nebeneffekt (`AGENTS.md`).
- **Vor jeder Löschung** den Durchlauf laufen lassen:
  `python3 docs/superpowers/notes/javafx-compose-sweep.py`.

## Review Focus

Fünf Dinge, die die Spezifikation voraussetzt, die aber keine Aufgabe hier von sich aus
prüft, nach Wahrscheinlichkeit geordnet:

1. **Eine Zuführung feuert nach dem Umbau nicht mehr, und nichts merkt es.** Genau das
   ist die Fehlerklasse, die Teil 1 gefunden hat: kein Übersetzungsfehler, kein
   Testfehler, nur eine leere Tabelle. Jede der Aufgaben 3, 4 und 5 bekommt deshalb
   einen Test, der den Compose-Zustand **nach** dem Füllen prüft, nicht nur das Mapping.
2. **Eine Zuführung feuert doppelt.** Bleibt der alte Aufruf stehen und kommt der neue
   dazu, schreiben zwei Wege denselben Zustand — unsichtbar, bis einer veraltete Daten
   liefert. Die Aufgaben 3, 4 und 5 prüfen deshalb mit einem Zähler, dass genau einmal
   geschrieben wird.
3. **Leere oder fehlende Eingabe.** `topCandidates()` kann leer sein, `getQrb()` kann
   `null` sein, ein `ChatMember` kann ohne Rufzeichen kommen. Aufgabe 2 und 3 prüfen
   den leeren und den unvollständigen Fall.
4. **Zustellung vom falschen Faden.** Die Controller-Listener feuern auf Netzfäden.
   Aufgabe 4 prüft, dass die Zuführung über `UiDispatcher` geht und nicht direkt
   schreibt.
5. **Die gelöschten Methoden waren doch erreichbar.** Aufgabe 1 prüft das vor dem
   Löschen mit einem Grep, der auch auskommentierte Aufrufe sichtbar macht.

---

## File Structure

| Datei | Verantwortung | Aufgabe |
|---|---|---|
| `app-desktop/src/main/kotlin/kst4contest/view/compose/TopPriorityCandidatesWindow.kt` | **neu** — das portierte Kandidatenfenster | 2 |
| `app-desktop/src/test/kotlin/kst4contest/view/compose/TopPriorityCandidatesWindowTest.kt` | **neu** — Beschriftung und Rendern | 2 |
| `app-desktop/src/main/kotlin/kst4contest/view/feed/TimelineFeed.kt` | **neu** — füllt `TimelineState`, weiß nichts von JavaFX | 3 |
| `app-desktop/src/test/kotlin/kst4contest/view/feed/TimelineFeedTest.kt` | **neu** | 3 |
| `app-desktop/src/main/kotlin/kst4contest/view/feed/SelectedStationMessagesFeed.kt` | **neu** — füllt `selectedStationMessages` | 4 |
| `app-desktop/src/test/kotlin/kst4contest/view/feed/SelectedStationMessagesFeedTest.kt` | **neu** | 4 |
| `app-desktop/src/main/kotlin/kst4contest/view/feed/ConnectionStateFeed.kt` | **neu** — füllt `MainWindowSurroundings` | 5 |
| `app-desktop/src/test/kotlin/kst4contest/view/feed/ConnectionStateFeedTest.kt` | **neu** | 5 |
| `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` | **ändern** — ruft die Zuführungen, besitzt sie nicht mehr | 1–6 |
| `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md` | **ändern** — abgearbeitete Punkte abhaken | 1–6 |

Ein eigenes Paket `kst4contest.view.feed`, weil diese drei Klassen dasselbe tun und
zusammen wandern werden: sie sind die Nahtstelle zwischen Controller und
Compose-Zustand und gehören nach Etappe 5 vermutlich näher an `core`.

**In Kotlin, nicht in Java.** Die Compose-Zustände haben Kotlin-Standardparameter —
`MainWindowSurroundings(menu = MainMenuState())` und `DataTableState(columns, rowKey,
tableId = "", widths = null)` —, und aus Java gibt es dafür keinen kurzen Konstruktor.
Kotlin-Tests bauen diese Zustände bereits (`MainWindowStateTest`, `TimelineStateTest`).
`Kst4ContestApplication.java` kann die Klassen unverändert rufen.

---

## Task 1: Die vier unreferenzierten Tabellen löschen

Die einzige Scheibe, bei der „der Build merkt es nicht" beweisbar statt argumentiert
ist. Sie steht voran, weil sie die Prämisse des ganzen Vorhabens billig prüft.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

**Interfaces:**
- Consumes: nichts.
- Produces: nichts. Vier Methoden verschwinden: `initShortcutTable` (`:4561`–`:4599`),
  `initNotifyAtCallSignTable` (`:5146`–`:5246`), `initTextSnippetsTable`
  (`:5248`–`:5294`), `initWkdStnTable` (`:5441`–`:5727`).

- [ ] **Step 1: Prüfen, dass wirklich niemand sie ruft — auch nicht auskommentiert**

Run:
```bash
cd /home/philipp/Projects/ham/kst4contest
for m in initShortcutTable initNotifyAtCallSignTable initTextSnippetsTable initWkdStnTable; do
  echo "--- $m"
  grep -rn "$m" --include='*.java' --include='*.kt' core/src app-desktop/src
done
```

Erwartet: für jede Methode **genau eine** Zeile, nämlich ihre eigene Deklaration in
`Kst4ContestApplication.java`. Findet der Befehl einen auskommentierten Aufruf, ist das
kein Hindernis — aber er gehört in die Commit-Nachricht, damit der nächste Leser weiß,
dass dort einmal etwas hing.

- [ ] **Step 2: Prüfen, dass es keine reflektive Hintertür gibt**

Run:
```bash
grep -cE "getDeclaredMethod|getMethod\(|Class\.forName|FXMLLoader" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: `0`. Ein Wert über 0 bedeutet, dass eine Methode über ihren Namen als
Zeichenkette erreichbar sein könnte; dann **nicht löschen**, sondern die Fundstelle
prüfen.

- [ ] **Step 3: Den Compose-Durchlauf laufen lassen**

Run: `python3 docs/superpowers/notes/javafx-compose-sweep.py`

Erwartet: die vier Methoden stehen als `clean to depth 3`. Stehen sie als
`REACHES COMPOSE`, ist der Befund veraltet — anhalten und ihn berichtigen.

- [ ] **Step 4: Die vier Methoden löschen**

Jede Methode vollständig entfernen, einschließlich ihres Javadoc-Blocks. Felder, die
nur in ihnen benutzt werden, mitnehmen — der Übersetzer zeigt sie als unbenutzt nicht
an, also nach jedem Löschen prüfen:

```bash
grep -n "tbl_shortcutTable\|tbl_notifyAtCallSignTable\|tbl_textSnippetsTable\|tbl_wkdStnTable" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Bleibt eine Fundstelle außerhalb der gelöschten Bereiche, gehört das Feld jemand
anderem und bleibt.

- [ ] **Step 5: Übersetzen und die ganze Suite laufen lassen**

Run:
```bash
./gradlew clean build --console=plain > /tmp/t1.txt 2>&1; echo "exit=$?"
python3 -c "
import glob,xml.etree.ElementTree as ET
t=f=0
for p in glob.glob('*/build/test-results/test/TEST-*.xml'):
    r=ET.parse(p).getroot(); t+=int(r.get('tests')); f+=int(r.get('failures'))+int(r.get('errors'))
print(f'tests={t} failures={f}')"
```

Erwartet: `exit=0`, `tests=769 failures=0`. Die Testzahl ändert sich **nicht** — das
ist der Punkt: 470 Zeilen weg, und kein Test hatte sie je berührt.

- [ ] **Step 6: Die Anwendung starten und bedienen**

Run: `./gradlew :app-desktop:run`

Erwartet: Sie startet, das Compose-Hauptfenster erscheint, die Stationsliste füllt sich,
und auf der Konsole steht keine `NullPointerException` und kein
`NoSuchMethodError`. Danach beenden.

Kein Test deckt diesen Pfad ab; dieser Schritt ist die einzige Prüfung, die er bekommt.

- [ ] **Step 7: Den Befund abhaken**

In `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md` unter „Reihenfolge für
Teil 2" die Scheibe 1 mit dem Datum und der Zeilenzahl als erledigt markieren:

```markdown
1. ~~Unreferenziert: `initShortcutTable`, `initNotifyAtCallSignTable`,
   `initTextSnippetsTable`, `initWkdStnTable`~~ — **erledigt am 2026-10-01**, 470 Zeilen,
   Suite unverändert bei 769 Tests.
```

- [ ] **Step 8: Commit**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete four station tables that nothing has referenced

initShortcutTable, initNotifyAtCallSignTable, initTextSnippetsTable and
initWkdStnTable each appeared exactly once in the tree - their own
declaration. No caller, commented out or otherwise, and no reflective or
FXML back door: getDeclaredMethod, getMethod(, Class.forName and
FXMLLoader appear zero times in the file.

470 lines, and the suite neither grew nor shrank: 769 tests before and
after. That is the point of starting here - it is the one slice where
'the build will not notice' is provable rather than argued, and it proves
the premise the rest of the removal rests on."
```

---

## Task 2: Das Kandidatenfenster nach Compose

Die einzige `Stage`, die dieser Quelltext selbst erzeugt (`new Stage()` bei `:4810`,
einziges Vorkommen in der Datei). Solange sie steht, kann keine JavaFX-Abhängigkeit
fallen.

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/compose/TopPriorityCandidatesWindow.kt`
- Create: `app-desktop/src/test/kotlin/kst4contest/view/compose/TopPriorityCandidatesWindowTest.kt`
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java:4806-4862` und `:10707`

**Interfaces:**
- Consumes: `kst4contest.controller.ScoreService.TopCandidate` mit
  `getDisplayCallSign(): String`, `getScore(): double`, `getCallSignRaw(): String`,
  `getPreferredChatCategory(): ChatCategory`.
  `ScoreService.topCandidates(): ObservableRoster<TopCandidate>` mit `snapshot(): List<TopCandidate>`.
- Produces:
  - `TopPriorityCandidatesWindow.show(candidates: () -> List<TopCandidate>, onPicked: (TopCandidate) -> Unit, darkMode: Boolean, baseFontSizeSp: Float)`
  - `TopPriorityCandidatesWindow.hide()`
  - `TopPriorityCandidatesWindow.applyDarkMode(darkMode: Boolean)`
  - `candidateLabel(candidate: TopCandidate): String` — `internal`, für den Test

So baut das alte Fenster (vollständig, `:4806`–`:4862`): eine `ListView` über
`topCandidates()`, Zelltext `displayCallSign + "  |  score " + "%.0f"`, Doppelklick wählt
aus und schließt, unten die Zeile „Double-click a candidate to select it.", Größe
360×500, Titel „Top priority candidates".

- [ ] **Step 1: Den Test für die Beschriftung schreiben**

```kotlin
package kst4contest.view.compose

import kst4contest.controller.ScoreService
import kst4contest.model.ChatCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The window the "more" button opens.
 *
 * Its whole content is one line per candidate, so the line is the thing worth pinning:
 * an operator reads the callsign and the score and nothing else, and the JavaFX version
 * spelled it a particular way for two releases.
 */
class TopPriorityCandidatesWindowTest {

    private fun candidate(call: String, score: Double) =
        ScoreService.TopCandidate(call, call, ChatCategory.values().first(), score)

    @Test
    fun `a candidate reads as its callsign and its score`() {
        assertEquals("PA6I  |  score 968", candidateLabel(candidate("PA6I", 968.0)))
    }

    /** The JavaFX cell used %.0f; 967.6 showed as 968, not as 967.6 or 967. */
    @Test
    fun `the score is rounded to whole points`() {
        assertEquals("DK7SE  |  score 968", candidateLabel(candidate("DK7SE", 967.6)))
        assertEquals("DK7SE  |  score 895", candidateLabel(candidate("DK7SE", 895.4)))
    }

    @Test
    fun `a score of zero still reads as a score`() {
        assertEquals("DL1ABC  |  score 0", candidateLabel(candidate("DL1ABC", 0.0)))
    }
}
```

- [ ] **Step 2: Den Test laufen lassen und scheitern sehen**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.TopPriorityCandidatesWindowTest' --console=plain 2>&1 | grep -E "^e: " | head -3`

Erwartet: `Unresolved reference 'candidateLabel'`.

Scheitert er stattdessen am Konstruktor von `TopCandidate`, dessen Signatur im Quelltext
nachsehen (`core/src/main/java/kst4contest/controller/ScoreService.java`, um `:300`) und
den Testhelfer anpassen — die Beschriftung ist der Prüfgegenstand, nicht der
Konstruktor.

- [ ] **Step 3: Das Fenster schreiben**

```kotlin
package kst4contest.view.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.controller.ScoreService
import java.util.Locale

/**
 * One line of the candidate list.
 *
 * Kept as a function rather than inlined into the row so the wording stays under a
 * test: the JavaFX cell spelled it `callsign + "  |  score " + %.0f` for two releases,
 * and an operator scanning the list reads the shape before the content.
 */
internal fun candidateLabel(candidate: ScoreService.TopCandidate): String =
    candidate.displayCallSign +
        "  |  score " +
        String.format(Locale.US, "%.0f", candidate.score)

/**
 * The full priority list, opened by the "more" button beside the two priority buttons.
 *
 * It exists so the main window can stay compact while the whole ranking is still
 * reachable. Its JavaFX predecessor was the only Stage this application constructed
 * itself, which is why it is ported before anything is deleted: no JavaFX dependency
 * can be dropped while it stands.
 */
object TopPriorityCandidatesWindow {

    private val host = ComposeWindowHost("TopPriorityCandidates")

    val isOpen: Boolean get() = host.isOpen

    fun applyDarkMode(darkMode: Boolean) = host.applyDarkMode(darkMode)

    fun hide() = host.close()

    /**
     * @param candidates read afresh while the window is open; the ranking changes as
     *        scores do, and the JavaFX list was bound to the roster for that reason
     * @param onPicked called with the chosen candidate, after which the window closes
     */
    fun show(
        candidates: () -> List<ScoreService.TopCandidate>,
        onPicked: (ScoreService.TopCandidate) -> Unit,
        darkMode: Boolean,
        baseFontSizeSp: Float,
    ) {
        host.show(
            title = "Top priority candidates",
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            widthDp = 360f,
            heightDp = 500f,
        ) { close ->
            var ranking by remember { mutableStateOf(candidates()) }

            /*
             * Polled rather than pushed. The ranking lives in the score service, which
             * is not Compose state, and this window is open for seconds at a time while
             * an operator reads it - a callback wired in and out for that is more
             * machinery than the question deserves.
             */
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(1_000)
                    ranking = candidates()
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(Modifier.fillMaxSize().padding(5.dp)) {
                    CandidateList(
                        ranking = ranking,
                        onPicked = { picked ->
                            onPicked(picked)
                            close()
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        "Double-click a candidate to select it.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun CandidateList(
        ranking: List<ScoreService.TopCandidate>,
        onPicked: (ScoreService.TopCandidate) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        if (ranking.isEmpty()) {
            Text("No candidates yet.", modifier = modifier.padding(4.dp))
            return
        }

        LazyColumn(modifier) {
            items(ranking) { candidate ->
                Text(
                    text = candidateLabel(candidate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onClick(onDoubleClick = { onPicked(candidate) }, onClick = { })
                        .padding(vertical = 3.dp, horizontal = 4.dp),
                )
            }
        }
    }
}
```

`androidx.compose.runtime.Composable` muss importiert werden; falls der Übersetzer ihn
anmahnt, `import androidx.compose.runtime.Composable` ergänzen.

- [ ] **Step 4: Den Test laufen lassen**

Run:
```bash
./gradlew :app-desktop:test --tests 'kst4contest.view.compose.TopPriorityCandidatesWindowTest' --console=plain > /tmp/t2.txt 2>&1
grep -E "^e: " /tmp/t2.txt | head -3
python3 -c "
import glob,xml.etree.ElementTree as ET
for f in glob.glob('app-desktop/build/test-results/test/TEST-*TopPriorityCandidatesWindowTest.xml'):
    r=ET.parse(f).getroot(); print(f\"tests={r.get('tests')} failures={r.get('failures')}\")
    for tc in r.iter('testcase'):
        for b in list(tc.iter('failure'))+list(tc.iter('error')): print(' ',tc.get('name'),(b.get('message') or '')[:200])"
```

Erwartet: `tests=3 failures=0`.

- [ ] **Step 5: Einen Rendertest ergänzen, der den leeren Fall einschließt**

Review Focus 3. Dasselbe Muster wie die Karten-Layouttests aus Etappe 6:

```kotlin
    @Test
    fun `the list renders with candidates and without`() {
        listOf(
            emptyList(),
            listOf(candidate("PA6I", 968.0), candidate("DK7SE", 895.0)),
        ).forEach { ranking ->
            val scene = androidx.compose.ui.ImageComposeScene(
                width = 360,
                height = 500,
                density = androidx.compose.ui.unit.Density(1f),
            ) {
                TopPriorityCandidatesWindow.CandidateListForTest(ranking) { }
            }
            try {
                scene.render()
            } finally {
                scene.close()
            }
        }
    }
```

Dafür `CandidateList` als `internal` zugänglich machen, unter dem Namen, den der Test
benutzt:

```kotlin
    /** The list on its own, so a test can render it without opening a window. */
    @Composable
    internal fun CandidateListForTest(
        ranking: List<ScoreService.TopCandidate>,
        onPicked: (ScoreService.TopCandidate) -> Unit,
    ) = CandidateList(ranking, onPicked)
```

- [ ] **Step 6: Den Rendertest laufen lassen**

Run: derselbe Befehl wie Schritt 4.

Erwartet: `tests=4 failures=0`. Wirft das Rendern, ist das ein echter Fund — die
Karten-Layouttests fangen genau diese Klasse, und dieses Fenster hatte bisher keinen.

- [ ] **Step 7: Den Aufrufer umstellen**

In `Kst4ContestApplication.java` die Methode `showTopPriorityCandidatesWindow`
(`:4806`–`:4862`) vollständig durch diesen Rumpf ersetzen:

```java
	/**
	 * Opens the complete priority candidate list in a separate window.
	 *
	 * This keeps the main UI compact while still making the full list available when
	 * the operator wants to inspect more than the first two candidates.
	 *
	 * The window is Compose. Its JavaFX predecessor was the only Stage this class
	 * constructed, and it carried the themed-scene registration and the shared system
	 * menu bar with it; both are gone with it.
	 */
	private void showTopPriorityCandidatesWindow() {
		TopPriorityCandidatesWindow.INSTANCE.show(
				() -> chatcontroller.getScoreService().topCandidates().snapshot(),
				candidate -> uiDispatcher.runOnUi(() -> selectTopCandidate(candidate)),
				chatcontroller.getChatPreferences().isGUI_darkModeActive(),
				12f
		);
	}
```

`selectTopCandidate` verliert seine beiden JavaFX-Parameter — es benutzt sie nicht, es
ruft nur `focusChatMemberAndPrepareCq(resolved)`. Signatur ändern auf
`private void selectTopCandidate(ScoreService.TopCandidate candidate)` und die Aufrufer
mitziehen: `:4761` in `selectTopCandidateAt` und der neue oben.

Den toten Aufrufer bei `:4706` (`moreButton.setOnAction(...)`) mit der Zeile löschen.
Den lebenden bei `:10707` auf die parameterlose Fassung umstellen:

```java
					runOnUi(() -> showTopPriorityCandidatesWindow());
```

- [ ] **Step 8: Das Fenster in den Design- und Profilwechsel einhängen**

In `applyTheme` (`:6111`) neben die übrigen `applyDarkMode`-Aufrufe:

```java
		TopPriorityCandidatesWindow.INSTANCE.applyDarkMode(darkMode);
```

Und dort, wo die anderen Compose-Fenster beim Profilwechsel geschlossen werden — die
Stelle findet `grep -n "SettingsWindow.INSTANCE.hide\|MonitorWindow.INSTANCE.hide"
app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` —
`TopPriorityCandidatesWindow.INSTANCE.hide();` ergänzen. Findet der Grep nichts, ist das
ein Fund: dann schließt der Profilwechsel die Compose-Fenster nicht, und das gehört in
die Commit-Nachricht.

- [ ] **Step 9: Übersetzen, Suite, Anwendung**

Run:
```bash
./gradlew clean build --console=plain > /tmp/t2b.txt 2>&1; echo "exit=$?"
python3 -c "
import glob,xml.etree.ElementTree as ET
t=f=0
for p in glob.glob('*/build/test-results/test/TEST-*.xml'):
    r=ET.parse(p).getroot(); t+=int(r.get('tests')); f+=int(r.get('failures'))+int(r.get('errors'))
print(f'tests={t} failures={f}')"
```

Erwartet: `exit=0`, `failures=0`, `tests=773` (769 + 4).

Dann `./gradlew :app-desktop:run`, den Knopf **more** drücken.

Erwartet: Ein Fenster „Top priority candidates" erscheint mit der Liste; ein Doppelklick
wählt die Station im Hauptfenster aus und schließt das Fenster; das Design folgt dem
Hauptfenster. **Das ist die erste Gelegenheit, dieses Fenster überhaupt zu sehen** —
die Vorlage wurde aus dem Quelltext nachgebaut, nicht aus der Anschauung.

- [ ] **Step 10: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/compose/TopPriorityCandidatesWindow.kt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/TopPriorityCandidatesWindowTest.kt \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Port the priority candidates window to Compose

It was the only Stage this class constructed - new Stage() at :4810, the
single occurrence in the file - and the Compose main window's 'more'
button is what opened it. No JavaFX dependency could be dropped while it
stood, however much dead construction was deleted around it.

The port is small because the window is: a list of 'callsign | score N',
a double-click that selects and closes, and a hint line. The label is
under a test because it is the whole content, and the list renders in a
test at both sizes that matter, empty and full.

selectTopCandidate loses its two JavaFX parameters. It never used them -
it resolves the candidate and calls focusChatMemberAndPrepareCq.

registerThemedScene and installSharedSystemMenuBar lose their only live
caller with this window."
```

---

## Task 3: Die Zeitleisten-Zuführung aus `start()` lösen

Der zweite kritische Fund aus Teil 1. `timelineView` wird in `start()` erzeugt
(`:7379`), und `updateTimelineVisuals` steigt aus, wenn es `null` ist (`:4886`) — danach
schreibt es in den **Compose**-Zustand (`:4914`–`:4919`). Ein Löschen des „toten Zweigs"
nähme der Compose-Zeitleiste Skeds, Kandidaten und Antennenrichtung.

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/feed/TimelineFeed.kt`
- Create: `app-desktop/src/test/kotlin/kst4contest/view/feed/TimelineFeedTest.kt`
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java:4885-4920`

**Interfaces:**
- Consumes: `kst4contest.view.compose.TimelineState` — `replaceSkeds(List<ContestSked>)`,
  `replaceCandidates(List<TimelineCandidate>)`, `setBeamWidth(Double)`, und die
  Eigenschaften `antennaAzimuth: Double`, `beamWidthDeg: Double`, `skeds`, `candidates`.
  **`beamWidthDeg`, nicht `beamWidth`** — der Setter heißt `setBeamWidth`, die
  Eigenschaft dahinter `beamWidthDeg`.
- Produces: `class TimelineFeed(target: TimelineState, dispatcher: UiDispatcher)` mit
  `fun push(skeds: List<ContestSked>, candidates: List<TimelineCandidate>, antennaAzimuthDeg: Double, beamWidthDeg: Double)`

- [ ] **Step 1: Den Test schreiben**

```kotlin
package kst4contest.view.feed

import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.TimelineState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * What fills the timeline above the send field.
 *
 * It has its own class because of how it was found: the feed sat in a method guarded by
 * a JavaFX field, inside window construction that is never shown, and deleting that
 * construction would have emptied the Compose timeline with a green build and no
 * compile error. A feed that can be constructed in a test cannot hide like that.
 */
class TimelineFeedTest {

    /** Runs inline and counts, so a test can tell "delivered" from "delivered twice". */
    private class CountingDispatcher(private val uiThread: Boolean = true) : UiDispatcher {
        var deliveries = 0
            private set

        override fun runOnUi(task: Runnable) {
            deliveries++
            task.run()
        }

        override fun isUiThread(): Boolean = uiThread
    }

    @Test
    fun `it puts what it is given into the timeline`() {
        val target = TimelineState()

        TimelineFeed(target, CountingDispatcher()).push(emptyList(), emptyList(), 229.0, 60.0)

        assertEquals(229.0, target.antennaAzimuth, 1e-9)
        assertEquals(60.0, target.beamWidthDeg, 1e-9)
    }

    /** Review Focus 2: two feeds writing one state is invisible until one goes stale. */
    @Test
    fun `one push is one delivery`() {
        val dispatcher = CountingDispatcher()

        TimelineFeed(TimelineState(), dispatcher).push(emptyList(), emptyList(), 0.0, 0.0)

        assertEquals(1, dispatcher.deliveries)
    }

    /** Review Focus 4: the controller's listeners fire on network threads. */
    @Test
    fun `it hands over rather than writing from the calling thread`() {
        val dispatcher = CountingDispatcher(uiThread = false)

        TimelineFeed(TimelineState(), dispatcher).push(emptyList(), emptyList(), 10.0, 5.0)

        assertTrue(dispatcher.deliveries > 0) { "the feed wrote without going through the dispatcher" }
    }

    /** Review Focus 3: empty is the normal state before the first score run. */
    @Test
    fun `an empty push is not an error`() {
        val target = TimelineState()

        TimelineFeed(target, CountingDispatcher()).push(emptyList(), emptyList(), 0.0, 0.0)

        assertEquals(0, target.skeds.size)
        assertEquals(0, target.candidates.size)
    }
}
```

- [ ] **Step 2: Den Test laufen lassen und scheitern sehen**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.feed.TimelineFeedTest' --console=plain 2>&1 | grep -E "^e: " | head -3`

Erwartet: `Unresolved reference 'TimelineFeed'`.

Scheitert er stattdessen an `antennaAzimuth`, `beamWidthDeg`, `skeds` oder `candidates`,
die Namen in `TimelineState.kt` nachsehen und die Zusicherungen anpassen — geprüft wird
die Zuführung, nicht die Benennung.

- [ ] **Step 3: Die Klasse schreiben**

```kotlin
package kst4contest.view.feed

import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.ContestSked
import kst4contest.view.compose.TimelineCandidate
import kst4contest.view.compose.TimelineState

/**
 * Fills the timeline above the send field.
 *
 * A class of its own because of how it was found. The feed used to sit inside
 * `updateTimelineVisuals()`, behind a guard on a JavaFX field that the never-shown
 * window construction created; deleting that construction would have emptied the
 * Compose timeline without a compile error and with a green suite. A feed that can be
 * constructed on its own cannot hide inside something else's lifetime.
 *
 * Knows nothing about JavaFX. Delivery goes through the [UiDispatcher] because the
 * controller's listeners fire on network threads.
 */
class TimelineFeed(
    private val target: TimelineState,
    private val dispatcher: UiDispatcher,
) {

    /**
     * Replaces everything the timeline shows, in one delivery.
     *
     * One delivery and not four: four would let the operator see a frame with new skeds
     * against an old antenna heading.
     *
     * The lists are copied before they are handed over. The caller's are snapshots of
     * roster state that keeps changing, and the timeline must not follow them.
     */
    fun push(
        skeds: List<ContestSked>,
        candidates: List<TimelineCandidate>,
        antennaAzimuthDeg: Double,
        beamWidthDeg: Double,
    ) {
        val skedsCopy = skeds.toList()
        val candidatesCopy = candidates.toList()

        dispatcher.runOnUi {
            target.replaceSkeds(skedsCopy)
            target.replaceCandidates(candidatesCopy)
            target.antennaAzimuth = antennaAzimuthDeg
            target.setBeamWidth(beamWidthDeg)
        }
    }
}
```

- [ ] **Step 4: Den Test laufen lassen**

Run:
```bash
./gradlew :app-desktop:test --tests 'kst4contest.view.feed.TimelineFeedTest' --console=plain > /tmp/t3.txt 2>&1
grep -E "^e: " /tmp/t3.txt | head -3
python3 -c "
import glob,xml.etree.ElementTree as ET
for f in glob.glob('app-desktop/build/test-results/test/TEST-*TimelineFeedTest.xml'):
    r=ET.parse(f).getroot(); print(f\"tests={r.get('tests')} failures={r.get('failures')}\")
    for tc in r.iter('testcase'):
        for b in list(tc.iter('failure'))+list(tc.iter('error')): print(' ',tc.get('name'),(b.get('message') or '')[:200])"
```

Erwartet: `tests=4 failures=0`.

- [ ] **Step 5: `updateTimelineVisuals` auf die Zuführung umstellen**

Ein Feld neben den übrigen Compose-Feldern anlegen:

```java
	private TimelineFeed timelineFeed;
```

In `updateTimelineVisuals` die Wache bei `:4886` ändern — `timelineView` darf die
Compose-Zuführung nicht mehr aufhalten:

```java
	private void updateTimelineVisuals() {
		if (chatcontroller == null) {
			return;
		}
```

Den Block, der direkt in den Compose-Zustand schreibt (`:4914`–`:4919`), ersetzen:

```java
			if (timelineFeed != null) {
				timelineFeed.push(
						skedsSnapshot,
						composeCandidates,
						chatcontroller.getChatPreferences().getActualQTF().get(),
						chatcontroller.getChatPreferences().getStn_antennaBeamWidthDeg()
				);
			}
```

Jeder verbleibende Zugriff auf `timelineView` in dieser Methode bekommt seine eigene
Null-Prüfung, damit der JavaFX-Teil fehlen darf, ohne den Compose-Teil mitzunehmen:

```bash
sed -n '4885,4925p' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java | grep -n "timelineView"
```

- [ ] **Step 6: Die Zuführung erzeugen, wo der Compose-Zustand entsteht**

Die Stelle findet
`grep -n "composeMainWindowState = " app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`:

```java
		timelineFeed = new TimelineFeed(composeMainWindowState.getTimeline(), uiDispatcher);
```

Den Listener bei `:7428` **nicht** löschen — er bleibt der Auslöser und wandert in
Teil 3 mit dem Rest von `start()`.

- [ ] **Step 7: Übersetzen und die ganze Suite**

Run: wie Aufgabe 1, Schritt 5. Erwartet: `exit=0`, `failures=0`, `tests=777`.

- [ ] **Step 8: Am laufenden Programm prüfen**

Run: `./gradlew :app-desktop:run`

Erwartet: Die Zeitleiste über dem Eingabefeld zeigt weiter Skeds und
Prioritätskandidaten, und der Antennenbalken folgt einer Änderung der QTF. Der Test
deckt die Zuführung ab, nicht ihren Auslöser — diese Prüfung deckt den Auslöser ab.

- [ ] **Step 9: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/feed/TimelineFeed.kt \
        app-desktop/src/test/kotlin/kst4contest/view/feed/TimelineFeedTest.kt \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Lift the timeline feed out of the window that is never shown

updateTimelineVisuals() returned at its first line unless timelineView was
non-null, and timelineView is created in the start() body that part 3 will
delete. Everything after that guard writes the Compose timeline: skeds,
candidates, antenna azimuth, beam width. Deleting the 'dead branch' would
have emptied the timeline above the send field, with no compile error and
a green suite.

TimelineFeed takes the Compose state and a UiDispatcher and knows nothing
about JavaFX. One push is one delivery, which is pinned: four deliveries
would let the operator see new skeds against an old antenna heading."
```

---

## Task 4: Die Nachrichten-Zuführung aus der Tabellenkonstruktion lösen

Der erste kritische Fund aus Teil 1. `initFurtherInfoAbtCallsignMSGTable` erzeugt bei
`:3400` die Bindung, deren Listener bei `:3404`–`:3409` in
`composeMainWindowState.getSelectedStationMessages()` schreibt — eine Compose-Tabelle.

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/feed/SelectedStationMessagesFeed.kt`
- Create: `app-desktop/src/test/kotlin/kst4contest/view/feed/SelectedStationMessagesFeedTest.kt`
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java:3400-3410`

**Interfaces:**
- Consumes: `kst4contest.view.compose.DataTableState<ChatMessage>` mit
  `replaceRows(List<ChatMessage>)` und der Eigenschaft `rows: List<ChatMessage>`.
  Konstruktor: `DataTableState(columns, rowKey, tableId = "", widths = null)`.
- Produces: `class SelectedStationMessagesFeed(target: DataTableState<ChatMessage>, dispatcher: UiDispatcher)`
  mit `fun push(messages: List<ChatMessage>)`

- [ ] **Step 1: Den Test schreiben**

```kotlin
package kst4contest.view.feed

import kst4contest.model.ChatMessage
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.DataTableState
import kst4contest.view.compose.RowKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * What fills the "messages of the selected station" table.
 *
 * Found the hard way: this feed was buried two hundred lines inside a JavaFX table
 * builder, so "who calls that builder" answered a different question than "is it safe
 * to delete". A feed with its own name and its own test cannot be lost that way.
 */
class SelectedStationMessagesFeedTest {

    private class CountingDispatcher : UiDispatcher {
        var deliveries = 0
            private set

        override fun runOnUi(task: Runnable) {
            deliveries++
            task.run()
        }

        override fun isUiThread(): Boolean = true
    }

    private fun emptyTable() =
        DataTableState<ChatMessage>(emptyList(), RowKeys.byReference(), "test")

    @Test
    fun `the messages it is given reach the table`() {
        val target = emptyTable()

        SelectedStationMessagesFeed(target, CountingDispatcher()).push(listOf(ChatMessage()))

        assertEquals(1, target.rows.size)
    }

    /** Review Focus 2. */
    @Test
    fun `one push is one delivery`() {
        val dispatcher = CountingDispatcher()

        SelectedStationMessagesFeed(emptyTable(), dispatcher).push(listOf(ChatMessage()))

        assertEquals(1, dispatcher.deliveries)
    }

    /** Review Focus 3: no station selected is the state the window opens in. */
    @Test
    fun `an empty push clears the table rather than failing`() {
        val target = emptyTable()
        val feed = SelectedStationMessagesFeed(target, CountingDispatcher())

        feed.push(listOf(ChatMessage()))
        feed.push(emptyList())

        assertEquals(0, target.rows.size)
    }

    /** The caller hands over a live mirror; the table must not follow it. */
    @Test
    fun `the table keeps its own copy`() {
        val target = emptyTable()
        val source = mutableListOf(ChatMessage())

        SelectedStationMessagesFeed(target, CountingDispatcher()).push(source)
        source.clear()

        assertEquals(1, target.rows.size, "the table followed the caller's list")
    }
}
```

- [ ] **Step 2: Den Test laufen lassen und scheitern sehen**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.feed.SelectedStationMessagesFeedTest' --console=plain 2>&1 | grep -E "^e: " | head -3`

Erwartet: `Unresolved reference 'SelectedStationMessagesFeed'`.

Scheitert er an `RowKeys.byReference()`, die Fassung nachsehen
(`grep -n "byReference" app-desktop/src/main/kotlin/kst4contest/view/compose/DataTableState.kt`)
— aus Kotlin heißt sie je nach Deklaration `RowKeys.byReference()` oder
`RowKeys.INSTANCE.byReference()`.

- [ ] **Step 3: Die Klasse schreiben**

```kotlin
package kst4contest.view.feed

import kst4contest.model.ChatMessage
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.DataTableState

/**
 * Fills the table of messages belonging to the selected station.
 *
 * Its own class for the reason it was nearly lost: the feed used to be a listener
 * registered two hundred lines inside a JavaFX table builder, so a reader asking "who
 * calls that builder" never discovered that deleting it would leave this table
 * permanently empty — with a green build and no compile error.
 *
 * Copies what it is handed. The caller's list is the live mirror of a roster and keeps
 * changing; a table holding a view of it would show rows that are no longer there.
 */
class SelectedStationMessagesFeed(
    private val target: DataTableState<ChatMessage>,
    private val dispatcher: UiDispatcher,
) {

    fun push(messages: List<ChatMessage>) {
        val copy = messages.toList()
        dispatcher.runOnUi { target.replaceRows(copy) }
    }
}
```

- [ ] **Step 4: Den Test laufen lassen**

Run: wie Aufgabe 3, Schritt 4, mit `SelectedStationMessagesFeedTest`.

Erwartet: `tests=4 failures=0`.

- [ ] **Step 5: Die Zuführung an die Stelle der Listener-Schreibung setzen**

Feld anlegen und neben `composeMainWindowState` erzeugen:

```java
	private SelectedStationMessagesFeed selectedStationMessagesFeed;
```
```java
		selectedStationMessagesFeed = new SelectedStationMessagesFeed(
				composeMainWindowState.getSelectedStationMessages(), uiDispatcher);
```

In `initFurtherInfoAbtCallsignMSGTable` den Listener bei `:3403`–`:3409` ersetzen:

```java
		selectedCallSignInfoMessageBinding.list().addListener(
				(javafx.collections.ListChangeListener<ChatMessage>) change -> {
					if (selectedStationMessagesFeed != null) {
						selectedStationMessagesFeed.push(selectedCallSignInfoMessageBinding.list());
					}
				});
```

**Die Bindung selbst bleibt vorerst, wo sie ist.** Sie herauszuziehen ist Teil 3; hier
geht es darum, dass das Ziel der Zuführung einen Namen und einen Test hat.

- [ ] **Step 6: Die weiteren Schreiber auf die Zuführung umstellen**

Der Befund nennt vier Schreiber. Die Zeilen haben sich durch Aufgabe 1 verschoben:

```bash
grep -n "getSelectedStationMessages()" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Jede Fundstelle, die `replaceRows(...)` ruft, auf `selectedStationMessagesFeed.push(...)`
umstellen. **Review Focus 2:** danach darf keine Fundstelle mehr direkt `replaceRows`
auf dieser Tabelle rufen.

Run zur Kontrolle:
```bash
grep -n "getSelectedStationMessages().replaceRows" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: keine Ausgabe.

- [ ] **Step 7: Übersetzen, Suite, Anwendung**

Run: wie Aufgabe 1, Schritt 5. Erwartet: `exit=0`, `failures=0`, `tests=781`.

Dann `./gradlew :app-desktop:run`, eine Station auswählen.

Erwartet: Die Tabelle „Nachrichten der ausgewählten Station" füllt sich, und ein Wechsel
auf eine andere Station tauscht ihren Inhalt aus.

- [ ] **Step 8: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/feed/SelectedStationMessagesFeed.kt \
        app-desktop/src/test/kotlin/kst4contest/view/feed/SelectedStationMessagesFeedTest.kt \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Give the selected-station message feed a name and a test

The listener that fills this Compose table was registered two hundred
lines inside a JavaFX table builder, which is why a review of 'who calls
that builder' concluded the builder was safe to delete. It was not: the
table would have gone permanently empty, with a green build.

SelectedStationMessagesFeed copies what it is handed, because the caller's
list is a live roster mirror and a table holding a view of it would show
rows that are no longer there. All writers now go through it, so there is
one path instead of four."
```

---

## Task 5: Den Verbindungszustand aus dem JavaFX-Indikator lösen

Der vierte Fund, den erst der transitive Durchlauf fand.
`updateConnectionStateIndicator` schreibt bei `:6217`–`:6219` in die Compose-Statusleiste
und fasst danach bei `:6224` ein JavaFX-Tooltip an. Sie hat lebende Aufrufer außerhalb
des toten Aufbaus (`:9844`, `:9871`).

**Files:**
- Create: `app-desktop/src/main/kotlin/kst4contest/view/feed/ConnectionStateFeed.kt`
- Create: `app-desktop/src/test/kotlin/kst4contest/view/feed/ConnectionStateFeedTest.kt`
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java:6200-6230`

**Interfaces:**
- Consumes: `kst4contest.view.compose.MainWindowSurroundings` mit den Eigenschaften
  `connectionState: On4KstConnectionState` und `connectionDetail: String`.
  Konstruktor `MainWindowSurroundings(menu: MainMenuState = MainMenuState())` —
  aus Kotlin reicht `MainWindowSurroundings()`.
- Produces: `class ConnectionStateFeed(target: MainWindowSurroundings, dispatcher: UiDispatcher)`
  mit `fun push(state: On4KstConnectionState?, detail: String?)`

- [ ] **Step 1: Den Test schreiben**

```kotlin
package kst4contest.view.feed

import kst4contest.controller.On4KstConnectionState
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.MainWindowSurroundings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * What fills the connection indicator in the Compose status bar.
 *
 * The JavaFX method this came out of did two things in one breath: it wrote the Compose
 * state and then touched a JavaFX tooltip. Deleting the construction that creates the
 * tooltip would have left live callers running into the second half.
 */
class ConnectionStateFeedTest {

    private class CountingDispatcher : UiDispatcher {
        var deliveries = 0
            private set

        override fun runOnUi(task: Runnable) {
            deliveries++
            task.run()
        }

        override fun isUiThread(): Boolean = true
    }

    @Test
    fun `the state and its detail reach the status bar`() {
        val target = MainWindowSurroundings()

        ConnectionStateFeed(target, CountingDispatcher())
            .push(On4KstConnectionState.CONNECTED, "logged in as DN9APW")

        assertEquals(On4KstConnectionState.CONNECTED, target.connectionState)
        assertEquals("logged in as DN9APW", target.connectionDetail)
    }

    /**
     * Review Focus 3. The JavaFX method substituted DISCONNECTED for an absent state
     * and the state's own name for a blank detail; an operator reading "null" in a
     * status bar learns nothing.
     */
    @Test
    fun `a missing state reads as disconnected`() {
        val target = MainWindowSurroundings()

        ConnectionStateFeed(target, CountingDispatcher()).push(null, null)

        assertEquals(On4KstConnectionState.DISCONNECTED, target.connectionState)
        assertEquals("DISCONNECTED", target.connectionDetail)
    }

    @Test
    fun `a blank detail reads as the state itself`() {
        val target = MainWindowSurroundings()

        ConnectionStateFeed(target, CountingDispatcher())
            .push(On4KstConnectionState.CONNECTING, "   ")

        assertEquals("CONNECTING", target.connectionDetail)
    }

    /** Review Focus 2. */
    @Test
    fun `one push is one delivery`() {
        val dispatcher = CountingDispatcher()

        ConnectionStateFeed(MainWindowSurroundings(), dispatcher)
            .push(On4KstConnectionState.CONNECTED, "x")

        assertEquals(1, dispatcher.deliveries)
    }
}
```

Scheitert der Test daran, dass `On4KstConnectionState` kein `CONNECTING` kennt, die
vorhandenen Werte nachsehen
(`grep -n "    [A-Z_]*," core/src/main/java/kst4contest/controller/On4KstConnectionState.java`)
und einen davon einsetzen — geprüft wird die Ersetzungsregel, nicht der Name des Werts.

- [ ] **Step 2: Den Test laufen lassen und scheitern sehen**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.feed.ConnectionStateFeedTest' --console=plain 2>&1 | grep -E "^e: " | head -3`

Erwartet: `Unresolved reference 'ConnectionStateFeed'`.

- [ ] **Step 3: Die Klasse schreiben**

```kotlin
package kst4contest.view.feed

import kst4contest.controller.On4KstConnectionState
import kst4contest.observe.UiDispatcher
import kst4contest.view.compose.MainWindowSurroundings

/**
 * Fills the connection indicator in the Compose status bar.
 *
 * Separated from `updateConnectionStateIndicator()`, which wrote this state and then
 * touched a JavaFX tooltip in the same method. The tooltip belongs to the window that
 * is never shown; its live callers do not, and they must not be taken down with it.
 *
 * The substitutions are the JavaFX method's own: an absent state reads as DISCONNECTED
 * and an absent detail as the state's name, because "null" in a status bar tells an
 * operator nothing.
 */
class ConnectionStateFeed(
    private val target: MainWindowSurroundings,
    private val dispatcher: UiDispatcher,
) {

    fun push(state: On4KstConnectionState?, detail: String?) {
        val effectiveState = state ?: On4KstConnectionState.DISCONNECTED
        val effectiveDetail =
            if (detail.isNullOrBlank()) effectiveState.name else detail

        dispatcher.runOnUi {
            target.connectionState = effectiveState
            target.connectionDetail = effectiveDetail
        }
    }
}
```

- [ ] **Step 4: Den Test laufen lassen**

Run: wie Aufgabe 3, Schritt 4, mit `ConnectionStateFeedTest`.

Erwartet: `tests=4 failures=0`.

- [ ] **Step 5: `updateConnectionStateIndicator` umstellen**

Feld anlegen und neben `composeMainWindowState` erzeugen:

```java
	private ConnectionStateFeed connectionStateFeed;
```
```java
		connectionStateFeed = new ConnectionStateFeed(
				composeMainWindowState.getSurroundings(), uiDispatcher);
```

Den Block bei `:6217`–`:6220` ersetzen:

```java
		if (connectionStateFeed != null) {
			connectionStateFeed.push(state, detail);
		}
```

Die Normalisierung bei `:6212`–`:6215` (`effectiveState`, `stateDetail`) bleibt stehen —
der JavaFX-Teil darunter braucht sie. Dass sie damit an zwei Stellen steht, ist Absicht
und endet, wenn der JavaFX-Teil in Teil 3 fällt; `ConnectionStateFeedTest` hält die
Fassung fest, die bleibt.

Jeden verbleibenden Zugriff auf `tipConnectionStateIndicator` und die übrigen
JavaFX-Felder in dieser Methode einzeln null-prüfen, damit sie fehlen dürfen.

- [ ] **Step 6: Übersetzen, Suite, Anwendung**

Run: wie Aufgabe 1, Schritt 5. Erwartet: `exit=0`, `failures=0`, `tests=785`.

Dann `./gradlew :app-desktop:run`, die Verbindung zu ON4KST herstellen und trennen.

Erwartet: Die Verbindungsanzeige in der Compose-Statusleiste wechselt mit, und der
Hinweistext daneben ändert sich.

- [ ] **Step 7: Commit**

```bash
git add app-desktop/src/main/kotlin/kst4contest/view/feed/ConnectionStateFeed.kt \
        app-desktop/src/test/kotlin/kst4contest/view/feed/ConnectionStateFeedTest.kt \
        app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
git commit -m "Separate the connection-state feed from the JavaFX indicator

updateConnectionStateIndicator wrote the Compose status bar and then
touched a JavaFX tooltip in the same method. The tooltip belongs to the
window that is never shown; the live callers at the bottom of the file do
not, and deleting the construction would have run them into the second
half.

Only the transitive sweep found this one: the init method's own body is
clean, and it is the method it calls that writes. The substitutions are
the originals - an absent state reads as DISCONNECTED and an absent detail
as the state's name, because 'null' in a status bar tells an operator
nothing."
```

---

## Task 6: Die Menüeinträge gegen die Compose-Leiste abgleichen

`initMenuBar` (337 Zeilen) öffnet über seine Einträge die Compose-Fenster. Es wird
portiert, nicht gelöscht — aber erst muss feststehen, **was** zu portieren ist.

**Files:**
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`
- Read only: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java:5729-6066`,
  `app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuBar.kt`,
  `app-desktop/src/main/kotlin/kst4contest/view/compose/MainWindowState.kt`

Diese Aufgabe ändert keinen Produktionscode. Ihr Ergebnis ist eine Liste, aus der Teil 3
die Portierung schreibt.

- [ ] **Step 1: Die JavaFX-Einträge auflisten**

Run:
```bash
sed -n '5729,6066p' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
  | grep -nE "new Menu\(|new MenuItem\(|new CheckMenuItem\(|setOnAction"
```

Erwartet: die Menüs und ihre Einträge mit den Aktionen, die daran hängen. Für jeden
Eintrag Beschriftung und Aktion notieren.

- [ ] **Step 2: Die Compose-Einträge auflisten**

Run:
```bash
grep -nE "Text\(|onClick|MenuItem|fun " app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuBar.kt
grep -n "class MainMenuState" -A 30 app-desktop/src/main/kotlin/kst4contest/view/compose/MainWindowState.kt
```

Erwartet: die Einträge, die das Compose-Hauptfenster bereits hat.

- [ ] **Step 3: Die Differenz in den Befund schreiben**

Unter „initMenuBar" im Befunddokument einen Abschnitt ergänzen:

```markdown
### Abgleich mit `MainMenuBar.kt` (Stand 2026-10-01)

| JavaFX-Eintrag | Aktion | In Compose vorhanden? |
|---|---|---|
| … | … | ja / **fehlt** |

**Zu portieren:** <die Einträge mit „fehlt", je mit der Aktion, die sie auslösen>
**Nur macOS:** <Einträge, die an der Systemmenüleiste hängen und auf Linux nie erscheinen>
```

Jede Zeile ausfüllen. Bleibt eine spitze Klammer stehen, ist die Aufgabe nicht fertig.

- [ ] **Step 4: Prüfen, dass keine Vorlage offen blieb**

Run:
```bash
grep -nE "<die Einträge|<Einträge|…" docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
```

Erwartet: keine Ausgabe aus dem neuen Abschnitt.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Record which menu items Compose does not have yet

initMenuBar is ported rather than deleted because its items open the
Compose windows. Part 3 writes that port from this table instead of
rediscovering it. No production code touched."
```

---

## Was dieser Plan nicht tut

- Er löscht nur die eine Scheibe, deren Unschädlichkeit beweisbar ist. Die anderen sieben
  sind Teil 3.
- Er fasst `FxRosterBinding`, den `UiDispatcher`, den Lebenszyklus, die zehn Dialoge und
  den Build nicht an.
- Er beantwortet die macOS-Frage nicht.

Nach ihm speist der nie angezeigte Aufbau nichts mehr — und erst dann ist Löschen keine
Wette.
