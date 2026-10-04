package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences

/**
 * The Log synch tab as a typed facade over ChatPreferences.
 *
 * Every property writes through immediately, and there is deliberately no apply().
 * That mirrors the JavaFX window it replaces: its controls each wrote into
 * ChatPreferences the moment they changed, and its two buttons only persisted
 * ("Save settings" calls writePreferencesToXmlFile) or closed the window.
 *
 * Two things this tab controlled are not settings at all but effects on the running
 * session, and no coverage test can see them because they are not ChatPreferences
 * setters. They are taken as callbacks rather than from a ChatController so the rules
 * stay testable without opening a database:
 *
 *  - the Win-Test UDP listener is started and stopped with its setting;
 *  - the Win-Test broadcast address is auto-detected while it is still the catch-all
 *    default, which the JavaFX window did while building the settings pane.
 *
 * Ports, transports and framing are untouched: the accepted values, the defaults and
 * the moment a socket is rebound are the ones the JavaFX window had.
 *
 * @param restartWintestListener rebinds the Win-Test UDP listener; maps to
 *   ChatController.restartWintestUdpListenerIfEnabled()
 * @param stopWintestListener maps to ChatController.stopWintestUdpListener()
 * @param detectWintestBroadcastAddress the detected subnet broadcast address, or null
 *   when none could be determined; maps to detectPreferredWintestBroadcastAddress()
 */
class LogSynchTabState(
    private val prefs: ChatPreferences,
    private val restartWintestListener: () -> Unit,
    private val stopWintestListener: () -> Unit,
    private val detectWintestBroadcastAddress: () -> String?,
) {

    init {
        autoDetectBroadcastAddressWhileStillDefault()
    }

    var logsynch_fileBasedWkdCallInterpreterEnabled: Boolean
        get() = prefs.isLogsynch_fileBasedWkdCallInterpreterEnabled()
        set(value) { prefs.setLogsynch_fileBasedWkdCallInterpreterEnabled(value) }

    /**
     * The log file polled for worked callsigns. The JavaFX tab showed it read-only and
     * changed it through a file chooser only, so the Compose tab does the same: a typed
     * path could not be checked for existence there either, and letting one be typed
     * would be a new capability, not a ported one.
     */
    var logsynch_fileBasedWkdCallInterpreterFileNameReadOnly: String
        get() = prefs.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly()
        set(value) { prefs.setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(value) }

    var logsynch_ucxUDPWkdCallListenerEnabled: Boolean
        get() = prefs.isLogsynch_ucxUDPWkdCallListenerEnabled()
        set(value) { prefs.setLogsynch_ucxUDPWkdCallListenerEnabled(value) }

    /**
     * Shared UDP port for QSO and TRX messages, default 12060. The JavaFX field
     * accepted any number here and rebound nothing — that listener comes up at start —
     * so neither a range nor a restart is added.
     */
    var logsynch_ucxUDPWkdCallListenerPort: Int
        get() = prefs.getLogsynch_ucxUDPWkdCallListenerPort()
        set(value) { prefs.setLogsynch_ucxUDPWkdCallListenerPort(value) }

    var logsynch_wintestNetworkListenerEnabled: Boolean
        get() = prefs.isLogsynch_wintestNetworkListenerEnabled()
        set(value) {
            prefs.setLogsynch_wintestNetworkListenerEnabled(value)
            if (value) restartWintestListener() else stopWintestListener()
        }

    /**
     * UDP port for the Win-Test listener, default 9871. The setting is written at once
     * like every other, but the socket is only rebound by [commitWintestNetworkPort] —
     * the JavaFX field did that on focus loss, and rebinding per keystroke would bind
     * and unbind a UDP port for every digit on the way to the intended one.
     */
    var logsynch_wintestNetworkPort: Int
        get() = prefs.getLogsynch_wintestNetworkPort()
        set(value) { prefs.setLogsynch_wintestNetworkPort(value) }

    /** Applies the entered port to the running listener, if there is one. */
    fun commitWintestNetworkPort() {
        if (prefs.isLogsynch_wintestNetworkListenerEnabled()) {
            restartWintestListener()
        }
    }

    /** The name KST4Contest uses as source of Win-Test SKED packets. */
    var logsynch_wintestNetworkStationNameOfKST: String
        get() = prefs.getLogsynch_wintestNetworkStationNameOfKST()
        set(value) { prefs.setLogsynch_wintestNetworkStationNameOfKST(value.trim()) }

    /** UDP broadcast address used for sending to Win-Test. */
    var logsynch_wintestNetworkBroadcastAddress: String
        get() = prefs.getLogsynch_wintestNetworkBroadcastAddress()
        set(value) { prefs.setLogsynch_wintestNetworkBroadcastAddress(value.trim()) }

    /**
     * Replaces the catch-all default 255.255.255.255 with the detected subnet
     * broadcast address. An address the operator or a previous detection chose is
     * left alone, and a failed detection keeps the default: guessing an address here
     * would send SKED packets somewhere nobody is listening.
     */
    private fun autoDetectBroadcastAddressWhileStillDefault() {
        if (prefs.getLogsynch_wintestNetworkBroadcastAddress() != DEFAULT_BROADCAST_ADDRESS) {
            return
        }
        val detected = try {
            detectWintestBroadcastAddress()
        } catch (probeFailure: RuntimeException) {
            null
        }
        if (detected != null && detected.isNotBlank()) {
            prefs.setLogsynch_wintestNetworkBroadcastAddress(detected)
        }
    }

    companion object {
        /** The stored value that still means "not configured for this network". */
        const val DEFAULT_BROADCAST_ADDRESS: String = "255.255.255.255"
    }
}
