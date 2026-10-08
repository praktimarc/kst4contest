package kst4contest.view.map;

/**
 * Standard EPSG:3857 Web Mercator projection mathematics.
 */
public final class WebMercatorProjection {

    private WebMercatorProjection() {
    }

    public static double lonToX(double lon, int zoom) {
        double n = Math.pow(2.0, zoom);
        return ((lon + 180.0) / 360.0) * 256.0 * n;
    }

    public static double latToY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        double n = Math.pow(2.0, zoom);
        double y = (1.0 - Math.log(Math.tan(latRad) + (1.0 / Math.cos(latRad))) / Math.PI) / 2.0;
        return y * 256.0 * n;
    }

    public static double xToLon(double x, int zoom) {
        double n = Math.pow(2.0, zoom);
        return (x / (256.0 * n)) * 360.0 - 180.0;
    }

    public static double yToLat(double y, int zoom) {
        double n = Math.pow(2.0, zoom);
        double latRad = Math.atan(Math.sinh(Math.PI * (1.0 - 2.0 * y / (256.0 * n))));
        return Math.toDegrees(latRad);
    }
}
