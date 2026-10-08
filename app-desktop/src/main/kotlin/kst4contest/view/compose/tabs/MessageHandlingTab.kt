package kst4contest.view.compose.tabs

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form

/** The message-handling tab: the automatic answer and the automatic QRG reply. */
@Composable
fun MessageHandlingTab(state: MessageHandlingTabState) {
    val strings = LocalStrings.current

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Form.section(strings.messageHandlingAutoAnswerSection) {
            Form.check(strings.messageHandlingAutoAnswerEnabled, state.autoAnswerEnabled) {
                state.autoAnswerEnabled = it
            }
            Form.text(strings.messageHandlingAutoAnswerText, state.autoAnswerText) { state.autoAnswerText = it }
        }

        Form.section(strings.messageHandlingQrgAnswerSection) {
            Form.check(
                strings.messageHandlingQrgAnswerEnabled,
                state.autoAnswerToQRGRequestEnabled,
            ) { state.autoAnswerToQRGRequestEnabled = it }
        }

        Form.section(strings.messageHandlingDiagnosticsSection) {
            Form.check(
                strings.messageHandlingDebugToFileEnabled,
                state.debugModeToFileEnabled,
            ) { state.debugModeToFileEnabled = it }
            Text(
                strings.messageHandlingDebugToFileHint,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
