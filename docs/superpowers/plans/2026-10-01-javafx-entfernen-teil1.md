# JavaFX entfernen, Teil 1: Bestandsaufnahme und `derive`-Pin

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Den `derive`-Pin so einfrieren, dass er das Entfernen von JavaFX überlebt, und belegen, welcher Teil des nie angezeigten JavaFX-Aufbaus folgenlos gelöscht werden kann.

**Architecture:** Zwei voneinander unabhängige Stränge. Der erste ersetzt einen Reflexionsaufruf im Test durch eingecheckte Daten — klein, testgetrieben, zeitkritisch. Der zweite ist Verifikationsarbeit an `Kst4ContestApplication`: für jede verdächtige Bindung wird durch Lesen **und** durch einen Lauf der Anwendung festgestellt, ob sie den Betrieb speist oder nur die tote Tabelle. Ergebnis ist ein Befunddokument, auf dem Teil 2 aufsetzt.

**Tech Stack:** Java 21, Kotlin 2.2, Gradle, JUnit 5, Compose Multiplatform 1.8.2, JavaFX 21 (noch vorhanden).

**Spec:** `docs/superpowers/specs/2026-10-01-javafx-entfernen-design.md`

## Warum nur Teil 1

Die Spezifikation hat sechs Etappen. Etappe 2 (toten Aufbau löschen), 4 (Dispatcher), 5 (Lebenszyklus) und 6 (Reste und Build) lassen sich heute **nicht ohne Platzhalter** schreiben: welcher Block gelöscht wird und was vorher zu entflechten ist, ist das Ergebnis von Etappe 1. Ein Plan, der das vorwegnimmt, wäre geraten.

Dieser Plan deckt Etappe 1 und Etappe 3 ab — die beiden, die heute vollständig spezifizierbar sind. Teil 2 wird geschrieben, wenn der Befund aus Aufgabe 5 vorliegt.

## Global Constraints

- Kommunikation mit dem Benutzer auf Deutsch; Quelltextkommentare und Javadoc ausschließlich auf Englisch (`AGENTS.md`).
- Keine neue Produktionsabhängigkeit ohne vorherige Freigabe (`AGENTS.md`).
- Kein Commit, Push, Merge, Tag oder Release ohne gesonderte ausdrückliche Freigabe (`AGENTS.md`). Die Commit-Schritte unten werden ausgeführt, weil dieser Plan sie benennt und der Benutzer ihn freigibt; alles darüber hinaus nicht.
- Der Build erlaubt historisch, dass Testfehler ignoriert werden. **Nicht** aus Exit-Code 0 auf „alle Tests grün" schließen — die XML-Ergebnisse unter `*/build/test-results/` lesen (`AGENTS.md`).
- Ausgangslage: `./gradlew clean build` ist grün, 766 Tests, 0 Fehler. Jede Rotfärbung ist ein Signal.
- Signierte Commits sind konfiguriert (`commit.gpgsign=true`, SSH). Schlägt das Signieren fehl, läuft der Bitwarden-Agent nicht — das ist kein Grund, unsigniert zu committen.
- In diesem Plan wird **keine** Produktionsdatei unter `src/main` gelöscht. Löschen ist Teil 2.

## Review Focus

Fünf Dinge, die die Spezifikation voraussetzt, die aber keine Aufgabe hier prüft, geordnet nach Wahrscheinlichkeit, dass sie jemanden treffen:

1. **Der eingefrorene `derive`-Pin könnte die falschen Werte einfrieren.** Werden die Daten aus der eigenen Umsetzung statt aus JavaFX erzeugt, prüft der Test danach nur noch sich selbst. Aufgabe 1 erzeugt sie deshalb nachweislich über den Reflexionsaufruf und Aufgabe 2 prüft, dass mindestens ein Datensatz von einer absichtlich verfälschten Umsetzung abgelehnt wird.
2. **Die eingefrorenen Daten könnten den benutzten Wertebereich nicht abdecken.** `derive` wird mit 26.4 % und 35 % auf Stylesheet-Farben gerufen; Daten nur für Weiß und Schwarz hülfen nicht. Aufgabe 1 deckt die Prozentwerte und Farben ab, die der Test heute benutzt, und zusätzlich die Sprungstellen der Helligkeitsstaffel (0.2, 0.3, 0.4, 0.5, 0.6, 0.85).
3. **Der Befund aus Aufgabe 5 könnte eine Bindung übersehen, die nur bei bestimmten Einstellungen lebt.** Ein Profil mit anderen aktiven Bändern oder abgeschalteter Karte durchläuft andere Zweige. Aufgabe 4 prüft deshalb gegen ein zweites Profil.
4. **Ein Listener könnte nur beim Profilwechsel lebendig werden.** `closeOwnedStages` und der Wiederaufbau laufen nur dort. Aufgabe 4 schließt einen Profilwechsel ein.
5. **Die Systemmenüleiste könnte auf macOS anders leben als auf Linux.** Geprüft wird auf Linux; der Befund hält das ausdrücklich als ungeprüft fest, statt es offenzulassen.

---

## File Structure

| Datei | Verantwortung | Aufgabe |
|---|---|---|
| `app-desktop/src/test/resources/javafx-derive-reference.txt` | **neu** — die von JavaFX gelieferten Werte als eingecheckte Daten | 1 |
| `app-desktop/src/test/kotlin/kst4contest/view/compose/JavaFxStylesheetTest.kt` | **ändern** — vergleicht gegen die Daten statt gegen den Reflexionsaufruf | 1, 2 |
| `app-desktop/src/main/kotlin/kst4contest/view/compose/JavaFxStylesheet.kt` | **ändern** — nur der veraltete Kommentar | 2 |
| `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md` | **neu** — der Befund, auf dem Teil 2 aufsetzt | 3, 4, 5 |

---

## Task 1: Die JavaFX-Werte als Daten einfrieren

**Files:**
- Create: `app-desktop/src/test/resources/javafx-derive-reference.txt`
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/JavaFxStylesheetTest.kt`
- Test: dieselbe Datei

**Interfaces:**
- Consumes: `JavaFxStylesheet.derive(color: Color, percent: Double): Color` — unverändert.
- Produces: die Ressource `javafx-derive-reference.txt`, Zeilenformat
  `<rgbHex>;<percent>;<erwarteterRgbHex>`, z. B. `#ececec;26.4;#f4f4f4`. Drei
  Kommentarzeilen am Anfang, jede mit `#`. Aufgabe 2 schreibt den Leser dafür.
  Ausserdem bleibt `deriveWithJavaFx(rgb: String, percent: Double): String` im Test
  erhalten — Aufgabe 2 entfernt nur seinen Aufruf aus dem laufenden Vergleich, nicht
  die Methode.

- [ ] **Step 1: Den bestehenden Test lesen, bevor etwas geändert wird**

Run: `sed -n '1,60p;160,180p' app-desktop/src/test/kotlin/kst4contest/view/compose/JavaFxStylesheetTest.kt`

Erwartet: `deriveWithJavaFx(rgb, percent)` ruft über `Class.forName("com.sun.javafx.util.Utils").getMethod("deriveColor", ...)` das echte JavaFX. Der Test `derive gives the values JavaFX gives` vergleicht drei Größen: die erwarteten Werte aus der Tabelle, `JavaFxStylesheet.derive` und `deriveWithJavaFx`. Notiere den genauen Namen der Hilfsmethoden `parse`, `hex` und `assertChannelsClose` — sie werden unten weiterbenutzt.

- [ ] **Step 2: Einen Generator schreiben, der die Daten aus JavaFX erzeugt**

Ein JUnit-Test, der die Datei schreibt statt etwas zu behaupten. Er wird einmal benutzt und bleibt danach als `@Disabled` stehen, damit nachvollziehbar ist, woher die Daten kommen.

In `JavaFxStylesheetTest.kt` ergänzen:

```kotlin
    /**
     * Writes the reference data the frozen test compares against.
     *
     * Disabled because it is a generator, not an assertion: it runs once, while JavaFX
     * is still on the classpath, and its output is checked in. Kept rather than deleted
     * so that the provenance of the data is readable — the values come from
     * com.sun.javafx.util.Utils.deriveColor and from nowhere else, which is the whole
     * point of pinning them.
     *
     * To regenerate: remove @Disabled, run, restore @Disabled.
     */
    @org.junit.jupiter.api.Disabled("generator; run by hand while JavaFX is present")
    @Test
    fun `generate the JavaFX reference data`() {
        val colours = listOf(
            "#ffffff", "#000000", "#ececec", "#373e43", "#1d1d1d", "#4da6ff",
            "#2b2b2b", "#808080", "#333333", "#4d4d4d", "#666666", "#999999",
            "#d9d9d9", "#f4f4f4", "#63067a", "#ff9900",
        )
        val percents = listOf(-80.0, -50.0, -35.0, -30.0, -10.0, 10.0, 26.4, 35.0, 50.0, 80.0)

        val lines = buildList {
            for (rgb in colours) {
                for (percent in percents) {
                    add("$rgb;$percent;${deriveWithJavaFx(rgb, percent)}")
                }
            }
        }

        val target = java.io.File("src/test/resources/javafx-derive-reference.txt")
        target.parentFile.mkdirs()
        target.writeText(
            "# Generated from com.sun.javafx.util.Utils.deriveColor by\n" +
                "# JavaFxStylesheetTest.`generate the JavaFX reference data`.\n" +
                "# Format: <rgbHex>;<percent>;<expectedRgbHex>\n" +
                lines.joinToString("\n") + "\n"
        )
        println("wrote ${lines.size} pairs to ${target.absolutePath}")
    }
```

Die Farbliste deckt ab: Weiß und Schwarz (Ränder), die Modena-Basis `#ececec`, die Abendblech-Basis `#373e43`, Marker- und Gitterfarben aus der Karte, und Graustufen, die die Sprungstellen der Helligkeitsstaffel treffen (0.2, 0.3, 0.4, 0.5, 0.6, 0.85 — siehe Review Focus 2). Die Prozentwerte decken die im Produktionscode benutzten 26.4 und 35 ab, dazu negative und positive Extreme.

- [ ] **Step 3: Den Generator einmal laufen lassen**

`@Disabled` vorübergehend auskommentieren, dann:

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.JavaFxStylesheetTest' --console=plain -i 2>&1 | grep "wrote"`

Erwartet: eine Zeile `wrote 160 pairs to /…/app-desktop/src/test/resources/javafx-derive-reference.txt`.

Danach `@Disabled` wieder einkommentieren.

- [ ] **Step 4: Prüfen, dass die Daten wirklich von JavaFX stammen**

Run: `head -5 app-desktop/src/test/resources/javafx-derive-reference.txt && wc -l app-desktop/src/test/resources/javafx-derive-reference.txt`

Erwartet: drei Kommentarzeilen, dann Zeilen der Form `#ffffff;-80.0;#333333`. Insgesamt 163 Zeilen.

Stichprobe von Hand: `#ffffff;-30.0;…` muss ein mittleres Grau sein, nicht Weiß. Ist die Spalte identisch mit der ersten, hat der Generator nicht JavaFX gefragt — dann Schritt 2 prüfen, **nicht** weitermachen.

- [ ] **Step 5: Commit**

```bash
git add app-desktop/src/test/resources/javafx-derive-reference.txt \
        app-desktop/src/test/kotlin/kst4contest/view/compose/JavaFxStylesheetTest.kt
git commit -m "Freeze the JavaFX colour-derivation reference as test data

JavaFxStylesheetTest compares derive() against com.sun.javafx.util.Utils
at runtime, through reflection. That comparison stops working the moment
JavaFX leaves the classpath, and the test would then quietly fall back to
checking derive() against itself.

The pairs are generated from the real JavaFX implementation while it is
still here and checked in. The generator stays, disabled, so the
provenance of the numbers is readable."
```

---

## Task 2: Den Test gegen die Daten prüfen lassen

**Files:**
- Modify: `app-desktop/src/test/kotlin/kst4contest/view/compose/JavaFxStylesheetTest.kt`
- Modify: `app-desktop/src/main/kotlin/kst4contest/view/compose/JavaFxStylesheet.kt` (nur Kommentar)
- Test: dieselbe Testdatei

**Interfaces:**
- Consumes: `javafx-derive-reference.txt` aus Aufgabe 1.
- Produces: nichts, was spätere Aufgaben benutzen.

- [ ] **Step 1: Den Test schreiben, der gegen die Daten prüft**

In `JavaFxStylesheetTest.kt` ergänzen:

```kotlin
    /** The reference pairs, as `(rgbHex, percent, expectedRgbHex)`. */
    private fun referencePairs(): List<Triple<String, Double, String>> {
        val resource = javaClass.getResourceAsStream("/javafx-derive-reference.txt")
            ?: error("javafx-derive-reference.txt is missing from the test resources")

        return resource.bufferedReader().readLines()
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { line ->
                val (rgb, percent, expected) = line.split(";")
                Triple(rgb, percent.toDouble(), expected)
            }
    }

    /**
     * derive() against the values JavaFX itself produced.
     *
     * Against checked-in data rather than a live reflection call, because the live call
     * disappears with JavaFX and would take the only real check with it — leaving a
     * test that compares derive() to derive().
     */
    @Test
    fun `derive matches the frozen JavaFX reference`() {
        val pairs = referencePairs()

        assertTrue(pairs.size >= 100) { "only ${pairs.size} reference pairs; the file looks truncated" }

        pairs.forEach { (rgb, percent, expected) ->
            assertChannelsClose(
                parse(expected),
                JavaFxStylesheet.derive(parse(rgb), percent),
                "derive($rgb, $percent%)",
            )
        }
    }
```

- [ ] **Step 2: Den Test laufen lassen und sehen, dass er grün ist**

Run: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.JavaFxStylesheetTest' --console=plain > /tmp/t1.txt 2>&1; python3 -c "
import glob,xml.etree.ElementTree as ET
for f in glob.glob('app-desktop/build/test-results/test/TEST-*JavaFxStylesheetTest.xml'):
    r=ET.parse(f).getroot(); print(f\"tests={r.get('tests')} failures={r.get('failures')} skipped={r.get('skipped')}\")
    for tc in r.iter('testcase'):
        for b in list(tc.iter('failure'))+list(tc.iter('error')): print(tc.get('name'), (b.get('message') or '')[:200])"`

Erwartet: `failures=0`, `skipped=1` (der Generator).

Ein grüner Test ist hier **noch kein Beweis** — er könnte grün sein, weil die Daten aus der eigenen Umsetzung stammen. Das prüft der nächste Schritt.

- [ ] **Step 3: Beweisen, dass der Test den Fehler fängt, den er fangen soll**

Review Focus 1. Die Produktionsumsetzung vorübergehend verfälschen:

In `JavaFxStylesheet.kt`, in `derive`, `calcBrightness = calcBrightness.coerceIn(-1.0, 1.0)` ersetzen durch `calcBrightness = (calcBrightness * 1.05).coerceIn(-1.0, 1.0)`.

Run: denselben Befehl wie Schritt 2.

Erwartet: `failures=1`, und die Meldung nennt ein konkretes `derive(#…, …%)`. Fällt der Test **nicht**, sind die Daten wertlos — dann stammen sie nicht von JavaFX; zurück zu Aufgabe 1.

Danach die Verfälschung zurücknehmen und erneut laufen lassen: `failures=0`.

- [ ] **Step 4: Den Reflexionsvergleich aus dem laufenden Test nehmen**

Die Zeile, die `deriveWithJavaFx` im Test `derive gives the values JavaFX gives` aufruft (um `:50`), entfernen, sodass dieser Test nur noch seine eigene Tabelle gegen `JavaFxStylesheet.derive` prüft. `deriveWithJavaFx` selbst **bleibt** — der Generator in Aufgabe 1 braucht es.

Begründung als Kommentar über den entfernten Vergleich setzen:

```kotlin
            /*
             * No live JavaFX comparison here. It lives in the frozen reference data
             * instead (javafx-derive-reference.txt), because a reflection call into
             * com.sun.javafx stops working when JavaFX is removed and would take the
             * only real check with it.
             */
```

- [ ] **Step 5: Den veralteten Kommentar in der Produktionsdatei berichtigen**

In `JavaFxStylesheet.kt` beschreibt der Kommentar über `derive` (um `:116`–`:129`) eine Reflexionsfassung, die es nicht mehr gibt. Ersetzen durch:

```kotlin
    /**
     * JavaFX `derive(colour, percent)`, reimplemented.
     *
     * No JavaFX call and no reflection: this is the algorithm itself. The brightness it
     * derives from is not the HSB value but `0.3R + 0.59G + 0.11B`, and the positive
     * branch follows a staircase rather than a formula, which is why an earlier
     * approximation here was measurably wrong.
     *
     * Correctness is not argued, it is pinned: `JavaFxStylesheetTest` checks this
     * against values the real `com.sun.javafx.util.Utils.deriveColor` produced, frozen
     * into `javafx-derive-reference.txt` so the check survives JavaFX being removed.
     */
```

- [ ] **Step 6: Die ganze Suite laufen lassen**

Run: `./gradlew clean build --console=plain > /tmp/b.txt 2>&1; echo "exit=$?"; python3 -c "
import glob,xml.etree.ElementTree as ET
t=f=0
for p in glob.glob('*/build/test-results/test/TEST-*.xml'):
    r=ET.parse(p).getroot(); t+=int(r.get('tests')); f+=int(r.get('failures'))+int(r.get('errors'))
print(f'tests={t} failures={f}')"`

Erwartet: `exit=0`, `failures=0`, und `tests` um eins höher als die 766 vom Ausgangsstand.

- [ ] **Step 7: Commit**

```bash
git add app-desktop/src/test/kotlin/kst4contest/view/compose/JavaFxStylesheetTest.kt \
        app-desktop/src/main/kotlin/kst4contest/view/compose/JavaFxStylesheet.kt
git commit -m "Check derive() against the frozen reference instead of live JavaFX

The reflection call stays only in the disabled generator. The running
test now reads checked-in values, so it keeps its meaning once JavaFX is
gone rather than silently comparing derive() with itself.

Verified by breaking derive() on purpose and watching the test fail.

Also corrects the doc comment on derive(), which still described a
reflection delegation that the code has not done for some time."
```

---

## Task 3: Feststellen, ob `FxRosterBinding` den Betrieb speist

**Files:**
- Create: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`
- Read only: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`, `app-desktop/src/main/java/kst4contest/view/FxRosterBinding.java`

**Interfaces:**
- Consumes: nichts.
- Produces: das Befunddokument mit dem Abschnitt „FxRosterBinding". Aufgabe 5 fasst zusammen, Teil 2 baut darauf auf.

**Keine Produktionsdatei wird in dieser Aufgabe geändert.**

- [ ] **Step 1: Alle Nutzungen des Spiegels auflisten**

Run: `grep -n "chatMemberListBinding" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: Erzeugung um `:2513`, Nullsetzung um `:6877`, eine Prüfung um `:1766`. Jede weitere Fundstelle notieren.

- [ ] **Step 2: Feststellen, wer den gespiegelten `ObservableList` liest**

Run: `grep -n "chatMemberListBinding.get\|chatMemberListBinding\.\(mirror\|list\|items\)" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java; grep -n "public" app-desktop/src/main/java/kst4contest/view/FxRosterBinding.java`

Erwartet: die öffentliche Fläche von `FxRosterBinding` und jede Stelle, die den Spiegel abruft. Für jede Stelle festhalten: landet sie in einer `TableView` (tot) oder in etwas, das Compose liest (lebend)?

- [ ] **Step 3: Feststellen, woher die Compose-Stationsliste ihre Zeilen bekommt**

Run: `grep -n "DataTableState<ChatMember>\|setRows\|rows =" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java | head -20; grep -n "fun setRows\|var rows\|val rows" app-desktop/src/main/kotlin/kst4contest/view/compose/DataTableState.kt`

Erwartet: eine durchgehende Kette von `ObservableRoster` bis `DataTableState.rows`. Notiere sie Glied für Glied. Führt die Kette durch `chatMemberListBinding`, ist der Spiegel **lebend**.

- [ ] **Step 4: Den Befund schreiben**

`docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md` anlegen:

```markdown
# Lebendbefund: was im JavaFX-Aufbau von KST4Contest tatsächlich benutzt wird

Grundlage für Teil 2 von `docs/superpowers/specs/2026-10-01-javafx-entfernen-design.md`.
Erhoben am 1. Oktober 2026 auf Linux, Branch `nextMajorRelease/version1_50`.

Jeder Block bekommt genau einen von zwei Töpfen:
**folgenlos löschbar** oder **zu entflechten** (mit der Stelle, die davon abhängt).

## FxRosterBinding (`chatMemberListBinding`)

**Topf:** <folgenlos löschbar | zu entflechten>

**Belege:**
- Erzeugt bei `Kst4ContestApplication.java:<Zeile>`, genullt bei `:<Zeile>`.
- Gelesen von: <Liste der Stellen, je mit Zeile und „tot/lebend">
- Die Compose-Stationsliste bezieht ihre Zeilen über: <die Kette aus Schritt 3>

**Folgerung:** <ein Satz>
```

Jede spitze Klammer wird durch das ersetzt, was die Schritte 1–3 ergeben haben. Bleibt eine stehen, ist die Aufgabe nicht fertig.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Record what FxRosterBinding actually feeds

First section of the finding that part 2 of the JavaFX removal builds on.
No production code touched."
```

---

## Task 4: Die übrigen Verdachtsfälle am laufenden Programm prüfen

**Files:**
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`
- Read only: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

**Interfaces:**
- Consumes: das Befunddokument aus Aufgabe 3.
- Produces: dasselbe Dokument mit den Abschnitten „registerThemedScene", „LayoutAutosave", „Systemmenüleiste" und „Kandidatenfenster".

**Keine Produktionsdatei wird in dieser Aufgabe geändert.** Diese Aufgabe erfordert, die Anwendung zu starten und zu bedienen; Lesen allein beantwortet sie nicht.

- [ ] **Step 1: Die Stellen auflisten**

Run: `grep -n "registerThemedScene\|LayoutAutosave\|layoutAutosave\|installSharedSystemMenuBar\|initMenuBar\|showTopPriorityCandidatesWindow" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet unter anderem: drei Aufrufe von `registerThemedScene`, `layoutAutosave` erzeugt um `:7420`, `installSharedSystemMenuBar(candidatesScene)` um `:4858`, `initMenuBar()` um `:5729`, `showTopPriorityCandidatesWindow` gerufen bei `:4706` (toter JavaFX-Knopf) und `:10707` (lebender Compose-Pfad).

- [ ] **Step 2: Die Anwendung starten und das Kandidatenfenster öffnen**

Run: `./gradlew :app-desktop:run`

Dann im Compose-Hauptfenster den Knopf **more** neben den Prioritätsknöpfen drücken.

Erwartet: ein Fenster mit der Kandidatenliste erscheint. Festhalten: Titelleiste, ob es eine eigene Menüleiste trägt, ob es modal ist, und ob ein Klick auf einen Eintrag im Hauptfenster eine Station auswählt.

Erscheint **kein** Fenster, ist der Pfad trotz `:10707` tot — das ist ein genauso wertvoller Befund und gehört so ins Dokument.

- [ ] **Step 3: Das Design umschalten und beobachten, ob die Compose-Fenster folgen**

Bei laufender Anwendung das Design über die Menüleiste des Hauptfensters umschalten (Tag/Abend).

Erwartet: Hauptfenster, Kartenfenster und Einstellungsfenster wechseln die Farben. Notieren, ob **alle** folgen. Folgt eines nicht, hängt es womöglich an `registerThemedScene` und gehört in den Topf „zu entflechten".

- [ ] **Step 4: Einen Profilwechsel durchführen**

Review Focus 4. Über die Menüleiste das Bedienerprofil wechseln.

Erwartet: die Fenster werden geschlossen und neu aufgebaut; die Anwendung bleibt bedienbar; in der Konsole erscheint keine Ausnahme. Notieren, was passiert.

- [ ] **Step 5: Mit einem zweiten Profil wiederholen**

Review Focus 3. Ein Profil mit anderen aktiven Bändern wählen (mindestens ein Band an- oder abgeschaltet gegenüber dem ersten) und die Schritte 2 bis 4 wiederholen.

Erwartet: dasselbe Verhalten. Weicht es ab, gehört die Abweichung mit der Einstellung ins Dokument, die sie auslöst.

- [ ] **Step 6: Fenstergrößen auf Persistenz prüfen**

Ein Fenster in der Größe ändern, die Anwendung beenden, neu starten.

Erwartet: die Größe ist erhalten. Ist sie es nicht, schreibt `LayoutAutosave` womöglich Werte, die niemand mehr liest — oder umgekehrt. Notieren.

- [ ] **Step 7: Die Abschnitte ins Befunddokument schreiben**

Für jeden der vier Blöcke — `registerThemedScene`, `LayoutAutosave`, Systemmenüleiste, Kandidatenfenster — denselben Aufbau wie in Aufgabe 3 Schritt 4: Topf, Belege, Folgerung.

Die Systemmenüleiste bekommt zusätzlich die Zeile:

```markdown
**Nicht geprüft:** macOS. Erhoben wurde auf Linux. Ob die Systemmenüleiste dort
sichtbar an der JavaFX-`MenuBar` hängt, ist offen und muss vor dem Entfernen auf
einem Mac nachgeholt werden.
```

- [ ] **Step 8: Commit**

```bash
git add docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Record what the themed scenes, autosave and menu bar actually do

Established by running the application and operating it, not by reading:
a listener that is never fired reads the same as one that does not exist.
Includes a profile switch and a second profile, and marks macOS as
unverified. No production code touched."
```

---

## Task 5: Den Befund abschließen und Teil 2 umreißen

**Files:**
- Modify: `docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

**Interfaces:**
- Consumes: alle Abschnitte aus Aufgabe 3 und 4.
- Produces: die Löschreihenfolge, aus der Teil 2 geschrieben wird.

- [ ] **Step 1: Den toten Aufbau in Scheiben zerlegen**

Run: `grep -n "private.*void init[A-Z][a-zA-Z]*(\|private.*Pane init[A-Z][a-zA-Z]*(\|private.*Node init[A-Z][a-zA-Z]*(" app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

Erwartet: die `init…`-Methoden, die zusammen das nie gezeigte Fenster bauen. Für jede notieren: Zeilenbereich, ungefähre Länge und ob der Befund aus Aufgabe 3 und 4 sie als „folgenlos löschbar" ausweist.

- [ ] **Step 2: Eine Reihenfolge festlegen, die den Build nach jeder Scheibe grün lässt**

Blätter zuerst, Wurzeln zuletzt: eine Methode, die niemand mehr ruft, lässt sich löschen; eine, die noch gerufen wird, nicht. Die Reihenfolge als nummerierte Liste ins Dokument schreiben, jede Scheibe mit den Methoden, die sie umfasst.

- [ ] **Step 3: Die Zusammenfassung schreiben**

Am Anfang des Dokuments, unter der Überschrift:

```markdown
## Ergebnis in einem Satz

<Von den N init-Methoden sind M folgenlos löschbar; K sind zu entflechten, nämlich …>

## Löschreihenfolge für Teil 2

1. <Scheibe> — <Methoden> — <geschätzte Zeilen>
2. …
```

- [ ] **Step 4: Prüfen, dass keine Lücke bleibt**

Run: `grep -c "zu entflechten\|folgenlos löschbar" docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md; grep -n "<" docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`

Erwartet: jeder Block hat genau einen Topf, und der zweite Befehl findet **keine** spitze Klammer mehr. Findet er eine, ist eine Vorlage nicht ausgefüllt.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md
git commit -m "Finish the live-code finding and derive the deletion order

Part 2 of the JavaFX removal is written from this, not from assumption."
```

---

## Was dieser Plan nicht tut

- Er löscht keine Produktionsdatei.
- Er fasst den `UiDispatcher`, den Lebenszyklus und die zehn Dialoge nicht an.
- Er entfernt `org.openjfx` nicht aus dem Build.

Das ist Teil 2, und der wird geschrieben, wenn der Befund aus Aufgabe 5 vorliegt.
