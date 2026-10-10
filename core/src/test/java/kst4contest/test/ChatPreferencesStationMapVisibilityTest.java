package kst4contest.test;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPreferencesStationMapVisibilityTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void pathAnalysisIsVisibleByDefault() {
        ChatPreferences preferences = new ChatPreferences();

        assertTrue(preferences.isGUIstationMapPathAnalysisVisible());
    }

    @Test
    void hiddenPathAnalysisStateSurvivesXmlRoundTrip() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("preferences.xml");

        ChatPreferences writtenPreferences = new ChatPreferences();
        writtenPreferences.setStoreAndRestorePreferencesFileName(
                preferencesFile.toString());
        writtenPreferences.setGUIstationMapPathAnalysisVisible(false);
        writtenPreferences.writePreferencesToXmlFile();

        String writtenXml = Files.readString(preferencesFile);
        assertTrue(writtenXml.contains(
                "<GUIstationMapPathAnalysisVisible>false"
                        + "</GUIstationMapPathAnalysisVisible>"));

        ChatPreferences restoredPreferences = new ChatPreferences();
        restoredPreferences.setStoreAndRestorePreferencesFileName(
                preferencesFile.toString());
        restoredPreferences.readPreferencesFromXmlFile();

        assertFalse(restoredPreferences.isGUIstationMapPathAnalysisVisible());
    }

    @Test
    void legacyXmlWithoutVisibilitySettingKeepsAnalysisDiscoverable()
            throws IOException {

        Path legacyPreferencesFile =
                temporaryDirectory.resolve("legacy-preferences.xml");

        Files.writeString(legacyPreferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>4</configVersion>
                    <guiOptions>
                        <GUIstationMapStageSceneSizeHW>1000.0;800.0</GUIstationMapStageSceneSizeHW>
                    </guiOptions>
                </praktiKST>
                """);

        ChatPreferences restoredPreferences = new ChatPreferences();
        restoredPreferences.setStoreAndRestorePreferencesFileName(
                legacyPreferencesFile.toString());
        restoredPreferences.readPreferencesFromXmlFile();

        assertTrue(restoredPreferences.isGUIstationMapPathAnalysisVisible());
    }
}