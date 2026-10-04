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
zwei Modulen: `core` (Fachlichkeit) und `app-desktop` (Oberfläche). Paketnamen
sind über beide Module hinweg dieselben.

## Laufende Arbeit

Spezifikationen liegen unter `docs/superpowers/specs/`, Umsetzungspläne unter
`docs/superpowers/plans/`. Der aktuelle Umbau ist die Ablösung von JavaFX durch
Compose Multiplatform in sechs Etappen; Etappe 1 (Gradle, zwei Module, JPMS raus)
ist abgeschlossen.
