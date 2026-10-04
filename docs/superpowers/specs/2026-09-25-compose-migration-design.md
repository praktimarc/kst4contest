# Konzept: Ablösung von JavaFX durch Compose Multiplatform

> Status: Konzept, noch nicht umgesetzt. Zielrelease **1.50**. Alle Zeilenanker beziehen sich auf `nextMajorRelease/version1_50` und wurden gegen den Branch verifiziert (Stand 2026-09-25).

## Ziel

KST4Contest verlässt JavaFX und wird auf Compose Multiplatform umgestellt. Ergebnis ist eine wartbare, optisch aufgefrischte Desktop-Oberfläche bei **unverändertem Funktionsumfang**. Der fachliche Kern wird dabei von jeder UI-Technologie gelöst.

Drei Treiber, gleichrangig:

1. Die Oberfläche soll modern aussehen und sich modern anfühlen.
2. Der UI-Code soll wieder änderbar sein.
3. Die JavaFX-Abhängigkeit soll verschwinden.

## Nicht-Ziele

- **Kein UX-Neuentwurf.** 1.50 ist ein funktionsgleicher Port. Bedienabläufe, Fensteraufteilung und Funktionsumfang bleiben erhalten. Ein echter Neuentwurf der Bedienung ist Thema für 2.0.
- **Keine Mobile-App.** Die Modulgrenze wird so gezogen, dass ein späteres Mobile-Repo den Kern abgreifen könnte. Mehr nicht. Mobile ist in diesem Konzept Randbedingung, kein Lieferergebnis.
- **Keine Kotlin-Konvertierung des Kerns.** Siehe „Bestätigte Festlegungen".
- **Keine Protokolländerungen.** Kein Framing, kein Port, kein Transport, keine Normalisierung wird angefasst.

## Ausgangslage

Gemessen am Branchstand:

| Kennzahl | Wert |
|---|---|
| Java gesamt | 61.572 Zeilen, 196 Dateien |
| Kotlin | 0 Dateien |
| Dateien mit direktem JavaFX-Bezug | 33 |
| `Kst4ContestApplication.java` | 13.564 Zeilen, 387 Methoden |
| davon reines JavaFX-Gerüst | 82 `call()`-Cell-Factories, 64 `changed()`-Listener, 77 `handle()`-Eventhandler |
| `Platform.runLater` / `ObservableList` | 51 / 93 Vorkommen |
| Tests | 43 Dateien, 148 Testmethoden |

### Die JavaFX-Kopplung ist flacher als die Zahlen vermuten lassen

- `ChatMember`: **ein** gekoppeltes Feld, `StringProperty frequency` (`ChatMember.java:42`, Zugriff `:510`, `:516`, Neuanlage `:616`). Der Rest der Klasse ist reines Java.
- `ChatPreferences`: fünf Properties (`actualQTF` `:228`, `MYQRGFirstCat` `:266`, `MYQRGSecondCat` `:268`, `notify_optionalFrequencyPrefix` `:296`, `notify_DXCSrv_SpottersCallSign` `:298`) und vier `ObservableList<String>` für Listeneinstellungen (`:307`, `:308`, `:324`, `:325`).
- `ScoreService`: bereits sauber getrennt. Es existiert ein Snapshot-Modell, `topCandidatesFx` (`:45`, `:62`) ist explizit als FX-Projektion benannt, und der Klassenkommentar hält „No per-member Platform.runLater flooding" fest.
- `MessageBusManagementThread`: eine einzige Methodensignatur nimmt `ObservableList` entgegen (`:577`).
- `ChatController` ist der Brocken: sieben `ObservableList`-Felder (`:1164`, `:1742`, `:1759`, `:1768` mit der synchronisierten Hülle `:1775`, `:1776`, `:1777`, `:1817`) und eine `FilteredList`/`SortedList`-Pipeline (`:1745`, `:1749`, `:1751`, `:1756`, `:1772`, `:1773`).

Die Klasse dokumentiert ihr eigenes Muster bereits: der Kommentar ab `ChatController.java:1783` begründet, warum eine threadsichere Map die primäre Quelle ist und die `ObservableList` nur die gebündelte Projektion. Die hier vorgeschlagene Trennung setzt das fort, statt es zu erfinden.

### Die Karte hängt nur flach an Leaflet

`MapHtmlResources.java` nutzt aus der Leaflet-API lediglich `L.map`, `L.tileLayer`, `L.marker`, `L.divIcon`, `L.circleMarker`, `L.polyline`, `L.polygon`, `L.rectangle`, `L.layerGroup`, `L.latLng` und `L.latLngBounds`. Geometrie (`PathGeometryUtils`), Gitterplanung (`MaidenheadGridRenderPlanner`), Kachelbeschaffung (`TileProxyServer`), Terrain und Pfadanalyse liegen bereits vollständig in Java.

### Weitere betroffene Stellen

- `javafx.scene.media` wird ausschließlich in `Kst4ContestApplication.java` benutzt (`:33`, `:34`, `:6599`, `:6900`). MP3-Wiedergabe läuft ohnehin schon über `jlayer` in `PlayAudioUtils`.
- Lombok ist in `pom.xml` als Abhängigkeit deklariert, wird aber in keiner Quelldatei verwendet.
- `src/main/java/module-info.java` bindet das Projekt an JPMS; `pom.xml` enthält eine Enforcer-Regel, die die jpackage-Modulliste gegen Drift absichert.

## Bestätigte Festlegungen

| Punkt | Entscheidung |
|---|---|
| Zielrelease | 1.50. Die Compose-Migration findet auf `nextMajorRelease/version1_50` statt; 1.50 wird das Compose-Release |
| Funktionsumfang | Funktionsgleicher Port. UX-Neuentwurf frühestens 2.0 |
| Sprache Kern | `core` bleibt **Java 21**. Kotlin nur dort, wo Compose es erzwingt |
| Sprache UI | Kotlin, `app-desktop` |
| Build | Maven wird durch **Gradle** ersetzt. Compose Multiplatform wird offiziell nur mit Gradle unterstützt, Desktop-Packaging inbegriffen |
| Modulsystem | `module-info.java` entfällt. Compose Multiplatform liefert keine echten JPMS-Module |
| Packaging | Etappe 1 behält den direkten `jpackage`-Aufruf, nur aus Gradle statt aus Maven. Die Umstellung auf `compose.desktop.nativeDistributions` erfolgt in Etappe 3, wenn Kotlin und Compose ohnehin einziehen. DMG-Drag-Layout, Icon-Erzeugung, Flatpak und AUR werden dabei nachgezogen |
| Karte | Native Compose-Canvas-Umsetzung. Kein eingebettetes Chromium (KCEF/JCEF) |
| God-Klasse | `Kst4ContestApplication` wird in Phase 1 **nicht** zerschnitten. Compose löscht den Widget-Code ohnehin |
| Mobile | Nur als Modulgrenze berücksichtigt, nicht umgesetzt |
| Lombok | Entfällt (ungenutzt) |
| Entwickler | Marc (DO5AMF) und Philipp (DN9APW). Kein Termin |

## Zielarchitektur

```
kst4contest/
├── core/          Java 21, KEIN JavaFX, KEIN Compose
│   ├── model/         ChatMember, ChatMessage, ChatPreferences, Band …
│   ├── controller/    ChatController, MessageBus, Score, Reachability …
│   ├── net/           ON4KST, AirScout, Win-Test, UCXLog, PSTRotator, DXCluster
│   ├── map/           Geometrie, Terrain, Kachel-Proxy, Pfadanalyse
│   ├── audio/         Wiedergabe hinter einem Interface
│   └── observe/       Beobachtungs-Abstraktion
└── app-desktop/   Kotlin + Compose Multiplatform
    ├── ui/            Fenster, Panels, Tabellen, Dialoge
    ├── state/         UI-Zustand, Projektionen aus core
    └── map/           Compose-Canvas-Karte
```

`core` hängt von keiner UI-Technologie ab. Das ist die Grenze, an der parallel gearbeitet werden kann, und dieselbe Grenze, die ein späteres Mobile-Repo abgreifen würde.

### Das Paket `core/observe`

Bewusst minimal. Kein Reaktiv-Framework, keine Bibliothek.

- **`ObservableValue<T>`** — `T get()`, `void addListener(Consumer<T>)`, `void removeListener(Consumer<T>)`.
- **`MutableValue<T> extends ObservableValue<T>`** — zusätzlich `void set(T)`.
- **`ObservableRoster<T>`** — **snapshotbasiert**: `List<T> snapshot()` liefert eine unveränderliche Liste; Listener erhalten die neue Liste. Es gibt **kein** inkrementelles Änderungsprotokoll. Das passt zum Neuzeichnungsmodell von Compose und ist ohne UI testbar.
- **`UiDispatcher`** — `void runOnUi(Runnable)` und `boolean isUiThread()`.

`UiDispatcher` ist der Schlüssel der gesamten Umstellung. In Phase 1 wird er von `Platform::runLater` und `Platform::isFxApplicationThread` implementiert, in Phase 2 vom Compose-Main-Dispatcher. Die Invariante aus `AGENTS.md` bleibt dabei wortgleich gültig: Worker-Threads mutieren keine UI-gebundenen Sammlungen, UI-sichtbare Änderungen laufen über den Dispatcher.

### Was aus der JavaFX-Pipeline wird

`FilteredList` und `SortedList` werden zu einer **reinen Funktion** in `core`: aus Mitgliederliste, Filterprädikaten und Komparator entsteht eine unveränderliche Ergebnisliste. Die Stationslisten-Logik wird damit erstmals ohne laufende UI testbar. Das ist der wesentliche fachliche Gewinn von Phase 1.

Die vorhandene Bündelung und Entprellung (`flushPendingChatMessagesToUi`, das Coalescing der Mitgliederliste) **bleibt erhalten**. Sie ist Leistungsschutz bei mehreren tausend ON4KST-Nutzern, kein JavaFX-Beiwerk.

## Etappen

Jede Etappe ist für sich abgeschlossen, lieferbar und bekommt einen eigenen Umsetzungsplan.

### Etappe 1 — Gradle, zwei Module, JPMS entfernen, Packaging

**Ziel:** Die unveränderte JavaFX-Anwendung baut, testet und paketiert unter Gradle.

**Umfang**

- Gradle-Wrapper ersetzt `mvnw`/`mvnw.cmd`.
- Aufteilung in die Module `core` und `app-desktop`; in dieser Etappe enthält `app-desktop` noch die JavaFX-Oberfläche.
- **`core` darf in dieser Etappe noch von JavaFX abhängen.** `ChatController`, `ChatMember`, `ChatPreferences` und `ScoreService` benutzen es; Etappe 2 entfernt die Abhängigkeit. Etappe 1 bleibt damit eine reine Build-Umstellung ohne fachliche Änderung.
- Paketnamen und `import`-Anweisungen ändern sich nicht. Gradle-Module erzwingen keine Paketumbenennung; die Aufteilung ist ein Verschieben von Verzeichnissen.
- `src/main/java/module-info.java` wird entfernt; die Enforcer-Regel zur jpackage-Modulliste in `pom.xml` entfällt mit ihr.
- SpotBugs- und PMD-Regelsätze wandern mit; `pmd-ruleset.xml` bleibt unverändert gültig.
- Lombok wird aus den Abhängigkeiten entfernt.
- Packaging für Windows, Linux und macOS ruft `jpackage` weiterhin direkt auf, künftig aus einer Gradle-Aufgabe. Das DMG-Drag-Layout, die Icon-Erzeugung, Flatpak und AUR bleiben inhaltlich unverändert; nur die Herkunft der Eingabedateien wechselt von `target/dist-libs` auf `app-desktop/build/dist-libs`.
- Die Projektversion wandert nach `gradle.properties`. Zehn Stellen in fünf Workflows und zwei PKGBUILDs lesen sie heute aus `pom.xml` und müssen umgestellt werden, bevor `pom.xml` entfällt.
- Die GitHub-Workflows unter `.github/` werden auf Gradle umgestellt.

**Abnahmekriterium:** Alle 148 vorhandenen Tests laufen unter Gradle. Auf allen drei Plattformen entsteht ein installierbares Paket, das sich wie das bisherige verhält. Stable-, Beta- und Nightly-Strecke funktionieren unverändert.

### Etappe 2 — `core` von JavaFX befreien

**Ziel:** `core` compiliert ohne JavaFX auf dem Klassenpfad. Die JavaFX-Oberfläche läuft unverändert und beweist, dass die Schnittstelle trägt.

**Umfang**

- `core/observe` entsteht wie oben beschrieben.
- `ChatMember.frequency` wird von `StringProperty` auf `MutableValue<String>` umgestellt (`ChatMember.java:42`, `:510`, `:516`, `:616`).
- Die fünf Properties und vier `ObservableList<String>` in `ChatPreferences` werden auf `MutableValue` beziehungsweise `ObservableRoster` umgestellt.
- Die sieben `ObservableList`-Felder in `ChatController` werden zu `ObservableRoster`; die `FilteredList`/`SortedList`-Pipeline wird durch die reine Filter-/Sortierfunktion ersetzt.
- `ScoreService` gibt seinen Snapshot über `ObservableRoster` heraus statt über `topCandidatesFx`; `Platform.runLater` wird durch `UiDispatcher` ersetzt.
- `MessageBusManagementThread.checkListForChatMemberIndexByCallSign` (`:577`) nimmt eine `List<ChatMember>` statt `ObservableList`.
- Audio wandert nach `core/audio` hinter ein Interface. `javafx.scene.media` (`Kst4ContestApplication.java:33`, `:34`, `:6599`, `:6900`) entfällt; MP3 über `jlayer`, WAV über `javax.sound.sampled`.
- `app-desktop` stellt die JavaFX-Implementierung von `UiDispatcher` bereit und adaptiert `ObservableRoster` auf `ObservableList` für die bestehenden `TableView`.

**Absicherung:** Jede herausgelöste Einheit bekommt **vorher** einen Test, der das heutige Verhalten festschreibt — insbesondere Filterung und Sortierung der Stationsliste, die heute in `FilteredList`/`SortedList` steckt und nicht testbar ist.

**Abnahmekriterium:** `core` hat keine JavaFX-Abhängigkeit mehr. Die Anwendung verhält sich unverändert: Stationsliste, Chat-Tabs, Prioritäten, Skeds, Cluster, Worked-Zustand, Profilwechsel.

### Etappe 3 — `DataTable` und Einstellungsfenster in Compose

**Ziel:** Das erste Compose-Fenster ist im Produkt.

**Umfang**

Der wesentliche Baustein dieser Etappe ist eine wiederverwendbare **`DataTable`**-Komponente. Compose kennt kein `TableView`, und die Anwendung hat fünf Tabellen: `TableView<ChatMember>`, `TableView<ChatMessage>`, `TableView<ClusterMessage>`, `TableView<String>` und die Top-Prioritätenliste. Alle brauchen dasselbe:

- Spaltenbreiten mit stabilen Tabellen- und Spalten-IDs, wie sie `preferences.xml` Version 7 bereits kennt;
- Sortierung nach Spalte;
- Zeilenauswahl und Kontextmenü;
- Zeilen- und Zell-Styling, heute `PrivateMessageRowStyleResolver`, `MessageTextTableCell`, `TruncatedTextTableCell`, `TruncatedTextTooltipSupport`.

Umgesetzt als `LazyColumn` mit eigener Kopfzeile. Wird diese Komponente gut, ist der Rest der Portierung Fleißarbeit; wird sie schlecht, zieht sich das durch die gesamte Anwendung.

Mit dieser Etappe ziehen Kotlin und das Compose-Gradle-Plugin ein. Das Packaging wird dabei von den direkten `jpackage`-Aufrufen auf `compose.desktop.nativeDistributions` umgestellt — der Zeitpunkt ist hier richtig, weil JavaFX dann nicht mehr über `jlink` gebündelt werden muss.

Danach das Einstellungsfenster mit seinen zwölf Reitern: Station, Log synch, TRX synch, Airscout, Notification, Shortcuts, Macros, Beacon, Messagehandling, Workedstn database, GUI, Profiles. Es ist in sich abgeschlossen, hat wenig Domänenkopplung und eignet sich deshalb als Lernstück.

**Abnahmekriterium:** Das Einstellungsfenster ist vollständig in Compose, alle zwölf Reiter sind funktionsgleich, und geänderte Einstellungen landen unverändert in `preferences.xml`.

> Nachzuschärfen nach Etappe 2: der genaue Zuschnitt der `DataTable`-Schnittstelle. Er hängt davon ab, wie sich `ObservableRoster` in der Praxis anfühlt.

### Etappe 4 — Profil-Dialog, Update-Fenster, Monitor-Fenster

**Ziel:** Drei weitere Fenster portiert; die Muster aus Etappe 3 sind bestätigt.

**Umfang**

- `OperatorProfilePickerDialog` und `OperatorProfileSettingsPane`.
- Das Update-Fenster (`Kst4ContestApplication.java:9242`).
- Das Monitor-Fenster für Cluster und QSOs (`:9180`), das die `DataTable` erstmals mit Live-Daten belastet.

Die Layout-IDs für das Monitor-Fenster bleiben von denen des Hauptfensters getrennt, wie in `PROJECT_CONTEXT.md` festgelegt.

**Abnahmekriterium:** Die drei Fenster sind funktionsgleich. Fenstergrößen, Positionen und Spaltenbreiten werden unverändert entprellt gespeichert und beim Herunterfahren geschrieben.

### Etappe 5 — Hauptfenster

**Ziel:** JavaFX wird nur noch für die Karte benötigt.

**Umfang**

- Stationsliste mit Filterleiste und Prioritätenspalten.
- Die drei Chat-Reiter „Public messages", „DXCluster messages", „QSO of the other".
- Der Info-Bereich zur ausgewählten Station (`generateFurtherInfoAbtSelectedCallsignBP`, `Kst4ContestApplication.java:707`) samt seinem vertikalen `SplitPane` und dessen gespeicherten Divider-Positionen.
- Die Top-Prioritätenliste (`initTopPriorityListPane`, `:4416`).
- Die Statusleiste mit ihren Indikatoren: Sked-Warnung (`:5868`), Verbindungszustand (`:5887`), Band-Upgrade (`:6045`).
- `TimelineView`.
- Menüleiste, einschließlich der macOS-Systemmenüleiste.

**Profilwechsel:** Der heutige Mechanismus reißt die Laufzeit ab und baut eine **neue** `Kst4ContestApplication`-Instanz auf, weil viele Bedienelemente inline initialisierte Instanzfelder sind. Dieser Grund entfällt in Compose — der Zustand hängt nicht mehr an Feldern einer Riesenklasse. Der Wechsel wird zum Austausch des Zustandsobjekts. `shutdownRuntime()` bleibt trotzdem verantwortlich für alles, was eine Verbindung überdauert: ON4KST-Supervisor, Sked-Erinnerung, Reachability-Executor, PSTRotator-Wiederholung, Kachel-Proxy, Zeitgeber. `ApplicationConstants.sessionRuntimeUniqueId` wird weiterhin nicht neu erzeugt.

**Abnahmekriterium:** Das Hauptfenster ist funktionsgleich. Der Profilwechsel im laufenden Betrieb funktioniert, ohne Listener doppelt zu registrieren und ohne Ressourcen zu halten.

> Nachzuschärfen nach Etappe 4: ob `Platform.setImplicitExit(false)` und `ApplicationRuntimeLauncher.exitApplication()` in der Compose-Fassung noch eine Entsprechung brauchen.

### Etappe 6 — Karte und Entfernung von JavaFX

**Ziel:** 1.50. Keine JavaFX-Abhängigkeit mehr im Projekt.

**Umfang**

Die Karte wird als Compose-Canvas neu umgesetzt:

- Web-Mercator-Projektion, Zoomstufen und Schwenken wie bisher;
- Kacheln über den bestehenden `TileProxyServer`;
- Marker, Linien, Polygone, Rechtecke und Kreismarker direkt gezeichnet, entsprechend der heutigen flachen Leaflet-Nutzung;
- Maidenhead-Gitter über den bestehenden `MaidenheadGridRenderPlanner`;
- Pfaddarstellung und Geländeprofil (`PathProfileChart`) als Compose-Zeichnung;
- bildschirmbasiertes Clustering als Vorschalt-Schritt vor dem Zeichnen. `GUIstationMapClusteringEnabled` bleibt eine Layout-Einstellung unterhalb `guiOptions` mit Vorgabewert `true`.

`MapHtmlResources`, `StationMapBridge` und die JavaScript-Brücke entfallen ersatzlos. `StationMapView` wird zur Compose-Komponente.

Zum Abschluss werden die JavaFX-Abhängigkeiten aus dem Build entfernt und `opens kst4contest.view.map to javafx.web` verliert seinen Gegenstand.

**Abnahmekriterium:** Die Karte zeigt dieselben Informationen wie die Leaflet-Fassung. Offline-DEM, Terrain-Pakete und der Profil-Cache funktionieren unverändert. Im Projekt existiert keine JavaFX-Abhängigkeit mehr.

> Nachzuschärfen nach Etappe 5: Leistungsverhalten des Canvas bei mehreren tausend Markern. Falls das Zeichnen nicht trägt, ist eine Vorabaggregation der Marker im Kern der Ausweg.

## Invarianten — was unverändert bleiben muss

Diese Punkte sind in `AGENTS.md` und `PROJECT_CONTEXT.md` festgelegt und werden von der Umstellung nicht berührt.

**Chat-Identität**

- Vollständiges sichtbares Rufzeichen plus Kategorie bildet die Identität eines Chat-Teilnehmers.
- Basisruf-Normalisierung nur für explizit basisrufweite Funktionen.
- Worked-Zustand wird über Suffixvarianten desselben Basisrufs geteilt.
- Suffixe werden nicht global als Band, Kategorie oder Frequenz gedeutet.

**Bänder und Verfügbarkeit**

- Kategorien 2 und 3 sind die Hauptkategorien; unerwartete Werte müssen sicher fehlschlagen.
- `NOT-QRV` hat Vorrang vor positiven Band-Hinweisen.
- Fehlende Frequenz-, QRB- oder QTF-Werte bleiben unbekannt. `null` bedeutet nicht verfügbar, nicht null.
- Kein stilles Zurückfallen auf feste 144 MHz.

**Externe Schnittstellen**

- CR/LF-Framing, XML-Framing, Ports und Transporte bleiben unverändert.
- Die lokale DX-Cluster-Ausgabe behält ihre feste 75-Zeichen-Zeile, das DX-Rufzeichen ab Spalte 27, den 30-Zeichen-Kommentar ab Spalte 40 und die UTC-Zeit ab Spalte 71, gefolgt von zwei BEL und CRLF.
- Unerwartete Eingaben beenden keinen Verarbeitungs- oder UI-Thread.

**Persistenz**

- `preferences.xml` bleibt in Version 7, einschließlich der verwalteten Spaltenbreiten unterhalb `guiOptions` und der Regel, dass Elternspalten ihre Breite aus den Blattspalten ableiten.
- Selektives Schreiben aktualisiert die Datei auf der Platte, bewahrt unbekannte XML-Knoten und schreibt keine unbestätigten funktionalen Einstellungen. **Save Settings** bleibt der vollständige Schreiber.
- Vollständige und selektive Schreibvorgänge sind synchronisiert und ersetzen `preferences.xml` atomar.
- Operator-Profile behalten ihr Verhalten: Wurzelprofil flach, weitere Profile unter `profiles/<profileId>/`, `profiles.xml` wird verzögert angelegt, die Registrierung speichert niemals einen Pfad, ein neues Profil bekommt eine **leere** Datenbank.
- Das SQLite-Schema und die Worked-Semantik samt Dreitagesablauf bleiben unverändert.
- Simplelogfile-Verhalten bleibt unverändert, einschließlich des minütlichen Lesens und der Tatsache, dass daraus abgeleiteter Worked-Zustand nicht in SQLite landet.

**Quelltext-Konventionen**

- Kommentare und Javadoc ausschließlich auf Englisch.
- Log-, Protokoll- und API-Literale in ihrer kanonischen Form.
- Kommunikation mit Marc auf Deutsch; Commit-Nachrichten knapp auf Englisch.

## Risiken

| Risiko | Bewertung | Umgang |
|---|---|---|
| `DataTable` wird der Engpass | hoch | Etappe 3 baut sie zuerst und belastet sie in Etappe 4 mit Live-Daten, bevor das Hauptfenster davon abhängt |
| Packaging auf drei Plattformen neu | hoch | Etappe 1 macht das zuerst, mit unveränderter JavaFX-App als Vergleichsmaßstab |
| Canvas-Leistung bei vielen Markern | mittel | Ausweg ist Vorabaggregation im Kern; wird in Etappe 6 gemessen, nicht angenommen |
| Compose-Erfahrung fehlt bei beiden Entwicklern | mittel | Das Einstellungsfenster als erstes Lernstück ist bewusst ungefährlich gewählt |
| Funktionsgleichheit ist schwer nachweisbar | mittel | Vergleich Fenster für Fenster gegen die noch laufende JavaFX-Fassung; Protokoll und Persistenz sind durch `core`-Tests gedeckt |
| 1.50 verschiebt sich erheblich | angenommen | Bewusste Entscheidung. Kein Termin gesetzt |

## Offene Punkte

Diese Fragen werden bewusst erst beantwortet, wenn die vorangehende Etappe Erfahrung geliefert hat. Sie blockieren Etappe 1 und 2 nicht.

1. Genauer Zuschnitt der `DataTable`-Schnittstelle — nach Etappe 2.
2. Ob `Platform.setImplicitExit(false)` in Compose eine Entsprechung braucht — nach Etappe 4.
3. Aggregationsstrategie für Kartenmarker, falls das Canvas-Zeichnen nicht trägt — nach Etappe 5.
4. Ob das Geländeprofil als eigene Compose-Zeichnung oder über eine Diagrammbibliothek entsteht — nach Etappe 5.
