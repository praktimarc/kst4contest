package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import kst4contest.model.OperatorProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorProfileManagementServiceTest {

    private static final String USER_HOME_PROPERTY = "user.home";

    @TempDir
    Path temporaryHomeDirectory;

    private String originalUserHome;
    private OperatorProfileManagementService managementService;

    @BeforeEach
    void redirectUserHomeToTemporaryDirectory() {
        originalUserHome = System.getProperty(USER_HOME_PROPERTY);
        System.setProperty(USER_HOME_PROPERTY, temporaryHomeDirectory.toString());
        managementService = new OperatorProfileManagementService();
    }

    @AfterEach
    void restoreUserHome() {
        if (originalUserHome == null) {
            System.clearProperty(USER_HOME_PROPERTY);
        } else {
            System.setProperty(USER_HOME_PROPERTY, originalUserHome);
        }
    }

    @Test
    void aPlainInstallationReportsExactlyOneImplicitRootProfile() {

        List<OperatorProfile> knownProfiles = managementService.listProfiles();

        assertEquals(1, knownProfiles.size());
        assertTrue(knownProfiles.get(0).isRootProfile());
        assertFalse(Files.exists(applicationFile("profiles.xml")),
                "Merely listing profiles must not create a registry");
    }

    @Test
    void creatingTheSecondProfileMaterialisesTheRegistryIncludingTheRootProfile() {

        OperatorProfile createdProfile = managementService.createProfile("DN9APW", false);

        assertNotNull(createdProfile);
        assertEquals("DN9APW", createdProfile.getProfileId());
        assertTrue(Files.exists(applicationFile("profiles.xml")));

        List<OperatorProfile> knownProfiles = managementService.listProfiles();

        assertEquals(2, knownProfiles.size());
        assertTrue(knownProfiles.get(0).isRootProfile());
        assertEquals("DN9APW", knownProfiles.get(1).getProfileId());

        assertTrue(Files.exists(applicationFile("profiles/DN9APW/preferences.xml")));

        // The historic files must stay exactly where an older release expects them.
        assertFalse(Files.exists(applicationFile("profiles/default")));
    }

    @Test
    void aNewProfileStartsWithoutLoginCredentials() {

        OperatorProfile createdProfile = managementService.createProfile("DN9APW", false);

        ChatPreferences createdPreferences =
                preferencesAt(OperatorProfilePaths.preferencesRelativeFileName(createdProfile));

        assertEquals("", createdPreferences.getStn_loginCallSign());
        assertEquals("", createdPreferences.getStn_loginPassword());
    }

    @Test
    void duplicatingKeepsTheStationSetupButClearsCallsignAndPassword() {

        OperatorProfile sourceProfile = managementService.createProfile("Source", false);

        ChatPreferences sourcePreferences =
                preferencesAt(OperatorProfilePaths.preferencesRelativeFileName(sourceProfile));
        sourcePreferences.setStn_loginCallSign("DM5M");
        sourcePreferences.setStn_loginPassword("secret");
        sourcePreferences.setStn_loginLocatorMainCat("JO51IJ");
        sourcePreferences.setStn_antennaBeamWidthDeg(17.5);
        assertTrue(sourcePreferences.writePreferencesToXmlFile());

        OperatorProfile duplicatedProfile =
                managementService.duplicateProfile(sourceProfile, "Copy of source");

        assertNotNull(duplicatedProfile);

        ChatPreferences duplicatedPreferences =
                preferencesAt(OperatorProfilePaths.preferencesRelativeFileName(duplicatedProfile));

        // The work worth keeping.
        assertEquals("JO51IJ", duplicatedPreferences.getStn_loginLocatorMainCat());
        assertEquals(17.5, duplicatedPreferences.getStn_antennaBeamWidthDeg());

        // The identity that must not be inherited.
        assertEquals("", duplicatedPreferences.getStn_loginCallSign());
        assertEquals("", duplicatedPreferences.getStn_loginPassword());
    }

    @Test
    void switchingBetweenSharedAndOwnWorkedDataChangesOnlyTheDatabasePath() {

        OperatorProfile createdProfile = managementService.createProfile("DN9APW", false);

        assertEquals("profiles/DN9APW/praktiKST.db",
                OperatorProfilePaths.workedDatabaseRelativeFileName(createdProfile));

        assertTrue(managementService.setSharedWorkedDatabase(createdProfile, true));

        OperatorProfile reloadedProfile = managementService.listProfiles().get(1);

        assertTrue(reloadedProfile.isSharedWorkedDatabase());
        assertEquals("praktiKST.db",
                OperatorProfilePaths.workedDatabaseRelativeFileName(reloadedProfile));
        assertEquals("profiles/DN9APW/preferences.xml",
                OperatorProfilePaths.preferencesRelativeFileName(reloadedProfile));
    }

    @Test
    void deletingRemovesTheProfileDirectoryButNeverTheRootProfile() {

        OperatorProfile createdProfile = managementService.createProfile("DN9APW", false);
        assertTrue(Files.exists(applicationFile("profiles/DN9APW/preferences.xml")));

        OperatorProfile rootProfile = managementService.listProfiles().get(0);
        assertFalse(managementService.deleteProfile(rootProfile),
                "The root profile is the installation itself and must not be removable");

        assertTrue(managementService.deleteProfile(createdProfile));
        assertFalse(Files.exists(applicationFile("profiles/DN9APW")));
        assertEquals(1, managementService.listProfiles().size());
    }

    @Test
    void renamingKeepsTheIdentifierAndTherebyAllPaths() {

        OperatorProfile createdProfile = managementService.createProfile("DN9APW", false);

        assertTrue(managementService.renameProfile(createdProfile, "Philipp portable"));

        OperatorProfile renamedProfile = managementService.listProfiles().get(1);

        assertEquals("Philipp portable", renamedProfile.getDisplayName());
        assertEquals("DN9APW", renamedProfile.getProfileId());
        assertEquals("profiles/DN9APW/preferences.xml",
                OperatorProfilePaths.preferencesRelativeFileName(renamedProfile));
    }

    private ChatPreferences preferencesAt(final String relativeFileName) {
        ChatPreferences preferences = new ChatPreferences(relativeFileName);
        preferences.readPreferencesFromXmlFile();
        return preferences;
    }

    private Path applicationFile(final String relativeFileName) {
        return temporaryHomeDirectory.resolve(".praktiKST").resolve(relativeFileName);
    }
}
