package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences

/**
 * The TRX synch tab as a typed facade over ChatPreferences.
 *
 * Every property writes through immediately and there is deliberately no apply(), the
 * way the JavaFX controls behaved.
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
 *   and detaches it otherwise; maps to attachOwnQrgFollower()/detachOwnQrgFollower()
 *   plus the tooltip on the own-QRG field. Not a ChatPreferences setter, so no coverage
 *   test can see it, and passed in so the rules stay testable without a controller.
 */
class TrxSynchTabState(
    private val prefs: ChatPreferences,
    private val applyOwnQrgFollower: (Boolean) -> Unit,
) {

    var trxSynch_ucxLogUDPListenerEnabled: Boolean
        get() = prefs.isTrxSynch_ucxLogUDPListenerEnabled()
        set(value) {
            prefs.setTrxSynch_ucxLogUDPListenerEnabled(value)
            applyOwnQrgFollower(anyQrgSourceEnabled)
        }

    var logsynch_wintestQrgSyncEnabled: Boolean
        get() = prefs.isLogsynch_wintestQrgSyncEnabled()
        set(value) {
            prefs.setLogsynch_wintestQrgSyncEnabled(value)
            applyOwnQrgFollower(anyQrgSourceEnabled)
        }

    /** Use the Win-Test pass frequency from STATUS instead of the own QRG. */
    var logsynch_wintestUsePassQrg: Boolean
        get() = prefs.isLogsynch_wintestUsePassQrg()
        set(value) { prefs.setLogsynch_wintestUsePassQrg(value) }

    /**
     * Win-Test station name filter. An empty filter accepts every station, which is
     * why blank is a legal value here and is stored as the empty string, trimmed the
     * way the JavaFX field committed it.
     */
    var logsynch_wintestNetworkStationNameOfWintestClient1: String
        get() = prefs.getLogsynch_wintestNetworkStationNameOfWintestClient1()
        set(value) { prefs.setLogsynch_wintestNetworkStationNameOfWintestClient1(value.trim()) }

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
    }
}
