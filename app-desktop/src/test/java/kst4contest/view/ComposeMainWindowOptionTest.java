package kst4contest.view;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The switch that opens the Compose main window beside the JavaFX one.
 *
 * <p>Off unless asked for. The two windows are meant to run side by side while the operator
 * compares them against a live session, and until that comparison is done nothing about an
 * ordinary start may change.</p>
 */
class ComposeMainWindowOptionTest {

    @Test
    void anOrdinaryStartDoesNotOpenTheComposeWindow() {
        assertFalse(CommandLineOptions.parse(List.of()).isComposeMainWindowRequested());
        assertFalse(CommandLineOptions.parse(null).isComposeMainWindowRequested());
        assertFalse(
                CommandLineOptions.parse(List.of("--profile", "contest"))
                        .isComposeMainWindowRequested());
    }

    @Test
    void theSwitchOpensIt() {
        assertTrue(
                CommandLineOptions.parse(List.of("--compose-main-window"))
                        .isComposeMainWindowRequested());
    }

    /** Alongside a profile, because the comparison is run inside one. */
    @Test
    void theSwitchCombinesWithAProfile() {
        CommandLineOptions options =
                CommandLineOptions.parse(List.of("--profile", "contest", "--compose-main-window"));

        assertTrue(options.isComposeMainWindowRequested());
        assertTrue("contest".equals(options.getRequestedProfileName()));
    }

    /** A near miss must not switch it on by accident. */
    @Test
    void anUnrelatedArgumentIsNotTheSwitch() {
        assertFalse(
                CommandLineOptions.parse(List.of("--compose-main-window-please"))
                        .isComposeMainWindowRequested());
    }
}
