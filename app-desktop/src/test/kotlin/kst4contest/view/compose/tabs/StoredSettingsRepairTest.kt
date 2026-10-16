package kst4contest.view.compose.tabs

import kst4contest.model.ChatPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/** The corrections made once before the settings window is built, outside any draft. */
class StoredSettingsRepairTest {

    @TempDir
    lateinit var directory: Path

    private fun preferences() = ChatPreferences(directory.resolve("preferences.xml"))

    @Test
    fun `an unsupported fallback band prefix is written back as 144`() {
        val prefs = preferences()
        prefs.setNotify_optionalFrequencyPrefix("4711")

        repairStoredSettings(prefs) { null }

        assertEquals("144", prefs.getNotify_optionalFrequencyPrefix().get(),
            "the JavaFX window repaired the stored value the same way")
    }

    @Test
    fun `a still-default broadcast address is replaced by the detected one`() {
        val prefs = preferences()
        prefs.setLogsynch_wintestNetworkBroadcastAddress("255.255.255.255")

        repairStoredSettings(prefs) { "192.168.178.255" }

        assertEquals("192.168.178.255", prefs.getLogsynch_wintestNetworkBroadcastAddress(),
            "the JavaFX window auto-detected the subnet broadcast while the stored "
                + "value was still the catch-all default")
    }

    @Test
    fun `a broadcast address the operator chose is never overwritten`() {
        val prefs = preferences()
        prefs.setLogsynch_wintestNetworkBroadcastAddress("10.0.0.255")

        repairStoredSettings(prefs) { "192.168.178.255" }

        assertEquals("10.0.0.255", prefs.getLogsynch_wintestNetworkBroadcastAddress())
    }

    @Test
    fun `a failed detection leaves the default in place`() {
        val prefs = preferences()
        prefs.setLogsynch_wintestNetworkBroadcastAddress("255.255.255.255")

        repairStoredSettings(prefs) { throw IllegalStateException("no interface") }

        assertEquals("255.255.255.255", prefs.getLogsynch_wintestNetworkBroadcastAddress())
    }
}
