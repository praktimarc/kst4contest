package kst4contest.view.compose.tabs

import kst4contest.model.Band
import kst4contest.model.ChatMember
import kst4contest.model.ChatPreferences
import kst4contest.observe.SimpleRoster
import kst4contest.observe.SimpleValue
import kst4contest.view.GuiUtils
import kst4contest.view.compose.CommitOutcome
import kst4contest.view.compose.EditableListState
import kst4contest.view.compose.EntryRule
import java.util.Locale

/**
 * State of the notification tab: sounds, band-upgrade hints, the local DX Cluster
 * server and the QSO monitoring list.
 *
 * Three of these reach past the preferences into the running session, which the
 * settings coverage test cannot see, so they are constructor callbacks rather than
 * plain properties: starting and stopping the server, restarting it after a port
 * change, and sending the test spot.
 *
 * @param monitoredRoster `ChatController.getLstNotify_QSOSniffer_sniffedCallSignList`;
 *        it lives on the controller, not in the preferences, which is why it is
 *        passed separately.
 * @param applyDxClusterServerEnabled starts or stops the server, the work the
 *        JavaFX checkbox did beyond writing the flag.
 * @param applyDxClusterServerPort restarts the server; called only when the port
 *        actually changed, as the JavaFX field did.
 * @param broadcastTestSpot delivers the spot and returns null on success or the
 *        reason it could not be delivered.
 */
class NotificationTabState(
    private val prefs: ChatPreferences,
    private val monitoredRoster: SimpleRoster<String>,
    private val applyDxClusterServerEnabled: (Boolean) -> Unit,
    private val applyDxClusterServerPort: (Int) -> Unit,
    private val broadcastTestSpot: (ChatMember) -> String?,
) {

    var notify_playSimpleSounds: Boolean
        get() = prefs.isNotify_playSimpleSounds()
        set(value) { prefs.setNotify_playSimpleSounds(value) }

    var notify_playCWCallsignsOnRxedPMs: Boolean
        get() = prefs.isNotify_playCWCallsignsOnRxedPMs()
        set(value) { prefs.setNotify_playCWCallsignsOnRxedPMs(value) }

    var notify_playVoiceCallsignsOnRxedPMs: Boolean
        get() = prefs.isNotify_playVoiceCallsignsOnRxedPMs()
        set(value) { prefs.setNotify_playVoiceCallsignsOnRxedPMs(value) }

    var notify_bandUpgradeHintOnLogEnabled: Boolean
        get() = prefs.isNotify_bandUpgradeHintOnLogEnabled()
        set(value) { prefs.setNotify_bandUpgradeHintOnLogEnabled(value) }

    var notify_bandUpgradePriorityBoostEnabled: Boolean
        get() = prefs.isNotify_bandUpgradePriorityBoostEnabled()
        set(value) { prefs.setNotify_bandUpgradePriorityBoostEnabled(value) }

    val notify_dxClusterServerEnabled: Boolean
        get() = prefs.isNotify_dxClusterServerEnabled()

    val notify_dxclusterServerPort: Int
        get() = prefs.getNotify_dxclusterServerPort()

    val notify_DXCSrv_SpottersCallSign: String
        get() = prefs.getNotify_DXCSrv_SpottersCallSign().get()

    /**
     * The band a spot falls back to when its frequency carries no band information.
     *
     * Stored as a prefix string, so an unreadable value is possible; reading it
     * repairs the stored value to 144, which is what the JavaFX window did when it
     * built the combo box (Kst4ContestApplication:11349).
     */
    var notify_optionalFrequencyPrefix: Band
        get() {
            val stored = Band.fromPrefix(prefs.getNotify_optionalFrequencyPrefix().get())

            if (stored == null) {
                prefs.setNotify_optionalFrequencyPrefix(Band.B_144.prefix)
                return Band.B_144
            }

            return stored
        }
        set(value) { prefs.setNotify_optionalFrequencyPrefix(value.prefix) }

    val monitoredCallSigns = EditableListState(
        monitoredRoster.snapshot(),
        rule = EntryRule { candidate, _, others -> judgeMonitoredCallSign(candidate, others) },
    )

    /** Writes the flag and lets the running session start or stop the server. */
    fun enableDxClusterServer(enabled: Boolean) {
        prefs.setNotify_dxClusterServerEnabled(enabled)
        applyDxClusterServerEnabled(enabled)
    }

    /**
     * @return null when applied, otherwise the reason the port was refused.
     */
    fun commitDxClusterServerPort(entered: String): String? {
        val previousPort = prefs.getNotify_dxclusterServerPort()
        val port = entered.trim().toIntOrNull()

        if (port == null || port < 1 || port > 65535) {
            return "\"" + entered.trim() + "\" is not a valid TCP port. " +
                "Enter a value from 1 to 65535."
        }

        prefs.setNotify_dxclusterServerPort(port)

        /*
         * Restarting on an unchanged port would drop a connected logger for nothing;
         * the JavaFX field guarded the restart the same way.
         */
        if (port != previousPort) {
            applyDxClusterServerPort(port)
        }

        return null
    }

    /**
     * @return null when stored, otherwise the reason the callsign was refused.
     */
    fun commitSpotterCallSign(entered: String): String? {
        val spotterCallSign = entered.trim().uppercase(Locale.ROOT)

        if (!GuiUtils.isCallSignSyntax(spotterCallSign)) {
            return "\"" + spotterCallSign + "\" is not a valid spotter callsign."
        }

        prefs.setNotify_DXCSrv_SpottersCallSign(spotterCallSign)
        return null
    }

    /**
     * Appends a callsign to the monitoring list and fills the roster at once, as the
     * JavaFX dialog did.
     *
     * @return null when added, otherwise the reason it was refused.
     */
    fun addMonitoredCallSign(entered: String): String? {
        val verdict = judgeMonitoredCallSign(entered, monitoredCallSigns.texts)

        if (verdict is CommitOutcome.Refused) {
            return verdict.message
        }

        if (verdict !is CommitOutcome.Stored) {
            return "Please enter a valid callsign."
        }

        /*
         * Appended, not inserted at the top: the JavaFX button called add on the
         * roster. The order is the order the operator entered them in.
         */
        monitoredCallSigns.addAtBottom(verdict.text)
        commitMonitoredCallSigns()
        return null
    }

    /** Fills the monitoring roster from the list. */
    fun commitMonitoredCallSigns() {
        monitoredCallSigns.commitTo(monitoredRoster)
    }

    /**
     * Builds the spot the JavaFX test button sent and hands it to the server.
     *
     * @return null when delivered, otherwise the reason it was not.
     */
    fun sendTestSpot(): String? {
        val testSpot = ChatMember()
        /*
         * The Java setters are called by name rather than through Kotlin property
         * assignment: the foreign-write coverage test looks for these three names,
         * and it is the only check that the test spot still carries its values.
         */
        testSpot.setFrequency(SimpleValue("300"))
        testSpot.setQra("DXC test: You donated \$100!")
        testSpot.setCallSign("DO5AMF")

        return broadcastTestSpot(testSpot)
    }

    /**
     * Monitoring works on the base callsign, so every SSID and portable suffix maps
     * to one entry. A blank text removes the entry, the removal gesture of every
     * list in this window.
     */
    private fun judgeMonitoredCallSign(candidate: String, others: List<String>): CommitOutcome {
        if (candidate.isBlank()) {
            return CommitOutcome.Removed
        }

        val baseCall = ChatMember.normalizeCallSignToBaseCallSign(
            candidate.trim().uppercase(Locale.ROOT)
        )

        if (baseCall == null || !GuiUtils.isCallSignSyntax(baseCall)) {
            return CommitOutcome.Refused("Please enter a valid callsign.")
        }

        if (others.any { it != null && it.equals(baseCall, ignoreCase = true) }) {
            return CommitOutcome.Refused("This base callsign is already in the monitoring list.")
        }

        return CommitOutcome.Stored(baseCall)
    }
}
