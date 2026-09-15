package kst4contest.model;

import java.util.Objects;

/**
 * Descriptor of one operator profile.
 *
 * <p>A profile always owns its own preferences file. Whether it also owns its own
 * worked-station database is decided by {@link #isSharedWorkedDatabase()}: a multi
 * operator contest station keeps one common log and therefore shares the database,
 * while two operators sharing a private computer usually want their worked data kept
 * apart.</p>
 *
 * <p>The descriptor deliberately carries no file paths. They are derived in exactly one
 * place, {@link kst4contest.controller.OperatorProfilePaths}, so a stored path can never
 * drift apart from the flag that produced it.</p>
 */
public class OperatorProfile {

    /**
     * Stable identifier of the profile. It is assigned once and never changes, so
     * renaming a profile never moves a directory.
     */
    private String profileId;

    /**
     * Name shown in the profile picker and in the settings window.
     */
    private String displayName;

    /**
     * True for the profile that uses the historic flat installation layout directly.
     */
    private boolean rootProfile;

    /**
     * True if this profile uses the common station worked-station database.
     */
    private boolean sharedWorkedDatabase;

    /**
     * Timestamp of the last activation, used to preselect an entry in the picker.
     */
    private long lastUsedEpochMs;

    public OperatorProfile() {
        // Default constructor for stepwise construction while reading the registry.
    }

    public OperatorProfile(final String profileId,
            final String displayName,
            final boolean rootProfile,
            final boolean sharedWorkedDatabase) {
        this.profileId = profileId;
        this.displayName = displayName;
        this.rootProfile = rootProfile;
        this.sharedWorkedDatabase = sharedWorkedDatabase;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(final String profileId) {
        this.profileId = profileId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public boolean isRootProfile() {
        return rootProfile;
    }

    public void setRootProfile(final boolean rootProfile) {
        this.rootProfile = rootProfile;
    }

    public boolean isSharedWorkedDatabase() {
        return sharedWorkedDatabase;
    }

    public void setSharedWorkedDatabase(final boolean sharedWorkedDatabase) {
        this.sharedWorkedDatabase = sharedWorkedDatabase;
    }

    public long getLastUsedEpochMs() {
        return lastUsedEpochMs;
    }

    public void setLastUsedEpochMs(final long lastUsedEpochMs) {
        this.lastUsedEpochMs = lastUsedEpochMs;
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof OperatorProfile)) {
            return false;
        }

        return Objects.equals(profileId, ((OperatorProfile) other).profileId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(profileId);
    }

    /**
     * Returns the display name so the descriptor can be shown in a list control directly.
     *
     * @return the display name, or the profile id when no name was set
     */
    @Override
    public String toString() {

        if (displayName == null || displayName.isBlank()) {
            return String.valueOf(profileId);
        }

        return displayName;
    }
}
