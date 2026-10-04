# Umsetzungsplan Etappe 5 — Hauptfenster

Spezifikation: `docs/superpowers/specs/2026-09-25-compose-migration-design.md`, Etappe 5.

## Der Umfang, gemessen

| Teil | Zeilen | Besonderheit |
|---|---|---|
| Stationsliste `initChatMemberTable` | 854 | 22 Spalten, 13 Zellfabriken, 1 Zeilenfabrik, 4 Vergleicher |
| Filterleiste | ~977 | 37 Bedienelemente, davon **25 Umschaltknöpfe** |
| Infobereich `generateFurtherInfoAbtSelectedCallsignBP` | 881 | eigener Teiler mit gespeicherter Position |
| Menüleiste (File, Options, Windows, Help) | ~343 | macOS-Systemmenüleiste |
| `TimelineView` | 376 | eigene Datei, eigene Zeichnung |
| Top-Prioritätenliste | 69 | |
| Statusanzeigen | ~40 | Sked-Warnung, Verbindung, Band-Upgrade |

Über **3500 Zeilen**. Etappe 3 war kleiner und wurde geteilt.

**Der Infobereich ist so groß wie die Stationsliste.** In der Spezifikation steht er
als einer von sieben Aufzählungspunkten; das untertreibt ihn um eine Größenordnung.

## Aufteilung in 5a, 5b und 5c

Nicht aus Ordnungsliebe, sondern weil eine Etappe lieferbar sein soll. Nach jedem
Abschnitt läuft die Anwendung, und du kannst abnehmen, bevor der nächste anfängt.

### 5a — Stationsliste und Filterleiste

Der Kern. Die mehrspaltige Tabelle aus Etappe 4 wurde **für diese Liste** zugeschnitten
und trägt hier ihre erste echte Last: 22 Spalten, Zellfärbung aus Zeile *und* Wert,
Zeilenfärbung nach `getCurrentPriorityScore()` über vier Schwellen, eigene Vergleicher,
23 gespeicherte Spaltenbreiten.

Die 25 Umschaltknöpfe der Filterleiste sind kein Formular, sondern Zustand mit Regeln —
welche Filter einander ausschließen, welche sich stapeln. Die Regeln gehören in einen
testbaren Zustand, bevor ein Knopf gezeichnet wird.

**Abnahme:** Die Liste zeigt dieselben Stationen in derselben Reihenfolge wie JavaFX,
die Filter wirken gleich, Spaltenbreiten überleben den Neustart.

### 5b — Infobereich, Top-Prioritätenliste, Chat-Reiter

Der Infobereich hängt an der Auswahl der Liste aus 5a — deshalb danach und nicht
davor. Sein Teiler wird hier einmal richtig gebaut; im Monitor-Fenster habe ich ihn
bewusst weggelassen, statt ihn zweimal halb zu bauen.

**Abnahme:** Auswahl in der Liste füllt den Infobereich wie bisher, Teilerpositionen
werden entprellt gespeichert.

### 5c — Menüleiste, Statusleiste, TimelineView, Profilwechsel

Die Menüleiste wird eine gemeinsame Composable-Funktion, die jedes Fenster aufruft —
so verlangt es Compose, und so bekommen die Compose-Fenster auf macOS ihre
Systemmenüleiste zurück, die sie seit Etappe 3b nicht haben.

**Der Profilwechsel ändert seine Natur.** Heute reißt er die Laufzeit ab und baut eine
neue `Kst4ContestApplication` auf, weil viele Bedienelemente inline erzeugte
Instanzfelder sind. Dieser Grund entfällt; der Wechsel wird zum Austausch des
Zustandsobjekts. `shutdownRuntime()` bleibt trotzdem verantwortlich für alles, was eine
Verbindung überdauert.

**Abnahme:** Profilwechsel im laufenden Betrieb, ohne doppelt angemeldete Zuhörer und
ohne gehaltene Ressourcen. Auf macOS erscheint die Menüleiste bei jedem Fenster.

## Was aus Etappe 4 mitgenommen wird

Vier Fehler dieser Etappe kamen **ausschließlich** durch Hinsehen ans Licht, keiner
durch einen Test, und der Prüfer fand nur einen Teil davon:

1. Ein Zwischenspeicher, der den Zustand nicht mehr las, und eine Tabelle, die deshalb
   nie wieder zeichnete.
2. Eine Kopfzeile, die die ganze Tabellenhöhe nahm.
3. Eine Liste, die bei jeder neuen Zeile vom Kopf wegdriftete.
4. Ein Menüeintrag, der seinen Umschalter verloren hatte.

Daraus drei Regeln für diese Etappe:

- **Was gezeichnet wird, muss beim Lesen Zustand berühren.** Der `Snapshot.observe`-Test
  aus Etappe 4 fängt genau das und wird hier für jede neue Ansicht angelegt.
- **Nach jedem sichtbaren Teil wird gestartet und hingesehen**, nicht erst am Ende des
  Abschnitts.
- **Kein Verhalten wird weggelassen, um ein Darstellungsproblem zu umgehen.** Der
  verlorene Umschalter war genau dieser Tausch.

## Offene Punkte

1. Ob `Platform.setImplicitExit(false)` und `ApplicationRuntimeLauncher.exitApplication()`
   in der Compose-Fassung noch eine Entsprechung brauchen — laut Spezifikation nach
   Etappe 4 zu klären, also jetzt.
2. Ob die Filterleiste ihre 25 Knöpfe behält oder ob mehrere davon derselbe Zustand
   sind. Vor dem Zeichnen zu lesen, nicht danach.
