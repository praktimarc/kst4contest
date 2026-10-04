package kst4contest.view.compose

import androidx.compose.runtime.Composable
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
 * - **It must open at most once.** Two windows writing the same preferences would work
 *   against each other.
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
     */
    fun close() {
        if (!open.get()) {
            return
        }

        visible.value = false
        windowThread?.join(CLOSE_TIMEOUT_MS)
    }

    /**
     * Opens the window, or does nothing when it is already open.
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
         * The window's content. A FrameWindowScope receiver, because that is the only scope
         * a MenuBar can be declared in — without it no window but the main one could ever
         * carry the shared menu bar.
         */
        content: @Composable FrameWindowScope.(close: () -> Unit) -> Unit,
    ) {
        darkModeState.value = darkMode

        if (!open.compareAndSet(false, true)) {
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
                        onCloseRequest = ::exitApplication,
                        state = windowState,
                        title = title,
                        alwaysOnTop = alwaysOnTop,
                        icon = applicationIcon(),
                    ) {
                        Kst4ContestTheme(
                            darkMode = darkModeState.value,
                            baseFontSizeSp = baseFontSizeSp,
                        ) {
                            content(::exitApplication)
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
