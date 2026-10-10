package kst4contest.controller;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatMember;
import kst4contest.utils.ApplicationFileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opens databases of older schema generations with a {@link DBController} and verifies that
 * the controller migrates them to the current schema: all columns present, PRAGMA
 * user_version at the current level, legacy callsign keys merged to base callsigns and the
 * existing rows preserved. The fixtures are built with plain sqlite-jdbc so the test does not
 * depend on the controller to create "old" files.
 */
class DBControllerSchemaMigrationTest {

    private static final String USER_HOME_PROPERTY = "user.home";

    /** Columns the current ChatMember CREATE TABLE statement in DBController declares. */
    private static final List<String> CHAT_MEMBER_COLUMNS = List.of(
            "callsign", "qra", "name", "lastActivityDateTime",
            "worked", "worked144", "worked432", "worked1240", "worked2300", "worked3400",
            "worked5600", "worked10G", "worked50", "worked70",
            "notQRV144", "notQRV432", "notQRV1240", "notQRV2300", "notQRV3400", "notQRV5600",
            "notQRV10G", "notQRV50", "notQRV70",
            "lastFlagsChangeEpochMs");

    /** Columns the current WorkedGrossField CREATE TABLE statement in DBController declares. */
    private static final List<String> WORKED_GROSS_FIELD_COLUMNS = List.of(
            "band", "grossField", "locator", "callsign", "source", "lastWorkedEpochMs");

    /**
     * ChatMember as created by release 1.1: no not-QRV columns, no timestamp, no 50/70 MHz.
     */
    private static final String CREATE_CHAT_MEMBER_V11 =
            "CREATE TABLE ChatMember ("
                    + "callsign TEXT NOT NULL PRIMARY KEY, qra TEXT, name TEXT, "
                    + "lastActivityDateTime TEXT, "
                    + "worked BOOLEAN DEFAULT 0, worked144 BOOLEAN DEFAULT 0, "
                    + "worked432 BOOLEAN DEFAULT 0, worked1240 BOOLEAN DEFAULT 0, "
                    + "worked2300 BOOLEAN DEFAULT 0, worked3400 BOOLEAN DEFAULT 0, "
                    + "worked5600 BOOLEAN DEFAULT 0, worked10G BOOLEAN DEFAULT 0);";

    /**
     * ChatMember as created by release 1.3: not-QRV columns and the expiry timestamp exist,
     * the 50/70 MHz columns of release 1.4 do not.
     */
    private static final String CREATE_CHAT_MEMBER_V13 =
            "CREATE TABLE ChatMember ("
                    + "callsign TEXT NOT NULL PRIMARY KEY, qra TEXT, name TEXT, "
                    + "lastActivityDateTime TEXT, "
                    + "worked BOOLEAN DEFAULT 0, worked144 BOOLEAN DEFAULT 0, "
                    + "worked432 BOOLEAN DEFAULT 0, worked1240 BOOLEAN DEFAULT 0, "
                    + "worked2300 BOOLEAN DEFAULT 0, worked3400 BOOLEAN DEFAULT 0, "
                    + "worked5600 BOOLEAN DEFAULT 0, worked10G BOOLEAN DEFAULT 0, "
                    + "notQRV144 BOOLEAN DEFAULT 0, notQRV432 BOOLEAN DEFAULT 0, "
                    + "notQRV1240 BOOLEAN DEFAULT 0, notQRV2300 BOOLEAN DEFAULT 0, "
                    + "notQRV3400 BOOLEAN DEFAULT 0, notQRV5600 BOOLEAN DEFAULT 0, "
                    + "notQRV10G BOOLEAN DEFAULT 0, "
                    + "lastFlagsChangeEpochMs INTEGER DEFAULT 0);";

    @TempDir
    Path temporaryHomeDirectory;

    private String originalUserHome;
    private DBController controller;

    @BeforeEach
    void redirectUserHomeToTemporaryDirectory() {
        originalUserHome = System.getProperty(USER_HOME_PROPERTY);
        System.setProperty(USER_HOME_PROPERTY, temporaryHomeDirectory.toString());
    }

    @AfterEach
    void closeControllerAndRestoreUserHome() {
        if (controller != null) {
            controller.closeDBConnection();
            controller = null;
        }
        if (originalUserHome == null) {
            System.clearProperty(USER_HOME_PROPERTY);
        } else {
            System.setProperty(USER_HOME_PROPERTY, originalUserHome);
        }
    }

    @Test
    void seededTemplateDatabaseIsMigratedToTheCurrentSchema() throws Exception {
        controller = new DBController("praktiKST.db", true);
        String databasePath = controller.getDatabaseFilePath();

        assertEquals(currentSchemaVersion(), userVersion(databasePath));
        assertColumns(databasePath, "ChatMember", CHAT_MEMBER_COLUMNS);
        assertColumns(databasePath, "WorkedGrossField", WORKED_GROSS_FIELD_COLUMNS);
        assertEquals(0, legacyCallsignRowCount(databasePath),
                "the normalisation rebuild must leave no slash or dash keys behind");

        Map<String, ChatMember> stations = controller.fetchChatMemberWkdDataFromDB();
        // The bundled template carries 3452 foreign callsigns, 199 of them with a suffix or
        // prefix; merging those to their base callsign shrinks the table but keeps the stations.
        assertTrue(stations.size() > 3000 && stations.size() < 3452,
                "unexpected station count after the rebuild: " + stations.size());

        // "OE/HA5DDX" is the only row of this station in the template.
        ChatMember ha5ddx = stations.get("HA5DDX");
        assertNotNull(ha5ddx, "OE/HA5DDX must be stored under its base callsign");
        assertEquals("JN77WM", ha5ddx.getQra());
        assertEquals("Shani", ha5ddx.getName());
        assertFalse(stations.containsKey("OE/HA5DDX"));

        // "F6KFH" and "F6KFH-2" both exist in the template and collapse into one row.
        assertTrue(stations.containsKey("F6KFH"));
        assertFalse(stations.containsKey("F6KFH-2"));
    }

    @Test
    void release11SchemaGainsAllColumnsAndMergesLegacyCallsignKeys() throws Exception {
        String relativeName = "profiles/legacy11/praktiKST.db";
        String databasePath = fixturePath(relativeName);
        try (Connection raw = openRaw(databasePath); Statement statement = raw.createStatement()) {
            statement.executeUpdate(CREATE_CHAT_MEMBER_V11);
            statement.executeUpdate("INSERT INTO ChatMember (callsign, qra, name, worked144) "
                    + "VALUES ('OK2M-70', 'JN69UN', 'Team', 1);");
            statement.executeUpdate("INSERT INTO ChatMember (callsign, worked432) "
                    + "VALUES ('OK2M', 1);");
            statement.executeUpdate("INSERT INTO ChatMember (callsign, qra, name, worked) "
                    + "VALUES ('EA5/G8MBI/P', 'IM98PA', 'Bob', 1);");
            statement.executeUpdate("INSERT INTO ChatMember (callsign, qra, name) "
                    + "VALUES ('DL1XYZ', 'JO31AA', 'Plain');");
        }
        assertEquals(0, userVersion(databasePath));

        controller = new DBController(relativeName, false);

        assertEquals(currentSchemaVersion(), userVersion(databasePath));
        assertColumns(databasePath, "ChatMember", CHAT_MEMBER_COLUMNS);
        assertColumns(databasePath, "WorkedGrossField", WORKED_GROSS_FIELD_COLUMNS);
        assertEquals(0, legacyCallsignRowCount(databasePath));

        Map<String, ChatMember> stations = controller.fetchChatMemberWkdDataFromDB();
        assertEquals(Set.of("DL1XYZ", "G8MBI", "OK2M"), new TreeSet<>(stations.keySet()));

        ChatMember ok2m = stations.get("OK2M");
        assertTrue(ok2m.isWorked144(), "worked144 of OK2M-70 must survive the merge");
        assertTrue(ok2m.isWorked432(), "worked432 of OK2M must survive the merge");
        assertEquals("JN69UN", ok2m.getQra(), "the merge fills an empty locator from the other row");
        assertEquals("Team", ok2m.getName());
        assertTrue(ok2m.isQrv144() && ok2m.isQrv50() && ok2m.isQrv70(),
                "columns added by the migration default to QRV");

        ChatMember g8mbi = stations.get("G8MBI");
        assertTrue(g8mbi.isWorked());
        assertEquals("IM98PA", g8mbi.getQra());

        ChatMember dl1xyz = stations.get("DL1XYZ");
        assertFalse(dl1xyz.isWorked());
        assertFalse(dl1xyz.isWorked144());
        assertEquals("Plain", dl1xyz.getName());
    }

    @Test
    void release13SchemaGainsBandColumnsAndKeepsRowsUntouched() throws Exception {
        String relativeName = "profiles/legacy13/praktiKST.db";
        String databasePath = fixturePath(relativeName);
        long recentFlagsChange = System.currentTimeMillis() - 60_000L;
        try (Connection raw = openRaw(databasePath); Statement statement = raw.createStatement()) {
            statement.executeUpdate(CREATE_CHAT_MEMBER_V13);
            statement.executeUpdate("INSERT INTO ChatMember (callsign, qra, name, "
                    + "lastActivityDateTime, worked144, notQRV432, lastFlagsChangeEpochMs) "
                    + "VALUES ('DL2ABC', 'JO62QL', 'Anna', 'Sun Aug 06 10:21:23 CEST 2023', 1, 1, "
                    + recentFlagsChange + ");");
            statement.executeUpdate("INSERT INTO ChatMember (callsign) VALUES ('DL3DEF');");
        }

        controller = new DBController(relativeName, false);

        assertEquals(currentSchemaVersion(), userVersion(databasePath));
        assertColumns(databasePath, "ChatMember", CHAT_MEMBER_COLUMNS);

        Map<String, ChatMember> stations = controller.fetchChatMemberWkdDataFromDB();
        assertEquals(Set.of("DL2ABC", "DL3DEF"), new TreeSet<>(stations.keySet()));

        ChatMember dl2abc = stations.get("DL2ABC");
        assertTrue(dl2abc.isWorked144());
        assertFalse(dl2abc.isQrv432());
        assertTrue(dl2abc.isQrv144());
        assertFalse(dl2abc.isWorked50());
        assertFalse(dl2abc.isWorked70());
        assertTrue(dl2abc.isQrv50());
        assertTrue(dl2abc.isQrv70());
        assertEquals(recentFlagsChange, dl2abc.getLastFlagsChangeEpochMs(),
                "a recent flags timestamp must not be reset by the expiry cleanup");

        // Without legacy keys no rebuild happens, so even the column the controller never
        // reads back stays as it was.
        assertEquals("Sun Aug 06 10:21:23 CEST 2023",
                scalar(databasePath, "SELECT lastActivityDateTime FROM ChatMember WHERE callsign = 'DL2ABC';"));
    }

    @Test
    void reopeningAnUpToDateDatabaseChangesNothing() throws Exception {
        String relativeName = "profiles/current/praktiKST.db";
        controller = new DBController(relativeName, false);
        String databasePath = controller.getDatabaseFilePath();

        ChatMember worked = new ChatMember();
        worked.setCallSign("DL0XYZ");
        worked.setQra("JO51IJ");
        worked.setWorked(true);
        worked.setWorked144(true);
        controller.storeChatMember(worked);
        controller.closeDBConnection();
        controller = null;

        assertEquals(currentSchemaVersion(), userVersion(databasePath));
        List<String> before = dump(databasePath);

        controller = new DBController(relativeName, false);
        assertTrue(controller.fetchChatMemberWkdDataFromDB().get("DL0XYZ").isWorked144());
        controller.closeDBConnection();
        controller = null;

        assertEquals(before, dump(databasePath),
                "schema, user_version and rows must be identical after reopening");
    }

    // ---------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------

    private static int currentSchemaVersion() throws ReflectiveOperationException {
        Field field = DBController.class.getDeclaredField("CURRENT_DATABASE_SCHEMA_VERSION");
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static String fixturePath(String relativeName) throws Exception {
        String path = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, relativeName);
        Files.createDirectories(Path.of(path).getParent());
        return path;
    }

    private static Connection openRaw(String databasePath) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    private static int userVersion(String databasePath) throws SQLException {
        return Integer.parseInt(scalar(databasePath, "PRAGMA user_version;"));
    }

    private static String scalar(String databasePath, String sql) throws SQLException {
        try (Connection raw = openRaw(databasePath);
             Statement statement = raw.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next(), "no row for " + sql);
            return resultSet.getString(1);
        }
    }

    private static int legacyCallsignRowCount(String databasePath) throws SQLException {
        return Integer.parseInt(scalar(databasePath,
                "SELECT count(*) FROM ChatMember WHERE callsign LIKE '%/%' OR callsign LIKE '%-%';"));
    }

    private static void assertColumns(String databasePath, String table, List<String> expected)
            throws SQLException {
        Set<String> actual = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Connection raw = openRaw(databasePath);
             Statement statement = raw.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ");")) {
            while (resultSet.next()) {
                actual.add(resultSet.getString("name"));
            }
        }
        List<String> missing = new ArrayList<>();
        for (String column : expected) {
            if (!actual.contains(column)) {
                missing.add(column);
            }
        }
        assertEquals(List.of(), missing, "columns missing in " + table + "; present: " + actual);
    }

    /** Captures schema, user_version and every row, so two states can be compared. */
    private static List<String> dump(String databasePath) throws SQLException {
        List<String> lines = new ArrayList<>();
        lines.add("user_version=" + userVersion(databasePath));
        try (Connection raw = openRaw(databasePath); Statement statement = raw.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT type, name, sql FROM sqlite_master ORDER BY type, name;")) {
                while (resultSet.next()) {
                    lines.add(resultSet.getString(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3));
                }
            }
            for (String table : List.of("ChatMember", "WorkedGrossField")) {
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table + " ORDER BY 1;")) {
                    int columns = resultSet.getMetaData().getColumnCount();
                    while (resultSet.next()) {
                        StringBuilder row = new StringBuilder(table);
                        for (int i = 1; i <= columns; i++) {
                            row.append('|').append(resultSet.getString(i));
                        }
                        lines.add(row.toString());
                    }
                }
            }
        }
        return lines;
    }
}
