package kst4contest.view.compose.tabs

import kst4contest.controller.OperatorProfileManagementService
import kst4contest.controller.OperatorProfileStore
import kst4contest.model.OperatorProfile
import kst4contest.model.OperatorProfileSelection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Rules of the profiles tab.
 *
 * Creating and deleting profiles is deliberately not exercised here: both reach into
 * the real application directory under the operator's home, and a test that writes
 * there could destroy a profile in use. Every rule below refuses before the service
 * is called, which is exactly the part worth pinning down.
 */
class ProfilesTabStateTest {

    @TempDir
    lateinit var registryDirectory: Path

    private fun service() = OperatorProfileManagementService(
        OperatorProfileStore(registryDirectory.resolve("profiles.xml").toString())
    )

    private fun state(
        active: OperatorProfileSelection? = null,
        onActivation: (OperatorProfile) -> Unit = { },
    ) = ProfilesTabState(service(), { active }, onActivation)

    @Test
    fun `an absent registry still offers the default profile`() {
        val state = state()

        assertEquals(1, state.profiles.size)
        assertTrue(state.profiles.first().isRootProfile)
        assertEquals(state.profiles.first(), state.selected,
            "the JavaFX table selected the first row when nothing was selected yet")
    }

    @Test
    fun `the default profile cannot be deleted`() {
        val state = state()

        val notice = state.deleteSelected()

        assertTrue(notice is ProfileNotice.Error)
        assertEquals(
            "The default profile uses the files of the installation itself and cannot be deleted.",
            notice!!.message,
        )
        assertEquals(1, state.profiles.size, "nothing was removed")
    }

    @Test
    fun `the default profile always uses the common worked stations`() {
        val state = state()

        val notice = state.changeWorkedDataModeOfSelected(false)

        assertTrue(notice is ProfileNotice.Error,
            "that database is the installation's own, so the choice does not exist")
        assertFalse(state.canChangeWorkedDataMode(state.profiles.first()))
    }

    @Test
    fun `the profile in use cannot be deleted`() {
        val plainService = service()
        val profile = plainService.listProfiles().first()
        val state = ProfilesTabState(
            plainService,
            { OperatorProfileSelection(profile, "preferences.xml", "worked.db", false) },
            { },
        )

        val notice = state.deleteSelected()

        assertTrue(notice is ProfileNotice.Error)
    }

    @Test
    fun `switching to the profile already in use does nothing`() {
        val plainService = service()
        val profile = plainService.listProfiles().first()
        var activations = 0
        val state = ProfilesTabState(
            plainService,
            { OperatorProfileSelection(profile, "preferences.xml", "worked.db", false) },
            { activations++ },
        )

        val notice = state.activateSelected()

        assertTrue(notice is ProfileNotice.Info)
        assertEquals("This profile is already active.", notice!!.message)
        assertEquals(0, activations,
            "a switch restarts into the other profile; doing that for no reason would "
                + "drop the operator out of a running contest")
    }

    @Test
    fun `an unchanged worked-stations mode is not written`() {
        /*
         * The registry is seeded in the temporary directory so a non-root profile
         * exists without creating one, which would write into the operator's home.
         */
        val store = OperatorProfileStore(registryDirectory.resolve("profiles.xml").toString())
        val shared = OperatorProfile("p1", "DN9APW", false, true)
        store.saveProfiles(listOf(shared), shared.profileId)

        val state = ProfilesTabState(OperatorProfileManagementService(store), { null }, { })

        assertEquals(shared, state.selected)
        assertNull(state.changeWorkedDataModeOfSelected(true),
            "the mode already is the requested one, so nothing is written and nothing is said")
    }

    @Test
    fun `selecting a profile that is not in the list clears the selection`() {
        val state = state()

        state.select(OperatorProfile("stale", "gone", false, false))

        assertNull(state.selected,
            "a profile deleted elsewhere must not stay selectable, or an action would "
                + "operate on a profile that no longer exists")
    }

    @Test
    fun `a blank name creates nothing`() {
        val state = state()

        assertNull(state.createProfile("   ", false))
        assertEquals(1, state.profiles.size)
    }

    @Test
    fun `the duplicate name suggestion is the one the JavaFX dialog pre-filled`() {
        val state = state()

        assertEquals("Copy of " + state.profiles.first().displayName, state.duplicateNameSuggestion())
    }

    @Test
    fun `the worked-stations column names the shared database for the root profile`() {
        val root = OperatorProfile("root", "Default", true, false)

        assertEquals("common station database", ProfilesTabState.describeWorkedDataMode(root),
            "the root profile reads the installation's database whatever its flag says")
        assertEquals(
            "own database",
            ProfilesTabState.describeWorkedDataMode(OperatorProfile("p", "DN9APW", false, false)),
        )
    }

    @Test
    fun `a profile never used shows no date`() {
        val unused = OperatorProfile("p", "DN9APW", false, false)

        assertEquals("", ProfilesTabState.describeLastUsed(unused))
    }
}
