package kst4contest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The two designs' colour overrides are stored independently.
 *
 * <p>That is the part worth pinning: two designs sharing one storage mechanism is exactly
 * the shape in which a change to the evening palette quietly drags the daylight one with
 * it.</p>
 */
class ChatPreferencesPaletteTest {

    @Test
    void theTwoDesignsAreStoredIndependently() {
        ChatPreferences prefs = new ChatPreferences();

        prefs.setGuiOptions_paletteOverridesDay("surface=#FFFFFF");
        prefs.setGuiOptions_paletteOverridesEvening("surface=#000000");

        assertEquals("surface=#FFFFFF", prefs.getGuiOptions_paletteOverridesDay());
        assertEquals("surface=#000000", prefs.getGuiOptions_paletteOverridesEvening());
    }

    @Test
    void anUnsetOverrideReadsAsEmptyAndNotAsNull() {
        // A preferences file from an older release has no such element at all.
        ChatPreferences prefs = new ChatPreferences();

        assertEquals("", prefs.getGuiOptions_paletteOverridesDay());
        assertEquals("", prefs.getGuiOptions_paletteOverridesEvening());
    }

    @Test
    void aNullSetterValueBecomesEmpty() {
        ChatPreferences prefs = new ChatPreferences();

        prefs.setGuiOptions_paletteOverridesDay(null);
        prefs.setGuiOptions_paletteOverridesEvening(null);

        assertEquals("", prefs.getGuiOptions_paletteOverridesDay());
        assertEquals("", prefs.getGuiOptions_paletteOverridesEvening());
    }
}
