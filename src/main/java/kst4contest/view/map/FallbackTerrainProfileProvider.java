package kst4contest.view.map;

import java.util.Objects;

/**
 * Terrain provider wrapper that tries a primary source first and falls back
 * to a secondary provider when the primary source returns no usable profile.
 */
public final class FallbackTerrainProfileProvider implements TerrainProfileProvider {

    private final TerrainProfileProvider primaryProvider;
    private final TerrainProfileProvider fallbackProvider;

    public FallbackTerrainProfileProvider(TerrainProfileProvider primaryProvider,
                                          TerrainProfileProvider fallbackProvider) {
        this.primaryProvider = Objects.requireNonNull(primaryProvider, "primaryProvider");
        this.fallbackProvider = Objects.requireNonNull(fallbackProvider, "fallbackProvider");
    }

    @Override
    public TerrainProfileData loadProfile(TerrainProfileRequest request) {
        TerrainProfileData primaryData = safeLoad(primaryProvider, request);
        if (primaryData.hasUsableProfile()) {
            return primaryData;
        }

        TerrainProfileData fallbackData = safeLoad(fallbackProvider, request);
        if (fallbackData.hasUsableProfile()) {
            return fallbackData;
        }

        return fallbackData.profilePoints().isEmpty() ? primaryData : fallbackData;
    }

    private TerrainProfileData safeLoad(TerrainProfileProvider provider, TerrainProfileRequest request) {
        try {
            TerrainProfileData result = provider.loadProfile(request);
            return result == null
                    ? TerrainProfileData.empty(provider.getClass().getSimpleName())
                    : result;
        } catch (Exception exception) {
            System.err.println("[StationMap] Terrain provider failed: "
                    + provider.getClass().getSimpleName()
                    + " -> " + exception.getMessage());
            return TerrainProfileData.empty(provider.getClass().getSimpleName());
        }
    }
}