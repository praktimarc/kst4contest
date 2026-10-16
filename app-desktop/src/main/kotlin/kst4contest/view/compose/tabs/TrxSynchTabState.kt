package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.setting

/**
 * The TRX synch tab as a typed facade over ChatPreferences.
 *
 * Edits are staged in the window's [SettingsDraft] and take effect when the operator
 * applies them.
 *
 * The tab carries the two sources that may update the own QRG. Both live under the
 * logsynch_ and trxSynch_ prefixes, but the JavaFX window put both controls on this
 * pane — the Win-Test QRG controls were built next to the Log synch ones and then
 * added to the TRX synch grid.
 *
 * Switching a source on is not itself a frequency: PROJECT_CONTEXT records that an
 * automatic QRG update needs an enabled source *and* valid incoming RadioInfo or
 * Win-Test STATUS data. [QRG_SOURCE_HINT_KEY] names the text the tab shows so an operator
 * cannot read a checked box as "my QRG is being kept current".
 *
 * @param applySecondRotator restarts or stops rotator 2 to match the stored SO2R and rotator 2
 *   switches, once per apply. Declared before [applyOwnQrgFollower] so callers passing the
 *   follower as a trailing lambda keep binding to it.
 * @param applyOwnQrgFollower attaches the own-QRG follower when any source is enabled
 *   and detaches it otherwise, once per apply however many sources changed; maps to attachOwnQrgFollower()/detachOwnQrgFollower()
 *   plus the tooltip on the own-QRG field. Not a ChatPreferences setter, so no coverage
 *   test can see it, and passed in so the rules stay testable without a controller.
 */
class TrxSynchTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
    private val applySecondRotator: () -> Unit = {},
    private val applyOwnQrgFollower: (Boolean) -> Unit,
) {

    var trxSynch_ucxLogUDPListenerEnabled: Boolean by draft.setting(
        { prefs.isTrxSynch_ucxLogUDPListenerEnabled() },
        effectKey = OWN_QRG_FOLLOWER,
        effect = ::applyFollower,
    ) { prefs.setTrxSynch_ucxLogUDPListenerEnabled(it) }

    var logsynch_wintestQrgSyncEnabled: Boolean by draft.setting(
        { prefs.isLogsynch_wintestQrgSyncEnabled() },
        effectKey = OWN_QRG_FOLLOWER,
        effect = ::applyFollower,
    ) { prefs.setLogsynch_wintestQrgSyncEnabled(it) }

    /** Use the Win-Test pass frequency from STATUS instead of the own QRG. */
    var logsynch_wintestUsePassQrg: Boolean by draft.setting(
        { prefs.isLogsynch_wintestUsePassQrg() },
    ) { prefs.setLogsynch_wintestUsePassQrg(it) }

    // Staged trimmed, so the draft holds what will be stored.
    private var stationFilter: String by draft.setting(
        { prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1() },
    ) { prefs.setLogsynch_wintestNetworkStationNameOfWintestClient1(it) }

    /**
     * Win-Test station name filter. An empty filter accepts every station, which is
     * why blank is a legal value here and is stored as the empty string, trimmed the
     * way the JavaFX field committed it.
     */
    var logsynch_wintestNetworkStationNameOfWintestClient1: String
        get() = stationFilter
        set(value) { stationFilter = value.trim() }

    /** SO2R: radio 2 gets SECONDQRG and, with its own switch, rotator 2. */
    var trxSynch_so2rEnabled: Boolean by draft.setting(
        { prefs.isTrxSynch_so2rEnabled() },
        effectKey = StationTabState.SECOND_ROTATOR,
        effect = { applySecondRotator() },
    ) { prefs.setTrxSynch_so2rEnabled(it) }

    private var stationRadio2: String by draft.setting(
        { prefs.getLogsynch_wintestNetworkStationNameOfWintestClient2() },
    ) { prefs.setLogsynch_wintestNetworkStationNameOfWintestClient2(it) }

    /** Win-Test station of radio 2; empty sends nothing to radio 2. Staged trimmed. */
    var logsynch_wintestNetworkStationNameOfWintestClient2: String
        get() = stationRadio2
        set(value) { stationRadio2 = value.trim() }

    /* Read from the stored values: it runs on apply, after both switches are written. */
    private fun applyFollower() {
        applyOwnQrgFollower(prefs.isTrxSynch_ucxLogUDPListenerEnabled() || prefs.isLogsynch_wintestQrgSyncEnabled())
    }

    /**
     * True when at least one source may update the own QRG. One source switched off is
     * not all of them off; the follower stays attached while the other is on.
     */
    val anyQrgSourceEnabled: Boolean
        get() = trxSynch_ucxLogUDPListenerEnabled || logsynch_wintestQrgSyncEnabled

    companion object {
        /**
         * Shown on the tab. Both halves are load-bearing: an enabled source without
         * incoming data leaves the own QRG exactly as the operator typed it.
         */
        /**
         * The key of the text the tab shows. The text itself lives in the translation files;
         * keeping a copy here would be two sentences that can drift apart, and the operator
         * would read whichever one the tab happened to use.
         */
        const val QRG_SOURCE_HINT_KEY: String = "trxSynch.hint"

        /** The effect key both QRG sources share. */
        private const val OWN_QRG_FOLLOWER = "ownQrgFollower"
    }
}
