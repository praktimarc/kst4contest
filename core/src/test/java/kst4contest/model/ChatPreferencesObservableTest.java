package kst4contest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPreferencesObservableTest {

    @Test
    void theDefaultsMatchTheFormerJavaFxProperties() {
        ChatPreferences prefs = new ChatPreferences();

        assertEquals(360.0, prefs.getActualQTF().get(), 0.0001,
                "actualQTF defaulted to 360 as a SimpleDoubleProperty");
        assertEquals("144", prefs.getNotify_optionalFrequencyPrefix().get());
        assertEquals("DO5AMF", prefs.getNotify_DXCSrv_SpottersCallSign().get());
    }

    @Test
    void theQrgValuesStartUnavailable() {
        ChatPreferences prefs = new ChatPreferences();

        assertNull(prefs.getMYQRGFirstCat().get(),
                "an unset own QRG must stay unavailable rather than becoming a fixed band");
        assertNull(prefs.getMYQRGSecondCat().get());
    }

    @Test
    void theListSettingsStartEmpty() {
        ChatPreferences prefs = new ChatPreferences();

        assertTrue(prefs.getLstNotify_QSOSniffer_sniffedWordsList().snapshot().isEmpty());
        assertTrue(prefs.getLstNotify_QSOSniffer_sniffedPrefixLocList().snapshot().isEmpty());
        assertTrue(prefs.getLst_txtSnipList().snapshot().isEmpty());
        assertTrue(prefs.getLst_txtShortCutBtnList().snapshot().isEmpty());
    }
}
