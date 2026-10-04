package kst4contest.view.compose.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kst4contest.view.compose.DataTable
import kst4contest.view.compose.Form

/**
 * The shortcuts-and-snippets tab: the two editable lists.
 *
 * The move buttons are shown here because the JavaFX tab had them for both lists —
 * the order decides the button order above the message field and which snippets get
 * Ctrl+1 through Ctrl+0.
 */
@Composable
fun ShortcutsTab(state: ShortcutsTabState, onRefused: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Form.section("Shortcut buttons above the message field") {
            DataTable(
                state = state.shortcuts,
                addButtonText = "Add shortcut",
                newEntryTemplate = ShortcutsTabState.NEW_ENTRY_TEMPLATE,
                onChanged = state::commitShortcuts,
                onRefused = onRefused,
                showMoveButtons = true,
            )
        }

        Form.section("Text snippets (the first 10 use Ctrl+1 through Ctrl+0)") {
            DataTable(
                state = state.snippets,
                addButtonText = "Add new snippet",
                newEntryTemplate = ShortcutsTabState.NEW_ENTRY_TEMPLATE,
                onChanged = state::commitSnippets,
                onRefused = onRefused,
                showMoveButtons = true,
            )
        }
    }
}
