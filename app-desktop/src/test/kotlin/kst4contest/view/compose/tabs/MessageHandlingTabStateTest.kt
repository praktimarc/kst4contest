package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.ImmediateDraft
import kst4contest.view.compose.SettingsDraft
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MessageHandlingTabStateTest(@TempDir private val tempDir: Path) {

    @Test
    fun `the state starts from the stored preferences`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        prefs.setMessageHandling_autoAnswerEnabled(true)
        prefs.setMessageHandling_autoAnswerToQRGRequestEnabled(true)
        prefs.setMessageHandling_autoAnswerTextMainCat("qrv 144.300")

        val state = MessageHandlingTabState(prefs, ImmediateDraft())

        assertTrue(state.autoAnswerEnabled)
        assertTrue(state.autoAnswerToQRGRequestEnabled)
        assertEquals("qrv 144.300", state.autoAnswerText)
    }

    @Test
    fun `an edit waits for apply, and the recording follows only then`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val draft = SettingsDraft()
        val recording = mutableListOf<Boolean>()
        val state = MessageHandlingTabState(prefs, draft) { recording += it }

        state.autoAnswerText = "qrv 432.200"
        state.debugModeToFileEnabled = !prefs.isMessageHandling_debugModeToFileEnabled()
        val wanted = state.debugModeToFileEnabled

        assertEquals("qrv 432.200", state.autoAnswerText, "the tab shows the edit")
        assertTrue(recording.isEmpty(), "the recording followed before Apply")

        draft.apply()

        assertEquals("qrv 432.200", prefs.getMessageHandling_autoAnswerTextSecondCat())
        assertEquals(listOf(wanted), recording)
    }

    @Test
    fun `a discarded edit changes nothing`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        prefs.setMessageHandling_autoAnswerEnabled(false)
        val draft = SettingsDraft()
        val state = MessageHandlingTabState(prefs, draft)

        state.autoAnswerEnabled = true
        draft.discard()

        assertFalse(prefs.isMessageHandling_autoAnswerEnabled())
        assertFalse(state.autoAnswerEnabled)
    }

    @Test
    fun `one checkbox drives both category flags`() {
        // The JavaFX window offered a single checkbox and wrote both preferences from
        // it; keeping only the main one would leave the second chat on its old value.
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val state = MessageHandlingTabState(prefs, ImmediateDraft())

        state.autoAnswerEnabled = true

        assertTrue(prefs.isMessageHandling_autoAnswerEnabled())
        assertTrue(prefs.isMessageHandling_autoAnswerEnabledSecondCat())

        state.autoAnswerEnabled = false

        assertFalse(prefs.isMessageHandling_autoAnswerEnabled())
        assertFalse(prefs.isMessageHandling_autoAnswerEnabledSecondCat())
    }

    @Test
    fun `one text field drives both category texts`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val state = MessageHandlingTabState(prefs, ImmediateDraft())

        state.autoAnswerText = "qrv 432.200"

        assertEquals("qrv 432.200", prefs.getMessageHandling_autoAnswerTextMainCat())
        assertEquals("qrv 432.200", prefs.getMessageHandling_autoAnswerTextSecondCat())
    }

    @Test
    fun `the QRG reply flag is stored on its own`() {
        val prefs = ChatPreferences(tempDir.resolve("preferences.xml"))
        val state = MessageHandlingTabState(prefs, ImmediateDraft())

        state.autoAnswerToQRGRequestEnabled = true

        assertTrue(prefs.isMessageHandling_autoAnswerToQRGRequestEnabled())
        assertFalse(prefs.isMessageHandling_autoAnswerEnabled(),
            "the two options are independent")
    }
}
