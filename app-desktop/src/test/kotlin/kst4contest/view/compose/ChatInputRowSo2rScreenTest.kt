package kst4contest.view.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.model.ChatCategory
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/** The send line with SO2R: the display-only QTF2 field appears only while SO2R is on. */
@OptIn(ExperimentalTestApi::class)
class ChatInputRowSo2rScreenTest {

    @TempDir
    lateinit var tempDir: Path

    private fun prefs() = ChatPreferences(tempDir.resolve("preferences.xml")).apply {
        getActualQTF().set(145.0)
        getActualQTF2().set(270.0)
    }

    private fun chatInputStateFor(prefs: ChatPreferences) = ChatInputState(object : ChatInputActions {
        override fun panelSelection(): ChatMember? = null
        override fun tableSelection(): ChatMember? = null
        override fun scoreSelection(): ChatMember? = null
        override fun resolveVariables(template: String, member: ChatMember?): String? = template
        override fun isAddressedToOwnCallsign(text: String): Boolean = false
        override fun resolveCategory(text: String, member: ChatMember?): ChatCategory? = null
        override fun mainCategory(): ChatCategory? = null
        override fun recordOutboundCq(text: String) {}
        override fun requestRecompute(reason: String) {}
        override fun queue(message: ChatMessage) {}
        override fun ownQrgMain(): String = prefs.getMYQRGFirstCat().get().orEmpty()
        override fun ownQrgSecond(): String = prefs.getMYQRGSecondCat().get().orEmpty()
    })

    @Test
    fun `QTF2 is shown only with SO2R and cannot be edited`() = runComposeUiTest {
        val prefs = prefs()
        var so2r by mutableStateOf(false)
        setContent {
            ChatInputRow(
                state = chatInputStateFor(prefs),
                prefs = prefs,
                ownQrgMain = ObservedValue(prefs.getMYQRGFirstCat()),
                ownQrgSecond = ObservedValue(prefs.getMYQRGSecondCat()),
                antennaQtf = ObservedValue(prefs.getActualQTF()),
                antennaQtf2 = ObservedValue(prefs.getActualQTF2()),
                so2rEnabled = so2r,
            )
        }

        onAllNodesWithText("270.0").assertCountEquals(0)
        so2r = true
        waitForIdle()
        onNodeWithText("270.0").assertExists()
        onNodeWithText("270.0").assertIsNotEnabled()
    }
}
