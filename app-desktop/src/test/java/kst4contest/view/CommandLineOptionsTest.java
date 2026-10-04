package kst4contest.view;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandLineOptionsTest {

    @Test
    void profileArgumentIsAcceptedInBothSpellings() {
        assertEquals("OP2", CommandLineOptions.parse(List.of("--profile=OP2")).getRequestedProfileName());
        assertEquals("OP2", CommandLineOptions.parse(List.of("--profile", "OP2")).getRequestedProfileName());
        assertEquals("DM5M Contest",
                CommandLineOptions.parse(List.of("--profile", "DM5M Contest")).getRequestedProfileName());
    }

    @Test
    void missingOrEmptyProfileArgumentsAreTreatedAsAbsent() {
        assertNull(CommandLineOptions.parse(List.of()).getRequestedProfileName());
        assertNull(CommandLineOptions.parse(null).getRequestedProfileName());
        assertNull(CommandLineOptions.parse(List.of("--profile=")).getRequestedProfileName());
        assertNull(CommandLineOptions.parse(List.of("--profile")).getRequestedProfileName());
    }

    @Test
    void unrelatedArgumentsAreIgnoredInsteadOfFailing() {
        assertEquals("OP2",
                CommandLineOptions.parse(List.of("--verbose", "--profile=OP2", "somefile.adi"))
                        .getRequestedProfileName());
        assertNull(CommandLineOptions.parse(List.of("--verbose", "-x")).getRequestedProfileName());
    }

    @Test
    void rememberedOptionsDefaultToEmptyInsteadOfNull() {
        CommandLineOptions.remember(null);
        assertNull(CommandLineOptions.remembered().getRequestedProfileName());

        CommandLineOptions.remember(new CommandLineOptions("OP2"));
        assertEquals("OP2", CommandLineOptions.remembered().getRequestedProfileName());

        CommandLineOptions.remember(null);
    }
}
