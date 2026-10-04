package kst4contest.view.compose.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.Form

/**
 * The Station tab: own station, server, active bands and path-analysis defaults.
 *
 * No confirmation callback: StationTabState writes through to ChatPreferences, the
 * way the JavaFX controls did. "Save settings" persists, it does not collect.
 */
@Composable
fun StationTab(state: StationTabState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        /*
         * These are the login credentials, so they are locked while the session is
         * logged in: the running connection would not pick up a change.
         */
        Form.section("Own station") {
            Form.text("Callsign", state.stn_loginCallSign, state.loginFieldsEnabled) {
                state.stn_loginCallSign = it
            }
            Form.text("Name, main category", state.stn_loginNameMainCat, state.loginFieldsEnabled) {
                state.stn_loginNameMainCat = it
            }
            Form.text("Name, second category", state.stn_loginNameSecondCat, state.loginFieldsEnabled) {
                state.stn_loginNameSecondCat = it
            }
            Form.text("Locator", state.stn_loginLocatorMainCat, state.loginFieldsEnabled) {
                state.stn_loginLocatorMainCat = it
            }
            Form.password("Password", state.stn_loginPassword, state.loginFieldsEnabled) {
                state.stn_loginPassword = it
            }
        }

        Form.section("Chat category") {
            Form.choice(
                label = "Main category",
                items = state.availableCategories,
                selected = state.loginChatCategoryMain,
                describe = state::describeCategory,
                enabled = state.loginFieldsEnabled,
                onSelect = state::selectMainCategory,
            )
            Form.check("2nd chat: log in to a second category", state.loginToSecondChatEnabled) {
                state.loginToSecondChatEnabled = it
            }
            Form.choice(
                label = "Second category",
                items = state.secondCategoryOptions,
                selected = state.loginChatCategorySecond,
                describe = state::describeCategory,
                enabled = state.loginFieldsEnabled,
                onSelect = state::selectSecondCategory,
            )
        }

        Form.section("ON4KST server") {
            Form.text("Server host name", state.stn_on4kstServersDns) { state.stn_on4kstServersDns = it }
            Form.int("Server port", state.stn_on4kstServersPort) { state.stn_on4kstServersPort = it }
        }

        Form.flowingSection("Active bands") {
            Form.check("50 MHz", state.stn_bandActive50) { state.stn_bandActive50 = it }
            Form.check("70 MHz", state.stn_bandActive70) { state.stn_bandActive70 = it }
            Form.check("144 MHz", state.stn_bandActive144) { state.stn_bandActive144 = it }
            Form.check("432 MHz", state.stn_bandActive432) { state.stn_bandActive432 = it }
            Form.check("1240 MHz", state.stn_bandActive1240) { state.stn_bandActive1240 = it }
            Form.check("2300 MHz", state.stn_bandActive2300) { state.stn_bandActive2300 = it }
            Form.check("3400 MHz", state.stn_bandActive3400) { state.stn_bandActive3400 = it }
            Form.check("5600 MHz", state.stn_bandActive5600) { state.stn_bandActive5600 = it }
            Form.check("10 GHz", state.stn_bandActive10G) { state.stn_bandActive10G = it }
        }

        Form.section("Defaults") {
            Form.decimal("Maximum QRB", state.stn_maxQRBDefault) { state.stn_maxQRBDefault = it }
            Form.decimal("QTF", state.stn_qtfDefault) { state.stn_qtfDefault = it }
            Form.decimal("Antenna beam width, degrees", state.stn_antennaBeamWidthDeg) { state.stn_antennaBeamWidthDeg = it }
        }

        Form.section("PSTRotator") {
            Form.check("Rotator sync enabled", state.stn_pstRotatorEnabled) { state.stn_pstRotatorEnabled = it }
            Form.text("Rotator host", state.stn_pstRotatorHost) { state.stn_pstRotatorHost = it }
            Form.int("Rotator port", state.stn_pstRotatorPort) { state.stn_pstRotatorPort = it }
        }

        Form.section("Path analysis defaults") {
            Form.decimal("Own TX power, watts", state.stn_pathAnalysisOwnTxPowerWatts) { state.stn_pathAnalysisOwnTxPowerWatts = it }
            Form.decimal("Own antenna gain, dBi", state.stn_pathAnalysisOwnAntennaGainDbi) { state.stn_pathAnalysisOwnAntennaGainDbi = it }
            Form.decimal("Own antenna height, metres", state.stn_pathAnalysisOwnAntennaHeightMeters) { state.stn_pathAnalysisOwnAntennaHeightMeters = it }
            Form.decimal("Target TX power, watts", state.stn_pathAnalysisDefaultTargetTxPowerWatts) { state.stn_pathAnalysisDefaultTargetTxPowerWatts = it }
            Form.decimal("Target antenna gain, dBi", state.stn_pathAnalysisDefaultTargetAntennaGainDbi) { state.stn_pathAnalysisDefaultTargetAntennaGainDbi = it }
            Form.text("DEM root directory", state.stn_pathAnalysisDemRootDirectory) { state.stn_pathAnalysisDemRootDirectory = it }
        }
    }
}
