package kst4contest.view.compose.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form

/**
 * The beacon tab: one CQ template per chat category and the interval they share.
 *
 * The interval is one field for both categories. The second category keeps its own
 * stored value for the sake of the configuration file, but the operator never saw
 * two intervals and setting them apart was never possible.
 */
@Composable
fun BeaconTab(state: BeaconTabState, onRefused: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Form.section("CQ beacon for " + state.mainCategoryName) {
            Form.check("Send the beacon in this category", state.bcn_beaconsEnabledMainCat) {
                state.bcn_beaconsEnabledMainCat = it
            }
            Form.committed(
                label = "Beacon message",
                stored = state.beaconTextMainCat,
                commit = state::commitBeaconTextMainCat,
                onRefused = onRefused,
            )
        }

        Form.section("CQ beacon for " + state.secondCategoryName) {
            Form.check("Send the beacon in this category", state.bcn_beaconsEnabledSecondCat) {
                state.bcn_beaconsEnabledSecondCat = it
            }
            Form.committed(
                label = "Beacon message",
                stored = state.beaconTextSecondCat,
                commit = state::commitBeaconTextSecondCat,
                onRefused = onRefused,
            )
        }

        Form.section("Shared interval") {
            Form.committed(
                label = "Interval in whole minutes",
                stored = state.beaconIntervalMinutes.toString(),
                commit = state::commitBeaconInterval,
                onRefused = onRefused,
            )
        }
    }
}
