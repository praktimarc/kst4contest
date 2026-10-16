package kst4contest.view.compose

import java.awt.Point
import java.awt.Rectangle
import kst4contest.model.ChatPreferences

/**
 * The stored window positions of one profile, gated by the operator's opt-in.
 *
 * The opt-in is read on every call, so applying the setting takes effect without a
 * restart: off means nothing is restored and nothing is written.
 *
 * @param requestSave asks the layout autosave to write the preferences file
 */
class PreferencesWindowPositionMemory(
    private val prefs: ChatPreferences,
    private val requestSave: Runnable,
) : WindowPositionMemory {

    override fun load(windowId: String): StoredWindowPosition? {
        if (!prefs.isGuiOptions_rememberWindowPositions()) {
            return null
        }
        val p = prefs.getWindowPosition(windowId) ?: return null
        return StoredWindowPosition(Point(p[0], p[1]), Rectangle(p[2], p[3], p[4], p[5]))
    }

    override fun save(windowId: String, stored: StoredWindowPosition) {
        if (!prefs.isGuiOptions_rememberWindowPositions()) {
            return
        }
        prefs.setWindowPosition(
            windowId,
            stored.position.x, stored.position.y,
            stored.screen.x, stored.screen.y, stored.screen.width, stored.screen.height,
        )
        requestSave.run()
    }
}
