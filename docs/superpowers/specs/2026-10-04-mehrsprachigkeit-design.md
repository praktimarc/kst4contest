# Entwurf — Mehrsprachigkeit

Vertieft: `docs/superpowers/specs/2026-09-25-compose-migration-design.md`, Etappe 9.

Dieser Entwurf ersetzt den dortigen Abschnitt. Er ist im September geschrieben worden, und
seine Zahlen und eine seiner Annahmen stimmen nicht mehr.

## Ziel

Die Oberfläche spricht die Sprache des Operateurs, Deutsch und Englisch zum Start, und
weitere Sprachen können von außen beigetragen werden, ohne dass der Beitragende Kotlin anfasst.

Erfolg ist nachprüfbar:

- Die Sprache lässt sich umschalten, wirkt **sofort in allen offenen Fenstern** und bleibt nach
  dem Neustart umgeschaltet.
- Sie gilt **pro Operateurprofil**.
- **Kein einziger Text, der an den ON4KST-Server oder an andere Funker geht, ändert sich mit
  der Sprache.** Geprüft, nicht angesehen.
- Eine fehlende Übersetzung fällt auf Englisch zurück, statt zu fehlen.
- Eine fehlerhafte Übersetzung bricht den Bau ab, bevor sie ausgeliefert wird.

## Umfang dieser Etappe: die Mechanik, nicht die Texte

Gemessen am heutigen Stand, nach den Etappen 4 bis 8:

| | Zeichenketten |
|---|---|
`app-desktop` Kotlin | 852
`app-desktop` Java | 417
davon nach Oberflächentext aussehend, verschieden | **~394**
`core` | 2.968, davon 273 `println`

Die Zahlen des Ursprungsabschnitts (232 Compose, 492 Java) sind durch den JavaFX-Ausbau
überholt.

**Diese Etappe baut die Mechanik und zieht ein Dutzend Texte als Beweis heraus.** Die übrigen
rund 380 sind mechanische Folgearbeit, die häppchenweise gehen kann und keine
Architekturentscheidung mehr enthält. Der Grund für diesen Schnitt: die Mechanik trägt alle
380, und sie jetzt falsch zu bauen kostet 380 Mal nach.

## Die vier Textsorten

Der Ursprungsabschnitt nennt drei. Gemessen sind es vier, und die vierte ist die größte.

**Die Regel, die entscheidet, ist der Adressat des Textes** — nicht, wie er aussieht.

| Adressat | Sorte | Wo sie heute lebt | übersetzen |
|---|---|---|---|
Der Operateur am Bildschirm | Oberflächentext | `app-desktop/src/main` | **ja** |
Der ON4KST-Server | Protokolltext | `On4KstProtocol` (`LOGINC\|`, `SDONE\|`, `ACHAT\|`, `CK`, `DXQ`), dazu die `/`-Chatbefehle und `SETNAME`/`MYQRG` | nein |
Andere Funker | Funkertext | `ChatPreferences`-Vorgabewerte, Schnipsel, Kurztasten | nein |
Der Entwickler beim Fehlersuchen | Diagnosetext | 273 `println` in `core` | nein |

Die beiden mittleren sind dasselbe Risiko aus zwei Richtungen: ein übersetzter Protokollbefehl
bricht die Verbindung, ein übersetztes Funkerkürzel macht den Operateur unverständlich.

Der Protokolltext hat dabei zwei Ebenen, die nicht zu verwechseln sind: die **Rahmen** auf dem
Draht (`LOGINC|`, `SDONE|`, `ACHAT|` in `On4KstProtocol`) brechen bei einer Übersetzung die
Verbindung, die **`/`-Chatbefehle** (`/CQ`, `/AWAY`) gehen als Nachricht durch und werden vom
Server nicht verstanden. Beide dürfen sich nicht mit der Sprache ändern.

Keiner dieser Fehler fällt in einem Test auf, der nur die Oberfläche ansieht — und im Betrieb
fällt er erst im Contest auf, also unter Zeitdruck.

Der Diagnosetext ist neu benannt und gehört ausdrücklich dazu: ein deutscher Fehlerbericht ist
für den Wartenden schlechter als ein englischer, und die 273 `println`-Aufrufe sehen im
Quelltext genauso aus wie Oberflächentext.

### Die Regel für `core`

**`core` erzeugt keinen Anzeigetext. Es liefert einen Schlüssel oder einen Aufzählungswert,
und `app-desktop` macht daraus Text.**

Das ist die Fortsetzung des bestehenden Schnitts „`core` bleibt toolkit-frei" in die Sprache
hinein: `core` hat nicht zu wissen, welche Sprache der Mensch liest. Der zweite, praktische
Grund: weil `core` keine Nachschlagefunktion bekommt, **kann kein deutscher Text in eine
Protokoll- oder Diagnosezeile geraten.** Die Regel erzwingt die Abgrenzung, statt sie zu
verlangen.

Abgewogen und verworfen wurde, `core` ein Bündel mitzugeben. Ein `ResourceBundle` ist reines
JDK und hätte den Toolkit-Schnitt nicht verletzt — aber es hätte die Nachschlagefunktion neben
die 273 `println`-Aufrufe gestellt, also genau dorthin, wo Deutsch nicht hingehört.

**Zwei heutige Verstöße** werden benannt und bleiben diese Etappe englisch:

- `core/src/main/java/kst4contest/view/map/PathPropagationAssessment.java` — die
  Ausbreitungsbeschreibungen („Aircraft scatter, tropo ducting/enhancement, …").
- `core/src/main/java/kst4contest/logic/PriorityCalculator.java` — die Prioritätsgründe
  („Active now").

Sie zu verlegen ist Folgearbeit. Der Nutzen der Regel jetzt: die ~380 Folgetexte haben eine
Regel statt einer Diskussion pro Fall.

## Die Übersetzungen sind Dateien, die niemand kompilieren muss

`app-desktop/src/main/i18n/strings_en.properties` als Grundlage, `strings_de.properties`
daneben. Flach, ein Schlüssel, ein Text:

```properties
settings.save=Save settings
alert.armedFor=Armed for {0}
```

Wer eine Sprache beitragen will, kopiert die englische Datei, übersetzt die Werte rechts vom
Gleichheitszeichen und öffnet eine PR. Kein Kotlin, kein Gradle, keine Escapes für Umlaute.

**Warum `.properties` und nicht JSON.** Gemessen: `java.util.Properties` ist JDK und liest
seit Java 9 UTF-8, funktioniert also im Erzeuger ohne jede Abhängigkeit. JSON hätte entweder
eine neue Bauwerkzeug-Abhängigkeit gekostet oder einen selbstgeschriebenen Leser, dessen
Escape- und Unicode-Fehler dann hausgemacht wären — `groovy.json.JsonSlurper` steht nur im
Gradle-Kontext zur Verfügung und nicht in einem Modul, dessen Tests im normalen Lauf
mitzählen. Für Beitragende ist das Format zudem robuster: keine Klammern und Kommas, die man
kaputtmachen kann.

**Eine Datei pro Sprache**, nicht eine Datei mit allen Sprachen nebeneinander: so bleiben
Sprach-PRs voneinander getrennt. Den Vorteil der Nebeneinander-Form — der Übersetzer sieht das
Original neben seinem Feld — bildet ein Arbeitsblatt nach (siehe unten).

Englisch ist die Quellsprache, weil der Quelltext es so hält und `AGENTS.md` es für
Oberflächentext vorschreibt.

## Der Erzeuger: Unvollständigkeit ist erlaubt, Unrichtigkeit nicht

Ein Gradle-Task liest die Textdateien und erzeugt daraus Kotlin. **Die Fehler entstehen
dort**, nicht in einem Prüfer, der danebensteht.

| Befund | Erzeuger |
|---|---|
Schlüssel fehlt in einer Übersetzung | **Warnung**, Rückfall auf Englisch zur Laufzeit
Schlüssel steht in der Übersetzung, aber nicht in der englischen Grundlage | **Bau bricht ab** — Tippfehler des Beitragenden
Platzhalter weichen ab (`{0} {1}` → `{0}`) | **Bau bricht ab** — verschluckt Daten lautlos, und `MessageFormat` sagt dazu nichts
Datei unlesbar oder doppelter Schlüssel | **Bau bricht ab**
Schlüssel lässt sich nicht eindeutig in einen Kotlin-Namen übersetzen | **Bau bricht ab**

Diese Trennung ist der Kern: eine zu 80 % fertige beigetragene Sprache **muss** einreichbar
sein, sonst ist die ganze Auslagerung zwecklos. Eine falsche Übersetzung darf dagegen nicht
ausgeliefert werden.

Erzeugt wird nach `build/generated/`, nicht in den Quellbaum — damit kann das Erzeugnis nicht
von den Textdateien abdriften.

**Der Erzeuger ist ein eigenes Modul, kein Bauskript.** Die Spezifikation verlangt, ihn zu
testen; `buildSrc` leistet das nicht — gemessen: `buildSrc`-Tests laufen bei
`./gradlew build` nicht mit, was genau den Fehler ergäbe, den der Erzeuger verhindern soll. Er
wird deshalb ein drittes, **reines Bauwerkzeug-Modul** `i18n-generator` im Hauptbau, von
`app-desktop` über eine `JavaExec`-Aufgabe vor `compileKotlin` aufgerufen. Seine Tests sind
gewöhnliche Projekttests: sie laufen bei `./gradlew build`, zählen in der Test-XML mit und
werden von der bestehenden CI gefahren.

Das Modul wird **nicht** ausgeliefert: es steht nicht im Laufzeit-Klassenpfad von
`app-desktop`, nur in einer eigenen Konfiguration für den Aufruf. `CLAUDE.md` und
`docs/PROJECT_CONTEXT.md` nennen heute zwei Module und sind entsprechend nachzuziehen.

**Kein Parser zur Laufzeit.** Die Texte stehen als Kotlin im Jar: kein `ResourceBundle`, keine
Datei-E/A, und **keine neue Abhängigkeit, weder im Jar noch im Bau**.

### Die erzeugte Form macht auch die Stelligkeit prüfbar

```kotlin
class Strings(private val lookup: (String) -> String) {
    val settingsSave: String get() = lookup("settings.save")
    fun alertArmedFor(arg0: Any): String = MessageFormat.format(lookup("alert.armedFor"), arg0)
}
```

Also `strings.settingsSave` statt `strings.get("settings.save")`. Ein Tippfehler ist ein
Übersetzungsfehler, und `alertArmedFor()` ohne Argument übersetzt nicht.

Das ist der Teil, der die ~380 Folgetexte trägt: bei stringbasierten Schlüsseln wäre jeder
davon eine neue Gelegenheit für einen Laufzeitfehler, und die Fehlerart wäre „Text fehlt im
Fenster" — dieselbe stille Sorte, die diesen Zweig schon mehrfach erwischt hat.

Form und Stelligkeit kommen **immer aus der englischen Grundlage.** Deshalb bleibt ein falscher
Platzhalter ein Bauabbruch, während ein fehlender Schlüssel nur ein Rückfall ist.

### Das Arbeitsblatt für Beitragende

Ein Task erzeugt `strings_<lang>.todo.properties`: die fehlenden Schlüssel mit dem
**englischen Text als Wert**. Ohne das müsste ein Beitragender zwei Dateien diffen, um zu finden, was fehlt.

### Deckung

Der Erzeuger gibt beim Bau eine Zeile aus — `de: 394/394, fr: 120/394` —, damit einer PR
ansehbar ist, wie weit sie ist.

**Deutsch wird durch einen Test auf Vollständigkeit gehalten, weitere Sprachen nicht.** Deutsch
ist eine ausgelieferte Sprache; dort ist eine Lücke ein Fehler. Bei einer beigetragenen Sprache
ist eine Lücke der Normalfall.

## Dass es alle Fenster erreicht

Dieselbe Bauform wie bei den Farben in Etappe 8, damit es nur eine zu verstehen gibt.

Ein **`LanguageStore`** hält die Sprache als Compose-Zustand. Zwei Aufrufwege, eine Wahrheit:

- **Compose:** `LocalStrings.current.settingsSave`, bereitgestellt in
  `ComposeWindowHost.show` neben `LocalPaletteStore`. Der Zustandszugriff ist wieder genau das,
  was die Neuzeichnung auslöst — in Etappe 8 war `remember(darkMode)` der Grund, warum eine
  Änderung gar kein Fenster erreichte.
- **Java:** `Strings.current()` gegen ein `@Volatile`-Feld, das der Store beim Wechsel setzt.
  Die verbliebenen AWT-Aufrufstellen holen damit beim nächsten Aufruf die neue Sprache, ohne
  eigene Mechanik.

Der Store wird beim Profilwechsel neu gebildet, wie jeder andere profilgebundene Zustand.

## Die Einstellung

Ein Feld `guiOptions_language` in `ChatPreferences` — pro Profil, wie jede andere
GUI-Einstellung. Bei einer Mehrmann-Station ist das auch das Richtige: zwei Operateure, zwei
Sprachen. Drei Werte: leer (Systemsprache), `en`, `de`. Ein `Form.choice` im GUI-Reiter.

Die Vorgabe ist eine **reine Funktion** `localeFor(stored, systemDefault)`, damit sie ohne
Systemwechsel prüfbar ist: leerer Wert heißt Systemsprache, und ist die nicht Deutsch, dann
Englisch.

## Die zwölf Beweistexte

Bewußt **über mehrere Fenster verteilt**, nicht die zwölf nächstliegenden aus einer Datei: die
Save- und Close-Knöpfe des Einstellungsfensters, die Abschnittstitel des GUI-Reiters, die
Beschriftung der Sprachwahl selbst, ein Menütitel im Hauptfenster, die Verbindungsanzeige in
der Statuszeile und ein `ComposeAlert`-Knopf.

Der Grund ist das Abnahmekriterium: beim Umschalten im Einstellungsfenster muss sich **das
Hauptfenster dahinter sichtbar mitändern.** Zwölf Texte aus einer Datei würden das nicht
zeigen, und genau diese Sorte Scheinbeweis hat diesen Zweig mehrfach erwischt — eine
Einstellung, die durchgeschrieben und nicht gezeichnet wurde.

## Prüfung

Das Wertvolle sind die Regeln, nicht die Oberfläche. Alles ohne Bildschirm prüfbar:

- **Die drei Abgrenzungstests.** (1) `On4KstProtocol` baut seine Rahmen unter
  `Locale.GERMAN` und `Locale.ENGLISH` zeichengleich — `login`, `settingsDone`, `addChat`,
  `clientLivenessProbe`, `serverLivenessProbeResponse`. Geprüft wird die Ausgabe, nicht das
  Bündel. Die Klasse ist paketprivat, der Test gehört also nach
  `core/src/test/java/kst4contest/controller/`. (2) Die `ChatPreferences`-Vorgabewerte ändern sich mit keiner Sprache, **über
  Reflexion über alle `String`-Felder**, nicht über eine gepflegte Liste: das deckt auch ab,
  was nach dieser Etappe dazukommt. (3) Kein Wert in einem Bündel ist ein
  Protokoll-Opcode, ein `/`-Chatbefehl, `SETNAME`, `MYQRG` oder ein
  `ChatPreferences`-Vorgabewert.
- **Der Erzeuger selbst**, je ein Test für die fünf Befunde seiner Tabelle, gegen kleine
  Textschnipsel. Das ist der Teil, der falsch sein kann, ohne dass es auffällt — ein
  Bauabbruch, der nie abbricht, sieht aus wie ein sauberes Projekt.
- **`localeFor`**: leer und deutsches System → Deutsch; leer und französisches System →
  Englisch; `"de"` gespeichert und englisches System → Deutsch; Müll gespeichert → Englisch.
- **Der Rückfall**: ein Schlüssel, den nur Englisch hat, kommt unter Deutsch englisch zurück.
- **Deutsch ist vollständig**, über die erzeugten Schlüssel.
- **Dass ein Sprachwechsel die Komposition erreicht** — dieselbe Bauform wie
  `ThemeReadsPaletteStoreTest`, das bei den Farben genau diesen Fehler gefunden hat.

**Die PR-Prüfung fängt das alles schon.** Der `compile`-Auftrag in `pr-compile-check.yml` läuft
`./gradlew -S classes testClasses`; der Erzeuger hängt an `classes`, also schlägt eine kaputte
Übersetzung dort mit seiner eigenen Meldung fehl. Der `test`-Auftrag fährt die
Abgrenzungstests. **Kein neuer Workflow nötig** — das ist eine Folge davon, den Erzeuger in den
Bau zu hängen statt in ein eigenes Prüfskript.

## Was unverändert bleibt

- Die vier `ChatPreferences`-Vorgabewerte und alles, was in der gespeicherten XML eines
  Operateurs steht: `bcn_beaconTextMainCat`, `bcn_beaconTextSecondCat`,
  `messageHandling_autoAnswerTextMainCat`, `messageHandling_autoAnswerTextSecondCat`, sowie
  Schnipsel und Kurztasten.
- `core` bekommt keine Nachschlagefunktion.
- Die Reiterreihenfolge im Einstellungsfenster; die Sprachwahl kommt in den bestehenden
  GUI-Reiter.
- Keine neue Abhängigkeit, weder im Jar noch im Bau.
- `app-desktop`s übrige ~380 Texte bleiben englisch, mit der Regel aus „Die vier Textsorten"
  für die Folgearbeit.
- Quelltextkommentare und Javadoc bleiben ausschließlich Englisch. Mehrsprachigkeit betrifft
  die Oberfläche, nicht den Quelltext.

## Risiken

**Die Abgrenzung ist der Preis dieser Etappe, nicht die Mechanik.** Ein übersetzter
Protokollbefehl oder ein übersetztes Funkerkürzel fällt beim Bauen nicht auf und beim Hinsehen
auch nicht. Die drei Abgrenzungstests sind die Gegenmaßnahme, und der reflexionsbasierte unter
ihnen ist der einzige, der auch das abdeckt, woran beim Schreiben niemand gedacht hat.

**Ein Sprachwechsel erreicht alle Fenster — also auch alle falsch.** Der `LanguageStore` ist
wie der `PaletteStore` eine Stelle, an der ein Fehler sich überall gleichzeitig zeigt. Dafür
zeigt er sich sofort.

**Eine beigetragene Sprache kann halb englisch sein.** Das ist gewollt und der Preis dafür,
Beiträge überhaupt annehmen zu können. Die Deckungszeile macht es sichtbar; ob die Sprachwahl
dem Operateur den Deckungsgrad anzeigen soll, ist hier **nicht** entschieden und wäre eine
eigene kleine Erweiterung.

**Der Erzeuger ist neuer Bau-Code und damit eine neue Stelle, an der der Bau brechen kann.**
Dagegen hilft nur, ihn zu testen wie Produktionscode — deshalb steht er in der Prüfliste.

## Festlegungen, die sonst im Bau beliebig würden

**Die Regel ist der Adressat, nicht das Aussehen.** Wer beim Herausziehen unsicher ist, fragt
nicht „sieht das nach Oberfläche aus", sondern „wer liest das".

**Unvollständigkeit ist erlaubt, Unrichtigkeit nicht.** Das entscheidet jeden Zweifelsfall im
Erzeuger.

**Die Stelligkeit kommt aus der englischen Grundlage.** Eine Übersetzung kann einen Schlüssel
weglassen, aber nicht seine Platzhalter umdeuten.
