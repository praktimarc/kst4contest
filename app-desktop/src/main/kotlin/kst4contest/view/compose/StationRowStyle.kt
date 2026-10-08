package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.model.ChatMember

/**
 * The row colouring of the station table (chat members).
 *
 * Mimics JavaFX Kst4ContestApplication.java:2663
 */
object StationRowStyle {

    // > 1000 = NUCLEAR (Imminent Sked) -> Blinking Red (simulated here with solid red)
    val SCORE_NUCLEAR_BG = Color(0xFFFF4D4D)
    val SCORE_NUCLEAR_FG = Color.White
    
    // > 200  = High Prio (AirScout / Good Sked) -> Orange
    // > 100  = Medium Prio (Unworked / New Multi) -> Light Yellow
    // <= 0   = Low Prio / Not Reachable -> Greyed out text
    
    // We only hardcode light mode colors here to be safe and match JavaFX for now.
    // In JavaFX it checked `isGUI_darkModeActive()`, but Compose might have its own dark mode check.
    val SCORE_HIGH_BG = Color(0xFFFFCC00)
    val SCORE_HIGH_FG = Color.Black

    val SCORE_MEDIUM_BG = Color(0xFFFFFFCC)
    val SCORE_MEDIUM_FG = Color.Black
    
    val SCORE_LOW_FG = Color(0xFFAAAAAA)

    fun accentFor(member: ChatMember, isDark: Boolean = false): CellAccent? {
        val score = member.currentPriorityScore

        return when {
            score > 1000 -> CellAccent(background = SCORE_NUCLEAR_BG, foreground = SCORE_NUCLEAR_FG)
            score >= 200 -> CellAccent(background = if (isDark) Color(0xFFCC6600) else SCORE_HIGH_BG, foreground = SCORE_HIGH_FG)
            score >= 100 -> CellAccent(background = if (isDark) Color(0xFF888800) else SCORE_MEDIUM_BG, foreground = SCORE_MEDIUM_FG)
            score <= 0 -> CellAccent(background = null, foreground = if (isDark) Color(0xFF666666) else SCORE_LOW_FG)
            else -> null
        }
    }
}
