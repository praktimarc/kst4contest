package kst4contest.view.compose

import kst4contest.controller.On4KstConnectionState

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
     * What the title says while the chat is not usable.
     *
     * The wording separates a scheduled reconnect from a dead session, which is the
     * difference between waiting and doing something about it.
     */
    /**
     * What the title's first half says.
     *
     * The live connection detail while the chat is usable -- which is the whole status line,
     * exactly what the JavaFX title carried -- and a wording about the attempt otherwise. A
     * blank detail falls through to the offline wording rather than leaving the title empty.
     *
     * @param state the connection state, null before the first push
     * @param detail the status line the controller pushes
     * @return the chat half of the title
     */
    fun chatState(state: On4KstConnectionState?, detail: String?): String =
        if (state == On4KstConnectionState.ONLINE && !detail.isNullOrBlank()) {
            detail
        } else {
            offlineChatState(state)
        }

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
