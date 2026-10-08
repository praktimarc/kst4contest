package kst4contest.view.compose.tabs

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form

/**
 * The Log synch tab: how worked callsigns reach KST4Contest — by polling a log file, by
 * UDP from N1MM+/QARTEST/UCXLog/DXLog, and by the Win-Test network.
 *
 * No confirmation callback: LogSynchTabState writes through to ChatPreferences, the way
 * the JavaFX controls did. "Save settings" persists, it does not collect.
 *
 * Labels, units and default hints are the JavaFX ones. The Win-Test port field is
 * disabled while its listener is off, as it was there.
 */
@Composable
fun LogSynchTab(state: LogSynchTabState) {
    /*
     * The state is a plain facade over ChatPreferences with no snapshot state in it, so
     * a control would keep showing the old value after its own write. Counting revisions
     * is the device WorkedDatabaseTab uses for the same reason.
     */
    var revision by remember { mutableStateOf(0) }
    val strings = LocalStrings.current


    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        @Suppress("UNUSED_EXPRESSION") revision

        Form.section(strings.logSynchFileSection) {
            Form.check(
                strings.logSynchFileEnabled,
                state.logsynch_fileBasedWkdCallInterpreterEnabled,
            ) { state.logsynch_fileBasedWkdCallInterpreterEnabled = it; revision++ }

            FilePathRow(
                label = strings.logSynchFileLabel,
                value = state.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly,
                chooserTitle = strings.logSynchFileChooserTitle,
            ) { state.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = it; revision++ }
        }

        Form.section(strings.logSynchNetworkSection) {
            Form.check(
                strings.logSynchNetworkEnabled,
                state.logsynch_ucxUDPWkdCallListenerEnabled,
            ) { state.logsynch_ucxUDPWkdCallListenerEnabled = it; revision++ }

            Form.int(
                strings.logSynchSharedPort,
                state.logsynch_ucxUDPWkdCallListenerPort,
            ) { state.logsynch_ucxUDPWkdCallListenerPort = it; revision++ }
        }

        Form.section(strings.logSynchWinTestSection) {
            Form.check(
                strings.logSynchWinTestEnabled,
                state.logsynch_wintestNetworkListenerEnabled,
            ) { state.logsynch_wintestNetworkListenerEnabled = it; revision++ }

            GuardedTextField(
                label = strings.logSynchWinTestPort,
                value = state.logsynch_wintestNetworkPort.toString(),
                enabled = state.logsynch_wintestNetworkListenerEnabled,
                accepts = { it.trim().toIntOrNull() != null },
                onAccepted = { state.logsynch_wintestNetworkPort = it.trim().toInt(); revision++ },
                // Rebinding the socket belongs to the finished entry, not to every digit.
                onCommit = { state.commitWintestNetworkPort() },
            )

            GuardedTextField(
                label = strings.logSynchWinTestStationName,
                value = state.logsynch_wintestNetworkStationNameOfKST,
                onAccepted = { state.logsynch_wintestNetworkStationNameOfKST = it; revision++ },
            )

            GuardedTextField(
                label = strings.logSynchWinTestBroadcast,
                value = state.logsynch_wintestNetworkBroadcastAddress,
                onAccepted = { state.logsynch_wintestNetworkBroadcastAddress = it; revision++ },
            )

            Text(
                strings.logSynchWinTestHint(LogSynchTabState.DEFAULT_BROADCAST_ADDRESS)
            )
        }
    }
}
