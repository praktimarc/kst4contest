package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;

import java.util.Collection;
import java.util.Locale;

/**
 * Derives the file names of an operator profile.
 *
 * <p>This is the single place that knows how a profile maps onto files. The registry
 * stores only the shared/own flag, never a path, so the two can never drift apart.</p>
 */
public final class OperatorProfilePaths {

    /**
     * Directory below the application directory that holds the additional profiles.
     */
    public static final String PROFILES_DIRECTORY = "profiles";

    /**
     * Identifier of the profile that uses the historic flat installation layout.
     */
    public static final String ROOT_PROFILE_ID = "default";

    /**
     * Maximum length of a generated profile identifier.
     */
    private static final int MAX_PROFILE_ID_LENGTH = 32;

    private OperatorProfilePaths() {
        // Utility class.
    }

    /**
     * Builds the profile descriptor of the historic flat installation.
     *
     * @param displayName name to show for the root profile
     * @return the root profile descriptor
     */
    public static OperatorProfile buildRootProfile(final String displayName) {
        return new OperatorProfile(ROOT_PROFILE_ID, displayName, true, true);
    }

    /**
     * Returns the profile directory relative to the application directory.
     *
     * @param profile profile to resolve
     * @return relative directory name
     */
    public static String profileRelativeDirectory(final OperatorProfile profile) {
        return PROFILES_DIRECTORY + "/" + profile.getProfileId();
    }

    /**
     * Returns the preferences file name relative to the application directory.
     *
     * @param profile profile to resolve
     * @return relative preferences file name
     */
    public static String preferencesRelativeFileName(final OperatorProfile profile) {

        if (profile.isRootProfile()) {
            return ChatPreferences.PREFERENCES_FILE;
        }

        return profileRelativeDirectory(profile) + "/" + ChatPreferences.PREFERENCES_FILE;
    }

    /**
     * Returns the worked-station database file name relative to the application directory.
     *
     * <p>A profile using the common station database always resolves to the historic flat
     * file, which is what a multi operator station wants: the existing contest state stays
     * the shared one.</p>
     *
     * @param profile profile to resolve
     * @return relative database file name
     */
    public static String workedDatabaseRelativeFileName(final OperatorProfile profile) {

        if (profile.isRootProfile() || profile.isSharedWorkedDatabase()) {
            return DBController.DATABASE_FILE;
        }

        return profileRelativeDirectory(profile) + "/" + DBController.DATABASE_FILE;
    }

    /**
     * Resolves a profile descriptor into the runtime selection used during startup.
     *
     * @param profile profile to resolve
     * @return resolved selection
     */
    public static OperatorProfileSelection resolve(final OperatorProfile profile) {

        boolean usesSharedStationDatabase = profile.isRootProfile() || profile.isSharedWorkedDatabase();

        return new OperatorProfileSelection(
                profile,
                preferencesRelativeFileName(profile),
                workedDatabaseRelativeFileName(profile),
                usesSharedStationDatabase
        );
    }

    /**
     * Derives a stable, file system safe identifier from a display name.
     *
     * <p>The identifier becomes a directory name and is never changed afterwards, so a
     * later rename of the profile does not move any file.</p>
     *
     * @param displayName name entered by the operator
     * @param takenProfileIds identifiers that are already in use
     * @return an identifier that is not yet taken
     */
    public static String toProfileId(final String displayName, final Collection<String> takenProfileIds) {

        StringBuilder sanitized = new StringBuilder();

        if (displayName != null) {
            String foldedDisplayName = foldGermanUmlauts(displayName.toUpperCase(Locale.ROOT));

            for (char currentCharacter : foldedDisplayName.toCharArray()) {
                boolean isAcceptable = (currentCharacter >= 'A' && currentCharacter <= 'Z')
                        || (currentCharacter >= '0' && currentCharacter <= '9')
                        || currentCharacter == '-';

                if (isAcceptable) {
                    sanitized.append(currentCharacter);
                } else if (sanitized.length() > 0 && sanitized.charAt(sanitized.length() - 1) != '_') {
                    sanitized.append('_');
                }
            }
        }

        while (sanitized.length() > 0 && sanitized.charAt(sanitized.length() - 1) == '_') {
            sanitized.setLength(sanitized.length() - 1);
        }

        if (sanitized.length() > MAX_PROFILE_ID_LENGTH) {
            sanitized.setLength(MAX_PROFILE_ID_LENGTH);
        }

        String candidate = sanitized.toString();

        if (candidate.isEmpty() || ROOT_PROFILE_ID.equalsIgnoreCase(candidate)) {
            candidate = "OP";
        }

        if (!isProfileIdTaken(candidate, takenProfileIds)) {
            return candidate;
        }

        int suffix = 2;

        while (isProfileIdTaken(candidate + "_" + suffix, takenProfileIds)) {
            suffix++;
        }

        return candidate + "_" + suffix;
    }

    /**
     * Folds German umlauts so a name like "Muller" written with an umlaut still produces a
     * readable identifier instead of a placeholder character.
     *
     * @param upperCaseText already upper-cased text
     * @return text with umlauts replaced by their base letters
     */
    private static String foldGermanUmlauts(final String upperCaseText) {
        return upperCaseText
                .replace("\u00C4", "A")
                .replace("\u00D6", "O")
                .replace("\u00DC", "U")
                .replace("\u00DF", "SS");
    }

    private static boolean isProfileIdTaken(final String candidate, final Collection<String> takenProfileIds) {

        if (ROOT_PROFILE_ID.equalsIgnoreCase(candidate)) {
            return true;
        }

        if (takenProfileIds == null) {
            return false;
        }

        for (String takenProfileId : takenProfileIds) {
            if (candidate.equalsIgnoreCase(takenProfileId)) {
                return true;
            }
        }

        return false;
    }
}
