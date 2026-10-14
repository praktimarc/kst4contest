package kst4contest.view.compose.tabs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import kst4contest.controller.OperatorProfileManagementService
import kst4contest.controller.OperatorProfileStore
import kst4contest.model.OperatorProfile
import kst4contest.model.PaletteRole
import kst4contest.view.compose.JavaFxStylesheet
import kst4contest.view.compose.OperatorProfilePaletteFiles
import kst4contest.view.compose.PaletteSource
import kst4contest.view.compose.PaletteStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kst4contest.view.compose.awaitNode
import kst4contest.view.compose.awaitGone
import kst4contest.view.compose.inDialog
import kst4contest.view.compose.inPopup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * The two tabs that belong to the operator profile, drawn: Profiles and Colours (#107).
 *
 * Profiles: only what refuses before the service writes anything, or what writes into a
 * temporary registry, is exercised. Creating, renaming and deleting a real profile reach
 * into the application directory under the operator's home ([ProfilesTabStateTest] says why
 * that is not done here either).
 *
 * Colours: a store made of lambdas and stylesheet files in a temporary directory, so nothing
 * reaches the operator's profile. The theme reading the store is `ThemeReadsPaletteStoreTest`;
 * this is the tab that writes into it.
 */
@OptIn(ExperimentalTestApi::class)
class ProfileTabsScreenTest {

    @TempDir
    lateinit var directory: Path

    // ------------------------------------------------------------------ Profiles

    private fun profilesState(activated: MutableList<OperatorProfile> = mutableListOf()) =
        ProfilesTabState(
            OperatorProfileManagementService(
                OperatorProfileStore(directory.resolve("profiles.xml").toString())
            ),
            { null },
            { activated += it },
        )

    @Test
    fun profilesShowsTheDefaultProfileAndItsActions() = runComposeUiTest {
        val state = profilesState()
        showTab { ProfilesTab(state) }

        onNodeWithText("Active profile:").assertExists()
        onNodeWithText("Profile").assertExists()
        onNodeWithText(state.profiles.single().displayName).assertExists()
        listOf("New profile...", "Duplicate...", "Rename...", "Delete...",
            "Change worked stations...", "Switch to selected profile...")
            .forEach { onNodeWithText(it).assertExists() }
    }

    /** The default profile is the installation's own; deleting it is refused, and said so. */
    @Test
    fun deletingTheDefaultProfileIsRefusedInADialog() = runComposeUiTest {
        val state = profilesState()
        showTab { ProfilesTab(state) }

        onNodeWithText("Delete...").performScrollTo().performClick()
        waitForIdle()

        // A selected profile is always asked about first; the refusal comes on confirmation.
        awaitNode(inDialog("Delete operator profile"))
        onNode(inDialog("Delete profile")).performClick()

        awaitNode(
            inDialog("The default profile uses the files of the installation itself and cannot be deleted."),
        )
        onNode(inDialog("OK")).performClick()
        awaitGone(inDialog("Operator profiles"))
        assertEquals(1, state.profiles.size)
    }

    @Test
    fun switchingAsksTheApplicationToActivateTheSelectedProfile() = runComposeUiTest {
        val activated = mutableListOf<OperatorProfile>()
        val state = profilesState(activated)
        showTab { ProfilesTab(state) }

        onNodeWithText("Switch to selected profile...").performScrollTo().performClick()
        waitForIdle()

        assertEquals(listOf(state.profiles.single()), activated)
    }

    /** The New dialog opens and, cancelled, creates nothing. */
    @Test
    fun aCancelledNewProfileDialogCreatesNothing() = runComposeUiTest {
        val state = profilesState()
        showTab { ProfilesTab(state) }

        onNodeWithText("New profile...").performScrollTo().performClick()
        waitForIdle()
        awaitNode(inDialog("New operator profile"))

        onNode(inDialog("Cancel")).performClick()
        awaitGone(inDialog("New operator profile"))
        assertEquals(1, state.profiles.size)
    }

    // ------------------------------------------------------------------ Colours

    private class Colours(directory: Path) {
        val stored = mutableMapOf(false to "", true to "")
        val store = PaletteStore(
            shippedOf = { dark ->
                JavaFxStylesheet.read(
                    if (dark) "/KST4ContestDefaultEvening.css" else "/KST4ContestDefaultDay.css"
                )
            },
            fileRolesOf = { emptyMap() },
            storedOverridesOf = { dark -> stored.getValue(dark) },
            storeOverrides = { dark, encoded -> stored[dark] = encoded },
        )
        val files = OperatorProfilePaletteFiles(
            dayFile = directory.resolve("day.css").toFile(),
            eveningFile = directory.resolve("evening.css").toFile(),
        )
        val state = ColoursTabState(store, { false }, files)
        val refusals = mutableListOf<String>()
        val outcomes = mutableListOf<String>()
    }

    private fun ComposeUiTest.show(c: Colours) {
        showTab { ColoursTab(c.state, { c.refusals += it }, { c.outcomes += it }) }
    }

    /** The label a role row carries right now: its name and where its colour came from. */
    private fun Colours.labelOf(role: PaletteRole, name: String): String {
        val provenance = when (state.roles().single { it.role == role }.source) {
            PaletteSource.SHIPPED -> "shipped"
            PaletteSource.FILE -> "from file"
            PaletteSource.CHANGED -> "changed"
        }
        return "$name ($provenance)"
    }

    @Test
    fun coloursShowsTheSixRolesAndTheWaysBack() = runComposeUiTest {
        val c = Colours(directory)
        show(c)

        onNodeWithText("Colours of this design").assertExists()
        onNodeWithText("Ways back").assertExists()
        listOf(
            PaletteRole.SURFACE to "Surface (menu strip, selected rows)",
            PaletteRole.WINDOW_SURFACE to "Window surface (the windows themselves)",
            PaletteRole.FIELD_INTERIOR to "Field interior (fields, lists, buttons)",
            PaletteRole.TEXT to "Text",
            PaletteRole.ACCENT to "Accent",
            PaletteRole.SEPARATOR to "Separator line",
        ).forEach { (role, name) ->
            fieldBeside(c.labelOf(role, name)).assertTextEquals(c.state.roles().single { it.role == role }.hex)
        }
        onNodeWithText(
            "Reset to the shipped colours (${ColourShortcuts.labelFor(ColourAction.RESET_TO_SHIPPED)})",
        ).assertExists()
    }

    @Test
    fun aTypedColourIsCommittedIntoTheStore() = runComposeUiTest {
        val c = Colours(directory)
        show(c)

        typeAndCommit(c.labelOf(PaletteRole.ACCENT, "Accent"), "#3C7A4B")

        assertEquals("#3C7A4B", c.state.roles().single { it.role == PaletteRole.ACCENT }.hex)
        assertEquals(Color(0xFF3C7A4B), c.store.resolved(false).palette.accent)
        assertTrue(c.refusals.isEmpty(), "a valid colour was refused: ${c.refusals}")
    }

    @Test
    fun aMistypedColourIsRefusedAndChangesNothing() = runComposeUiTest {
        val c = Colours(directory)
        show(c)
        val before = c.state.roles().single { it.role == PaletteRole.TEXT }.hex

        typeAndCommit(c.labelOf(PaletteRole.TEXT, "Text"), "#12")

        assertEquals(1, c.refusals.size)
        assertEquals(before, c.state.roles().single { it.role == PaletteRole.TEXT }.hex)
        fieldBeside(c.labelOf(PaletteRole.TEXT, "Text")).assertTextEquals(before)
    }

    @Test
    fun resetToShippedPutsEveryRoleBack() = runComposeUiTest {
        val c = Colours(directory)
        show(c)
        typeAndCommit(c.labelOf(PaletteRole.ACCENT, "Accent"), "#3C7A4B")

        onNodeWithText(
            "Reset to the shipped colours (${ColourShortcuts.labelFor(ColourAction.RESET_TO_SHIPPED)})",
        ).performScrollTo().performClick()
        waitForIdle()

        assertEquals(c.store.shipped(false).accent, c.store.resolved(false).palette.accent)
    }

    /** The export writes the shipped sheet into the profile, and never over an existing file. */
    @Test
    fun theExportWritesTheStylesheetOnceAndThenLeavesItAlone() = runComposeUiTest {
        val c = Colours(directory)
        show(c)
        val day: File = c.files.dayFile

        onNodeWithText("Write this design's stylesheet into my profile").performScrollTo().performClick()
        waitForIdle()

        assertTrue(day.isFile, "nothing was written")
        assertEquals(listOf("Written to ${day.path}. Edit it and restart to see the result."), c.outcomes)

        day.writeText("/* the operator's own */")
        onNodeWithText("Write this design's stylesheet into my profile").performScrollTo().performClick()
        waitForIdle()

        assertEquals("/* the operator's own */", day.readText(), "an existing stylesheet was replaced")
        assertEquals("${day.path} is already there and was left untouched.", c.outcomes.last())
    }
}
