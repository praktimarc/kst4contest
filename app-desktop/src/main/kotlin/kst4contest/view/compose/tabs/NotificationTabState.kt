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
import kst4contest.view.i18n.CurrentStrings
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.setting
import java.util.Locale

/**
 * State of the notification tab: sounds, band-upgrade hints, the local DX Cluster
 * server and the QSO monitoring list.
 *
 * Edits are staged in the window's [SettingsDraft] and take effect when the operator
 * applies them; the monitoring list too. The test spot is an action and is sent at once.
 *
 * Two of these reach past the preferences into the running session, which the settings
 * coverage test cannot see, so they are constructor callbacks rather than plain
 * properties: bringing the server in line with its switch and port, and sending the test
 * spot.
 *
 * @param monitoredRoster `ChatController.getLstNotify_QSOSniffer_sniffedCallSignList`;
 *        it lives on the controller, not in the preferences, which is why it is
 *        passed separately.
 * @param applyDxClusterServer brings the running server in line with the applied switch
 *        and port: restarted while enabled, stopped otherwise. The switch and the port
 *        share it, so one apply restarts the server once, on the new port.
 * @param broadcastTestSpot delivers the spot and returns null on success or the
 *        reason it could not be delivered.
 */
class NotificationTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
    private val monitoredRoster: SimpleRoster<String>,
    private val applyDxClusterServer: () -> Unit,
    private val broadcastTestSpot: (ChatMember) -> String?,
) {

    var notify_playSimpleSounds: Boolean by draft.setting(
        { prefs.isNotify_playSimpleSounds() },
    ) { prefs.setNotify_playSimpleSounds(it) }

    var notify_playCWCallsignsOnRxedPMs: Boolean by draft.setting(
        { prefs.isNotify_playCWCallsignsOnRxedPMs() },
    ) { prefs.setNotify_playCWCallsignsOnRxedPMs(it) }

    var notify_playVoiceCallsignsOnRxedPMs: Boolean by draft.setting(
        { prefs.isNotify_playVoiceCallsignsOnRxedPMs() },
    ) { prefs.setNotify_playVoiceCallsignsOnRxedPMs(it) }

    var notify_bandUpgradeHintOnLogEnabled: Boolean by draft.setting(
        { prefs.isNotify_bandUpgradeHintOnLogEnabled() },
    ) { prefs.setNotify_bandUpgradeHintOnLogEnabled(it) }

    var notify_bandUpgradePriorityBoostEnabled: Boolean by draft.setting(
        { prefs.isNotify_bandUpgradePriorityBoostEnabled() },
    ) { prefs.setNotify_bandUpgradePriorityBoostEnabled(it) }

    private var dxClusterServerEnabled: Boolean by draft.setting(
        { prefs.isNotify_dxClusterServerEnabled() },
        effectKey = DX_CLUSTER_SERVER,
        effect = { applyDxClusterServer() },
    ) { prefs.setNotify_dxClusterServerEnabled(it) }

    private var dxClusterServerPort: Int by draft.setting(
        { prefs.getNotify_dxclusterServerPort() },
        effectKey = DX_CLUSTER_SERVER,
        effect = { applyDxClusterServer() },
    ) { prefs.setNotify_dxclusterServerPort(it) }

    private var spotterCallSign: String by draft.setting(
        { prefs.getNotify_DXCSrv_SpottersCallSign().get() },
    ) { prefs.setNotify_DXCSrv_SpottersCallSign(it) }

    val notify_dxClusterServerEnabled: Boolean
        get() = dxClusterServerEnabled

    val notify_dxclusterServerPort: Int
        get() = dxClusterServerPort

    val notify_DXCSrv_SpottersCallSign: String
        get() = spotterCallSign

    /*
     * Stored as a prefix string, so an unreadable value is possible. It is shown as 144 here
     * and written back as 144 by repairStoredSettings before the window is built -- never
     * from this read, which runs during composition.
     */
    private var fallbackBand: Band by draft.setting(
        { Band.fromPrefix(prefs.getNotify_optionalFrequencyPrefix().get()) ?: Band.B_144 },
    ) { prefs.setNotify_optionalFrequencyPrefix(it.prefix) }

    /** The band a spot falls back to when its frequency carries no band information. */
    var notify_optionalFrequencyPrefix: Band
        get() = fallbackBand
        set(value) { fallbackBand = value }

    val monitoredCallSigns = EditableListState(
        monitoredRoster.snapshot(),
        rule = EntryRule { candidate, _, others -> judgeMonitoredCallSign(candidate, others) },
    )

    /** Stages the server switch; applying it starts or stops the server. */
    fun enableDxClusterServer(enabled: Boolean) {
        dxClusterServerEnabled = enabled
    }

    /**
     * @return null when staged, otherwise the reason the port was refused.
     */
    fun commitDxClusterServerPort(entered: String): String? {
        val port = entered.trim().toIntOrNull()

        if (port == null || port < 1 || port > 65535) {
            return CurrentStrings.get().notificationInvalidPort(entered.trim())
        }

        /*
         * The stored port staged again is no change, so it restarts nothing: restarting on
         * an unchanged port would drop a connected logger for nothing, and the JavaFX field
         * guarded the restart the same way.
         */
        dxClusterServerPort = port
        return null
    }

    /**
     * @return null when staged, otherwise the reason the callsign was refused.
     */
    fun commitSpotterCallSign(entered: String): String? {
        val spotterCallSign = entered.trim().uppercase(Locale.ROOT)

        if (!GuiUtils.isCallSignSyntax(spotterCallSign)) {
            return CurrentStrings.get().notificationInvalidSpotter(spotterCallSign)
        }

        this.spotterCallSign = spotterCallSign
        return null
    }

    /**
     * Appends a callsign to the monitoring list and stages the list.
     *
     * @return null when added, otherwise the reason it was refused.
     */
    fun addMonitoredCallSign(entered: String): String? {
        val verdict = judgeMonitoredCallSign(entered, monitoredCallSigns.texts)

        if (verdict is CommitOutcome.Refused) {
            return verdict.message
        }

        if (verdict !is CommitOutcome.Stored) {
            return CurrentStrings.get().notificationInvalidCallsign
        }

        /*
         * Appended, not inserted at the top: the JavaFX button called add on the
         * roster. The order is the order the operator entered them in.
         */
        monitoredCallSigns.addAtBottom(verdict.text)
        commitMonitoredCallSigns()
        return null
    }

    /** Stages the list; applying it fills the monitoring roster. */
    fun commitMonitoredCallSigns() {
        draft.stage(
            MONITORED_CALL_SIGNS,
            monitoredCallSigns.texts.toList(),
            live = { monitoredRoster.snapshot() },
            write = { monitoredCallSigns.commitTo(monitoredRoster) },
        )
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
            return CommitOutcome.Refused(CurrentStrings.get().notificationInvalidCallsign)
        }

        if (others.any { it != null && it.equals(baseCall, ignoreCase = true) }) {
            return CommitOutcome.Refused(CurrentStrings.get().notificationAlreadyMonitored)
        }

        return CommitOutcome.Stored(baseCall)
    }

    private companion object {
        /** The effect key the server switch and port share. */
        const val DX_CLUSTER_SERVER = "dxClusterServer"

        const val MONITORED_CALL_SIGNS = "NotificationTabState.monitoredCallSigns"
    }
}
