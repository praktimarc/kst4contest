package kst4contest.view.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kst4contest.observe.SimpleRoster

/**
 * One row of an [EditableListState].
 *
 * The key is what a LazyColumn is keyed by, and it must survive an insertion
 * above the row: keying by list index makes every row below an insertion change
 * identity, which moves the selection. Keying by text would collapse duplicates,
 * and the shortcut and snippet lists can hold the same text twice.
 */
data class ListEntry(val key: Long, val text: String)

/**
 * What committing an edit did to the list.
 *
 * A commit is the counterpart of the JavaFX `setOnEditCommit` handler, which ran
 * when the operator left the cell — not on every keystroke. That distinction is
 * what makes a rule such as "fold a callsign to its base call" usable: applying
 * it per keystroke would fight the typist.
 */
sealed interface CommitOutcome {

    /** The entry now holds this text, which a rule may have rewritten. */
    data class Stored(val text: String) : CommitOutcome

    /** The entry is gone: clearing the text was how the operator removed one. */
    data object Removed : CommitOutcome

    /**
     * The edit was refused and the stored text stands. The message is the one the
     * JavaFX table showed in an alert before calling `refresh()` on itself.
     */
    data class Refused(val message: String) : CommitOutcome

    /** The row no longer exists: a pending edit can outlive its row. */
    data object Ignored : CommitOutcome
}

/**
 * Decides what a committed edit becomes. The default accepts the text as typed
 * and treats a blank one as a removal; the monitored-callsign list replaces it
 * with the normalize-and-reject-duplicates rule the JavaFX table had.
 */
fun interface EntryRule {

    /**
     * @param candidate text as the operator typed it
     * @param index row being committed
     * @param others texts of the other rows in order, the edited row excluded, so a
     *               duplicate check does not compare a row against itself
     */
    fun decide(candidate: String, index: Int, others: List<String>): CommitOutcome

    companion object {

        /** Accept as typed; a blank text removes the entry. */
        val asTyped: EntryRule = EntryRule { candidate, _, _ ->
            if (candidate.isBlank()) CommitOutcome.Removed else CommitOutcome.Stored(candidate)
        }
    }
}

/**
 * Editing state of a single-column string list, free of Compose so the rules are
 * testable without a toolkit.
 *
 * Reproduces what the JavaFX tables in the settings window did: a new entry goes
 * to the top and becomes the selection, and an entry edited to blank disappears —
 * that was how the operator removed one
 * (Kst4ContestApplication, ShortCol.setOnEditCommit).
 */
class EditableListState(
    initial: List<String>,
    private val rule: EntryRule = EntryRule.asTyped,
) {

    private var nextKey: Long = 0

    /*
     * Compose state, so a row redraws when the selection moves off it or its text
     * changes. Plain collections left the user interface guessing: select() is called
     * on a focus change without any other notification, and the row that lost the
     * selection kept its accent border.
     */
    private val items: SnapshotStateList<ListEntry> =
        mutableStateListOf<ListEntry>().apply {
            initial.forEach { add(ListEntry(nextKey++, it)) }
        }

    val entries: List<ListEntry>
        get() = items.toList()

    val texts: List<String>
        get() = items.map { it.text }

    var selectedIndex: Int? by mutableStateOf(null)
        private set

    fun select(index: Int?) {
        selectedIndex = index?.takeIf { it in items.indices }
    }

    fun addAtTop(entry: String) {
        items.add(0, ListEntry(nextKey++, entry))
        selectedIndex = 0
    }

    /**
     * Appends an entry. The monitoring list grew this way: its Add button asked for
     * a callsign in a dialog and called `add` on the roster, which appends.
     */
    fun addAtBottom(entry: String) {
        items.add(ListEntry(nextKey++, entry))
        selectedIndex = items.lastIndex
    }

    /** An index outside the list is ignored: a pending edit can outlive its row. */
    fun updateAt(index: Int, text: String) {
        if (index !in items.indices) {
            return
        }
        if (text.isBlank()) {
            removeAt(index)
            return
        }
        items[index] = items[index].copy(text = text)
    }

    /**
     * Commits an edit through the list's [EntryRule]. This is what the user interface
     * calls when the operator leaves a row or presses Enter, mirroring
     * `setOnEditCommit`; [updateAt] stays the unguarded path.
     */
    fun commitAt(index: Int, candidate: String): CommitOutcome {
        if (index !in items.indices) {
            return CommitOutcome.Ignored
        }

        val others = items.filterIndexed { position, _ -> position != index }.map { it.text }

        return when (val verdict = rule.decide(candidate, index, others)) {
            is CommitOutcome.Stored -> {
                items[index] = items[index].copy(text = verdict.text)
                verdict
            }

            CommitOutcome.Removed -> {
                removeAt(index)
                verdict
            }

            else -> verdict
        }
    }

    fun removeAt(index: Int) {
        if (index !in items.indices) {
            return
        }
        items.removeAt(index)
        selectedIndex = selectedIndex?.let { current ->
            if (current >= items.size) items.indices.lastOrNull() else current
        }
    }

    /**
     * Moves the selected entry by [offset] positions and keeps it selected, the way
     * `moveSelectedTableEntry` did. Returns false when there is no selection or the
     * target position is outside the list, so the caller can skip its follow-up work.
     */
    fun moveSelected(offset: Int): Boolean {
        val current = selectedIndex ?: return false
        val target = current + offset

        if (current !in items.indices || target !in items.indices) {
            return false
        }

        items.add(target, items.removeAt(current))
        selectedIndex = target
        return true
    }

    /**
     * Fills the given roster. Deliberately setAll and not a field swap: replacing
     * the roster would orphan every listener registered on it, which is the rule
     * the ChatPreferences setters have followed since Etappe 2.
     */
    fun commitTo(roster: SimpleRoster<String>) {
        roster.setAll(texts)
    }
}

/**
 * An editable single-column list, the Compose counterpart of the three
 * TableView<String> tables in the settings window.
 *
 * Column widths, sorting and row styling are deliberately absent: no consumer in
 * the settings window needs them. They belong with the main-window tables, where
 * TableLayoutManager persists widths against stable column ids.
 *
 * Editing is committed when the operator leaves a row or presses Enter, not on
 * every keystroke. That is what the JavaFX `TextFieldTableCell` did: the typed
 * text lived in the cell editor until it was committed, which is why a rule could
 * reject it and put the stored value back.
 *
 * @param onChanged called after every committed change — add, edit, removal, move.
 *        The lists write through, so this is where the roster is filled.
 * @param onRefused shown to the operator when a rule refuses an edit; the JavaFX
 *        tables opened an alert here.
 * @param onAdd replaces the default "insert the template at the top" behaviour.
 *        The monitoring list needs it: its Add button asked for a callsign first.
 * @param listHeight the list is a bounded, scrolling area like the TableView it
 *        replaces. It also keeps a LazyColumn out of an unbounded parent scroll,
 *        which Compose rejects.
 */
@Composable
fun DataTable(
    state: EditableListState,
    addButtonText: String,
    newEntryTemplate: String,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    onRefused: (String) -> Unit = {},
    onAdd: (() -> Unit)? = null,
    showMoveButtons: Boolean = false,
    listHeight: Dp = 220.dp,
) {
    var revision by remember { mutableStateOf(0) }
    val focusOnNewRow = remember { FocusRequester() }

    fun changed() {
        revision++
        onChanged()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        @Suppress("UNUSED_EXPRESSION") revision // read so edits recompose the list

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(listHeight)
                .padding(vertical = 4.dp),
        ) {
            itemsIndexed(state.entries, key = { _, entry -> entry.key }) { index, entry ->
                /*
                 * Keyed on the revision too, so a refused edit is visibly put back:
                 * the stored text did not change, and without the revision the draft
                 * would keep the text the rule just rejected.
                 */
                var draft by remember(entry.key, entry.text, revision) { mutableStateOf(entry.text) }
                var hadFocus by remember(entry.key) { mutableStateOf(false) }

                fun commit() {
                    when (val outcome = state.commitAt(index, draft)) {
                        is CommitOutcome.Refused -> {
                            changed()
                            onRefused(outcome.message)
                        }

                        CommitOutcome.Ignored -> Unit

                        else -> changed()
                    }
                }

                val isSelected = state.selectedIndex == index

                /*
                 * The selected row is marked on the field's own outline rather than by a
                 * second frame around it: every row here already is a bordered text
                 * field, and a frame inside a frame reads as noise instead of as a
                 * selection. The tinted background carries it the rest of the way.
                 */
                CompactTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else null,
                    borderThickness = if (isSelected) 2.dp else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            }
                        )
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                state.select(index)
                            } else if (hadFocus) {
                                commit()
                            }
                            hadFocus = focusState.isFocused
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                commit()
                                true
                            } else {
                                false
                            }
                        }
                        .let { if (index == 0) it.focusRequester(focusOnNewRow) else it },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Density.BUTTON_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Form.button(addButtonText) {
                if (onAdd != null) {
                    onAdd()
                    revision++
                    return@button
                }

                state.addAtTop(newEntryTemplate)
                changed()
                /*
                 * Focus is requested explicitly. The JavaFX table opened the new row
                 * for editing; hoping Compose does the same would repeat the focus
                 * defects of Etappe 2, where two of three bugs were focus bugs.
                 */
                runCatching { focusOnNewRow.requestFocus() }
            }

            if (showMoveButtons) {
                Form.button("Move selected down") { if (state.moveSelected(1)) changed() }
                Form.button("Move selected up") { if (state.moveSelected(-1)) changed() }
            }

            Text(
                "Clear the text of an entry to remove it.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
