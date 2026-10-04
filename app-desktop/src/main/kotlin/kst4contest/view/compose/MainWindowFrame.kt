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
    fun offlineChatState(state: On4KstConnectionState?): String =
        when (state ?: On4KstConnectionState.DISCONNECTED) {
            On4KstConnectionState.RECONNECT_WAIT -> "CONNECTION LOST – reconnect scheduled"
            On4KstConnectionState.CONNECTING -> "Connecting to ON4KST…"
            On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT,
            On4KstConnectionState.AUTHENTICATING -> "Connected – authenticating with ON4KST…"
            On4KstConnectionState.SYNCING_MAIN_CHAT,
            On4KstConnectionState.SYNCING_SECOND_CHAT -> "Connected – synchronizing ON4KST chat data…"
            else -> "DISCONNECTED!"
        }
}
