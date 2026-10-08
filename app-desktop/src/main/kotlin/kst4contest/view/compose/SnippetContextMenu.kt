package kst4contest.view.compose

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable

/**
 * The right-click menu of the station list and the message tables: one entry per
 * configured snippet, appended to the message field when chosen.
 *
 * The same roster is reachable three ways and all three behave differently — see
 * [ChatInputState.appendSnippet]. This is the one that appends the snippet alone.
 *
 * @param snippets the configured snippets, in the operator's own order
 */
@Composable
fun SnippetContextMenu(
    snippets: List<String>,
    state: ChatInputState,
    content: @Composable () -> Unit,
) {
    ContextMenuArea(
        items = {
            snippets.map { snippet ->
                ContextMenuItem(snippet) { state.appendSnippet(snippet) }
            }
        },
        content = content,
    )
}
