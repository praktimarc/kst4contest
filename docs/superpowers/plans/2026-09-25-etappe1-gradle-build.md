# Etappe 1 — Gradle-Build, zwei Module, JPMS-Ausbau: Umsetzungsplan

> **Für agentische Bearbeiter:** ERFORDERLICHE UNTER-SKILL: superpowers:subagent-driven-development (empfohlen) oder superpowers:executing-plans, um diesen Plan Aufgabe für Aufgabe umzusetzen. Schritte nutzen Checkbox-Syntax (`- [ ]`).

**Ziel:** Die unveränderte JavaFX-Anwendung baut, testet und paketiert unter Gradle in zwei Modulen, ohne JPMS.

**Architektur:** Ein Gradle-Multiprojekt mit `core` (Fachlichkeit) und `app-desktop` (Oberfläche). Paketnamen und Importe ändern sich **nicht** — Gradle-Module erzwingen keine Paketumbenennung, die Aufteilung ist ein reines Verschieben von Verzeichnissen. `module-info.java` entfällt; die Anwendung läuft künftig vom Klassenpfad. Das Packaging ruft `jpackage` weiterhin direkt auf, nur aus einer Gradle-Aufgabe statt aus Maven.

**Tech-Stack:** Gradle 8.10, Java 21, JavaFX 21.0.5 (unverändert), `org.openjfx.javafxplugin` 0.1.0, SpotBugs- und PMD-Gradle-Plugins, `jpackage` aus dem JDK.

**Spec:** `docs/superpowers/specs/2026-09-25-compose-migration-design.md`

## Zwei Präzisierungen gegenüber dem Spec

Beide sind Verkleinerungen des Risikos, keine Zieländerungen. Sie sind hier festgehalten, weil das Spec sie noch nicht kennt.

1. **`core` darf in Etappe 1 noch von JavaFX abhängen.** Das Spec trennt Modulaufteilung (Etappe 1) und JavaFX-Ausbau (Etappe 2). Da `ChatController`, `ChatMember`, `ChatPreferences` und `ScoreService` heute JavaFX benutzen, deklariert `core` die JavaFX-Abhängigkeit in Etappe 1 weiterhin. Etappe 2 entfernt sie. Damit bleibt Etappe 1 eine reine Build-Umstellung ohne fachliche Änderung.

2. **`compose.desktop.nativeDistributions` kommt erst in Etappe 3.** Das Spec nennt es für Etappe 1. Das Compose-Gradle-Plugin bündelt seine Laufzeit über `jlink`, und JavaFX ohne `module-info.java` vom Klassenpfad zu jlinken ist ein ungelöster Nebenschauplatz. Etappe 1 behält deshalb den heutigen, erprobten Weg: direkter `jpackage`-Aufruf auf ein Verzeichnis mit JAR und Abhängigkeiten. Auf `nativeDistributions` wird umgestellt, wenn mit Etappe 3 ohnehin Kotlin und Compose einziehen und JavaFX aus dem Packaging verschwindet.

## Globale Randbedingungen

- Java 21. `sourceCompatibility` und `targetCompatibility` bleiben 21.
- JavaFX bleibt auf 21.0.5. Kein Versionssprung in dieser Etappe.
- Projektversion bleibt `1.50.0-nightly`, identisch zum heutigen `pom.xml:9`.
- Hauptklasse bleibt `kst4contest.view.Kst4ContestApplication` (`pom.xml:26`).
- Anwendungsname bleibt `praktiKST` (`pom.xml:8`), da Launcher-, Paket- und Verzeichnisnamen daran hängen.
- **Keine Quelldatei unter `src/main/java/kst4contest/` wird inhaltlich geändert**, mit einer Ausnahme: `module-info.java` wird gelöscht. Dateien werden verschoben, nicht bearbeitet.
- Paketnamen und `import`-Anweisungen bleiben unverändert.
- Kommentare und Javadoc auf Englisch; Commit-Nachrichten knapp auf Englisch.
- Die Versionen der Gradle-Plugins werden zu Beginn von Aufgabe 1 gegen die aktuellen Veröffentlichungen geprüft; die hier genannten sind der Stand vom 2026-09-25.

## Prüfschwerpunkte

Build-Umstellungen brechen typischerweise nicht beim Übersetzen, sondern zur Laufzeit. Diese fünf Klassen von Fehlern deckt kein Kompilierlauf ab; jede bekommt ihren Test in der zuständigen Aufgabe.

1. **Ressourcen fehlen im JAR.** `praktiKST.db`, `praktiKSTpreferences.xml`, CSS und die Karten-HTML werden über `getResourceAsStream` geladen. Ein falsch zugeordnetes `resources`-Verzeichnis fällt erst beim Start auf. → Aufgabe 4.
2. **JavaFX-Natives fehlen.** Ohne `module-info.java` lädt JavaFX vom Klassenpfad und braucht die plattformspezifischen Klassifikator-Artefakte. Fehlen sie, startet die Anwendung nicht. → Aufgabe 3.
3. **`jdk.jsobject` fehlt zur Laufzeit.** Die Karten-Brücke `StationMapBridge` benutzt `netscape.javascript.JSObject`. Das Modul stand bisher in `module-info.java` und in der jpackage-`addmodules`-Liste. Ohne JPMS muss es über `--add-modules` beim jpackage-Lauf erhalten bleiben. → Aufgabe 8.
4. **SQLite-Treiber wird nicht gefunden.** `sqlite-jdbc` lädt über den `ServiceLoader`; ein verlorener `META-INF/services`-Eintrag beim Zusammenstellen des Verzeichnisses zeigt sich erst beim ersten Datenbankzugriff. → Aufgabe 4.
5. **Die Versionsauflösung bricht.** Zehn Stellen in fünf Workflows und zwei PKGBUILDs lesen die Projektversion mit `grep -m1 '<version>' pom.xml`. Wird `pom.xml` gelöscht, liefern sie eine leere Version und die Artefaktnamen werden falsch — ohne dass der Build scheitert. → Aufgaben 1, 9, 10 und 12.
6. **Das Paket startet, die Anwendung nicht.** Ein `jpackage`-Image kann sich erzeugen lassen und beim Start scheitern. Jede Packaging-Aufgabe endet mit einem echten Start des erzeugten Artefakts. → Aufgaben 8 und 9.

---

## Aufgabe 1: Gradle-Gerüst neben Maven

Maven bleibt in dieser Aufgabe vollständig funktionsfähig. Erst Aufgabe 12 entfernt es.

**Dateien:**
- Anlegen: `settings.gradle.kts`
- Anlegen: `build.gradle.kts`
- Anlegen: `gradle/libs.versions.toml`
- Anlegen: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` (durch `gradle wrapper` erzeugt)
- Ändern: `.gitignore`

**Schnittstellen:**
- Erzeugt: die Projekte `:core` und `:app-desktop`, auf die alle folgenden Aufgaben verweisen.

- [ ] **Schritt 1: Aktuelle Plugin-Versionen prüfen**

Vor dem Schreiben der Dateien die aktuellen Veröffentlichungen nachsehen:
- Gradle (hier angenommen: 8.10)
- `org.openjfx.javafxplugin` (hier angenommen: 0.1.0)
- `com.github.spotbugs` Gradle-Plugin (hier angenommen: 6.0.26)

Weichen sie ab, die neueren Versionen verwenden und die Abweichung im Ledger vermerken.

- [ ] **Schritt 2: Wrapper erzeugen**

Ausführen: `gradle wrapper --gradle-version 8.10`
Erwartet: `gradlew`, `gradlew.bat` und `gradle/wrapper/` entstehen.

Ist kein Gradle installiert, den Wrapper aus einer temporären Gradle-Distribution erzeugen.

- [ ] **Schritt 3: `settings.gradle.kts` schreiben**

```kotlin
rootProject.name = "kst4contest"

include(":core")
include(":app-desktop")
```

- [ ] **Schritt 3b: `gradle.properties` schreiben**

```properties
version=1.50.0-nightly
group=de.praktimarc
org.gradle.parallel=true
org.gradle.caching=true
```

Diese Datei ist ab Aufgabe 9 die **einzige** Quelle der Projektversion für CI und Paketbau. Sie ist bewusst so einfach gehalten, dass ein `grep` sie lesen kann, ohne Gradle zu starten.

`version` und `group` müssen den Werten aus `pom.xml:9` und dem dortigen `groupId` entsprechen.

- [ ] **Schritt 4: `gradle/libs.versions.toml` schreiben**

```toml
[versions]
java = "21"
javafx = "21.0.5"
javafxPlugin = "0.1.0"
sqliteJdbc = "3.46.1.3"
jlayer = "1.0.1"
junit = "5.10.2"
mockito = "5.12.0"
spotbugsPlugin = "6.0.26"

[libraries]
sqlite-jdbc = { module = "org.xerial:sqlite-jdbc", version.ref = "sqliteJdbc" }
jlayer = { module = "javazoom:jlayer", version.ref = "jlayer" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
mockito-junit-jupiter = { module = "org.mockito:mockito-junit-jupiter", version.ref = "mockito" }

[plugins]
javafx = { id = "org.openjfx.javafxplugin", version.ref = "javafxPlugin" }
spotbugs = { id = "com.github.spotbugs", version.ref = "spotbugsPlugin" }
```

Die Bibliotheksversionen müssen den Werten in `pom.xml` entsprechen. Vor dem Schreiben mit `grep -n "sqlite-jdbc" -A2 pom.xml` und den entsprechenden Blöcken abgleichen und bei Abweichung die Werte aus `pom.xml` übernehmen.

- [ ] **Schritt 5: `build.gradle.kts` im Wurzelverzeichnis schreiben**

```kotlin
plugins {
    java
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
```

`group` und `version` kommen aus `gradle.properties`. Prüfen: `./gradlew -q properties | grep -E "^(group|version):"` — erwartet `group: de.praktimarc` und `version: 1.50.0-nightly`, übereinstimmend mit `pom.xml:9`.

- [ ] **Schritt 6: Modulverzeichnisse und Platzhalter-Builds anlegen**

```bash
mkdir -p core/src/main/java app-desktop/src/main/java
printf 'plugins {\n    java\n}\n' > core/build.gradle.kts
printf 'plugins {\n    java\n}\n' > app-desktop/build.gradle.kts
```

- [ ] **Schritt 7: `.gitignore` ergänzen**

Folgende Zeilen anfügen:

```
.gradle/
build/
core/build/
app-desktop/build/
```

- [ ] **Schritt 8: Gerüst prüfen**

Ausführen: `./gradlew projects`
Erwartet: Ausgabe listet `Root project 'kst4contest'` mit den Unterprojekten `core` und `app-desktop`.

- [ ] **Schritt 9: Maven ist unberührt**

Ausführen: `./mvnw -q -DskipTests compile`
Erwartet: Erfolg. Die Gradle-Dateien dürfen den Maven-Build nicht stören.

- [ ] **Schritt 10: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle core/build.gradle.kts app-desktop/build.gradle.kts gradlew gradlew.bat .gitignore
git commit -m "Add the Gradle multi-project skeleton alongside Maven"
```

---

## Aufgabe 2: Quellen auf die beiden Module verteilen

**Dateien:**
- Verschieben: `src/main/java/kst4contest/{model,controller,logic,utils,locatorUtils,service}` → `core/src/main/java/kst4contest/`
- Verschieben: `src/main/java/kst4contest/{view,test}` → `app-desktop/src/main/java/kst4contest/`
- Löschen: `src/main/java/module-info.java`
- Ändern: `core/build.gradle.kts`, `app-desktop/build.gradle.kts`

**Schnittstellen:**
- Konsumiert: die Projekte `:core` und `:app-desktop` aus Aufgabe 1.
- Erzeugt: `:app-desktop` hängt über `implementation(project(":core"))` an `:core`.

- [ ] **Schritt 1: Aufteilung festlegen und prüfen**

Ausführen: `find src/main/java/kst4contest -maxdepth 1 -mindepth 1 -type d | sort`
Erwartet: `controller`, `locatorUtils`, `logic`, `model`, `service`, `test`, `utils`, `view`.

`view` (samt `view/map`) geht nach `app-desktop`, alles andere nach `core`. `kst4contest.test` enthält Hilfsklassen mit JavaFX-Bezug und geht ebenfalls nach `app-desktop`.

- [ ] **Schritt 2: Verschieben**

```bash
mkdir -p core/src/main/java/kst4contest app-desktop/src/main/java/kst4contest
git mv src/main/java/kst4contest/model core/src/main/java/kst4contest/
git mv src/main/java/kst4contest/controller core/src/main/java/kst4contest/
git mv src/main/java/kst4contest/logic core/src/main/java/kst4contest/
git mv src/main/java/kst4contest/utils core/src/main/java/kst4contest/
git mv src/main/java/kst4contest/locatorUtils core/src/main/java/kst4contest/
git mv src/main/java/kst4contest/service core/src/main/java/kst4contest/
git mv src/main/java/kst4contest/view app-desktop/src/main/java/kst4contest/
git mv src/main/java/kst4contest/test app-desktop/src/main/java/kst4contest/
git rm src/main/java/module-info.java
```

- [ ] **Schritt 3: `core/build.gradle.kts` schreiben**

```kotlin
plugins {
    java
    alias(libs.plugins.javafx)
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.media")
}

dependencies {
    implementation(libs.sqlite.jdbc)
    implementation(libs.jlayer)
}
```

> JavaFX steht hier bewusst noch drin. `ChatController`, `ChatMember`, `ChatPreferences` und `ScoreService` benutzen es. Etappe 2 entfernt diesen Block.

- [ ] **Schritt 4: `app-desktop/build.gradle.kts` schreiben**

```kotlin
plugins {
    java
    application
    alias(libs.plugins.javafx)
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.media")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.sqlite.jdbc)
    implementation(libs.jlayer)
}

application {
    mainClass.set("kst4contest.view.Kst4ContestApplication")
}
```

- [ ] **Schritt 5: Übersetzen**

Ausführen: `./gradlew :core:compileJava`
Erwartet: BUILD SUCCESSFUL.

Scheitert es mit fehlenden Typen aus `kst4contest.view`, ist eine Klasse falsch einsortiert. Die betroffene Klasse ermitteln, prüfen ob sie Fachlichkeit oder Oberfläche ist, und entsprechend verschieben. Jede solche Entscheidung als `Ruling:` ins Ledger.

- [ ] **Schritt 6: `app-desktop` übersetzen**

Ausführen: `./gradlew :app-desktop:compileJava`
Erwartet: BUILD SUCCESSFUL.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Split the sources into the core and app-desktop modules"
```

---

## Aufgabe 3: JavaFX-Natives und Start aus Gradle

Deckt Prüfschwerpunkt 2 ab.

**Dateien:**
- Ändern: `app-desktop/build.gradle.kts`

- [ ] **Schritt 1: Start versuchen**

Ausführen: `./gradlew :app-desktop:run`
Erwartet: Das Hauptfenster erscheint.

Erscheint stattdessen eine Meldung wie `Graphics Device initialization failed` oder `no javafx.graphics in java.library.path`, fehlen die plattformspezifischen JavaFX-Artefakte. Weiter mit Schritt 2. Startet die Anwendung, Schritt 2 überspringen und im Ledger vermerken.

- [ ] **Schritt 2: Klassifikator-Abhängigkeiten ergänzen**

In `app-desktop/build.gradle.kts` vor dem `dependencies`-Block einfügen:

```kotlin
val javafxPlatform = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX ->
        if (System.getProperty("os.arch") == "aarch64") "mac-aarch64" else "mac"
    else -> "linux"
}
```

und im `dependencies`-Block:

```kotlin
listOf("javafx-base", "javafx-graphics", "javafx-controls", "javafx-fxml", "javafx-web", "javafx-media")
    .forEach { module ->
        runtimeOnly("org.openjfx:$module:${libs.versions.javafx.get()}:$javafxPlatform")
    }
```

- [ ] **Schritt 3: Start erneut versuchen**

Ausführen: `./gradlew :app-desktop:run`
Erwartet: Das Hauptfenster erscheint.

- [ ] **Schritt 4: Commit**

```bash
git add app-desktop/build.gradle.kts
git commit -m "Resolve the JavaFX native artifacts for the Gradle run task"
```

---

## Aufgabe 4: Ressourcen und Tests zuordnen

Deckt Prüfschwerpunkte 1 und 4 ab.

**Dateien:**
- Verschieben: `src/main/resources/*` → `core/src/main/resources/` bzw. `app-desktop/src/main/resources/`
- Verschieben: `src/test/java/kst4contest/*` → `core/src/test/java/kst4contest/` bzw. `app-desktop/src/test/java/kst4contest/`
- Ändern: `core/build.gradle.kts`, `app-desktop/build.gradle.kts`

**Schnittstellen:**
- Konsumiert: die Modulaufteilung aus Aufgabe 2.

**Zuordnungsregel:** `app-desktop` sieht die Ressourcen von `core` zur Laufzeit, umgekehrt nicht. **Im Zweifel gehört eine Ressource nach `core`.** Eine fälschlich nach `core` gelegte Ressource ist folgenlos; eine fälschlich nach `app-desktop` gelegte bricht `core` zur Laufzeit.

- [ ] **Schritt 1: Ressourcen verschieben**

```bash
mkdir -p core/src/main/resources app-desktop/src/main/resources
git mv src/main/resources/praktiKST.db core/src/main/resources/
git mv src/main/resources/praktiKSTpreferences.xml core/src/main/resources/
git mv src/main/resources/praktiKSTpreferences.old core/src/main/resources/
git mv src/main/resources/icons core/src/main/resources/
for f in src/main/resources/*.mp3; do git mv "$f" core/src/main/resources/; done
git mv src/main/resources/leaflet-1.9.4.css app-desktop/src/main/resources/
git mv src/main/resources/leaflet-1.9.4.js app-desktop/src/main/resources/
git mv src/main/resources/KST4ContestDefaultDay.css app-desktop/src/main/resources/
git mv src/main/resources/KST4ContestDefaultEvening.css app-desktop/src/main/resources/
git mv src/main/resources/web app-desktop/src/main/resources/
```

- [ ] **Schritt 2: Tests verschieben**

```bash
mkdir -p core/src/test/java/kst4contest app-desktop/src/test/java/kst4contest
git mv src/test/java/kst4contest/controller core/src/test/java/kst4contest/
git mv src/test/java/kst4contest/test core/src/test/java/kst4contest/
git mv src/test/java/kst4contest/view app-desktop/src/test/java/kst4contest/
```

- [ ] **Schritt 3: Testabhängigkeiten ergänzen**

In `core/build.gradle.kts` und `app-desktop/build.gradle.kts` jeweils im `dependencies`-Block anfügen:

```kotlin
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
```

- [ ] **Schritt 4: Tests übersetzen**

Ausführen: `./gradlew :core:compileTestJava :app-desktop:compileTestJava`
Erwartet: BUILD SUCCESSFUL.

Scheitert eine Testklasse in `core` an einem Typ aus `kst4contest.view`, gehört sie nach `app-desktop`. Verschieben und als `Ruling:` ins Ledger.

- [ ] **Schritt 5: Tests ausführen**

Ausführen: `./gradlew test`
Erwartet: BUILD SUCCESSFUL. Die Summe der **ausgeführten Testfälle** beider Module beträgt **208**.

Die Zahl prüfen:
`find core/build/test-results app-desktop/build/test-results -name "TEST-*.xml" -exec grep -ho 'tests="[0-9]*"' {} + | grep -o '[0-9]*' | paste -sd+ | bc`
Erwartet: `208`.

Die Zahl ist nicht die der `@Test`-Annotationen (148): sechs `@ParameterizedTest`-Methoden expandieren zu 60 Fällen. Die belastbare Invariante ist die Dateizahl:

`find core/src/test app-desktop/src/test -name "*.java" | wc -l`
Erwartet: `43` — dieselbe Zahl wie vorher unter `src/test/java`.

Weicht eine der Zahlen ab, fehlt eine Testklasse oder wurde eine doppelt eingebunden. Ursache suchen, nicht die Erwartung anpassen.

- [ ] **Schritt 6: Anwendung starten und Datenbank prüfen**

Ausführen: `./gradlew :app-desktop:run`
Erwartet: Das Hauptfenster erscheint, ohne dass auf der Konsole `SQLException`, `ClassNotFoundException: org.sqlite.JDBC` oder eine Meldung über eine fehlende Ressource erscheint.

Im laufenden Programm das Einstellungsfenster öffnen und den Reiter **Workedstn database** aufrufen.
Erwartet: Der Reiter zeigt Zahlen aus der Datenbank, keine Fehlermeldung. Das beweist, dass `praktiKST.db` gefunden und der SQLite-Treiber über den `ServiceLoader` geladen wurde.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Assign resources and tests to the core and app-desktop modules"
```

---

## Aufgabe 5: Lombok entfernen

**Dateien:**
- Ändern: `pom.xml`

- [ ] **Schritt 1: Nachweisen, dass Lombok ungenutzt ist**

Ausführen: `grep -rl "lombok" core/src app-desktop/src | wc -l`
Erwartet: `0`.

Ist die Zahl größer als 0, wird Lombok doch benutzt. Dann diese Aufgabe überspringen, im Ledger als `Ruling:` festhalten und mit Aufgabe 6 weitermachen.

- [ ] **Schritt 2: Aus `pom.xml` entfernen**

Den `<dependency>`-Block mit `<artifactId>lombok</artifactId>` (ab `pom.xml:114`) und den `<annotationProcessorPath>`-Eintrag für Lombok im `maven-compiler-plugin` (ab `pom.xml:251`) löschen.

In den Gradle-Dateien taucht Lombok nicht auf, dort ist nichts zu tun.

- [ ] **Schritt 3: Beide Builds prüfen**

Ausführen: `./gradlew build -x test`
Erwartet: BUILD SUCCESSFUL.

Für den Maven-Build gilt ab hier: die Enforcer-Ausführung `verify-packaging-module-list` wurde bereits in dieser Aufgabe entfernt, weil das Löschen von `module-info.java` in Aufgabe 2 sie sonst dauerhaft brechen würde. Ohne sie übersetzt Maven weiter:

Ausführen: `./mvnw -q -DskipTests compile`
Erwartet: Erfolg.

- [ ] **Schritt 4: Commit**

```bash
git add pom.xml
git commit -m "Drop the unused Lombok dependency"
```

---

## Aufgabe 6: SpotBugs und PMD unter Gradle

**Dateien:**
- Ändern: `build.gradle.kts`
- Unverändert: `pmd-ruleset.xml`

- [ ] **Schritt 1: Heutige Konfiguration ablesen**

Ausführen: `grep -n -A12 "spotbugs-maven-plugin" pom.xml && grep -n -A12 "maven-pmd-plugin" pom.xml`

Aus der Ausgabe `effort`, `threshold`, `failOnViolation` und den Verweis auf `pmd-ruleset.xml` notieren. Diese Werte werden im nächsten Schritt übernommen, nicht neu erfunden.

- [ ] **Schritt 2: Plugins im Wurzel-Build ergänzen**

In `build.gradle.kts` den `plugins`-Block erweitern:

```kotlin
plugins {
    java
    alias(libs.plugins.spotbugs) apply false
    pmd apply false
}
```

und im `subprojects`-Block anfügen:

```kotlin
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "pmd")

    extensions.configure<PmdExtension> {
        toolVersion = "7.7.0"
        ruleSetFiles = files(rootProject.file("pmd-ruleset.xml"))
        ruleSets = emptyList()
        isIgnoreFailures = true
    }
```

`isIgnoreFailures` entspricht dem heutigen `failOnViolation`. Steht dort `true`, muss hier `false` stehen.

- [ ] **Schritt 3: Analysen ausführen**

Ausführen: `./gradlew pmdMain spotbugsMain`
Erwartet: BUILD SUCCESSFUL. Berichte liegen unter `core/build/reports/` und `app-desktop/build/reports/`.

- [ ] **Schritt 4: Commit**

```bash
git add build.gradle.kts
git commit -m "Run SpotBugs and PMD from the Gradle build"
```

---

## Aufgabe 7: jpackage-Image aus Gradle

**Dateien:**
- Ändern: `app-desktop/build.gradle.kts`

**Schnittstellen:**
- Erzeugt: die Gradle-Aufgabe `:app-desktop:packageImage`, die aus `app-desktop/build/dist-libs/` (JAR als `app.jar`) ein `jpackage`-Image unter `app-desktop/build/jpackage/` erzeugt. Aufgaben 9, 10 und 11 rufen sie auf.

Die heutige Maven-Strecke sammelt JAR und Abhängigkeiten in `target/modules` und ruft darauf `jpackage --type IMAGE`. Gradle bekommt dieselbe Strecke.

- [ ] **Schritt 1: Sammel- und Packaging-Aufgabe ergänzen**

An `app-desktop/build.gradle.kts` anfügen:

```kotlin
val collectRuntime by tasks.registering(Sync::class) {
    dependsOn(tasks.jar)
    from(tasks.jar) { rename { "app.jar" } }
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("dist-libs"))
}

val packageImage by tasks.registering(Exec::class) {
    dependsOn(collectRuntime)
    val input = layout.buildDirectory.dir("dist-libs").get().asFile
    val output = layout.buildDirectory.dir("jpackage").get().asFile
    val os = org.gradle.internal.os.OperatingSystem.current()
    val icon = when {
        os.isWindows -> "packaging/icons/kst4contest.ico"
        os.isMacOsX -> "packaging/icons/kst4contest.icns"
        else -> "packaging/icons/kst4contest.png"
    }
    doFirst { output.deleteRecursively() }
    commandLine(
        "jpackage",
        "--icon", rootProject.file(icon).absolutePath,
        "--type", "app-image",
        "--name", "praktiKST",
        "--input", input.absolutePath,
        "--dest", output.absolutePath,
        "--main-jar", "app.jar",
        "--main-class", "kst4contest.view.Kst4ContestApplication",
        "--java-options", "-Dfile.encoding=UTF-8"
    )
}
```

> `--icon` ist nicht optional: ohne dieses Argument liefert `jpackage` stillschweigend sein Standardsymbol aus. Alle heutigen CI-Aufrufe setzen es (`nightly-artifacts.yml:72`, `:130` und weitere).

- [ ] **Schritt 2: Image bauen**

Ausführen: `./gradlew :app-desktop:packageImage`
Erwartet: BUILD SUCCESSFUL, und `app-desktop/build/jpackage/praktiKST/` existiert.

Prüfen: `ls app-desktop/build/jpackage/praktiKST/bin/`
Erwartet: eine ausführbare Datei `praktiKST`.

- [ ] **Schritt 3: Erzeugtes Image starten**

Ausführen: `./app-desktop/build/jpackage/praktiKST/bin/praktiKST`
Erwartet: Das Hauptfenster erscheint.

Scheitert der Start an fehlenden JavaFX-Klassen, enthält `runtime-input` die Klassifikator-Artefakte nicht. Dann in Schritt 1 `configurations.runtimeClasspath` gegen eine Konfiguration prüfen, die die `runtimeOnly`-Einträge aus Aufgabe 3 enthält.

> Das Verzeichnis heißt bewusst `dist-libs` und das Haupt-JAR bewusst `app.jar`: die CI-Strecken in `.github/workflows/` rufen `jpackage` heute auf genau diese Form auf. Damit ändert Aufgabe 9 nur die Build-Befehle, nicht die `jpackage`-Aufrufe.

- [ ] **Schritt 4: Commit**

```bash
git add app-desktop/build.gradle.kts
git commit -m "Build the jpackage app-image from Gradle"
```

---

## Aufgabe 8: JPMS-Reste entfernen und `--add-modules` sichern

Deckt Prüfschwerpunkte 3 und 5 ab.

**Dateien:**
- Ändern: `app-desktop/build.gradle.kts`
- Löschen: `packaging/AddModules.java`
- Ändern: `pom.xml` (Enforcer-Ausführung `verify-packaging-module-list`, ab `pom.xml:225`)

- [ ] **Schritt 1: Die Karten-Brücke ohne `--add-modules` scheitern sehen**

Das Image aus Aufgabe 7 starten und das Kartenfenster öffnen.
Erwartet: Entweder erscheint die Karte nicht, oder auf der Konsole steht ein Fehler zu `netscape.javascript.JSObject` bzw. `jdk.jsobject`.

Erscheint die Karte fehlerfrei, ist `jdk.jsobject` in der Standard-Laufzeit enthalten. Das im Ledger festhalten und Schritt 2 trotzdem ausführen — die Liste sichert auch die übrigen Module ab.

- [ ] **Schritt 2: `--add-modules` in die Gradle-Aufgabe übernehmen**

Die heutige Liste steht in `pom.xml` ab `:478`. Ohne `javafx.*` (JavaFX kommt vom Klassenpfad) bleibt:

In `app-desktop/build.gradle.kts` die `commandLine`-Liste in `packageImage` um folgende Einträge **vor** `--java-options` erweitern:

```kotlin
        "--add-modules",
        "java.desktop,java.net.http,java.sql,jdk.crypto.ec,jdk.jsobject,jdk.net,jdk.xml.dom",
```

- [ ] **Schritt 3: Neu bauen und Karte prüfen**

Ausführen: `./gradlew :app-desktop:packageImage && ./app-desktop/build/jpackage/praktiKST/bin/praktiKST`
Erwartet: Das Hauptfenster erscheint, das Kartenfenster zeigt Kacheln und Stationsmarker, und auf der Konsole steht kein Fehler zu `JSObject`.

- [ ] **Schritt 4: Helfer und Enforcer-Regel entfernen**

```bash
git rm packaging/AddModules.java
```

Die Enforcer-Ausführung `verify-packaging-module-list` samt `exec-maven-plugin`-Aufruf und erklärendem Kommentar wurde bereits in Aufgabe 5 aus `pom.xml` entfernt — sie brach ab Aufgabe 2, weil `module-info.java` dort entfiel. Hier ist nur noch zu prüfen:

Ausführen: `grep -c "verify-packaging-module-list" pom.xml`
Erwartet: `0`.

- [ ] **Schritt 5: Prüfen, dass nichts mehr auf den Helfer verweist**

Ausführen: `grep -rln "AddModules" .github/ packaging/ pom.xml`
Erwartet: `.github/workflows/pr-compile-check.yml`, `.github/workflows/nightly-artifacts.yml`, `.github/workflows/tagged-release.yml`, `packaging/macos/build-signed-dmg.sh`, `packaging/aur/kst4contest/PKGBUILD`, `packaging/aur/kst4contest-git/PKGBUILD` und ein erklärender Kommentar in `pom.xml`. Zusammen 18 Aufrufstellen — sie werden in den Aufgaben 9, 10 und 11 auf `gradle.properties` umgestellt.

Die Modulliste ist ohne `module-info.java` eine Konstante geworden. Damit sie nicht über 18 Stellen driftet — genau das verhinderte bisher `AddModules.java` —, steht sie als `jpackageAddModules` in `gradle.properties`, neben der Version. CI und PKGBUILDs lesen sie mit demselben `grep` wie die Version:

```bash
ADD_MODULES=$(grep -m1 '^jpackageAddModules=' gradle.properties | cut -d= -f2)
```

- [ ] **Schritt 6: Commit**

```bash
git add -A
git commit -m "Drop the JPMS module list helper and pin the runtime modules in Gradle"
```

---

## Aufgabe 9: PR-Prüfung und Nightly-Strecke auf Gradle

Deckt Prüfschwerpunkt 5 auf Windows und Linux ab — dort ist die CI die einzige Prüfmöglichkeit.

**Dateien:**
- Ändern: `.github/workflows/pr-compile-check.yml`
- Ändern: `.github/workflows/nightly-artifacts.yml`

**Schnittstellen:**
- Konsumiert: `:app-desktop:collectRuntime` aus Aufgabe 7, das `app-desktop/build/dist-libs/` mit `app.jar` füllt.

Das Muster ist in allen Strecken gleich. Ersetzt wird jeweils nur der Build-Schritt; die `jpackage`-Aufrufe bleiben, nur ihr `--input`-Pfad wechselt von `target/dist-libs` auf `app-desktop/build/dist-libs`.

- [ ] **Schritt 1: `pr-compile-check.yml` anpassen**

Ersetzen:

```yaml
      - name: Ensure mvnw is executable
        run: chmod +x mvnw
      - name: Verify packaging module list
        run: java packaging/AddModules.java --verify-pom
      - name: Compile
        run: ./mvnw -B -DskipTests compile
```

durch:

```yaml
      - name: Ensure gradlew is executable
        run: chmod +x gradlew
      - name: Compile
        run: ./gradlew -S compileJava compileTestJava
```

Der Schritt „Verify packaging module list" entfällt ersatzlos — `packaging/AddModules.java` wurde in Aufgabe 8 gelöscht.

- [ ] **Schritt 2: Prüfen, dass kein Verweis auf den Helfer bleibt**

Ausführen: `grep -rn "AddModules" .github/ packaging/ pom.xml`
Erwartet: keine Ausgabe.

- [ ] **Schritt 3: `nightly-artifacts.yml` — Build-Schritte ersetzen**

`nightly-artifacts.yml` hat sieben bauende Jobs: `build-windows-zip` (`:25`), `build-linux-appimage` (`:95`), `build-linux-deb` (`:177`), `build-linux-rpm` (`:238`), `build-linux-arch` (`:299`), `build-flatpak` (`:420`) und `build-macos-dmg` (`:611`). `publish-flatpak-repo` (`:550`) baut nicht und bleibt unverändert. In **jedem** der sieben Jobs die Zeilenpaare

```yaml
      - name: Ensure mvnw is executable
        run: chmod +x mvnw
```

ersetzen durch

```yaml
      - name: Ensure gradlew is executable
        run: chmod +x gradlew
```

und jede Zeile der Form

```
./mvnw -B -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dist-libs
cp "$(ls -t target/praktiKST-*.jar | head -n 1)" target/dist-libs/app.jar
```

ersetzen durch die eine Zeile

```
./gradlew -S :app-desktop:collectRuntime
```

Auf Windows lautet die Entsprechung `.\gradlew.bat -S :app-desktop:collectRuntime` anstelle von `.\mvnw.cmd ...`.

- [ ] **Schritt 4: `--input`-Pfade umstellen**

In jedem `jpackage`-Aufruf in `nightly-artifacts.yml` das Argument `--input target/dist-libs` durch `--input app-desktop/build/dist-libs` ersetzen.

Ausführen: `grep -n "target/dist-libs" .github/workflows/nightly-artifacts.yml`
Erwartet: keine Ausgabe.

- [ ] **Schritt 5: `--add-modules` in der CI nachziehen**

Jeder `jpackage`-Aufruf, der bisher auf die JPMS-Modulliste gebaut hat, braucht dieselbe Liste wie Aufgabe 8:

```
--add-modules java.desktop,java.net.http,java.sql,jdk.crypto.ec,jdk.jsobject,jdk.net,jdk.xml.dom
```

Enthält ein Aufruf bereits `--add-modules` mit `javafx.*`-Einträgen, werden die `javafx.*`-Einträge gestrichen und die übrigen auf obige Liste gebracht.

- [ ] **Schritt 5b: Versionsauflösung umstellen**

Jeder bauende Job beginnt mit einem Schritt „Resolve nightly version info", der so aussieht:

```yaml
          VERSION=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
```

Diese Zeile in **jedem** Job ersetzen durch:

```yaml
          VERSION=$(grep -m1 '^version=' gradle.properties | cut -d= -f2)
```

Ausführen: `grep -c "pom.xml" .github/workflows/nightly-artifacts.yml`
Erwartet: `0`.

- [ ] **Schritt 6: Lokale Trockenprüfung der YAML-Dateien**

Ausführen: `python3 -c "import yaml,sys; [yaml.safe_load(open(f)) for f in sys.argv[1:]]; print('YAML ok')" .github/workflows/pr-compile-check.yml .github/workflows/nightly-artifacts.yml`
Erwartet: `YAML ok`.

- [ ] **Schritt 6b: Linux-Jobs lokal mit `act` prüfen**

`act` (0.2.89) und Docker sind vorhanden. Fünf der sieben bauenden Jobs laufen auf `ubuntu-latest` und damit lokal:

```bash
act -j build-linux-appimage -W .github/workflows/nightly-artifacts.yml
act -j build-linux-deb      -W .github/workflows/nightly-artifacts.yml
act -j build-linux-rpm      -W .github/workflows/nightly-artifacts.yml
act -j build-linux-arch     -W .github/workflows/nightly-artifacts.yml
act --privileged -j build-flatpak -W .github/workflows/nightly-artifacts.yml
```

Erwartet: Jeder Lauf endet mit `Job succeeded`.

`build-flatpak` braucht `--privileged`, weil `flatpak-builder` Namensräume anlegt. Scheitert es trotzdem an fehlenden Rechten im Container, ist das ein Umgebungsproblem von `act` und kein Fehler der Umstellung: im Ledger als `Ruling:` festhalten und diesen einen Job über die CI prüfen.

`build-windows-zip` (`runs-on: windows-latest`) und `build-macos-dmg` (`runs-on: macos-latest`, `macos-15-intel`) kann `act` **nicht** ausführen — es startet ausschließlich Linux-Container. Beide bleiben Schritt 7.

> **Ohne Push verifiziert am 2026-09-25.** Linux über `act` (Schritt 6b), macOS auf einem Gerät mit macOS 27.0 / arm64, Windows 11 Pro / AMD64 in WinBoat. Auf allen drei Plattformen: Gradle-Build grün, `jpackage` erzeugt ein startfähiges Artefakt mit allen acht Laufzeitmodulen, und die Anwendung initialisiert JavaFX (RSS 182 MB Linux, 183 MB macOS, 193 MB Windows).
>
> **Was ein echter CI-Lauf trotzdem noch zeigen muss:** die Artefakt-Uploads, die MSI-Strecke mit WiX, die Flatpak-Signierung und das signierte DMG. Alle vier scheitern lokal an fehlenden Secrets oder Diensten, nicht am Build.

- [ ] **Schritt 7: Commit und CI-Lauf abwarten**

```bash
git add .github/workflows/pr-compile-check.yml .github/workflows/nightly-artifacts.yml
git commit -m "Build the PR check and nightly artifacts with Gradle"
git push
```

Danach den Nightly-Workflow in GitHub Actions auslösen und **das Ergebnis abwarten**.
Erwartet: Windows-ZIP, Linux-AppImage und Debian-Paket entstehen.

Windows- und macOS-Artefakte lassen sich lokal nicht prüfen. Scheitert ein Job, ist die CI-Ausgabe die einzige Fehlerquelle; nicht raten, sondern das Protokoll lesen.

- [ ] **Schritt 8: Erzeugtes Linux-Artefakt starten**

Das AppImage aus dem Workflow-Lauf herunterladen und ausführen.
Erwartet: Das Hauptfenster erscheint, das Kartenfenster zeigt Kacheln.

---

## Aufgabe 10: Release-Strecke und AUR

**Dateien:**
- Ändern: `.github/workflows/tagged-release.yml`
- Ändern: `packaging/aur/kst4contest/PKGBUILD`
- Prüfen: `.github/workflows/aur-publish.yml`, `.github/workflows/aur-nightly-git.yml`, `.github/workflows/docs-pdf.yml`, `.github/workflows/github-wiki.yml`, `.github/workflows/test-publish-version-xml.yml`

- [ ] **Schritt 1: `tagged-release.yml` umstellen**

Dieselben Ersetzungen wie in Aufgabe 9, Schritte 3 bis 5, an allen betroffenen Stellen. Die Zeilenanker zum Zeitpunkt der Planerstellung: `:40`, `:92-98`, `:172-178`, `:226-232`, `:280-286`, `:387-400`, `:523-524`. Vor dem Bearbeiten mit `grep -n "mvnw\|target/dist-libs" .github/workflows/tagged-release.yml` neu ermitteln, da sich die Zeilen durch die vorherigen Aufgaben verschoben haben können.

- [ ] **Schritt 2: Prüfen**

Ausführen: `grep -n "mvnw\|target/dist-libs" .github/workflows/tagged-release.yml`
Erwartet: keine Ausgabe.

- [ ] **Schritt 3: `PKGBUILD` umstellen**

In `packaging/aur/kst4contest/PKGBUILD` die `build()`-Funktion anpassen: der Maven-Aufruf und das `cp` auf `target/dist-libs/app.jar` (`:26`) werden durch `./gradlew --offline -S :app-desktop:collectRuntime` ersetzt, und der `jpackage`-Aufruf (`:44`) bekommt `--input app-desktop/build/dist-libs`.

Die `depends`- und `makedepends`-Einträge müssen `gradle` statt `maven` nennen, falls dort `maven` steht.

Ausführen: `grep -n "maven\|mvn\|target/" packaging/aur/kst4contest/PKGBUILD`
Erwartet: keine Ausgabe.

- [ ] **Schritt 4: Flatpak-Job prüfen**

Der Job `build-flatpak` in `nightly-artifacts.yml` (`:420`) und sein Gegenstück in `tagged-release.yml` bauen inline, es gibt kein `packaging/flatpak`-Verzeichnis. Dort gelten dieselben Ersetzungen wie in Aufgabe 9, Schritte 3 bis 5.

Ausführen: `grep -n "mvnw\|target/dist-libs" .github/workflows/nightly-artifacts.yml`
Erwartet: keine Ausgabe.

- [ ] **Schritt 5: Die übrigen Workflows prüfen**

Ausführen: `grep -ln "mvnw\|mvn " .github/workflows/*.yml`
Erwartet: keine Ausgabe.

Treffer werden nach demselben Muster umgestellt. `docs-pdf.yml` und `github-wiki.yml` bauen vermutlich kein Java und bleiben dann unverändert.

- [ ] **Schritt 5b: Versionsauflösung in den übrigen Dateien umstellen**

Dieselbe Ersetzung wie Aufgabe 9, Schritt 5b, in `tagged-release.yml`, `aur-publish.yml`, `aur-nightly-git.yml` und `docs-pdf.yml`.

In `packaging/aur/kst4contest/PKGBUILD` und `packaging/aur/kst4contest-git/PKGBUILD` jede Stelle, die `pom.xml` nach der Version durchsucht, auf `gradle.properties` umstellen.

Ausführen: `grep -rn "pom.xml" .github/ packaging/`
Erwartet: keine Ausgabe.

- [ ] **Schritt 6: YAML prüfen**

Ausführen: `python3 -c "import yaml,glob; [yaml.safe_load(open(f)) for f in glob.glob('.github/workflows/*.yml')]; print('YAML ok')"`
Erwartet: `YAML ok`.

- [ ] **Schritt 7: Commit**

```bash
git add .github/workflows packaging/aur
git commit -m "Build the release and AUR packages with Gradle"
```

---

## Aufgabe 11: macOS-DMG

**Dateien:**
- Ändern: `packaging/macos/build-signed-dmg.sh`

Nur auf macOS oder über die CI prüfbar.

- [ ] **Schritt 1: Eingabepfade im Skript ermitteln**

Ausführen: `grep -n "dist-libs\|target/\|jpackage\|--input" packaging/macos/build-signed-dmg.sh`

- [ ] **Schritt 2: Umstellen**

Jeden Verweis auf `target/dist-libs` auf `app-desktop/build/dist-libs` ändern und den Maven-Aufruf, falls vorhanden, durch `./gradlew -S :app-desktop:collectRuntime` ersetzen. Die `--add-modules`-Liste wie in Aufgabe 8, Schritt 2.

Signierung, Entitlements (`packaging/macos/kst4contest.entitlements`), das Drag-Layout und `packaging/macos/dmg/render-background.swift` bleiben **unverändert**. Diese Aufgabe ändert ausschließlich, woher die Eingabedateien kommen.

- [ ] **Schritt 3: Prüfen**

Ausführen: `grep -n "target/" packaging/macos/build-signed-dmg.sh`
Erwartet: keine Ausgabe.

> **Teilweise verifiziert am 2026-09-25** auf einem Mac mit macOS 27.0 / arm64 (Temurin 21.0.12.1), ohne Push: `./gradlew :app-desktop:collectRuntime` und `:app-desktop:packageImage` laufen durch. Es entsteht `praktiKST.app` mit allen acht Laufzeitmodulen, eingebettetem `praktiKST.icns`, `app.mainclass=kst4contest.view.Main` und den sechs `mac-aarch64`-JavaFX-JARs. Die App startet, läuft stabil (182 MB RSS) und initialisiert den JavaFX-Toolkit — `sample` zeigt JavaFX Application Thread, QuantumRenderer und Prism.
>
> **Offen bleibt die Signierung und das DMG selbst.** `build-signed-dmg.sh` bricht ohne `SIGNING_IDENTITY` ab; ein Zertifikat ist auf dem Rechner vorhanden ("Developer ID Application: Philipp Wagner (RNGLLTC995)"), wurde aber bewusst nicht benutzt. Die von dieser Etappe geänderten Zeilen des Skripts sind dieselben, die `packageImage` oben verifiziert hat — Signierung, Entitlements und Drag-Layout sind unverändert.

- [ ] **Schritt 4: Commit und CI-Lauf**

```bash
git add packaging/macos/build-signed-dmg.sh
git commit -m "Point the macOS DMG build at the Gradle output"
git push
```

Den macOS-Job in GitHub Actions auslösen und abwarten.
Erwartet: Ein DMG entsteht, zeigt das Drag-to-Install-Layout und die darin enthaltene Anwendung startet.

---

## Aufgabe 12: Maven entfernen

Erst ausführen, wenn Aufgabe 9, 10 und 11 auf allen drei Plattformen grün sind.

**Dateien:**
- Löschen: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`
- Ändern: `README.md`, `AGENTS.md`, `docs/PROJECT_CONTEXT.md`

- [ ] **Schritt 1: Grüne CI bestätigen**

Ausführen: `gh run list --limit 5`
Erwartet: Die letzten Läufe von `nightly-artifacts` und `tagged-release` sind erfolgreich.

Ist ein Lauf rot, diese Aufgabe **nicht** beginnen. Erst die Ursache beheben.

- [ ] **Schritt 2: Entfernen**

```bash
git rm pom.xml mvnw mvnw.cmd
git rm -r .mvn
```

- [ ] **Schritt 3: Dokumentation nachziehen**

In `README.md` den Abschnitt „Building from source" anpassen: statt des Maven-Wrappers wird `./gradlew build` genannt. JDK 21 bleibt die Voraussetzung.

In `AGENTS.md` den Eintrag `pom.xml` in der Liste der Repository-Bereiche durch `build.gradle.kts`, `settings.gradle.kts` und `gradle/` ersetzen.

In `docs/PROJECT_CONTEXT.md` unter „Current Architecture" den Satz „Java 21 / JavaFX desktop application built with Maven." ersetzen durch „Java 21 / JavaFX desktop application built with Gradle in the modules `core` and `app-desktop`."

- [ ] **Schritt 4: Prüfen, dass kein Verweis bleibt**

Ausführen: `grep -rn "mvnw\|pom\.xml\|Maven" README.md AGENTS.md docs/PROJECT_CONTEXT.md .github/ packaging/`
Erwartet: keine Ausgabe. Treffer in historischen Abschnitten von `docs/` dürfen stehen bleiben, wenn sie die Vergangenheit beschreiben; das im Ledger als `Ruling:` festhalten.

- [ ] **Schritt 4b: Versionsauflösung ist von `pom.xml` gelöst**

Ausführen: `grep -rn "pom.xml" .github/ packaging/ README.md AGENTS.md`
Erwartet: keine Ausgabe.

Bleibt hier ein Treffer, liefert die betroffene Strecke nach dem Löschen von `pom.xml` eine **leere** Version und benennt ihre Artefakte falsch, ohne zu scheitern. Diese Prüfung ist deshalb nicht optional.

- [ ] **Schritt 5: Vollständiger Bau- und Testlauf**

Ausführen: `./gradlew clean build`
Erwartet: BUILD SUCCESSFUL, 208 Testfälle grün, 0 Fehler.

- [ ] **Schritt 6: Image bauen und starten**

Ausführen: `./gradlew :app-desktop:packageImage && ./app-desktop/build/jpackage/praktiKST/bin/praktiKST`
Erwartet: Das Hauptfenster erscheint. Stationsliste, Chat-Reiter, Einstellungsfenster und Kartenfenster funktionieren.

- [ ] **Schritt 7: Commit**

```bash
git add -A
git commit -m "Remove the Maven build in favour of Gradle"
```

---

## Abnahme der Etappe

Etappe 1 ist abgeschlossen, wenn alles davon zutrifft:

- [ ] `./gradlew clean build` läuft durch, 208 Testfälle aus 43 Testdateien grün.
- [ ] `./gradlew :app-desktop:packageImage` erzeugt ein Image, das startet.
- [ ] Im gestarteten Image funktionieren Stationsliste, die drei Chat-Reiter, das Einstellungsfenster mit allen zwölf Reitern, das Kartenfenster und der Profilwechsel.
- [ ] Windows-ZIP, Linux-AppImage, Debian-Paket, RPM, Arch-Paket, Flatpak und macOS-DMG entstehen in der CI und starten.
- [ ] `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `module-info.java` und `packaging/AddModules.java` existieren nicht mehr.
- [ ] `grep -rn "pom.xml" .github/ packaging/` liefert keine Ausgabe; die Version kommt überall aus `gradle.properties`.
- [ ] Keine Quelldatei unter `kst4contest/` wurde inhaltlich geändert — nur verschoben. Prüfbar mit `git log --stat` über die Commits dieser Etappe.
