# Umsetzungsplan Etappe 5c, Task 13 — das Hauptfenster zusammensetzen

Task 13 stand im Etappenplan als **ein** Schritt: „das Hauptfenster zusammengesetzt, das
JavaFX-Fenster ersetzt und entfernt". Nach dem Vermessen der Vorlage ist das nicht ein
Schritt, sondern fünf. Dieser Plan sagt, welche, und warum die Reihenfolge so ist.

## Der Baum des JavaFX-Fensters

`start()` reicht von Zeile 7236 bis 9400 — **2164 Zeilen für ein Fenster**. Was darin
steht:

```
Stage (primaryStage)                     Größe aus GUIscn_ChatwindowMainSceneSizeHW,
└─ Scene                                 auf den Schirm begrenzt
   └─ BorderPane bPaneChatWindow
      ├─ top:    FlowPane flwpne_StatusBar                    ✅ Task 10 + 11
      └─ center: SplitPane links (waagerecht, 1 Trenner)
         ├─ SplitPane Nachrichtenteil (senkrecht, 4 Trenner)
         │   ├─ privateMessageTable                           ❌ fehlt
         │   ├─ FlowPane flwPane_textSnippets                  ❌ fehlt
         │   ├─ VBox pnl_inputAndSendButtons → TimelineView     ✅ Task 12
         │   ├─ HBox textInputFlowPane                          ❌ fehlt
         │   └─ TabPane bottomGlobalMessageTabPane              ✅ Etappe 5b
         └─ SplitPane rechts (senkrecht, 2 Trenner)
             ├─ BorderPane chatMemberTableBorderPane            ✅ Etappe 5a
             ├─ topPriorityListPane                             ✅ Etappe 5b
             └─ selectedCallSignFurtherInfoPane                 ✅ Etappe 5b
```

Sechs von zehn Bausteinen stehen. Die vier fehlenden sind nicht die kleinen.

## Was fehlt, mit Zeilen

| Fehlt | Vorlage | Bemerkung |
|---|---|---|
| Tabelle der gerichteten Nachrichten | `initChatprivateMSGTable` ab 3730 | eigener Bereich im Splitter, nicht in `MessageTabs` |
| Knopfreihe der Textschnipsel | `flwPane_textSnippets` 7890, `buttonFactory` 5233, `refreshShortcutButtons` 4541 | Der **Bestand** liegt schon in `ShortcutsTabState`; nur die Knöpfe fehlen |
| Sendezeile | `textInputFlowPane` 7886 | Eingabefeld, `TX`, `Clear`, Trenner, eigene QRG Haupt-/Zweitkategorie, eigener QTF |
| Kontextmenüs | `initChatMemberTableContextMenu` 3132, `insertTextSnippet` 7473 | Stationsliste **und** Nachrichtentabellen; Strg+1…Strg+0 für Schnipsel |
| Fensterrahmen | `getScreenAwareMainSceneSizeHW` 7178, `ensureStageFitsPrimaryScreen` 9396, `buildOperatorProfileTitleSuffix` 6595 | Größe merken und begrenzen, Titel mit Profilzusatz |
| **Splitter mit gemerkten Trennern** | 3 `SplitPane`, 7 Trenner | **Compose hat das nicht** — siehe unten |

## Das eine Stück, das Compose nicht mitbringt

`SplitPane` gibt es in Compose Multiplatform nicht. Sieben Trennerstellungen werden
heute in den Einstellungen gemerkt:

```java
GUImainWindowLeftSplitPane_dividerposition  = {0.51}
GUImainWindowRightSplitPane_dividerposition = {0.53, 0.78}
GUImessageSectionSplitpane_dividerposition  = {0.62, 0.7, 0.75, 0.9}
```

Das ist kein Beiwerk: ein Operator richtet sein Fenster einmal für seinen Schirm ein und
erwartet es beim nächsten Contest genauso. Wir brauchen also eine eigene, gezogene
Trennleiste mit gemerkter Stellung — dasselbe Muster wie die Spaltengriffe aus Etappe 4
(`Density.COLUMN_HANDLE_WIDTH`, Haarlinie in 5 dp Greiffläche), nur senkrecht wie
waagerecht und mit Anteilen statt Pixeln.

`ChatPreferences` liefert die Felder als `double[]`; die Compose-Fassung braucht sie als
Zustand, sonst zeichnet das Ziehen nicht mit. Das ist derselbe Fehler wie in Etappe 3b
und wird gleich als solcher behandelt.

## Aufteilung

### 13a — `SplitterState` und `SplitterPane`

Ein `n`-teiliger Splitter, waagerecht oder senkrecht, mit `n−1` Trennern als
Compose-Zustand, Mindestgrößen pro Bereich und Schreiben nach `ChatPreferences` beim
Loslassen (nicht bei jeder Mausbewegung — die JavaFX-Fassung hat bei jedem Pixel
`requestLayoutSave()` gerufen und dabei auf die Platte geschrieben).

**Tests:** Anteile summieren sich zu 1; ein Trenner schiebt keinen anderen vorbei; eine
Mindestgröße wird eingehalten; ein abgelehntes Ziehen schreibt nichts (das ist der
aufgenommene Kleinmangel aus Etappe 4 bei `resizeColumn`); `Snapshot.observe` auf die
Stellungen; ein Bestand mit zu wenigen gemerkten Werten fällt auf die Vorgaben zurück,
statt zu stürzen.

**Abnahme:** Prüfstand mit fünf Bereichen, Ziehen in beiden Richtungen.

### 13b — Sendezeile und Schnipsel-Knöpfe

Die Zeile, in der der Operator tatsächlich tippt. `ChatInputState` mit dem Text, dem
Ziel (gewählte Station oder Hauptkategorie), den beiden eigenen QRG-Feldern und dem QTF.
Darüber die Knopfreihe aus dem Schnipsel-Bestand, die sich neu baut, wenn die
Einstellungen sie ändern — `ShortcutsTabState` ruft dafür schon
`refreshShortcutButtons`, das heute noch ins Leere zeigt.

**Tests:** Eingabetaste sendet und leert; `Clear` leert ohne zu senden; die Zielauflösung
in der Reihenfolge der Vorlage (gewählte Station → Tabellenauswahl → `ScoreService` →
Hauptkategorie); ein Schnipsel wird an der Einfügemarke eingesetzt, nicht angehängt;
`Snapshot.observe` auf Text und Ziel.

**Risiko:** Das Feld hat in JavaFX den Fokus zurückgeholt (`requestFocus`,
`selectEnd`) — in Compose ist das ein `FocusRequester` und muss nach jedem Senden und
nach jedem Schnipsel wieder greifen. Genau hier hat Etappe 3b Fehler gehabt.

### 13c — Gerichtete Nachrichten und die Kontextmenüs

Die vierte Tabelle, mit `DataTableState` wie die anderen, und die Kontextmenüs, die es
in Compose noch gar nicht gibt: Rechtsklick auf eine Station (Einträge aus dem
gemerkten Bestand), auf eine Nachricht, und Strg+1…Strg+0 für die Schnipsel.

**Tests:** die Spalten; welche Einträge bei welcher Auswahl erscheinen; dass ein
Menüeintrag ohne Auswahl nichts sendet.

### 13d — Der Fensterrahmen

Größe aus den Einstellungen, auf die Arbeitsfläche des Schirms begrenzt (die Vorlage tut
das, weil ein Fenster von einem größeren Monitor sonst unbedienbar aufgeht), Speichern
bei Änderung, Titel aus Chatzustand und Profilzusatz, Schließen über `ComposeWindowHost`.

**Tests:** die Begrenzungsrechnung mit erfundenen Schirmgrößen (das ist reine Rechnung
und gehört nach `core`-Art getestet); der Titel bei gesetztem und fehlendem Profil.

### 13e — Zusammensetzen, ersetzen, entfernen

Erst hier entsteht `MainWindow.kt`, und erst hier fällt der JavaFX-Teil. Reihenfolge:

1. `MainWindow` zusammensetzen, **neben** dem JavaFX-Fenster startbar (beide offen,
   Vergleich am Bildschirm — so sind in Etappe 4 die Unterschiede aufgefallen).
2. Abnahme durch den Operator, mit laufender Verbindung.
3. Erst danach `start()` ausräumen und die Zeilenzahl festhalten.

**Abnahme:** ein Contest-Ablauf von Hand — anmelden, Station wählen, tippen, senden,
Sked setzen, Profil wechseln — ohne dass ein Zuhörer doppelt hängt.

## Reihenfolge und Begründung

13a zuerst, weil ohne Splitter kein Fenster zusammengeht. 13b vor 13c, weil die
Sendezeile das ist, was der Operator die meiste Zeit benutzt; ein Fehler dort wiegt mehr
als eine fehlende Tabelle. 13d ist klein und unabhängig. 13e zuletzt und mit beiden
Fenstern offen.

## Was aus Etappe 4 und 5 mitkommt

- Nach **jedem** sichtbaren Teil starten und hinsehen. Elf Fehler dieser Umstellung sind
  ausschließlich durch Hinsehen aufgefallen, keiner durch einen Test.
- `Snapshot.observe` für jede neue Ansicht.
- Kein Verhalten weglassen, um ein Darstellungsproblem zu umgehen.
- Vor dem Schreiben die Vorlage lesen, nicht nach dem Schreiben. Die falschen
  Zuschreibungen dieser Etappe kamen alle daher, dass ich nach dem Namen geraten habe.

## Offene Entscheidungen

1. **Die vier Nachrichtenfilter** und **„Always on top"** sind aus Etappe 5b nach 5c
   verschoben. Gehören sie in 13b (bei der Sendezeile) oder in 13c (bei den Tabellen)?
   Vorschlag: 13c, weil sie Tabellenfilter sind.
2. Der Kleinmangel `GUIpnl_directedMSGWin_dividerposition` ist verwaist — die Vorlage
   hat nur den `...Default`-Wert. In 13a wegwerfen oder stehen lassen?
   Vorschlag: wegwerfen, mit einem Satz in der Einbuchung.
3. **Der dritte Sortierklick** (Sortierung löschen) fehlt seit Etappe 4. In 13c
   mitnehmen oder eigene Kleinigkeit?
