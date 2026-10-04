package kst4contest.view.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import java.awt.Dialog
import java.awt.Dimension
import java.awt.EventQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.WindowConstants
import kst4contest.view.ExternalDocuments

/** One button of an alert, and whether pressing it means "go ahead". */
internal data class AlertButton(val text: String, val confirming: Boolean)

/**
 * Which JavaFX button type the confirming button stood for.
 *
 * Not a detail: JavaFX ordered `YES` and `OK_DONE` differently, and the two dialogs this
 * class replaces used one each. Flattening them put the button that gives up a running
 * contest session where the cancel button used to be.
 */
enum class ConfirmKind(internal val code: Char) { YES('Y'), OK_DONE('O') }

/*
 * The JavaFX ButtonBar orders, read off JavaFX 21.0.5 on a real machine rather than from
 * memory. Each letter is a ButtonData type code; only three matter here: Y = YES,
 * C = CANCEL_CLOSE, O = OK_DONE. Keeping the whole strings rather than a hand-made table
 * means the rule below is the same rule JavaFX applied, and can be checked against it.
 */
internal const val BUTTON_ORDER_LINUX = "L_HE+UNYACBXIO_R"
internal const val BUTTON_ORDER_MAC_OS = "L_HE+U+FBIX_NCYOA_R"
internal const val BUTTON_ORDER_WINDOWS = "L_E+U+FBXI_YNOCAH_R"

private const val CANCEL_CODE = 'C'

/** The order string for an operating system name, as JavaFX chose one. */
internal fun buttonOrderFor(osName: String): String = when {
    osName.startsWith("mac", ignoreCase = true) -> BUTTON_ORDER_MAC_OS
    osName.startsWith("darwin", ignoreCase = true) -> BUTTON_ORDER_MAC_OS
    osName.startsWith("windows", ignoreCase = true) -> BUTTON_ORDER_WINDOWS
    else -> BUTTON_ORDER_LINUX
}

/**
 * The button row of an alert, in the order JavaFX would have shown it.
 *
 * The operator reaches for a position, not a word. Both confirmations this class shows
 * give something up — the running ON4KST session, or the whole client — so a row in the
 * wrong order is a wrong action, not a cosmetic complaint.
 */
internal fun alertButtons(
    confirmText: String,
    cancelText: String?,
    confirmKind: ConfirmKind,
    buttonOrder: String,
): List<AlertButton> {

    val confirm = AlertButton(confirmText, confirming = true)

    if (cancelText == null) {
        return listOf(confirm)
    }

    val cancel = AlertButton(cancelText, confirming = false)

    return if (buttonOrder.indexOf(confirmKind.code) < buttonOrder.indexOf(CANCEL_CODE)) {
        listOf(confirm, cancel)
    } else {
        listOf(cancel, confirm)
    }
}

/**
 * The button Enter presses: always the confirming one, as the JavaFX Alert's default
 * button was.
 */
internal fun buttonForEnter(buttons: List<AlertButton>): AlertButton? =
    buttons.firstOrNull { it.confirming }

/**
 * The button ESC presses.
 *
 * The cancelling one when there is one. On an acknowledgement there is not, and dismissing
 * an acknowledgement is acknowledging it — which is what ESC did to a JavaFX Alert holding
 * a lone OK button.
 */
internal fun buttonForEscape(buttons: List<AlertButton>): AlertButton? =
    buttons.firstOrNull { !it.confirming } ?: buttons.firstOrNull()

/**
 * Builds and shows a modal dialog on the AWT event thread, blocking the caller.
 *
 * Both halves matter and neither is optional.
 *
 * A `ComposeDialog` is a heavyweight Swing component wrapping a Skia layer, so it has to
 * be created and realised on the event thread. Doing it on another thread is not merely
 * against the Swing rule: skiko then tries to create its OpenGL context off that thread
 * and fails outright — `Failed to create Skia OpenGL context! Can't wrap nullptr`,
 * followed by `RenderException: Cannot init graphic context`. The dialog appears as an
 * empty frame that never paints, and because nothing can be clicked, a modal wait on it
 * never ends. That was reproducible three runs out of three from `main`.
 *
 * `invokeAndWait` is what keeps the blocking contract while moving the work: it returns
 * only once [open] returns, and [open] returns only once the modal dialog closes. So the
 * caller still blocks exactly as `Stage.showAndWait()` made it block, and it also gives
 * the happens-before edge that makes the answer written on the event thread visible here.
 */
internal fun openOnEventThread(open: () -> Unit) {
    if (EventQueue.isDispatchThread()) {
        open()
    } else {
        EventQueue.invokeAndWait(open)
    }
}

/**
 * What closing the window without pressing a button means.
 *
 * The JavaFX Alert returned an empty Optional and every caller read that as "do not
 * proceed"; `closeWindowEvent` would otherwise quit the client when the operator
 * dismisses the question rather than answering it.
 */
internal fun dismissalConfirms(): Boolean = false

/**
 * The replacement for the JavaFX `Alert`.
 *
 * A modal [ComposeDialog] rather than a Compose `application { }` window on its own
 * thread: a modal AWT dialog runs a nested event pump, so it blocks its caller *and*
 * keeps drawing. Measured before this was written — from the event thread and from a
 * worker thread alike, the call blocks for the dialog's lifetime and the content
 * composes. The thread-plus-latch pattern [OperatorProfilePickerWindow] uses cannot do
 * that: once the dispatcher hands menu actions to the event thread, blocking it with a
 * latch would deadlock against the window trying to draw on that very thread.
 */
object ComposeAlert {

    /** The font size the JavaFX dialogs had; the settings windows use the same value. */
    private const val DIALOG_FONT_SIZE_SP = 12f

    private val DIALOG_SIZE = Dimension(520, 260)

    /** Shows the alert and returns at once. For notices nothing waits on. */
    @JvmStatic
    @JvmOverloads
    fun show(title: String, header: String?, body: String, darkMode: Boolean = false) {
        EventQueue.invokeLater {
            open(
                title = title,
                header = header,
                body = body,
                confirmText = "OK",
                cancelText = null,
                confirmKind = ConfirmKind.OK_DONE,
                darkMode = darkMode,
                modal = false,
            )
        }
    }

    /** Shows the alert and blocks until the operator acknowledges it. */
    @JvmStatic
    @JvmOverloads
    fun acknowledge(title: String, header: String?, body: String, darkMode: Boolean = false) {
        open(
            title = title,
            header = header,
            body = body,
            confirmText = "OK",
            cancelText = null,
            confirmKind = ConfirmKind.OK_DONE,
            darkMode = darkMode,
            modal = true,
        )
    }

    /**
     * Asks a yes/no question and blocks until it is answered.
     *
     * @return true only when the operator pressed the confirming button; dismissing the
     *         window counts as a no, the way an empty Optional did.
     */
    @JvmStatic
    @JvmOverloads
    fun confirm(
        title: String,
        header: String?,
        body: String,
        confirmText: String,
        cancelText: String,
        darkMode: Boolean = false,
        /**
         * Which JavaFX button type the confirming button stood for. It decides the button
         * order per platform, so it is the caller's to state rather than a default to
         * forget: the quit dialog used YES, the profile switch used OK_DONE, and on Linux
         * those two order the row differently.
         */
        confirmKind: ConfirmKind = ConfirmKind.OK_DONE,
    ): Boolean = open(
        title = title,
        header = header,
        body = body,
        confirmText = confirmText,
        cancelText = cancelText,
        confirmKind = confirmKind,
        darkMode = darkMode,
        modal = true,
    )

    /**
     * Shows a notice with one clickable line below the text, and returns at once.
     *
     * Only `onSimpleLogFileCreated` needs this; the JavaFX version put a Hyperlink into
     * the dialog pane. Kept as its own entry point rather than a nullable parameter on
     * [show], so the ordinary notice stays a three-argument call.
     */
    /*
     * The key-handling overload of ComposeDialog.setContent is marked experimental. Opted
     * into the way the rest of this package already opts into Compose's experimental
     * surface; if it changes, the compiler says so and the fallback is an AWT key listener.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @JvmStatic
    @JvmOverloads
    fun showWithLink(
        title: String,
        header: String?,
        body: String,
        linkText: String,
        linkTarget: String,
        darkMode: Boolean = false,
    ) {
        EventQueue.invokeLater {
            val dialog = ComposeDialog(owner = null, modalityType = Dialog.ModalityType.MODELESS)
            dialog.title = title
            dialog.size = DIALOG_SIZE
            dialog.setLocationRelativeTo(null)
            /* Disposed and not hidden, for the reason spelled out in open(). */
            dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            dialog.isAlwaysOnTop = true
            dialog.setContent(
                onKeyEvent = { event ->
                    if (event.type == KeyEventType.KeyDown
                        && (event.key == Key.Enter || event.key == Key.NumPadEnter
                                || event.key == Key.Escape)) {
                        dialog.dispose()
                        true
                    } else {
                        false
                    }
                },
            ) {
                Kst4ContestTheme(darkMode = darkMode, baseFontSizeSp = DIALOG_FONT_SIZE_SP) {
                    AlertContent(
                        header = header,
                        body = body,
                        buttons = alertButtons("OK", null, ConfirmKind.OK_DONE, BUTTON_ORDER_LINUX),
                        onPressed = { dialog.dispose() },
                        link = linkText to linkTarget,
                    )
                }
            }
            dialog.isVisible = true
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun open(
        title: String,
        header: String?,
        body: String,
        confirmText: String,
        cancelText: String?,
        confirmKind: ConfirmKind,
        darkMode: Boolean,
        modal: Boolean,
    ): Boolean {
        /*
         * Atomic rather than a captured var. Every confirm() caller happens to press and
         * read on the same thread today, so a plain var would work by an unwritten AWT
         * detail; this makes it correct on purpose, and the cost is nothing.
         */
        val answer = AtomicBoolean(dismissalConfirms())

        if (!EventQueue.isDispatchThread() && modal) {
            /*
             * A modal alert asked for from a non-event thread — startup warnings and the
             * failure notice in main do this. The dialog must still be built on the event
             * thread, so the whole open is handed over and this call blocks until it
             * returns, which is the behaviour the caller expects.
             */
            openOnEventThread {
                answer.set(open(title, header, body, confirmText, cancelText, confirmKind, darkMode, true))
            }
            return answer.get()
        }

        val dialog = ComposeDialog(
            owner = null,
            modalityType =
                if (modal) Dialog.ModalityType.APPLICATION_MODAL else Dialog.ModalityType.MODELESS,
        )
        dialog.title = title
        dialog.size = DIALOG_SIZE
        dialog.setLocationRelativeTo(null)
        /*
         * Disposed, not hidden. The JDialog default is HIDE_ON_CLOSE, which would leave the
         * window and its Skia layer alive behind the operator's back — one per dismissal,
         * and a reconnect storm shows this dialog repeatedly.
         */
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        /*
         * Above everything else on purpose. These dialogs are asked for from Compose
         * windows, which are not their owner; without this the question opens behind the
         * window that asked it and the operator sees nothing happen at all. The JavaFX
         * profile-switch confirmation needed the same and said so.
         */
        dialog.isAlwaysOnTop = true

        val buttons = alertButtons(
            confirmText,
            cancelText,
            confirmKind,
            buttonOrderFor(System.getProperty("os.name") ?: ""),
        )

        val press: (AlertButton) -> Unit = { pressed ->
            answer.set(pressed.confirming)
            dialog.dispose()
        }

        dialog.setContent(
            /*
             * Enter and ESC, which the JavaFX Alert bound for free. Without them the quit
             * question ignored both keys, and a dialog one cannot dismiss from the keyboard
             * is an obstacle in a contest.
             */
            onKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            buttonForEnter(buttons)?.let(press)
                            true
                        }
                        Key.Escape -> {
                            buttonForEscape(buttons)?.let(press)
                            true
                        }
                        else -> false
                    }
                }
            },
        ) {
            Kst4ContestTheme(darkMode = darkMode, baseFontSizeSp = DIALOG_FONT_SIZE_SP) {
                AlertContent(
                    header = header,
                    body = body,
                    buttons = buttons,
                    onPressed = press,
                )
            }
        }

        dialog.isVisible = true
        return answer.get()
    }
}

@Composable
private fun AlertContent(
    header: String?,
    body: String,
    buttons: List<AlertButton>,
    onPressed: (AlertButton) -> Unit,
    link: Pair<String, String>? = null,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (header != null) {
                Text(header, style = MaterialTheme.typography.titleMedium)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                /* Scrolls rather than clips: onSimpleLogFileCreated shows five paragraphs. */
                Text(body, style = MaterialTheme.typography.bodyMedium)

                if (link != null) {
                    Text(
                        link.first,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { ExternalDocuments.open(link.second) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP)) {
                buttons.forEach { button ->
                    Form.button(button.text) { onPressed(button) }
                }
            }
        }
    }
}
