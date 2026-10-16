package kst4contest.view.compose.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.Form
import kst4contest.view.compose.Kst4ContestTheme
import kst4contest.view.compose.SettingsContent
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.SettingsNotice
import kst4contest.view.compose.SettingsNotices
import kst4contest.view.compose.SettingsTab
import kst4contest.view.compose.applyAndSave
import kst4contest.view.compose.applyRefusing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The settings window around its tabs: edits wait in the draft, Apply/Close applies them,
 * Save applies before it writes.
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
    }
}
