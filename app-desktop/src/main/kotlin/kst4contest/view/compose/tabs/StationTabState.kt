package kst4contest.view.compose.tabs

import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences
import kst4contest.view.compose.SettingsDraft
import kst4contest.view.compose.setting

/**
 * The Station tab as a typed facade over ChatPreferences.
 *
 * Edits are staged in the window's [SettingsDraft] and take effect when the operator
 * applies them (Apply/Close prefs, Save settings). The login credentials are the exception
 * and still write through at once: callsign, names, locator, password and both chat
 * categories. They are locked while a session exists ([loginFieldsEnabled]), and without
 * one they take effect at the next login anyway, so holding them back would only make
 * Connect use something other than what the tab shows.
 *
 * The property list was derived mechanically from the setters the JavaFX tab
 * called, intersected with the setters ChatPreferences declares.
 */
class StationTabState(
    private val prefs: ChatPreferences,
    private val draft: SettingsDraft,
    /**
     * Applies the chat category to the running session. Passed in rather than taken
     * from a ChatController so the rules stay testable without building a controller,
     * which would open a database.
     */
    private val applyMainCategory: (ChatCategory?) -> Unit,
    private val applySecondCategory: (ChatCategory?) -> Unit,
    /**
     * The state of the ON4KST session. A lambda over Compose state, so the login fields
     * follow it while the tab is open; the default suits a state built without a session.
     */
    private val connectionState: () -> On4KstConnectionState = { On4KstConnectionState.DISCONNECTED },
) {

    var loginChatCategoryMain: ChatCategory?
        get() = prefs.getLoginChatCategoryMain()
        set(value) { prefs.setLoginChatCategoryMain(value) }

    /** Null means "no second chat" — the JavaFX window wrote null for exactly that. */
    var loginChatCategorySecond: ChatCategory?
        get() = prefs.getLoginChatCategorySecond()
        set(value) { prefs.setLoginChatCategorySecond(value) }

    var stn_antennaBeamWidthDeg: Double by draft.setting({ prefs.getStn_antennaBeamWidthDeg() }) { prefs.setStn_antennaBeamWidthDeg(it) }

    var stn_bandActive10G: Boolean by draft.setting({ prefs.isStn_bandActive10G() }) { prefs.setStn_bandActive10G(it) }

    var stn_bandActive1240: Boolean by draft.setting({ prefs.isStn_bandActive1240() }) { prefs.setStn_bandActive1240(it) }

    var stn_bandActive144: Boolean by draft.setting({ prefs.isStn_bandActive144() }) { prefs.setStn_bandActive144(it) }

    var stn_bandActive2300: Boolean by draft.setting({ prefs.isStn_bandActive2300() }) { prefs.setStn_bandActive2300(it) }

    var stn_bandActive3400: Boolean by draft.setting({ prefs.isStn_bandActive3400() }) { prefs.setStn_bandActive3400(it) }

    var stn_bandActive432: Boolean by draft.setting({ prefs.isStn_bandActive432() }) { prefs.setStn_bandActive432(it) }

    var stn_bandActive50: Boolean by draft.setting({ prefs.isStn_bandActive50() }) { prefs.setStn_bandActive50(it) }

    var stn_bandActive5600: Boolean by draft.setting({ prefs.isStn_bandActive5600() }) { prefs.setStn_bandActive5600(it) }

    var stn_bandActive70: Boolean by draft.setting({ prefs.isStn_bandActive70() }) { prefs.setStn_bandActive70(it) }

    var stn_loginCallSign: String
        get() = prefs.getStn_loginCallSign()
        set(value) { prefs.setStn_loginCallSign(value) }

    var stn_loginLocatorMainCat: String
        get() = prefs.getStn_loginLocatorMainCat()
        set(value) { prefs.setStn_loginLocatorMainCat(value) }

    var stn_loginNameMainCat: String
        get() = prefs.getStn_loginNameMainCat()
        set(value) { prefs.setStn_loginNameMainCat(value) }

    var stn_loginNameSecondCat: String
        get() = prefs.getStn_loginNameSecondCat()
        set(value) { prefs.setStn_loginNameSecondCat(value) }

    var stn_loginPassword: String
        get() = prefs.getStn_loginPassword()
        set(value) { prefs.setStn_loginPassword(value) }

    var stn_maxQRBDefault: Double by draft.setting({ prefs.getStn_maxQRBDefault() }) { prefs.setStn_maxQRBDefault(it) }

    var stn_on4kstServersDns: String by draft.setting({ prefs.getStn_on4kstServersDns() }) { prefs.setStn_on4kstServersDns(it) }

    var stn_on4kstServersPort: Int by draft.setting({ prefs.getStn_on4kstServersPort() }) { prefs.setStn_on4kstServersPort(it) }

    var stn_pathAnalysisDefaultTargetAntennaGainDbi: Double by draft.setting({ prefs.getStn_pathAnalysisDefaultTargetAntennaGainDbi() }) { prefs.setStn_pathAnalysisDefaultTargetAntennaGainDbi(it) }

    var stn_pathAnalysisDefaultTargetTxPowerWatts: Double by draft.setting({ prefs.getStn_pathAnalysisDefaultTargetTxPowerWatts() }) { prefs.setStn_pathAnalysisDefaultTargetTxPowerWatts(it) }

    var stn_pathAnalysisDemRootDirectory: String by draft.setting({ prefs.getStn_pathAnalysisDemRootDirectory() }) { prefs.setStn_pathAnalysisDemRootDirectory(it) }

    var stn_pathAnalysisOwnAntennaGainDbi: Double by draft.setting({ prefs.getStn_pathAnalysisOwnAntennaGainDbi() }) { prefs.setStn_pathAnalysisOwnAntennaGainDbi(it) }

    var stn_pathAnalysisOwnAntennaHeightMeters: Double by draft.setting({ prefs.getStn_pathAnalysisOwnAntennaHeightMeters() }) { prefs.setStn_pathAnalysisOwnAntennaHeightMeters(it) }

    var stn_pathAnalysisOwnTxPowerWatts: Double by draft.setting({ prefs.getStn_pathAnalysisOwnTxPowerWatts() }) { prefs.setStn_pathAnalysisOwnTxPowerWatts(it) }

    var stn_pstRotatorEnabled: Boolean by draft.setting({ prefs.isStn_pstRotatorEnabled() }) { prefs.setStn_pstRotatorEnabled(it) }

    var stn_pstRotatorHost: String by draft.setting({ prefs.getStn_pstRotatorHost() }) { prefs.setStn_pstRotatorHost(it) }

    var stn_pstRotatorPort: Int by draft.setting({ prefs.getStn_pstRotatorPort() }) { prefs.setStn_pstRotatorPort(it) }

    var stn_qtfDefault: Double by draft.setting({ prefs.getStn_qtfDefault() }) { prefs.setStn_qtfDefault(it) }

    /**
     * The chat category is held twice: in ChatPreferences and in ChatController.
     * The JavaFX window wrote both on every selection change, and the second write
     * is invisible to the settings coverage test because ChatController is not
     * ChatPreferences. Dropping it would leave the running session on the old
     * category while the stored setting said otherwise.
     */
    /**
     * Whether the station logs into a second chat category as well.
     *
     * It lives on the Station tab because that is where the JavaFX checkbox stood
     * (Kst4ContestApplication:9779, "2nd Chat:"), right next to the two category
     * pickers it governs. It decides what the Connect button does, so it belongs with
     * the login settings and not with the display options.
     */
    var loginToSecondChatEnabled: Boolean by draft.setting({ prefs.isLoginToSecondChatEnabled() }) { prefs.setLoginToSecondChatEnabled(it) }

    /**
     * Whether the login fields may be edited. The JavaFX window disabled callsign,
     * password, name, locator and both category pickers once the session was logged
     * in: changing them there would not reach the running connection, so an edit
     * would look applied and be ignored until the next login.
     *
     * Locked for every state but DISCONNECTED: a reconnect wait or a session being
     * stopped still belongs to the credentials it was started with. Derived on each read
     * rather than held, so a composition reading it follows the connection state the
     * moment that changes, provided [connectionState] reads Compose state.
     */
    val loginFieldsEnabled: Boolean
        get() = connectionState() == On4KstConnectionState.DISCONNECTED

    /**
     * The categories the operator can log into, built the way the JavaFX choice box
     * was: one per entry of `getPossibleCategoryNumbers`, numbered from 1.
     */
    val availableCategories: List<ChatCategory> by lazy {
        val probe = ChatCategory(0)

        (1..probe.possibleCategoryNumbers.size).map { ChatCategory(it) }
    }

    /**
     * The categories offered for the second chat. The main category is left out — the
     * JavaFX box did that too, and logging into the same chat twice is not a thing.
     */
    val secondCategoryOptions: List<ChatCategory>
        get() {
            val mainNumber = loginChatCategoryMain?.categoryNumber

            return availableCategories.filter { it.categoryNumber != mainNumber }
        }

    /** The label a category carries in the pickers. */
    fun describeCategory(category: ChatCategory): String =
        category.getChatCategoryName(category.categoryNumber)

    fun selectMainCategory(category: ChatCategory?) {
        loginChatCategoryMain = category
        applyMainCategory(category)
    }

    fun selectSecondCategory(category: ChatCategory?) {
        loginChatCategorySecond = category
        applySecondCategory(category)
    }
}
