package kst4contest.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a set of colour overrides survives in the preferences XML.
 *
 * <p>One string per design rather than twelve elements. The parsing is where an operator's
 * stored colours are either kept or quietly lost, and the input is not always ours: a
 * hand-edited XML, a file written by an older version, or a half-finished write.</p>
 */
class PaletteOverridesTest {

    @Test
    void anEmptyStoreMeansNoOverridesRatherThanAFailure() {
        assertTrue(PaletteOverrides.parse(null).isEmpty());
        assertTrue(PaletteOverrides.parse("").isEmpty());
        assertTrue(PaletteOverrides.parse("   ").isEmpty());
    }

    @Test
    void onlyTheRolesActuallyNamedComeBack() {
        Map<PaletteRole, String> parsed = PaletteOverrides.parse("surface=#ECECEC");

        // The whole point of the layering: one named role overrides one role.
        assertEquals(1, parsed.size());
        assertEquals("#ECECEC", parsed.get(PaletteRole.SURFACE));
    }

    @Test
    void aRoundTripKeepsEveryRole() {
        Map<PaletteRole, String> original = new LinkedHashMap<>();
        for (PaletteRole role : PaletteRole.values()) {
            original.put(role, "#10203" + role.ordinal());
        }

        assertEquals(original, PaletteOverrides.parse(PaletteOverrides.format(original)));
    }

    @Test
    void anUnknownKeyIsSkippedAndTheRestSurvives() {
        // An older or newer version may have written a role this one does not know.
        Map<PaletteRole, String> parsed =
                PaletteOverrides.parse("surface=#ECECEC;chrome=#123456;accent=#0096C9");

        assertEquals(2, parsed.size());
        assertEquals("#ECECEC", parsed.get(PaletteRole.SURFACE));
        assertEquals("#0096C9", parsed.get(PaletteRole.ACCENT));
    }

    @Test
    void anUnparsableColourIsSkippedRatherThanStored() {
        /*
         * This is the difference between an operator losing one colour and an operator
         * losing the readability of the whole client: a bad value must not become black.
         */
        Map<PaletteRole, String> parsed =
                PaletteOverrides.parse("surface=rot;accent=#0096C9;text=#GGGGGG;separator=");

        assertEquals(1, parsed.size());
        assertEquals("#0096C9", parsed.get(PaletteRole.ACCENT));
    }

    @Test
    void garbageIsSurvivedWithoutAnException() {
        // Half-written files exist. None of these may throw.
        for (String garbage : new String[] {
                ";;;", "=", "surface", "surface=", "=#ECECEC", "surface==#ECECEC",
                "surface=#ECECEC;", "\n\t", "a=b=c",
        }) {
            PaletteOverrides.parse(garbage);
        }
    }

    @Test
    void whatCountsAsAColour() {
        assertTrue(PaletteOverrides.isValidColour("#ECECEC"));
        assertTrue(PaletteOverrides.isValidColour("#ececec"));
        assertTrue(PaletteOverrides.isValidColour("  #ECECEC  "));

        assertFalse(PaletteOverrides.isValidColour(null));
        assertFalse(PaletteOverrides.isValidColour(""));
        assertFalse(PaletteOverrides.isValidColour("ECECEC"));
        assertFalse(PaletteOverrides.isValidColour("#ECECE"));
        assertFalse(PaletteOverrides.isValidColour("#ECECECE"));
        assertFalse(PaletteOverrides.isValidColour("#GGGGGG"));
        assertFalse(PaletteOverrides.isValidColour("rot"));
    }

    @Test
    void formattingAnEmptyMapGivesAnEmptyString() {
        assertEquals("", PaletteOverrides.format(new LinkedHashMap<>()));
    }
}
