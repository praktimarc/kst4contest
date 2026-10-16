package kst4contest.view.compose

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup

/*
 * Waiting for what a click opens on top of the window (#107).
 *
 * In Compose Multiplatform 1.8.2 a Material3 AlertDialog goes through the skiko Dialog, and a
 * DropdownMenu through the skiko Popup; both compose their content into a ComposeSceneLayer of
 * the same scene (Dialog_skikoKt calls rememberComposeSceneLayer, checked in the bytecode of
 * ui-desktop 1.8.2). The layer's content is set from the composition that opened it, so it can
 * take one more frame than the click itself. These helpers wait for the node instead of
 * assuming that frame has already happened, and they look only inside the overlay, so a text
 * that also stands in the window below can never be picked by accident.
 */

private const val OVERLAY_TIMEOUT_MILLIS = 5_000L

/** Waits until a node matching [matcher] is in the tree, then returns it. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.awaitNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
    waitUntil(timeoutMillis = OVERLAY_TIMEOUT_MILLIS) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    return onNode(matcher)
}

/** Waits until no node matches [matcher] any more. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.awaitGone(matcher: SemanticsMatcher) {
    waitUntil(timeoutMillis = OVERLAY_TIMEOUT_MILLIS) { onAllNodes(matcher).fetchSemanticsNodes().isEmpty() }
}

/** A text inside an open dialog. */
internal fun inDialog(text: String): SemanticsMatcher =
    hasText(text, substring = false) and hasAnyAncestor(isDialog())

/** A text inside an open popup, which is what a dropdown menu is. */
internal fun inPopup(text: String): SemanticsMatcher =
    hasText(text, substring = false) and hasAnyAncestor(isPopup())
