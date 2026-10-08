package kst4contest.view.compose.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kst4contest.controller.OperatorProfileManagementService
import kst4contest.model.OperatorProfile
import kst4contest.model.OperatorProfileSelection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** What the profile tab has to tell the operator after an action. */
sealed interface ProfileNotice {

    val message: String

    /** Succeeded, but there is something the operator needs to know. */
    data class Info(override val message: String) : ProfileNotice

    /** Did not happen, and why. */
    data class Error(override val message: String) : ProfileNotice
}

/**
 * State of the profiles tab.
 *
 * Every rule of `OperatorProfileSettingsPane` lives here so it can be tested without
 * a toolkit: the default profile cannot be deleted and always uses the common worked
 * stations because that database belongs to the installation, the profile in use
 * cannot be deleted, and switching to the profile already in use does nothing.
 *
 * @param activeSelection the profile in use; a function rather than a value because
 *        it changes under the tab when the operator switches profiles
 * @param requestActivation hands the switch to the application, which restarts into
 *        the other profile — the tab must not do that itself
 */
class ProfilesTabState(
    private val service: OperatorProfileManagementService,
    private val activeSelection: () -> OperatorProfileSelection?,
    private val requestActivation: (OperatorProfile) -> Unit,
) {

    /*
     * Compose state, not plain properties. A LazyColumn row only redraws when
     * something it observes changes; with plain properties the selection moved in the
     * state and the list went on showing the old row highlighted.
     */
    var profiles: List<OperatorProfile> by mutableStateOf(emptyList())
        private set

    var selected: OperatorProfile? by mutableStateOf(null)
        private set

    init {
        refresh()
    }

    val activeProfileName: String
        get() = activeSelection()?.profile?.displayName ?: "unknown"

    val preferencesPath: String
        get() = activeSelection()?.preferencesAbsolutePath ?: ""

    val workedDatabasePath: String
        get() = activeSelection()?.workedDatabaseAbsolutePath ?: ""

    fun select(profile: OperatorProfile?) {
        selected = profile?.takeIf { profiles.contains(it) }
    }

    /**
     * Re-reads the registry and keeps the selection when the selected profile is
     * still there, as `refreshProfileTable` did.
     */
    fun refresh() {
        val known = service.listProfiles()
        profiles = known

        selected = when {
            selected != null && known.contains(selected) -> selected
            known.isNotEmpty() -> known.first()
            else -> null
        }
    }

    fun createProfile(name: String, sharedWorkedDatabase: Boolean): ProfileNotice? {
        if (name.isBlank()) {
            return null
        }

        val created = service.createProfile(name.trim(), sharedWorkedDatabase)
            ?: return ProfileNotice.Error(
                "The profile could not be created. The profile registry could not be written."
            )

        refresh()
        selected = created

        /*
         * Said explicitly because a new profile that cannot log in looks broken
         * otherwise: callsign and password are deliberately not copied or invented.
         */
        return ProfileNotice.Info(
            "The profile \"" + created.displayName + "\" was created without callsign and " +
                "password. Enter them on the Station tab after switching to it."
        )
    }

    fun duplicateSelected(name: String): ProfileNotice? {
        val profile = selected ?: return notASelection()

        if (name.isBlank()) {
            return null
        }

        val duplicate = service.duplicateProfile(profile, name)
            ?: return ProfileNotice.Error("The profile could not be duplicated.")

        refresh()
        selected = duplicate
        return null
    }

    /** The suggested name of a duplicate, the one the JavaFX dialog pre-filled. */
    fun duplicateNameSuggestion(): String =
        selected?.let { "Copy of " + it.displayName } ?: ""

    fun renameSelected(name: String): ProfileNotice? {
        val profile = selected ?: return notASelection()

        if (name.isBlank()) {
            return null
        }

        service.renameProfile(profile, name)
        refresh()
        return null
    }

    fun deleteSelected(): ProfileNotice? {
        val profile = selected ?: return notASelection()

        if (profile.isRootProfile) {
            return ProfileNotice.Error(
                "The default profile uses the files of the installation itself and cannot be deleted."
            )
        }

        if (isActive(profile)) {
            return ProfileNotice.Error(
                "The profile currently in use cannot be deleted. Switch to another profile first."
            )
        }

        if (!service.deleteProfile(profile)) {
            refresh()
            return ProfileNotice.Error("The profile could not be deleted.")
        }

        refresh()
        return null
    }

    /** The folder the deletion confirmation names, and what happens to worked stations. */
    fun describeDeletion(profile: OperatorProfile): String =
        "The following folder is removed permanently:\n" +
            service.getProfileDirectory(profile) + "\n\n" +
            if (profile.isSharedWorkedDatabase) {
                "The common station worked stations are not touched."
            } else {
                "The worked stations of this profile are deleted as well."
            }

    fun changeWorkedDataModeOfSelected(sharedWorkedDatabase: Boolean): ProfileNotice? {
        val profile = selected ?: return notASelection()

        if (profile.isRootProfile) {
            return ProfileNotice.Error(
                "The default profile always uses the common station worked stations, because " +
                    "that database is the one of the installation itself."
            )
        }

        if (sharedWorkedDatabase == profile.isSharedWorkedDatabase) {
            return null
        }

        service.setSharedWorkedDatabase(profile, sharedWorkedDatabase)
        refresh()

        if (isActive(profile)) {
            return ProfileNotice.Info("The change takes effect after switching to this profile again.")
        }

        return null
    }

    fun activateSelected(): ProfileNotice? {
        val profile = selected ?: return notASelection()

        if (isActive(profile)) {
            return ProfileNotice.Info("This profile is already active.")
        }

        requestActivation(profile)
        return null
    }

    /** Whether changing the worked-stations mode is possible at all for a profile. */
    fun canChangeWorkedDataMode(profile: OperatorProfile): Boolean = !profile.isRootProfile

    fun isActive(profile: OperatorProfile): Boolean {
        val active = activeSelection() ?: return false

        return active.profile.profileId == profile.profileId
    }

    private fun notASelection(): ProfileNotice =
        ProfileNotice.Info("Select a profile in the table first.")

    companion object {

        private val LAST_USED_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

        /**
         * The root profile always reads the installation's own database, whatever its
         * stored flag says, which is why it is checked first here too.
         */
        fun describeWorkedDataMode(profile: OperatorProfile): String =
            if (profile.isRootProfile || profile.isSharedWorkedDatabase) {
                "common station database"
            } else {
                "own database"
            }

        /** Blank for a profile never used, rather than an epoch date. */
        fun describeLastUsed(profile: OperatorProfile): String =
            if (profile.lastUsedEpochMs <= 0L) {
                ""
            } else {
                LAST_USED_FORMATTER.format(Instant.ofEpochMilli(profile.lastUsedEpochMs))
            }
    }
}
