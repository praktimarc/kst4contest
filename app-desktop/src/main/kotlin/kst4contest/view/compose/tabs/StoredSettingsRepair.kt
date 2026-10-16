package kst4contest.view.compose.tabs

import kst4contest.model.Band
import kst4contest.model.ChatPreferences

/**
 * Corrects stored settings the settings window cannot show as they are, once, before the
 * window is built.
 *
 * These writes go to ChatPreferences at once and not through the draft: they are no edit of
 * the operator's, there is nothing to apply or discard, and they are what the JavaFX window
 * did while building its panes. They used to happen inside the tab states -- one during a
 * read in composition, one in a constructor -- where a write to the preferences is easy to
 * miss and runs whenever Compose decides to read. Here they run at one known moment.
 *
 *  - The fallback band of the DX cluster is stored as a prefix string; one no band has is
 *    repaired to 144 (Kst4ContestApplication:11349 in the JavaFX window).
 *  - The Win-Test broadcast address is replaced by the detected subnet broadcast address
 *    while it is still the catch-all default 255.255.255.255. An address the operator or a
 *    previous detection chose is left alone, and a failed detection keeps the default:
 *    guessing an address would send SKED packets somewhere nobody is listening.
 *
 * @param detectWintestBroadcastAddress the detected subnet broadcast address, or null when
 *        none could be determined; maps to detectPreferredWintestBroadcastAddress()
 */
fun repairStoredSettings(prefs: ChatPreferences, detectWintestBroadcastAddress: () -> String?) {
    if (Band.fromPrefix(prefs.getNotify_optionalFrequencyPrefix().get()) == null) {
        prefs.setNotify_optionalFrequencyPrefix(Band.B_144.prefix)
    }

    if (prefs.getLogsynch_wintestNetworkBroadcastAddress() == LogSynchTabState.DEFAULT_BROADCAST_ADDRESS) {
        val detected = try {
            detectWintestBroadcastAddress()
        } catch (probeFailure: RuntimeException) {
            null
        }
        if (!detected.isNullOrBlank()) {
            prefs.setLogsynch_wintestNetworkBroadcastAddress(detected)
        }
    }
}
