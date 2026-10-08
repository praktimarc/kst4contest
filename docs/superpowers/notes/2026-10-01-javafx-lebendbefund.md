# Lebendbefund: was im JavaFX-Aufbau von KST4Contest tatsächlich benutzt wird

Grundlage für Teil 2 von `docs/superpowers/specs/2026-10-01-javafx-entfernen-design.md`.
Erhoben am 1. Oktober 2026 auf Linux, Branch `nextMajorRelease/version1_50`.

> **Teil 4 abgeschlossen am 2026-10-03.** Etappe 4 und 5 der Spezifikation sind in einem
> Zug umgesetzt. `UiDispatcher` liegt jetzt auf dem AWT-Ereignisfaden (`AwtUiDispatcher`, 5
> Vertragstests), `JavaFxUiDispatcher` ist gelöscht. Alle zehn `Alert`-Aufrufe sind
> Compose-Dialoge (`ComposeAlert`: `show`/`acknowledge`/`confirm`/`showWithLink`, modal über
> `ComposeDialog`, damit die vier blockierenden ihre `showAndWait`-Semantik behalten).
> `getHostServices().showDocument` → `ExternalDocuments` auf `java.awt.Desktop`, mit der
> Schema-Entscheidung Mail gegen Web, die JavaFX nicht brauchte. `javafx.stage.Screen` →
> `ScreenBounds` auf `GraphicsEnvironment`. Der Lebenszyklus ist frei: kein
> `extends Application`, kein `init`/`start(Stage)`/`stop`, kein `launch`; `main` liest die
> Kommandozeile selbst und ruft das neue `startRuntime()`, ein Abschalthaken ersetzt `stop()`.
> `ApplicationRuntimeLauncher`, `LayoutAutosave` und `GuiUtils` sind JavaFX-frei.
> **Bilanz:** Dateien mit `javafx.` unter `src/main` von 16 auf **11**, `javafx`-Importe in
> `Kst4ContestApplication` von 41 auf **19**, Datei von 5.117 auf 4.933 Zeilen. Build grün,
> **821 Tests, 0 Fehler** (Grundlinie 800 + 21 neue).
>
> **Zwei harte Lehren aus Teil 4:**
>
> 1. **Der grüne Build sah den Startabbruch nicht.** Der erste Start nach dem Wegfall von
>    `launch()` starb mit `IllegalStateException: Toolkit not initialized` in
>    `Kst4ContestApplication.<init>` — über `javafx.scene.control.Control.<clinit>`. Ursache:
>    die Klasse hält noch **19 abgehängte JavaFX-Steuerelemente** als Wertbehälter
>    (`txt_chatMessageUserInput` und Geschwister). Sie hängen in keiner Szene, aber das
>    Erzeugen des ersten läuft durch `Control`s statischen Initialisierer, der das Toolkit
>    braucht. Behelf: `startJavaFxToolkitForTheRemainingDetachedControls()` als erste Zeile in
>    `main` (`Platform.startup`, `setImplicitExit(false)`). **Diese Methode fällt in Etappe 6
>    mit den 19 Steuerelementen** — sie ist ausdrücklich so dokumentiert.
> 2. **Zwei `Platform.runLater`-Stellen durften *nicht* auf den Dispatcher.** Ihr Zweck ist
>    das Aufschieben über den laufenden Ereignisdurchlauf hinaus, und `runOnUi` führt auf dem
>    UI-Faden sofort aus — genau der Fall dort. Sie liegen jetzt auf `EventQueue.invokeLater`:
>    die Fokusübergabe nach dem Vorbelegen von `/cq` und das Zurücksetzen von
>    `markOperatorChatMemberSelectionIntent`. Der Kommentar an der ersten Stelle hatte das
>    vorausgesehen; wer ihn überliest, baut einen Fokusverlust und eine falsch zugeordnete
>    Bedienerabsicht ein.
>
> Zusätzlich: `LayoutAutosave` benutzte `javafx.animation.PauseTransition`, was ein laufendes
> JavaFX-Toolkit braucht — jetzt `javax.swing.Timer`, und die Klasse nimmt die Schreibaktion
> als `Runnable` statt der `ChatPreferences` (deren Konstruktor ins Benutzerprofil schreibt
> und darum nicht testbar war). Erstmals mit Tests: vier, darunter „ein Stoß von 20
> Ereignissen wird zu einem Schreibvorgang". **Offen (Etappe 6):** die 11 Restdateien, die 19
> Steuerelemente, `org.openjfx` aus Build und Versionskatalog.
> Behoben in Etappe 4/5; der Plan ist nach seiner Ausführung entfernt worden, die
> Begründung steht in der Commit-Nachricht und hier.

> **Etappe 6 abgeschlossen am 2026-10-04 — JavaFX ist raus.** Nachgewiesen, nicht
> geschlussfolgert: `grep -rl "javafx\." core/src/main app-desktop/src/main` ist leer, es
> gibt im ganzen Projekt keinen `import javafx` mehr, `:app-desktop:dependencies` zeigt
> JavaFX auf **keinem** der beiden Klassenpfade, der Build ist grün mit **849 Tests**, und
> die Anwendung läuft ohne JavaFX: Einstellungen gelesen, Datenbank verbunden, ON4KST
> verbunden, vollständige Benutzerliste, Nachrichten geparst, Frequenzerkennung — null
> Ausnahmen, kein `QuantumRenderer`-Faden. `Kst4ContestApplication`: 11.179 → **4.165**
> Zeilen.
>
> **Zwei Stellen waren lebend und mussten ersetzt werden, nicht gelöscht:**
> der 300-ms-Entpreller der Benutzerliste (`PauseTransition` → `CoalescingTrigger` auf
> `javax.swing.Timer`, fünf Tests) und `FxRosterBinding` → `RosterListBinding`
> (`ObservableList` → gewöhnliche Liste plus Rückruf; `SelectedStationMessagesFeed.push`
> nimmt ohnehin nur ein `List` und kopiert es). Beide waren vorher ungetestet.
>
> **Die Falle dieser Etappe**, und sie wäre ein lautlos falsch kategorisiertes Senden
> gewesen: `lastAutoPreparedCqTargetCallsign` und `-Category` sehen wie toter Schatten aus,
> werden aber von `resolveOutgoingChatCategory` gelesen, das Compose über
> `resolveOutgoingChatCategoryFromCompose` ruft. `ChatInputState` führt den vorbelegten
> Text selbst mit, aber **keine Kategorie** — dieser Zustand hat kein Compose-Gegenstück.
> Geschrieben wurde er nur von der Methode, die zu löschen war. Also an der Naht getrennt:
> `prepareCqTextForCallsign` → `rememberAutoPreparedCqTarget`, Kategoriebuchführung bleibt,
> Textfeld und Überschreibschutz fallen.
>
> **Zwei vorbestehende Defekte kamen dabei heraus und sind behoben:**
> (1) Niemand rief `refresh()` auf dem Compose-Spiegel der Kurztasten und Schnipsel — die
> Haken des Einstellungsfensters gingen auf den JavaFX-Knopfbau in `flwPane_textSnippets`,
> das seit dem Abklemmen `null` ist. Eine bearbeitete Kurztaste erreichte den Bediener erst
> nach einem Fensterneubau. (2) Das Schließen des Hauptfensters beendete die Anwendung
> nicht: `ComposeWindowHost` gab jedem Fenster Composes Vorgabe
> `onCloseRequest = ::exitApplication`, die nur das `application {}` dieses Fensters
> beendet — Fenster weg, Prozess lief mit offener Verbindung weiter. Von Marc gefunden,
> weil ein Tiling-Fenstermanager genau so schließt. Das Hauptfenster führt jetzt in
> `closeWindowEvent`. Mit demselben Griff kam heraus, dass die **Fenstergröße** gar nicht
> mehr gespeichert wurde — der Verzicht war mit „solange beide Fenster leben" begründet,
> und das zweite Fenster gibt es nicht mehr.
>
> **Namensfallen, an denen ein Löschen nach bloßem Namen scheitert:** `TimelineView` und
> `PathProfileChart` existierten doppelt, je einmal JavaFX und einmal Compose. Gelöscht
> wurde nur die Java-Fassung. `TimelineView.CandidateEvent` war ein reines Zwischen-DTO,
> das 1:1 in `TimelineCandidate` umkopiert wurde; der Erbauer baut jetzt direkt den
> Compose-Typ, vorher mit einem Test festgenagelt, weil drei der zehn Felder `Int` sind und
> eine Vertauschung lautlos durchgeht. Und `RosterMirror` war schon vergeben — die
> Compose-Fassung in `MainWindowState.kt`; die Java-Klasse heißt deshalb
> `RosterListBinding`.
>
> **`TableLayoutManager` bleibt**, auf `calculateInitialContentWidth` zusammengeschrumpft:
> sechs Compose-Dateien nennen die Klasse namentlich, wer dem Verweis folgt, soll die
> Regel finden und keine Lücke. **`MessageVariableResolverTest`** lag in `src/main/java`
> und wurde nie ausgeführt — verschoben, daher die Testzahl von 842 auf 848.

> **Teil 4, Prüfung und Korrekturen vom 2026-10-04.** Die Abschlussprüfung durch einen
> frischen Prüfer fand **zwei Critical-Fehler, beide dieselbe Fehlerklasse** — und beide
> waren im grünen Build unsichtbar. Behoben in `72d35f84`; Build grün, **835 Tests**.
>
> **Die Fehlerklasse, die jeder künftige Umbau hier treffen wird: ein blockierendes Warten
> auf dem Ereignisfaden.** Seit der Dispatcher dort liegt, laufen alle Menüaktionen auf dem
> AWT-Ereignisfaden. Skikos `SwingDispatcher` stellt **jede** Fortsetzung von
> `application { }` über `SwingUtilities.invokeLater` zu und überschreibt
> `isDispatchNeeded` nicht — im Bytecode nachgeprüft. Wer auf diesem Faden auf etwas
> wartet, das nur als Ereignis auf diesem Faden vorankommt, verklemmt ohne Zeitlimit.
>
> 1. **`OperatorProfilePickerWindow`** wartete auf einem `CountDownLatch`. Menü → Profil
>    wechseln fror die Anwendung dauerhaft ein. Jetzt ein modaler `ComposeDialog`: der
>    blockiert über eine verschachtelte Ereignisschleife und zeichnet dabei weiter.
> 2. **`ComposeWindowHost.close()`** wartete mit `Thread.join`. Jeder Fensterschalter im
>    Windows-Menü fror für das Zeitlimit ein und kehrte zurück, **während das Fenster noch
>    als offen galt** — womit ein Profilwechsel das Fenster der toten Laufzeit hervorhob und
>    das neue Profil ohne Einstellungsfenster, also ohne Connect-Knopf, dastand. Genau der
>    Defekt, den das Javadoc derselben Klasse als behoben beschreibt. Jetzt eine
>    `SecondaryLoop`, deren `exit()` über `invokeLater` gepostet wird, um das dokumentierte
>    exit-vor-enter-Wettrennen zu schließen.
>
> **Eine dritte Lehre, die der Fix-Durchgang selbst erzeugt hat:** ein `ComposeDialog` ist
> eine Swing-Komponente um eine Skia-Fläche und **muss auf dem Ereignisfaden erzeugt
> werden**. Vom Hauptfaden aus erzeugt, bekommt skiko keinen GL-Kontext
> (`Can't wrap nullptr` → `RenderException: Cannot init graphic context`), der Dialog
> zeichnet nie, und ein modales Warten darauf endet nie. Drei von drei Startversuchen.
> Gefunden nur durch Starten der Anwendung und einen `jcmd Thread.print`; kein Test sah es.
> Dafür gibt es jetzt `openOnEventThread` und zwei Tests. **Merksatz: der grüne Build sagt
> über Fadenzugehörigkeit nichts.**
>
> **Knopfreihenfolge:** die JavaFX-`ButtonBar`-Reihenfolgen wurden gemessen, nicht erinnert —
> `LINUX = L_HE+UNYACBXIO_R`, `MAC_OS = L_HE+U+FBIX_NCYOA_R`,
> `WINDOWS = L_E+U+FBXI_YNOCAH_R`. Der Beenden-Dialog nutzte `ButtonType.YES`, der
> Profilwechsel `OK_DONE`, und die beiden ordnen auf Linux **gegenläufig**. `ComposeAlert`
> trägt deshalb die Knopfart und ordnet nach diesen Zeichenketten. Der Prüfer hatte hier
> aus dem Gedächtnis eine macOS-Reihenfolge für Linux behauptet und das nach dem Nachmessen
> zurückgezogen — beide Seiten irrten, die Messung entschied.
>
> **Zwei offene Entscheidungen für Marc**, bewusst nicht im Fix-Durchgang erledigt: die
> Dialoge haben die Warn-/Fehler-/Info-Symbole der JavaFX-`AlertType`s verloren (Symbolwahl
> ist Gestaltung, keine Fehlerbehebung), und die vier blockierenden Dialoge sind jetzt
> `APPLICATION_MODAL`, sperren also die Eingabe in den Compose-Fenstern, solange sie offen
> sind — was der toolkit-fremde JavaFX-`Alert` nicht tat. Ebenfalls unverifiziert:
> `ScreenBounds` auf einem skalierten Bildschirm (hier ein unskalierter, beide Quellen
> lieferten denselben Wert).

> **Teil 4, Sondenergebnis vom 2026-10-03.** Ein modaler
> `androidx.compose.ui.awt.ComposeDialog` (Compose 1.8.2) hält seinen Aufrufer an **und**
> zeichnet sich dabei — gemessen von beiden Fäden: vom AWT-Ereignisfaden 1514 ms blockiert,
> komponiert; von einem Fremdfaden 1503 ms blockiert, komponiert. Damit ist die
> `showAndWait`-Semantik der vier blockierenden Dialoge erhaltbar, ohne den Ereignisfaden zu
> verklemmen. Das ist die Voraussetzung für den Dispatcher-Tausch: nach ihm laufen alle
> Menüaktionen über `ComposeMenuActions` auf dem Ereignisfaden, und das Muster von
> `OperatorProfilePickerWindow` (eigener Faden plus `CountDownLatch`) würde dort verklemmen,
> weil es genau den Faden blockiert, auf dem das neue Fenster zeichnen will. Der
> Einzelkonstruktor `ComposeDialog(Dialog.ModalityType)` existiert und wird benutzt.
> Behoben in Etappe 4/5; der Plan ist nach seiner Ausführung entfernt worden, die
> Begründung steht in der Commit-Nachricht und hier.

> **Teil 3 abgeschlossen am 2026-10-02.** Der gesamte durchsetzte JavaFX-Aufbau in
> `Kst4ContestApplication` ist gelöscht; die Klasse ist von 11.179 auf ~5.100 Zeilen
> geschrumpft, der Durchlauf meldet `0 of 0 init methods`. Erledigt in Scheiben: die drei
> Statusindikatoren an ihrer Naht getrennt (S1), `initMenuBar` samt macOS-Menü gelöscht
> (S2), der tote `scn_ChatwindowMainScene`-`try`-Block aus `start()` entfernt und die
> lebenden Trigger (Sked-Reminder, Zeitleisten-Listener) herausgerettet (S3), die sieben
> Tabellen-Builder, der Info-Pane-Cluster und die Snippet-Kontextmenüs gelöscht/entflochten
> (S3b–S4), und zuletzt der tote `timelineView`, die Szenen-Theming-Maschinerie
> (`registerThemedScene`/`themedScenes`) und die tote CW-/Voice-Ausgabe samt
> `javafx.scene.media`-Importen (S6). Zwei Laufzeitfunde aus GUI-Abnahmen wurden behoben:
> ein NPE auf dem gelöschten `flwpne_StatusBar` in `updateStatusButton`, und die
> Compose-Menü-Regression — die 16 Menüaktionen hingen über `ComposeMenuActions` an den
> gelöschten JavaFX-`MenuItem`s und wurden auf eigene `menuAction…`-Methoden umgestellt
> (vorgezogen aus Etappe 5). Build nach jeder Scheibe grün, 800 Tests. **Offen (Etappe
> 4–6):** `UiDispatcher`-Tausch, Lebenszyklus (`extends Application`, die zehn Alerts),
> die Restdateien und das Entfernen von `org.openjfx` aus dem Build; in
> `Kst4ContestApplication` stehen noch ~41 `javafx`-Importe.

> **Zweite Fassung.** Die erste kam aus einer Prüfung, die nur fragte „wer ruft diese
> Methode auf?". Eine Abschlussprüfung hat daran zwei kritische Fehler gefunden: zwei
> Blöcke, die als folgenlos löschbar geführt waren, speisen in Wahrheit die
> Compose-Oberfläche — die Nachrichten der ausgewählten Station und die Zeitleiste.
> Beide hätte Teil 2 gelöscht, ohne dass ein Test oder der Übersetzer etwas gemerkt
> hätte. Die Methode ist unten berichtigt; sie ist der eigentliche Inhalt dieses
> Dokuments.

## Wie geprüft werden muss

Drei Fragen pro Kandidat, nicht eine:

1. **Wer ruft ihn?** Auch aus dem Compose-Pfad heraus — die Compose-Fenster rufen in
   `Kst4ContestApplication` zurück, etwa über `focusChatMemberAndPrepareCq` bei
   `:10720`, `:10884` und `:10916`.
2. **Was tut er innen?** Eine 200-Zeilen-Methode, die eine JavaFX-Tabelle baut, kann
   mittendrin einen Listener anmelden, der in eine **Compose**-Tabelle schreibt. Genau
   das war Fehler eins.
3. **Meldet er Controller-Listener an?** Die überleben die Oberfläche, die sie
   angemeldet hat, und sind oft das Einzige, was ein Compose-Fenster speist.

Frage 2 braucht einen **transitiven** Durchlauf, keinen einstufigen Grep. Das ist
bezahltes Lehrgeld: ein einstufiger Grep über die Methodenrümpfe fand zwei Fälle, die
Abschlussprüfung einen dritten, und erst der transitive fand den vierten. Ein Rumpf,
der sauber aussieht, kann eine Hilfsmethode rufen, die schreibt.

Der Durchlauf: jede `init…`-Methode bis Tiefe 3 über ihre Aufrufziele verfolgen und auf
diese Bezeichner prüfen —

```
composeMainWindowState  MainWindowHost  SettingsWindow  MonitorWindow  UpdateWindow
StationMapWindow  OperatorProfilePickerWindow  replaceRows  replaceSkeds
replaceCandidates  DataTableState  composeMemberListener
```

**Nicht** auf das Wort „compose" prüfen. Menüeinträge öffnen Compose-Fenster, indem sie
`SettingsWindow` und Geschwister rufen; das Wort kommt dort nicht vor.

Der Durchlauf liegt als Skript daneben, damit der nächste Leser messen statt glauben
kann:

```bash
python3 docs/superpowers/notes/javafx-compose-sweep.py
```

Am 1. Oktober 2026 meldete es **drei** der 18 `init…`-Methoden als Compose-erreichend.
Die vierte Zuführung — die Zeitleiste — sitzt in `start()`, ist keine `init…`-Methode
und wird vom Skript nicht erfasst. Wer die Liste `COMPOSE_TOKENS` im Skript nicht
pflegt, sobald ein neues Compose-Fenster dazukommt, bekommt eine Methode fälschlich als
sauber gemeldet.


Ergebnis: **vier** der 18 `init…`-Methoden erreichen Compose-Zustand —
`initFurtherInfoAbtCallsignMSGTable`, `initMenuBar`,
`initConnectionStateIndicatorButton` und (über `start()`, keine `init…`-Methode) die
Zeitleiste. Die übrigen 15 sind bis Tiefe 3 sauber.

---

## initFurtherInfoAbtCallsignMSGTable — **zu entflechten**

**Belege:**

- Erreicht aus dem **Compose**-Pfad: `:10720` / `:10884` / `:10916` →
  `focusChatMemberAndPrepareCq(member, false)` (`:2728`) →
  `handleChatMemberSelectionChanged` (`:2770`) →
  `generateFurtherInfoAbtSelectedCallsignBP` (`:2799`) → `:893`.
- Innen bei `:3400`–`:3409`: `selectedCallSignInfoMessageBinding` wird erzeugt, und sein
  Listener schiebt in `composeMainWindowState.getSelectedStationMessages().replaceRows(...)`
  — eine **Compose**-Tabelle, erzeugt bei `:10492`, übergeben bei `:10588`.
- Alle Schreiber von `selectedStationMessages` (`:319`, `:3405`, `:10659`, `:10866`)
  laufen über diese Bindung.

**Folgerung:** Wird die Methode gelöscht, bleibt `selectedCallSignInfoMessageBinding`
`null`, alle vier Zuführungen sind null-geprüft und tun still nichts, und die Tabelle
„Nachrichten der ausgewählten Station" im Compose-Hauptfenster bleibt für immer leer.
Der Build bleibt grün. Die Bindung muss **vorher** aus der Tabellenkonstruktion
herausgelöst werden.

**Scheibe C erledigt am 2026-10-02.** Die Erzeugung sitzt jetzt in
`rebuildSelectedCallSignInfoMessageBinding()` ohne `TableView`-Bezug;
`initFurtherInfoAbtCallsignMSGTable` ruft sie nur noch vor dem `setItems`. Der Durchlauf meldet die
Methode weiterhin als Compose-erreichend, aber über die neue Hilfsmethode
(`-> rebuildSelectedCallSignInfoMessageBinding @:selectedStationMessagesFeed`), nicht
mehr über den Tabellen-Builder selbst. Damit kann die Tabelle in der nächsten Scheibe
fallen, ohne die Compose-Tabelle leerzuziehen.

## `start()`, Zeilen 7356–9573 — **zu entflechten, kein toter Zweig**

**Belege:**

- 2.217 Zeilen, in denen toter Oberflächenaufbau und **lebende** Verdrahtung
  durcheinanderliegen.
- `:7379` `timelineView = new TimelineView()` und `:7428`
  `chatcontroller.getActiveSkeds().addListener(skeds -> updateTimelineVisuals())`.
- `updateTimelineVisuals` (`:4885`) steigt bei `:4886` sofort aus, wenn `timelineView`
  `null` ist — und schiebt bei `:4914`–`:4919` in `composeMainWindowState.getTimeline()`:
  Skeds, Kandidaten, Antennenazimut und Keulenbreite.
- Weitere lebende Anmeldungen im selben Rumpf: `:7431` `uiPulse()`, `:7446`
  `getActualQTF()`.
- `:9565` ruft `openSettingsWindow()`.

### Nachtrag nach Teil 2: das Ziel ist abgelöst, der Auslöser nicht

Teil 2 hat `TimelineFeed` eingeführt und die Wache auf `timelineView` aus
`updateTimelineVisuals` entfernt. Das löst das **Ziel** der Zuführung vom
JavaFX-Aufbau. Die **Auslöser** liegen weiter in `start()`:

| Stelle | Was | Status |
|---|---|---|
| `:6896`, `:6905` | `timelineView`-Breite/Höhe | **tot** — der Knoten wird nie gezeigt |
| `:6931` | `chatcontroller.getActiveSkeds().addListener(...)` | **lebend** |
| `:6935` | `chatcontroller.getScoreService().uiPulse().addListener(...)` | **lebend** |
| `:6951` | `getActualQTF().addListener(...)` — fasst auch `timelineView` an | **lebend, gemischt** |
| `:9073` | `Platform.runLater(this::updateTimelineVisuals)` | **lebend** — die Erstbefüllung |

**Warnung für Teil 3:** `updateTimelineVisuals` hat ausschließlich Aufrufer innerhalb
von `start()` (6859–9075). Wer diesen Rumpf löscht, ohne die drei
Controller-Listener und die Erstbefüllung vorher umzuhängen, nimmt der
Compose-Zeitleiste jeden Anstoß — bei grünem Build und ohne Übersetzungsfehler, also
genau die Fehlerklasse, die dieses Dokument verhindern soll. Bei `:6951` muss
zusätzlich der `timelineView`-Zugriff herausfallen.

### Und es ist mehr als die Zeitleiste

Eine erste Fassung dieser Warnung nannte nur die vier Zeitleisten-Zeilen. Eine
Abschlussprüfung hat gezeigt, dass das zu eng ist. **In `start()` liegt nicht nur der
Anstoß, sondern auch die Zuführung selbst:**

| Was | Wo | Folge beim Löschen von `start()` |
|---|---|---|
| Alle drei Zuführungen werden erzeugt | `:10137`–`:10141`, in `openComposeMainWindowIfRequested` (`:9919`), gerufen aus `start()` bei `:9049` | `timelineFeed`, `selectedStationMessagesFeed` und `connectionStateFeed` bleiben `null`; jeder `push` ist ein bewachtes Nichts. Zeitleiste, Nachrichtentabelle **und** Verbindungsanzeige werden leer |
| Auslöser der Nachrichten-Zuführung | der Listener in `initFurtherInfoAbtCallsignMSGTable` (`:3411`) samt der Bindung, die er beobachtet | die Nachrichtentabelle bekommt keinen Anstoß mehr |
| Auslöser der Zeitleiste | `:6931`, `:6935`, `:6951`, `:9073` | siehe oben |

**Damit ist eine Behauptung aus dem Plan zu Teil 2 falsch.** Dort stand: „Nach diesem
Plan ist der tote Aufbau zum ersten Mal wirklich tot." Das gilt nicht. Teil 2 hat die
**Ziele** der Zuführungen abgelöst — sie haben Namen, Tests und einen einzigen
Schreibweg. Ihre **Erzeugung** und ihre **Auslöser** liegen weiter im Aufrufbaum von
`start()`. Teil 3 muss beides umhängen, bevor dort irgendetwas fällt; `start()` ist
nicht zu beschneiden, sondern Zeile für Zeile zu trennen — was dieses Dokument weiter
oben schon über `start()` sagt und was für die Zuführungen genauso gilt.

Der Grund, warum das erst nachträglich auffiel: `openComposeMainWindowIfRequested` liest
sich wie Compose-Verdrahtung und ist es auch — sie hängt nur am falschen Aufrufer.

**Folgerung:** Die Zeitleiste über dem Eingabefeld des **Compose**-Fensters hängt an
einem Objekt, das wie toter JavaFX-Aufbau aussieht. Ein Löschen des „toten Zweigs"
nimmt ihr Skeds, Kandidaten und Antennenrichtung — ohne Übersetzungsfehler und ohne
Testfehler. `start()` ist Zeile für Zeile zu trennen, nicht am Stück zu löschen.

### Die empfohlene Form für Teil 3: die Erzeugung umhängen, nicht die Warnung lesen

Diese Warnung ist ein Dokument, und ein Dokument schützt nur den, der es liest. Die
Abschlussprüfung hat die bessere Form benannt, und sie gehört hierher, damit Teil 3
nicht erst darauf kommen muss:

> Die drei Zuführungen dort erzeugen, wo `composeMainWindowState` **deklariert** wird,
> nicht dort, wo `start()` sie zufällig erreicht.

Dann kann das Umhängen von `start()` die Zuführungen nicht mehr mit `null`
zurücklassen, weil ihr Leben nicht mehr an `start()` hängt. Die Garantie wird
strukturell statt dokumentiert.

Das ist **Scheibe B von Teil 3**, nicht Teil 2 — dort wäre es ein Umbau ohne Auftrag.
Wer Scheibe B beginnt, fängt damit an.

`FeedIsTheOnlyWriterTest` deckt diesen Fall ausdrücklich **nicht** ab: der Test prüft,
wer schreibt, nicht wann die Zuführung entsteht. Ein grüner Lauf ist hier kein
Freibrief.

Inzwischen gibt es dafür `FeedLifetimeIsTiedToTheWindowTest`: er prüft, dass alle drei
Zuführungen in derselben Methode entstehen wie `composeMainWindowState`. Damit ist die
Kopplung strukturell und nicht mehr nur hier beschrieben. Die empfohlene Form oben
bleibt trotzdem die bessere Endgestalt — der Test hält den Zustand fest, er stellt ihn
nicht her.

### Ein Aktualisieren, zwei Zustellungen — vorgefunden, nicht neu

`applySelectedCallSignInfoFilter` (`:317`) und die Stationsauswahl (`:10380`) rufen beide
`selectedCallSignInfoMessageBinding.refresh()` **und** schieben anschließend in einem
`Platform.runLater` selbst nach. `refresh()` geht über `FxRosterBinding:75` nach
`applyNow`, ändert die Spiegelliste, und der Listener bei `:3410` schiebt dadurch schon.
Jeder Filterwechsel und jede Stationsauswahl ersetzt die Tabelle also **zweimal** mit
demselben Inhalt.

Das ist geordnet und harmlos, aber es ist festzuhalten, weil es leicht falsch gelesen
wird: „ein `push` ist eine Zustellung" gilt für die Zuführungsklasse und **nicht** für
das System. Entstanden ist es nicht mit den Zuführungen — der Listener stand schon vor
`21c8e62a` dort, die Zuführung hat die Doppelung geerbt. Aufzulösen ist sie, wenn in
Teil 3 die Bindung aus dem JavaFX-Aufbau gelöst wird: dann fällt einer der beiden Wege
ohnehin weg. Vorher einen davon zu entfernen hieße raten, welcher bleibt.

### „more" holt ein offenes Kandidatenfenster nicht nach vorn

`ComposeWindowHost.show` kehrt bei `!open.compareAndSet(false, true)` zurück (`:106`).
Das JavaFX-Original erzeugte je Druck eine frische `Stage` und hob sie damit an. Wer
also zum Hauptfenster zurückklickt und erneut auf „more" drückt, sieht nichts
geschehen.

**Es betraf nur das Kandidatenfenster — eine erste Fassung dieses Abschnitts behauptete
das Gegenteil.** `ComposeWindowHost` trägt zwar sechs Fenster, aber fünf davon sichern
die Stelle beim Aufruf ab und erreichen die frühe Rückkehr nie:

| Fenster | Aufruf | erreicht `show()` bei offenem Fenster? |
|---|---|---|
| Einstellungen | `:5393`, `isOpen` → `close()` | nein |
| Monitor | `:5376`, dieselbe Form | nein |
| Karte | `:400`/`:406`, `toggle` bzw. `isShowing()` | nein |
| Update | `:10610`, einmal beim Start | nein |
| Hauptfenster | `:10185`, einmal | nein |
| **Kandidaten** | `:4772`, ungeprüft, nur öffnen, kein Umschaltweg | **ja** |

Damit ist es **keine geerbte Eigenschaft des Hosts, sondern eine mit dieser Portierung
eingeführte Regression**. Dazu passt, dass `TopPriorityCandidatesWindow.isOpen` mit
`@JvmStatic` angelegt wurde und **keinen Aufrufer** hat: die Prüfung wurde gebaut und
nie angeschlossen.

Die erste Fassung schrieb „betrifft alle sechs" und überzeichnete damit die Kosten der
Reparatur — und zwar gegenüber dem Betreiber, der genau danach entschieden hat. Die
Entscheidung wurde mit der berichtigten Lage noch einmal gestellt.

**Behoben am 2026-10-01, nach erneuter Entscheidung des Betreibers, im Host.** `show`
holt ein offenes Fenster jetzt nach vorn, statt stillschweigend zurückzukehren. Die
Regel steckt in `raiseSteps`, getrennt von den AWT-Aufrufen, damit sie prüfbar ist: ein
minimiertes Fenster wird erst wiederhergestellt und dann angehoben — `toFront` auf einem
eingeklappten Fenster lässt es eingeklappt, also wäre das derselbe tote Knopf in anderer
Verkleidung — und ein nicht mehr anzeigbares Fenster wird in Ruhe gelassen, damit ein
Druck im Schließfenster nicht gegen das Schließen arbeitet.

Im Host und nicht an der Aufrufstelle, obwohl heute nur ein Fenster davon profitiert:
die vier Geschwister sind wirkungslos abgesichert, und ein künftiges Fenster, das `show`
ungeprüft ruft, macht sonst denselben Fehler noch einmal. Ein Umschalter an der
Aufrufstelle wäre die Alternative gewesen, hätte aber ein anderes Verhalten ergeben —
das JavaFX-Original schloss beim zweiten Druck nicht, es erzeugte eine frische `Stage`
und holte das Fenster damit nach vorn.

Was **nicht** geprüft ist: `toFront`. Nur das — und nicht, weil das Gerüst zu schwach
wäre, sondern weil das Ergebnis dem Fenstermanager gehört und aus der JVM gar nicht
beobachtbar ist. Es gibt kein „liege ich oben" zum Prüfen.

Eine erste Fassung zählte hier drei ungeprüfte AWT-Aufrufe auf und hatte in genau der
Zeile einen Fehler, die sie sich zu prüfen erspart hatte: das Wiederherstellen schrieb
`Frame.NORMAL`, also eine Null, statt das Bit zu löschen. `extendedState` ist ein
Bitfeld; ein Fenster, das maximiert und dann minimiert wurde, trägt 7, und die Null
hätte es wiederhergestellt-klein zurückgegeben — eine Maximierung weggeworfen, um die
niemand gebeten hatte. Herausgelöst als `deiconifiedState(current)`, reine Arithmetik,
drei Prüfungen; die alte Fehlerform wird rot.

Ein `requestFocus` stand ebenfalls in der ersten Fassung, ohne Beleg, dass es je etwas
bewirkt. `toFront` ist der dokumentierte Weg; `Component.requestFocus` auf einem Fenster
ignorieren die meisten Fenstermanager, und unter X11 entscheidet ohnehin die
Fokusdiebstahl-Sperre. Entfernt — zwei Schritte sind die ehrliche Regel.

## initConnectionStateIndicatorButton — **zu entflechten**

Vom transitiven Durchlauf gefunden; ein einstufiger Grep über den Rumpf übersieht ihn.

**Belege:**

- Der Rumpf (`:6180`–`:6198`) ist innen sauber, ruft aber bei `:6195`
  `updateConnectionStateIndicator(...)`.
- Diese Methode (`:6200`) schreibt bei `:6217`–`:6219`
  `composeMainWindowState.getSurroundings().setConnectionState(...)` und
  `setConnectionDetail(...)` — den Verbindungszustand in der **Compose**-Statusleiste.
- Sie hat weitere, lebende Aufrufer außerhalb des toten Aufbaus: `:9844` und `:9871`.
- Danach fasst sie bei `:6224` ein JavaFX-Tooltip an (`tipConnectionStateIndicator`).

**Folgerung:** Eine gemischte Methode wie `updateTimelineVisuals`. Wird
`initConnectionStateIndicatorButton` gelöscht, verschwindet das Tooltip-Objekt, das
`:6224` anfasst, während `:9844`/`:9871` weiter rufen. Der Compose-Schreibzugriff ist
vorher aus dem JavaFX-Teil herauszulösen.

**Scheibe S1 erledigt am 2026-10-02.** `updateConnectionStateIndicator` ist an seiner
markierten Linie getrennt (Feed-Push und Log bleiben, der JavaFX-Indikator fällt);
`initConnectionStateIndicatorButton`, `initSkedWarnIndicatorButton` und
`initBandUpgradeIndicatorButton` sind gelöscht. Mitgefunden:
`showBlinkingSkedWarnIndicator`/`showBlinkingBandUpgradeIndicator` waren dieselbe Naht — JavaFX-Knopf plus
Compose-Notice-Push. Beide behalten nur den Compose-Push; die Blink-Timelines, die
`hide…`-Gegenstücke und die Knopf-/Tooltip-Felder sind weg. Die Compose-Notice regelt
ihr Ausblenden selbst. `menuConnectionStateMacOs`/`menuItemConnectionStateDetailMacOs`
bleiben noch (fallen mit `initMenuBar`, S2).

## initMenuBar — **zu entflechten**

**Belege:**

- Zehn Compose-Berührungen innen (`:5863` `SettingsWindow`, `:5884`–`:5888`
  `MonitorWindow`, und weitere): die Menüeinträge **öffnen** die Compose-Fenster.
- Installiert nur auf der Szene des Kandidatenfensters (`:4858`).

**Folgerung:** Nicht löschen, sondern die Aktionen übernehmen. Der Abgleich ist unten
gemacht — und er fällt kleiner aus als erwartet.

**Scheibe S2 erledigt am 2026-10-02.** `initMenuBar` und `installSharedSystemMenuBar`
(aufruferlos) sind gelöscht, mit ihnen das tote `Macros`-Menü, die zehn `Alert`-tragenden
Menü-Handler und die zwei macOS-Menüfelder `menuConnectionStateMacOs`/
`menuItemConnectionStateDetailMacOs`. Der `start()`-Aufruf und die Menüleisten-Einhängung
fielen mit; `flwpne_StatusBar` bleibt (wird von `updateStatusButton` mitbenutzt, fällt in
S6). **Noch stehen geblieben:** die `menuItem…`-Felder und ihr `setDisable`-Update im
ON4KST-Connection-Callback — die Felder sind seit dem Wegfall von `initMenuBar` `null`,
die Aufrufe null-geprüft und wirkungslos; sie gehören zum toten Aufbau und werden in S6
mit der Callback-Naht aufgeräumt. `macOsConnectionStateMenuTitle` und sein Test bleiben
bewusst (reine String-Funktion, macOS-Punkt der Spec, später zu klären). Der Durchlauf
meldet jetzt 1 of 10 — nur noch `initFurtherInfoAbtCallsignMSGTable` über die
rebuild-Hilfsmethode, die in S4 fällt.

### Abgleich mit `MainMenuBar.kt` (Stand 2026-10-01)

Die Leiste wird bei `:5546` zusammengesetzt:
`menubar.getMenus().addAll(fileMenu, optionsMenu, windowMenu, helpMenu);` — mit dem
Kommentar `// macromenu deleted`.

| JavaFX-Eintrag | Aktion | In Compose? |
|---|---|---|
| File → Connect (Beschriftung dynamisch) | `connect` | ja (`state.connectLabel`) |
| File → Disconnect | `disconnect` | ja |
| File → Switch operator profile... | `showOperatorProfileSwitchDialog` | ja |
| File → Exit + disconnect | beenden | ja |
| Options → Set QRG as name in Chat (main category) | QRG als Name setzen | ja |
| Options → Show me as away in chat | Away/Back | ja (`state.awayMenuLabel`) |
| Options → Show options / hide options | Einstellungsfenster | ja (dynamische Beschriftung) |
| Windows → Hide/Show cluster / stranger QSOs | Monitorfenster | ja (dynamische Beschriftung) |
| Windows → hide options | Einstellungsfenster | **fehlt** — in Compose mit „Show options" zusammengelegt |
| Windows → Use dark mode design | Design | ja |
| Windows → Use default mode design | Design | ja |
| Windows → Show / hide station map | Kartenfenster | ja |
| Info → Donate for kst4Contest development via PayPal | PayPal | ja |
| Info → `_______________________` (`help3`) | keine | **fehlt** — reiner Trenner |
| Info → Visit DARC X08-Homepage | Browser | ja |
| Info → Donate for OV3T´s plane feed service | Browser | ja |
| Info → Donate for ON4KST Chatservers… | **keine** — `setOnAction` ist auskommentiert | **fehlt**, und tot |
| Info → Contact the author using default mail app | Mailprogramm | ja |
| Info → Join kst4Contest newsgroup | Browser | ja |
| Info → About… | Infofenster | ja |
| **macOS:** LINK-Menü mit Zustandsdetail (`:5553`) | nur Anzeige, `setDisable(true)` | **fehlt** |

**Zu portieren:** nichts, was eine Aktion trägt. Die drei Lücken sind ein Trenner, ein
in Compose zusammengelegter Doppeleintrag und ein Eintrag, dessen Aktion seit
Längerem auskommentiert ist.

**Nur macOS:** Das `LINK`-Menü samt `menuItemConnectionStateDetailMacOs` wird nur bei
`PlatformUtils.isMacOs()` eingehängt, zusammen mit `setUseSystemMenuBar(true)`. Es
zeigt den Verbindungszustand schreibgeschützt in der Systemmenüleiste. Compose hat
dafür keine Entsprechung; ob das auf einem Mac auffällt, ist auf Linux nicht
entscheidbar.

**Tot, nicht fehlend:** Das Menü `Macros` (`:5361`–`:5370`) mit sechs Einträgen wird
gebaut, bekommt keine einzige Aktion und wird der Leiste **nie** hinzugefügt. Die
ursprünglichen Makro-Handler stehen bei `:3193` ff. auskommentiert. Mit `initMenuBar`
zu löschen, nicht zu portieren.

## FxRosterBinding — Klasse **bleibt**, ein Feld ist löschbar

**Belege:**

- `chatMemberListBinding` (`:299`): `list()` hat **genau einen** Leser, `:2517`
  `tbl_chatMemberTable.setItems(...)` — die nie gezeigte Tabelle. Mit ihr löschbar.
- `selectedCallSignInfoMessageBinding` (`:307`) ist dieselbe Klasse und **lebend**
  (siehe oben).

**Folgerung:** Das Feld `chatMemberListBinding` entfällt, die Klasse `FxRosterBinding`
nicht. Sie benutzt `ObservableList`/`FXCollections`; solange sie lebt, lebt dieser
JavaFX-Bezug. Ihr Ersatz ist eigene Arbeit in Teil 2.

## registerThemedScene / `themedScenes` — **folgenlos löschbar**, mit dem Kandidatenfenster

**Belege:**

- Zwei Aufrufer: `:7554` (tote Hauptszene), `:4856` (Kandidatenfenster, lebend).
- `applyTheme` (`:6111`) durchläuft `themedScenes` **und** benachrichtigt jedes
  Compose-Fenster getrennt über `applyDarkMode` (`:6122`–`:6126`).

**Folgerung:** Die Compose-Themenumschaltung hängt nicht daran.

## LayoutAutosave — **bleibt**

**Belege:**

- `requestLayoutSave()` hat **vierzehn** Aufrufer, nicht drei: `:1564`, `:7563`,
  `:7571`, `:8280`, `:9461`, `:9495`, `:10442`, `:10605`, `:10615`, `:10625`, `:11017`,
  `:11062`, `:11096`, `:11125`. Sieben davon liegen oberhalb `:10400`, also im
  Compose-Bereich; `:10442` ist die `requestSave()`-Umsetzung des Breiten-Rückrufs, den
  die **Compose**-Tabellen bekommen (`:10432`–`:10444`).

**Warnung für Teil 2:** Eine frühere Fassung dieses Abschnitts zählte drei Aufrufer.
Wer danach aufräumt und `requestLayoutSave()` entfernt, weil „seine drei Aufrufer weg
sind", bricht sieben lebende Compose-Speicherungen.

**Folgerung:** Speichert die Spaltenbreiten der Compose-Tabellen. Bleibt. Die drei
JavaFX-Bezüge in `LayoutAutosave.java` sind in Teil 2 gesondert zu prüfen.

## Kandidatenfenster — **zu entflechten, und Sperrriegel der Etappe**

**Belege:**

- `showTopPriorityCandidatesWindow` (`:4806`) baut die einzige `Stage`, die dieser
  Quelltext selbst erzeugt (`new Stage()` bei `:4810`, das einzige Vorkommen in der
  Datei), gibt ihr eine `Scene` (`:4855`) und zeigt sie (`:4860`).
- **Nicht** die einzige JavaFX-Oberfläche, die erscheint: die zehn `Alert`-Dialoge
  (`:476`, `:5756`, `:5777`, `:6040`, `:6693`, `:6741`, `:6794`, `:9755`, `:9787`,
  `:9896`) bringen ihre Bühne selbst mit und sperren die Etappe genauso.
- Zwei Aufrufer: `:4706` (toter JavaFX-Knopf) und `:10707` aus dem **Compose**-Pfad.
- Trägt `registerThemedScene` (`:4856`) und `installSharedSystemMenuBar` (`:4858`).

**Folgerung:** Solange es steht, kann keine JavaFX-Abhängigkeit fallen — aber es ist
nicht das Einzige, was sie hält; die zehn Alerts müssen mit. Es steht trotzdem zuerst,
weil es als einziges eine eigene Bühne, ein Thema und eine Menüleiste mitbringt.

**Nicht geprüft:** macOS — und auf Linux auch **nicht prüfbar**:
`installSharedSystemMenuBar` steigt bei `:6075` sofort aus, wenn
`PlatformUtils.isMacOs()` falsch ist. Was dort sichtbar an der JavaFX-`MenuBar` hängt,
kann kein Lesen auf Linux beantworten; vor dem Entfernen auf einem Mac nachzuholen.

---

## Ergebnis in einem Satz

Von den 18 `init…`-Methoden sind 15 bis Tiefe 3 sauber und nach Prüfung ihrer Aufrufer
löschbar — vier davon haben gar keinen Aufrufer; **drei** erreichen die
Compose-Oberfläche und sind zu entflechten; dazu kommen `start()` selbst, die Klasse
`FxRosterBinding`, `LayoutAutosave`, das Kandidatenfenster und die zehn
`Alert`-Dialoge. „Toter Aufbau" ist deshalb die falsche Beschreibung: es ist
durchsetzter Aufbau.

## Die `init…`-Methoden

„Kein Aufrufer" heißt hier wirklich keiner: `getDeclaredMethod`, `getMethod(`,
`Class.forName` und `FXMLLoader` kommen in der Datei **null**mal vor — es gibt keine
reflektive oder FXML-Hintertür, über die eine der vier dennoch erreichbar wäre.

| Methode | Zeile | ca. Zeilen | Aufrufer | erreicht Compose? |
|---|---|---|---|---|
| `initShortcutTable` | 4561 | 38 | **keiner** | nein |
| `initNotifyAtCallSignTable` | 5146 | 100 | **keiner** | nein |
| `initTextSnippetsTable` | 5248 | 46 | **keiner** | nein |
| `initWkdStnTable` | 5441 | 286 | **keiner** | nein |
| `initChatMemberTableContextMenu` | 3173 | 43 | 5314, 5315, 8017, 8433 | nein |
| `initBottomGlobalMessageTabPane` | 3577 | 23 | 8257 | nein |
| `initDXClusterTable` | 4156 | 181 | 3590 | nein |
| `initChatToOtherMSGTable` | 4339 | 220 | 3594 | nein |
| `initSelectedCallSignCompactControlsPane` | 3451 | 108 | 1521 | nein |
| `initChatGeneralMSGTable` | 3607 | 169 | 8162 | nein |
| `initChatprivateMSGTable` | 3778 | 376 | 8015 | nein |
| `initChatMemberTable` | 1824 | 852 | 8293 | nein |
| `initTopPriorityListPane` | 4671 | 44 | 9442 | nein |
| `initSkedWarnIndicatorButton` | 6161 | 17 | 7611 | nein |
| **`initConnectionStateIndicatorButton`** | 6180 | 18 | 7605 | **ja — über `:6195` → `:6217`** |
| `initBandUpgradeIndicatorButton` | 6361 | 16 | 7634 | nein |
| **`initFurtherInfoAbtCallsignMSGTable`** | 3218 | 213 | 893 | **ja — :3404** |
| **`initMenuBar`** | 5729 | 337 | 7599 | **ja — 10 Stellen** |

## Reihenfolge für Teil 2

Nach jeder Scheibe `./gradlew clean build` — und bei jeder Scheibe, die etwas löscht,
vorher der Durchlauf aus „Wie geprüft werden muss".

**Vorab, ohne Löschen:**

- **A — Kandidatenfenster nach Compose.** Der Sperrriegel. Mit ihm fallen
  `registerThemedScene` und `installSharedSystemMenuBar` als einzige lebende Nutzer.
- **B — Die Zeitleisten-Zuführung aus `start()` lösen.** `timelineView` und die
  Listener bei `:7428`, `:7431`, `:7446` bekommen ein eigenes Zuhause, das nicht am
  JavaFX-Aufbau hängt. Prüfbar: die Compose-Zeitleiste zeigt weiter Skeds und
  Kandidaten.
- **C — `selectedCallSignInfoMessageBinding` aus `initFurtherInfoAbtCallsignMSGTable`
  lösen.** Prüfbar: die Nachrichtentabelle der ausgewählten Station füllt sich noch.
- **D — Die Menüeinträge aus `initMenuBar` gegen `MainMenuBar.kt` abgleichen.**
- **E — Den Compose-Schreibzugriff aus `updateConnectionStateIndicator` lösen**
  (`:6217`–`:6219`), damit `:9844`/`:9871` ihn weiter erreichen, wenn das
  JavaFX-Tooltip fällt. Prüfbar: die Verbindungsanzeige in der Compose-Statusleiste
  wechselt weiter.

**Dann löschen, Blätter zuerst:**

1. ~~Unreferenziert: `initShortcutTable`, `initNotifyAtCallSignTable`,
   `initTextSnippetsTable`, `initWkdStnTable`~~ — **erledigt am 2026-10-01**,
   474 Zeilen, Suite unverändert bei 769 Tests.
2. `initDXClusterTable`, `initChatToOtherMSGTable`, dann
   `initBottomGlobalMessageTabPane` — ~424 Zeilen.
3. `initFurtherInfoAbtCallsignMSGTable` (nach C) und
   `initSelectedCallSignCompactControlsPane` — ~321 Zeilen. Vorher prüfen, ob die
   Aufrufer `:893` und `:1521` nach C noch erreichbar sind.
4. `initChatGeneralMSGTable`, `initChatprivateMSGTable` — ~545 Zeilen.
5. `initChatMemberTable` (852) und `initChatMemberTableContextMenu` — größte Scheibe.
6. `initTopPriorityListPane` und die drei Statusknöpfe — ~95 Zeilen;
   `initConnectionStateIndicatorButton` erst nach E.
7. `initMenuBar` (337) — nach A und D.
8. Der Rest von `start()`, Zeile für Zeile: was nicht in B übernommen wurde, die
   `scn_ChatwindowMainScene` samt Listenern, `themedScenes`, `chatMemberListBinding`
   und die tote CW-Ausgabe (`:6966` ff.).

Erst danach sind `UiDispatcher`, Lebenszyklus, Dialoge und der Build an der Reihe.
