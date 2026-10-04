package kst4contest.view.feed

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Nothing outside the feeds writes the Compose state the feeds own.
 *
 * This exists because of how two bypasses survived a review of my own making. The claim
 * "one path writes it now" was checked with `grep getSelectedStationMessages().replaceRows`
 * and `grep getSurroundings().setConnection` — greps shaped like the code that had just
 * been written, which therefore could not match a call through a local variable. Both
 * missed a live second writer.
 *
 * A source-level assertion is the right instrument for that claim, because the claim is
 * about the source and not about a run. It also catches the next bypass, which a
 * hand-run grep does not.
 *
 * The first version of this test repeated the original mistake in a new spelling: it
 * matched `selectedStationMessages` case-sensitively, so the accessor spelling with the
 * capital S slipped through, and it required `getTimeline()` and the mutator on one
 * line, so a two-line write through a local was invisible. Both are closed below. What
 * remains uncovered is named in `the residual limit` at the foot of this file — stating
 * it is the point, because claiming completeness is what failed the first two times.
 */
class FeedIsTheOnlyWriterTest {

    /**
     * Where the feeds themselves and the state declarations live.
     *
     * These files contain the mutator names legitimately: the feed is the one caller,
     * and the state classes declare the methods. Everything else is a bypass.
     */
    private val ownedByTheFeeds = listOf(
        "/view/feed/",
        "/view/compose/TimelineState.kt",
        "/view/compose/DataTableState.kt",
        "/view/compose/MainWindowState.kt",
    )

    /** Resolves whether the test runs from the module directory or the repository root. */
    private fun sourceRoot(): File =
        listOf(File("src/main"), File("app-desktop/src/main"))
            .firstOrNull { it.isDirectory }
            ?: error("cannot find app-desktop/src/main from ${File(".").absolutePath}")

    /**
     * Every live line of the module's main sources, as path, number and text.
     *
     * The whole tree, not one file: the three states are reachable from the Kotlin in
     * this source set too, and a bypass in `MainWindow.kt` would be unscanned otherwise.
     */
    private fun sources(): List<File> {
        val files = sourceRoot().walkTopDown()
            .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
            .filterNot { file -> ownedByTheFeeds.any { file.path.replace('\\', '/').contains(it) } }
            .toList()

        assertTrue(files.size > 20) { "only found ${files.size} sources; the scan is not reaching the tree" }
        return files
    }

    private fun liveLines(): List<Triple<String, Int, String>> {
        return sources().flatMap { file ->
            file.readLines().mapIndexed { index, line -> Triple(file.name, index + 1, line) }
        }.filterNot { (_, _, line) ->
            line.trim().startsWith("//") || line.trim().startsWith("*")
        }
    }

    private fun report(feed: String, offenders: List<Triple<String, Int, String>>) {
        assertTrue(offenders.isEmpty()) {
            "these write the state directly instead of going through $feed:\n" +
                offenders.joinToString("\n") { (file, number, line) -> "  $file:$number ${line.trim()}" }
        }
    }

    /**
     * `replaceRows` on the selected-station message table.
     *
     * The receiver has to be part of the match, because `replaceRows` belongs to seven
     * other tables as well — stations, directed, public, cluster and the monitor's two
     * fill themselves the same way and are none of this feed's business. Matched on the
     * lowercased line so that both the local `selectedStationMessages` and the accessor
     * `getSelectedStationMessages()` are caught; the first version matched only the
     * local, which is the spelling the bug happened to have.
     */
    @Test
    fun `only the feed fills the selected-station message table`() {
        report("SelectedStationMessagesFeed", liveLines().filter { (_, _, line) ->
            val lowered = line.lowercase()
            "selectedstationmessages" in lowered && "replacerows" in lowered
        })
    }

    /**
     * Nobody outside the feed takes a writable handle on that table.
     *
     * This is what closes the two-line write — `var t = …getSelectedStationMessages();`
     * on one line and `t.replaceRows(rows);` on the next, the exact shape that defeated
     * the original verification. To write it at all you must first call the accessor,
     * and the feed's own construction is the only place that may.
     *
     * Scanned per statement rather than per line, because that construction spans two
     * lines: a line-wise rule reports the feed building itself as its own bypass.
     */
    @Test
    fun `only the feed takes a handle on that table`() {
        report("SelectedStationMessagesFeed", sources().flatMap { file ->
            val code = file.readLines()
                .map { if (it.trim().startsWith("//") || it.trim().startsWith("*")) "" else it }
            var line = 1
            code.joinToString("\n").split(";").mapNotNull { statement ->
                val start = line
                line += statement.count { it == '\n' }
                if ("getSelectedStationMessages()" !in statement ||
                    "SelectedStationMessagesFeed(" in statement
                ) {
                    return@mapNotNull null
                }
                // Report the line the accessor is actually on, not where its statement
                // began: a statement starts after the previous semicolon, which can be
                // several lines of unrelated code earlier.
                val within = statement.lines().indexOfFirst { "getSelectedStationMessages()" in it }
                Triple(file.name, start + within, statement.lines()[within].trim())
            }
        })
    }

    /**
     * The connection state and its detail belong together.
     *
     * A writer that sets the state without the detail leaves the badge beside
     * "No ON4KST connection" until something else corrects it, which is the stale-pair
     * case ConnectionStateFeed exists to end. No receiver is needed in the match: both
     * names belong to this state alone, so a two-line write is caught as well.
     *
     * `.connectionState =` is matched too, because `MainWindowSurroundings` is Kotlin:
     * Java call sites spell it `setConnectionState(`, but a Kotlin one would assign the
     * property and the setter spelling would never appear. `MainWindowState.kt` itself
     * is out of the scan — it declares the property and propagates it to the menu.
     *
     * The leading dot is required, and is not decoration: without it the rule reports
     * `connectionState = state.surroundings.connectionState` in `MainWindow.kt`, which
     * is a named argument handing the value to a composable — a read, not a write.
     */
    @Test
    fun `only the feed fills the connection indicator`() {
        report("ConnectionStateFeed", liveLines().filter { (_, _, line) ->
            "setConnectionState(" in line || "setConnectionDetail(" in line ||
                Regex("""\.(connectionState|connectionDetail)\s*=[^=]""").containsMatchIn(line)
        })
    }

    /**
     * The timeline's four writes are the feed's business for the same reason.
     *
     * Matched on the mutator names alone — they belong to `TimelineState` and to nothing
     * else — so a write through a local is caught too. `setBeamWidth(` is spelled with
     * the bracket on purpose: `TimelineView.setBeamWidthDeg` is the JavaFX view's own
     * method at `Kst4ContestApplication.java:6942` and is not this state.
     */
    @Test
    fun `only the feed fills the timeline`() {
        report("TimelineFeed", liveLines().filter { (_, _, line) ->
            "replaceSkeds(" in line || "replaceCandidates(" in line ||
                "setAntennaAzimuth" in line || "setBeamWidth(" in line
        })
    }

    /**
     * The residual limit, stated rather than papered over.
     *
     * A Kotlin bypass could reach the message table as `state.selectedStationMessages`
     * without the accessor, and split over two lines it would pass every rule above.
     * Nothing does that today — `MainWindow.kt` only hands the table to the composable
     * that renders it — and closing it would mean forbidding that read as well. If a
     * Kotlin writer ever appears, this is the test that has to grow, not the place to
     * conclude that none can exist.
     */
    @Test
    fun `the residual limit is the Kotlin property path`() {
        val kotlinReads = liveLines().filter { (file, _, line) ->
            file.endsWith(".kt") && "selectedStationMessages" in line
        }

        kotlinReads.forEach { (file, number, line) ->
            assertTrue("replaceRows" !in line) { "$file:$number writes the table from Kotlin: ${line.trim()}" }
        }
    }
}
