package kst4contest.view;

import kst4contest.model.OperatorProfile;

import java.util.List;
import java.util.Optional;

/**
 * Asks the operator which profile to start with.
 *
 * <p>The startup logic depends on this interface rather than on a dialog, so the
 * decision which profile to use can be tested without a JavaFX runtime.</p>
 */
@FunctionalInterface
public interface OperatorProfileChoiceRequester {

    /**
     * Requests a profile choice.
     *
     * @param selectableProfiles  profiles to choose from, never empty
     * @param preselectedProfileId identifier to preselect, may be null
     * @return the chosen profile, or empty when the operator wants to quit
     */
    Optional<OperatorProfile> requestProfileChoice(List<OperatorProfile> selectableProfiles,
            String preselectedProfileId);
}
