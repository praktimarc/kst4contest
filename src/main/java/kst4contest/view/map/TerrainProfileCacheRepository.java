package kst4contest.view.map;

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Persistent terrain profile cache, stored globally in its own SQLite database.
 *
 * <p>Terrain profiles are pure geometry derived from two locators and a sample count.
 * They do not belong to one operator, so the cache is deliberately not part of an
 * operator profile: at a multi operator station both operators share one location, and
 * duplicating the cache would double the traffic against an external terrain service.</p>
 *
 * <p>Entries are separated by owner identity through the primary key instead. Earlier
 * versions kept a single owner identity and dropped the whole cache whenever the
 * configured callsign or locator changed; with several operator profiles that would
 * discard every computed profile on each switch.</p>
 */
public final class TerrainProfileCacheRepository {

    /**
     * File name of the global terrain profile cache below the application directory.
     */
    public static final String TERRAIN_CACHE_DATABASE_FILE = "terrainprofilecache.db";

    private final String databasePath;

    public TerrainProfileCacheRepository() {
        this.databasePath = ApplicationFileUtils.getFilePath(
                ApplicationConstants.APPLICATION_NAME,
                TERRAIN_CACHE_DATABASE_FILE
        );

        // SQLite only creates the database file itself, not the directory holding it.
        Path applicationDirectory = Path.of(databasePath).getParent();

        if (applicationDirectory != null) {
            try {
                Files.createDirectories(applicationDirectory);
            } catch (IOException exception) {
                System.err.println("[StationMap] Terrain cache directory could not be created: "
                        + exception.getMessage());
            }
        }
    }

    public synchronized Optional<TerrainProfileData> load(String ownerCallsignRaw,
                                                          String ownerLocator6,
                                                          String targetCallsignRaw,
                                                          String targetLocator6,
                                                          int sampleCount,
                                                          String providerId) {

        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            ensureOwnerIdentity(connection, ownerCallsignRaw, ownerLocator6);

            String sql = """
                    SELECT profile_points_text, source_name, synthetic
                    FROM TerrainProfileCache
                    WHERE owner_callsign_raw = ?
                      AND owner_locator6 = ?
                      AND target_callsign_raw = ?
                      AND target_locator6 = ?
                      AND sample_count = ?
                      AND provider_id = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalize(ownerCallsignRaw));
                statement.setString(2, normalize(ownerLocator6));
                statement.setString(3, normalize(targetCallsignRaw));
                statement.setString(4, normalize(targetLocator6));
                statement.setInt(5, sampleCount);
                statement.setString(6, normalize(providerId));

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }

                    String serializedProfile = resultSet.getString("profile_points_text");
                    String sourceName = resultSet.getString("source_name");
                    boolean synthetic = resultSet.getInt("synthetic") != 0;

                    List<PathProfilePoint> points = deserializeProfile(serializedProfile);
                    TerrainProfileData result = new TerrainProfileData(points, sourceName, synthetic);

                    return result.hasUsableProfile() ? Optional.of(result) : Optional.empty();
                }
            }
        } catch (Exception exception) {
            System.err.println("[StationMap] Terrain cache load failed: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public synchronized void save(String ownerCallsignRaw,
                                  String ownerLocator6,
                                  String targetCallsignRaw,
                                  String targetLocator6,
                                  int sampleCount,
                                  String providerId,
                                  TerrainProfileData terrainProfileData) {

        if (terrainProfileData == null || !terrainProfileData.hasUsableProfile() || terrainProfileData.synthetic()) {
            return;
        }

        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            ensureOwnerIdentity(connection, ownerCallsignRaw, ownerLocator6);

            String sql = """
                    INSERT INTO TerrainProfileCache (
                        owner_callsign_raw,
                        owner_locator6,
                        target_callsign_raw,
                        target_locator6,
                        sample_count,
                        provider_id,
                        profile_points_text,
                        source_name,
                        synthetic,
                        created_at_epoch_ms
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(
                        owner_callsign_raw,
                        owner_locator6,
                        target_callsign_raw,
                        target_locator6,
                        sample_count,
                        provider_id
                    ) DO UPDATE SET
                        profile_points_text = excluded.profile_points_text,
                        source_name = excluded.source_name,
                        synthetic = excluded.synthetic,
                        created_at_epoch_ms = excluded.created_at_epoch_ms
                    """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalize(ownerCallsignRaw));
                statement.setString(2, normalize(ownerLocator6));
                statement.setString(3, normalize(targetCallsignRaw));
                statement.setString(4, normalize(targetLocator6));
                statement.setInt(5, sampleCount);
                statement.setString(6, normalize(providerId));
                statement.setString(7, serializeProfile(terrainProfileData.profilePoints()));
                statement.setString(8, terrainProfileData.sourceName());
                statement.setInt(9, terrainProfileData.synthetic() ? 1 : 0);
                statement.setLong(10, System.currentTimeMillis());
                statement.executeUpdate();
            }
        } catch (Exception exception) {
            System.err.println("[StationMap] Terrain cache save failed: " + exception.getMessage());
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    private void ensureSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS TerrainProfileCache (
                        owner_callsign_raw TEXT NOT NULL,
                        owner_locator6 TEXT NOT NULL,
                        target_callsign_raw TEXT NOT NULL,
                        target_locator6 TEXT NOT NULL,
                        sample_count INTEGER NOT NULL,
                        provider_id TEXT NOT NULL,
                        profile_points_text TEXT NOT NULL,
                        source_name TEXT NOT NULL,
                        synthetic INTEGER NOT NULL DEFAULT 0,
                        created_at_epoch_ms INTEGER NOT NULL,
                        PRIMARY KEY (
                            owner_callsign_raw,
                            owner_locator6,
                            target_callsign_raw,
                            target_locator6,
                            sample_count,
                            provider_id
                        )
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS TerrainProfileCacheOwner (
                        owner_callsign_raw TEXT NOT NULL,
                        owner_locator6 TEXT NOT NULL,
                        last_used_epoch_ms INTEGER NOT NULL,
                        PRIMARY KEY (owner_callsign_raw, owner_locator6)
                    )
                    """);
        }
    }

    /**
     * Records that the given owner identity is in use.
     *
     * <p>Entries of other owners stay untouched. The cached profiles of an identity are
     * separated by the primary key already, so a different callsign or locator simply
     * misses the cache instead of invalidating everybody else's entries.</p>
     */
    private void ensureOwnerIdentity(Connection connection,
                                     String currentOwnerCallsignRaw,
                                     String currentOwnerLocator6) throws Exception {

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO TerrainProfileCacheOwner (
                    owner_callsign_raw, owner_locator6, last_used_epoch_ms
                ) VALUES (?, ?, ?)
                ON CONFLICT(owner_callsign_raw, owner_locator6) DO UPDATE SET
                    last_used_epoch_ms = excluded.last_used_epoch_ms
                """)) {
            statement.setString(1, normalize(currentOwnerCallsignRaw));
            statement.setString(2, normalize(currentOwnerLocator6));
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private String serializeProfile(List<PathProfilePoint> profilePoints) {
        StringBuilder builder = new StringBuilder();

        for (PathProfilePoint point : profilePoints) {
            if (point == null) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append('\n');
            }

            builder.append(String.format(
                    Locale.US,
                    "%.6f;%.8f;%.8f;%.3f",
                    point.distanceKm(),
                    point.latitudeDeg(),
                    point.longitudeDeg(),
                    point.elevationMeters()
            ));
        }

        return builder.toString();
    }

    private List<PathProfilePoint> deserializeProfile(String serializedProfile) {
        if (serializedProfile == null || serializedProfile.isBlank()) {
            return List.of();
        }

        String[] lines = serializedProfile.split("\\R+");
        List<PathProfilePoint> points = new ArrayList<>(lines.length);

        for (String line : lines) {
            String[] parts = line.split(";");
            if (parts.length != 4) {
                continue;
            }

            try {
                points.add(new PathProfilePoint(
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3])
                ));
            } catch (NumberFormatException ignored) {
            }
        }

        return List.copyOf(points);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}