package kst4contest.view.compose.tabs

import kst4contest.model.ChatCategory
import kst4contest.model.ChatPreferences

/**
 * The Station tab as a typed facade over ChatPreferences.
 *
 * Every property writes through immediately, and there is deliberately no apply().
 * That mirrors the JavaFX window it replaces: its controls each wrote into
 * ChatPreferences the moment they changed, and its two buttons only persisted
 * ("Save settings" calls writePreferencesToXmlFile) or closed the window
 * ("Apply/Close prefs" just hides it). Buffering edits until a confirmation would
 * change behaviour an operator may rely on: today a changed setting takes effect
 * in the running session at once, whether or not it is ever written to disk.
 *
 * The property list was derived mechanically from the setters the JavaFX tab
 * called, intersected with the setters ChatPreferences declares.
 */
class StationTabState(
    private val prefs: ChatPreferences,
    /**
     * Applies the chat category to the running session. Passed in rather than taken
     * from a ChatController so the rules stay testable without building a controller,
     * which would open a database.
     */
    private val applyMainCategory: (ChatCategory?) -> Unit,
    private val applySecondCategory: (ChatCategory?) -> Unit,
) {

    var loginChatCategoryMain: ChatCategory?
        get() = prefs.getLoginChatCategoryMain()
        set(value) { prefs.setLoginChatCategoryMain(value) }

    /** Null means "no second chat" — the JavaFX window wrote null for exactly that. */
    var loginChatCategorySecond: ChatCategory?
        get() = prefs.getLoginChatCategorySecond()
        set(value) { prefs.setLoginChatCategorySecond(value) }

    var stn_antennaBeamWidthDeg: Double
        get() = prefs.getStn_antennaBeamWidthDeg()
        set(value) { prefs.setStn_antennaBeamWidthDeg(value) }

    var stn_bandActive10G: Boolean
        get() = prefs.isStn_bandActive10G()
        set(value) { prefs.setStn_bandActive10G(value) }

    var stn_bandActive1240: Boolean
        get() = prefs.isStn_bandActive1240()
        set(value) { prefs.setStn_bandActive1240(value) }

    var stn_bandActive144: Boolean
        get() = prefs.isStn_bandActive144()
        set(value) { prefs.setStn_bandActive144(value) }

    var stn_bandActive2300: Boolean
        get() = prefs.isStn_bandActive2300()
        set(value) { prefs.setStn_bandActive2300(value) }

    var stn_bandActive3400: Boolean
        get() = prefs.isStn_bandActive3400()
        set(value) { prefs.setStn_bandActive3400(value) }

    var stn_bandActive432: Boolean
        get() = prefs.isStn_bandActive432()
        set(value) { prefs.setStn_bandActive432(value) }

    var stn_bandActive50: Boolean
        get() = prefs.isStn_bandActive50()
        set(value) { prefs.setStn_bandActive50(value) }

    var stn_bandActive5600: Boolean
        get() = prefs.isStn_bandActive5600()
        set(value) { prefs.setStn_bandActive5600(value) }

    var stn_bandActive70: Boolean
        get() = prefs.isStn_bandActive70()
        set(value) { prefs.setStn_bandActive70(value) }

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

    var stn_maxQRBDefault: Double
        get() = prefs.getStn_maxQRBDefault()
        set(value) { prefs.setStn_maxQRBDefault(value) }

    var stn_on4kstServersDns: String
        get() = prefs.getStn_on4kstServersDns()
        set(value) { prefs.setStn_on4kstServersDns(value) }

    var stn_on4kstServersPort: Int
        get() = prefs.getStn_on4kstServersPort()
        set(value) { prefs.setStn_on4kstServersPort(value) }

    var stn_pathAnalysisDefaultTargetAntennaGainDbi: Double
        get() = prefs.getStn_pathAnalysisDefaultTargetAntennaGainDbi()
        set(value) { prefs.setStn_pathAnalysisDefaultTargetAntennaGainDbi(value) }

    var stn_pathAnalysisDefaultTargetTxPowerWatts: Double
        get() = prefs.getStn_pathAnalysisDefaultTargetTxPowerWatts()
        set(value) { prefs.setStn_pathAnalysisDefaultTargetTxPowerWatts(value) }

    var stn_pathAnalysisDemRootDirectory: String
        get() = prefs.getStn_pathAnalysisDemRootDirectory()
        set(value) { prefs.setStn_pathAnalysisDemRootDirectory(value) }

    var stn_pathAnalysisOwnAntennaGainDbi: Double
        get() = prefs.getStn_pathAnalysisOwnAntennaGainDbi()
        set(value) { prefs.setStn_pathAnalysisOwnAntennaGainDbi(value) }

    var stn_pathAnalysisOwnAntennaHeightMeters: Double
        get() = prefs.getStn_pathAnalysisOwnAntennaHeightMeters()
        set(value) { prefs.setStn_pathAnalysisOwnAntennaHeightMeters(value) }

    var stn_pathAnalysisOwnTxPowerWatts: Double
        get() = prefs.getStn_pathAnalysisOwnTxPowerWatts()
        set(value) { prefs.setStn_pathAnalysisOwnTxPowerWatts(value) }

    var stn_pstRotatorEnabled: Boolean
        get() = prefs.isStn_pstRotatorEnabled()
        set(value) { prefs.setStn_pstRotatorEnabled(value) }

    var stn_pstRotatorHost: String
        get() = prefs.getStn_pstRotatorHost()
        set(value) { prefs.setStn_pstRotatorHost(value) }

    var stn_pstRotatorPort: Int
        get() = prefs.getStn_pstRotatorPort()
        set(value) { prefs.setStn_pstRotatorPort(value) }

    var stn_qtfDefault: Double
        get() = prefs.getStn_qtfDefault()
        set(value) { prefs.setStn_qtfDefault(value) }

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
    var loginToSecondChatEnabled: Boolean
        get() = prefs.isLoginToSecondChatEnabled()
        set(value) { prefs.setLoginToSecondChatEnabled(value) }

    /**
     * Whether the login fields may be edited. The JavaFX window disabled callsign,
     * password, name, locator and both category pickers once the session was logged
     * in: changing them there would not reach the running connection, so an edit
     * would look applied and be ignored until the next login.
     */
    var loginFieldsEnabled: Boolean = true

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
