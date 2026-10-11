package kst4contest.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the QSO sniffer callsign list against the historical {@code <snifferWords>} block.
 * Older preference files still carry that element; its entries must not end up in the
 * monitored-callsign list (issue #103).
 */
class ChatPreferencesSnifferListTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void legacySnifferWordsDoNotLeakIntoTheCallsignList() throws IOException {
        Path preferencesFile = temporaryDirectory.resolve("sniffer.xml");
        Files.writeString(preferencesFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <praktiKST>
                    <snifferWords>
                        <w>DL1ABC</w>
                    </snifferWords>
                    <snifferCallSigns>
                        <callSign>DN9APW-2</callSign>
                    </snifferCallSigns>
                </praktiKST>
                """);

        ChatPreferences preferences = new ChatPreferences(preferencesFile);
        assertTrue(preferences.readPreferencesFromXmlFile());

        assertEquals(List.of("DN9APW"),
                preferences.getLstNotify_QSOSniffer_sniffedCallSignList().snapshot(),
                "only the <snifferCallSigns> entry, normalised to its base callsign, "
                        + "may be monitored; <snifferWords> is an ignored legacy element");
    }
}
