package kst4contest.controller;

import kst4contest.model.OperatorProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorProfileStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingRegistryIsNotAnErrorAndIsNotCreated() {

        OperatorProfileStore store = storeAt("profiles.xml");

        assertFalse(store.isRegistryPresent());
        assertTrue(store.loadProfiles().isEmpty());
        assertEquals(Optional.empty(), store.loadLastUsedProfileId());

        // A single operator installation must stay untouched by merely starting up.
        assertFalse(Files.exists(temporaryDirectory.resolve("profiles.xml")));
    }

    @Test
    void profilesSurviveAWriteReadRoundTrip() {

        OperatorProfileStore store = storeAt("profiles.xml");

        OperatorProfile rootProfile = OperatorProfilePaths.buildRootProfile("DM5M station");
        OperatorProfile secondProfile = new OperatorProfile("OP2", "DN9APW", false, false);
        secondProfile.setLastUsedEpochMs(1757328000000L);

        assertTrue(store.saveProfiles(List.of(rootProfile, secondProfile), "OP2"));
        assertTrue(store.isRegistryPresent());

        List<OperatorProfile> restored = store.loadProfiles();

        assertEquals(2, restored.size());
        assertEquals("default", restored.get(0).getProfileId());
        assertEquals("DM5M station", restored.get(0).getDisplayName());
        assertTrue(restored.get(0).isRootProfile());
        assertTrue(restored.get(0).isSharedWorkedDatabase());

        assertEquals("OP2", restored.get(1).getProfileId());
        assertEquals("DN9APW", restored.get(1).getDisplayName());
        assertFalse(restored.get(1).isRootProfile());
        assertFalse(restored.get(1).isSharedWorkedDatabase());
        assertEquals(1757328000000L, restored.get(1).getLastUsedEpochMs());

        assertEquals(Optional.of("OP2"), store.loadLastUsedProfileId());
    }

    @Test
    void atomicWriteLeavesNoTemporaryFileBehind() throws IOException {

        OperatorProfileStore store = storeAt("profiles.xml");
        store.saveProfiles(List.of(OperatorProfilePaths.buildRootProfile("Default")), "default");

        try (var directoryEntries = Files.list(temporaryDirectory)) {
            assertTrue(directoryEntries.noneMatch(entry -> entry.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void malformedRegistryFallsBackToNoAdditionalProfiles() throws IOException {

        Path registryFile = temporaryDirectory.resolve("profiles.xml");
        Files.writeString(registryFile, "<praktiKSTProfiles><profile><profileId>OP2");

        OperatorProfileStore store = storeAt("profiles.xml");

        assertTrue(store.isRegistryPresent());
        assertTrue(store.loadProfiles().isEmpty());
        assertEquals(Optional.empty(), store.loadLastUsedProfileId());
    }

    @Test
    void entriesWithoutAnIdentifierAreSkippedInsteadOfBreakingTheRegistry() throws IOException {

        Path registryFile = temporaryDirectory.resolve("profiles.xml");
        Files.writeString(registryFile,
                "<praktiKSTProfiles>"
                        + "<profile><displayName>broken</displayName></profile>"
                        + "<profile><profileId>OP2</profileId><displayName>DN9APW</displayName>"
                        + "<sharedWorkedDatabase>false</sharedWorkedDatabase></profile>"
                        + "</praktiKSTProfiles>");

        List<OperatorProfile> restored = storeAt("profiles.xml").loadProfiles();

        assertEquals(1, restored.size());
        assertEquals("OP2", restored.get(0).getProfileId());
    }

    @Test
    void recordLastUsedDoesNothingWhenNoRegistryExists() {

        OperatorProfileStore store = storeAt("profiles.xml");

        assertFalse(store.recordLastUsed("default"));
        assertFalse(store.isRegistryPresent());
    }

    private OperatorProfileStore storeAt(final String fileName) {
        return new OperatorProfileStore(temporaryDirectory.resolve(fileName).toString());
    }
}
