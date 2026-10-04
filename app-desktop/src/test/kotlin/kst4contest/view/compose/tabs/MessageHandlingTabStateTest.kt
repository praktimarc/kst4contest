package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class MessageHandlingTabStateTest {

    @Test
    fun `the state starts from the stored preferences`() {
        val prefs = ChatPreferences()
        prefs.setMessageHandling_autoAnswerEnabled(true)
        prefs.setMessageHandling_autoAnswerToQRGRequestEnabled(true)
        prefs.setMessageHandling_autoAnswerTextMainCat("qrv 144.300")

        val state = MessageHandlingTabState(prefs)

        assertTrue(state.autoAnswerEnabled)
        assertTrue(state.autoAnswerToQRGRequestEnabled)
        assertEquals("qrv 144.300", state.autoAnswerText)
    }

    @Test
    fun `one checkbox drives both category flags`() {
        // The JavaFX window offered a single checkbox and wrote both preferences from
        // it; keeping only the main one would leave the second chat on its old value.
        val prefs = ChatPreferences()
        val state = MessageHandlingTabState(prefs)

        state.autoAnswerEnabled = true

        assertTrue(prefs.isMessageHandling_autoAnswerEnabled())
        assertTrue(prefs.isMessageHandling_autoAnswerEnabledSecondCat())

        state.autoAnswerEnabled = false

        assertFalse(prefs.isMessageHandling_autoAnswerEnabled())
        assertFalse(prefs.isMessageHandling_autoAnswerEnabledSecondCat())
    }

    @Test
    fun `one text field drives both category texts`() {
        val prefs = ChatPreferences()
        val state = MessageHandlingTabState(prefs)

        state.autoAnswerText = "qrv 432.200"

        assertEquals("qrv 432.200", prefs.getMessageHandling_autoAnswerTextMainCat())
        assertEquals("qrv 432.200", prefs.getMessageHandling_autoAnswerTextSecondCat())
    }

    @Test
    fun `the QRG reply flag writes through on its own`() {
        val prefs = ChatPreferences()
        val state = MessageHandlingTabState(prefs)

        state.autoAnswerToQRGRequestEnabled = true

        assertTrue(prefs.isMessageHandling_autoAnswerToQRGRequestEnabled())
        assertFalse(prefs.isMessageHandling_autoAnswerEnabled(),
            "the two options are independent")
    }
}
