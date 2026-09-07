package kst4contest.model;

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;

import java.util.Objects;

/**
 * Resolved runtime view of the active operator profile.
 *
 * <p>This is the only profile information the rest of the application needs: two file
 * names relative to the application directory plus the flag whether a missing
 * worked-station database may be seeded from the bundled template. Everything else is
 * derived from the descriptor.</p>
 */
public class OperatorProfileSelection {

    private final OperatorProfile profile;
    private final String preferencesRelativeFileName;
    private final String workedDatabaseRelativeFileName;
    private final boolean seedWorkedDatabaseFromResource;

    public OperatorProfileSelection(final OperatorProfile profile,
            final String preferencesRelativeFileName,
            final String workedDatabaseRelativeFileName,
            final boolean seedWorkedDatabaseFromResource) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.preferencesRelativeFileName =
                Objects.requireNonNull(preferencesRelativeFileName, "preferencesRelativeFileName");
        this.workedDatabaseRelativeFileName =
                Objects.requireNonNull(workedDatabaseRelativeFileName, "workedDatabaseRelativeFileName");
        this.seedWorkedDatabaseFromResource = seedWorkedDatabaseFromResource;
    }

    public OperatorProfile getProfile() {
        return profile;
    }

    public String getPreferencesRelativeFileName() {
        return preferencesRelativeFileName;
    }

    public String getWorkedDatabaseRelativeFileName() {
        return workedDatabaseRelativeFileName;
    }

    public boolean isSeedWorkedDatabaseFromResource() {
        return seedWorkedDatabaseFromResource;
    }

    /**
     * Returns the absolute preferences path, for display in the settings window.
     *
     * @return absolute path of the preferences file
     */
    public String getPreferencesAbsolutePath() {
        return ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME, preferencesRelativeFileName);
    }

    /**
     * Returns the absolute worked-station database path, for display in the settings window.
     *
     * @return absolute path of the worked-station database
     */
    public String getWorkedDatabaseAbsolutePath() {
        return ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME, workedDatabaseRelativeFileName);
    }
}
