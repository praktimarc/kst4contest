package kst4contest.controller;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatPreferences;
import kst4contest.model.OperatorProfile;
import kst4contest.utils.ApplicationFileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Creates, renames, duplicates and removes operator profiles.
 *
 * <p>Kept free of user interface code so the behaviour can be tested without a JavaFX
 * runtime. All methods work on the registry and on the files below the application
 * directory.</p>
 */
public class OperatorProfileManagementService {

    private static final Logger LOGGER =
            Logger.getLogger(OperatorProfileManagementService.class.getName());

    private final OperatorProfileStore profileStore;

    public OperatorProfileManagementService() {
        this(new OperatorProfileStore());
    }

    public OperatorProfileManagementService(final OperatorProfileStore profileStore) {
        this.profileStore = profileStore;
    }

    /**
     * Lists all profiles, including the implicit root profile of a plain installation.
     *
     * @return the known profiles, never empty
     */
    public List<OperatorProfile> listProfiles() {

        List<OperatorProfile> knownProfiles = profileStore.loadProfiles();

        if (knownProfiles.isEmpty()) {
            knownProfiles = new ArrayList<>();
            knownProfiles.add(profileStore.buildImplicitRootProfile());
        }

        return knownProfiles;
    }

    /**
     * Creates a new operator profile with its own preferences file.
     *
     * <p>Creating the first additional profile is also the moment the registry appears:
     * the root profile is written alongside, so both are selectable afterwards.</p>
     *
     * @param displayName          name entered by the operator
     * @param sharedWorkedDatabase true to use the common station worked database
     * @return the created profile, or null when it could not be stored
     */
    public OperatorProfile createProfile(final String displayName, final boolean sharedWorkedDatabase) {

        List<OperatorProfile> knownProfiles = listProfiles();
        Set<String> takenProfileIds = new LinkedHashSet<>();

        for (OperatorProfile existingProfile : knownProfiles) {
            takenProfileIds.add(existingProfile.getProfileId());
        }

        OperatorProfile createdProfile = new OperatorProfile(
                OperatorProfilePaths.toProfileId(displayName, takenProfileIds),
                displayName == null || displayName.isBlank() ? "New profile" : displayName.trim(),
                false,
                sharedWorkedDatabase);

        knownProfiles.add(createdProfile);

        if (!profileStore.saveProfiles(knownProfiles, createdProfile.getProfileId())) {
            return null;
        }

        createPreferencesFile(createdProfile, null);

        return createdProfile;
    }

    /**
     * Creates a copy of an existing profile.
     *
     * <p>The preferences are taken over completely except for the login credentials:
     * callsign and password are cleared on purpose, because a duplicate is meant for
     * another operator. Antenna, locator, layout and integration settings are exactly
     * what the operator does not want to enter twice.</p>
     *
     * <p>The worked-station database is never copied.</p>
     *
     * @param sourceProfile profile to copy
     * @param displayName   name of the new profile
     * @return the created profile, or null when it could not be stored
     */
    public OperatorProfile duplicateProfile(final OperatorProfile sourceProfile, final String displayName) {

        if (sourceProfile == null) {
            return null;
        }

        List<OperatorProfile> knownProfiles = listProfiles();
        Set<String> takenProfileIds = new LinkedHashSet<>();

        for (OperatorProfile existingProfile : knownProfiles) {
            takenProfileIds.add(existingProfile.getProfileId());
        }

        OperatorProfile createdProfile = new OperatorProfile(
                OperatorProfilePaths.toProfileId(displayName, takenProfileIds),
                displayName == null || displayName.isBlank() ? "Copy" : displayName.trim(),
                false,
                sourceProfile.isSharedWorkedDatabase());

        knownProfiles.add(createdProfile);

        if (!profileStore.saveProfiles(knownProfiles, createdProfile.getProfileId())) {
            return null;
        }

        createPreferencesFile(createdProfile, sourceProfile);

        return createdProfile;
    }

    /**
     * Changes the visible name of a profile. The identifier and all paths stay as they are.
     *
     * @param profile        profile to rename
     * @param newDisplayName new name
     * @return true if the registry was updated
     */
    public boolean renameProfile(final OperatorProfile profile, final String newDisplayName) {

        if (profile == null || newDisplayName == null || newDisplayName.isBlank()) {
            return false;
        }

        List<OperatorProfile> knownProfiles = listProfiles();

        for (OperatorProfile currentProfile : knownProfiles) {
            if (currentProfile.getProfileId().equals(profile.getProfileId())) {
                currentProfile.setDisplayName(newDisplayName.trim());
            }
        }

        return profileStore.saveProfiles(knownProfiles, profileStore.loadLastUsedProfileId().orElse(null));
    }

    /**
     * Switches a profile between the common station database and its own one.
     *
     * @param profile              profile to change
     * @param sharedWorkedDatabase true to use the common station worked database
     * @return true if the registry was updated
     */
    public boolean setSharedWorkedDatabase(final OperatorProfile profile, final boolean sharedWorkedDatabase) {

        if (profile == null || profile.isRootProfile()) {
            return false;
        }

        List<OperatorProfile> knownProfiles = listProfiles();

        for (OperatorProfile currentProfile : knownProfiles) {
            if (currentProfile.getProfileId().equals(profile.getProfileId())) {
                currentProfile.setSharedWorkedDatabase(sharedWorkedDatabase);
            }
        }

        return profileStore.saveProfiles(knownProfiles, profileStore.loadLastUsedProfileId().orElse(null));
    }

    /**
     * Removes a profile and its directory.
     *
     * <p>The root profile can never be removed, because its files are the installation
     * itself. A profile using the common station database keeps that database untouched;
     * only its own directory is deleted.</p>
     *
     * @param profile profile to remove
     * @return true if the profile was removed
     */
    public boolean deleteProfile(final OperatorProfile profile) {

        if (profile == null || profile.isRootProfile()) {
            return false;
        }

        List<OperatorProfile> remainingProfiles = new ArrayList<>();

        for (OperatorProfile currentProfile : listProfiles()) {
            if (!currentProfile.getProfileId().equals(profile.getProfileId())) {
                remainingProfiles.add(currentProfile);
            }
        }

        if (!profileStore.saveProfiles(remainingProfiles,
                profileStore.loadLastUsedProfileId().orElse(null))) {
            return false;
        }

        deleteProfileDirectory(profile);

        return true;
    }

    /**
     * Returns the absolute directory of a profile.
     *
     * @param profile profile to resolve
     * @return absolute profile directory
     */
    public String getProfileDirectory(final OperatorProfile profile) {
        return ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME,
                OperatorProfilePaths.profileRelativeDirectory(profile));
    }

    /**
     * Creates the preferences file of a new profile.
     *
     * <p>The file is either seeded from the bundled template or copied from the source
     * profile. In both cases the login credentials are cleared, so a new profile never
     * carries another operator's callsign or password.</p>
     *
     * @param createdProfile profile that needs a preferences file
     * @param sourceProfile  profile to copy the preferences from, or null for the template
     */
    private void createPreferencesFile(final OperatorProfile createdProfile,
            final OperatorProfile sourceProfile) {

        String createdRelativeFileName = OperatorProfilePaths.preferencesRelativeFileName(createdProfile);

        if (sourceProfile != null) {
            copyPreferencesFile(
                    OperatorProfilePaths.preferencesRelativeFileName(sourceProfile),
                    createdRelativeFileName);
        }

        // Seeds from the bundled template when nothing was copied, and always resolves
        // the preferences of the new profile.
        ChatPreferences createdPreferences = new ChatPreferences(createdRelativeFileName);
        createdPreferences.readPreferencesFromXmlFile();

        createdPreferences.setStn_loginCallSign("");
        createdPreferences.setStn_loginPassword("");

        createdPreferences.writePreferencesToXmlFile();
    }

    private void copyPreferencesFile(final String sourceRelativeFileName,
            final String targetRelativeFileName) {

        Path sourcePath = Path.of(ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME, sourceRelativeFileName));
        Path targetPath = Path.of(ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME, targetRelativeFileName));

        if (!Files.isRegularFile(sourcePath)) {
            return;
        }

        try {
            Path targetDirectory = targetPath.getParent();

            if (targetDirectory != null) {
                Files.createDirectories(targetDirectory);
            }

            Files.copy(sourcePath, targetPath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Could not copy the preferences of the source profile, using the defaults instead", e);
        }
    }

    private void deleteProfileDirectory(final OperatorProfile profile) {

        Path profileDirectory = Path.of(getProfileDirectory(profile));

        if (!Files.isDirectory(profileDirectory)) {
            return;
        }

        try (Stream<Path> containedPaths = Files.walk(profileDirectory)) {
            List<Path> deepestFirst = containedPaths
                    .sorted(Comparator.reverseOrder())
                    .toList();

            for (Path currentPath : deepestFirst) {
                Files.deleteIfExists(currentPath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Could not remove the directory of the deleted operator profile", e);
        }
    }
}
