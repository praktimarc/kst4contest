package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.setting

/**
 * The Log synch tab as a typed facade over ChatPreferences.
 *
 * Edits are staged in the window's [SettingsDraft] and take effect when the operator
 * applies them. The log file path is the exception: it changes through a file chooser,
 * an action, and is written when chosen.
 *
 * Two things this tab controlled are not settings at all but effects on the running
 * session, and no coverage test can see them because they are not ChatPreferences
 * setters. They are taken as callbacks rather than from a ChatController so the rules
 * stay testable without opening a database:
 *
 *  - the Win-Test UDP listener is started, stopped or rebound when its switch or its port
 *    is applied -- once per apply, see [applyWintestListener];
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
    private val draft: SettingsDraft,
    private val restartWintestListener: () -> Unit,
    private val stopWintestListener: () -> Unit,
    private val detectWintestBroadcastAddress: () -> String?,
) {

    init {
        autoDetectBroadcastAddressWhileStillDefault()
    }

    var logsynch_fileBasedWkdCallInterpreterEnabled: Boolean by draft.setting(
        { prefs.isLogsynch_fileBasedWkdCallInterpreterEnabled() },
    ) { prefs.setLogsynch_fileBasedWkdCallInterpreterEnabled(it) }

    /**
     * The log file polled for worked callsigns. The JavaFX tab showed it read-only and
     * changed it through a file chooser only, so the Compose tab does the same: a typed
     * path could not be checked for existence there either, and letting one be typed
     * would be a new capability, not a ported one.
     */
    var logsynch_fileBasedWkdCallInterpreterFileNameReadOnly: String
        get() = prefs.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly()
        set(value) { prefs.setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(value) }

    var logsynch_ucxUDPWkdCallListenerEnabled: Boolean by draft.setting(
        { prefs.isLogsynch_ucxUDPWkdCallListenerEnabled() },
    ) { prefs.setLogsynch_ucxUDPWkdCallListenerEnabled(it) }

    /**
     * Shared UDP port for QSO and TRX messages, default 12060. The JavaFX field
     * accepted any number here and rebound nothing — that listener comes up at start —
     * so neither a range nor a restart is added.
     */
    var logsynch_ucxUDPWkdCallListenerPort: Int by draft.setting(
        { prefs.getLogsynch_ucxUDPWkdCallListenerPort() },
    ) { prefs.setLogsynch_ucxUDPWkdCallListenerPort(it) }

    var logsynch_wintestNetworkListenerEnabled: Boolean by draft.setting(
        { prefs.isLogsynch_wintestNetworkListenerEnabled() },
        effectKey = WINTEST_LISTENER,
        effect = ::applyWintestListener,
    ) { prefs.setLogsynch_wintestNetworkListenerEnabled(it) }

    /**
     * UDP port for the Win-Test listener, default 9871. The socket is rebound when the port
     * is applied, not per keystroke: rebinding per digit would bind and unbind a UDP port
     * for every number on the way to the intended one.
     */
    var logsynch_wintestNetworkPort: Int by draft.setting(
        { prefs.getLogsynch_wintestNetworkPort() },
        effectKey = WINTEST_LISTENER,
        effect = ::applyWintestListener,
    ) { prefs.setLogsynch_wintestNetworkPort(it) }

    /*
     * The two trimmed values are staged trimmed, so the draft holds what will be stored and a
     * trailing space does not count as a change.
     */
    private var stationNameOfKst: String by draft.setting(
        { prefs.getLogsynch_wintestNetworkStationNameOfKST() },
    ) { prefs.setLogsynch_wintestNetworkStationNameOfKST(it) }

    private var broadcastAddress: String by draft.setting(
        { prefs.getLogsynch_wintestNetworkBroadcastAddress() },
    ) { prefs.setLogsynch_wintestNetworkBroadcastAddress(it) }

    /** The name KST4Contest uses as source of Win-Test SKED packets. */
    var logsynch_wintestNetworkStationNameOfKST: String
        get() = stationNameOfKst
        set(value) { stationNameOfKst = value.trim() }

    /** UDP broadcast address used for sending to Win-Test. */
    var logsynch_wintestNetworkBroadcastAddress: String
        get() = broadcastAddress
        set(value) { broadcastAddress = value.trim() }

    /**
     * Brings the Win-Test listener in line with the applied switch and port: rebound while
     * enabled, stopped otherwise. The switch and the port share it, so applying both runs it
     * once and binds the new port; a port changed while the listener is off meets a listener
     * that is not running, and stopping that does nothing.
     */
    private fun applyWintestListener() {
        if (prefs.isLogsynch_wintestNetworkListenerEnabled()) restartWintestListener() else stopWintestListener()
    }

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

        /** The effect key the Win-Test switch and port share. */
        private const val WINTEST_LISTENER = "wintestListener"
    }
}
