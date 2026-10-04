package kst4contest.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The six roles the operator may set.
 *
 * <p>A role is a selector and a property, not a property alone: the shipped sheets state
 * three of the six outside `.root`, and `-fx-background-color` on its own appears a dozen
 * times in one sheet. The selector is what tells the separator's line from a scroll bar.</p>
 *
 * <p>All three strings are part of a file format -- two of them the operator's own
 * hand-edited stylesheet, the third the preferences XML. Renaming any of them silently
 * drops a colour somebody chose, which is why they are pinned here rather than inlined.</p>
 */
class PaletteRoleTest {

    @Test
    void thereAreSixRolesAndEachNamesOneDeclaration() {
        assertEquals(6, PaletteRole.values().length);

        Set<String> declarations = new HashSet<>();
        for (PaletteRole role : PaletteRole.values()) {
            assertTrue(role.cssProperty().startsWith("-fx-"), role + " has no CSS property");
            assertTrue(role.selector().startsWith("."), role + " has no selector");
            assertTrue(
                    declarations.add(role.selector() + "|" + role.cssProperty()),
                    role + " names the same declaration as another role");
        }
    }

    @Test
    void theThreeRolesOutsideRootKeepTheirOwnSelectors() {
        /*
         * These three are the reason a role carries a selector at all. The values are the
         * ones JavaFxStylesheet.read already uses, so the two must not drift apart.
         */
        assertEquals(".label", PaletteRole.TEXT.selector());
        assertEquals("-fx-text-fill", PaletteRole.TEXT.cssProperty());

        assertEquals(".separator *.line", PaletteRole.SEPARATOR.selector());
        assertEquals("-fx-background-color", PaletteRole.SEPARATOR.cssProperty());

        assertEquals(".root", PaletteRole.SURFACE.selector());
    }

    @Test
    void theStorageKeysAreDistinctAndStable() {
        // These strings land in the operator's preferences XML. Renaming one drops a colour.
        assertEquals("surface", PaletteRole.SURFACE.key());
        assertEquals("windowSurface", PaletteRole.WINDOW_SURFACE.key());
        assertEquals("fieldInterior", PaletteRole.FIELD_INTERIOR.key());
        assertEquals("text", PaletteRole.TEXT.key());
        assertEquals("accent", PaletteRole.ACCENT.key());
        assertEquals("separator", PaletteRole.SEPARATOR.key());
    }

    @Test
    void aDeclarationResolvesBackToItsRole() {
        assertEquals(PaletteRole.SURFACE, PaletteRole.of(".root", "-fx-base"));
        assertEquals(PaletteRole.TEXT, PaletteRole.of(".label", "-fx-text-fill"));
    }

    @Test
    void theSamePropertyUnderAnotherSelectorIsNotThatRole() {
        /*
         * The whole point of carrying the selector. Both sheets set -fx-text-fill on a
         * dozen selectors and -fx-background-color on more; only one of each is a role.
         */
        assertNull(PaletteRole.of(".button:hover", "-fx-text-fill"));
        assertNull(PaletteRole.of(".scroll-bar", "-fx-background-color"));
    }

    @Test
    void anUnknownDeclarationIsNotARole() {
        assertNull(PaletteRole.of(".root", "-fx-font-size"));
        assertNull(PaletteRole.of("", ""));
        assertNull(PaletteRole.of(null, null));
    }
}
