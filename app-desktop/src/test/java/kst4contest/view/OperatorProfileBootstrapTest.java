package kst4contest.view;

import kst4contest.controller.OperatorProfilePaths;
import kst4contest.controller.OperatorProfileStore;
import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorProfileBootstrapTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void installationWithoutRegistryStartsSilentlyOnTheHistoricLayout() {

        AtomicInteger pickerInvocations = new AtomicInteger();
        OperatorProfileStore store = storeAt();

        OperatorProfileSelection resolved = new OperatorProfileBootstrap().resolveAtStartup(
                store, new CommandLineOptions(null), countingPicker(pickerInvocations, null));

        assertNotNull(resolved);
        assertEquals("preferences.xml", resolved.getPreferencesRelativeFileName());
        assertEquals("praktiKST.db", resolved.getWorkedDatabaseRelativeFileName());

        // Nothing may be asked, and nothing may be written.
        assertEquals(0, pickerInvocations.get());
        assertTrue(store.loadProfiles().isEmpty());
    }

    @Test
    void singleProfileStartsWithoutAskingAnything() {

        AtomicInteger pickerInvocations = new AtomicInteger();
        OperatorProfileStore store = storeAt();
        store.saveProfiles(List.of(OperatorProfilePaths.buildRootProfile("Default")), "default");

        OperatorProfileSelection resolved = new OperatorProfileBootstrap().resolveAtStartup(
                store, new CommandLineOptions(null), countingPicker(pickerInvocations, null));

        assertNotNull(resolved);
        assertEquals(0, pickerInvocations.get());
    }

    @Test
    void twoProfilesAskTheOperatorAndPreselectTheLastUsedOne() {

        OperatorProfileStore store = storeAt();
        OperatorProfile secondProfile = new OperatorProfile("OP2", "DN9APW", false, false);
        store.saveProfiles(
                List.of(OperatorProfilePaths.buildRootProfile("Default"), secondProfile), "OP2");

        AtomicInteger pickerInvocations = new AtomicInteger();
        String[] observedPreselection = new String[1];

        OperatorProfileSelection resolved = new OperatorProfileBootstrap().resolveAtStartup(
                store,
                new CommandLineOptions(null),
                (profiles, preselectedProfileId) -> {
                    pickerInvocations.incrementAndGet();
                    observedPreselection[0] = preselectedProfileId;
                    return Optional.of(profiles.get(1));
                });

        assertEquals(1, pickerInvocations.get());
        assertEquals("OP2", observedPreselection[0]);
        assertNotNull(resolved);
        assertEquals("profiles/OP2/praktiKST.db", resolved.getWorkedDatabaseRelativeFileName());
    }

    @Test
    void aValidProfileArgumentSkipsThePicker() {

        OperatorProfileStore store = storeAt();
        store.saveProfiles(
                List.of(OperatorProfilePaths.buildRootProfile("Default"),
                        new OperatorProfile("OP2", "DN9APW", false, false)),
                "default");

        AtomicInteger pickerInvocations = new AtomicInteger();
        OperatorProfileBootstrap bootstrap = new OperatorProfileBootstrap();

        OperatorProfileSelection byId = bootstrap.resolveAtStartup(
                store, new CommandLineOptions("op2"), countingPicker(pickerInvocations, null));

        assertEquals(0, pickerInvocations.get());
        assertEquals("profiles/OP2/preferences.xml", byId.getPreferencesRelativeFileName());
        assertNull(bootstrap.getStartupWarning());

        OperatorProfileSelection byDisplayName = bootstrap.resolveAtStartup(
                store, new CommandLineOptions("DN9APW"), countingPicker(pickerInvocations, null));

        assertEquals(0, pickerInvocations.get());
        assertEquals("profiles/OP2/preferences.xml", byDisplayName.getPreferencesRelativeFileName());
    }

    @Test
    void anUnknownProfileArgumentWarnsAndFallsBackToTheNormalSelection() {

        OperatorProfileStore store = storeAt();
        store.saveProfiles(
                List.of(OperatorProfilePaths.buildRootProfile("Default"),
                        new OperatorProfile("OP2", "DN9APW", false, false)),
                "default");

        AtomicInteger pickerInvocations = new AtomicInteger();
        OperatorProfileBootstrap bootstrap = new OperatorProfileBootstrap();

        OperatorProfileSelection resolved = bootstrap.resolveAtStartup(
                store, new CommandLineOptions("NOPE"), countingPicker(pickerInvocations, 0));

        assertEquals(1, pickerInvocations.get());
        assertNotNull(resolved);
        assertNotNull(bootstrap.getStartupWarning());
        assertTrue(bootstrap.getStartupWarning().contains("NOPE"));
    }

    @Test
    void quittingInThePickerYieldsNoSelection() {

        OperatorProfileStore store = storeAt();
        store.saveProfiles(
                List.of(OperatorProfilePaths.buildRootProfile("Default"),
                        new OperatorProfile("OP2", "DN9APW", false, false)),
                "default");

        OperatorProfileSelection resolved = new OperatorProfileBootstrap().resolveAtStartup(
                store, new CommandLineOptions(null), (profiles, preselected) -> Optional.empty());

        assertNull(resolved);
    }

    private OperatorProfileChoiceRequester countingPicker(final AtomicInteger invocationCounter,
            final Integer profileIndexToChoose) {
        return (profiles, preselectedProfileId) -> {
            invocationCounter.incrementAndGet();

            if (profileIndexToChoose == null) {
                return Optional.of(profiles.get(0));
            }

            return Optional.of(profiles.get(profileIndexToChoose));
        };
    }

    private OperatorProfileStore storeAt() {
        return new OperatorProfileStore(temporaryDirectory.resolve("profiles.xml").toString());
    }
}
