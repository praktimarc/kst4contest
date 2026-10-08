package kst4contest.view.compose

import androidx.compose.runtime.snapshots.Snapshot
import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ChatPreferences hands out MutableValue, which notifies its own listeners and nothing
 * Compose can see. A field drawn straight from one renders once and then goes stale — and
 * these three in particular move on their own: the rotator writes the QTF, and the
 * frequency follower writes MYQRG.
 *
 * The JavaFX window solved it with attachQtfFollower() and attachOwnQrgFollower(); this is
 * the same idea, ending in Compose state instead of a TextField.
 */
class ObservedValueTest {

    @Test
    fun `the mirror starts at the stored value`() {
        val prefs = ChatPreferences()
        prefs.getMYQRGFirstCat().set("144.370")

        val observed = ObservedValue(prefs.getMYQRGFirstCat())

        assertEquals("144.370", observed.value)
    }

    /** The rotator moving must reach the screen without anything asking it to. */
    @Test
    fun `a change to the underlying value reaches the mirror by itself`() {
        val prefs = ChatPreferences()
        prefs.getActualQTF().set(90.0)
        val observed = ObservedValue(prefs.getActualQTF())

        prefs.getActualQTF().set(135.0)

        assertEquals(135.0, observed.value)
    }

    @Test
    fun `a null value is carried through rather than turned into a default`() {
        val prefs = ChatPreferences()
        val observed = ObservedValue(prefs.getMYQRGSecondCat())

        prefs.getMYQRGSecondCat().set(null)

        assertEquals(null, observed.value)
    }

    /**
     * The field is drawn from this, so reading it has to register as a read. Without this
     * the whole exercise is pointless: the value would be correct and invisible.
     */
    @Test
    fun `drawing from the mirror subscribes to it`() {
        val prefs = ChatPreferences()
        prefs.getMYQRGFirstCat().set("144.370")
        val observed = ObservedValue(prefs.getMYQRGFirstCat())

        val read = mutableSetOf<String>()
        Snapshot.observe(readObserver = { read += it.toString() }) {
            observed.value
        }

        assertTrue(read.isNotEmpty(), "the mirrored value was read without subscribing")
    }

    /**
     * A mirror that is dropped must stop listening. A profile switch builds a new window
     * over the same preferences, and a leaked listener keeps the old one alive and writing.
     */
    @Test
    fun `releasing the mirror stops it following`() {
        val prefs = ChatPreferences()
        prefs.getMYQRGFirstCat().set("144.370")
        val observed = ObservedValue(prefs.getMYQRGFirstCat())

        observed.release()
        prefs.getMYQRGFirstCat().set("432.200")

        assertEquals("144.370", observed.value, "a released mirror kept following")
    }

    @Test
    fun `releasing twice is harmless`() {
        val prefs = ChatPreferences()
        val observed = ObservedValue(prefs.getMYQRGFirstCat())
        observed.release()
        observed.release()
    }
}
