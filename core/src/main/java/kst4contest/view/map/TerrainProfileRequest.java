package kst4contest.view.map;

/**
 * Immutable request for terrain/profile sampling between two endpoints.
 */
public record TerrainProfileRequest(
        double fromLatitudeDeg,
        double fromLongitudeDeg,
        double toLatitudeDeg,
        double toLongitudeDeg,
        double totalDistanceKm,
        int requestedSampleCount
) {
    public TerrainProfileRequest {
        requestedSampleCount = Math.max(0, requestedSampleCount);
    }

    public boolean hasUsableEndpoints() {
        return Double.isFinite(fromLatitudeDeg)
                && Double.isFinite(fromLongitudeDeg)
                && Double.isFinite(toLatitudeDeg)
                && Double.isFinite(toLongitudeDeg)
                && Double.isFinite(totalDistanceKm)
                && totalDistanceKm >= 0.0;
    }
}