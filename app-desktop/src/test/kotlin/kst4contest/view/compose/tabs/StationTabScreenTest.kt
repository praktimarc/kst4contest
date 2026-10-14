package kst4contest.view.compose.tabs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The Station tab, drawn (#107).
 *
 * [StationTabStateTest] pins the facade; this pins that the drawn controls show what the
 * profile holds and that editing one writes through, which is the class of fault the
 * settings window shipped repeatedly with a green build (`83e23f55`, Etappe 7).
 */
@OptIn(ExperimentalTestApi::class)
class StationTabScreenTest {

    private class Session {
        val prefs = ChatPreferences().apply {
            stn_loginCallSign = "DN9APW"
            stn_on4kstServersPort = 23001
            isStn_bandActive144 = true
            isStn_bandActive10G = false
        }
        val mainApplied = mutableListOf<ChatCategory?>()
        val state = StationTabState(prefs, { mainApplied += it }, { })
    }

    @Test
    fun theTabShowsItsSectionsAndWhatTheProfileHolds() = runComposeUiTest {
        val s = Session()
        showTab { StationTab(s.state) }

        listOf("Own station", "Chat category", "ON4KST server", "Active bands", "Defaults",
            "PSTRotator", "Path analysis defaults").forEach { onNodeWithText(it).assertExists() }

        fieldBeside("Callsign").assertTextEquals("DN9APW")
        fieldBeside("Server port").assertTextEquals("23001")
        checkBeside("144 MHz").assertIsOn()
        checkBeside("10 GHz").assertIsOff()
    }

    @Test
    fun editingTheTextFieldsWritesThroughToThePreferences() = runComposeUiTest {
        val s = Session()
        showTab { StationTab(s.state) }

        type("Callsign", "DO5AMF")
        type("Locator", "JO51IJ")
        type("Server host name", "www.on4kst.info")

        assertEquals("DO5AMF", s.prefs.stn_loginCallSign)
        assertEquals("JO51IJ", s.prefs.stn_loginLocatorMainCat)
        assertEquals("www.on4kst.info", s.prefs.stn_on4kstServersDns)
        fieldBeside("Callsign").assertTextEquals("DO5AMF")
    }

    /** A number field writes only what parses; a half-typed entry leaves the stored value. */
    @Test
    fun theNumberFieldsWriteOnlyWhatParses() = runComposeUiTest {
        val s = Session()
        showTab { StationTab(s.state) }

        type("Server port", "23000")
        assertEquals(23000, s.prefs.stn_on4kstServersPort)

        type("Server port", "23x")
        assertEquals(23000, s.prefs.stn_on4kstServersPort, "an unparsable port was written")

        type("QTF", "135,5")
        assertEquals(135.5, s.prefs.stn_qtfDefault, 1e-9)
    }

    @Test
    fun aBandCheckboxWritesThrough() = runComposeUiTest {
        val s = Session()
        showTab { StationTab(s.state) }

        toggle("10 GHz")
        assertTrue(s.prefs.isStn_bandActive10G)
        checkBeside("10 GHz").assertIsOn()

        toggle("144 MHz")
        assertFalse(s.prefs.isStn_bandActive144)
    }

    @Test
    fun choosingTheMainCategoryWritesAndAppliesIt() = runComposeUiTest {
        val s = Session()
        s.prefs.loginChatCategoryMain = ChatCategory(2)
        showTab { StationTab(s.state) }

        val target = s.state.availableCategories.first { it.categoryNumber == 3 }
        val current = s.state.describeCategory(s.prefs.loginChatCategoryMain)
        // The main category is the upper of the two pickers; the second may show the same name.
        onAllNodesWithText(current).onFirst().performScrollTo().performClick()
        waitForIdle()
        // The dropdown is drawn last, above everything else.
        onAllNodesWithText(s.state.describeCategory(target)).onLast().performClick()
        waitForIdle()

        assertEquals(3, s.prefs.loginChatCategoryMain?.categoryNumber)
        assertEquals(3, s.mainApplied.single()?.categoryNumber, "the running session was not told")
    }
}
