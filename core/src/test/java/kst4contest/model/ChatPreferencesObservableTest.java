package kst4contest.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;

class ChatPreferencesObservableTest {

    @TempDir
    Path tempDir;

    @Test
    void theDefaultsMatchTheFormerJavaFxProperties() {
        ChatPreferences prefs = new ChatPreferences(tempDir.resolve("preferences.xml"));

        assertEquals(360.0, prefs.getActualQTF().get(), 0.0001,
                "actualQTF defaulted to 360 as a SimpleDoubleProperty");
        assertEquals("144", prefs.getNotify_optionalFrequencyPrefix().get());
        assertEquals("DO5AMF", prefs.getNotify_DXCSrv_SpottersCallSign().get());
    }

    @Test
    void theQrgValuesStartUnavailable() {
        ChatPreferences prefs = new ChatPreferences(tempDir.resolve("preferences.xml"));

        assertNull(prefs.getMYQRGFirstCat().get(),
                "an unset own QRG must stay unavailable rather than becoming a fixed band");
        assertNull(prefs.getMYQRGSecondCat().get());
    }

    @Test
    void theListSettingsStartEmpty() {
        ChatPreferences prefs = new ChatPreferences(tempDir.resolve("preferences.xml"));

        assertTrue(prefs.getLstNotify_QSOSniffer_sniffedCallSignList().snapshot().isEmpty());
        assertTrue(prefs.getLstNotify_QSOSniffer_sniffedPrefixLocList().snapshot().isEmpty());
        assertTrue(prefs.getLst_txtSnipList().snapshot().isEmpty());
        assertTrue(prefs.getLst_txtShortCutBtnList().snapshot().isEmpty());
    }
}
