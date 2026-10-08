package kst4contest.view.compose

import kst4contest.observe.SimpleRoster
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue

class EditableListStateTest {

    @Test
    fun `a new entry lands at the top and becomes the selection`() {
        val state = EditableListState(listOf("alt"))

        state.addAtTop("neu")

        assertEquals(listOf("neu", "alt"), state.texts)
        assertEquals(0, state.selectedIndex,
            "the JavaFX table selected row 0 and opened it for editing")
    }

    @Test
    fun `an entry edited to blank disappears`() {
        val state = EditableListState(listOf("a", "b", "c"))

        state.updateAt(1, "   ")

        assertEquals(listOf("a", "c"), state.texts,
            "clearing the text was how the operator removed an entry")
    }

    @Test
    fun `an edit keeps the other entries and their order`() {
        val state = EditableListState(listOf("a", "b", "c"))

        state.updateAt(1, "B")

        assertEquals(listOf("a", "B", "c"), state.texts)
    }

    @Test
    fun `removing the last entry clears the selection`() {
        val state = EditableListState(listOf("a"))

        state.select(0)
        state.removeAt(0)

        assertEquals(emptyList<String>(), state.texts)
        assertNull(state.selectedIndex)
    }

    @Test
    fun `an index outside the list is ignored`() {
        val state = EditableListState(listOf("a"))

        state.updateAt(5, "x")
        state.removeAt(-1)

        assertEquals(listOf("a"), state.texts,
            "a stale index from a pending edit must not corrupt the list")
    }

    @Test
    fun `every entry keeps a stable key across an insertion at the top`() {
        val state = EditableListState(listOf("a", "b"))
        val keyOfA = state.entries[0].key

        state.addAtTop("neu")

        assertEquals(keyOfA, state.entries[1].key,
            "a row must keep its key when another is inserted above it, or the "
                + "selection jumps in a LazyColumn")
    }

    @Test
    fun `duplicate texts get distinct keys`() {
        val state = EditableListState(listOf("gleich", "gleich"))

        assertNotEquals(state.entries[0].key, state.entries[1].key,
            "keying by text would collapse duplicates into one row")
    }

    @Test
    fun `committing fills the existing roster instead of replacing it`() {
        val roster = SimpleRoster<String>()
        roster.add("alt")
        val state = EditableListState(listOf("neu"))

        state.commitTo(roster)

        assertEquals(listOf("neu"), roster.snapshot())
        assertSame(roster, roster,
            "replacing the roster would orphan every listener registered on it")
    }

    @Test
    fun `a new entry can also be appended`() {
        val state = EditableListState(listOf("a"))

        state.addAtBottom("b")

        assertEquals(listOf("a", "b"), state.texts,
            "the monitoring list appended: chatcontroller.getLstNotify_...().add(call)")
        assertEquals(1, state.selectedIndex)
    }

    @Test
    fun `moving the selection down swaps it with the entry below`() {
        val state = EditableListState(listOf("a", "b", "c"))
        state.select(0)

        assertTrue(state.moveSelected(1))

        assertEquals(listOf("b", "a", "c"), state.texts)
        assertEquals(1, state.selectedIndex, "the moved row stays selected")
    }

    @Test
    fun `moving the selection up swaps it with the entry above`() {
        val state = EditableListState(listOf("a", "b", "c"))
        state.select(2)

        assertTrue(state.moveSelected(-1))

        assertEquals(listOf("a", "c", "b"), state.texts)
        assertEquals(1, state.selectedIndex)
    }

    @Test
    fun `a move past either end changes nothing`() {
        val state = EditableListState(listOf("a", "b"))

        state.select(0)
        assertFalse(state.moveSelected(-1))

        state.select(1)
        assertFalse(state.moveSelected(1))

        assertEquals(listOf("a", "b"), state.texts)
    }

    @Test
    fun `a move without a selection changes nothing`() {
        val state = EditableListState(listOf("a", "b"))

        assertFalse(state.moveSelected(1))

        assertEquals(listOf("a", "b"), state.texts)
    }

    @Test
    fun `committing an edit stores the text when no rule objects`() {
        val state = EditableListState(listOf("a", "b"))

        val outcome = state.commitAt(1, "B")

        assertEquals(CommitOutcome.Stored("B"), outcome)
        assertEquals(listOf("a", "B"), state.texts)
    }

    @Test
    fun `committing a blank text removes the entry`() {
        val state = EditableListState(listOf("a", "b"))

        val outcome = state.commitAt(1, "  ")

        assertEquals(CommitOutcome.Removed, outcome)
        assertEquals(listOf("a"), state.texts)
    }

    @Test
    fun `a rule may rewrite the committed text`() {
        val upperCase = EntryRule { candidate, _, _ ->
            if (candidate.isBlank()) CommitOutcome.Removed
            else CommitOutcome.Stored(candidate.uppercase())
        }
        val state = EditableListState(listOf("dn9apw"), upperCase)

        state.commitAt(0, "do5amf")

        assertEquals(listOf("DO5AMF"), state.texts)
    }

    @Test
    fun `a refused edit leaves the stored text alone`() {
        val noB = EntryRule { candidate, _, _ ->
            if (candidate == "b") CommitOutcome.Refused("no b here") else CommitOutcome.Stored(candidate)
        }
        val state = EditableListState(listOf("a"), noB)

        val outcome = state.commitAt(0, "b")

        assertEquals(CommitOutcome.Refused("no b here"), outcome)
        assertEquals(listOf("a"), state.texts,
            "the JavaFX table refreshed the cell back to the stored value")
    }

    @Test
    fun `a rule sees the other entries but not the row being edited`() {
        var seen: List<String>? = null
        val rule = EntryRule { candidate, _, others -> seen = others; CommitOutcome.Stored(candidate) }
        val state = EditableListState(listOf("a", "b", "c"), rule)

        state.commitAt(1, "b")

        assertEquals(listOf("a", "c"), seen,
            "the duplicate check skipped the edited row, or renaming it to itself would fail")
    }

    @Test
    fun `committing on a vanished row is ignored`() {
        val state = EditableListState(listOf("a"))

        assertEquals(CommitOutcome.Ignored, state.commitAt(7, "x"))
        assertEquals(listOf("a"), state.texts)
    }
}
