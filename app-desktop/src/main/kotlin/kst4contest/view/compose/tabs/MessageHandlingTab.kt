package kst4contest.view.compose.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form

/** The message-handling tab: the automatic answer and the automatic QRG reply. */
@Composable
fun MessageHandlingTab(state: MessageHandlingTabState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Form.section("Automatic answer to private messages") {
            Form.check("Answer private messages automatically", state.autoAnswerEnabled) {
                state.autoAnswerEnabled = it
            }
            Form.text("Answer text", state.autoAnswerText) { state.autoAnswerText = it }
        }

        Form.section("Automatic answer to a QRG request") {
            Form.check(
                "Answer a QRG request with the current frequency",
                state.autoAnswerToQRGRequestEnabled,
            ) { state.autoAnswerToQRGRequestEnabled = it }
        }
    }
}
