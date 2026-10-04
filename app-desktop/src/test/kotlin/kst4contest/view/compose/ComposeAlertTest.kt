package kst4contest.view.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.EventQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The rules of the alert dialog that hold without a screen.
 *
 * The drawing is covered by the GUI acceptance run, the way `raiseSteps` and
 * `deiconifiedState` of ComposeWindowHost split the same way: the rule is the part that
 * can be wrong silently.
 *
 * Button order is in here because getting it wrong is not cosmetic. The operator reaches
 * for a position, not a word, and both confirmations this class shows give something up —
 * a running contest session, or the whole client.
 */
class ComposeAlertTest {

    @Test
    fun anAcknowledgementHasOneButtonAndItConfirms() {
        val buttons = alertButtons("OK", null, ConfirmKind.OK_DONE, BUTTON_ORDER_LINUX)

        assertEquals(listOf(AlertButton("OK", confirming = true)), buttons)
    }

    @Test
    fun closingTheWindowCountsAsNotConfirming() {
        // The JavaFX Alert returned Optional.empty when dismissed, and every caller
        // treated that as "do not proceed". The window close button must do the same.
        assertFalse(dismissalConfirms())
    }

    /*
     * The four cases below are the JavaFX ButtonBar orders, read off JavaFX 21.0.5 on this
     * machine rather than from memory:
     *   LINUX   = L_HE+UNYACBXIO_R   -> YES < CANCEL_CLOSE < OK_DONE
     *   MAC_OS  = L_HE+U+FBIX_NCYOA_R -> CANCEL_CLOSE < YES < OK_DONE
     *   WINDOWS = L_E+U+FBXI_YNOCAH_R -> YES < OK_DONE < CANCEL_CLOSE
     * The quit dialog used ButtonType.YES, the profile switch used OK_DONE, so the two
     * disagree on Linux — which is why a single "confirming button first" rule cannot be
     * right for both.
     */

    @Test
    fun theQuitDialogKeepsYesFirstWhereJavaFxPutItFirst() {
        val linux = alertButtons("Yes", "Cancel", ConfirmKind.YES, BUTTON_ORDER_LINUX)
        val windows = alertButtons("Yes", "Cancel", ConfirmKind.YES, BUTTON_ORDER_WINDOWS)

        assertEquals(listOf("Yes", "Cancel"), linux.map { it.text })
        assertEquals(listOf("Yes", "Cancel"), windows.map { it.text })
    }

    @Test
    fun theQuitDialogPutsCancelFirstOnMacOsWhereJavaFxDid() {
        val mac = alertButtons("Yes", "Cancel", ConfirmKind.YES, BUTTON_ORDER_MAC_OS)

        assertEquals(listOf("Cancel", "Yes"), mac.map { it.text })
    }

    @Test
    fun theProfileSwitchPutsCancelFirstOnLinuxAndMacOs() {
        val linux = alertButtons("Switch profile", "Cancel", ConfirmKind.OK_DONE, BUTTON_ORDER_LINUX)
        val mac = alertButtons("Switch profile", "Cancel", ConfirmKind.OK_DONE, BUTTON_ORDER_MAC_OS)

        // The regression this pins: a plain "confirming first" rule put the button that
        // gives up a live contest session under the cursor's resting place.
        assertEquals(listOf("Cancel", "Switch profile"), linux.map { it.text })
        assertEquals(listOf("Cancel", "Switch profile"), mac.map { it.text })
    }

    @Test
    fun theProfileSwitchKeepsItsConfirmFirstOnWindows() {
        val windows = alertButtons("Switch profile", "Cancel", ConfirmKind.OK_DONE, BUTTON_ORDER_WINDOWS)

        assertEquals(listOf("Switch profile", "Cancel"), windows.map { it.text })
    }

    @Test
    fun confirmingStaysMarkedAsConfirmingWhateverTheOrder() {
        val mac = alertButtons("Switch profile", "Cancel", ConfirmKind.OK_DONE, BUTTON_ORDER_MAC_OS)

        // Reordering must never move the meaning onto the other button.
        assertEquals(
            mapOf("Cancel" to false, "Switch profile" to true),
            mac.associate { it.text to it.confirming },
        )
    }

    @Test
    fun theOrderStringIsChosenByOperatingSystem() {
        assertEquals(BUTTON_ORDER_MAC_OS, buttonOrderFor("Mac OS X"))
        assertEquals(BUTTON_ORDER_WINDOWS, buttonOrderFor("Windows 11"))
        assertEquals(BUTTON_ORDER_LINUX, buttonOrderFor("Linux"))
        // Anything else follows Linux, the way JavaFX treated unknown platforms.
        assertEquals(BUTTON_ORDER_LINUX, buttonOrderFor("FreeBSD"))
    }

    @Test
    fun enterConfirmsAndEscapeCancels() {
        val buttons = alertButtons("Switch profile", "Cancel", ConfirmKind.OK_DONE, BUTTON_ORDER_LINUX)

        assertEquals("Switch profile", buttonForEnter(buttons)?.text)
        assertEquals("Cancel", buttonForEscape(buttons)?.text)
    }

    /*
     * The two below pin the rule that cost three failed application starts: a ComposeDialog
     * is a Swing component wrapping a Skia layer, so building it anywhere but the AWT event
     * thread makes skiko fail to create its GL context ("Can't wrap nullptr") and the dialog
     * never paints. Since a modal wait on an unpaintable dialog cannot end, the client hung
     * at startup. No unit test saw it; only running the application did.
     */

    @Test
    fun aDialogAskedForFromAWorkerThreadIsBuiltOnTheEventThread() {
        val onEventThread = AtomicBoolean(false)

        // The startup path: main() asks for the profile picker and the startup warning.
        openOnEventThread { onEventThread.set(EventQueue.isDispatchThread()) }

        assertTrue(
            onEventThread.get(),
            "a dialog built off the event thread cannot create its graphics context",
        )
    }

    @Test
    fun aDialogAskedForOnTheEventThreadIsBuiltInline() {
        val ranInline = AtomicBoolean(false)

        // The menu path: already on the event thread, where invokeAndWait would throw.
        EventQueue.invokeAndWait {
            openOnEventThread { ranInline.set(EventQueue.isDispatchThread()) }
        }

        assertTrue(ranInline.get(), "the dialog must still be built on the event thread")
    }

    @Test
    fun escapeOnAnAcknowledgementPressesItsOnlyButton() {
        // No cancel to fall back on: dismissing an acknowledgement is acknowledging it,
        // which is what the JavaFX Alert's ESC did with a lone OK button.
        val buttons = alertButtons("OK", null, ConfirmKind.OK_DONE, BUTTON_ORDER_LINUX)

        assertEquals("OK", buttonForEnter(buttons)?.text)
        assertEquals("OK", buttonForEscape(buttons)?.text)
    }
}
