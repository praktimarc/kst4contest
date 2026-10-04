package kst4contest.view.compose.tabs

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

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        @Suppress("UNUSED_EXPRESSION") revision

        Form.section("File polling for worked callsigns") {
            Form.check(
                "Read worked callsigns periodically from a log file (without band information)",
                state.logsynch_fileBasedWkdCallInterpreterEnabled,
            ) { state.logsynch_fileBasedWkdCallInterpreterEnabled = it; revision++ }

            FilePathRow(
                label = "Log file to be monitored:",
                value = state.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly,
                chooserTitle = "Choose Readonly-Loginterpreter-File",
            ) { state.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = it; revision++ }
        }

        Form.section("Network-based QSO synchronization (N1MM/QARTEST/UCXLog/DXLog)") {
            Form.check(
                "Process QSO messages from N1MM+, QARTEST, UCXLog and DXLog.net",
                state.logsynch_ucxUDPWkdCallListenerEnabled,
            ) { state.logsynch_ucxUDPWkdCallListenerEnabled = it; revision++ }

            Form.int(
                "Shared UDP port for QSO and TRX messages [default 12060]:",
                state.logsynch_ucxUDPWkdCallListenerPort,
            ) { state.logsynch_ucxUDPWkdCallListenerPort = it; revision++ }
        }

        Form.section("Win-Test Network-Listener") {
            Form.check(
                "Receive Win-Test network based UDP log messages",
                state.logsynch_wintestNetworkListenerEnabled,
            ) { state.logsynch_wintestNetworkListenerEnabled = it; revision++ }

            GuardedTextField(
                label = "UDP-Port for Win-Test listener (default is 9871)",
                value = state.logsynch_wintestNetworkPort.toString(),
                enabled = state.logsynch_wintestNetworkListenerEnabled,
                accepts = { it.trim().toIntOrNull() != null },
                onAccepted = { state.logsynch_wintestNetworkPort = it.trim().toInt(); revision++ },
                // Rebinding the socket belongs to the finished entry, not to every digit.
                onCommit = { state.commitWintestNetworkPort() },
            )

            GuardedTextField(
                label = "KST station name in Win-Test network (src of SKED packets)",
                value = state.logsynch_wintestNetworkStationNameOfKST,
                onAccepted = { state.logsynch_wintestNetworkStationNameOfKST = it; revision++ },
            )

            GuardedTextField(
                label = "UDP broadcast address for Win-Test (default = internet interface broadcast)",
                value = state.logsynch_wintestNetworkBroadcastAddress,
                onAccepted = { state.logsynch_wintestNetworkBroadcastAddress = it; revision++ },
            )

            Text(
                "The broadcast address is detected once while it is still " +
                    "${LogSynchTabState.DEFAULT_BROADCAST_ADDRESS}. A port change takes " +
                    "effect on the running listener when the field loses focus."
            )
        }
    }
}
