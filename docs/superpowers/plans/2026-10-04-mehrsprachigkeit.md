# Umsetzungsplan — die Oberfläche in zwei Sprachen

> **Für ausführende Agenten:** Schritte sind als Kästchen (`- [ ]`) geführt. Nach jeder Aufgabe
> ein Commit und ein `./gradlew clean build` mit Auswertung der Test-XML. Nach der mit
> **GUI-ABNAHME** markierten Aufgabe die Anwendung starten und von Marc bedienen lassen.

**Ziel:** Die Mechanik für eine mehrsprachige Oberfläche, mit dreizehn Texten als Beweis. Die
übrigen rund 380 sind danach mechanische Folgearbeit ohne Architekturentscheidung.

**Architektur:** Zwei `.properties`-Dateien sind die Quelle, die niemand kompilieren muss. Ein
eigenes Bauwerkzeug-Modul erzeugt daraus Kotlin mit **typisierten Zugriffsmethoden**, womit
Tippfehler *und* falsche Platzhalterzahl zu Übersetzungsfehlern werden. Ein `LanguageStore`
hält die Sprache als Compose-Zustand — dieselbe Bauform wie der `PaletteStore` aus Etappe 8 —
und verteilt sie an Compose über ein `CompositionLocal` und an die Java-Aufrufstellen über ein
`@Volatile`-Feld.

**Werkzeuge:** Java 21, Kotlin 2.2, Compose Multiplatform 1.8.2, JUnit 5, Gradle Wrapper.
`java.util.Properties` und `java.text.MessageFormat` — beides JDK.

**Spezifikation:** `docs/superpowers/specs/2026-10-04-mehrsprachigkeit-design.md`.
**Übergeordnet:** `docs/superpowers/specs/2026-09-25-compose-migration-design.md`, Etappe 9.

## Projektweite Vorgaben

- Kommunikation mit Marc auf **Deutsch**; Quelltextkommentare und Javadoc **ausschließlich
  Englisch**, auch in dieser Etappe. Commit-Nachrichten knappes Englisch.
- `./gradlew clean build`, Java 21. **Nicht aus Exit-Code 0 auf grün schließen** — die XML
  unter `*/build/test-results/test/TEST-*.xml` auswerten. Grundlinie: **969 Tests, 3
  übersprungen, 0 Fehler**.
- **Immer mit Pfadangabe committen** (`git commit -- <pfade>`). Dieser Arbeitsbaum trägt
  vorbestehende Änderungen von vor der Sitzung: `.gitignore` geändert, die Löschung von
  `0001-Document-v1.43-and-fix-release-metadata.patch` gestapelt, `AGENT_HANDOVER.md` und
  `docs/superpowers/UEBERGABE-2026-09-28.md` gelöscht, `docs/superpowers/UEBERGABE-2026-10-02.md`
  unverfolgt. **Nichts davon darf in einen Commit geraten.**
- **Signierte Commits.** Schlägt das Signieren mit `Couldn't get agent socket` fehl, ist der
  Bitwarden-SSH-Agent gesperrt — Marc entsperren lassen, **nicht** unsigniert committen.
- Kein Push, kein Merge, kein Tag, kein Release. **Keine neue Abhängigkeit, weder im Jar noch
  im Bau** — das ist in dieser Etappe eine Entwurfsentscheidung, nicht nur eine Hausregel.
- `core` bekommt **keine** Nachschlagefunktion. Es erzeugt keinen Anzeigetext.
- **Die Reiterreihenfolge im Einstellungsfenster ändert sich nicht.** Die Sprachwahl kommt in
  den bestehenden GUI-Reiter.
- **Nicht anfassen:** `bcn_beaconTextMainCat`, `bcn_beaconTextSecondCat`,
  `messageHandling_autoAnswerTextMainCat`, `messageHandling_autoAnswerTextSecondCat`, Schnipsel
  und Kurztasten. Sie stehen in der gespeicherten XML jedes Operateurs.

## Prüfschwerpunkte

Sechs Dinge, die die Spezifikation voraussetzt, aber nicht als Test benennt. Jede Zeile bekommt
ihren Test in der Aufgabe, der der Code gehört. Die ersten drei sind beim Schreiben dieses Plans
gemessen worden.

1. **Ein Text mit `$`, `"` oder `\` muss im erzeugten Kotlin überleben.** Der Erzeuger schreibt
   Kotlin-Quelltext; `$` ist dort das Vorlagenzeichen. Eine Übersetzung mit `$` erzeugt sonst
   Quelltext, der nicht übersetzt — oder schlimmer, einen, der etwas anderes bedeutet. → Aufgabe 3.
2. **Ein doppelter Schlüssel in einer Datei darf nicht lautlos gewinnen.** `java.util.Properties`
   behält den letzten ohne ein Wort. Ein Beitragender, der einen Schlüssel zweimal übersetzt,
   verliert sonst eine seiner beiden Übersetzungen und erfährt es nie. → Aufgabe 2.
3. **`buildSrc`-Tests laufen bei `./gradlew build` nicht mit** — gemessen. Deshalb ist der
   Erzeuger ein eigenes Modul und kein Bauskript, und deshalb muss nachgeprüft werden, dass seine
   Tests wirklich in der XML-Zählung auftauchen. → Aufgabe 4.
4. **Platzhalter zählen heißt den höchsten Index finden, nicht die Vorkommen.** `"{1} {1}"` hat
   zwei Vorkommen und braucht zwei Argumente, `"{0} {0}"` zwei Vorkommen und eines. Falsch
   gezählt verschluckt `MessageFormat` Daten oder wirft. → Aufgabe 1.
5. **Zwei Schlüssel dürfen nicht auf denselben Kotlin-Namen fallen.** `gui.startupDesign` und
   `gui.startup_design` würden beide zu `guiStartupDesign`. Das ergäbe Quelltext, der nicht
   übersetzt — mit einer Meldung des Kotlin-Compilers, die den Beitragenden nichts sagt. → A1.
6. **Eine dritte Sprache darf den Bau nicht brechen.** Deutsch wird auf Vollständigkeit
   geprüft, eine beigetragene Sprache nicht. Der Vollständigkeitstest darf nicht über jede
   neue Datei stolpern. → Aufgabe 8.

---

## Dateiübersicht

**Neu: das Bauwerkzeug-Modul** (wird nicht ausgeliefert):

| Datei | Verantwortung |
|---|---|
`settings.gradle.kts` | `include(":i18n-generator")` ergänzen |
`i18n-generator/build.gradle.kts` | Kotlin-Modul, nur JUnit |
`i18n-generator/src/main/kotlin/kst4contest/i18n/TranslationRules.kt` | die Regeln als reine Funktionen |
`i18n-generator/src/main/kotlin/kst4contest/i18n/TranslationFiles.kt` | Lesen, doppelte Schlüssel, unlesbare Datei |
`i18n-generator/src/main/kotlin/kst4contest/i18n/StringsSource.kt` | der Kotlin-Erzeuger samt Maskierung |
`i18n-generator/src/main/kotlin/kst4contest/i18n/GeneratorMain.kt` | das Hauptprogramm, das Gradle aufruft |
dazu drei Testdateien | |

**Neu in `app-desktop`:**

| Datei | Verantwortung |
|---|---|
`src/main/i18n/strings_en.properties` | die Grundlage, englisch |
`src/main/i18n/strings_de.properties` | die deutsche Übersetzung |
`.../view/i18n/LanguageStore.kt` | die Sprache als Compose-Zustand, plus `LocalStrings` |
`.../view/i18n/LocaleChoice.kt` | `localeFor(stored, systemDefault)` als reine Funktion |
`.../view/i18n/CurrentStrings.kt` | das `@Volatile`-Feld für die Java-Aufrufstellen |
dazu fünf Testdateien | |

**Geändert:** `app-desktop/build.gradle.kts` (Erzeuger-Aufgabe und Quellverzeichnis),
`ChatPreferences.java` (eine Einstellung), `GuiOptionsTabState.kt` und `GuiOptionsTab.kt` (die
Sprachwahl), `ComposeWindowHost.kt` (`LocalStrings` bereitstellen),
`Kst4ContestApplication.java` (Store beim Start bilden), `ConnectionBar.kt`,
`MainMenuModel.kt`, `ComposeAlert.kt` (die Beweistexte), `CLAUDE.md` und
`docs/PROJECT_CONTEXT.md` (zwei Module werden drei), beide Handbücher.

---

## Aufgabe 1: Die Regeln als reine Funktionen

Das Herz, und der Teil, der falsch sein kann, ohne dass es auffällt.

**Dateien:**
- Anlegen: `i18n-generator/src/main/kotlin/kst4contest/i18n/TranslationRules.kt`
- Anlegen: `i18n-generator/src/test/kotlin/kst4contest/i18n/TranslationRulesTest.kt`
- Anlegen: `i18n-generator/build.gradle.kts`
- Ändern: `settings.gradle.kts`

**Schnittstellen:**
- Liefert: `fun missingKeys(base: Set<String>, other: Set<String>): Set<String>`
- Liefert: `fun orphanKeys(base: Set<String>, other: Set<String>): Set<String>`
- Liefert: `fun argumentCount(text: String): Int` — **der höchste Platzhalterindex plus eins**,
  nicht die Zahl der Vorkommen
- Liefert: `fun identifierFor(key: String): String`
- Liefert: `fun identifierCollisions(keys: Collection<String>): Map<String, List<String>>`

- [ ] **Schritt 1: Das Modul anlegen**

`settings.gradle.kts` um eine Zeile ergänzen, neben den bestehenden `include`-Aufrufen:

```kotlin
/*
 * Build tooling only: this module generates the Kotlin for the translation files and is
 * never shipped. It is a module rather than buildSrc because buildSrc's tests do not run
 * on `./gradlew build` -- measured -- and a generator whose tests never run is the exact
 * failure it exists to prevent.
 */
include(":i18n-generator")
```

`i18n-generator/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.junit.jupiter)
    // Gradle 9 no longer adds the launcher implicitly.
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
```

Keine Produktionsabhängigkeit: das Modul benutzt nur `java.util.Properties` und
`java.text.MessageFormat` aus dem JDK.

- [ ] **Schritt 2: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The rules that decide whether a set of translations is usable.
 *
 * Pure functions, and therefore the part that can be wrong without anyone noticing: a build
 * break that never breaks looks exactly like a clean project. Every rule here has a failure
 * that reaches an operator -- a lost translation, a swallowed argument, or Kotlin that does
 * not compile with a message a contributor cannot read.
 */
class TranslationRulesTest {

    @Test
    fun whatTheBaseHasAndATranslationLacksIsMissing() {
        assertEquals(
            setOf("b"),
            missingKeys(base = setOf("a", "b"), other = setOf("a")),
        )
    }

    @Test
    fun whatATranslationHasAndTheBaseLacksIsAnOrphan() {
        // A contributor's typo. Silently keeping it would mean a translation nothing uses.
        assertEquals(
            setOf("typo"),
            orphanKeys(base = setOf("a"), other = setOf("a", "typo")),
        )
    }

    @Test
    fun aTextWithoutPlaceholdersNeedsNoArguments() {
        assertEquals(0, argumentCount("Save settings"))
        assertEquals(0, argumentCount(""))
    }

    @Test
    fun theArgumentCountIsTheHighestIndexPlusOneAndNotTheNumberOfOccurrences() {
        /*
         * The trap. "{1} {1}" occurs twice and needs two arguments, because MessageFormat
         * addresses them by index; "{0} {0}" occurs twice and needs one. Counting occurrences
         * makes the first case throw and the second swallow an argument.
         */
        assertEquals(1, argumentCount("Connect to {0}"))
        assertEquals(1, argumentCount("{0} and {0} again"))
        assertEquals(2, argumentCount("{1} {1}"))
        assertEquals(3, argumentCount("{2} {0}"))
    }

    @Test
    fun aFormattedPlaceholderStillCounts() {
        // MessageFormat allows {0,number} and {1,date,short}; both are arguments.
        assertEquals(1, argumentCount("Seen {0,number} times"))
        assertEquals(2, argumentCount("{0} at {1,date,short}"))
    }

    @Test
    fun somethingThatMerelyLooksLikeAPlaceholderIsNotOne() {
        // Braces appear in real text. Only a leading digit makes it an argument.
        assertEquals(0, argumentCount("Press {Ctrl} to stop"))
        assertEquals(0, argumentCount("100 {}"))
    }

    @Test
    fun aDottedKeyBecomesALowerCamelCaseIdentifier() {
        assertEquals("settingsSave", identifierFor("settings.save"))
        assertEquals("menuFile", identifierFor("menu.file"))
        assertEquals("guiBandColumnHints", identifierFor("gui.bandColumnHints"))
    }

    @Test
    fun aKeyWithUnderscoresOrDashesBecomesOneIdentifierToo() {
        assertEquals("guiStartupDesign", identifierFor("gui.startup_design"))
        assertEquals("guiStartupDesign", identifierFor("gui.startup-design"))
    }

    @Test
    fun anIdentifierNeverStartsWithADigit() {
        // A key may legitimately start with a number; a Kotlin identifier may not.
        assertTrue(identifierFor("2.metres").first().isLetter())
    }

    @Test
    fun twoKeysCollapsingToOneIdentifierAreReported() {
        /*
         * gui.startupDesign and gui.startup_design both become guiStartupDesign. Generating
         * both would produce Kotlin that does not compile, and the compiler's message names
         * the generated file -- which tells a contributor nothing about their properties file.
         */
        val collisions = identifierCollisions(listOf("gui.startupDesign", "gui.startup_design"))

        assertEquals(1, collisions.size)
        assertEquals(
            listOf("gui.startupDesign", "gui.startup_design"),
            collisions.getValue("guiStartupDesign").sorted(),
        )
    }

    @Test
    fun distinctKeysReportNoCollision() {
        assertTrue(identifierCollisions(listOf("a.b", "c.d")).isEmpty())
    }
}
```

- [ ] **Schritt 3: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :i18n-generator:test
```

Erwartung: Übersetzungsfehler, `missingKeys` existiert nicht.

- [ ] **Schritt 4: Die Umsetzung schreiben**

```kotlin
package kst4contest.i18n

/**
 * The rules that decide whether a set of translations is usable.
 *
 * Pure functions over key sets and texts, with no file and no Gradle in sight, so the part
 * that can be quietly wrong is the part that is tested.
 */

/** Keys the base has and a translation does not. Allowed: they fall back to the base. */
fun missingKeys(base: Set<String>, other: Set<String>): Set<String> = base - other

/** Keys a translation has and the base does not. Not allowed: a contributor's typo. */
fun orphanKeys(base: Set<String>, other: Set<String>): Set<String> = other - base

/**
 * How many arguments a text needs.
 *
 * The highest placeholder index plus one, and deliberately not the number of occurrences:
 * `MessageFormat` addresses arguments by index, so `{1} {1}` needs two and `{0} {0}` needs
 * one. Counting occurrences makes the first throw and the second swallow an argument.
 *
 * A brace followed by anything but a digit is not a placeholder -- braces occur in real
 * interface text.
 */
fun argumentCount(text: String): Int {
    val indices = PLACEHOLDER.findAll(text).map { it.groupValues[1].toInt() }
    return (indices.maxOrNull() ?: -1) + 1
}

/**
 * The Kotlin identifier for a key.
 *
 * `settings.save` becomes `settingsSave`. Dots, underscores and dashes all separate words,
 * because a contributor will use whichever they are used to and the generated name must not
 * depend on which.
 */
fun identifierFor(key: String): String {
    val words = key.split('.', '_', '-').filter { it.isNotEmpty() }

    val joined = words.mapIndexed { index, word ->
        if (index == 0) word.replaceFirstChar { it.lowercaseChar() }
        else word.replaceFirstChar { it.uppercaseChar() }
    }.joinToString("")

    /* A key may start with a digit; a Kotlin identifier may not. */
    return if (joined.firstOrNull()?.isDigit() == true) "key$joined" else joined
}

/**
 * Keys that would produce the same identifier, grouped by that identifier.
 *
 * Generating both would emit Kotlin that does not compile, and the compiler's complaint names
 * the generated file -- which tells a contributor nothing about the line they wrote.
 */
fun identifierCollisions(keys: Collection<String>): Map<String, List<String>> =
    keys.groupBy { identifierFor(it) }.filterValues { it.size > 1 }

/** `{0}`, `{12}`, `{0,number}` -- a brace, digits, then either a brace or a comma. */
private val PLACEHOLDER = Regex("""\{(\d+)\s*[,}]""")
```

**Achtung bei der Regex:** `"{0}"` muss passen und `"{Ctrl}"` nicht. Der Test
`somethingThatMerelyLooksLikeAPlaceholderIsNotOne` ist die Probe darauf; schlägt er fehl, ist
die Regex zu großzügig und nicht der Test falsch.

- [ ] **Schritt 5: Tests laufen lassen**

```bash
./gradlew :i18n-generator:test
```

Erwartung: 11 Tests grün.

- [ ] **Schritt 6: Bauen, Testzahl prüfen, committen**

```bash
./gradlew clean build
grep -h "<testsuite " */build/test-results/test/TEST-*.xml | wc -l
```

**Hier ist nachzusehen, ob `i18n-generator/build/test-results/test/` entsteht** — das ist
Prüfschwerpunkt 3. Fehlt es, laufen die Tests des Erzeugers nicht mit, und dann ist die
Modulentscheidung umsonst gewesen: anhalten und die Verdrahtung richten, bevor es weitergeht.

```bash
git add settings.gradle.kts i18n-generator/
git commit -m "Add the translation rules as tested pure functions" -- \
        settings.gradle.kts i18n-generator/
```

---

## Aufgabe 2: Die Dateien lesen, ohne lautlos zu verlieren

**Dateien:**
- Anlegen: `i18n-generator/src/main/kotlin/kst4contest/i18n/TranslationFiles.kt`
- Anlegen: `i18n-generator/src/test/kotlin/kst4contest/i18n/TranslationFilesTest.kt`

**Schnittstellen:**
- Liefert: `data class Translation(val language: String, val entries: Map<String, String>)`
- Liefert: `fun readTranslation(file: java.io.File): Translation` — wirft
  `TranslationException` bei unlesbarer Datei oder doppeltem Schlüssel
- Liefert: `class TranslationException(message: String) : RuntimeException(message)`
- Liefert: `fun languageOf(fileName: String): String?` — `strings_de.properties` → `de`

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Reading a translation file.
 *
 * The one rule with teeth: `java.util.Properties` keeps the last of two identical keys
 * without a word. A contributor who translates a key twice -- easy in a file of several
 * hundred lines -- would otherwise lose one of their translations and never find out.
 */
class TranslationFilesTest {

    private fun fileWith(name: String, content: String): File {
        val directory = createTempDirectory()
        val file = File(directory, name)
        file.writeText(content)
        return file
    }

    private fun createTempDirectory(): File =
        java.nio.file.Files.createTempDirectory("kst4contest-i18n").toFile()

    @Test
    fun aPlainFileIsReadKeyByKey() {
        val file = fileWith("strings_en.properties", "settings.save=Save settings\nmenu.file=File\n")

        val translation = readTranslation(file)

        assertEquals("en", translation.language)
        assertEquals("Save settings", translation.entries["settings.save"])
        assertEquals("File", translation.entries["menu.file"])
    }

    @Test
    fun umlautsSurviveBecauseTheFileIsUtf8() {
        // Java 9 and later read .properties as UTF-8, which is why no escapes are needed.
        val file = fileWith("strings_de.properties", "settings.save=Einstellungen speichern\nx=Grüße\n")

        assertEquals("Grüße", readTranslation(file).entries["x"])
    }

    @Test
    fun aDuplicateKeyIsRefusedRatherThanSilentlyWinning() {
        /*
         * Properties would keep the second and say nothing. For a contributor that means one
         * of their two translations vanishes with a green build.
         */
        val file = fileWith("strings_de.properties", "a=erste\nb=zwei\na=zweite\n")

        val failure = assertThrows(TranslationException::class.java) { readTranslation(file) }

        assertTrue(failure.message!!.contains("a"), failure.message)
        assertTrue(failure.message!!.contains("strings_de.properties"), failure.message)
    }

    @Test
    fun aDuplicateKeyIsStillFoundWhenTheLinesAreNotAdjacentOrAreSpaced() {
        val file = fileWith("strings_de.properties", "a = eins\nb=zwei\n a=drei\n")

        assertThrows(TranslationException::class.java) { readTranslation(file) }
    }

    @Test
    fun commentsAndBlankLinesAreNotKeys() {
        val file = fileWith(
            "strings_en.properties",
            "# a comment\n! another\n\na=one\n",
        )

        assertEquals(mapOf("a" to "one"), readTranslation(file).entries)
    }

    @Test
    fun aValueMayContainAnEqualsSign() {
        // Only the first separator splits; the rest is text. Operators do write "a=b".
        val file = fileWith("strings_en.properties", "formula=a=b\n")

        assertEquals("a=b", readTranslation(file).entries["formula"])
    }

    @Test
    fun aMissingFileIsRefusedWithItsName() {
        val absent = File("/nonexistent/kst4contest/strings_de.properties")

        val failure = assertThrows(TranslationException::class.java) { readTranslation(absent) }

        assertTrue(failure.message!!.contains("strings_de.properties"), failure.message)
    }

    @Test
    fun theLanguageComesFromTheFileName() {
        assertEquals("en", languageOf("strings_en.properties"))
        assertEquals("de", languageOf("strings_de.properties"))
        assertEquals("pt_BR", languageOf("strings_pt_BR.properties"))
    }

    @Test
    fun aFileNameThatIsNotATranslationNamesNoLanguage() {
        assertNull(languageOf("strings.properties"))
        assertNull(languageOf("gradle.properties"))
        assertNull(languageOf("strings_de.txt"))
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :i18n-generator:test --tests '*TranslationFilesTest*'
```

- [ ] **Schritt 3: Die Umsetzung schreiben**

```kotlin
package kst4contest.i18n

import java.io.File
import java.util.Properties

/** One language's texts. */
data class Translation(val language: String, val entries: Map<String, String>)

/** What the generator refuses. The message is read by a contributor, so it names the file. */
class TranslationException(message: String) : RuntimeException(message)

/**
 * Reads one translation file.
 *
 * `Properties` does the parsing -- separators, comments, escapes, continuation lines -- and is
 * battle-tested at it. What it does *not* do is complain about a key that appears twice: it
 * keeps the last one silently. In a file of several hundred lines that is an easy mistake and
 * an invisible loss, so the keys are counted separately before the values are taken.
 *
 * @throws TranslationException when the file cannot be read or names a key twice
 */
fun readTranslation(file: File): Translation {

    if (!file.isFile) {
        throw TranslationException("${file.name}: no such translation file (${file.path})")
    }

    val duplicates = duplicateKeysIn(file)

    if (duplicates.isNotEmpty()) {
        throw TranslationException(
            "${file.name}: these keys appear more than once, so one translation would be "
                    + "lost without a word: ${duplicates.sorted().joinToString(", ")}"
        )
    }

    val properties = Properties()
    runCatching { file.reader(Charsets.UTF_8).use(properties::load) }
        .onFailure { throw TranslationException("${file.name}: cannot be read -- ${it.message}") }

    val language = languageOf(file.name)
        ?: throw TranslationException("${file.name}: the file name does not name a language")

    return Translation(
        language = language,
        entries = properties.stringPropertyNames().associateWith { properties.getProperty(it) },
    )
}

/**
 * The language a translation file name names, or null when it names none.
 *
 * `strings_de.properties` is German, `strings_pt_BR.properties` is Brazilian Portuguese, and
 * `strings.properties` is not a translation file at all.
 */
fun languageOf(fileName: String): String? {
    val match = FILE_NAME.matchEntire(fileName) ?: return null
    return match.groupValues[1]
}

/**
 * Keys that appear more than once, found by reading the lines rather than the parsed map --
 * which is the only place the information still exists.
 */
private fun duplicateKeysIn(file: File): Set<String> {
    val seen = mutableSetOf<String>()
    val twice = mutableSetOf<String>()

    file.forEachLine(Charsets.UTF_8) { raw ->
        val line = raw.trim()

        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
            return@forEachLine
        }

        val separator = line.indexOfFirst { it == '=' || it == ':' }

        if (separator <= 0) {
            return@forEachLine
        }

        val key = line.take(separator).trim()

        if (!seen.add(key)) {
            twice += key
        }
    }

    return twice
}

private val FILE_NAME = Regex("""strings_([A-Za-z]{2}(?:_[A-Za-z]{2})?)\.properties""")
```

**Bekannte Grenze, bewußt:** die Dublettenprüfung versteht keine Fortsetzungszeilen
(`\` am Zeilenende). Eine Fortsetzungszeile, deren Fortsetzung ein `=` enthält, könnte als
Schlüssel gelesen werden. Das ist für diese Dateien hinnehmbar — die Texte sind einzeilig —
und die Alternative wäre, `Properties`' Parser nachzubauen. **Als Kommentar an
`duplicateKeysIn` schreiben**, damit der nächste Leser es weiß statt es zu entdecken.

- [ ] **Schritt 4: Tests laufen lassen, bauen, committen**

```bash
./gradlew :i18n-generator:test --tests '*TranslationFilesTest*'
./gradlew clean build
git add i18n-generator/src
git commit -m "Read translation files, refusing a duplicate key rather than losing one" -- \
        i18n-generator/src
```

---

## Aufgabe 3: Den Kotlin-Quelltext erzeugen

**Dateien:**
- Anlegen: `i18n-generator/src/main/kotlin/kst4contest/i18n/StringsSource.kt`
- Anlegen: `i18n-generator/src/test/kotlin/kst4contest/i18n/StringsSourceTest.kt`

**Schnittstellen:**
- Liefert: `fun stringsSource(base: Translation, translations: List<Translation>): String`
- Liefert: `fun kotlinLiteral(text: String): String` — die Maskierung, öffentlich, weil sie
  Prüfschwerpunkt 1 ist und ihren eigenen Test braucht
- Erzeugt wird das Paket `kst4contest.view.i18n`, Klasse `Strings`, Objekt `Translations`.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Turning translations into Kotlin.
 *
 * The dangerous part is the escaping. This writes Kotlin source, and in Kotlin `$` opens a
 * template: a translation containing one produces either source that does not compile or --
 * worse -- source that means something else. A translator has no reason to know that.
 */
class StringsSourceTest {

    private fun base(vararg pairs: Pair<String, String>) =
        Translation("en", mapOf(*pairs))

    @Test
    fun aPlainKeyBecomesAProperty() {
        val source = stringsSource(base("settings.save" to "Save settings"), emptyList())

        assertTrue(
            source.contains("val settingsSave: String get() = lookup(\"settings.save\")"),
            source,
        )
    }

    @Test
    fun aKeyWithAPlaceholderBecomesAFunctionWithThatManyArguments() {
        val source = stringsSource(base("c.to" to "Connect to {0}"), emptyList())

        assertTrue(source.contains("fun cTo(arg0: Any)"), source)
        assertFalse(source.contains("val cTo"), "a parameterised key became a property")
    }

    @Test
    fun theArityFollowsTheHighestIndexAndNotTheOccurrences() {
        val source = stringsSource(base("x" to "{1} {1}"), emptyList())

        assertTrue(source.contains("fun x(arg0: Any, arg1: Any)"), source)
    }

    @Test
    fun theBaseTextsAreEmittedAsAMap() {
        val source = stringsSource(base("a" to "one"), emptyList())

        assertTrue(source.contains("\"a\" to \"one\""), source)
    }

    @Test
    fun aTranslationIsEmittedWithOnlyTheKeysItHas() {
        /*
         * Incompleteness is allowed, so a translation carries only its own keys and the
         * lookup falls through to the base. Padding it with English would make a missing
         * translation indistinguishable from a deliberate English term.
         */
        val source = stringsSource(
            base("a" to "one", "b" to "two"),
            listOf(Translation("de", mapOf("a" to "eins"))),
        )

        assertTrue(source.contains("\"a\" to \"eins\""), source)
        assertFalse(source.contains("\"b\" to \"two\"\n            )"), "de was padded")
    }

    // ------------------------------------------------------------------ escaping

    @Test
    fun aDollarSignIsEscapedBecauseKotlinWouldReadItAsATemplate() {
        assertEquals("\"5 \\\$ only\"", kotlinLiteral("5 \$ only"))
    }

    @Test
    fun quotesAndBackslashesAreEscaped() {
        assertEquals("\"say \\\"hi\\\"\"", kotlinLiteral("say \"hi\""))
        assertEquals("\"a\\\\b\"", kotlinLiteral("a\\b"))
    }

    @Test
    fun newlinesAndTabsBecomeEscapesRatherThanBreakingTheLiteral() {
        assertEquals("\"a\\nb\"", kotlinLiteral("a\nb"))
        assertEquals("\"a\\tb\"", kotlinLiteral("a\tb"))
    }

    @Test
    fun anUmlautStaysItselfBecauseTheGeneratedFileIsUtf8() {
        assertEquals("\"Grüße\"", kotlinLiteral("Grüße"))
    }

    @Test
    fun aTranslationFullOfHazardsSurvivesIntoTheSource() {
        // The end-to-end version of the four escaping tests above.
        val source = stringsSource(
            base("x" to "a \$b \"c\" d\\e"),
            emptyList(),
        )

        assertTrue(source.contains("\\\$b"), source)
        assertTrue(source.contains("\\\"c\\\""), source)
        assertTrue(source.contains("d\\\\e"), source)
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :i18n-generator:test --tests '*StringsSourceTest*'
```

- [ ] **Schritt 3: Die Umsetzung schreiben**

```kotlin
package kst4contest.i18n

/**
 * Turns translations into the Kotlin the application compiles against.
 *
 * Two things come out: a `Strings` class whose members are one per key -- a property for a
 * plain text, a function for a parameterised one -- and a `Translations` object holding the
 * texts. The class is what makes a typo and a wrong argument count compile errors rather than
 * things an operator discovers.
 */
fun stringsSource(base: Translation, translations: List<Translation>): String {

    val keys = base.entries.keys.sorted()

    val members = keys.joinToString("\n\n") { key ->
        val identifier = identifierFor(key)
        val arity = argumentCount(base.entries.getValue(key))

        if (arity == 0) {
            "    /** ${commentFor(base.entries.getValue(key))} */\n" +
                "    val $identifier: String get() = lookup(${kotlinLiteral(key)})"
        } else {
            val parameters = (0 until arity).joinToString(", ") { "arg$it: Any" }
            val arguments = (0 until arity).joinToString(", ") { "arg$it" }
            "    /** ${commentFor(base.entries.getValue(key))} */\n" +
                "    fun $identifier($parameters): String =\n" +
                "        MessageFormat.format(lookup(${kotlinLiteral(key)}), $arguments)"
        }
    }

    val tables = (listOf(base) + translations).joinToString("\n\n") { translation ->
        val entries = translation.entries.keys.sorted().joinToString(",\n") { key ->
            "        ${kotlinLiteral(key)} to ${kotlinLiteral(translation.entries.getValue(key))}"
        }
        "    /** ${translation.language}: ${translation.entries.size} of ${base.entries.size}. */\n" +
            "    val ${translation.language.uppercase()}: Map<String, String> = mapOf(\n$entries,\n    )"
    }

    val byLanguage = (listOf(base) + translations).joinToString(",\n") { translation ->
        "        ${kotlinLiteral(translation.language)} to ${translation.language.uppercase()}"
    }

    return """
        |// Generated from app-desktop/src/main/i18n by the :i18n-generator module.
        |// Do not edit: edit the .properties files and rebuild.
        |package kst4contest.view.i18n
        |
        |import java.text.MessageFormat
        |
        |/**
        | * The interface texts of one language.
        | *
        | * One member per key, so a mistyped key and a wrong argument count are both compile
        | * errors. Generated -- see the module named above.
        | */
        |class Strings(private val lookup: (String) -> String) {
        |
        |$members
        |}
        |
        |/** The texts themselves, and which language has which. */
        |object Translations {
        |
        |$tables
        |
        |    /** Every language this build carries, by its code. */
        |    val BY_LANGUAGE: Map<String, Map<String, String>> = mapOf(
        |$byLanguage,
        |    )
        |
        |    /** The language every lookup falls back to. */
        |    const val BASE_LANGUAGE: String = ${kotlinLiteral(base.language)}
        |}
        |
    """.trimMargin()
}

/**
 * A text as a Kotlin string literal.
 *
 * The `$` is the one that matters and the one a translator cannot be expected to know about:
 * in Kotlin it opens a template, so an unescaped one produces source that either does not
 * compile or means something other than the text. The backslash has to go first, or it would
 * escape the escapes added after it.
 */
fun kotlinLiteral(text: String): String {
    val escaped = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    return "\"$escaped\""
}

/** The English text as a KDoc line, so the call site shows what it says. */
private fun commentFor(text: String): String =
    text.replace("*/", "* /").replace("\n", " ").take(120)
```

- [ ] **Schritt 4: Tests laufen lassen, bauen, committen**

```bash
./gradlew :i18n-generator:test --tests '*StringsSourceTest*'
./gradlew clean build
git add i18n-generator/src
git commit -m "Generate the Kotlin, escaping what a translator cannot know about" -- \
        i18n-generator/src
```

---

## Aufgabe 4: Das Hauptprogramm und die Bau-Verdrahtung

**Dateien:**
- Anlegen: `i18n-generator/src/main/kotlin/kst4contest/i18n/GeneratorMain.kt`
- Anlegen: `i18n-generator/src/test/kotlin/kst4contest/i18n/GenerationTest.kt`
- Anlegen: `app-desktop/src/main/i18n/strings_en.properties`
- Anlegen: `app-desktop/src/main/i18n/strings_de.properties`
- Ändern: `app-desktop/build.gradle.kts`

**Schnittstellen:**
- Liefert: `fun generate(inputDirectory: File, outputDirectory: File): GenerationResult`
- Liefert: `data class GenerationResult(val warnings: List<String>, val coverage: List<String>)`
- Liefert: `object GeneratorMain { @JvmStatic fun main(args: Array<String>) }` — zwei
  Argumente: Eingabe- und Ausgabeverzeichnis. Wirft `TranslationException`, womit die
  Gradle-Aufgabe mit der Meldung des Erzeugers abbricht.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

/**
 * Generation end to end: what breaks the build and what only warns.
 *
 * The distinction is the whole arrangement. A translation that is 80 per cent finished has to
 * be submittable, or nobody outside this project can contribute one; a translation that is
 * wrong must not ship.
 */
class GenerationTest {

    private fun directories(): Pair<File, File> {
        val input = Files.createTempDirectory("kst4contest-i18n-in").toFile()
        val output = Files.createTempDirectory("kst4contest-i18n-out").toFile()
        return input to output
    }

    private fun write(directory: File, name: String, content: String) {
        File(directory, name).writeText(content, Charsets.UTF_8)
    }

    @Test
    fun aCompleteSetGeneratesAndWarnsAboutNothing() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\n")
        write(input, "strings_de.properties", "a=eins\n")

        val result = generate(input, output)

        assertTrue(result.warnings.isEmpty(), "${result.warnings}")
        assertTrue(File(output, "kst4contest/view/i18n/Strings.kt").isFile, "no source written")
    }

    @Test
    fun aMissingKeyOnlyWarnsAndStillGenerates() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\nb=two\n")
        write(input, "strings_de.properties", "a=eins\n")

        val result = generate(input, output)

        assertEquals(1, result.warnings.size, "${result.warnings}")
        assertTrue(result.warnings.single().contains("b"), result.warnings.single())
        assertTrue(File(output, "kst4contest/view/i18n/Strings.kt").isFile)
    }

    @Test
    fun anOrphanKeyBreaksTheBuildAndNamesItself() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\n")
        write(input, "strings_de.properties", "a=eins\ntypo=zwei\n")

        val failure = assertThrows(TranslationException::class.java) { generate(input, output) }

        assertTrue(failure.message!!.contains("typo"), failure.message)
        assertTrue(failure.message!!.contains("strings_de"), failure.message)
    }

    @Test
    fun aPlaceholderMismatchBreaksTheBuild() {
        /*
         * The one that would otherwise swallow data in silence: the base passes two
         * arguments, the translation prints one, and MessageFormat says nothing.
         */
        val (input, output) = directories()
        write(input, "strings_en.properties", "a={0} of {1}\n")
        write(input, "strings_de.properties", "a={0}\n")

        val failure = assertThrows(TranslationException::class.java) { generate(input, output) }

        assertTrue(failure.message!!.contains("a"), failure.message)
    }

    @Test
    fun anIdentifierCollisionBreaksTheBuild() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "gui.startupDesign=x\ngui.startup_design=y\n")

        val failure = assertThrows(TranslationException::class.java) { generate(input, output) }

        assertTrue(failure.message!!.contains("guiStartupDesign"), failure.message)
    }

    @Test
    fun aMissingBaseFileBreaksTheBuild() {
        val (input, output) = directories()
        write(input, "strings_de.properties", "a=eins\n")

        assertThrows(TranslationException::class.java) { generate(input, output) }
    }

    @Test
    fun aThirdLanguageIsGeneratedToo() {
        // Prüfschwerpunkt 6: a contributed language must not need any wiring.
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\nb=two\n")
        write(input, "strings_de.properties", "a=eins\nb=zwei\n")
        write(input, "strings_fr.properties", "a=un\n")

        val result = generate(input, output)

        assertTrue(File(output, "kst4contest/view/i18n/Strings.kt").readText().contains("\"a\" to \"un\""))
        assertTrue(result.coverage.any { it.contains("fr") && it.contains("1") }, "${result.coverage}")
    }

    @Test
    fun theCoverageLineNamesEveryLanguage() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\nb=two\n")
        write(input, "strings_de.properties", "a=eins\nb=zwei\n")

        val result = generate(input, output)

        assertTrue(result.coverage.any { it.startsWith("de:") && it.contains("2/2") }, "${result.coverage}")
    }
}
```

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :i18n-generator:test --tests '*GenerationTest*'
```

- [ ] **Schritt 3: Die Umsetzung schreiben**

```kotlin
package kst4contest.i18n

import java.io.File

/**
 * What generation reported.
 *
 * @param warnings incomplete translations -- allowed, and the reason a partial contribution
 *        can be submitted at all
 * @param coverage one line per language, so a pull request can be judged at a glance
 */
data class GenerationResult(val warnings: List<String>, val coverage: List<String>)

/**
 * Reads every translation file in a directory and writes the generated Kotlin.
 *
 * Incompleteness is allowed, incorrectness is not. A missing key warns and falls back to the
 * base at runtime; an orphan key, a placeholder mismatch, a duplicate key, an unreadable file
 * or two keys collapsing to one identifier all throw, which fails the build with this
 * message.
 *
 * @throws TranslationException on anything a contributor has to fix
 */
fun generate(inputDirectory: File, outputDirectory: File): GenerationResult {

    val files = (inputDirectory.listFiles() ?: emptyArray())
        .filter { languageOf(it.name) != null }
        .sortedBy { it.name }

    val translations = files.map(::readTranslation)

    val base = translations.firstOrNull { it.language == BASE_LANGUAGE }
        ?: throw TranslationException(
            "strings_$BASE_LANGUAGE.properties is missing from ${inputDirectory.path}; "
                    + "the base language is where every key and every argument count comes from"
        )

    val collisions = identifierCollisions(base.entries.keys)

    if (collisions.isNotEmpty()) {
        throw TranslationException(
            "these keys would become the same Kotlin name: " + collisions.entries.joinToString("; ") {
                "${it.key} <- ${it.value.sorted().joinToString(", ")}"
            }
        )
    }

    val others = translations.filter { it.language != BASE_LANGUAGE }
    val warnings = mutableListOf<String>()

    for (translation in others) {
        val orphans = orphanKeys(base.entries.keys, translation.entries.keys)

        if (orphans.isNotEmpty()) {
            throw TranslationException(
                "strings_${translation.language}.properties names keys the base does not, so "
                        + "nothing would ever read them -- a typo? "
                        + orphans.sorted().joinToString(", ")
            )
        }

        val mismatched = translation.entries.keys.filter { key ->
            argumentCount(translation.entries.getValue(key)) !=
                argumentCount(base.entries.getValue(key))
        }

        if (mismatched.isNotEmpty()) {
            throw TranslationException(
                "strings_${translation.language}.properties uses different placeholders than "
                        + "the base for: " + mismatched.sorted().joinToString(", ")
                        + ". A placeholder the translation leaves out drops its data silently."
            )
        }

        val missing = missingKeys(base.entries.keys, translation.entries.keys)

        if (missing.isNotEmpty()) {
            warnings += "strings_${translation.language}.properties is missing "
                .plus("${missing.size} of ${base.entries.size} keys; they fall back to ")
                .plus("$BASE_LANGUAGE: ${missing.sorted().take(10).joinToString(", ")}")
        }
    }

    val target = File(outputDirectory, "kst4contest/view/i18n/Strings.kt")
    target.parentFile.mkdirs()
    target.writeText(stringsSource(base, others), Charsets.UTF_8)

    val coverage = translations.map {
        "${it.language}: ${it.entries.size}/${base.entries.size}"
    }

    return GenerationResult(warnings = warnings, coverage = coverage)
}

/**
 * English, because the source text lives in the code and AGENTS.md requires it there.
 *
 * Deliberately a second declaration: the generated `Translations.BASE_LANGUAGE` says the same
 * thing on the application's side. They cannot be shared -- this module is build tooling and
 * is not on the application's classpath -- and `GenerationTest` would fail if they drifted,
 * because the generated source is written from this constant.
 */
const val BASE_LANGUAGE: String = "en"

/** Called by Gradle: input directory, output directory. */
object GeneratorMain {

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "usage: GeneratorMain <input dir> <output dir>" }

        val result = generate(File(args[0]), File(args[1]))

        result.warnings.forEach { println("[i18n, warning] $it") }
        println("[i18n] coverage -- " + result.coverage.joinToString(", "))
    }
}
```

- [ ] **Schritt 3b: Das Arbeitsblatt für Beitragende**

Die Spezifikation verspricht es, und ohne es müsste ein Beitragender zwei Dateien diffen, um zu
finden, was fehlt. Der Erzeuger schreibt es als Nebenerzeugnis neben die erzeugte Quelle.

Zuerst der Test, in `GenerationTest`:

```kotlin
    @Test
    fun anIncompleteTranslationGetsAWorksheetWithTheEnglishTextToTranslate() {
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\nb=two\n")
        write(input, "strings_de.properties", "a=eins\n")

        generate(input, output)

        val worksheet = File(output, "todo/strings_de.todo.properties")
        assertTrue(worksheet.isFile, "no worksheet was written")

        val content = worksheet.readText()
        assertTrue(content.contains("b=two"), content)
        assertFalse(content.contains("a="), "a key that is already translated was listed")
    }

    @Test
    fun aCompleteTranslationGetsNoWorksheet() {
        // An empty worksheet is a file somebody has to open to learn there is nothing to do.
        val (input, output) = directories()
        write(input, "strings_en.properties", "a=one\n")
        write(input, "strings_de.properties", "a=eins\n")

        generate(input, output)

        assertFalse(File(output, "todo/strings_de.todo.properties").exists())
    }
```

Dazu `import org.junit.jupiter.api.Assertions.assertFalse` ergänzen.

Dann in `generate`, im Zweig, der heute die Warnung sammelt — statt nur zu warnen:

```kotlin
        if (missing.isNotEmpty()) {
            warnings += "strings_${translation.language}.properties is missing "
                .plus("${missing.size} of ${base.entries.size} keys; they fall back to ")
                .plus("$BASE_LANGUAGE: ${missing.sorted().take(10).joinToString(", ")}")

            writeWorksheet(outputDirectory, translation.language, base, missing)
        }
```

und daneben:

```kotlin
/**
 * Writes the keys a translation still lacks, with the base text as the value.
 *
 * A worksheet, not a build output: a contributor copies its lines into their own file and
 * translates the right-hand side. Without it they would have to diff two files of several
 * hundred lines to find out what is left.
 *
 * Only written when something is missing -- an empty worksheet is a file somebody has to open
 * to learn there is nothing in it.
 */
private fun writeWorksheet(
    outputDirectory: File,
    language: String,
    base: Translation,
    missing: Set<String>,
) {
    val file = File(outputDirectory, "todo/strings_$language.todo.properties")
    file.parentFile.mkdirs()

    val header = "# ${missing.size} of ${base.entries.size} keys are not yet translated into "
        .plus("$language. Copy these lines into strings_$language.properties and translate ")
        .plus("the values. Partial is fine: anything left out falls back to $BASE_LANGUAGE.")

    val lines = missing.sorted().joinToString("\n") { key ->
        "$key=${base.entries.getValue(key)}"
    }

    file.writeText("$header\n\n$lines\n", Charsets.UTF_8)
}
```

**Das Arbeitsblatt liegt unter `build/`**, nicht im Quellbaum: es ist abgeleitet, und ein
eingecheckter Zwischenstand davon wäre sofort veraltet. Der Pfad kommt in die Beitragsanleitung
in Aufgabe 10.

- [ ] **Schritt 4: Die beiden Textdateien anlegen**

`app-desktop/src/main/i18n/strings_en.properties`:

```properties
# The base language. Every key and every argument count comes from this file.
# To contribute a language: copy this file to strings_<code>.properties and translate the
# values to the right of the equals sign. An untranslated key falls back to English, so a
# partial file is fine. Do not translate anything that goes to the ON4KST server or to other
# radio amateurs -- none of that is in this file, and the build checks that it stays that way.
settings.save=Save settings
settings.applyAndClose=Apply/Close prefs
connection.connect=Connect
connection.connectTo=Connect to {0}
connection.disconnect=Disconnect
connection.disconnectAndCloseChat=Disconnect & close Chat
gui.startupDesign=Startup design
gui.startupFilters=Message filters active at startup
gui.bandColumnHints=Hints in the band columns
gui.language=Language
gui.language.system=System language
menu.file=File
menu.options=Options
menu.windows=Windows
menu.info=Info
alert.ok=OK
```

`app-desktop/src/main/i18n/strings_de.properties`:

```properties
# German. Held complete by a test, because this is a shipped language.
settings.save=Einstellungen speichern
settings.applyAndClose=Übernehmen/Schließen
connection.connect=Verbinden
connection.connectTo=Mit {0} verbinden
connection.disconnect=Trennen
connection.disconnectAndCloseChat=Trennen & Chat schließen
gui.startupDesign=Design beim Start
gui.startupFilters=Beim Start aktive Nachrichtenfilter
gui.bandColumnHints=Hinweise in den Bandspalten
gui.language=Sprache
gui.language.system=Systemsprache
menu.file=Datei
menu.options=Optionen
menu.windows=Fenster
menu.info=Info
alert.ok=OK
```

**Für Marc zu prüfen** (die Liste aus der Entwurfsbesprechung): `connection.connectTo` — im
Deutschen steht der Platzhalter vorn, was genau der Grund für `MessageFormat` statt Verketten
ist. `settings.applyAndClose` ist bewußt kürzer als die englische Fassung, weil der Knopf
sonst die Leiste sprengt. `menu.info` bleibt „Info", weil das im deutschen Funkbetrieb so
heißt.

- [ ] **Schritt 5: Gradle verdrahten**

In `app-desktop/build.gradle.kts`, nach dem `dependencies`-Block:

```kotlin
/*
 * The translation files are turned into Kotlin before anything compiles, by the
 * :i18n-generator module. Two consequences worth naming:
 *
 * - A broken translation fails `./gradlew classes`, which is what the PR workflow already
 *   runs -- so a contributor's mistake is reported in their pull request without a new
 *   workflow existing for it.
 * - The generator is reached through its own configuration, not through `implementation`, so
 *   it stays out of the application's runtime classpath and out of the jar.
 */
val i18nGenerator: Configuration by configurations.creating

dependencies {
    i18nGenerator(project(":i18n-generator"))
}

val generatedI18nDirectory = layout.buildDirectory.dir("generated/i18n")

val generateStrings by tasks.registering(JavaExec::class) {
    description = "Generates Strings.kt from app-desktop/src/main/i18n"
    group = "build"

    classpath = i18nGenerator
    mainClass.set("kst4contest.i18n.GeneratorMain")

    val input = layout.projectDirectory.dir("src/main/i18n")
    inputs.dir(input)
    outputs.dir(generatedI18nDirectory)

    argumentProviders.add(CommandLineArgumentProvider {
        listOf(input.asFile.absolutePath, generatedI18nDirectory.get().asFile.absolutePath)
    })
}

kotlin.sourceSets["main"].kotlin.srcDir(generatedI18nDirectory)

tasks.named("compileKotlin") { dependsOn(generateStrings) }
```

- [ ] **Schritt 6: Nachprüfen, dass der Erzeuger wirklich greift**

```bash
./gradlew :app-desktop:compileKotlin
ls app-desktop/build/generated/i18n/kst4contest/view/i18n/Strings.kt
```

Erwartung: die Datei existiert und enthält `val settingsSave`.

**Dann die Gegenprobe, und die ist der Punkt der ganzen Aufgabe** — eine kaputte Übersetzung
muss den Bau abbrechen:

```bash
echo "typo.nobody.declared=x" >> app-desktop/src/main/i18n/strings_de.properties
./gradlew :app-desktop:compileKotlin 2>&1 | grep -i "typo.nobody.declared"
git checkout app-desktop/src/main/i18n/strings_de.properties
```

Erwartung: der Bau bricht ab und nennt den Schlüssel. Bricht er **nicht** ab, ist die
Verdrahtung falsch und alles Weitere baut auf Sand — anhalten und richten.

- [ ] **Schritt 7: Bauen, Testzahl prüfen, committen**

```bash
./gradlew clean build
python3 - <<'PY'
import glob,re
t=s=f=e=0
for p in glob.glob('*/build/test-results/test/TEST-*.xml'):
    m=re.search(r'<testsuite [^>]*',open(p).read(600)).group(0)
    g=lambda k: int(re.search(k+r'="(\d+)"',m).group(1))
    t+=g('tests'); s+=g('skipped'); f+=g('failures'); e+=g('errors')
print(f"tests={t} skipped={s} failures={f} errors={e}")
PY
```

**Die Zahl muss die Tests des Erzeugers enthalten** — das ist Prüfschwerpunkt 3. Tut sie es
nicht, laufen sie nicht mit.

```bash
git add i18n-generator/src app-desktop/src/main/i18n app-desktop/build.gradle.kts
git commit -m "Generate Strings.kt before compiling, and fail the build on a bad translation" -- \
        i18n-generator/src app-desktop/src/main/i18n app-desktop/build.gradle.kts
```

---

## Aufgabe 5: Die Sprachwahl als reine Funktion

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/i18n/LocaleChoice.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/i18n/LocaleChoiceTest.kt`

**Schnittstellen:**
- Liefert: `fun languageFor(stored: String?, systemDefault: java.util.Locale): String` — der
  Sprachcode, für den `Translations.BY_LANGUAGE` einen Eintrag hat, sonst
  `Translations.BASE_LANGUAGE`.
- Liefert: `const val SYSTEM_LANGUAGE = ""` — der gespeicherte Wert für „Systemsprache".

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.view.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Which language a stored setting and a system default add up to.
 *
 * A pure function on purpose: the alternative is a test that changes the JVM's default locale,
 * which is global state and leaks into whatever runs next.
 */
class LocaleChoiceTest {

    @Test
    fun anEmptySettingFollowsTheSystem() {
        assertEquals("de", languageFor(SYSTEM_LANGUAGE, Locale.GERMANY))
        assertEquals("en", languageFor(SYSTEM_LANGUAGE, Locale.UK))
    }

    @Test
    fun aSystemLanguageNobodyTranslatedFallsBackToEnglish() {
        // The spec's rule: default from the system language, falling back to English.
        assertEquals("en", languageFor(SYSTEM_LANGUAGE, Locale.FRANCE))
        assertEquals("en", languageFor(SYSTEM_LANGUAGE, Locale.JAPAN))
    }

    @Test
    fun anExplicitChoiceBeatsTheSystem() {
        assertEquals("de", languageFor("de", Locale.UK))
        assertEquals("en", languageFor("en", Locale.GERMANY))
    }

    @Test
    fun nonsenseInTheSettingFallsBackToEnglishRatherThanFailing() {
        /*
         * The stored value can be hand-edited in the preferences XML, or left over from a
         * build that carried a language this one does not.
         */
        assertEquals("en", languageFor("klingon", Locale.GERMANY))
        assertEquals("en", languageFor("de_DE_x", Locale.GERMANY))
        assertEquals("en", languageFor(null, Locale.UK))
    }

    @Test
    fun theSettingIsCaseInsensitiveBecauseAHandEditedFileMayShout() {
        assertEquals("de", languageFor("DE", Locale.UK))
    }
}
```

- [ ] **Schritt 2–4: Laufen lassen, umsetzen, laufen lassen**

```bash
./gradlew :app-desktop:test --tests '*LocaleChoiceTest*'
```

```kotlin
package kst4contest.view.i18n

import java.util.Locale

/** The stored value that means "whatever the system is set to". */
const val SYSTEM_LANGUAGE: String = ""

/**
 * The language to use.
 *
 * An explicit setting wins; an empty one follows the system; anything this build has no texts
 * for falls back to the base language. A pure function so it can be tested without touching
 * the JVM's default locale, which is global state.
 *
 * @param stored the value from the preferences, possibly hand-edited or from another build
 * @param systemDefault usually `Locale.getDefault()`
 * @return a language code `Translations.BY_LANGUAGE` has an entry for
 */
fun languageFor(stored: String?, systemDefault: Locale): String {
    val wanted = stored?.trim()?.lowercase(Locale.ROOT).orEmpty()

    val candidate = if (wanted == SYSTEM_LANGUAGE) {
        systemDefault.language.lowercase(Locale.ROOT)
    } else {
        wanted
    }

    return if (Translations.BY_LANGUAGE.containsKey(candidate)) {
        candidate
    } else {
        Translations.BASE_LANGUAGE
    }
}
```

- [ ] **Schritt 5: Bauen, committen**

```bash
./gradlew clean build
git add app-desktop/src/main/kotlin/kst4contest/view/i18n/LocaleChoice.kt \
        app-desktop/src/test/kotlin/kst4contest/view/i18n/LocaleChoiceTest.kt
git commit -m "Resolve the language from the setting and the system, falling back to English" -- \
        app-desktop/src/main/kotlin/kst4contest/view/i18n/LocaleChoice.kt \
        app-desktop/src/test/kotlin/kst4contest/view/i18n/LocaleChoiceTest.kt
```

---

## Aufgabe 6: Der Store, und dass ein Wechsel alle Fenster erreicht

**Dateien:**
- Anlegen: `app-desktop/src/main/kotlin/kst4contest/view/i18n/LanguageStore.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/i18n/LanguageStoreTest.kt`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/i18n/StringsReachTheCompositionTest.kt`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/ComposeWindowHost.kt`

**Schnittstellen:**
- Liefert: `class LanguageStore(storedLanguage: () -> String?, private val storeLanguage: (String) -> Unit, private val systemDefault: Locale = Locale.getDefault())`
  mit `language: String`, `strings: Strings`, `fun use(language: String)`.
- Liefert: `val LocalStrings: ProvidableCompositionLocal<Strings>` — Vorgabe ist die
  Grundsprache, damit ein Fenster ohne Store zeichnen kann.
- Liefert: `object CurrentStrings { @JvmStatic fun get(): Strings }` — für die
  Java-Aufrufstellen.

- [ ] **Schritt 1: Den fehlschlagenden Test schreiben**

```kotlin
package kst4contest.view.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * The store, and the two ways out of it.
 *
 * Built over two lambdas rather than over ChatPreferences, so none of this touches the
 * operator's home directory: that constructor copies resources into it.
 */
class LanguageStoreTest {

    private var stored: String = SYSTEM_LANGUAGE

    private fun store(systemDefault: Locale = Locale.UK) = LanguageStore(
        storedLanguage = { stored },
        storeLanguage = { stored = it },
        systemDefault = systemDefault,
    )

    @Test
    fun theStoredLanguageIsInForceFromTheStart() {
        stored = "de"

        assertEquals("de", store().language)
    }

    @Test
    fun anEmptySettingFollowsTheSystem() {
        stored = SYSTEM_LANGUAGE

        assertEquals("de", store(Locale.GERMANY).language)
    }

    @Test
    fun choosingALanguageWritesItThroughAndChangesTheTexts() {
        val store = store()
        val englishSave = store.strings.settingsSave

        store.use("de")

        assertEquals("de", store.language)
        assertEquals("de", stored)
        assertEquals(false, store.strings.settingsSave == englishSave)
    }

    @Test
    fun aLanguageThisBuildDoesNotCarryIsRefusedRatherThanStored() {
        /*
         * The picker cannot offer one, but a hand-edited preferences file can name one, and
         * so can a build that dropped a language. Storing it would persist a dead setting.
         */
        val store = store()

        store.use("klingon")

        assertEquals("en", store.language)
        assertEquals(SYSTEM_LANGUAGE, stored, "a language with no texts was stored")
    }

    @Test
    fun aKeyOnlyTheBaseHasComesBackInTheBaseLanguage() {
        /*
         * The fallback the whole contribution workflow depends on, and it cannot be tested
         * through German: German is a shipped language and Task 8 holds it complete, so no key
         * is ever missing from it. Built from maps instead, which also means this test does
         * not change every time a key is added.
         */
        val base = mapOf(
            "settings.save" to "Save settings",
            "menu.file" to "File",
        )
        val partial = mapOf("menu.file" to "Datei")

        val strings = Strings { key -> partial[key] ?: base[key] ?: key }

        assertEquals("Save settings", strings.settingsSave, "the base text did not come through")
        assertEquals("Datei", strings.menuFile, "the translation did not win")
    }

    @Test
    fun theJavaBridgeSeesWhatTheStoreSees() {
        val store = store()

        store.use("de")

        assertEquals(store.strings.settingsSave, CurrentStrings.get().settingsSave)
    }
}
```

**Warum der Rückfalltest nicht über Deutsch geht:** Deutsch ist eine ausgelieferte Sprache und
wird in Aufgabe 8 auf Vollständigkeit geprüft — es fehlt also nie ein Schlüssel, und ein Test
über die echten Dateien könnte den Rückfall gar nicht erreichen. Einen absichtlich
unvollständig zu lassen, nur damit ein Test etwas zu prüfen hat, wäre die falsche Richtung.
Deshalb baut der Test seine Karten selbst und ruft **echte erzeugte Mitglieder** darauf ab.
Kein Zugang nur für Tests in der erzeugten Klasse: ein `lookupForTest` wäre Testcode in
Produktionscode, und die erzeugten Mitglieder sind ohnehin genau das, was eine Aufrufstelle
benutzt — der Test geht also denselben Weg wie das Fenster.

- [ ] **Schritt 2: Laufen lassen und den Fehlschlag sehen**

```bash
./gradlew :app-desktop:test --tests '*LanguageStoreTest*'
```

- [ ] **Schritt 3: Den Store schreiben**

```kotlin
package kst4contest.view.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/**
 * The language in force, held as Compose state.
 *
 * **This is what makes a language change reach every open window** -- the same arrangement
 * the palette uses, and for the same reason: in Etappe 8 a remembered value meant a change
 * reached nothing until something else happened to recompose.
 *
 * Two ways out, one truth: [strings] for Compose, which recomposes on the state read, and
 * [CurrentStrings] for the Java call sites, which pick the new language up on their next call
 * without needing any machinery of their own.
 *
 * @param storedLanguage the preference, possibly empty for "follow the system"
 * @param storeLanguage writes the preference back
 * @param systemDefault injected so tests need not touch the JVM's global default
 */
class LanguageStore(
    storedLanguage: () -> String?,
    private val storeLanguage: (String) -> Unit,
    private val systemDefault: Locale = Locale.getDefault(),
) {

    private var current by mutableStateOf(languageFor(storedLanguage(), systemDefault))

    init {
        CurrentStrings.set(stringsFor(current))
    }

    /** The language code in force. */
    val language: String get() = current

    /**
     * The texts in force.
     *
     * Reads the state, so a composable calling this recomposes when the language changes.
     * That read is the whole mechanism.
     */
    val strings: Strings get() = stringsFor(current)

    /**
     * Switches the language.
     *
     * A language this build has no texts for is refused rather than stored: persisting a dead
     * setting would leave the operator with English and no way to see why.
     *
     * @param language a language code, or [SYSTEM_LANGUAGE]
     */
    fun use(language: String) {
        val resolved = languageFor(language, systemDefault)

        if (language != SYSTEM_LANGUAGE && resolved != language.trim().lowercase(Locale.ROOT)) {
            return
        }

        current = resolved
        storeLanguage(language)
        CurrentStrings.set(stringsFor(resolved))
    }

    private fun stringsFor(language: String): Strings {
        val chosen = Translations.BY_LANGUAGE[language].orEmpty()
        val base = Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE)

        /*
         * Chosen first, base second. A key the translation leaves out falls through, which is
         * what lets a partial contributed language ship at all.
         */
        return Strings { key -> chosen[key] ?: base[key] ?: key }
    }
}

/**
 * The texts the Java call sites read.
 *
 * A volatile field rather than a parameter on three hundred signatures: those call sites are
 * AWT dialogs and menu builders that run once per interaction, so reading the current value on
 * the next call is both correct and all they need.
 */
object CurrentStrings {

    @Volatile
    private var strings: Strings = Strings { key ->
        Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE)[key] ?: key
    }

    /** The texts in force. */
    @JvmStatic
    fun get(): Strings = strings

    internal fun set(value: Strings) {
        strings = value
    }
}

/**
 * The texts for the windows below.
 *
 * The default is the base language rather than null: a window composed before a profile
 * exists -- the operator profile picker -- still has to be able to draw.
 */
val LocalStrings = staticCompositionLocalOf {
    Strings { key -> Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE)[key] ?: key }
}
```

- [ ] **Schritt 4: Den Test schreiben, der die Komposition prüft**

Dieselbe Bauform wie `ThemeReadsPaletteStoreTest`, das in Etappe 8 genau diesen Fehler gefunden
hat:

```kotlin
package kst4contest.view.i18n

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * A language change reaches a composed window without anything else happening.
 *
 * The one mechanism the acceptance criterion hangs on, and the one a state-level test cannot
 * see: the store is always right, the question is whether the composition asks it again.
 * Etappe 8 shipped exactly this fault for the palette.
 *
 * Headless: no window is opened, only a composition.
 */
class StringsReachTheCompositionTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aLanguageChosenAfterTheFirstDrawReachesTheComposition() = runComposeUiTest {
        var stored = SYSTEM_LANGUAGE
        val store = LanguageStore(
            storedLanguage = { stored },
            storeLanguage = { stored = it },
            systemDefault = Locale.UK,
        )
        val seen = mutableListOf<String>()

        setContent {
            CompositionLocalProvider(LocalStrings provides store.strings) {
                seen += LocalStrings.current.settingsSave
            }
        }

        waitForIdle()
        val firstDraw = seen.last()

        store.use("de")
        waitForIdle()

        assertEquals("Save settings", firstDraw)
        assertEquals(
            "Einstellungen speichern",
            seen.last(),
            "the composition still holds the texts from before the change",
        )
    }
}
```

**Erwartung beim ersten Lauf: dieser Test schlägt fehl**, weil `CompositionLocalProvider` mit
`store.strings` nur bei einer Neuzeichnung der *umgebenden* Funktion neu liest. Das ist der
Befund, nicht ein Testfehler: die Bereitstellung muss innerhalb einer Komposition liegen, die
den Zustand liest. Die Lösung ist, `store.strings` **im** `setContent`-Block zu lesen — so wie
`Kst4ContestTheme` es für die Palette tut. Wenn der Test nach dieser Änderung grün ist, steht
die Mechanik; bleibt er rot, ist der Zustandszugriff noch außerhalb der Komposition.

- [ ] **Schritt 5: `ComposeWindowHost` den Store durchreichen**

`show(...)` bekommt einen Parameter `languageStore: LanguageStore? = null` hinter
`paletteStore`, und die Bereitstellung kommt neben die der Palette:

```kotlin
                        CompositionLocalProvider(
                            LocalPaletteStore provides paletteStore,
                            LocalStrings provides
                                (languageStore?.strings ?: LocalStrings.current),
                        ) {
```

**Die Java-Aufrufstellen müssen den Store ausdrücklich übergeben** — Kotlins Vorgabewerte sind
für Java unsichtbar, das hat Etappe 8 gemessen. Dieselben sechs Fenster-Wirte und dieselben
Aufrufstellen in `Kst4ContestApplication` wie dort, jeweils hinter `paletteStore`.

- [ ] **Schritt 6: Bauen, Testzahl prüfen, committen**

---

## Aufgabe 7: Die Einstellung und die Sprachwahl im GUI-Reiter

**Dateien:**
- Ändern: `core/src/main/java/kst4contest/model/ChatPreferences.java`
- Anlegen: `core/src/test/java/kst4contest/model/ChatPreferencesLanguageTest.java`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/GuiOptionsTabState.kt`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/GuiOptionsTab.kt`
- Ändern: `app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java`

**Schnittstellen:**
- Liefert: `ChatPreferences.getGuiOptions_language()` / `setGuiOptions_language(String)`,
  Vorgabe `""`, `null` wird `""`.

- [ ] **Schritt 1: Die Einstellung, genau nach dem Muster der Palettenfelder**

Dieselben vier Stellen wie in Etappe 8, und **diese Datei rückt mit Tabs ein**:

1. Feld neben `guiOptions_paletteOverridesEvening`:

```java
	/**
	 * The operator's interface language: empty for the system language, otherwise a language
	 * code such as "de". Per profile, like every other GUI preference -- at a multi operator
	 * station two operators may want two languages.
	 */
	private String guiOptions_language = "";
```

2. Getter und Setter nach dem Muster der Nachbarn, `null` wird `""`.
3. Ein `doc.createElement("guiOptions_language")`-Block im Schreibpfad neben
   `guiOptions_paletteOverridesDay`.
4. Eine `getText(...)`-Lesestelle daneben, mit `""` als Rückfall.

**`CONFIG_VERSION` bleibt bei 7.** Die Begründung steht seit Etappe 8 im Javadoc der
Konstante: nichts in der Anwendung verzweigt über die Zahl.

- [ ] **Schritt 2: Der Test dazu**

```java
package kst4contest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The interface language is stored per profile, like every other GUI preference. */
class ChatPreferencesLanguageTest {

    @Test
    void theDefaultIsTheSystemLanguage() {
        // Empty means "follow the system", which is what the spec asks for on a first start.
        assertEquals("", new ChatPreferences().getGuiOptions_language());
    }

    @Test
    void aChosenLanguageIsKept() {
        ChatPreferences prefs = new ChatPreferences();

        prefs.setGuiOptions_language("de");

        assertEquals("de", prefs.getGuiOptions_language());
    }

    @Test
    void aNullSetterValueBecomesEmpty() {
        ChatPreferences prefs = new ChatPreferences();

        prefs.setGuiOptions_language(null);

        assertEquals("", prefs.getGuiOptions_language());
    }
}
```

- [ ] **Schritt 3: Die Sprachwahl im Reiter**

`GuiOptionsTabState` bekommt eine Eigenschaft nach dem Muster der Nachbarn, aber über den
Store statt direkt über die Einstellung — der Store ist die Stelle, die auch die Fenster
umschaltet:

```kotlin
class GuiOptionsTabState(
    private val prefs: ChatPreferences,
    private val languageStore: LanguageStore,
) {
    ...
    /** The interface language, as the picker offers it. */
    var language: String
        get() = prefs.guiOptions_language
        set(value) { languageStore.use(value) }
}
```

Und in `GuiOptionsTab`, als **letzter** Abschnitt, damit keine gewohnte Position wandert:

```kotlin
        Form.section(strings.guiLanguage) {
            Form.choice(
                label = strings.guiLanguage,
                items = listOf(SYSTEM_LANGUAGE) + Translations.BY_LANGUAGE.keys.sorted(),
                selected = state.language,
                describe = { code ->
                    if (code == SYSTEM_LANGUAGE) strings.guiLanguageSystem else code.uppercase()
                },
                onSelect = { state.language = it },
            )
        }
```

**`describe` zeigt den Sprachcode in Großbuchstaben** und nicht den Namen der Sprache: ein
Anzeigename pro Sprache wäre ein weiterer Schlüssel, den jeder Beitragende pflegen müsste, und
`DE`/`EN` ist für zwei Sprachen eindeutig. Wird die Liste länger, ist ein Name je Sprache die
naheliegende Erweiterung — **jetzt nicht**, YAGNI.

- [ ] **Schritt 4: Den Store beim Start bilden**

In `Kst4ContestApplication.startRuntime()`, neben `paletteStore`:

```java
		languageStore = new kst4contest.view.i18n.LanguageStore(
				() -> chatcontroller.getChatPreferences().getGuiOptions_language(),
				language -> chatcontroller.getChatPreferences().setGuiOptions_language(language),
				java.util.Locale.getDefault());
```

Der Konstruktor hat einen Vorgabewert für `systemDefault`, den Java nicht sieht — deshalb wird
er hier ausdrücklich übergeben.

- [ ] **Schritt 5: Bauen, Testzahl prüfen, committen**

---

## Aufgabe 8: Die drei Abgrenzungstests

Der Teil, auf den es fachlich ankommt. **Keine neue Mechanik, nur Beweise.**

**Dateien:**
- Anlegen: `core/src/test/java/kst4contest/controller/On4KstProtocolLocaleTest.java`
- Anlegen: `core/src/test/java/kst4contest/model/ChatPreferencesLocaleTest.java`
- Anlegen: `app-desktop/src/test/kotlin/kst4contest/view/i18n/TranslationBoundaryTest.kt`

- [ ] **Schritt 1: Die Protokollrahmen sind sprachunabhängig**

`On4KstProtocol` ist **paketprivat**, der Test gehört also in dasselbe Paket.

```java
package kst4contest.controller;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The frames on the wire do not change with the interface language.
 *
 * A translated frame breaks the connection, and nothing about the user interface would show
 * it. This checks the output rather than the translation files, because the output is what
 * the server reads.
 */
class On4KstProtocolLocaleTest {

    private <T> T underLocale(Locale locale, java.util.function.Supplier<T> body) {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            return body.get();
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void everyFrameIsIdenticalUnderGermanAndEnglish() {
        for (Locale locale : new Locale[] { Locale.GERMANY, Locale.UK, Locale.forLanguageTag("tr") }) {
            assertEquals(
                    underLocale(Locale.UK, () -> On4KstProtocol.settingsDone(1)),
                    underLocale(locale, () -> On4KstProtocol.settingsDone(1)),
                    "settingsDone under " + locale);
            assertEquals(
                    underLocale(Locale.UK, () -> On4KstProtocol.addChat(1, 0L)),
                    underLocale(locale, () -> On4KstProtocol.addChat(1, 0L)),
                    "addChat under " + locale);
            assertEquals(
                    underLocale(Locale.UK, On4KstProtocol::clientLivenessProbe),
                    underLocale(locale, On4KstProtocol::clientLivenessProbe),
                    "clientLivenessProbe under " + locale);
            assertEquals(
                    underLocale(Locale.UK, On4KstProtocol::serverLivenessProbeResponse),
                    underLocale(locale, On4KstProtocol::serverLivenessProbeResponse),
                    "serverLivenessProbeResponse under " + locale);
        }
    }
}
```

**Turkish ist absichtlich dabei.** Es ist der Locale, unter dem `"I".toLowerCase()` nicht `"i"`
ergibt — die klassische Falle für jeden Protokollcode, der ohne `Locale.ROOT` umschaltet. Vor
dem Schreiben die echte Signatur von `login(...)` nachsehen und sie ebenfalls aufnehmen; sie
nimmt ein Passwort, das die Klasse laut eigenem Javadoc nie protokolliert.

- [ ] **Schritt 2: Die Vorgabewerte ändern sich mit keiner Sprache, über Reflexion**

```java
package kst4contest.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * No default that goes to another radio amateur changes with the interface language.
 *
 * Over every String field by reflection rather than over a maintained list: the beacon texts
 * and the auto answers are the ones anybody would think of, and they are also in every
 * operator's stored XML, so translating one would alter existing profiles. The fields nobody
 * thinks of are the reason this is reflective -- including the ones added after this test.
 */
class ChatPreferencesLocaleTest {

    private Map<String, String> stringFieldsUnder(Locale locale) throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            ChatPreferences prefs = new ChatPreferences();
            Map<String, String> values = new LinkedHashMap<>();

            for (Field field : ChatPreferences.class.getDeclaredFields()) {
                if (field.getType() != String.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                values.put(field.getName(), (String) field.get(prefs));
            }

            return values;
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void everyStringDefaultIsTheSameUnderGermanAndEnglish() throws Exception {
        assertEquals(stringFieldsUnder(Locale.UK), stringFieldsUnder(Locale.GERMANY));
    }

    @Test
    void theFourTextsThatGoToOtherRadioAmateursAreStillEnglish() throws Exception {
        /*
         * Named as well as covered reflectively: these four are in the stored XML of every
         * operator, so a change to them is a change to existing profiles. If a later stage
         * moves them, this test is the place that says it was deliberate.
         */
        Map<String, String> values = stringFieldsUnder(Locale.GERMANY);

        assertEquals("Hi, pse call us", values.get("bcn_beaconTextMainCat"));
        assertEquals("Hi, pse call us", values.get("bcn_beaconTextSecondCat"));
        org.junit.jupiter.api.Assertions.assertTrue(
                values.get("messageHandling_autoAnswerTextMainCat").startsWith("Hi, sry I am not qrv"),
                values.get("messageHandling_autoAnswerTextMainCat"));
    }
}
```

**Erwartung: dieser Test ist von Anfang an grün** — er beweist einen Zustand, der heute gilt,
und wacht darüber. Das ist Absicht und keine vergeudete Arbeit: er wird rot, wenn jemand in der
Folgearbeit einen Vorgabewert ins Bündel zieht.

- [ ] **Schritt 3: Kein Bündelwert ist Protokoll- oder Funkertext**

```kotlin
package kst4contest.view.i18n

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

/**
 * Nothing that goes to the server or to another radio amateur has got into the texts.
 *
 * The third of the three boundary checks, and the one that catches the follow-up work: the
 * remaining ~380 strings get pulled into these files over time, and a beacon template or a
 * `/CQ` sitting among them would be translated by the next contributor in good faith.
 */
class TranslationBoundaryTest {

    private val everyText: List<String> =
        Translations.BY_LANGUAGE.values.flatMap { it.values }

    @Test
    fun noTextIsAProtocolFrameOrAChatCommand() {
        val forbidden = listOf("LOGINC", "SDONE", "ACHAT", "DXQ", "SETNAME", "MYQRG")

        for (text in everyText) {
            assertTrue(
                forbidden.none { text.contains(it) },
                "'$text' contains a protocol token; that text goes to the ON4KST server and "
                        + "must not be in a translation file",
            )
            assertTrue(
                !text.trimStart().startsWith("/"),
                "'$text' looks like a chat command; those go to the server as a message",
            )
        }
    }

    @Test
    fun noTextIsOneOfThePreferenceDefaultsThatGoToOtherRadioAmateurs() {
        val prefs = ChatPreferences()

        val defaults = ChatPreferences::class.java.declaredFields
            .filter { it.type == String::class.java && !Modifier.isStatic(it.modifiers) }
            .mapNotNull { field ->
                field.isAccessible = true
                (field.get(prefs) as String?)?.takeIf { it.isNotBlank() }
            }
            .toSet()

        for (text in everyText) {
            assertTrue(
                text !in defaults,
                "'$text' is a ChatPreferences default; it goes to other radio amateurs and is "
                        + "in every operator's stored XML",
            )
        }
    }

    @Test
    fun germanIsCompleteBecauseItIsAShippedLanguage() {
        // Prüfschwerpunkt 6: only German. A contributed language may be partial by design.
        val base = Translations.BY_LANGUAGE.getValue(Translations.BASE_LANGUAGE).keys
        val german = Translations.BY_LANGUAGE.getValue("de").keys

        assertEquals(
            emptySet<String>(),
            base - german,
            "German is shipped, so a gap in it is a defect and not a contribution in progress",
        )
    }

    @Test
    fun aThirdLanguageIsNotHeldToCompleteness() {
        /*
         * Nothing here asserts over every language, and that is the point: this test exists so
         * that adding strings_fr.properties with ten keys does not turn the build red.
         */
        val held = setOf(Translations.BASE_LANGUAGE, "de")

        assertTrue(
            Translations.BY_LANGUAGE.keys.all { it in held || true },
            "placeholder-free restatement: no language outside $held is checked for coverage",
        )
    }
}
```

**Der letzte Test ist als Zusicherung schwach** — `|| true` ist immer wahr. Er steht als
*Erinnerung* im Quelltext und nicht als Prüfung; **beim Umsetzen durch etwas Echtes ersetzen
oder ganz streichen** und die Begründung stattdessen in das Javadoc von
`germanIsCompleteBecauseItIsAShippedLanguage` schreiben. Eine Zusicherung, die nicht fehlschlagen
kann, ist schlimmer als keine: sie sieht nach Abdeckung aus.

- [ ] **Schritt 4: Bauen, Testzahl prüfen, committen**

---

## Aufgabe 9: Die dreizehn Texte einsetzen — **GUI-ABNAHME**

**Dateien:**
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/ConnectionBar.kt:45,47,91,92,93,94`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/MainMenuModel.kt:48,61,86,107`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/tabs/GuiOptionsTab.kt:22,28,43`
- Ändern: `app-desktop/src/main/kotlin/kst4contest/view/compose/ComposeAlert.kt:171,188`

- [ ] **Schritt 1: Die Aufrufstellen umstellen**

Jede Compose-Aufrufstelle liest `LocalStrings.current`:

```kotlin
    val strings = LocalStrings.current
    ...
        Form.button(strings.settingsSave, onClick = onSave)
        Form.button(strings.settingsApplyAndClose, onClick = onApplyAndClose)
```

`ConnectionBar.kt:47` ist der **Platzhalterfall** und der Grund, warum `MessageFormat` da ist:

```kotlin
            var label = strings.connectionConnectTo(main.getChatCategoryName(main.categoryNumber))
```

Vorher stand dort `"Connect to " + main.getChatCategoryName(...)`. Im Deutschen steht der
Platzhalter vorn (`Mit {0} verbinden`), was durch Verketten nicht zu erreichen ist.

`MainMenuModel.kt` ist **keine** Composable-Funktion; `mainMenuModel(...)` bekommt die Texte
als Parameter:

```kotlin
fun mainMenuModel(
    state: MainMenuState,
    actions: MainMenuActions,
    settingsWindowOpen: Boolean,
    monitorWindowOpen: Boolean,
    strings: Strings,
): List<MenuSpec>
```

Die beiden Aufrufer — `Kst4ContestMenuBar` und `Kst4ContestMenuRow` — sind Composables und
geben `LocalStrings.current` weiter. Das hält das Modell frei von Compose, wie es heute ist.

- [ ] **Schritt 2: Bauen, Testzahl prüfen**

```bash
./gradlew clean build
```

Erwartung: grün. `DrawingLayerRegressionTest` und die beiden Menü-Tests prüfen Texte — schlagen
sie fehl, erwarten sie englische Literale und sind auf `strings` umzustellen, **nicht** die
Texte zurückzuändern.

- [ ] **Schritt 3: GUI-ABNAHME**

```bash
./gradlew :app-desktop:run --args="--profile default"
```

Zu prüfen:

1. GUI-Reiter → Sprache auf **Deutsch** → die drei Abschnittstitel und die Knöpfe der
   Verbindungsleiste wechseln **sofort**.
2. **Das Hauptfenster dahinter wechselt mit** — die vier Menütitel werden Datei, Optionen,
   Fenster, Info. Das ist das Abnahmekriterium; wechselt nur das Einstellungsfenster, liest es
   den Zustand nicht.
3. Verbindungsknopf: `Mit 144 MHz verbinden`, nicht `Verbinden mit 144 MHz` — der Platzhalter
   steht vorn.
4. Einstellungen speichern, beenden, neu starten → noch deutsch.
5. Zweites Profil → eigene Sprache, das erste bleibt unberührt.
6. Auf **Systemsprache** stellen → bei deutschem System deutsch, sonst englisch.
7. **Eine Verbindung zu ON4KST aufbauen und einen `/CQ` senden** — das ist die Abnahme, die
   die Abgrenzungstests nicht leisten können: nichts am Protokoll oder am Funkertext darf sich
   geändert haben.

- [ ] **Schritt 4: Committen**

---

## Aufgabe 10: Dokumentation

- [ ] **Schritt 1: Beide Handbücher**

Ein Abschnitt zur Sprachwahl in `github_docs/de-Funktionen.md` und
`github_docs/en-Features.md`: wo sie sitzt, dass sie sofort wirkt und pro Profil gilt, dass
Systemsprache die Vorgabe ist und Englisch der Rückfall, und dass ein unübersetzter Text
englisch erscheint statt zu fehlen.

- [ ] **Schritt 2: Wie man eine Sprache beiträgt**

Das ist der Zweck der Auslagerung und gehört dorthin, wo ein Beitragender sucht. In
`github_docs/en-Features.md` (englisch, weil Beitragende von außen kommen) und als Hinweis in
`README.md`: Datei kopieren, Werte übersetzen, `./gradlew :app-desktop:classes` baut und meldet
Fehler, eine unvollständige Datei ist willkommen. Dazu das Arbeitsblatt
`strings_<lang>.todo.properties` erwähnen.

- [ ] **Schritt 3: `CLAUDE.md` und `docs/PROJECT_CONTEXT.md`**

`CLAUDE.md` sagt heute „Das Projekt besteht aus zwei Modulen" — es sind drei, und das dritte
wird nicht ausgeliefert. In `PROJECT_CONTEXT.md` die dauerhaften Entscheidungen: die vier
Textsorten und die Regel des Adressaten, dass `core` keinen Anzeigetext erzeugt,
„Unvollständigkeit ist erlaubt, Unrichtigkeit nicht", und dass die Stelligkeit aus der
englischen Grundlage kommt.

- [ ] **Schritt 4: Veraltete Aussagen suchen**

```bash
grep -rniE "englisch|english|sprache|language" github_docs/*.md docs/PROJECT_CONTEXT.md CLAUDE.md \
  | grep -viE "changelog" | head -20
```

Alles, was sagt, die Oberfläche sei englisch, ist jetzt unvollständig.

- [ ] **Schritt 5: Committen**

---

## Was unverändert bleibt

- Die vier `ChatPreferences`-Vorgabewerte und alles in der gespeicherten XML.
- `core` bekommt keine Nachschlagefunktion und erzeugt keinen Anzeigetext. Die beiden
  bekannten Verstöße (`PathPropagationAssessment`, `PriorityCalculator`) bleiben englisch.
- Die Reiterreihenfolge im Einstellungsfenster; die Sprachwahl kommt als letzter Abschnitt in
  den bestehenden GUI-Reiter.
- `CONFIG_VERSION` bleibt 7.
- Keine neue Abhängigkeit, weder im Jar noch im Bau.
- Quelltextkommentare und Javadoc bleiben ausschließlich Englisch.
- Die übrigen ~380 Texte bleiben englisch, mit der Regel aus den Prüfschwerpunkten für die
  Folgearbeit.

## Bekannte Fallstricke

- **`buildSrc` ist keine Option** — gemessen, seine Tests laufen bei `./gradlew build` nicht
  mit. Deshalb das eigene Modul.
- **Kotlins Vorgabewerte sind für Java unsichtbar.** Jede Java-Aufrufstelle übergibt den Store
  ausdrücklich; Etappe 8 hat das auf die harte Art gelernt.
- **`ComposeStationMapDrawingTest` ist flaky** unter paralleler Last. Bei Rot zuerst isoliert
  nachprüfen.
- **PMD/SpotBugs** melden rund 4448 vorbestehende Befunde und brechen den Build nicht. Das
  erzeugte `Strings.kt` liegt unter `build/` und wird davon nicht erfaßt.
- **Eine Zahl aus diesem Plan ist keine Messung.** Die Testzahlen stehen als Erwartung da;
  abgelesen wird aus der XML.
