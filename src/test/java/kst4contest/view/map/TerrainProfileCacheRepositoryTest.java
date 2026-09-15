package kst4contest.view.map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the property that made the terrain cache useless with several operator
 * profiles: it used to drop every cached profile whenever the configured callsign or
 * locator changed.
 */
class TerrainProfileCacheRepositoryTest {

    private static final String USER_HOME_PROPERTY = "user.home";
    private static final String PROVIDER_ID = "test-provider";
    private static final int SAMPLE_COUNT = 3;

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
    void profilesOfDifferentOwnersCoexistAndSurviveSwitchingBackAndForth() {

        TerrainProfileCacheRepository repository = new TerrainProfileCacheRepository();

        repository.save("DM5M", "JO51IJ", "DL0ABC", "JN49FK",
                SAMPLE_COUNT, PROVIDER_ID, profileData("station-a"));

        // Another operator profile with a different callsign and locator.
        repository.save("DN9APW", "JN59AA", "DL0ABC", "JN49FK",
                SAMPLE_COUNT, PROVIDER_ID, profileData("station-b"));

        Optional<TerrainProfileData> firstOwnerEntry = repository.load(
                "DM5M", "JO51IJ", "DL0ABC", "JN49FK", SAMPLE_COUNT, PROVIDER_ID);
        Optional<TerrainProfileData> secondOwnerEntry = repository.load(
                "DN9APW", "JN59AA", "DL0ABC", "JN49FK", SAMPLE_COUNT, PROVIDER_ID);

        assertTrue(firstOwnerEntry.isPresent(),
                "Working under a second operator identity must not discard the first one's cache");
        assertTrue(secondOwnerEntry.isPresent());
        assertEquals("station-a", firstOwnerEntry.get().sourceName());
        assertEquals("station-b", secondOwnerEntry.get().sourceName());
    }

    @Test
    void anUnknownOwnerSimplyMissesTheCacheInsteadOfClearingIt() {

        TerrainProfileCacheRepository repository = new TerrainProfileCacheRepository();

        repository.save("DM5M", "JO51IJ", "DL0ABC", "JN49FK",
                SAMPLE_COUNT, PROVIDER_ID, profileData("station-a"));

        assertTrue(repository.load("DL0XYZ", "JO60AA", "DL0ABC", "JN49FK",
                SAMPLE_COUNT, PROVIDER_ID).isEmpty());

        assertTrue(repository.load("DM5M", "JO51IJ", "DL0ABC", "JN49FK",
                SAMPLE_COUNT, PROVIDER_ID).isPresent(),
                "A cache miss of one owner must not remove the entries of another");
    }

    @Test
    void theCacheLivesInItsOwnGlobalFileAndNotInTheWorkedStationDatabase() {

        TerrainProfileCacheRepository repository = new TerrainProfileCacheRepository();
        repository.save("DM5M", "JO51IJ", "DL0ABC", "JN49FK",
                SAMPLE_COUNT, PROVIDER_ID, profileData("station-a"));

        Path applicationDirectory = temporaryHomeDirectory.resolve(".praktiKST");

        assertTrue(Files.exists(applicationDirectory.resolve("terrainprofilecache.db")));
        assertFalse(Files.exists(applicationDirectory.resolve("praktiKST.db")),
                "The terrain cache must not pull in the worked station database");
    }

    private static TerrainProfileData profileData(final String sourceName) {
        return new TerrainProfileData(
                List.of(
                        new PathProfilePoint(0.0, 51.0, 10.0, 100.0),
                        new PathProfilePoint(10.0, 51.1, 10.1, 220.0),
                        new PathProfilePoint(20.0, 51.2, 10.2, 150.0)),
                sourceName,
                false);
    }
}
