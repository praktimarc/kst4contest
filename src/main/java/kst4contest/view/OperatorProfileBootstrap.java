package kst4contest.view;

import kst4contest.controller.OperatorProfilePaths;
import kst4contest.controller.OperatorProfileStore;
import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;

import java.util.List;
import java.util.Optional;

/**
 * Decides which operator profile the application starts with.
 *
 * <p>The class contains no user interface code so the decision can be tested headless.
 * Asking the operator is delegated to an {@link OperatorProfileChoiceRequester}, and a
 * problem worth telling the operator about is reported through
 * {@link #getStartupWarning()} instead of being shown here.</p>
 *
 * <p>The most important property of this logic is what it does <em>not</em> do: an
 * installation with no or exactly one profile is resolved without asking anything and
 * without touching a single file, so a single operator start stays exactly as fast and
 * as quiet as it was before profiles existed.</p>
 */
public class OperatorProfileBootstrap {

    private String startupWarning;

    /**
     * Resolves the operator profile to start with.
     *
     * @param store           registry to read the profiles from
     * @param commandLineOptions parsed command line options
     * @param choiceRequester requester used when the operator has to choose
     * @return the resolved selection, or null when the operator chose to quit
     */
    public OperatorProfileSelection resolveAtStartup(final OperatorProfileStore store,
            final CommandLineOptions commandLineOptions,
            final OperatorProfileChoiceRequester choiceRequester) {

        startupWarning = null;

        List<OperatorProfile> availableProfiles = store.loadProfiles();
        String requestedProfileName = commandLineOptions == null
                ? null
                : commandLineOptions.getRequestedProfileName();

        if (requestedProfileName != null) {
            OperatorProfile requestedProfile = findProfile(availableProfiles, requestedProfileName);

            if (requestedProfile != null) {
                return OperatorProfilePaths.resolve(requestedProfile);
            }

            startupWarning = "The operator profile \"" + requestedProfileName
                    + "\" is unknown. KST4Contest continues with the normal profile selection.";
        }

        if (availableProfiles.isEmpty()) {
            // No registry at all: the historic flat installation is the only profile.
            return OperatorProfilePaths.resolve(store.buildImplicitRootProfile());
        }

        if (availableProfiles.size() == 1) {
            return OperatorProfilePaths.resolve(availableProfiles.get(0));
        }

        String preselectedProfileId = store.loadLastUsedProfileId().orElse(null);
        Optional<OperatorProfile> chosenProfile =
                choiceRequester.requestProfileChoice(availableProfiles, preselectedProfileId);

        return chosenProfile.map(OperatorProfilePaths::resolve).orElse(null);
    }

    /**
     * Returns a message that should be shown to the operator after startup.
     *
     * @return the warning text, or null when startup was unremarkable
     */
    public String getStartupWarning() {
        return startupWarning;
    }

    /**
     * Finds a profile by identifier or display name, ignoring case.
     *
     * @param availableProfiles profiles to search
     * @param requestedName     identifier or display name entered by the operator
     * @return the matching profile, or null
     */
    private static OperatorProfile findProfile(final List<OperatorProfile> availableProfiles,
            final String requestedName) {

        for (OperatorProfile currentProfile : availableProfiles) {
            if (requestedName.equalsIgnoreCase(currentProfile.getProfileId())) {
                return currentProfile;
            }
        }

        for (OperatorProfile currentProfile : availableProfiles) {
            if (requestedName.equalsIgnoreCase(currentProfile.getDisplayName())) {
                return currentProfile;
            }
        }

        return null;
    }
}
