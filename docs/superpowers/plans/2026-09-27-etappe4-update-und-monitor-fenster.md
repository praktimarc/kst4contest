# Umsetzungsplan Etappe 4 — Update-Fenster und Monitor-Fenster

Spezifikation: `docs/superpowers/specs/2026-09-25-compose-migration-design.md`, Etappe 4.

## Was diese Etappe wirklich ist

Die Spezifikation nennt zwei Fenster. Beim Lesen des Quelltextes zeigt sich, dass das
untertreibt:

| Fenster | Stelle | braucht |
|---|---|---|
| Update | `:9458` | ein Textgitter, einen Verweis, **einen Baum** mit Änderungsprotokoll und bekannten Fehlern, Größenspeicherung |
| Monitor | `:9396` | `initDXClusterTable` mit **8 Spalten** über `ClusterMessage`, `initChatToOtherMSGTable` mit **10 Spalten** über `ChatMessage` |

`DataTable` aus Etappe 3b kann **eine** Spalte Text — `ListEntry(key, text)`. Die
mehrspaltige Tabelle existiert nicht. Das Monitor-Fenster ist also nicht der Abnehmer
einer fertigen Komponente, sondern ihr Anlass.

**Etappe 5 braucht dieselbe Komponente** für die Stationsliste mit Filterleiste und
Prioritätenspalten. Was hier entsteht, trägt also drei Tabellen, nicht zwei. Der
Zuschnitt wird danach entschieden, nicht nach dem, was für diese zwei Fenster gerade
reicht — aber auch nicht darüber hinaus: die Stationsliste wird gelesen, bevor die
Schnittstelle festgelegt wird, nicht danach.

## Reihenfolge

Update-Fenster zuerst, wie besprochen. Es ist klein und klärt die Fenster-Mechanik
aus einem Hintergrundfaden — dieselbe Stelle, an der in Etappe 3b der Profilwechsel
scheiterte, weil `closeOwnedStages` ein Compose-Fenster nicht erreichte. Das Monitor-
Fenster folgt mit dieser Gewissheit im Rücken, sonst käme die Fenster-Mechanik als
zweite Fehlerquelle zu den Live-Daten dazu.

## Aufgaben

### 1. `UpdateWindowState` mit Tests — **erledigt**

Umgesetzt in `UpdateWindowState.kt`, neun Testfälle. Beim Lesen kam heraus, dass das
Fenster mehr enthält als die Spezifikation andeutet: neben dem Textgitter steht eine
`TreeView` mit verborgener Wurzel und zwei Zweigen, „ChangeLog" und „Known bugs".
Beide haben **dieselbe Form** — erstes Feld ist die Überschrift, der Rest sind Zeilen —
also trägt ein Typ `UpdateSection` beide. Eine allgemeine Baumkomponente braucht es
nicht: die Tiefe ist fest zwei.

Zwei Entscheidungen sind im Quelltext begründet: fehlende Update-Informationen gelten
als „kein Update" (die Prüfung läuft beim Start, ein gescheiterter Abruf darf den
Start nicht aufhalten), und eine Überschrift ohne Zeilen bleibt erhalten, statt
weggefiltert zu werden.

Die ursprüngliche Beschreibung der Aufgabe:



Reine Fachlogik, ohne Oberfläche: Was wird angezeigt, und **wann überhaupt**. Die
Bedingung steht heute bei `:9583`: `getLatestVersionNumberOnServer() >
APPLICATION_CURRENTVERSIONNUMBER`. Dazu die vier Textfelder aus
`getUpdateInformation()` und der Verweis auf die Veröffentlichungsseite.

Zu prüfen: gleiche Version zeigt nichts, ältere Serverversion zeigt nichts, fehlende
Update-Information führt nicht zu einer Ausnahme beim Start.

### 2. Update-Fenster in Compose — **erledigt und abgenommen**

Titel „Update information", die sechs Beschriftungen wörtlich. Der Verweis öffnet den
Systembrowser. Größe aus `GUIstage_updateStage_SceneSizeHW`.

**Es folgt dem Design zur Laufzeit**, wie `SettingsWindow` seit Etappe 3b, und es
lässt sich von außen schließen. Beides ist keine Kür: ohne das erste steht ein helles
Fenster neben einem dunklen, ohne das zweite überlebt es den Profilwechsel.

**Abgenommen am laufenden Programm**, Tag- und Abenddesign, einschließlich Umschalten
zur Laufzeit über „Windows → Use dark mode design". Damit ist auch die neue
`applyDarkMode`-Verdrahtung des Update-Fensters belegt.

### 3. Den JavaFX-Block entfernen und nachmessen — **erledigt**

`stage_updateStage` samt Feld, `closeOwnedStages`-Eintrag und
`registerThemedScene`-Aufruf. Zeilenzahl von `Kst4ContestApplication.java` festhalten
(Stand jetzt: 10 904).

**Ergebnis: 10 904 → 10 802.** Der Block war 145 Zeilen; zurück kamen 36 für
`openUpdateWindowIfAvailable`, das nur noch die Entscheidung abfragt und den
Systembrowser erreicht.

**Die Fenstermechanik ist herausgezogen.** `ComposeWindowHost` trägt jetzt das, was
jedes Compose-Fenster neben JavaFX braucht: nicht den Prozess beenden, höchstens
einmal öffnen, von außen schließbar sein, dem Design folgen. `SettingsWindow` wurde
darauf umgestellt und behält seine Schnittstelle zur Java-Seite unverändert. Das war
kein Aufräumen um seiner selbst willen: dieselbe Mechanik zum dritten Mal abzuschreiben
hätte bedeutet, die `close()`-Verdrahtung zum dritten Mal von Hand zu treffen — und
genau deren Fehlen war der Profilwechsel-Fehler aus Etappe 3b.

`UpdateWindow.close()` und `applyDarkMode` sind in `closeOwnedStages` und `applyTheme`
eingetragen, nicht nachgereicht.

### 4. Die Stationsliste lesen, **bevor** die Tabellenschnittstelle steht — **erledigt**

**Ergebnis: die drei Tabellen sind ungleich schwer.**

| Tabelle | Zeilen | Zellfabriken | Zeilenfabrik | Vergleicher |
|---|---|---|---|---|
| Monitor: DX-Cluster | 183 | 1 | 0 | 0 |
| Monitor: QSO der anderen | 222 | 1 | 0 | 0 |
| Stationsliste (Etappe 5) | **854** | **13** | **1** | **4** |

Die beiden Monitor-Tabellen sind schlicht: Text in Spalten, keine Zeilenfärbung, keine
eigene Sortierung. Nach ihnen allein zugeschnitten entstünde eine Komponente, die die
Stationsliste nicht trägt — **derselbe Fehler wie bei der `DataTable` in Etappe 3b**.

**Was die Stationsliste zusätzlich verlangt**, aus dem Quelltext gelesen:

- **22 Spalten**: Callsign, Name, QRA, QRB, QTF, QRG, „AP [minutes / pot%]", Tropo,
  Score, Act, worked, wkdany, die Bänder 50/70/144/432/23/13/9/6/3, „NOT QRV @",
  Category.
- **Zellinhalt ist nicht gleich Zellenanzeige.** Eine Zellfabrik liest den Wert *und*
  das Zeilenobjekt *und* Controllerzustand — Beispiel `qraCol`: Kürzen des Textes,
  Kurzhinweis aus Rasterfeld und „schon gearbeitet", Einfärbung nur wenn der
  Raster-Knopf aktiv ist.
- **Zeilenfärbung nach `getCurrentPriorityScore()`** mit vier Schwellen: über 1000 rot
  („imminent sked"), ab 200 orange, ab 100 gelb, 0 und darunter ausgegraut. **Die
  Farben unterscheiden sich nach Design** — im Quelltext steht heute ein `isDark`-Zweig
  je Schwelle.
- **Eigene Vergleicher** für vier Spalten; die übrigen sortieren nach Text.
- **23 Spaltenbreiten** über `TableLayoutManager`, entprellt gespeichert.

**Daraus der Zuschnitt: die Schnittstelle wird für die Stationsliste entworfen, die
Umsetzung nur so weit gebaut, wie Etappe 4 sie braucht.** Eine Spalte ist damit:

- eine feste Kennung (für die Breitenspeicherung),
- eine Überschrift,
- eine Funktion Zeile → angezeigter Text,
- wahlweise ein Kurzhinweis aus (Zeile, Wert),
- wahlweise eine Zellenfärbung aus (Zeile, Wert),
- wahlweise ein Vergleicher.

Die Tabelle nimmt zusätzlich wahlweise eine **Zeilenfärbung** aus der Zeile. So
braucht die Stationsliste in Etappe 5 keinen zweiten Zuschnitt, und die
Monitor-Tabellen benutzen schlicht die einfachen Fälle.

Die ursprüngliche Beschreibung der Aufgabe:


`initChatMemberTable` und die Filterleiste aus Etappe 5 durchsehen und aufschreiben,
was sie verlangt: Spaltentypen, Sortierung, Zeilenfärbung, Auswahlverhalten,
Spaltenbreitenspeicherung. Erst danach die Schnittstelle festlegen.

Begründung: In Etappe 3b wurde die `DataTable` nach dem Einstellungsfenster
zugeschnitten und kann deshalb keine zweite Spalte. Derselbe Fehler ein zweites Mal
wäre vermeidbar gewesen.

### 5. Mehrspaltige Tabelle: Zustand und Regeln, mit Tests — **erledigt**

`DataTableState` mit zehn Testfällen. Die Zeilenidentität war die Kernentscheidung,
und beim Lesen der drei Zeilentypen zeigte sich: **es gibt zwei Sorten Liste, und eine
Antwort reicht nicht.**

| Sorte | Beispiele | Identität |
|---|---|---|
| Entitätsliste | Stationsliste, gearbeitete Stationen | eigener Schlüssel, `RowKeys.byValue` mit dem rohen Rufzeichen |
| Ereignisliste | DX-Cluster, QSO der anderen | `RowKeys.byReference` |

`ClusterMessage` und `ChatMessage` haben **gar keinen Schlüssel**. Einer aus Zeit,
Absender und Text kollidiert, sobald jemand dasselbe Makro zweimal in derselben
Sekunde schickt — genau die Falle „Text lässt Dubletten verschmelzen" aus Etappe 2.
Zwei gleiche Nachrichten *sind* zwei Nachrichten. `byReference` vergibt Schlüssel beim
ersten Sehen und merkt sie sich an der Objektreferenz; das trägt, weil die Roster bei
jedem Schnappschuss dieselben Instanzen zurückgeben.

**Es gibt bewusst keine Vorgabe.** In Etappe 2 kamen die sichtbaren Fehler aus der
Zeilenidentität; der Aufrufer zu zwingen, sie zu benennen, hält die Entscheidung dort,
wo das Wissen sitzt.

Zwei weitere Regeln sind festgeschrieben:

- **Die Auswahl folgt der Zeile, nicht der Position.** Kommt oben etwas dazu, bleibt
  die gewählte Station dieselbe. Verschwindet sie, wird die Auswahl **gelöscht** statt
  auf den Nachbarn zu rutschen — sonst zeigt der Operateur auf jemanden, den er nie
  gewählt hat.
- **Die Sortierung ist stabil.** Gleiche Zeilen behalten ihre Ankunftsreihenfolge. Die
  JavaFX-`SortedList` fügte per Binärsuche ein und legte Gleichstände irgendwohin,
  weshalb Zeilen sprangen.

Die ursprüngliche Beschreibung der Aufgabe:


Ohne Oberfläche prüfbar: Spaltendefinition, Sortierung, Auswahl, Zeilenidentität.

**Die Zeilenidentität ist der kritische Punkt.** In Etappe 2 kamen die Fehler nicht
aus der Tabelle, sondern aus der Kopplung zwischen Roster und Anzeige: ein
Listenschlüssel darf nicht die Position sein (verschiebt sich beim Einfügen oben) und
nicht der Text (fällt bei Dubletten zusammen). Das ist hier erneut zu entscheiden und
festzuhalten.

**Der Zustand hält alles, womit gemalt wird, in Compose-Zustand.** Aus Etappe 3b:
gewöhnliche Eigenschaften melden nichts, und bei Live-Daten fällt das nicht als
„reagiert nicht" auf, sondern als veraltete Anzeige.

### 6. Die Tabelle als Compose-Komponente

Spaltenbreiten werden über die bestehende `TableLayoutManager`-Kennung entprellt
gespeichert. Die Kennungen `dx-cluster-monitor` und `qso-other-monitor` bleiben
getrennt von denen des Hauptfensters, wie `PROJECT_CONTEXT.md` festlegt.

### 7. Monitor-Fenster in Compose

Titel „Cluster & QSO of the other", geteilte Fläche mit den zwei Tabellen, Größe aus
`GUIclusterAndQSOMonStage_SceneSizeHW`. Die Teilerposition wird gespeichert wie
bisher.

### 8. Lasttest mit dem Simulator

Wie in Etappe 2, denn dort hat genau das die Sortier- und Fokusfehler gefunden — kein
Test hat sie gesehen. Eine Stunde Dauerlast. Beobachtet wird: springt die Auswahl,
kippt die Sortierung, bleibt die Anzeige aktuell, wächst der Speicherbedarf.

### 9. Den JavaFX-Monitorblock entfernen und nachmessen

Erst wenn der Lasttest steht.

## Was unverändert bleiben muss

- Layout-Kennungen getrennt von denen des Hauptfensters.
- `GUIclusterAndQSOMonStage_SceneSizeHW` und `GUIstage_updateStage_SceneSizeHW`
  behalten Namen und Bedeutung; `configVersion` bleibt 7.
- Die Update-Prüfung selbst wird nicht angefasst — nur ihre Anzeige.

## Offene Punkte

1. ~~**Die macOS-Systemmenüleiste.**~~ **Geklärt.** Compose kennt `MenuBar()` im
   Geltungsbereich von `Window()`, und auf macOS erscheint sie in der Systemmenüleiste
   am oberen Bildschirmrand, sobald das Fenster aktiv ist — genau das Verhalten, das
   `setUseSystemMenuBar(true)` heute erzeugt. Eine *gemeinsame* Leiste über mehrere
   Fenster gibt es nicht: jedes Fenster erklärt seine eigene.

   Das passt zum heutigen Vorgehen, nur anders herum. `installSharedSystemMenuBar`
   spiegelt die Menüs des Hauptfensters über `Bindings.bindContent` in eine
   unsichtbare Leiste jedes Nebenfensters. In Compose wird daraus eine gemeinsame
   Composable-Funktion, die jedes Fenster aufruft — eine Quelle, fünf Aufrufstellen.

   **Ein Punkt bleibt zu beachten:** die Menüeinträge werden heute aus
   `onConnectionStateChanged` aktiviert und deaktiviert (`menuItemFileDisconnect`,
   `menuItemOptionsAwayBack` und weitere). Deren Zustand muss beobachtbar sein, sonst
   wiederholt sich der Fehler aus Etappe 3b an einer Stelle, an der er besonders
   schlecht auffällt: ein Menüeintrag, der falsch aktiviert aussieht.

   Quelle: [Menu bar, Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform/compose-desktop-menu-bar.html)
2. **Entprellte Spaltenbreiten.** `TableLayoutManager` hängt an JavaFX-`TableColumn`.
   Ob die Compose-Tabelle ihn benutzt oder ein Gegenstück bekommt, entscheidet sich
   mit Aufgabe 4.
