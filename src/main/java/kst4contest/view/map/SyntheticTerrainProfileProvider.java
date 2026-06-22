package kst4contest.view.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic synthetic fallback profile provider for UI/testing.
 *
 * This provider intentionally stays fully offline and deterministic.
 * It is only used when no real offline DEM data is available.
 */
public final class SyntheticTerrainProfileProvider implements TerrainProfileProvider {

    @Override
    public TerrainProfileData loadProfile(TerrainProfileRequest request) {
        if (request == null || !request.hasUsableEndpoints() || request.requestedSampleCount() < 2) {
            return TerrainProfileData.empty("Synthetic fallback profile");
        }

        int sampleCount = Math.max(2, request.requestedSampleCount());
        List<PathProfilePoint> points = new ArrayList<>(sampleCount);

        for (int i = 0; i < sampleCount; i++) {
            double t = sampleCount == 1 ? 0.0 : (double) i / (double) (sampleCount - 1);

            double latitudeDeg = interpolate(request.fromLatitudeDeg(), request.toLatitudeDeg(), t);
            double longitudeDeg = interpolate(request.fromLongitudeDeg(), request.toLongitudeDeg(), t);
            double distanceKm = request.totalDistanceKm() * t;

            double elevationMeters = syntheticElevationMeters(t, request.totalDistanceKm());

            points.add(new PathProfilePoint(
                    distanceKm,
                    latitudeDeg,
                    longitudeDeg,
                    elevationMeters
            ));
        }

        return new TerrainProfileData(points, "Synthetic fallback profile", true);
    }

    private double interpolate(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private double syntheticElevationMeters(double t, double totalDistanceKm) {
        double baseLevelMeters = 80.0 + Math.min(60.0, totalDistanceKm * 0.12);

        double broadHill = 90.0 * Math.sin(Math.PI * t);
        double window = Math.pow(Math.sin(Math.PI * t), 1.35);
        double secondaryShape = window * 28.0 * Math.sin(3.0 * Math.PI * t + 0.55);
        double fineStructure = window * 10.0 * Math.cos(7.0 * Math.PI * t + 0.25);

        return Math.max(0.0, baseLevelMeters + broadHill + secondaryShape + fineStructure);
    }
}