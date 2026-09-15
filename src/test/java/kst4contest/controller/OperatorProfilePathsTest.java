package kst4contest.controller;

import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorProfilePathsTest {

    @Test
    void rootProfileKeepsTheHistoricFlatFileNames() {

        OperatorProfileSelection resolved =
                OperatorProfilePaths.resolve(OperatorProfilePaths.buildRootProfile("Default"));

        // This is the downgrade guard: an older KST4Contest release reads exactly these
        // two files. If this test ever fails, existing installations would silently lose
        // their configuration and worked data when the operator reverts a version.
        assertEquals("preferences.xml", resolved.getPreferencesRelativeFileName());
        assertEquals("praktiKST.db", resolved.getWorkedDatabaseRelativeFileName());
        assertTrue(resolved.isSeedWorkedDatabaseFromResource());
    }

    @Test
    void additionalProfileWithSharedDatabaseUsesItsOwnPreferencesButTheStationDatabase() {

        OperatorProfile sharedProfile = new OperatorProfile("OP2", "DN9APW", false, true);
        OperatorProfileSelection resolved = OperatorProfilePaths.resolve(sharedProfile);

        assertEquals("profiles/OP2/preferences.xml", resolved.getPreferencesRelativeFileName());
        assertEquals("praktiKST.db", resolved.getWorkedDatabaseRelativeFileName());
        assertTrue(resolved.isSeedWorkedDatabaseFromResource());
    }

    @Test
    void additionalProfileWithOwnDatabaseIsFullySeparatedAndNotSeeded() {

        OperatorProfile ownDatabaseProfile = new OperatorProfile("OP2", "DN9APW", false, false);
        OperatorProfileSelection resolved = OperatorProfilePaths.resolve(ownDatabaseProfile);

        assertEquals("profiles/OP2/preferences.xml", resolved.getPreferencesRelativeFileName());
        assertEquals("profiles/OP2/praktiKST.db", resolved.getWorkedDatabaseRelativeFileName());

        // Seeding would hand a new operator the several thousand callsigns of the
        // bundled template database.
        assertFalse(resolved.isSeedWorkedDatabaseFromResource());
    }

    @Test
    void profileIdIsFileSystemSafe() {

        assertEquals("DN9APW", OperatorProfilePaths.toProfileId("dn9apw", Set.of()));
        assertEquals("DM5M_CONTEST", OperatorProfilePaths.toProfileId("DM5M Contest", Set.of()));
        assertEquals("A_B", OperatorProfilePaths.toProfileId("a/../b", Set.of()));
        assertEquals("MULLER", OperatorProfilePaths.toProfileId("Müller", Set.of()));
        assertEquals("OP", OperatorProfilePaths.toProfileId("   ", Set.of()));
        assertEquals("OP", OperatorProfilePaths.toProfileId(null, Set.of()));

        String longName = "A".repeat(60);
        assertEquals(32, OperatorProfilePaths.toProfileId(longName, Set.of()).length());
    }

    @Test
    void profileIdNeverCollidesAndNeverClaimsTheRootIdentifier() {

        assertEquals("DN9APW_2", OperatorProfilePaths.toProfileId("DN9APW", List.of("DN9APW")));
        assertEquals("DN9APW_3",
                OperatorProfilePaths.toProfileId("DN9APW", List.of("DN9APW", "DN9APW_2")));

        // "default" is reserved for the historic flat installation.
        assertNotEquals(OperatorProfilePaths.ROOT_PROFILE_ID,
                OperatorProfilePaths.toProfileId("default", Set.of()));
    }
}
