package kst4contest.view.compose.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.Form
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.compose.SettingsCloseRequest
import kst4contest.view.compose.SettingsContent
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.SettingsNotice
import kst4contest.view.compose.SettingsNotices
import kst4contest.view.compose.SettingsTab
import kst4contest.view.compose.applyAndSave
import kst4contest.view.compose.applyRefusing
import kst4contest.view.compose.awaitGone
import kst4contest.view.compose.awaitNode
import kst4contest.view.compose.inDialog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The settings window around its tabs: edits wait in the draft, Apply/Close applies them,
 * Save applies before it writes, and the X asks about what is still waiting.
 *
 * Drawn with the Beacon tab because it has all three kinds of control that matter here: a
 * checkbox, a field that commits on Enter or focus loss, and an effect (the timer restart).
 * The button bar is a stand-in: the real one needs a ChatController, which opens a database;
 * it calls the same [applyRefusing] and [applyAndSave] this one does.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsApplyScreenTest {

    @TempDir
    lateinit var directory: Path

    private inner class Window(refuse: Boolean = false) {
        val prefs = ChatPreferences(directory.resolve("preferences.xml")).apply {
            bcn_beaconIntervalInMinutesMainCat = 5
            isBcn_beaconsEnabledMainCat = false
        }
        val draft = SettingsDraft()
        val notices = SettingsNotices()
        val closeRequest = SettingsCloseRequest()
        var closed = false
        var restarts = 0
        val state = BeaconTabState(
            prefs,
            draft,
            validateBeaconTemplate = { if (refuse) throw IllegalArgumentException("no variable %foo%") },
            restartBeaconTimer = { restarts++ },
            mainCategoryName = "Microwave",
            secondCategoryName = "144/432 MHz",
        )
        val tabs = listOf(SettingsTab("Beacon") { BeaconTab(state, notices::problem) })
    }

    private fun androidx.compose.ui.test.ComposeUiTest.show(w: Window) {
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SettingsContent(
                    tabs = w.tabs,
                    notices = w.notices,
                    buttons = { close ->
                        Form.button(APPLY_AND_CLOSE) { if (applyRefusing(w.draft, w.notices)) close() }
                    },
                    close = { w.closed = true },
                    draft = w.draft,
                    closeRequest = w.closeRequest,
                )
            }
        }
        waitForIdle()
    }

    @Test
    fun anEditTakesEffectOnlyWithApplyAndClose() = runComposeUiTest {
        val w = Window()
        show(w)

        toggle("Send the beacon in this category")
        typeAndCommit("Interval in whole minutes", "9")

        assertFalse(w.prefs.isBcn_beaconsEnabledMainCat, "the box took effect before Apply")
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertEquals(0, w.restarts, "the timer restarted before Apply")

        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertTrue(w.prefs.isBcn_beaconsEnabledMainCat)
        assertEquals(9, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertEquals(1, w.restarts)
        assertTrue(w.closed)
    }

    /*
     * A click on a button leaves the focus in the text field, so the field has not committed
     * when the button runs. Apply commits it first; otherwise the entry would reach the draft
     * only when the closing window removed the field -- after the apply, into nothing.
     */
    @Test
    fun applyAndCloseTakesTheEntryStillInTheFocusedField() = runComposeUiTest {
        val w = Window()
        show(w)

        val field = fieldBeside("Interval in whole minutes")
        field.performScrollTo().performClick()
        waitForIdle()
        field.performTextReplacement("9")
        waitForIdle()

        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertEquals(9, w.prefs.bcn_beaconIntervalInMinutesMainCat, "the typed entry was lost")
        assertEquals(1, w.restarts)
        assertTrue(w.closed)
    }

    /* A refused entry stops the button, so the reason is not closed away with the window. */
    @Test
    fun aRefusedEntryKeepsTheWindowOpenAndShowsWhy() = runComposeUiTest {
        val w = Window(refuse = true)
        show(w)

        val field = fieldBeside("Beacon message")
        field.performScrollTo().performClick()
        waitForIdle()
        field.performTextReplacement("cq %foo%")
        waitForIdle()

        onNodeWithText(APPLY_AND_CLOSE).performClick()
        waitForIdle()

        assertFalse(w.closed, "the window closed over a refused entry")
        assertTrue(w.notices.current is SettingsNotice.Problem)
    }

    @Test
    fun theXClosesAtOnceWhenNothingIsPending() = runComposeUiTest {
        val w = Window()
        show(w)

        runOnIdle { w.closeRequest.request() }
        waitForIdle()

        assertTrue(w.closed)
    }

    @Test
    fun theXAsksAboutPendingEditsAndApplyAppliesThem() = runComposeUiTest {
        val w = Window()
        show(w)
        toggle("Send the beacon in this category")

        runOnIdle { w.closeRequest.request() }
        awaitNode(inDialog(PENDING_QUESTION))
        assertFalse(w.closed, "the window closed without asking")

        awaitNode(inDialog("Apply")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertTrue(w.prefs.isBcn_beaconsEnabledMainCat)
        assertTrue(w.closed)
    }

    @Test
    fun discardingOnTheXChangesNothing() = runComposeUiTest {
        val w = Window()
        show(w)
        toggle("Send the beacon in this category")
        typeAndCommit("Interval in whole minutes", "9")

        runOnIdle { w.closeRequest.request() }
        awaitNode(inDialog("Discard")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertFalse(w.prefs.isBcn_beaconsEnabledMainCat)
        assertEquals(5, w.prefs.bcn_beaconIntervalInMinutesMainCat)
        assertEquals(0, w.restarts)
        assertTrue(w.closed)
        assertFalse(w.draft.hasPendingChanges)
    }

    @Test
    fun goingBackKeepsTheWindowAndTheEdits() = runComposeUiTest {
        val w = Window()
        show(w)
        toggle("Send the beacon in this category")

        runOnIdle { w.closeRequest.request() }
        awaitNode(inDialog("Back to the settings")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertFalse(w.closed)
        assertTrue(w.draft.hasPendingChanges)
        assertFalse(w.prefs.isBcn_beaconsEnabledMainCat)
        checkBeside("Send the beacon in this category").assertExists()
    }

    /* An entry typed just before the X counts as pending, although no Enter committed it. */
    @Test
    fun theXAsksAboutAnEntryStillInTheFocusedField() = runComposeUiTest {
        val w = Window()
        show(w)

        val field = fieldBeside("Interval in whole minutes")
        field.performScrollTo().performClick()
        waitForIdle()
        field.performTextReplacement("9")
        waitForIdle()

        runOnIdle { w.closeRequest.request() }
        awaitNode(inDialog(PENDING_QUESTION))

        assertFalse(w.closed)
    }

    /* A list row still being edited counts too: the lists commit a row on Enter or focus loss. */
    @Test
    fun theXAsksAboutARowStillBeingEdited() = runComposeUiTest {
        val prefs = ChatPreferences(directory.resolve("preferences.xml")).apply {
            lst_txtShortCutBtnList.setAll(listOf("MYQRG", "pse sked"))
        }
        val draft = SettingsDraft()
        val closeRequest = SettingsCloseRequest()
        var closed = false
        val state = ShortcutsTabState(prefs, draft, { }, { })
        setContent {
            Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) {
                SettingsContent(
                    tabs = listOf(SettingsTab("Shortcuts") { ShortcutsTab(state) { } }),
                    notices = SettingsNotices(),
                    buttons = { },
                    close = { closed = true },
                    draft = draft,
                    closeRequest = closeRequest,
                )
            }
        }
        waitForIdle()

        val row = onNode(hasSetTextAction() and hasText("pse sked"))
        row.performScrollTo().performClick()
        waitForIdle()
        row.performTextReplacement("pse sked 432")
        waitForIdle()

        runOnIdle { closeRequest.request() }
        awaitNode(inDialog("Apply")).performClick()
        awaitGone(inDialog(PENDING_QUESTION))

        assertEquals(listOf("MYQRG", "pse sked 432"), prefs.lst_txtShortCutBtnList.snapshot())
        assertTrue(closed)
    }

    /* Save applies first, so the file holds what the window shows; the window stays open. */
    @Test
    fun saveAppliesBeforeItWrites() {
        val prefs = ChatPreferences(directory.resolve("preferences.xml"))
        prefs.bcn_beaconIntervalInMinutesMainCat = 5
        val draft = SettingsDraft()
        val notices = SettingsNotices()
        val state = BeaconTabState(prefs, draft, { }, { }, "Microwave", "144/432 MHz")
        var intervalWhenSaved = -1

        state.commitBeaconInterval("9")
        applyAndSave(draft, notices) {
            intervalWhenSaved = prefs.bcn_beaconIntervalInMinutesMainCat
            "preferences.xml"
        }

        assertEquals(9, intervalWhenSaved, "the file was written before the draft was applied")
        assertFalse(draft.hasPendingChanges)
        assertTrue(notices.current is SettingsNotice.Info)
    }

    private companion object {
        const val APPLY_AND_CLOSE = "Apply/Close prefs"
        const val PENDING_QUESTION =
            "Some settings have been changed but not applied yet. Apply them before the window closes?"
    }
}
