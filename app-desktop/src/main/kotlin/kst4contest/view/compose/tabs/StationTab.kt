package kst4contest.view.compose.tabs

import kst4contest.view.i18n.LocalStrings
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
    val strings = LocalStrings.current

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        /*
         * These are the login credentials, so they are locked while the session is
         * logged in: the running connection would not pick up a change.
         */
        Form.section(strings.stationOwnSection) {
            Form.text(strings.stationCallsign, state.stn_loginCallSign, state.loginFieldsEnabled) {
                state.stn_loginCallSign = it
            }
            Form.text(strings.stationNameMain, state.stn_loginNameMainCat, state.loginFieldsEnabled) {
                state.stn_loginNameMainCat = it
            }
            Form.text(strings.stationNameSecond, state.stn_loginNameSecondCat, state.loginFieldsEnabled) {
                state.stn_loginNameSecondCat = it
            }
            Form.text(strings.stationLocator, state.stn_loginLocatorMainCat, state.loginFieldsEnabled) {
                state.stn_loginLocatorMainCat = it
            }
            Form.password(strings.stationPassword, state.stn_loginPassword, state.loginFieldsEnabled) {
                state.stn_loginPassword = it
            }
        }

        Form.section(strings.stationChatSection) {
            Form.choice(
                label = strings.stationMainCategory,
                items = state.availableCategories,
                selected = state.loginChatCategoryMain,
                describe = state::describeCategory,
                enabled = state.loginFieldsEnabled,
                onSelect = state::selectMainCategory,
            )
            Form.check(strings.stationSecondChatEnabled, state.loginToSecondChatEnabled) {
                state.loginToSecondChatEnabled = it
            }
            Form.choice(
                label = strings.stationSecondCategory,
                items = state.secondCategoryOptions,
                selected = state.loginChatCategorySecond,
                describe = state::describeCategory,
                enabled = state.loginFieldsEnabled,
                onSelect = state::selectSecondCategory,
            )
        }

        Form.section(strings.stationServerSection) {
            Form.text(strings.stationServerHost, state.stn_on4kstServersDns) { state.stn_on4kstServersDns = it }
            Form.int(strings.stationServerPort, state.stn_on4kstServersPort) { state.stn_on4kstServersPort = it }
        }

        Form.flowingSection(strings.stationBandsSection) {
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

        Form.section(strings.stationDefaultsSection) {
            Form.decimal(strings.stationMaxQrb, state.stn_maxQRBDefault) { state.stn_maxQRBDefault = it }
            Form.decimal(strings.stationQtf, state.stn_qtfDefault) { state.stn_qtfDefault = it }
            Form.decimal(strings.stationBeamWidth, state.stn_antennaBeamWidthDeg) { state.stn_antennaBeamWidthDeg = it }
        }

        Form.section(strings.stationRotatorSection) {
            Form.check(strings.stationRotatorEnabled, state.stn_pstRotatorEnabled) { state.stn_pstRotatorEnabled = it }
            Form.text(strings.stationRotatorHost, state.stn_pstRotatorHost) { state.stn_pstRotatorHost = it }
            Form.int(strings.stationRotatorPort, state.stn_pstRotatorPort) { state.stn_pstRotatorPort = it }
        }

        Form.section(strings.stationPathSection) {
            Form.decimal(strings.stationOwnTxPower, state.stn_pathAnalysisOwnTxPowerWatts) { state.stn_pathAnalysisOwnTxPowerWatts = it }
            Form.decimal(strings.stationOwnAntennaGain, state.stn_pathAnalysisOwnAntennaGainDbi) { state.stn_pathAnalysisOwnAntennaGainDbi = it }
            Form.decimal(strings.stationOwnAntennaHeight, state.stn_pathAnalysisOwnAntennaHeightMeters) { state.stn_pathAnalysisOwnAntennaHeightMeters = it }
            Form.decimal(strings.stationTargetTxPower, state.stn_pathAnalysisDefaultTargetTxPowerWatts) { state.stn_pathAnalysisDefaultTargetTxPowerWatts = it }
            Form.decimal(strings.stationTargetAntennaGain, state.stn_pathAnalysisDefaultTargetAntennaGainDbi) { state.stn_pathAnalysisDefaultTargetAntennaGainDbi = it }
            Form.text(strings.stationDemDirectory, state.stn_pathAnalysisDemRootDirectory) { state.stn_pathAnalysisDemRootDirectory = it }
        }
    }
}
