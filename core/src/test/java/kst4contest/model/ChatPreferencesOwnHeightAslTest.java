package kst4contest.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The own ground elevation above sea level: unset by default, limited to a sensible range,
 * and an unset value survives the XML file as unset rather than as a number.
 */
class ChatPreferencesOwnHeightAslTest {

    @TempDir
    Path directory;

    private ChatPreferences preferences() {
        return new ChatPreferences(directory.resolve("preferences.xml"));
    }

    @Test
    void itIsUnsetByDefault() {
        assertTrue(Double.isNaN(preferences().getStn_pathAnalysisOwnHeightAslMeters()));
    }

    @Test
    void valuesInsideTheRangeAreKept() {
        ChatPreferences preferences = preferences();

        preferences.setStn_pathAnalysisOwnHeightAslMeters(-430.0);
        assertEquals(-430.0, preferences.getStn_pathAnalysisOwnHeightAslMeters(), 1e-12);

        preferences.setStn_pathAnalysisOwnHeightAslMeters(ChatPreferences.PATH_ANALYSIS_OWN_HEIGHT_ASL_MAX_METERS);
        assertEquals(ChatPreferences.PATH_ANALYSIS_OWN_HEIGHT_ASL_MAX_METERS,
                preferences.getStn_pathAnalysisOwnHeightAslMeters(), 1e-12);
    }

    @Test
    void valuesOutsideTheRangeClearIt() {
        ChatPreferences preferences = preferences();
        preferences.setStn_pathAnalysisOwnHeightAslMeters(300.0);

        preferences.setStn_pathAnalysisOwnHeightAslMeters(9000.1);
        assertTrue(Double.isNaN(preferences.getStn_pathAnalysisOwnHeightAslMeters()));

        preferences.setStn_pathAnalysisOwnHeightAslMeters(300.0);
        preferences.setStn_pathAnalysisOwnHeightAslMeters(-500.1);
        assertTrue(Double.isNaN(preferences.getStn_pathAnalysisOwnHeightAslMeters()));

        preferences.setStn_pathAnalysisOwnHeightAslMeters(Double.POSITIVE_INFINITY);
        assertTrue(Double.isNaN(preferences.getStn_pathAnalysisOwnHeightAslMeters()));
    }

    @Test
    void anUnsetValueIsWrittenEmptyAndReadBackUnset() throws Exception {
        Path file = directory.resolve("preferences.xml");
        ChatPreferences written = new ChatPreferences(file);
        written.setStn_pathAnalysisOwnHeightAslMeters(Double.NaN);
        assertTrue(written.writePreferencesToXmlFile());

        String xml = Files.readString(file);
        assertTrue(xml.contains("<stn_pathAnalysisOwnHeightAslMeters/>")
                        || xml.contains("<stn_pathAnalysisOwnHeightAslMeters></stn_pathAnalysisOwnHeightAslMeters>"),
                "unset is written as an empty element");
        assertFalse(xml.contains(">NaN<"), "NaN must not reach the file");

        ChatPreferences restored = new ChatPreferences(file);
        restored.setStn_pathAnalysisOwnHeightAslMeters(123.0);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertTrue(Double.isNaN(restored.getStn_pathAnalysisOwnHeightAslMeters()),
                "an empty element clears a value held before reading");
    }

    @Test
    void anOutOfRangeValueInTheFileIsReadAsUnset() throws Exception {
        Path file = directory.resolve("preferences.xml");
        ChatPreferences written = new ChatPreferences(file);
        written.setStn_pathAnalysisOwnHeightAslMeters(250.0);
        assertTrue(written.writePreferencesToXmlFile());

        String xml = Files.readString(file).replace(
                "<stn_pathAnalysisOwnHeightAslMeters>250.0</stn_pathAnalysisOwnHeightAslMeters>",
                "<stn_pathAnalysisOwnHeightAslMeters>25000</stn_pathAnalysisOwnHeightAslMeters>");
        Files.writeString(file, xml);

        ChatPreferences restored = new ChatPreferences(file);
        assertTrue(restored.readPreferencesFromXmlFile());
        assertTrue(Double.isNaN(restored.getStn_pathAnalysisOwnHeightAslMeters()));
    }
}
