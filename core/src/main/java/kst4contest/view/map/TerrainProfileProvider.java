package kst4contest.view.map;

/**
 * Abstraction for terrain/profile retrieval.
 */
public interface TerrainProfileProvider {

    TerrainProfileData loadProfile(TerrainProfileRequest request);
}