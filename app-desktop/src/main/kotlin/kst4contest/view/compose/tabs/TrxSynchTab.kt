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
 * The TRX synch tab: which source may update the own QRG.
 *
 * The hint below the sources is not filler. PROJECT_CONTEXT records that an automatic
 * QRG update needs an enabled source *and* valid incoming RadioInfo or Win-Test STATUS
 * data, so a checked box must not read as "my QRG is being kept current".
 *
 * No confirmation callback: TrxSynchTabState writes through to ChatPreferences, the way
 * the JavaFX controls did.
 */
@Composable
fun TrxSynchTab(state: TrxSynchTabState) {
    /* See LogSynchTab: the facade holds no snapshot state, so writes need a revision. */
    var revision by remember { mutableStateOf(0) }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        @Suppress("UNUSED_EXPRESSION") revision

        Form.section("Receive UCXLog TRX info") {
            Form.check(
                "Update MYQRG from RadioInfo messages received on the shared log-sync port",
                state.trxSynch_ucxLogUDPListenerEnabled,
            ) { state.trxSynch_ucxLogUDPListenerEnabled = it; revision++ }
        }

        Form.section("Win-Test TRX sync") {
            Form.check(
                "Win-Test STATUS QRG Sync (updates own QRG from Win-Test transceiver frequency)",
                state.logsynch_wintestQrgSyncEnabled,
            ) { state.logsynch_wintestQrgSyncEnabled = it; revision++ }

            Form.check(
                "Use pass frequency from Win-Test STATUS (instead of own QRG)",
                state.logsynch_wintestUsePassQrg,
            ) { state.logsynch_wintestUsePassQrg = it; revision++ }

            GuardedTextField(
                label = "Win-Test station name filter (e.g. STN1, empty = accept all)",
                value = state.logsynch_wintestNetworkStationNameOfWintestClient1,
                onAccepted = {
                    state.logsynch_wintestNetworkStationNameOfWintestClient1 = it
                    revision++
                },
            )
        }

        Form.section("What an enabled source does") {
            Text(TrxSynchTabState.QRG_SOURCE_HINT)
        }
    }
}
