package kst4contest.view.map;

/**
 * Placeholder terrain provider.
 */
public final class NoOpTerrainProfileProvider implements TerrainProfileProvider {

    @Override
    public TerrainProfileData loadProfile(TerrainProfileRequest request) {
        return TerrainProfileData.empty("No terrain provider");
    }
}