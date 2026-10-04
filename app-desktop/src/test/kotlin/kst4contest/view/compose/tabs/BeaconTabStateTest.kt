package kst4contest.view.compose.tabs

import kst4contest.controller.ChatController
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class BeaconTabStateTest {

    private class Session(private val templateRefusal: String? = null) {
        val prefs = ChatPreferences()
        var timerRestarts = 0
        var validated = mutableListOf<String>()

        fun state() = BeaconTabState(
            prefs,
            validateBeaconTemplate = { template ->
                validated.add(template)
                templateRefusal?.let { throw IllegalArgumentException(it) }
            },
            restartBeaconTimer = { timerRestarts++ },
            mainCategoryName = "144 MHz and up",
            secondCategoryName = "50 MHz",
        )
    }

    @Test
    fun `the enable flags write through at once`() {
        val s = Session()
        val state = s.state()

        state.bcn_beaconsEnabledMainCat = true
        state.bcn_beaconsEnabledSecondCat = true

        assertTrue(s.prefs.isBcn_beaconsEnabledMainCat())
        assertTrue(s.prefs.isBcn_beaconsEnabledSecondCat())

        state.bcn_beaconsEnabledSecondCat = false
        assertFalse(s.prefs.isBcn_beaconsEnabledSecondCat())
        assertTrue(s.prefs.isBcn_beaconsEnabledMainCat())
    }

    @Test
    fun `a valid beacon template is validated and stored verbatim`() {
        val s = Session()
        val state = s.state()
        val template = "CQ MYCALL MYLOC QRV MYQRG"

        assertNull(state.commitBeaconTextMainCat(template))

        assertEquals(template, s.prefs.getBcn_beaconTextMainCat(),
            "MYCALL and MYQRG are resolved by MessageVariableResolver; "
                + "any rewriting would break that silently")
        assertEquals(listOf(template), s.validated)
    }

    @Test
    fun `the second category has its own template`() {
        val s = Session()
        val state = s.state()

        state.commitBeaconTextSecondCat("CQ MYCALL on MYQRG")

        assertEquals("CQ MYCALL on MYQRG", s.prefs.getBcn_beaconTextSecondCat())
        assertEquals("Hi, pse call us", s.prefs.getBcn_beaconTextMainCat(),
            "the main-category template is untouched")
    }

    @Test
    fun `a refused template is reported and the stored one stands`() {
        val s = Session("Message too long")
        s.prefs.setBcn_beaconTextMainCat("CQ MYCALL")
        val state = s.state()

        val refusal = state.commitBeaconTextMainCat("x".repeat(500))

        assertNotNull(refusal)
        assertEquals("CQ MYCALL", s.prefs.getBcn_beaconTextMainCat(),
            "the JavaFX field restored the previous template on an alert")
    }

    @Test
    fun `the shared interval never reads below the minimum`() {
        val s = Session()
        s.prefs.setBcn_beaconIntervalInMinutesMainCat(0)

        assertEquals(ChatController.MIN_BEACON_INTERVAL_MINUTES, s.state().beaconIntervalMinutes)
    }

    @Test
    fun `a valid interval is written to both categories and restarts the timer`() {
        val s = Session()
        val state = s.state()

        assertNull(state.commitBeaconInterval("7"))

        assertEquals(7, s.prefs.getBcn_beaconIntervalInMinutesMainCat())
        assertEquals(7, s.prefs.getBcn_beaconIntervalInMinutesSecondCat(),
            "the legacy second-category value stays synchronized with the shared interval")
        assertEquals(1, s.timerRestarts)
    }

    @Test
    fun `an interval below the minimum is refused and the timer is left alone`() {
        val s = Session()
        s.prefs.setBcn_beaconIntervalInMinutesMainCat(5)
        val state = s.state()

        assertNotNull(state.commitBeaconInterval("0"))

        assertEquals(5, s.prefs.getBcn_beaconIntervalInMinutesMainCat())
        assertEquals(0, s.timerRestarts,
            "restarting the timer per rejected keystroke would disturb the cooldown")
    }

    @Test
    fun `an interval that is not a whole number is refused`() {
        val s = Session()
        s.prefs.setBcn_beaconIntervalInMinutesMainCat(5)
        val state = s.state()

        assertNotNull(state.commitBeaconInterval("2,5"))

        assertEquals(5, s.prefs.getBcn_beaconIntervalInMinutesMainCat())
    }

    @Test
    fun `the category names are the labels the tab shows`() {
        val s = Session()

        assertEquals("144 MHz and up", s.state().mainCategoryName)
        assertEquals("50 MHz", s.state().secondCategoryName)
    }
}
