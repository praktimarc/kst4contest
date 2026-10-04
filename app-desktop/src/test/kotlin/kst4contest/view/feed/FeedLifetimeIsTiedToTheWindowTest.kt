package kst4contest.view.feed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The feeds are built where the Compose window's state is built, and nowhere else.
 *
 * This is the executable half of a warning that part 3 would otherwise have to read and
 * believe. All three feeds are constructed inside the method that also constructs
 * `composeMainWindowState`, which today is reached from `start()`. Part 3 removes
 * `start()` piece by piece; if it follows the warning's table faithfully and rehomes the
 * four timeline triggers, the constructions still go with the rest of `start()` and all
 * three feeds stay null — a silently empty timeline, message table and connection badge,
 * with no compile error and no failing test to show for it.
 *
 * Asserting "same method as the Compose state" rather than "not inside `start()`" is
 * what makes it structural: part 3 cannot drop the feed construction without dropping
 * the window's own state, which it will never do, because that is the window. It also
 * survives the line numbers moving, which a range check against `start()` would not.
 *
 * This says nothing about who writes the states once they exist — that is
 * `FeedIsTheOnlyWriterTest`, and the two together are the whole claim.
 */
class FeedLifetimeIsTiedToTheWindowTest {

    private val source: File
        get() = listOf(
            File("src/main/java/kst4contest/view/Kst4ContestApplication.java"),
            File("app-desktop/src/main/java/kst4contest/view/Kst4ContestApplication.java"),
        ).firstOrNull { it.isFile } ?: error("cannot find Kst4ContestApplication.java from ${File(".").absolutePath}")

    /** A method declaration at class level; the body ends at the matching outdented brace. */
    private val declaration = Regex(
        """^\t(?:(?:public|private|protected|static|final|synchronized|abstract)\s+)+""" +
            """[\w<>,\[\]. ]+\s+(\w+)\s*\("""
    )

    /** The name of the method whose body contains [line], or null at class level. */
    private fun methodAt(lines: List<String>, line: Int): String? {
        var name: String? = null
        for (index in 0 until line) {
            val text = lines[index]
            declaration.find(text)?.let { name = it.groupValues[1] }
            if (text.startsWith("\t}")) name = null
        }
        return name
    }

    @Test
    fun `every feed is constructed where the Compose window state is`() {
        val lines = source.readLines()
        val live = lines.withIndex().filterNot { (_, text) ->
            text.trim().startsWith("//") || text.trim().startsWith("*")
        }

        val stateAt = live.filter { "composeMainWindowState = new MainWindowState(" in it.value }
        assertEquals(1, stateAt.size) {
            "expected exactly one construction of the Compose window state, found ${stateAt.size}"
        }
        val home = methodAt(lines, stateAt.single().index)
        assertTrue(home != null) { "the Compose window state is built outside any method body" }

        val feeds = live.filter { Regex("""\b\w*[Ff]eed = new\b""").containsMatchIn(it.value) }
        assertEquals(3, feeds.size) {
            "expected three feed constructions, found ${feeds.size}:\n" +
                feeds.joinToString("\n") { "  :${it.index + 1} ${it.value.trim()}" }
        }

        val strays = feeds.filter { methodAt(lines, it.index) != home }
        assertTrue(strays.isEmpty()) {
            "these feeds are built outside $home, where composeMainWindowState is built, " +
                "so removing start() can leave them null:\n" +
                strays.joinToString("\n") { feed ->
                    "  :${feed.index + 1} in ${methodAt(lines, feed.index)} — ${feed.value.trim()}"
                }
        }
    }
}
