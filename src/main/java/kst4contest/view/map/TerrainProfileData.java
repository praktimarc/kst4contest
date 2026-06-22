package kst4contest.view.map;

import java.util.List;

/**
 * Immutable terrain/profile payload including source metadata.
 */
public record TerrainProfileData(
        List<PathProfilePoint> profilePoints,
        String sourceName,
        boolean synthetic
) {

    public TerrainProfileData {
        profilePoints = profilePoints == null ? List.of() : List.copyOf(profilePoints);
        sourceName = sourceName == null ? "" : sourceName.trim();
    }

    public static TerrainProfileData empty(String sourceName) {
        return new TerrainProfileData(List.of(), sourceName, false);
    }

    public boolean hasUsableProfile() {
        return profilePoints.size() >= 2;
    }
}