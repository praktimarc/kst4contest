package kst4contest.view.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The edits made in the settings window that have not been applied yet.
 *
 * One draft exists per opened settings window and is handed to every tab state. A tab
 * state stages a change here instead of writing ChatPreferences, and reads its values
 * back through [valueOf], so the window shows what the operator entered while the running
 * session still uses what is stored. "Apply/Close prefs" and "Save settings" call [apply];
 * closing the window and choosing to discard calls [discard].
 *
 * Why a list of pending writes rather than a copy of ChatPreferences: a setting is more
 * than its value. Several settings carry an effect on the running session -- the beacon
 * timer restarts, a UDP listener rebinds, the DX cluster server starts -- and that effect
 * has to stay attached to the setting it belongs to, so that applying runs exactly what an
 * immediate edit ran, once per changed setting.
 *
 * The order [apply] keeps is part of the contract, see there. The design and the list of
 * settings that deliberately bypass the draft are in
 * `docs/superpowers/specs/2026-10-16-settings-apply-drafts-design.md`.
 *
 * Not thread-safe, and it need not be: the settings window is drawn on the AWT event
 * thread, and every caller -- a control, a button, the close request -- runs there.
 * Open for the tests, which use a draft that applies every change at once to pin the
 * rules of a tab state without the draft in between.
 */
open class SettingsDraft {

    private class Change(
        val value: Any?,
        val write: () -> Unit,
        val effectKey: String?,
        val effect: (() -> Unit)?,
    )

    /*
     * Snapshot state, so a composition reading a value through valueOf is redrawn when that
     * value is staged. The tab states are plain facades; this is what makes them observable.
     */
    private val changes = mutableStateMapOf<String, Change>()

    /*
     * The order in which settings were first changed. The state map keeps no order, and
     * apply() must not run effects in hash order.
     */
    private val order = mutableListOf<String>()

    private val typedEntries = mutableListOf<() -> String?>()

    /** Whether anything waits to be applied. Compose state: a composition reading it follows. */
    val hasPendingChanges: Boolean
        get() = changes.isNotEmpty()

    /** The staged value of [key], or the stored one read from [live]. */
    @Suppress("UNCHECKED_CAST")
    fun <T> valueOf(key: String, live: () -> T): T {
        val change = changes[key] ?: return live()
        return change.value as T
    }

    /**
     * Records a change of the setting [key]. A later stage of the same key replaces the value
     * and keeps the position. Staging the stored value removes the change: a box ticked and
     * unticked again leaves nothing to apply and nothing to ask about on closing.
     *
     * Only valid values may be staged. The fields refuse an invalid entry before it gets
     * here, as they always did.
     *
     * @param live reads the stored value
     * @param write stores the value; called by [apply]
     * @param effectKey settings with the same key share one effect, which [apply] runs once.
     *        Defaults to [key].
     * @param effect what the running session needs after the write, run by [apply] after all
     *        writes
     */
    open fun <T> stage(
        key: String,
        value: T,
        live: () -> T,
        write: (T) -> Unit,
        effectKey: String? = null,
        effect: (() -> Unit)? = null,
    ) {
        if (value == live()) {
            changes.remove(key)
            order.remove(key)
            return
        }

        if (!changes.containsKey(key)) {
            order += key
        }
        changes[key] = Change(value, { write(value) }, effectKey ?: key, effect)
    }

    /**
     * Writes every change, then runs every effect, then forgets what was written.
     *
     * Writes come first, in the order the settings were first changed, so that each effect
     * sees the finished state: enabling the Win-Test listener and moving its port in one go
     * would otherwise restart the listener on the old port. Effects follow in the same order,
     * each effect key once: two restarts of one socket in quick succession can fail on the
     * port the first one has not released yet.
     *
     * Every write and every effect runs on its own: one that throws is recorded and the rest
     * still run, so a socket that cannot be rebound does not leave the beacon timer or the
     * next setting unapplied. A change whose write failed stays in the draft, and its effect
     * does not run -- it would act on a value that was never stored. A failed effect's write
     * did happen, so that change is gone from the draft like every other.
     *
     * Typed entries still held by a field are not committed here; the caller does that first
     * and decides on a refusal before anything is applied, see [commitTypedEntries].
     *
     * @return what could not be done; empty when everything went through
     */
    fun apply(): ApplyOutcome {
        val staged = order.mapNotNull { key -> changes[key]?.let { key to it } }
        val failedWrites = mutableListOf<String>()
        val failedKeys = HashSet<String>()

        staged.forEach { (key, change) ->
            try {
                change.write()
            } catch (failure: Exception) {
                failedWrites += describe(key, failure)
                failedKeys += key
            }
        }

        val failedEffects = mutableListOf<String>()
        val ran = HashSet<String?>()
        staged.forEach { (key, change) ->
            val effect = change.effect ?: return@forEach
            if (key in failedKeys || !ran.add(change.effectKey)) return@forEach
            try {
                effect()
            } catch (failure: Exception) {
                failedEffects += describe(change.effectKey ?: key, failure)
            }
        }

        staged.forEach { (key, _) ->
            if (key !in failedKeys) {
                changes.remove(key)
                order.remove(key)
            }
        }

        return ApplyOutcome(failedWrites, failedEffects)
    }

    /** Forgets every change without writing anything. */
    fun discard() {
        clear()
    }

    /**
     * Registers a field that holds typed text the draft has not seen yet: a field that commits
     * on leaving it (Form.committed), a list row being edited, or a field whose text does not
     * parse and therefore staged nothing.
     *
     * Clicking a button does not reliably take the focus from a text field, so without this an
     * entry typed into such a field and followed straight by "Apply/Close prefs" would be
     * committed only when the closing window removed the field -- into a draft nobody applies
     * any more. And a field showing invalid text in red would let Apply go ahead with the last
     * valid value, which the operator no longer sees.
     *
     * @param commit commits what the field holds and returns null, or returns the reason it
     *        cannot be accepted -- worded for the operator, naming the field
     * @return closing it unregisters the field
     */
    fun registerTypedEntry(commit: () -> String?): AutoCloseable {
        typedEntries += commit
        return AutoCloseable { typedEntries.remove(commit) }
    }

    /**
     * Commits what the registered fields hold, so it is staged, found unchanged or refused.
     *
     * @return the refusals, empty when every field was accepted
     */
    fun commitTypedEntries(): List<String> =
        typedEntries.toList().mapNotNull { it() }

    private fun describe(key: String, failure: Exception): String =
        key.substringAfterLast('.') + ": " + (failure.message ?: failure.javaClass.simpleName)

    private fun clear() {
        changes.clear()
        order.clear()
    }
}

/**
 * What [SettingsDraft.apply] could not do, each entry worded as "setting: reason".
 *
 * @param failedWrites settings that could not be stored; they stay in the draft
 * @param failedEffects effects that failed after their settings were stored
 */
class ApplyOutcome(val failedWrites: List<String>, val failedEffects: List<String>) {

    val failures: List<String>
        get() = failedWrites + failedEffects

    val isClean: Boolean
        get() = failures.isEmpty()
}

/**
 * A tab-state property whose reads go through [this] draft and whose writes are staged in it.
 *
 * The key is the owning class and the property name, so two tabs cannot collide and a
 * property needs no key spelled out beside it.
 *
 * @param read reads the stored value
 * @param write stores a value; runs on apply
 * @param effectKey see [SettingsDraft.stage]
 * @param effect see [SettingsDraft.stage]
 */
fun <T> SettingsDraft.setting(
    read: () -> T,
    effectKey: String? = null,
    effect: (() -> Unit)? = null,
    write: (T) -> Unit,
): ReadWriteProperty<Any, T> = object : ReadWriteProperty<Any, T> {

    private fun keyOf(owner: Any, property: KProperty<*>) =
        (owner::class.simpleName ?: "settings") + "." + property.name

    override fun getValue(thisRef: Any, property: KProperty<*>): T =
        valueOf(keyOf(thisRef, property), read)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        stage(keyOf(thisRef, property), value, read, write, effectKey, effect)
    }
}

/**
 * The draft of the settings window being drawn, or null outside it. Form.committed registers
 * its typed entry here; the tab screen tests draw a tab alone and need none.
 */
val LocalSettingsDraft = compositionLocalOf<SettingsDraft?> { null }
