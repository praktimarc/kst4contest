# JavaFX entfernen, Teil 3: Entlang der Schnittlinien trennen und den durchsetzten Aufbau löschen

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die drei gemischten Methoden, die Teil 2 mit einer Schnittlinie markiert hat, entlang dieser Linie trennen — der lebende Compose-Schreibweg bleibt, der nie gezeigte JavaFX-Teil fällt — und danach den gesamten durchsetzten Aufbau in `Kst4ContestApplication` in bauenden, geprüften Scheiben löschen, bis die Klasse auf ihre lebende Fläche zusammengeschrumpft ist.

**Architecture:** Teil 2 hat die harte Arbeit vorweggenommen: die drei Zuführungen (`TimelineFeed`, `SelectedStationMessagesFeed`, `ConnectionStateFeed`) existieren als benannte, getestete Einheiten, werden neben der Deklaration von `composeMainWindowState` erzeugt (nicht mehr verstreut in `start()`), und drei Methoden tragen den Kommentar „part 3 cuts at this line". Teil 3 ist deshalb kein Entwurf, sondern eine Ausführung: entlang gesetzter Linien schneiden, dann Blätter vor Wurzeln löschen. Jede Löschscheibe wird einzeln gebaut, mit dem Durchlauf-Skript vorgeprüft und mit einem Anwendungsstart abgenommen, weil kein Test beantwortet, ob ein entfernter Listener gefehlt hat.

**Tech Stack:** Java 21, Kotlin 2.2, Gradle, JUnit 5, Compose Multiplatform 1.8.2, JavaFX 21 (noch vorhanden).

**Spec:** `docs/superpowers/specs/2026-10-01-javafx-entfernen-design.md`

**Befund, auf dem jede Aufgabe aufsetzt:** `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

## Was Teil 2 geliefert hat — und was dieser Plan am echten Code vorfand

Dieser Plan wurde gegen den Code vom 2. Oktober 2026 geschrieben, nicht gegen die
Zeilennummern des Befunds. Die Datei ist seit dem 1. Oktober von ~11.655 auf **11.179
Zeilen** geschrumpft, und mehrere Stellen des Befunds sind durch Teil 2 überholt. Die
wichtigsten Abweichungen, damit der ausführende Agent nicht nach veralteten Zeilen
sucht:

- **Baseline ist 800 Tests, 0 Fehler** (nicht 769/773 wie im Befund und im Teil-2-Plan).
  `./gradlew clean build` ist grün, Stand 2026-10-02. Verifiziert über die XML unter
  `*/build/test-results/test/TEST-*.xml`: 106 Dateien, `tests=800 failures=0 errors=0
  skipped=3`. Die drei übersprungenen sind Generatoren (u. a. der `derive`-Generator aus
  Teil 1).
- **Scheibe B ist strukturell erledigt.** Die drei Feeds werden bei `:10133`–`:10138`
  erzeugt, unmittelbar nach `composeMainWindowState = new MainWindowState(...)` bei
  `:10078` — also genau in der „empfohlenen Endform" des Befunds.
  `FeedLifetimeIsTiedToTheWindowTest` hält das fest. **`openComposeMainWindowIfRequested` existiert unter
  diesem Namen nicht mehr**; der erzeugende Rumpf ist die Methode um `:10078`.
- **Die Zuführungs-Ziele sind bereits entkoppelt.** `updateTimelineVisuals` (`:4802`)
  hat keine Wache mehr auf `timelineView` am Compose-Datenpfad; es schiebt bei
  `:4839`–`:4846` in `timelineFeed`, wenn der nicht null ist, und berührt `timelineView`
  nur noch im eigenen, toten Zweig (`:4819`–`:4821`).
- **Die drei Schnittlinien sind im Code markiert.** `updateTimelineVisuals`,
  `updateConnectionStateIndicator` (`:5693`, Kommentar bei `:5736`: „part 3 cuts at this
  line") und `initFurtherInfoAbtCallsignMSGTable` (`:3220`, Kommentar bei `:3406`: „the
  binding itself still lives in this JavaFX builder - prising it out is part 3") sagen
  selbst, wo Teil 3 schneidet.
- **Scheibe D ist im Compose-Code fertig.** `MainMenuBar.kt` trägt alle Aktionen, das
  schreibgeschützte macOS-Verbindungsmenü und den auskommentierten Changelog-Eintrag
  (`openChangelog`, als „kept so the bar can say so" dokumentiert). Für Teil 3 bleibt
  nur, `initMenuBar` in der JavaFX-Datei zu löschen, nachdem der Abgleich bestätigt,
  dass nichts Portierbares verloren geht.
- **Scheibe E ist halb erledigt.** Die Doppel-Zustellung in
  `updateConnectionStateIndicator` ist schon auf „one write" reduziert
  (`:5712`–`:5717`), der Feed-Push sitzt oberhalb der Schnittlinie. Offen ist nur das
  Löschen des JavaFX-Indikators unterhalb der Linie, zusammen mit
  `initConnectionStateIndicatorButton`.

**Daraus folgt der Zuschnitt dieses Plans.** Scheibe B und D brauchen keine eigene
Entflechtungsaufgabe mehr. Übrig bleiben: eine echte Entflechtung (Scheibe C, die
Bindung aus dem Builder lösen), die Löschscheiben der `init…`-Methoden, das Schneiden
der drei gemischten Methoden an ihrer Linie und zuletzt die zeilenweise Zerlegung von
`start()`.

## Warum nur Teil 3

Die Spezifikation hat sechs Etappen. Teil 1 deckte Etappe 1 und 3 ab, Teil 2 den ersten
Abschnitt von Etappe 2. Dieser Plan ist der **Rest von Etappe 2**: der durchsetzte
Aufbau in `Kst4ContestApplication` wird gelöscht, bis die Klasse auf ihre lebende Fläche
zusammengeschrumpft ist. Was danach kommt — der `UiDispatcher`-Tausch (Etappe 4), der
Lebenszyklus samt der zehn `Alert`-Dialoge (Etappe 5), die Restdateien und die Befreiung
des Builds von `org.openjfx` (Etappe 6) — ist **nicht** Teil dieses Plans. Diese Etappen
sind erst schreibbar, wenn `start()` zerlegt ist; sie vorwegzunehmen hieße raten.

## Global Constraints

- Kommunikation mit dem Benutzer auf Deutsch; Quelltextkommentare und Javadoc
  ausschließlich auf Englisch (`AGENTS.md`).
- Keine neue Produktionsabhängigkeit ohne vorherige Freigabe (`AGENTS.md`).
- Kein Push, Merge, Tag oder Release. Die Commit-Schritte unten sind durch die Freigabe
  dieses Plans gedeckt, nichts darüber hinaus.
- **Nicht** aus Exit-Code 0 auf „alle Tests grün" schließen — die XML-Ergebnisse unter
  `*/build/test-results/test/TEST-*.xml` lesen (`AGENTS.md`). Das Zählskript steht in
  Aufgabe 0.
- Ausgangslage: `./gradlew clean build` → exit 0, **800 Tests, 0 Fehler**. Jede
  Rotfärbung ist ein Signal.
- Signierte Commits sind konfiguriert (`commit.gpgsign=true`, SSH). Schlägt das Signieren
  fehl, läuft der Bitwarden-Agent nicht — das ist kein Grund, unsigniert zu committen.
- `ObservableRoster` ist der Speicher, die Listen sind Spiegel. Kein Worker-Faden fasst
  UI-Sammlungen an. Zustellung nur über `UiDispatcher` (`AGENTS.md`).
- Zoom, Auswahl, Fokus, Sortierung und vorbelegter Text ändern sich nicht als
  Nebeneffekt (`AGENTS.md`).
- **Vor jeder Löschung** den Durchlauf laufen lassen:
  `python3 docs/superpowers/notes/javafx-compose-sweep.py`.
- Die 91 MP3-Dateien in `core/src/main/resources` **bleiben** — sie gehören der lebenden
  CW-Ausgabe in `PlayAudioUtils`, nicht dem toten `playCWLauncher`.

## Review Focus

Sechs Dinge, die die Spezifikation voraussetzt, die aber keine Aufgabe hier von sich aus
prüft, nach Wahrscheinlichkeit geordnet, dass sie jemanden treffen:

1. **Eine Löschung nimmt der Compose-Oberfläche still einen Anstoß.** Das ist die
   Fehlerklasse, die Teil 1 gefunden hat: kein Übersetzungsfehler, kein Testfehler, nur
   eine leere Tabelle oder eine eingefrorene Zeitleiste. Jede Löschscheibe bekommt
   deshalb einen Anwendungsstart als Abnahme, nicht nur einen grünen Build — und bei den
   drei Zuführungen zusätzlich eine konkrete Bedienhandlung (Station wählen, Verbindung
   wechseln, Sked eintreffen lassen).
2. **Ein Feld überlebt die Methode, die es allein benutzte.** Der Übersetzer meldet ein
   unbenutztes privates Feld nicht als Fehler. Jede Löschscheibe prüft deshalb mit einem
   Grep auf die Feldnamen, ob außerhalb des gelöschten Bereichs noch eine Fundstelle
   steht.
3. **Eine „tote" Methode war über einen Compose-Rückruf doch erreichbar.** Die
   Compose-Fenster rufen in `Kst4ContestApplication` zurück. Das Durchlauf-Skript prüft
   das transitiv bis Tiefe 3; jede Löschscheibe lässt es **vorher** laufen und bricht
   ab, wenn eine zu löschende Methode als `REACHES COMPOSE` gemeldet wird.
4. **Scheibe C löst die falsche Hälfte der Doppel-Zustellung.** `refresh()` **und** ein
   `Platform.runLater` schieben heute beide (`:320`/`:10381`); der Listener bei `:3410`
   schiebt zusätzlich. Aufgabe 1 benennt ausdrücklich, welcher Weg bleibt, und prüft mit
   einem Zähler, dass die ausgewählte Station nach dem Umbau genau einmal zugestellt
   wird.
5. **Die zeilenweise Zerlegung von `start()` lässt einen lebenden Listener fallen.** In
   `start()` liegen tote `timelineView`-Zeilen und lebende Auslöser dicht beieinander
   (`:6927`, `:6930`, `:6945`, `:9069`). Aufgabe 7 trennt sie einzeln und hält jeden
   lebenden Auslöser mit einer Begründung fest, warum er bleibt.
6. **Das Durchlauf-Skript meldet eine Methode fälschlich sauber, weil `COMPOSE_TOKENS`
   unvollständig ist.** Kommt in Teil 3 ein neues Compose-Fenster oder eine neue
   `replace…`-Senke dazu, ist die Liste im Skript zu pflegen. Aufgabe 0 prüft die Liste
   einmal gegen die heute existierenden Compose-Senken.

---

## File Structure

| Datei | Verantwortung | Aufgabe |
|---|---|---|
| `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` | **ändern** — Bindung entflechten, dann in Scheiben löschen | 1–7 |
| `app-desktop/src/test/kotlin/kst4contest/view/feed/SelectedStationMessagesFeedTest.kt` | **ändern** — ein Zähler-Test für die eine Zustellung (Review Focus 4) | 1 |
| `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md` | **ändern** — abgearbeitete Scheiben abhaken | 1–7 |

Keine neue Produktionsdatei. Teil 3 ist Entflechten und Löschen in **einer** Datei; die
Zuführungsklassen stehen schon.

---

## Task 0: Baseline festnageln und das Durchlauf-Skript prüfen

Keine Produktionsänderung. Diese Aufgabe stellt sicher, dass die folgenden Löschungen
auf einem belastbaren Netz stehen, und dass das Durchlauf-Skript die heutigen
Compose-Senken kennt.

**Files:**
- Read only: `docs/superpowers/notes/javafx-compose-sweep.py`,
  `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

- [ ] **Step 1: Den grünen Ausgangsstand bestätigen**

Run:
```bash
cd /home/philipp/Projects/ham/kst4contest
./gradlew clean build --console=plain > /tmp/t3-base.txt 2>&1; echo "exit=$?"
python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
t=f=e=s=0
for p in glob.glob("*/build/test-results/test/TEST-*.xml"):
    r=ET.parse(p).getroot()
    t+=int(r.get("tests")); f+=int(r.get("failures")); e+=int(r.get("errors")); s+=int(r.get("skipped"))
print(f"tests={t} failures={f} errors={e} skipped={s}")
PY
```

Erwartet: `exit=0`, `tests=800 failures=0 errors=0 skipped=3`. Weicht die Zahl ab, ist
der Ausgangsstand ein anderer als beim Schreiben dieses Plans — anhalten und die
Abweichung klären, bevor etwas gelöscht wird.

- [ ] **Step 2: Den Durchlauf laufen lassen und die drei lebenden Methoden sehen**

Run: `python3 docs/superpowers/notes/javafx-compose-sweep.py`

Erwartet: Genau drei `init…`-Methoden als `REACHES COMPOSE`:
`initConnectionStateIndicatorButton`, `initFurtherInfoAbtCallsignMSGTable`, `initMenuBar`.
Die Zeitleiste ist keine `init…`-Methode und erscheint nicht; sie wird in Aufgabe 7
behandelt. Meldet das Skript **mehr** als drei, ist eine neue Compose-Senke dazugekommen
und der Plan ist vor dem Löschen an ihr zu messen.

- [ ] **Step 3: `COMPOSE_TOKENS` gegen die heutigen Senken prüfen**

Run: `grep -nE "replace[A-Z]\w+|Window\.INSTANCE|composeMainWindowState" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java | grep -oE "replace[A-Z]\w+|[A-Z]\w+Window" | sort -u`

Erwartet: Jeder gefundene `replace…`-Name und jedes `…Window` steht in `COMPOSE_TOKENS`
im Skript. Fehlt einer (etwa `replaceRows`, `replaceSkeds`, `replaceCandidates`,
`StationMapWindow`), ist das Skript zu ergänzen, **bevor** es als Prüfung benutzt wird —
sonst meldet es eine Methode fälschlich sauber (Review Focus 6).

Keine Datei wird in dieser Aufgabe geändert und kein Commit erzeugt.

---

## Task 1: `selectedCallSignInfoMessageBinding` aus dem JavaFX-Builder lösen (Scheibe C)

Der Code sagt selbst, dass dies Teil 3 ist: der Kommentar bei `:3406` lautet „The
binding itself still lives in this JavaFX builder - prising it out is part 3". Danach
kann `initFurtherInfoAbtCallsignMSGTable` in Aufgabe 4 fallen, ohne die Nachrichtentabelle
der ausgewählten Station mitzunehmen.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/feed/SelectedStationMessagesFeedTest.kt`

**Interfaces:**
- Consumes: `derivedBinding(...)`, `selectedStationMessagesFeed.push(List<ChatMessage>)`,
  `rosterBindings`.
- Produces: nichts Neues. `selectedCallSignInfoMessageBinding` entsteht danach außerhalb
  der Tabellenkonstruktion.

**Vorgefundene Doppel-Zustellung (Review Focus 4):** Heute schieben drei Wege in die
Compose-Nachrichtentabelle — der Listener auf `selectedCallSignInfoMessageBinding.list()`
(`:3410`–`:3414`), und zusätzlich `applySelectedCallSignInfoFilter` (`:319`–`:324`) und
die Stationsauswahl (`:10380`–`:10385`), die beide `refresh()` rufen **und** danach selbst
`push(...)`. `refresh()` ändert die Spiegelliste und löst den Listener aus; der folgende
`push` stellt denselben Inhalt ein zweites Mal zu. **Welcher Weg bleibt:** der Listener.
Er ist der eine Schreibweg, der an die Datenänderung gekoppelt ist; die beiden
nachgezogenen `push`-Aufrufe sind die Doppelung. Diese Aufgabe entfernt die beiden
nachgezogenen `push`-Aufrufe **nicht** — das ist eine Verhaltensänderung über die
Entflechtung hinaus und gehört, falls erwünscht, in eine eigene, benannte Aufgabe. Sie
hält den Zustand nur mit einem Zähler-Test fest, damit eine spätere Auflösung messbar
ist.

- [ ] **Step 1: Die heutige Erzeugung und ihre drei Zuführungswege lesen**

Run:
```bash
sed -n '3395,3416p' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
sed -n '315,326p' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
sed -n '10374,10388p' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
grep -n "selectedCallSignInfoMessageBinding" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: Erzeugung über `derivedBinding(...)` innerhalb von `initFurtherInfoAbtCallsignMSGTable`
(um `:3402`), der Listener, der `selectedStationMessagesFeed.push(...)` ruft, und die
Nullsetzung bei `:6376`. Jede Fundstelle notieren — die Zeilen verschieben sich mit jeder
Löschung, also hier festhalten, nicht später raten.

- [ ] **Step 2: Eine eigene Erzeugungsmethode schreiben, die nichts von der Tabelle weiß**

Eine neue private Methode anlegen, die genau das tut, was heute mitten in
`initFurtherInfoAbtCallsignMSGTable` steht — die alte Bindung freigeben, die neue über
`derivedBinding` erzeugen, den Listener anmelden —, aber ohne jeden `TableView`-Bezug:

```java
	/**
	 * Creates the mirror that feeds the Compose "messages of the selected station" table.
	 *
	 * Lifted out of initFurtherInfoAbtCallsignMSGTable, which built the never-shown
	 * JavaFX table around it. The feed it drives does not depend on that table existing,
	 * so the binding must not either - otherwise deleting the table would silently empty
	 * the Compose table with a green build.
	 */
	private void rebuildSelectedCallSignInfoMessageBinding() {
		if (selectedCallSignInfoMessageBinding != null) {
			selectedCallSignInfoMessageBinding.dispose();
			rosterBindings.remove(selectedCallSignInfoMessageBinding);
		}
		selectedCallSignInfoMessageBinding = derivedBinding(
				chatcontroller::selectedCallSignInfoMessages,
				chatcontroller.getLst_globalChatMessageList());
		selectedCallSignInfoMessageBinding.list().addListener((javafx.collections.ListChangeListener<ChatMessage>) c -> {
			if (selectedStationMessagesFeed != null) {
				selectedStationMessagesFeed.push(selectedCallSignInfoMessageBinding.list());
			}
		});
	}
```

Den herausgelösten Block in `initFurtherInfoAbtCallsignMSGTable` durch einen Aufruf von
`rebuildSelectedCallSignInfoMessageBinding()` ersetzen, **bevor**
`tbl_furtherInfoAbtCallsignMSGTable.setItems(selectedCallSignInfoMessageBinding.list())`
steht — die tote Tabelle darf die Liste weiter beziehen, solange sie existiert; das
ändert sich erst mit ihrer Löschung in Aufgabe 4.

- [ ] **Step 3: Den Aufruf dorthin hängen, wo die anderen Zuführungen erzeugt werden**

In der Methode, die `composeMainWindowState` und die Feeds erzeugt (um `:10078`–`:10138`),
nach der Feed-Erzeugung `rebuildSelectedCallSignInfoMessageBinding()` aufrufen, falls die
Bindung dort noch nicht steht. **Vorsicht:** Die Bindung wird heute auch beim Öffnen des
Info-Fensters neu erzeugt (der Builder läuft je Auswahl). Prüfen, ob der neue Aufruf eine
Doppelerzeugung verursacht; ist das der Fall, nur den Builder-Aufruf behalten und diesen
Schritt überspringen — der Zähler-Test in Schritt 5 fängt eine Doppelerzeugung.

Run: `grep -n "rebuildSelectedCallSignInfoMessageBinding\|initFurtherInfoAbtCallsignMSGTable()" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: der neue Methodenname und seine Aufrufer sind sichtbar; die Erzeugung läuft
genau einmal pro Info-Fenster-Aufbau.

- [ ] **Step 4: Übersetzen**

Run: `./gradlew :app-desktop:compileJava --console=plain 2>&1 | tail -5`

Erwartet: `BUILD SUCCESSFUL`. Ein Übersetzungsfehler hier ist am billigsten zu beheben,
bevor Tests laufen.

- [ ] **Step 5: Einen Zähler-Test ergänzen, der die eine Zustellung festhält**

In `SelectedStationMessagesFeedTest.kt` einen Test ergänzen, der mit einem zählenden
`UiDispatcher` prüft, dass ein `push` genau eine Zustellung ist — dasselbe Muster, das
`TimelineFeedTest` schon benutzt (Review Focus 4). Der bestehende Test bleibt unberührt;
der neue hält fest, dass die Zuführungsklasse selbst nicht doppelt schreibt (die
Systemdoppelung über `refresh()` + nachgezogenem `push` ist davon unberührt und bleibt
bewusst bestehen — siehe Interfaces oben).

Run:
```bash
./gradlew :app-desktop:test --tests 'kst4contest.view.feed.SelectedStationMessagesFeedTest' --console=plain > /tmp/t3-1.txt 2>&1
python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
for f in glob.glob("app-desktop/build/test-results/test/TEST-*SelectedStationMessagesFeedTest.xml"):
    r=ET.parse(f).getroot(); print(f"tests={r.get('tests')} failures={r.get('failures')} errors={r.get('errors')}")
    for tc in r.iter("testcase"):
        for b in list(tc.iter("failure"))+list(tc.iter("error")): print(" ", tc.get("name"), (b.get("message") or "")[:200])
PY
```

Erwartet: `failures=0 errors=0`, Testzahl um eins höher als zuvor.

- [ ] **Step 6: Die ganze Suite und ein Anwendungsstart**

Run:
```bash
./gradlew clean build --console=plain > /tmp/t3-1b.txt 2>&1; echo "exit=$?"
python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
t=f=0
for p in glob.glob("*/build/test-results/test/TEST-*.xml"):
    r=ET.parse(p).getroot(); t+=int(r.get("tests")); f+=int(r.get("failures"))+int(r.get("errors"))
print(f"tests={t} failures={f}")
PY
```

Erwartet: `exit=0`, `failures=0`, `tests=801` (800 + 1).

Dann `./gradlew :app-desktop:run`: Anwendung starten, eine Station wählen, prüfen dass
sich die Nachrichtentabelle der ausgewählten Station füllt, einen Filter umschalten,
prüfen dass sie folgt. Beenden. **Das ist die Prüfung, die kein Test leistet** (Review
Focus 1).

- [ ] **Step 7: Den Befund abhaken und committen**

In `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md` die Scheibe C als erledigt
markieren.

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        app-desktop/src/test/kotlin/kst4contest/view/feed/SelectedStationMessagesFeedTest.kt \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Prise the selected-station message binding out of the JavaFX builder

initFurtherInfoAbtCallsignMSGTable built the never-shown JavaFX table and
created selectedCallSignInfoMessageBinding in the middle of doing so. The
Compose feed the binding drives does not depend on that table, so the
binding must not either - otherwise deleting the table in a later slice
would silently empty the Compose 'messages of the selected station' table
with a green build and no compile error, the exact failure class part 1
was written to prevent.

The binding now has its own builder, rebuildSelectedCallSignInfoMessage-
Binding, with no TableView reference. A counting-dispatcher test pins that
one push is one delivery inside the feed; the system-level double delivery
(refresh() plus a trailing push) is left as found and noted, because
resolving it changes behaviour beyond this disentanglement."
```

---

## Befund nach Task 1: der Rest-Zuschnitt ist anders als zuerst geplant

Beim Vorbereiten von Task 2 am echten Code (Stand 2026-10-02, nach den Commits bis
`03b9c1bd`) sind zwei Annahmen der ersten Planfassung widerlegt worden. Die Tasks 2–7
unten sind deshalb durch die Scheiben **S1–S6** ersetzt.

1. **Es gibt keine freistehenden Blatt-Methoden mehr.** Die vier unreferenzierten
   `init…`-Methoden, die die erste Fassung in Task 2 löschen wollte
   (`initShortcutTable`, `initNotifyAtCallSignTable`, `initTextSnippetsTable`,
   `initWkdStnTable`), sind bereits in Teil 2 gelöscht — ein Grep über den ganzen Baum
   findet sie **nullmal**. Task 2 der ersten Fassung ist gegenstandslos.
2. **Jeder verbliebene Tabellen-Builder hat genau einen Aufrufer, und der sitzt im toten
   `try`-Block von `start()`** (`:6961`–`:9028`; `primaryStage.setScene(...)` und
   `primaryStage.show()` sind beide auskommentiert, `:9019`/`:9028`). Methode und
   Aufrufer fallen also zusammen; „Blätter zuerst, Wurzeln zuletzt" greift nicht, weil es
   keine Blätter ohne Aufrufer gibt.
3. **Der lebende Compose-Pfad liegt außerhalb des toten Blocks.**
   `openComposeMainWindowIfRequested()` (erzeugt `MainWindowState`, die drei Feeds und
   die `composeMemberListener`/`composeChatListener`) wird **nach** dem `catch` gerufen
   (um `:9055`), ebenso `openMonitorWindow`, `openUpdateWindowIfAvailable`,
   `openSettingsWindow` und die Zeitleisten-Erstbefüllung
   `Platform.runLater(this::updateTimelineVisuals)` (um `:9079`). Der tote Szenenaufbau
   ist damit ein abgrenzbarer `try`-Block, dessen lebende Zuführungen daneben, nicht
   darin, liegen.

Die reglose Reihenfolge bleibt: risikoarm zuerst, die Mitgliedertabelle mit der
Filterleiste als heikelste Scheibe mit ausdrücklicher Listener-Prüfung, der
`start()`-Rest zuletzt. Jede Scheibe wird einzeln gebaut (`clean build` + XML), lässt
vorher den Durchlauf laufen, erhält jeden lebenden Listener belegt und wird committet.
Die GUI-Abnahme (Anwendungsstart) kann der ausführende Agent nicht selbst leisten; sie
wird gesammelt und am Ende von Marc gebündelt durchgeführt.

---

## S1: Die drei Statusknöpfe schneiden und löschen (Scheibe E)

`updateConnectionStateIndicator` ist gemischt: oben der lebende Feed-Push, unten der
JavaFX-Indikator. Der Code markiert die Linie selbst („part 3 cuts at this line", heute
um `:5745`). Die risikoärmste der Rest-Scheiben, deshalb zuerst.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Durchlauf und lebende Aufrufer erheben**

Run:
```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py | grep -iE "connection|sked|bandupgrade"
grep -n "updateConnectionStateIndicator\|initConnectionStateIndicatorButton\|initSkedWarnIndicatorButton\|initBandUpgradeIndicatorButton\|btnConnectionStateIndicator\|tipConnectionStateIndicator\|btnSkedWarnIndicator\|btnBandUpgradeIndicator" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: `updateConnectionStateIndicator` hat lebende Aufrufer außerhalb des toten
Aufbaus (die den Feed speisen) — diese bleiben. Die drei `init…`-Knopf-Methoden werden
nur aus `start()` gerufen (`:7116`/`:7122`/`:7145`).

- [ ] **Step 2: `updateConnectionStateIndicator` an der Linie trennen**

Alles unterhalb von „part 3 cuts at this line" (Tooltip, Button-Stil, macOS-Menütexte)
entfernen. Oben bleiben: Fadenprüfung, Normalisierung (`effectiveState`/`stateDetail`),
der eine Feed-Push, `logConnectionIndicatorTransition`. Die Normalisierung bleibt — sie
ist der Vertrag, den `ConnectionStateFeedTest` festhält. Die macOS-Menüfelder
(`menuConnectionStateMacOs`/`menuItemConnectionStateDetailMacOs`) werden nur in
`initMenuBar` eingehängt und fallen mit S2.

- [ ] **Step 3: Die drei Knopf-Builder und ihre `start()`-Aufrufer löschen**

`initConnectionStateIndicatorButton`, `initSkedWarnIndicatorButton`,
`initBandUpgradeIndicatorButton` samt Javadoc entfernen, die Aufrufzeilen in `start()`
mitnehmen, und die nur von ihnen genutzten Felder. Prüfen:

Run: `grep -n "btnConnectionStateIndicator\|tipConnectionStateIndicator\|btnSkedWarnIndicator\|btnBandUpgradeIndicator" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: nach dem Löschen keine Fundstelle außerhalb der gelöschten Bereiche. **Vorsicht
beim Verbindungsknopf:** er wird außerhalb des toten Blocks in die Statusleiste gehängt
(`!isMacOs()`), diese Zeile fällt mit.

- [ ] **Step 4: Übersetzen, Suite, Durchlauf**

Run:
```bash
./gradlew clean build --console=plain > /tmp/s1.txt 2>&1; echo "exit=$?"
python3 /tmp/count_tests.py
python3 docs/superpowers/notes/javafx-compose-sweep.py | grep -iE "connection|sked|bandupgrade"
```

Erwartet: `exit=0`, `tests=800 failures=0`, die drei Methoden erscheinen nicht mehr im
Durchlauf. GUI-Abnahme (Marc, gesammelt): Verbindung herstellen/trennen → die
Compose-Statusleiste wechselt weiter.

- [ ] **Step 5: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Cut updateConnectionStateIndicator at its seam and delete the status buttons

Above the line stays - thread check, normalisation, the single feed push,
the log. Below it goes, with initConnectionStateIndicatorButton and its two
neighbours and the fields only they touched. The Compose status bar is fed
from above the line, so its badge keeps changing. The macOS menu fields
fall with initMenuBar in S2."
```

---

## S2: `initMenuBar` löschen (Scheibe D ist im Compose-Code bereits erledigt)

`MainMenuBar.kt` trägt alle Aktionen schon — beim Schreiben dieses Plans am aktuellen
`MainMenuBar.kt` bestätigt: jede aktionstragende JavaFX-Position hat eine
Compose-Entsprechung; die drei Lücken sind ein Trenner, ein zusammengelegter
Doppeleintrag und der auskommentierte `openChangelog`. Nichts zu portieren, nur zu
löschen.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Den Abgleich am echten Code bestätigen**

Run:
```bash
grep -n "setOnAction\|menubar.getMenus()\|installSharedSystemMenuBar\|Macros" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java | sed -n '1,80p'
grep -nE "Item\(|Menu\(" app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuBar.kt
```

Erwartet: Jede `setOnAction`-tragende Position hat ein `Item(...)` in `MainMenuBar.kt`.
Eine Aktion ohne Compose-Entsprechung ist ein Fund — anhalten und in den Befund
schreiben, nicht löschen. Das tote `Macros`-Menü (gebaut, nie der Leiste hinzugefügt,
Handler auskommentiert) fällt mit, es wird nicht portiert.

- [ ] **Step 2: Durchlauf, dann löschen**

`initMenuBar`, `installSharedSystemMenuBar`, die macOS-Menüfelder und das tote
`Macros`-Menü samt dem `start()`-Aufruf `mainScreenMenuBar = initMenuBar()` entfernen.
Verwaiste Hilfsmethoden/Felder (`mainScreenMenuBar`, `flwpne_StatusBar`, soweit nur hier
benutzt, `macOsConnectionStateMenuTitle` falls `MainMenuBar.kt` eine eigene Entsprechung
hat) mitnehmen; im Zweifel bleibt die Hilfsmethode und der Fund kommt in die
Commit-Nachricht.

- [ ] **Step 3: Übersetzen, Suite, Durchlauf**

Run:
```bash
./gradlew clean build --console=plain > /tmp/s2.txt 2>&1; echo "exit=$?"
python3 /tmp/count_tests.py
python3 docs/superpowers/notes/javafx-compose-sweep.py | tail -3
```

Erwartet: `exit=0`, `tests=800 failures=0`, und `0 of N init methods reach Compose
state.` — zum ersten Mal erreicht keine `init…`-Methode mehr Compose-Zustand. GUI-Abnahme
(Marc, gesammelt): die Compose-Fenster-Öffner im Menü (Einstellungen, Monitor, Karte)
laufen weiter.

- [ ] **Step 4: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete initMenuBar; its port already lives in MainMenuBar.kt

Nothing to port: MainMenuBar.kt carries every action, the read-only macOS
connection menu and the commented-out changelog item. The three gaps are a
separator, a double entry Compose merged, and a long-commented action. With
this the sweep reports zero init methods reaching Compose - the interleaved
construction is fully separated. installSharedSystemMenuBar, the macOS menu
fields and the dead Macros menu go with it."
```

---

## S3: Die Nachrichtentabellen-Scheibe löschen

Die vier Nachrichten-/Cluster-Tabellen und ihr Tab-Container, alle nur aus dem toten
`try`-Block gerufen. Die lebenden `composeChatListener` liegen außerhalb (nach dem
`catch`) und bleiben.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Durchlauf und Aufrufer prüfen**

Run:
```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py | tail -3
grep -n "initChatprivateMSGTable\|initChatGeneralMSGTable\|initChatToOtherMSGTable\|initDXClusterTable\|initBottomGlobalMessageTabPane" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: keine dieser Methoden erreicht Compose; jede hat genau einen Aufrufer im toten
`try`-Block (`:7526`/`:7673`/`:7768`/`:3607`/`:3611`). Die zugehörigen toten
Selektionslistener (`privateChatselectionModel…`, `generalChatselectionModel…`) stehen
daneben und fallen mit.

- [ ] **Step 2: Löschen, verwaiste Felder mitnehmen**

Die fünf Methoden samt Javadoc und ihre `start()`-Aufrufzeilen entfernen, dazu die toten
Selektionslistener und die nur hier benutzten Felder (`tbl_generalMessageTable`,
`tbl_chatToOther…` usw.). Prüfen:

Run: `grep -n "tbl_generalMessageTable\|tbl_dxCluster\|tbl_chatToOther\|tbl_chatprivate\|bottomGlobalMessageTabPane" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: nach dem Löschen keine Fundstelle außerhalb der gelöschten Bereiche.

- [ ] **Step 3: Übersetzen, Suite**

Run:
```bash
./gradlew clean build --console=plain > /tmp/s3.txt 2>&1; echo "exit=$?"
python3 /tmp/count_tests.py
```

Erwartet: `exit=0`, `tests=800 failures=0`. GUI-Abnahme (Marc, gesammelt): die
Chat-/Cluster-Panes im Compose-Fenster füllen sich weiter (über `composeChatListener`).

- [ ] **Step 4: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete the never-shown message and cluster tables

The private, general, to-other and DX-cluster tables and their tab pane,
each called once from the dead try block in start(). The Compose chat and
cluster panes are fed by composeChatListener, which lives after the catch
and stays. Their dead selection listeners go with them."
```

---

## S4: `initFurtherInfoAbtCallsignMSGTable` und die Kompaktsteuerung löschen

Sicher erst nach Task 1 (Scheibe C): die lebende Bindung lebt seither in
`rebuildSelectedCallSignInfoMessageBinding`, nicht im Tabellen-Builder.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Durchlauf und Aufrufer prüfen**

Run:
```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py | grep -iE "furtherinfo|compact"
grep -n "initFurtherInfoAbtCallsignMSGTable\|initSelectedCallSignCompactControlsPane\|rebuildSelectedCallSignInfoMessageBinding" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: `initFurtherInfoAbtCallsignMSGTable` erreicht Compose jetzt **über**
`rebuildSelectedCallSignInfoMessageBinding`. Diese Hilfsmethode bleibt; der Tabellen-
Builder und seine Aufrufer (`:895`, `:1547`) sowie
`initSelectedCallSignCompactControlsPane` (`:1523`/`:3468`) fallen.

- [ ] **Step 2: Löschen, Bindungserzeugung sichern**

Beide Methoden samt Javadoc und ihre Aufrufer entfernen. **Kritisch:** Der Aufruf von
`rebuildSelectedCallSignInfoMessageBinding()` muss erhalten bleiben — er stand bisher im
Tabellen-Builder. Wird der Builder gelöscht, muss der Aufruf an eine lebende Stelle
wandern, die bei jeder Stationsauswahl läuft (`generateFurtherInfoAbtSelectedCallsignBP`
oder deren Aufrufer). Vor dem Löschen prüfen, wo `rebuild…` dann gerufen wird:

Run: `grep -n "rebuildSelectedCallSignInfoMessageBinding\|generateFurtherInfoAbtSelectedCallsignBP" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: `rebuild…` wird weiterhin bei jeder Auswahl gerufen;
`selectedCallSignInfoMessageBinding` bleibt lebend.

- [ ] **Step 3: Übersetzen, Suite**

Run:
```bash
./gradlew clean build --console=plain > /tmp/s4.txt 2>&1; echo "exit=$?"
python3 /tmp/count_tests.py
```

Erwartet: `exit=0`, `tests=800 failures=0`. GUI-Abnahme (Marc, gesammelt): Station
wählen → die Nachrichtentabelle der ausgewählten Station füllt sich weiter.

- [ ] **Step 4: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete the selected-callsign info table and its compact controls

Safe because S/Scheibe C moved the binding into rebuildSelectedCallSign-
InfoMessageBinding first. The Compose messages-of-the-selected-station
table is fed by that binding, not by this JavaFX table, so the table goes
while the feed stays. Verified by selecting a station after the deletion."
```

---

## S5: Die Mitgliedertabelle, das Kontextmenü und die Filterleiste löschen

Die größte und heikelste Scheibe: `initChatMemberTable` (die größte Methode),
`initChatMemberTableContextMenu` und die Filter-UI im toten `try`-Block. Hier liegen
lebende und tote Listener dicht beieinander.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Jeden Listener im Filterbereich tot/lebend einordnen**

Dies ist der eigentliche Prüfschritt der Scheibe und läuft **vor** jeder Löschung.

Run:
```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py | grep -iE "chatMember"
grep -n "getLst_chatMemberListFilterPredicates\|getStationFilter\|chatMemberTableFilter\|tglBtnQRBEnable\|tglGrpQTF\|selectionModelChatMember\|tbl_chatMember\b\|chatMemberContextMenu\|chatMessageContextMenu" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Für jeden Treffer entscheiden: speist er die **Compose**-Stationsliste (lebend, muss
bleiben oder umgehängt werden) oder nur die tote `TableView` (fällt)? Der Lebendbefund
legt nahe, dass die Compose-Filterung über `composeMainWindowState.getStationFilter()`
(`:9440` ff.) läuft und **nicht** über diese JavaFX-Filter-Listener — das ist hier zu
**bestätigen**, nicht anzunehmen. Jeder lebende Listener wird mit Fundstelle im Befund
notiert, bevor etwas fällt.

- [ ] **Step 2: Löschen, ggf. in zwei Commits (Kontextmenü, dann Tabelle+Filter)**

`initChatMemberTable`, `initChatMemberTableContextMenu`, die Filter-UI und die toten
Selektions-/Filter-Listener samt `start()`-Aufrufern entfernen. Das Feld
`chatMemberListBinding` und seine Erzeugung mitnehmen, sobald sein letzter Leser
(`setItems` der toten Tabelle) weg ist:

Run: `grep -n "chatMemberListBinding" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: nach dem Löschen keine Fundstelle außerhalb der gelöschten Bereiche. Steht ein
anderer Leser da, bleibt das Feld.

- [ ] **Step 3: Übersetzen, Suite**

Run:
```bash
./gradlew clean build --console=plain > /tmp/s5.txt 2>&1; echo "exit=$?"
python3 /tmp/count_tests.py
```

Erwartet: `exit=0`, `tests=800 failures=0`. GUI-Abnahme (Marc, gesammelt, **die
wichtigste**): die Compose-Stationsliste füllt sich, Filtern (QRB/QTF/Text) wirkt
weiter, Auswahl/Sortierung/`/cq`-Vorbelegung unverändert.

- [ ] **Step 4: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete the never-shown member table, its context menu and the filter bar

The roots of the window that is built every start and shown never. The
Compose station list is fed by composeMemberListener and filtered through
composeMainWindowState.getStationFilter(), both outside the dead block, so
they stay; the JavaFX table, its context menu, the filter listeners and
chatMemberListBinding - whose only reader was the dead table's setItems -
go together. Each live listener was classified before deletion; the finding
records them."
```

---

## S6: Den Rest von `start()` zerlegen

Der letzte Schnitt: der tote `timelineView`, die `scn_ChatwindowMainScene` samt Listenern
und `registerThemedScene`/`themedScenes`, der leer gewordene `try/catch`-Block, und die
tote CW-Ausgabe. Die drei lebenden Zeitleisten-Listener bleiben, von ihren
`timelineView`-Berührungen befreit.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

**Vorgefundene Verzahnung** (vor der Arbeit neu greppen):

| Stelle | Was | tot/lebend |
|---|---|---|
| `~:6878`–`:6885` | `timelineView = new TimelineView()`, Styling, Einhängen | **tot** |
| `~:6890`, `:6899` | `timelineView` Width/Height-Listener → `updateTimelineVisuals()` | **tot** |
| `~:6927` | `getActiveSkeds().addListener(... updateTimelineVisuals())` | **lebend** |
| `~:6930` | `getScoreService().uiPulse().addListener(... updateTimelineVisuals())` | **lebend** |
| `~:6941`, `:6942` | `timelineView.setCurrentAntennaAzimuth(...)`, `setBeamWidthDeg(...)` | **tot** |
| `~:6945`–`:6948` | `getActualQTF().addListener(...)`: `timelineView.setCurrentAntennaAzimuth` **und** `updateTimelineVisuals()` | **lebend, gemischt** |
| `~:9079` | `Platform.runLater(this::updateTimelineVisuals)` | **lebend** (Erstbefüllung) |
| `~:9849` | `return timelineView;` (Getter) | mit `timelineView` zu prüfen |

- [ ] **Step 1: Die Verzahnung am echten Code neu erheben**

Run:
```bash
grep -n "timelineView\|updateTimelineVisuals\|getActiveSkeds().addListener\|uiPulse().addListener\|getActualQTF().addListener\|scn_ChatwindowMainScene\|registerThemedScene\|themedScenes\|playCWLauncher\|javafx.scene.media" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Jede Zeile einem Topf zuordnen — tot (fällt) oder lebend (bleibt, oder der lebende Teil
einer gemischten Zeile bleibt).

- [ ] **Step 2: Die lebenden Auslöser vom toten Knoten trennen**

- `getActualQTF().addListener(...)` zu einem reinen `updateTimelineVisuals()`-Auslöser
  machen, den `timelineView.setCurrentAntennaAzimuth`-Aufruf entfernen.
  `updateTimelineVisuals` liest den Azimut ohnehin frisch aus den Preferences.
- `getActiveSkeds()`- und `uiPulse()`-Listener bleiben unverändert.
- Die Erstbefüllung `Platform.runLater(this::updateTimelineVisuals)` bleibt.

- [ ] **Step 3: Den toten `timelineView`, die tote Szene und den leeren Block löschen**

- `timelineView`-Erzeugung, Größen-Listener, Setter, Getter, Feld (`:213`) und den toten
  Zweig in `updateTimelineVisuals` entfernen; der Feed-Push bleibt.
- `scn_ChatwindowMainScene` samt Größen-/Key-Listenern, `registerThemedScene` und
  `themedScenes` entfernen, soweit der Grep keinen lebenden Nutzer zeigt.
- Den dann leeren/toten `try { … } catch`-Block in `start()` auflösen, ohne die lebenden
  Aufrufe danach (`openMonitorWindow`, `openComposeMainWindowIfRequested`,
  `openUpdateWindowIfAvailable`, `openSettingsWindow`, Erstbefüllung) zu berühren.
- Die tote CW-Ausgabe (`playCWLauncher`, `musicList`, `mediaPlayer`) und den Import
  `javafx.scene.media` nur löschen, wenn der Grep sie als unerreichbar belegt.
- **`TimelineView.java` selbst bleibt** (Etappe 6). Diese Scheibe entfernt nur seine
  Nutzung.

- [ ] **Step 4: Übersetzen, Suite**

Run:
```bash
./gradlew clean build --console=plain > /tmp/s6.txt 2>&1; echo "exit=$?"
python3 /tmp/count_tests.py
```

Erwartet: `exit=0`, `tests=800 failures=0`. GUI-Abnahme (Marc, gesammelt): die
Compose-Zeitleiste über dem Eingabefeld reagiert weiter auf eintreffende Skeds und
Rotordrehung.

- [ ] **Step 5: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Separate start(): drop the dead timeline node and scene, keep the triggers

The dead TimelineView node, the never-attached scn_ChatwindowMainScene with
its size and key listeners, registerThemedScene/themedScenes and the now-
empty try block are gone. The three live timeline triggers stay - the sked
listener, the uiPulse listener and the QTF listener, the last cut down to
the updateTimelineVisuals call it shares with a removed timelineView setter.
The dead CW launcher and the javafx.scene.media import go where the grep
proves nothing live reaches them. TimelineView.java itself stays for stage 6.

Verified by watching the Compose timeline update on an incoming sked and a
rotor turn - the signal a deleted live trigger would have silenced."
```

---

## Prüfung

Das Netz dieser Etappe:

- **800 Tests, 0 Fehler** als Ausgangsstand, nach jeder Scheibe neu gezählt über die XML
  — nicht aus Exit-Code 0 geschlossen.
- Das **Durchlauf-Skript** vor jeder Löschung; nach S2 meldet es `0 of N`.
- Eine **GUI-Abnahme** je Scheibe, von Marc gebündelt durchgeführt (der ausführende Agent
  kann die GUI nicht bedienen): Stationsliste, Nachrichtentabelle bei Auswahl,
  Verbindungsbadge, Zeitleiste auf Sked/Rotor, Menü-Fensteröffner, Design-/Profilwechsel.
- Die bestehenden Feed-Tests (`TimelineFeedTest`, `SelectedStationMessagesFeedTest`,
  `ConnectionStateFeedTest`, `FeedIsTheOnlyWriterTest`,
  `FeedLifetimeIsTiedToTheWindowTest`).

## Was unverändert bleibt

- Die Fachlogik in `core` und sämtliche Protokollformate.
- Die **Schnittstelle** `UiDispatcher` und ihre Umsetzung `JavaFxUiDispatcher` — der
  Tausch ist Etappe 4.
- Die Invarianten aus `AGENTS.md`: `ObservableRoster` ist der Speicher, die Listen sind
  Spiegel; kein Worker-Faden fasst UI-Sammlungen an; Zoom, Auswahl, Fokus, Sortierung und
  vorbelegter Text ändern sich nicht als Nebeneffekt.
- Die 91 MP3-Dateien und die lebende CW-Ausgabe in `PlayAudioUtils`.
- `TimelineView.java`, `PathProfileChart.java`, `StationMapView.java` und die übrigen
  JavaFX-Dateien als Dateien — nur ihre Nutzung in `Kst4ContestApplication` fällt.
- Die vorgefundene System-Doppelzustellung der Nachrichtentabelle — in Task 1
  festgehalten, nicht aufgelöst.

## Risiken

**Eine Löschung nimmt der Compose-Oberfläche still einen Anstoß.** Die Fehlerklasse, der
dieser Plan gilt. Jede Scheibe hat deshalb eine GUI-Abnahme, nicht nur einen grünen
Build. S5 (Mitgliedertabelle + Filterleiste) ist die gefährlichste, weil dort lebende und
tote Listener am dichtesten liegen; ihr Step 1 ordnet jeden Listener vor dem Schnitt ein.

**Die Zeilennummern altern mit jeder Scheibe.** Jede Scheibe erhebt ihre Stellen vor der
Arbeit neu per Grep; keine Löschung stützt sich auf eine notierte Zeile, nur auf den
Namen.

**Die GUI-Abnahme ist ausgelagert.** Der ausführende Agent kann die Anwendung nicht
bedienen; die Abnahme je Scheibe liegt bei Marc. Bis dahin ist ein grüner Build ein
notwendiger, aber kein hinreichender Nachweis.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Den Durchlauf laufen lassen**

Run: `python3 docs/superpowers/notes/javafx-compose-sweep.py`

Erwartet: unverändert drei `REACHES COMPOSE`. Keine der hier zu löschenden Methoden darf
darunter sein.

- [ ] **Step 2: Für jede verdächtige `init…`-Methode die Aufrufer am echten Code zählen**

Run:
```bash
for m in initDXClusterTable initChatToOtherMSGTable initBottomGlobalMessageTabPane \
         initChatGeneralMSGTable initChatprivateMSGTable initChatMemberTable \
         initChatMemberTableContextMenu initSelectedCallSignCompactControlsPane \
         initTopPriorityListPane initSkedWarnIndicatorButton initBandUpgradeIndicatorButton; do
  echo "--- $m"
  grep -rn "\b$m\b" --include='*.java' --include='*.kt' app-desktop/src core/src
done
grep -cE "getDeclaredMethod|getMethod\(|Class\.forName|FXMLLoader" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: Für jede Methode genau die Deklaration und ihre echten Aufrufer; die letzte
Zeile ist `0` (keine reflektive oder FXML-Hintertür). **Diese Scheibe löscht nur
Methoden, deren einziger Aufrufer selbst in derselben Scheibe gelöscht wird oder die gar
keinen Aufrufer haben.** Welche das sind, entscheidet die Ausgabe — nicht der Befund,
dessen Zeilen veraltet sind. Methoden, die noch von lebendem Compose-Code erreicht
werden, bleiben für spätere Scheiben.

- [ ] **Step 3: Die Blatt-Scheibe löschen**

Die in Schritt 2 als blattartig bestätigten Methoden vollständig entfernen, je mit
Javadoc-Block. Die Konstruktionskette von `scn_ChatwindowMainScene` baut diese Tabellen
ineinander; eine Methode, die eine noch lebende Methode aufruft, bleibt. Blätter zuerst:
die Tabellen, die niemand mehr baut, bevor der Rahmen fällt, der sie baute.

- [ ] **Step 4: Verwaiste Felder mitnehmen**

Run:
```bash
grep -nE "tbl_dxClusterTable|tbl_chatToOtherMSGTable|tbl_chatGeneralMSGTable|tbl_chatprivateMSGTable|tbl_chatMemberTable" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: Jedes Feld, dessen einzige Nutzer gelöscht wurden, erscheint nur noch in seiner
Deklaration — dann mit entfernen. Bleibt eine Fundstelle außerhalb der gelöschten
Bereiche, gehört das Feld jemandem und bleibt (Review Focus 2).

- [ ] **Step 5: Übersetzen, Suite, Anwendungsstart**

Run:
```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py | tail -3
./gradlew clean build --console=plain > /tmp/t3-2.txt 2>&1; echo "exit=$?"
python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
t=f=0
for p in glob.glob("*/build/test-results/test/TEST-*.xml"):
    r=ET.parse(p).getroot(); t+=int(r.get("tests")); f+=int(r.get("failures"))+int(r.get("errors"))
print(f"tests={t} failures={f}")
PY
```

Erwartet: `exit=0`, `failures=0`, Testzahl **unverändert** (die gelöschten Methoden hatte
kein Test berührt). Dann `./gradlew :app-desktop:run`: Anwendung startet, Stationsliste
füllt sich, keine `NullPointerException`/`NoSuchMethodError` auf der Konsole. Beenden.

- [ ] **Step 6: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete the never-shown station and message table builders

The slices from the live-code finding, confirmed against today's tree:
each deleted builder's only reachable caller was itself deleted in the
same slice, and the sweep script reports none of them reaching Compose
state. getDeclaredMethod, getMethod(, Class.forName and FXMLLoader are
still zero in the file, so there is no reflective back door either.

The test count did not move: nothing ever exercised this construction.
That is the premise the whole removal rests on, re-proven on a tree that
is now several slices smaller than when it was first stated."
```

---

## Task 3: Die Nachrichten- und Kompaktsteuerungs-Scheibe löschen

Nach Aufgabe 1 ist `initFurtherInfoAbtCallsignMSGTable` von seiner lebenden Zuführung
getrennt und kann mit seiner Nachbarschaft fallen.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Durchlauf und Aufrufer prüfen**

Run:
```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py | grep -E "FurtherInfo|SelectedCallSignCompact"
grep -n "initFurtherInfoAbtCallsignMSGTable\|initSelectedCallSignCompactControlsPane\|tbl_furtherInfoAbtCallsignMSGTable" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: `initFurtherInfoAbtCallsignMSGTable` wird vom Durchlauf **nicht mehr** als
`REACHES COMPOSE` gemeldet (Aufgabe 1 hat die Compose-Senke entfernt). Steht es noch dort,
ist Aufgabe 1 unvollständig — anhalten. Die verbliebenen Aufrufer müssen selbst in dieser
oder einer früheren Scheibe gelöscht sein.

- [ ] **Step 2: Löschen, Felder mitnehmen, Binding-Erzeugung sichern**

Die beiden Methoden samt Javadoc entfernen. **Kritisch:** `selectedCallSignInfoMessageBinding`
darf durch das Löschen von `initFurtherInfoAbtCallsignMSGTable` **nicht** verschwinden —
seine Erzeugung lebt seit Aufgabe 1 in `rebuildSelectedCallSignInfoMessageBinding()`.
Prüfen, dass dieser Aufruf erhalten bleibt:

Run: `grep -n "rebuildSelectedCallSignInfoMessageBinding\|selectedCallSignInfoMessageBinding" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: die Bindung wird weiter in `rebuildSelectedCallSignInfoMessageBinding` erzeugt
und vom Feed-Listener benutzt; der `setItems`-Aufruf auf die gelöschte Tabelle ist weg.

- [ ] **Step 3: Übersetzen, Suite, Anwendungsstart**

Run: wie Aufgabe 2, Schritt 5.

Erwartet: `exit=0`, `failures=0`, Testzahl unverändert gegenüber Aufgabe 1 (801). Beim
Anwendungsstart **erneut** eine Station wählen und prüfen, dass die Nachrichtentabelle
sich füllt — das ist die Scheibe, bei der ein Fehler in Aufgabe 1 sichtbar würde.

- [ ] **Step 4: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete the selected-callsign info table and its compact controls

Safe only because task 1 moved selectedCallSignInfoMessageBinding into its
own builder first. The Compose 'messages of the selected station' table is
fed by that binding, not by this JavaFX table, so the table goes while the
feed stays. Verified by selecting a station after the deletion and watching
the Compose table fill."
```

---

## Task 4: Die großen Tabellen-Scheiben löschen (Mitglieder-, Chat-, Kontextmenü)

Die Wurzeln des nie gezeigten Fensters: `initChatMemberTable` (die größte Methode) und
ihr Kontextmenü, die privaten und allgemeinen Chat-Tabellen. Sie fallen, nachdem die
Blätter weg sind.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Durchlauf und Aufrufer prüfen**

Run:
```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py | tail -3
grep -n "initChatMemberTable\b\|initChatMemberTableContextMenu\|initChatprivateMSGTable\|initChatGeneralMSGTable" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
grep -n "chatMemberListBinding" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: keine dieser Methoden erreicht Compose. `chatMemberListBinding` (das Feld, das
der Befund als „mit der toten Tabelle löschbar" ausweist) hat als einzigen Leser den
`setItems`-Aufruf der toten Mitgliedertabelle — der mit ihr fällt. Steht ein anderer
Leser da, bleibt das Feld.

- [ ] **Step 2: Löschen, in sinnvoller Teilung**

`initChatMemberTable` ist groß genug, um die Scheibe bei Bedarf in zwei Commits zu teilen
(Kontextmenü zuerst, dann die Tabelle). Jede Teillöschung einzeln bauen. Das Feld
`chatMemberListBinding` und seine Erzeugung mitnehmen, sobald sein letzter Leser weg ist.

- [ ] **Step 3: Übersetzen, Suite, Anwendungsstart**

Run: wie Aufgabe 2, Schritt 5.

Erwartet: `exit=0`, `failures=0`, Testzahl unverändert. Beim Anwendungsstart die
Stationsliste und die Chat-Tabellen im Compose-Fenster prüfen — sie werden von den
Compose-Listenern (`composeMemberListener`, `composeChatListener`) gespeist, nicht von
diesen gelöschten JavaFX-Tabellen; sie müssen sich weiter füllen.

- [ ] **Step 4: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete the never-shown member and chat tables

The roots of the window that is built every start and shown never. The
Compose station list and chat panes are fed by composeMemberListener and
composeChatListener, which stay; these JavaFX tables and chatMemberList-
Binding, whose only reader was the dead table's setItems, go together.
Verified by watching the Compose lists fill after the deletion."
```

---

## Task 5: Die drei Statusknöpfe schneiden und löschen (Scheibe E inbegriffen)

`initConnectionStateIndicatorButton` ist gemischt: oben der lebende Feed-Push, unten der
JavaFX-Indikator. Der Code markiert die Linie bei `:5736` („part 3 cuts at this line").
`initSkedWarnIndicatorButton` und `initBandUpgradeIndicatorButton` sind die beiden
Nachbarn.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Die Schnittlinie und die lebenden Aufrufer lesen**

Run:
```bash
sed -n '5693,5760p' app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
grep -n "updateConnectionStateIndicator\|initConnectionStateIndicatorButton\|btnConnectionStateIndicator\|tipConnectionStateIndicator\|menuConnectionStateMacOs\|menuItemConnectionStateDetailMacOs" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: `updateConnectionStateIndicator` wird außerhalb des toten Aufbaus gerufen (die
lebenden Aufrufer, die den Feed speisen). Diese Aufrufer **bleiben**; nur der
JavaFX-Rumpf unterhalb der Schnittlinie fällt. Die macOS-Felder `menuConnectionStateMacOs`
/ `menuItemConnectionStateDetailMacOs` werden nur in `initMenuBar` eingehängt und fallen
mit Aufgabe 6.

- [ ] **Step 2: `updateConnectionStateIndicator` an der Linie trennen**

Alles unterhalb von „part 3 cuts at this line" (`:5736` ff.: Tooltip, Button-Stil,
macOS-Menütexte) entfernen. Oben bleiben: die Fadenprüfung, die Normalisierung
(`effectiveState`/`stateDetail`), der eine Feed-Push und `logConnectionIndicatorTransition`.
**Die Normalisierung bleibt**, auch wenn sie damit nur noch den Feed-Push und das Log
speist — sie ist der Vertrag, den `ConnectionStateFeedTest` festhält.

Prüfen, ob die Methode nach dem Schnitt noch `tipConnectionStateIndicator` oder
`btnConnectionStateIndicator` berührt; wenn nicht, können diese Felder mit
`initConnectionStateIndicatorButton` fallen.

- [ ] **Step 3: Die drei Knopf-Builder löschen**

`initConnectionStateIndicatorButton`, `initSkedWarnIndicatorButton`,
`initBandUpgradeIndicatorButton` samt Javadoc entfernen, und die nur von ihnen benutzten
Felder (`btnConnectionStateIndicator`, `tipConnectionStateIndicator` und die beiden
Nachbarknöpfe) mitnehmen. Prüfen:

Run: `grep -n "btnConnectionStateIndicator\|tipConnectionStateIndicator\|btnSkedWarn\|btnBandUpgrade" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: nach dem Löschen keine Fundstelle mehr außerhalb der gelöschten Bereiche.

- [ ] **Step 4: Übersetzen, Suite, Anwendungsstart**

Run: wie Aufgabe 2, Schritt 5.

Erwartet: `exit=0`, `failures=0`, Testzahl unverändert. Beim Anwendungsstart die
Verbindung herstellen/trennen und prüfen, dass die Verbindungsanzeige in der
**Compose**-Statusleiste weiter wechselt — der Feed-Push oberhalb der Linie speist sie,
der gelöschte JavaFX-Indikator war nie sichtbar (Review Focus 1).

- [ ] **Step 5: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Cut updateConnectionStateIndicator at its seam and delete the buttons

The method mixed a live feed push with the JavaFX indicator of the window
that is never shown; the code already marked where part 3 cuts. Above the
line stays - thread check, normalisation, the single feed push, the log.
Below it goes, with initConnectionStateIndicatorButton and its two
neighbours and the fields only they touched.

The Compose status bar's connection badge is fed from above the line, so
it keeps changing; verified by connecting and disconnecting after the cut.
The macOS menu fields fall with initMenuBar in the next task."
```

---

## Task 6: `initMenuBar` löschen (Scheibe D ist im Compose-Code bereits erledigt)

`MainMenuBar.kt` trägt alle Aktionen schon — der Abgleich ist im Befund gemacht und beim
Schreiben dieses Plans am aktuellen `MainMenuBar.kt` erneut bestätigt: jede
aktionstragende JavaFX-Position hat eine Compose-Entsprechung; die drei Lücken sind ein
Trenner, ein in Compose zusammengelegter Doppeleintrag und ein seit Längerem
auskommentierter Eintrag (`openChangelog`, in `MainMenuBar.kt` ausdrücklich „kept so the
bar can say so"). Es ist also nichts zu portieren, nur zu löschen.

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

- [ ] **Step 1: Den Abgleich am echten Code bestätigen**

Run:
```bash
grep -n "initMenuBar\|installSharedSystemMenuBar\|menubar.getMenus()\|setOnAction" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java | sed -n '1,60p'
grep -nE "Item\(|Menu\(" app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuBar.kt
```

Erwartet: Jede `setOnAction`-tragende JavaFX-Position hat ein `Item(...)` in
`MainMenuBar.kt`. Taucht eine Aktion in JavaFX auf, die in Compose fehlt, ist das ein
Fund — anhalten und ihn in den Befund schreiben, **nicht** löschen. Das tote `Macros`-Menü
(gebaut, nie der Leiste hinzugefügt, Handler auskommentiert) fällt mit, es wird nicht
portiert.

- [ ] **Step 2: Den Durchlauf prüfen und löschen**

Run: `python3 docs/superpowers/notes/javafx-compose-sweep.py | grep -i menubar`

Erwartet: `initMenuBar` ist zu diesem Zeitpunkt die einzige verbliebene `REACHES
COMPOSE`-Meldung — und das zu Recht, weil seine Einträge Compose-Fenster öffnen. Das
Löschen entfernt diese Aufrufe mit; die Compose-Fenster werden weiter über
`ComposeMenuActions` / `MainMenuBar.kt` geöffnet, nicht über diese JavaFX-Leiste.
`initMenuBar`, `installSharedSystemMenuBar` und die macOS-Menüfelder samt dem toten
`Macros`-Menü entfernen.

- [ ] **Step 3: Verwaiste Felder und Hilfsmethoden mitnehmen**

Run: `grep -nE "menubar\b|macOsConnectionStateMenuTitle|menuConnectionStateMacOs|menuItemConnectionStateDetailMacOs|fileMenu|optionsMenu|windowMenu|helpMenu" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: Felder und Hilfsmethoden, deren einzige Nutzer in `initMenuBar` standen,
erscheinen nur noch in ihrer Deklaration — mit entfernen. **`macOsConnectionStateMenuTitle`
prüfen:** falls `MainMenuBar.kt` eine eigene Entsprechung hat
(`ConnectionIndicator.macOsMenuTitle`), ist die Java-Fassung verwaist und fällt; falls nicht, bleibt sie, bis Etappe 5
den macOS-Pfad klärt. Im Zweifel bleibt sie und der Fund kommt in die Commit-Nachricht.

- [ ] **Step 4: Übersetzen, Suite, Anwendungsstart**

Run: wie Aufgabe 2, Schritt 5.

Erwartet: `exit=0`, `failures=0`, Testzahl unverändert. Beim Anwendungsstart jeden
Compose-Fenster-Öffner aus dem Menü prüfen (Einstellungen, Monitor, Karte) — sie laufen
über `MainMenuBar.kt`, nicht über die gelöschte Leiste.

- [ ] **Step 5: Durchlauf-Erfolg festhalten**

Run: `python3 docs/superpowers/notes/javafx-compose-sweep.py | tail -3`

Erwartet: `0 of N init methods reach Compose state.` — zum ersten Mal erreicht **keine**
`init…`-Methode mehr Compose-Zustand. Das ist die messbare Abnahme, dass der durchsetzte
Aufbau getrennt ist.

- [ ] **Step 6: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Delete initMenuBar; its port already lives in MainMenuBar.kt

Nothing to port: MainMenuBar.kt carries every action the JavaFX bar did,
the read-only macOS connection menu, and even the commented-out changelog
item, kept there so the bar can say so. The three gaps are a separator, a
double entry Compose merged, and an action that has been commented out for
some time.

With this the sweep reports zero of the init methods reaching Compose
state - the first time the interleaved construction is fully separated.
installSharedSystemMenuBar, the macOS menu fields and the dead Macros menu
(built, never added, handlers commented out) go with it."
```

---

## Task 7: `start()` Zeile für Zeile zerlegen

Der letzte und heikelste Schnitt dieser Etappe. In `start()` (`:6855` ff.) liegen tote
`timelineView`-Zeilen und **lebende** Auslöser dicht beieinander. `start()` wird nicht am
Stück beschnitten, sondern Zeile für Zeile getrennt (Review Focus 5).

**Files:**
- Modify: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

**Die vorgefundene Verzahnung** (Stand 2026-10-02, Zeilen verschieben sich mit den
früheren Scheiben — vor der Arbeit neu greppen):

| Stelle | Was | tot/lebend |
|---|---|---|
| `:6878`–`:6885` | `timelineView = new TimelineView()`, Styling, Einhängen in `pnl_inputAndSendButtons` | **tot** — der Knoten wird nie gezeigt |
| `:6890`, `:6899` | `timelineView` Width/Height-Listener → `updateTimelineVisuals()` | **tot** — feuern nur, wenn der tote Knoten Größe bekommt |
| `:6927` | `chatcontroller.getActiveSkeds().addListener(... updateTimelineVisuals())` | **lebend** — speist die Compose-Zeitleiste |
| `:6930` | `chatcontroller.getScoreService().uiPulse().addListener(... updateTimelineVisuals())` | **lebend** |
| `:6941`, `:6942` | `timelineView.setCurrentAntennaAzimuth(...)`, `setBeamWidthDeg(...)` | **tot** — füttern nur den toten Knoten |
| `:6945`–`:6948` | `getActualQTF().addListener(...)`: ruft `timelineView.setCurrentAntennaAzimuth` **und** `updateTimelineVisuals()` | **lebend, gemischt** — der `updateTimelineVisuals`-Teil bleibt, der `timelineView`-Teil fällt |
| `:7823` | `timelineView.setOnCandidateClicked(...)` | **tot** |
| `:9069` | `Platform.runLater(this::updateTimelineVisuals)` | **lebend** — die Erstbefüllung |
| `:9849` | `return timelineView;` (Getter) | mit `timelineView` zu prüfen |

- [ ] **Step 1: Die Verzahnung am echten Code neu erheben**

Run:
```bash
grep -n "timelineView\|updateTimelineVisuals\|getActiveSkeds().addListener\|uiPulse().addListener\|getActualQTF().addListener" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: dieselbe Verzahnung wie in der Tabelle oben, mit aktuellen Zeilen. Jede Zeile
genau einem Topf zuordnen — tot (fällt) oder lebend (bleibt, oder der lebende Teil einer
gemischten Zeile bleibt).

- [ ] **Step 2: Die lebenden Auslöser vom toten Knoten trennen**

- `:6945`–`:6948` zu einem reinen `updateTimelineVisuals()`-Auslöser machen, den
  `timelineView.setCurrentAntennaAzimuth`-Aufruf entfernen. `updateTimelineVisuals` liest
  den Azimut ohnehin frisch aus den Preferences (`:4843`), der tote Knoten brauchte ihn
  nicht.
- `:6927` und `:6930` bleiben unverändert — sie rufen schon nur `updateTimelineVisuals()`.
- `:9069` bleibt — die Erstbefüllung.

- [ ] **Step 3: Den toten `timelineView` und seinen eigenen Zweig löschen**

- Die Erzeugung (`:6878`–`:6885`), die beiden Größen-Listener, `setCurrentAntennaAzimuth`
  /`setBeamWidthDeg`, `setOnCandidateClicked` und den Getter entfernen.
- In `updateTimelineVisuals` den toten Zweig `if (timelineView != null) { timelineView.updateVisuals(...) }`
  (`:4819`–`:4821`) entfernen; der Feed-Push darunter bleibt.
- Das Feld `private TimelineView timelineView;` (`:213`) fällt zuletzt.
- **`TimelineView.java` selbst bleibt** — es ist eine eigene Datei und gehört in Etappe 6
  (Restdateien), nicht hierher. Diese Aufgabe entfernt nur seine Nutzung in
  `Kst4ContestApplication`.

- [ ] **Step 4: Die tote CW-Ausgabe und die tote Szene mitnehmen, soweit erreichbar**

Der Befund nennt für den Rest von `start()` auch die tote CW-Ausgabe (`playCWLauncher`,
`musicList`, `mediaPlayer`, Import `javafx.scene.media`) und die nie gehängte
`scn_ChatwindowMainScene` samt Listenern und `themedScenes`. Am echten Code prüfen, was
davon nach den Scheiben 2–6 noch steht und ohne lebende Nebenwirkung fällt:

Run:
```bash
grep -n "playCWLauncher\|musicList\|mediaPlayer\|javafx.scene.media\|scn_ChatwindowMainScene\|themedScenes\|registerThemedScene" \
     app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java
```

Erwartet: `playCWLauncher` ohne Aufrufer (der Befund nennt ihn tot), `registerThemedScene`
/`themedScenes` nach dem Wegfall des Kandidatenfensters (Teil 2) und der toten Hauptszene
ohne lebenden Nutzer. Jede dieser Gruppen nur löschen, wenn der Grep bestätigt, dass kein
lebender Pfad sie erreicht; sonst in den Befund schreiben und stehen lassen. Der Import
`javafx.scene.media` fällt mit der toten CW-Ausgabe.

- [ ] **Step 5: Übersetzen, Suite, Anwendungsstart — mit Sked-Prüfung**

Run: wie Aufgabe 2, Schritt 5.

Erwartet: `exit=0`, `failures=0`, Testzahl unverändert. Beim Anwendungsstart die
**Compose-Zeitleiste** über dem Eingabefeld prüfen: ein eintreffender Sked und eine
Rotordrehung müssen sie weiter aktualisieren (Marker, Antennenrichtung). Bleibt sie leer
oder eingefroren, ist ein lebender Auslöser mitgelöscht worden — die Fehlerklasse, die
dieser Plan verhindern soll (Review Focus 5).

- [ ] **Step 6: Abhaken und committen**

```bash
git add app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java \
        docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Separate start() line by line: drop the dead timeline node, keep its triggers

In start() the dead TimelineView node and the live triggers that drive the
Compose timeline sat next to each other. The node, its size listeners, its
azimuth setters, its click handler and its getter are gone; the three live
triggers - the sked listener, the uiPulse listener and the QTF listener -
stay, with the QTF one cut down to the updateTimelineVisuals call it shares
with a now-removed timelineView setter. updateTimelineVisuals reads the
azimuth fresh from preferences, so the setter was never load-bearing.

The dead CW launcher and the never-attached main scene go where the grep
proves no live path reaches them, taking the javafx.scene.media import with
them. TimelineView.java itself stays for stage 6.

Verified by watching the Compose timeline still update on an incoming sked
and a rotor turn - the exact signal a deleted live trigger would have
silenced with a green build."
```

---

## Prüfung

Das Netz dieser Etappe:

- **800 Tests, 0 Fehler** als Ausgangsstand (Aufgabe 0), nach jeder Scheibe neu gezählt
  über die XML — nicht aus Exit-Code 0 geschlossen.
- Das **Durchlauf-Skript** vor jeder Löschung; es muss die zu löschende Methode als
  `clean` führen, und nach Aufgabe 6 `0 of N` melden.
- Ein **Anwendungsstart nach jeder Löschscheibe**, weil kein Test beantwortet, ob ein
  entfernter Listener gefehlt hat — bei den Zuführungen mit konkreter Bedienhandlung
  (Station wählen, Verbindung wechseln, Sked eintreffen lassen).
- Die bestehenden Feed-Tests (`TimelineFeedTest`, `SelectedStationMessagesFeedTest`,
  `ConnectionStateFeedTest`, `FeedIsTheOnlyWriterTest`,
  `FeedLifetimeIsTiedToTheWindowTest`) und der neue Zähler-Test aus Aufgabe 1.

## Was unverändert bleibt

- Die Fachlogik in `core` und sämtliche Protokollformate.
- Die **Schnittstelle** `UiDispatcher` und ihre Umsetzung `JavaFxUiDispatcher` — der
  Tausch ist Etappe 4.
- Die Invarianten aus `AGENTS.md`: `ObservableRoster` ist der Speicher, die Listen sind
  Spiegel; kein Worker-Faden fasst UI-Sammlungen an; Zoom, Auswahl, Fokus, Sortierung und
  vorbelegter Text ändern sich nicht als Nebeneffekt.
- Die 91 MP3-Dateien und die lebende CW-Ausgabe in `PlayAudioUtils`.
- `TimelineView.java`, `PathProfileChart.java`, `StationMapView.java` und die übrigen
  JavaFX-Dateien als Dateien — nur ihre Nutzung in `Kst4ContestApplication` fällt, soweit
  diese Scheiben sie berühren.
- Die vorgefundene System-Doppelzustellung der Nachrichtentabelle (`refresh()` +
  nachgezogener `push`) — sie wird in Aufgabe 1 festgehalten, nicht aufgelöst.

## Risiken

**Eine Löschung nimmt der Compose-Oberfläche still einen Anstoß.** Das ist die
Fehlerklasse, der dieser ganze Plan gilt. Jede Scheibe hat deshalb einen Anwendungsstart
als Abnahme, nicht nur einen grünen Build.

**Die Zeilennummern in diesem Plan altern mit jeder Scheibe.** Jede Aufgabe erhebt ihre
Stellen vor der Arbeit neu per Grep; keine Löschung stützt sich auf eine hier notierte
Zeile, nur auf den Namen.

**`start()` ist 2.000+ Zeilen durchsetzter Aufbau.** Aufgabe 7 ist zeilenweise und
einzeln zu prüfen; ein am Stück beschnittenes `start()` ist der sichere Weg, einen
lebenden Auslöser zu verlieren.

## Was dieser Plan nicht tut

Alles Folgende ist **nicht** Teil 3, sondern ein späterer Plan (Spec-Etappen 4–6), weil
es erst schreibbar ist, wenn `start()` zerlegt und der durchsetzte Aufbau gelöscht ist:

- **Etappe 4:** `JavaFxUiDispatcher` → eine Umsetzung auf dem AWT-Ereignisfaden. Die
  Schnittstelle `UiDispatcher` bleibt, nur ihre Umsetzung wird getauscht.
- **Etappe 5:** der Lebenszyklus (`extends Application`, `launch(args)`, `start(Stage)` →
  einfaches `main`), die zehn `Alert`-Dialoge als eigene Compose-Fenster, Fensterrahmen,
  Startgröße (`Screen`) und Anwendungssymbol, und der macOS-Systemmenüleisten-Pfad.
- **Etappe 6:** die Restdateien (`StationMapView`, `StationMapBridge`, `MapHtmlResources`,
  Leaflet-Ressourcen, `TileProxyServer`, `PathProfileChart`, `TimelineView`,
  `MessageTextTableCell`, `TruncatedTextTableCell`, `JavaFxUiDispatcher`), das Verschieben
  von `MessageVariableResolverTest` nach `src/test`, das Entfernen der `javafx`-Importe
  aus `TableLayoutManager.java` (die Klasse **bleibt**, sie wird von sechs Compose-Dateien
  benutzt), und das Befreien des Builds — der `org.openjfx`-Block aus
  `app-desktop/build.gradle.kts` und `gradle/libs.versions.toml`.

Erst wenn `grep -rl "javafx\." core/src/main app-desktop/src/main` nichts mehr liefert und
der `org.openjfx`-Block aus dem Build fällt, ist das Ziel der Spezifikation erreicht — das
ist das Ende von Etappe 6, nicht von Teil 3.
