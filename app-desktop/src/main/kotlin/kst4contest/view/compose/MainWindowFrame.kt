package kst4contest.view.compose

import kst4contest.controller.On4KstConnectionState
import kst4contest.model.ChatCategory

/** How big the main window opens. */
data class MainWindowSize(val heightDp: Double, val widthDp: Double)

/**
 * The frame around the main window: the size it opens at and what its title says.
 *
 * Both are arithmetic and string building rather than drawing, which is why they live
 * apart from the window itself and are covered by tests instead of by looking.
 */
object MainWindowFrame {

    private const val FALLBACK_HEIGHT = 768.0
    private const val FALLBACK_WIDTH = 1234.0

    /**
     * Room left for the native window decoration and the screen edge. The JavaFX scene
     * size did not include the decoration, and Compose's window size does not include
     * everything a tiling compositor puts around it either.
     */
    private const val SCREEN_MARGIN = 40.0

    /**
     * The remembered size, never larger than the screen it is opening on.
     *
     * The clamp is the point: a window remembered from a larger monitor opens with its
     * buttons past the edge and no way to reach them. Each dimension falls back on its
     * own, because half a stored size is still half a usable one.
     */
    fun startupSize(stored: DoubleArray?, screenHeight: Double, screenWidth: Double): MainWindowSize {
        var height = FALLBACK_HEIGHT
        var width = FALLBACK_WIDTH

        if (stored != null && stored.size >= 2) {
            if (stored[0].isFinite() && stored[0] > 0) height = stored[0]
            if (stored[1].isFinite() && stored[1] > 0) width = stored[1]
        }

        val availableHeight = (screenHeight - SCREEN_MARGIN).coerceAtLeast(1.0)
        val availableWidth = (screenWidth - SCREEN_MARGIN).coerceAtLeast(1.0)

        return MainWindowSize(
            heightDp = minOf(height, availableHeight),
            widthDp = minOf(width, availableWidth),
        )
    }

    /**
     * The window title: what the chat is doing, and which profile, when it is not the
     * only one. Two profiles open side by side have to be tellable apart from the
     * taskbar.
     */
    fun title(chatState: String, profileName: String?): String =
        if (profileName.isNullOrBlank()) chatState else "$chatState - $profileName"

    /**
     * What the title's first half says.
     *
     * The live connection line while the chat is usable -- exactly what the JavaFX title
     * carried -- and a wording about the attempt otherwise. The connection manager's own
     * detail ("session is authenticated and synchronized") is a diagnostic for the status
     * bar, not a title: it says nothing about category, call or traffic.
     *
     * @param state the connection state, null before the first push
     * @param onlineLine the line built by [onlineChatState], asked for only while online
     * @return the chat half of the title
     */
    fun chatState(state: On4KstConnectionState?, onlineLine: () -> String): String =
        if (state == On4KstConnectionState.ONLINE) onlineLine() else offlineChatState(state)

    /**
     * The connection line while the chat is usable, as the JavaFX title had it:
     * "Connected to: 2: 144/432 MHz as DO1JKS (Johannes 70/2) in JN59MU (106 users online,
     * 106 shown), 14 messages total."
     *
     * Counts are passed as text so MessageFormat does not group them by locale. Without a
     * main category there is nothing to name, so the line falls back to the offline wording
     * for ONLINE instead of reading "Connected to:  as ...".
     *
     * @param mainCategory the main chat category, null when none is configured
     * @param secondCategory the second category, null when it is not logged in
     * @param callSign the login callsign
     * @param name the login name of the main category
     * @param locator the login locator
     * @param usersOnline every station in the chat
     * @param shown the stations the filters let through
     * @param messagesTotal every chat message held, the global list behind the public tab
     */
    fun onlineChatState(
        mainCategory: ChatCategory?,
        secondCategory: ChatCategory?,
        callSign: String?,
        name: String?,
        locator: String?,
        usersOnline: Int,
        shown: Int,
        messagesTotal: Int,
    ): String {
        if (mainCategory == null) {
            return offlineChatState(On4KstConnectionState.ONLINE)
        }
        val strings = kst4contest.view.i18n.CurrentStrings.get()
        val categories = if (secondCategory == null) {
            mainCategory.toString()
        } else {
            strings.titleBothCategories(mainCategory.toString(), secondCategory.toString())
        }
        return strings.titleOnline(
            categories,
            callSign.orEmpty(),
            name.orEmpty(),
            locator.orEmpty(),
            usersOnline.toString(),
            shown.toString(),
            messagesTotal.toString(),
        )
    }

    /**
     * What the title says while the chat is not usable.
     *
     * The wording separates a scheduled reconnect from a dead session, which is the
     * difference between waiting and doing something about it.
     */
    fun offlineChatState(state: On4KstConnectionState?): String =
        when (state ?: On4KstConnectionState.DISCONNECTED) {
            On4KstConnectionState.RECONNECT_WAIT -> kst4contest.view.i18n.CurrentStrings.get().titleConnectionLost
            On4KstConnectionState.CONNECTING -> kst4contest.view.i18n.CurrentStrings.get().titleConnecting
            On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT,
            On4KstConnectionState.AUTHENTICATING -> kst4contest.view.i18n.CurrentStrings.get().titleAuthenticating
            On4KstConnectionState.SYNCING_MAIN_CHAT,
            On4KstConnectionState.SYNCING_SECOND_CHAT -> kst4contest.view.i18n.CurrentStrings.get().titleSynchronizing
            else -> kst4contest.view.i18n.CurrentStrings.get().titleDisconnected
        }
}
