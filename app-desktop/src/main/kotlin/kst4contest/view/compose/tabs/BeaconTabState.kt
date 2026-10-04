package kst4contest.view.compose.tabs

import kst4contest.controller.ChatController
import kst4contest.model.ChatPreferences

/**
 * State of the beacon tab: the two CQ templates and the one interval they share.
 *
 * The two commit methods are the ports of `applyBeaconTextSetting` and
 * `applySharedBeaconInterval`. Both returned nothing and opened an alert; here they
 * return the refusal text instead, so the tab decides how to show it and a test can
 * read it.
 *
 * @param validateBeaconTemplate throws IllegalArgumentException with the reason,
 *        the contract of `ChatController.validateBeaconTemplate`.
 * @param restartBeaconTimer called only after the interval actually changed; the
 *        JavaFX code restarted the shared timer there and nowhere else.
 */
class BeaconTabState(
    private val prefs: ChatPreferences,
    private val validateBeaconTemplate: (String) -> Unit,
    private val restartBeaconTimer: () -> Unit,
    val mainCategoryName: String,
    val secondCategoryName: String,
) {

    var bcn_beaconsEnabledMainCat: Boolean
        get() = prefs.isBcn_beaconsEnabledMainCat()
        set(value) { prefs.setBcn_beaconsEnabledMainCat(value) }

    var bcn_beaconsEnabledSecondCat: Boolean
        get() = prefs.isBcn_beaconsEnabledSecondCat()
        set(value) { prefs.setBcn_beaconsEnabledSecondCat(value) }

    val beaconTextMainCat: String
        get() = prefs.getBcn_beaconTextMainCat()

    val beaconTextSecondCat: String
        get() = prefs.getBcn_beaconTextSecondCat()

    /**
     * The interval both categories share, never below the minimum.
     *
     * The clamp is not cosmetic: a stored zero would otherwise be displayed as a
     * valid setting while `ChatController` sends at the minimum anyway
     * (ChatController:3425). The JavaFX field applied the same `Math.max` when it
     * restored the value after a rejected entry.
     */
    val beaconIntervalMinutes: Int
        get() = maxOf(
            ChatController.MIN_BEACON_INTERVAL_MINUTES,
            prefs.getBcn_beaconIntervalInMinutesMainCat(),
        )

    /** @return null when stored, otherwise the reason it was refused. */
    fun commitBeaconTextMainCat(template: String): String? =
        commitBeaconText(template) { prefs.setBcn_beaconTextMainCat(it) }

    /** @return null when stored, otherwise the reason it was refused. */
    fun commitBeaconTextSecondCat(template: String): String? =
        commitBeaconText(template) { prefs.setBcn_beaconTextSecondCat(it) }

    private fun commitBeaconText(template: String, store: (String) -> Unit): String? {
        try {
            validateBeaconTemplate(template)
        } catch (refusal: IllegalArgumentException) {
            /*
             * The stored template stands. MYCALL and MYQRG are resolved by the timer,
             * so a refused template that replaced a working one would silently stop
             * the beacon instead of reporting anything.
             */
            return "The beacon message is invalid: " + refusal.message
        }

        store(template)
        return null
    }

    /**
     * Applies the shared interval to both categories and restarts the timer.
     *
     * @return null when applied, otherwise the reason it was refused.
     */
    fun commitBeaconInterval(entered: String): String? {
        val minutes = entered.trim().toIntOrNull()

        if (minutes == null || minutes < ChatController.MIN_BEACON_INTERVAL_MINUTES) {
            return "Enter a whole beacon interval of at least " +
                ChatController.MIN_BEACON_INTERVAL_MINUTES + " minute."
        }

        prefs.setBcn_beaconIntervalInMinutesMainCat(minutes)

        /*
         * The second-category value is legacy XML which nothing reads any more, but
         * it is written to keep the two in step, exactly as applySharedBeaconInterval
         * did.
         */
        prefs.setBcn_beaconIntervalInMinutesSecondCat(minutes)

        restartBeaconTimer()
        return null
    }
}
