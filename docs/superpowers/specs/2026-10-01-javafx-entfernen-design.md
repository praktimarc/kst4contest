# Entwurf — JavaFX aus KST4Contest entfernen

Vertieft: `docs/superpowers/specs/2026-09-25-compose-migration-design.md`, Etappe 6,
zweite Hälfte („Entfernung von JavaFX").

Die erste Hälfte — die Karte als Compose-Canvas — ist umgesetzt und abgenommen
(`docs/superpowers/plans/2026-10-01-etappe6-karte-compose-parity.md`). Dieser Entwurf
behandelt ausschließlich, was danach noch zwischen dem Projekt und einem Build ohne
`org.openjfx` steht.

## Ziel

Release 1.50 ohne jede JavaFX-Abhängigkeit. Erfolg ist nachprüfbar:

- `grep -rl "javafx\." core/src/main app-desktop/src/main` liefert nichts.
- Der `org.openjfx`-Block ist aus `app-desktop/build.gradle.kts` und
  `gradle/libs.versions.toml` entfernt.
- `./gradlew clean build` bleibt grün (Stand heute: 766 Tests, 0 Fehler).
- Die Anwendung verhält sich unverändert: Ton, Dialoge, Farben, Fenstergrößen.
- Offline-DEM, Terrain-Pakete und der Profil-Cache funktionieren unverändert
  (Abnahmekriterium der übergeordneten Spezifikation).

## Der Befund, der die Etappe umkrempelt

Die naheliegende Lesart — „11.655 Zeilen `Kst4ContestApplication` nach Compose
portieren" — ist falsch.

**Das JavaFX-Hauptfenster wird bei jedem Start vollständig aufgebaut und nie
angezeigt.** `primaryStage.setScene(...)` und `primaryStage.show()` sind beide
auskommentiert (`Kst4ContestApplication.java:9508`, `:9517`). Angezeigt wird
`MainWindowHost.show(...)` (`:10667`), also Compose. Die Szene
`scn_ChatwindowMainScene` wird gebaut (`:7548`), thematisiert und mit
Größen-Listenern versehen, aber an keine Bühne gehängt.

Der Aufbau läuft trotzdem: 93 `new TableView` / `new TableColumn`, Kontextmenüs,
Tastaturhandler. Das kostet Startzeit und hält Listener, ohne je sichtbar zu werden.

Ein zweiter Irrweg, den dieser Entwurf ausräumt: die **CW-Tonausgabe** sieht nach
einem harten Blocker aus, weil `Kst4ContestApplication` `javafx.scene.media.Media`
und `MediaPlayer` benutzt und 91 MP3-Dateien im Projekt liegen. Diese Kopie ist tot —
`playCWLauncher` bei `:6966` ruft niemand. Die **lebende** Ausgabe sitzt in
`core/src/main/java/kst4contest/utils/PlayAudioUtils.java:182`, wird aus
`MessageBusManagementThread.java:1105` gerufen und spielt über JLayer
(`javazoom.jl.player.Player`). Kein JavaFX, keine Ersatzentscheidung, keine neue
Abhängigkeit.

## Die lebende JavaFX-Fläche

| Was | Wo | Ersatz |
|---|---|---|
| Anwendungs-Lebenszyklus | `extends Application`, `launch(args)` (`:9806`), `start(Stage)` (`:7356`) | Compose-Anwendung aus einem einfachen `main` |
| UI-Thread-Zustellung | `JavaFxUiDispatcher` → `Platform.runLater` | AWT-Ereignisfaden |
| Dialoge | 10 × `new Alert(...)` | eigene Compose-Fenster über `ComposeWindowHost` |
| Farbableitung | **bereits erledigt** — siehe Korrektur unten | — |
| Fensterrahmen | `Screen` (Startgröße), Anwendungssymbol | Compose / AWT |
| Kandidatenfenster | eigene `Scene` + `stage.show()` (`:4855`–`:4860`), geöffnet aus dem **Compose**-Pfad (`:10707`) | Compose-Fenster |
| Systemmenüleiste | `installSharedSystemMenuBar(candidatesScene)` (`:4858`), `initMenuBar()` (`:5729`) | hängt am Kandidatenfenster, siehe Etappe 1 |
| Roster-Spiegel | `FxRosterBinding` nutzt `ObservableList`/`FXCollections` | **Status ungeklärt — siehe Etappe 1** |

Dateien mit JavaFX-Importen unter `src/main`, nach Größe:

```
11655  Kst4ContestApplication.java      49 Importe
 1777  StationMapView.java              20
 1161  PathProfileChart.java             4
  480  StationMapBridge.java             5
  376  TimelineView.java                11
  353  JavaFxStylesheet.kt               1
  326  MessageTextTableCell.java        11
  280  TableLayoutManager.java           9
  136  ComposeMenuActions.java           1
  129  FxRosterBinding.java              2
  123  GuiUtils.java                     4
  110  MessageVariableResolverTest.java  1   (Testdatei im main-Quellbaum)
  109  TruncatedTextTableCell.java       4
  106  ApplicationRuntimeLauncher.java   2
   50  LayoutAutosave.java               3
   31  JavaFxUiDispatcher.java           1
```

## Der Dreh- und Angelpunkt

`core` kennt JavaFX nicht. Es kennt die Schnittstelle `UiDispatcher`, und
`JavaFxUiDispatcher` ist die einzige Umsetzung, die sie an den JavaFX-Faden bindet.

**Diese 31 Zeilen sind der Hebel.** Wird die Zustellung auf den AWT-Ereignisfaden
umgestellt — dort läuft Compose Desktop ohnehin —, fällt die Thread-Kopplung zwischen
`core` und JavaFX in einem Stück. Lebenszyklus, Dialoge und Fensterrahmen hängen
daran, nicht umgekehrt.

Der Vertrag bleibt derselbe: `runOnUi` stellt zu und führt sofort aus, wenn es bereits
auf dem UI-Faden läuft; `isUiThread` beantwortet dieselbe Frage; eine geworfene
Ausnahme darf den Faden nicht reißen. Diese drei Punkte sind testbar, und genau das
macht den Austausch beherrschbar.

## Vorgehen: erst löschen, dann umhängen

Abgewogen wurden drei Wege.

**Erst umhängen, dann löschen** wurde verworfen: der Eingriff am Lebenszyklus fände
genau dann statt, wenn die Datei am unübersichtlichsten ist. Das Nebenwirkungsrisiko
wäre dort am höchsten, wo es am schlechtesten zu sehen ist.

**Zweiter Einstiegspunkt daneben**, umschaltbar, wurde verworfen: zwei Lebenszyklen in
einem Prozess mit globalem Zustand (`ApplicationRuntimeLauncher`, Profilwechsel), und
der Schalter wird selbst zur Sache, die gepflegt und getestet werden muss.

**Gewählt: erst löschen, dann umhängen.** Der tote Aufbau ist das Volumen und zugleich
das Risikoärmste. Danach steht eine kleine, lesbare Datei, und erst darin die
schwierige Operation.

## Etappen

### 1 — Feststellen, was lebt

Keine Löschung. Ergebnis ist eine belegte Liste.

Zu klären ist für jeden Teil des nie gezeigten Aufbaus, ob er Nebenwirkungen hat, auf
die der laufende Betrieb angewiesen ist. Drei Verdachtsfälle sind benannt:

- **`FxRosterBinding`** (`chatMemberListBinding`, erzeugt bei `:2513`, genullt bei
  `:6877`). Es spiegelt den Roster in eine `ObservableList`. Speist es noch etwas, das
  die Compose-Oberfläche liest, oder nur die tote Tabelle?
- **`registerThemedScene`** — drei Aufrufe. Hängt die Themenumschaltung der
  Compose-Fenster daran?
- **`LayoutAutosave`** (`:7420`) — schreibt es Layoutwerte, die auch die
  Compose-Fenster benutzen?

Dazu die veralteten Begründungen, die beim Lesen auffallen: der Kommentar bei
`:10446` rechtfertigt eigene Spalten-IDs damit, dass „beide Fenster auf dem Bildschirm
sind". Das stimmt seit dem Abklemmen nicht mehr.

**Abnahme:** eine Liste, die jeden Block des toten Aufbaus einem von zwei Töpfen
zuordnet — „folgenlos löschbar" oder „zu entflechten", letzteres mit der Stelle, die
davon abhängt.

### 2 — Toten Aufbau löschen

In Scheiben, jede für sich gebaut und geprüft. Eine Scheibe ist ein zusammenhängender
Block: die Stationstabelle, die Nachrichtentabellen, die Kontextmenüs, die
Filterleiste, die Szene samt Listenern.

Mit fällt die tote CW-Ausgabe (`:6966` ff., `musicList`, `mediaPlayer`) und damit der
Import von `javafx.scene.media`. Die 91 MP3-Dateien in `core/src/main/resources`
**bleiben** — sie gehören der lebenden Ausgabe in `PlayAudioUtils`.

**Abnahme:** `./gradlew clean build` grün nach jeder Scheibe; die Anwendung startet und
verhält sich unverändert; `Kst4ContestApplication` ist auf die lebende Fläche
zusammengeschrumpft.

### 3 — Den `derive`-Pin einfrieren

**Korrektur gegenüber der ersten Fassung dieses Entwurfs.** Dort stand, `derive` rufe
JavaFX über Reflexion und müsse ersetzt werden. Das ist falsch, und die übergeordnete
Spezifikation ist an dieser Stelle inzwischen veraltet: `JavaFxStylesheet.derive`
(`JavaFxStylesheet.kt:131` ff.) ist eine vollständige, JavaFX-freie Umsetzung. Das
einzige Vorkommen von „javafx" in der Datei steht in einem Kommentar, der noch die
alte Reflexionsfassung beschreibt.

Mehr noch: die Umsetzung ist bereits gegen echtes JavaFX gepinnt.
`JavaFxStylesheetTest.kt:165`–`:170` ruft `com.sun.javafx.util.Utils.deriveColor` per
Reflexion **im Test** und prüft, dass beide Fassungen übereinstimmen (`:48`–`:50`).
Die Reflexion sitzt also dort, wo sie hingehört.

Was bleibt, ist klein und trotzdem zeitkritisch: **dieser Vergleich hört auf zu
funktionieren, sobald JavaFX weg ist.** Der Test fiele dann stillschweigend auf die
Prüfung gegen sich selbst zurück. Die von JavaFX gelieferten Werte müssen vorher als
Daten eingecheckt werden, damit der Pin das Entfernen überlebt.

Der veraltete Kommentar bei `:116`–`:129` wird mitkorrigiert.

Zur Einordnung: die übergeordnete Spezifikation hält fest, dass eine freihändige
Nachbildung in Etappe 3b nachweislich falsch war: die Helligkeit, von der JavaFX
ableitet, ist nicht der HSB-Wert, sondern `0.3R + 0.59G + 0.11B`, und der positive
Zweig folgte keiner naheliegenden Formel.

Vorgehen: solange JavaFX im Prozess ist, die Paare, die der Test heute zur Laufzeit
von `deriveColor` holt, über den benutzten Wertebereich erzeugen und als Testdaten
einchecken. Der Test vergleicht danach gegen diese Daten.

**Abnahme:** die von JavaFX gelieferten Werte liegen als eingecheckte Daten vor; der
Test prüft `derive` gegen diese Daten und nicht mehr gegen einen Reflexionsaufruf; er
bleibt grün, wenn JavaFX nicht auf dem Klassenpfad ist.

### 4 — `UiDispatcher` umhängen

`JavaFxUiDispatcher` → eine Umsetzung auf dem AWT-Ereignisfaden.

**Abnahme:** Tests decken Zustellung von einem Fremdfaden, sofortige Ausführung auf
dem UI-Faden, korrekte Thread-Erkennung und ab, dass eine geworfene Ausnahme den Faden
nicht reißt. Die Anwendung läuft unverändert.

Dies ist die risikoreichste Etappe: `core` stellt über diese Schnittstelle **jede**
UI-sichtbare Änderung zu. Deshalb steht sie hinter dem Aufräumen und vor dem
Lebenszyklus.

### 5 — Lebenszyklus, Dialoge, Rahmen

- `Kst4ContestApplication extends Application` und `launch(args)` weichen einem
  einfachen `main`, das die Compose-Anwendung startet.
- Die zehn `Alert`-Aufrufe werden eigene Compose-Fenster über `ComposeWindowHost`,
  modal, wo sie es heute sind: die Bestätigungen liefern eine Antwort, die den
  weiteren Ablauf steuert, und müssen weiter blockieren.
- `Screen` für die Startgröße und das Anwendungssymbol gehen an Compose beziehungsweise
  AWT.
- Das **Kandidatenfenster** (`:4855`–`:4860`) wird ein Compose-Fenster. Es ist die
  einzige JavaFX-Bühne, die tatsächlich erscheint, und sie wird aus dem Compose-Pfad
  geöffnet (`:10707`): der „more"-Knopf neben den Prioritätsknöpfen. Solange es steht,
  kann keine JavaFX-Abhängigkeit fallen.

**Abnahme:** die Anwendung startet ohne JavaFX-Bühne; jeder der zehn Dialoge erscheint
an derselben Stelle im Ablauf wie bisher und mit derselben Wirkung auf den Ablauf.

### 6 — Reste löschen und den Build befreien

- `StationMapView.java`, `StationMapBridge.java`, `MapHtmlResources.java` und die
  Leaflet-Ressourcen unter `app-desktop/src/main/resources/web/leaflet/`.
- `PathProfileChart.java` und `TimelineView.java` — Compose-Gegenstücke existieren.
- `MessageTextTableCell.java`, `TruncatedTextTableCell.java` — kein Compose-Nutzer.
- `JavaFxUiDispatcher.java` — fällt in jedem Fall, sein Ersatz entsteht in Etappe 4.
- `FxRosterBinding.java`, `GuiUtils.java`, `LayoutAutosave.java`, soweit Etappe 1 sie
  als entbehrlich ausweist.
- `MessageVariableResolverTest.java` liegt im **main**-Quellbaum und gehört nach
  `src/test`.
- `app-desktop/build.gradle.kts` Zeilen 3, 9–11 und 19; `gradle/libs.versions.toml`
  Zeilen 2–3 und 28.

**Eine Falle:** `TableLayoutManager.java` sieht genauso tot aus, wird aber von sechs
Compose-Dateien benutzt. Er muss seine JavaFX-Importe verlieren, nicht gelöscht werden.

`TileProxyServer` in `core` verliert mit `StationMapView` seinen einzigen Nutzer. Er
war eine Antwort auf TLS-Probleme der JavaFX-WebView in AppImage- und
Flatpak-Verpackung; der Compose-Canvas holt seine Kacheln selbst. Mit entfernen.

**Abnahme:** die drei Prüfungen aus dem Abschnitt „Ziel".

## Prüfung

Das Netz, auf das diese Etappe sich stützt, ist seit heute belastbar: `./gradlew clean
build` ist grün (766 Tests, 0 Fehler), erstmals seit dem 30. September. Jede
Rotfärbung während dieser Arbeit ist damit ein Signal und kein Rauschen.

Dazu kommen:

- die gepinnte `derive`-Tabelle (Etappe 3),
- echte Tests für den neuen Dispatcher (Etappe 4),
- die Compose-Layouttests aus der Kartenetappe, die die Fenster von 900×600 bis 1×1 px
  abdecken,
- nach jeder Löschscheibe ein Start der Anwendung, weil kein Test beantwortet, ob ein
  entfernter Listener gefehlt hat.

## Was unverändert bleibt

- Die Fachlogik in `core` und sämtliche Protokollformate.
- Die **Schnittstelle** `UiDispatcher`; nur ihre Umsetzung wird getauscht.
- Die Invarianten aus `AGENTS.md`: `ObservableRoster` ist der Speicher und die Listen
  sind Spiegel; kein Worker-Faden fasst UI-Sammlungen an; Zoom, Auswahl, Fokus,
  Sortierung und vorbelegter Text ändern sich nicht als Nebeneffekt.
- Die 91 MP3-Dateien und die CW-Ausgabe in `PlayAudioUtils`.
- Verpackung und Vertrieb, soweit sie nicht am JavaFX-Modulpfad hängen.

## Risiken

**Etappe 1 kann das Vorgehen kippen.** Ergibt die Prüfung, dass der „tote" Aufbau
lebende Nebenwirkungen hat, wird aus Löschen ein Entflechten und die Etappe wird
länger. Das ist der Grund, warum die Prüfung eine eigene Etappe ist und keine Annahme
im Plan.

**Der Dispatcher-Tausch wirkt überall.** Ein Fehler darin zeigt sich nicht als
Übersetzungsfehler, sondern als sporadisch nicht aktualisierte Oberfläche. Deshalb die
Tests für den Vertrag und nicht nur für die Anwendung.

**Die Dialoge sind Ablaufsteuerung, keine Dekoration.** Wer eine Bestätigung
nicht-blockierend macht, ändert den Ablauf. Jeder der zehn Aufrufe ist einzeln zu
lesen.

## Offene Punkte

- Der Status von `FxRosterBinding` ist Gegenstand von Etappe 1 und wird hier bewusst
  nicht vorweggenommen.
- Ob `ApplicationRuntimeLauncher` und `ComposeMenuActions` nach dem Wegfall der Bühne
  noch eine JavaFX-Berührung behalten, entscheidet sich in Etappe 5.
- Die macOS-Systemmenüleiste ist eine JavaFX-`MenuBar` (`:172`, `:5729`) und wird auf
  die Szene des Kandidatenfensters gesetzt (`:4858`). Ob sie darüber hinaus an etwas
  hängt — und was von ihr auf macOS tatsächlich sichtbar ist, seit das Hauptfenster
  Compose ist — gehört in Etappe 1.
