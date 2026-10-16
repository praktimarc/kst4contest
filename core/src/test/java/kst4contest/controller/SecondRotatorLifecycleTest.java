package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SecondRotatorLifecycleTest {

    @TempDir
    Path directory;

    @Test
    void rotatorTwoIsWantedOnlyWithSo2rAndItsOwnSwitch() {
        ChatPreferences prefs = new ChatPreferences(directory.resolve("p.xml"));
        assertFalse(ChatController.secondRotatorWanted(prefs));

        prefs.setStn_pstRotator2Enabled(true);
        assertFalse(ChatController.secondRotatorWanted(prefs), "rotator 2 without SO2R");

        prefs.setTrxSynch_so2rEnabled(true);
        assertTrue(ChatController.secondRotatorWanted(prefs));

        prefs.setStn_pstRotator2Enabled(false);
        assertFalse(ChatController.secondRotatorWanted(prefs));
    }
}
