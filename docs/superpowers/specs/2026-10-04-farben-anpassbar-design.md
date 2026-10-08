# Entwurf — die Farben pro Operateurprofil anpassbar machen

Vertieft: `docs/superpowers/specs/2026-09-25-compose-migration-design.md`, Etappe 8.

Dieser Entwurf ersetzt den dortigen Abschnitt. Er ist im September geschrieben worden, und
zwei seiner Annahmen stimmen nicht mehr.

## Ziel

Der Operateur bestimmt die Farben seines Clients, ohne Dateien im Installationsverzeichnis
anfassen zu müssen. Erfolg ist nachprüfbar:

- Eine geänderte Palette überlebt den Neustart.
- Sie gilt in **allen offenen Fenstern gleichzeitig**, nicht erst nach einem Fensterneubau.
- Sie ist **pro Profil** verschieden.
- Das Zurücksetzen stellt **exakt** die ausgelieferten Farben her — nicht etwas, das ihnen
  gleicht.

## Was sich gegenüber dem Ursprungsabschnitt geändert hat

**Die Voraussetzung ist entfallen.** Er schreibt: „Ab Etappe 6 gibt es kein JavaFX mehr, das
dagegenhält — vorher muss beides dieselbe Datei lesen." Es gibt kein JavaFX mehr. Und der
`derive`-Pin, auf den er ausdrücklich wartet, ist seit Etappe 3 eine JavaFX-freie Umsetzung,
gegen eingecheckte Referenzwerte geprüft. Diese Etappe ist frei.

**Ein Befund kommt hinzu, den er nicht hatte.** `Kst4ContestTheme` liest die Palette als
`remember(darkMode)` (`Theme.kt:32`). Eine Palettenänderung erreicht die Fenster damit **gar
nicht**, bis jemand Tag/Abend umschaltet. Das Abnahmekriterium „gilt für alle Fenster
gleichzeitig" hängt also nicht an der Speicherung, sondern hier. Der Weg dafür existiert
bereits: `applyTheme` (`Kst4ContestApplication`) ruft `applyDarkMode` auf sechs Fenster-Wirten,
jeder hält einen `mutableStateOf` — nur trägt der heute bloß ein Boolean.

**Der Defektbefund bleibt richtig.** `copyResourceIfRequired` legt beim Start Kopien von
`KST4ContestDefaultDay.css` und `KST4ContestDefaultEvening.css` nach
`~/.kst4contest/` (`Kst4ContestApplication:2138`–`:2139`). Geprüft: **nichts liest sie.** Wer
seine Kopie bearbeitet, sieht keinerlei Wirkung.

## Die Quelle der Farben: drei Schichten

| | Schicht | Woher | Vorrang |
|---|---|---|---|
1 | **Auslieferung** | die Vorlage aus dem Klassenpfad | Boden, immer vorhanden |
2 | **Datei** | die CSS-Datei im Profilverzeichnis, falls vorhanden | überschreibt 1 |
3 | **Geändert** | die sechs Rollen aus den Einstellungen des Profils | überschreibt 1 und 2 |

**Die spätere Schicht gewinnt, Rolle für Rolle.** Eine Datei, die nur `-fx-base` nennt, ändert
nur diese Rolle; die übrigen kommen weiter aus der Auslieferung.

### Warum drei und nicht eine

Abgewogen wurden zwei Wege.

**Nur die CSS-Datei maßgeblich**, wie der Ursprungsabschnitt es wörtlich sagt, wurde
verworfen: eine Farbänderung müsste `-fx-base` in die 6,4-KB-Vorlage zurückschreiben, und das
braucht einen Serialisierer, der jede übrige Regel unverändert durchträgt. Ein Rundlauf, der
eine Regel verliert, zerstört das Aussehen still. `JavaFxStylesheet` ist ein Leser; ein
Schreiber daneben wäre die risikoreichste Stelle dieser Etappe, und die Anforderung — „ohne
Dateien anfassen zu müssen" — verlangt ihn nicht.

**Nur die Einstellungen** wäre das Einfachste und wurde bewusst nicht gewählt: es nimmt dem
Operateur den Handweg, den er heute zu haben *glaubt* (die Kopie liegt ja da), und der
Defektbefund würde gelöscht statt behoben.

**Gewählt: beide, mit ausdrücklichem Vorrang.** Der Preis ist benannt und wird im nächsten
Abschnitt bezahlt: bei zwei Quellen ist später nicht mehr ersichtlich, warum eine Farbe so
ist, wie sie ist.

### Was diesen Preis bezahlt

**Jede der sechs Rollen zeigt in der Oberfläche, woher ihr geltender Wert kommt** —
*Auslieferung*, *Datei* oder *geändert*. Die Auflösung weiß das ohnehin, es kostet nichts, und
ohne das ist die Frage „warum ist diese Farbe so?" unbeantwortbar. Das ist keine Zierde,
sondern die Bedingung, unter der die Dreischicht-Lösung vertretbar ist.

## Die sechs Rollen

Die, die `JavaFxPalette` benennt. Der Ursprungsabschnitt zählt fünf auf; es sind sechs,
weil `base` und `windowBackground` getrennt geführt werden — siehe unter der Tabelle.

| Rolle | im Palettenobjekt |
|---|---|
Fläche | `base` |
Fensterfläche | `windowBackground` |
Feldinneres | `controlInnerBackground` |
Text | `labelTextFill` |
Akzent | `accent` |
Trennlinie | `separatorLine` |

`base` und `windowBackground` sind getrennt, weil die Vorlagen sie getrennt führen und die
Fensterfläche aus der Fläche abgeleitet wird (`derive(base, 26.4%)`) — wer nur eine davon
setzen könnte, bekäme die andere ungefragt mitgezogen.

**Nicht anpassbar** bleiben `textAccent`, `buttonHoverGradient` und `buttonPressedBorder`. Sie
sind abgeleitete Werte, keine Rollen, die jemand einzeln wählen will; sie folgen dem Akzent.
`fontSizePx` ist eine bestehende Einstellung und wird hier nicht angefasst.

**Tag und Abend sind zwei Paletten.** Jede Rolle existiert zweimal, und eine Änderung am
Abendentwurf lässt den Tagentwurf unberührt.

## Die Wirkung ist sofort, und es gibt drei Wege zurück

**Eine Änderung gilt unmittelbar in allen offenen Fenstern.** Kein Musterfeld, kein
„noch nicht gespeichert"-Zustand.

Der Grund gegen ein Musterfeld ist die Geschichte dieses Projekts: die Fehlerklassen, die hier
wiederholt mit grünem Bau auslieferten, waren „sah einzeln richtig aus und war an seinem Platz
falsch". Ein zweihundert Pixel breites Muster ist genau diese Isolation. Es sagt nichts
darüber, wie die Stationsliste mit 22 Spalten in einer Palette liest. Das Einstellungsfenster
selbst ist die bessere Vorschau: es färbt sich mit und enthält Beschriftungen, Felder, Knöpfe
und Trennlinien — alles, was die sechs Rollen betrifft.

Weil es keinen Zwischenzustand gibt, sind die Rückwege keine Bequemlichkeit, sondern der
Ersatz dafür:

- **„Zurück zum Stand von vorher"** — beim Öffnen des Reiters wird die geltende Palette
  gemerkt; dieser Knopf stellt sie wieder her. Das ist der Rückweg für den Normalfall: eine
  Farbe im echten Fenster ausprobiert, gefällt nicht, weg damit.
- **„Meine Änderungen verwerfen"** — leert Schicht 3. Es gilt wieder Datei oder Auslieferung,
  je nachdem, was da ist.
- **„Auf Auslieferung zurücksetzen"** — setzt alle Rollen auf die ausgelieferten Werte, als
  ausdrückliche Änderung der Schicht 3. Damit spielt eine vorhandene Datei keine Rolle mehr.
  **Das ist die Aktion, die das Abnahmekriterium erfüllt**, und sie erfüllt es auch dann, wenn
  eine Datei im Spiel ist.

Mit einer Datei zwischen Auslieferung und Änderung kann **ein** Knopf nicht alle drei Fälle
abdecken: löscht er nur die Änderungen, gilt wieder die Datei und nicht die Auslieferung.

### „Auf Auslieferung zurücksetzen" wird immer in den ausgelieferten Farben gezeichnet

**Dieser eine Knopf nimmt die aktive Palette nicht an.** Er wird in den ausgelieferten Farben
gezeichnet, ganz gleich, was der Operateur eingestellt hat — Fläche, Text und Rahmen aus
Schicht 1, nicht aus der Auflösung.

Der Grund ist die Fehlerart, die sich sonst selbst einschließt: wer Text und Fläche auf
dasselbe Schwarz stellt, sieht die Knöpfe nicht mehr, mit denen er es zurücknehmen könnte. Ein
Tastaturweg allein hilft dann nur, wer ihn kennt. Ein Knopf, der die kaputte Palette nicht
annimmt, hilft immer.

Dass er dadurch bei einer stark angepassten Palette **fremd aussieht, ist beabsichtigt.** Er ist
eine Rettungsleine, keine Zierde; dass er sich nicht einfügt, macht ihn auffindbar.

Die anderen zwei Rückwege nehmen die aktive Palette an. Sie sind Bequemlichkeit für den
Normalfall; die Rettungsleine ist diese eine, und genau eine ist leichter zu merken als drei.

Ein Tastaturweg kommt zusätzlich, für alle drei. Er ist jetzt die zweite Absicherung, nicht
mehr die einzige.

## Die Datei im Profilverzeichnis

**Für das Wurzelprofil existiert sie schon:** `~/.kst4contest/KST4ContestDefaultDay.css` und
`…Evening.css`, von `copyResourceIfRequired` geschrieben. Sie wird jetzt **gelesen**. Das
behebt den dokumentierten Defekt, statt seine Spuren zu beseitigen, und gibt dem Operateur den
Handweg, den die Datei ihm die ganze Zeit versprochen hat.

**Für andere Profile wird nicht ungefragt kopiert.** Der Pfad folgt dem Muster, das
`OperatorProfilePaths` für Einstellungen und Datenbank schon benutzt: Wurzelprofil flach, sonst
`profiles/<id>/<datei>`. Eine Datei, die ungefragt entsteht und niemand braucht, ist dieselbe
Falle wie die bisherige ungelesene Kopie. Stattdessen schreibt ein Knopf im Reiter die Vorlage
ins Profilverzeichnis, wenn der Operateur von Hand bearbeiten will.

**Eine unlesbare oder fehlerhafte Datei darf den Start nicht verhindern.** Sie wird
übersprungen, die Rolle fällt auf die Auslieferung zurück, und der Reiter sagt es. Ein Client,
der wegen einer kaputten Farbdatei nicht startet, ist schlimmer als einer in den falschen
Farben.

## Dass es alle Fenster erreicht

Ein **`PaletteStore`** hält die Palette beider Entwürfe für das aktive Profil als
Compose-Zustand. `Kst4ContestTheme` liest daraus statt aus `remember(darkMode)`. Damit zeichnet
eine Änderung jedes offene Fenster neu — über denselben Weg, den der Tag/Abend-Wechsel schon
benutzt, und ohne dass `applyTheme` oder die sechs Fenster-Wirte etwas Neues lernen müssen.

Der Store wird beim Profilwechsel neu gebildet, wie jeder andere profilgebundene Zustand.

## Der Kontrastwächter

Eine frei gewählte Textfarbe auf einer frei gewählten Fläche kann unlesbar werden. Die
Oberfläche **sagt es, speichert aber trotzdem**: ein Funker, der im Contest bewusst eine harte
Kombination will, bekommt sie. Das Maß ist das WCAG-Kontrastverhältnis aus relativer
Leuchtdichte — eine reine Funktion, und damit der Teil, der falsch sein kann, ohne dass es
auffällt.

Geprüft werden die Paare, die tatsächlich übereinanderliegen: Text auf Fläche, Text auf
Feldinnerem, und Akzent auf Fläche.

## Prüfung

Das Wertvolle hier sind die Regeln, nicht die Oberfläche. Alle ohne Bildschirm prüfbar:

- **Die Dreischicht-Auflösung** samt Vorrang und Herkunftsangabe, einschließlich: Datei fehlt;
  Datei nennt nur eine Rolle; Datei ist unlesbar; Änderung und Datei widersprechen sich.
- **Die drei Rückwege** — besonders, dass „auf Auslieferung" auch bei vorhandener Datei exakt
  die ausgelieferten Werte ergibt, und dass „zum Stand von vorher" die Palette herstellt, die
  beim Öffnen des Reiters galt, einschließlich einer damals schon vorhandenen Änderung.
- **Dass der Auslieferungs-Knopf die ausgelieferten Farben benutzt und nicht die aufgelöste
  Palette** — geprüft mit einer Palette, in der jede Rolle auf etwas anderes gesetzt ist. Das
  ist der Test, der die Rettungsleine davor bewahrt, still an der Palette zu hängen, die sie
  retten soll.
- **Das Kontrastverhältnis**, gegen von Hand nachgerechnete Werte.
- **Dass Tag und Abend getrennt bleiben.**
- Dazu die Rückfalltests aus Etappe 7: Klasse 4 prüft, dass die Farben aus der Stilvorlage
  kommen, und muss mit dieser Etappe weiter gelten.

## Was unverändert bleibt

- Die ausgelieferten Vorlagen selbst.
- `JavaFxStylesheet` als **Leser**. Kein CSS-Schreiber, nirgends.
- Der `derive`-Pin samt eingecheckter Referenztabelle und `JavaFxStylesheetTest`.
- Die Speicherung pro Profil über `ChatPreferences` — dieselbe wie jede andere Einstellung.
- `fontSizePx` und die bestehende Schriftgrößeneinstellung.
- Die Invarianten aus `AGENTS.md`.

## Risiken

**Die zweite Quelle ist der Preis dieser Etappe.** Zwei Schichten, die dasselbe bestimmen
können, sind eine Stelle, an der später niemand weiß, warum eine Farbe so ist. Die
Herkunftsangabe pro Rolle ist die einzige Gegenmaßnahme, und sie ist deshalb nicht optional.

**Eine Änderung, die alle Fenster erreicht, erreicht sie auch alle falsch.** Der `PaletteStore`
ist eine Stelle, an der ein Fehler sich überall gleichzeitig zeigt. Dafür zeigt er sich
wenigstens sofort und nicht erst nach einem Fensterneubau.

**Der Kontrastwächter warnt nur.** Ein Operateur kann sich einen unlesbaren Client bauen. Das
ist die Entscheidung, die hier bewusst bei ihm liegt, und der Ausweg ist der Knopf, der die
kaputte Palette nicht annimmt.

**Die Warnung des Wächters hat dieselbe Angreifbarkeit wie die Knöpfe** und ist nicht
ausgenommen: in einer unlesbaren Palette ist auch sie unlesbar. Sie auszunehmen wäre
folgerichtig, würde aber den Reiter mit zwei fremd aussehenden Flächen besetzen statt mit
einer. Ich lasse es bei der einen Rettungsleine und halte den Punkt hier fest, statt ihn
stillschweigend auszuweiten.

## Zwei Festlegungen, die sonst im Bau beliebig würden

**Die Wirkung ist sofort, der Rückweg gemerkt.** Kein Musterfeld — die Begründung steht unter
„Die Wirkung ist sofort". Beurteilt wird im echten Fenster, zurück geht es über eine beim
Öffnen des Reiters gemerkte Palette.

**Alle drei Rückwege bleiben.** Ohne Zwischenzustand sind sie der einzige Weg zurück, und alle
drei müssen ohne Farbwahrnehmung erreichbar sein — also auch über die Tastatur.
