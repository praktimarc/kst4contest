package kst4contest.view;

import java.util.List;

/**
 * Command line options of the application.
 *
 * <p>The parsed value is additionally kept in a process wide holder. JavaFX only knows
 * the parameters of the {@code Application} instance it launched itself, so an instance
 * created during a profile switch would see no parameters at all. Parsing once at
 * startup and remembering the result avoids that entirely.</p>
 */
public class CommandLineOptions {

    /**
     * Command line switch selecting the operator profile to start with.
     */
    public static final String PROFILE_ARGUMENT = "--profile";

    /**
     * System property used as an alternative to the command line switch.
     */
    public static final String PROFILE_SYSTEM_PROPERTY = "kst4contest.profile";

    /**
     * Opens the Compose main window beside the JavaFX one.
     *
     * <p>Off unless asked for. The two are meant to run side by side while the operator
     * compares them against a live session; until that comparison is done, an ordinary start
     * must behave exactly as it did.</p>
     */
    public static final String COMPOSE_MAIN_WINDOW_ARGUMENT = "--compose-main-window";

    private static volatile CommandLineOptions rememberedOptions = new CommandLineOptions(null);

    private final String requestedProfileName;

    private final boolean composeMainWindowRequested;

    public CommandLineOptions(final String requestedProfileName) {
        this(requestedProfileName, false);
    }

    public CommandLineOptions(
            final String requestedProfileName,
            final boolean composeMainWindowRequested) {
        this.composeMainWindowRequested = composeMainWindowRequested;
        this.requestedProfileName = requestedProfileName;
    }

    /**
     * Parses the raw application arguments.
     *
     * <p>Unknown arguments are ignored on purpose. A typo in a command line must never
     * keep an operator out of the application shortly before a contest.</p>
     *
     * @param rawArguments raw arguments, may be null
     * @return the parsed options
     */
    public static CommandLineOptions parse(final List<String> rawArguments) {

        String requestedProfileName = null;
        boolean composeMainWindow = false;

        if (rawArguments != null) {
            for (int argumentIndex = 0; argumentIndex < rawArguments.size(); argumentIndex++) {
                String currentArgument = rawArguments.get(argumentIndex);

                if (currentArgument == null) {
                    continue;
                }

                if (COMPOSE_MAIN_WINDOW_ARGUMENT.equals(currentArgument)) {
                    composeMainWindow = true;
                } else if (currentArgument.startsWith(PROFILE_ARGUMENT + "=")) {
                    requestedProfileName = currentArgument.substring(PROFILE_ARGUMENT.length() + 1);
                } else if (PROFILE_ARGUMENT.equals(currentArgument)
                        && argumentIndex + 1 < rawArguments.size()) {
                    requestedProfileName = rawArguments.get(argumentIndex + 1);
                    argumentIndex++;
                }
            }
        }

        if (requestedProfileName == null || requestedProfileName.isBlank()) {
            requestedProfileName = System.getProperty(PROFILE_SYSTEM_PROPERTY);
        }

        if (requestedProfileName != null && requestedProfileName.isBlank()) {
            requestedProfileName = null;
        }

        return new CommandLineOptions(
                requestedProfileName == null ? null : requestedProfileName.trim(),
                composeMainWindow);
    }

    /**
     * Stores the parsed options for the lifetime of the process.
     *
     * @param options options to remember
     */
    public static void remember(final CommandLineOptions options) {
        rememberedOptions = options == null ? new CommandLineOptions(null) : options;
    }

    /**
     * Returns the options parsed at application startup.
     *
     * @return the remembered options, never null
     */
    public static CommandLineOptions remembered() {
        return rememberedOptions;
    }

    /**
     * Returns the operator profile requested on the command line.
     *
     * @return the requested profile name, or null when none was given
     */
    /**
     * Whether the Compose main window should be opened beside the JavaFX one.
     *
     * @return {@code true} only when --compose-main-window was passed
     */
    public boolean isComposeMainWindowRequested() {
        return composeMainWindowRequested;
    }

    public String getRequestedProfileName() {
        return requestedProfileName;
    }
}
