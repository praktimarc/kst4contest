package kst4contest.view.map;

import java.util.List;

/**
 * Tries terrain providers in order and returns the first usable profile.
 */
public final class ChainedTerrainProfileProvider implements TerrainProfileProvider {

    private final List<TerrainProfileProvider> providers;

    public ChainedTerrainProfileProvider(List<TerrainProfileProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    @Override
    public TerrainProfileData loadProfile(TerrainProfileRequest request) {
        TerrainProfileData lastResult = TerrainProfileData.empty("No terrain provider");

        for (TerrainProfileProvider provider : providers) {
            if (provider == null) {
                continue;
            }

            TerrainProfileData currentResult = provider.loadProfile(request);
            if (currentResult != null) {
                lastResult = currentResult;
                if (currentResult.hasUsableProfile()) {
                    return currentResult;
                }
            }
        }

        return lastResult;
    }
}