# Etappe 3b — `DataTable` und Einstellungsfenster: Umsetzungsplan

> **Für agentische Bearbeiter:** ERFORDERLICHE UNTER-SKILL: superpowers:subagent-driven-development (empfohlen) oder superpowers:executing-plans, um diesen Plan Aufgabe für Aufgabe umzusetzen. Schritte nutzen Checkbox-Syntax (`- [ ]`).

**Ziel:** Das Einstellungsfenster mit seinen zwölf Reitern ist in Compose, funktionsgleich, und schreibt unverändert dasselbe `preferences.xml`.

**Architektur:** Ein Compose-Fenster ersetzt `settingsStage`. Jeder Reiter bekommt eine Zustandsklasse ohne Compose-Bezug — eine **typisierte Fassade**, die jede Eigenschaft direkt aus `ChatPreferences` liest und **sofort** dorthin zurückschreibt. Eine `DataTable`-Komponente bedient die drei einspaltigen, editierbaren Listen des Fensters.

**Tech-Stack:** Kotlin 2.2.0, Compose Multiplatform 1.8.2, Gradle 9.7.1, Java 21.

**Spec:** `docs/superpowers/specs/2026-09-25-compose-migration-design.md`

## Korrektur am Plan: es gibt kein Bestätigen, nur Durchschreiben

Dieser Plan schrieb zunächst vor, jeder Reiter solle aus den Einstellungen lesen und **nur bei `apply()`** zurückschreiben, mit der Begründung, unbestätigte Werte dürften nicht in `preferences.xml` landen. **Am Code geprüft ist das falsch**, und zwar nicht im Detail, sondern im Modell:

- **„Save settings"** (`Kst4ContestApplication:12523`) ruft ausschliesslich `writePreferencesToXmlFile()` und bricht einen anstehenden Layout-Autosave ab. Es **sammelt keine Werte ein**.
- **„Apply/Close prefs"** (`:12382`) ruft `settingsStage.hide()`. Nichts weiter.

Jedes Bedienelement des JavaFX-Fensters schreibt also **beim Ändern sofort** in das `ChatPreferences`-Objekt im Speicher; die beiden Knöpfe schreiben nur auf die Platte beziehungsweise schliessen. Was `PROJECT_CONTEXT` verbietet, ist etwas anderes: dass **selektive Layout-Schreibvorgänge** unbestätigte funktionale Einstellungen mitpersistieren. Das betrifft den Autosave, nicht die Bedienelemente.

**Folge:** Eine Zustandsklasse mit Puffer und `apply()` wäre eine Verhaltensänderung, auf die sich ein Operator verlassen kann — heute wirkt eine geänderte Einstellung sofort in der laufenden Sitzung, auch wenn sie nie auf die Platte kommt. Die Zustandsklassen sind deshalb **durchschreibende Fassaden ohne `apply()`**:

```kotlin
var stn_loginCallSign: String
    get() = prefs.getStn_loginCallSign()
    set(value) { prefs.setStn_loginCallSign(value) }
```

Das macht sie zugleich einfacher: kein Puffer, kein Abgleich, keine Frage wann geschrieben wird.

## Das zweite Netz: Schreibstellen ausserhalb der Einstellungen

`SettingsFieldCoverageTest` prüft nur `ChatPreferences`-Setter. Eine Schreibstelle auf einem anderen Objekt fällt durch — und das JavaFX-Fenster hat **fünf** davon, mechanisch ermittelt: die Setter im Bereich, die `ChatPreferences` nicht deklariert, geschnitten mit den Settern der eigenen Projektklassen.

| Setter | Klasse | Wofür |
|---|---|---|
| `setChatCategoryMain` | `ChatController` | bei jeder Kategorieauswahl, **zusätzlich** zum Einstellungs-Setter |
| `setChatCategorySecondChat` | `ChatController` | dito; `null` bedeutet „kein zweiter Chat" |
| `setCallSign`, `setFrequency`, `setQra` | `ChatMember` | das eigene Stationsobjekt, im Gleichschritt mit den Stationsfeldern |

Fällt der `ChatController`-Teil weg, bleibt die laufende Sitzung auf der alten Kategorie, während die gespeicherte Einstellung etwas anderes sagt — und **kein Test des ersten Netzes würde das merken**. `SettingsForeignWriteCoverageTest` deckt es ab.

Weil eine Zustandsklasse dafür einen `ChatController` bräuchte und damit im Test eine Datenbank öffnen würde, nimmt sie stattdessen **Rückrufe** entgegen:

```kotlin
class StationTabState(
    private val prefs: ChatPreferences,
    private val applyMainCategory: (ChatCategory?) -> Unit,
    private val applySecondCategory: (ChatCategory?) -> Unit,
)
```

## Korrektur am Spec: die `DataTable` wird hier nicht validiert

Das Spec nennt die `DataTable` „den wichtigsten einzelnen Baustein" und erwartet, dass das Einstellungsfenster sie belastet: *„Wird diese Komponente gut, ist der Rest Fleißarbeit."* Gemessen am Code trägt das nicht.

Die sieben Tabellen mit gespeicherten Spaltenbreiten sitzen **alle** in den Hauptfenstern:

| Tabelle | `TableLayoutManager`-ID |
|---|---|
| `tbl_chatMemberTable` | `chat-members` |
| `tbl_generalMSGTable` | `public-messages` |
| `tbl_privateMSGTable` | `private-messages` |
| `tbl_toOtherMSGTable` | — |
| `tbl_furtherInfoAbtCallsignMSGTable` | `selected-station-messages` |
| `tbl_DXCTable` | `dx-cluster-main` / `dx-cluster-monitor` |
| `tbl_chatMemberWkdDBTable` | `worked-database` |

Die drei Tabellen im Einstellungsfenster — `tblVw_shortcuts`, `tblVw_textsnippets`, `tblVw_notify_sniffCallSigns` — sind **einspaltige, editierbare Zeichenkettenlisten** mit `TextFieldTableCell` und `setOnEditCommit`. Sie haben **keine** gespeicherten Breiten (null `install`-Aufrufe), keine Sortierung, kein Zell-Styling, keine mehrspaltige Struktur.

**Folge für diesen Plan:** Die `DataTable` wird hier für genau das gebaut, was diese drei Listen brauchen, mit einer Schnittstelle, die die Anforderungen der Hauptfenster später aufnehmen kann, ohne umgebaut zu werden. Spaltenbreiten mit stabilen IDs, Sortierung und Zeilen-Styling entstehen in **Etappe 5**, wo ihre Verbraucher sitzen. Sie jetzt zu bauen hieße, blind zu entwerfen.

Das ist keine Verkleinerung des Ziels, sondern eine Verschiebung an die Stelle, an der sie prüfbar ist.

## Was aus Etappe 2 in die `DataTable` einfließt

Vier Dinge, die dort Fehler gekostet haben und hier von Anfang an richtig gehören:

1. **Schlüssel statt Gleichheit.** Der JavaFX-Spiegel ersetzte bei jeder Änderung die ganze Liste und kostete Auswahl und Scrollposition; dagegen half ein `equals`-Vergleich, der wiederum an `ChatMember.equals` als Überladung hing. In Compose ist der Hebel ein **stabiler Schlüssel je Zeile** (`key = { … }` in `LazyColumn`). Kein Gleichheitsvergleich auf Fachobjekten.
2. **Sortierung leitet aus der kanonischen Liste ab**, nie aus dem vorher Angezeigten. `SortedList` fügte per Binärsuche ein und liess bei Gleichstand eine undefinierte Position — die neue Regel ist Ankunftsreihenfolge, festgeschrieben in `ChatMemberListViewTest`.
3. **Fokus wird ausdrücklich geführt.** Zwei der drei Fehler aus Etappe 2 waren Fokusfehler, und keiner war durch Lesen zu finden. In Compose ist Fokus explizit (`FocusRequester`) — das ist eine Chance, keine Last.
4. **Ein Sammelvorgang ist eine Zustandsänderung**, nicht eine je Element.

## Globale Randbedingungen

- **`preferences.xml` bleibt bitweise gleichwertig.** Version 7, dieselben Elementnamen, dieselbe Reihenfolge, unbekannte Knoten bleiben erhalten. Das ist die härteste Vorgabe dieses Plans und der Gegenstand der Abnahme.
- **Kein Funktionsverlust.** Jede Einstellung, die das JavaFX-Fenster setzen konnte, muss das Compose-Fenster setzen können. Ein Reiter gilt erst als fertig, wenn seine Felder vollständig sind.
- `core` wird **nicht** angefasst.
- **`Platform.runLater` und `uiDispatcher.runOnUi` sind nicht dasselbe.** Der Dispatcher führt inline aus, wenn der Aufrufer schon auf dem UI-Thread ist. Siehe `prepareCqTextForCallsign`; das hat in Etappe 2 einen Fehler gekostet.
- **`application(exitProcessOnExit = false)`** bei jedem Compose-Fenster. Die Vorgabe `true` beendet den Prozess, wenn das letzte Compose-Fenster schliesst — in Etappe 3a hätte das die Anwendung nach der Profilwahl beendet.
- Kommentare und KDoc auf Englisch, Kommunikation auf Deutsch.
- Die Invarianten aus dem Spec-Abschnitt „Invarianten" gelten unverändert.

## Prüfschwerpunkte

1. **Eine Einstellung verschwindet still.** 2341 Zeilen mit Hunderten Feldern: ein beim Portieren vergessenes Feld fällt nicht beim Übersetzen auf, sondern wenn ein Operator im Contest merkt, dass seine Einstellung weg ist. → Aufgabe 2 und die Abnahme.
2. **`preferences.xml` verliert Knoten.** Selektives Schreiben muss unbekannte XML-Knoten erhalten; **Save Settings** bleibt der vollständige Schreiber. Ein Compose-Fenster, das die Datei neu aufbaut statt zu aktualisieren, löscht stillschweigend, was es nicht kennt. → Aufgabe 2.
3. **Eine Liste verliert Einträge beim Bearbeiten.** Die drei Listen sind editierbar; ein Bearbeitungsvorgang, der den Roster ersetzt statt den Eintrag zu ändern, wirft angemeldete Zuhörer ab. In Etappe 2 war das der Grund, Setter auf `setAll` umzustellen statt das Feld zu tauschen. → Aufgabe 1.
4. **Der Profilwechsel bricht.** Der Reiter **Profiles** löst einen Laufzeitwechsel aus, der die ganze `Kst4ContestApplication` neu baut — aus einem Compose-Fenster heraus, das dabei selbst verschwinden muss. → Aufgabe 6.
5. **Zwei Einstellungsfenster gleichzeitig.** Solange das JavaFX-Fenster noch existiert, können beide offen sein und gegeneinander schreiben. → Aufgabe 7.

---

## Aufgabe 1: `DataTable` für editierbare einspaltige Listen

Deckt Prüfschwerpunkt 3 ab.

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/DataTable.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/EditableListStateTest.kt`

**Schnittstellen:**
- Erzeugt: `EditableListState` mit `entries: List<String>`, `selectedIndex: Int?`, `addAtTop(String)`, `updateAt(Int, String)`, `removeAt(Int)`, `select(Int?)`, `commitTo(SimpleRoster<String>)`; und die Composable `DataTable(state, onCommit, modifier)`. Konsumiert von den Aufgaben 4 und 5.

Die drei Listen des Einstellungsfensters verhalten sich heute so: **Add** legt einen Platzhaltertext an **Position 0** ein, wählt ihn aus und öffnet die Bearbeitung; ein Eintrag, dessen Text vollständig geleert wird, **verschwindet**. Beides ist in `Kst4ContestApplication` beim Knopf „Add shortcut" und im `setOnEditCommit` nachzulesen und muss erhalten bleiben.

- [ ] **Schritt 1: Das heutige Verhalten ablesen**

Ausführen:
```bash
grep -n -A24 "private TableView<String> initShortcutTable" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
grep -n -B2 -A12 "btn_Short_addLine.setOnAction" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Notieren: was `setOnEditCommit` mit einem leeren Text tut, wo ein neuer Eintrag landet, und was mit der Auswahl passiert. Der Test im nächsten Schritt schreibt **dieses** Verhalten fest, nicht ein besseres.

- [ ] **Schritt 2: Den fehlschlagenden Test schreiben**

`app-desktop/src/test/kotlin/kst4contest/view/compose/EditableListStateTest.kt`:

```kotlin
package kst4contest.view.compose

import kst4contest.observe.SimpleRoster
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame

class EditableListStateTest {

    @Test
    fun `a new entry lands at the top and becomes the selection`() {
        val state = EditableListState(listOf("alt"))

        state.addAtTop("neu")

        assertEquals(listOf("neu", "alt"), state.entries)
        assertEquals(0, state.selectedIndex,
            "the JavaFX table selected row 0 and opened it for editing")
    }

    @Test
    fun `an entry edited to blank disappears`() {
        val state = EditableListState(listOf("a", "b", "c"))

        state.updateAt(1, "   ")

        assertEquals(listOf("a", "c"), state.entries,
            "clearing the text was how the operator removed an entry")
    }

    @Test
    fun `an edit keeps the other entries and their order`() {
        val state = EditableListState(listOf("a", "b", "c"))

        state.updateAt(1, "B")

        assertEquals(listOf("a", "B", "c"), state.entries)
    }

    @Test
    fun `removing the last entry clears the selection`() {
        val state = EditableListState(listOf("a"))

        state.select(0)
        state.removeAt(0)

        assertEquals(emptyList<String>(), state.entries)
        assertNull(state.selectedIndex)
    }

    @Test
    fun `an index outside the list is ignored`() {
        val state = EditableListState(listOf("a"))

        state.updateAt(5, "x")
        state.removeAt(-1)

        assertEquals(listOf("a"), state.entries,
            "a stale index from a pending edit must not corrupt the list")
    }

    @Test
    fun `committing fills the existing roster instead of replacing it`() {
        val roster = SimpleRoster<String>()
        roster.add("alt")
        val before = roster
        val state = EditableListState(listOf("neu"))

        state.commitTo(roster)

        assertEquals(listOf("neu"), roster.snapshot())
        assertSame(before, roster,
            "replacing the roster would orphan every listener registered on it")
    }

    @Test
    fun `committing an unchanged list still yields the same content`() {
        val roster = SimpleRoster<String>()
        roster.addAll(listOf("a", "b"))
        val state = EditableListState(roster.snapshot())

        state.commitTo(roster)

        assertEquals(listOf("a", "b"), roster.snapshot())
    }
}
```

- [ ] **Schritt 3: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.EditableListStateTest'`
Erwartet: FAIL — `Unresolved reference 'EditableListState'`.

- [ ] **Schritt 4: Den Zustand schreiben**

`app-desktop/src/main/kotlin/kst4contest/view/compose/DataTable.kt`, erster Teil:

```kotlin
package kst4contest.view.compose

import kst4contest.observe.SimpleRoster

/**
 * Editing state of a single-column string list, free of Compose so the rules are
 * testable without a toolkit.
 *
 * Reproduces what the JavaFX tables in the settings window did: a new entry goes
 * to the top and becomes the selection, and an entry edited to blank disappears —
 * that was how the operator removed one.
 */
class EditableListState(initial: List<String>) {

    private val items: MutableList<String> = initial.toMutableList()

    val entries: List<String>
        get() = items.toList()

    var selectedIndex: Int? = null
        private set

    fun select(index: Int?) {
        selectedIndex = index?.takeIf { it in items.indices }
    }

    fun addAtTop(entry: String) {
        items.add(0, entry)
        selectedIndex = 0
    }

    /** An index outside the list is ignored: a pending edit can outlive its row. */
    fun updateAt(index: Int, text: String) {
        if (index !in items.indices) {
            return
        }
        if (text.isBlank()) {
            removeAt(index)
            return
        }
        items[index] = text
    }

    fun removeAt(index: Int) {
        if (index !in items.indices) {
            return
        }
        items.removeAt(index)
        if (selectedIndex?.let { it >= items.size } == true) {
            selectedIndex = items.indices.lastOrNull()
        }
    }

    /**
     * Fills the given roster. Deliberately setAll and not a field swap: replacing
     * the roster would orphan every listener registered on it, which is the same
     * rule the ChatPreferences setters follow since Etappe 2.
     */
    fun commitTo(roster: SimpleRoster<String>) {
        roster.setAll(items)
    }
}
```

- [ ] **Schritt 5: Test laufen lassen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.EditableListStateTest'`
Erwartet: PASS, 7 Testfälle.

- [ ] **Schritt 6: Die Composable schreiben**

An `DataTable.kt` anfügen. Zwei Punkte sind nicht verhandelbar:

- `LazyColumn` mit `key = { index, _ -> index }` ist **falsch** — bei einer Einfügung an Position 0 verschieben sich alle Schlüssel und die Auswahl springt. Der Schlüssel muss die **Identität des Eintrags** tragen. Weil Zeichenketten doppelt vorkommen können, bekommt jeder Eintrag beim Anlegen eine laufende Nummer, die `EditableListState` mitführt.
- Der Fokus geht beim Anlegen eines Eintrags ausdrücklich in das neue Textfeld, über einen `FocusRequester`. Nicht hoffen, dass es von selbst passiert: in Etappe 2 waren zwei von drei Fehlern Fokusfehler.

Ergibt sich daraus, dass `EditableListState` Einträge nicht als `String`, sondern als `(id, text)` führen muss, ist der Test aus Schritt 2 entsprechend nachzuziehen — **vor** der Umsetzung, und als `Ruling:` ins Ledger.

- [ ] **Schritt 7: Gesamtlauf und Commit**

Ausführen: `./gradlew clean build`
Erwartet: BUILD SUCCESSFUL, **272** Testfälle (265 + 7), 0 Fehler.

```bash
git add -A
git commit -m "Add an editable single-column table for Compose windows"
```

---

## Aufgabe 2: Das Sicherheitsnetz — Bestandsaufnahme der Einstellungsfelder

Deckt die Prüfschwerpunkte 1 und 2 ab. **Ohne diese Aufgabe ist der Rest nicht abnehmbar.**

**Dateien:**
- Anlegen: `app-desktop/src/test/resources/settings-fields.txt` (erzeugt, dann eingecheckt)
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/compose/SettingsFieldCoverageTest.kt`

**Schnittstellen:**
- Erzeugt: die Prüfung, an der jede folgende Aufgabe gemessen wird.

Das JavaFX-Einstellungsfenster ruft **72 verschiedene** `ChatPreferences`-Setter.

> Korrigiert am 2026-09-27: Der Plan nannte zuvor 93. Das waren rohe Setter-**Aufrufe** im Bereich, inklusive JavaFX-Steuerelementen wie `setDisable` und `setPrefWidth`. Die belastbare Zahl entsteht durch Schnitt mit den Settern, die `ChatPreferences` wirklich deklariert: **72**. Ein beim Portieren vergessenes Feld fällt nicht beim Übersetzen auf, sondern wenn ein Operator im Contest merkt, dass seine Einstellung weg ist. Diese Aufgabe macht daraus eine Zahl, die der Build prüft.

Korrektur zum Spec: es sind **elf** Reiter, nicht zwölf. `Macros` ist auskommentiert (`Kst4ContestApplication:12346`). Aktiv sind Station, Log synch, TRX synch, Airscout, Notification, Shortcuts, Beacon, Messagehandling, Workedstn database, GUI und Profiles.

- [ ] **Schritt 1: Die Bestandsaufnahme erzeugen**

Mechanisch, nicht nach Urteil: die Setter-Aufrufe im Bereich, geschnitten mit den Settern, die `ChatPreferences` wirklich deklariert. Damit fallen JavaFX-Steuerelement-Setter ohne Einzelfallentscheidung heraus.

```bash
awk 'NR>=9588 && NR<=12400' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
  | grep -oE "\.set[A-Za-z_0-9]+\(" | sed 's/^\.//;s/(//' | sort -u > /tmp/setters-raw.txt
grep -oE "public void (set[A-Za-z_0-9]+)\(" core/src/main/java/kst4contest/model/ChatPreferences.java \
  | sed 's/public void //;s/(//' | sort -u > /tmp/prefs-setters.txt
comm -12 /tmp/setters-raw.txt /tmp/prefs-setters.txt > app-desktop/src/test/resources/settings-fields.txt
wc -l < app-desktop/src/test/resources/settings-fields.txt
```
Erwartet: **72**.

Die Zeilennummern 9588 und 12400 vorher gegen den Ist-Stand prüfen:
```bash
grep -n "settingsStage = new Stage()" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
grep -n 'Tab tbProfiles = new Tab' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Dann die **ausgeschlossenen** durchsehen — `comm -23 /tmp/setters-raw.txt /tmp/prefs-setters.txt` — und bei jedem Namen prüfen, der nicht offensichtlich ein Steuerelement ist. Zwei Fälle sind belegt: `setLoginChatCategory` steht nur in einer auskommentierten Zeile, die echten Aufrufe heissen `…Main` und `…Second`; `setMYQRGFirstCat` fehlt zu Recht, die setzt die TRX-Synchronisation zur Laufzeit. Jede solche Prüfung als `Ruling:` ins Ledger — hier fällt ein echtes Feld unbemerkt heraus, wenn man es übergeht.

- [ ] **Schritt 2: Den Test schreiben, der die Abdeckung prüft**

`app-desktop/src/test/kotlin/kst4contest/view/compose/SettingsFieldCoverageTest.kt`:

```kotlin
package kst4contest.view.compose

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Guards against a setting quietly disappearing during the port.
 *
 * The JavaFX settings window called 93 distinct ChatPreferences setters. Every one
 * of them must still be called from the Compose settings sources, or an operator
 * loses a setting they could configure before — and nothing about that fails to
 * compile.
 */
class SettingsFieldCoverageTest {

    @Test
    fun `every setting the JavaFX window could write is still written`() {
        val expected = File("src/test/resources/settings-fields.txt")
            .readLines().map { it.trim() }.filter { it.isNotEmpty() }

        val composeSources = File("src/main/kotlin/kst4contest/view/compose")
            .walkTopDown().filter { it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        val missing = expected.filterNot { composeSources.contains("$it(") }

        assertTrue(missing.isEmpty(),
            "settings not written by any Compose source (${missing.size} of ${expected.size}):\n" +
                missing.joinToString("\n") { "  $it" })
    }
}
```

- [ ] **Schritt 3: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.SettingsFieldCoverageTest'`
Erwartet: FAIL, mit `settings not written by any Compose source (72 of 72)`. Das ist der Ausgangspunkt; jede folgende Aufgabe verkleinert die Liste.

> Der Test prüft Textvorkommen, nicht Laufzeitverhalten. Er kann nicht erkennen, ob ein Setter mit dem richtigen Wert gerufen wird — nur, dass er überhaupt gerufen wird. Das ist wenig, aber es ist die einzige Prüfung, die 93 Felder mechanisch abdeckt, und sie fällt sofort auf, wenn eines fehlt.

- [ ] **Schritt 4: Commit**

```bash
git add -A
git commit -m "Pin down which settings the port must keep writable"
```

---

## Aufgabe 3: Das Fenster und der erste Reiter

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/SettingsWindow.kt`
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/StationTab.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/tabs/StationTabStateTest.kt`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

**Schnittstellen:**
- Konsumiert: `Kst4ContestTheme` und den Weg aus Etappe 3a (`application(exitProcessOnExit = false)` in eigenem Thread).
- Erzeugt: `SettingsWindow.show(preferences, autosave, onProfileSwitch)` und das Muster `<Name>TabState`, dem die Aufgaben 4 bis 6 folgen.

Anders als der Profil-Dialog ist dieses Fenster **nicht blockierend**: der Operator arbeitet weiter, während es offen ist. Es darf daher nicht mit einem Latch auf das Schließen warten.

- [ ] **Schritt 1: Den heutigen Aufbau ablesen**

Ausführen: `sed -n '9588,9640p' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Notieren: Modalität, Größe, Titel, ob das Fenster wiederverwendet oder neu gebaut wird, und wie **Save Settings** angebunden ist. Laut `PROJECT_CONTEXT` ist **Save Settings** der vollständige Schreiber und enthält das aktuelle Layout — das muss so bleiben.

- [ ] **Schritt 2: Den Zustand des Station-Reiters mit Test**

Zuerst der Test. Er prüft, dass der Zustand aus `ChatPreferences` liest und **nur beim Speichern** zurückschreibt — nicht bei jedem Tastendruck, sonst landen unbestätigte Werte in der Datei. `PROJECT_CONTEXT` verlangt genau das: *„Selective layout writes … must not persist unconfirmed functional settings from the current UI."*

```kotlin
package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals

class StationTabStateTest {

    @Test
    fun `the state starts from the stored preferences`() {
        val prefs = ChatPreferences()
        prefs.stn_loginCallSign = "DN9APW"

        val state = StationTabState(prefs)

        assertEquals("DN9APW", state.callSign)
    }

    @Test
    fun `editing does not touch the preferences until apply`() {
        val prefs = ChatPreferences()
        prefs.stn_loginCallSign = "DN9APW"
        val state = StationTabState(prefs)

        state.callSign = "DO5AMF"

        assertNotEquals("DO5AMF", prefs.stn_loginCallSign,
            "an unconfirmed edit must not reach preferences.xml")
    }

    @Test
    fun `apply writes every field of this tab`() {
        val prefs = ChatPreferences()
        val state = StationTabState(prefs)

        state.callSign = "DO5AMF"
        state.apply()

        assertEquals("DO5AMF", prefs.stn_loginCallSign)
    }
}
```

Die Feldnamen gegen `ChatPreferences` prüfen, bevor der Test geschrieben wird — `grep -n "stn_login" core/src/main/java/kst4contest/model/ChatPreferences.java`.

- [ ] **Schritt 3: Fehlschlag sehen, dann umsetzen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.tabs.StationTabStateTest'`
Erwartet: FAIL — `StationTabState` existiert nicht.

Dann `StationTabState` und `StationTab` schreiben. **Alle** Felder des Reiters, nicht nur das Rufzeichen; die Liste aus `settings-fields.txt` und der Blick in `vbxStation` sagen, welche dazugehören.

- [ ] **Schritt 4: Das Fenster schreiben und anbinden**

`SettingsWindow` mit Reiterleiste, zunächst nur dem Station-Reiter, plus **Save Settings** und **Cancel**. Anbinden an denselben Menüpunkt, der heute `settingsStage` öffnet — das JavaFX-Fenster bleibt vorerst erreichbar, damit die noch nicht portierten Reiter benutzbar sind.

> Prüfschwerpunkt 5: solange beide Fenster existieren, können sie gegeneinander schreiben. Der Menüpunkt öffnet deshalb **entweder** das eine **oder** das andere, nie beide. Welches, entscheidet ein Schalter im Quelltext, der in Aufgabe 7 verschwindet.

- [ ] **Schritt 5: Prüfen**

Ausführen: `./gradlew build && ./gradlew :app-desktop:run`

Erwartet: Das Compose-Einstellungsfenster öffnet, zeigt den Station-Reiter mit den gespeicherten Werten, **Save Settings** schreibt sie, und nach einem Neustart sind sie noch da. Das Hauptfenster bleibt währenddessen bedienbar — das Fenster ist nicht blockierend.

Ausführen: `grep -c "configVersion>7" ~/.praktiKST/preferences.xml`
Erwartet: `1`.

- [ ] **Schritt 6: Commit**

```bash
git add -A
git commit -m "Open the settings window in Compose with the station tab"
```

---

## Aufgaben 4 bis 6: Die übrigen zehn Reiter

Drei Aufgaben, gruppiert nach Verwandtschaft, nicht nach Reihenfolge im Quelltext. Jede folgt demselben Muster wie Aufgabe 3 und endet damit, dass `SettingsFieldCoverageTest` weniger fehlende Felder meldet als vorher.

> **Warum hier kein Quelltext steht, anders als in den Aufgaben 1 bis 3.** Zehn Reiter mit zusammen 93 Feldern auszuschreiben würde diesen Plan verdoppeln und dabei nur wiederholen, was schon zweimal festgeschrieben ist: in `settings-fields.txt` steht, **welche** Felder es sind, und Aufgabe 3 zeigt an einem vollständigen Beispiel, **wie** ein Reiter gebaut wird. Was dem Umsetzer fehlen könnte, ist keine Vorlage, sondern die Zuordnung Feld zu Reiter — und die steht im Quelltext, nicht in einem Plan, der sie abschreibt.
>
> Das ist eine Abweichung von der Regel, dass jeder Schritt seinen Inhalt mitbringt. Sie ist bewusst: bei mechanischer Wiederholung ist eine geprüfte Vorlage plus eine mechanische Abnahme mehr wert als zehn abgeschriebene Formulare, in denen sich Fehler verstecken.

**Für jeden Reiter, in dieser Reihenfolge:**

1. Die Felder des Reiters aus `settings-fields.txt` und dem zugehörigen `vbx…`-Block in `Kst4ContestApplication` heraussuchen.
2. Test für `<Name>TabState` schreiben: liest aus den Preferences und schreibt **sofort** zurück, für **alle** Felder des Reiters. Kein `apply()`.
3. Fehlschlag sehen.
4. `<Name>TabState` und `<Name>Tab` schreiben.
5. Test grün.
6. Reiter in `SettingsWindow` eintragen.
7. `SettingsFieldCoverageTest` laufen lassen und die neue Zahl fehlender Felder ins Ledger.
8. **Den JavaFX-Block des Reiters auf Schreibstellen ausserhalb der Einstellungen durchsehen.** Das erste Netz sieht sie nicht. Gefundene Stellen als Rückruf in die Zustandsklasse und in `settings-foreign-writes.txt` nachtragen, falls sie dort noch fehlen.

### Aufgabe 4: Log synch, TRX synch, Airscout

**Dateien:** `tabs/LogSynchTab.kt`, `tabs/TrxSynchTab.kt`, `tabs/AirscoutTab.kt` samt Zustandsklassen und Tests.

Diese drei hängen an externen Schnittstellen. **Ports und Transporte werden nicht angetastet** — weder Vorgabewerte noch Wertebereiche. `AGENTS.md` ist hier ausdrücklich: Framing, Ports und Transportannahmen sind Protokollverhalten.

Achtung bei TRX synch: laut `PROJECT_CONTEXT` verlangen automatische QRG-Aktualisierungen **beide** Dinge — eine aktivierte Quelle **und** gültige eingehende Daten. Das Einschalten allein liefert keine QRG. Der Reiter darf daher nicht suggerieren, dass eine aktivierte Quelle schon eine Frequenz bedeutet.

### Aufgabe 5: Notification, Shortcuts, Messagehandling, Beacon

**Dateien:** `tabs/NotificationTab.kt`, `tabs/ShortcutsTab.kt`, `tabs/MessageHandlingTab.kt`, `tabs/BeaconTab.kt` samt Zustandsklassen und Tests.

**Hier kommt die `DataTable` aus Aufgabe 1 zum Einsatz**, dreimal:

| Liste | Roster in `ChatPreferences` |
|---|---|
| Überwachte Rufzeichen (Notification) | `lstNotify_QSOSniffer_sniffedCallSignList` (über `ChatController`) |
| Textkürzel (Shortcuts) | `lst_txtShortCutBtnList` |
| Textbausteine (Messagehandling) | `lst_txtSnipList` |

Beim Speichern wird `EditableListState.commitTo(roster)` gerufen — es füllt den vorhandenen Roster, statt ihn zu ersetzen. Ein Feldtausch würde angemeldete Zuhörer abwerfen; genau deshalb hat Etappe 2 die `ChatPreferences`-Setter auf `setAll` umgestellt.

Der Beacon-Reiter enthält die Vorgabetexte mit Platzhaltern wie `MYCALL` und `MYQRG`. Diese Zeichenketten sind **Literale und bleiben unverändert** — `MessageVariableResolver` löst sie auf, und eine geänderte Schreibweise bricht das still.

### Aufgabe 6: Workedstn database, GUI, Profiles

**Dateien:** `tabs/WorkedDatabaseTab.kt`, `tabs/GuiOptionsTab.kt`, `tabs/ProfilesTab.kt` samt Zustandsklassen und Tests.

Deckt Prüfschwerpunkt 4 ab.

**Workedstn database** enthält den Zurücksetzen-Knopf. Die Worked-Semantik ist unverändert: normalisierter Basisruf als Schlüssel, Worked-Zustand über Suffixvarianten geteilt, drei Tage Ablauf, manuelles Zurücksetzen. Ein Zurücksetzen fragt vorher nach.

**GUI** enthält unter anderem `GUIstationMapClusteringEnabled`. Laut `PROJECT_CONTEXT` ist das eine Layout-Einstellung unterhalb `guiOptions` mit Vorgabewert `true`, selektiv autogespeichert; fehlende oder fehlerhafte Werte behalten `true`. Diese drei Eigenschaften bleiben.

**Profiles** ist der schwierigste Reiter dieses Plans. Er ersetzt `OperatorProfileSettingsPane` (488 Zeilen) und löst einen **Laufzeitwechsel** aus: `ApplicationRuntimeLauncher` reißt die Laufzeit ab und baut eine neue `Kst4ContestApplication`. Das Compose-Einstellungsfenster muss dabei selbst verschwinden — es gehört zur alten Laufzeit.

Zu klären und als `Ruling:` festzuhalten: schließt das Fenster sich vor dem Wechsel selbst, oder räumt `shutdownRuntime()` es ab? Die zweite Variante braucht eine Rückmeldung von Compose nach Java. Die erste ist einfacher und vermutlich richtig.

> In Etappe 3a wurde bestätigt, dass ein Profilwechsel mit einem Compose-Fenster im Spiel funktioniert. Damals war das Fenster der Profil-Dialog, der ohnehin schließt. Hier bleibt ein Fenster offen — das ist der neue Teil.

Bekannter Nebenbefund, **vorbestehend**, der beim Prüfen im Protokoll erscheinen wird: Beim Profilwechsel stirbt der UCX-Log-UDP-Zuhörer der alten Laufzeit mit `NullPointerException`, `BindException` und der Meldung `Program Restart needed`, weil `ReadUDPbyUCXMessageThread` eine `while (true)`-Schleife ohne Abbruchbedingung hat. Für die neue Laufzeit kommt ein frischer Zuhörer hoch, die Funktion erholt sich. **Das ist nicht von dieser Etappe** und wird hier nicht behoben.

---

## Aufgabe 7: Das JavaFX-Einstellungsfenster entfernen

Deckt Prüfschwerpunkt 5 ab.

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` (`settingsStage` und der zugehörige Aufbau, etwa `:9588`–`:12400`)
- Löschen: `app-desktop/src/main/java/kst4contest/view/OperatorProfileSettingsPane.java` (488 Zeilen)

- [ ] **Schritt 1: Erst prüfen, dass nichts fehlt**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.SettingsFieldCoverageTest'`
Erwartet: PASS. **Solange dieser Test rot ist, wird nichts gelöscht.**

- [ ] **Schritt 2: Den Schalter und das alte Fenster entfernen**

Den Schalter aus Aufgabe 3 Schritt 4 löschen, den `settingsStage`-Aufbau entfernen, `OperatorProfileSettingsPane` löschen.

Ausführen: `grep -rn "settingsStage\|OperatorProfileSettingsPane" app-desktop/src`
Erwartet: keine Ausgabe.

- [ ] **Schritt 3: Zählstand**

Ausführen: `wc -l app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Die Datei begann Etappe 2 mit 13.564 Zeilen. Der neue Wert gehört ins Ledger: er ist das erste harte Maß dafür, dass die God-Klasse schrumpft.

- [ ] **Schritt 4: Die Abnahme von Hand**

Ausführen: `./gradlew clean build && ./gradlew :app-desktop:packageImage` — beziehungsweise `createDistributable`, je nachdem was Etappe 3a hinterlassen hat — und das Paket starten.

Alle elf Reiter durchgehen und in **jedem** mindestens einen Wert ändern. Dann **Save Settings**, Programm beenden, neu starten, und prüfen dass alle Änderungen noch da sind.

Ausführen danach:
```bash
grep -c "configVersion>7" ~/.praktiKST/preferences.xml
python3 -c "import xml.etree.ElementTree as E; E.parse('$HOME/.praktiKST/preferences.xml'); print('XML gültig')"
```
Erwartet: `1` und `XML gültig`.

**Vorher eine Sicherung anlegen**: `cp ~/.praktiKST/preferences.xml ~/.praktiKST/preferences.xml.vor-etappe3b`. Diese Aufgabe schreibt in die echte Konfiguration des Operators.

- [ ] **Schritt 5: Commit**

```bash
git add -A
git commit -m "Remove the JavaFX settings window"
```

---

## Abnahme der Etappe 3b

- [ ] `./gradlew clean build` läuft durch, 0 Fehler.
- [ ] `SettingsFieldCoverageTest` ist grün: alle 72 Einstellungen werden von Compose-Quellen geschrieben.
- [ ] `SettingsForeignWriteCoverageTest` ist grün: alle fünf Schreibstellen ausserhalb der Einstellungen werden ausgeführt.
- [ ] `grep -rn "settingsStage\|OperatorProfileSettingsPane" app-desktop/src` liefert keine Ausgabe.
- [ ] Alle elf Reiter sind bedienbar, und in jedem übersteht eine Änderung **Save Settings** plus Neustart.
- [ ] `preferences.xml` bleibt `configVersion 7`, ist gültiges XML, und unbekannte Knoten sind erhalten.
- [ ] Die drei editierbaren Listen können Einträge anlegen, ändern und durch Leeren entfernen — wie vorher.
- [ ] Der Profilwechsel über den Reiter **Profiles** funktioniert, und das Einstellungsfenster verschwindet dabei.
- [ ] `core` ist unberührt: `grep -rn "javafx" core/src` liefert nichts.
- [ ] Die neue Zeilenzahl von `Kst4ContestApplication.java` steht im Ledger.

## Was danach kommt

Etappe 4 nach Spec: Update-Fenster und Monitor-Fenster. Das Monitor-Fenster belastet die `DataTable` erstmals mit **Live-Daten** und mit mehreren Spalten — dort entsteht, was dieser Plan bewusst nicht gebaut hat: Spaltenbreiten mit stabilen IDs, Sortierung und Zeilen-Styling. Die Erfahrung aus diesem Plan gehört dann in dessen Entwurf.

## Befunde aus Gruppe B (Aufgaben 4–6)

Gruppe B hat ihre vier Tests geschrieben, bevor die Umsetzungen existierten. Ich
habe **jede** darin behauptete Verhaltensweise gegen den JavaFX-Quelltext geprüft,
bevor ich sie umgesetzt habe. Ergebnis: alle Behauptungen waren richtig. Belege:

| Behauptung | Belegstelle in `Kst4ContestApplication.java` |
|---|---|
| Platzhaltertext neuer Listeneinträge | `:11743` und `:11797`, in beiden Listen wörtlich gleich |
| Leerer Text entfernt den Eintrag | `ShortCol.setOnEditCommit`, `newValue.isBlank()` → `remove` |
| „Move selected up/down" existiert in beiden Listen | `:11753`, `:11760`, `:11806`, `:11813` |
| Eine Ankreuzfläche schreibt beide Kategorien | `:12004` und `:12005` |
| Ein Textfeld schreibt beide Kategorietexte | `:12039` und `:12040` |
| QRG-Antwort ist davon unabhängig | `:12026` |
| Port 1–65535, Vorwert bei Ablehnung | `:11243`–`:11282` |
| Neustart nur bei geändertem Port | `:11262`, `port != previousPort` |
| Spotter-Rufzeichen wird großgeschrieben und geprüft | `:11390`–`:11420` |
| Unbekanntes Frequenzpräfix wird auf 144 repariert | `:11349`–`:11356` |
| Testspot: `DO5AMF`, `300`, `DXC test: You donated $100!` | `:11469`–`:11473` |
| Überwachte Rufzeichen: Basisrufzeichen, **angehängt**, Doppelprüfung ohne Groß-/Kleinschreibung | `:11635`–`:11700` |
| Vorlage prüfen, Vorwert bleibt bei Ablehnung | `applyBeaconTextSetting`, `:5271` |
| Intervall in beide Kategorien, Zeitgeber nur bei Erfolg | `applySharedBeaconInterval`, `:5306` |

Zwei Stellen gehen bewusst über den JavaFX-Stand hinaus, beide dokumentiert im
Quelltext:

1. **`beaconIntervalMinutes` klemmt beim Lesen auf den Mindestwert.** JavaFX wandte
   `Math.max` nur im Fehlerzweig an; `ChatController:3425` klemmt aber ohnehin beim
   Senden. Ein gespeicherter Nullwert wurde also als gültige Einstellung angezeigt,
   obwohl gar nicht danach gesendet wurde.
2. **Das Auffrischen nach dem Hinzufügen eines Eintrags.** JavaFX frischte nur beim
   Bearbeitungsabschluss und beim Verschieben auf, nicht beim Hinzufügen — dort folgte
   das Bearbeiten aber unmittelbar. Beim Übergeben aufzufrischen ist derselbe Endstand.

### Was mein eigener Aufgabe-1-Bericht übersehen hatte

Ich hatte die Verschiebeknöpfe der Listen nicht erfasst. Gruppe B hat sie in
`EditableListState.moveSelected` vorgesehen; die Knöpfe existieren in beiden Listen
wirklich. Ohne die Prüfung hätte ich Bs Arbeit für erfunden gehalten.

### Grenzen der Netze, erneut bestätigt

Netz 2 sah die drei Testspot-Schreibvorgänge zunächst nicht, weil ich sie als
Kotlin-Eigenschaftszuweisung geschrieben hatte (`spot.qra = …`) und das Netz nach
`setQra(` sucht. Das Netz prüft **Text, nicht Verhalten**. Behoben, indem die
Java-Setzer namentlich aufgerufen werden — was hier ohnehin die treuere Entsprechung
ist. Wer künftig eine Zuweisung bevorzugt, muss das Netz mitändern, sonst wird es
still nutzlos.

### Stand

- Beide Netze grün: **72 von 72** Feldern erfasst, **5 von 5** Fremdschreibvorgängen.
- **362 Testfälle, 0 Fehler** (vorher 322 mit zwei absichtlich roten).
- Verdrahtung in `SettingsTabs.kt`, zehn Reiter in der Reihenfolge der JavaFX-`TabPane`.

### Offen

- `ProfilesTab` als Ersatz für `OperatorProfileSettingsPane` (488 Zeilen).
- Aufgabe 7: `settingsStage` und `OperatorProfileSettingsPane` löschen, den Aufruf
  von `buildSettingsTabs`/`SettingsWindow.show` in `Kst4ContestApplication` setzen und
  `SettingsHost` dort umsetzen. Neue Zeilenzahl festhalten (Anfang Etappe 2: 13 564).
- Abnahme von Hand: alle Reiter, je eine Änderung, „Save settings", Neustart,
  `configVersion 7` unversehrt. Vorher `~/.praktiKST/preferences.xml` sichern.

## Aufgabe 7: was der Plan nicht wusste

Der Plan sagte „`settingsStage` und `OperatorProfileSettingsPane` löschen". Beim
Lesen des Blocks kam heraus, dass das Einstellungsfenster **mehr war als
Einstellungen**: seine Knopfleiste trug

| Knopf | Wirkung |
|---|---|
| `btnOptionspnlConnect` | `chatcontroller.execute()` — die **einzige** Stelle, an der der Chat startet |
| `btnOptionspnlDisconnect` | `closeWindowEvent(null)`, Chat schließen |
| `btnOptionspnlDisconnectOnly` | `disconnect(DISCSTRING_DISCONNECTONLY)` |
| `btn_preferences_saveAsDefault` | Einstellungen sichern |
| `btnOptionsPnlApply` | Fenster schließen |

Deshalb stand `settingsStage.show()` im Startpfad und nicht an einem Knopf: **das
Einstellungsfenster war das Anmeldefenster.** Ein Löschen ohne Portieren hätte den
Anmeldeweg entfernt. `ConnectionBar.kt` bildet die Leiste nach, in derselben
Reihenfolge.

### Weitere Lücken, die dabei auffielen

1. **Die Kategorie-Auswahl fehlte im Compose-Station-Reiter.** `StationTabState`
   hatte `selectMainCategory`/`selectSecondCategory`, der Reiter zeigte sie nie.
   Ohne sie lässt sich der Chatraum nicht wählen — die Anwendung wäre unbenutzbar
   gewesen.
2. **Die Sperre der Anmeldefelder fehlte.** JavaFX sperrte Rufzeichen, Kennwort,
   Name, Locator und beide Kategorien, sobald die Sitzung angemeldet war: eine
   Änderung erreicht die laufende Verbindung nicht.
3. **Die Tabelle der gearbeiteten Stationen fehlte.** Ich hielt sie zunächst für
   tote Oberfläche, weil `tblVw_worked.setItems()` auskommentiert ist — falsch:
   `initWkdStnTable` setzt die Einträge selbst (`:5585`,
   `getLst_DBBasedWkdCallSignList()`). Jetzt nachgebaut, eine Zeile je Rufzeichen,
   eine Spalte je Band.
4. **Reihenfolge und Titel der Reiter waren bei mir falsch.** Ich hatte sortiert
   und umbenannt; der JavaFX-Quelltext warnt ausdrücklich davor
   („operators navigate these tabs by muscle memory"). Jetzt zeichengleich:
   Station, Log synch, TRX synch, Airscout, Notification, Shortcuts, Beacon,
   Messagehandling, Workedstn database, GUI, Profiles.

### Bewusste Abweichungen

- **„hide options" heißt jetzt „show options" und schaltet nicht um.** Ein
  Compose-Fenster kann auch am eigenen Rahmen geschlossen werden; eine Beschriftung,
  die den Zustand hier mitführt, würde veralten, ohne es zu erfahren.
- **Die Fenstergröße wird gelesen, aber nicht zurückgeschrieben.** JavaFX hatte
  Listener auf `widthProperty`/`heightProperty`, die `GUIsettingsStageSceneSizeHW`
  pflegten. Offen.
- **Grundschriftgröße 12sp festgelegt.** Es gibt keine Einstellung dafür; JavaFX
  nahm die Vorgabe der Plattform. Gleicher Wert wie im Profil-Wähler.
- **`refreshWorkedStationsView` ist absichtlich leer.** Die Compose-Tabelle liest
  den Roster selbst; die JavaFX-Tabelle brauchte den Aufruf nur, weil ein
  `TableView` Änderungen in seinen Objekten nicht bemerkt.

### Nebenbefund, nicht behoben

`finalTblVwWorked` wurde von der **leeren** `new TableView<>()` zugewiesen, bevor
`tblVw_worked = initWkdStnTable()` neu zuwies. Die drei `.refresh()`-Aufrufe trafen
also ein weggeworfenes Objekt. Sichtbar wurde das nie, weil der Roster-Spiegel die
angezeigte Tabelle ohnehin nachzieht. Mit dem Block gelöscht.

### Ergebnis

- `Kst4ContestApplication.java`: **13 755 → 10 881 Zeilen** (−2 874).
  Zum Vergleich: Anfang Etappe 2 waren es 13 564.
- `OperatorProfileSettingsPane.java` (488 Zeilen) gelöscht.
- **373 Testfälle, 0 Fehler.** Beide Netze grün.
- Rauchtest: Anwendung startet, Compose läuft, keine Ausnahme. Weiter als bis zum
  Profil-Wähler kam der Test ohne Klick nicht.

### Abnahme von Hand, offen

1. Profil wählen → Einstellungsfenster muss erscheinen.
2. **Anmelden**: Kategorie wählen, Verbinden. Der Knopf muss die Kategorie(n) im
   Text nennen. Danach müssen die Anmeldefelder gesperrt sein.
3. Trennen und „Disconnect & close Chat".
4. Je eine Änderung in allen elf Reitern, „Save settings", Neustart,
   `configVersion 7` unversehrt.
5. Profil-Reiter: anlegen, umbenennen, verdoppeln, Betriebsart der gearbeiteten
   Stationen ändern, wechseln. Löschen zuletzt und nur an einem Wegwerfprofil.
6. Menü „Options" und „Windows → show options" müssen das Fenster öffnen.

## Abnahme von Hand — durchgeführt am 2026-09-27

Abgenommen von Philipp (DN9APW) am laufenden Programm. Die Belege stammen aus einem
Vergleich der `preferences.xml` gegen eine Sicherung, nicht aus Augenschein allein.

### Persistenz

- **`configVersion` bleibt 7.** 138 Werte vor der Abnahme, 138 danach, keiner neu,
  keiner entfallen. Das war das Hauptrisiko beim Schreiben.
- `writePreferencesToXmlFile()` hat im ganzen Programm **genau einen** Aufrufer,
  `savePreferencesFromSettings()` in `Kst4ContestApplication`. Die geänderten Werte
  stehen also in der Datei, weil „Save settings" funktioniert — nicht, weil irgendein
  Automatismus nebenher geschrieben hätte. Die Layoutwerte sind davon getrennt:
  `LayoutAutosave` schreibt sie über `writeLayoutPreferencesToXmlFile()`.

### Alle neun Reiter mit Feldern sind belegt

| Reiter | Beleg in der XML |
|---|---|
| Station | `stn_antennaBeamWidthDeg` 50 → 20 |
| Log synch | `logsynch_ucxUDPWkdCallListenerEnabled`, `logsynch_wintestNetworkListenerEnabled` |
| TRX synch | `logsynch_wintestQrgSyncEnabled`, `logsynch_wintestUsePassQrg` |
| Airscout | `asQry_airScoutClientName` KST → KST4CONTEST |
| Notification | `notify_SimpleAudioNotificationsEnabled` true → false |
| Beacon | Intervall 5 → 7, beide Kategorien freigeschaltet |
| Messagehandling | `autoAnswerEnabled` und Antworttext, beide Kategorien |
| GUI | `guiOptions_darkModeActiveByDefault` true → false |
| Shortcuts | Listeneintrag `MYQRG` → `AAAA-TEST` |

Die zwei Reiter ohne Felder — **Workedstn database** und **Profiles** — wurden über
ihre Aktionen abgenommen.

### Was die Abnahme darüber hinaus bewiesen hat

**Die „ein Bedienelement schreibt beide Kategorien"-Umsetzungen stimmen.** Beacon-
Intervall, Beacon-Freigabe, automatische Antwort und Antworttext haben jeweils beide
Kategoriefelder geschrieben. Das war gegen den JavaFX-Quelltext geprüft, aber bis zur
Abnahme nie bis in die Datei verfolgt.

### Zwei Zuordnungsfehler bei der Auswertung

Beide sind es wert, notiert zu werden, weil sie dieselbe Ursache haben: **von einem
Namen auf die Zugehörigkeit zu schließen.**

1. `logsynch_wintestUsePassQrg` wurde nach dem Präfix dem Log-synch-Reiter
   zugeschlagen. Tatsächlich gehört es zu `TrxSynchTabState` — der TRX-synch-Reiter
   zeigt vier Win-Test-Felder, die in der XML unter `logsynch_` liegen.
2. Ein Skript, das Java-Setzernamen gegen XML-Elementnamen paarte, meldete vier
   Reiter als unbelegt. Die Namen unterscheiden sich:
   `setNotify_playSimpleSounds` schreibt `notify_SimpleAudioNotificationsEnabled`,
   `setGUI_darkModeActiveByDefault` schreibt `guiOptions_darkModeActiveByDefault`.

Wer die Abdeckung künftig auswertet, muss die Zugehörigkeit aus dem Quelltext der
`*TabState`-Klassen nehmen, nicht aus dem Feldnamen.

### Nebenbefund, nicht behoben

`ChatPreferences` schreibt einige Beacon-Werte doppelt: `bcn_beaconIntervalInMinutesMainCat`
und `beaconCQIntervalMinutes` tragen denselben Wert, ebenso die Freigabe-Flags.
Vorbestehend. Relevant für jeden, der die XML von Hand anfasst.
