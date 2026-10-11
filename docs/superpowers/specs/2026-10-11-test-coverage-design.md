# Konzept: Testabdeckung (#62)

> Status: Konzept, freigegeben am 2026-10-11, umgesetzt im Zweig `feature/62-test-coverage`.
> Alle Zeilenanker beziehen sich auf `main` bei `4a90ae7d`.

Issue #62 verlangt einen Blick auf die Testsuite als Ganzes: Was läuft, was sieht nur so aus,
als liefe es, und wie viel des Codes erreicht die Suite tatsächlich. Dieses Dokument hält den
vorgefundenen Stand fest, die getroffenen Entscheidungen und das, was bewusst eigenen Issues
überlassen wird.

## Das Problem

Vier Befunde, gemessen am Stand `4a90ae7d`.

**Sieben JUnit-Klassen liegen unter `src/main` und werden nie ausgeführt.** Gradles
`test`-Task durchsucht nur `src/test`. Deshalb werden
`core/src/main/java/kst4contest/controller/On4KstProtocolTest.java`,
`On4KstSocketThreadTest.java`, `MessageBusManagementThreadFrequencyContextTest.java`
(vollständig auskommentiert), `core/src/main/java/kst4contest/model/ChatMemberDirectionOpportunityTest.java`
und die drei Klassen unter `app-desktop/src/main/java/kst4contest/test/`
(`Utils4KSTFrequencyTest`, `FrequencyTextParserTest`, `ChatPreferencesStationMapVisibilityTest`)
bei jedem Build kompiliert und zählen für nichts. Der Protokolltest darunter nagelt die
exakten `LOGINC|`-, `SDONE|`- und `ACHAT|`-Frames fest, der Socket-Test genau ein CR/LF je
Frame. Beides sind Invarianten, die `AGENTS.md` ausdrücklich nennt, und keine davon hat seit
der Maven-Zeit ein Build geprüft.

**Wegen dieser Klassen wird JUnit und Mockito mit der Anwendung ausgeliefert.** `core` und
`app-desktop` deklarieren `implementation(libs.junit.jupiter.api)` und
`implementation(libs.mockito.core.compile)`. Damit liegen `junit-jupiter-api`,
`junit-platform-commons`, `opentest4j`, `mockito-core`, `byte-buddy`, `byte-buddy-agent` und
`objenesis` auf dem Laufzeit-Klassenpfad und in `lib/app` jedes App-Image, Flatpak und
Installers. `docs/CONCEPT_IN_APP_UPDATE.md` nennt das bereits als Vorbedingung für einen
signierten Update-Pfad: Ein Bytecode-Manipulations-Agent in einem signierten Produktionsbinary
ist das Falsche, was man erklären müsste.

**Acht Dateien unter `core/src/test/java/kst4contest/test/` sind toter Scratch-Code von
2023/2024.** `TestChatPreferences`, `TestReadUDPASListenerThread`,
`TestReadUDPUCXListenerThread`, `UDPAirScoutSendMessages`, `testTimeGeneration`,
`TestApplicationFileUtils`, `TestDBController` und `TestLocatorUtils` enthalten keine einzige
Assertion. Fünf davon tragen `@Test` an einer `public static void main`, die Jupiter nie
ausführt; die anderen drei haben gar keine Annotation. Manche schreiben beim manuellen Start die
echte Preferences-Datei, senden echten UDP-Broadcast oder öffnen die echte Nutzerdatenbank.
Fünf weitere `main()`-Programme derselben Art liegen unter `src/main`
(`ReadUDPByWintestThreadTest`, `PatternMatcherTest`, `mapTest`, `RegexTester`,
`locatorUtils/TestLocatorUtils`), dazu `MockKstServer`, auf den nur noch ein ungenutzter Import
in `ChatController` verweist.

**Es gibt keine Abdeckungszahl.** Die Suite ist durch die Compose-Migration auf mehrere hundert
Tests gewachsen, aber niemand kann sagen, welchen Teil von `core` sie erreicht. Drei Bereiche
sind bekannt dünn: der XML-Roundtrip von `ChatPreferences` jenseits der Handvoll Felder, die
Einzeltests festnageln, die Schema-Migration in `DBController`, und die Netzwerk-Threads
(`ReadThread`/`WriteThread`, die UDP-Listener), die bestenfalls je einen Loopback-Test haben.

## Entscheidungen

**Tote Tests löschen, nicht retten.** Ein Test ohne Assertion dokumentiert nichts, und die mit
Seiteneffekten sind eine Gefahr, sobald jemand „alle Tests“ aus der IDE startet. Ihre
Gegenstände sind entweder anderswo abgedeckt (`ChatPreferences*Test`,
`DBControllerProfileDatabaseTest`, `DirectionUtilsConsoleOutputTest`) oder werden zu den zwei
neuen Testbereichen unten. Der auskommentierte `MessageBusManagementThreadFrequencyContextTest`
wird ebenfalls gelöscht; was er abgedeckt hätte
(`MessageBusManagementThread.hasExplicitBareFrequencyContext` und `Band.fromPrefix`), ist im
Umsetzungsbericht vermerkt, damit es mit der Frequenz-Parsing-Arbeit aufgegriffen und nicht
stillschweigend verloren wird.

**Tests liegen ausschließlich unter `src/test`.** Die sechs lebenden JUnit-Klassen wandern nach
`src/test` des Moduls, dem die getestete Klasse gehört, mit unveränderten Paketnamen, neben die
Geschwistertests, die dort schon liegen (`FrequencyTextParserRegressionTest`,
`ChatPreferencesStationMapClusteringTest`). Die drei Klassen aus `app-desktop` testen
ausschließlich `core`-Klassen und ziehen deshalb nach `core`. Ohne Testklasse unter `src/main`
entfallen die beiden `implementation`-Deklarationen und die doppelten Katalogeinträge
(`junit-jupiter-api`, `mockito-core-compile`, Version `mockitoCompile`). JUnit und Mockito sind
nur noch `testImplementation`, was die vorhandenen `testImplementation`-Zeilen ohnehin schon
sagten. Nachgewiesen über `:app-desktop:dependencies --configuration runtimeClasspath` ohne die
sieben Artefakte und über die Jar-Liste von `createDistributable`.

**JaCoCo nur als Bericht, ohne Schwellwert.** Das `jacoco`-Plugin wird aus dem Root-Build auf
jedes Modul angewandt, `jacocoTestReport` läuft nach `test` und schreibt XML und HTML je Modul,
und ein Root-Task `jacocoAggregatedReport` führt `core` und `app-desktop` zu einem Bericht unter
`build/reports/jacoco/aggregated/` zusammen. `i18n-generator` bleibt außen vor: Es ist ein
Bauwerkzeug, seine Abdeckung sagt nichts über die ausgelieferte Anwendung. Es gibt bewusst keine
`jacocoTestCoverageVerification` und keinen Mindestprozentsatz. Ein heute gesetzter Schwellwert
stünde bei der Zahl, die zufällig gerade herauskommt, und wäre damit ein Stolperdraht für die
nächste unabhängige Änderung statt einer Aussage über Qualität; ein Schwellwert auf Zielniveau
ließe jeden Build scheitern, bis jemand Tests um der Zahl willen schreibt. Der Bericht existiert,
damit die Zahl in jedem Pull Request sichtbar ist und bei der Entscheidung hilft, was als
Nächstes zu testen ist. Sie zu heben ist Sache der unten gelisteten Issues, nicht der
Build-Konfiguration.

**Tests laufen vor dem Packaging.** Der Nightly- und der Release-Workflow bauen sieben
Paketformate aus einem Commit, den unterwegs niemand getestet hat: `pr-compile-check.yml` führt
die Suite nur für Pull Requests aus, und `main` kann direkte Pushes erhalten. Beide Workflows
bekommen eine Kopie des `test`-Jobs aus `pr-compile-check.yml`, einschließlich des
`libgl1`-Schritts und der XML-basierten Zusammenfassung, und jeder Packaging-Job deklariert
`needs: test`. Der Job wird dupliziert statt in einen wiederverwendbaren Workflow ausgelagert:
Drei Kopien von vierzig Zeilen lesen sich leichter als eine Indirektion, und die Kommentare im
Job erklären Entscheidungen, die die Packaging-Workflows bei sich tragen sollen. Der
PR-Workflow führt zusätzlich `jacocoAggregatedReport` aus, schreibt aus dem JaCoCo-XML eine
Abdeckungstabelle in die Job-Zusammenfassung und lädt den aggregierten HTML-Bericht für
vierzehn Tage als Artefakt `coverage-report` hoch.

**Zwei neue Testbereiche entstehen im selben Zweig, aber in eigenen Commits.** Issue #62 nennt
den `ChatPreferences`-Roundtrip und die `DBController`-Schema-Migration als die zwei
ungetesteten Risikobereiche, die sich jetzt schließen lassen. Es sind gewöhnliche
Testergänzungen, die vom Aufräumen nicht abhängen; sie landen nach dem Aufräumen, damit ihre
Wirkung im aggregierten Bericht sichtbar wird.

## Bewusst außerhalb des Umfangs

Jeder dieser Punkte ist eine echte Lücke, die unterwegs gefunden wurde, und jeder wird ein
eigenes Issue, weil jeder eine Entscheidung braucht, die dieses Aufräumen nicht nebenbei
treffen sollte:

- **Refactoring von `MessageBusManagementThread` für Testbarkeit.** Frequenzextraktion,
  Worked-Prüfung und der DX-Cluster-Spot-Pfad sind heute nur über einen konstruierten Thread
  mit gemockten Mitspielern erreichbar. Sie in testbare Einheiten zu teilen berührt die
  Nachrichten-Pipeline und braucht ein eigenes Konzept.
- **Loopback-Tests für die Netzwerk-Threads.** `On4KstSocketThreadTest` deckt `ReadThread`
  und `WriteThread` ab; die UDP-Listener für Win-Test, UCXLog und AirScout und der
  PSTRotator-Client haben kein Gegenstück. Ein Loopback-Harness mit Port-0-Socket je Protokoll
  ist die richtige Form, aber das Framing jedes Protokolls verdient eine Prüfung, bevor ein Test
  es festnagelt.
- **Compose-Screen-Tests.** Die State-Tests und die zwei Settings-Abdeckungsnetze prüfen, dass
  Setter aufgerufen werden; nur `runComposeUiTest` kann prüfen, dass ein Wert den Bildschirm
  erreicht hat. Welche Screens diesen Aufwand wert sind, ist eine Oberflächenentscheidung.
- **Pfadanalyse-Mathematik.** `PathPropagationAssessment` und die Terrainprofil-Berechnung
  haben Layout-Tests, aber keine numerischen Referenzfälle. Referenzwerte brauchen eine Quelle,
  die sich zitieren lässt.
- **Entfernen von `Thread.sleep`.** Mehrere Tests und einige Produktionspfade warten noch auf
  die Wanduhr. Sie durch injizierbare Uhren oder Latches zu ersetzen ist mechanisch, aber breit.

## Definition von „fertig“ für #62

- Unter keinem `src/main` bleibt eine mit `@Test` annotierte Klasse oder ein Import von
  `org.junit` oder `org.mockito`.
- `./gradlew clean build` führt die verschobenen Tests aus; die Ergebnis-XML unter
  `*/build/test-results/test/` meldet null Fehlschläge und null Fehler.
- `./gradlew :app-desktop:dependencies --configuration runtimeClasspath` listet keines von
  `junit`, `mockito`, `byte-buddy`, `objenesis`, `opentest4j`, und
  `app-desktop/build/compose/binaries/main/app/KST4Contest/lib/app` enthält kein Jar dieser
  Namen.
- `./gradlew jacocoAggregatedReport` schreibt `build/reports/jacoco/aggregated/jacoco.xml` und
  einen HTML-Bericht; die PR-Job-Zusammenfassung zeigt eine Abdeckungstabelle und bietet das
  Artefakt `coverage-report` an.
- `nightly-artifacts.yml` und `tagged-release.yml` führen den Test-Job aus, und kein
  Packaging-Job startet, bevor er bestanden ist.
- `AGENTS.md` und `docs/PROJECT_CONTEXT.md` behaupten nicht mehr, Testfehler könnten den Build
  passieren; sie sagen, dass nur PMD und SpotBugs mit `ignoreFailures` laufen.
- Die zwei neuen Testbereiche (`ChatPreferences`-Roundtrip, `DBController`-Schema-Migration)
  liegen im Zweig, und die Punkte außerhalb des Umfangs existieren als Issues.

## Was unverändert bleibt

- Testverhalten: Keine Assertion wird abgeschwächt und kein Test deaktiviert, damit der Umzug
  kompiliert.
- Der Produktionscode, bis auf den entfernten `MockKstServer`-Import in `ChatController`.
- PMD und SpotBugs behalten `ignoreFailures`; ihre Befunde werden wie bisher aus den Berichten
  gelesen.
- Die Packaging-Jobs selbst: Es kommt nur `needs: test` hinzu.
- Keine neue Laufzeitabhängigkeit. JaCoCo ist ein Gradle-Plugin, das die Test-JVM
  instrumentiert; nichts davon erreicht das Jar.
