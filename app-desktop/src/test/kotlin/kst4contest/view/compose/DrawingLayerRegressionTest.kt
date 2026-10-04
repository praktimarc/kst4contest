package kst4contest.view.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.tabs.MessageHandlingTab
import kst4contest.view.compose.tabs.MessageHandlingTabState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The four ways the drawing layer went wrong while the build was green.
 *
 * Every one of these shipped in the branch at the same time as a passing suite of 382
 * tests, because not one of those tests drew anything. The state-level tests and the two
 * settings coverage nets answer "was the setter called"; these answer "did it reach the
 * screen", which is a different question and the one the operator asks.
 *
 * | Class | How it showed up |
 * |---|---|
 * | A control reads a non-observable value and never redraws | every keystroke in the settings was written through and not displayed |
 * | A layout clips instead of wrapping | the tab strip cut "Workedstn database" mid-word |
 * | Content is taller than the window and unreachable | a tab's form ended mid-way with nothing to say more was below |
 * | Colours do not come from the stylesheet | the window surface took `-fx-base` instead of `derive(-fx-base, 26.4%)`; the accent was Material violet |
 *
 * All four run without a display: `runComposeUiTest` reaches the semantics tree headless,
 * and the colour check reads the palette the theme hands out. That was measured before
 * these were written, because the spec left it open and a net that only works on a
 * developer's desk is worth saying so about.
 */
class DrawingLayerRegressionTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        Kst4ContestTheme(darkMode = false, baseFontSizeSp = 12f) { content() }
    }

    // ---------------------------------------------------------------- class 1

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aTypedSettingAppearsOnScreenAndNotOnlyInThePreferences() = runComposeUiTest {
        val prefs = ChatPreferences()
        prefs.setMessageHandling_autoAnswerTextMainCat("")
        val state = MessageHandlingTabState(prefs)

        // The real tab, not a field in isolation: the fault was in the wiring between them.
        setContent(themed { MessageHandlingTab(state) })

        // MessageHandlingTab has exactly one text field, so this finder is unambiguous.
        onNode(hasSetTextAction()).performTextInput("qrv 144.300")

        assertEquals(
            "qrv 144.300",
            prefs.getMessageHandling_autoAnswerTextMainCat(),
            "the value did not reach the preferences",
        )
        onNodeWithText("qrv 144.300").assertExists()
    }

    // ---------------------------------------------------------------- class 2

    private val tabTitles = listOf(
        "Station", "Log synch", "TRX synch", "Airscout", "Notification",
        "Message handling", "Beacon", "Shortcuts", "GUI", "Profiles",
        "Workedstn database",
    )

    /**
     * The strip inside a box of a given width.
     *
     * The width is forced with a modifier rather than by sizing the test window, which is
     * what makes this a measurement instead of a does-it-crash check: the same content is
     * laid out twice and the two results are compared.
     */
    private fun stripAt(widthDp: Int): @Composable () -> Unit = themed {
        Box(Modifier.width(widthDp.dp)) {
            SettingsTabStrip(
                tabs = tabTitles.map { title -> SettingsTab(title) { Text(title) } },
                selectedIndex = 0,
                onSelect = {},
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theTabStripWrapsRatherThanSqueezingItsTitles() = runComposeUiTest {
        setContent(stripAt(WIDE_ENOUGH_FOR_EVERY_TAB))

        val wide = onNodeWithText(LONGEST_TAB).fetchSemanticsNode()
        val wideWidth = wide.size.width
        val wideTop = wide.positionInRoot.y

        assertTrue(wideWidth > 0, "the longest tab title was not laid out at all")

        setContent(stripAt(TOO_NARROW_FOR_ONE_ROW))

        val narrow = onNodeWithText(LONGEST_TAB).fetchSemanticsNode()

        assertEquals(
            wideWidth,
            narrow.size.width,
            "the tab got narrower when the row did, which means it is being squeezed "
                    + "instead of wrapped — that is what cut '$LONGEST_TAB' mid-word",
        )
        assertTrue(
            narrow.positionInRoot.y > wideTop,
            "eleven tabs fitted on one line at $TOO_NARROW_FOR_ONE_ROW dp, so nothing "
                    + "was measured; pick a narrower width",
        )
    }

    // ---------------------------------------------------------------- class 3

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aFormTallerThanTheWindowCanStillBeReached() = runComposeUiTest {
        val fieldCount = 40
        val lastLabel = "field $fieldCount"

        setContent(
            themed {
                // The window's own scroll region, with content that cannot possibly fit.
                SettingsContent(
                    tabs = listOf(
                        SettingsTab("Tall") {
                            Column {
                                (1..fieldCount).forEach { Text("field $it", Modifier.height(40.dp)) }
                            }
                        },
                    ),
                    notices = SettingsNotices(),
                    buttons = {},
                    close = {},
                )
            }
        )

        /*
         * performScrollTo fails if nothing in the chain scrolls. That is exactly the fault:
         * each tab used to scroll for itself, so content simply ended mid-form with no
         * scrollbar and no way down.
         */
        onNodeWithText(lastLabel).performScrollTo().assertExists()
    }

    // ---------------------------------------------------------------- class 4

    @Test
    fun theWindowSurfaceIsDerivedFromTheBaseAndNotTheBaseItself() {
        val evening = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")

        /*
         * The fault was taking -fx-base for the window surface. The sheets derive it, and
         * the two are visibly different greys — which is why the first Compose windows
         * looked flat against the JavaFX ones beside them.
         */
        assertTrue(
            evening.windowBackground != evening.base,
            "the window surface is the raw base colour again",
        )
    }

    @Test
    fun theAccentComesFromTheStylesheetAndNotFromMaterial() {
        val evening = JavaFxStylesheet.read("/KST4ContestDefaultEvening.css")
        val day = JavaFxStylesheet.read("/KST4ContestDefaultDay.css")

        /*
         * Material's default primary is a violet (#6650a4 family). The sheets name their
         * own accent, and the two sheets name different ones — so a palette that agreed
         * with Material, or a palette that was the same in both sheets, would mean the
         * stylesheet is not being read.
         */
        val materialViolet = androidx.compose.ui.graphics.Color(0xFF6650A4)

        assertTrue(evening.accent != materialViolet, "the accent is Material's, not the sheet's")
        assertTrue(day.accent != materialViolet, "the accent is Material's, not the sheet's")
        assertTrue(
            evening.accent != day.accent || evening.base != day.base,
            "both sheets produced the same palette, so neither was really read",
        )
    }

    private companion object {
        const val LONGEST_TAB = "Workedstn database"

        /** Comfortably more than the eleven tabs need, so none of them wraps. */
        const val WIDE_ENOUGH_FOR_EVERY_TAB = 2000

        /** Narrow enough that the row must break; the wrap is the point of the test. */
        const val TOO_NARROW_FOR_ONE_ROW = 300
    }
}
