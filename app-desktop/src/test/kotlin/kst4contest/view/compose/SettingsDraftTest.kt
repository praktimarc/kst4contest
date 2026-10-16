package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The settings draft: edits wait in it, and only apply() writes them and runs their effects.
 */
class SettingsDraftTest {

    /** A stored setting with a log of what happened to it. */
    private class Stored(var value: Int, private val log: MutableList<String>, private val name: String) {
        fun write(v: Int) {
            value = v
            log += "write $name=$v"
        }
    }

    @Test
    fun `a staged value is shown but not written`() {
        val log = mutableListOf<String>()
        val port = Stored(9871, log, "port")
        val draft = SettingsDraft()

        draft.stage("port", 9872, live = { port.value }, write = port::write)

        assertEquals(9872, draft.valueOf("port") { port.value })
        assertEquals(9871, port.value, "a staged value reached the stored setting")
        assertTrue(draft.hasPendingChanges)
        assertTrue(log.isEmpty())
    }

    @Test
    fun `an unstaged key reads the stored value`() {
        val draft = SettingsDraft()

        assertEquals(42, draft.valueOf("anything") { 42 })
        assertFalse(draft.hasPendingChanges)
    }

    @Test
    fun `apply writes and runs the effect, then nothing is pending`() {
        val log = mutableListOf<String>()
        val port = Stored(9871, log, "port")
        val draft = SettingsDraft()

        draft.stage("port", 9872, live = { port.value }, write = port::write, effect = { log += "restart" })
        draft.apply()

        assertEquals(9872, port.value)
        assertEquals(listOf("write port=9872", "restart"), log)
        assertFalse(draft.hasPendingChanges)
    }

    @Test
    fun `discard writes nothing and runs nothing`() {
        val log = mutableListOf<String>()
        val port = Stored(9871, log, "port")
        val draft = SettingsDraft()

        draft.stage("port", 9872, live = { port.value }, write = port::write, effect = { log += "restart" })
        draft.discard()

        assertEquals(9871, port.value)
        assertTrue(log.isEmpty())
        assertFalse(draft.hasPendingChanges)
        assertEquals(9871, draft.valueOf("port") { port.value })
    }

    @Test
    fun `staging the stored value again leaves nothing pending`() {
        val log = mutableListOf<String>()
        val flag = Stored(0, log, "flag")
        val draft = SettingsDraft()

        draft.stage("flag", 1, live = { flag.value }, write = flag::write, effect = { log += "effect" })
        draft.stage("flag", 0, live = { flag.value }, write = flag::write, effect = { log += "effect" })

        assertFalse(draft.hasPendingChanges, "a box ticked and unticked again is no change")
        draft.apply()
        assertTrue(log.isEmpty(), "apply ran something for a setting that did not change")
    }

    @Test
    fun `the last staged value of a key wins`() {
        val log = mutableListOf<String>()
        val port = Stored(1, log, "port")
        val draft = SettingsDraft()

        draft.stage("port", 2, live = { port.value }, write = port::write)
        draft.stage("port", 3, live = { port.value }, write = port::write)
        draft.apply()

        assertEquals(listOf("write port=3"), log, "one write per setting, with the final value")
    }

    /*
     * Every write lands before the first effect, so an effect sees the finished state: a
     * listener restarted between "enabled" and "port" would bind the old port.
     */
    @Test
    fun `all writes come before any effect, in the order first changed`() {
        val log = mutableListOf<String>()
        val a = Stored(0, log, "a")
        val b = Stored(0, log, "b")
        val draft = SettingsDraft()

        draft.stage("b", 1, live = { b.value }, write = b::write, effect = { log += "effect b" })
        draft.stage("a", 1, live = { a.value }, write = a::write, effect = { log += "effect a" })
        draft.stage("b", 2, live = { b.value }, write = b::write, effect = { log += "effect b" })
        draft.apply()

        assertEquals(listOf("write b=2", "write a=1", "effect b", "effect a"), log)
    }

    @Test
    fun `settings sharing an effect key run that effect once`() {
        val log = mutableListOf<String>()
        val enabled = Stored(0, log, "enabled")
        val port = Stored(9871, log, "port")
        val draft = SettingsDraft()

        draft.stage("enabled", 1, live = { enabled.value }, write = enabled::write,
            effectKey = "listener", effect = { log += "restart" })
        draft.stage("port", 9872, live = { port.value }, write = port::write,
            effectKey = "listener", effect = { log += "restart" })
        draft.apply()

        assertEquals(listOf("write enabled=1", "write port=9872", "restart"), log)
    }

    @Test
    fun `a failing effect still leaves the draft empty`() {
        val draft = SettingsDraft()
        var stored = 0

        draft.stage("k", 1, live = { stored }, write = { stored = it }, effect = { error("socket refused") })

        runCatching { draft.apply() }

        assertEquals(1, stored)
        assertFalse(draft.hasPendingChanges, "a half-applied draft would be applied a second time")
    }

    private class TwoSettings(draft: SettingsDraft, val log: MutableList<String>) {
        var storedA = 1
        var storedB = 1
        var a: Int by draft.setting({ storedA }) { storedA = it; log += "a=$it" }
        var b: Int by draft.setting({ storedB }, effect = { log += "effect b" }) { storedB = it; log += "b=$it" }
    }

    @Test
    fun `a drafted property reads through the draft and stages its writes`() {
        val draft = SettingsDraft()
        val tab = TwoSettings(draft, mutableListOf())

        tab.a = 7

        assertEquals(7, tab.a)
        assertEquals(1, tab.storedA)
        assertEquals(1, tab.b, "staging one property touched another")
        draft.apply()
        assertEquals(7, tab.storedA)
        assertEquals(listOf("a=7"), tab.log)
    }

    @Test
    fun `a drafted property carries its effect`() {
        val draft = SettingsDraft()
        val tab = TwoSettings(draft, mutableListOf())

        tab.b = 3
        draft.apply()

        assertEquals(listOf("b=3", "effect b"), tab.log)
    }

    @Test
    fun `typed entries are committed before apply`() {
        val draft = SettingsDraft()
        var stored = 0
        val entry = draft.registerTypedEntry {
            draft.stage("k", 5, live = { stored }, write = { stored = it })
        }

        draft.apply()
        assertEquals(5, stored, "a field still holding the focus lost its entry on Apply")

        entry.close()
        draft.commitTypedEntries()
        assertFalse(draft.hasPendingChanges, "a removed field was still committed")
    }
}
