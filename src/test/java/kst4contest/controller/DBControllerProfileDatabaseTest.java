package kst4contest.controller;

import kst4contest.model.ChatMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that one DBController instance works on exactly one database file, so two
 * operator profiles can keep independent worked-station data inside the same process.
 */
class DBControllerProfileDatabaseTest {

    private static final String USER_HOME_PROPERTY = "user.home";

    @TempDir
    Path temporaryHomeDirectory;

    private String originalUserHome;

    @BeforeEach
    void redirectUserHomeToTemporaryDirectory() {
        originalUserHome = System.getProperty(USER_HOME_PROPERTY);
        System.setProperty(USER_HOME_PROPERTY, temporaryHomeDirectory.toString());
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
    void profileDatabaseIsCreatedEmptyAndKeepsWorkedDataSeparate() throws SQLException {

        DBController firstProfileDatabase =
                new DBController("profiles/OP1/praktiKST.db", false);
        DBController secondProfileDatabase =
                new DBController("profiles/OP2/praktiKST.db", false);

        try {
            // A profile database must not inherit the several thousand callsigns of the
            // bundled template database.
            assertTrue(firstProfileDatabase.fetchChatMemberWkdDataFromDB().isEmpty());
            assertTrue(secondProfileDatabase.fetchChatMemberWkdDataFromDB().isEmpty());

            assertNotEquals(firstProfileDatabase.getDatabaseFilePath(),
                    secondProfileDatabase.getDatabaseFilePath());
            assertTrue(Files.exists(Path.of(firstProfileDatabase.getDatabaseFilePath())));
            assertTrue(Files.exists(Path.of(secondProfileDatabase.getDatabaseFilePath())));

            ChatMember workedOnFirstProfile = new ChatMember();
            workedOnFirstProfile.setCallSign("DL0XYZ");
            workedOnFirstProfile.setQra("JO51IJ");
            workedOnFirstProfile.setWorked(true);
            workedOnFirstProfile.setWorked144(true);

            firstProfileDatabase.storeChatMember(workedOnFirstProfile);

            Map<String, ChatMember> firstProfileContent =
                    firstProfileDatabase.fetchChatMemberWkdDataFromDB();
            Map<String, ChatMember> secondProfileContent =
                    secondProfileDatabase.fetchChatMemberWkdDataFromDB();

            assertEquals(1, firstProfileContent.size());
            assertTrue(firstProfileContent.get("DL0XYZ").isWorked144());
            assertTrue(secondProfileContent.isEmpty(),
                    "A worked station of one profile must not appear in the other profile");
        } finally {
            firstProfileDatabase.closeDBConnection();
            secondProfileDatabase.closeDBConnection();
        }
    }

    @Test
    void twoProfilesPointingAtTheSameFileShareTheirWorkedData() throws SQLException {

        DBController sharedStationDatabase = new DBController("praktiKST.db", false);
        DBController sameSharedDatabaseAgain = new DBController("praktiKST.db", false);

        try {
            ChatMember workedAtTheStation = new ChatMember();
            workedAtTheStation.setCallSign("DL0ABC");
            workedAtTheStation.setWorked(true);
            workedAtTheStation.setWorked432(true);

            sharedStationDatabase.storeChatMember(workedAtTheStation);

            Map<String, ChatMember> seenByTheOtherOperator =
                    sameSharedDatabaseAgain.fetchChatMemberWkdDataFromDB();

            assertTrue(seenByTheOtherOperator.containsKey("DL0ABC"),
                    "Operators sharing one station database must see the same worked stations");
            assertTrue(seenByTheOtherOperator.get("DL0ABC").isWorked432());
        } finally {
            sharedStationDatabase.closeDBConnection();
            sameSharedDatabaseAgain.closeDBConnection();
        }
    }

    @Test
    void closingTheConnectionDeregistersTheShutdownHook() {

        DBController profileDatabase = new DBController("profiles/OP3/praktiKST.db", false);
        profileDatabase.closeDBConnection();

        // A second close must stay harmless, and the hook must already be gone.
        profileDatabase.closeDBConnection();

        assertFalse(Files.notExists(Path.of(profileDatabase.getDatabaseFilePath())),
                "The database file stays on disk after the connection was closed");
    }
}
