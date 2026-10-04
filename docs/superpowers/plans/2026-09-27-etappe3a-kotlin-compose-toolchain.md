# Etappe 3a — Kotlin, Compose und Packaging: Umsetzungsplan

> **Für agentische Bearbeiter:** ERFORDERLICHE UNTER-SKILL: superpowers:subagent-driven-development (empfohlen) oder superpowers:executing-plans, um diesen Plan Aufgabe für Aufgabe umzusetzen. Schritte nutzen Checkbox-Syntax (`- [ ]`).

**Ziel:** Kotlin und Compose Multiplatform sind im Projekt, das Packaging läuft über `compose.desktop.nativeDistributions`, und **ein echtes Fenster der Anwendung ist in Compose geschrieben**.

**Architektur:** `app-desktop` wird ein gemischtes Java/Kotlin-Modul. Die JavaFX-Oberfläche bleibt vollständig erhalten; ein einzelnes, kleines Fenster — der Operator-Profil-Auswahldialog, 145 Zeilen — wird durch eine Compose-Fassung ersetzt. Damit ist bewiesen, dass beide Toolkits im selben Prozess koexistieren, was die Voraussetzung für die Etappen 3b, 4 und 5 ist.

**Tech-Stack:** Kotlin 2.2.0, Compose Multiplatform 1.8.2, Gradle 9.7.1, Java 21. Keine Änderung an `core`.

**Spec:** `docs/superpowers/specs/2026-09-25-compose-migration-design.md`

## Warum Etappe 3 in zwei Pläne geteilt ist

Das Spec beschreibt Etappe 3 als eine Etappe: Kotlin und Compose einziehen, Packaging umstellen, die `DataTable` bauen, das Einstellungsfenster portieren. Gemessen am Code ist das mehr als Etappe 1 und 2 zusammen:

| Teil | Umfang |
|---|---|
| Einstellungsfenster | 2341 Zeilen in `Kst4ContestApplication` plus 488 in `OperatorProfileSettingsPane` |
| `DataTable`-Vorarbeit | `TableLayoutManager` 280, `MessageTextTableCell` 326, `TruncatedTextTableCell` 109, `PrivateMessageRowStyleResolver` 83, `TruncatedTextTooltipSupport` 28 |
| Toolchain | Sprache, Toolkit und Packaging auf drei Plattformen |

**Dieser Plan (3a)** liefert die Toolchain und ein kleines, echtes Compose-Fenster. **Plan 3b** liefert die `DataTable` und das Einstellungsfenster. Die Teilung folgt einer Abhängigkeit: die `DataTable` lässt sich nur an einem echten Verbraucher prüfen, und der kleinste echte Verbraucher ist ein Reiter des Einstellungsfensters. Beide gehören deshalb zusammen — die Toolchain nicht.

## Das Risiko, das dieser Plan zuerst prüft

Compose Desktop zeichnet über Skiko in ein AWT-Fenster und betreibt seinen eigenen Ereignis-Thread. JavaFX hat seinen eigenen. Ob ein Compose-Fenster aus einer laufenden JavaFX-Anwendung aufgehen kann, ohne dass eines von beiden stehen bleibt, ist **nicht belegt** — und die Etappen 3b, 4 und 5 setzen es voraus, weil sie Fenster einzeln umstellen.

Aufgabe 3 klärt das, bevor Arbeit darauf aufbaut. **Scheitert sie, endet dieser Plan dort**, und die Etappenfolge im Spec muss neu gedacht werden: dann bleibt nur ein Umschalttag, an dem die gesamte Oberfläche wechselt. Das wäre eine Änderung am Spec, nicht am Plan.

Zusätzlich gilt für den Profilwechsel: `Platform.setImplicitExit(false)` hält den Prozess am Leben, wenn alle JavaFX-Fenster zu sind. Ein offenes Compose-Fenster verlängert die Prozesslebensdauer auf eine zweite, unabhängige Weise. Aufgabe 6 prüft, dass das Beenden weiterhin über `ApplicationRuntimeLauncher.exitApplication()` geht und der Prozess wirklich endet.

## Globale Randbedingungen

- Java 21, Gradle 9.7.1. `core` wird **nicht** angefasst und bleibt frei von UI-Technologie.
- Die JavaFX-Oberfläche bleibt funktionsfähig. Diese Etappe ersetzt genau ein Fenster.
- Versionen zu Beginn von Aufgabe 1 gegen die aktuellen Veröffentlichungen prüfen; die hier genannten sind der Stand vom 2026-09-27.
- Kommentare und Javadoc auf Englisch, auch in Kotlin (KDoc). Kommunikation auf Deutsch.
- Die Invarianten aus dem Spec-Abschnitt „Invarianten" gelten unverändert, insbesondere `preferences.xml` Version 7 und das Verhalten der Operator-Profile.
- **`Platform.runLater` und `uiDispatcher.runOnUi` sind nicht dasselbe.** Der Dispatcher führt inline aus, wenn der Aufrufer schon auf dem UI-Thread ist. Wer innerhalb des UI-Threads nach hinten verschieben will, braucht `Platform.runLater` ausdrücklich. Siehe `prepareCqTextForCallsign`; das hat in Etappe 2 einen Fehler gekostet.

## Prüfschwerpunkte

Fünf Fehlerklassen, die kein Übersetzungslauf zeigt.

1. **Die beiden Toolkits blockieren sich.** Ein Compose-Fenster, das den JavaFX-Thread anhält oder umgekehrt, äußert sich als hängende Oberfläche, nicht als Ausnahme. → Aufgabe 3.
2. **Der Prozess endet nicht mehr.** Mit `Platform.setImplicitExit(false)` und einem zusätzlichen AWT-Fenster kann ein Nicht-Daemon-Thread den Prozess überleben lassen. Der Operator schließt die Anwendung und sie läuft weiter. → Aufgabe 6.
3. **Das Paket wird größer oder unvollständig.** `compose.desktop.nativeDistributions` ermittelt die JDK-Module selbst. Die in Etappe 1 mühsam ermittelte Liste — `jdk.jsobject` für die Karten-Brücke, `jdk.unsupported` für den Marlin-Renderer — kann dabei verloren gehen, und das zeigt sich erst beim Start des Pakets. → Aufgabe 5.
4. **Die Schriftgröße und das Erscheinungsbild springen.** Compose kennt die CSS-Themen des Projekts nicht. Ein Compose-Fenster neben JavaFX-Fenstern kann anders skaliert aussehen, besonders bei der einheitlichen Basis-Schriftgröße aus 1.50. → Aufgabe 6.
5. **Der Profilwechsel bricht.** Der Auswahldialog läuft im Startpfad, **vor** dem Aufbau der Hauptoberfläche (`OperatorProfileBootstrap`). Ein Compose-Fenster dort verschiebt die Reihenfolge von Toolkit-Initialisierungen. → Aufgabe 6.

---

## Aufgabe 1: Kotlin ins Projekt

**Dateien:**
- Ändern: `gradle/libs.versions.toml`
- Ändern: `app-desktop/build.gradle.kts`
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/BuildInfo.kt`
- Test: `app-desktop/src/test/kotlin/kst4contest/view/BuildInfoTest.kt`

**Schnittstellen:**
- Erzeugt: das Quellverzeichnis `app-desktop/src/main/kotlin`, von allen folgenden Aufgaben benutzt.

- [ ] **Schritt 1: Aktuelle Versionen prüfen**

Nachsehen, welche Fassungen aktuell sind:
- Kotlin (Stand 2026-09-27: **2.2.0** neueste stabile)
- Compose Multiplatform (Stand 2026-09-27: **1.8.2** neueste stabile)

**Kotlin darf hier nicht beliebig alt sein:** das Kotlin-Gradle-Plugin unterstützt Gradle 9 erst ab 2.2. Das Projekt läuft auf Gradle 9.7.1, also ist 2.2.0 die Untergrenze, nicht nur die neueste Wahl.

Die beiden müssen zueinander passen: jede Compose-Multiplatform-Fassung nennt in ihren Veröffentlichungshinweisen die Kotlin-Fassungen, mit denen sie arbeitet. Weicht etwas ab, die passende Kombination verwenden und die Abweichung im Ledger vermerken.

- [ ] **Schritt 2: Versionskatalog erweitern**

In `gradle/libs.versions.toml` unter `[versions]` ergänzen:

```toml
kotlin = "2.2.0"
composeMultiplatform = "1.8.2"
```

und unter `[plugins]`:

```toml
kotlinJvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
```

> Das Compose-Compiler-Plugin gehört seit Kotlin 2.0 zur Kotlin-Auslieferung und trägt deshalb die Kotlin-Version, nicht die von Compose. Das sind zwei verschiedene Versionsnummern für zwei verschiedene Dinge.

- [ ] **Schritt 3: Kotlin in `app-desktop` einschalten**

In `app-desktop/build.gradle.kts` den `plugins`-Block erweitern:

```kotlin
plugins {
    java
    application
    alias(libs.plugins.javafx)
    alias(libs.plugins.kotlinJvm)
}
```

und nach dem `javafx`-Block ergänzen:

```kotlin
kotlin {
    jvmToolchain(21)
}
```

- [ ] **Schritt 4: Eine Kotlin-Datei mit echtem Inhalt schreiben**

Kein leeres Gerüst: diese Datei soll etwas tun, das die Oberfläche später braucht.

`app-desktop/src/main/kotlin/kst4contest/view/BuildInfo.kt`:

```kotlin
package kst4contest.view

/**
 * Build identity shown in window titles and in the about text.
 *
 * Reads the version the Gradle build stamped into the jar manifest and falls
 * back to a marker that is obviously not a release, so an unstamped build can
 * never be mistaken for one.
 */
object BuildInfo {

    const val UNKNOWN_VERSION: String = "dev-unstamped"

    val version: String
        get() = BuildInfo::class.java.`package`?.implementationVersion ?: UNKNOWN_VERSION

    fun windowTitle(base: String): String = "$base $version"
}
```

- [ ] **Schritt 5: Den fehlschlagenden Test schreiben**

`app-desktop/src/test/kotlin/kst4contest/view/BuildInfoTest.kt`:

```kotlin
package kst4contest.view

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class BuildInfoTest {

    @Test
    fun `an unstamped build reports a version that cannot be mistaken for a release`() {
        // Tests run from class directories, where no jar manifest exists.
        assertEquals(BuildInfo.UNKNOWN_VERSION, BuildInfo.version)
        assertFalse(BuildInfo.version.first().isDigit(),
                "an unstamped version must not look like a release number")
    }

    @Test
    fun `the window title carries the version`() {
        val title = BuildInfo.windowTitle("KST4Contest")
        assertTrue(title.startsWith("KST4Contest "))
        assertTrue(title.endsWith(BuildInfo.version))
    }
}
```

- [ ] **Schritt 6: Test laufen lassen und Fehlschlag sehen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.BuildInfoTest'`
Erwartet: FAIL — Kotlin-Testquellen werden noch nicht übersetzt, oder `BuildInfo` fehlt.

- [ ] **Schritt 7: Kotlin-Testquellen einschalten**

Das Kotlin-Plugin richtet `src/test/kotlin` selbst ein. Schlägt der Lauf weiterhin fehl, prüfen:

Ausführen: `./gradlew :app-desktop:dependencies --configuration testCompileClasspath | grep -i kotlin`
Erwartet: `kotlin-stdlib` ist enthalten.

- [ ] **Schritt 8: Test laufen lassen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.BuildInfoTest'`
Erwartet: PASS, 2 Testfälle.

- [ ] **Schritt 9: Gesamtlauf**

Ausführen: `./gradlew clean build`
Erwartet: BUILD SUCCESSFUL. Vorher sind es **255** Testfälle, jetzt **257**, 0 Fehler.

Ausführen: `grep -rn "javafx" core/src | wc -l`
Erwartet: `0` — Kotlin darf `core` nicht berühren.

- [ ] **Schritt 10: Commit**

```bash
git add -A
git commit -m "Compile Kotlin in the desktop module"
```

---

## Aufgabe 2: Compose-Plugin, nur Übersetzung

Noch kein Fenster, noch kein Packaging. Nur: Compose-Code übersetzt.

**Dateien:**
- Ändern: `app-desktop/build.gradle.kts`
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/Theme.kt`

**Schnittstellen:**
- Erzeugt: `kst4contest.view.compose.Kst4ContestTheme`, von Aufgabe 4 und von Plan 3b benutzt.

- [ ] **Schritt 1: Plugins ergänzen**

```kotlin
plugins {
    java
    application
    alias(libs.plugins.javafx)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}
```

und im `dependencies`-Block:

```kotlin
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
```

- [ ] **Schritt 1b: Googles Maven-Repository ergänzen**

Compose Multiplatform holt `androidx.lifecycle` und `androidx.annotation` aus Googles Repository; auf Maven Central liegen sie nicht. Ohne diesen Eintrag scheitert schon das Auflösen des Klassenpfads mit `Could not find androidx.lifecycle:lifecycle-common`.

In `build.gradle.kts` im `repositories`-Block von `allprojects` ergänzen:

```kotlin
        google()
```

- [ ] **Schritt 2: Ein Thema schreiben, das die Vorgaben des Projekts aufnimmt**

`app-desktop/src/main/kotlin/kst4contest/view/compose/Theme.kt`:

```kotlin
package kst4contest.view.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The application theme for Compose windows.
 *
 * Release 1.50 gave the JavaFX side one base font size per operator profile.
 * Compose windows take the same value so a Compose window next to a JavaFX one
 * does not look scaled differently.
 *
 * @param darkMode the operator's configured day/evening design
 * @param baseFontSizeSp the configured base font size, in scale-independent points
 */
@Composable
fun Kst4ContestTheme(
    darkMode: Boolean,
    baseFontSizeSp: Float,
    content: @Composable () -> Unit,
) {
    val colors = if (darkMode) darkColorScheme() else lightColorScheme()
    val typography = MaterialTheme.typography.scaledTo(baseFontSizeSp)

    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
```

Die Hilfsfunktion `scaledTo` wird in Schritt 3 geschrieben.

- [ ] **Schritt 3: Die Schriftskalierung schreiben, mit Test**

Zuerst der Test, `app-desktop/src/test/kotlin/kst4contest/view/compose/TypographyScalingTest.kt`:

```kotlin
package kst4contest.view.compose

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TypographyScalingTest {

    @Test
    fun `the base size replaces the body size and the rest keeps its ratio`() {
        val original = Typography()
        val originalBody = original.bodyMedium.fontSize.value
        val originalTitle = original.titleMedium.fontSize.value

        val scaled = original.scaledTo(originalBody * 2f)

        assertEquals(originalBody * 2f, scaled.bodyMedium.fontSize.value, 0.01f)
        assertEquals(originalTitle * 2f, scaled.titleMedium.fontSize.value, 0.01f,
                "every style scales by the same factor, so the design keeps its proportions")
    }

    @Test
    fun `a base size of zero or less is ignored`() {
        val original = Typography()

        assertEquals(original.bodyMedium.fontSize.value,
                original.scaledTo(0f).bodyMedium.fontSize.value, 0.01f)
        assertEquals(original.bodyMedium.fontSize.value,
                original.scaledTo(-5f).bodyMedium.fontSize.value, 0.01f)
    }
}
```

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.TypographyScalingTest'`
Erwartet: FAIL — `scaledTo` existiert nicht.

Dann die Umsetzung, an `Theme.kt` angefügt:

```kotlin
/**
 * Scales every text style by the factor that turns the body size into
 * [targetBodySizeSp]. A value of zero or less is ignored, so a missing or
 * malformed configured size keeps the default design rather than collapsing it.
 */
internal fun Typography.scaledTo(targetBodySizeSp: Float): Typography {
    if (targetBodySizeSp <= 0f) {
        return this
    }
    val factor = targetBodySizeSp / bodyMedium.fontSize.value
    if (factor == 1f) {
        return this
    }
    fun TextStyle.scaled() = copy(fontSize = fontSize.value.times(factor).sp)
    return copy(
        displayLarge = displayLarge.scaled(), displayMedium = displayMedium.scaled(),
        displaySmall = displaySmall.scaled(), headlineLarge = headlineLarge.scaled(),
        headlineMedium = headlineMedium.scaled(), headlineSmall = headlineSmall.scaled(),
        titleLarge = titleLarge.scaled(), titleMedium = titleMedium.scaled(),
        titleSmall = titleSmall.scaled(), bodyLarge = bodyLarge.scaled(),
        bodyMedium = bodyMedium.scaled(), bodySmall = bodySmall.scaled(),
        labelLarge = labelLarge.scaled(), labelMedium = labelMedium.scaled(),
        labelSmall = labelSmall.scaled(),
    )
}
```

Die Importe `androidx.compose.material3.Typography`, `androidx.compose.ui.text.TextStyle` und `androidx.compose.ui.unit.sp` ergänzen.

- [ ] **Schritt 4: Test laufen lassen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.TypographyScalingTest'`
Erwartet: PASS, 2 Testfälle.

- [ ] **Schritt 5: Gesamtlauf**

Ausführen: `./gradlew clean build`
Erwartet: BUILD SUCCESSFUL, **259** Testfälle, 0 Fehler.

- [ ] **Schritt 6: Größenzuwachs festhalten**

Ausführen: `./gradlew :app-desktop:collectRuntime && du -sh app-desktop/build/dist-libs`

Den Wert im Ledger notieren. Vor Compose liegt er bei etwa 60 MB; Skiko bringt plattformabhängige Bibliotheken mit. Aufgabe 5 vergleicht die Paketgröße dagegen.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Compile Compose in the desktop module and carry the configured font size into it"
```

---

## Aufgabe 3: Machbarkeitsprobe — Compose-Fenster neben JavaFX

Deckt Prüfschwerpunkt 1 ab. **Diese Aufgabe entscheidet, ob die Etappenfolge 3b bis 5 tragfähig ist.**

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/CoexistenceProbe.kt` — **Wegwerfcode**, wird in Schritt 6 gelöscht

**Schnittstellen:**
- Erzeugt: nichts Dauerhaftes. Das Ergebnis ist eine Antwort, kein Code.

Compose Desktop zeichnet über Skiko in ein AWT-Fenster und betreibt einen eigenen Ereignis-Thread. JavaFX betreibt seinen. Beide in einem Prozess zu haben, ist nicht dasselbe wie beide einzeln zu haben.

- [ ] **Schritt 1: Die Probe schreiben**

`app-desktop/src/main/kotlin/kst4contest/view/compose/CoexistenceProbe.kt`:

```kotlin
package kst4contest.view.compose

import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import javafx.application.Platform

/**
 * THROWAWAY. Answers one question: can a Compose window open while a JavaFX
 * toolkit is running in the same process, with both staying responsive?
 *
 * Deleted once the answer is recorded.
 */
object CoexistenceProbe {

    @JvmStatic
    fun main(args: Array<String>) {
        Platform.startup {
            println("[probe] JavaFX toolkit started on " + Thread.currentThread().name)
        }

        // A heartbeat on the JavaFX thread. If Compose blocks it, this stops.
        val fxTicks = java.util.concurrent.atomic.AtomicInteger()
        val timer = java.util.Timer(true)
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                Platform.runLater { fxTicks.incrementAndGet() }
            }
        }, 0L, 250L)

        Thread {
            Thread.sleep(8000)
            println("[probe] fx ticks after 8 s: " + fxTicks.get() + " (expect roughly 32)")
            println("[probe] compose window still up: see whether it repaints")
        }.start()

        application {
            Window(onCloseRequest = ::exitApplication, title = "Coexistence probe") {
                Text("If this window paints and the tick count keeps rising, both toolkits live.")
            }
        }
        println("[probe] compose application() returned; fx ticks: " + fxTicks.get())
    }
}
```

- [ ] **Schritt 2: Die Probe laufen lassen**

Ausführen:
```bash
./gradlew :app-desktop:collectRuntime -q
java -cp "app-desktop/build/dist-libs/*" kst4contest.view.compose.CoexistenceProbe 2>&1 | tee /tmp/probe.log
```

Erwartet: Ein Fenster erscheint mit dem Text. Nach acht Sekunden erscheint `fx ticks after 8 s:` mit einem Wert **um 32**.

Bewertung:
- **Fenster erscheint und Ticks steigen** → beide Toolkits koexistieren. Weiter mit Schritt 3.
- **Fenster erscheint, Ticks bleiben bei 0 oder stehen** → Compose blockiert den JavaFX-Thread. Weiter mit Schritt 4.
- **Kein Fenster, Ausnahme oder Absturz** → Weiter mit Schritt 4.

- [ ] **Schritt 3: Die umgekehrte Richtung prüfen**

Der eigentliche Fall ist nicht „Compose startet JavaFX", sondern „die laufende JavaFX-Anwendung öffnet ein Compose-Fenster". Die Probe umbauen: `Platform.startup` behalten, eine JavaFX-`Stage` zeigen, und **aus einem JavaFX-Knopfdruck heraus** das Compose-Fenster öffnen.

`application { }` blockiert den aufrufenden Thread bis zum Schließen. Aus dem JavaFX-Thread darf das deshalb **nicht** direkt aufgerufen werden. Zu prüfen ist, ob ein eigener Thread mit `application { }` funktioniert, oder ob die `ComposeWindow`-Schnittstelle ohne `application { }` der richtige Weg ist.

Erwartet: Nach dem Knopfdruck erscheint das Compose-Fenster, das JavaFX-Fenster bleibt bedienbar — Knopf drückbar, Fenster verschiebbar —, und die Tickzahl steigt weiter.

Das Ergebnis samt des benutzten Wegs (`application { }` im eigenen Thread, oder `ComposeWindow` direkt) im Ledger festhalten. **Plan 3b baut darauf auf.**

- [ ] **Schritt 4: Wenn die Probe scheitert**

Dann endet dieser Plan hier. Im Ledger festhalten, was genau passiert ist, und berichten:

> Die Etappenfolge im Spec setzt voraus, dass Fenster einzeln umgestellt werden können. Wenn Compose und JavaFX nicht koexistieren, ist das nicht möglich, und es bleibt nur ein Umschalttag, an dem die gesamte Oberfläche wechselt. Das ist eine Änderung am Spec, nicht am Plan, und gehört vor jeder weiteren Arbeit entschieden.

Aufgabe 4 bis 7 **nicht** ausführen.

- [ ] **Schritt 5: Das Ergebnis festhalten**

Ins Ledger, wörtlich messbar: erscheint das Fenster, bleibt JavaFX bedienbar, wie viele Ticks nach acht Sekunden, welcher Weg funktioniert hat.

- [ ] **Schritt 6: Die Probe löschen**

```bash
git rm app-desktop/src/main/kotlin/kst4contest/view/compose/CoexistenceProbe.kt
```

Eine Probe ist eine Antwort, kein Bestandteil. Die Antwort steht im Ledger.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Record the Compose/JavaFX coexistence probe result"
```

---

## Aufgabe 4: Der Profil-Auswahldialog in Compose

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/compose/OperatorProfilePickerWindow.kt`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java` (`:6546` als Methodenverweis `OperatorProfilePickerDialog::showAndSelect`, `:6612` als direkter Aufruf)
- Löschen: `app-desktop/src/main/java/kst4contest/view/OperatorProfilePickerDialog.java` (145 Zeilen)
- Test: `app-desktop/src/test/kotlin/kst4contest/view/compose/OperatorProfilePickerStateTest.kt`

**Schnittstellen:**
- Konsumiert: `Kst4ContestTheme` aus Aufgabe 2, das Ergebnis von Aufgabe 3.
- Erzeugt: `OperatorProfilePickerWindow.showAndSelect(profiles, preselectedId)` mit **derselben Signatur und demselben Rückgabetyp** wie `OperatorProfilePickerDialog.showAndSelect`. Der Name bleibt, weil `Kst4ContestApplication:6546` ihn als Methodenverweis `::showAndSelect` benutzt — eine abweichende Signatur bricht dort nicht sichtbar, sondern erst beim Übersetzen an einer unerwarteten Stelle.

Dieses Fenster ist mit Bedacht gewählt: 145 Zeilen, in sich abgeschlossen, und es läuft **vor** dem Aufbau der Hauptoberfläche. Es ist damit der einzige Ort, an dem ein Compose-Fenster ohne gleichzeitig offene JavaFX-Fenster steht — der einfachste mögliche Anfang.

- [ ] **Schritt 1: Den bisherigen Vertrag ablesen**

Ausführen:
```bash
sed -n '1,60p' app-desktop/src/main/java/kst4contest/view/OperatorProfilePickerDialog.java
grep -rn "OperatorProfilePickerDialog" app-desktop/src core/src
```

Notieren: Signatur, Rückgabewert bei Abbruch, und wer den Dialog aufruft. Der neue Weg muss **denselben** Vertrag erfüllen, insbesondere bei Abbruch: laut `PROJECT_CONTEXT.md` fragt ein Start mit keinem oder genau einem Profil gar nichts und schreibt nichts.

- [ ] **Schritt 2: Den Zustand von der Darstellung trennen, mit Test**

Die Auswahllogik gehört in eine testbare Klasse ohne Compose-Bezug. Zuerst der Test:

`app-desktop/src/test/kotlin/kst4contest/view/compose/OperatorProfilePickerStateTest.kt`:

```kotlin
package kst4contest.view.compose

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class OperatorProfilePickerStateTest {

    @Test
    fun `the preselected profile is selected when it is in the list`() {
        val state = OperatorProfilePickerState(listOf("root", "contest"), preselectedId = "contest")
        assertEquals("contest", state.selectedId)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `an unknown preselection falls back to the first profile`() {
        val state = OperatorProfilePickerState(listOf("root", "contest"), preselectedId = "gone")
        assertEquals("root", state.selectedId,
                "a profile that disappeared must not leave the dialog unusable")
    }

    @Test
    fun `an empty list cannot be confirmed`() {
        val state = OperatorProfilePickerState(emptyList(), preselectedId = null)
        assertNull(state.selectedId)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `selecting changes the selection`() {
        val state = OperatorProfilePickerState(listOf("root", "contest"), preselectedId = "root")
        state.select("contest")
        assertEquals("contest", state.selectedId)
    }

    @Test
    fun `selecting something absent is ignored`() {
        val state = OperatorProfilePickerState(listOf("root"), preselectedId = "root")
        state.select("nope")
        assertEquals("root", state.selectedId, "an unknown id must not clear the selection")
    }
}
```

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.OperatorProfilePickerStateTest'`
Erwartet: FAIL — `OperatorProfilePickerState` existiert nicht.

- [ ] **Schritt 3: Den Zustand schreiben**

In `OperatorProfilePickerWindow.kt`:

```kotlin
package kst4contest.view.compose

/**
 * Selection state of the operator profile picker, free of Compose so the rules
 * are testable without a toolkit.
 */
class OperatorProfilePickerState(
    val profileIds: List<String>,
    preselectedId: String?,
) {
    var selectedId: String? = preselectedId.takeIf { profileIds.contains(it) } ?: profileIds.firstOrNull()
        private set

    val canConfirm: Boolean
        get() = selectedId != null

    /** Ignores an id that is not in the list: a vanished profile must not clear the selection. */
    fun select(id: String) {
        if (profileIds.contains(id)) {
            selectedId = id
        }
    }
}
```

- [ ] **Schritt 4: Test laufen lassen**

Ausführen: `./gradlew :app-desktop:test --tests 'kst4contest.view.compose.OperatorProfilePickerStateTest'`
Erwartet: PASS, 5 Testfälle.

- [ ] **Schritt 5: Das Fenster schreiben**

Den in Aufgabe 3 Schritt 3 ermittelten Weg benutzen. Der Aufruf ist blockierend und liefert die Wahl oder `null` bei Abbruch — dieselbe Form wie bisher, damit `OperatorProfileChoiceRequester` sich nicht ändert.

Das Fenster nutzt `Kst4ContestTheme` mit dem konfigurierten Dunkelmodus und der Basis-Schriftgröße. Woher beide zu diesem frühen Zeitpunkt kommen, ist zu klären: der Dialog läuft, bevor ein Profil gewählt ist, also bevor dessen `preferences.xml` gelesen wurde. Zu benutzen sind die Werte des Wurzelprofils; ist auch das nicht lesbar, die Vorgaben. Diese Entscheidung als `Ruling:` festhalten.

- [ ] **Schritt 6: Den Aufrufer umstellen und den alten Dialog löschen**

```bash
git rm app-desktop/src/main/java/kst4contest/view/OperatorProfilePickerDialog.java
```

Beide Aufrufstellen in `Kst4ContestApplication` umstellen: den Methodenverweis `:6546` und den direkten Aufruf `:6612`. Bleibt die Signatur gleich, genügt der Austausch des Klassennamens.

Ausführen: `grep -rn "OperatorProfilePickerDialog" app-desktop/src core/src`
Erwartet: keine Ausgabe.

- [ ] **Schritt 7: Übersetzen und testen**

Ausführen: `./gradlew clean build`
Erwartet: BUILD SUCCESSFUL, **264** Testfälle, 0 Fehler.

- [ ] **Schritt 8: Den Dialog wirklich sehen**

Der Dialog erscheint nur, wenn mehr als ein Operator-Profil vorhanden ist. Ein zweites anlegen, falls nötig — im laufenden Programm über **Einstellungen → Profiles**, damit `profiles.xml` auf dem vorgesehenen Weg entsteht und nicht von Hand.

Dann: `./gradlew :app-desktop:run`

Erwartet: Das Compose-Fenster erscheint, listet beide Profile, die Vorauswahl steht auf dem zuletzt benutzten. Auswählen und bestätigen startet die Anwendung mit diesem Profil. Abbrechen verhält sich wie zuvor.

- [ ] **Schritt 9: Commit**

```bash
git add -A
git commit -m "Pick the operator profile in a Compose window"
```

---

## Aufgabe 5: Packaging auf `compose.desktop.nativeDistributions`

Deckt Prüfschwerpunkt 3 ab.

**Dateien:**
- Ändern: `app-desktop/build.gradle.kts`
- Ändern: `gradle.properties`
- Ändern: `.github/workflows/nightly-artifacts.yml`, `.github/workflows/tagged-release.yml`, `.github/workflows/pr-compile-check.yml`
- Ändern: `packaging/macos/build-signed-dmg.sh`, `packaging/aur/kst4contest/PKGBUILD`, `packaging/aur/kst4contest-git/PKGBUILD`

**Schnittstellen:**
- Konsumiert: nichts.
- Erzeugt: die Gradle-Aufgaben des Compose-Plugins (`packageDistributionForCurrentOS`, `createDistributable`) an der Stelle von `packageImage`.

Etappe 1 hat die Modulliste in `gradle.properties` unter `jpackageAddModules` hinterlegt, weil `AddModules.java` entfiel. Zwei Einträge darin wurden **teuer erkauft**: `jdk.jsobject` für die Karten-Brücke und `jdk.unsupported`, ohne das der Marlin-Renderer an `sun/misc/Unsafe` stirbt. Das Compose-Plugin ermittelt Module selbst — deshalb ist die erste Frage dieser Aufgabe, ob beide noch enthalten sind.

- [ ] **Schritt 1: Die heutigen Werte festhalten**

Ausführen:
```bash
grep -n "^jpackageAddModules" gradle.properties
./gradlew :app-desktop:packageImage -q
du -sh app-desktop/build/jpackage/praktiKST
grep -o "MODULES=.*" app-desktop/build/jpackage/praktiKST/lib/runtime/release | tr ' ' '\n' | sort > /tmp/module-liste-vorher.txt
wc -l < /tmp/module-liste-vorher.txt
```

Paketgröße und Modulzahl im Ledger notieren. Sie sind der Vergleichsmaßstab.

- [ ] **Schritt 2: `nativeDistributions` einrichten**

An `app-desktop/build.gradle.kts` anfügen:

```kotlin
compose.desktop {
    application {
        mainClass = "kst4contest.view.Main"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
            )
            packageName = "praktiKST"
            packageVersion = providers.gradleProperty("composePackageVersion").get()

            /*
             * Kept from the former module-info.java, minus the javafx.* modules.
             * jdk.jsobject carries netscape.javascript for the map bridge, and
             * jdk.unsupported carries sun.misc.Unsafe, without which the JavaFX
             * Marlin renderer fails to start. Neither is inferred: JavaFX comes
             * from the classpath, so nothing declares them.
             */
            modules(
                "java.desktop", "java.net.http", "java.sql",
                "jdk.crypto.ec", "jdk.jsobject", "jdk.net",
                "jdk.xml.dom", "jdk.unsupported",
            )

            linux { iconFile.set(rootProject.file("packaging/icons/kst4contest.png")) }
            windows { iconFile.set(rootProject.file("packaging/icons/kst4contest.ico")) }
            macOS {
                iconFile.set(rootProject.file("packaging/icons/kst4contest.icns"))
                bundleID = "de.x08.KST4Contest"
            }
        }
    }
}
```

> `mainClass` ist `kst4contest.view.Main`, nicht `Kst4ContestApplication`. Der Grund steht in Etappe 1: eine `Application`-Unterklasse startet vom Klassenpfad nicht.

> `bundleID` muss `de.x08.KST4Contest` bleiben — `packaging/macos/build-signed-dmg.sh` signiert dagegen, und ein geänderter Wert bricht die Signatur.

- [ ] **Schritt 3: Die Paketversion bereitstellen**

`nativeDistributions` verlangt eine Version nach den Regeln des Zielformats: MSI und DMG akzeptieren **kein** `1.50.0-nightly`, sondern nur `MAJOR.MINOR.PATCH` mit Zahlen.

In `gradle.properties` ergänzen:

```properties
# nativeDistributions requires MAJOR.MINOR.PATCH with digits only. MSI and DMG
# reject a qualifier, so the nightly suffix from `version` cannot be used here.
composePackageVersion=1.50.0
```

Ausführen: `grep -n "^version=\|^composePackageVersion=" gradle.properties`
Erwartet: beide Zeilen; `version` behält den Zusatz `-nightly`, `composePackageVersion` nicht.

- [ ] **Schritt 4: Lokal paketieren und vergleichen**

Ausführen:
```bash
./gradlew :app-desktop:createDistributable -q
find app-desktop/build/compose/binaries -maxdepth 4 -name "praktiKST" -type d
```

Die Modulliste des neuen Runtime gegen die alte prüfen:
```bash
NEW=$(find app-desktop/build/compose/binaries -name release -path "*runtime*" | head -1)
grep -o "MODULES=.*" "$NEW" | tr ' ' '\n' | sort > /tmp/module-liste-nachher.txt
diff /tmp/module-liste-vorher.txt /tmp/module-liste-nachher.txt
```

Erwartet: `jdk.jsobject` und `jdk.unsupported` sind **enthalten**. Fehlt einer, ist die `modules(...)`-Angabe aus Schritt 2 nicht angekommen — nicht weitergehen, bis sie drin ist.

- [ ] **Schritt 5: Das Paket starten**

Ausführen: den Launcher unter `app-desktop/build/compose/binaries/main/app/praktiKST/bin/praktiKST`

Erwartet: Das Fenster erscheint. Im Protokoll **kein** `NoClassDefFoundError: sun/misc/Unsafe`, **kein** Fehler zu `JSObject`. Das Kartenfenster öffnen und Kacheln sehen — das ist die Prüfung auf `jdk.jsobject`.

- [ ] **Schritt 6: Größe vergleichen**

Ausführen: `du -sh app-desktop/build/compose/binaries/main/app/praktiKST`

Gegen den Wert aus Schritt 1 stellen und beide ins Ledger. Ein Zuwachs ist zu erwarten: Skiko bringt eigene Bibliotheken mit. Wächst das Paket um mehr als etwa 100 MB, im Ledger festhalten und berichten — die Download-Größe ist für Anwender im Feld ein echtes Thema.

- [ ] **Schritt 7: Die CI umstellen**

In den Workflows die Aufrufe von `:app-desktop:collectRuntime` plus dem folgenden `jpackage`-Block ersetzen durch die passende Compose-Aufgabe:

| Job | Aufgabe |
|---|---|
| AppImage | `:app-desktop:createDistributable`, dann das bestehende AppDir-Verfahren auf dem neuen Pfad |
| Deb | `:app-desktop:packageDeb` |
| Rpm | `:app-desktop:packageRpm` |
| Windows ZIP | `:app-desktop:createDistributable`, dann ZIP wie bisher |
| macOS DMG | `:app-desktop:createDistributable`, danach `build-signed-dmg.sh` |
| Arch, Flatpak | `:app-desktop:createDistributable` |

Der Ausgabepfad wechselt von `app-desktop/build/jpackage/praktiKST` auf `app-desktop/build/compose/binaries/main/app/praktiKST`. Vor dem Bearbeiten den echten Pfad aus Schritt 4 ablesen und nicht aus diesem Plan übernehmen — er hängt von der Plugin-Fassung ab.

`--add-modules`-Argumente und die `ADD_MODULES`-Zeilen entfallen in der CI: die Modulliste steht jetzt in `build.gradle.kts`. `jpackageAddModules` in `gradle.properties` wird gelöscht, **nachdem** alle Verwendungen weg sind.

Ausführen: `grep -rn "jpackageAddModules\|collectRuntime\|--add-modules" .github/ packaging/`
Erwartet: keine Ausgabe.

- [ ] **Schritt 8: Die Linux-Jobs lokal prüfen**

Wie in Etappe 1, mit `act`:

```bash
act -j build-linux-appimage -W .github/workflows/nightly-artifacts.yml
act -j build-linux-deb      -W .github/workflows/nightly-artifacts.yml
act -j build-linux-rpm      -W .github/workflows/nightly-artifacts.yml
act -j build-linux-arch     -W .github/workflows/nightly-artifacts.yml
act --privileged --pull=false -j build-flatpak -W .github/workflows/nightly-artifacts.yml
```

Erwartet: Alle Bauschritte erfolgreich. Der jeweils letzte Schritt — Artefakt-Upload beziehungsweise Flatpak-Signierschlüssel — scheitert an `act`, nicht am Build; das war in Etappe 1 genauso.

- [ ] **Schritt 9: Windows und macOS**

Beide sind lokal nur eingeschränkt prüfbar. Der Weg aus Etappe 1 gilt weiter: WinBoat für Windows, ein erreichbarer Mac für macOS. Die Modulprüfung aus Schritt 4 und der Start aus Schritt 5 sind auf **jeder** Plattform zu wiederholen — `jdk.unsupported` fiel in Etappe 1 erst beim Start auf, nicht beim Bauen.

Die **MSI-Strecke mit WiX** ist bis heute nie gelaufen. `packageMsi` ist neu und hier der Punkt mit dem größten Restrisiko.

- [ ] **Schritt 10: Commit**

```bash
git add -A
git commit -m "Package through compose.desktop.nativeDistributions"
```

---

## Aufgabe 6: Lebensdauer, Beenden und Erscheinungsbild

Deckt die Prüfschwerpunkte 2, 4 und 5 ab.

**Dateien:**
- Ändern: `app-desktop/src/main/java/kst4contest/view/ApplicationRuntimeLauncher.java`, falls nötig
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/OperatorProfilePickerWindow.kt`, falls nötig

- [ ] **Schritt 1: Beenden nach dem Profil-Dialog prüfen**

`Platform.setImplicitExit(false)` hält den Prozess am Leben, wenn alle JavaFX-Fenster zu sind; alle Ausgänge laufen über `ApplicationRuntimeLauncher.exitApplication()`. Ein AWT-Fenster von Compose bringt eine zweite, unabhängige Ursache für einen laufenden Prozess mit.

Ausführen: Anwendung starten, Profil wählen, Anwendung über das Fenster-Kreuz schließen.

```bash
pgrep -f Kst4ContestApplication || echo "Prozess beendet"
```
Erwartet: `Prozess beendet`.

Dasselbe für den Abbruch im Profil-Dialog: Dialog abbrechen, dann prüfen.
Erwartet: `Prozess beendet`.

Bleibt der Prozess stehen, ist es ein Nicht-Daemon-Thread von AWT. Im Ledger festhalten, welcher — `jstack` zeigt es — und den Compose-Fensterweg so ändern, dass er beim Schließen aufräumt.

- [ ] **Schritt 2: Profilwechsel im laufenden Betrieb**

Ausführen: Anwendung starten, **Einstellungen → Profiles**, auf ein anderes Profil wechseln.

Erwartet: Der Wechsel funktioniert wie vor dieser Etappe. Die Stationsliste des neuen Profils füllt sich, keine doppelten Listener, der Prozess läuft weiter. Laut `PROJECT_CONTEXT.md` reißt ein Wechsel die Laufzeit ab und baut eine neue `Kst4ContestApplication`; das Compose-Fenster des Dialogs darf davon nicht betroffen sein, weil es zu diesem Zeitpunkt längst geschlossen ist.

- [ ] **Schritt 3: Erscheinungsbild vergleichen**

Zwei Profile mit **unterschiedlicher** Basis-Schriftgröße anlegen, dann jeweils starten.

Erwartet: Die Schrift im Compose-Dialog folgt der Größe des Wurzelprofils (die Entscheidung aus Aufgabe 4 Schritt 5) und wirkt neben den JavaFX-Fenstern nicht auffällig größer oder kleiner. Der Dunkelmodus stimmt mit der Vorgabe überein.

Weicht es sichtbar ab, ist das ein Befund für Plan 3b — dort entsteht das Einstellungsfenster, das die Werte kennt. Im Ledger festhalten, nicht hier reparieren.

- [ ] **Schritt 4: Commit**

```bash
git add -A
git commit -m "Keep process lifetime and appearance intact with a Compose window in the mix"
```

---

## Abnahme der Etappe 3a

- [ ] `./gradlew clean build` läuft durch, mindestens **264** Testfälle, 0 Fehler.
- [ ] `grep -rn "javafx" core/src` liefert keine Ausgabe — `core` blieb unberührt.
- [ ] Das Ergebnis der Koexistenzprobe steht im Ledger, mit dem benutzten Weg.
- [ ] Der Profil-Auswahldialog ist in Compose, `OperatorProfilePickerDialog.java` existiert nicht mehr.
- [ ] `createDistributable` erzeugt ein startfähiges Paket, dessen Runtime `jdk.jsobject` und `jdk.unsupported` enthält.
- [ ] Das Kartenfenster zeigt im gepackten Stand Kacheln — die Prüfung auf `jdk.jsobject`.
- [ ] Alle fünf Linux-Jobs laufen unter `act` bis zum Upload-Schritt durch.
- [ ] Der Prozess endet nach dem Schließen und nach einem Abbruch im Profil-Dialog.
- [ ] Der Profilwechsel im laufenden Betrieb funktioniert unverändert.
- [ ] Paketgröße vorher und nachher stehen im Ledger.

## Was danach kommt

**Plan 3b: `DataTable` und Einstellungsfenster.** Er wird geschrieben, wenn dieser Plan durch ist — und zwar aus zwei Gründen erst dann. Der Zuschnitt der `DataTable` hängt am Ergebnis der Koexistenzprobe, und die Erfahrung aus Etappe 2 gehört hinein:

- Ein Spiegel, der die ganze Liste ersetzt, kostet Auswahl und Scrollposition. In Compose ist der Hebel dagegen ein **stabiler Schlüssel** je Zeile, nicht ein Gleichheitsvergleich.
- Sortierung wird aus der kanonischen Liste abgeleitet, nie aus dem vorher Angezeigten.
- Fokus ist ausdrücklich zu führen. Zwei der drei Fehler aus Etappe 2 waren Fokusfehler, und keiner war durch Lesen zu finden.
- Ein Sammelvorgang ist **eine** Zustandsänderung, nicht eine je Element.
