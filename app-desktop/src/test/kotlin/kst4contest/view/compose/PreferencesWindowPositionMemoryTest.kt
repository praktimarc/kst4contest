package kst4contest.view.compose

import java.awt.Point
import java.awt.Rectangle
import java.nio.file.Path
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * The opt-in gate of the stored window positions: off means nothing is restored and
 * nothing is written, not even a request to save.
 */
class PreferencesWindowPositionMemoryTest {

    @TempDir
    lateinit var directory: Path

    private val stored = StoredWindowPosition(Point(2100, 40), Rectangle(1920, 0, 2560, 1440))

    @Test
    fun `with the opt-in off nothing is written or restored`() {
        val prefs = ChatPreferences(directory.resolve("preferences.xml"))
        prefs.setWindowPosition("Map", 1, 2, 0, 0, 10, 10)
        var saves = 0
        val memory = PreferencesWindowPositionMemory(prefs) { saves++ }

        memory.save("settings-window", stored)

        assertEquals(0, saves)
        assertNull(prefs.getWindowPosition("settings-window"))
        assertNull(memory.load("Map"))
    }

    @Test
    fun `with the opt-in on a position is stored, saved and restored`() {
        val prefs = ChatPreferences(directory.resolve("preferences.xml"))
        prefs.setGuiOptions_rememberWindowPositions(true)
        var saves = 0
        val memory = PreferencesWindowPositionMemory(prefs) { saves++ }

        memory.save("Map", stored)

        assertEquals(1, saves)
        assertEquals(stored, memory.load("Map"))
    }
}
