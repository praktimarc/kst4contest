package kst4contest.view.compose.tabs

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatMember
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.ImmediateDraft
import kst4contest.observe.SimpleRoster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kst4contest.view.compose.awaitNode
import kst4contest.view.compose.awaitGone
import kst4contest.view.compose.inDialog
import kst4contest.view.compose.inPopup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The two tabs built around a list, drawn: Shortcuts and the worked-station database (#107).
 *
 * The shortcut lists are what the button row above the send line and the snippet menus are
 * built from. Their write-through has a history: the refresh hook was called into thin air
 * after the JavaFX buttons were unplugged, so an edited shortcut reached the operator only
 * after the window was rebuilt (`fe4215bc`, Remove JavaFX). The hook is what these tests see.
 */
@OptIn(ExperimentalTestApi::class)
class ListTabsScreenTest {

    /*
     * Never the no-argument ChatPreferences: it resolves the operator's real profile and
     * seeds preferences.xml there when it is missing. A file in a temporary directory is
     * never read or written by these tests, it only gives the preferences a home.
     */
    @TempDir
    lateinit var directory: Path

    private fun preferences(): ChatPreferences = ChatPreferences(directory.resolve("preferences.xml"))

    // ------------------------------------------------------------------ Shortcuts

    private class Shortcuts(isolated: ChatPreferences) {
        val prefs = isolated.apply {
            lst_txtShortCutBtnList.setAll(listOf("MYQRG", "pse sked"))
            lst_txtSnipList.setAll(listOf("tnx qso 73"))
        }
        var shortcutRefreshes = 0
        var snippetRefreshes = 0
        val state = ShortcutsTabState(prefs, ImmediateDraft(), { shortcutRefreshes++ }, { snippetRefreshes++ })
    }

    @Test
    fun shortcutsShowsBothListsWithWhatTheProfileHolds() = runComposeUiTest {
        val s = Shortcuts(preferences())
        showTab { ShortcutsTab(s.state) { } }

        onNodeWithText("Shortcut buttons above the message field").assertExists()
        onNodeWithText("Text snippets (the first 10 use Ctrl+1 through Ctrl+0)").assertExists()
        onNode(hasSetTextAction() and hasText("pse sked")).assertExists()
        onNode(hasSetTextAction() and hasText("tnx qso 73")).assertExists()
        onNodeWithText("Add shortcut").assertExists()
        onNodeWithText("Add new snippet").assertExists()
    }

    @Test
    fun anEditedShortcutIsWrittenAndTheButtonsAreRefreshed() = runComposeUiTest {
        val s = Shortcuts(preferences())
        showTab { ShortcutsTab(s.state) { } }

        val row = onNode(hasSetTextAction() and hasText("pse sked"))
        row.performScrollTo().performClick()
        row.performTextReplacement("pse sked 432")
        onNode(hasSetTextAction() and hasText("pse sked 432")).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertEquals(listOf("MYQRG", "pse sked 432"), s.prefs.lst_txtShortCutBtnList.snapshot())
        assertTrue(s.shortcutRefreshes > 0, "the shortcut buttons were not told")
    }

    @Test
    fun addingASnippetPutsTheTemplateOnTopAndWritesIt() = runComposeUiTest {
        val s = Shortcuts(preferences())
        showTab { ShortcutsTab(s.state) { } }

        onNodeWithText("Add new snippet").performScrollTo().performClick()
        waitForIdle()

        assertEquals(
            listOf(ShortcutsTabState.NEW_ENTRY_TEMPLATE, "tnx qso 73"),
            s.prefs.lst_txtSnipList.snapshot(),
        )
        assertTrue(s.snippetRefreshes > 0, "the snippet menus were not told")
        assertEquals(listOf("MYQRG", "pse sked"), s.prefs.lst_txtShortCutBtnList.snapshot())
    }

    // ------------------------------------------------------------------ Worked database

    private class Worked(affected: Int = 12) {
        val roster = SimpleRoster<ChatMember>().apply {
            setAll(listOf(ChatMember().apply { callSign = "DL1ABC"; isWorked = true; isWorked144 = true }))
        }
        var resets = 0
        var guiRefreshes = 0
        var viewRefreshes = 0
        val state = WorkedDatabaseTabState(
            resetInDatabase = { resets++; affected },
            refreshGuiLists = { guiRefreshes++ },
            refreshView = { viewRefreshes++ },
            workedRoster = roster,
        )
    }

    @Test
    fun theWorkedDatabaseShowsItsRows() = runComposeUiTest {
        val w = Worked()
        showTab { WorkedDatabaseTab(w.state) }

        onNodeWithText("Worked station database").assertExists()
        onNodeWithText("DL1ABC").assertExists()
        onNodeWithText("wkd").assertExists()
    }

    @Test
    fun refreshReReadsTheRoster() = runComposeUiTest {
        val w = Worked()
        showTab { WorkedDatabaseTab(w.state) }

        runOnIdle { w.roster.setAll(listOf(ChatMember().apply { callSign = "OK1XYZ" })) }
        onNodeWithText("Refresh worked database").performScrollTo().performClick()
        waitForIdle()

        assertEquals(1, w.viewRefreshes)
        onNodeWithText("OK1XYZ").assertExists()
        onNodeWithText("DL1ABC").assertDoesNotExist()
    }

    /** Nothing is touched until the dialog is confirmed; then the database goes first. */
    @Test
    fun aResetAsksFirstAndReportsWhatItDid() = runComposeUiTest {
        val w = Worked(affected = 12)
        showTab { WorkedDatabaseTab(w.state) }

        onNodeWithText("Reset worked, NOT-QRV and grid data...").performScrollTo().performClick()
        waitForIdle()
        awaitNode(inDialog("Reset contest data"))
        assertEquals(0, w.resets, "the reset ran before it was confirmed")

        onNode(inDialog("Cancel")).performClick()
        awaitGone(inDialog("Reset contest data"))
        assertEquals(0, w.resets)

        onNodeWithText("Reset worked, NOT-QRV and grid data...").performClick()
        waitForIdle()
        awaitNode(inDialog("Reset data")).performClick()
        awaitGone(inDialog("Reset data"))

        assertEquals(1, w.resets)
        assertEquals(1, w.guiRefreshes)
        onNodeWithText("Last reset affected 12 rows.").assertExists()
    }

    @Test
    fun aFailedResetIsSaidAndLeavesTheListsAlone() = runComposeUiTest {
        val w = Worked(affected = -1)
        showTab { WorkedDatabaseTab(w.state) }

        onNodeWithText("Reset worked, NOT-QRV and grid data...").performScrollTo().performClick()
        waitForIdle()
        awaitNode(inDialog("Reset data")).performClick()
        awaitGone(inDialog("Reset data"))

        awaitNode(
            inDialog(
                "The contest data could not be reset. The internal database may be unavailable or " +
                    "inconsistent. No successful reset was confirmed.",
            ),
        )
        assertEquals(0, w.guiRefreshes, "the lists were cleared although the database was not")
    }
}
