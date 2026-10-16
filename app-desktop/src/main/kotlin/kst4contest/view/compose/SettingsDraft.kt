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

    private val typedEntries = mutableListOf<() -> Unit>()

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
     * Writes every change, then runs every effect, then forgets them.
     *
     * Writes come first, in the order the settings were first changed, so that each effect
     * sees the finished state: enabling the Win-Test listener and moving its port in one go
     * would otherwise restart the listener on the old port. Effects follow in the same order,
     * each effect key once: two restarts of one socket in quick succession can fail on the
     * port the first one has not released yet.
     *
     * Typed entries still held by a field are committed first; see [registerTypedEntry].
     *
     * The draft is empty afterwards even when an effect throws: applying a half-applied draft
     * a second time would write and restart again.
     */
    fun apply() {
        commitTypedEntries()

        val staged = order.mapNotNull { changes[it] }
        try {
            staged.forEach { it.write() }

            val ran = HashSet<String?>()
            staged.forEach { change ->
                val effect = change.effect ?: return@forEach
                if (ran.add(change.effectKey)) {
                    effect()
                }
            }
        } finally {
            clear()
        }
    }

    /** Forgets every change without writing anything. */
    fun discard() {
        clear()
    }

    /**
     * Registers a field that holds typed text until the focus leaves it (Form.committed).
     *
     * Clicking a button does not take the focus from a text field, so without this an entry
     * typed into such a field and followed straight by "Apply/Close prefs" would be committed
     * only when the closing window removed the field -- into a draft nobody applies any more.
     *
     * @return closing it unregisters the field
     */
    fun registerTypedEntry(commit: () -> Unit): AutoCloseable {
        typedEntries += commit
        return AutoCloseable { typedEntries.remove(commit) }
    }

    /** Commits what the registered fields hold, so it is staged, refused or found unchanged. */
    fun commitTypedEntries() {
        typedEntries.toList().forEach { it() }
    }

    private fun clear() {
        changes.clear()
        order.clear()
    }
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
