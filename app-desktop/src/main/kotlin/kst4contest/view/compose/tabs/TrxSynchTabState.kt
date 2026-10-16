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
 * @param applyOwnQrgFollower attaches the own-QRG follower when any source is enabled
 *   and detaches it otherwise, once per apply however many sources changed; maps to attachOwnQrgFollower()/detachOwnQrgFollower()
 *   plus the tooltip on the own-QRG field. Not a ChatPreferences setter, so no coverage
 *   test can see it, and passed in so the rules stay testable without a controller.
 */
class TrxSynchTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
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
