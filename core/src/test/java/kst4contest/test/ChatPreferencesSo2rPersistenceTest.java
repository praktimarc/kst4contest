package kst4contest.test;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChatPreferencesSo2rPersistenceTest {

    @TempDir
    Path directory;

    @Test
    void so2rSettingsSurviveAFullRoundTrip() {
        Path file = directory.resolve("preferences.xml");
        ChatPreferences written = new ChatPreferences(file);
        written.setTrxSynch_so2rEnabled(true);
        written.setLogsynch_wintestNetworkStationNameOfWintestClient2("STN2");
        written.setStn_pstRotator2Enabled(true);
        written.setStn_pstRotator2Host("10.0.0.7");
        written.setStn_pstRotator2Port(12020);
        assertTrue(written.writePreferencesToXmlFile());

        ChatPreferences restored = new ChatPreferences(file);
        assertTrue(restored.readPreferencesFromXmlFile());

        assertTrue(restored.isTrxSynch_so2rEnabled());
        assertEquals("STN2", restored.getLogsynch_wintestNetworkStationNameOfWintestClient2());
        assertTrue(restored.isStn_pstRotator2Enabled());
        assertEquals("10.0.0.7", restored.getStn_pstRotator2Host());
        assertEquals(12020, restored.getStn_pstRotator2Port());
    }

    @Test
    void anOlderFileWithoutThemYieldsTheDefaults() throws IOException {
        Path file = directory.resolve("old.xml");
        Files.writeString(file, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <configVersion>8</configVersion>
                    <station><stn_pstRotatorPort>12000</stn_pstRotatorPort></station>
                    <trxSynchUCX><trxSynch_ucxLogUDPListenerEnabled>true</trxSynch_ucxLogUDPListenerEnabled></trxSynchUCX>
                </praktiKST>
                """);

        ChatPreferences restored = new ChatPreferences(file);
        assertTrue(restored.readPreferencesFromXmlFile());

        assertFalse(restored.isTrxSynch_so2rEnabled());
        assertEquals("", restored.getLogsynch_wintestNetworkStationNameOfWintestClient2());
        assertFalse(restored.isStn_pstRotator2Enabled());
        assertEquals("127.0.0.1", restored.getStn_pstRotator2Host());
        assertEquals(12010, restored.getStn_pstRotator2Port());
        assertTrue(Double.isNaN(restored.getActualQTF2().get()));
    }

    @Test
    void invalidRotator2ValuesFallBackToTheDefaults() {
        ChatPreferences prefs = new ChatPreferences(directory.resolve("p.xml"));

        prefs.setStn_pstRotator2Host("   ");
        prefs.setStn_pstRotator2Port(70000);

        assertEquals("127.0.0.1", prefs.getStn_pstRotator2Host());
        assertEquals(12010, prefs.getStn_pstRotator2Port());
    }
}
