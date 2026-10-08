# CLAUDE.md

Diese Datei gilt für Claude Code in diesem Repository.

## Projektregeln

Die fachlichen und stilistischen Regeln stehen in **[AGENTS.md](AGENTS.md)** und
**[docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md)**. Beide sind verbindlich und
werden hier nicht wiederholt. Besonders wichtig: Kommunikation auf Deutsch,
Quelltextkommentare und Javadoc auf Englisch, und das Konzept-vor-Umsetzung-Vorgehen
aus dem Abschnitt „Mandatory interaction rule" in `AGENTS.md`.

## Subagenten

Subagenten (Agent-Tool) dürfen ohne Rückfrage genutzt werden, wenn eine Aufgabe
davon profitiert — insbesondere für Abschlussprüfungen ganzer Zweige und für
umfangreiche Umbauten, bei denen ein frischer Blick pro Teilaufgabe etwas wert ist.

Nicht sinnvoll bei kleinen, aufeinander aufbauenden Schritten: dort liest jeder
neue Subagent die Codebasis von null und kostet mehr, als er beiträgt.

## Build

Gradle, nicht Maven. `./gradlew clean build`, Java 21. Das Projekt besteht aus
drei Modulen: `core` (Fachlichkeit), `app-desktop` (Oberfläche) und
`i18n-generator` (reines Bauwerkzeug, das aus den Übersetzungsdateien Kotlin
erzeugt und **nicht ausgeliefert** wird). Paketnamen sind über `core` und
`app-desktop` hinweg dieselben.

## Sprache

Kommunikation auf Deutsch, in **neuer Rechtschreibung** — `muss` und `dass`,
nicht `muß` und `daß`. Quelltextkommentare und Javadoc bleiben ausschließlich Englisch.

Oberflächentexte stehen seit Etappe 9 in `app-desktop/src/main/i18n/strings_<code>.properties`
und nicht mehr im Quelltext. Wer dort etwas ändert: der Erzeuger prüft beim Bau, und die
Regel, was übersetzt wird, steht in `docs/PROJECT_CONTEXT.md` unter „Important Decisions".

## Laufende Arbeit

Spezifikationen liegen unter `docs/superpowers/specs/` und bleiben: sie sagen, **warum** die
Architektur so ist, und das steht nirgends sonst. Umsetzungspläne werden nach der Ausführung
entfernt — sie sind Arbeitsanweisungen für erledigte Arbeit, und ein liegengebliebener Plan
liest sich für den Nächsten wie eine offene Absicht. Was aus ihnen zu behalten war, steht in
den Commit-Nachrichten und in `docs/PROJECT_CONTEXT.md`. Die Ablösung von JavaFX durch Compose Multiplatform ist
**abgeschlossen**: unter `src/main` steht kein `javafx.` mehr, und `org.openjfx` ist
aus `app-desktop/build.gradle.kts` und `gradle/libs.versions.toml` entfernt. Die
Begründungen und die harten Lehren stehen in
`docs/superpowers/notes/2026-10-01-javafx-lebendbefund.md`; die wichtigste: ein
blockierendes Warten auf dem AWT-Ereignisfaden verklemmt die Oberfläche, und der grüne
Build sagt über Fadenzugehörigkeit nichts.
