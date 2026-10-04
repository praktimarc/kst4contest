package kst4contest.view.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.isSpecified
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.awt.ComposeWindow
import java.awt.EventQueue
import java.awt.Frame
import java.awt.Toolkit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Compose window that lives beside the JavaFX application.
 *
 * Every such window needs the same four things, and getting any of them wrong has
 * already cost a defect:
 *
 * - **It must not end the process.** `application` defaults to calling `exitProcess`
 *   when its last window closes, which would quit the client when the operator closes
 *   a settings window.
 * - **It must open at most once, and pressing the opener again must raise it.** Two
 *   windows writing the same preferences would work against each other, so a second
 *   press cannot build a second window — but it must not do nothing either. The JavaFX
 *   originals built a fresh Stage per press and were raised as a side effect of that;
 *   refusing silently leaves the operator pressing a button that appears dead while the
 *   window sits behind the main one.
 * - **It must be closable from outside.** A profile switch tears every window down and
 *   rebuilds it. `closeOwnedStages` walks JavaFX Stages and cannot reach a Compose
 *   window; when the settings window survived a switch, its "already open" flag stayed
 *   set and the rebuilt profile silently got no window at all — and that window carries
 *   the Connect button.
 * - **It must follow a design switch.** The operator can change the design from the
 *   main window's menu while this window is open.
 *
 * @param threadName names the thread in a stack dump, which is where one looks when a
 *        window will not close
 */
class ComposeWindowHost(private val threadName: String) {

    private val open = AtomicBoolean(false)
    private val darkModeState = mutableStateOf(false)
    private val visible = mutableStateOf(true)

    @Volatile
    private var windowThread: Thread? = null

    /** The AWT frame behind the Compose window, held only so a second press can raise it. */
    @Volatile
    private var frame: ComposeWindow? = null

    /** Whether the window is currently open, so a caller can tell without opening one. */
    val isOpen: Boolean
        get() = open.get()

    /** Follows a design switch made in the main window. */
    fun applyDarkMode(darkMode: Boolean) {
        darkModeState.value = darkMode
    }

    /**
     * Closes the window and waits for it to be gone.
     *
     * The wait is not politeness: a profile switch opens the replacement window
     * immediately afterwards, and it must not be refused by the old one still closing.
     *
     * How it waits depends on the calling thread, and the distinction is not cosmetic.
     * This window's `application { }` advances only as continuations that skiko's
     * SwingDispatcher posts to the AWT event thread — unconditionally, with no
     * `isDispatchNeeded` shortcut. So on that thread a plain `join` can never be
     * satisfied: nothing drains the queue the teardown is sitting in. Every Windows-menu
     * toggle and the whole of `shutdownRuntime` call this from there, and before the fix
     * the interface froze for the timeout and then returned with [isOpen] still true —
     * which made a profile switch raise the dead runtime's window and leave the rebuilt
     * profile with no settings window, the one carrying the Connect button.
     *
     * A secondary loop is the way out: it blocks this caller while continuing to dispatch
     * events, which is the same mechanism a modal dialog uses.
     */
    fun close() {
        if (!open.get()) {
            return
        }

        visible.value = false
        val thread = windowThread ?: return

        if (!EventQueue.isDispatchThread()) {
            thread.join(CLOSE_TIMEOUT_MS)
            return
        }

        val loop = Toolkit.getDefaultToolkit().systemEventQueue.createSecondaryLoop()
        Thread({
            thread.join(CLOSE_TIMEOUT_MS)
            /*
             * Posted rather than called directly, and that is what closes the documented
             * race in SecondaryLoop: an exit() that lands before enter() has begun returns
             * false and leaves enter() blocked forever. Posting it means the running loop
             * itself dispatches the exit, so it cannot arrive too early. The join's
             * timeout also bounds the whole wait, so a window that refuses to close costs
             * the timeout rather than the session.
             */
            EventQueue.invokeLater { loop.exit() }
        }, "$threadName-close").start()

        loop.enter()
    }

    /**
     * Raises the open window to the front, de-iconified if the operator had minimised it.
     *
     * Hops to the AWT event queue because the caller is the JavaFX thread. Both decisions
     * live beside this method as plain functions — [raiseSteps] for what to do and
     * [deiconifiedState] for what to write — so the only thing left here that no headless
     * test can observe is the single call to `toFront`.
     */
    private fun raise() {
        val frame = this.frame ?: return

        EventQueue.invokeLater {
            raiseSteps(frame.isDisplayable, frame.extendedState and Frame.ICONIFIED != 0)
                .forEach { step ->
                    when (step) {
                        RaiseStep.DEICONIFY ->
                            frame.extendedState = deiconifiedState(frame.extendedState)
                        RaiseStep.TO_FRONT -> frame.toFront()
                    }
                }
        }
    }

    /**
     * Opens the window, or raises it when it is already open.
     *
     * @param content receives the action that closes the window; only the window itself
     *        can end its own Compose application
     * @param onResized called with the new size whenever the operator resizes the
     *        window. The JavaFX scenes had width and height listeners writing the
     *        stored size, and the spec's acceptance criterion for this stage names
     *        window sizes explicitly.
     */
    fun show(
        title: String,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        widthDp: Float,
        heightDp: Float,
        onResized: ((Float, Float) -> Unit)? = null,
        /**
         * Raised above the other windows. The update window needs it: it opens during
         * startup, and behind the main window it goes unnoticed, which is the one thing
         * it exists not to do.
         */
        alwaysOnTop: Boolean = false,
        /**
         * The palette of the active profile. Null draws the shipped palette, which is right
         * for a window that opens before a profile exists.
         */
        paletteStore: PaletteStore? = null,
        /**
         * What a close request from outside should do — the title bar's X, and a tiling
         * window manager's close, which is the same WM_DELETE_WINDOW either way.
         *
         * Null means close this window and leave the process alone, which is right for the
         * settings, monitor and update windows. The MAIN window must pass one: closing it
         * has to run the application's quit flow. Without it Compose's default merely ends
         * this window's `application { }`, and the client lives on with the chat connected
         * and no window to see it in — a tiling window manager shows that plainly, because
         * the window disappears while the launching Gradle task keeps running.
         */
        onCloseRequest: (() -> Unit)? = null,
        /**
         * The window's content. A FrameWindowScope receiver, because that is the only scope
         * a MenuBar can be declared in — without it no window but the main one could ever
         * carry the shared menu bar.
         */
        content: @Composable FrameWindowScope.(close: () -> Unit) -> Unit,
    ) {
        darkModeState.value = darkMode

        if (!open.compareAndSet(false, true)) {
            raise()
            return
        }

        visible.value = true

        val thread = Thread({
            try {
                application(exitProcessOnExit = false) {
                    if (!visible.value) {
                        /* Nothing rendered means no window, and the application ends. */
                        return@application
                    }

                    val windowState = rememberWindowState(
                        width = widthDp.dp,
                        height = heightDp.dp,
                    )

                    /*
                     * Written back on every change. The JavaFX scenes did this through
                     * width and height listeners, and the size matters here beyond
                     * comfort: these tables are wider than the default window, so an
                     * operator who widens it once must not have to do it again.
                     */
                    if (onResized != null) {
                        LaunchedEffect(windowState) {
                            snapshotFlow { windowState.size }.collect { size ->
                                if (size.width.isSpecified && size.height.isSpecified) {
                                    onResized(size.width.value, size.height.value)
                                }
                            }
                        }
                    }

                    Window(
                        onCloseRequest = { onCloseRequest?.invoke() ?: exitApplication() },
                        state = windowState,
                        title = title,
                        alwaysOnTop = alwaysOnTop,
                        icon = applicationIcon(),
                    ) {
                        /*
                         * The frame is the only handle that can raise this window later,
                         * and it must go when the window does: a stale frame would make a
                         * second press raise a window that is no longer there.
                         */
                        DisposableEffect(window) {
                            frame = window
                            /*
                             * The only place the handle is dropped. Clearing it again in
                             * the thread's finally block looks like a belt to this brace,
                             * but that block runs after open.set(false) has already let
                             * another thread open a new window and publish its frame — so
                             * the late write would wipe a live handle and bring back the
                             * dead button this whole method exists to remove.
                             */
                            onDispose { frame = null }
                        }

                        CompositionLocalProvider(LocalPaletteStore provides paletteStore) {
                            Kst4ContestTheme(
                                darkMode = darkModeState.value,
                                baseFontSizeSp = baseFontSizeSp,
                            ) {
                                content(::exitApplication)
                            }
                        }
                    }
                }
            } finally {
                open.set(false)
                windowThread = null
            }
        }, threadName)

        windowThread = thread
        thread.start()
    }

    private companion object {
        const val CLOSE_TIMEOUT_MS = 2_000L
    }
}

/**
 * The application icon, so these windows are not the only ones in the taskbar showing
 * the default Java cup. Null when the resource is missing rather than failing: a window
 * without an icon still works.
 */
@Composable
private fun applicationIcon(): Painter? = remember {
    runCatching {
        ComposeWindowHost::class.java.getResourceAsStream(APPLICATION_ICON)?.use {
            BitmapPainter(loadImageBitmap(it))
        }
    }.getOrNull()
}

private const val APPLICATION_ICON = "/icons/kst4contest.png"

/** One thing raising a window has to do. */
internal enum class RaiseStep { DEICONIFY, TO_FRONT }

/**
 * The frame state that restores a minimised window without disturbing anything else.
 *
 * `extendedState` is a bit set, so this clears one bit rather than assigning a value.
 * Writing `Frame.NORMAL` would be an assignment of zero: a window the operator maximised
 * and then minimised holds `MAXIMIZED_BOTH or ICONIFIED` — 7 — and raising it would hand
 * it back restored-down, dropping a maximisation nobody asked to drop.
 */
internal fun deiconifiedState(current: Int): Int = current and Frame.ICONIFIED.inv()

/**
 * What raising an already-open window has to do, decided without touching AWT.
 *
 * Split out so the rule can be tested at all: the three calls it describes need a real
 * display and a real window, while the rule itself is the part that can be wrong.
 *
 * A minimised window has to be restored before it is raised — `toFront` on an iconified
 * frame leaves it iconified on every desktop this client runs on, which would be the
 * same dead button in a different disguise. A frame that is no longer displayable gets
 * nothing: the window is on its way out and raising it would fight the close.
 *
 * There is no focus step. `toFront` is the documented way to ask for a top-level window
 * to come forward, while `Component.requestFocus` on a window is a focus-cycle-root
 * request that most window managers ignore — and under X11 focus-stealing prevention has
 * the last word either way. It was in the first version of this list without evidence
 * that it ever changed an outcome, which is not a reason to pin a third step.
 */
internal fun raiseSteps(displayable: Boolean, iconified: Boolean): List<RaiseStep> = when {
    !displayable -> emptyList()
    iconified -> listOf(RaiseStep.DEICONIFY, RaiseStep.TO_FRONT)
    else -> listOf(RaiseStep.TO_FRONT)
}
