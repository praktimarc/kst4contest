package kst4contest.view.compose.tabs

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.model.Band
import kst4contest.view.compose.DataTable
import kst4contest.view.compose.Form

/**
 * The notification tab: sounds, band-upgrade hints, the local DX Cluster server and
 * the QSO monitoring list.
 *
 * The port and the spotter callsign are committed when the field is left, not while
 * it is typed: both are validated, and a rule applied per keystroke would refuse
 * every prefix of a valid entry.
 */
@Composable
fun NotificationTab(state: NotificationTabState, onRefused: (String) -> Unit) {
    var askForCallSign by remember { mutableStateOf(false) }
    val strings = LocalStrings.current


    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Form.section(strings.notificationSoundsSection) {
            Form.check(strings.notificationSimpleSounds, state.notify_playSimpleSounds) {
                state.notify_playSimpleSounds = it
            }
            Form.check(
                strings.notificationCwCallsign,
                state.notify_playCWCallsignsOnRxedPMs,
            ) { state.notify_playCWCallsignsOnRxedPMs = it }
            Form.check(
                strings.notificationSpeakCallsign,
                state.notify_playVoiceCallsignsOnRxedPMs,
            ) { state.notify_playVoiceCallsignsOnRxedPMs = it }
        }

        Form.section(strings.notificationBandUpgradeSection) {
            Form.check(
                strings.notificationBandUpgradeHint,
                state.notify_bandUpgradeHintOnLogEnabled,
            ) { state.notify_bandUpgradeHintOnLogEnabled = it }
            Form.check(
                strings.notificationBandUpgradePriority,
                state.notify_bandUpgradePriorityBoostEnabled,
            ) { state.notify_bandUpgradePriorityBoostEnabled = it }
        }

        Form.section(strings.notificationClusterSection) {
            Form.check(strings.notificationClusterEnabled, state.notify_dxClusterServerEnabled) {
                state.enableDxClusterServer(it)
            }
            Form.committed(
                label = strings.notificationClusterPort,
                stored = state.notify_dxclusterServerPort.toString(),
                commit = state::commitDxClusterServerPort,
                onRefused = onRefused,
            )
            Form.committed(
                label = strings.notificationSpotterCallsign,
                stored = state.notify_DXCSrv_SpottersCallSign,
                commit = state::commitSpotterCallSign,
                onRefused = onRefused,
            )
            /*
             * Shows the band prefix, as the JavaFX ComboBox did: the prefix is what the
             * stored setting contains.
             */
            Form.choice(
                label = strings.notificationFallbackBand,
                items = Band.entries,
                selected = state.notify_optionalFrequencyPrefix,
                describe = { it.prefix + " MHz" },
                onSelect = { state.notify_optionalFrequencyPrefix = it },
            )
            Row {
                /*
                 * A refusal is the normal outcome here: the JavaFX button reported
                 * "no logger connected" the same way, which is exactly what the
                 * operator is testing for.
                 */
                Form.button(strings.notificationSendTestSpot) { state.sendTestSpot()?.let(onRefused) }
            }
        }

        Form.section(strings.notificationMonitoringSection) {
            DataTable(
                state = state.monitoredCallSigns,
                addButtonText = strings.notificationAddMonitored,
                newEntryTemplate = "",
                onChanged = state::commitMonitoredCallSigns,
                onRefused = onRefused,
                /*
                 * The JavaFX button asked for the callsign in a dialog before adding
                 * it, so there was never a placeholder row to edit.
                 */
                onAdd = { askForCallSign = true },
            )
        }
    }

    if (askForCallSign) {
        var entered by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { askForCallSign = false },
            title = { Text(strings.notificationMonitoringSection) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.notificationAddCallsignToList)
                    OutlinedTextField(
                        value = entered,
                        onValueChange = { entered = it },
                        singleLine = true,
                        label = { Text(strings.notificationCallsign) },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    askForCallSign = false
                    state.addMonitoredCallSign(entered)?.let(onRefused)
                }) { Text(strings.dialogOk) }
            },
            dismissButton = {
                TextButton(onClick = { askForCallSign = false }) { Text(strings.dialogCancel) }
            },
        )
    }
}
