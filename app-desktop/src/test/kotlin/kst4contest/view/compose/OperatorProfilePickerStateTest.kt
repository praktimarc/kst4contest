package kst4contest.view.compose

import kst4contest.model.OperatorProfile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class OperatorProfilePickerStateTest {

    private fun profile(id: String, name: String = id, root: Boolean = false, shared: Boolean = false) =
        OperatorProfile(id, name, root, shared)

    @Test
    fun `the preselected profile is selected when it is in the list`() {
        val state = OperatorProfilePickerState(
            listOf(profile("root"), profile("contest")), preselectedProfileId = "contest")

        assertEquals("contest", state.selected?.profileId)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `an unknown preselection falls back to the first profile`() {
        val state = OperatorProfilePickerState(
            listOf(profile("root"), profile("contest")), preselectedProfileId = "gone")

        assertEquals("root", state.selected?.profileId,
            "a profile that disappeared must not leave the dialog unusable")
    }

    @Test
    fun `no preselection falls back to the first profile`() {
        val state = OperatorProfilePickerState(listOf(profile("root")), preselectedProfileId = null)

        assertEquals("root", state.selected?.profileId)
    }

    @Test
    fun `an empty list cannot be confirmed`() {
        val state = OperatorProfilePickerState(emptyList(), preselectedProfileId = null)

        assertNull(state.selected)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `selecting changes the selection`() {
        val state = OperatorProfilePickerState(
            listOf(profile("root"), profile("contest")), preselectedProfileId = "root")

        state.select(profile("contest"))

        assertEquals("contest", state.selected?.profileId)
    }

    @Test
    fun `the worked database label matches the JavaFX dialog`() {
        // The dialog this replaces showed exactly these two strings, and the rule
        // behind them: the root profile and a sharing profile use the shared file.
        assertEquals("shared station worked database",
            workedDatabaseDescription(profile("root", root = true)))
        assertEquals("shared station worked database",
            workedDatabaseDescription(profile("sharer", shared = true)))
        assertEquals("own worked database",
            workedDatabaseDescription(profile("owner")))
    }
}
