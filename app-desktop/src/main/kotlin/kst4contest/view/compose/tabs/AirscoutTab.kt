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
 * The Airscout tab: the UDP link to AirScout and how the queried frequency is chosen.
 *
 * Labels, defaults and the closing note are the JavaFX ones, including that the server
 * identifier, the client identifier and the frequency mode take effect at once while a
 * changed UDP port needs a reconnect.
 *
 * The identifier, port and band fields keep what the operator typed and mark it as an
 * error until it is valid; see AirscoutTabState for why an invalid value must not be
 * written rather than merely corrected.
 */
@Composable
fun AirscoutTab(state: AirscoutTabState) {
    /* See LogSynchTab: the facade holds no snapshot state, so writes need a revision. */
    var revision by remember { mutableStateOf(0) }
    val strings = LocalStrings.current


    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        @Suppress("UNUSED_EXPRESSION") revision

        Form.section(strings.airscoutUdpSection) {
            Form.check(
                strings.airscoutEnabled,
                state.airScout_asUDPListenerEnabled,
            ) { state.airScout_asUDPListenerEnabled = it; revision++ }

            GuardedTextField(
                label = strings.airscoutServerIdentifier,
                value = state.airScout_asServerNameString,
                accepts = { AirscoutTabState.isValidIdentifier(it) },
                onAccepted = { state.airScout_asServerNameString = it; revision++ },
            )

            GuardedTextField(
                label = strings.airscoutClientIdentifier,
                value = state.airScout_asClientNameString,
                accepts = { AirscoutTabState.isValidIdentifier(it) },
                onAccepted = { state.airScout_asClientNameString = it; revision++ },
            )

            GuardedTextField(
                label = strings.airscoutUdpPort,
                value = state.airScout_asCommunicationPort.toString(),
                accepts = { typed ->
                    typed.trim().toIntOrNull()?.let { AirscoutTabState.isValidPort(it) } == true
                },
                onAccepted = { state.airScout_asCommunicationPort = it.trim().toInt(); revision++ },
            )
        }

        Form.section(strings.airscoutFrequencySection) {
            Form.check(
                strings.airscoutAutomaticFrequency,
                state.airScout_autoBandSelectionEnabled,
            ) { state.airScout_autoBandSelectionEnabled = it; revision++ }

            GuardedTextField(
                label = strings.airscoutForcedBand,
                value = state.airScout_asBandString,
                enabled = state.bandValueEditable,
                accepts = { AirscoutTabState.parseBandValue(it) != null },
                onAccepted = { state.airScout_asBandString = it; revision++ },
            )

            Text(
                strings.airscoutAutomaticHint
            )
        }

        Text(
            strings.airscoutApplyHint
        )
    }
}
