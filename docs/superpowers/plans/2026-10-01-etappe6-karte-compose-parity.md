# Umsetzungsplan Etappe 6 — Karte: Angleichung an die Leaflet-Fassung

Spezifikation: `docs/superpowers/specs/2026-09-25-compose-migration-design.md`, Etappe 6.

## Was dieser Plan ist — und was nicht

Etappe 6 hat zwei Hälften: die Karte nach Compose bringen, und JavaFX aus dem Build
werfen. Die erste Hälfte wurde bereits angefangen (im Übergabeprotokoll „Task 18/19"),
und das Ergebnis trägt nicht. Dieser Plan ist die **Nachbesserung dieser ersten
Hälfte**. Das Entfernen von JavaFX und der dafür nötige Ersatz von
`JavaFxStylesheet.derive` bleiben ausdrücklich außen vor.

Der Anlass ist nicht Geschmack. Die Compose-Karte bildet mehrere Mechanismen der
Leaflet-Fassung **gar nicht** ab, nicht bloß anders. Drei davon liegen fertig und
getestet in `core` und werden nicht aufgerufen.

## Beweislage

Verglichen wurde `github_docs/station_map_compact.png` und
`github_docs/station_map_path_analysis.png` (JavaFX) gegen den Stand vom 30.09.
(`scratch/screenshots/AAA011.png`, `AAA012.png`).

| # | Befund | Stelle |
|---|---|---|
| 1 | `MaidenheadGridRenderPlanner` wird nicht aufgerufen — jede Zelle **und jedes Label** wird gezeichnet | `ComposeStationMap.kt:286` gegen `StationMapView.java:1301` |
| 2 | Gitterlabel in der Ecke statt in der Zellenmitte, weiß statt `#63067a` | `ComposeStationMap.kt:307` gegen `MapHtmlResources.java:302` |
| 3 | Gebrochener Zoom mit `scale()` über die ganze Zeichenfläche; alle Größen werden mit `/ scaleFactor` gegengerechnet | `ComposeStationMap.kt:243` |
| 4 | Clustering: 50 px fest, ab 2 Stationen, immer an — statt 55/70/95/125 px je Zoom, ab 3 Stationen, aus ab Zoom 8 | `ComposeStationMap.kt:68` gegen `MapHtmlResources.java:393-410`, `:758` |
| 5 | Kopfzeile: nur „Selected: X" statt der sechs Felder; rohe Material-3-Knöpfe statt `Fields.button` | `StationMapWindow.kt:159` gegen `StationMapView.java:571`, `:1010` |
| 6 | Zoomknöpfe und OSM-Attribution fehlen | — |
| 7 | Kein Kartenhintergrund; fehlende Kacheln zeigen Systemgrau | gegen `MapHtmlResources.java:57`, `:70` |
| 8 | `TileFetcher`: abgekündigte a/b/c-Subdomains, nur RAM-Cache, Fehler werden still verschluckt | `TileFetcher.java:44` |
| 9 | Absturz „maxWidth must be >= than minWidth", übertüncht durch eine Sperre, die das Fenster unter 500×400 dp leer lässt | `StationMapWindow.kt:151`, `:329` |

**Der dritte Befund ist die Wurzel von mehreren anderen.** Weil die gesamte
Zeichenfläche skaliert wird, hängen Punktradius, Strichstärke und Schriftgröße am
Zoom-Bruchteil. In der Leaflet-Fassung war ein Marker **immer** 12 px und ein Label
**immer** 12 px, weil Leaflet Overlays in Bildschirmkoordinaten zeichnet. Solange der
`scale()`-Block steht, ist jede Farb- und Größenkorrektur nur an einer Zoomstufe
richtig — und genau das erklärt, warum mehrere Anläufe nacheinander „fast richtig"
aussahen.

## Getroffene Entscheidungen

| Frage | Entscheidung |
|---|---|
| Zielbild | Nah genug an Leaflet, eigene Handschrift in Details erlaubt — gleiche Informationsdichte und Bedienlogik sind Pflicht |
| Zoom | Weich zoomen, aber **Overlay in Bildschirmkoordinaten**. Nicht der ganzzahlige Zoom der JavaFX-Fassung |
| Absturz | Zuerst reproduzieren und beheben, dann umbauen |
| Kacheln | `TileFetcher` reparieren und um einen Plattencache erweitern |

**Abweichung von der Spezifikation, bewusst.** Die Spezifikation schreibt „Kacheln
über den bestehenden `TileProxyServer`". Der Proxy existiert, weil die JavaFX-WebView
in AppImage- und Flatpak-Verpackung an TLS scheiterte — ein Compose-Canvas holt die
Kacheln selbst und hat dieses Problem nicht. Der Proxy bleibt für den JavaFX-Pfad
stehen, bis dieser entfällt.

**Der ganzzahlige Zoom war ebenfalls ein WebView-Zugeständnis**, nicht Absicht:
`MapHtmlResources.java:884` begründet `zoomSnap: 1` mit unzuverlässiger
Markerpositionierung in der JavaFX-WebView. Der Grund entfällt mit der WebView.

## Reihenfolge

Erst der Absturz, dann die Zeichenebene, dann die Fachregeln, zuletzt die Hülle.
Umgekehrt müsste jeder Schritt gegen einen Fehler arbeiten, der ihn jederzeit
abbrechen kann.

## Aufgaben

### 1. Absturz beheben, Sperre entfernen

`StationMapWindow.kt:328-330` legt in **eine** `Row` ein `Text` mit fester
`Modifier.width(130.dp)` und daneben eine `Box(Modifier.weight(1f))`. Wird das rechte
Pane schmaler als diese 130 dp samt Innenabstand, bleibt für das gewichtete Kind eine
**negative** Restbreite — und daraus baut Compose die `Constraints`, die mit
„maxWidth must be >= than minWidth, maxHeight must be >= than minHeight, minWidth and
minHeight must be >= 0" abbrechen.

Dazu fehlt dem Detailbereich die Mindestbreite, die JavaFX hatte:
`detailScrollPane.setMinWidth(210)` und `setPrefWidth(350)`, `StationMapView.java:468`.

- Reproduzieren: Teiler ganz nach rechts ziehen.
- Feste Breite durch `weight` ersetzen, wie es `DetailRow` bereits macht.
- Mindestbreite des Detailbereichs im `SplitterState` verankern, nicht im Fenster.
- **Erst danach** die Sperre `BoxWithConstraints { if (maxWidth < 500.dp …) return@… }`
  in Zeile 151 entfernen.

**Abnahme:** Der Teiler lässt sich über die gesamte Breite ziehen, ohne dass ein
Dialog erscheint, und das Fenster bleibt bei jeder Größe gezeichnet.

### 2. Zeichenebene: Welt- gegen Bildschirmkoordinaten trennen

Eine Projektionshilfe (Zentrum, Zoom als `Float`, Canvasgröße) rechnet den gebrochenen
Zoom über `2^zoom` direkt ein, statt mit `floor(zoom)` zu rechnen und die Zeichenfläche
hinterher zu skalieren.

- Nur die Kachelschicht wird skaliert gezeichnet: Kacheln der Stufe `floor(zoom)`,
  Zielgröße über den Skalierungsfaktor.
- Gitter, Marker, Labels, Antennenkeule, Pfadlinie und Kurzhinweis zeichnen
  **unskaliert in Bildschirmpixeln**.
- `scale()` und sämtliche `/ scaleFactor` entfallen.
- Kartenhintergrund `#ede9df` hell, `#23282d` dunkel als erste Zeichenoperation.
- Zoombereich 3–18 wie Leaflet; Compose begrenzt heute auf 4–18.

**Abnahme:** Ein Marker behält seine Größe über den gesamten Zoombereich. Bei
abgeschalteten Kacheln ist die Fläche in der Kartenfarbe, nicht in Systemgrau.

### 3. Gitter über den vorhandenen Planer

`MaidenheadGridRenderPlanner.createPlan(floor(zoom), bbox, canvasW, canvasH)` aufrufen,
Zellen über `buildVisibleCells(…, plan.precision())` holen.

- Label nur bei `plan.shouldShowLabel(cell)`, Schriftgröße aus `plan.labelFontSizePx()`.
- Label **mittig** in der Zelle, in `#63067a` auf halbdurchsichtigem Kästchen.
- Linien `#e1e7ec` bei 48 % dunkel, `#46586c` bei 56 % hell, Strichstärke 1.4.

Der Planer ist vorhanden und wird nicht geändert.

**Abnahme:** Bei weitem Zoom Zweistellen-Felder, bei mittlerem die vierstelligen
Quadrate wie in `station_map_compact.png`; keine Labelteppiche.

### 4. Clustering nach den Leaflet-Regeln

`StationMapClusterer` in `core` vom Nächster-Nachbar-Verfahren auf das Bildschirmraster
der Leaflet-Fassung umstellen:

- Zellgröße 55 px ab Zoom 7, 70 px ab 6, 95 px ab 5, darunter 125 px.
- Bucket über `floor(bildschirmX / zellgröße)`.
- Cluster **erst ab 3** Stationen; darunter werden die Stationen einzeln gezeichnet.
- Clustering **aus ab Zoom ≥ 8**.
- Ausgewählte Stationen und solche mit `warningToMyDirection` werden **nie** geclustert.
- Blasendurchmesser 32 / 36 / 42 px ab 1 / 8 / 20 Stationen.

Das sind Fachregeln, keine Zeichenregeln — sie gehören mit Tests nach `core`. Die
Prüfung der Konstanten gegen `MapHtmlResources.java:393-410` und `:758` ist Teil der
Aufgabe.

**Abnahme:** Tests in `core` decken die vier Zellgrößen, die Schwelle von drei
Stationen, die Abschaltung ab Zoom 8 und beide Ausnahmen ab.

### 5. Kopfzeile und Bedienelemente

- Statuszeile mit dem vollen Text aus `updateStatusLabel()`
  (`StationMapView.java:1010`): `Showing N visible stations`, optional
  `| filtered view active`, `| Selected: …`, Locator, `km / °`, Bänder, QRG.
- Statuszeile mit `Modifier.weight(1f)`, damit die Knöpfe rechtsbündig stehen.
- `Fields.button` (`FormControls.kt:367`) und `CompactCheckbox` (`:556`) statt roher
  Material-3-Bedienelemente. Die grünen Pillen entstehen, weil `Theme.kt:97`
  `primary` auf den Grünton des Abend-Stylesheets legt und Material `Button` genau
  diese Rolle als Flächenfarbe nimmt.
- Zoomknöpfe oben links als Überlagerung.
- OSM-Attribution unten rechts. Die ist bei OSM-Kacheln lizenzrechtlich nicht
  freigestellt.

**Abnahme:** Die Kopfzeile zeigt dieselben Angaben wie `station_map_compact.png` und
sieht aus wie die übrigen Compose-Fenster.

### 6. Kacheln

- Host `tile.openstreetmap.org`; die Subdomains `a`/`b`/`c` sind abgekündigt.
- Plattencache im Profilverzeichnis, nicht im Arbeitsverzeichnis.
- Fehler nach außen melden, damit die Karte „Kacheln nicht erreichbar" anzeigen kann,
  statt wie in `AAA012.png` kommentarlos grau zu bleiben.
- `User-Agent` bleibt wie bisher; die OSM-Nutzungsbedingungen verlangen einen
  sprechenden Wert.

**Abnahme:** Nach einem Neustart ohne Netz zeigt die Karte die zuletzt geholten
Kacheln aus dem Plattencache und einen Hinweis für die fehlenden.

## Was unverändert bleibt

- Der **gesamte JavaFX-Pfad**: `StationMapView.java`, `MapHtmlResources.java`,
  `StationMapBridge.java`, `TileProxyServer.java`. Sie sind bis zum Entfernen von
  JavaFX die Referenz.
- Die Fachlogik in `core` wird **benutzt, nicht geändert**: `PathAnalysis*`,
  `Terrain*`, `PathGeometryUtils`, `MapCallsignRawSnapshotBuilder`,
  `MaidenheadGridUtils`, `MaidenheadGridRenderPlanner`.
- `GUIstationMapClusteringEnabled` bleibt eine Layout-Einstellung unterhalb
  `guiOptions` mit Vorgabewert `true`.
- Zurücksetzen der Ansicht lässt den Zoom in Ruhe und löscht nur das Ziel (AGENTS.md).
- Die `/cq`-Vorbelegung bei Stationsauswahl.
- Der Zoom auf die Bounding Box zwischen eigenem Standort und Zielstation.

## Abnahmekriterium der Etappe

Die Karte zeigt dieselben Informationen wie die Leaflet-Fassung. Offline-DEM,
Terrain-Pakete und der Profil-Cache funktionieren unverändert.

Das zweite Kriterium der Spezifikation — keine JavaFX-Abhängigkeit mehr im Projekt —
gehört zur zweiten Hälfte der Etappe und ist hier **nicht** erfüllt.

## Offene Punkte

- Leistungsverhalten des Canvas bei mehreren tausend Markern. Die Spezifikation nennt
  als Ausweg eine Vorabaggregation im Kern; Schritt 4 ist genau diese Vorstufe und
  sollte vor einer Messung fertig sein.
- `JavaFxStylesheet.derive` ruft `com.sun.javafx.util.Utils.deriveColor` über
  Reflexion auf. Das muss **vor** dem Entfernen von JavaFX ersetzt werden, sonst
  verschieben sich alle abgeleiteten Farben still. Nicht Teil dieses Plans, aber Teil
  dieser Etappe.
